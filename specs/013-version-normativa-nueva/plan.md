# Implementation Plan: Crear una versión nueva de la normativa

**Branch**: `013-version-normativa-nueva` | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-version-normativa-nueva/spec.md` (issue #41, F04-H02)

## Summary

Sobre la versión 1 y la consulta de F04-H01:
- **Dominio:** `VersionNormativa` gana comportamiento, y sigue inmutable:
  - **`copiaDe`:** crea una versión con el siguiente número y el evento `VersionNormativaCreada`;
  - **`editada`:** aplica los cambios de cabecera, filas y parámetros, y devuelve lo que cambió;
  - **`asociadaA`:** agrega una `AsignacionNormativa`. Desde ahí la versión está en uso y no se edita.
- **Una sola forma de cambiar una versión:** `editarVersion` recibe la versión completa, como la carga 7 por archivo de F01 (FR-010). No se pueden agregar ni quitar filas ni parámetros (FR-002).
- **Asociar a un periodo:** `asociarPeriodo` y `consultarPeriodo`. Un periodo, nombrado por su código, tiene una sola versión. Académico lo usará en F02-H01 por el puerto publicado.
- **Errores:** tres códigos nuevos (`NORMATIVA_VERSION_EN_USO`, `NORMATIVA_CODIGO_REPETIDO` y `NORMATIVA_PERIODO_CON_VERSION`), y 400 con detalle para las invariantes.
- **Registro:** cada cambio queda en `registro_cambio`, con su valor anterior y el nuevo.
- **D-67 (aceptada):** el Vicerrector académico escribe «Parámetros y normativa».
- **SPA:** «Normativa» gana crear, editar y asociar.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: no hay dependencias nuevas.
- **Normativa:** `jackson-databind` para el detalle en `jsonb` del registro, que ahora escribe Java. Ya lo usa Roles.
- **Transacciones:** `TransactionOperations`, como en Roles.

**Storage**: esquema `normativa`:
- **`V3__versiones_y_periodos.sql`:** `copiada_de` en `version_normativa`, la tabla `asignacion_normativa` y las acciones nuevas del registro.
- **Roles, `V5`:** un permiso nuevo.

**Testing**:
- **Dominio:** JUnit y AssertJ, sin dobles.
- **Aplicación:** Mockito.
- **Adaptadores:** Testcontainers, con la concurrencia de la numeración y del `FOR UPDATE`.
- **Contrato:** swagger-request-validator, `VerificadorDePermisos` y `ErroresNormativaTest`.
- **SPA:** Vitest y Playwright con axe.
- **Punta a punta:** `probar_normativa.sh`, con tres pasos nuevos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Normativa (proceso `datos`), Roles (una migración), los contratos, la SPA y el despliegue.

**Performance Goals**:
- crear una versión copia unas 60 filas en una transacción, en menos de 200 ms;
- editar compara y guarda la versión completa en menos de 200 ms;
- la pantalla permite crear y cambiar en menos de 2 minutos (SC-001).

**Constraints**:
- una versión con un periodo no cambia (principio VII);
- crear o cambiar una versión no altera las demás (FR-005);
- la pantalla y el archivo cambian una versión por el mismo camino (FR-010);
- el periodo es un código: Normativa no lee las tablas de Académico (principio V);
- la versión anterior del equipo sigue arrancando (D-55);
- 60 % de líneas por módulo.

**Scale/Scope**:
- 4 operaciones nuevas, para un total de 6;
- 3 códigos de error;
- 3 acciones del registro;
- 1 permiso nuevo en la matriz;
- 3 pasos nuevos de punta a punta.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas, en el agregado. La aplicación orquesta la transacción, el registro y la numeración; los adaptadores traducen HTTP y SQL. | ✅ |
| II. DDD según el subdominio | Normativa es central: el agregado con su entidad `AsignacionNormativa`, sus objetos de valor, su evento y sus excepciones de dominio (research §1). | ✅ |
| III. Lenguaje ubicuo | Versión de normativa, Tabla de carga horaria, Perfil, Regla, Periodo académico y Asignación normativa, del glosario; la última se agregó antes del código y se usa siempre completa (research §10). | ✅ |
| IV. API primero | Las cuatro operaciones y los campos nuevos en `contratos/normativa/openapi.yaml`, y los tres códigos en `errores.md`, antes del código. | ✅ |
| V. Límites | El periodo es un código; Académico usará `AsociarVersionAPeriodo` por `puertos.entrada` (F02-H01). El permiso nuevo lo cambia Roles con su migración. | ✅ |
| VI. Las reglas son datos | Las cifras cambian en una versión nueva, nunca en el código. | ✅ |
| VII. Inmutabilidad y trazabilidad | Una versión en uso no cambia. Cada creación, edición y asociación queda en el registro, con el valor anterior y el nuevo. | ✅ |
| VIII. Pruebas | Primero las pruebas. Las reglas se prueban sin dobles; la concurrencia, con PostgreSQL real; una prueba de autorización por operación. | ✅ |
| IX. Seguridad | La escritura exige `PARAMETROS:ESCRITURA`, validada en el servidor. | ✅ |
| X. Simplicidad | Una sola operación de edición, con la versión completa, en lugar de una por celda. Sin estados «borrador» ni «publicada», y sin envío de eventos sin consumidor. | ✅ |
| XI y XII | JavaDoc en español en el agregado, los puertos y las excepciones. | ✅ |
| XIII. Errores | Tres códigos nuevos en el catálogo, con su prueba. | ✅ |
| §6.4 Decisiones | D-67, aceptada por el autor el 2026-10-18. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-67 y la entrada «Asignación normativa» del glosario quedaron aprobadas el 2026-10-18.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/013-version-normativa-nueva/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── normativa.md         # las operaciones nuevas, los campos y los errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/normativa/openapi.yaml, contratos/comun/errores.md
backend/servicios/normativa/src/main/java/ec/mdd/normativa/
├── dominio/                 # VersionNormativa (operaciones), AsignacionNormativa, DatosDeVersion, CambioDeValor, VersionNormativaCreada, VersionEnUso, CambioNoPermitido, FilaInvalida
├── puertos/entrada/         # CrearVersion, EditarVersion, AsociarVersionAPeriodo (publicado), ConsultarNormativa (con el periodo), CodigoRepetido, PeriodoConVersion, PeriodoSinVersion
├── puertos/salida/          # RepositorioVersiones (guardar, numerar, bloquear), RegistroDeCambios
├── aplicacion/              # los servicios
├── adaptadores/entrada/     # NormativaControlador, ErroresNormativa
├── adaptadores/salida/      # RepositorioVersionesJdbc, RegistroDeCambiosJdbc
└── infraestructura/         # ConfiguracionNormativa
backend/servicios/normativa/src/main/resources/db/migration/normativa/V3__versiones_y_periodos.sql
backend/servicios/roles/src/main/resources/db/migration/roles/V5__vicerrector_escribe_normativa.sql
frontend/src/pantallas/normativa/  # Normativa con crear, editar y asociar
deploy/probar_normativa.sh         # pasos 5 a 7 y la limpieza
```

