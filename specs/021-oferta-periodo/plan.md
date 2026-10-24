# Implementation Plan: Mantener la oferta del periodo

**Branch**: `021-oferta-periodo` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/021-oferta-periodo/spec.md` (issue #37, F03-H03)

## Summary

- **La oferta en Académico:** una tabla `asignatura_paralelo` con clave periodo + asignatura + paralelo, con las reglas de la carga 5. Las horas y la sede se guardan solo si se indican; si no, la consulta da las de la asignatura y las de la carrera. Nada se borra: se retira y se vuelve a ofertar. Cada cambio queda en el registro.
- **Las reglas:** el periodo en Planificación (aclaración 2), la asignatura activa de una carrera vigente, la jornada de la carrera, la sede válida y las horas que no pasan las de la asignatura (RN-18, aclaración 3). Viven en `OfertaDelPeriodo`, sobre los `DatosMaestros` de F03-H02.
- **Horas a cubrir:** se suman por asignatura, por carrera y del periodo; las retiradas no suman (RF-15).
- **Permisos:** escriben `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`, el Administrador y el Vicerrector. Leen `[OFERTA:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, entre ellos el Responsable de consolidación (D-70, aclaración 1). Sin decisiones nuevas.
- **Contrato:** 5 operaciones y 2 códigos `ACADEMICO_*`.
- **SPA:** la pantalla «Oferta del periodo».
- **Punta a punta:** `deploy/probar_oferta.sh`, con las 165 asignaturas-paralelo de prueba, que suman 660 horas.
- **Glosario:** «Asignatura-paralelo» se amplía con el retiro y se agrega «Horas a cubrir»; aprobado por el autor y aplicado el 2026-10-24 (research §10).

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: sin dependencias nuevas.

**Storage**: esquema `academico`, `V5__oferta_del_periodo.sql`: `asignatura_paralelo` y las 4 acciones nuevas del registro ([data-model.md](data-model.md)).

**Testing**:
- JUnit y AssertJ en el dominio, sin dobles, con las cargas 02, 04 y 05 de prueba;
- Mockito en la aplicación y Testcontainers en el repositorio;
- swagger-request-validator en el contrato y `VerificadorDePermisos` en los permisos;
- Vitest, y Playwright con axe;
- `probar_oferta.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Académico (`datos`), el contrato, la SPA y un script.

**Performance Goals**: consultar la oferta de un periodo con 165 asignaturas-paralelo y sus totales por debajo de 300 ms. SC-001: ofertar una asignatura-paralelo en menos de 1 minuto.

**Constraints**:
- los valores permitidos, en un solo lugar, los mismos de F03-H02 (principio VI);
- nada se borra y cada cambio queda registrado (principio VII);
- escritura con dos permisos alternativos (principio IX, D-70);
- 60 % de líneas por módulo.

**Scale/Scope**: 1 tabla; 5 operaciones y 2 códigos; 1 pantalla; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Reglas en `AsignaturaParalelo` y `OfertaDelPeriodo`; la aplicación orquesta; los adaptadores traducen. | ✅ |
| II. DDD según el subdominio | Académico es de soporte: valores con sus reglas y un valor para las reglas entre entidades, sin agregado ni eventos (research §1). | ✅ |
| III. Lenguaje ubicuo | Oferta, Asignatura-paralelo, Paralelo, Jornada y Modalidad, del glosario. «Retirada» y «Horas a cubrir» se aprobaron y aplicaron antes del código (research §10). | ✅ |
| IV. API primero | Las 5 operaciones y los 2 códigos en el contrato antes del código. | ✅ |
| V. Límites | Todo en Académico. El distributivo la consumirá por la API publicada (Ola 2). | ✅ |
| VI. Las reglas son datos | Jornadas y modalidades, las enumeraciones de F03-H02, ya comparadas con el diccionario; el estado, una enumeración nueva. | ✅ |
| VII. Inmutabilidad y trazabilidad | Nada se borra (retirar en lugar de borrar); 4 acciones nuevas en `registro_cambio`. | ✅ |
| VIII. Pruebas | Primero las del dominio, sin dobles, con las 165 asignaturas-paralelo de prueba como caso grande. | ✅ |
| IX. Seguridad | Escritura con dos permisos alternativos validados en el servidor (D-70). El coordinador lee toda la oferta mientras el servidor no filtre lo propio, como ya pasa con las carreras (research §5). | ✅ |
| X. Simplicidad | Las horas a cubrir se calculan al consultar; las horas y la sede, sin copia. | ✅ |
| XI y XII | Sin abstracciones nuevas; JavaDoc en español. | ✅ |
| XIII. Errores | 2 códigos en el catálogo; los datos inválidos, 400 con su campo. | ✅ |
| §6.4 Decisiones | Ninguna nueva: aplica D-69 y D-70. Siguiente número libre: D-72 · ADR-022. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. Los dos cambios del glosario quedaron aprobados por el autor y aplicados el 2026-10-24 (copia `modelo_dominio.md.antes-F03-H03`).

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/021-oferta-periodo/
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
contratos/academico/openapi.yaml, contratos/comun/errores.md     # 5 operaciones y 2 códigos
backend/servicios/academico/src/main/java/ec/mdd/academico/
├── dominio/                 # AsignaturaParalelo, OfertaDelPeriodo, HorasACubrir, EstadoDeAsignaturaParalelo
├── puertos/{entrada,salida}/# ConsultarOferta, MantenerOferta, RepositorioOferta
├── aplicacion/              # servicios
└── adaptadores/             # OfertaControlador, RepositorioOfertaJdbc, ErroresAcademico
backend/servicios/academico/src/main/resources/db/migration/academico/V5__oferta_del_periodo.sql
backend/servicios/academico/src/test/resources/academico/datos-de-prueba/   # carga 05
frontend/src/pantallas/oferta/                               # «Oferta del periodo»
frontend/src/navegacion/Marco.tsx, frontend/src/rutas.tsx, frontend/src/sesion/funcionalidades.ts
deploy/probar_oferta.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: la oferta es Datos maestros, en Académico, junto a los periodos y las asignaturas a los que apunta.

## Fases de implementación (guía para /speckit-tasks)

1. **Glosario:** aprobado por el autor el 2026-10-24 y ya aplicado (copia `modelo_dominio.md.antes-F03-H03`).
2. **Contrato y catálogo:** las 5 operaciones y los 2 códigos.
3. **Dominio,** con las pruebas primero: `AsignaturaParalelo`, `OfertaDelPeriodo` y `HorasACubrir`, con las cargas 02, 04 y 05 de prueba (660 horas, y los totales de cada carrera).
4. **Persistencia:** V5 y el repositorio, con Testcontainers.
5. **Aplicación y API:** los servicios, el controlador, y las pruebas de contrato y de permisos (las 30 operaciones de Académico).
6. **SPA:** «Oferta del periodo».
7. **Punta a punta:** `probar_oferta.sh`, en el perfil y en `perfil-consolidado`.
8. **Cierre:**
   - evidencia y PR;
   - fusión con la aprobación del autor y despliegue;
   - la comprobación con `vicerrectorado.demo` y `consolidacion.demo`;
   - el PR de evidencia con `Closes #37`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `OfertaDelPeriodo` con el periodo y los `DatosMaestros` | Las reglas del estado del periodo, la asignatura activa, la carrera vigente, la jornada, la sede y las horas miran a otras entidades | Validar en el repositorio: la regla quedaría fuera del dominio. |
| El estado del periodo puesto en la base en `probar_oferta.sh` | Salir de Planificación por la API exige abrir el periodo con una versión de Normativa | Abrirlo por la API: repetiría `probar_periodos.sh` y su limpieza en Normativa y en Roles. |
