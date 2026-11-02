# Feature Specification: Justificar una asignación con una regla blanda

**Feature Branch**: `035-justificar-regla-blanda`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Especifica la historia F07-H03 · Justificar una asignación con una regla blanda (issue #52), a partir de RF-28, RN-19, MD-07 y RNF-EQU-01, con las aclaraciones del autor."

**Origen**: issue #52 · F07-H03 · sub-issue de F07 (Validar cada asignación al instante, con los totales del docente, #9) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · tamaño M

**Requerimientos que cubre**:
- **RF-28:** una violación blanda genera una advertencia que requiere justificación.
- **RN-19:** un docente bajo el mínimo de afinidad, salvo en tronco común, exige justificación.
- **MD-07:** una inconsistencia blanda justificada deja de estar abierta.
- **RNF-EQU-01:** cada asignación registra su justificación: afinidad, disponibilidad o excepción.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-72, D-74, D-78, D-83, ADR-010 y ADR-023. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), VI (las reglas son datos), VII (inmutabilidad y trazabilidad), VIII (pruebas primero en el dominio; 80 % en el motor de reglas), XIII (catálogo de errores) y XIV (validación de entradas).

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero guardar una asignación que incumple una regla blanda si escribo la justificación, para resolver excepciones reales sin bloquear el parcial.

**Punto de partida.** Proponer a un docente bajo el mínimo de afinidad ya exige un texto, y queda como «excepción» (F06-H03). Pero esa regla está escrita fuera del motor, no respeta la severidad de la normativa, y sin texto responde como un campo inválido, no como una advertencia.

Esta historia agrega:
- **RN-19 en el motor de reglas,** junto a RN-02 y RN-13.
- **La severidad de la normativa:** si una versión la marca como dura, bloquea.
- **La advertencia:** el sistema dice qué pasa y pide la justificación, sin tratarlo como un error.
- **La base para F10-H01:** una inconsistencia blanda se distingue entre abierta y justificada.

## Clarifications

### Session 2026-11-01

- Q: ¿Qué alcance tiene la historia? → A: Se completa lo que falta, no solo se prueba: RN-19 entra al motor, con su severidad, responde como advertencia y queda la base para F10-H01.
- Q: ¿Qué pasa si una versión marca RN-19 como dura? → A: La normativa manda. Dura bloquea y no se puede justificar; blanda se puede. Hoy es blanda.
- Q: ¿Cuándo se justifica un docente que se aparta de 60/40? → A: En F10-H01, con el panel, porque es una regla global. Corrige lo que se dijo en F07-H01.
- Q: ¿Cómo responde una regla blanda sin justificación? → A: Con una respuesta correcta, con su advertencia, sin guardar, como dice la constitución. Lo confirmó el autor.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recibir una advertencia, no un error (Priority: P1)

Como Coordinador de carrera, si propongo a un docente bajo el mínimo sin justificar, el sistema me advierte y me pide la justificación.

**Why this priority**: es el primer escenario del issue.

**Independent Test**: proponer a un docente bajo el mínimo sin texto, y ver la advertencia y que no se guardó.

**Acceptance Scenarios**:

1. **Dado** un docente bajo el mínimo fuera de tronco común, **cuando** lo propongo sin justificación, **entonces** recibo una advertencia que dice cuál es la regla y qué hacer.
2. **Dado** ese caso, **cuando** recibo la advertencia, **entonces** la propuesta no se guardó ni dejó registro.
3. **Dada** la advertencia, **cuando** la veo en pantalla, **entonces** no se presenta como un error y puedo escribir la justificación sin salir.
4. **Dado** un docente cuyo puntaje bajó del mínimo después de abrir la sugerencia, **cuando** lo propongo sin texto, **entonces** la pantalla me pide la justificación.
5. **Dada** una versión con otro mínimo, **cuando** propongo, **entonces** se evalúa con ese mínimo.

---

