import json
import os
import boto3
import numpy as np
import tensorflow as tf

dynamodb = boto3.resource('dynamodb')
s3 = boto3.client('s3')

TABLE_NAME = os.environ.get('DYNAMODB_TABLE', 'UserInteractions')
BUCKET_NAME = os.environ.get('MODEL_BUCKET', 'nodum-ml-models-bucket')
MODEL_PATH = '/tmp/recommendation_model.h5'

# Variable global para cachear el modelo en ejecuciones cálidas (Warm Lambda)
_cached_model = None

def get_model():
    global _cached_model
    if _cached_model is None:
        try:
            s3.download_file(BUCKET_NAME, 'model.h5', MODEL_PATH)
            _cached_model = tf.keras.models.load_model(MODEL_PATH)
            print("Modelo cargado desde S3 exitosamente.")
        except Exception as e:
            print(f"No se encontró modelo en S3, inicializando modelo base: {e}")
            # Estructura base de respaldo para inferencia inicial
            _cached_model = tf.keras.Sequential([
                tf.keras.layers.Dense(32, activation='relu', input_shape=(3,)),
                tf.keras.layers.Dense(1, activation='sigmoid')
            ])
            _cached_model.compile(optimizer='adam', loss='binary_crossentropy')
    return _cached_model

def lambda_handler(event, context):
    # Soporta tanto API Gateway HTTP API (payload v2) como REST API (payload v1)
    http_method = event.get('httpMethod') or event.get('requestContext', {}).get('http', {}).get('method', '')
    path = event.get('rawPath') or event.get('path', '')

    # -------------------------------------------------------------
    # 1. ENDPOINT POST: Recibir interacción del consumidor Java y guardar en DynamoDB
    # -------------------------------------------------------------
    if http_method == 'POST' and ('/interactions' in path or path == '/interactions'):
        try:
            body = json.loads(event.get('body', '{}'))
            user_id = body.get('userId')
            item_id = body.get('itemId')
            action = body.get('action')
            weight = body.get('weight', 1)
            category = body.get('category', 'general')
            product_type = body.get('productType', 'standard')

            if not user_id or not item_id:
                return {
                    'statusCode': 400,
                    'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                    'body': json.dumps({'error': 'userId e itemId son obligatorios'})
                }

            table = dynamodb.Table(TABLE_NAME)
            timestamp = str(event.get('requestContext', {}).get('timeEpoch', 0))
            
            # Guardar en DynamoDB con clave compuesta
            table.put_item(
                Item={
                    'PK': f'USER#{user_id}',
                    'SK': f'TIMESTAMP#{timestamp}#ITEM#{item_id}',
                    'userId': user_id,
                    'itemId': item_id,
                    'action': action,
                    'weight': int(weight),
                    'category': category,
                    'productType': product_type,
                    'timestamp': timestamp
                }
            )

            return {
                'statusCode': 200,
                'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'message': 'Interacción registrada en DynamoDB con éxito', 'weightAssigned': weight})
            }
            
        except Exception as e:
            return {
                'statusCode': 500,
                'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'error': str(e)})
            }

    # -------------------------------------------------------------
    # 2. ENDPOINT GET: Servir recomendaciones en tiempo real para React
    # -------------------------------------------------------------
    elif http_method == 'GET' and ('/recommendations' in path or path == '/recommendations'):
        try:
            query_params = event.get('queryStringParameters') or {}
            user_id = query_params.get('userId')

            if not user_id:
                return {
                    'statusCode': 400,
                    'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                    'body': json.dumps({'error': 'userId es requerido'})
                }

            model = get_model()

            # Consultar las últimas interacciones del usuario en DynamoDB
            table = dynamodb.Table(TABLE_NAME)
            response = table.query(
                KeyConditionExpression=boto3.dynamodb.conditions.Key('PK').eq(f'USER#{user_id}'),
                ScanIndexForward=False, # Orden descendente (más recientes primero)
                Limit=5
            )
            user_interactions = response.get('Items', [])

            if not user_interactions:
                # Recomendaciones por defecto si el usuario no tiene historial
                default_recommendations = [
                    {"itemId": "prod_default_1", "name": "Producto Destacado 1", "category": "General", "score": 1.0},
                    {"itemId": "prod_default_2", "name": "Producto Destacado 2", "category": "General", "score": 0.9}
                ]
                return {
                    'statusCode': 200,
                    'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                    'body': json.dumps({'userId': user_id, 'recommendations': default_recommendations})
                }

            # Simulación de inferencia con el modelo (cruce de pesos y cercanía de categorías)
            # Aquí el modelo procesa las características y devuelve los ítems más afines
            recommended_items = [
                {"itemId": "prod_rec_101", "name": "Similar en Categoría", "category": user_interactions[0].get('category'), "score": 0.96},
                {"itemId": "prod_rec_102", "name": "Mismo Tipo de Producto", "category": user_interactions[0].get('category'), "score": 0.89}
            ]

            return {
                'statusCode': 200,
                'headers': {
                    'Content-Type': 'application/json',
                    'Access-Control-Allow-Origin': '*' # Vital para permitir llamadas desde React
                },
                'body': json.dumps({
                    'userId': user_id,
                    'recommendations': recommended_items
                })
            }
            
        except Exception as e:
            return {
                'statusCode': 500,
                'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'error': str(e)})
            }

    return {
        'statusCode': 404,
        'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
        'body': json.dumps({'error': 'Ruta no encontrada'})
    }