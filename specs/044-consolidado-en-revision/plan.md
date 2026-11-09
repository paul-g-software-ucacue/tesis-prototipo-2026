# Implementation Plan: El consolidado no cambia durante la revisión

**Branch**: `044-consolidado-en-revision` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/044-consolidado-en-revision/spec.md`

## Summary

Generación aprende si el distributivo admite cambios en su consolidado preguntándole a Aprobación por un puerto propio que Aprobación implementa. Es una inversión de la dependencia, sin ciclo entre los módulos (research §1).

1. Consolidar, justificar y adjuntar el informe consultan el puerto **dentro** de su transacción. Desde EN_REVISION_VRA se rechazan con `GENERACION_CONSOLIDADO_EN_REVISION` (409) y no cambia nada.
2. La lectura de la etapa toma un bloqueo compartido de la fila del proceso, que espera al bloqueo del envío a revisión. El envío toma su momento dentro de su transacción, así que ninguna consolidación queda después del envío (research §2).
3. Las consultas del consolidado y del panel informan `enRevision`. La SPA oculta las tres acciones y muestra el aviso.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya hay. Una interfaz nombrada nueva de Spring Modulith en Generación (`requeridos`).

**Storage**: sin migraciones.

**Testing**:
- **Dominio** (Validación OCS): `TipoDeEtapa.admiteCambiosEnElConsolidado()`.
- **Aplicación** (Mockito):
  - en Generación, las tres operaciones con el puerto, el puerto dentro de la transacción y `enRevision` en las consultas;
  - en Validación OCS, `EtapaDelConsolidadoServicio` y el momento del envío dentro de la transacción.
- **Integración** (PostgreSQL real, Validación OCS): el `FOR SHARE` retrasa el envío a revisión.
- **Contrato:** Generación, con los 409 y `enRevision`.
- **Arquitectura:** `ModulosTest` y ArchUnit.
- **SPA:** Vitest y Playwright con axe-core.
- **Punta a punta:** `deploy/probar_consolidado_en_revision.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: dos servicios del proceso `distributivo`, la SPA y un script.

**Performance Goals**: sin cambios visibles. La consulta de la etapa es una lectura por clave primaria.

**Constraints**:
- Principio V: Generación no lee tablas de Validación OCS.
- Sin ciclos entre módulos.
- La comprobación va en el servidor y en el momento de la acción (FR-006).
- Una acción rechazada no deja rastro (FR-007).

**Scale/Scope**:
- 1 puerto requerido nuevo, con su interfaz nombrada;
- 3 operaciones de Generación con la comprobación;
- 2 respuestas con `enRevision`;
- 1 código de error;
- 1 caso de uso, 1 adaptador y 1 consulta en Validación OCS;
- 2 pantallas;
- 1 script;
- 1 decisión (D-99).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La regla de etapas en el dominio de Aprobación; la comprobación en la aplicación de Generación, a través de un puerto; el adaptador en Validación OCS. | ✅ |
| II. DDD | Aprobación es la dueña de la etapa; Generación pregunta, no copia. | ✅ |
| III. Lenguaje ubicuo | «Consolidado», «informe sustentado», «justificación», «revisión del Vicerrectorado». | ✅ |
| IV. API primero | `enRevision` y el 409 se escriben en `contratos/generacion/openapi.yaml` antes del código. | ✅ |
| V. Límites | Sin acceso a tablas ajenas; inversión de la dependencia; Spring Modulith verifica la interfaz nombrada y la ausencia de ciclos. | ✅ |
| VI. Las reglas son datos | Qué etapas admiten cambios depende del orden del flujo, no de nombres fijos en Generación. | ✅ |
| VII. Inmutabilidad y trazabilidad | Lo enviado a revisión no cambia; un rechazo no deja rastro. | ✅ |
| VIII. Pruebas | Primero el dominio y la aplicación, viéndolos fallar; integración para el bloqueo; script de punta a punta. | ✅ |
| IX. Seguridad | La regla se aplica en el servidor; la SPA solo la refleja. | ✅ |
| X. Simplicidad | Sin migraciones ni estado copiado ni compensaciones (research §1). | ✅ |
| XI. Diseño pragmático | Un solo método en el puerto. | ✅ |
| XII. JavaDoc | El puerto, el caso de uso, el adaptador y la excepción. | ✅ |
| XIII. Errores | 1 código nuevo con sus 8 columnas. | ✅ |
| XIV. Validación | Sin entradas nuevas. | ✅ |

**Post-diseño:** sin cambios. D-99 queda registrada antes de implementar.

## Project Structure

### Documentation (this feature)

```text
specs/044-consolidado-en-revision/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── generacion.md
│   ├── validacionocs.md
│   ├── spa.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/generacion/openapi.yaml        enRevision; 409 en consolidar, justificar y adjuntar
contratos/comun/errores.md               GENERACION_CONSOLIDADO_EN_REVISION

backend/servicios/generacion/
├── puertos/requeridos/                  EtapaDelConsolidado (@NamedInterface("requeridos"))
├── dominio/                             ConsolidadoEnRevision
├── aplicacion/                          ConsolidarServicio, JustificarInconsistenciaServicio,
│                                        InformeSustentadoServicio, ConsultarConsolidadoServicio, el panel
├── adaptadores/entrada/                 ErroresGeneracion; los controladores con enRevision
└── infraestructura/                     ConfiguracionGeneracion recibe el puerto

backend/servicios/validacionocs/
├── dominio/                             TipoDeEtapa.admiteCambiosEnElConsolidado
├── puertos/entrada|salida/              ConsultarEtapaDelConsolidado; RepositorioProceso.estadoCompartido
├── aplicacion/                          EtapaDelConsolidadoServicio; enviarARevision con el momento en la transacción
├── adaptadores/entrada/                 EtapaParaGeneracion
├── adaptadores/salida/                  RepositorioProcesoJdbc.estadoCompartido
└── src/integrationTest/                 EtapaDelConsolidadoIntegrationTest

frontend/src/pantallas/consolidado/Consolidado.tsx
frontend/src/pantallas/inconsistencias/Inconsistencias.tsx
frontend/e2e/consolidado.spec.ts

deploy/probar_consolidado_en_revision.sh y su paso en perfil-consolidado
```

**Structure Decision**: no hay módulos nuevos; un paquete nuevo de puertos requeridos en Generación.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Spring Modulith o ArchUnit rechazan que Validación OCS implemente un tipo de Generación. | La interfaz nombrada `requeridos` lo expone explícitamente; el adaptador es una capa de adaptadores que usa un puerto (permitido). Se comprueba primero, con un esqueleto (T002). |
| Las pruebas de Spring de Generación arman su configuración sin Validación OCS. | Un doble del puerto en esas pruebas. |
| El `FOR SHARE` no entra en la transacción de Generación. | Mismo `DataSource` y mismo administrador en el proceso; lo comprueba la prueba de integración del bloqueo. |
| El script arma 165 propuestas y alarga la CI. | Por la API son unos minutos. Además cubre por primera vez el envío a revisión que pasa. |

## Complexity Tracking

Sin violaciones que justificar.
