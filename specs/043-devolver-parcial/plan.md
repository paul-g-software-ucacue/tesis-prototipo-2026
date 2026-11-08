# Implementation Plan: Devolver un parcial a su carrera con una observación

**Branch**: `043-devolver-parcial` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/043-devolver-parcial/spec.md`

## Summary

Validación OCS (contexto Aprobación) suma la devolución desde EN_CONSOLIDACION.

1. `ProcesoAprobacion` comprueba sin cambiar nada:
   - el rol de la etapa;
   - la etapa, y que admita devolución según la copia de la configuración;
   - las carreras, sin repetirse, cada una con su parcial enviado.
2. Generación marca DEVUELTO los parciales elegidos por su interfaz publicada, todos o ninguno, en su transacción. Desde ahí, el coordinador los puede editar.
3. Con el proceso bloqueado, el agregado los pasa a DEVUELTO y el distributivo a BORRADOR_CARRERA. Guarda una transición `DEVOLUCION` con la observación de cada carrera y una entrada `DISTRIBUTIVO_DEVUELTO` en la bitácora.

El reenvío usa la operación de F12-H02. Con el último, el distributivo vuelve a la siguiente etapa activa.

La SPA:
- en «Consolidado del periodo», un diálogo para devolver una o varias carreras con su observación;
- en el estado del flujo, «devuelto por … el …» con la observación, visible también en «Oferta del periodo».

No hay notificación (D-97, D-98).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash · SQL (PostgreSQL 18, Flyway)

**Primary Dependencies**: las que ya hay. Validación OCS ya depende de la interfaz publicada de Generación (F12-H02).

**Storage**: la migración `V4__devolucion.sql` de Validación OCS (data-model.md):
- `parcial_del_proceso.estado` admite `DEVUELTO`;
- `transicion.tipo` admite `DEVOLUCION`;
- la tabla `observacion_de_devolucion`, solo de inserción;
- `admiteDevolucion` en la copia de las etapas de los procesos que ya existen.

Generación no cambia su esquema.

**Testing**:
- **Dominio** (sin dobles), en `ProcesoAprobacion`:
  - `validarDevolucion` y `devolver`: cada rechazo en su orden; los parciales y el estado después; las observaciones en la transición; que un rechazo no cambia nada;
  - los cambios de F12-H02: enviar un DEVUELTO, la acción `ENVIAR_PARCIAL` y `validarRevision`;
  - `DevolucionDeParcial` con sus límites.
- **Aplicación** (Mockito):
  - `FlujoDelDistributivoServicio.devolver`: no llama a Generación si el dominio rechaza; una sola entrada con su detalle; nada registrado si se rechaza;
  - `enviarARevision` con un parcial no enviado en Generación (research §2);
  - `ParcialesDelDistributivoServicio.marcarParcialesDevueltos`.
- **Integración** (PostgreSQL real):
  - en Validación OCS, la devolución guardada con sus observaciones, el reenvío que vuelve a EN_CONSOLIDACION, dos devoluciones a la vez y la `V4` sobre un proceso de F12-H02;
  - en Generación, marcar todo o nada y editar el parcial devuelto.
- **Contrato:** `FlujoContratoTest`, `PermisosValidacionocsTest` y `ErroresValidacionocsTest`.
- **SPA:**
  - Vitest de `EstadoDelFlujo` y `Consolidado`;
  - Playwright `consolidado.spec.ts` y `oferta.spec.ts`, con axe-core.
- **Punta a punta:** `deploy/probar_devolucion.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web, con dos servicios del proceso `distributivo`, la SPA y un script.

**Performance Goals**: la devolución responde con el estado nuevo en la misma respuesta (SC-002). El Responsable devuelve una carrera en menos de 1 minuto desde que abre el consolidado (SC-005).

**Constraints**:
- Una entrada de bitácora por transición, aunque devuelva varias carreras (D-95).
- El 403 no dice si faltó el permiso o el rol.
- Todo o nada, también entre Generación y Aprobación: si la segunda transacción falla, repetir la devolución la completa (research §2).
- La copia de la configuración manda (FR-003 de F12-H02).

