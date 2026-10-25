# Implementation Plan: Registrar los cargos y los proyectos aprobados

**Branch**: `023-cargos-proyectos` | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/023-cargos-proyectos/spec.md` (issue #39, F03-H05)

## Summary

- **Dos tablas más en Docentes:** `cargo` y `proyecto`, en `V3__cargos_y_proyectos.sql`, con el mismo `registro_cambio` y cuatro acciones más. Nada se borra: un cargo se finaliza y un proyecto cambia de estado.
- **Las reglas:** las del diccionario de las cargas 8 y 9 (principio XIV), en los records `Cargo` y `Proyecto`:
  - el docente del cargo, activo y de tiempo completo;
  - un coordinador vigente por carrera (RN-12);
  - la unidad según el cargo;
  - el fin de funciones hasta dos años después;
  - las horas de los participantes que no pasan las aprobadas.
- **Advertencias de los cargos:** se calculan al leer. Avisan de un coordinador o un gestor sin cuarto nivel (RN-20), de un cargo vigente con la fecha de fin vencida y de un docente que dejó de estar activo a tiempo completo.
- **Asignable:** un método puro, `Proyecto.asignableEn(inicio, fin)`, contra las fechas del periodo que llegan de Académico.
- **Académico:** `consultarReferencias` suma las unidades y los periodos; es un cambio compatible del contrato.
- **Catálogo de cargos:** el diccionario da los cargos de gestor solo como rango. Se fijan los 16 códigos G06 a G21 (**D-73**, aprobada; research §3), que pasan a `diccionario-de-datos.yaml`.
- **Permisos:** escriben `[DOCENTES|CATALOGOS|DISTRIBUTIVO]:ESCRITURA` y leen `[DOCENTES|CATALOGOS|DISTRIBUTIVO]:LECTURA` (D-70). «Proyectos: lectura», que el Gestor de unidad tiene solo de lo propio, no basta hasta F08 (D-64). La matriz no cambia.
- **Contrato:** 7 operaciones en Docentes, la ampliación de `consultarReferencias` y de `consultarReferenciasDeDocentes`, y 4 códigos `DOCENTES_*` con sus 8 columnas (D-72).
- **SPA:** las pantallas «Cargos» y «Proyectos», con la validación del principio XIV en pantalla.
- **Punta a punta:** `deploy/probar_cargos_proyectos.sh`, con los 21 cargos y los 11 proyectos de prueba.
- **Glosario:** cambian «Cargo» y «Proyecto aprobado», y se agregan «Participante», «Proyecto asignable» y «Requisito del cargo». Aprobados por el autor y aplicados el 2026-10-25 (research §12).

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: ninguna nueva. Docentes ya tiene `mdd.contrato`, JDBC, Jackson y su dependencia de `:servicios:academico`.

**Storage**: esquema `docentes`, `V3__cargos_y_proyectos.sql`: `cargo`, `proyecto` y el `check` ampliado de `registro_cambio` ([data-model.md](data-model.md)).

**Testing**:
- **Dominio:** JUnit y AssertJ, sin dobles, con las cargas 01, 02, 03, 06, 08 y 09 de prueba. Cada regla de las tablas de la especificación tiene sus casos válidos, inválidos y en el límite (principio XIV).
- **Catálogos:** `ValoresPermitidosTest` contra el diccionario, también los 22 cargos.
- **Aplicación y repositorio:** Mockito en la aplicación y Testcontainers en el repositorio.
- **Contrato y permisos:** swagger-request-validator en el contrato, `VerificadorDePermisos` en los permisos y `VerificadorDeCatalogo` en los 4 códigos.
- **Límites:** `ModulosTest` del proceso `datos`.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** `probar_cargos_proyectos.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Docentes y las referencias de Académico (`datos`), el contrato, el diccionario de cargas, la SPA y un script.

**Performance Goals**: listar los proyectos de un periodo en menos de 300 ms; la prueba falla desde 500 ms. SC-001: registrar un cargo o consultar si un proyecto es asignable en menos de 1 minuto.

