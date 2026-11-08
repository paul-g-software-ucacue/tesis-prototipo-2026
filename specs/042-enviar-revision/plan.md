# Implementation Plan: Enviar a revisión con cero inconsistencias abiertas

**Branch**: `042-enviar-revision` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/042-enviar-revision/spec.md`

## Summary

Validación OCS (contexto Aprobación) suma la operación propia de EN_CONSOLIDACION: enviar a revisión.

1. `ProcesoAprobacion` comprueba sin cambiar nada el rol de la etapa, la etapa y que todos los parciales estén enviados.
2. Generación entrega por su interfaz publicada las condiciones del consolidado, calculadas en ese momento con el mismo panel que ve la pantalla: consolidado, al día, con informe, y duras y blandas abiertas.
3. Con el proceso bloqueado, el agregado aplica las condiciones en orden y pasa a la siguiente etapa activa (EN_REVISION_VRA por defecto).
4. La transición deja una entrada `CONSOLIDADO_ENVIADO_A_REVISION` en la bitácora.

Cada falta tiene su 409. El de las inconsistencias lleva el número en `detalles`. La SPA muestra en «Consolidado del periodo» lo que falta y el botón «Enviar a revisión». No hay notificación (D-97): llega con F16-H01.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya hay. Validación OCS ya depende de la interfaz publicada de Generación (F12-H02).

**Storage**: sin migraciones. La transición es un `PASO_DE_ETAPA`, que la tabla `validacionocs.transicion` ya admite (research §5).

**Testing**:
- **Dominio** (sin dobles), en `ProcesoAprobacion`:
  - `validarRevision` y `enviarARevision`: el rol, la etapa, los parciales pendientes, cada condición en su orden y el número de abiertas;
  - la siguiente etapa activa con EN_REVISION_VRA desactivada;
  - que un rechazo no cambia nada;
  - la acción `ENVIAR_A_REVISION`.
- **Aplicación** (Mockito):
  - `FlujoDelDistributivoServicio.enviarARevision`: no llama a Generación si el dominio rechaza; una sola entrada con su acción y su detalle; nada registrado si se rechaza;
  - `ParcialesDelDistributivoServicio.condicionesDeRevision` en Generación.
- **Integración** (PostgreSQL real): una prueba nueva en Validación OCS con el servicio, el repositorio y el registro reales y un `Distributivo` falso. Comprueba el envío que pasa, la transición guardada y dos envíos simultáneos con un solo paso (research §10).
- **Contrato:** `FlujoContratoTest` y `PermisosValidacionocsTest`.
- **SPA:** Vitest de `EstadoDelFlujo` y `Consolidado`; Playwright `consolidado.spec.ts` con axe-core.
- **Punta a punta:** `deploy/probar_revision.sh` (los rechazos con datos reales).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web, con dos servicios del proceso `distributivo`, la SPA y un script.

**Performance Goals**: el envío responde con el estado nuevo en la misma respuesta (SC-002). El Responsable envía en menos de 30 s desde que abre el consolidado (SC-005).

**Constraints**:
- No se abre una transacción durante la llamada a Datos que hace el panel (D-83, research §2).
- Una entrada de bitácora por transición (D-95).
- El 403 no dice si faltó el permiso o el rol.
- El número del 409 coincide con el del panel (SC-001): sale del mismo cálculo.

**Scale/Scope**:
- 1 operación nueva en el contrato de Validación OCS, más 1 valor de `acciones`;
- 1 método y 1 registro nuevos en la interfaz publicada de Generación;
- 6 códigos de error nuevos;
- 1 acción nueva en la bitácora;
- 2 componentes de la SPA;
- 1 script nuevo;
- 1 decisión (D-97), ya registrada.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las condiciones y su orden están en `ProcesoAprobacion`, en dominio puro. Generación las calcula en su aplicación y el adaptador traduce. | ✅ |
| II. DDD | Aprobación es central: el agregado protege la regla (rol, etapa, condiciones). Se diseña con pruebas primero. | ✅ |
| III. Lenguaje ubicuo | «Panel de inconsistencias», «inconsistencia abierta», «informe sustentado», «Responsable de consolidación» y `ConsolidadoEnviadoARevision`, del glosario §6 y §7. | ✅ |
| IV. API primero | `enviarARevision` va en `contratos/validacionocs/openapi.yaml` antes del código. El método de Generación está en `contracts/generacion.md`. | ✅ |
| V. Límites | Validación OCS no lee tablas de Generación: usa su interfaz publicada, con tipos simples. Spring Modulith lo verifica. | ✅ |
| VI. Las reglas son datos | La etapa siguiente y su rol salen de la configuración. Las severidades del panel salen de la normativa. | ✅ |
| VII. Inmutabilidad y trazabilidad | La transición solo se inserta, con la versión y el hash. La bitácora lleva la huella del informe. | ✅ |
| VIII. Pruebas | Primero el dominio, viéndolo fallar. El envío que pasa tiene prueba de integración; los rechazos, script de punta a punta. 60 % por módulo. | ✅ |
| IX. Seguridad | Permiso en el contrato y rol de la etapa en el dominio, en el backend; 403 sin detalle. | ✅ |
| X. Simplicidad | Sin migraciones, sin un tipo de transición nuevo y sin consulta nueva para la pantalla (research §5 y §9). | ✅ |
| XI. Diseño pragmático | Un método más en la interfaz publicada que ya existe. | ✅ |
| XII. JavaDoc | Las operaciones nuevas del agregado, el método publicado, el registro y el servicio. | ✅ |
| XIII. Errores | 6 códigos nuevos, con sus 8 columnas, cubiertos por la prueba de contrato. | ✅ |
| XIV. Validación | `periodo` con el patrón de los contratos existentes; la operación no tiene cuerpo. | ✅ |

**Post-diseño:** sin cambios. D-97 quedó registrada antes de implementar.

## Project Structure

### Documentation (this feature)

```text
specs/042-enviar-revision/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── validacionocs.md
│   ├── generacion.md
│   ├── bitacora.md
│   ├── spa.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml     enviarARevision; ENVIAR_A_REVISION en acciones
contratos/comun/errores.md               6 códigos nuevos

