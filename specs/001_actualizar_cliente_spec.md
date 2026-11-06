# Spec 001 — Actualizar Configuración de Cliente

- **Tarea ID:** `001`
- **Identificador:** `actualizar_cliente`
- **Origen:** Prosa
- **Estado:** `APROBADA`

## 1. Contexto y Objetivo (El POR QUÉ)

Actualmente el proveedor de identidad Hodor permite registrar nuevos clientes OAuth2/OIDC a través de la API administrativa (`POST /api/admin/clients`), pero carece de un mecanismo para modificar la configuración de un cliente ya existente. Cuando un servicio consumidor actualiza sus dominios, altera sus rutas de callback o incorpora soporte para cierre de sesión centralizado (RP-Initiated Logout), los administradores deben intervenir manualmente la base de datos o recrear el cliente perdiendo continuidad. Esta funcionalidad permite actualizar de forma parcial y segura los parámetros operativos de un cliente registrado (`redirectUri`, `postLogoutRedirectUri`, `scopes`, `accessTokenTimeoutMinutes` y `refreshTokenTimeoutDays`), preservando intacto el identificador y el secreto criptográfico preexistente.

## 2. Actores y Usuarios

- **Administrador de Sistemas:** Usuario o servicio autorizado con rol administrativo (`ROLE_ADMIN`) encargado de gestionar la configuración de los clientes en Hodor a través de la API administrativa.

## 3. Historias de Usuario

- **HU-01:** Como Administrador de Sistemas, quiero actualizar selectivamente los parámetros de configuración de un cliente existente (como su `postLogoutRedirectUri`, `redirectUri`, `scopes` o tiempos de expiración) a través de la API administrativa para sincronizar los cambios de integración sin afectar la identidad del cliente ni exponer sus secretos.

## 4. Definiciones de Dominio (Opcional)

- **Cliente OAuth2:** Aplicación o sistema registrado en Hodor que solicita tokens e interactúa mediante flujos OIDC/OAuth2.
- **Post-Logout Redirect URI:** Dirección URI autorizada a la cual el proveedor de identidad redirige al usuario tras completar un flujo de cierre de sesión iniciado por la aplicación consumidora.
- **Redirect URI:** Dirección URI autorizada a la cual el proveedor de identidad envía el código de autorización tras la autenticación exitosa.
- **Actualización Parcial (PATCH Semántico):** Mecanismo de actualización donde únicamente los atributos explícitamente provistos en el cuerpo de la solicitud son modificados, manteniendo intactos los valores de los atributos no enviados.

## 5. Requisitos Funcionales (Criterios de Aceptación en EARS)

- **RF-01 (Event-Driven):** CUANDO un Administrador solicite la actualización parcial de un cliente mediante una petición a su ruta identificadora (`PATCH /api/admin/clients/{clientId}`) enviando uno o más atributos mutables válidos (`redirectUri`, `postLogoutRedirectUri`, `scopes`, `accessTokenTimeoutMinutes`, `refreshTokenTimeoutDays`), EL SISTEMA DEBE persistir las modificaciones preservando los valores preexistentes de los campos no provistos y responder con código HTTP 200 (OK) retornando la representación del cliente con su estado final.
- **RF-02 (Unwanted Behavior):** SI se solicita la actualización de un cliente cuyo identificador (`clientId`) no existe en el sistema, ENTONCES EL SISTEMA DEBE rechazar la petición con código HTTP 404 (Not Found) y un mensaje descriptivo sin alterar ningún registro.
- **RF-03 (Unwanted Behavior):** SI la solicitud de actualización contiene valores inválidos (tales como formato de URI malformado en `redirectUri` o `postLogoutRedirectUri`, conjunto vacío en `scopes` cuando se envíe, o valores menores o iguales a cero en los tiempos de expiración), ENTONCES EL SISTEMA DEBE rechazar la petición con código HTTP 400 (Bad Request) detallando las infracciones de validación.
- **RF-04 (Unwanted Behavior):** SI un solicitante no autenticado o que carezca del rol administrativo (`ROLE_ADMIN`) intenta invocar la actualización de un cliente, ENTONCES EL SISTEMA DEBE denegar el acceso respondiendo con código HTTP 401 (Unauthorized) o 403 (Forbidden).
- **RF-05 (Ubiquitous):** EL SISTEMA DEBE garantizar que el identificador de cliente (`clientId`) sea inmutable y no pueda ser alterado mediante la operación de actualización.
- **RF-06 (Ubiquitous):** EL SISTEMA DEBE preservar intacto el secreto criptográfico (`clientSecret`) del cliente durante la actualización parcial y omitir dicho secreto en la respuesta HTTP 200.

## 6. Requisitos No Funcionales (Verificables)

- **RNF-01 (Seguridad y Control de Acceso):** El endpoint de actualización debe residir bajo el prefijo `/api/admin/**` y aplicar autorización obligatoria de rol administrativo conforme a las reglas de `docs/constitution.md`.
- **RNF-02 (Transaccionalidad):** La actualización de la configuración del cliente debe ejecutarse bajo una frontera transaccional atómica, revirtiendo cualquier cambio ante fallos imprevistos de persistencia.

## 7. Casos Límite (Edge Cases)

- **CL-01 (Remoción de URI post-logout):** Si la solicitud envía explícitamente `postLogoutRedirectUri` como cadena vacía (`""`), el sistema debe remover la URI post-logout previamente asignada al cliente.
- **CL-02 (Preservación de valores omitidos):** Cualquier campo opcional omitido o nulo en el payload de actualización debe conservar estrictamente su valor anterior sin ser reseteado a valores por defecto.
- **CL-03 (Actualización idempotente):** Si la solicitud envía valores idénticos a los ya configurados o un objeto sin modificaciones, el sistema debe responder HTTP 200 con el estado actual sin generar inconsistencias.

## 8. Fuera de Alcance (Out of Scope)

- Rotación o regeneración de secretos de cliente (`clientSecret`) en esta especificación (se gestionará en una especificación dedicada de gestión de credenciales).
- Soporte para listas múltiples de URIs (se mantiene el esquema 1:1 existente para `redirectUri` y `postLogoutRedirectUri`).
- Interfaz gráfica o vistas web Thymeleaf para gestión de clientes (aplica exclusivamente a la API REST administrativa).
- Baja o eliminación física del cliente registrado.

## 9. Criterios de Finalización (Definition of Done)

- Todos los requisitos (`RF-01` a `RF-06`) cuentan con verificación programática en `[PASA]` ejecutada mediante `.seb/bin/seb verify actualizar_cliente`.
- Cero violaciones a `docs/constitution.md`.

## 10. Dudas Abiertas y Puntos por Clarificar

Ninguna duda pendiente. Todas las aclaraciones sobre semántica, cardinalidad, atributos mutables y respuesta fueron acordadas y consolidadas en esta versión.