**Constraints**:
- los valores permitidos en un solo lugar, comparados con el diccionario (principio VI);
- nada se borra y cada cambio queda registrado (principio VII);
- datos ficticios, sin nombres ni correos en los logs ni en los mensajes (principio IX);
- escritura con tres permisos alternativos (D-70);
- sin claves foráneas entre esquemas (principio V);
- 60 % de líneas por módulo;
- cada campo validado por su tipo, sus caracteres y su largo en el servidor y en la pantalla (principio XIV);
- cada rechazo con un código del catálogo completo (principio XIII, D-72).

**Scale/Scope**: 2 tablas; 7 operaciones en Docentes, 1 operación ampliada en Académico y 4 códigos; 2 pantallas; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas, en `Cargo`, `Proyecto` y `Referencias`; la aplicación orquesta; los adaptadores traducen. | ✅ |
| II. DDD según el subdominio | Docentes es de soporte: valores con sus reglas, sin agregado ni eventos. | ✅ |
| III. Lenguaje ubicuo | Cargo, Gestión autorizada y Proyecto aprobado, del glosario. Los cinco cambios de research §12 se aprobaron y aplicaron el 2026-10-25, antes del código. | ✅ |
| IV. API primero | El contrato, los 4 códigos y la ampliación de Académico, antes del código. | ✅ |
| V. Límites | Docentes usa Académico solo por `ConsultarReferencias` (`puertos.entrada`), sin claves foráneas entre esquemas (research §2). | ✅ |
| VI. Las reglas son datos | Las enumeraciones, comparadas con las cargas 8 y 9 del diccionario; los 16 cargos de gestor, en el diccionario (D-73). | ✅ |
| VII. Inmutabilidad y trazabilidad | Nada se borra; 4 acciones en `registro_cambio` con el valor anterior y el nuevo (research §9). | ✅ |
| VIII. Pruebas | Primero las del dominio, sin dobles, con los 21 cargos y los 11 proyectos de prueba. | ✅ |
| IX. Seguridad y datos personales | Permisos alternativos validados en el servidor (D-70). «Proyectos: lectura» solo de lo propio no da acceso mientras su filtro no se pueda aplicar (D-64, aclaración 8). Datos ficticios. Los mensajes no repiten identificaciones. | ✅ |
| X. Simplicidad | Las listas del proyecto en `jsonb`, la advertencia calculada y las listas filtradas en memoria (research §5 y §6). | ✅ |
| XI y XII | Ninguna interfaz nueva fuera de los puertos de los casos de uso; JavaDoc en español. | ✅ |
| XIII. Errores | 4 códigos con sus 8 columnas; los datos inválidos, 400 con su campo y su elemento (research §10). | ✅ |
| XIV. Validación de las entradas | Las tablas de la especificación, en el contrato (`pattern`, `minLength`, `maxLength`, `minimum`, `maximum`, `enum`), en el dominio y en la SPA. Lo que el diccionario no define no se valida; los dos huecos se aclararon con el autor (aclaraciones 5 y 6). | ✅ |
| §6.4 Decisiones | D-73: los 16 cargos de gestor, aprobada el 2026-10-25. Siguiente número libre: D-74 · ADR-022. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. El autor aprobó el 2026-10-25 los cambios del glosario (research §12) y D-73 (research §3); ya están aplicados, con la copia `modelo_dominio.md.antes-F03-H05`.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/023-cargos-proyectos/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── cargos-proyectos.md  # operaciones, esquemas y errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
docs/decisiones.md                                               # D-73
contratos/docentes/openapi.yaml, contratos/comun/errores.md      # 7 operaciones y 4 códigos
contratos/academico/openapi.yaml                                 # consultarReferencias con unidades y periodos
backend/servicios/cargas/src/main/resources/cargas/diccionario-de-datos.yaml   # los 16 cargos de gestor
backend/servicios/docentes/src/main/java/ec/mdd/docentes/
├── dominio/                 # Cargo, TipoDeCargo, EstadoDeCargo, AdvertenciaDeCargo, Proyecto, Participante,
│                            # TipoDeProyecto, EstadoDeProyecto, RolDeParticipante, Asignabilidad;
│                            # Referencias con unidades y periodos
├── puertos/{entrada,salida}/# ConsultarCargos, MantenerCargos, ConsultarProyectos, MantenerProyectos,
│                            # sus excepciones; RepositorioCargos, RepositorioProyectos
├── aplicacion/              # sus servicios
└── adaptadores/             # CargosControlador, ProyectosControlador, ErroresDocentes,
                             # RepositorioCargosJdbc, RepositorioProyectosJdbc, AcademicoDesdePuertos
