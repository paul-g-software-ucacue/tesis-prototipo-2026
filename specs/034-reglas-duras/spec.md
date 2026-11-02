# Feature Specification: Impedir una asignación que viola una regla dura

**Feature Branch**: `034-reglas-duras`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Especifica la historia F07-H02 · Impedir una asignación que viola una regla dura (issue #51), a partir de RF-28, RN-02, RN-13, RN-25 y MD-01, con las cuatro aclaraciones del autor."

**Origen**: issue #51 · F07-H02 · sub-issue de F07 (Validar cada asignación al instante, con los totales del docente, #9) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · tamaño L

**Requerimientos que cubre**:
- **RF-28:** validar cada asignación contra las reglas parametrizadas, con el total del docente en todas las carreras y unidades. Una violación dura bloquea.
- **RN-02:** el total de un docente no supera lo que le corresponde, y en ningún caso 40 h.
- **RN-13:** un docente con proyecto de investigación o vinculación aprobado tiene como máximo 16 h de docencia.
- **RN-25:** investigación, vinculación y tutoría de prácticas solo contra un proyecto aprobado y vigente.
- **MD-01:** todas las asignaciones de una versión forman un solo agregado, y la regla se garantiza al guardar.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-01, D-72, D-74, D-75, D-77, D-78, ADR-010 y ADR-023. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), VII (inmutabilidad y trazabilidad), VIII (pruebas primero en el dominio; 80 % en el motor de reglas), IX (mínimo privilegio), XIII (catálogo de errores) y XIV (validación de entradas).

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero que el sistema no guarde una asignación que viola una regla dura, para no dejar violaciones de la política en mi parcial.

**Punto de partida.** Hoy un coordinador puede pasarse de las horas de un docente: se guarda, con un aviso (D-78). Los totales ya dicen cuánto se pasó, pero nada lo impide. Dos coordinadores que asignan a la vez tampoco se ven entre sí.

Esta historia agrega:
- **El bloqueo de horas:** una asignación que deja al docente por encima de lo que le corresponde no se guarda.
- **El tope de 16 h de clase** para quien participa en un proyecto de investigación o vinculación.
- **La seguridad con dos coordinadores a la vez:** solo se guarda lo que cabe.
- **La sugerencia al día:** se ve a quién no se le puede proponer y por qué.

## Clarifications

### Session 2026-11-01

- Q: ¿Cuál es el límite exacto de cada docente? → A: Sus horas contratadas, con el tope de 40 h de la normativa. Es lo que ya muestran el panel y la sugerencia (D-75).
- Q: ¿Qué horas cuentan para las 16 h de RN-13? → A: Solo las horas de clase, como dice la tabla de carga de la política. Las otras actividades de docencia no cuentan.
- Q: ¿Quién tiene el tope de 16 h? → A: Quien participa en un proyecto de investigación o de vinculación asignable en el periodo. Prácticas preprofesionales y servicio comunitario no lo activan.
- Q: ¿Qué pasa con un docente que ya estaba excedido? → A: Lo guardado se conserva. Se puede quitar o reducir, aunque siga excedido, pero no agregar ni aumentar.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - No pasarse de las horas del docente (Priority: P1)

Como Coordinador de carrera, intento guardar una asignación que supera las horas del docente, y el sistema no la guarda y me dice por qué.

**Why this priority**: es la historia, y la regla más importante del distributivo.

**Independent Test**: con un docente al que le quedan 2 h, proponerlo para un paralelo de 4 h y ver que no se guarda.

**Acceptance Scenarios**:

