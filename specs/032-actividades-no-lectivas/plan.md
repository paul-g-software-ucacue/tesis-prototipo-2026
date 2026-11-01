# Implementation Plan: Asignar actividades no lectivas

**Branch**: `032-actividades-no-lectivas` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/032-actividades-no-lectivas/spec.md` (issue #49, F06-H04)

## Summary

- **El catálogo:** 47 actividades en Normativa, por versión, con lo que exige cada una como dato (D-79).
- **La regla:** `ActividadDelCatalogo`, una función pura del dominio de Generación que decide si una actividad se puede asignar. La usan el formulario y el guardado.
- **Asignar:** `asignarActividad` y `quitarActividad`, en el parcial de la carrera, con el filtro de F06-H05.
- **Las consultas:** dos rutas internas nuevas: el catálogo en Normativa, y los proyectos y cargos del docente en Docentes.
- **Las horas:** `HorasDelPeriodo` suma también las actividades.
- **SPA:** la pantalla «Actividades no lectivas», y el texto «Solo lectura» corregido.
- **Pruebas:** el dominio sin dobles y `deploy/probar_actividades.sh`, con una suma aparte de las horas.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: migración `V5` en `normativa` y `V4` en `generacion`.

**Testing**:
- **Dominio:** sin dobles.
- **Aplicación:** Mockito.
- **Repositorios y catálogo:** Testcontainers, en `integrationTest`.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con los dos procesos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 3 servicios, los contratos, la SPA y un script.

**Performance Goals**: guardar, en menos de 3 s; con un servicio caído, el mensaje en menos de 10 s (SC-006 y SC-007, con holgura).

**Constraints**:
- solo rutas internas entre procesos (ADR-023);
- la regla, en el servidor; la pantalla solo muestra lo que recibe;
- el filtro por carrera, en el servidor (D-64);
- 60 % de líneas por módulo (principio VIII).

**Scale/Scope**:
- 4 operaciones públicas y 2 internas nuevas;
- 2 migraciones, con 2 tablas;
- 47 filas de catálogo;
- 4 códigos de error;
- 1 pantalla y 1 script;
- 1 decisión (D-79).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La regla y las horas son dominio puro; los clientes y repositorios, adaptadores de salida. | ✅ |
| II. DDD según el subdominio | `ActividadNoLectiva` entra al agregado `VersionDistributivo` (MD-01). | ✅ |
| III. Lenguaje ubicuo | `ActividadNoLectiva`, `Componente` y «Catálogo de actividades» ya están en el glosario. No hay términos nuevos. | ✅ |
| IV. API primero | Los contratos antes del código. | ✅ |
| V. Límites | Entre procesos, solo `/interno/…`. Sin llaves foráneas a otros esquemas. | ✅ |
| VI. Las reglas son datos | El catálogo y lo que exige cada actividad están en la versión de normativa. | ✅ |
| VII. Inmutabilidad y trazabilidad | Cada cambio, en `generacion.registro_cambio`, con las horas anteriores. | ✅ |
| VIII. Pruebas | Primero `ActividadDelCatalogo` y las horas. Esta vez, en el orden que pide el principio. | ✅ |
| IX. Seguridad y datos personales | La ruta interna solo trae proyectos y cargos, sin contacto. | ✅ |
| X. Simplicidad | Sin bloqueo optimista: llega con F07-H02. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Errores | 4 códigos con sus 8 columnas, en «tú». | ✅ |
| XIV. Validación de las entradas | Horas, actividad, proyecto y docente, en el contrato, el servidor y la pantalla. | ✅ |
| §6.4 Decisiones | D-79, aprobada el 2026-11-01. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-79 quedó aprobada por el autor el 2026-11-01.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/032-actividades-no-lectivas/
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
│   ├── dominio/{Componente, ActividadDelCatalogo, ProyectosYCargosDelDocente, ActividadNoLectiva, SumaPorComponente}
│   ├── aplicacion/{AsignarActividadServicio, ConsultarActividadesServicio}
│   ├── puertos/{entrada/…, salida/…}
│   └── adaptadores/{entrada/ActividadesControlador, salida/…}
└── resources/db/migration/generacion/V4__actividades_no_lectivas.sql
backend/servicios/normativa/                      # V5, el catálogo y su ruta interna
backend/servicios/docentes/                       # los proyectos y cargos del docente, por ruta interna
contratos/{generacion,normativa,docentes}/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/actividades/, frontend/src/pantallas/oferta/Oferta.tsx
deploy/probar_actividades.sh, .github/workflows/perfil-consolidado.yml
docs/decisiones.md                                # D-79
```

**Structure Decision**: las actividades son del servicio 5 (MD-01); el catálogo, de Normativa (modelo de dominio, `ReglasDelPeriodo`).

## Fases de implementación (guía para /speckit-tasks)

1. **Base:** D-79, los contratos, los códigos y las dos migraciones.
2. **El dominio:** `ActividadDelCatalogo`, las horas y la suma por componente, sin dobles.
3. **Las rutas internas:** Normativa y Docentes, y el cliente.
4. **Asignar y quitar:** el caso de uso, el repositorio y el controlador.
5. **Ver y las horas reales.**
6. **SPA:** la pantalla, el formulario y el texto de la oferta.
7. **Punta a punta y cierre:**
   - `probar_actividades.sh`;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación;
   - el PR de evidencia con `Closes #49`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| El catálogo por versión | Lo pide el modelo de dominio y RF-21, y prepara habilitar por periodo. | Una tabla sin versión: habría que migrarla cuando el catálogo cambie. |
| Un índice único con `coalesce` | El proyecto es opcional, y un valor vacío no choca en un índice común. | Un proyecto vacío como texto: esconde la ausencia del dato. |