**Scale/Scope**:
- 1 operación nueva en el contrato de Validación OCS, más 1 acción, 1 tipo de transición y 1 campo en `ParcialDelFlujo`;
- 1 método nuevo en la interfaz publicada de Generación;
- 5 códigos de error nuevos;
- 1 acción nueva en la bitácora;
- 1 migración;
- 2 componentes de la SPA;
- 1 script nuevo;
- 1 decisión (D-98).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La regla de devolver está en `ProcesoAprobacion`, en dominio puro. Generación marca en su aplicación y el adaptador traduce. | ✅ |
| II. DDD | Aprobación es central: el agregado protege rol, etapa, configuración y parciales. Se diseña con pruebas primero. | ✅ |
| III. Lenguaje ubicuo | «Devolución», «observación», «parcial devuelto» y `DistributivoDevuelto`, del glosario §6 y §7; los estados `DEVUELTO` de `ParcialCarrera`. | ✅ |
| IV. API primero | `devolverParciales` va en `contratos/validacionocs/openapi.yaml` antes del código. El método de Generación está en `contracts/generacion.md`. | ✅ |
| V. Límites | Validación OCS no toca tablas de Generación: usa su interfaz publicada, con tipos simples. Dos transacciones, una por servicio. Spring Modulith lo verifica. | ✅ |
| VI. Las reglas son datos | Si EN_CONSOLIDACION admite devolución y su rol salen de la copia de la configuración. | ✅ |
| VII. Inmutabilidad y trazabilidad | La transición y sus observaciones solo se insertan, con la versión y el hash. La bitácora lleva cada observación. | ✅ |
| VIII. Pruebas | Primero el dominio, viéndolo fallar. Integración con PostgreSQL real para la migración, la concurrencia y el todo o nada. Script de punta a punta. 60 % por módulo. | ✅ |
| IX. Seguridad | Permiso en el contrato y rol de la etapa en el dominio; 403 sin detalle; SQL parametrizado. | ✅ |
| X. Simplicidad | Reutiliza el reenvío de F12-H02, el bloqueo del proceso y el cálculo de «desactualizado» de F10-H02 (research §5 y §7). | ✅ |
| XI. Diseño pragmático | Un método más en la interfaz publicada que ya existe; una tabla para lo que no cabe en la transición. | ✅ |
| XII. JavaDoc | Las operaciones nuevas del agregado, `DevolucionDeParcial`, el método publicado y el servicio. | ✅ |
| XIII. Errores | 5 códigos nuevos, con sus 8 columnas, cubiertos por la prueba de contrato. | ✅ |
| XIV. Validación | Patrones y longitudes en el contrato; la observación sin espacios y las carreras repetidas, en el dominio (research §9). | ✅ |

**Post-diseño:** sin cambios. D-98 queda registrada antes de implementar.

## Project Structure

### Documentation (this feature)

```text
specs/043-devolver-parcial/
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
contratos/validacionocs/openapi.yaml     devolverParciales; DEVOLVER_PARCIALES; DEVOLUCION; ParcialDelFlujo.devolucion
contratos/comun/errores.md               5 códigos nuevos

backend/servicios/generacion/
├── puertos/entrada/                     ParcialesDelDistributivo.marcarParcialesDevueltos
└── aplicacion/                          ParcialesDelDistributivoServicio

backend/servicios/validacionocs/
├── dominio/                             ProcesoAprobacion (validarDevolucion, devolver), DevolucionDeParcial,
│                                        EtapaDelProceso.admiteDevolucion, EstadoDeEnvio.DEVUELTO,
│                                        TipoDeTransicion.DEVOLUCION, Transicion.observaciones,
│                                        AccionDelProceso.DEVOLVER_PARCIALES y 5 errores
├── puertos/entrada|salida/              DevolverParciales; Distributivo.marcarDevueltos; CambioDeFlujo.Accion
├── aplicacion/                          FlujoDelDistributivoServicio.devolver; EtapasAlCrearse
├── adaptadores/entrada/                 FlujoControlador; ErroresValidacionocs
├── adaptadores/salida/                  DistributivoDesdeGeneracion; RepositorioProcesoJdbc
├── resources/db/migration/validacionocs/V4__devolucion.sql
└── src/integrationTest/                 DevolucionIntegrationTest

frontend/src/pantallas/comun/EstadoDelFlujo.tsx      la devolución de cada parcial
frontend/src/pantallas/comun/flujo.ts                devolverParciales
frontend/src/pantallas/consolidado/Consolidado.tsx   el diálogo de devolver
frontend/e2e/consolidado.spec.ts, oferta.spec.ts

deploy/probar_devolucion.sh              y su paso en perfil-consolidado
```

**Structure Decision**: no hay módulos nuevos.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Falla la transacción de Aprobación después de que Generación marcó los parciales. | Repetir la devolución la completa (idempotente). El envío a revisión exige todos los parciales enviados en Generación (research §2). |
| La `V4` cambia el jsonb de un proceso real en la demo. | Solo agrega una clave con el valor de la plantilla, que es la configuración de la demo. Prueba de integración sobre un proceso guardado como lo guarda F12-H02. |
| Dos devoluciones con carreras distintas a la vez: la segunda deja parciales DEVUELTO en Generación. | El 409 de la segunda; el envío a revisión los atrapa; repetir la devolución o reenviar los normaliza (research §11). |
| SC-005 de F10-H03 sigue sin poder medirse con los datos de prueba. | Tarea con tiempo acotado en el perfil local; si no llega a cero duras, se documenta (research §12). |

## Complexity Tracking

Sin violaciones que justificar.
