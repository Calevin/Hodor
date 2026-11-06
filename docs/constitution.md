# Constitución del Proyecto — Hodor (Java-Auth)

> **Ley Suprema (`seb-harness`):** Este documento define los principios innegociables del proyecto. Toda especificación (`*_spec.md`), plan (`*_plan.md`), verificador (`*_verificador.md`) e implementación debe cumplirlos estrictamente.

## A. Naturaleza del Proyecto

- **Propósito y Misión:** Hodor es un Proveedor de Identidad (Identity Provider — IdP) centralizado, desacoplado y basado en estándares de la industria (OAuth2 y OpenID Connect / OIDC). Su misión es autenticar identidades, autorizar accesos y emitir credenciales criptográficas para un ecosistema de servicios personales (Blog, APIs en Go, etc.) mediante firmas asimétricas seguras (RS256).
- **Usuarios / Consumidores:**
  - Usuarios finales que interactúan de forma interactiva con interfaces de inicio de sesión (Login) y autorización de consentimiento (Consent) provistas por vistas Thymeleaf.
  - Clientes y microservicios del ecosistema (ej. Blog, APIs Go) que validan tokens JWT de forma autónoma mediante el conjunto de claves públicas expuesto en el endpoint estándar `/.well-known/jwks.json`.
  - Administradores de sistemas que configuran clientes y asignan roles mediante la API REST administrativa (`/api/admin/**`).
- **Tipo de Sistema y Alcance:** Monolito modular de autenticación y autorización (Spring Security OAuth2 Authorization Server). Su alcance abarca la autenticación de usuarios, gestión de clientes OAuth2, persistencia de sesiones web en base de datos, generación y almacenamiento de claves asimétricas RSA rotables en base de datos, y provisión de endpoints OIDC/OAuth2 estándar.

## B. Stack Tecnológico (Reglas de Implementación)

