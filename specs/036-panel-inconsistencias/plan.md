# Implementation Plan: Ver el panel de inconsistencias globales

**Branch**: `036-panel-inconsistencias` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/036-panel-inconsistencias/spec.md`

## Summary

`MotorReglas` evalúa la versión completa con siete comprobaciones globales, con la severidad, la meta, el margen y el rango de la versión de normativa. Generación entrega el panel, calculado al abrirlo, y guarda las justificaciones de las blandas. La SPA agrega la pantalla «Panel de inconsistencias». Normativa agrega el margen de 60/40 y marca las actividades obligatorias. Docentes entrega los cargos vigentes, y Académico las horas de la asignatura en la malla.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: 2 migraciones:
- Normativa, `V6`: el parámetro `margen_puntos` de RN-05 y la marca `obligatoria` del catálogo, en todas las versiones (D-68).
- Generación, `V5`: la tabla `justificacion_inconsistencia`, solo de inserción, y la acción nueva del registro.

**Testing**:
- **Dominio:** sin dobles, con 80 % o más en el motor (RNF-MAN-02).
- **Aplicación:** Mockito.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Persistencia:** integración con la base real.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con los procesos reales.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 4 servicios, los contratos, la SPA y un script.

**Performance Goals**: abrir el panel en menos de 5 s (SC-002) y guardar en menos de 3 s (SC-005), con holgura.

**Constraints**:
- las reglas, en el dominio; la pantalla solo muestra lo que recibe;
- el panel no se guarda: se calcula al abrirlo;
- las cifras y severidades salen de la versión (principio VI);
- 60 % de líneas por módulo y 80 % en el motor (principio VIII).

**Scale/Scope**:
- 2 operaciones públicas y 2 rutas internas nuevas, y 1 campo nuevo en una ruta interna;
- 2 migraciones;
- 2 códigos de error nuevos;
- 1 pantalla nueva, 1 script nuevo;
- 2 decisiones (D-86 y D-87) y 3 términos en el glosario.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las comprobaciones son dominio puro; las indicaciones, del adaptador. | ✅ |
| II. DDD según el subdominio | `MotorReglas` evalúa la versión, como dice el modelo táctico. | ✅ |
| III. Lenguaje ubicuo | Los términos nuevos entran al glosario antes del código. | ✅ con T001 |
| IV. API primero | Los contratos antes del código. | ✅ |
| V. Límites | Generación pide los datos por rutas internas; no lee otros esquemas. | ✅ |
| VI. Las reglas son datos | Severidades, meta, margen, rango y actividades obligatorias salen de la versión. | ✅ |
| VII. Inmutabilidad y trazabilidad | Las justificaciones no se reescriben, y cada una va al registro. | ✅ |
| VIII. Pruebas | Primero el motor, con esqueleto, viéndolo fallar. | ✅ |
| IX. Seguridad y datos personales | El panel lleva lo mínimo del docente, y el texto no va al registro técnico. | ✅ |
| X. Simplicidad | Sin foto del panel ni caché. Una sola tabla nueva. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Catálogo de errores | 2 códigos nuevos con sus 8 columnas. Sin distributivo no es un error. | ✅ |
| XIV. Validación de entradas | El texto, con sus límites en la interfaz y en el servidor; el tipo y el objeto, por contrato. | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/036-panel-inconsistencias/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── generacion.md
│   ├── normativa.md
│   ├── docentes.md
│   └── academico.md
└── tasks.md
```

### Source Code (repository root)

```text
contratos/
├── generacion/openapi.yaml     el panel y la justificación
├── normativa/openapi.yaml      reglas-del-panel
├── docentes/openapi.yaml       cargos-vigentes
├── academico/openapi.yaml      horasDeLaAsignatura en la oferta
└── comun/errores.md            2 códigos

backend/servicios/normativa/    V6, parámetro, marca y ruta interna
backend/servicios/docentes/     ruta interna de cargos vigentes
backend/servicios/academico/    las horas de la asignatura en la ruta interna

backend/servicios/generacion/
├── dominio/                    MotorReglas, InconsistenciaGlobal, ObjetoAfectado, ReglasDelPanel
├── aplicacion/                 ConsultarPanelServicio, JustificarInconsistenciaServicio
├── puertos/                    los casos de uso y DatosParaElPanel
└── adaptadores/                PanelControlador, cliente interno, repositorio, V5

frontend/src/pantallas/inconsistencias/  la pantalla, sus consultas y su diálogo
frontend/src/pantallas/normativa/reglas.ts  el nombre del parámetro nuevo

deploy/probar_panel.sh          punta a punta
```

**Structure Decision**: no hay módulos nuevos. La pantalla nueva sigue la forma de las demás.

## Complexity Tracking

Sin violaciones que justificar. La historia es grande por las rutas entre procesos, no por el diseño: se implementa en un solo PR, en el orden de `tasks.md`.