backend/servicios/generacion/
├── puertos/entrada/                     ParcialesDelDistributivo.condicionesDeRevision, CondicionesDeRevision
└── aplicacion/                          ParcialesDelDistributivoServicio (usa ConsultarConsolidado)

backend/servicios/validacionocs/
├── dominio/                             ProcesoAprobacion (validarRevision, enviarARevision), CondicionesDeRevision,
│                                        AccionDelProceso.ENVIAR_A_REVISION y 6 errores
├── puertos/entrada|salida/              EnviarARevision; Distributivo.condicionesDeRevision; CambioDeFlujo.Accion
├── aplicacion/                          FlujoDelDistributivoServicio.enviarARevision
├── adaptadores/entrada/                 FlujoControlador; ErroresValidacionocs
├── adaptadores/salida/                  DistributivoDesdeGeneracion
└── src/integrationTest/                 EnvioARevisionIntegrationTest

frontend/src/pantallas/comun/EstadoDelFlujo.tsx      ENVIAR_A_REVISION y faltan
frontend/src/pantallas/consolidado/Consolidado.tsx   arma faltan
frontend/e2e/consolidado.spec.ts

deploy/probar_revision.sh                y su paso en perfil-consolidado
```

**Structure Decision**: no hay módulos nuevos.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Con los datos de prueba, el envío que pasa no se alcanza de punta a punta (research §10). | Prueba de integración con PostgreSQL real para el éxito y la concurrencia. El script y la demo cubren los rechazos con el número real del panel. Se avisa en el quickstart y en la evidencia. |
| Un consolidado que cambia entre leer las condiciones y registrar. | En EN_CONSOLIDACION los parciales no admiten cambios. Lo que sí puede pasar solo mejora las condiciones, y un parcial nuevo lo atrapa el agregado (research §2 y §4). |
| Una inconsistencia dura deja el distributivo detenido en EN_CONSOLIDACION. | La pantalla dice qué falta. Devolver el parcial es #211 (F12-H05). |
| El panel llama a Datos: si no responde, el envío falla. | Error del catálogo 502 o 504, sin transición (Edge Cases). |

## Complexity Tracking

Sin violaciones que justificar.
