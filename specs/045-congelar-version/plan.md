# Implementation Plan: Aprobar el contenido y congelar la versión

**Branch**: `045-congelar-version` | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/045-congelar-version/spec.md`

## Summary

El Vicerrector aprueba el contenido desde la tarjeta del flujo, con un diálogo de confirmación. Aprobar son dos transacciones, como enviar un parcial o devolver (research §1):

1. Generación congela la versión vigente por orden de Aprobación (MD-04), de forma idempotente.
2. Aprobación registra el paso de EN_REVISION_VRA a CONGELADO_EN_FIRMA, con la versión y el hash del distributivo (el mismo del envío a revisión, research §2). La bitácora recibe una entrada `CONTENIDO_APROBADO` (D-95).

**La versión congelada.**
- Rechaza toda edición con `GENERACION_VERSION_CONGELADA` (409), dentro de la transacción de cada operación (research §4).
- La SPA muestra la constancia (versión, quién, cuándo y la huella con su botón de copiar), oculta las ediciones y muestra el aviso.

**Sin revisión.** Si EN_REVISION_VRA está desactivada, el envío a revisión congela la versión igual (research §3).

**Sin migraciones ni notificaciones.**

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya hay.

**Storage**: PostgreSQL, sin migraciones. `version_distributivo.estado` ya admite `CONGELADA`, y la transición usa `PASO_DE_ETAPA`.

**Testing**:
- **Dominio:**
  - Validación OCS: `ProcesoAprobacion` (validar, aprobar, acciones, `congelaAlAvanzar`, `congelacion`);
  - Generación: `VersionDistributivo.exigirEditable` con `CONGELADA`.
- **Aplicación (Mockito):**
  - `FlujoDelDistributivoServicio.aprobarContenido`: orden de las llamadas; un rechazo no toca Generación; reintento idempotente; bitácora;
  - `enviarARevision` con EN_REVISION_VRA desactivada congela;
  - `ParcialesDelDistributivoServicio.congelarVersion`, idempotente;
  - consolidar, justificar e informe: con la versión congelada, `VersionCongelada` antes que `EnRevision`.
- **Integración (PostgreSQL real):**
  - dos aprobaciones simultáneas dejan una transición;
  - congelar y proponer a la vez: la propuesta posterior se rechaza.
- **Contrato:** Validación OCS (la operación nueva, `congelacion`, la acción) y Generación (`versionCongelada`, los 409).
- **Arquitectura:** ArchUnit y `ModulosTest`. **Permisos:** `PermisosValidacionocsTest` con una operación más.
- **SPA:** Vitest y Playwright con axe-core.
- **Punta a punta:** `deploy/probar_aprobacion.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web. Dos servicios del proceso `distributivo`, la SPA y un script.

**Performance Goals**: aprobar en menos de 1 minuto desde que se abre la pantalla (SC-003). La operación hace lo mismo que el envío de un parcial: una lectura, un hash y dos transacciones cortas.

**Constraints**:
- Principio V: Validación OCS no lee tablas de Generación; usa su API publicada.
- Sin ciclos entre módulos.
- La regla se aplica en el servidor (FR-008).
- Un rechazo no deja rastro (FR-009).

**Scale/Scope**:
- 1 operación HTTP nueva y 1 método nuevo en la API publicada de Generación;
- 2 códigos de error;
- 1 acción del proceso y 1 acción de bitácora;
- 7 operaciones de Generación con la regla de la versión;
- 2 campos nuevos en las respuestas;
- 1 diálogo y 3 pantallas;
- 1 script;
- 1 decisión (D-100).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas, en el dominio: `ProcesoAprobacion` (la etapa) y `VersionDistributivo` (la versión). La coordinación, en la aplicación. Generación se usa por su API publicada, a través de un adaptador. | ✅ |
| II. DDD | Aprobación es dueña de la etapa; Generación, de la versión. Aprobación ordena y Generación ejecuta (MD-04). | ✅ |
| III. Lenguaje ubicuo | «Aprobar el contenido», «versión congelada», «huella», «En firma». | ✅ |
| IV. API primero | La operación, `congelacion`, `versionCongelada` y los 409 se escriben en los OpenAPI antes del código. | ✅ |
| V. Límites | Sin acceso a tablas ajenas; Spring Modulith verifica las dependencias. | ✅ |
| VI. Las reglas son datos | Quién aprueba sale de la copia de la configuración (el rol de EN_REVISION_VRA), y la siguiente etapa, del orden del flujo. | ✅ |
| VII. Inmutabilidad y trazabilidad | La versión congelada no cambia; la transición y la bitácora registran quién, cuándo, la versión y el hash; un rechazo no deja rastro. | ✅ |
| VIII. Pruebas | Primero el dominio y la aplicación, viéndolos fallar; integración para la concurrencia; script de punta a punta. | ✅ |
| IX. Seguridad | Permiso `DISTRIBUTIVO:APROBACION` y rol de la etapa en el servidor; la SPA solo refleja. | ✅ |
| X. Simplicidad | Sin migraciones; reusa `PASO_DE_ETAPA`, el hash existente y el patrón de dos transacciones. | ✅ |
| XI. Diseño pragmático | Un método en la API publicada; una acción derivada de la etapa de origen, como el envío a revisión. | ✅ |
| XII. JavaDoc | La operación, el método publicado, las excepciones, las reglas del dominio y el adaptador. | ✅ |
| XIII. Errores | 2 códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación | Sin cuerpo; el periodo se valida con el patrón de siempre. | ✅ |

