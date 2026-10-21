# Implementation Plan: Designar al Responsable de consolidación del periodo

**Branch**: `016-responsable-consolidacion` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/016-responsable-consolidacion/spec.md` (issue #45, F05-H03)

## Summary

- **La designación vive en Roles** (proceso `identidad`): un Responsable por periodo, que solo puede ser un usuario con el rol Docente y nunca quien designa. La designa el Vicerrector con `DISTRIBUTIVO:ESCRITURA` y queda en el registro.
- **Los permisos:** el usuario recibe el rol `RESPONSABLE_CONSOLIDACION` en su token mientras tenga una designación en un periodo no cerrado (D-62); el filtro por periodo lo aplican F09 y F10 (D-64).
- **Los periodos llegan a Roles por eventos** (ADR-018, aprobado por el autor): Académico (proceso `datos`) publica `academico.PeriodoActualizado` al registrar y al cambiar de estado; Roles guarda una copia y valida en local. El proceso `datos` empieza a publicar eventos, con una clave de proceso nueva.
- **SPA:** la columna y el diálogo del Responsable en «Periodos».
- **Punta a punta:** pasos nuevos en `probar_periodos.sh`, y `preparar.sh` agrega la clave nueva al equipo.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: sin dependencias externas nuevas. `apps/datos` gana `plataforma:eventos`, como `apps/distributivo`.

**Storage**: esquema `roles`, migración nueva con `periodo_conocido`, `designacion_responsable`, `evento_recibido` y la acción `DESIGNAR_RESPONSABLE`; `eventos_datos` (ya existe) gana el registro de publicaciones con `flywayEventos`.

**Testing**: JUnit y AssertJ en el dominio; Mockito en la aplicación; Testcontainers en los adaptadores y en el proceso `datos`; swagger-request-validator en el contrato; Vitest y Playwright con axe; `probar_periodos.sh` y `probar_equipo.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Roles (`identidad`), Académico y el proceso `datos`, el contrato, la SPA, los scripts y el equipo.

**Performance Goals**:
- designar responde por debajo de 200 ms;
- la copia de un periodo llega a Roles en segundos (entrega inmediata; reenvío cada 30 s si falla);
- SC-001: designar en menos de 1 minuto.

**Constraints**:
- el inicio de sesión no depende del proceso `datos` (los permisos salen de la copia local);
- cada transacción queda en su servicio; lo que cruza procesos va por eventos (principio V);
- ningún secreto por el chat: la clave nueva la genera `preparar.sh` en el equipo;
- 60 % de líneas por módulo; sin JVM nueva.

**Scale/Scope**: 3 tablas y 1 acción en Roles; 4 operaciones y 1 código de error; 1 tipo de evento; 1 columna y 1 diálogo en la SPA.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas en `Designacion` y `EstadoConocido`; la aplicación orquesta; los adaptadores traducen el contrato, la base y los eventos. | ✅ |
| II. DDD según el subdominio | Roles es genérico: un objeto de valor con sus reglas, sin agregado ni eventos propios. | ✅ |
| III. Lenguaje ubicuo | Responsable de consolidación y Periodo académico, del glosario; «designar», de ADR-003. | ✅ |
| IV. API primero | `contratos/roles/openapi.yaml`, el código nuevo y el evento antes del código. | ✅ |
| V. Límites | Roles no lee las tablas de Académico: recibe sus eventos (ADR-018). Sin transacciones entre servicios. | ✅ |
| VI. Las reglas son datos | Los permisos del rol salen de la matriz D-16, no del código. | ✅ |
| VII. Inmutabilidad y trazabilidad | Cada designación en `registro_cambio` con el anterior y el nuevo; un periodo cerrado no admite designaciones. | ✅ |
| VIII. Pruebas | Primero las pruebas del dominio, sin dobles; el receptor con PostgreSQL real. | ✅ |
| IX. Seguridad | `DISTRIBUTIVO:ESCRITURA` y `LECTURA` en el servidor; el receptor solo con token de proceso; la clave nueva fuera del repositorio. | ✅ |
| X. Simplicidad | Un solo tipo de evento; sin republicar los periodos viejos; sin cambiar el formato del token. | ✅ |
| XI y XII | Un puerto de salida en Académico para los avisos, porque cruza procesos. JavaDoc en español. | ✅ |
| XIII. Errores | `ROLES_PERIODO_CERRADO` en el catálogo; el 400 con su detalle. | ✅ |
| §6.4 Decisiones | Los eventos entre `datos` e `identidad` aplican ADR-018 sin cambiarlo; la elección la aprobó el autor el 2026-10-21. No hace falta una decisión nueva. | ✅ |

