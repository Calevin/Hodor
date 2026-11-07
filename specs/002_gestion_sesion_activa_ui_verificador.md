# Verificador Programático — gestion_sesion_activa_ui (Spec 002)

> **Contrato Inmutable (`seb-harness`):** Este archivo es ejecutado automáticamente por `.seb/bin/seb verify gestion_sesion_activa_ui` y por el hook `pre-commit` de Git.
> **Prohibición Anti-Reward-Hacking:** El agente implementador tiene terminantemente prohibido editar este archivo o relajar los tests invocados.

## 1. Matriz de Controles Ejecutables (Parseada por `seb verify`)

| Check ID | Requisito | Descripción de la Verificación | Comando Determinista |
| :--- | :--- | :--- | :--- |
| CHK-01 | RF-01 | Test web de visualización de sesión activa con usuario conectado | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticated_ShowsActiveSessionView` |
| CHK-02 | RF-02 | Test web de presencia del formulario POST único de cierre de sesión | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticated_ContainsLogoutPostForm` |
| CHK-03 | RF-02 | Test web de ausencia de controles redundantes de sesión | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticated_DoesNotContainRedundantControls` |
| CHK-04 | RF-03 | Test web de cierre de sesión POST con redirección e invalidación | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#logout_WhenTriggered_InvalidatesAndRedirectsToLogin` |
| CHK-05 | RF-04 | Test web de formulario de credenciales para usuarios anónimos en /login | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAnonymous_ShowsCredentialsForm` |
| CHK-06 | RF-05 | Test web de redirección 302 a /login al acceder como anónimo a la raíz / | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#root_WhenAnonymous_RedirectsToLogin` |
| CHK-07 | RF-06 | Test web de redirección 302 a /login al acceder como autenticado a la raíz / | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#root_WhenAuthenticated_RedirectsToLogin` |
| CHK-08 | RF-07 | Test web de opción para continuar flujo OAuth2 con cliente retenido | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticatedWithSavedRequest_ShowsContinueOption` |
| CHK-09 | RNF-02 | Verificación de existencia de claves de internacionalización de sesión activa | `grep -q "login.session.active" src/main/resources/messages.properties && grep -q "login.session.active" src/main/resources/messages_en.properties` |
| CHK-98 | RNF-03 | Análisis estático y compilación estricta Java 25 | `direnv exec . ./mvnw test-compile` |
| CHK-99 | GLOBAL | Suite completa de controladores y servicios sin regresiones | `direnv exec . ./mvnw test -Dtest=LoginControllerTest,AdminControllerTest,ClientManagementServiceTest` |

## 2. Criterio de Aprobación

- La tarea solo se considera implementada cuando `.seb/bin/seb verify gestion_sesion_activa_ui` reporta **100% de los controles (`CHK-01` a `CHK-99`) en estado `[PASA]`**.
