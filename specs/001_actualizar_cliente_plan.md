# Plan Técnico — actualizar_cliente (Spec 001)

- **Spec Origen:** `specs/001_actualizar_cliente_spec.md`
- **Verificador Asociado:** `specs/001_actualizar_cliente_verificador.md`
- **Estado:** `PLANIFICADO`

## 1. Resumen Arquitectónico y Aislamiento de Lógica

El diseño implementa la actualización parcial de clientes OAuth2 mediante semántica `PATCH /api/admin/clients/{clientId}` respetando la arquitectura hexagonal y las normas de `docs/constitution.md`:
- **Aislamiento de Lógica:** La lógica de mutación, validación de estado y actualización parcial se encapsula completamente en `ClientManagementService` dentro de la capa de aplicación. Los controladores se limitan a la delegación y al mapeo HTTP.
- **Inmutabilidad y Preservación de Credenciales:** El `clientId` se obtiene de la variable de ruta (`@PathVariable`) y el `RegisteredClient` existente se recupera de `RegisteredClientRepository`. Los atributos provistos en `ClientUpdateRequest` modifican el cliente usando `RegisteredClient.from(existing)`, preservando el `id` interno, el `clientSecret` encriptado preexistente y los ajustes no modificados.
- **DTOs de Entrada y Salida:** Se crean `ClientUpdateRequest` (con validaciones de formato de URIs y límites positivos) y `ClientResponse` (que modela el estado final del cliente omitiendo terminantemente cualquier secreto criptográfico).

## 2. Mapa de Archivos a Crear o Modificar

| Archivo / Ruta | Acción (`CREAR` / `MODIFICAR`) | Responsabilidad Técnica | Requisitos Cubiertos (`RF-XX`) |
| :------------- | :----------------------------- | :---------------------- | :----------------------------- |
| `src/main/java/com/calevin/hodor/application/dtos/ClientUpdateRequest.java` | `CREAR` | DTO inmutable (record) con validaciones para actualización parcial | `RF-01`, `RF-03` |
| `src/main/java/com/calevin/hodor/application/dtos/ClientResponse.java` | `CREAR` | DTO inmutable (record) para exponer el estado final sin credenciales | `RF-01`, `RF-06` |
| `src/main/java/com/calevin/hodor/application/exceptions/ClientNotFoundException.java` | `CREAR` | Excepción de dominio para clientes no encontrados (HTTP 404) | `RF-02` |
| `src/main/java/com/calevin/hodor/application/services/ClientManagementService.java` | `MODIFICAR` | Método `updateClient` con actualización parcial, `@Transactional` | `RF-01`, `RF-02`, `RF-05`, `RF-06`, `RNF-02` |
| `src/main/java/com/calevin/hodor/infrastructure/web/AdminController.java` | `MODIFICAR` | Endpoint `@PatchMapping("/clients/{clientId}")` con `@PreAuthorize` | `RF-01`, `RF-04`, `RNF-01` |
| `src/test/java/com/calevin/hodor/application/services/ClientManagementServiceTest.java` | `CREAR` | Tests unitarios aislados con Mockito para la lógica de actualización | `RF-01`, `RF-02`, `RF-05`, `RF-06` |
| `src/test/java/com/calevin/hodor/infrastructure/web/AdminControllerTest.java` | `MODIFICAR` | Tests web con MockMvc para el endpoint PATCH, validaciones y seguridad | `RF-01`, `RF-02`, `RF-03`, `RF-04`, `RF-06` |

## 3. Contratos, Firmas y Modelo de Datos

### DTOs de Aplicación

