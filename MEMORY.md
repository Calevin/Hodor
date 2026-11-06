# MEMORY.md — Bitácora Técnica y Restricciones (seb-harness)

> **Regla de Higiene de Contexto:** Este archivo almacena lo aprendido empíricamente en el proyecto entre sesiones.
>
> - **Límite duro:** Máximo 150 líneas (~15 KB). Resume o elimina entradas obsoletas.
> - **Promoción:** Si un aprendizaje se vuelve una ley permanente de arquitectura, muévelo a `docs/constitution.md`.
> - **Seguridad:** Prohibido guardar credenciales, tokens, rutas privadas del host o datos personales.
> - **Invariante del Agente:** Antes de proponer un diseño en `seb-planificador` o escribir código, verifica las secciones 2 y 3 para no reintentar enfoques fallidos.

## 1. Estado Actual del Proyecto

- **Fase SDD activa:** Implementación y verificación completadas para Spec `001_actualizar_cliente`.
- **Documento rector:** `docs/constitution.md` generado y activo.
- **Última Spec verificada:** `001_actualizar_cliente` (2026-11-06).
- **Estado de la suite de verificación:** 9/9 controles deterministas en `[PASA]` (100% de cobertura programática).

## 2. Callejones sin Salida y Enfoques Descartados (NO REINTENTAR)

_Registro de librerías, patrones o configuraciones que se probaron en este proyecto y fallaron o resultaron incompatibles._

- _(Vacío actualmente — Formato requerido: `[Fecha] [Tarea/Spec] Qué se intentó -> Por qué falló -> Alternativa adoptada`)_

## 3. Restricciones Técnicas y Trampas Conocidas (Gotchas)

_Limitaciones del entorno (NixOS, kernel, contenedores, base de datos, versiones del compilador/runtime) o del dominio que no son obvias leyendo el código._

- `[NixOS / Java 25]` Ejecutar `./mvnw` directamente falla si no se inicializa el devShell de Nix -> Impacto en tests o compilación: Error de `JAVA_HOME is not defined`. Mitigación: Prefijar siempre comandos con `direnv exec .` o ejecutarlos vía `nix develop --command`.
- `[Docker / Testcontainers]` Las suites de integración (`@SpringBootTest` con `@Testcontainers`) requieren acceso al socket de Docker -> Impacto en tests: Si el daemon de Docker no está accesible, fallan las pruebas que levantan PostgreSQLContainer.

## 4. Riesgos Conocidos y Deuda Técnica Activa

_Riesgos arquitectónicos o casos borde postergados conscientemente para futuras specs._

- _(Vacío actualmente — Formato requerido: `[ID-Spec] Descripción del riesgo -> Condición de mitigación`)_

## 5. Decisiones Técnicas Relevantes (Histórico Reciente)

- `[D-01]` Consolidación de Constitución desde `AGENTS.pre-seb.md` y `GEMINI.pre-seb.md` -> Alternativa descartada: Mantener múltiples archivos de contexto dispersos -> Motivo: Establecer `docs/constitution.md` como la única fuente de verdad innegociable bajo `seb-harness`.
- `[D-02]` `[001_actualizar_cliente]` Actualización parcial vía `PATCH /api/admin/clients/{clientId}` preservando `clientSecret` y reconstruyendo con `RegisteredClient.from(existing)` -> Alternativa descartada: `PUT` con payload completo -> Motivo: Evitar sobreescrituras accidentales de timeouts/secretos y desacoplar la rotación de credenciales.
