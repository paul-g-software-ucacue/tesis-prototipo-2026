# Feature Specification: Mantener la oferta del periodo

**Feature Branch**: `021-oferta-periodo`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Especifica la historia F03-H03 · Mantener la oferta del periodo (issue #37), a partir de RF-15, RN-18 y el diccionario de datos de la carga 5, con las tres aclaraciones del autor del 2026-10-24."

**Origen**: issue #37 · F03-H03 · sub-issue de F03 (Mantener los datos maestros: estructura, mallas, oferta, docentes y proyectos) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:11-academico`, `RF-15` · tamaño propuesto S · depende de #36 (F03-H02), cerrada.

**Requerimientos que cubre**: RF-15 (la oferta del periodo: asignatura × paralelo × jornada × modalidad; criterio: «El número de paralelos por asignatura se refleja en las horas a cubrir»), RN-18 (las horas de clase de una carrera no exceden las de su malla aprobada) y la ERS §8.3, carga 5. **Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-69 (el Vicerrector escribe con «Distributivo del periodo: escritura»), D-70 (permisos alternativos), ADR-016 (catálogo de errores), ADR-021 (permisos). **Principios de la constitución**: II (Académico es de soporte), III (lenguaje ubicuo), VI (las reglas son datos), VII (nada se borra y toda acción relevante deja registro) y IX (mínimo privilegio).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero registrar las asignaturas-paralelo del periodo con su jornada y modalidad, para saber cuántas horas de clase hay que asignar.

**Punto de partida.** Académico ya tiene los periodos con sus estados (F02-H01), la estructura orgánica con sus sedes (F03-H01) y las carreras, mallas y asignaturas, con las horas de cada malla (F03-H02). No tiene la oferta: qué asignaturas se dictan en un periodo, en cuántos paralelos y con qué jornada. Sin ella no se sabe cuántas horas de clase hay que asignar, y el distributivo no tiene qué proponer. Esta historia permite registrar, consultar, corregir y retirar asignaturas-paralelo del periodo en pantalla, con las reglas de la carga 5 del diccionario de datos, y da las horas a cubrir por asignatura, por carrera y por periodo.

**Cómo queda tras las aclaraciones**: mantienen la oferta el Administrador del sistema y el Vicerrector académico, como las carreras; la leen quienes leen la oferta, el distributivo o los catálogos, entre ellos el Responsable de consolidación. La oferta de un periodo solo cambia mientras el periodo está en Planificación. RN-18 se aplica por asignatura-paralelo: sus horas no pasan las de su asignatura en la malla.

## Clarifications

### Session 2026-10-24

- Q: ¿Quién mantiene la oferta del periodo? → A: El Administrador del sistema y el Vicerrector académico, como las carreras (F03-H02): mantener exige «Catálogos y datos maestros: escritura» o «Distributivo del periodo: escritura» (D-70). La leen quienes tienen «Oferta y propuesta de docentes: lectura», «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura»; el Responsable de consolidación tiene la primera (D-65). El Coordinador de carrera tiene «Oferta: escritura» solo de lo propio, y el servidor todavía no sabe de qué carrera es cada coordinador: se agrega cuando F03-H05 lo diga. La matriz no cambia y no hay decisiones nuevas.
- Q: ¿En qué estados del periodo se puede cambiar la oferta? → A: Solo en Planificación. En «En aprobación», «Vigente» y «Cerrado» la oferta queda en solo lectura, para que las horas a cubrir no cambien por debajo del distributivo.
- Q: ¿Cómo se aplica RN-18 en la oferta? → A: Por asignatura-paralelo: sus horas semanales no pasan las de su asignatura en la malla; si se omiten, son esas. Como cada asignatura va una sola vez en cada paralelo, cada paralelo tampoco pasa las horas de su malla.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Registrar la oferta del periodo (Priority: P1)

Como quien mantiene los datos maestros, quiero registrar las asignaturas-paralelo de un periodo en planificación, con su paralelo, su jornada, su modalidad y sus estudiantes previstos, para que el distributivo sepa qué hay que cubrir (RF-15, carga 5).

**Why this priority**: Sin la oferta no hay horas que asignar: es el centro de la historia.

**Independent Test**: En un periodo en planificación, registrar una asignatura activa de una carrera vigente en el paralelo `A_NOCTURNA`, con jornada nocturna, modalidad presencial y 28 estudiantes; verla en la oferta con las horas de su asignatura.

**Acceptance Scenarios**:

1. **Given** un periodo en planificación y una asignatura activa de una carrera vigente, **When** la registro en un paralelo con una jornada de su carrera, una modalidad y el número de estudiantes previstos, **Then** queda ofertada con las horas semanales de su asignatura y la sede de su carrera, y el registro guarda quién y cuándo.
2. **Given** las horas semanales o la sede indicadas, **When** registro, **Then** quedan esas en lugar de las de la asignatura y la carrera.
3. **Given** la misma asignatura y el mismo paralelo ya ofertados en el periodo, **When** registro, **Then** se rechaza con el motivo en español.
4. **Given** una asignatura inactiva, una carrera que no está vigente, una jornada que no es de la carrera, una sede que no es sede, extensión ni campus activa, un número de estudiantes negativo o unas horas mayores que las de la asignatura, **When** guardo, **Then** se rechaza con su campo y el motivo.
5. **Given** un periodo que ya no está en planificación, **When** intento registrar, corregir o retirar, **Then** se rechaza, y la oferta queda como estaba.

---

### User Story 2 - Paralelos y horas a cubrir (Priority: P1)

Como quien prepara el distributivo, quiero consultar la oferta de un periodo con las horas a cubrir de cada asignatura, de cada carrera y del periodo, para saber cuántas horas de clase hay que asignar (RF-15, RN-18).

**Why this priority**: Es el escenario del issue y el criterio de RF-15.

**Independent Test**: Ofertar una asignatura de 4 horas en 3 paralelos y ver que suma 12 horas a cubrir; retirar uno y ver que suma 8.

**Acceptance Scenarios**:

```gherkin
Escenario: Paralelos y horas a cubrir
  Dado una asignatura de 4 horas semanales ofertada en 3 paralelos
  Cuando consulto la oferta
  Entonces la asignatura suma 12 horas a cubrir
```

1. **Given** una asignatura de 4 horas semanales ofertada en 3 paralelos, **When** consulto la oferta del periodo, **Then** la asignatura suma 12 horas a cubrir, en 3 paralelos.
2. **Given** la oferta de un periodo, **When** la consulto, **Then** veo cada asignatura-paralelo con su asignatura, carrera, ciclo, paralelo, jornada, modalidad, sede, estudiantes y horas, y los totales de horas a cubrir por asignatura, por carrera y del periodo; las retiradas se ven, pero no suman.
3. **Given** una asignatura-paralelo, **When** la consulto, **Then** trae también lo que la afinidad necesita de su asignatura: tronco común, campo amplio y específico y palabras clave.

---

### User Story 3 - Corregir y retirar (Priority: P1)

Como quien mantiene los datos maestros, quiero corregir una asignatura-paralelo o retirarla de la oferta mientras el periodo está en planificación, sin borrarla, para que la oferta refleje lo que de verdad se dicta (principio VII).

**Why this priority**: La oferta se ajusta varias veces antes de enviar el distributivo; un error no puede quedarse ni borrar el rastro.

**Independent Test**: Corregir la jornada y los estudiantes de una asignatura-paralelo; retirarla y volver a ofertarla.

**Acceptance Scenarios**:

1. **Given** una asignatura-paralelo ofertada en un periodo en planificación, **When** corrijo su jornada, modalidad, sede, estudiantes u horas, **Then** queda así, con las mismas reglas que al registrar, y el registro guarda el valor anterior y el nuevo. El periodo, la asignatura y el paralelo no cambian.
2. **Given** una asignatura-paralelo ofertada, **When** la retiro, **Then** queda retirada: se sigue viendo, pero no suma horas a cubrir. Retirada, se puede volver a ofertar. Ninguna se borra.

---

### User Story 4 - La oferta en pantalla (Priority: P1)

Como quien mantiene los datos maestros, o como quien los consulta, quiero una pantalla con la oferta de un periodo agrupada por carrera, con las horas a cubrir, para mantenerla sin depender de un archivo.

**Why this priority**: La carga 5 por archivo llega con F01; sin pantalla, la oferta solo se podría registrar llamando al servidor.

**Independent Test**: Elegir un periodo; ver su oferta por carrera con los totales; registrar una asignatura-paralelo; sin el permiso de escritura o con el periodo fuera de planificación, verla en solo lectura.

**Acceptance Scenarios**:

1. **Given** un periodo con oferta, **When** lo elijo en la pantalla, **Then** veo su oferta agrupada por carrera, con las asignaturas-paralelo por ciclo y las horas a cubrir de cada asignatura, de cada carrera y del periodo.
2. **Given** el permiso de escritura y un periodo en planificación, **When** registro, corrijo o retiro una asignatura-paralelo, **Then** el cambio y los nuevos totales se ven; un valor inválido muestra su motivo junto al campo.
3. **Given** un usuario sin el permiso de escritura, o un periodo que ya no está en planificación, **When** abre la oferta, **Then** la ve en solo lectura, sin acciones, y la pantalla dice por qué.

---

### Edge Cases

- **Clave de una asignatura-paralelo**: periodo + asignatura + paralelo; la misma asignatura no va dos veces en el mismo paralelo de un periodo, pero sí en varios paralelos (en los datos de prueba, 35 asignaturas van en `A_NOCTURNA` y `B_NOCTURNA`).
- **Paralelo**: hasta 20 caracteres, en mayúsculas, números, guion y guion bajo (por ejemplo, `A_NOCTURNA`); no cambia.
- **Periodo**: uno registrado (F02-H01); solo en Planificación se cambia su oferta (aclaración 2). La carga 3 del diccionario dice «APROBADO»; en el sistema, el periodo pasa por Planificación, En aprobación, Vigente y Cerrado, y cómo se traduce al cargar el archivo lo decide F01-H03.
- **Asignatura**: activa, de una carrera vigente («solo VIGENTE admite oferta», carga 2). La malla puede estar derogada: sus asignaturas se conservan y se siguen dictando a quienes la cursan. Si la asignatura se inactiva o la carrera deja de estar vigente después, su oferta se conserva; corregirla exige que vuelvan a cumplirse las reglas.
- **Jornada**: una de las jornadas de su carrera (carga 5).
- **Modalidad**: una del catálogo de modalidades de la carrera (presencial, semipresencial, en línea, a distancia, dual o híbrida); puede ser distinta de la de la carrera. En los datos de prueba siempre es la de la carrera.
- **Sede**: opcional; si se omite, la de la carrera. Si se indica, una sede matriz, sede, extensión o campus activa de la estructura orgánica.
- **Estudiantes previstos**: entero de 0 a 9999, por la longitud 4 del diccionario (en los datos de prueba, de 5 a 32).
- **Horas semanales**: opcionales; si se omiten, las de clase de la asignatura. Si se indican, de 0,5 a las de la asignatura (RN-18, aclaración 3). Si la asignatura se corrige después, la oferta que tomó sus horas las sigue (no guarda una copia); la que indicó las suyas las conserva, y la consulta la marca si quedaron por encima.
- **Retirada**: no suma horas a cubrir ni recibe propuestas; se puede volver a ofertar mientras el periodo está en planificación.
- **Franjas horarias**: la columna opcional de la carga 5 (RF-60) no se guarda en esta historia: es Should y el MVP no maneja horarios (F22).
- **Usuario sin permiso**: quien no tenga «Catálogos: escritura» ni «Distributivo: escritura» ve la oferta en solo lectura, y el servidor responde «Acceso no autorizado» (403) si intenta cambiar algo; quien no tenga «Oferta: lectura», «Distributivo: lectura» ni «Catálogos: lectura» no la ve.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE guardar cada asignatura-paralelo de un periodo con su asignatura, su paralelo, su jornada, su modalidad, su sede, el número de estudiantes previstos, sus horas semanales y su estado (ofertada o retirada), como en la carga 5, sin las franjas horarias.
- **FR-002**: Se DEBE poder registrar, consultar, corregir, retirar y volver a ofertar asignaturas-paralelo; el periodo, la asignatura y el paralelo no cambian y nada se borra.
- **FR-003**: Registrar, corregir, retirar y volver a ofertar DEBEN rechazarse si el periodo no está en Planificación (aclaración 2).
- **FR-004**: Una asignatura-paralelo DEBE tener una asignatura activa de una carrera vigente, una jornada de su carrera, una modalidad del catálogo, una sede válida o la de su carrera y 0 estudiantes o más; la misma asignatura no DEBE ir dos veces en el mismo paralelo de un periodo.
- **FR-005**: Las horas semanales de una asignatura-paralelo DEBEN ser las de su asignatura si se omiten, y no DEBEN pasar las de su asignatura si se indican (RN-18, aclaración 3).
- **FR-006**: Cada dato inválido DEBE rechazarse entero, con su campo y el motivo en español, sin dejar nada a medias.
- **FR-007**: La consulta de la oferta de un periodo DEBE dar cada asignatura-paralelo y las horas a cubrir: por asignatura (la suma de sus paralelos ofertados), por carrera y del periodo; las retiradas no suman (RF-15).
- **FR-008**: La consulta de una asignatura-paralelo DEBE traer de su asignatura el ciclo, el tronco común, el campo amplio y específico y las palabras clave, para la afinidad y el distributivo.
- **FR-009**: Mantener la oferta DEBE exigir «Catálogos y datos maestros: escritura» o «Distributivo del periodo: escritura», y consultarla, «Oferta y propuesta de docentes: lectura», «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura», validados en el servidor (ADR-021, D-70, aclaración 1).
- **FR-010**: Cada registro, corrección, retiro y vuelta a la oferta DEBE quedar registrado con quién, cuándo, qué cambió, el valor anterior y el nuevo (principio VII).
- **FR-011**: La pantalla DEBE mostrar la oferta del periodo elegido agrupada por carrera, con sus asignaturas-paralelo por ciclo y las horas a cubrir de cada asignatura, de cada carrera y del periodo, y permitir a quien tenga el permiso registrar, corregir, retirar y volver a ofertar mientras el periodo está en Planificación.
- **FR-012**: Las jornadas y las modalidades DEBEN ser las del diccionario de datos, las mismas enumeraciones de F03-H02; el estado de una asignatura-paralelo (ofertada o retirada), que no está en el diccionario, DEBE vivir en un solo lugar (principio VI).

### Key Entities

- **Oferta**: conjunto de asignaturas-paralelo que se dictan en el periodo (glosario, `Oferta`, Datos maestros).
- **Asignatura-paralelo**: elemento de la oferta: una asignatura en un paralelo, una jornada y una modalidad; es lo que recibe una propuesta (`AsignaturaParalelo`). Lleva su sede, los estudiantes previstos, sus horas semanales y su estado.
- **Paralelo**: grupo de estudiantes de una asignatura (`paralelo`).
- **Jornada** y **Modalidad**: las del glosario, ya usadas por las carreras (F03-H02).
- **Periodo académico**: el de F02-H01; su estado decide si la oferta se puede cambiar.
- **Cambio de los datos maestros**: registro de quién, cuándo y qué cambió, con el valor anterior y el nuevo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quien mantiene los datos registra una asignatura-paralelo de una asignatura existente en menos de 1 minuto, sin ayuda.
- **SC-002**: Las 165 asignaturas-paralelo de los datos de prueba de la carga 5 (periodo 2026-I) se pueden registrar todas sobre las 300 asignaturas de prueba, y la consulta da 660 horas a cubrir en el periodo, con el total de cada una de las 7 carreras.
- **SC-003**: Con una asignatura de 4 horas en 3 paralelos, la consulta da 12 horas a cubrir para esa asignatura; con uno retirado, 8.
- **SC-004**: El 100 % de los intentos de cambiar la oferta fuera de Planificación, o con un dato fuera de las reglas de la carga 5, se rechazan con su motivo, también llamando directamente al servidor.
- **SC-005**: Cada cambio de una asignatura-paralelo aparece en el registro con quién, cuándo, el valor anterior y el nuevo.

## Assumptions

- Las reglas de cada columna son las del diccionario de datos de la carga 5 (F01-H01); si la ERS cambia, cambian allí.
- La modalidad de una asignatura-paralelo puede ser distinta de la de su carrera: el diccionario solo pide el mismo catálogo.
- La malla de la asignatura puede estar derogada: el diccionario solo pide que la asignatura esté activa.
- Registrar por pantalla es la vía de esta historia; la carga 5 por archivo se aplica con F01-H04 sobre las mismas reglas, y cómo se traduce el estado «APROBADO» de la carga 3 lo decide F01-H03.
- La consulta de la oferta queda disponible para el distributivo, que la usa desde la Ola 2.
- El Coordinador de carrera tiene «Oferta» solo de lo propio (D-65). Mientras el servidor no sepa de qué carrera es cada coordinador, lee la oferta de todas las carreras, como hoy lee las carreras y la estructura, y no la mantiene. El filtro de lo propio lo aplica F03-H05, con los cargos (D-64).
- Fuera de alcance: la carga 5 por archivo (F01-H03 y F01-H04), las franjas horarias y los choques (RF-60, RN-26, Should; F22), las propuestas de docentes y el distributivo, el coordinador de carrera como persona y su permiso de lo propio (F03-H05), los matriculados (carga 10).
