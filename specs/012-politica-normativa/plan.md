# Implementation Plan: La política 2024 como primera versión de normativa

**Branch**: `012-politica-normativa` | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-politica-normativa/spec.md` (issue #40, F04-H01)

## Summary

Sobre el esqueleto del servicio Normativa:
- **Dominio (subdominio central, DDD táctico):** el agregado `VersionNormativa`, con `TablaCargaHoraria`, `FilaCargaHoraria`, `Regla`, `TipoIst` y `Severidad`, cada uno con sus invariantes; el evento `VersionNormativaCreada`.
- **Versión 1 por migración:** `V2__politica_2024.sql` crea las tablas e inserta la versión 1:
  - la Política SENESCYT 2024 con sus 22 filas;
  - el tipo de IST T2;
  - 7 reglas con 29 parámetros de RN-01 a RN-15;
  - el registro de su creación.

  Una prueba la compara, valor por valor, con la carga 7.
- **Consulta:** el contrato `contratos/normativa/openapi.yaml`, con `listarVersiones` y `consultarVersion`, protegidas con `PARAMETROS:LECTURA` (ADR-021).
- **D-66 (propuesta):** el Vicerrector académico gana `PARAMETROS:LECTURA`, con una migración de Roles.
- **SPA:** la pantalla «Normativa», de solo lectura.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: no hay dependencias nuevas en el proyecto.
- **Normativa:** aplica `mdd.contrato`, con `spring-boot-starter-jdbc` (`JdbcClient`), `spring-security-core` (`@PreAuthorize`) y `jackson-databind` (el detalle en `jsonb`), como Roles.
- **SPA:** los componentes de MUI que ya usa.

**Storage**: esquema `normativa`, con las tablas `version_normativa`, `fila_carga_horaria`, `regla`, `parametro_regla` y `registro_cambio` (V2). En el esquema `roles`, V4 con un permiso nuevo.

**Testing**:
- **Dominio:** JUnit Jupiter 6 y AssertJ, sin dobles.
- **Aplicación:** Mockito, solo en los puertos.
- **Adaptadores:** Testcontainers con PostgreSQL. Incluye la comparación con la política y el repositorio que reconstruye el agregado.
- **Contrato:** swagger-request-validator y `VerificadorDePermisos`.
- **SPA:** Vitest y Playwright con axe-core.
- **Punta a punta:** `deploy/probar_normativa.sh`, nuevo.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: el servicio Normativa (proceso `datos`), Roles (una migración), los contratos, la SPA y el despliegue.

**Performance Goals**: la consulta de una versión lee unas 60 filas en tres consultas, en menos de 100 ms. La pantalla se abre en menos de un minuto desde el inicio de sesión (SC-002).

**Constraints**:
- ninguna cifra de la política queda en el código (principio VI);
- la versión 1 no se duplica ni cambia al reiniciar o desplegar;
- la versión anterior del equipo sigue arrancando con el esquema nuevo (D-55);
- un servicio no lee las tablas de otro (principio V);
- el contrato va antes que el código;
- 60 % de líneas por módulo.

**Scale/Scope**:
- 1 versión, con 22 filas, 7 reglas y 29 parámetros;
- 2 operaciones;
- 1 pantalla de solo lectura;
- 1 permiso nuevo en la matriz;
- 1 script de punta a punta.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Normativa con sus cinco capas. Las invariantes, en el dominio. La consulta, por un puerto de entrada, y la persistencia, por un puerto de salida. | ✅ |
| II. DDD según el subdominio | Normativa es central: agregado, objetos de valor, evento de dominio y repositorio (research §1). | ✅ |
| III. Lenguaje ubicuo | Versión de normativa, Regla, Severidad, Tipo de IST, Componente y Política, tal como en el glosario §7.3. «Tabla de carga horaria» y «Fila» siguen el modelo de dominio §5.1 (`TablaCargaHoraria`). | ✅ |
| IV. API primero | `contratos/normativa/openapi.yaml` antes del código, con `x-mdd-permiso`. | ✅ |
| V. Límites | Normativa solo en su esquema. El permiso del Vicerrector lo cambia Roles con su propia migración. | ✅ |
| VI. Las reglas son datos | Las cifras de la política están solo en la versión 1, en la base. Las severidades son datos de la versión (MD-08). | ✅ |
| VII. Inmutabilidad y trazabilidad | La versión 1 no tiene operaciones que la cambien; su creación queda en `registro_cambio`. | ✅ |
| VIII. Pruebas | Primero las pruebas. Las invariantes se prueban sin dobles; Mockito solo en la aplicación; Testcontainers en los adaptadores; una prueba de autorización por operación. | ✅ |
| IX. Seguridad | Lectura con mínimo privilegio, validada en el servidor (RNF-SEG-03). Sin datos personales. | ✅ |
| X. Simplicidad | La versión 1 por migración, sin un caso de uso de creación ni un envío de eventos sin consumidor (research §2 y §5). | ✅ |
| XI y XII | JavaDoc en español en los puertos y el dominio, con la regla RN-xx que aplica cada pieza. | ✅ |
| XIII. Errores | Sin códigos nuevos: el 404 y el 403 son los comunes. | ✅ |
| §6.4 Decisiones | D-66, aceptada por el autor el 2026-10-18. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-66 quedó aprobada el 2026-10-18.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/012-politica-normativa/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: dominio, tablas, versión 1 y D-66
├── quickstart.md            # fase 1
├── contracts/
│   └── normativa.md         # las dos operaciones y sus esquemas
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/normativa/openapi.yaml                  # nuevo
backend/servicios/normativa/                      # build.gradle con mdd.contrato
├── src/main/java/ec/mdd/normativa/
│   ├── dominio/                                  # VersionNormativa, TablaCargaHoraria, FilaCargaHoraria, Regla, TipoIst, Severidad, VersionNormativaCreada
│   ├── puertos/entrada/                          # ConsultarNormativa
│   ├── puertos/salida/                           # RepositorioVersiones
│   ├── aplicacion/                               # ConsultarNormativaServicio, VersionNoEncontrada
│   ├── adaptadores/entrada/                      # NormativaControlador
│   ├── adaptadores/salida/                       # RepositorioVersionesJdbc
│   └── infraestructura/                          # ConfiguracionNormativa
├── src/main/resources/db/migration/normativa/V2__politica_2024.sql
├── src/test/…, src/test/resources/politica-2024.csv
└── src/integrationTest/…                         # PoliticaDe2024IntegrationTest, repositorio y vuelta atrás
backend/servicios/roles/src/main/resources/db/migration/roles/V4__vicerrector_lee_normativa.sql
frontend/src/
├── pantallas/normativa/                          # Normativa, consultas, perfiles
├── rutas.tsx, navegacion/Marco.tsx               # /normativa y su entrada en el menú
deploy/probar_normativa.sh                        # nuevo; también en perfil-consolidado
```