- **Lenguaje y Runtime:** Java 25 (OpenJDK 25) bajo NixOS, aprovechando Virtual Threads y Pattern Matching moderno.
- **Frameworks y Librerías Core:**
  - Spring Boot 4.0.4 (`spring-boot-starter-webmvc`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`).
  - Spring Security y Spring Security OAuth2 Authorization Server.
  - Spring Session JDBC (gestión persistente de sesiones HTTP en base de datos).
  - Spring Data JPA con Hibernate.
  - Flyway para migraciones evolutivas de base de datos (`flyway-database-postgresql`).
  - Project Lombok para reducción de boilerplate (`@Data`, `@Value`, `@Builder`, `@RequiredArgsConstructor`).
  - Thymeleaf para renderizado de formularios de login y consentimiento.
- **Persistencia e Infraestructura:**
  - Motor de base de datos: PostgreSQL 17 (`postgres:17-alpine`).
  - Despliegue en contenedores: Docker y Docker Compose (perfil local `docker-compose.yml` para dependencias; perfil de producción `docker-compose.prod.yml` con integración al reverse proxy Caddy y `Dockerfile` multi-stage con eclipse-temurin:25).
  - Entorno reproducible: Nix Flakes (`flake.nix`) y `direnv` (`.envrc`).
- **Comandos Oficiales de Verificación Programática:**
  - **Compilación / Build:** `direnv exec . ./mvnw clean compile` (o `nix develop --command ./mvnw clean compile`)
  - **Tests Unitarios / Integración:** `direnv exec . ./mvnw test`
  - **Linter / Análisis Estático:** `direnv exec . ./mvnw test-compile` (compilación estricta y procesamiento de anotaciones en Java 25)
- **Prohibiciones de Stack:**
  - Prohibido añadir nuevas dependencias externas al archivo `pom.xml` sin aprobación explícita en la especificación y plan de trabajo.
  - Prohibido reemplazar librerías core del proyecto por alternativas equivalentes (ej. no sustituir Spring Security OAuth2 por implementaciones manuales de JWT).
  - Prohibido el uso de `hibernate.ddl-auto` con modos que alteren el esquema en tiempo de ejecución (`update`, `create`, `create-drop`); toda modificación de base de datos debe ser versionada con Flyway.

## C. Reglas de Dominio y Lógica de Negocio

- **Invariantes del Dominio:**
  - **Desacoplamiento Asimétrico (RS256):** Hodor firma tokens con su llave privada RSA (mínimo 2048 bits) persistida en base de datos (`KeyEntity`) y publica únicamente sus llaves públicas en `/.well-known/jwks.json`. Ningún cliente externo accede a llaves privadas ni comparte secretos simétricos para validar tokens.
  - **Gestión de Identidad Multi-sistema:** Los usuarios pertenecen a clientes (sistemas). La autorización se controla mediante Authorities delimitadas por sistema (ej. `ROLE_USER_BLOG`, `ROLE_ADMIN_API`).
  - **Almacenamiento Seguro de Contraseñas:** Queda estrictamente prohibido persistir o comparar contraseñas en texto plano; es obligatorio el uso de `BCryptPasswordEncoder`.
  - **Bootstrap Idempotente:** El usuario administrador inicial y el cliente administrativo se configuran a partir de variables de entorno (`AUTH_ADMIN_USER`, `AUTH_ADMIN_PASS`, `AUTH_ADMIN_CLIENT_ID`, `AUTH_ADMIN_CLIENT_SECRET`) únicamente si la base de datos se encuentra vacía (`AdminSeeder`).
  - **Protección Administrativa:** Los endpoints bajo `/api/admin/**` requieren un token con scope `admin` o validación contra credenciales maestras autorizadas.
- **Fronteras de Datos y Consistencia:**
  - **Database-First:** Cualquier cambio en el modelo de datos relacional debe nacer en un script SQL de migración en `src/main/resources/db/migration/V<N>__<descripcion>.sql`.
  - **Transaccionalidad:** Los métodos de servicio que modifican estado deben anotarse con `@Transactional` delimitando claramente las fronteras de consistencia.
  - **Persistencia de Sesión:** Las sesiones de usuario web deben persistir en PostgreSQL mediante Spring Session JDBC (`SPRING_SESSION`).
- **Aislamiento de Lógica:**
  - La lógica de negocio, validaciones y orquestación de identidad deben residir puramente en la capa de aplicación/servicios, desacopladas de controladores HTTP y del protocolo de transporte, garantizando pruebas unitarias rápidas y deterministas sin requerir contenedores activos cuando no sea estrictamente necesario.

## D. Estructura y Estilo de Código

- **Topología de Directorios:**
  - `src/main/java/com/calevin/hodor/application/`: Servicios de aplicación, casos de uso y DTOs de solicitud/respuesta.
  - `src/main/java/com/calevin/hodor/infrastructure/bootstrap/`: Inicializadores idempotentes del sistema (`AdminSeeder`).
  - `src/main/java/com/calevin/hodor/infrastructure/config/`: Configuraciones de aplicación web y MVC.
  - `src/main/java/com/calevin/hodor/infrastructure/persistence/`: Entidades JPA y repositorios Spring Data JPA.
  - `src/main/java/com/calevin/hodor/infrastructure/security/`: Configuración del Authorization Server, JWKS, customización de tokens JWT, UserDetails y filtros de seguridad.
  - `src/main/java/com/calevin/hodor/infrastructure/ui/controller/`: Controladores MVC para Login y Consentimiento (vistas web Thymeleaf).
  - `src/main/java/com/calevin/hodor/infrastructure/web/`: Controladores de API REST administrativa (`AdminController`).
  - `src/main/resources/db/migration/`: Scripts evolutivos SQL de Flyway.
  - `src/test/`: Pruebas unitarias de servicios, controladores y pruebas de integración con Testcontainers.
- **Patrones Arquitectónicos:**
  - Arquitectura Hexagonal / Puertos y Adaptadores con separación clara entre aplicación e infraestructura.
  - Inyección de dependencias exclusivamente por constructor sobre campos declarados `private final`.
  - Aislamiento de capas de transporte: Prohibido exponer entidades JPA directamente a la API REST. Se deben utilizar DTOs específicos para requests y responses.
- **Convenciones de Nomenclatura e Idioma:**
  - Sintaxis de código, clases, métodos, variables, tablas y columnas escritas exclusivamente en inglés.
  - Comentarios explicativos y documentación técnica redactados estrictamente en español.
  - Queda terminantemente prohibido el uso de emojis dentro de archivos de código fuente, comentarios y mensajes de commit.
- **Prohibiciones de Diseño:**
  - Prohibido ubicar lógica de negocio dentro de controladores (`@RestController` o `@Controller`).
  - Prohibido el uso de inyección de dependencias por campo (`@Autowired` en variables miembro).
  - Prohibido el uso de estado global mutable.
  - Prohibidas las dependencias circulares entre paquetes o componentes.

## E. Manejo de Errores y Validaciones

- **Estrategia de Errores:**
  - Manejo global y centralizado de excepciones en la API REST con `@RestControllerAdvice` y `@ExceptionHandler`, retornando estructuras de error consistentes con códigos de estado HTTP semánticos (400, 401, 403, 404, 409, 500).
  - Rutas de interfaz web delegadas a vistas de error estandarizadas (`error.html`).
- **Validación de Entradas:**
  - Validación declarativa en la capa de entrada (DTOs) utilizando anotaciones de Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Email`) antes de delegar a la capa de servicios.
- **Observabilidad y Seguridad:**
  - Registro de logs con SLF4J mediante mensajes parametrizados (`logger.info("User {} authenticated", username)`).
  - Prohibición absoluta de registrar en logs secretos, contraseñas en texto plano, llaves privadas RSA, tokens completos o datos personales sensibles (PII).
  - Métricas y verificación de estado operativo expuestas a través de Spring Boot Actuator (`/actuator/health`).

## F. Comportamiento del Agente de IA (Reglas SDD — `seb-harness`)

1. **La Spec Manda:** Ninguna funcionalidad se implementa sin un archivo `specs/<NNN>_<tarea>_spec.md` aprobado y sin etiquetas `[NECESITA ACLARACIÓN]`.
2. **Testabilidad Programática Obligatoria:** Ningún plan (`specs/<NNN>_<tarea>_plan.md`) es válido sin su correspondiente `specs/<NNN>_<tarea>_verificador.md` con comandos que devuelvan código de salida `0`.
3. **Anti-Reward-Hacking:** Está terminantemente prohibido modificar `docs/constitution.md`, `specs/*_spec.md` o `specs/*_verificador.md` durante la implementación, así como deshabilitar o relajar tests para forzar que pasen.
4. **Memoria Persistente (`MEMORY.md`):** Antes de planificar o codificar, el agente debe consultar `MEMORY.md` para evitar callejones sin salida ya documentados, y registrar allí cualquier nueva limitación técnica descubierta.
5. **Commits Protegidos:** Todo commit de código debe pasar la puerta determinista `.seb/bin/seb verify <tarea>`.
