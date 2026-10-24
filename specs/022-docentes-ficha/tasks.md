---

description: "Tareas de F03-H04: mantener los docentes con su ficha académica"
---

# Tasks: Mantener los docentes con su ficha académica

**Input**: documentos de diseño en `specs/022-docentes-ficha/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/docentes.md](contracts/docentes.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-16, D-65, D-69, D-70, ADR-012, ADR-015, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-16, RF-17 y RN-01 de la ERS; el glosario §7.1 («Docente», «Tipo de personal», «Categoría de escalafón», «Dedicación», «Condición especial», «Ficha académica», «Carrera base», «Asignatura afín declarada», «Estado laboral», «Campo de conocimiento»), aprobado y aplicado el 2026-10-24.

**Organization**:
- **Fundacional:** el contrato, el puerto de referencias de Académico, el dominio, la migración V2 y los repositorios.
- **US1:** registrar un docente.
- **US2:** consultar y buscar.
- **US3:** corregir los datos y la ficha.
- **US4:** cambiar el estado laboral.
- **US5:** la pantalla «Docentes».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US5.
- Rutas relativas a la raíz. Abreviaturas:
  - `DOC` = `backend/servicios/docentes/src`, `JDOC` = `DOC/main/java/ec/mdd/docentes`, `PDOC` = `DOC/test/java/ec/mdd/docentes` e `IDOC` = `DOC/integrationTest/java/ec/mdd/docentes`;
  - `JACA` = `backend/servicios/academico/src/main/java/ec/mdd/academico` y `PACA` = `backend/servicios/academico/src/test/java/ec/mdd/academico`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura. `ArquitecturaTest` de Docentes y `ModulosTest` del proceso `datos` lo comprueban.
- **Soporte (principio II):** `Docente` y su `FichaAcademica` son valores con sus reglas; `Referencias` tiene las que miran a Académico; sin agregado ni eventos.
- **Límites (principio V):** Docentes usa Académico solo por `ec.mdd.academico.puertos.entrada`, sin claves foráneas entre esquemas.
- **Principio VI:** cada lista de valores permitidos vive en una enumeración, igual a la carga 6 del diccionario; los campos amplios son los de Académico.
- **Principio VII:** nada se borra (estado laboral); cada cambio, en `docentes.registro_cambio`.
- **Datos personales (principio IX):** solo los datos de prueba ficticios de la ERS. Ningún mensaje de excepción ni log lleva una identificación, un correo ni un teléfono.
- **Datos de prueba:** las cargas 02, 04 y 06 de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/`, copiadas a `DOC/test/resources/docentes/datos-de-prueba/`.
- **Pruebas:** Mockito solo en la aplicación; el adaptador hacia Académico, con un doble de `ConsultarReferencias` escrito a mano, como `NormativaDesdePuertosTest`; repositorios con Testcontainers; contrato con swagger-request-validator; 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores; la evidencia va en `specs/022-docentes-ficha/evidencia.md` y los commits llevan `Refs #38`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/022-docentes-ficha/evidencia.md` con estas secciones: entorno; base (contrato, referencias de Académico, dominio, migración y repositorios); registrar (US1); consultar y buscar (US2); corregir (US3); estado laboral (US4); pantalla (US5); cierre; equipo de demostración; notas. Copiar `02_carreras.csv`, `04_asignaturas_malla.csv` y `06_docentes.csv` a `DOC/test/resources/docentes/datos-de-prueba/`, con su origen en la evidencia.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV, ADR-015, contracts/docentes.md)

- [X] T002 Crear `contratos/docentes/openapi.yaml` según contracts/docentes.md y `backend/plantillas/README.md` («Agregar un contrato a un servicio»):
  - OpenAPI 3.0.3, `security: [bearer: []]` global, el tag `Docentes`, los esquemas y las 6 operaciones;
  - las de lectura con `x-mdd-permiso: [DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` y las de escritura con `[DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`;
  - las respuestas de error con `$ref` a `../comun/errores.yaml` y `../comun/seguridad.yaml`;
  - `example` en cada parámetro (`0109990010`, ficticio);
  - en `DocenteNuevo`, obligatorios los campos obligatorios de la carga 6, y opcionales `segundoApellido`, `segundoNombre`, `telefono`, `finContrato`, `condicionEspecial` y las listas de la ficha;
  - el comentario de cabecera cita F03-H04, RF-16, RF-17, RN-01 y D-70.

  Agregar a `contratos/academico/openapi.yaml` la operación `consultarReferencias` según contracts/docentes.md («Cambio en el contrato de Académico»). Agregar los 2 códigos a `contratos/comun/errores.md`. En `backend/servicios/docentes/build.gradle`, agregar `id 'mdd.contrato'` y las dependencias de plan.md, cada una con su comentario. En la SPA, agregar `docentes` a `generar:api` de `frontend/package.json` y `apiDocentes` a `frontend/src/api/cliente.ts`. Regenerar (`openApiGenerate`, `generar:api`); `verificar:ruptura` en verde. Los `summary` con «: » van entre comillas.

### Referencias de Académico (research §4)

- [ ] T003 Prueba primero, en `PACA/aplicacion/ConsultarReferenciasServicioTest.java` (Mockito): `referencias()` trae todas las carreras (código, nombre, siglas, en cualquier estado), todas las asignaturas (código, nombre, carrera, en cualquier estado) y los 11 campos amplios, ordenados por código. Crear en `JACA/puertos/entrada/` el puerto `ConsultarReferencias` y los *records* `ReferenciasDeAcademico`, `CarreraDeReferencia`, `AsignaturaDeReferencia` y `CampoAmplioDeReferencia`, con JavaDoc que diga que es la API publicada para Docentes (F03-H04). Crear `JACA/aplicacion/ConsultarReferenciasServicio.java` con los repositorios que ya existen, y su bean en `ConfiguracionAcademico`. Prueba primero también en `PACA/adaptadores/entrada/ReferenciasContratoTest.java` (200 validado contra el contrato) y ampliar `PermisosAcademicoTest` a 31 operaciones; crear `JACA/adaptadores/entrada/ReferenciasControlador.java` con `hasAnyAuthority('PERMISO_CATALOGOS:LECTURA','PERMISO_DISTRIBUTIVO:LECTURA','PERMISO_DOCENTES:LECTURA')`. T003 pasa.

### Dominio (research §1 a §3, §6, §9; data-model.md)

- [ ] T004 [P] Prueba primero, en `PDOC/dominio/IdentificacionTest.java` y `PDOC/dominio/DocenteTest.java`, sin dobles, con el año actual como parámetro. Cada regla de research §3 con su campo y su motivo en español en `DatoInvalido`:
  - cédula con 9 u 11 dígitos, con letras, con provincia 00 o 25, o con el dígito verificador mal; pasaporte vacío, de 14 caracteres o con signos;
  - primer apellido o primer nombre vacíos o de 61 caracteres; los nombres se guardan en mayúsculas y sin espacios en los extremos;
  - correo sin `@` o de 101 caracteres; se guarda en minúsculas; teléfono con letras o de 6 u 11 dígitos;
  - titular con `NO_APLICA`, y ocasional con `AGREGADO_1`;
  - horas contratadas vacías, 0 o 41; `TP` con 40 horas **se acepta**, porque la coherencia con la dedicación es de la normativa (RN-01, principio VI);
  - ocasional sin fin de contrato; fin igual o anterior al inicio;
  - un docente nuevo queda `ACTIVO`; `corregido` no cambia la identificación ni el estado y devuelve cada cambio con su valor anterior y el nuevo; `conEstado` cambia solo el estado; el mensaje de ningún `DatoInvalido` contiene la identificación, el correo ni el teléfono.

  Crear en `JDOC/dominio/` `Identificacion`, `NombresDelDocente`, `Docente`, `DatoInvalido`, `CambioDeValor`, `Cambios` y las enumeraciones de data-model.md, con JavaDoc en español que cite RF-16, RN-01 y el glosario.
- [ ] T005 [P] Prueba primero, en `PDOC/dominio/FichaAcademicaTest.java`, sin dobles:
  - el título de tercer nivel vacío o de 151 caracteres, el registro SENESCYT de 31, un campo amplio de 3 dígitos y un específico que no empieza por el amplio;
  - un título de cuarto nivel con año 1949 o el año siguiente al actual; un curso con 0 horas o 2001; una experiencia con 0 años o 61;
  - una lista de 21 elementos; una asignatura afín repetida;
  - el campo de cada error dice el elemento (`ficha.cursos[1].horas`);
  - el campo amplio de un título de cuarto nivel, de un curso o de una experiencia son los dos primeros dígitos de su específico.

  Crear `FichaAcademica`, `TituloTercerNivel`, `TituloCuartoNivel`, `Curso`, `ExperienciaLaboral` y `CampoConocimiento` en `JDOC/dominio/`, con JavaDoc que cite RF-17.
- [ ] T006 Prueba primero, en `PDOC/dominio/ReferenciasTest.java`, `PDOC/dominio/BusquedaDeDocentesTest.java` y `PDOC/dominio/ValoresPermitidosTest.java`, sin dobles:
  - `Referencias.verificar` rechaza una carrera base inexistente (en `carreraBase`), una asignatura afín inexistente (en `ficha.asignaturasAfines[i]`) y un campo amplio fuera de los 11 (en `ficha.tercerNivel.campoAmplio`), y acepta una carrera en cierre;
  - `BusquedaDeDocentes` encuentra «peralta», «PERALTA» y «Peraltá» en «PERALTA MALDONADO JUAN ANDRES», encuentra por el inicio de la identificación, filtra por carrera y por estado, y sin criterios trae todos;
  - **los 47 docentes de la carga 06 de prueba**, leídos con un `DocentesDePrueba` de prueba sobre las referencias de las cargas 02 y 04 de prueba, son todos válidos, y cada uno se encuentra por su cédula (SC-002);
  - cada enumeración tiene los mismos valores que su columna de la carga 6 en `diccionario-de-datos.yaml` (leído como en `ValoresPermitidosTest` de Académico).

  Crear `Referencias` y `BusquedaDeDocentes` en `JDOC/dominio/`. T004 a T006 pasan.

### Persistencia (data-model.md)

- [ ] T007 Prueba primero, en `IDOC/DocentesEsquemaIntegrationTest.java` (ampliar): `docente` y sus `check` rechazan cada combinación inválida (tipo de identificación, tipo de personal, categoría, dedicación, condición especial o estado fuera de su catálogo, horas 0 o 41, fin de contrato igual al inicio, un específico que no empieza por el amplio, un correo repetido con otras mayúsculas, la misma identificación dos veces); `registro_cambio` acepta las 3 acciones y rechaza otra. Tiene que fallar sin la migración.
- [ ] T008 Crear `DOC/main/resources/db/migration/docentes/V2__docentes.sql` según data-model.md, con un comentario que cite RF-16, RF-17 y F03-H04. T007 pasa.
- [ ] T009 Prueba primero, en `IDOC/RepositorioDocentesJdbcIntegrationTest.java`:
  - guarda y lee un docente igual, campo por campo, con la ficha completa y con la ficha mínima;
  - `todos` los trae ordenados por apellidos y nombres;
  - `actualizar` cambia cada dato, la ficha y el estado;
  - `existeCorreo` no distingue mayúsculas y excluye al docente indicado;
  - `bloquear` toma el candado de la tabla;
  - `RegistroDeCambiosJdbc` guarda la acción y el detalle.

  Crear `JDOC/puertos/salida/RepositorioDocentes.java` y `RegistroDeCambios.java`, y `JDOC/adaptadores/salida/RepositorioDocentesJdbc.java` y `RegistroDeCambiosJdbc.java` (JdbcClient y Jackson para el `jsonb`). T009 pasa.
- [ ] T010 Prueba primero, en `PDOC/adaptadores/salida/AcademicoDesdePuertosTest.java` (con un doble de `ConsultarReferencias` escrito a mano, sin Mockito, como `NormativaDesdePuertosTest`): traduce `ReferenciasDeAcademico` a `Referencias`. Crear `JDOC/puertos/salida/ReferenciasAcademicas.java` y `JDOC/adaptadores/salida/AcademicoDesdePuertos.java`, y agregar `implementation project(':servicios:academico')` a Docentes con su comentario. `./gradlew :apps:datos:test` corre `ModulosTest` y pasa. T010 pasa.

**Checkpoint**: el contrato, las referencias, el dominio, V2 y los repositorios están listos.

---

## Phase 3: User Story 1 - Registrar un docente (Priority: P1) 🎯 MVP

**Goal**: registrar un docente con su ficha, con sus reglas (FR-001 a FR-006, FR-009, FR-010).

**Independent Test**: registrar un docente ocasional a tiempo completo con su ficha y verlo; el registro tiene la acción.

- [ ] T011 [US1] Prueba primero, en `PDOC/aplicacion/MantenerDocentesServicioTest.java` (Mockito):
  - registrar bloquea la tabla, comprueba la identificación y el correo, verifica las referencias, guarda y registra `DOCENTE_REGISTRADO` con la identificación;
  - una identificación repetida lanza `DocenteRepetido`, y un correo de otro, `CorreoRepetido`, sin registro;
  - una carrera base inexistente lanza `DatoInvalido` sin guardar.

  Crear los puertos de entrada `ConsultarDocentes` y `MantenerDocentes`, sus excepciones y `MantenerDocentesServicio`. T011 pasa.
- [ ] T012 [US1] Prueba primero, en `PDOC/adaptadores/entrada/DocentesContratoTest.java`, con dobles en memoria que usan las reglas reales del dominio:
  - `registrarDocente` 201;
  - 400 con el campo `horasContratadas`, `identificacion`, `carreraBase` y `ficha.cursos[0].horas`;
  - **una dedicación que no existe da 400 y el detalle dice los valores permitidos** (escenario del issue);
  - 409 `DOCENTES_DOCENTE_REPETIDO` y `DOCENTES_CORREO_REPETIDO`;
  - cada respuesta valida contra el contrato.

  `ErroresDocentesTest` con `VerificadorDeCatalogo`. Crear `JDOC/adaptadores/entrada/DocentesControlador.java` y `ErroresDocentes.java`:
  - lecturas con `hasAnyAuthority('PERMISO_DOCENTES:LECTURA','PERMISO_DISTRIBUTIVO:LECTURA','PERMISO_CATALOGOS:LECTURA')`;
  - escrituras con `hasAnyAuthority('PERMISO_DOCENTES:ESCRITURA','PERMISO_CATALOGOS:ESCRITURA','PERMISO_DISTRIBUTIVO:ESCRITURA')`.

  Crear `JDOC/infraestructura/ConfiguracionDocentes.java` con los beans. T012 pasa.
- [ ] T013 [US1] Prueba primero, en `PDOC/adaptadores/entrada/PermisosDocentesTest.java` con `VerificadorDePermisos`: las 6 operaciones exigen sus permisos alternativos. Comprobar una vez que falla si la escritura deja fuera `DOCENTES:ESCRITURA`, restaurarla y anotarlo en la evidencia. T013 pasa.

**Checkpoint**: un docente se registra por la API.

---

## Phase 4: User Story 2 - Consultar y buscar (Priority: P1)

**Goal**: buscar por nombre o identificación y consultar la ficha con los nombres de Académico (FR-007, FR-008; SC-001, SC-003).

- [ ] T014 [US2] Prueba primero, en `PDOC/aplicacion/ConsultarDocentesServicioTest.java` (Mockito) y en `DocentesContratoTest` (ampliar):
  - `buscarDocentes` con `texto`, `carrera` y `estado` trae los resúmenes que coinciden, con el nombre de la carrera base, ordenados por apellidos;
  - `consultarDocente` trae todos los datos y la ficha; **cada título trae su campo amplio con su nombre y su específico, y cada curso sus horas** (escenario del issue);
  - cada asignatura afín trae su nombre;
  - 404 con un docente que no existe;
  - `consultarReferenciasDeDocentes` trae las carreras, las asignaturas y los 11 campos amplios.

  Crear `ConsultarDocentesServicio` y ampliar el controlador. T014 pasa.

---

## Phase 5: User Story 3 - Corregir los datos y la ficha (Priority: P1)

**Goal**: corregir con las mismas reglas, sin cambiar la identificación, con su registro (FR-003, FR-006, FR-010).

- [ ] T015 [US3] Prueba primero, en `MantenerDocentesServicioTest` y `DocentesContratoTest` (ampliar):
  - corregir registra `DOCENTE_CORREGIDO` con cada campo cambiado (una lista, entera antes y después), y sin cambios no registra;
  - agregar un curso y quitar una experiencia quedan así;
  - el correo de otro docente da 409 `DOCENTES_CORREO_REPETIDO`; el propio, no;
  - una asignatura afín inexistente da 400 con su elemento, y el docente queda como estaba;
  - 404 con un docente que no existe;
  - `corregirDocente` valida contra el contrato.

  Ampliar el servicio y el controlador. T015 pasa.

---

## Phase 6: User Story 4 - Cambiar el estado laboral (Priority: P1)

**Goal**: licencia, inactivo y vuelta a activo, sin borrar (FR-003, FR-010).

- [ ] T016 [US4] Prueba primero, en `MantenerDocentesServicioTest` y `DocentesContratoTest` (ampliar): cambiar de `ACTIVO` a `LICENCIA`, a `INACTIVO` y de vuelta a `ACTIVO` registra `DOCENTE_ESTADO_CAMBIADO` con el anterior y el nuevo; el mismo estado no registra; un estado fuera del enum da 400 con los valores permitidos; 404 con un docente que no existe; `cambiarEstadoDocente` valida contra el contrato. Ampliar el servicio y el controlador. T016 pasa.

**Checkpoint**: los docentes se mantienen enteros por la API.

---

## Phase 7: User Story 5 - Los docentes en pantalla (Priority: P1)

**Goal**: «Docentes» con la lista, la ficha y el formulario (FR-011; SC-001).

- [ ] T017 [US5] Prueba primero, en `frontend/src/pantallas/docentes/Docentes.test.tsx` (Vitest con la API simulada):
  - **Lista:** el buscador filtra por nombre o identificación; los filtros por carrera base y estado; «N docentes»; cada fila con el nombre completo, la identificación, la carrera, el tipo de personal, la dedicación con sus horas y el estado, en español.
  - **Ficha:** al abrir un docente, sus datos y su ficha; cada título con su campo amplio (código y nombre) y su específico; cada curso con sus horas; las asignaturas afines con su nombre.
  - **Con escritura** (`DOCENTES:ESCRITURA`, `CATALOGOS:ESCRITURA` o `DISTRIBUTIVO:ESCRITURA`):
    - «Registrar docente» con sus secciones; las horas contratadas son obligatorias, con la pista «De 1 a 40. Tiempo completo suele ser 40; medio tiempo, 20.»; la categoría solo se activa para un titular; el fin de contrato es obligatorio para un ocasional;
    - las listas de la ficha agregan y quitan filas; las asignaturas afines se eligen de las de Académico, agrupadas por carrera;
    - «Corregir» con los datos actuales y la identificación fija; «Cambiar estado».
  - **Errores:** un 400 marca su campo y, en una lista, su fila, con el motivo; un 409 muestra su título.
  - **Solo lectura:** sin escritura, sin acciones y con un aviso que dice por qué.
  - **Menú y ruta:** «Docentes» con `[DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`.

  Crear la pantalla en `frontend/src/pantallas/docentes/` (las etiquetas y los ayudantes en módulos `.ts`, por la regla `react-refresh/only-export-components`), reusando `frontend/src/pantallas/comun/`; su ruta `/docentes` en `rutas.tsx`, su entrada en `Marco.tsx` después de «Oferta del periodo» y la lista de lectura en `funcionalidades.ts`. T017 pasa.
- [ ] T018 [US5] Prueba con Playwright, en `frontend/e2e/docentes.spec.ts`: `datos.demo` registra un docente con un curso, lo busca por su apellido en minúsculas y abre su ficha con las horas del curso; axe-core sin fallas WCAG 2.1 AA en la lista, la ficha y los diálogos de registrar y cambiar estado.

**Checkpoint**: las cinco historias pasan.

---

## Phase 8: Punta a punta y cierre

- [ ] T019 Crear `deploy/probar_docentes.sh`, con el estilo de `probar_oferta.sh`:
  - **Datos:** registra con un prefijo `P` + 3 dígitos al azar las carreras y asignaturas de las cargas 02 y 04 que usan los docentes (en la sede matriz que ya tenga la base o en una temporal, como `probar_carreras.sh`) y luego los 47 docentes de la carga 06, con `datos.demo`, con las carreras y asignaturas con prefijo. Las identificaciones de prueba ya son ficticias; si alguna existe en la base, el script falla sin tocarla.
  - **Pasos:** cada docente se encuentra por su cédula; la búsqueda «peralta» lo encuentra; una corrección; un 400 con los valores permitidos de la dedicación; los dos 409; licencia y vuelta a activo; `consolidacion.demo` lee y recibe 403 al escribir; un usuario sin lectura recibe 403.
  - **Tiempo:** mide cuánto tarda `buscarDocentes` con los 47, lo muestra y falla si pasa de 500 ms (la meta del plan es 300 ms; el resto es holgura). Anotarlo en la evidencia.
  - **Limpieza:** borra en la base solo lo que creó (los docentes y sus registros, las asignaturas, mallas y carreras con prefijo y sus registros).

  Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces seguidas y anotarlo; `shellcheck` sin observaciones.
- [ ] T020 Correr todo, como en las historias anteriores: backend, SPA, los 12 scripts con el perfil reconstruido, `probar_equipo.sh`, `shellcheck` y Gitleaks sobre un clon. Después de `probar_docentes.sh`, buscar una cédula de prueba en `docker compose logs gateway datos` y anotar en la evidencia que no aparece (principio IX). Hacer commit y push, abrir el PR en borrador con `Refs #38` y marcarlo listo después de sus runs.
- [ ] T021 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después:
  1. esperar el despliegue («Sano en N s»), con `docentes` en V2;
  2. el autor entra con `datos.demo`, registra un docente con su ficha, lo busca por nombre y por cédula y abre su ficha en menos de 30 s (SC-001), y corrige su dedicación;
  3. lo ve con `consolidacion.demo` en solo lectura;
  4. abrir el PR de evidencia con `Closes #38` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 primero; T003 después de T002; T004 y T005 en cualquier orden; T006 después de ambos; T007 antes que T008; T009 después de T006 y T008; T010 después de T003 y T006.
- **US1:** T011 después de la fase 2; T012 después de T011; T013 después de T012.
- **US2:** T014 después de T012.
- **US3:** T015 después de T014.
- **US4:** T016 después de T015.
- **US5:** T017 después de T002; T018 después de T016 y T017.
- **Cierre:** T019 después de T016; T020 y T021 al final.

### Parallel Opportunities

- T004 y T005 tocan archivos distintos.
- La SPA (T017) puede empezar después de T002, con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, las referencias de Académico, el dominio con los datos de prueba, V2 y los repositorios.
2. Registrar por la API. **Validar:** los 47 docentes de prueba son válidos y se encuentran por su cédula.

### Incremental

1. **US2, US3 y US4:** la búsqueda y la ficha, corregir y el estado laboral.
2. **US5:** la pantalla.
3. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- Que el ocasional sea solo TC o MT (POL 5.1), que el contrato cubra el periodo y las horas que cambia la condición especial no se aplican aquí: son de la normativa y del periodo (spec, supuestos).
- La carga 6 por archivo se aplica con F01-H04, sobre las mismas reglas.
- La validación de las asignaturas afines por el coordinador y su efecto en el puntaje quedan para F06-H01.
- Hacer commit por tarea o por grupo lógico en `022-docentes-ficha`, con `Refs #38`.