**Structure Decision**: Normativa deja de ser un esqueleto con su primer agregado. Las historias siguientes de F04 le agregan crear y editar versiones, y F02-H01 la asocia a un periodo.

## Fases de implementación (guía para /speckit-tasks)

1. **D-66:** aprobada por el autor el 2026-10-18 (`docs/decisiones.md`).
2. **Contrato:** `normativa/openapi.yaml` y la generación en el backend y en la SPA.
3. **Dominio de Normativa,** con sus pruebas primero.
4. **Persistencia:** V2 con la versión 1, el repositorio JDBC, la prueba contra la política y la de la vuelta atrás.
5. **Aplicación y API:** la consulta, el controlador, el 404 y `PermisosNormativaTest`.
6. **Roles:** V4 con el permiso del Vicerrector y la matriz D-16 actualizada.
7. **SPA:** la pantalla «Normativa», la ruta con permiso y el menú.
8. **Punta a punta:** `probar_normativa.sh`, en local y en `perfil-consolidado`.
9. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue automático, comprobación con `vicerrectorado.demo` y el PR de evidencia con `Closes #40`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Invariantes en el agregado para datos que llegan por migración | La versión 1 no pasa por un caso de uso. Reconstruirla con el agregado en la prueba es lo que atrapa un error de digitación de la política | Confiar solo en las restricciones `check` de la base: no expresan «mínimo ≤ clase ≤ máximo» ni «la suma no supera la dedicación» de forma legible ni reutilizable para F04-H02. |
| Parámetros con nombre (`parametro_regla`) en lugar de columnas | RN-01 a RN-26 tienen formas distintas, y las versiones nuevas agregan reglas | Una columna por parámetro: cada regla nueva cambia el esquema y el agregado. |
