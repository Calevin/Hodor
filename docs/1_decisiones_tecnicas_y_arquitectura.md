# 1. Decisiones Técnicas y Arquitectura

Esta sección documenta las decisiones estructurales fundamentales adoptadas durante la construcción del Identity Provider (IdP) "Hodor", estableciendo el porqué de cada elección de diseño, las alternativas descartadas y los compromisos asumidos para garantizar una plataforma escalable, segura y mantenible.

### ADR 001: Estrategia de Bootstrap y Autogestión (Dogfooding)

* **Título de la decisión:** Auto-aprovisionamiento de administración inicial mediante modelo *Dogfooding*.
* **Contexto y problema que resolvía:** Al desplegar el IdP en un entorno virgen, se requería una forma segura y automatizada de crear el primer superadministrador y el primer cliente de gestión para poder interactuar con la plataforma sin necesidad de insertar registros manualmente en la base de datos de producción.
* **Decisión final adoptada:** Se implementó un componente `AdminSeeder` en el arranque de la aplicación. Este componente auto-aprovisiona atómicamente un usuario administrador y un cliente confidencial (`hodor-admin-cli`), convirtiendo a Hodor en *Resource Server* de sí mismo.
* **Alternativas evaluadas y motivo por el que se descartaron:**
* *Scripts de migración SQL (Flyway):* Descartados porque los `client_secret` y las contraseñas de los usuarios deben pasar por el codificador criptográfico de Spring (ej. BCrypt) con "salts" dinámicos, lo cual es inviable e inseguro mediante texto plano en SQL.
* *Inserción manual (DBA):* Descartada por generar fricción operativa y romper el principio de infraestructura inmutable y automatizada.


* **Trade-offs y restricciones aceptadas:** El proceso de arranque de la aplicación asume una leve sobrecarga computacional, ya que en cada reinicio debe verificar la existencia previa de estos registros críticos antes de levantar el contexto web.

### ADR 002: Estrategia de Persistencia de Estado y Sesión

* **Título de la decisión:** Migración integral del estado efímero a almacenamiento relacional (PostgreSQL).
* **Contexto y problema que resolvía:** Las implementaciones por defecto de Spring Authorization Server operan *In-Memory*. Ante cualquier actualización o reinicio del contenedor Docker, Hodor sufría "amnesia": se perdían las llaves de firma (invalidando los tokens emitidos), las sesiones activas, los consentimientos otorgados y la capacidad de revocar tokens.
* **Decisión final adoptada:** Hodor adoptó un estado 100% *stateless* a nivel aplicativo volcando la carga a la base de datos. Se implementaron los adaptadores JDBC para las llaves criptográficas (`oauth2_keys`), las autorizaciones de OAuth2 (`oauth2_authorization`), los consentimientos (`oauth2_authorization_consent`) y las sesiones web del navegador (vía Spring Session JDBC).
* **Alternativas evaluadas y motivo por el que se descartaron:**
* *Mantener In-Memory:* Descartado categóricamente al ser incompatible con un entorno de producción o con el balanceo de carga.
* *Redis:* Descartado temporalmente para evitar introducir una nueva pieza de infraestructura compleja en esta fase del ecosistema. PostgreSQL es suficientemente capaz de manejar el volumen actual de transacciones.


* **Trade-offs y restricciones aceptadas:** Introduce mayor latencia de I/O en los flujos de login y validación de tokens. Además, obliga a mantener una configuración estricta de serialización polimórfica (Mixins de Jackson) para evitar vulnerabilidades o fallos al hidratar objetos Java desde la base de datos.

### ADR 003: Desacoplamiento de la Interfaz de Administración

* **Título de la decisión:** Separación estricta entre *User-Facing UI* y *Management API*.
* **Contexto y problema que resolvía:** Se requería definir el mecanismo tecnológico para construir las pantallas de gestión administrativa (alta de usuarios, registro de nuevos clientes). Existía la tentación de aprovechar el motor de plantillas existente en el proyecto para construir paneles CRUD.
* **Decisión final adoptada:** Thymeleaf se restringió única y exclusivamente a las interfaces que participan en el flujo de identidad del usuario final (Login, Consentimiento, Errores 401). Para la administración, Hodor expone una *Management API* (endpoints REST) protegida por tokens OAuth2, separando el panel de control en un proyecto cliente independiente.
* **Alternativas evaluadas y motivo por el que se descartaron:**
* *Monolito con Thymeleaf:* Construir el panel de control dentro de Hodor. Descartado por violar el principio de responsabilidad única (mezclando el core de identidad con CRUDs de gestión), acoplar fuertemente la UI al ciclo de despliegue del IdP y vulnerar la escalabilidad aislada.


* **Trade-offs y restricciones aceptadas:** Obliga a desarrollar, mantener y desplegar una aplicación separada (ej. un frontend React o un CLI en Go) para tareas operativas de Hodor, aumentando el tiempo inicial de desarrollo del ecosistema.

