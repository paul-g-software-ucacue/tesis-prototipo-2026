---

description: "Tareas de F01-H03: validar las reglas de los datos e informe por fila"
---

# Tasks: Validar las reglas de los datos e informe por fila

**Input**: documentos de diseño en `specs/024-validar-reglas-carga/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cargas.md](contracts/cargas.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero. El principio XIV exige casos válidos, inválidos y en el límite de cada regla.

**Decisiones**: D-65, D-70, D-71, D-72, D-73, MD-13, ADR-012, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-07, RF-08, RF-09 y RNF-SEG-04 de la ERS; el glosario §7 («Carga», «Diccionario de datos», «Informe de validación de la carga»), sin cambios.

**Organization**:
- **Fundacional:** el contrato, el intérprete de las reglas del diccionario y el puerto de Docentes.
- **US1:** las reglas de cada fila.
- **US2:** las referencias.
- **US3:** el informe en CSV.
- **US4:** corregir y reintentar.
- **US5:** la pantalla.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US5.
- Rutas relativas a la raíz. Abreviaturas:
  - `CAR` = `backend/servicios/cargas/src`, `JCAR` = `CAR/main/java/ec/mdd/cargas` y `PCAR` = `CAR/test/java/ec/mdd/cargas`;
  - `JDOC` = `backend/servicios/docentes/src/main/java/ec/mdd/docentes` y `PDOC` = `backend/servicios/docentes/src/test/java/ec/mdd/docentes`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura. `ArquitecturaTest` de Cargas y `ModulosTest` del proceso `datos` lo comprueban.
- **Principio VI:** todas las reglas salen de `diccionario-de-datos.yaml`. Lo que se traduce a mano (las 5 condiciones, los patrones del formato, la clave compuesta, los destinos de las `FK`) se escribe una sola vez y se prueba contra el diccionario real.
- **Principio V:** Cargas usa solo `ec.mdd.academico.puertos.entrada` y `ec.mdd.docentes.puertos.entrada`.
- **Principio IX:** nada se guarda ni se registra en los logs; los motivos no repiten los valores; solo los datos de prueba ficticios de la ERS.
- **Datos de prueba:** los 10 archivos de `CAR/test/resources/cargas/datos-de-prueba/`.
- **Pruebas:**
  - Mockito solo en la aplicación;
  - el adaptador de referencias, con dobles escritos a mano de los dos puertos;
  - el contrato, con swagger-request-validator;
  - 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores. La evidencia va en `specs/024-validar-reglas-carga/evidencia.md` y los commits llevan `Refs #29`.

---

## Phase 1: Setup

- [ ] T001 Crear `specs/024-validar-reglas-carga/evidencia.md` con estas secciones (y en notas, la fila 22 de la carga 07 completada con 0 y 0: la copia de la ERS y `V2__politica_2024.sql` de Normativa siguen con las celdas vacías o en nulo):
  - entorno;
  - base: contrato, intérprete y puerto de Docentes;
  - una por historia: reglas (US1), referencias (US2), informe (US3), reintentar (US4) y pantalla (US5);
  - cierre;
  - equipo de demostración;
  - notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV; contracts/cargas.md)

- [ ] T002 En `contratos/cargas/openapi.yaml`:
  - ampliar `LecturaDeArchivo` con `valido`, `errores`, `advertencias` y `problemas` (los primeros 100, con `maxItems: 100`);
  - agregar los esquemas `TipoDeProblema` y `ProblemaDeFila`, con `fila` como entero ≥ 2;
  - cambiar la descripción de `validarArchivo`: el 200 trae el informe por fila; los detalles de un error de estructura siguen sin valores, y el informe por fila sí los trae;
  - agregar `descargarInforme` (`POST /api/cargas/plantillas/{codigo}/archivo/informe`, `x-mdd-permiso: CARGAS:ESCRITURA`, 200 `text/csv` con `Content-Disposition`, y 400, 404 y 413 como `validarArchivo`).

  Regenerar (`openApiGenerate`, `generar:api`) y dejar `verificar:ruptura` en verde.

### Las reglas del diccionario (research §3, §4, §6 y §7; data-model.md)