### User Story 2 - Guardar con la justificación (Priority: P1)

Como Coordinador de carrera, escribo la justificación y la asignación queda guardada como excepción.

**Why this priority**: es el segundo escenario del issue.

**Independent Test**: proponer al mismo docente con un texto y ver la propuesta como excepción, justificada.

**Acceptance Scenarios**:

1. **Dado** un docente bajo el mínimo, **cuando** lo propongo con una justificación, **entonces** queda guardado con la justificación «excepción» y ese texto.
2. **Dada** esa propuesta, **cuando** la consulto, **entonces** dice que su inconsistencia está justificada, no abierta.
3. **Dada** una justificación demasiado corta o demasiado larga, **cuando** guardo, **entonces** veo qué corregir y no se guarda.
4. **Dado** un docente que alcanza el mínimo, **cuando** lo propongo con texto, **entonces** queda como «afinidad» y el texto no se conserva.
5. **Dada** una asignatura de tronco común, **cuando** propongo a un docente bajo el mínimo, **entonces** queda como «disponibilidad», sin advertencia.

---

### User Story 3 - La normativa decide la severidad (Priority: P2)

Como Vicerrector académico, si una versión de normativa marca RN-19 como dura, nadie la puede saltar con una justificación.

**Why this priority**: es una decisión del autor, pero hoy RN-19 es blanda.

**Independent Test**: con una versión que marca RN-19 como dura, proponer bajo el mínimo con texto y ver que no se guarda.

**Acceptance Scenarios**:

1. **Dada** una versión con RN-19 dura, **cuando** propongo bajo el mínimo con o sin texto, **entonces** no se guarda y veo el motivo en palabras claras.
2. **Dada** esa versión, **cuando** veo la sugerencia, **entonces** los docentes bajo el mínimo salen sin poder proponerse, con su motivo.
3. **Dada** una versión con RN-19 blanda, **cuando** veo la sugerencia, **entonces** esos docentes se pueden proponer con justificación.
4. **Dada** tronco común, **cuando** RN-19 es dura, **entonces** sigue exento.

---

### User Story 4 - Primero las reglas duras (Priority: P2)

Como Coordinador de carrera, no pierdo tiempo justificando algo que igual no se puede guardar.

**Why this priority**: evita escribir un texto inútil.

**Independent Test**: proponer bajo el mínimo a un docente sin horas y ver el error de horas, no la advertencia.

**Acceptance Scenarios**:

1. **Dado** un docente bajo el mínimo y sin horas, **cuando** lo propongo sin texto, **entonces** veo el error de horas, no la advertencia.
2. **Dado** ese docente, **cuando** lo propongo con texto, **entonces** tampoco se guarda.
3. **Dado** un docente bajo el mínimo y con el tope de clase alcanzado, **cuando** lo propongo, **entonces** veo el error del tope.

---

### Edge Cases

- Volver a proponer al mismo docente bajo el mínimo pide otra vez el texto, porque se vuelve a evaluar.
- Dos coordinadores a la vez siguen protegidos por D-83: la advertencia no guarda nada, así que no compite.
- Una justificación solo con espacios cuenta como vacía: responde con la advertencia.
- Si la versión no dice la severidad de RN-19, no se guarda: sin ese dato no se evalúa.
- Una propuesta guardada no se vuelve a evaluar si cambia el mínimo.
- Las actividades no tienen reglas blandas de asignación.

## Requirements *(mandatory)*

### Functional Requirements

**La regla**

- **FR-001**: RN-19 DEBE evaluarse en el motor de reglas, con las reglas duras.
- **FR-002**: Las reglas duras DEBEN evaluarse antes que RN-19.
- **FR-003**: El mínimo y la severidad DEBEN leerse de la versión de normativa que rige el periodo (principio VI).
- **FR-004**: Tronco común DEBE seguir exento: queda como «disponibilidad», sin texto.
- **FR-005**: Quien alcanza el mínimo DEBE quedar como «afinidad», sin conservar un texto enviado.

