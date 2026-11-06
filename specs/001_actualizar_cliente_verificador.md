# Verificador Programático — actualizar_cliente (Spec 001)

> **Contrato Inmutable (`seb-harness`):** Este archivo es ejecutado automáticamente por `.seb/bin/seb verify actualizar_cliente` y por el hook `pre-commit` de Git.
> **Prohibición Anti-Reward-Hacking:** El agente implementador tiene terminantemente prohibido editar este archivo o relajar los tests invocados.

## 1. Matriz de Controles Ejecutables (Parseada por `seb verify`)

| Check ID | Requisito | Descripción de la Verificación | Comando Determinista |
| :--- | :--- | :--- | :--- |
| CHK-01 | RF-01 | Test unitario de actualización parcial en el servicio | `direnv exec . ./mvnw test -Dtest=ClientManagementServiceTest#updateClient_Success` |
| CHK-02 | RF-01 | Test web del endpoint PATCH con 200 OK y body actualizado | `direnv exec . ./mvnw test -Dtest=AdminControllerTest#updateClient_WhenValidPayloadAndAdmin_ReturnsOk` |
| CHK-03 | RF-02 | Test de cliente inexistente con error 404 Not Found | `direnv exec . ./mvnw test -Dtest=AdminControllerTest#updateClient_WhenClientNotFound_ReturnsNotFound` |
| CHK-04 | RF-03 | Test de validación con payload inválido y error 400 Bad Request | `direnv exec . ./mvnw test -Dtest=AdminControllerTest#updateClient_WhenInvalidPayload_ReturnsBadRequest` |
| CHK-05 | RF-04 | Test de seguridad de acceso sin rol admin con 401 o 403 | `direnv exec . ./mvnw test -Dtest=AdminControllerTest#updateClient_SecurityChecks*` |
| CHK-06 | RF-05 | Test unitario de inmutabilidad del clientId en la actualización | `direnv exec . ./mvnw test -Dtest=ClientManagementServiceTest#updateClient_ClientIdIsImmutable` |
| CHK-07 | RF-06 | Test unitario de preservación y no exposición del clientSecret | `direnv exec . ./mvnw test -Dtest=ClientManagementServiceTest#updateClient_PreservesSecretAndOmittedFromResponse` |
| CHK-98 | RNF-01 | Análisis estático y compilación estricta Java 25 | `direnv exec . ./mvnw test-compile` |
| CHK-99 | GLOBAL | Verificación completa de suite unitaria y controladores sin regresiones | `direnv exec . ./mvnw test -Dtest=ClientManagementServiceTest,AdminControllerTest,ApplicationServicesTest` |

## 2. Criterio de Aprobación

- La tarea solo se considera implementada cuando `.seb/bin/seb verify actualizar_cliente` reporta **100% de los controles (`CHK-01` a `CHK-99`) en estado `[PASA]`**.
