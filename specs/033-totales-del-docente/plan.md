# Implementation Plan: Ver los totales del docente al instante

**Branch**: `033-totales-del-docente` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/033-totales-del-docente/spec.md` (issue #50, F07-H01)

## Summary

- **El cálculo:** `CargaHoraria` y `Proporcion`, funciones puras del dominio de Generación, con una sola forma de contar las horas, compartida con `HorasDelPeriodo`.
- **La consulta:** `consultarCargaHoraria`, de solo lectura, con el permiso de leer la oferta y sin filtro de carrera (D-82).
- **La lista:** `consultarActividades` trae los totales de cada docente en todas las carreras, en lugar de la suma de su carrera.
- **La meta:** una ruta interna nueva en Normativa, con los parámetros de RN-05.
- **SPA:** un panel común, en el formulario de actividades, en la pantalla de actividades y en la sugerencia de la oferta.
- **Pruebas:** el dominio sin dobles y `deploy/probar_totales.sh`, con una suma aparte de las horas.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: sin migraciones. Es solo lectura.

**Testing**:
- **Dominio:** sin dobles.
- **Aplicación:** Mockito.
- **Normativa:** Testcontainers, en `integrationTest`.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con los dos procesos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 2 servicios, los contratos, la SPA y un script.

**Performance Goals**: los totales, en menos de 3 s después de guardar; con un servicio caído, el mensaje en menos de 10 s (SC-004 y SC-006, con holgura).

**Constraints**:
- solo rutas internas entre procesos (ADR-023);
- el cálculo, en el servidor; la pantalla solo muestra lo que recibe;
- sumas por componente, sin el detalle de otras carreras (principio IX);
- 60 % de líneas por módulo (principio VIII).

**Scale/Scope**:
- 1 operación pública y 1 interna nuevas, y 1 respuesta que cambia;
- ninguna migración;
- ningún código de error nuevo;
- 1 panel y 1 script;
- 1 decisión (D-82) y 2 términos en el glosario.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El cálculo es dominio puro; los clientes, adaptadores de salida. | ✅ |
| II. DDD según el subdominio | `CargaHoraria` es el objeto de valor calculado del modelo táctico. | ✅ |
| III. Lenguaje ubicuo | `CargaHoraria` ya existe. `Proporcion` y `MetaDeProporcion` entran al glosario antes del código. | ✅ con T001 |
| IV. API primero | Los contratos antes del código. | ✅ |
| V. Límites | Entre procesos, solo `/interno/…`. | ✅ |
| VI. Las reglas son datos | La meta sale de los parámetros de RN-05 de la versión de normativa. | ✅ |
| VII. Inmutabilidad y trazabilidad | No cambia nada, así que no deja registro. | ✅ |
| VIII. Pruebas | Primero `Proporcion`, `CargaHoraria` y su igualdad con `HorasDelPeriodo`. | ✅ |
| IX. Seguridad y datos personales | Solo sumas y horas contratadas, ya visibles en la sugerencia. Sin contacto. | ✅ |
| X. Simplicidad | No se guarda ni se almacena en caché en el servidor. Guardar no cambia su respuesta. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Catálogo de errores | Se reutilizan los códigos que ya existen, con su estado. | ✅ |
| XIV. Validación de entradas | El periodo y el docente, con su patrón en el contrato. | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/033-totales-del-docente/
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
├── generacion/openapi.yaml     consultarCargaHoraria, CargaHoraria y HorasPorComponente
└── normativa/openapi.yaml      consultarProporcionDelPeriodo

backend/servicios/normativa/
└── …                           ruta interna y lectura de los parámetros de RN-05

backend/servicios/generacion/
├── dominio/                    CargaHoraria, Proporcion, MetaDeProporcion; HorasDelPeriodo comparte el conteo
├── aplicacion/                 ConsultarCargaHorariaServicio; ConsultarActividadesServicio cambia
├── puertos/                    ConsultarCargaHoraria, CargaDelDocente; DatosParaActividades.meta
└── adaptadores/                controlador y cliente interno

frontend/src/pantallas/
├── comun/CargaHoraria.tsx      el panel, con su consulta
├── actividades/                el panel y la columna de totales
└── oferta/                     el panel en la sugerencia

deploy/probar_totales.sh        punta a punta, con una suma aparte
```

**Structure Decision**: no hay módulos nuevos. El panel va en `comun` porque lo usan dos pantallas.

## Complexity Tracking

Sin violaciones de la constitución.
