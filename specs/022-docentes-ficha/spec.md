# Feature Specification: Mantener los docentes con su ficha académica

**Feature Branch**: `022-docentes-ficha`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Especifica la historia F03-H04 · Mantener los docentes con su ficha académica (issue #38), a partir de RF-16, RF-17, RN-01 y el diccionario de datos de la carga 6, con las tres aclaraciones del autor del 2026-10-24."

**Origen**: issue #38 · F03-H04 · sub-issue de F03 (Mantener los datos maestros: estructura, mallas, oferta, docentes y proyectos) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:04-docentes`, `RF-16`, `RF-17`, `RN-01` · tamaño propuesto L · depende de #35 (F03-H01), cerrada.

**Requerimientos que cubre**: RF-16 (los datos del docente; criterio: «La consulta por nombre o cédula muestra los datos. Una edición con datos inválidos no se guarda»), RF-17 (la ficha académica dentro del registro del docente, que alimenta el puntaje de afinidad), RN-01 (dedicaciones: tiempo completo 40 h, medio tiempo 20 h, tiempo parcial hasta 19 h), C-02 (la condición especial va aparte de la dedicación) y la ERS §8.3, carga 6. **Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-69 (el Vicerrector escribe con «Distributivo del periodo: escritura»), D-70 (permisos alternativos), ADR-010 y MD-02 (el puntaje de afinidad lo calcula Generación, no Docentes), ADR-016 (catálogo de errores), ADR-021 (permisos). **Principios de la constitución**: II (Datos maestros es de soporte), III (lenguaje ubicuo), IV (API primero), VI (las reglas son datos), VII (nada se borra y toda acción relevante deja registro) y IX (mínimo privilegio).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero consultar y corregir los datos de cada docente y su ficha académica, para que la afinidad y las reglas usen datos correctos.

**Punto de partida.** El servicio de Docentes existe solo como esqueleto: no guarda docentes, no tiene contrato ni pantalla. Académico ya tiene la estructura orgánica (F03-H01), las carreras, mallas y asignaturas (F03-H02) y la oferta del periodo (F03-H03). Sin docentes no hay a quién asignar horas, y sin su ficha académica la afinidad no tiene con qué calcularse. Esta historia permite registrar, consultar, buscar y corregir docentes con su ficha académica en pantalla, con las reglas de la carga 6 del diccionario de datos, y cambiar su estado laboral sin borrarlos. La carga 6 por archivo (F01-H04) aplicará después las mismas reglas.

**Cómo queda tras las aclaraciones**: mantienen los docentes Talento Humano, el Administrador del sistema y el Vicerrector académico; los consultan, además, el Responsable de consolidación y los coordinadores de carrera. Un docente se registra a mano, se corrige y cambia de estado (activo, en licencia o inactivo); nada se borra. Toda la ficha académica se corrige aquí, también las asignaturas afines declaradas; la carrera base y las asignaturas afines tienen que existir en Académico.

## Clarifications

### Session 2026-10-24

- Q: ¿Quién corrige los docentes y quién los consulta? → A: Corrigen quienes tienen «Docentes: escritura» (Talento Humano), «Catálogos y datos maestros: escritura» (Administrador del sistema) o «Distributivo del periodo: escritura» (Vicerrector académico), como las carreras y la oferta (D-70). Consultan quienes tienen «Docentes: lectura», «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura»: entre ellos el Responsable de consolidación y los coordinadores de carrera. La matriz no cambia y no hay decisiones nuevas.
- Q: ¿Se puede registrar un docente nuevo a mano? → A: Sí. Se registra, se corrige y se cambia su estado laboral (activo, en licencia o inactivo). Nada se borra (principio VII). La carga 6 usará después las mismas reglas.
- Q: ¿Qué parte de la ficha académica se corrige aquí? → A: Toda: el título de tercer nivel, los títulos de cuarto nivel, los cursos y capacitaciones, la experiencia laboral y las asignaturas afines declaradas, como listas en las que se agregan y quitan elementos. La carrera base y las asignaturas afines se validan contra Académico.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Registrar un docente (Priority: P1)

Como quien mantiene los datos de los docentes, quiero registrar un docente con sus datos personales y de contrato, su carrera base y su ficha académica, para tener a quién asignar horas sin esperar la carga por archivo (RF-16, RF-17, carga 6).

**Why this priority**: Sin docentes registrados no hay nada que consultar ni corregir; es la base de la historia.

**Independent Test**: Registrar un docente ocasional a tiempo completo, con cédula válida, carrera base vigente, un título de tercer nivel, una maestría, dos cursos y una experiencia; verlo en la lista.

**Acceptance Scenarios**:

1. **Given** una carrera existente y los datos completos de un docente, **When** lo registro, **Then** queda guardado, activo, con su ficha académica, y el registro guarda quién y cuándo.
2. **Given** un número de identificación ya registrado o un correo institucional que ya usa otro docente, **When** registro, **Then** se rechaza con el motivo en español.
3. **Given** una cédula cuyo dígito verificador no cuadra, unas horas contratadas que no corresponden a la dedicación, un docente titular sin categoría de escalafón, un docente ocasional sin fin de contrato, un fin de contrato anterior al inicio, una carrera base que no existe o un campo específico que no empieza por su campo amplio, **When** guardo, **Then** se rechaza con su campo y el motivo, y no se guarda nada.

---

### User Story 2 - Consultar y buscar (Priority: P1)

Como Responsable de consolidación, o como quien mantiene o consulta los datos, quiero buscar un docente por su nombre o su cédula y ver sus datos con su ficha académica, para saber con qué datos trabajarán la afinidad y las reglas (RF-16, RF-17).

**Why this priority**: Es el primer escenario del issue y el criterio de RF-16.

**Independent Test**: Buscar «peralta» y ver al docente; buscar su cédula y verlo; abrirlo y ver su tipo de personal, su dedicación, su condición especial y su ficha.

**Acceptance Scenarios**:

```gherkin
Escenario: Consulta
  Dado un docente cargado
  Cuando lo busco por su nombre o su cédula
  Entonces veo su tipo de personal, su dedicación, su condición especial y su ficha académica

Escenario: Ficha académica
  Dado un docente con un título de tercer nivel, uno de cuarto nivel, cursos y experiencia
  Cuando veo su ficha
  Entonces cada título muestra su campo amplio y específico, y cada curso sus horas
```

1. **Given** docentes registrados, **When** busco por una parte de su nombre o de sus apellidos, sin importar mayúsculas ni tildes, o por su número de identificación, **Then** veo los que coinciden, con su nombre completo, identificación, carrera base, tipo de personal, dedicación y estado.
2. **Given** un docente, **When** lo abro, **Then** veo sus datos personales y de contrato, su tipo de personal, su categoría de escalafón, su dedicación con sus horas contratadas, su condición especial, su estado y su ficha académica.
3. **Given** un docente con un título de tercer nivel, uno de cuarto nivel, cursos y experiencia, **When** veo su ficha, **Then** cada título muestra su campo amplio y su campo específico, con el nombre del campo amplio; cada título de cuarto nivel, su tipo y su año; cada curso, sus horas y su año; cada experiencia, su cargo, institución, campo específico y años; y las asignaturas afines declaradas, con su nombre.

---

### User Story 3 - Corregir los datos y la ficha (Priority: P1)

Como quien mantiene los datos de los docentes, quiero corregir los datos de un docente y su ficha académica, agregando o quitando títulos, cursos, experiencias y asignaturas afines, para que la afinidad y las reglas usen datos correctos (RF-16, RF-17, principio VII).

**Why this priority**: Es el objetivo de la historia: una ficha incompleta o mal cargada da una afinidad equivocada.

**Independent Test**: Cambiar la dedicación de un docente a medio tiempo con 20 horas; agregarle un curso y quitarle una experiencia; intentar guardar una dedicación que no existe y ver el valor permitido.

**Acceptance Scenarios**:

```gherkin
Escenario: Edición inválida
  Dado un docente
  Cuando guardo una dedicación que no existe
  Entonces el cambio no se guarda y se indica el valor permitido
```

1. **Given** un docente, **When** corrijo sus datos o su ficha con valores válidos, **Then** quedan así, con las mismas reglas que al registrar, y el registro guarda el valor anterior y el nuevo. El número de identificación no cambia.
2. **Given** un docente, **When** guardo una dedicación que no existe, **Then** el cambio no se guarda y el motivo dice los valores permitidos: tiempo completo, medio tiempo y tiempo parcial.
3. **Given** un título de cuarto nivel con un tipo que no existe o un año fuera de 1950 al año actual, un curso con 0 horas o una asignatura afín que no existe, **When** guardo, **Then** se rechaza con el elemento, su campo y el motivo, y el docente queda como estaba.

---

### User Story 4 - Cambiar el estado laboral (Priority: P1)

Como quien mantiene los datos de los docentes, quiero marcar a un docente en licencia o inactivo, y volverlo a activar, sin borrarlo, para que solo los activos reciban asignaciones y su historia se conserve (carga 6, principio VII).

**Why this priority**: Un docente que sale de la institución no puede borrarse: su rastro y sus distributivos anteriores lo necesitan.

**Independent Test**: Marcar a un docente como inactivo; verlo con ese estado en la búsqueda; volverlo a activar.

**Acceptance Scenarios**:

1. **Given** un docente activo, **When** lo marco en licencia o inactivo, **Then** queda en ese estado, se sigue viendo y el registro guarda el cambio.
2. **Given** un docente en licencia o inactivo, **When** lo vuelvo a activar, **Then** queda activo. Ninguno se borra.

---

### User Story 5 - Los docentes en pantalla (Priority: P1)

Como quien mantiene los datos de los docentes, o como quien los consulta, quiero una pantalla para buscar docentes, ver su ficha y mantenerlos, para no depender de un archivo.

**Why this priority**: La carga 6 por archivo llega con F01-H04; sin pantalla, los docentes solo se podrían registrar llamando al servidor.

**Independent Test**: Abrir «Docentes», buscar uno, ver su ficha; registrar uno; sin el permiso de escritura, verla en solo lectura.

**Acceptance Scenarios**:

1. **Given** docentes registrados, **When** abro la pantalla, **Then** veo la lista con un buscador por nombre o identificación y puedo filtrar por carrera base y estado.
2. **Given** el permiso de escritura, **When** registro, corrijo o cambio el estado de un docente, **Then** el cambio se ve; un valor inválido muestra su motivo junto al campo, y en las listas de la ficha, junto al elemento.
3. **Given** un usuario sin el permiso de escritura, **When** abre la pantalla, **Then** la ve en solo lectura, sin acciones, y la pantalla dice por qué.

---

### Edge Cases

- **Identificación**: tipo cédula o pasaporte. La cédula tiene 10 dígitos como texto, con el 0 inicial, y su dígito verificador cumple el módulo 10. El pasaporte tiene hasta 13 caracteres, letras y números. Es la clave del docente y no cambia.
- **Nombres**: primer apellido y primer nombre obligatorios; segundo apellido y segundo nombre opcionales; hasta 60 caracteres cada uno, en mayúsculas (se guardan en mayúsculas aunque se escriban en minúsculas).
- **Correo institucional**: con formato de correo, hasta 100 caracteres, único entre los docentes. **Teléfono**: opcional, hasta 10 dígitos.
- **Tipo de personal**: titular, ocasional, invitado u honorario (RCE art. 137; el glosario nombra los dos primeros).
- **Categoría de escalafón**: auxiliar 1 y 2, agregado 1 a 3, principal 1 a 3 o «no aplica». Un titular necesita una categoría que no sea «no aplica»; los demás tipos, «no aplica».
- **Dedicación y horas contratadas** (RN-01): tiempo completo con 40 horas, medio tiempo con 20, tiempo parcial con 1 a 19. Si se omiten las horas, se toman las de la dedicación (40 o 20); en tiempo parcial son obligatorias.
- **Fechas**: ingreso, inicio y fin de contrato. El fin es obligatorio para un ocasional y, si se indica, es posterior al inicio. Que el contrato cubra el periodo depende del periodo y lo revisa la validación del distributivo, no esta historia.
- **Condición especial**: cuidador sustituto, maternidad o lactancia, apoyo a la SIES o ninguna (por omisión, ninguna). Va aparte de la dedicación (C-02); cómo cambia las horas lo dice la normativa (RN-14 a RN-16).
- **Carrera base**: una carrera existente de Académico, en cualquier estado: un docente puede seguir perteneciendo a una carrera en cierre.
- **Título de tercer nivel**: obligatorio, hasta 150 caracteres, como consta en la SENESCYT; registro SENESCYT opcional, hasta 30; campo amplio del catálogo CINE (2 dígitos) y campo específico de 3 dígitos que empieza por el amplio.
- **Títulos de cuarto nivel**: lista opcional; cada uno con su título, su tipo (maestría, especialización o doctorado), su campo específico y su año (1950 al año actual). Su campo amplio son los dos primeros dígitos del específico.
- **Cursos y capacitaciones**: lista opcional; cada uno con su nombre, campo específico, horas mayores que 0 y año.
- **Experiencia laboral**: lista opcional; cada una con su cargo, institución, campo específico y años (decimal mayor que 0).
- **Asignaturas afines declaradas**: lista opcional de códigos de asignaturas existentes en Académico, sin repetir. Que el coordinador las valide, y que sumen al puntaje, queda fuera de esta historia.
- **Tamaño de la ficha**: el diccionario limita cada lista de la carga 6 a 600 o 1000 caracteres en el archivo; en pantalla, cada lista admite hasta 20 elementos, que cubren esos límites.
- **Estado laboral**: activo, en licencia o inactivo; un docente nuevo queda activo. Solo los activos reciben asignaciones (lo aplica el distributivo).
- **Búsqueda**: por una parte del nombre o los apellidos, sin distinguir mayúsculas ni tildes, o por el número de identificación; la lista se ordena por apellidos.
- **Datos personales**: la cédula, el correo y el teléfono son datos personales. Los datos de prueba del repositorio y del equipo de demostración son ficticios.
- **Usuario sin permiso**: quien no tenga ningún permiso de escritura de los tres ve los docentes en solo lectura, y el servidor responde «Acceso no autorizado» (403) si intenta cambiar algo; quien no tenga ninguno de los tres de lectura no los ve.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE guardar cada docente con su identificación, sus nombres, su correo institucional, su teléfono, su tipo de personal, su categoría de escalafón, su dedicación con sus horas contratadas, sus fechas de ingreso, inicio y fin de contrato, su condición especial, su carrera base, su ficha académica y su estado laboral, como en la carga 6.
- **FR-002**: La ficha académica DEBE guardarse dentro del registro del docente: el título de tercer nivel con su registro SENESCYT, su campo amplio y su campo específico; los títulos de cuarto nivel con su tipo, campo específico y año; los cursos y capacitaciones con su campo específico, horas y año; la experiencia laboral con su cargo, institución, campo específico y años; y las asignaturas afines declaradas (RF-17).
- **FR-003**: Se DEBE poder registrar, buscar, consultar, corregir y cambiar el estado laboral de un docente; el número de identificación no cambia y nada se borra (aclaración 2).
- **FR-004**: Cada dato DEBE cumplir las reglas de la carga 6 del diccionario de datos descritas en los casos límite: cédula con dígito verificador válido, identificación y correo únicos, horas coherentes con la dedicación (RN-01), categoría según el tipo de personal, fin de contrato del ocasional, campo específico que empieza por su campo amplio, años y horas de la ficha en su rango.
- **FR-005**: La carrera base y las asignaturas afines declaradas DEBEN existir en Académico al registrar o corregir (aclaración 3).
- **FR-006**: Cada dato inválido DEBE rechazarse entero, con su campo (y, en las listas de la ficha, su elemento) y el motivo en español; un valor fuera de un catálogo DEBE decir los valores permitidos. No se guarda nada a medias.
- **FR-007**: La búsqueda DEBE encontrar docentes por una parte de su nombre o apellidos, sin distinguir mayúsculas ni tildes, o por su número de identificación, y permitir filtrar por carrera base y estado laboral.
- **FR-008**: La consulta de un docente DEBE mostrar todos sus datos y su ficha académica, con el nombre del campo amplio de cada título y el nombre de cada asignatura afín.
- **FR-009**: Mantener docentes DEBE exigir «Docentes: escritura», «Catálogos y datos maestros: escritura» o «Distributivo del periodo: escritura», y consultarlos, «Docentes: lectura», «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura», validados en el servidor (ADR-021, D-70, aclaración 1).
- **FR-010**: Cada registro, corrección y cambio de estado DEBE quedar registrado con quién, cuándo, qué cambió, el valor anterior y el nuevo (principio VII).
- **FR-011**: La pantalla «Docentes» DEBE listar y buscar docentes, mostrar la ficha de cada uno y permitir a quien tenga el permiso registrar, corregir y cambiar el estado, con las listas de la ficha editables.
- **FR-012**: Los catálogos de la carga 6 (tipo de identificación, tipo de personal, categoría de escalafón, dedicación, condición especial, tipo de título de cuarto nivel, estado laboral) y el catálogo de campos amplios CINE DEBEN ser los del diccionario de datos y vivir en un solo lugar (principio VI).

### Key Entities

- **Docente**: persona contratada por la institución a la que se asignan horas (glosario, `Docente`, Datos maestros). Su clave es su número de identificación.
- **Tipo de personal**, **Dedicación** y **Condición especial**: los del glosario; la condición especial va aparte de la dedicación (C-02).
- **Ficha académica**: formación y experiencia del docente dentro de su registro (`FichaAcademica`): título de tercer nivel, títulos de cuarto nivel, cursos y capacitaciones, experiencia laboral y asignaturas afines declaradas.
- **Campo de conocimiento**: campo amplio y específico CINE, el mismo de las carreras y asignaturas (F03-H02).
- **Carrera** y **Asignatura**: las de Académico (F03-H02), a las que se refieren la carrera base y las asignaturas afines.
- **Cambio de los datos maestros**: registro de quién, cuándo y qué cambió, con el valor anterior y el nuevo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quien mantiene los datos encuentra un docente por su nombre o su cédula y abre su ficha en menos de 30 segundos, sin ayuda.
- **SC-002**: Los 47 docentes de los datos de prueba de la carga 6 se pueden registrar todos con las mismas reglas, sobre las carreras y asignaturas de prueba, y cada uno se encuentra por su cédula.
- **SC-003**: Con un docente que tiene un título de tercer nivel, uno de cuarto nivel, cursos y experiencia, la ficha muestra el campo amplio y específico de cada título y las horas de cada curso.
- **SC-004**: El 100 % de los intentos de guardar un dato fuera de las reglas de la carga 6 se rechazan con su motivo, también llamando directamente al servidor; una dedicación inexistente dice los valores permitidos.
- **SC-005**: Cada cambio de un docente aparece en el registro con quién, cuándo, el valor anterior y el nuevo.

## Assumptions

- Las reglas de cada columna son las del diccionario de datos de la carga 6 (F01-H01); si la ERS cambia, cambian allí.
- La restricción de POL 5.1 (el personal ocasional solo a tiempo completo o medio tiempo) y las horas que cambia la condición especial son reglas de la normativa (F04) y las revisa la validación del distributivo; el diccionario de la carga 6 no las pide, y aquí no se rechazan.
- Que el fin de contrato cubra el periodo depende del periodo: lo revisa la validación del distributivo, no el registro del docente.
- Registrar por pantalla es la vía de esta historia; la carga 6 por archivo se aplica con F01-H04 sobre las mismas reglas.
- La consulta de los docentes con su ficha queda disponible para Generación, que calcula el puntaje de afinidad desde la Ola 2 (F06-H01, MD-02).
- El Coordinador de carrera lee los docentes de todas las carreras (D-65 le da «Docentes: lectura» de todo); no los mantiene.
- Un docente se identifica por su número de identificación; vincularlo con su usuario del sistema (para que consulte su propio distributivo) queda para la historia que lo necesite.
- Fuera de alcance: la carga 6 por archivo (F01-H04), el cálculo del puntaje de afinidad (F06-H01), la validación de las asignaturas afines por el coordinador, los cargos y los proyectos (F03-H05), la ficha firmada en PDF como adjunto, las franjas horarias del docente (RF-60).
