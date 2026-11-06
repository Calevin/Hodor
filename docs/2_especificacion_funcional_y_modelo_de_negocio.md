# 2. Especificación Funcional y Modelo de Negocio

Esta sección detalla la versión consolidada del comportamiento funcional de **Hodor**, describiendo los módulos que lo componen, el recorrido de la información a través del sistema, las reglas de negocio innegociables y los escenarios de excepción que la arquitectura es capaz de gestionar exitosamente.

---

### 2.1. Responsabilidades de cada Módulo o Componente

El ecosistema de Hodor se estructura en módulos con responsabilidades estrictamente delimitadas, eliminando solapamientos y garantizando el principio de responsabilidad única (SRP):

* **Motor de Identidad y Autenticación (Identity Core):** Responsable de verificar "quién es el usuario". Gestiona la validación de credenciales criptográficas (BCrypt) y carga en el contexto de seguridad las identidades globales (tabla `authorities`).
* **Motor de Autorización (OAuth2/OIDC Engine):** Responsable de evaluar "qué puede hacer el usuario" y emitir los salvoconductos. Se encarga de la generación, firma criptográfica y entrega de JWTs (Access, Refresh e ID Tokens). Incorpora un `OAuth2TokenCustomizer` que inyecta dinámicamente los roles contextuales según la aplicación solicitante.
* **Capa de Persistencia de Estado (JDBC Persistence):** Módulo encargado de dotar de "memoria a largo plazo" al IdP. Administra el ciclo de vida en base de datos de: llaves RSA (`oauth2_keys`), sesiones activas y tokens emitidos (`oauth2_authorization`), y aprobaciones de acceso de los usuarios (`oauth2_authorization_consent`). Adicionalmente, sincroniza la sesión web del navegador (`JSESSIONID`) utilizando Spring Session.
* **Portal del Usuario (User-Facing UI):** Capa de presentación (Thymeleaf) estrictamente limitada a las interacciones del flujo de seguridad: pantalla de inicio de sesión personalizada por aplicación, solicitud de consentimientos y pantallas de error (ej. 401/403).
* **API de Administración (Management API):** Módulo *headless* (sin interfaz gráfica) que expone endpoints REST protegidos para la gestión operativa (altas de clientes, bloqueos de usuarios, asignación de roles). Requiere consumo desde un cliente externo administrativo autenticado (ej. `hodor-admin-cli`).
* **Bootstrap / Auto-Aprovisionamiento:** Componente transitorio (`AdminSeeder`) que se ejecuta únicamente en el arranque de la aplicación para garantizar la existencia inmutable del usuario superadministrador y del cliente fundacional de administración.

---

### 2.2. Flujo de Datos y Ciclo de Vida de Entidades Principales

El modelo de Hodor se basa en la separación entre la identidad de la persona y sus autorizaciones en el ecosistema (Single Sign-On).

**Ciclo de Vida de la Identidad y el Acceso:**

1. **Génesis:** Un individuo es registrado en la tabla `users` (una sola vez para todo el ecosistema). Se le asignan roles intrínsecos al IdP en `identity_authorities` (ej. `ROLE_USER` o `ROLE_ADMIN`).
2. **Vinculación:** Para que el usuario pueda usar una aplicación satélite (ej. el Blog), se genera una relación. Esta relación dicta sus privilegios específicos en esa aplicación mediante la tabla `client_authorities` (o `user_client_roles`).
3. **Aislamiento:** El usuario interactúa con la aplicación "A" como `ROLE_EDITOR` y con la aplicación "B" como `ROLE_PLAYER`. Su identidad es la misma, pero su contexto de autorización muta según el *Client ID* solicitante.

**Ciclo de Vida del Token y la Sesión (Flujo OIDC):**

1. **Iniciación:** Una aplicación externa (ej. React) redirige al usuario a Hodor. Hodor valida si existe una sesión previa en PostgreSQL (Spring Session). Si no existe, despliega el *User-Facing UI*.
2. **Consentimiento y Emisión:** Tras el login exitoso, Hodor consulta los roles contextuales, ensambla el JWT a medida, lo firma con su llave RSA privada y persiste los metadatos del token emitido.
3. **Destrucción (Logout):** El cliente invoca el endpoint de revocación/logout enviando su ID Token como prueba de identidad. Hodor destruye la sesión web (`JSESSIONID`) e invalida los tokens en la base de datos, imposibilitando accesos futuros con esos artefactos.

