# Feature Specification: Mantener carreras, mallas y asignaturas

**Feature Branch**: `020-mallas-asignaturas`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Especifica la historia F03-H02 · Mantener mallas y asignaturas (issue #36), con las carreras de la carga 02 por la aclaración 3 de F03-H01, a partir de RF-12, RF-14, RN-12, RN-18, RN-19, RN-24 y el diccionario de datos de las cargas 2 y 4."

**Origen**: issue #36 · F03-H02 · sub-issue de F03 (Mantener los datos maestros: estructura, mallas, oferta, docentes y proyectos) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:11-academico`, `RF-14` · tamaño propuesto M · depende de #35 (F03-H01), cerrada. El alcance incluye las carreras de la carga 02 (comentario del issue, aclaración 3 de F03-H01).

**Requerimientos que cubre**: RF-14 (las asignaturas por carrera y malla: código, ciclo, unidad de organización curricular, horas semanales y tronco común; criterio: «No se puede asignar una asignatura sin carrera ni horas»), RF-12 (las carreras, parte de la estructura de la institución), RN-12 (un coordinador por carrera ofertada), RN-18 (las horas de clase de una carrera no exceden las de su malla aprobada), RN-19 y RN-24 (la afinidad usa los campos y las palabras clave de la asignatura; el tronco común queda exento del mínimo) y la ERS §8.3, cargas 2 y 4. **Decisiones que aplica** (`docs/decisiones.md`): D-16, D-65 y D-71 (matriz de permisos), D-70 (permisos alternativos), ADR-016 (catálogo de errores), ADR-021 (permisos). **Principios de la constitución**: II (Académico es de soporte), III (lenguaje ubicuo), VI (las reglas son datos), VII (toda acción relevante deja registro) y IX (mínimo privilegio).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero mantener las asignaturas de cada malla con su ciclo, horas, campos y palabras clave, para saber cuántas horas hay que cubrir y con qué formación.

**Punto de partida.** Académico ya tiene los periodos (F02-H01) y la estructura orgánica (F03-H01), con las sedes y las coordinaciones de carrera como unidades. No tiene las carreras ni sus mallas ni sus asignaturas, que son la base de la oferta (F03-H03), de las horas a cubrir (RN-18) y de la afinidad (RN-19). Las cargas 2 y 4 por archivo ya se pueden validar en estructura (F01-H02), pero todavía no hay dónde aplicarlas. Esta historia permite registrar, consultar, corregir e inactivar carreras, mallas y asignaturas en pantalla, con las reglas del diccionario de datos.

**Cómo queda tras las aclaraciones**: mantienen carreras, mallas y asignaturas el Administrador del sistema y el Vicerrector académico; las leen quienes leen el distributivo o los catálogos. La malla es una entidad con su código de versión y su estado, y cada asignatura pertenece a una. El coordinador de cada carrera se registra con los cargos (F03-H05).

## Clarifications

### Session 2026-10-24

- Q: ¿Quién mantiene las carreras, las mallas y las asignaturas, y quién las lee? → A: Las mantiene quien tenga «Catálogos y datos maestros: escritura» o «Distributivo del periodo: escritura», con los permisos alternativos de D-70: el Administrador del sistema y el Vicerrector académico, que ya escribe los periodos con ese permiso (D-69); así lo dice la ERS §8.3 («Vicerrectorado / Coord. académica»). Las leen, como la estructura orgánica, quienes tienen «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura», entre ellos el Responsable de consolidación y los coordinadores. La matriz no cambia. Los coordinadores no las mantienen: el servidor todavía no filtra por «lo propio».
- Q: ¿Qué es una malla en el sistema? → A: Una entidad con su código de versión (por ejemplo, `DS-2019` y `DS-AC-2025`, las dos mallas de Desarrollo de Software en los datos de prueba), su carrera y su estado (vigente o derogada). Cada asignatura pertenece a una malla, y la pantalla muestra las horas de cada malla. RN-18 (las horas de clase no pasan las de la malla aprobada) se aplica en la oferta (F03-H03), con esos totales.
- Q: ¿Cómo se representa el coordinador de cada carrera (RN-12)? → A: Con los cargos (F03-H05): la carga 08 trae el cargo `COORDINADOR_CARRERA` con su carrera, y allí se comprueba RN-12. Esta historia no guarda coordinador, como el diccionario de la carga 02.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Mantener las carreras (Priority: P1)

Como quien mantiene los datos maestros, quiero registrar, consultar, corregir e inactivar las carreras de la institución, con su código SENESCYT, la sede donde se ofertan y sus datos de la carga 02, para que las mallas y la oferta apunten a ellas (RF-12, carga 2).

**Why this priority**: Una asignatura sin carrera no se puede guardar (RF-14): las carreras van primero.

**Independent Test**: Registrar una carrera en la sede matriz, con su código, nombre, siglas, nivel, modalidad, jornadas, campos y número de ciclos; consultarla; corregir su nombre; ponerla en cierre.

**Acceptance Scenarios**:

1. **Given** una carrera con su código SENESCYT, nombre, siglas, una sede, extensión o campus activa de la estructura, nivel de formación, modalidad, jornadas, campo amplio y específico y número de ciclos, **When** la registro, **Then** queda vigente y el registro guarda quién y cuándo.
2. **Given** un código o unas siglas que ya usa otra carrera, o una unidad que no es sede, extensión ni campus, **When** registro, **Then** se rechaza con el motivo en español.
3. **Given** un campo específico que no empieza por el campo amplio, o un campo detallado que no empieza por el específico, **When** guardo, **Then** se rechaza con su campo.
4. **Given** una carrera registrada, **When** corrijo sus datos o cambio su estado (vigente, no vigente, en cierre), **Then** queda así y el registro guarda el valor anterior y el nuevo. El código no cambia. Ninguna carrera se borra.

---

### User Story 2 - Mantener las mallas y sus asignaturas (Priority: P1)

Como quien mantiene los datos maestros, quiero registrar las mallas de cada carrera y sus asignaturas, con su ciclo, unidad de organización curricular, horas, campos y palabras clave, para saber cuántas horas hay que cubrir y con qué formación (RF-14, carga 4).

**Why this priority**: Es el primer escenario del issue y el centro de la historia.

**Independent Test**: Registrar una malla vigente de una carrera vigente y una asignatura en ella con todos sus datos; intentar guardar otra sin carrera o sin horas y ver qué falta.

**Acceptance Scenarios**:

```gherkin
Escenario: Asignatura incompleta
  Dado una asignatura sin carrera o sin horas semanales
  Cuando intento guardarla
  Entonces no se guarda y se indica qué falta
```

1. **Given** una carrera vigente, **When** registro una malla con su código de versión (por ejemplo, `DS-2019`), **Then** queda vigente en la carrera; una carrera puede tener varias mallas, y una malla se puede derogar después. El código de versión es único y no cambia.
2. **Given** una asignatura con su carrera vigente, una malla vigente de esa carrera, código, nombre, ciclo, unidad de organización curricular, horas semanales de clase, tronco común, campo amplio y específico y al menos dos palabras clave, **When** la registro, **Then** queda activa en su malla y el registro guarda quién y cuándo.
3. **Given** una asignatura sin carrera, sin horas semanales de clase o con cero horas, **When** intento guardarla, **Then** no se guarda y se indica cada dato que falta.
4. **Given** un ciclo mayor que el número de ciclos de su carrera, una carrera que no está vigente, una malla derogada o de otra carrera, un código que ya usa otra asignatura, horas fuera de 0,5 a 20 o una sola palabra clave, **When** guardo, **Then** se rechaza con el motivo en español.
5. **Given** una asignatura registrada, **When** corrijo sus datos o la inactivo, **Then** queda así y el registro guarda el valor anterior y el nuevo. El código, la carrera y la malla no cambian. Ninguna asignatura ni malla se borra.

---

### User Story 3 - Los datos para la afinidad (Priority: P1)

Como quien prepara el distributivo, quiero consultar una asignatura con su campo de conocimiento, sus palabras clave y si es de tronco común, para que la afinidad con los docentes se calcule con datos correctos (RN-19, RN-24).

**Why this priority**: Es el segundo escenario del issue: sin esos datos la afinidad no se puede calcular.

**Independent Test**: Consultar una asignatura guardada y ver su campo amplio y específico, sus palabras clave y si es de tronco común.

**Acceptance Scenarios**:

```gherkin
Escenario: Datos para la afinidad
  Dado una asignatura guardada
  Cuando la consulto
  Entonces muestra su campo amplio y específico, sus palabras clave y si es de tronco común
```

1. **Given** una asignatura guardada, **When** la consulto, **Then** veo su campo amplio con su nombre CINE, su campo específico, sus palabras clave una por una y si es de tronco común. El diccionario no trae los nombres de los campos específicos, así que se muestra su código.

---

### User Story 4 - Carreras y asignaturas en pantalla (Priority: P1)

Como quien mantiene los datos maestros, o como quien los consulta, quiero una pantalla con las carreras y, en cada una, su malla y sus asignaturas por ciclo, con las horas de cada ciclo, para mantenerlas sin depender de un archivo.

**Why this priority**: La carga por archivo llega con F01; sin pantalla, estos datos solo se podrían registrar llamando al servidor.

**Independent Test**: Ver las carreras; abrir una y ver sus mallas y, en cada una, sus asignaturas agrupadas por ciclo con las horas de cada ciclo; registrar una asignatura; sin el permiso de escritura, verlas en solo lectura.

**Acceptance Scenarios**:

1. **Given** carreras y asignaturas registradas, **When** abro la pantalla, **Then** veo cada carrera con su código, nombre, siglas, sede, modalidad, jornadas, estado y sus mallas; al abrir una malla, sus asignaturas por ciclo, con las horas semanales de clase de cada ciclo y de la malla.
2. **Given** el permiso de escritura, **When** registro, corrijo o cambio el estado de una carrera, una malla o una asignatura, **Then** el cambio se ve; un valor inválido muestra su motivo junto al campo.
3. **Given** un usuario sin el permiso de escritura, **When** abre la pantalla, **Then** la ve en solo lectura, sin acciones.

---

### Edge Cases

- **Código de carrera**: el código SENESCYT, de hasta 20 caracteres (por ejemplo, `550613A01-P-0301`); único y no cambia.
- **Siglas**: hasta 10 caracteres, únicas entre las carreras.
- **Sede de la carrera**: una unidad activa de la estructura orgánica de tipo sede matriz, sede, extensión o campus (F03-H01). Si esa unidad se inactiva después, la carrera sigue apuntando a ella; corregir la carrera exige elegir una activa.
- **Jornadas**: una o más de matutina, vespertina, nocturna e intensiva, sin repetir.
- **Campos CINE**: el campo amplio es uno de los 11 de la hoja de catálogos (00 a 10); el específico, de 3 dígitos, empieza por el amplio; el detallado, opcional, de 4, empieza por el específico.
- **Número de ciclos**: de 1 a 10. Bajarlo por debajo del ciclo de alguna asignatura activa de la carrera se rechaza.
- **Estado de una carrera**: vigente, no vigente o en cierre; solo una carrera vigente admite asignaturas nuevas y, después, oferta (F03-H03). Las asignaturas de una carrera que deja de estar vigente se conservan.
- **Código de asignatura**: hasta 30 caracteres (por ejemplo, `DS-P-II-POO-204`); único y no cambia.
- **Horas**: las de clase, de 0,5 a 20, con decimales de punto; las prácticas, de 0 a 20 y opcionales; los créditos, opcionales.
- **Palabras clave**: al menos dos términos distintos, sin vacíos; se guardan sin espacios en los extremos.
- **Tronco común**: sí o no; si es sí, la asignatura queda exenta del mínimo de afinidad (RN-19), que se aplica en F07.
- **Malla**: su código de versión es único entre todas las mallas, de hasta 20 caracteres (como `codigo_version_malla` de la carga 04), y no cambia. Una malla derogada conserva sus asignaturas, pero no admite nuevas. Una carrera puede tener varias mallas vigentes, como en los datos de prueba (la de 2019 y la del ajuste de 2025).
- **Coordinador de la carrera (RN-12)**: no se guarda aquí; se registra y se comprueba con los cargos (F03-H05, aclaración 3).
- **Usuario sin permiso**: quien no tenga «Catálogos: escritura» ni «Distributivo: escritura» ve la pantalla en solo lectura, y el servidor responde «Acceso no autorizado» (403) si intenta cambiar algo; quien no tenga «Distributivo: lectura» ni «Catálogos: lectura» no la ve.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE guardar las carreras con su código SENESCYT, nombre, siglas, sede, nivel de formación, modalidad, jornadas, campo amplio, específico y detallado, número de ciclos y estado, como en la carga 02.
- **FR-002**: El sistema DEBE guardar las mallas con su código de versión, su carrera y su estado (vigente o derogada), y las asignaturas con su carrera, su malla, código, nombre, ciclo, unidad de organización curricular, horas semanales de clase y de prácticas, créditos, tronco común, campo amplio y específico, palabras clave y estado, como en la carga 04.
- **FR-003**: Se DEBE poder registrar, consultar, corregir y cambiar el estado de carreras, mallas y asignaturas; los códigos no cambian y nada se borra.
- **FR-004**: Una asignatura NO DEBE guardarse sin carrera ni sin horas semanales de clase, y el rechazo DEBE decir cada dato que falta (RF-14).
- **FR-005**: Cada dato inválido DEBE rechazarse entero, con su campo y el motivo en español, sin dejar nada a medias.
- **FR-006**: Los valores permitidos (niveles, modalidades, jornadas, unidades de organización curricular, campos CINE, estados) DEBEN ser los del diccionario de datos, cada lista en un solo lugar, y una prueba DEBE comprobar que coinciden (principio VI).
- **FR-007**: La consulta de una asignatura DEBE mostrar su campo amplio y específico, sus palabras clave y si es de tronco común (RN-19, RN-24).
- **FR-008**: Mantener carreras y asignaturas DEBE exigir un permiso de escritura, y consultarlas un permiso de lectura, validados en el servidor (ADR-021). Mantener DEBE exigir «Catálogos y datos maestros: escritura» o «Distributivo del periodo: escritura», y consultar, «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura» (D-70, aclaración 1).
- **FR-009**: Cada registro, corrección y cambio de estado DEBE quedar registrado con quién, cuándo, qué cambió, el valor anterior y el nuevo (principio VII).
- **FR-010**: El sistema DEBE ofrecer la consulta de las carreras y de las asignaturas de una carrera, para que la oferta (F03-H03), las cargas (F01-H04) y la afinidad (F07) las usen.
- **FR-011**: La pantalla DEBE mostrar las carreras y, en cada una, sus mallas con sus asignaturas por ciclo y las horas de cada ciclo y de cada malla, y permitir a quien tenga el permiso registrar, corregir y cambiar el estado de carreras, mallas y asignaturas.
- **FR-012**: La consulta de una malla DEBE dar el total de horas semanales de clase de sus asignaturas activas, para que la oferta (F03-H03) aplique RN-18.

### Key Entities

- **Carrera**: carrera ofertada, con código SENESCYT, modalidad, jornadas y campo amplio y específico (glosario, `Carrera`, Datos maestros); se oferta en una sede de la estructura orgánica.
- **Malla**: plan de estudios aprobado de una carrera, con sus asignaturas y horas (`Malla`); tiene su código de versión y su estado.
- **Asignatura**: materia de una malla con código, ciclo, unidad de organización curricular, horas semanales, campos y palabras clave (`Asignatura`).
- **Ciclo**: nivel de la malla en que se dicta una asignatura; no es el periodo (`ciclo`).
- **Tronco común**: asignatura compartida que queda exenta del mínimo de afinidad (`troncoComun`).
- **Campo de conocimiento**: campo amplio y campo específico (CINE) de una carrera o una asignatura (`CampoConocimiento`).
- **Cambio de los datos maestros**: registro de quién, cuándo y qué cambió, con el valor anterior y el nuevo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quien mantiene los datos registra una asignatura completa de una carrera existente en menos de 2 minutos, sin ayuda.
- **SC-002**: Las 7 carreras, sus 12 mallas y las 300 asignaturas de los datos de prueba de las cargas 2 y 4 se pueden registrar todas, y la pantalla muestra las horas de clase de cada malla (100 en cada una).
- **SC-003**: El 100 % de los intentos de guardar una asignatura sin carrera o sin horas, o con un dato fuera de las reglas del diccionario, se rechazan con su campo, también llamando directamente al servidor.
- **SC-004**: Cada cambio de una carrera o una asignatura aparece en el registro con quién, cuándo, el valor anterior y el nuevo.

## Assumptions

- Las reglas de cada columna son las del diccionario de datos de las cargas 2 y 4 (F01-H01); si la ERS cambia, cambian allí.
- La columna «coordinador» que la ERS §8.3 nombra para la carga 2 no está en su diccionario de datos; esta historia sigue el diccionario.
- Los campos CINE amplios son los 11 del catálogo del diccionario; los específicos y detallados se validan por su forma (3 y 4 dígitos que empiezan por el anterior), porque el diccionario no trae su lista.
- Registrar por pantalla es la vía de esta historia; las cargas 2 y 4 por archivo se aplican con F01-H04 sobre las mismas reglas. Si una fila de la carga 04 no trae `codigo_version_malla`, a qué malla va lo decide F01-H04.
- RN-18 se aplica en la oferta (F03-H03), con el total de horas de cada malla que da esta historia.
- Fuera de alcance: la oferta del periodo y la aplicación de RN-18 (F03-H03), el cálculo de la afinidad (F07), los cargos y el coordinador de cada carrera (F03-H05).
