# Verificador Programático — gestion_sesion_activa_ui (Spec 002)

> **Contrato Inmutable (`seb-harness`):** Este archivo es ejecutado automáticamente por `.seb/bin/seb verify gestion_sesion_activa_ui` y por el hook `pre-commit` de Git.
> **Prohibición Anti-Reward-Hacking:** El agente implementador tiene terminantemente prohibido editar este archivo o relajar los tests invocados.

## 1. Matriz de Controles Ejecutables (Parseada por `seb verify`)

| Check ID | Requisito | Descripción de la Verificación | Comando Determinista |
| :--- | :--- | :--- | :--- |
| 002-CHK-01 | 002-RF-01 | Test web de visualización de sesión activa con usuario conectado | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticated_ShowsActiveSessionView` |
| 002-CHK-02 | 002-RF-02 | Test web de presencia del formulario POST único de cierre de sesión | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticated_ContainsLogoutPostForm` |
| 002-CHK-03 | 002-RF-02 | Test web de ausencia de controles redundantes de sesión | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticated_DoesNotContainRedundantControls` |
| 002-CHK-04 | 002-RF-03 | Test web de cierre de sesión POST con redirección e invalidación | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#logout_WhenTriggered_InvalidatesAndRedirectsToLogin` |
| 002-CHK-05 | 002-RF-04 | Test web de formulario de credenciales para usuarios anónimos en /login | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAnonymous_ShowsCredentialsForm` |
| 002-CHK-06 | 002-RF-05 | Test web de redirección 302 a /login al acceder como anónimo a la raíz / | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#root_WhenAnonymous_RedirectsToLogin` |
| 002-CHK-07 | 002-RF-06 | Test web de redirección 302 a /login al acceder como autenticado a la raíz / | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#root_WhenAuthenticated_RedirectsToLogin` |
| 002-CHK-08 | 002-RF-07 | Test web de opción para continuar flujo OAuth2 con cliente retenido | `direnv exec . ./mvnw test -Dtest=LoginControllerTest#login_WhenAuthenticatedWithSavedRequest_ShowsContinueOption` |
| 002-CHK-09 | 002-RNF-02 | Verificación de existencia de claves de internacionalización de sesión activa | `grep -q "login.session.active" src/main/resources/messages.properties && grep -q "login.session.active" src/main/resources/messages_en.properties` |
| 002-CHK-98 | 002-RNF-03 | Análisis estático y compilación estricta Java 25 | `direnv exec . ./mvnw test-compile` |
| 002-CHK-99 | GLOBAL | Suite completa de controladores y servicios sin regresiones | `direnv exec . ./mvnw test -Dtest=LoginControllerTest,AdminControllerTest,ClientManagementServiceTest` |

## 2. Criterio de Aprobación

- La tarea solo se considera implementada cuando `.seb/bin/seb verify gestion_sesion_activa_ui` reporta **100% de los controles (`002-CHK-01` a `002-CHK-99`) en estado `[PASA]`**.
