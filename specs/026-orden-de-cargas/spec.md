# Feature Specification: Configurar las 10 cargas en su orden

**Feature Branch**: `026-orden-de-cargas`

**Created**: 2026-10-27

**Status**: Draft

**Input**: User description: "Especifica la historia F01-H05 · Configurar las 10 cargas en su orden (issue #31), a partir de RF-56, RF-18, RF-57 y la sección 8.3 de la ERS, con las cuatro aclaraciones del autor del 2026-10-27."

**Origen**: issue #31 · F01-H05 · sub-issue de F01 (Cargar los 10 archivos con validación y vista previa) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:03-cargas`, `RF-18`, `RF-56` · tamaño propuesto L · depende de #30 (F01-H04), #39 (F03-H05) y #40 (F04-H01), cerradas.

**Requerimientos que cubre**:
- **RF-56:** respetar el orden de dependencias de las cargas («no cargar asignaturas sin carreras») y mostrar el estado de cada carga. El tablero de preparación del periodo y la habilitación de «Crear distributivo» son de F02-H02 (#33).
- **RF-18:** los proyectos y prácticas aprobados son obligatorios para que haya horas de investigación, vinculación y prácticas. Un periodo que de verdad no tiene proyectos se confirma explícitamente como «sin proyectos».
- **RF-57 (S, interfaz):** la obtención de los datos va detrás de una interfaz de fuente de datos, con un adaptador inicial de archivos CSV o XLSX. Criterio: «Revisión de arquitectura: las cargas no dependen del formato ni del origen».
- **ERS §8.3:** el orden y las dependencias de las 10 cargas, ya escritos en el diccionario de datos (F01-H01):

  | Nº | Carga | Depende de | Obligatoria |
  |---|---|---|---|
  | 1 | Estructura orgánica | — | Sí |
  | 2 | Carreras | 1 | Sí |
  | 3 | Ciclos académicos aprobados | — | Sí |
  | 4 | Asignaturas y malla | 2 | Sí |
  | 5 | Oferta del periodo | 3, 4 | Sí |
  | 6 | Docentes contratados (con ficha académica) | 1, 2 | Sí |
  | 7 | Horas según la política de contratación | — | Sí |
  | 8 | Cargos y autoridades | 1, 2, 6 | Sí |
  | 9 | Proyectos y prácticas aprobados | 2, 3, 6 | Sí, o «sin proyectos» |
  | 10 | Matriculados por carrera | 2, 3 | Recomendada |

**Decisiones que aplica** (`docs/decisiones.md`): D-70 (permisos alternativos), D-71 (quién ejecuta las cargas), D-72 (validación de las entradas y catálogo de errores completo), D-74 (mensajes en «tú»), MD-13 y ADR-009 (cada carga tiene un servicio dueño), ADR-022 (lenguaje publicado de las cargas). **Principios de la constitución 2.2.0**: III (lenguaje ubicuo), IV (API primero), V (límites entre servicios, puertos y adaptadores), VII (trazabilidad), IX (mínimo privilegio), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero que las 10 cargas respeten el orden de dependencias de la ERS, para que ningún dato quede apuntando a algo que todavía no existe.

**Punto de partida.** Hoy «Plantillas de carga» deja subir cualquiera de las 10 cargas en cualquier momento. Si falta una carga anterior, cada fila que la referencia sale con un error («regístrala o confirma antes su carga», F01-H03): con 300 asignaturas y sin carreras, son 300 errores que dicen lo mismo. Las cargas ya se confirman y quedan en el historial (F01-H04), pero la pantalla no dice qué cargas tienen datos, ni deja decir que un periodo no tiene proyectos.

Esta historia agrega lo que falta:
- **El orden:** una carga cuyas cargas anteriores no tienen datos no se puede subir, y la pantalla dice cuál falta.
- **El estado de cada carga:** pendiente o cargada.
- **«Sin proyectos»:** la confirmación explícita, por periodo, de que no hay proyectos aprobados.
- **La fuente de datos intercambiable:** una prueba de arquitectura de que las reglas no dependen del formato ni del origen del archivo.

## Clarifications

### Session 2026-10-27

- Q: ¿Cuándo cuenta una carga como «cargada» para las que dependen de ella? → A: Cuando su servicio dueño ya tiene datos de esa carga, hayan entrado por archivo o por pantalla. Es coherente con F01-H04, que compara con lo que hay en el sistema, y respeta lo registrado a mano: la estructura orgánica y las carreras del equipo de demostración se registraron por pantalla.
- Q: Cuando falta una carga anterior, ¿cómo se impide subir la siguiente? → A: En la pantalla y en el servidor. En «Plantillas de carga», «Subir archivo» queda deshabilitado y dice qué carga falta. El servidor rechaza validar y confirmar esa carga con un código nuevo del catálogo de errores, por si alguien llama a la API directamente.
- Q: ¿A qué se aplica la confirmación «sin proyectos»? → A: A un periodo. Quien confirma cargas elige un periodo de los ciclos académicos cargados y confirma «sin proyectos». Queda en el historial con quién y cuándo. Si después se confirma una carga de proyectos con al menos un proyecto asignable en ese periodo, la confirmación deja de valer.
- Q: ¿Qué parte del tablero de RF-56 entra aquí? → A: El estado de cada carga en «Plantillas de carga» (pendiente, cargada, o «sin proyectos» para la 9) y qué carga anterior falta. El tablero del periodo y el botón «Crear distributivo» son de F02-H02 (#33).

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Respetar el orden de las cargas (Priority: P1)

Como quien ejecuta las cargas, quiero que el sistema no me deje subir una carga cuyas cargas anteriores no tienen datos, y que me diga cuál falta, para no recibir cientos de errores que dicen lo mismo y no dejar datos que apunten a algo que no existe (RF-56, ERS §8.3).

**Why this priority**: Es el primer escenario del issue y el corazón de la historia.

**Independent Test**: Con una base sin carreras, abrir «Plantillas de carga» y ver «Subir archivo» de «Asignaturas y malla» deshabilitado con «Falta la carga de Carreras». Llamar a la API para validar un archivo de asignaturas y recibir el error del catálogo que nombra la carga que falta.

**Acceptance Scenarios**:

```gherkin
Escenario: Orden de dependencias
  Dado un periodo sin la carga de carreras
  Cuando intento cargar asignaturas y malla
  Entonces el sistema no lo permite e indica que falta la carga de carreras
```

1. **Given** una base sin carreras, **When** abro «Plantillas de carga», **Then** «Subir archivo» de «Asignaturas y malla» está deshabilitado y la fila dice que falta la carga de Carreras.
2. **Given** una base sin carreras, **When** alguien pide validar o confirmar un archivo de asignaturas por la API, **Then** el sistema lo rechaza con un error del catálogo que nombra cada carga que falta, sin leer el archivo y sin cambiar ningún dato.
3. **Given** una carga con varias cargas anteriores (por ejemplo, Cargos y autoridades, que depende de 1, 2 y 6) y solo una de ellas sin datos, **When** la intento subir, **Then** el sistema nombra solo la que falta.
4. **Given** carreras registradas a mano en su pantalla, sin ninguna carga confirmada, **When** abro «Plantillas de carga», **Then** «Carreras» está «cargada» y puedo subir «Asignaturas y malla».
5. **Given** una carga sin cargas anteriores (Estructura orgánica, Ciclos académicos, Política), **When** abro la pantalla, **Then** siempre puedo subirla.

---

### User Story 2 - Ver el estado de cada carga (Priority: P1)

Como quien ejecuta las cargas, quiero ver en «Plantillas de carga» qué cargas ya tienen datos y cuáles están pendientes, para saber cuál sigue (RF-56).

**Why this priority**: Sin el estado no se entiende por qué un botón está deshabilitado ni qué falta.

**Independent Test**: Con estructura orgánica y carreras registradas y nada más, ver «Cargada» en 1 y 2, «Pendiente» en las demás, y la política «Cargada» porque el sistema la trae precargada (F04-H01).

**Acceptance Scenarios**:

1. **Given** el sistema con datos de algunas cargas, **When** abro «Plantillas de carga», **Then** cada carga dice «Cargada» si su servicio dueño tiene datos de ella, o «Pendiente» si no tiene.
2. **Given** la carga 9 sin proyectos y un periodo confirmado «sin proyectos», **When** abro la pantalla, **Then** la carga 9 dice «Sin proyectos» y el periodo.
3. **Given** que confirmo una carga, **When** termina, **Then** el estado de esa carga y los botones de las que dependen de ella se actualizan sin recargar la página.
4. **Given** un usuario que solo consulta las plantillas, **When** abre la pantalla, **Then** ve el estado de cada carga, pero no los botones de subir ni de confirmar «sin proyectos».

---

### User Story 3 - Confirmar un periodo «sin proyectos» (Priority: P1)

Como quien ejecuta las cargas, quiero confirmar explícitamente que un periodo no tiene proyectos aprobados, para que la carga de proyectos cuente como cargada en ese periodo sin subir un archivo vacío (RF-18).

**Why this priority**: Es el tercer escenario del issue. Sin él, un periodo sin proyectos nunca tendría sus cargas obligatorias completas (F02-H02).

**Independent Test**: Con el periodo 2026-II cargado y sin proyectos asignables en él, confirmar «sin proyectos» para 2026-II y ver la carga 9 «Sin proyectos (2026-II)» y una entrada en su historial. Confirmar luego una carga de proyectos con uno asignable en 2026-II y ver que la confirmación ya no vale.

**Acceptance Scenarios**:

```gherkin
Escenario: Periodo sin proyectos
  Dado un periodo que no tiene proyectos aprobados
  Cuando confirmo explícitamente "sin proyectos"
  Entonces la carga de proyectos cuenta como cargada
```

1. **Given** un periodo de los ciclos cargados sin proyectos asignables en él, **When** confirmo «sin proyectos» para ese periodo, **Then** la carga 9 cuenta como cargada en ese periodo y el historial de la carga 9 muestra la confirmación con el usuario, el momento y el periodo.
2. **Given** un periodo con al menos un proyecto asignable (F03-H05), **When** intento confirmar «sin proyectos», **Then** el sistema no lo permite y dice que el periodo tiene proyectos aprobados.
3. **Given** un periodo confirmado «sin proyectos», **When** después se confirma una carga de proyectos con al menos un proyecto asignable en ese periodo, **Then** la confirmación «sin proyectos» deja de valer y la carga 9 cuenta como cargada por sus proyectos.
4. **Given** un código de periodo que no está en los ciclos cargados, o vacío, o con caracteres o largo no admitidos, **When** intento confirmar «sin proyectos», **Then** el sistema lo rechaza con un error del catálogo y no guarda nada.
5. **Given** un periodo ya confirmado «sin proyectos», **When** lo confirmo otra vez, **Then** no se duplica: el sistema responde que ya estaba confirmado.
6. **Given** un usuario sin el permiso de escribir cargas, **When** intenta confirmar «sin proyectos», **Then** recibe «no autorizado» y no cambia nada.

---

### User Story 4 - Las 10 cargas de prueba entran en orden (Priority: P1)

Como autor de la tesis, quiero comprobar que los 10 archivos de prueba entran sin errores en su orden, con el orden ya impuesto, para cerrar la Ola 1 (criterio de cierre de la ola).

**Why this priority**: Es el segundo escenario del issue y el criterio de cierre de la Ola 1.

**Independent Test**: Con una base vacía, intentar subir la carga 4 antes de la 2 y ver el rechazo; luego subir y confirmar las 10 en su orden y ver que todas quedan «Cargada», sin errores.

**Acceptance Scenarios**:

```gherkin
Escenario: Datos de prueba completos
  Dado los 10 archivos de cargas/datos_prueba/
  Cuando los cargo en orden
  Entonces las 10 cargas entran sin errores
```

1. **Given** una base vacía y los 10 archivos de prueba, **When** los valido y confirmo en su orden, **Then** las 10 entran sin errores y las 10 quedan «Cargada».
2. **Given** una base vacía, **When** intento la carga 4 antes que la 2, **Then** el sistema la rechaza porque falta la carga de Carreras, y después, en su orden, entra.

---

### User Story 5 - La fuente de datos es intercambiable (Priority: P1)

Como responsable técnico, quiero que las reglas de las cargas no dependan de que los datos lleguen en un archivo CSV o XLSX, para que en el futuro se pueda agregar otra fuente (Moodle, SIGA o Talento Humano) sin cambiar las reglas (RF-57).

**Why this priority**: Es el cuarto escenario del issue; lo pide la revisión de arquitectura.

**Independent Test**: Una prueba de arquitectura comprueba que las reglas y la vista previa no conocen los formatos de archivo. Otra prueba alimenta las mismas reglas desde una fuente que no es un archivo y obtiene el mismo resultado que con el CSV.

**Acceptance Scenarios**:

```gherkin
Escenario: Fuente de datos intercambiable
  Dado el adaptador de archivos detrás del puerto de fuente de datos
  Cuando se revisa la arquitectura
  Entonces las validaciones no dependen del formato ni del origen del archivo
```

1. **Given** las reglas de las filas, la vista previa y el orden de las cargas, **When** corre la revisión de arquitectura, **Then** ninguna de ellas depende del adaptador de archivos ni de las bibliotecas de CSV o XLSX.
2. **Given** los mismos registros de una carga entregados por una fuente de prueba que no es un archivo, **When** pasan por las reglas, **Then** el resultado es el mismo que con el archivo CSV.

---

### Edge Cases

- **Una carga anterior cuyos datos se borran o se inactivan a mano después:** el estado se calcula con lo que hay en el sistema en ese momento; si ya no hay datos activos, la carga vuelve a «Pendiente» y las que dependen de ella se deshabilitan.
- **Datos que existen pero están todos inactivos** (por ejemplo, todas las carreras dadas de baja): no cuentan; la carga queda «Pendiente».
- **La política de carga horaria:** el sistema trae la política 2024 precargada (F04-H01), así que la carga 7 está «Cargada» desde el inicio.
- **Un servicio dueño que no responde al calcular el estado:** la pantalla muestra el error del catálogo con qué hacer, no un estado falso; el servidor no deja subir una carga cuyas dependencias no pudo comprobar.
- **Una confirmación con la vista previa ya calculada y, entre medio, alguien borra los datos de una carga anterior:** la confirmación vuelve a comprobar el orden y la rechaza con el mismo error.
- **«Sin proyectos» para un periodo y proyectos asignables para otro:** cada periodo se evalúa por separado; la carga 9 está «Cargada» en general si hay proyectos, y la pantalla lista además los periodos confirmados «sin proyectos».
- **Confirmar «sin proyectos» y después subir proyectos que no son asignables en ese periodo** (vencidos o suspendidos): la confirmación sigue valiendo.
- **La carga 10 (Recomendada):** respeta el orden igual que las obligatorias; ser recomendada solo cambia lo que exige F02-H02 para crear el distributivo.

## Requirements *(mandatory)*

### Functional Requirements

**El orden**

- **FR-001**: El sistema MUST considerar «cargada» una carga cuando su servicio dueño tiene al menos un registro vigente de ella, haya entrado por archivo o por pantalla. Para la carga 9 de un periodo, también cuando el periodo está confirmado «sin proyectos».
- **FR-002**: El sistema MUST tomar las dependencias de cada carga del diccionario de datos (`dependeDe`), sin repetirlas en otro lugar.
- **FR-003**: El sistema MUST rechazar validar o confirmar el archivo de una carga mientras alguna de sus cargas anteriores no esté cargada, antes de leer el archivo, sin cambiar datos ni dejar entrada en el historial.
- **FR-004**: El rechazo de FR-003 MUST usar un código nuevo del catálogo de errores con sus 8 columnas, en «tú», y nombrar cada carga anterior que falta con su número y nombre.
- **FR-005**: La confirmación MUST volver a comprobar el orden en el momento de aplicar, para que una carga anterior borrada entre la vista previa y la confirmación no deje datos apuntando a algo que no existe.

**El estado**

- **FR-006**: El sistema MUST ofrecer, a quien puede consultar las plantillas, el estado de cada una de las 10 cargas: «Pendiente» o «Cargada», las cargas anteriores que le faltan y, para la carga 9, los periodos confirmados «sin proyectos».
- **FR-007**: «Plantillas de carga» MUST mostrar el estado de cada carga y deshabilitar «Subir archivo» de las que tienen cargas anteriores pendientes, con el texto de cuál falta, legible por lectores de pantalla.
- **FR-008**: Después de confirmar una carga o un periodo «sin proyectos», la pantalla MUST actualizar el estado y los botones sin recargar la página.

**«Sin proyectos»**

- **FR-009**: Quien tiene el permiso de escribir cargas MUST poder confirmar «sin proyectos» para un periodo que está en los ciclos académicos cargados.
- **FR-010**: El sistema MUST validar el código del periodo por tipo, caracteres y largo con las reglas del diccionario de datos de los ciclos académicos, en el servidor y en la pantalla, y rechazar uno que no existe con un error del catálogo.
- **FR-011**: El sistema MUST rechazar «sin proyectos» para un periodo con al menos un proyecto asignable en él (estado APROBADO o EN_EJECUCION y vigencia que se cruza con las fechas del periodo, F03-H05), con un código nuevo del catálogo.
- **FR-012**: Cada confirmación «sin proyectos» MUST quedar en el historial de la carga 9 con el usuario, el momento y el periodo. Confirmar otra vez un periodo ya confirmado no duplica la entrada.
- **FR-013**: Una confirmación «sin proyectos» MUST dejar de valer cuando el periodo pasa a tener al menos un proyecto asignable, sin borrar su entrada del historial.

**La fuente de datos**

- **FR-014**: Las reglas de las filas, la vista previa y el orden de las cargas MUST recibir los registros a través de la interfaz de fuente de datos, sin depender del formato (CSV o XLSX) ni del origen; el adaptador de archivos MUST ser solo una implementación de esa interfaz.
- **FR-015**: Una prueba de arquitectura MUST fallar si las reglas, la vista previa o el orden dependen del adaptador de archivos o de sus bibliotecas.

**Transversales**

- **FR-016**: Consultar el estado MUST requerir el mismo permiso que consultar las plantillas; confirmar «sin proyectos», el mismo que confirmar una carga (D-70, D-71).
- **FR-017**: Todo mensaje nuevo al usuario MUST salir del catálogo de errores, en «tú», sin detalles técnicos, y decir qué hacer (D-72, D-74).
- **FR-018**: Ni el estado ni los mensajes de orden MUST mostrar datos personales; el estado solo dice si hay datos, no cuáles.

### Key Entities

- **Estado de la carga**: por cada una de las 10 cargas, si está «Pendiente» o «Cargada», qué cargas anteriores le faltan y, para la 9, los periodos «sin proyectos». Se calcula con lo que hay en el sistema; no se guarda.
- **Confirmación «sin proyectos»**: el periodo, quién confirmó y cuándo. Es una entrada del historial de la carga 9; vale mientras el periodo no tenga proyectos asignables.
- **Fuente de datos**: la interfaz por la que llegan los registros de una carga; hoy su única implementación es el adaptador de archivos CSV y XLSX.
- **Historial de cargas** (F01-H04): suma las confirmaciones «sin proyectos».

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Desde una base vacía, las 10 cargas de prueba entran sin errores en su orden y las 10 quedan «Cargada» (criterio de cierre de la Ola 1).
- **SC-002**: Una carga subida antes que sus cargas anteriores se rechaza el 100 % de las veces, con un solo mensaje que nombra lo que falta, en lugar de un error por fila.
- **SC-003**: Quien ejecuta las cargas sabe cuál sigue mirando una sola pantalla: el estado de las 10 se ve en «Plantillas de carga» en menos de 2 segundos (con holgura).
- **SC-004**: Un periodo sin proyectos queda con la carga 9 cargada con una sola confirmación, sin subir un archivo.
- **SC-005**: La revisión de arquitectura comprueba en cada build que las reglas no dependen del formato ni del origen del archivo.
- **SC-006**: Ningún dato personal aparece en el estado, en los mensajes nuevos ni en los registros de log.

## Assumptions

- «Tiene datos» se evalúa en el servicio dueño de cada carga (MD-13): Cargas pregunta, no lee tablas ajenas.
- Registro vigente: para las entidades con estado, solo cuentan los activos; las que no tienen estado cuentan si existen.
- El estado «con errores» de RF-56 lo trae F02-H02 con el tablero del periodo; validar un archivo no deja huella en el historial (F01-H04), así que aquí no hay un estado «con errores».
- El orden aplica a validar y a confirmar; descargar la plantilla y el diccionario siempre se puede.
- Los periodos para «sin proyectos» son los de los ciclos académicos cargados, en cualquier estado.
- La pantalla de «sin proyectos» está en la fila de la carga 9 de «Plantillas de carga».
- Los datos de prueba son ficticios (repositorio público).

## Out of Scope

- El tablero de preparación del periodo y el botón «Crear distributivo» (F02-H02, #33).
- Los plazos del periodo (F02).
- Un adaptador de Moodle, SIGA o Talento Humano (RF-57, parte C).
- Borrar o aplicar las bajas (F01-H04 solo las informa).
- Editar en pantalla las cargas de parametrización.
- Anular a mano una confirmación «sin proyectos».
