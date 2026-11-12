import os
import json
import boto3
from boto3.dynamodb.conditions import Key
import numpy as np
import tensorflow as tf
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional

app = FastAPI(title="Nodum ML Recommendation Engine", version="1.0")

# Habilitar CORS para que React pueda consumir los endpoints sin problemas
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuración de AWS y DynamoDB
dynamodb = boto3.resource('dynamodb', region_name=os.environ.get('AWS_REGION', 'us-east-1'))
s3 = boto3.client('s3', region_name=os.environ.get('AWS_REGION', 'us-east-1'))

TABLE_NAME = os.environ.get('DYNAMODB_TABLE', 'UserInteractions')
BUCKET_NAME = os.environ.get('MODEL_BUCKET', 'nodum-ml-models-bucket')
MODEL_PATH = '/tmp/recommendation_model.h5'

# Variable global para mantener el modelo en memoria (evita recargarlo en cada petición)
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

# Esquemas de Pydantic para validar los datos de entrada
class InteractionPayload(BaseModel):
    userId: str
    itemId: str
    action: str
    weight: Optional[int] = 1
    category: Optional[str] = "general"
    productType: Optional[str] = "standard"


# -------------------------------------------------------------
# 1. ENDPOINT POST: Recibir interacción y guardar en DynamoDB
# -------------------------------------------------------------
@app.post("/interactions")
def register_interaction(payload: InteractionPayload):
    try:
        table = dynamodb.Table(TABLE_NAME)
        timestamp = str(int(tf.timestamp().numpy())) if hasattr(tf, 'timestamp') else "1000"
        
        # 1. Guardar en DynamoDB (Histórico)
        table.put_item(
            Item={
                'PK': f'USER#{payload.userId}',
                'SK': f'TIMESTAMP#{timestamp}#ITEM#{payload.itemId}',
                'userId': payload.userId,
                'itemId': payload.itemId,
                'action': payload.action,
                'weight': int(payload.weight),
                'category': payload.category,
                'productType': payload.productType,
                'timestamp': timestamp
            }
        )

        # 2. Reentrenamiento en caliente (Incremental Learning con TensorFlow)
        model = get_model()
        
        # Preparar los datos de esta interacción singular como ejemplo de entrenamiento
        # (Ej: si la categoría es 'electronica' damos 1.0, si no 0.0, el peso y un target simulado de 1.0 por ser interacción real)
        X_train = np.array([[1.0 if payload.category == 'electronica' else 0.0, float(payload.weight), 0.8]], dtype=np.float32)
        y_train = np.array([[1.0]], dtype=np.float32) # Target: alta afinidad porque interactuó con él
        
        # Entrenar por 1 sola época (epoch) con este nuevo dato de forma síncrona
        model.fit(X_train, y_train, epochs=1, verbose=0)
        
        print(f"Modelo actualizado en caliente con la interacción del usuario {payload.userId}")

        return {
            "status": "success",
            "message": "Interacción registrada y modelo actualizado en caliente",
            "weightAssigned": payload.weight
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# -------------------------------------------------------------
# 2. ENDPOINT GET: Servir recomendaciones en tiempo real
# -------------------------------------------------------------
@app.get("/recommendations")
def get_recommendations(userId: str):
    try:
        model = get_model()

        # 1. Obtener el historial reciente del usuario desde DynamoDB
        table = dynamodb.Table(TABLE_NAME)
        response = table.query(
            KeyConditionExpression=Key('userId').eq(userId),
            ScanIndexForward=False,
            Limit=5
        )
        user_interactions = response.get('Items', [])

        # Si el usuario no tiene interacciones, retornar destacados por defecto
        if not user_interactions:
            default_recommendations = [
                {"itemId": "item_101", "name": "Audífonos Inalámbricos Pro", "category": "electronica", "score": 1.0},
                {"itemId": "item_102", "name": "Teclado Mecánico RGB Gamer", "category": "electronica", "score": 0.9}
            ]
            return {
                "userId": userId,
                "recommendations": default_recommendations
            }
        categoria_pesos = {}
        for interaction in user_interactions:
            cat = interaction.get('category', 'general')
            w = float(interaction.get('weight', 1))
            categoria_pesos[cat] = categoria_pesos.get(cat, 0) + w
        # Catálogo real de productos
        catalogo_productos = [
            {"itemId": "item_101", "name": "Audífonos Inalámbricos Pro", "category": "electronica", "subcategory": "audio", "brand": "Sony", "price": 89990, "rating": 4.8},
            {"itemId": "item_102", "name": "Teclado Mecánico RGB Gamer", "category": "electronica", "subcategory": "perifericos", "brand": "Redragon", "price": 45990, "rating": 4.6},
            {"itemId": "item_103", "name": "Zapatillas Running Air", "category": "ropa", "subcategory": "calzado", "brand": "Nike", "price": 69990, "rating": 4.7},
            {"itemId": "item_104", "name": "Polerón con Cierre Sport", "category": "ropa", "subcategory": "vestuario", "brand": "Adidas", "price": 39990, "rating": 4.4},
            {"itemId": "item_105", "name": "Cafetera Italiana Express", "category": "hogar", "subcategory": "cocina", "brand": "DeLonghi", "price": 55990, "rating": 4.9},
            {"itemId": "item_106", "name": "Mochila Antirrobo Portátil", "category": "hogar", "subcategory": "accesorios", "brand": "XD Design", "price": 34990, "rating": 4.3},
            {"itemId": "item_107", "name": "Smartwatch Deportivo Fit", "category": "electronica", "subcategory": "wearables", "brand": "Xiaomi", "price": 29990, "rating": 4.5},
            {"itemId": "item_108", "name": "Proteína Whey 1kg", "category": "deportes", "subcategory": "suplementos", "brand": "Optimum Nutrition", "price": 49990, "rating": 4.9},
            {"itemId": "item_109", "name": "Monitor 24\" IPS 144Hz", "category": "electronica", "subcategory": "monitores", "brand": "LG", "price": 179900, "rating": 4.9},
            {"itemId": "item_110", "name": "Mouse Inalámbrico Ergonómico", "category": "electronica", "subcategory": "perifericos", "brand": "Logitech", "price": 24990, "rating": 4.7},
            {"itemId": "item_111", "name": "Parlante Bluetooth Resistente al Agua", "category": "electronica", "subcategory": "audio", "brand": "JBL", "price": 59990, "rating": 4.8},
            {"itemId": "item_112", "name": "Notebook Gamer 15.6\"", "category": "electronica", "subcategory": "computacion", "brand": "ASUS", "price": 799900, "rating": 4.6},
            {"itemId": "item_113", "name": "Chaqueta Impermeable Trekking", "category": "ropa", "subcategory": "vestuario", "brand": "The North Face", "price": 89990, "rating": 4.9},
            {"itemId": "item_114", "name": "Jeans Slim Fit Clásicos", "category": "ropa", "subcategory": "vestuario", "brand": "Levi's", "price": 42990, "rating": 4.3},
            {"itemId": "item_115", "name": "Gorro Deportivo Dri-FIT", "category": "ropa", "subcategory": "accesorios", "brand": "Nike", "price": 14990, "rating": 4.6},
            {"itemId": "item_116", "name": "Aspiradora Robot Inteligente", "category": "hogar", "subcategory": "electrodomesticos", "brand": "Roborock", "price": 249900, "rating": 4.8},
            {"itemId": "item_117", "name": "Set de Sartenes Antiadherentes", "category": "hogar", "subcategory": "cocina", "brand": "T-fal", "price": 39990, "rating": 4.5},
            {"itemId": "item_118", "name": "Mancuernas Ajustables 20kg", "category": "deportes", "subcategory": "fitness", "brand": "ProForm", "price": 99990, "rating": 4.7},
            {"itemId": "item_119", "name": "Bicicleta de Spinning Indoor", "category": "deportes", "subcategory": "ciclismo", "brand": "Oxford", "price": 349900, "rating": 4.4},
            {"itemId": "item_120", "name": "Mat de Yoga Antideslizante", "category": "deportes", "subcategory": "fitness", "brand": "Manduka", "price": 21990, "rating": 4.8}
        ]

        # Extraer la categoría de la última interacción del usuario
        ultima_categoria = user_interactions[0].get('category', 'electronica')
        peso_interaccion = float(user_interactions[0].get('weight', 1))

        # 2. INICIALIZAR Y LLENAR features_list AQUÍ
        features_list = []
        features_list = []
        for prod in catalogo_productos:
            prod_cat = prod['category']
            
            # Si la categoría del producto está en el historial del usuario, 
            # heredamos el puntaje acumulado de esa categoría; si no, vale 0.0
            cat_affinity = float(categoria_pesos.get(prod_cat, 0.0))
            
            # Normalizar un poco el valor para que la red no se sature (ej: dividir por un factor o usar escala 0-1)
            cat_match_score = min(cat_affinity / 10.0, 1.0) 
            
            rating_norm = float(prod['rating']) / 5.0
            
            # Features pasadas a TensorFlow: [Afinidad acumulada de categoría, Rating normalizado, Precio normalizado opcional]
            features_list.append([cat_match_score, rating_norm, float(prod['price']) / 1000000.0])

        # 3. Convertir a NumPy array y predecir
        X_candidates = np.array(features_list, dtype=np.float32)
        predictions = model.predict(X_candidates)

        # 4. Mapear resultados y ordenar
        scored_products = []
        for i, prod in enumerate(catalogo_productos):
            score_neural = float(predictions[i][0])
            scored_products.append({
                "itemId": prod["itemId"],
                "name": prod["name"],
                "category": prod["category"],
                "price": prod["price"],
                "score": round(score_neural, 4)
            })

        scored_products.sort(key=lambda x: x["score"], reverse=True)
        top_recommendations = scored_products[:5]

        return {
            "userId": userId,
            "recommendations": top_recommendations
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
        
@app.post("/model/reset")
def reset_model():
    global _cached_model
    
    # 1. Volver a instanciar la arquitectura base de tu red neuronal
    nuevo_modelo = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(3,)), # O la cantidad de features que uses
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ])
    
    # 2. Compilarlo de nuevo con su optimizador y función de pérdida iniciales
    nuevo_modelo.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
    
    # 3. Reemplazar la variable global en memoria
    _cached_model = nuevo_modelo
    
    return {"status": "success", "message": "El modelo neuronal ha sido limpiado y restablecido a su estado inicial."}