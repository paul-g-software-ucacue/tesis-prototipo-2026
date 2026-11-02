# Implementation Plan: Impedir una asignación que viola una regla dura

**Branch**: `034-reglas-duras` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/034-reglas-duras/spec.md` (issue #51, F07-H02)

## Summary

- **La regla:** `MotorReglas`, una función pura del dominio que compara la carga del docente antes y después de una asignación con `ReglasDelPeriodo`. Viola solo si queda sobre el límite y además sube.
- **Las cifras:** una ruta interna nueva en Normativa, con el tope de horas de RN-02 y el tope de clase de la tabla de carga.
- **Quién tiene el tope:** una ruta interna nueva en Docentes, para no pedir uno por uno a los candidatos.
- **A la vez:** guardar bloquea la versión, la vuelve a leer y valida dentro de la transacción (D-83).
- **La sugerencia:** cada candidato dice si se puede proponer y por qué no, con el mismo motor.
- **SPA:** sin botón para quien no se puede proponer, el motivo en el formulario y sin el aviso de D-78.
- **Pruebas:** el motor sin dobles, una prueba de concurrencia contra PostgreSQL y `deploy/probar_reglas_duras.sh`, con 20 intentos a la vez.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: sin migraciones. Se bloquea la fila de `generacion.version_distributivo` que ya existe.

**Testing**:
- **Dominio:** sin dobles, con 80 % o más en el motor (RNF-MAN-02).
- **Aplicación:** Mockito.
- **Concurrencia y repositorio:** Testcontainers, en `integrationTest`.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con los dos procesos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 3 servicios, los contratos, la SPA y un script.

**Performance Goals**: guardar en menos de 3 s; con un servicio caído, el mensaje en menos de 10 s (SC-005 y SC-006, con holgura).

**Constraints**:
- solo rutas internas entre procesos (ADR-023);
- ninguna llamada a otro proceso con la transacción abierta;
- la regla, en el servidor; la pantalla solo muestra lo que recibe;
- 60 % de líneas por módulo y 80 % en el motor (principio VIII).

**Scale/Scope**:
- 2 operaciones internas nuevas y 3 respuestas que cambian;
- ninguna migración;
- 2 códigos de error nuevos;
- 1 script;
- 1 decisión (D-83) y 1 término en el glosario.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El motor es dominio puro; el bloqueo, un adaptador de salida. | ✅ |
| II. DDD según el subdominio | `MotorReglas`, `ReglasDelPeriodo` e `Inconsistencia` son del modelo táctico; la versión protege el agregado (MD-01). | ✅ con D-83 |
| III. Lenguaje ubicuo | «Límite del docente» entra al glosario antes del código. | ✅ con T001 |
| IV. API primero | Los contratos antes del código. | ✅ |
| V. Límites | Entre procesos, solo `/interno/…`. | ✅ |
| VI. Las reglas son datos | Los dos topes salen de la versión de normativa. Qué tipos de proyecto activan el tope es lógica de la comprobación, como dice `Regla`. | ✅ |
| VII. Inmutabilidad y trazabilidad | Lo guardado sigue en el registro. Un rechazo no cambia nada. | ✅ |
| VIII. Pruebas | Primero el motor, sin dobles, y la prueba de concurrencia antes del bloqueo. | ✅ |
| IX. Seguridad y datos personales | La ruta interna solo trae identificaciones y tipos de proyecto. | ✅ |
| X. Simplicidad | Un bloqueo de fila, sin columna de versión ni reintentos. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Catálogo de errores | 2 códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación de entradas | Sin entradas nuevas de usuario. | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/034-reglas-duras/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── generacion.md
│   ├── normativa.md
│   └── docentes.md
└── tasks.md
```

### Source Code (repository root)

```text
contratos/
├── generacion/openapi.yaml     Candidato.motivo, ActividadGuardada sin horasSuficientes
├── normativa/openapi.yaml      consultarReglasDelPeriodo
├── docentes/openapi.yaml       consultarDocentesConProyecto
└── comun/errores.md            los 2 códigos

backend/servicios/normativa/    ruta interna de las cifras
backend/servicios/docentes/     ruta interna de los proyectos asignables por docente

backend/servicios/generacion/
├── dominio/                    MotorReglas, ReglasDelPeriodo, Inconsistencia, límite en Candidato
├── aplicacion/                 ProponerDocenteServicio, AsignarActividadServicio, sugerencia, carga
├── puertos/                    las excepciones y los datos nuevos; bloquear la versión
└── adaptadores/                errores, clientes internos y RepositorioDistributivosJdbc

frontend/src/pantallas/
├── oferta/                     sin «Proponer» con motivo
└── actividades/                sin el aviso de horas

deploy/probar_reglas_duras.sh   punta a punta, con 20 intentos a la vez
```

**Structure Decision**: no hay módulos nuevos. El motor vive en el dominio de Generación, junto a `CargaHoraria`.

## Complexity Tracking

Sin violaciones de la constitución. D-83 precisa cómo se cumple MD-01.
