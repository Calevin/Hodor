# Spec 002 — Gestión de Sesión Activa en la Interfaz de Usuario

- **Tarea ID:** `002`
- **Identificador:** `gestion_sesion_activa_ui`
- **Origen:** Prosa
- **Estado:** `APROBADA`

## 1. Contexto y Objetivo (El POR QUÉ)

Actualmente, cuando un usuario navega a la interfaz de acceso del proveedor de identidad Hodor (`/login`) teniendo ya una sesión web válida y autenticada en su navegador, el sistema despliega nuevamente el formulario en blanco para captura de credenciales. Esta conducta genera confusión sobre el estado real de la conexión, invisibiliza la identidad activa bajo Single Sign-On (SSO) y dificulta el cierre de sesión explícito antes de acceder a otros servicios consumidores del ecosistema.

El objetivo de esta especificación es dotar a la interfaz de usuario de Hodor de la capacidad de detectar un estado de sesión activa, presentar de forma visible la cuenta autenticada, brindar acciones para continuar hacia aplicaciones clientes en caso de flujos OAuth2 pendientes, y proveer un control seguro y no redundante para cerrar la sesión actual y permitir el ingreso con cualquier cuenta.

## 2. Actores y Usuarios

- **Usuario Autenticado:** Persona que ha validado sus credenciales y cuenta con una sesión web activa en el proveedor de identidad.
- **Usuario Anónimo:** Persona que accede al proveedor de identidad sin contar con una sesión activa o cuya sesión previa ha expirado.

## 3. Historias de Usuario

- **HU-01:** Como usuario con una sesión activa en Hodor, quiero visualizar en pantalla qué cuenta está actualmente conectada cuando visite la interfaz del proveedor de identidad, para tener certeza sobre la identidad activa antes de interactuar con el ecosistema.
- **HU-02:** Como usuario con una sesión activa en Hodor, quiero disponer de una acción visible para cerrar la sesión actual mediante un método HTTP seguro, para finalizar mi acceso de forma determinista y permitir el ingreso con otra cuenta.
- **HU-03:** Como usuario autenticado en Hodor que es redirigido por una aplicación consumidora OAuth2, quiero ver el nombre de dicha aplicación y tener la opción de continuar hacia ella con mi cuenta actual o cerrar sesión antes de autorizar el acceso.

## 4. Definiciones de Dominio

- **Sesión Activa:** Contexto de seguridad vigente y persistido en el proveedor de identidad asociado al navegador del usuario.
- **Cierre de Sesión Seguro (Logout):** Invalidación inmediata y definitiva del contexto de seguridad y de los identificadores de sesión en el servidor y en el navegador ejecutada mediante petición HTTP POST, retornando al formulario de autenticación limpio.
- **Petición en Espera (Saved Request):** Parámetros del flujo de autorización OAuth2 retenidos temporalmente en sesión cuando una aplicación cliente redirige al usuario hacia el proveedor de identidad.

## 5. Requisitos Funcionales (Criterios de Aceptación en EARS)

- **RF-01 (State-Driven):** MIENTRAS un usuario mantenga una sesión autenticada activa, EL SISTEMA DEBE presentar en la interfaz de acceso (`/login`) una vista de sesión conectada con el nombre de usuario autenticado en lugar de los campos de captura de credenciales.
- **RF-02 (State-Driven):** MIENTRAS un usuario mantenga una sesión autenticada activa y visualice la vista de sesión conectada, EL SISTEMA DEBE proveer un único control interactivo para cerrar la sesión mediante una solicitud HTTP POST hacia el endpoint de logout (`/logout`).
- **RF-03 (Event-Driven):** CUANDO el usuario active el control de cierre de sesión, EL SISTEMA DEBE invalidar la sesión en el servidor, eliminar las cookies de sesión del navegador y redirigir a `/login?logout` presentando el mensaje de confirmación de salida junto al formulario limpio de inicio de sesión.
- **RF-04 (Event-Driven):** CUANDO un usuario no autenticado (anónimo o con sesión expirada) acceda a la interfaz de acceso (`/login`), EL SISTEMA DEBE presentar el formulario tradicional de solicitud de credenciales (usuario y contraseña).
- **RF-05 (Event-Driven):** CUANDO un usuario no autenticado acceda a la ruta raíz (`/`) del proveedor de identidad, EL SISTEMA DEBE responder con una redirección HTTP 302 hacia `/login`.
- **RF-06 (State-Driven):** MIENTRAS un usuario mantenga una sesión autenticada activa y acceda a la ruta raíz (`/`) del proveedor de identidad, EL SISTEMA DEBE responder con una redirección HTTP 302 hacia `/login` (donde se presentará la vista de sesión conectada conforme a `RF-01`).
- **RF-07 (Event-Driven):** CUANDO un usuario con sesión activa acceda a `/login` en el contexto de una solicitud OAuth2 retenida (`SavedRequest`), EL SISTEMA DEBE desplegar el nombre de la aplicación cliente solicitante y un botón interactivo «Continuar a [Nombre de App] como [usuario]» que prosiga el flujo de autorización hacia dicha aplicación, manteniendo disponible el control de cierre de sesión.

## 6. Requisitos No Funcionales (Verificables)

- **RNF-01 (Seguridad y Destrucción de Sesión):** La acción de cierre de sesión debe ejecutarse forzosamente mediante el método HTTP POST para mitigar ataques de prefetching y cross-site invocation.
- **RNF-02 (Internacionalización):** Todas las etiquetas, textos y mensajes de la vista de sesión conectada deben estar traducidos en los idiomas soportados (Español e Inglés).
- **RNF-03 (Consistencia de Diseño y Minimalismo):** La interfaz de sesión conectada debe mantener la línea visual existente (paleta Solarized), diseño responsivo y evitar controles redundantes que produzcan el mismo efecto de estado.

## 7. Casos Límite (Edge Cases)

- **CL-01 (Expiración de sesión durante visualización):** Si la sesión expira en el servidor mientras la vista de sesión conectada permanece abierta y el usuario interactúa, el sistema debe tratar la petición como anónima y mostrar el formulario de login regular sin provocar excepciones no controladas.
- **CL-02 (Parámetro logout explícito):** Si la petición contiene el parámetro de salida (`?logout`), el sistema debe mostrar con prioridad el formulario de login y el mensaje de confirmación de salida, ignorando cualquier estado residual.
- **CL-03 (Múltiples pestañas concurrentes):** Si el usuario cierra sesión en una pestaña y recarga otra pestaña que mostraba la sesión conectada, el sistema debe presentar el formulario de inicio de sesión estándar.

## 8. Fuera de Alcance (Out of Scope)

- Botón redundante de «Iniciar sesión con otra cuenta» (descartado para evitar duplicar la acción de logout en un modelo de sesión única).
- Soporte para concurrencia multi-cuenta en el mismo navegador (selección entre múltiples cuentas abiertas simultáneamente).
- Propagación de cierre de sesión centralizado (OIDC Front-Channel o Back-Channel Logout) hacia las aplicaciones clientes.
- Edición de perfiles de usuario o reseteo de contraseñas.

## 9. Criterios de Finalización (Definition of Done)

- Todos los requisitos funcionales (`RF-01` a `RF-07`) cuentan con verificación programática automatizada y reproducible mediante `.seb/bin/seb verify gestion_sesion_activa_ui`.
- Ausencia de violaciones a las reglas innegociables de `docs/constitution.md`.

## 10. Dudas Abiertas y Puntos por Clarificar

Ninguna duda pendiente. Se consolidó la simplificación arquitectónica (Alternativa A) eliminando el control duplicado de cambio de cuenta.
