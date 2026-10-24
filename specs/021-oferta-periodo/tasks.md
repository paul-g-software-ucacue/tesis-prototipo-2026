---

description: "Tareas de F03-H03: mantener la oferta del periodo"
---

# Tasks: Mantener la oferta del periodo

**Input**: documentos de diseño en `specs/021-oferta-periodo/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/academico.md](contracts/academico.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-16, D-65, D-69, D-70, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-15 y RN-18 de la ERS; el glosario §7 («Oferta», «Asignatura-paralelo», «Paralelo», «Horas a cubrir», «Jornada», «Modalidad»).

**Organization**:
- **Fundacional:** el contrato, el dominio, la migración V5 y el repositorio.
- **US1:** registrar la oferta del periodo.
- **US2:** paralelos y horas a cubrir.
- **US3:** corregir y retirar.
- **US4:** la pantalla «Oferta del periodo».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US4.
- Rutas relativas a la raíz. Abreviaturas: `ACA` = `backend/servicios/academico/src`, `JACA` = `ACA/main/java/ec/mdd/academico`, `PACA` = `ACA/test/java/ec/mdd/academico` e `IACA` = `ACA/integrationTest/java/ec/mdd/academico`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura. `ArquitecturaTest` y `ModulosTest` lo comprueban.
- **Soporte (principio II):** `AsignaturaParalelo` es un valor con sus reglas; `OfertaDelPeriodo` tiene las reglas entre entidades, sobre los `DatosMaestros` de F03-H02; sin agregado ni eventos.
- **Principio VI:** las jornadas y las modalidades son las enumeraciones de F03-H02; ninguna lista se repite.
- **Principio VII:** nada se borra (se retira); cada cambio, en `academico.registro_cambio`.
- **Datos de prueba:** la carga 05 de la ERS (sintética, RNF-SEG-06), copiada a `ACA/test/resources/academico/datos-de-prueba/`, junto a las 02 y 04 que ya están.
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers; contrato con swagger-request-validator; 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores; la evidencia va en `specs/021-oferta-periodo/evidencia.md` y los commits llevan `Refs #37`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/021-oferta-periodo/evidencia.md` con estas secciones: entorno; base (contrato, dominio, migración y repositorio); registrar la oferta (US1); paralelos y horas a cubrir (US2); corregir y retirar (US3); pantalla (US4); cierre; equipo de demostración; notas. Copiar `05_oferta_periodo.csv` de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `ACA/test/resources/academico/datos-de-prueba/`, con su origen en la evidencia.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV, research §7)

- [X] T002 Cambiar `contratos/academico/openapi.yaml` según contracts/academico.md:
  - el tag `Oferta`, los esquemas y las 5 operaciones;
  - las de lectura con `x-mdd-permiso: [OFERTA:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` y las de escritura con `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`;
  - `example` en cada parámetro de ruta (`2026-I`, `DS-P-II-POO-204`, `A_NOCTURNA`);
  - en `AsignaturaParaleloNueva`, `asignatura`, `paralelo`, `jornada`, `modalidad` y `estudiantes` obligatorios, y `sede` y `horasSemanales` opcionales;
  - el comentario de cabecera cita F03-H03, RF-15, RN-18 y D-70.

  Agregar los 2 códigos a `contratos/comun/errores.md`. Regenerar (`openApiGenerate`, `generar:api`); `verificar:ruptura` en verde. Los `summary` con «: » van entre comillas.

### Dominio (research §1 a §3, data-model.md)

