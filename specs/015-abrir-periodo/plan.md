# Implementation Plan: Abrir el periodo académico con su versión de normativa

**Branch**: `015-abrir-periodo` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-abrir-periodo/spec.md` (issue #32, F02-H01)

## Summary

Académico gana su primer dato de negocio, el periodo académico:
- **Periodo:** código, nombre, fechas y estado. Se registra a mano y empieza en Planificación. El dominio guarda tres reglas: el orden de los estados, que un periodo cerrado no cambia y que salir de Planificación exige versión.
- **Abrir:** Académico pide a Normativa, por su interfaz publicada dentro del proceso `datos`, que asocie la versión al periodo. Normativa sigue siendo la única que guarda la versión (`AsignacionNormativa`), sin una transacción entre los dos servicios (research §3).
- **Contrato nuevo** `contratos/academico/openapi.yaml`: seis operaciones y cinco códigos de error `ACADEMICO_*`.
- **Permisos:** se leen con `DISTRIBUTIVO:LECTURA` y se escriben con `DISTRIBUTIVO:ESCRITURA`, sin cambiar la matriz. Es **D-69 (aceptada)**, que también quita el diálogo «Asociar a un periodo» de «Normativa».
- **SPA:** la pantalla «Periodos».
- **Punta a punta:** `deploy/probar_periodos.sh`, nuevo.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: no hay dependencias externas nuevas. Académico gana las que ya usa Normativa: `spring-boot-starter-jdbc`, `jackson-databind` para el `jsonb`, `spring-security-core` y el plugin `mdd.contrato`; y `project(':servicios:normativa')`, del que solo usa la interfaz `api`.

**Storage**: esquema `academico`, `V2__periodos_academicos.sql`: `periodo_academico` y `registro_cambio` ([data-model.md](data-model.md)).

**Testing**:
- **Dominio:** JUnit y AssertJ, sin dobles.
- **Aplicación:** Mockito.
- **Adaptadores:** Testcontainers, con la Normativa real para abrir el periodo.
- **Contrato:** swagger-request-validator.
- **Arquitectura:** `ModulosTest` del proceso `datos` y ArchUnit.
- **SPA:** Vitest y Playwright con axe.
- **Punta a punta:** `probar_periodos.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Académico (proceso `datos`), el contrato, la SPA y el script de punta a punta.

**Performance Goals**:
- listar y abrir por debajo de 200 ms con decenas de periodos (una consulta a Normativa por periodo, en el mismo proceso);
- la pantalla permite abrir un periodo y pasarlo a En aprobación en menos de 2 minutos (SC-001).

**Constraints**:
- cada transacción queda dentro de un servicio: abrir no envuelve la llamada a Normativa (principio V, research §3);
- Académico no lee las tablas de Normativa ni sus clases internas, solo `puertos.entrada` (principio V);
- un periodo cerrado no cambia y toda acción deja registro (principio VII);
- 60 % de líneas por módulo; sin memoria nueva (no hay JVM nueva).