**Resultado del gate antes de la fase 0:** pasa.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado. La especificación se ajusta en FR-004: Roles no puede comprobar «activo» sin un ciclo con Usuarios (research §2).

## Project Structure

### Documentation (this feature)

```text
specs/016-responsable-consolidacion/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── roles.md             # operaciones, error y evento
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/roles/openapi.yaml                    # 4 operaciones
contratos/comun/errores.md                      # ROLES_PERIODO_CERRADO
backend/servicios/roles/src/main/java/ec/mdd/roles/
├── dominio/                                    # EstadoConocido, PeriodoConocido, Designacion, excepciones
├── puertos/{entrada,salida}/                   # designar, consultar, docentes, periodo conocido
├── aplicacion/                                 # servicios; ConsultarAutorizacionServicio con la designación
├── adaptadores/entrada/                        # RolesControlador (o uno nuevo), EventosRolesControlador
└── adaptadores/salida/                         # repositorios JDBC
backend/servicios/roles/src/main/resources/db/migration/roles/V6__responsable_de_consolidacion.sql
backend/servicios/academico/…                   # AvisosDelPeriodo y su adaptador; los dos servicios lo llaman
backend/apps/datos/                             # plataforma:eventos y su configuración
backend/apps/identidad/src/main/resources/application.yaml   # la clave del proceso datos
.env.ejemplo, compose.yaml, deploy/equipo/compose.demo.yml, deploy/equipo/preparar.sh
.github/workflows/perfil-consolidado.yml        # la clave de prueba
frontend/src/pantallas/periodos/                # columna y diálogo del Responsable
deploy/probar_periodos.sh, deploy/equipo/probar_equipo.sh
```

**Structure Decision**: la designación es de Identidad y acceso (Roles); Académico solo avisa de sus periodos.

## Fases de implementación (guía para /speckit-tasks)

1. **Contrato y catálogo:** las 4 operaciones, `ROLES_PERIODO_CERRADO` y el evento.
2. **Dominio de Roles,** con las pruebas primero: `EstadoConocido`, `PeriodoConocido`, `Designacion`.
3. **Persistencia de Roles:** la migración y los repositorios, con Testcontainers.
4. **Eventos:** Académico publica `PeriodoActualizado`; `datos` gana `plataforma:eventos`; Roles recibe y guarda la copia, idempotente.
5. **Designar y autorizar:** los servicios, `ConsultarAutorizacion` con la designación, el controlador y las pruebas de contrato y de permisos.
6. **Entornos:** la clave del proceso `datos` en local, en la CI y en el equipo (`preparar.sh` agrega la que falte).
7. **SPA:** la columna y el diálogo en «Periodos».
8. **Punta a punta:** los pasos nuevos en `probar_periodos.sh`; `probar_equipo.sh`.
9. **Cierre:** evidencia, PR, `preparar.sh` en el equipo **antes** de fusionar, fusión con la aprobación del autor, despliegue y comprobación con `vicerrectorado.demo` y `docente.demo`, y el PR de evidencia con `Closes #45`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Eventos de `datos` a `identidad` y la copia de los periodos en Roles | Roles necesita el estado del periodo para validar y para el token, sin que el inicio de sesión dependa de `datos` | Llamar a Académico al designar: Roles no se enteraría del cierre (aprobado por el autor, research §1). |
| Clave de proceso nueva | El receptor de Roles solo acepta un token de proceso (D-49) | Un receptor sin autenticación: cualquier contenedor de la red podría cambiar los periodos de Roles. |
