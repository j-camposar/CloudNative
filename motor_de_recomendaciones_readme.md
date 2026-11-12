# 🧠 Motor de Recomendación Inteligente en Tiempo Real

Este repositorio (rama `Recomendaciones`) contiene la arquitectura híbrida de recomendación en tiempo real para la plataforma de e-commerce. Combina procesamiento de eventos mediante **Apache Kafka**, un microservicio backend en **Spring Boot**, una base de datos relacional (**MySQL**), almacenamiento NoSQL cronológico (**AWS DynamoDB**) y un servicio de Machine Learning en **Python (FastAPI + TensorFlow)** capaz de realizar aprendizaje incremental (*online learning*).

## 🏗️ Arquitectura del Servicio

El flujo de recomendaciones opera de la siguiente manera:

1. **Ingesta de Eventos:** Las interacciones del usuario (clics, agregar al carro, compras) se publican en un tópico de Kafka (`interacciones-user`).

2. **Consumo y Persistencia (`Spring Boot`):** El consumidor de Kafka procesa el evento, calcula el peso de la interacción según la acción, persiste el registro en **MySQL** y reenvía asíncronamente el payload vía HTTP POST al servicio de IA.

3. **Persistencia NoSQL y IA (`FastAPI + TensorFlow`):**

   * Recibe el evento en la EC2 y lo almacena en **AWS DynamoDB** usando una clave compuesta (`userId` + `timestamp`) para mantener un historial ordenado cronológicamente.

   * Ejecuta un entrenamiento en caliente (`model.fit()`) con la nueva interacción para adaptar los pesos neuronales de forma incremental.

4. **Generación de Recomendaciones Híbridas:** Al consultar el endpoint `GET /recommendations`, el sistema evalúa el perfil ponderado del usuario en base a sus últimas interacciones y cruza los features contra el catálogo de productos mediante inferencia neuronal.

## 🛠️ Stack Tecnológico

* **Backend / Consumidor:** Java 21+, Spring Boot, Spring Kafka, Spring Data JPA, RestTemplate.

* **Motor de IA / API:** Python 3.10+, FastAPI, Uvicorn, TensorFlow / Keras, NumPy.

* **Bases de Datos:** MySQL (histórico relacional local), AWS DynamoDB (histórico NoSQL en la nube con clave compuesta).

* **Streaming:** Apache Kafka.

* **Infraestructura:** AWS EC2 (Hosting del servicio Python).

## 📁 Estructura del Proyecto

* `src/main/java/com/kafka/recomendaciones/` -> Código fuente del microservicio Spring Boot (Componentes Kafka, Controladores, Modelos y Repositorios).

* `main.py` -> Servicio FastAPI en Python que aloja el modelo neuronal de TensorFlow y la conexión con DynamoDB.

* `application.yml` -> Configuración centralizada de Spring Boot (conexión a MySQL, Kafka y URL parametrizada del servicio Python).

## 🚀 Guía de Puesta en Marcha

### 1. Configurar y Levantar el Microservicio (Spring Boot)

Asegúrate de configurar tu archivo `src/main/resources/application.yml` con las credenciales de tu base de datos MySQL y la URL del motor de Python:

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tu_base_de_datos
    username: tu_usuario
    password: tu_contraseña

recommendation:
  python:
    url: ${PYTHON_FASTAPI_URL:http://54.92.222.130:8000/interactions}

```

Compila y ejecuta el proyecto con Maven:

```
mvn clean package
java -jar target/recomendaciones-0.0.1-SNAPSHOT.jar

```

### 2. Levantar el Servicio de Machine Learning (FastAPI en EC2)

En tu máquina virtual de AWS con Python y el entorno virtual activado:

```
source venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

```

## 🔌 Endpoints Principales

### A. FastAPI (Python / EC2)

* **`POST /interactions`**: Recibe un evento de interacción, lo guarda en DynamoDB y entrena el modelo de forma incremental.

* **`GET /recommendations?userId=X`**: Devuelve el top 5 de productos recomendados para el usuario cruzando su perfil histórico con TensorFlow.

* **`POST /model/reset`**: Restablece los pesos del modelo neuronal en memoria a su estado inicial.

### B. Spring Boot (`/api/interacciones`)

* **`POST /api/interacciones`**: Registra y dispara una interacción individual hacia Kafka.

* **`POST /api/interacciones/generar-lote?cantidad=20`**: Endpoint de prueba para simular tráfico masivo de interacciones al azar de múltiples usuarios.

```
curl -X POST "http://localhost:8080/api/interacciones/generar-lote?cantidad=20" -H "Accept: application/json"

```