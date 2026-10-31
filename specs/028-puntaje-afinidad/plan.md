# Implementation Plan: Calcular el puntaje de afinidad con su desglose

**Branch**: `028-puntaje-afinidad` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/028-puntaje-afinidad/spec.md` (issue #46, F06-H01)

## Summary

- **Consultas entre procesos (ADR-023 propuesto, research §1):** Generación pide los datos a `datos` por rutas `/interno/…`, con el token de proceso y el plazo de ADR-009. Es la primera vez, y deja el mecanismo para toda la Ola 2. No sirve reenviar el token del usuario: un coordinador no puede leer Normativa.
- **Tres consultas internas,** una por dueño: las asignaturas ofertadas del periodo (Académico), los pesos de su versión (Normativa) y la formación del docente (Docentes). Cada una devuelve solo lo necesario.
- **El núcleo en Generación:** `CalculadoraAfinidad`, función pura, y `PuntajeAfinidad` con su desglose.
- **La operación pública** `consultarAfinidadDelDocente`, con los permisos de leer un docente, y 5 códigos nuevos, entre ellos los 502 y 504 entre procesos.
- **SPA:** la sección «Afinidad por asignatura» en la ficha del docente.
- **Pruebas:** el dominio sin dobles y `deploy/probar_afinidad.sh`, con los dos procesos reales.

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

**Project Type**: monorepo web: un módulo de plataforma, 4 servicios, los contratos, la SPA y un script.

**Performance Goals**:
- La lista de un docente en la oferta de un periodo, en menos de 3 s (SC-003, con holgura).
- Con `datos` caído, el mensaje en menos de 10 s (SC-004, con holgura).

**Constraints**:
- solo rutas internas entre procesos (ADR-018);
- ninguna ficha en los logs (principio IX);
- ningún peso en el código (principio VI);
- 60 % de líneas por módulo, y la meta del núcleo en el dominio de Generación.

**Scale/Scope**:
- 1 operación pública y 3 internas;
- 1 cliente de plataforma;
- 5 códigos de error;
- 1 sección de pantalla;
- 1 script;
- ninguna migración.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La calculadora es dominio puro; los clientes HTTP, adaptadores de salida; los puertos de entrada son obligatorios en Generación (ADR-009). | ✅ |
| II. DDD según el subdominio | Generación es núcleo: servicio de dominio y objetos de valor, con su capa anticorrupción. Los dueños solo agregan una consulta. | ✅ |
| III. Lenguaje ubicuo | `CalculadoraAfinidad` y `PuntajeAfinidad`, del modelo táctico. Propone agregar «Puntaje de afinidad» al glosario antes del código (research §10). | ⏳ aprobación |
| IV. API primero | Los contratos, también las rutas internas, antes del código. | ✅ |
| V. Límites | Entre procesos, solo `/interno/…` con el token de proceso. Sin tipos compartidos. | ✅ con ADR-023 |
| VI. Las reglas son datos | Todos los pesos salen de la versión de normativa del periodo. | ✅ |
| VII. Inmutabilidad y trazabilidad | Solo lee; no guarda nada. | ✅ |
| VIII. Pruebas | Primero la calculadora, con los 8 escenarios y pesos que no son los de por defecto. | ✅ |
| IX. Seguridad y datos personales | Las rutas internas devuelven lo mínimo, solo a un token de proceso; el usuario necesita leer el docente. Sin ficha en los logs. | ✅ |
| X. Simplicidad | Sin réplicas ni colas; reusa el token y el cliente que ya existen. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Errores | 5 códigos con sus 8 columnas, en «tú». | ✅ |
| XIV. Validación de las entradas | El periodo y la identificación, con las reglas de sus dueños, en el contrato, el servidor y la pantalla, con pruebas válidas e inválidas. | ✅ |
| §6.4 Decisiones | ADR-023 y la entrada del glosario, que apruebas tú antes de implementar. | ⏳ aprobación |

**Resultado del gate antes de la fase 0:** pasa, con ADR-023 y el glosario pendientes de tu aprobación.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/028-puntaje-afinidad/
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
backend/plataforma/eventos/                       # ConsultasEntreProcesos
backend/plataforma/errores-nucleo/                # COMUN_PROCESO_CON_ERROR y _SIN_RESPUESTA
backend/servicios/generacion/src/main/java/ec/mdd/generacion/
├── dominio/{CalculadoraAfinidad, PuntajeAfinidad, AporteDeAfinidad, …}
├── aplicacion/ConsultarAfinidadServicio.java
├── puertos/{entrada/ConsultarAfinidad, salida/DatosParaAfinidad}
└── adaptadores/{entrada/AfinidadControlador, salida/DatosDesdeProcesoDatos}
backend/servicios/{academico,normativa,docentes}/ # una consulta interna en cada uno
contratos/{generacion,academico,normativa,docentes}/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/docentes/                  # «Afinidad por asignatura»
deploy/probar_afinidad.sh, .github/workflows/perfil-consolidado.yml
docs/decisiones.md                                # ADR-023
```

**Structure Decision**: el cálculo es del servicio 5 (ADR-010). Cada dueño publica solo lo que el cálculo necesita.

## Fases de implementación (guía para /speckit-tasks)

1. **Base:**
   - ADR-023 y el glosario, con tu aprobación;
   - los contratos y los 5 códigos;
   - `CalculadoraAfinidad`, probada sin dobles.
2. **Las consultas internas:** una en cada dueño, con su prueba de contrato y de seguridad.
3. **El cliente entre procesos:** `ConsultasEntreProcesos` y los 502 y 504.
4. **Generación:** el caso de uso, el adaptador y el controlador, con su contrato y sus permisos.
5. **SPA:** la sección de la ficha; Vitest, y Playwright con axe.
6. **Punta a punta y cierre:**
   - `probar_afinidad.sh`, con `datos` detenido;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación;
   - el PR de evidencia con `Closes #46`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Consultas internas con token de proceso | Generación está en otro proceso, y un coordinador no puede leer Normativa. | Reenviar el token del usuario: fallaría para el actor de la historia, salvo cambiando la matriz de permisos. |
| Tipos propios de Generación para los datos que recibe | Generación es el núcleo y otro proceso (principio V). | Compartir tipos con los dueños: acopla procesos que se despliegan aparte. |