- [X] T003 Prueba primero, en `PACA/dominio/AsignaturaParaleloTest.java`, sin dobles: cada regla de research §3 con su campo y su motivo en español en `DatoInvalido` (paralelo vacío o con caracteres fuera de `^[0-9A-Z_-]{1,20}$`, estudiantes negativos o mayores que 9999, horas indicadas fuera de 0,5 a 20 o con más de dos decimales); el paralelo se guarda sin espacios en los extremos; `corregida` no cambia el periodo, la asignatura ni el paralelo y devuelve los cambios con su valor anterior y el nuevo; `conEstado` cambia solo el estado. Crear `EstadoDeAsignaturaParalelo` y `AsignaturaParalelo` en `JACA/dominio/`.
- [X] T004 Prueba primero, en `PACA/dominio/OfertaDelPeriodoTest.java`, sin dobles:
  - un periodo en En aprobación, Vigente o Cerrado lanza `OfertaCerrada` al ofertar, corregir, retirar o volver a ofertar;
  - una asignatura inexistente o inactiva, o de una carrera no vigente, da `DatoInvalido` en `asignatura`; una de una malla derogada sí se oferta;
  - una jornada que no es de la carrera, en `jornada`; una sede inexistente, inactiva o de otro tipo, en `sede`;
  - unas horas indicadas mayores que las de la asignatura, en `horasSemanales` (RN-18);
  - la misma asignatura y paralelo, ofertada o retirada, lanza `AsignaturaParaleloRepetida`;
  - sin horas ni sede, las efectivas son las de la asignatura y las de la carrera;
  - **una asignatura de 4 horas en 3 paralelos suma 12 horas a cubrir; con uno retirado, 8; al volver a ofertarlo, 12** (escenario del issue, SC-003);
  - las **165 asignaturas-paralelo** de la carga 05 de prueba, sobre las 7 carreras, 12 mallas y 300 asignaturas de `DatosMaestrosDePrueba`, se ofertan todas en un periodo en Planificación y suman **660 horas**: 160 de `550411C02-P-0301`, 80 de `550613A01-P-0301`, 140 de `550713B01-P-0301`, 140 de `550715F02-P-0301`, 20 de `550732E01-D-0301`, 100 de `550811F01-P-0301` y 20 de `551021B01-D-0301` (SC-002);
  - una asignatura-paralelo con horas indicadas que quedaron por encima de su asignatura, porque esta se corrigió después, se marca.

  Crear `OfertaDelPeriodo`, `HorasACubrir` y las excepciones en `JACA/dominio/`, con JavaDoc en español que cite RF-15, RN-18 y el glosario. T003 y T004 pasan.

### Persistencia (data-model.md)

- [X] T005 Prueba primero, en `IACA/AcademicoEsquemaIntegrationTest.java` (ampliar): `asignatura_paralelo` y sus `check` rechazan cada combinación inválida (paralelo en minúsculas, jornada o modalidad fuera del catálogo, estudiantes negativos, horas 0,4, estado desconocido, la misma clave dos veces, un periodo o una asignatura que no existen); `registro_cambio` acepta las 4 acciones nuevas y sigue aceptando las 17 de antes. Tiene que fallar sin la migración.
- [X] T006 Crear `ACA/main/resources/db/migration/academico/V5__oferta_del_periodo.sql` según data-model.md, con un comentario que cite RF-15 y F03-H03. T005 pasa.
- [X] T007 Prueba primero, en `IACA/RepositorioOfertaJdbcIntegrationTest.java`:
  - registra y lee una asignatura-paralelo igual, campo por campo, con y sin horas y sede;
  - `delPeriodo` trae solo las del periodo, ordenadas por asignatura y paralelo;
  - `guardar` cambia cada dato y el estado;
  - `bloquear` toma el candado de la tabla.

  Crear `JACA/puertos/salida/RepositorioOferta.java` y `JACA/adaptadores/salida/RepositorioOfertaJdbc.java`. T007 pasa.

**Checkpoint**: el contrato, el dominio, V5 y el repositorio están listos.

---

## Phase 3: User Story 1 - Registrar la oferta del periodo (Priority: P1) 🎯 MVP

**Goal**: ofertar asignaturas-paralelo en un periodo en Planificación, con sus reglas (FR-001, FR-003 a FR-006, FR-009, FR-010).

**Independent Test**: en un periodo en Planificación, ofertar una asignatura activa en `A_NOCTURNA` y verla con las horas de su asignatura; el registro tiene la acción.

- [X] T008 [US1] Prueba primero, en `PACA/aplicacion/MantenerOfertaServicioTest.java` (Mockito):
  - ofertar bloquea el periodo, las carreras y la oferta, valida con `OfertaDelPeriodo`, guarda y registra `ASIGNATURA_PARALELO_REGISTRADA` con periodo, asignatura y paralelo;
  - una repetida lanza `AsignaturaParaleloRepetida`, y con el periodo fuera de Planificación, `OfertaCerrada`, sin registro;
  - un periodo que no existe lanza `PeriodoNoEncontrado`.

  Crear los puertos de entrada `ConsultarOferta` y `MantenerOferta` y el servicio `MantenerOfertaServicio`. T008 pasa.
