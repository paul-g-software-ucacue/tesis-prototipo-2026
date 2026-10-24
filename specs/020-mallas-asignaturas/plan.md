# Implementation Plan: Mantener carreras, mallas y asignaturas

**Branch**: `020-mallas-asignaturas` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/020-mallas-asignaturas/spec.md` (issue #36, F03-H02)

## Summary

- **Carreras, mallas y asignaturas en Académico:** tres tablas nuevas y la de los 11 campos amplios CINE, con las reglas de las cargas 2 y 4 del diccionario de datos. La sede de cada carrera es una unidad de la estructura orgánica (F03-H01). Nada se borra; cada cambio queda en el registro.
- **La malla** es una entidad con su código de versión y su estado; la consulta da sus horas, para que la oferta (F03-H03) aplique RN-18 (aclaración 2).
- **Los valores permitidos** son enumeraciones del dominio, y una prueba los compara con el diccionario de datos.
- **Permisos:** escriben el Administrador y el Vicerrector, `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`; leen `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` (D-70, aclaración 1). Sin decisiones nuevas.
- **Contrato:** 13 operaciones y 4 códigos `ACADEMICO_*`.
- **SPA:** la pantalla «Carreras y mallas».
- **Punta a punta:** `deploy/probar_carreras.sh`, con las 7 carreras, 12 mallas y 300 asignaturas de los datos de prueba.
- **Glosario:** «Jornada» gana vespertina, y se agregan «Modalidad» y «Nivel de formación», pendiente de la aprobación del autor antes del código (research §10).

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: sin dependencias nuevas.

**Storage**: esquema `academico`, `V4__carreras_mallas_asignaturas.sql`: `campo_amplio` (con los 11), `carrera`, `malla`, `asignatura` y las 9 acciones nuevas del registro ([data-model.md](data-model.md)).

**Testing**: JUnit y AssertJ en el dominio, sin dobles, con las cargas 02 y 04 de prueba; una prueba que compara las enumeraciones con el diccionario de datos; Mockito en la aplicación; Testcontainers en el repositorio; swagger-request-validator en el contrato; `VerificadorDePermisos`; Vitest y Playwright con axe; `probar_carreras.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Académico (`datos`), el contrato, la SPA y un script.

**Performance Goals**: listar las 7 carreras y las 300 asignaturas de una vez, por debajo de 300 ms; SC-001: registrar una asignatura en menos de 2 minutos.

**Constraints**:
- los valores permitidos, en un solo lugar e iguales al diccionario (principio VI);
- nada se borra y cada cambio queda registrado (principio VII);
- escritura con dos permisos alternativos (principio IX, D-70);
- 60 % de líneas por módulo.

**Scale/Scope**: 4 tablas; 13 operaciones y 4 códigos; 1 pantalla; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Reglas en `Carrera`, `Malla`, `Asignatura` y `DatosMaestros`; la aplicación orquesta; los adaptadores traducen. | ✅ |
| II. DDD según el subdominio | Académico es de soporte: valores con sus reglas y un valor para las reglas entre entidades, sin agregado elaborado ni eventos (research §1). | ✅ |
| III. Lenguaje ubicuo | Carrera, Malla, Asignatura, Ciclo, Tronco común y Campo de conocimiento, del glosario; «Jornada», «Modalidad» y «Nivel de formación» se ponen al día antes del código (research §10). | ⏳ pendiente de aprobación |
| IV. API primero | Las 13 operaciones y los 4 códigos en el contrato antes del código. | ✅ |
| V. Límites | Todo en Académico; la sede se valida con su propia estructura orgánica. El diccionario de datos solo lo lee una prueba, como archivo. | ✅ |
| VI. Las reglas son datos | Los campos amplios son filas; los demás catálogos, enumeraciones únicas comparadas con el diccionario (research §2). | ✅ |
| VII. Inmutabilidad y trazabilidad | Nada se borra; 9 acciones nuevas en `registro_cambio`. | ✅ |
| VIII. Pruebas | Primero las del dominio, sin dobles, con las 300 asignaturas de prueba como caso grande. | ✅ |
| IX. Seguridad | Escritura con dos permisos alternativos validados en el servidor (D-70); los coordinadores solo leen. | ✅ |
| X. Simplicidad | Las horas de cada malla se calculan al consultar; sin tablas de catálogo para listas fijas. | ✅ |
| XI y XII | Sin abstracciones nuevas; JavaDoc en español. | ✅ |
| XIII. Errores | 4 códigos en el catálogo; los datos inválidos, 400 con su campo. | ✅ |
| §6.4 Decisiones | Ninguna nueva: aplica D-69 y D-70. Siguiente número libre: D-72 · ADR-022. | ✅ |

**Resultado del gate antes de la fase 0:** pasa, con una condición: los tres términos del glosario (research §10) se aprueban y se aplican antes del código.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado. Dos ajustes menores de la spec se proponen en research §2 (FR-006 y el nombre CINE del campo específico).

## Project Structure

### Documentation (this feature)

```text
specs/020-mallas-asignaturas/
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
contratos/academico/openapi.yaml, contratos/comun/errores.md     # 13 operaciones y 4 códigos
backend/servicios/academico/src/main/java/ec/mdd/academico/
├── dominio/                 # Carrera, Malla, Asignatura, CampoConocimiento, DatosMaestros y enums
├── puertos/{entrada,salida}/# ConsultarCarreras, MantenerCarreras, RepositorioCarreras
├── aplicacion/              # servicios
└── adaptadores/             # CarrerasControlador, RepositorioCarrerasJdbc, ErroresAcademico
backend/servicios/academico/src/main/resources/db/migration/academico/V4__carreras_mallas_asignaturas.sql
backend/servicios/academico/src/test/resources/academico/datos-de-prueba/   # cargas 02 y 04
frontend/src/pantallas/carreras/                             # «Carreras y mallas»
frontend/src/navegacion/Marco.tsx, frontend/src/rutas.tsx, frontend/src/sesion/funcionalidades.ts
deploy/probar_carreras.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: carreras, mallas y asignaturas son Datos maestros, en Académico, junto a la estructura orgánica a la que apuntan.

## Fases de implementación (guía para /speckit-tasks)

1. **Glosario:** el autor aprueba los tres términos y se aplican (copia `modelo_dominio.md.antes-F03-H02`); los dos ajustes de la spec de research §2.
2. **Contrato y catálogo:** las 13 operaciones y los 4 códigos.
3. **Dominio,** con las pruebas primero: enumeraciones (comparadas con el diccionario), `CampoConocimiento`, `Carrera`, `Malla`, `Asignatura`, `DatosMaestros`, con las cargas 02 y 04 de prueba.
4. **Persistencia:** V4 con los 11 campos amplios, y el repositorio, con Testcontainers.
5. **Aplicación y API:** los servicios, el controlador, las pruebas de contrato y de permisos.
6. **SPA:** «Carreras y mallas».
7. **Punta a punta:** `probar_carreras.sh`, en el perfil y en `perfil-consolidado`.
8. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue, la comprobación con `vicerrectorado.demo` y `consolidacion.demo`, y el PR de evidencia con `Closes #36`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `DatosMaestros` con lo necesario para validar | La sede activa, la carrera vigente, la malla de la misma carrera y el ciclo frente al número de ciclos miran a otra entidad | Validar en el repositorio: la regla quedaría fuera del dominio. |
| Prueba de las enumeraciones contra el diccionario | Las listas viven en Académico y en el diccionario de Cargas | Leer el diccionario en tiempo de ejecución: Académico dependería de Cargas (principio V). |
