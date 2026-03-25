# ---

**🛡️ Hodor: Agent Context & Guidelines**

Este documento define las reglas de arquitectura, estándares técnicos y restricciones para el desarrollo del servicio **Hodor (Java-Auth)**. Cualquier sugerencia de código o cambio estructural debe alinearse con estos principios.

## **🎯 Misión del Proyecto**

Hodor es un **Identity Provider (IdP)** centralizado, desacoplado y basado en estándares (OAuth2/OIDC). Su propósito es gestionar la identidad de un ecosistema personal (Blog, APIs, etc.) de forma segura mediante firmas asimétricas.

## ---

**🛠️ Stack Tecnológico (Core 2026\)**

| Componente | Tecnología | Versión / Detalle |
| :---- | :---- | :---- |
| **Runtime** | **Java 25** | Uso mandatorio de Virtual Threads y Pattern Matching. |
| **Framework** | **Spring Boot 4.0.x** | Basado en Spring Framework 6.2+. |
| **Seguridad** | **Spring Auth Server** | Implementación nativa de OAuth2 y JWKS. |
| **Persistencia** | **PostgreSQL 17+** | Acceso vía Spring Data JPA. |
| **Migraciones** | **Flyway** | Prohibido el uso de hibernate.ddl-auto. |
| **Build Tool** | **Maven** | Soporte para GraalVM Native Image. |

## ---

**🏗️ Principios de Arquitectura**

### **1\. Desacoplamiento Asimétrico (RS256)**

* Hodor **firma** tokens con una llave privada RSA (2048 bits).  
* Hodor **expone** las llaves públicas en el endpoint /.well-known/jwks.json.  
* Ningún servicio externo debe tener acceso a la llave privada ni compartir secretos simétricos.

### **2\. Estructura de Código (Hexagonal)**

Se prefiere un diseño de **Puertos y Adaptadores**:

* domain: Lógica pura de identidad y reglas de negocio.  
* application: Casos de uso (ej. RegisterUserUseCase).  
* infrastructure: Implementaciones de Spring Security, JPA, y controladores REST.

### **3\. Gestión de Identidad (Multi-sistema)**

* Los usuarios están vinculados a **Clients** (sistemas).  
* El acceso se controla mediante **Roles/Authorities** específicos por sistema (ROLE\_USER\_BLOG, ROLE\_ADMIN\_API).

## ---

**🔐 Reglas de Seguridad Innegociables**

* **Passwords:** Nunca en texto plano. Uso obligatorio de BCryptPasswordEncoder.  
* **Bootstrap:** El primer administrador se crea vía variables de entorno (AUTH\_ADMIN\_USER, AUTH\_ADMIN\_PASS) solo si la base de datos está vacía (Patrón Idempotente).  
* **Endpoints Admin:** Los recursos bajo /api/admin/\*\* deben requerir un token con el scope admin o ser validados contra una Master Key de infraestructura.

## ---

**🚀 Flujo de Desarrollo (Workflow)**

1. **Database First (via Flyway):** Cualquier cambio en el modelo debe iniciar con un archivo .sql de migración en src/main/resources/db/migration.  
2. **Contract First:** Los cambios en la API deben reflejarse primero en el archivo openapi-spec.json.  
3. **Observability:** Todos los componentes críticos deben ser monitoreables vía **Spring Actuator**.  
4. **Optimización:** El código debe ser compatible con la compilación nativa de **GraalVM** para reducir el footprint en el servidor Docker.
5. **Testing Obligatorio:** El proyecto debe tener tests. Toda funcionalidad que se agregue debe incluir test unitarios.
6. **Docker:** El proyecto debe correr tanto en local como en producción usando Docker.

## ---

**📝 Notas para el Agente**

Cuando sugieras código para Hodor, prioriza la sintaxis moderna de Java 25\. Evita el uso de librerías externas si Spring Boot ya provee una solución nativa. Si propones un cambio en la seguridad, explica siempre el impacto en el flujo de los clientes (Blog/API Go).

### ---