**La advertencia**

- **FR-006**: Con RN-19 blanda y sin justificación, el sistema DEBE responder de forma correcta con una advertencia y sin guardar.
- **FR-007**: La advertencia DEBE decir cuál es la regla y qué hacer, en palabras claras.
- **FR-008**: Una advertencia NO DEBE dejar nada guardado ni en el registro.
- **FR-009**: Una justificación solo con espacios DEBE tratarse como vacía.
- **FR-010**: Una justificación de 1 a 9 caracteres o de más de 500 DEBE responder como un error de validación del campo.

**La justificación**

- **FR-011**: Con RN-19 blanda y una justificación de 10 a 500 caracteres, DEBE guardarse como «excepción», con su texto.
- **FR-012**: El registro DEBE conservar quién y cuándo, como hoy, sin el texto.
- **FR-013**: Una propuesta DEBE decir si tiene una inconsistencia justificada (MD-07).
- **FR-014**: Una propuesta sin excepción NO DEBE presentarse como inconsistencia.

**Severidad dura**

- **FR-015**: Con RN-19 dura, el sistema NO DEBE guardar bajo el mínimo, con o sin texto.
- **FR-016**: Ese rechazo DEBE tener un código propio del catálogo, en palabras claras (principio XIII).
- **FR-017**: La sugerencia DEBE decir, por candidato bajo el mínimo, si se puede proponer, con la misma regla del guardado.
- **FR-018**: Sin la severidad en la versión, el sistema NO DEBE guardar, y DEBE responder con un mensaje claro.

**La pantalla**

- **FR-019**: La advertencia DEBE mostrarse como advertencia, no como error.
- **FR-020**: Si el servidor pide la justificación sin que la pantalla la haya pedido, DEBE abrirse el campo para escribirla.
- **FR-021**: Lo escrito NO DEBE perderse si la justificación vuelve con un error.
- **FR-022**: La propuesta guardada DEBE mostrar que está justificada.

**Corrección**

- **FR-023**: Los documentos de F07-H01 DEBEN aclarar que 60/40 se justifica en F10-H01.

### Key Entities

- **Inconsistencia**: una regla que una asignación no cumple, dura o blanda, abierta o justificada.
- **Justificación**: afinidad, disponibilidad o excepción. La excepción lleva texto.
- **Advertencia**: lo que responde el sistema ante una inconsistencia blanda sin justificar. No guarda nada.
- **Severidad**: dura o blanda, dato de la versión de normativa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En los casos de prueba, ninguna propuesta bajo el mínimo fuera de tronco común queda guardada sin justificación.
- **SC-002**: Con RN-19 dura, ninguna propuesta bajo el mínimo queda guardada, con o sin texto.
- **SC-003**: Lo que dice la sugerencia y lo que decide el guardado coinciden en el 100 % de los casos de prueba.
- **SC-004**: Un coordinador justifica una propuesta en menos de 1 minuto, sin ayuda.
- **SC-005**: Guardar responde en menos de 3 segundos, con holgura.
- **SC-006**: Las reglas mantienen al menos 80 % de cobertura (RNF-MAN-02).

## Assumptions

- Lo que ya existe no cambia: tronco común exento, texto de 10 a 500 caracteres, y la pantalla sigue abriendo el campo para quien sale bajo el mínimo.
- Con una regla dura violada no se muestra la advertencia: primero hay que resolver eso.
- RN-19 es la única regla blanda que se evalúa al guardar.
- Volver a proponer vuelve a evaluar y pide otra vez el texto.
- F10-H01 leerá de las propuestas qué está justificado.

## Out of Scope

- El panel de inconsistencias y las reglas globales: F10-H01.
- Justificar 60/40: F10-H01.
- Enviar a revisión con 0 inconsistencias.
- Volver a evaluar lo guardado si cambia el mínimo o el puntaje.