- [ ] T003 [P] Prueba primero, en `PCAR/dominio/ReglasDeColumnaTest.java`, sin dobles, con el diccionario real:
  - cada patrón de research §3 con un caso que pasa, uno que no y uno en el límite:
    - un texto de largo justo y uno de más;
    - `12` y `1a` para un entero;
    - `2.5`, `2,5` y `1.000` para un decimal;
    - `2026-02-28`, `2026-02-30` y `28/02/2026`;
    - `S`, `s` y `X`;
    - el catálogo `CARGOS` (22 valores, D-73) y `AGREGADO_1..3` expandido;
    - `Igual catálogo que 02.modalidad`;
    - `3 dígitos` con `061`, `61` y `0611`;
    - `Mayúsculas` con `PÉREZ`, `Pérez` y `PÉREZ ñ`; `Mayúsculas, dígitos, «-» o «_»` con `UA-01_X` y `ua-01`; `AAAA-I o AAAA-II` con `2026-I`, `2026-II`, `2026-III` y `26-I`; `usuario@dominio` con `a@b.ec`, `a@b` y `a b@c.ec`;
    - los rangos `1–10` (0, 1, 10, 11), `0,5–20` (0.4, 0.5), `≥ 0` (-1, 0), `> 0` (0, 0.1) y `≤ 40` (40, 41);
    - una lista de `«título|tipo|campo_especifico|año»` con 4 partes, con 3 y con un tipo fuera de su enumeración; un participante con 3 partes;
    - una jornada que no existe en `jornadas`;
    - una franja con `25:00` en `franjas_horarias`;
    - una experiencia con `2,5` años en `experiencia_laboral`;
  - **la lista exacta de los `formato` que no se interpretan** (research §3), sacada del diccionario real: si aparece uno nuevo, la prueba falla;
  - cada motivo dice qué hacer y un catálogo dice los valores permitidos (o los primeros 10 y cuántos más).

  Crear `ReglasDeColumna` en `JCAR/dominio/`, con JavaDoc que cite FR-012 y el principio XIV.
- [ ] T004 [P] Prueba primero, en `PCAR/dominio/CondicionDeObligatoriedadTest.java`, `PCAR/dominio/ClaveDeCargaTest.java` y `PCAR/dominio/ReferenciaDeColumnaTest.java`:
  - las 5 condiciones de research §4, cada una con la fila que la cumple y la que no;
  - un texto de obligatoriedad desconocido hace fallar la carga del diccionario, con su columna;
  - las claves simples y las 3 compuestas de research §7, leídas del diccionario real;
  - los 6 destinos de `FK` de research §6;
  - una `FK` sin destino conocido hace fallar la carga.

  Crear las tres clases en `JCAR/dominio/`.

### Docentes publica sus identificaciones (research §6)

- [ ] T005 [P] Prueba primero, en `PDOC/aplicacion/ConsultarIdentificacionesServicioTest.java` (Mockito): devuelve el conjunto de identificaciones registradas, de cualquier estado laboral, y nada más. Crear `JDOC/puertos/entrada/ConsultarIdentificaciones.java`, con JavaDoc que diga que es la API publicada para Cargas (F01-H03), `ConsultarIdentificacionesServicio` y su bean en `ConfiguracionDocentes`. T005 pasa.

**Checkpoint**: el contrato, el intérprete y el puerto de Docentes están listos.

---

## Phase 3: User Story 1 - Revisar las reglas de cada fila (Priority: P1) 🎯 MVP

**Goal**: cada fila con sus reglas, la cédula y la clave (FR-001 a FR-003, FR-005, FR-006).

- [ ] T006 [US1] Prueba primero, en `PCAR/dominio/ValidadorDeFilasTest.java`, sin dobles:
  - **la cédula `109990010` de la carga 06 da «Cédula inválida o sin cero inicial: le falta el 0 inicial.», con la sugerencia `0109990010`** (escenario del issue);
  - la cédula con el dígito verificador mal, sin sugerencia; un pasaporte de 14 caracteres;
  - una fila de docente con una fecha `2026-02-30`, una dedicación `XX` y un nombre de 61 caracteres: tres problemas en su fila y su campo, con la fila contando el encabezado como 1;
  - un obligatorio vacío y uno condicional: la categoría de un titular y el fin de contrato de un ocasional;
  - dos filas con la misma clave, simple (carga 06) y compuesta (carga 05): la segunda dice «Esta clave ya está en la fila N.»;
  - un valor con espacios en los extremos: una advertencia, y el valor recortado pasa sus reglas;
  - una fila vacía entre registros: una advertencia, y `registros()` no la cuenta (ajustar `TablaDeDatosTest` y el JavaDoc de `TablaDeDatos`);
  - el informe es válido sin errores y rechazado con uno; los totales y `primeros(100)` con un archivo de 150 problemas;
  - ningún motivo contiene el valor de su celda.

  Crear `ProblemaDeFila`, `InformeValidacionCarga` y `ValidadorDeFilas` en `JCAR/dominio/`.
