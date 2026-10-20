# Implementation Plan: Ajustar pesos y mínimo de afinidad y la severidad de las reglas

**Branch**: `014-afinidad-severidad` | **Date**: 2026-10-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-afinidad-severidad/spec.md` (issue #42, F04-H03)

## Summary

Sobre las versiones de normativa de F04-H02:
- **Afinidad como reglas:**
  - los pesos de afinidad son los parámetros de una regla RN-24, que no tiene severidad;
  - el mínimo es el parámetro de RN-19, que es blanda;
  - el agregado arma con ellos `PesosAfinidad` y `MinimoAfinidad` y comprueba que el campo amplio no pese más que el específico.
- **Las reglas del motor que faltan:** RN-12, RN-16, RN-17, RN-18, RN-19, RN-22, RN-25 y RF-24 se agregan con la severidad por defecto de la tabla 5.2. Una regla puede no tener parámetros, y el identificador admite `RF-xx`.
- **Severidad editable:** `ReglaEditada` gana `severidad`, opcional, por la misma `editarVersion` (FR-010 de F04-H02). Una versión en uso sigue sin cambiar (409).
- **Versiones existentes:** `V4` completa cada versión con las reglas y los valores por defecto, con su registro. Es **D-68 (aceptada)**.
- **SPA:** «Normativa» gana la sección «Afinidad» y el selector de severidad en la edición.

Sin operaciones ni códigos de error nuevos. Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: no hay dependencias nuevas.

**Storage**: esquema `normativa`, `V4__afinidad_y_reglas_del_motor.sql`:
- el `check` del identificador de `regla`;
- las reglas y los parámetros por defecto en cada versión;
- un `VERSION_EDITADA` de `sistema` por versión.

**Testing**:
- **Dominio:** JUnit y AssertJ, sin dobles.
- **Aplicación:** Mockito, solo si cambia un servicio. `EditarVersionServicio` no cambia: la severidad viaja en `CambiosDeVersion`.
- **Adaptadores:** Testcontainers: la migración sobre las versiones existentes y la vuelta atrás.
- **Contrato:** swagger-request-validator.
- **SPA:** Vitest y Playwright con axe.
- **Punta a punta:** `probar_normativa.sh`, con dos pasos nuevos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Normativa (proceso `datos`), el contrato, la SPA y el script de punta a punta.

**Performance Goals**:
- editar sigue por debajo de 200 ms, con 16 reglas en lugar de 7;
- la pantalla permite cambiar un peso y una severidad en menos de 2 minutos (SC-001).

**Constraints**:
- una versión con un periodo no cambia (principio VII), con la excepción acotada de D-68 para la migración;
- las cifras y las severidades son datos, nunca código (principio VI);
- la carga 7 por archivo sigue sirviendo sin severidades (research §3);
- la versión anterior del equipo sigue arrancando y leyendo las versiones (D-55, research §6);
- 60 % de líneas por módulo.

**Scale/Scope**:
- 9 reglas y 8 parámetros nuevos por versión (16 reglas en total);
- 2 esquemas del contrato;
- 0 operaciones y 0 códigos de error nuevos;
- 2 pasos nuevos de punta a punta.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las invariantes, en `Regla`, `PesosAfinidad` y el agregado. La aplicación no cambia; los adaptadores traducen la severidad del contrato y leen las reglas sin parámetros. | ✅ |
| II. DDD según el subdominio | `PesosAfinidad` y `MinimoAfinidad`, objetos de valor del modelo de dominio §5.1, dentro del agregado. | ✅ |
| III. Lenguaje ubicuo | Pesos de afinidad, Mínimo de afinidad, Regla y Severidad, del glosario. Los dos ajustes al glosario se aprobaron y aplicaron antes del código (research §10). | ✅ |
| IV. API primero | Los dos esquemas cambian en `contratos/normativa/openapi.yaml` antes del código. | ✅ |
| V. Límites | Distributivo leerá las reglas por la API de Normativa (F07), sin leer sus tablas. | ✅ |
| VI. Las reglas son datos | Los pesos, el mínimo y cada severidad están en la versión. | ✅ |
| VII. Inmutabilidad y trazabilidad | Una versión en uso no cambia por la API. La migración completa versiones con su registro (D-68). | ✅ |
| VIII. Pruebas | Primero las pruebas. Las invariantes, sin dobles; la migración, con PostgreSQL real. | ✅ |
| IX. Seguridad | La escritura sigue exigiendo `PARAMETROS:ESCRITURA`, validada en el servidor. | ✅ |
| X. Simplicidad | La afinidad usa los parámetros que ya existen; ni tablas, ni operaciones, ni códigos nuevos. `ReglasDelPeriodo` espera a su consumidor (F07). | ✅ |
| XI y XII | JavaDoc en español en `Regla`, los objetos de valor nuevos y el agregado. | ✅ |
| XIII. Errores | Los 400 con su detalle y el 409 de F04-H02. | ✅ |
| §6.4 Decisiones | D-68, aceptada por el autor el 2026-10-20. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-68 y los dos ajustes al glosario quedaron aprobados el 2026-10-20.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/014-afinidad-severidad/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── normativa.md         # los esquemas que cambian y los errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/normativa/openapi.yaml
backend/servicios/normativa/src/main/java/ec/mdd/normativa/
├── dominio/                 # Regla, PesosAfinidad, MinimoAfinidad, VersionNormativa, CambiosDeVersion
├── adaptadores/entrada/     # NormativaControlador: la severidad de ReglaEditada
└── adaptadores/salida/      # RepositorioVersionesJdbc: reglas sin parámetros y su orden
backend/servicios/normativa/src/main/resources/db/migration/normativa/V4__afinidad_y_reglas_del_motor.sql
frontend/src/pantallas/normativa/  # reglas.ts, Normativa (Afinidad), EdicionDeVersion (Severidad)
deploy/probar_normativa.sh         # pasos de afinidad y de severidad
docs/decisiones.md                 # D-68
```

**Structure Decision**: todo vive en Normativa. Distributivo solo lo leerá en F06 y F07.

## Fases de implementación (guía para /speckit-tasks)

1. **D-68 y el glosario:** aprobados por el autor el 2026-10-20.
2. **Contrato:** los dos esquemas y la descripción de `editarVersion`; la generación.
3. **Dominio,** con sus pruebas primero: `Regla`, `PesosAfinidad`, `MinimoAfinidad` y la severidad en `editada`.
4. **Persistencia:** V4 y el repositorio (reglas sin parámetros y su orden), con Testcontainers y la vuelta atrás.
5. **API:** la severidad de `ReglaEditada`; las pruebas de contrato.
6. **SPA:** los nombres, la sección «Afinidad» y el selector de severidad.
7. **Punta a punta:** los pasos de afinidad y de severidad en `probar_normativa.sh`.
8. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue y comprobación con `vicerrectorado.demo`, y el PR de evidencia con `Closes #42`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Identificador `RF-xx` | La tabla 5.2 nombra RF-24 a la comprobación de la oferta sin propuesta, que no tiene número RN | Inventarle un número RN: no existe en la ERS y confundiría la trazabilidad. |
| Reglas sin parámetros | Siete reglas del motor solo tienen severidad | Darles un parámetro ficticio: sería una cifra sin sentido en la pantalla y en el registro. |
| Completar versiones existentes por migración (D-68) | La versión 1 y la 2 del equipo necesitan los pesos y las reglas para F06 y F07 | Agregarlas solo a las versiones nuevas: las existentes quedarían incompletas para siempre, porque una versión en uso no cambia. |
