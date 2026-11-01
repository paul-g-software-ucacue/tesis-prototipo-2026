# Implementation Plan: Proponer un docente por asignatura-paralelo

**Branch**: `031-proponer-docente` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/031-proponer-docente/spec.md` (issue #48, F06-H03)

## Summary

- **El agregado:** `VersionDistributivo`, con sus parciales y sus propuestas, en cuatro tablas nuevas del esquema `generacion`. Es la primera escritura de negocio de Generación.
- **La regla de la justificación:** `Justificacion.para(...)`, una función pura del dominio.
- **Crear:** `crearDistributivo` pregunta a Cargas si el periodo está listo y a Académico qué carreras ofertan, por rutas internas nuevas.
- **Proponer:** `proponerDocente` y `quitarPropuesta`, con el filtro de F06-H05.
- **Las horas reales:** `HorasDelPeriodo`, una función pura del dominio, reemplaza a `SinAsignacionesTodavia`. Cruza las propuestas con la oferta, así que un paralelo retirado no cuenta y las horas son siempre las actuales.
- **La sugerencia:** suma `bajoElMinimo`, la propuesta actual y si se puede proponer.
- **SPA:** el botón «Crear distributivo» crea de verdad; el diálogo propone; la oferta muestra la propuesta y el avance del parcial.
- **Pruebas:** el dominio sin dobles y `deploy/probar_propuestas.sh`, con una suma aparte de las horas.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: migración `V3__distributivo.sql` en el esquema `generacion`: `distributivo`, `version_distributivo`, `parcial_carrera`, `propuesta` y `registro_cambio`.

**Testing**:
- **Dominio:** sin dobles.
- **Aplicación:** Mockito.
- **Repositorios:** Testcontainers, en `integrationTest`.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con los dos procesos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: un módulo de plataforma, 3 servicios, los contratos, la SPA y un script.

**Performance Goals**: proponer, en menos de 3 s; crear, en menos de 5 s; con un servicio caído, el mensaje en menos de 10 s (SC-006 y SC-007, con holgura).

**Constraints**:
- solo rutas internas entre procesos (ADR-023);
- la justificación la decide el servidor (FR-013);
- el filtro por carrera, en el servidor (D-64);
- ninguna justificación ni ficha en los logs (principio IX);
- 60 % de líneas por módulo (principio VIII).

**Scale/Scope**:
- 5 operaciones públicas y 3 internas nuevas, 2 ampliadas;
- 1 migración con 5 tablas;
- 8 códigos de error;
- 1 script;
- 2 decisiones (D-77 y D-78) y 1 cambio en el modelo táctico.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El agregado y la justificación son dominio puro; los repositorios y clientes, adaptadores de salida. | ✅ |
| II. DDD según el subdominio | Generación es núcleo: agregado `VersionDistributivo` (MD-01) y objetos de valor. | ✅ |
| III. Lenguaje ubicuo | Todos los términos ya están en el glosario. `Horas` se precisó antes del código, aprobado el 2026-10-31. | ✅ |
| IV. API primero | Los contratos antes del código. | ✅ |
| V. Límites | Entre procesos, solo `/interno/…`. Sin llaves foráneas a otros esquemas. | ✅ |
| VI. Las reglas son datos | El mínimo y los pesos salen de la versión del periodo. | ✅ |
| VII. Inmutabilidad y trazabilidad | Cada cambio, en `generacion.registro_cambio`, con el docente anterior y quién. | ✅ |
| VIII. Pruebas | Primero `Justificacion`, `VersionDistributivo` y las horas. | ✅ |
| IX. Seguridad y datos personales | Solo el nombre y la identificación del docente. El texto de la justificación no va a los logs. | ✅ |
| X. Simplicidad | Sin bloqueo optimista todavía: una propuesta por paralelo se garantiza con la llave primaria. F07-H02 lo agrega. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Errores | 8 códigos con sus 8 columnas, en «tú». | ✅ |
| XIV. Validación de las entradas | La identificación, los códigos y la justificación, en el contrato, el servidor y la pantalla. | ✅ |
| §6.4 Decisiones | D-77 y D-78, aprobadas el 2026-10-31. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-77, D-78 y el cambio del modelo táctico quedaron aprobados por el autor el 2026-10-31.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/031-proponer-docente/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── generacion.md
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
backend/servicios/generacion/src/main/
├── java/ec/mdd/generacion/
│   ├── dominio/{VersionDistributivo, ParcialCarrera, Propuesta, Justificacion, TipoDeJustificacion, HorasDelPeriodo}
│   ├── aplicacion/{CrearDistributivoServicio, ProponerDocenteServicio, ConsultarParcialesServicio}
│   ├── puertos/{entrada/…, salida/RepositorioDistributivos}
│   └── adaptadores/{entrada/DistributivoControlador, salida/RepositorioDistributivosJdbc}
└── resources/db/migration/generacion/V3__distributivo.sql
backend/plataforma/errores-nucleo/                # AlcanceDeCarrera sobre un permiso
backend/servicios/cargas/                         # la preparación, por ruta interna
backend/servicios/academico/                      # toda la oferta del periodo, por ruta interna
contratos/{generacion,cargas,academico}/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/{preparacion,oferta}/
deploy/probar_propuestas.sh, .github/workflows/perfil-consolidado.yml
docs/decisiones.md                                # D-77 y D-78
```

**Structure Decision**: el distributivo es del servicio 5 (MD-01, ADR-010).

## Fases de implementación (guía para /speckit-tasks)

1. **Base:** D-77 y D-78, el modelo táctico, los contratos, los códigos y la migración.
2. **El dominio:** `Justificacion`, el agregado y las horas, sin dobles.
3. **Crear el distributivo:** la ruta de Cargas, la de Académico, el caso de uso y el tablero.
4. **Proponer:** el caso de uso, quitar, y la sugerencia con los docentes bajo el mínimo.
5. **El parcial y las horas reales.**
6. **SPA:** el tablero, el diálogo y la oferta.
7. **Punta a punta y cierre:**
   - `probar_propuestas.sh`;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación;
   - el PR de evidencia con `Closes #48`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Cruzar propuestas y oferta para las horas | Un paralelo retirado no cuenta y las horas pueden corregirse (FR-018). | Copiar las horas en la propuesta: quedarían viejas, y retirar exigiría avisar entre procesos. |
| Tres tablas para versión, parcial y propuesta | Son las entidades del agregado (MD-01), y la historia siguiente las usa. | Una sola tabla de propuestas: F06-H04, el envío del parcial y las versiones tendrían que rehacerla. |