- [X] T009 [US1] Prueba primero, en `PACA/adaptadores/entrada/OfertaContratoTest.java`, con un doble en memoria que usa las reglas reales del dominio:
  - `ofertarAsignatura` 201;
  - 400 con el campo `jornada`, `horasSemanales`, `asignatura` y `sede`;
  - 409 `ACADEMICO_ASIGNATURA_PARALELO_REPETIDA` y `ACADEMICO_OFERTA_CERRADA`;
  - 404 con un periodo que no existe;
  - cada respuesta valida contra el contrato.

  `ErroresAcademicoTest` con los 2 códigos nuevos. Crear `JACA/adaptadores/entrada/OfertaControlador.java` y las entradas del catálogo:
  - lecturas con `hasAnyAuthority('PERMISO_OFERTA:LECTURA','PERMISO_DISTRIBUTIVO:LECTURA','PERMISO_CATALOGOS:LECTURA')`;
  - escrituras con `hasAnyAuthority('PERMISO_CATALOGOS:ESCRITURA','PERMISO_DISTRIBUTIVO:ESCRITURA')`.

  T009 pasa.
- [X] T010 [US1] Ampliar `PACA/adaptadores/entrada/PermisosAcademicoTest.java` con `OfertaControlador`: las 5 operaciones nuevas exigen sus permisos alternativos (30 en total). Comprobar una vez que falla si la lectura deja fuera `OFERTA:LECTURA`, restaurarla y anotarlo en la evidencia. Registrar los beans en `ConfiguracionAcademico`, con nombres que no choquen con los de Normativa ni con los de F03-H02. T010 pasa.

**Checkpoint**: la oferta se registra por la API.

---

## Phase 4: User Story 2 - Paralelos y horas a cubrir (Priority: P1)

**Goal**: la consulta de la oferta con las horas a cubrir por asignatura, por carrera y del periodo, y la de una asignatura-paralelo con los datos para la afinidad (FR-007, FR-008; SC-002, SC-003).

- [ ] T011 [US2] Prueba primero, en `PACA/aplicacion/ConsultarOfertaServicioTest.java` (Mockito) y en `OfertaContratoTest` (ampliar):
  - `consultarOferta` trae las asignaturas-paralelo, los totales por asignatura con sus paralelos, por carrera y del periodo, el estado del periodo y `modificable`;
  - **4 horas en 3 paralelos dan 12** (escenario del issue);
  - las retiradas se ven y no suman;
  - `consultarAsignaturaParalelo` trae el ciclo, `troncoComun`, `campoAmplio` con su nombre, `campoEspecifico`, `palabrasClave`, las horas efectivas y si son indicadas;
  - con horas indicadas que quedaron por encima de las de su asignatura (porque se corrigió después), `porEncimaDeLaAsignatura` viene en `true`, y en `false` en los demás casos;
  - 404 con una asignatura-paralelo que no existe.

  Crear `ConsultarOfertaServicio` y ampliar el controlador. T011 pasa.

---

## Phase 5: User Story 3 - Corregir y retirar (Priority: P1)

**Goal**: corregir, retirar y volver a ofertar sin borrar, con su registro (FR-002, FR-003, FR-010).

- [ ] T012 [US3] Prueba primero, en `MantenerOfertaServicioTest` y `OfertaContratoTest` (ampliar):
  - corregir registra `ASIGNATURA_PARALELO_CORREGIDA` con cada cambio, y sin cambios no registra;
  - retirar registra `ASIGNATURA_PARALELO_RETIRADA`, y volver a ofertar `ASIGNATURA_PARALELO_REOFERTADA`;
  - volver a ofertar revalida como corregir; el mismo estado no registra;
  - fuera de Planificación, 409 `ACADEMICO_OFERTA_CERRADA`;
  - `corregirAsignaturaParalelo` y `cambiarEstadoAsignaturaParalelo` validan contra el contrato.

  Ampliar el servicio y el controlador. T012 pasa.

**Checkpoint**: la oferta se mantiene entera por la API.

---

## Phase 6: User Story 4 - La oferta en pantalla (Priority: P1)

**Goal**: «Oferta del periodo» agrupada por carrera, con las horas a cubrir (FR-011; SC-001).