1. **Dado** un docente con 38 de 40 horas asignadas, **cuando** le propongo una asignatura de 4 horas, **entonces** no se guarda y veo «Horas laborales excedidas», con las horas que le quedan.
2. **Dado** un docente con 30 horas en otra carrera, **cuando** le asigno 12 horas en mi carrera, **entonces** no se guarda, porque se valida el total en todas las carreras.
3. **Dado** un docente con 20 horas contratadas y 18 asignadas, **cuando** le asigno 3 horas de una actividad, **entonces** no se guarda.
4. **Dado** un docente con horas contratadas sobre el tope de la normativa, **cuando** una asignación lo lleva sobre ese tope, **entonces** no se guarda.
5. **Dada** una asignación que llega justo al límite, **cuando** la guardo, **entonces** se guarda.
6. **Dada** una actividad que reemplazo por otras horas, **cuando** guardo, **entonces** se cuentan las horas nuevas y no las anteriores.
7. **Dada** una propuesta que reemplazo por otro docente, **cuando** guardo, **entonces** se valida al docente nuevo, sin contar esas horas al anterior.

---

### User Story 2 - El tope de 16 h con proyecto (Priority: P1)

Como Coordinador de carrera, no puedo darle más de 16 h de clase a un docente con proyecto de investigación o vinculación.

**Why this priority**: es el tercer escenario del issue y una regla dura de la política.

**Independent Test**: con un docente en un proyecto de investigación y 14 h de clase, proponerlo para un paralelo de 4 h y ver que no se guarda.

**Acceptance Scenarios**:

1. **Dado** un docente con un proyecto de investigación y 16 horas de clase, **cuando** le propongo más horas de clase, **entonces** no se guarda y el mensaje explica el tope.
2. **Dado** ese docente, **cuando** le asigno otras actividades de docencia, **entonces** se guardan si le alcanzan las horas.
3. **Dado** un docente solo con un proyecto de prácticas o de servicio comunitario, **cuando** le propongo más de 16 horas de clase, **entonces** se guarda si le alcanzan las horas.
4. **Dado** un proyecto no asignable en el periodo, **cuando** le propongo clases a su participante, **entonces** el tope no aplica.
5. **Dada** una versión de normativa con otro tope, **cuando** valido un periodo que la usa, **entonces** se aplica ese tope.

---

### User Story 3 - Dos coordinadores a la vez (Priority: P1)

Como Coordinador de carrera, si otro coordinador asigna al mismo docente al mismo tiempo, solo se guarda lo que cabe.

**Why this priority**: es el quinto escenario del issue. Sin esto, el bloqueo se puede saltar.

**Independent Test**: enviar a la vez dos asignaciones cuya suma supera las horas, y ver que solo una queda guardada.

**Acceptance Scenarios**:

1. **Dados** dos coordinadores que asignan al mismo docente a la vez, y la suma supera sus horas, **cuando** los dos guardan, **entonces** solo se guarda una, y la otra recibe el error de horas excedidas.
2. **Dados** dos coordinadores que asignan a la vez y la suma cabe, **cuando** los dos guardan, **entonces** se guardan las dos.
3. **Dados** dos coordinadores con docentes distintos, **cuando** guardan a la vez, **entonces** no se bloquean entre sí.

---

### User Story 4 - Saber a quién se le puede proponer (Priority: P2)

Como Coordinador de carrera, veo en la sugerencia a quién no le caben las horas o tiene el tope, antes de intentarlo.

**Why this priority**: evita el intento fallido, pero el bloqueo ya protege.

**Independent Test**: abrir la sugerencia de un paralelo con un candidato sin horas, y ver el motivo y que no se puede proponer.

**Acceptance Scenarios**:

1. **Dado** un candidato sin horas suficientes, **cuando** veo la sugerencia, **entonces** sigue en la lista, con su motivo, y no lo puedo proponer.
2. **Dado** un candidato con el tope de 16 h alcanzado, **cuando** veo la sugerencia, **entonces** dice el motivo, y no lo puedo proponer.
3. **Dado** el formulario de actividades, **cuando** el docente no tiene horas para lo que escribo, **entonces** al guardar veo el motivo y el formulario sigue abierto.
4. **Dado** un candidato con horas, **cuando** veo la sugerencia, **entonces** lo puedo proponer.
5. **Dado** un candidato con mayor puntaje que tiene el tope alcanzado, **cuando** veo la sugerencia, **entonces** aparece después de los que se pueden proponer.

---

