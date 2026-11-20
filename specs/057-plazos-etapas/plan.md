# Implementation Plan: Seguir los plazos de cada etapa

**Branch**: `057-plazos-etapas` | **Date**: 2026-11-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/057-plazos-etapas/spec.md`

## Summary

**El plazo de la etapa** (research §1, §2):
- La fecha límite es la llegada a la etapa actual más los días de esa etapa.
  - La llegada es la última transición que cambia de etapa o, si no hay, la creación del distributivo.
  - Los días salen de la configuración que regía al crear el distributivo, reconstruida con `EtapasAlCrearse`, porque la copia del proceso no guarda el plazo.
- Un objeto de valor nuevo, `Plazo`, da el estado: `A_TIEMPO`, `VENCE_HOY` o `VENCIDO`, en hora de Ecuador.

**La revisión diaria** (§3 a §5):
- `RevisarPlazosServicio` corre a las 07:00 de Ecuador (`@Scheduled`, configurable) y al arrancar.
- Para cada proceso en curso con el plazo vencido y el periodo no cerrado, con el proceso bloqueado:
  - inserta el vencimiento en `plazo_vencido`, con una clave única por llegada;
  - si lo insertó, avisa al rol de la etapa y al rol de EN_REVISION_VRA (el Vicerrector), con el evento de Notificaciones de D-108, y anota `PLAZO_VENCIDO` en la bitácora.
- No bloquea nada.

**El plazo a la vista** (§6, §8): `EstadoDelFlujo` suma `plazo` y `etapas`, calculados en el servidor con el mismo reloj, y la tarjeta del flujo los muestra con texto, no solo con color.

**RN-23** (§7):
- Al cargar la configuración, el último plazo es la fecha de la carga más la suma de los plazos hasta APROBADO, y se compara con el inicio del periodo (de Académico, que suma `inicio` a su ruta interna) menos los días de la normativa.
- Los días los da Normativa: RN-23, `dias_antes_del_inicio = 30`, con una ruta interna nueva.
- Si no hay margen, la carga responde 200 con la advertencia `RN23_MARGEN_INSUFICIENTE`; sin versión de normativa, con `RN23_SIN_NORMATIVA`. La advertencia se guarda con la carga.

**Los scripts** (§9):
- `probar_avance.sh` corre hacia atrás 6 días las transiciones de prueba y reinicia `distributivo` con `identidad` detenido: el aviso llega una vez.
- `probar_flujo.sh` prueba la advertencia con tres periodos.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están, sin dependencias nuevas. La programación de Spring ya está habilitada por `plataforma/eventos`.

**Storage**: PostgreSQL.
- Validación OCS V9: la tabla `plazo_vencido` y la columna `configuracion_flujo.advertencia`.
- Normativa V7: la regla RN-23 con su parámetro en cada versión.

**Testing**:
- **Dominio de Validación OCS** (sin dobles, viéndolos fallar primero):
  - `PlazoTest`: la fecha límite en Ecuador cerca de la medianoche UTC, los tres estados y los días;
  - `ProcesoAprobacionTest`:
    - `llegadaALaEtapa` con envío de parcial, último parcial, devolución, rechazo, versión nueva y reforma;
    - `destinosDelVencimiento` en BORRADOR_CARRERA, EN_REVISION_VRA, CONGELADO_EN_FIRMA y EN_OCS;
    - `vencimiento`;
    - el plazo vencido no cambia `acciones` ni las validaciones;
  - `AdvertenciaDeRemisionTest`;
  - `MensajesDePlazoTest`.
- **Aplicación (Mockito en los puertos):**
  - `RevisarPlazosServicioTest`;
  - `CargarConfiguracionServicioTest`: la advertencia, sin normativa y con Normativa caída;
  - `FlujoDelDistributivoServicioTest`: `plazo` y `etapas`;
  - `EtapasAlCrearse`: `plazos` con la carga anterior a la creación.
- **Integración (PostgreSQL real):**
  - `RevisarPlazosIntegrationTest`: tres revisiones, una fila y un evento; dos revisiones a la vez;
  - `ValidacionocsEsquemaIntegrationTest`: V9;
  - `RepositorioConfiguracionIntegrationTest`: la advertencia;
  - Normativa: V7 y la copia de parámetros.
- **Contrato:**
  - `FlujoContratoTest`;
  - `ConfiguracionContratoTest`;
  - `InternoNormativaContratoTest`;
  - `InternoAcademicoContratoTest`.
- **SPA:** Vitest y Playwright con axe-core ([contracts/spa.md](contracts/spa.md)).
- **Punta a punta:** `probar_avance.sh` y `probar_flujo.sh` ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- Validación OCS, en el proceso `distributivo`;
- Normativa y Académico, en el proceso `datos`;
- la SPA y dos scripts.

**Performance Goals**:
- SC-004: el aviso antes de 2 minutos desde que vuelve `identidad`.
- SC-005: la tarjeta, legible en menos de 10 segundos.
- La revisión de los procesos en curso (uno por periodo) termina en segundos.

**Constraints**:
- **D-89:** días corridos desde la llegada.
- **RF-34:** la configuración de un distributivo en curso no cambia.
- **D-108 y MD-03:** el camino de las notificaciones, que no se pierden ni se repiten.
- **Principio VI:** los 30 días son de la normativa, y el Vicerrector es el rol de EN_REVISION_VRA.
- **Principio X:** sin colas ni infraestructura nueva.

**Scale/Scope**:
- **Validación OCS:** 1 caso de uso nuevo con su adaptador programado, 4 objetos de valor (`Plazo`, `PlazosDeEtapas`, `VencimientoDePlazo`, `AdvertenciaDeRemision`), `MensajesDePlazo`, 3 operaciones de consulta del agregado, 2 puertos de salida nuevos y 3 que cambian, 1 acción de la bitácora, 2 esquemas que cambian en el contrato y 1 migración.
- **Normativa:** 1 regla, 1 ruta interna y 1 migración.
- **Académico:** 1 campo en la ruta interna.
- **SPA:** 2 bloques de la tarjeta y 1 aviso.
- **Scripts:** 8 pasos en `probar_avance.sh` y 3 en `probar_flujo.sh`.
- **La decisión D-114.**

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El plazo, la llegada, los destinos y la advertencia son del dominio. El reloj, la programación, la base, Académico, Normativa, Notificaciones y la bitácora quedan detrás de puertos o en adaptadores. | ✅ |
| II. DDD | `Plazo` y `PlazoDeEtapaVencido` son del glosario; el agregado `ProcesoAprobacion` responde por su llegada y sus destinos. | ✅ |
| III. Lenguaje ubicuo | Usa «plazo», «etapa», «llegada», «fecha límite», «vencido», `Plazo`, `VencimientoDePlazo` y `PLAZO_VENCIDO`. | ✅ |
| IV. API primero | `PlazoDeLaEtapa`, `EtapaDelDistributivo`, `AdvertenciaDelFlujo`, `inicio` y `RemisionDelPeriodo` van en los `openapi.yaml` antes del código. | ✅ |
| V. Límites | Validación OCS lee Académico y Normativa por sus rutas internas (ADR-023) y notifica con el evento que ya usa. Nadie lee el esquema de otro. | ✅ |
| VI. Las reglas son datos | Los días por etapa salen de la configuración; los 30 días de RN-23, de la versión de normativa del periodo; el Vicerrector es el rol de EN_REVISION_VRA; la hora de la revisión es configurable. | ✅ |
| VII. Inmutabilidad | `plazo_vencido` es solo de inserción; las transiciones no cambian; cada vencimiento y cada carga dejan su registro en la bitácora. | ✅ |
| VIII. Pruebas | El dominio primero, sin dobles; la aplicación con Mockito; PostgreSQL real para la idempotencia; el script con la caída de `identidad`. | ✅ |
| IX. Seguridad | No hay rutas públicas nuevas. Las internas exigen el token de proceso. Los mensajes no llevan datos personales. Ninguna opción de «fecha simulada» llega a producción. | ✅ |
| X. Simplicidad | No suma estados, transiciones ni colas, ni guarda el plazo en la copia: lo reconstruye como ya se hace con la cadena de firmas. La revisión es una tarea programada del proceso que ya existe. | ✅ |
| XI. Diseño pragmático | Sigue los moldes de `MensajesDeTransicion`, `PublicadorDeNotificaciones`, D-86 en Normativa y las advertencias de las cargas. | ✅ |
| XII. JavaDoc | Explica por qué el plazo se reconstruye, qué reinicia la llegada, por qué un aviso por llegada y por qué la advertencia no rechaza. | ✅ |
| XIII. Errores | No hay códigos nuevos: la advertencia no es un error. La caída de Normativa usa el 502 o el 504 común. | ✅ |
| XIV. Validación | No hay entradas nuevas del usuario. El `cron` lo valida Spring al arrancar, y las respuestas internas se leen con su esquema. | ✅ |

**Post-diseño:** sin violaciones. Dos ajustes de la spec, con research §7 y §9:
- **FR-016:** las advertencias llevan un código estable del contrato, no del catálogo de errores.
- **El supuesto de la demo:** la prueba corre hacia atrás sus datos; el sistema no tiene una fecha simulada.

La decisión D-114 se registra antes de implementar, con research §1 a §9.

## Project Structure

### Documentation (this feature)

```text
specs/057-plazos-etapas/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (validacionocs.md, interno.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml   PlazoDeLaEtapa, EtapaDelDistributivo, AdvertenciaDelFlujo
contratos/academico/openapi.yaml       EstadoDelPeriodo.inicio
contratos/normativa/openapi.yaml       consultarRemisionDelPeriodo, RemisionDelPeriodo

backend/servicios/validacionocs/
├── dominio/            Plazo, EstadoDelPlazo, PlazosDeEtapas, VencimientoDePlazo,
│                       AdvertenciaDeRemision, MensajesDePlazo, EstadoDelPeriodo (inicio),
│                       ProcesoAprobacion (llegadaALaEtapa, plazoActual, vencimiento,
│                       destinosDelVencimiento)
├── puertos/entrada/    RevisarPlazos, FlujoDelDistributivo (plazo, etapas),
│                       ConfiguracionVigente (advertencia)
├── puertos/salida/     RepositorioPlazos, AvisoDePlazoVencido, RepositorioProceso (enCurso),
│                       RepositorioConfiguracion, DatosDelPeriodo (diasDeRemision),
│                       CambioDeFlujo.Accion (PLAZO_VENCIDO)
├── aplicacion/         RevisarPlazosServicio, EtapasAlCrearse (plazos),
│                       CargarConfiguracionServicio, ConsultarConfiguracionServicio,
│                       FlujoDelDistributivoServicio (vista)
├── adaptadores/entrada/ RevisionDiariaDePlazos (@Scheduled y ApplicationReadyEvent),
│                       FlujoControlador, ConfiguracionControlador
├── adaptadores/salida/ RepositorioPlazosJdbc, PublicadorDeNotificaciones (AvisoDePlazoVencido),
│                       RepositorioProcesoJdbc, RepositorioConfiguracionJdbc, DatosDesdeProcesos
├── infraestructura/    ConfiguracionValidacionocs (los beans)
└── resources/db/migration/validacionocs/V9__plazos.sql

backend/servicios/normativa/
├── aplicacion/         ConsultarRemisionDelPeriodoServicio
├── adaptadores/entrada/ InternoNormativaControlador (remision)
└── resources/db/migration/normativa/V7__plazo_de_remision.sql

backend/servicios/academico/adaptadores/entrada/ InternoAcademicoControlador (inicio)
backend/apps/distributivo/src/main/resources/application.yaml   mdd.validacionocs.plazos.revision

frontend/src/pantallas/comun/ EstadoDelFlujo.tsx (+ prueba)
frontend/src/pantallas/flujo/ Flujo.tsx (+ prueba)
frontend/e2e/consolidado.spec.ts, frontend/e2e/flujo.spec.ts
deploy/probar_avance.sh, deploy/probar_flujo.sh
docs/decisiones.md       D-114
```

**Structure Decision**:
- El plazo y su revisión son de Aprobación (Validación OCS), dueña del flujo y de `Plazo` según el glosario.
- Normativa da la cifra de RN-23, y Académico, el inicio del periodo.
- Notificaciones, Firma y Generación no cambian.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El día de la fecha límite se corre uno por la zona horaria: una llegada a las 20:00 de Ecuador ya es el día siguiente en UTC. | Todo cálculo de fecha usa `ProcesoAprobacion.ZONA`. `PlazoTest` prueba llegadas cerca de la medianoche UTC. |
| La revisión avisa una etapa que se acaba de dejar. | Con el proceso bloqueado, la revisión vuelve a calcular el plazo antes de insertar (research §3). |
| Un reinicio, el `cron` y el arranque a la vez duplican el aviso. | La clave única de `plazo_vencido` y la publicación solo si se insertó. Lo prueba `RevisarPlazosIntegrationTest`. |
| Al desplegar en la demo, 2026-I avisa su vencimiento real. | Es lo esperado y sirve de evidencia (quickstart §4). Es un solo aviso por llegada. |
| Correr hacia atrás las transiciones en el script rompe su orden. | Se corren todas las del periodo a la vez, con el mismo intervalo, y P3 comprueba el orden. |
| El reinicio de `distributivo` alarga el job de la CI. | Se mide al implementar. P8 puede quedar solo en la prueba de integración ([contracts/demostracion.md](contracts/demostracion.md)). |
| Los pasos que ya existen en `probar_flujo.sh` cambian de respuesta por `RN23_SIN_NORMATIVA`. | La carga sigue en 200, y solo se agrega un campo. Se revisan los pasos que comparan el cuerpo completo. |
| Un `datos` anterior, sin `inicio`, durante el despliegue. | `inicio` es opcional en el cliente; sin él, la carga no calcula la advertencia y anota un `WARN` (D-55). |

## Complexity Tracking

Sin violaciones que justificar.