**Post-diseño:** sin cambios. D-100 queda registrada antes de implementar.

## Project Structure

### Documentation (this feature)

```text
specs/045-congelar-version/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── validacionocs.md
│   ├── generacion.md
│   ├── spa.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml       aprobarContenido; congelacion; APROBAR_CONTENIDO
contratos/generacion/openapi.yaml          versionCongelada; 409 GENERACION_VERSION_CONGELADA
contratos/comun/errores.md                 2 códigos

backend/servicios/generacion/
├── dominio/                               VersionCongelada; VersionDistributivo.exigirEditable
├── puertos/entrada/                       ParcialesDelDistributivo.congelarVersion,
│                                          VersionCongeladaEnGeneracion
├── aplicacion/                            ParcialesDelDistributivoServicio; Consolidar,
│                                          JustificarInconsistencia e InformeSustentado
├── adaptadores/entrada/                   ErroresGeneracion; ConsolidadoControlador y
│                                          PanelControlador con versionCongelada
└── adaptadores/salida/                    RepositorioDistributivosJdbc.congelar

backend/servicios/validacionocs/
├── dominio/                               ProcesoAprobacion; AccionDelProceso; EtapaNoAdmiteAprobacion
├── puertos/entrada/                       AprobarContenido; FlujoDelDistributivo.congelacion
├── puertos/salida/                        Distributivo.congelar; CambioDeFlujo.Accion.CONTENIDO_APROBADO
├── aplicacion/                            FlujoDelDistributivoServicio (aprobar; enviar sin revisión)
├── adaptadores/entrada/                   FlujoControlador; ErroresValidacionocs
├── adaptadores/salida/                    DistributivoDesdeGeneracion
└── src/integrationTest/                   AprobacionIntegrationTest

frontend/src/pantallas/comun/              EstadoDelFlujo.tsx, DialogoDeAprobacion.tsx, flujo.ts
frontend/src/pantallas/consolidado/Consolidado.tsx
frontend/src/pantallas/inconsistencias/Inconsistencias.tsx
frontend/e2e/consolidado.spec.ts

deploy/probar_aprobacion.sh y su paso en perfil-consolidado
docs/decisiones.md                         D-100
```

**Structure Decision**: sin módulos nuevos.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| La versión queda congelada y el distributivo sigue en EN_REVISION_VRA, si falla la segunda transacción. | Congelar es idempotente; volver a aprobar lo completa. La SPA sigue ofreciendo «Aprobar el contenido» hasta que la etapa avance. Una prueba de aplicación lo cubre. |
| Un cambio de error en propuestas y actividades rompe pruebas de la SPA o scripts que esperan `GENERACION_PARCIAL_NO_EDITABLE`. | Solo cambia con la versión congelada, que hoy no existe en ningún dato. Los scripts actuales no llegan a congelar. |
| Los scripts que esperan las acciones del flujo de cada rol (`probar_revision.sh`, `probar_devolucion.sh`, `probar_consolidado_en_revision.sh`). | Revisar sus aserciones de `acciones` para el Vicerrector, que ahora tiene `APROBAR_CONTENIDO` en EN_REVISION_VRA. |
| El script nuevo repite las 165 propuestas y alarga la CI. | Unos 50 s, medido con `probar_consolidado_en_revision.sh` (46 s). |

## Complexity Tracking

Sin violaciones que justificar.
