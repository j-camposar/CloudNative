# 💳 Microservicio de Pasarela de Pagos & Detección de Fraude en Tiempo Real

Este repositorio contiene el microservicio transaccional de pasarela de pagos desarrollado como parte de una arquitectura orientada a eventos. Combina procesamiento financiero seguro mediante integración oficial con **Webpay Plus (Transbank)**, streaming de eventos distribuido con **Apache Kafka**, persistencia relacional y un motor heurístico de **detección de fraude en tiempo real** (*velocity checks*).

---

## 🏗️ Arquitectura del Servicio

El flujo de procesamiento de pagos y control antifraude opera de la siguiente manera:

1. **Iniciación de Transacción:** El cliente solicita el cobro (`POST /api/v1/pago/crear`). El microservicio valida el monto, la sesión y registra la orden en estado `PENDING` en base de datos.
2. **Evaluación Antifraude en Tiempo Real (**Velocity Checks**):**
   * Antes de interactuar con la pasarela externa, el motor analiza el perfil de riesgo (`userId`, dispositivo) evaluando la frecuencia de intentos en ventanas de tiempo críticas.
   * **Bloqueo y Alerta en Streaming (**Kafka**):** Si se detecta un patrón anómalo, la transacción se marca como `FRAUD_SUSPECTED`, se bloquea preventivamente y se publica un evento inmediato en el tópico `alertas-fraude` para auditoría y bloqueo en otros sistemas.
3. **Integración con Webpay Plus (**Transbank**):** Si la evaluación es limpia, el servicio genera el token transaccional con el SDK de Transbank y retorna la URL de redirección junto con el token al cliente.
4. **Confirmación, Commit y Evento de Pago:** Al recibir el callback en `POST /api/v1/pago/calback` con `token_ws`, el microservicio ejecuta el *Commit* ante Transbank, actualiza el estado final (`AUTHORIZED` o `REJECTED`) y publica un evento en el tópico `transacciones-exitosas` para desacoplar procesos posteriores (facturación, inventario, notificaciones).

---

## 🛠️ Stack Tecnológico

* **Backend / Core:** Java 21 (Virtual Threads), Spring Boot, Spring Kafka, Spring Data JPA, Spring Security.
* **Integración de Pagos:** SDK Transbank Webpay Plus (API REST).
* **Streaming de Eventos:** Apache Kafka (Productores y Consumidores de eventos transaccionales y de alerta).
* **Base de Datos:** PostgreSQL / MySQL (Histórico transaccional y auditoría antifraude).
* **Infraestructura & Contenedores:** Docker, Docker Compose.

---

## 📁 Estructura del Proyecto

* `src/main/java/com/pasarela/pagos/` -> Código fuente del microservicio Spring Boot (Controladores REST, Servicios Webpay, Motor Antifraude, Productores Kafka y Repositorios).
* `src/main/resources/application.yml` -> Configuración centralizada (conexión a base de datos, brokers de Kafka, credenciales de integración Transbank y umbrales antifraude).
* `Dockerfile` & `docker-compose.yml` -> Definición de despliegue contenerizado multi-stage.

---

## 🚀 Guía de Puesta en Marcha

### 1. Configurar el Entorno (`application.yml`)

Asegúrate de configurar tus credenciales de integración de Transbank, la base de datos y el servidor de Kafka en `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: pago

  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://mysql-db:3306/pagos_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
    username: ${SPRING_DATASOURCE_USERNAME:root}
    password: ${SPRING_DATASOURCE_PASSWOeRD:123456}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      connection-timeout: 30000
      validation-timeout: 5000

  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTR_SERVERS}
    producer:
      key-serializr: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: payment-velocity-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
 
    listener:
      missing-topics-fatal: false
      auto-startup: true

```

### 2. Compilar y Ejecutar el Microservicio

Compila y ejecuta el proyecto utilizando Maven:

```bash
mvn clean package
java -jar target/ms-pagos-0.0.1-SNAPSHOT.jar
```

O despliega el contenedor directamente con Docker:

```bash
docker build -t ms-pagos-webpay:v1 .
docker run -d -p 8080:8080 -e KAFKA_BROKERS=localhost:9092 --name ms-pagos ms-pagos-webpay:v1
```

---

## 🔌 Endpoints Principales (`/api/v1/pago`)

* `POST /api/v1/pago/crear` : Inicia una transacción, ejecuta el análisis antifraude en tiempo real y genera el token en Webpay Plus.
* `POST /api/v1/pago/retorno` : Endpoint de retorno (*callback*) de Transbank que ejecuta el *Commit* y publica el resultado en Kafka.
* `GET /api/v1/pago/estado/{buyOrder}` : Consulta el estado actual y trazabilidad completa de una orden de compra.
* `GET /api/v1/pago/alertas-fraude` : Lista las transacciones marcadas y bloqueadas preventivamente por el motor de fraude.

```bash
curl -X POST "http://localhost:8080/api/v1/pago/crear" \
  -H "Content-Type: application/json" \
  -d '{"buyOrder": "ORD-2026-001", "sessionId": "SES-987", "amount": 25000, "returnUrl": "http://localhost:8080/api/v1/pago/retorno"}'
```

---

## 👨‍💻 Autor
* **José Campos** - *Senior Full Stack & Cloud Engineer* - [j-camposar](https://github.com/j-camposar)
