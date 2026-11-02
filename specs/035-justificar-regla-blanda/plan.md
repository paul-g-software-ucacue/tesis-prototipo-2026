# Implementation Plan: Justificar una asignación con una regla blanda

**Branch**: `035-justificar-regla-blanda` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/035-justificar-regla-blanda/spec.md` (issue #52, F07-H03)

## Summary

- **La regla:** `MotorReglas` evalúa RN-19 después de RN-02 y RN-13, con la severidad de la normativa.
- **La respuesta:** `proponerDocente` responde 200 con `guardada` y, si falta la justificación, una `advertencia`. Sin guardar.
- **Severidad dura:** un código nuevo, `GENERACION_AFINIDAD_INSUFICIENTE`, y el motivo `AFINIDAD` en la sugerencia.
- **La normativa:** `consultarReglasDeAsignacion` agrega la severidad de RN-19.
- **Justificada:** la propuesta dice que su inconsistencia está justificada, para que F10-H01 lo lea.
- **SPA:** la advertencia abre el campo de justificación, como aviso y no como error.
- **Pruebas:** el motor sin dobles y `deploy/probar_justificacion.sh`, con otra versión de normativa.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: sin migraciones. `justificacion` y `texto` ya están en la propuesta.

**Testing**:
- **Dominio:** sin dobles, con 80 % o más en el motor (RNF-MAN-02).
- **Aplicación:** Mockito.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con los dos procesos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 2 servicios, los contratos, la SPA y un script.

**Performance Goals**: guardar en menos de 3 s (SC-005, con holgura).

**Constraints**:
- la regla, en el servidor; la pantalla solo muestra lo que recibe;
- una advertencia no guarda nada ni deja registro;
- 60 % de líneas por módulo y 80 % en el motor (principio VIII).

**Scale/Scope**:
- 1 respuesta que cambia, `proponerDocente`;
- ninguna operación nueva;
- ninguna migración;
- 1 código de error nuevo;
- 1 script nuevo y 4 que se ajustan;
- 1 decisión (D-85) y 1 término en el glosario.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La regla es dominio puro; el texto de la advertencia, del adaptador. | ✅ |
| II. DDD según el subdominio | RN-19 entra a `MotorReglas`, e `Inconsistencia` agrega la severidad. | ✅ |
| III. Lenguaje ubicuo | «Advertencia» entra al glosario antes del código. | ✅ con T001 |
| IV. API primero | Los contratos antes del código. | ✅ |
| V. Límites | La severidad viaja por la ruta interna que ya existe. | ✅ |
| VI. Las reglas son datos | El mínimo y la severidad salen de la versión de normativa. | ✅ |
| VII. Inmutabilidad y trazabilidad | Guardar queda en el registro; una advertencia no cambia nada. | ✅ |
| VIII. Pruebas | Primero el motor, con esqueleto, viéndolo fallar. | ✅ |
| IX. Seguridad y datos personales | El registro sigue sin llevar el texto. | ✅ |
| X. Simplicidad | Sin tablas ni operaciones nuevas. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Catálogo de errores | 1 código nuevo con sus 8 columnas. La advertencia no es un error, como dice la constitución. | ✅ |
| XIV. Validación de entradas | El texto sigue con sus límites en la interfaz y en el servidor. | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/035-justificar-regla-blanda/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── generacion.md
│   └── normativa.md
└── tasks.md
```

### Source Code (repository root)

```text
contratos/
├── generacion/openapi.yaml     ResultadoDePropuesta, Advertencia, motivo AFINIDAD
├── normativa/openapi.yaml      severidadAfinidad
└── comun/errores.md            GENERACION_AFINIDAD_INSUFICIENTE

backend/servicios/normativa/    la severidad de RN-19 en la ruta interna

backend/servicios/generacion/
├── dominio/                    MotorReglas, Inconsistencia, ReglasDelPeriodo, Sugerencia, Propuesta
├── aplicacion/                 ProponerDocenteServicio
├── puertos/                    ResultadoDePropuesta, AfinidadInsuficiente
└── adaptadores/                controlador, errores y cliente interno

frontend/src/pantallas/oferta/  la advertencia abre la justificación

deploy/probar_justificacion.sh  punta a punta
```

**Structure Decision**: no hay módulos nuevos.

## Complexity Tracking

Sin violaciones de la constitución.