---

### 2.3. Reglas de Negocio Críticas y Validaciones Acordadas

Para garantizar la seguridad y coherencia del sistema, se han establecido las siguientes reglas innegociables:

* **Tipificación Estricta de Clientes (Confidencial vs. Público):**
* Los clientes de backend (Server-to-Server) deben autenticarse mediante secreto (`client_secret`).
* Los clientes de frontend puro (Single Page Applications como React/Vue) están catalogados como **Public Clients**. Se les prohíbe poseer un secreto y se les obliga criptográficamente a utilizar el flujo Authorization Code con **PKCE** (`requireProofKey = true`).


* **Principio de Privilegio Mínimo Temporal (TTL):** Los tiempos de expiración (`TokenSettings`) no son globales. Se aplican políticas granulares:
* *SPAs:* Access Tokens de vida muy corta (15-30 min) y Refresh Tokens rotativos (un solo uso).
* *Sistemas Administrativos:* Access Tokens cortos y Refresh Tokens restringidos a la duración de una jornada laboral.


* **Independencia del Access Link:** La sola existencia de un usuario en el IdP no le otorga acceso a ninguna aplicación del ecosistema. Todo acceso debe ser explícitamente concedido a nivel de base de datos, lo que permite suspensiones selectivas (ej. banear a un usuario de un juego web sin bloquear su acceso al blog).
* **Integridad Referencial Absoluta:** Cualquier asignación de roles contextuales debe estar estrictamente vinculada mediante claves foráneas (Foreign Keys) al nombre lógico único (`client_id`) del cliente registrado, evitando la corrupción por orfandad de datos.

---

### 2.4. Casos Borde (Edge Cases) y Escenarios de Error Resueltos

Durante el diseño de la arquitectura, se neutralizaron múltiples vectores de falla e inconsistencias técnicas que son críticos en entornos de producción:

1. **La "Amnesia" de Reinicio y Sesiones Fantasmas:**
* *Escenario:* Al actualizar o reiniciar el contenedor de Hodor, se perdían todos los inicios de sesión y los tokens emitidos devolvían falsos positivos de revocación (HTTP 200 pero sin impacto real).
* *Resolución:* Implementación de JDBC integral. Si Hodor se reinicia, las llaves de firma, las sesiones web y las autorizaciones son recuperadas desde PostgreSQL, garantizando tolerancia a fallos.


2. **El Secuestro de Sesión por el Health Check:**
* *Escenario:* Los pings automáticos de Docker sobre `/actuator/health` sobreescribían el *Request Cache* de Spring Security, provocando que los usuarios fueran redirigidos a la ruta de monitoreo interno tras el login en lugar de a su aplicación destino.
* *Resolución:* Configuración estricta en el `SecurityFilterChain` para excluir los endpoints de Actuator del mecanismo de guardado de solicitudes.


3. **Conflictos de Redirección por Terminación SSL (Proxy Inverso):**
* *Escenario:* Hodor operando en HTTP interno detrás de un proxy Caddy/Cloudflare (HTTPS) provocaba rechazos por *Mismatch* en la validación de las URLs de redirección de OAuth2.
* *Resolución:* Integración del filtro de `ForwardedHeaderFilter` para que Hodor asuma dinámicamente el protocolo, dominio y puerto provistos por las cabeceras `X-Forwarded-*` del proxy.


4. **Vulnerabilidad de Deserialización Polimórfica (Jackson):**
* *Escenario:* Al revocar un token, Jackson arrojaba una excepción `ImmutableCollections$ListN` al intentar leer los roles inyectados en la base de datos, deteniendo el flujo por medidas preventivas de seguridad.
* *Resolución:* Registro explícito de los *Security Mixins* de Jackson en el `OAuth2AuthorizationService` para definir una lista blanca de clases seguras permitidas durante la hidratación de datos.


5. **Ataques de Logout Forzado (CSRF) y Tokens Huérfanos:**
* *Escenario:* Posibilidad de que un script malicioso deslogueara a un usuario sin su permiso, o vulnerabilidades de redirección abierta (Open Redirect) en el endpoint de salida.
* *Resolución:* Implementación estricta de *RP-Initiated Logout*. Hodor solo procesa salidas y redirecciones si se provee el `id_token_hint` de la sesión activa como prueba de origen, validando la URL de destino contra una lista blanca (`post_logout_redirect_uri`).