### User Story 5 - Corregir a un docente ya excedido (Priority: P2)

Como Coordinador de carrera, puedo bajar las horas de un docente que quedó excedido antes de esta regla.

**Why this priority**: sin esto, un dato anterior no se podría corregir de a poco.

**Independent Test**: con un docente excedido, reducir una actividad, y ver que se guarda aunque siga excedido.

**Acceptance Scenarios**:

1. **Dado** un docente excedido, **cuando** reduzco las horas de una actividad, **entonces** se guarda, aunque siga excedido.
2. **Dado** un docente excedido, **cuando** quito una asignación, **entonces** se quita.
3. **Dado** un docente excedido, **cuando** le aumento una actividad o le agrego una asignación, **entonces** no se guarda.
4. **Dado** un docente excedido, **cuando** vuelvo a guardar su propuesta con las mismas horas, **entonces** se guarda.

---

### User Story 6 - Las horas sin proyecto (Priority: P3)

Como Coordinador de carrera, no puedo guardar horas de investigación o vinculación sin un proyecto aprobado y vigente.

**Why this priority**: es el cuarto escenario del issue, y ya funciona desde F06-H04. Aquí queda probado junto a las demás.

**Independent Test**: asignar horas de investigación a un docente sin proyecto y ver que no se guarda.

**Acceptance Scenarios**:

1. **Dado** un docente sin proyecto aprobado y vigente, **cuando** le asigno horas de investigación, **entonces** no se guarda.

---

### Edge Cases

- Las horas con dos decimales se comparan sin perder precisión (D-77).
- Un paralelo retirado no cuenta en las horas del docente.
- Si una asignación viola las dos reglas, el mensaje dice el de las horas.
- Un docente con 0 horas contratadas no puede recibir asignaciones.
- Las horas se validan con la oferta del momento de guardar.
- Una propuesta sobre el mismo paralelo y el mismo docente no suma dos veces.
- Si no se puede leer la normativa, Docentes o la oferta, no se guarda: sin datos no se valida.
- Una regla sin sus cifras en la versión de normativa no se da por cumplida: no se guarda.

## Requirements *(mandatory)*

### Functional Requirements

**Las horas**

- **FR-001**: El sistema NO DEBE guardar una propuesta ni una actividad que deje al docente por encima de su límite.
- **FR-002**: El límite de un docente DEBE ser el menor entre sus horas contratadas y el tope de horas de la versión de normativa que rige el periodo.
- **FR-003**: La validación DEBE usar el total del docente en todas las carreras y unidades (D-01), con las horas de esta versión del distributivo.
- **FR-004**: La validación DEBE contar las horas con la misma forma que los totales (F07-H01).
- **FR-005**: Llegar justo al límite DEBE guardarse.
- **FR-006**: Al reemplazar, DEBEN contarse las horas nuevas y no las anteriores.

**El tope de clase**

- **FR-007**: Un docente que participa en un proyecto de investigación o de vinculación asignable en el periodo NO DEBE superar el tope de horas de clase de la normativa.
- **FR-008**: El tope DEBE leerse de la versión de normativa que rige el periodo (principio VI).
- **FR-009**: Solo las horas de clase DEBEN contar para ese tope.
- **FR-010**: Los proyectos de prácticas preprofesionales y de servicio comunitario NO DEBEN activar el tope.

**Lo que ya estaba**

- **FR-011**: Un docente excedido DEBE conservar lo guardado.
- **FR-012**: El sistema DEBE permitir quitar o reducir las horas de un docente excedido, aunque siga excedido.
- **FR-013**: El sistema NO DEBE permitir agregar ni aumentar horas a un docente excedido.
- **FR-014**: Volver a validar lo ya guardado queda fuera: una asignación no cambia por un dato que cambió después.

**A la vez**

- **FR-015**: Si dos asignaciones del mismo docente llegan a la vez y la suma no cabe, DEBE guardarse solo una.
- **FR-016**: La otra DEBE recibir el mismo error de horas, sin un error técnico.
- **FR-017**: Dos asignaciones de docentes distintos NO DEBEN impedirse entre sí.