**Structure Decision**: todo el cambio de versiones vive en Normativa. Académico solo lo llamará en F02-H01.

## Fases de implementación (guía para /speckit-tasks)

1. **D-67 y el glosario:** aprobados por el autor el 2026-10-18.
2. **Contrato y catálogo:** las operaciones, los campos y los errores; la generación.
3. **Dominio,** con sus pruebas primero.
4. **Persistencia:** V3, el repositorio (guardar, numerar con candado, `FOR UPDATE`) y el registro, con Testcontainers.
5. **Aplicación y API:** crear, editar y asociar; los errores; `PermisosNormativaTest` con las seis operaciones.
6. **Roles:** V5 y la matriz.
7. **SPA:** crear, editar, asociar y el solo lectura de una versión en uso.
8. **Punta a punta:** los pasos nuevos de `probar_normativa.sh`, con su limpieza.
9. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue y comprobación con `vicerrectorado.demo`, y el PR de evidencia con `Closes #41`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Candado de la transacción para numerar | Dos creaciones simultáneas no pueden obtener el mismo número (caso límite) | Reintentar ante la violación de `unique`: más código y un error visible en el peor caso. |
| `SELECT … FOR UPDATE` en editar y asociar | Una edición no puede colarse después de una asociación (principio VII) | Comprobar «en uso» sin bloquear: entre la comprobación y el guardado, otro podría asociar la versión. |
| Limpieza por SQL en `probar_normativa.sh` | La API no permite borrar versiones (principio VII), y el script se repite sobre la misma base | No limpiar: cada corrida dejaría versiones y periodos de prueba, y el paso 1 («una sola versión») fallaría. |