- [ ] T007 [US1] Prueba primero, en `ValidadorDeFilasTest` (ampliar): **los 10 archivos de prueba no tienen ningún error** con las referencias que salen de las cargas 01 a 06 de prueba (SC-001). Las advertencias que tengan quedan anotadas en la evidencia. Si algún archivo de prueba choca con una regla del diccionario, anotarlo en la evidencia y preguntar al autor antes de cambiar el archivo o la regla. T006 y T007 pasan.

---

## Phase 4: User Story 2 - Referencias que no existen (Priority: P1)

**Goal**: cada `FK` contra lo que hay en el sistema (FR-004).

- [ ] T008 [US2] Prueba primero, en `ValidadorDeFilasTest` (ampliar):
  - **una asignatura de la carga 04 que cita una carrera que no existe: el error en su fila y en `codigo_carrera`** (escenario del issue);
  - un periodo, una unidad y un docente que no existen;
  - una carrera de la lista de un proyecto (carga 09) que no existe, y un participante que no existe (la parte «identificación»): el motivo dice cuál;
  - el padre de una unidad de la carga 01 que está en el mismo archivo pasa, y uno que no está ni en el archivo ni en el sistema no pasa.

  Ampliar `ValidadorDeFilas` con `ReferenciasExistentes`. Después, prueba primero, en `PCAR/adaptadores/salida/ReferenciasDesdePuertosTest.java`, con dobles escritos a mano de `ConsultarReferencias` y `ConsultarIdentificaciones`: traduce los códigos. Crear el puerto de salida `ReferenciasDelSistema` y el adaptador `ReferenciasDesdePuertos`, sumar `project(':servicios:academico')` y `project(':servicios:docentes')` a `backend/servicios/cargas/build.gradle` con su comentario, y el bean en `ConfiguracionCargas`. `./gradlew :apps:datos:test` corre `ModulosTest` y pasa. T008 pasa.

**Checkpoint**: las filas se revisan con todas sus reglas.

---

## Phase 5: User Story 3 - Descargar el informe (Priority: P1)

**Goal**: el informe en el 200 y en CSV, neutralizado (FR-005, FR-007; SC-003).

- [ ] T009 [US3] Prueba primero, en `PCAR/aplicacion/ValidarArchivoServicioTest.java` (ampliar, con Mockito): la estructura falla igual que en F01-H02 antes de revisar las filas (FR-009); con la estructura bien, devuelve el informe; las referencias se piden una vez por archivo. Ampliar `ArchivoValidado` y `ValidarArchivoServicio`. T009 pasa.
- [ ] T010 [US3] Prueba primero, en `PCAR/adaptadores/entrada/EscritorDeInformeTest.java`:
  - las columnas `fila,campo,valor,motivo,tipo,sugerencia`, en UTF-8 con BOM, con la sugerencia de la cédula;
  - **las celdas que empiezan con `=`, `+`, `-`, `@`, tabulación o retorno de carro llevan un apóstrofo delante**, entre ellas `=HYPERLINK("x")`, `-1` y una sugerencia que empieza con `-`;
  - los valores con comas, comillas y saltos de línea, según RFC 4180;
  - sin problemas, solo los encabezados.

  Crear `EscritorDeInforme` en `JCAR/adaptadores/entrada/`, sobre el `EscritorCsv` que ya escribe las plantillas (RFC 4180), con la neutralización encima. Después, prueba primero en `SubidaContratoTest` (ampliar):
  - `validarArchivo` responde 200 con `valido: false` y los problemas;
  - `descargarInforme` responde 200 `text/csv` con `Content-Disposition`;
  - las dos validan contra el contrato.

  Ampliar `PermisosCargasTest`: las dos exigen `CARGAS:ESCRITURA`; comprobar una vez que falla sin ese permiso y anotarlo. Ampliar el controlador. T010 pasa.

---

## Phase 6: User Story 4 - Corregir y reintentar (Priority: P1)

**Goal**: cada envío se valida desde cero (FR-008).

- [ ] T011 [US4] Prueba primero, en `SubidaContratoTest` (ampliar): subir un archivo con errores y después el corregido; **el segundo informe es solo el del archivo nuevo**, sin errores. Revisar que el servicio y el controlador no guardan nada entre peticiones (ningún campo con estado). T011 pasa.

**Checkpoint**: las cuatro historias de la API pasan.

---

## Phase 7: User Story 5 - Ver el informe en pantalla (Priority: P2)

**Goal**: «Plantillas de carga» con el resultado, los totales, la tabla y la descarga (FR-011).