### ADR 004: Modelo Híbrido de Roles (Multi-Tenancy y SSO)

* **Título de la decisión:** Separación entre Roles de Identidad (Globales) y Roles de Negocio (Contextuales).
* **Contexto y problema que resolvía:** Al consolidar un Single Sign-On (SSO) para múltiples aplicaciones (como el blog "El libro de arena" y futuros juegos web), un usuario necesita privilegios diferentes dependiendo del sistema que consume, sin contaminar su identidad base.
* **Decisión final adoptada:** Se implementó una arquitectura de autorización de dos capas. Se conservó la tabla global `authorities` para permisos transversales al IdP (ej. acceso a la API administrativa de Hodor) y se introdujo la tabla `user_client_roles` para permisos de negocio acotados al cliente (`client_id`). Hodor intercepta la emisión del JWT (vía `OAuth2TokenCustomizer`) para inyectar dinámicamente solo los roles que correspondan a la aplicación que solicita el token.
* **Alternativas evaluadas y motivo por el que se descartaron:**
* *Roles Globales Únicos ("Token Bloat"):* Incluir todos los roles de todas las aplicaciones en un solo token. Descartado porque rompe el Principio de Menor Privilegio (un servicio vería los roles de los demás) e incrementa el tamaño del token JWT, arriesgando los límites de los headers HTTP.
* *Eliminar la tabla de Identidad Global:* Descartado porque rompería la integración nativa de los filtros de Spring Security encargados de gestionar los permisos intrínsecos de sesión del usuario en Hodor.


* **Trade-offs y restricciones aceptadas:** Aumenta la complejidad del modelo de datos y requiere que el IdP ejecute consultas adicionales a la base de datos en el instante preciso en el que se emite cada JWT, demandando índices eficientes para no penalizar la performance.

### ADR 005: Integración de Clientes SPA (React) mediante PKCE

* **Título de la decisión:** Adopción obligatoria de Authorization Code con PKCE para Single Page Applications.
* **Contexto y problema que resolvía:** Aplicaciones frontend como "El libro de arena" no pueden almacenar un secreto de manera segura, dado que todo su código fuente es descargado y visible en el navegador del usuario final.
* **Decisión final adoptada:** Para integrar las aplicaciones React, se decidió registrarlas en Hodor como clientes públicos (`client_authentication_method = none`) y forzar criptográficamente el uso de *Proof Key for Code Exchange* (`requireProofKey = true`), sustituyendo el secreto estático por un verificador dinámico por cada petición de login.
* **Alternativas evaluadas y motivo por el que se descartaron:**
* *Implicit Flow:* Descartado por ser considerado obsoleto y altamente inseguro en los estándares modernos (OAuth 2.1).
* *Provisionar Client Secret al frontend:* Descartado inmediatamente por ser una vulnerabilidad crítica que permitiría la suplantación de la aplicación.


* **Trade-offs y restricciones aceptadas:** Requiere que el frontend asuma la complejidad criptográfica de generar un *code_challenge* (SHA-256) y un *code_verifier*, forzando al ecosistema frontend a depender de librerías OIDC certificadas (como `oidc-client-ts`).

### ADR 006: Hardening y Tipado Estricto del Modelo de Datos

* **Título de la decisión:** Aplicación de restricciones lógicas y referenciales en la base de datos de dominio.
* **Contexto y problema que resolvía:** El modelo de datos presentaba deuda técnica silenciosa: campos de identificación lógica sin relación física (riesgo de datos huérfanos), identificadores criptográficos sin unicidad, y campos de tiempo susceptibles a desviaciones por zonas horarias en despliegues distribuidos.
* **Decisión final adoptada:** Se ejecutó un proceso de endurecimiento (hardening) sobre el DDL de PostgreSQL. Esto incluyó: la imposición de Foreign Keys desde `user_client_roles` hacia el `client_id` único del repositorio de clientes, la adición de `UNIQUE` constraints al Key ID (`kid`) de las llaves RSA, y la estandarización estricta al tipo `TIMESTAMP WITH TIME ZONE` para toda la capa de dominio propietaria.
* **Alternativas evaluadas y motivo por el que se descartaron:**
* *Delegar la integridad a la capa lógica de Java:* Descartada porque, en una arquitectura madura, la base de datos debe ser el último bastión de validación. La corrupción silenciosa ante eliminaciones accidentales de clientes era un riesgo inaceptable.


* **Trade-offs y restricciones aceptadas:** Reduce la flexibilidad para hacer alteraciones directas en la base de datos ("quick fixes") o eliminar clientes que contengan un historial extenso de roles asociados sin gestionar apropiadamente los bloqueos o cascadas relacionales. El mapeo de fechas a nivel de ORM requiere configuración explícita para evitar discrepancias con el Timezone local del framework.