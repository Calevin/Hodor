# Plan Técnico — gestion_sesion_activa_ui (Spec 002)

- **Spec Origen:** `specs/002_gestion_sesion_activa_ui_spec.md`
- **Verificador Asociado:** `specs/002_gestion_sesion_activa_ui_verificador.md`
- **Estado:** `PLANIFICADO`

## 1. Resumen Arquitectónico y Aislamiento de Lógica

El diseño implementa la detección de sesiones autenticadas, renderizado condicional de estado y redirecciones seguras en la capa de interfaz de usuario de Hodor:
- **Aislamiento en Controladores UI:** La lógica de detección de sesión se concentra en `LoginController` mediante el objeto `Authentication` de Spring Security y la inspección de `RequestCache` (`SavedRequest`). No se contamina la capa de dominio ni los servicios de aplicación con lógica de presentación HTTP.
- **Seguridad HTTP en Cierre de Sesión:** El cierre de sesión (`Logout`) se canaliza exclusivamente a través de un único formulario HTTP POST dirigido a `/logout`, garantizando la destrucción atómica de la sesión en Spring Session JDBC y la eliminación de cookies sin exposición a vulnerabilidades de prefetching asociadas a peticiones GET.
- **Normalización de la Ruta Raíz (`/`):** Se añade la ruta raíz `/` a la configuración de seguridad (`SecurityConfig`) como permitida y se implementa una redirección HTTP 302 hacia `/login`, centralizando toda la experiencia de sesión en una única vista coherente.
- **Renderizado Reactivo y Minimalista en Thymeleaf:** La plantilla `login.html` se bifurca condicionalmente con directivas `th:if="${isAuthenticated}"` para presentar la tarjeta de sesión activa (con un único botón no redundante de cierre de sesión) o `th:unless="${isAuthenticated}"` para desplegar el formulario tradicional de credenciales.

## 2. Mapa de Archivos a Crear o Modificar

| Archivo / Ruta | Acción (`CREAR` / `MODIFICAR`) | Responsabilidad Técnica | Requisitos Cubiertos (`RF-XX`) |
| :------------- | :----------------------------- | :---------------------- | :----------------------------- |
| `src/main/java/com/calevin/hodor/infrastructure/security/config/SecurityConfig.java` | `MODIFICAR` | Exponer la ruta `/` en `permitAll()` para permitir la redirección controlada por el controlador | `RF-05`, `RF-06` |
| `src/main/java/com/calevin/hodor/infrastructure/ui/controller/LoginController.java` | `MODIFICAR` | Mapear `/` y `/login`, inyectar `Authentication`, poblar atributos de sesión y `SavedRequest` | `RF-01`, `RF-04`, `RF-05`, `RF-06`, `RF-07` |
| `src/main/resources/templates/login.html` | `MODIFICAR` | Tarjeta de sesión activa condicional con botón POST único de logout y botón continuar OAuth2 | `RF-01`, `RF-02`, `RF-04`, `RF-07`, `RNF-03` |
| `src/main/resources/messages.properties` | `MODIFICAR` | Claves de internacionalización en español para los nuevos elementos de la UI | `RNF-02` |
| `src/main/resources/messages_en.properties` | `MODIFICAR` | Claves de internacionalización en inglés para los nuevos elementos de la UI | `RNF-02` |
| `src/test/java/com/calevin/hodor/infrastructure/ui/controller/LoginControllerTest.java` | `CREAR` | Suite de tests WebMvc aislados para validar las transiciones de estado de login, raíz y logout | `RF-01` a `RF-07`, `RNF-01` |

## 3. Contratos, Firmas y Modelo de Datos

### Métodos del Controlador (`LoginController`)

```java
@GetMapping("/")
public String root() {
    // Redirección HTTP 302 estándar hacia /login
    return "redirect:/login";
}

@GetMapping("/login")
public String login(
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "logout", required = false) String logout,
        Authentication authentication,
        HttpServletRequest request,
        HttpServletResponse response,
        Model model) {
    // 1. Si logout está presente o la autenticación es nula/anónima:
    //    isAuthenticated = false -> Renderizar formulario limpio de login.
    // 2. Si authentication != null && authentication.isAuthenticated() && !isAnonymous && logout == null:
    //    isAuthenticated = true, username = authentication.getName().
    //    Verificar savedRequest: si existe, extraer client_id y redirectUrl para 'continueUrl'.
}
```

### Modelo de Vista (Atributos agregados al Model)

- `isAuthenticated` (`boolean`): Indica si existe una sesión activa para bifurcar la vista Thymeleaf.
- `username` (`String`): Nombre del usuario autenticado actual.
- `continueUrl` (`String`): URL de redirección original de la petición OAuth2 retenida (`SavedRequest`).

## 4. Decisiones Técnicas Justificadas

- **Decisión 1:** Cierre de sesión exclusivamente mediante formulario HTTP POST hacia `/logout` y descarte del botón clon de switch account.
  - _Alternativa descartada:_ Mantener dos botones idénticos que redirigen a `/logout`. Se descartó por redundancia y confusión cognitiva en un modelo de sesión única.
- **Decisión 2:** Redirección 302 de `/` hacia `/login` en lugar de servir vistas duplicadas.
  - _Alternativa descartada:_ Mapear `/` directamente a otra plantilla. Se descartó para no duplicar lógica de controladores ni generar divergencias de assets o estados entre URLs.
- **Decisión 3:** Unificación en la plantilla `login.html` mediante bifurcación condicional (`th:if` / `th:unless`).
  - _Alternativa descartada:_ Crear una plantilla `session.html` separada. Se descartó porque la gestión de tema solarized, selector de idioma y decoraciones CSS ya están consolidadas en `login.html`.

## 5. Secuencia de Tareas Atómicas (TDD: Test en Rojo -> Código -> Test en Verde)

- [x] **T1. Preparación de Suite de Tests WebMvc y Casos Base (TDD)** (`RF-04`, `RF-05`, `RF-06`)
  - _Archivos:_ `LoginControllerTest.java`
  - _Hecho cuando:_ Tests para acceso anónimo a `/login` y redirección desde `/` implementados.

- [x] **T2. Configuración de Seguridad y Redirección de Raíz** (`RF-05`, `RF-06`)
  - _Archivos:_ `SecurityConfig.java`, `LoginController.java`
  - _Hecho cuando:_ Pruebas de acceso a `/` (302 redirect) pasan en verde.

- [x] **T3. Lógica de Detección de Sesión Activa y Contexto OAuth2 en LoginController** (`RF-01`, `RF-04`, `RF-07`)
  - _Archivos:_ `LoginController.java`, `LoginControllerTest.java`
  - _Hecho cuando:_ Pruebas de modelo (`isAuthenticated`, `username`, `continueUrl`) pasan en verde.

- [x] **T4. Internacionalización y Actualización Minimalista de Vista Thymeleaf** (`RF-01`, `RF-02`, `RF-03`, `RNF-02`, `RNF-03`)
  - _Archivos:_ `messages.properties`, `messages_en.properties`, `login.html`, `login.css`
  - _Hecho cuando:_ La vista renderiza un único control de logout sin botones redundantes y pasa 100% de la suite.
