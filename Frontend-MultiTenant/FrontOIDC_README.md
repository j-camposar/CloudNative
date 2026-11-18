# 🛒 Interfaz Frontend & Autenticación OIDC (React + Vite)

Este repositorio contiene la interfaz de usuario de la plataforma de e-commerce, desarrollada con **React**, **Vite** y **Tailwind CSS**. Implementa autenticación segura basada en **OpenID Connect (OIDC)** integrada con **AWS Cognito** y **Azure EntraID**, permitiendo la navegación de productos, gestión del carrito y simulación de interacciones en tiempo real dentro de una arquitectura multi-tenant y orientada a eventos.

---

## 🛠️ Stack Tecnológico

* **Core & Build Tool:** React, Vite, JavaScript (ESM).
* **Estilos & UI:** Tailwind CSS, Lucide React (Iconos).
* **Autenticación & Seguridad:** AWS Cognito (AWS Amplify / OIDC Standard Client) Azure EntraID.
* **Gestión de Estado & HTTP:** React Context, Axios / Fetch API para comunicación con los microservicios backend (*PasarelaPagos* y *Recomendaciones*).

---

## 🏗️ Arquitectura y Flujo de Autenticación OIDC

1. **Autenticación de Usuario:** El usuario inicia sesión en la interfaz, redirigiendo o conectándose directamente al proveedor de identidad de **AWS Cognito** mediante OpenID Connect.
2. **Emisión de Tokens:** Una vez autenticado, se almacenan de forma segura los tokens (`IdToken`, `AccessToken`, `RefreshToken`) para autorizar las peticiones HTTP hacia los microservicios protegidos.
3. **Simulación de Interacciones (Streaming de Eventos):** Las acciones del usuario (navegar por el catálogo, añadir productos al carro, realizar compras) generan eventos autenticados que se comunican directamente con el ecosistema backend (Kafka / Spring Boot / FastAPI).

---

## 📁 Estructura del Proyecto (`Frontend-MultiTenant`)

* `src/components/` -> Componentes modulares de UI (Navbar, Catálogo de Productos, Carrito, Paneles de Usuario).
* `src/context/` -> Contextos de autenticación OIDC y estado global de la sesión.
* `src/services/` -> Clientes API configurados para conectar con los microservicios de pagos y recomendaciones.
* `src/App.jsx` -> Enrutador principal y layout de la aplicación.
* `.env` -> Variables de entorno para la configuración de AWS Cognito y URLs de los backends.

---

## 🚀 Guía de Puesta en Marcha



### Instalar Dependencias y Ejecutar en Desarrollo

```bash
# Entrar a la carpeta del frontend
cd Frontend-MultiTenant

# Instalar dependencias
npm install

# Levantar servidor de desarrollo con Vite
npm run dev
```

La aplicación estará disponible por defecto en `http://localhost:5173`.

---

## 🔌 Conexión con el Ecosistema Multi-Rama

Para que el frontend interactúe correctamente con los demás servicios de la plataforma, asegúrate de tener levantados los módulos correspondientes en sus respectivas ramas:

* **Rama `PasarelaPagos`**: Gestiona el cobro seguro con Webpay Plus (Transbank) y el motor antifraude en tiempo real.
* **Rama `Recomendaciones`**: Recibe las interacciones del usuario vía Kafka para alimentar el motor de IA en tiempo real.

---

## 👨‍💻 Autor
* **José Campos** - *Senior Full Stack & Cloud Engineer* - [j-camposar](https://github.com/j-camposar)
