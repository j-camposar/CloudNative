# 🛒 Arquitectura de Microservicios en Tiempo Real - E-Commerce

¡Bienvenidos al repositorio central de nuestra plataforma de e-commerce basada en microservicios! Para mantener un código limpio, desacoplado y organizado, este repositorio utiliza una **estrategia multi-rama**, donde cada proyecto independiente vive en su propia rama dedicada.

---

## 🌿 Estructura de Ramas y Proyectos

Cada módulo de la plataforma se encuentra en una rama específica. A continuación se detalla el índice de proyectos activos:

| Rama | Componente | Stack Tecnológico | Descripción |
| :--- | :--- | :--- | :--- |
| `main` / `master` | **Documentación Raíz** | Markdown | Documentación general de arquitectura y visión global del ecosistema. |
| `PasarelaPagos` | **Pasarela de Pagos** | Spring Boot, Webpay (Transbank), Motor antifraude | Gestiona transacciones seguras de pago, integración con Webpay y un sistema de **detección de fraudes** por análisis de múltiples intentos de un mismo usuario. |
| `Recomendaciones` | **Motor de Recomendación Inteligente** | Spring Boot, Kafka, Python (FastAPI), TensorFlow, AWS DynamoDB, MySQL | Consumidor de eventos en tiempo real, persistencia híbrida (MySQL local y AWS DynamoDB cloud), aprendizaje incremental (*online learning*) con TensorFlow y generación de recomendaciones híbridas multiclase. |
| `FrontOIDC` | **Frontend y Autenticación** | React, Vite, Tailwind CSS, AWS Cognito | Interfaz de usuario para navegación de productos, carrito, simulación de interacciones y autenticación segura basada en OpenID Connect. |

---

## 🏗️ Flujo General de la Arquitectura

El sistema está diseñado como un ecosistema orientado a eventos y analítica en tiempo real:

1. **Interacción del Usuario (`FrontOIDC`):** El usuario navega, hace clic, añade al carro o compra. Estas acciones generan eventos autenticados mediante OpenID Connect.
2. **Streaming de Eventos (Kafka):** Las interacciones viajan a través de tópicos en Kafka (ej. `interacciones-user`).
3. **Consumo y Persistencia Híbrida (`Recomendaciones` - Spring Boot):** 
   - El microservicio en Spring Boot consume los eventos, registra el histórico transaccional en **MySQL** y reenvía asíncronamente el payload vía HTTP POST al motor de IA.
4. **Aprendizaje en Caliente y Base de Datos NoSQL (`Recomendaciones` - FastAPI & DynamoDB):**
   - El servicio en FastAPI (alojado en AWS EC2) almacena el historial cronológico del usuario en **AWS DynamoDB** usando una clave compuesta (`userId` + `timestamp`).
   - El modelo de **TensorFlow** actualiza sus pesos mediante entrenamiento incremental (`model.fit()`).
   - Al consultar las recomendaciones, cruza el perfil ponderado del usuario con las características del catálogo para ofrecer sugerencias personalizadas.
5. **Transacciones y Seguridad (`PasarelaPagos`):**
   - Valida el flujo de pago con Webpay y evalúa patrones de riesgo mediante el motor de **detección de fraudes**, bloqueando o alertando sobre intentos masivos sospechosos de un mismo usuario.

---

## 🚀 Cómo Trabajar con las Ramas

Para cambiar de proyecto y explorar el código fuente de cada módulo, utiliza los siguientes comandos de Git:

```bash
# Clonar el repositorio principal
git clone <url-del-repositorio>
cd <nombre-del-repositorio>

# 1. Para trabajar en el Motor de Recomendaciones (Spring Boot + FastAPI)
git checkout Recomendaciones

# 2. Para trabajar en la Pasarela de Pagos y Antifraude (Spring Boot + Webpay)
git checkout PasarelaPagos

# 3. Para trabajar en la Interfaz de Usuario y Autenticación (React + OIDC)
git checkout FrontOIDC
```

---

## 🛠️ Configuración y Despliegue

Cada rama contiene sus propias configuraciones independientes (como archivos `application.yml` para los microservicios de Spring Boot, variables de entorno `.env` para React o scripts de FastAPI en la EC2). Asegúrate de revisar el archivo `README.md` específico dentro de cada rama antes de levantar los entornos locales o en la nube.

---

## 🎓 Contexto Académico y Profesional

Este proyecto fue desarrollado como una arquitectura empresarial de microservicios avanzada, integrando patrones de diseño modernos, procesamiento de eventos distribuidos, bases de datos relacionales y NoSQL, machine learning en caliente y seguridad transaccional orientada a la industria e-commerce.