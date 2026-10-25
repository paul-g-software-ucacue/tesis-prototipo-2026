---

description: "Tareas de F03-H05: registrar los cargos y los proyectos aprobados"
---

# Tasks: Registrar los cargos y los proyectos aprobados

**Input**: documentos de diseño en `specs/023-cargos-proyectos/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cargos-proyectos.md](contracts/cargos-proyectos.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero. El principio XIV exige, además, que cada regla de campo tenga sus casos válidos, inválidos y en el límite.

**Decisiones**: D-16, D-64, D-65, D-70, D-72, D-73, ADR-012, ADR-015, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-18, RN-12, RN-20 y RN-25 de la ERS; el glosario §7.2 («Cargo», «Requisito del cargo», «Gestión autorizada», «Proyecto aprobado», «Participante», «Proyecto asignable»), aprobado y aplicado el 2026-10-25.

**Organization**:
- **Fundacional:** el contrato, el catálogo y el diccionario; las referencias de Académico; el dominio; la migración V3 y los repositorios.
- **US1:** registrar el cargo de un docente.
- **US2:** advertir los requisitos del cargo.
- **US3:** registrar un proyecto aprobado.
- **US4:** saber si un proyecto es asignable en un periodo.
- **US5:** corregir y finalizar sin borrar.
- **US6:** las pantallas «Cargos» y «Proyectos».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US6.
- Rutas relativas a la raíz. Abreviaturas:
  - `DOC` = `backend/servicios/docentes/src`, `JDOC` = `DOC/main/java/ec/mdd/docentes`, `PDOC` = `DOC/test/java/ec/mdd/docentes` e `IDOC` = `DOC/integrationTest/java/ec/mdd/docentes`;
  - `JACA` = `backend/servicios/academico/src/main/java/ec/mdd/academico` y `PACA` = `backend/servicios/academico/src/test/java/ec/mdd/academico`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura. `ArquitecturaTest` de Docentes y `ModulosTest` del proceso `datos` lo comprueban.
- **Soporte (principio II):** `Cargo` y `Proyecto` son valores con sus reglas; `Referencias` tiene las que miran a Académico. No hay agregado ni eventos.
- **Límites (principio V):** Docentes usa Académico solo por `ec.mdd.academico.puertos.entrada`, sin claves foráneas entre esquemas.
- **Principio VI:** cada lista de valores permitidos vive en una enumeración, igual a su columna de las cargas 8 y 9 del diccionario. Los 22 cargos son los de D-73.
- **Principio VII:** nada se borra. Un cargo se finaliza y un proyecto cambia de estado; cada cambio va a `docentes.registro_cambio`.
- **Datos personales (principio IX):** solo los datos de prueba ficticios de la ERS. Ningún mensaje de excepción ni log lleva una identificación, un nombre ni un correo.
- **Principio XIV:** cada campo de las tablas de la especificación se valida en el contrato, en el dominio y en la SPA. Lo que el diccionario no define no se valida.
- **Errores (principio XIII, D-72):** los 4 códigos nuevos con sus 8 columnas en `contratos/comun/errores.md`; `VerificadorDeCatalogo` lo comprueba.
- **Datos de prueba:** las cargas 01, 03, 08 y 09 de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/`, copiadas a `DOC/test/resources/docentes/datos-de-prueba/`, junto a las 02, 04 y 06 que ya están.
- **Pruebas:**
  - Mockito solo en la aplicación;
  - el adaptador hacia Académico, con el doble de `ConsultarReferencias` escrito a mano que ya existe;
  - los repositorios, con Testcontainers;
  - el contrato, con swagger-request-validator;
  - 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores. La evidencia va en `specs/023-cargos-proyectos/evidencia.md` y los commits llevan `Refs #39`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/023-cargos-proyectos/evidencia.md` con estas secciones:
  - entorno;
  - base: contrato, catálogo, referencias de Académico, dominio, migración y repositorios;
  - una por historia: cargos (US1), requisitos (US2), proyectos (US3), asignables (US4), corregir y finalizar (US5) y pantallas (US6);
  - cierre;
  - equipo de demostración;
  - notas.

  Copiar `01_estructura_organica.csv`, `03_ciclos_academicos.csv`, `08_cargos_autoridades.csv` y `09_proyectos_practicas.csv` a `DOC/test/resources/docentes/datos-de-prueba/`, y anotar su origen en la evidencia.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato, catálogo y diccionario (principio IV, D-72, D-73; contracts/cargos-proyectos.md)

- [X] T002 Ampliar `contratos/docentes/openapi.yaml` según contracts/cargos-proyectos.md:
  - los tags `Cargos` y `Proyectos`, los esquemas y las 7 operaciones;
  - **lecturas** con `x-mdd-permiso: [DOCENTES:LECTURA, CATALOGOS:LECTURA, DISTRIBUTIVO:LECTURA]` (sin `PROYECTOS:LECTURA`, D-64, aclaración 8), y **escrituras** con `[DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`;
  - `ReferenciasDeDocentes` suma `unidades` y `periodos`;
  - las reglas del principio XIV en cada campo: `pattern` `^[0-9A-Z-]{1,30}$` en el código del proyecto y `^[A-Z0-9_-]{1,20}$` en la unidad; `maxLength` 60 en el documento y la resolución, y 250 en el nombre; `HorasSemanales` con `exclusiveMinimum` 0, `maximum` 999.9 y `multipleOf` 0.01; las listas de 1 a 20 carreras y de 1 a 40 participantes, sin `uniqueItems`;
  - un `example` ficticio en cada parámetro;
  - el comentario de cabecera cita F03-H05, RF-18, RN-12, RN-20, D-70 y D-73.

  En `contratos/academico/openapi.yaml`, agregar a `ReferenciasDeAcademico` las listas `unidades` (`UnidadDeReferencia`) y `periodos` (`PeriodoDeReferencia`). Agregar los 4 códigos de research §10 a `contratos/comun/errores.md`, con sus 8 columnas. Regenerar (`openApiGenerate`, `generar:api`) y dejar `verificar:ruptura` en verde: el cambio de Académico es compatible.
- [X] T003 [P] En `backend/servicios/cargas/src/main/resources/cargas/diccionario-de-datos.yaml`, reemplazar la fila del rango del catálogo `CARGOS` por los 16 cargos de D-73, con su gestión en la descripción. Ajustar `DiccionarioYamlTest` y `DiccionarioDeDatosTest`, y comprobar que la plantilla y el diccionario descargables de la carga 8 listan los 22 cargos; anotarlo en la evidencia. `./gradlew :servicios:cargas:test` pasa.

### Referencias de Académico (research §2)

- [X] T004 Prueba primero, en `PACA/aplicacion/ConsultarReferenciasServicioTest.java` (ampliar): `referencias()` trae todas las unidades, activas e inactivas, con su código, nombre, tipo, gestión autorizada y si está activa, y todos los periodos con su código, nombre, inicio y fin, ordenados por código. Crear `UnidadDeReferencia` y `PeriodoDeReferencia` en `JACA/puertos/entrada/`, y ampliar `ReferenciasDeAcademico` y `ConsultarReferenciasServicio` con los repositorios que ya existen. Ampliar `CarrerasContratoTest` (la respuesta de `consultarReferencias` valida contra el contrato) y su doble en memoria. T004 pasa.

### Dominio (research §3 a §7; data-model.md)

- [X] T005 [P] Prueba primero, en `PDOC/dominio/CargoTest.java`, sin dobles, con las referencias de las cargas 01 y 02 de prueba y con docentes de `DocentesDePrueba`. Casos **válidos, inválidos y en el límite** de la tabla «Cargo» de la especificación, cada uno con su campo y su motivo en `DatoInvalido`:
  - un docente en licencia, inactivo, de medio tiempo o de tiempo parcial lanza `DocenteNoHabilitado` (409); uno activo a tiempo completo pasa;
  - un coordinador de carrera sin carrera o con una que no existe; con carrera y sin unidad pasa;
  - cualquier otro cargo sin unidad o con una que no existe o está inactiva;
  - `G07_GESTOR_INVESTIGACION` con la unidad G-INV (gestión G07) pasa, y con G-VIN (G06) no;
  - `MIEMBRO_COMISION` con COM-DIST pasa, y con G-INV no;
  - el fin de funciones igual a la designación, un día antes, exactamente 2 años después (pasa) y 2 años y un día después;
  - el documento vacío, de 60 caracteres (pasa) y de 61;
  - un cargo nuevo queda `VIGENTE`; `corregido` no cambia la clave y devuelve cada cambio con su valor anterior y el nuevo;
  - el mensaje de ningún error contiene la identificación.

  Crear en `JDOC/dominio/` `Cargo`, `TipoDeCargo` (los 22, con su gestión, `exigeCuartoNivel()` y la unidad que pide), `EstadoDeCargo` y `DocenteNoHabilitado`, con JavaDoc en español que cite la carga 8, POL 5.4 y D-73.
- [X] T006 [P] Prueba primero, en `PDOC/dominio/ProyectoTest.java`, sin dobles, con los mismos casos **válidos, inválidos y en el límite** de la tabla «Proyecto aprobado»:
  - el código con minúsculas (se rechaza: en pantalla se pasan a mayúsculas antes), con espacio, con `_`, de 30 caracteres (pasa) y de 31;
  - el nombre vacío, de 250 caracteres y de 251;
  - sin carreras, con una repetida (`carreras[1]`), con una que no existe;
  - la resolución vacía y de 61; el fin de vigencia igual al inicio;
  - el director o un participante que no son docentes registrados (`participantes[2].docente`); un participante repetido;
  - horas de `0`, `0.01`, `2.55`, `99.99` y `999.9` (pasan), `2.50` (pasa: cuenta como `2.5`), y `2.555`, `100.25` (6 caracteres) y `1000` (se rechazan);
  - participantes que suman exactamente las horas aprobadas (pasa) y `0.1` más;
  - cada rol fuera de su enumeración;
  - `corregido` devuelve cada cambio.

  Crear `Proyecto`, `Participante`, `TipoDeProyecto`, `EstadoDeProyecto` y `RolDeParticipante` en `JDOC/dominio/`, con JavaDoc que cite RF-18 y la carga 9.
- [X] T007 Prueba primero, en `PDOC/dominio/ReferenciasTest.java` y `PDOC/dominio/ValoresPermitidosTest.java` (ampliar):
  - `Referencias` resuelve las unidades y los periodos de las cargas 01 y 03 de prueba, y rechaza un código que no existe en su campo;
  - **los 21 cargos de la carga 08 y los 11 proyectos de la carga 09 de prueba**, leídos con `CargosDePrueba` y `ProyectosDePrueba` sobre los 47 docentes, son todos válidos (SC-002);
  - `TipoDeCargo`, `EstadoDeCargo`, `TipoDeProyecto`, `EstadoDeProyecto` y `RolDeParticipante` tienen los mismos valores que su columna en `diccionario-de-datos.yaml`.

  Ampliar `Referencias` en `JDOC/dominio/`. T005 a T007 pasan.

### Persistencia (data-model.md)

- [X] T008 Prueba primero, en `IDOC/CargosYProyectosEsquemaIntegrationTest.java`: `cargo`, `proyecto` y sus `check` rechazan cada combinación inválida, cada una con su propia clave:
  - un tipo, un estado o un rol fuera de su catálogo;
  - un fin de funciones igual a la designación o a más de 2 años;
  - un coordinador de carrera sin carrera, u otro cargo sin unidad;
  - **dos coordinadores vigentes para la misma carrera** (el índice parcial), mientras que uno vigente y uno finalizado pasan;
  - un código de proyecto con minúsculas;
  - horas 0;
  - un fin de vigencia igual al inicio;
  - un cargo o un director con una identificación que no está en `docente`.

  `registro_cambio` acepta las 4 acciones nuevas y las 3 de F03-H04. Tiene que fallar sin la migración.
- [X] T009 Crear `DOC/main/resources/db/migration/docentes/V3__cargos_y_proyectos.sql` según data-model.md, con un comentario que cite RF-18, RN-12, la carga 8, la carga 9 y F03-H05. T008 pasa.
- [X] T010 Prueba primero, en `IDOC/RepositorioCargosJdbcIntegrationTest.java` y `IDOC/RepositorioProyectosJdbcIntegrationTest.java`:
  - cada repositorio guarda y lee igual, campo por campo, y `todos` los trae ordenados por docente o por código;
  - `actualizar` cambia cada dato;
  - `existe` encuentra la clave;
  - `coordinadorVigente(carrera)` excluye el cargo indicado;
  - `bloquear` toma el candado;
  - los proyectos guardan las carreras y los participantes en `jsonb`, con las horas exactas.

  Crear los puertos `RepositorioCargos` y `RepositorioProyectos` en `JDOC/puertos/salida/`, y `RepositorioCargosJdbc` y `RepositorioProyectosJdbc` en `JDOC/adaptadores/salida/`. Agregar las 4 acciones a `CambioRegistrado`. T010 pasa.
- [X] T011 Prueba primero, en `PDOC/adaptadores/salida/AcademicoDesdePuertosTest.java` (ampliar, con el mismo doble escrito a mano): traduce las unidades y los periodos. Ampliar `AcademicoDesdePuertos`. `./gradlew :apps:datos:test` corre `ModulosTest` y pasa. T011 pasa.

**Checkpoint**: el contrato, el catálogo, las referencias, el dominio, V3 y los repositorios están listos.

---

## Phase 3: User Story 1 - Registrar el cargo de un docente (Priority: P1) 🎯 MVP

**Goal**: registrar un cargo con sus reglas (FR-001, FR-003 a FR-005, FR-008, FR-010 a FR-012).

**Independent Test**: registrar al coordinador de una carrera y verlo en la lista; el registro tiene la acción.

- [X] T012 [US1] Prueba primero, en `PDOC/aplicacion/MantenerCargosServicioTest.java` (Mockito):
  - registrar verifica las referencias antes de la transacción;
  - después bloquea la tabla, comprueba la clave, el coordinador vigente y el docente, guarda y registra `CARGO_REGISTRADO` con la clave;
  - una clave repetida lanza `CargoRepetido`, un segundo coordinador vigente `CoordinadorVigente` y un docente no habilitado `DocenteNoHabilitado`, los tres sin registro;
  - un docente que no existe da `DatoInvalido` en `docente`.

  Crear los puertos `ConsultarCargos` y `MantenerCargos`, sus excepciones y `MantenerCargosServicio`. T012 pasa.
- [X] T013 [US1] Prueba primero, en `PDOC/adaptadores/entrada/CargosContratoTest.java`, con dobles en memoria que usan las reglas reales del dominio:
  - `registrarCargo` responde 201;
  - responde 400 con el campo `unidad`, `carrera`, `fechaFin` y `documento`;
  - un cargo fuera del enum da 400 con los valores permitidos;
  - responde 409 `DOCENTES_CARGO_REPETIDO`, `DOCENTES_COORDINADOR_VIGENTE` y `DOCENTES_DOCENTE_NO_HABILITADO`;
  - `listarCargos` filtra por estado y por docente, con los nombres del docente, la unidad y la carrera;
  - cada respuesta valida contra el contrato.

  Ampliar `ErroresDocentes` y `ErroresDocentesTest` (`VerificadorDeCatalogo`, con las 8 columnas). Crear `JDOC/adaptadores/entrada/CargosControlador.java`, con las lecturas y las escrituras de T002 en `hasAnyAuthority`, y sus beans en `ConfiguracionDocentes`. T013 pasa.

---

## Phase 4: User Story 2 - Advertir los requisitos del cargo (Priority: P1)

**Goal**: la advertencia de RN-20 al registrar, al corregir y al listar (FR-006; SC-004).

- [X] T014 [US2] Prueba primero, en `CargoTest` y `CargosContratoTest` (ampliar):
  - **un coordinador de carrera cuyo docente no tiene títulos de cuarto nivel se registra con 201, y su respuesta trae la advertencia `CUARTO_NIVEL` con «Le falta un título de cuarto nivel registrado (RN-20).»** (escenario del issue);
  - lo mismo para un coordinador de sede, un coordinador académico institucional y cada gestor G06 a G21;
  - ninguna advertencia para un rector, un vicerrector, un miembro de comisión ni un docente con una maestría;
  - `listarCargos` la sigue mostrando, y deja de mostrarla cuando la ficha del docente gana un título de cuarto nivel;
  - con `hoy` fijo: un cargo vigente con la fecha de fin de ayer trae `FECHA_FIN_VENCIDA`; con la de hoy, no; uno finalizado, no;
  - un cargo vigente cuyo docente pasa después a licencia o a medio tiempo trae `DOCENTE_NO_HABILITADO`, y registrar uno nuevo para ese docente sigue dando 409.

  Agregar `AdvertenciaDeCargo` y `Cargo.advertencias(Docente, LocalDate hoy)` en el dominio, y llevarlas a la respuesta del controlador, que usa la fecha de hoy. T014 pasa.

**Checkpoint**: los cargos se registran por la API con su advertencia.

---

## Phase 5: User Story 3 - Registrar un proyecto aprobado (Priority: P1)

**Goal**: registrar un proyecto con sus participantes y sus horas (FR-002, FR-003, FR-007, FR-010 a FR-012).

- [X] T015 [US3] Prueba primero, en `PDOC/aplicacion/MantenerProyectosServicioTest.java` (Mockito) y en `PDOC/adaptadores/entrada/ProyectosContratoTest.java`:
  - registrar verifica las referencias y los docentes, bloquea, comprueba el código, guarda y registra `PROYECTO_REGISTRADO`;
  - `registrarProyecto` responde 201;
  - responde 400 con el campo `codigo`, `carreras[1]`, `participantes[2].horas`, `participantes[0].docente`, `director`, `vigenciaFin` y `horasAprobadas`, también cuando los participantes suman más que las aprobadas;
  - responde 409 `DOCENTES_PROYECTO_REPETIDO`;
  - cada respuesta valida contra el contrato.

  Crear los puertos `ConsultarProyectos` y `MantenerProyectos`, sus excepciones, `MantenerProyectosServicio` y `JDOC/adaptadores/entrada/ProyectosControlador.java`, con sus beans. T015 pasa.

---

## Phase 6: User Story 4 - Saber si un proyecto es asignable en un periodo (Priority: P1)

**Goal**: la condición de asignable, con su motivo, contra las fechas del periodo (FR-009; SC-003).

- [X] T016 [US4] Prueba primero, en `ProyectoTest` (ampliar), sin dobles, con los periodos 2026-I (2026-04-06 a 2026-08-07) y 2026-II (2026-10-05 a 2027-02-05) de la carga 03 de prueba:
  - aprobado o en ejecución con la vigencia que se cruza: asignable;
  - **finalizado o suspendido: no asignable por `ESTADO`** (escenario del issue);
  - **una vigencia que termina el día antes del inicio del periodo, o que empieza el día después de su fin: no asignable por `VIGENCIA`** (escenario del issue); la que termina el mismo día del inicio sí cruza;
  - suspendido y fuera de vigencia: los dos motivos;
  - con los 11 proyectos de prueba, los 7 de prácticas son asignables en 2026-I y no en 2026-II, y `VIN-2026-02` lo es en los dos.

  Después, en `PDOC/aplicacion/ConsultarProyectosServicioTest.java` y `ProyectosContratoTest`:
  - `listarProyectos?periodo=2026-I` trae `asignable` y `motivos`;
  - sin periodo, no los trae;
  - un periodo que no existe da 400 con el detalle `periodo`;
  - `consultarProyecto` trae los nombres del director, de los participantes y de las carreras;
  - un proyecto que no existe da 404.

  Crear `Asignabilidad`, `MotivoNoAsignable`, `Proyecto.asignableEn` y `ConsultarProyectosServicio`. T016 pasa.
- [X] T017 [US4] Prueba primero, en `PDOC/adaptadores/entrada/PermisosDocentesTest.java` (ampliar, con `VerificadorDePermisos`): las 7 operaciones nuevas exigen sus permisos alternativos, y **un token con solo `PROYECTOS:LECTURA` recibe 403 en todas** (D-64, aclaración 8). Comprobar una vez que la prueba falla si las lecturas dejan fuera `DISTRIBUTIVO:LECTURA`, restaurarlo y anotarlo en la evidencia. T017 pasa.

**Checkpoint**: los cargos y los proyectos se registran y se consultan por la API.

---

## Phase 7: User Story 5 - Corregir y finalizar sin borrar (Priority: P2)

**Goal**: corregir con las mismas reglas, sin cambiar la clave, con su registro (FR-003, FR-012).

- [X] T018 [US5] Prueba primero, en `MantenerCargosServicioTest`, `MantenerProyectosServicioTest` y en los dos `…ContratoTest` (ampliar):
  - `corregirCargo` con `FINALIZADO` y su fecha de fin registra `CARGO_CORREGIDO` con el anterior y el nuevo; la carrera puede recibir otro coordinador vigente;
  - volver a `VIGENTE` un coordinador cuando la carrera ya tiene otro da 409 `DOCENTES_COORDINADOR_VIGENTE`;
  - sin cambios no se registra nada;
  - `corregirProyecto` cambia los participantes, la vigencia y el estado con las mismas reglas, y registra `PROYECTO_CORREGIDO` con cada campo cambiado;
  - 404 con una clave o un código que no existe;
  - ninguna operación borra;
  - las respuestas validan contra el contrato.

  Ampliar los servicios y los controladores. T018 pasa.

**Checkpoint**: los cargos y los proyectos se mantienen enteros por la API.

---

## Phase 8: User Story 6 - Las pantallas de cargos y proyectos (Priority: P2)

**Goal**: «Cargos» y «Proyectos», con la validación en pantalla del principio XIV (FR-013; SC-001).

- [X] T019 [US6] Mover `filtrar` y `Filtro` de `frontend/src/pantallas/docentes/validacionDeDocente.ts` a `frontend/src/pantallas/comun/validacion.ts`, sin cambiar lo que hacen. Las pruebas de `validacionDeDocente.test.ts` y `Docentes.test.tsx` siguen en verde. Después, prueba primero en `frontend/src/pantallas/cargos/validacionDeCargo.test.ts` y `frontend/src/pantallas/proyectos/validacionDeProyecto.test.ts`, con los mismos casos válidos, inválidos y en el límite de T005 y T006:
  - el código del proyecto se pasa a mayúsculas y pierde lo que no es letra, dígito o guion;
  - las horas admiten hasta dos decimales y 5 caracteres;
  - el documento y la resolución cortan en 60 caracteres;
  - cada motivo dice qué hacer.

  Crear `validacionDeCargo.ts` y `validacionDeProyecto.ts`. T019 pasa.
- [X] T020 [US6] Prueba primero, en `frontend/src/pantallas/cargos/Cargos.test.tsx` y `frontend/src/pantallas/proyectos/Proyectos.test.tsx` (Vitest con la API simulada):
  - **Cargos:**
    - la lista con el docente, el cargo en español, la unidad o la carrera, las fechas, el estado y la advertencia;
    - los filtros por estado y por docente;
    - el diálogo de registrar y corregir: la unidad se elige de las que corresponden al cargo elegido, y la carrera solo aparece para el coordinador de carrera;
    - registrar un coordinador sin cuarto nivel muestra la advertencia que devuelve el servidor;
    - «Finalizar».
  - **Proyectos:**
    - el periodo elegido, por omisión el más reciente;
    - la lista con «Asignable» o su motivo;
    - el diálogo con las carreras y los participantes como listas editables;
    - la suma de horas que pasa las aprobadas se marca antes de enviar.
  - **En las dos:**
    - un 400 marca su campo y, en una lista, su fila;
    - un 409 muestra su título;
    - sin escritura, la pantalla es de solo lectura y dice por qué;
    - el menú muestra «Cargos» y «Proyectos» después de «Docentes» con las tres lecturas, y no con solo «Proyectos: lectura».

  Crear las pantallas, sus rutas `/cargos` y `/proyectos`, sus entradas en `Marco.tsx` y sus listas en `funcionalidades.ts`. Las etiquetas y los ayudantes van en módulos `.ts`. T020 pasa.
- [X] T021 [US6] Prueba con Playwright, en `frontend/e2e/cargos-proyectos.spec.ts`:
  - `datos.demo` registra un coordinador sin cuarto nivel y ve la advertencia;
  - registra un proyecto y lo ve asignable en 2026-I y no asignable en 2026-II;
  - axe-core sin fallas WCAG 2.1 AA en las dos listas, en los diálogos y en un diálogo con los campos marcados en rojo.

**Checkpoint**: las seis historias pasan.

---

## Phase 9: Punta a punta y cierre

- [X] T022 Crear `deploy/probar_cargos_proyectos.sh`, con el estilo de `probar_docentes.sh`:
  - **Datos:** registra con un prefijo `P` y 3 dígitos al azar lo que necesita de las cargas 01, 02, 03 y 06 de prueba: unidades, carreras, periodos y docentes, como `probar_docentes.sh` y `probar_estructura.sh`. Después registra los 21 cargos y los 11 proyectos con `datos.demo`. Si algo ya existe en la base, el script falla sin tocarlo.
  - **Pasos:**
    - la advertencia de un coordinador sin cuarto nivel;
    - los cuatro 409;
    - un 400 por horas que pasan las aprobadas;
    - los asignables en 2026-I y en 2026-II con sus motivos;
    - un proyecto suspendido;
    - un cargo finalizado y otro coordinador para su carrera;
    - `consolidacion.demo` lee y recibe 403 al escribir;
    - un usuario con solo «Proyectos: lectura», como el Gestor de unidad, recibe 403;
    - un usuario sin lectura recibe 403.
  - **Tiempo:** mide `listarProyectos` con un periodo, lo muestra y falla si pasa de 500 ms (la meta es 300 ms; el resto es holgura). Anotarlo en la evidencia.
  - **Limpieza:** borra en la base solo lo que creó, con sus registros, también la copia de los periodos de prueba en Roles, como `probar_periodos.sh`.

  Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces seguidas y anotarlo; `shellcheck` sin observaciones.
- [X] T023 Correr todo, como en las historias anteriores:
  - backend y SPA;
  - los 13 scripts con el perfil reconstruido;
  - `probar_equipo.sh`;
  - `shellcheck` y Gitleaks sobre un clon.

  Después de `probar_cargos_proyectos.sh`, buscar una cédula de prueba en `docker compose logs gateway datos` y anotar en la evidencia que no aparece (principio IX). Comprobar los criterios de terminado de la sección 6 de la constitución 2.2.0 y anotarlos en la evidencia:
  - las validaciones de tipo y largo, probadas;
  - los errores, mapeados al catálogo;
  - los mensajes, accionables;
  - los resultados, documentados.

  Hacer commit y push, abrir el PR en borrador con `Refs #39` y marcarlo listo después de sus runs.
- [ ] T024 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después:
  1. esperar el despliegue («Sano en N s»), con `docentes` en V3;
  2. el autor entra con `datos.demo`, registra un coordinador y un gestor (uno sin cuarto nivel, para ver la advertencia) y un proyecto, y consulta los asignables de un periodo en menos de 1 minuto (SC-001);
  3. los ve con `consolidacion.demo` en solo lectura;
  4. abrir el PR de evidencia con `Closes #39` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:**
  - T002 primero; T003 en cualquier orden después de T002;
  - T004 después de T002;
  - T005 y T006 en cualquier orden; T007 después de ambos y de T003;
  - T008 antes que T009; T010 después de T007 y T009;
  - T011 después de T004 y T007.
- **US1:** T012 después de la fase 2; T013 después de T012.
- **US2:** T014 después de T013.
- **US3:** T015 después de la fase 2.
- **US4:** T016 después de T015; T017 después de T013 y T016.
- **US5:** T018 después de T014 y T016.
- **US6:** T019 después de T002; T020 después de T019; T021 después de T018 y T020.
- **Cierre:** T022 después de T018; T023 y T024 al final.

### Parallel Opportunities

- T005 y T006 tocan archivos distintos; T003 no depende del dominio.
- Los cargos (US1, US2) y los proyectos (US3, US4) son independientes hasta US5.
- La SPA (T019 y T020) puede empezar después de T002, con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato y el catálogo, las referencias de Académico, el dominio con los datos de prueba, V3 y los repositorios.
2. Registrar un cargo por la API. **Validar:** los 21 cargos de prueba son válidos y un segundo coordinador vigente se rechaza.

### Incremental

1. **US2:** la advertencia de RN-20.
2. **US3 y US4:** los proyectos y su condición de asignable.
3. **US5:** corregir y finalizar.
4. **US6:** las pantallas.
5. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- El número de gestores por tipo de IST (RN-10), el aviso del coordinador académico institucional (RN-09), las horas de gestión por cargo y el máximo de 16 h de docencia con proyecto son del motor (F06), no de esta historia.
- Las cargas 8 y 9 por archivo y la confirmación «sin proyectos» se aplican con F01-H04 y F01-H05, sobre las mismas reglas.
- El filtro «solo lo propio» del Gestor de unidad queda para F08.
- Hacer commit por tarea o por grupo lógico en `023-cargos-proyectos`, con `Refs #39`.