backend/servicios/docentes/src/main/resources/db/migration/docentes/V3__cargos_y_proyectos.sql
backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/   # más las cargas 01, 03, 08 y 09
backend/servicios/academico/src/main/java/ec/mdd/academico/
├── puertos/entrada/         # UnidadDeReferencia, PeriodoDeReferencia; ReferenciasDeAcademico ampliado
└── aplicacion/              # ConsultarReferenciasServicio con unidades y periodos
frontend/src/pantallas/comun/validacion.ts                       # filtrar, movido de docentes
frontend/src/pantallas/cargos/, frontend/src/pantallas/proyectos/
frontend/src/navegacion/Marco.tsx, frontend/src/rutas.tsx, frontend/src/sesion/funcionalidades.ts
frontend/e2e/cargos-proyectos.spec.ts
deploy/probar_cargos_proyectos.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: los cargos y los proyectos son Datos maestros del servicio 4, junto al docente (ERS §9, research §1). Académico solo amplía sus referencias.

## Fases de implementación (guía para /speckit-tasks)

1. **Aprobaciones del autor:** el glosario (research §12) y D-73 (research §3), aprobados el 2026-10-25 y ya aplicados en `modelo_dominio.md` (copia `.antes-F03-H05`) y en `docs/decisiones.md`.
2. **Contrato, catálogo y diccionario:**
   - los tags `Cargos` y `Proyectos` en `contratos/docentes/openapi.yaml`;
   - la ampliación de Académico;
   - los 4 códigos con sus 8 columnas;
   - los 16 cargos en `diccionario-de-datos.yaml`;
   - el tipo generado en la SPA.
3. **Académico:** las unidades y los periodos en `ConsultarReferencias`, con sus pruebas de aplicación y de contrato.
4. **Dominio,** con las pruebas primero:
   - `Cargo` y `TipoDeCargo`;
   - `Proyecto` y `Participante`;
   - `asignableEn`;
   - `Referencias` ampliada;
   - `ValoresPermitidosTest`;
   - los 21 cargos y los 11 proyectos de prueba, más los casos válidos, inválidos y en el límite de cada campo.
5. **Persistencia:** V3 y los dos repositorios, con Testcontainers. Incluye el índice parcial de RN-12 y el `check` ampliado.
6. **Aplicación y API:**
   - los servicios y los dos controladores;
   - `AcademicoDesdePuertos` ampliado;
   - las pruebas de contrato, de permisos y de catálogo;
   - `ModulosTest`.
7. **SPA:** `validacion.ts` común, y «Cargos» y «Proyectos», con Vitest y con Playwright y axe, también con los campos marcados en rojo.
8. **Punta a punta:** `probar_cargos_proyectos.sh`, en el perfil y en `perfil-consolidado`.
9. **Cierre:**
   - la evidencia y el PR;
   - la fusión con la aprobación del autor y el despliegue;
   - la comprobación con `datos.demo` y `consolidacion.demo`;
   - el PR de evidencia con `Closes #39`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Unidades y periodos en `ConsultarReferencias` | Docentes valida la unidad de cada cargo y decide si un proyecto es asignable con las fechas del periodo, sin tipos del dominio de Académico (principio V). | Usar `ConsultarEstructura` y `ConsultarPeriodos`: devuelven `UnidadOrganica` y `PeriodoAcademico`, que `ModulosTest` prohíbe a Docentes. |
| Las listas del proyecto en `jsonb` | Las carreras y los participantes se leen y se guardan con el proyecto, entero. | Dos tablas hijas: joins y borrados de filas al corregir, sin una consulta que hoy los necesite. |
| El índice único parcial de RN-12 | Garantiza un coordinador vigente por carrera aunque dos registros lleguen a la vez. | Solo la comprobación del servicio: con el `LOCK TABLE` basta en la práctica, pero el índice protege la regla en la base. |