**Scale/Scope**:
- 2 tablas;
- 6 operaciones y 5 códigos de error;
- 1 pantalla nueva y 1 diálogo menos en «Normativa»;
- 1 script de punta a punta nuevo, con 9 pasos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas en `PeriodoAcademico` y `EstadoPeriodo`; la aplicación orquesta; los adaptadores traducen el contrato, la base y la llamada a Normativa. | ✅ |
| II. DDD según el subdominio | Académico es de soporte: un `record` y un `enum` con sus reglas, sin agregado elaborado ni eventos (research §1). | ✅ |
| III. Lenguaje ubicuo | Periodo académico y Estado del periodo, del glosario. La frase sobre «abrir» y el orden de los estados se aprobó y aplicó en el glosario antes del código (research §10). | ✅ |
| IV. API primero | `contratos/academico/openapi.yaml` y los códigos de `errores.md` antes del código. | ✅ |
| V. Límites | Académico llama a Normativa por `puertos.entrada` con un adaptador de salida, como Usuarios a Roles; sin tablas ajenas; una transacción por servicio (research §3 y §4). `ModulosTest` lo comprueba. | ✅ |
| VI. Las reglas son datos | El periodo no tiene cifras de la política; su versión de normativa las trae. | ✅ |
| VII. Inmutabilidad y trazabilidad | Un periodo cerrado no cambia; una versión con periodo tampoco (Normativa). Cada acción queda en `registro_cambio`. | ✅ |
| VIII. Pruebas | Primero las pruebas del dominio, sin dobles; la llamada a Normativa, con la Normativa real. | ✅ |
| IX. Seguridad | `DISTRIBUTIVO:ESCRITURA` y `LECTURA` validados en el servidor con `x-mdd-permiso`. Datos ficticios. | ✅ |
| X. Simplicidad | Sin eventos, sin copia de la versión, sin funcionalidad nueva en la matriz. | ✅ |
| XI y XII | Un puerto de salida hacia Normativa, porque marca un límite entre servicios. JavaDoc en español en el dominio y los puertos. | ✅ |
| XIII. Errores | Cinco códigos `ACADEMICO_*` en el catálogo; los errores de Normativa se traducen, no se reenvían. | ✅ |
| §6.4 Decisiones | D-69 (permisos de los periodos y abrir solo desde Académico), aceptada por el autor el 2026-10-21. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-69 y la frase del glosario quedaron aprobadas el 2026-10-21.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/015-abrir-periodo/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── academico.md         # operaciones, esquemas y errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/academico/openapi.yaml               # nuevo
contratos/comun/errores.md                     # los 5 códigos ACADEMICO_*
backend/servicios/academico/
├── build.gradle                               # jdbc, jackson, security, mdd.contrato, :servicios:normativa
└── src/main/java/ec/mdd/academico/
    ├── dominio/                               # EstadoPeriodo, PeriodoAcademico, CambioDeValor, excepciones
    ├── puertos/entrada/                       # ConsultarPeriodos, RegistrarPeriodo, CorregirPeriodo, AbrirPeriodo, CambiarEstadoPeriodo
    ├── puertos/salida/                        # RepositorioPeriodos, RegistroDeCambios, NormativaDelPeriodo, Transaccion
    ├── aplicacion/                            # un servicio por caso de uso
    ├── adaptadores/entrada/                   # AcademicoControlador, ErroresAcademico
    ├── adaptadores/salida/                    # RepositorioPeriodosJdbc, RegistroDeCambiosJdbc, NormativaDesdePuertos, TransaccionSpring
    └── infraestructura/                       # ConfiguracionAcademico
backend/servicios/academico/src/main/resources/db/migration/academico/V2__periodos_academicos.sql
frontend/package.json                          # generar:api con academico.d.ts
frontend/src/pantallas/periodos/               # Periodos, diálogos y consultas
frontend/src/navegacion/Marco.tsx, frontend/src/rutas.tsx
frontend/src/pantallas/normativa/              # sin el diálogo «Asociar a un periodo»
frontend/e2e/periodos.spec.ts
deploy/probar_periodos.sh                      # nuevo
.github/workflows/perfil-consolidado.yml       # el paso del script nuevo
docs/decisiones.md                             # D-69
```

**Structure Decision**: el periodo vive en Académico, y su versión en Normativa. Los dos están en el proceso `datos`, y se hablan por la interfaz publicada de Normativa.

## Fases de implementación (guía para /speckit-tasks)

1. **D-69 y el glosario:** aprobados por el autor el 2026-10-21 y ya aplicados: D-69 en `docs/decisiones.md` y la frase en el glosario §7.3, con la copia `modelo_dominio.md.antes-F02-H01`.
2. **Contrato:** `contratos/academico/openapi.yaml`, los códigos en `errores.md`, `mdd.contrato` en Académico y `generar:api` en la SPA.
3. **Dominio,** con sus pruebas primero: `EstadoPeriodo`, `PeriodoAcademico` y sus excepciones.
4. **Aplicación:** los cinco casos de uso con Mockito; abrir sin transacción alrededor de Normativa.
5. **Persistencia y Normativa:** V2, los repositorios, el registro y `NormativaDesdePuertos`, con Testcontainers y `ModulosTest`.
6. **API:** `AcademicoControlador` y `ErroresAcademico`; las pruebas de contrato y de permisos.
7. **SPA:** «Periodos», su entrada en el menú, y «Normativa» sin el diálogo de asociar.
8. **Punta a punta:** `probar_periodos.sh`, en el perfil local y en `perfil-consolidado`.
9. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue y comprobación con `vicerrectorado.demo`, y el PR de evidencia con `Closes #32`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Puerto de salida `NormativaDelPeriodo` y su adaptador | Es un límite entre servicios (principio XI); si Normativa se separa, solo cambia el adaptador | Llamar a los puertos de Normativa desde la aplicación: el núcleo de Académico dependería de las clases de otro servicio. |
| Abrir en tres pasos sin transacción común | El principio V no permite una transacción entre dos servicios | Una sola transacción: uniría los dos servicios, y se rompería al separarlos. |
| Consultar la versión a Normativa en cada lectura | Una sola fuente de verdad para la versión del periodo | Copiar la versión en Académico: dos fuentes que pueden diferir. |
