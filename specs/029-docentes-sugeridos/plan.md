# Implementation Plan: Ver los docentes sugeridos para una asignatura

**Branch**: `029-docentes-sugeridos` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/029-docentes-sugeridos/spec.md` (issue #47, F06-H02)

## Summary

- **Reusa lo de F06-H01:** `CalculadoraAfinidad`, las consultas entre procesos de ADR-023 y sus errores 502 y 504. No hace falta un ADR nuevo.
- **Lo que falta de los dueños:** una consulta interna de una asignatura-paralelo en Académico, otra de los docentes activos en Docentes, y el mínimo de afinidad en la consulta de pesos de Normativa.
- **El núcleo en Generación:** `Sugerencia`, que arma y ordena los candidatos con una función pura, y `Candidato`, con `horasDisponibles()`.
- **Las horas asignadas** salen de un puerto, `HorasAsignadas`. Hoy lo cumple un adaptador que responde 0; F06-H03 lo reemplaza sin tocar lo demás (FR-009).
- **La operación pública** `consultarSugerencia`, con los permisos de leer un docente y 2 códigos nuevos.
- **SPA:** la acción «Sugerencia» en «Oferta», con un diálogo que reusa el desglose de la ficha.
- **Pruebas:** el dominio sin dobles y `deploy/probar_sugerencia.sh`, con los dos procesos reales y una lista hecha aparte.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: sin migraciones. Nada se guarda.

**Testing**:
- **Dominio:** sin dobles.
- **Aplicación:** Mockito.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **Rutas internas:** contrato y seguridad en cada dueño.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** el script, con los dos procesos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 4 servicios, los contratos, la SPA y un script.

**Performance Goals**:
- La sugerencia de un paralelo con 47 docentes de prueba (46 activos), en menos de 3 s (SC-003, con holgura).
- Con `datos` caído, el mensaje en menos de 10 s (SC-004, con holgura).

**Constraints**:
- solo rutas internas entre procesos (ADR-018, ADR-023);
- ninguna ficha ni dato de contacto en los logs ni entre procesos (principio IX);
- ningún mínimo en el código (principio VI);
- 60 % de líneas por módulo, también en el dominio de Generación (principio VIII).

**Scale/Scope**:
- 1 operación pública, 2 internas nuevas y 1 ampliada;
- 2 códigos de error;
- 1 diálogo de pantalla;
- 1 script;
- 1 decisión (D-75) y 2 cambios en el glosario;
- ninguna migración.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | `Sugerencia` y `Candidato` son dominio puro; los clientes HTTP y el adaptador de las horas, adaptadores de salida. | ✅ |
| II. DDD según el subdominio | Generación es núcleo: objetos de valor con su regla adentro. Los dueños solo agregan una consulta. | ✅ |
| III. Lenguaje ubicuo | `Sugerencia` y `horasDisponibles()` ya son del modelo táctico. «Horas disponibles» se precisó y «Candidato» entró al glosario antes del código, aprobados el 2026-10-31 (research §9). | ✅ |
| IV. API primero | Los contratos, también las rutas internas, antes del código. | ✅ |
| V. Límites | Entre procesos, solo `/interno/…` con el token de proceso. Sin tipos compartidos. | ✅ |
| VI. Las reglas son datos | El mínimo y los pesos salen de la versión de normativa del periodo. | ✅ |
| VII. Inmutabilidad y trazabilidad | Solo lee; no guarda nada. | ✅ |
| VIII. Pruebas | Primero `Sugerencia`, con el orden, el mínimo, el tronco común y las horas simuladas. | ✅ |
| IX. Seguridad y datos personales | La ruta interna de docentes trae lo mínimo: sin correo, teléfono ni contrato. La identificación viaja, porque es la clave del docente, pero la pantalla no la muestra. | ✅ |
| X. Simplicidad | Sin réplicas ni colas. Tres consultas por sugerencia, la misma calculadora y un adaptador de una línea para las horas. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Errores | 2 códigos con sus 8 columnas, en «tú». | ✅ |
| XIV. Validación de las entradas | El periodo, la asignatura y el paralelo, con las reglas de Académico, en el contrato y el servidor, con pruebas válidas e inválidas. La pantalla no tiene campos: los códigos salen de la fila. | ✅ |
| §6.4 Decisiones | D-75: las horas disponibles salen de las horas contratadas, aprobada el 2026-10-31. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-75 y los dos cambios del glosario quedaron aprobados por el autor el 2026-10-31.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/029-docentes-sugeridos/
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
backend/servicios/generacion/src/main/java/ec/mdd/generacion/
├── dominio/{Sugerencia, Candidato, AsignaturaParalelo, DocenteActivo, HorasDelDocente, Dedicacion}
├── aplicacion/ConsultarSugerenciaServicio.java
├── puertos/{entrada/ConsultarSugerencia, salida/DatosParaSugerencia, salida/HorasAsignadas}
└── adaptadores/{entrada/SugerenciaControlador, salida/DatosDesdeProcesoDatos, salida/SinAsignacionesTodavia}
backend/servicios/academico/                      # la asignatura-paralelo, por ruta interna
backend/servicios/docentes/                       # los docentes activos, por ruta interna
backend/servicios/normativa/                      # el mínimo, en la consulta de pesos
contratos/{generacion,academico,normativa,docentes}/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/oferta/SugerenciaDelParalelo.tsx
frontend/src/pantallas/comun/{DesgloseDeAfinidad.tsx, numeros.ts}  # salen de AfinidadDelDocente.tsx
deploy/probar_sugerencia.sh, .github/workflows/perfil-consolidado.yml
docs/decisiones.md                                # D-75
```

**Structure Decision**: la sugerencia es del servicio 5 (ADR-010). Cada dueño publica solo lo que hace falta.

## Fases de implementación (guía para /speckit-tasks)

1. **Base:**
   - D-75 en `docs/decisiones.md`;
   - los contratos y los 2 códigos;
   - `Sugerencia` y `Candidato`, probados sin dobles.
2. **Los dueños:** la asignatura-paralelo, los docentes activos y el mínimo, con su prueba de contrato y de seguridad.
3. **Generación:** el adaptador de datos, `SinAsignacionesTodavia`, el caso de uso y el controlador, con su contrato y sus permisos.
4. **SPA:** sacar el desglose a un componente común, la acción y el diálogo; Vitest, y Playwright con axe.
5. **Punta a punta y cierre:**
   - `probar_sugerencia.sh`, con `datos` detenido;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación;
   - el PR de evidencia con `Closes #47`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Puerto `HorasAsignadas` que hoy responde 0 | La pantalla ya debe mostrar las horas, y F06-H03 debe poner las reales sin cambiar la sugerencia (FR-009). | Escribir 0 en el caso de uso: F06-H03 tendría que cambiar la regla, y el orden por horas no se podría probar. |
| Ruta interna con todos los docentes activos | La sugerencia necesita la formación y las horas de cada uno. | Pedir la formación docente por docente: 47 llamadas entre procesos por sugerencia. |