- [ ] T013 [US4] Prueba primero, en `frontend/src/pantallas/oferta/Oferta.test.tsx` (Vitest con la API simulada):
  - **Periodo:** el selector elige por omisión el más reciente en Planificación; si la lista de periodos responde 403, la pantalla muestra «Acceso no autorizado» y no se rompe.
  - **Totales:** el total del periodo; por carrera, su total y una fila por asignatura con su ciclo, su código, su nombre, sus paralelos (jornada, modalidad, estudiantes, horas y estado en español) y sus horas a cubrir; «12 h» con 3 paralelos de 4 h.
  - **Con escritura** (`CATALOGOS:ESCRITURA` o `DISTRIBUTIVO:ESCRITURA`) y el periodo en Planificación:
    - «Ofertar asignatura»: la carrera de las vigentes, la asignatura de sus activas, la jornada de las de la carrera, la modalidad con la de la carrera por omisión, la sede con «La de la carrera» por omisión, y las horas vacías con la pista de las de la asignatura;
    - «Corregir», «Retirar» con confirmación, y «Volver a ofertar».
  - **Errores:** un 400 marca su campo, con el motivo junto a la pista; un 409 muestra su título.
  - **Solo lectura:** sin escritura, o con el periodo fuera de Planificación, sin acciones y con un aviso que dice por qué.
  - **Menú y ruta:** con `[OFERTA:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`.

  Crear la pantalla en `frontend/src/pantallas/oferta/`, su ruta en `rutas.tsx`, su entrada en `Marco.tsx` debajo de «Carreras y mallas» y la lista de lectura en `funcionalidades.ts`. T013 pasa.
- [ ] T014 [US4] Prueba con Playwright, en `frontend/e2e/oferta.spec.ts`: `vicerrectorado.demo` oferta una asignatura de 4 horas en 3 paralelos y ve 12 horas a cubrir; retira uno y ve 8; axe-core sin fallas WCAG 2.1 AA en la oferta y en los diálogos de ofertar, corregir y retirar.

**Checkpoint**: las cuatro historias pasan.

---

## Phase 7: Punta a punta y cierre

- [ ] T015 Crear `deploy/probar_oferta.sh`, con el estilo de `probar_carreras.sh` y los 7 pasos de research §9:
  - **Datos:** registra con un prefijo `P` + 3 dígitos al azar las carreras, mallas y asignaturas de las cargas 02 y 04, con las carreras en la sede matriz que ya tenga la base o en una temporal, como `probar_carreras.sh`, y un periodo `<prefijo>2026-I` en Planificación; luego oferta la carga 05 de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/`, con los códigos de asignatura con prefijo.
  - **Estado del periodo:** lo pone en `EN_APROBACION` directamente en la base para el paso 5, con un comentario que diga por qué.
  - **Tiempo:** mide cuánto tarda `consultarOferta` con las 165 asignaturas-paralelo, lo muestra en la salida y falla si pasa de 300 ms (meta del plan). Anotarlo en la evidencia.
  - **Limpieza:** borra en la base solo lo que creó (la oferta, las asignaturas, mallas y carreras, el periodo y sus registros, y lo que el registro del periodo dejó en Roles y en los eventos, como `probar_periodos.sh`).

  Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces seguidas y anotarlo; `shellcheck` sin observaciones.
- [ ] T016 Correr todo, como en las historias anteriores: backend, SPA, los 11 scripts con el perfil reconstruido, `probar_equipo.sh`, `shellcheck` y Gitleaks sobre un clon. Hacer commit y push, abrir el PR en borrador con `Refs #37` y marcarlo listo después de sus runs.
- [ ] T017 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después:
  1. esperar el despliegue («Sano en N s»), con `academico` en V5;
  2. el autor entra con `vicerrectorado.demo`, oferta en un periodo en Planificación una asignatura en varios paralelos en menos de 1 minuto cada una (SC-001), y ve sus horas a cubrir;
  3. la ve con `consolidacion.demo` en solo lectura;
  4. abrir el PR de evidencia con `Closes #37` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 primero; T003 antes que T004; T005 antes que T006; T007 después de T004 y T006.
- **US1:** T008 después de la fase 2; T009 después de T008; T010 después de T009.
- **US2:** T011 después de T009.
- **US3:** T012 después de T011.
- **US4:** T013 después de T002; T014 después de T012 y T013.
- **Cierre:** T015 después de T012; T016 y T017 al final.

### Parallel Opportunities

- La SPA (T013) puede empezar después de T002, con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el dominio con los datos de prueba, V5 y el repositorio.
2. Ofertar por la API. **Validar:** las 165 asignaturas-paralelo de prueba entran y suman 660 horas.

### Incremental

1. **US2 y US3:** la consulta con los totales, corregir y retirar.
2. **US4:** la pantalla.
3. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- Las franjas horarias (RF-60) no se guardan: son Should y el MVP no maneja horarios (F22).
- **Lo propio (D-64):** el coordinador lee hoy la oferta de todas las carreras y no la mantiene. F03-H05 aplica el filtro de lo propio de `OFERTA` (lectura y escritura) con los cargos. Anotarlo en la evidencia como limitación conocida.
- La carga 5 por archivo se aplica con F01-H04, sobre las mismas reglas.
- Hacer commit por tarea o por grupo lógico en `021-oferta-periodo`, con `Refs #37`.
