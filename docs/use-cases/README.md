# Catálogo de Casos de Uso del Sistema (Hodor)

Este directorio documenta de forma estandarizada los casos de uso funcionales (perspectiva de caja negra / black-box) implementados y formalmente verificados bajo **`seb-harness`**.

Cada caso de uso está respaldado por especificaciones formales en notación EARS, por la constitución innegociable del proyecto (`docs/constitution.md`) y por suites de verificación programática determinista.

## Índice General de Casos de Uso

| ID | Caso de Uso | Dominio | Actor Principal | Disparador | Trazabilidad / Origen |
| :--- | :--- | :--- | :--- | :--- | :--- |
| [`UC-CLIENT-REGISTER`](UC-client_registration.md) | Registro de Nuevo Cliente OAuth2 | Administración de Clientes | Administrador (`ROLE_ADMIN`) | `POST /api/admin/clients` | [`docs/constitution.md`](../constitution.md) |
| [`UC-CLIENT-UPDATE`](UC-client_update.md) | Actualización de Configuración de Cliente OAuth2 | Administración de Clientes | Administrador (`ROLE_ADMIN`) | `PATCH /api/admin/clients/{clientId}` | [`001_actualizar_cliente_spec.md`](../../specs/001_actualizar_cliente_spec.md) |
| [`UC-USER-REGISTER`](UC-user_registration.md) | Registro Administrativo de Usuario para un Sistema | Administración de Usuarios | Administrador (`ROLE_ADMIN`) | `POST /api/admin/users` | [`docs/constitution.md`](../constitution.md) |
| [`UC-USER-LIST`](UC-user_list.md) | Consulta y Listado de Usuarios con Sistemas Autorizados | Administración de Usuarios | Administrador (`ROLE_ADMIN`) | `GET /api/admin/users` | [`docs/constitution.md`](../constitution.md) |
| [`UC-USER-ROLE-ASSIGN`](UC-user_role_assignment.md) | Asignación de Roles Específicos por Cliente a Usuario | Administración de Usuarios | Administrador (`ROLE_ADMIN`) | `POST /api/admin/users/{userId}/roles` | [`docs/constitution.md`](../constitution.md) |
| [`UC-SESSION-ACTIVE-UI`](UC-session_active_ui.md) | Gestión y Detección de Sesión Activa en la Interfaz de Usuario | Autenticación y UI | Usuario Final (`Authenticated` / `Anonymous`) | `GET /login`, `GET /`, `POST /logout` | [`002_gestion_sesion_activa_ui_spec.md`](../../specs/002_gestion_sesion_activa_ui_spec.md) |
| [`UC-AUTH-CONSENT`](UC-auth_consent.md) | Flujo de Autorización OIDC y Consentimiento de Scopes | Protocolo OIDC / OAuth2 | Usuario Final y Aplicación Consumidora | `GET /oauth2/authorize`, `POST /oauth2/token` | [`docs/constitution.md`](../constitution.md) |

## Estructura Estándar de los Documentos

Cada caso de uso documentado dentro de este catálogo contiene:
1. **Ficha de Intención:** Identificador canónico, actor principal, disparador, precondiciones e invariantes del dominio.
2. **Diagrama de Interacción del Sistema (SSD):** Diagrama de secuencia funcional black-box en formato Mermaid con numeración secuencial (`autonumber`).
3. **Árbol de Decisiones:** Diagrama de flujo condicional con bifurcaciones, validaciones y puntos de control.
4. **Caminos Alternativos y Catálogo de Errores:** Matriz de códigos HTTP, condiciones de fallo y respuestas devueltas por el sistema.
5. **Postcondiciones y Efectos Secundarios:** Impacto en el almacenamiento relacional, artefactos criptográficos y registros de auditoría.