**Lo que se ve**

- **FR-018**: Cada regla violada DEBE responder con un código propio del catálogo, en palabras claras, con qué hacer (principio XIII).
- **FR-019**: El mensaje de horas excedidas DEBE decir cuántas horas le quedan al docente.
- **FR-020**: La sugerencia DEBE decir, por candidato, si se le puede proponer y por qué no, con la misma regla del guardado.
- **FR-021**: En la pantalla NO DEBE poderse proponer a un candidato que no se puede proponer. El servidor DEBE rechazarlo igual.
- **FR-022**: El formulario de actividades DEBE mostrar el motivo y seguir abierto, sin perder lo escrito.
- **FR-023**: El aviso de que el docente no tiene horas DEBE desaparecer, porque ya no se puede guardar en ese caso.

**Errores**

- **FR-024**: Sin la normativa, los datos del docente o la oferta, el sistema NO DEBE guardar, y DEBE responder con un mensaje claro antes de 10 segundos.
- **FR-025**: Una regla sin sus cifras en la versión DEBE impedir guardar, con un mensaje claro.
- **FR-026**: Un intento rechazado NO DEBE dejar nada guardado.
- **FR-027**: RN-25 DEBE seguir cumpliéndose al guardar, y quedar probada junto a las demás reglas.
- **FR-028**: La sugerencia DEBE mostrar primero a quienes se pueden proponer. Dentro de cada grupo se mantiene el orden actual: puntaje, horas disponibles y nombre.

### Key Entities

- **Regla dura**: una regla de la versión de normativa que bloquea. Aquí, RN-02 y RN-13, con sus cifras como dato.
- **Límite del docente**: el menor entre sus horas contratadas y el tope de horas.
- **Carga horaria**: los totales de F07-H01, que la validación usa sin duplicar la cuenta.
- **Proyecto**: ya existe en Docentes. Aquí importa si es de investigación o vinculación y si es asignable en el periodo.
- **Versión del distributivo**: el conjunto de asignaciones que se protege de dos guardados a la vez.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En los casos de prueba, ninguna asignación deja a un docente por encima de su límite, en una o en varias carreras.
- **SC-002**: En 20 pares de intentos a la vez de dos coordinadores, ninguno deja al docente excedido, y todos los rechazados reciben el error de horas.
- **SC-003**: Lo que dice la sugerencia y lo que decide el guardado coinciden en el 100 % de los casos de prueba.
- **SC-004**: Un coordinador entiende por qué no se guardó y qué hacer, sin ayuda, en menos de 1 minuto.
- **SC-005**: Guardar una asignación responde en menos de 3 segundos, con holgura.
- **SC-006**: Con un servicio caído, el usuario ve un mensaje claro en menos de 10 segundos, con holgura, y no se guarda nada.
- **SC-007**: Las reglas tienen al menos 80 % de cobertura (RNF-MAN-02).

## Assumptions

- La regla aplica a proponer y a asignar actividades.
- Una justificación no levanta una regla dura: solo las reglas blandas se justifican (F07-H03).
- RN-25 ya funciona desde F06-H04 y aquí se comprueba.
- Si una asignación viola las dos reglas, el mensaje dice el de las horas, que suele ser lo primero que hay que resolver.
- Una versión de normativa puede cambiar los dos topes.
- Cambiar las horas de un paralelo en la oferta no se bloquea aquí: puede dejar a un docente excedido, y lo señalará F10-H01.
- Los totales de F07-H01 no cambian. El exceso solo podrá venir de datos anteriores o de cambios en la oferta.
- Consultar no deja registro. Un intento rechazado tampoco, porque no cambió nada.

## Out of Scope

- Las reglas blandas y su justificación: F07-H03.
- Las reglas globales y el panel de inconsistencias: F10-H01.
- Volver a validar lo guardado cuando cambia un proyecto, un cargo o la oferta.
- RN-03 y la tabla de carga por perfil, que dependen del cargo.
- Las condiciones especiales.