```java
public record ClientUpdateRequest(
    @Pattern(regexp = "^(https?://.+)?$", message = "redirectUri must be a valid HTTP/HTTPS URL")
    String redirectUri,

    @Pattern(regexp = "^(https?://.+)?$", message = "postLogoutRedirectUri must be a valid HTTP/HTTPS URL")
    String postLogoutRedirectUri,

    Set<@NotBlank String> scopes,

    @Positive(message = "accessTokenTimeoutMinutes must be greater than zero")
    Long accessTokenTimeoutMinutes,

    @Positive(message = "refreshTokenTimeoutDays must be greater than zero")
    Long refreshTokenTimeoutDays
) {}

public record ClientResponse(
    String clientId,
    String redirectUri,
    String postLogoutRedirectUri,
    Set<String> scopes,
    Long accessTokenTimeoutMinutes,
    Long refreshTokenTimeoutDays
) {}
```

### Excepción de Dominio

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String message) {
        super(message);
    }
}
```

### Firma del Servicio

```java
@Transactional
public ClientResponse updateClient(String clientId, ClientUpdateRequest request) {
    // 1. Buscar RegisteredClient por clientId en RegisteredClientRepository
    // 2. Si no existe, lanzar ClientNotFoundException
    // 3. Reconstruir usando RegisteredClient.from(existing)
    // 4. Aplicar mutaciones condicionales para redirectUri, postLogoutRedirectUri (soportando limpieza con ""), scopes y TokenSettings
    // 5. Persistir con registeredClientRepository.save(updated)
    // 6. Retornar ClientResponse mapeado
}
```

## 4. Decisiones Técnicas Justificadas

- **Decisión 1:** Uso de `@PatchMapping("/clients/{clientId}")` con campos opcionales en el DTO en lugar de `PUT`.
  - _Alternativa descartada:_ Obligar al cliente a enviar la totalidad de los datos en un `PUT`. Se descartó porque fuerza a conocer de antemano todos los timeouts y configuraciones previas solo para cambiar una URI post-logout, incrementando el riesgo de sobreescritura accidental.
- **Decisión 2:** Limpieza de `postLogoutRedirectUri` mediante cadena vacía `""`.
  - _Alternativa descartada:_ Interpretar `null` como orden de borrado. Se descartó porque en una actualización tipo PATCH, `null` denota "campo no provisto / no modificar", por lo que se requiere un valor centinela explícito para eliminar la URI opcional.
- **Decisión 3:** Suite de tests unitarios dedicada `ClientManagementServiceTest` separada de clases anidadas.
  - _Alternativa descartada:_ Añadir más métodos a la clase anidada dentro de `ApplicationServicesTest`. Se descartó porque la ejecución selectiva por método en Maven Surefire sobre clases anidadas JUnit 5 presenta fricciones con sintaxis de línea de comandos.

## 5. Secuencia de Tareas Atómicas (TDD: Test en Rojo -> Código -> Test en Verde)

- [x] **T1. Creación de DTOs y Excepción de Dominio** (`RF-01`, `RF-02`, `RF-03`)
  - _Archivos:_ `ClientUpdateRequest.java`, `ClientResponse.java`, `ClientNotFoundException.java`
  - _Hecho cuando:_ `direnv exec . ./mvnw test-compile` compila con éxito.
- [x] **T2. Tests Unitarios y Lógica de Servicio en ClientManagementService (TDD)** (`RF-01`, `RF-02`, `RF-05`, `RF-06`)
  - _Archivos:_ `ClientManagementServiceTest.java`, `ClientManagementService.java`
  - _Hecho cuando:_ Los comandos `CHK-01`, `CHK-06` y `CHK-07` devuelven exit code `0`.
- [x] **T3. Tests WebMvc y Endpoint PATCH en AdminController (TDD)** (`RF-01`, `RF-02`, `RF-03`, `RF-04`)
  - _Archivos:_ `AdminControllerTest.java`, `AdminController.java`
  - _Hecho cuando:_ Los comandos `CHK-02`, `CHK-03`, `CHK-04` y `CHK-05` devuelven exit code `0`.
- [x] **T4. Verificación Determinista y Análisis Estático Global** (`RNF-01`, `RNF-02`)
  - _Archivos:_ Verificación general
  - _Hecho cuando:_ `CHK-98` y `CHK-99` devuelven exit code `0` y `.seb/bin/seb verify actualizar_cliente` aprueba al 100%.