- [ ] T012 [US5] Prueba primero, en `frontend/src/pantallas/plantillas/Plantillas.test.tsx` (ampliar, con la API simulada):
  - un archivo válido sin problemas: «Válido» y su número de registros, como hoy;
  - un archivo rechazado: «Rechazado», los totales en un `role="status"` y la tabla con la fila, el campo, el valor, el motivo, el tipo en español y la sugerencia;
  - solo advertencias: «Válido, con N advertencias»;
  - 150 problemas: la tabla muestra 100 y dice «Se muestran 100 de 150; el informe los trae todos.»;
  - «Descargar el informe» vuelve a enviar el mismo archivo a `descargarInforme` y guarda `informe-<carga>.csv`.

  Ampliar la pantalla y sus consultas en `frontend/src/pantallas/plantillas/`. T012 pasa.
- [ ] T013 [US5] Prueba con Playwright, en `frontend/e2e/plantillas.spec.ts` (ampliar):
  - `datos.demo` sube un archivo de docentes con la cédula sin el cero y ve el problema con su sugerencia;
  - descarga el informe y el archivo trae las columnas;
  - axe-core sin fallas WCAG 2.1 AA con el informe a la vista.

**Checkpoint**: las cinco historias pasan.

---

## Phase 8: Punta a punta y cierre

- [ ] T014 Crear `deploy/probar_reglas_carga.sh`, con el estilo de `probar_subida.sh`:
  - **Pasos:**
    - los CSV los arma el script en una carpeta temporal (SC-001 con los 10 archivos fijos lo comprueba T007): registra por la API una carrera con un prefijo al azar, como en `probar_cargos_proyectos.sh`;
    - un `04` con una asignatura que cita esa carrera no tiene errores;
    - un `06` con docentes ficticios, con cédulas generadas con su dígito verificador, no tiene errores;
    - un archivo de docentes con la cédula sin el cero, una fecha imposible y una dedicación `XX` da 3 errores con su fila, su campo y la sugerencia;
    - una asignatura con una carrera que no existe, su error en la fila;
    - `descargarInforme` responde un CSV donde una celda `=1+1` sale como `'=1+1`;
    - el archivo corregido, sin errores;
    - `consolidacion.demo` valida; `docente.demo` recibe 403.
  - **Tiempo:** genera un archivo de docentes de unos 5 MB con filas ficticias, lo valida, muestra cuánto tarda y falla desde 30 s (la meta del plan es 10 s; SC-004). Anotarlo en la evidencia.
  - **Limpieza:** borra en la base solo lo que creó, con sus registros.

  Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces seguidas y anotarlo; `shellcheck` sin observaciones.
- [ ] T015 Correr todo, como en las historias anteriores:
  - backend y SPA;
  - los 14 scripts con el perfil reconstruido;
  - `probar_equipo.sh`;
  - `shellcheck` y Gitleaks sobre un clon.

  Buscar un valor de prueba en `docker compose logs gateway datos` y anotar que no aparece (principio IX). Comprobar los criterios de terminado de la sección 6 de la constitución 2.2.0 y anotarlos. Hacer commit y push, abrir el PR en borrador con `Refs #29` y marcarlo listo después de sus runs.
- [ ] T016 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después:
  1. esperar el despliegue («Sano en N s»);
  2. el autor entra con `datos.demo`, sube un archivo con errores, lee el informe en pantalla, descarga el CSV y lo abre, corrige el archivo y lo vuelve a subir;
  3. abrir el PR de evidencia con `Closes #29` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 primero; T003, T004 y T005 en cualquier orden.
- **US1:** T006 después de T003 y T004; T007 después de T006.
- **US2:** T008 después de T005 y T007.
- **US3:** T009 después de T008; T010 después de T009.
- **US4:** T011 después de T010.
- **US5:** T012 después de T002; T013 después de T011 y T012.
- **Cierre:** T014 después de T011; T015 y T016 al final.

### Parallel Opportunities

- T003, T004 y T005 tocan archivos distintos.
- La SPA (T012) puede empezar después de T002, con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el intérprete del diccionario y las reglas de cada fila.
2. **Validar:** la cédula sin el cero tiene su sugerencia y los 10 archivos de prueba no tienen errores.

### Incremental

1. **US2:** las referencias.
2. **US3 y US4:** el informe en CSV y reintentar.
3. **US5:** la pantalla.
4. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- Las reglas propias de los dueños (un coordinador por carrera, la suma de horas, el docente activo a tiempo completo de un cargo) las aplica el dueño al confirmar (F01-H04).
- El mapeo configurable de etiquetas a códigos queda para F01-H05.
- Hacer commit por tarea o por grupo lógico en `024-validar-reglas-carga`, con `Refs #29`.
