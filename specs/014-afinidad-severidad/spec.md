# Feature Specification: Ajustar pesos y mínimo de afinidad y la severidad de las reglas

**Feature Branch**: `014-afinidad-severidad`

**Created**: 2026-10-20

**Status**: Draft

**Input**: User description: "Especifica la historia F04-H03 · Ajustar pesos y mínimo de afinidad y la severidad de las reglas (issue #42), a partir de RN-19, RN-24, MD-08, el modelo de dominio de Normativa y lo que dejaron F04-H01 y F04-H02."

**Origen**: issue #42 · F04-H03 · sub-issue de F04 (Parametrizar y versionar la política) · milestone «Ola 1 · Datos listos» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:12-normativa`, `RN-19`, `RN-24` · tamaño propuesto M · depende de #41 (F04-H02), cerrada.

**Requerimientos que cubre**: RN-24 (pesos de afinidad parametrizables, con sus valores por defecto), RN-19 en la parte del mínimo configurado, MD-08 (la severidad de cada regla es un parámetro de la versión de normativa) y la ERS §8.3, línea de parámetros sin archivo («los pesos de afinidad (RN-24) y el mínimo de afinidad (RN-19) se configuran en pantallas de administración»). **Decisiones que aplica** (`docs/decisiones.md`): D-67 (el Vicerrector académico escribe «Parámetros y normativa»), ADR-021 (permisos), ADR-016 (catálogo de errores), MD-02 y ADR-010 (el puntaje de afinidad lo calcula Distributivo). **Principios de la constitución**: VI (las cifras y su peso son datos versionados) y VII (una versión en uso no cambia; toda acción deja registro).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero ajustar en una versión de normativa los pesos y el mínimo de afinidad y la severidad de cada regla, para que la institución decida cuánto pesa cada criterio sin cambiar código.

**Punto de partida.** F04-H01 dejó la versión 1 (Política SENESCYT 2024) con 22 filas de la tabla de carga horaria y 7 reglas con 29 parámetros, algunas con severidad (RN-02 y RN-13 duras; RN-05, RN-09 y RN-10 blandas) y otras sin ella (RN-01 y RN-06, que son solo parámetros). F04-H02 permitió crear una versión nueva copiando otra, cambiar sus cifras mientras no rija un periodo y asociarla a un periodo, desde la pantalla «Normativa». Hoy una versión **no tiene pesos ni mínimo de afinidad**, y la severidad de sus reglas **no se puede cambiar**. Esta historia agrega los pesos y el mínimo de afinidad a la versión, con sus valores por defecto, y permite cambiar la severidad de cada regla, por el mismo camino de edición y con las mismas garantías: una versión que rige un periodo no cambia.

## Clarifications

### Session 2026-10-20

- Q: ¿Qué reglas tienen severidad editable? → A: Las 7 que ya tiene la versión 1 y las reglas del motor de la tabla 5.2 del modelo de dominio que faltan: RN-12, RN-16, RN-17, RN-18, RN-19, RN-22, RN-25 y la de la oferta sin propuesta (RF-24), cada una con su severidad por defecto. RN-19 lleva el mínimo de afinidad y RN-24 los pesos, como parámetros; RN-24 es solo un parámetro y no tiene severidad. Las filas de la tabla de carga horaria (RN-03 a RN-15) siguen duras: su doble severidad (dura fuera del rango del RCE, blanda dentro del rango) la define F07. RN-26 queda fuera (Should, F22).
- Q: ¿Qué valores por defecto se usan donde la ERS no da cifra? → A: Los cursos relacionados suman como máximo 3 puntos; el mínimo de afinidad es 2 (un título de tercer nivel en el campo específico, o experiencia relacionada); si solo coincide el campo amplio, el título vale 1 punto menos (cuarto nivel 2, tercer nivel 1).
- Q: ¿Cómo se prueba «el motor la aplica como dura», si el motor llega con F07? → A: Basta con que la severidad cambiada quede en la versión y la consulta del periodo la devuelva. F07 la aplica, con su propia prueba, y lo anota en sus dependencias.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Ver y ajustar los pesos y el mínimo de afinidad (Priority: P1)

Como Vicerrector académico, quiero ver los pesos y el mínimo de afinidad de una versión de normativa y ajustarlos en una versión nueva, para que la institución decida cuánto pesa cada título, experiencia o curso al sugerir docentes (RN-19, RN-24).

**Why this priority**: Es el primer escenario del issue. Sin los pesos en la versión, Distributivo no tendría de dónde leerlos al calcular el puntaje de afinidad (MD-02), y cambiarlos exigiría cambiar código (principio VI).

**Independent Test**: Consultar la versión 1 y comprobar los pesos por defecto de RN-24 y el mínimo de afinidad; crear una versión nueva, cambiar un peso y el mínimo, y comprobar que la nueva tiene los valores nuevos y la 1 sigue igual.

**Acceptance Scenarios**:

```gherkin
Escenario: Valores por defecto
  Dado la versión 1
  Cuando consulto los pesos de afinidad
  Entonces el cuarto nivel en el campo de la asignatura vale 3, el tercer nivel 2, la experiencia relacionada 2 y cada curso relacionado 1, con tope
```

1. **Given** la versión 1, **When** consulto sus pesos de afinidad, **Then** el título de cuarto nivel en el campo de la asignatura vale 3, el de tercer nivel 2, la experiencia laboral relacionada 2 y cada curso o capacitación relacionada 1, hasta su tope, y los cursos suman como máximo 3; si solo coincide el campo amplio, el cuarto nivel vale 2 y el tercer nivel 1; y el mínimo de afinidad es 2.
2. **Given** una versión sin periodo, **When** cambio un peso o el mínimo de afinidad y guardo, **Then** la versión tiene el valor nuevo, queda registrado con su valor anterior, y ninguna otra versión cambia.
3. **Given** un peso o un mínimo inválido (negativo o con más decimales de los admitidos), **When** guardo, **Then** se rechaza con el motivo en español y la versión no cambia.
4. **Given** una versión que rige un periodo, **When** intento cambiar un peso o el mínimo, **Then** se rechaza con 409 y un mensaje que pide crear una versión nueva.

---

### User Story 2 - Cambiar la severidad de una regla (Priority: P1)

Como Vicerrector académico, quiero cambiar en una versión nueva la severidad de una regla, de blanda a dura o de dura a blanda, para que la institución decida qué incumplimientos bloquean y cuáles solo piden justificación (MD-08).

**Why this priority**: Es el segundo escenario del issue y la decisión MD-08 del modelo de dominio: la severidad es un dato de la versión, no del código.

**Independent Test**: En una versión nueva sin periodo, cambiar la severidad de RN-05 de blanda a dura, asociar la versión a un periodo y comprobar que la consulta del periodo devuelve RN-05 como dura, mientras la versión anterior la sigue teniendo blanda.

**Acceptance Scenarios**:

```gherkin
Escenario: Severidad de una regla
  Dado una versión nueva
  Cuando cambio la severidad de una regla de blanda a dura
  Entonces el motor la aplica como dura en los periodos asociados a esa versión
```

1. **Given** una versión nueva sin periodo, **When** cambio la severidad de una regla de blanda a dura y guardo, **Then** la versión la tiene como dura, queda registrado con su valor anterior, y la versión de la que se copió la sigue teniendo blanda.
2. **Given** esa versión asociada a un periodo, **When** se consulta qué versión rige el periodo y las reglas de esa versión, **Then** la regla aparece como dura. El motor de reglas que la aplica llega con F07, que lo prueba con esta consulta.
3. **Given** una regla que es solo un parámetro (RN-01, RN-06, RN-24), **When** intento darle severidad, **Then** se rechaza con el motivo en español.
4. **Given** una versión que rige un periodo, **When** intento cambiar una severidad, **Then** se rechaza con 409, como cualquier otro cambio.

---

### User Story 3 - Ajustar en pantalla (Priority: P1)

Como Vicerrector académico, quiero ajustar los pesos, el mínimo y las severidades en la pantalla «Normativa», para no depender de un archivo (ERS §8.3: estos parámetros se configuran en pantalla, no por archivo).

**Why this priority**: La ERS dice que estos parámetros se configuran en pantallas de administración; sin la pantalla, solo se podrían cambiar llamando al servidor.

**Independent Test**: En la pantalla «Normativa», con una versión sin periodo, editar un peso, el mínimo y la severidad de una regla, guardar y comprobar que se ven al volver a abrirla; con una versión en uso, ver los mismos datos en solo lectura.

**Acceptance Scenarios**:

1. **Given** cualquier versión, **When** la abro en «Normativa», **Then** veo sus pesos de afinidad, su mínimo de afinidad y la severidad de cada regla, con nombres en español.
2. **Given** una versión sin periodo y el permiso de escritura, **When** edito en pantalla un peso, el mínimo o una severidad y guardo, **Then** el cambio queda en la versión y se ve al volver a abrirla.
3. **Given** un valor inválido, **When** guardo, **Then** veo el motivo junto al campo, en español, y no se guarda nada.

---

### Edge Cases

- **Qué reglas tienen severidad**: las reglas del motor de la versión: RN-02, RN-05, RN-09, RN-10 y RN-13, que ya estaban, y RN-12, RN-16, RN-17, RN-18, RN-19, RN-22, RN-25 y la de la oferta sin propuesta (RF-24), que se agregan con la severidad por defecto de la tabla 5.2. RN-01, RN-06 y RN-24 son solo parámetros. Las filas de la tabla de carga horaria siguen duras y no se editan aquí (F07).
- **Reglas nuevas sin parámetros**: varias reglas del motor que se agregan (por ejemplo, RN-17, «ningún docente queda sin horas de clase») no tienen cifras; solo tienen su severidad.
- **Pesos que se cruzan**: si el peso del campo amplio queda mayor que el del campo específico en un mismo nivel (por ejemplo, cuarto nivel amplio 4 y específico 3), se rechaza con el motivo. Iguales es válido. Un tope menor que un curso, o un mínimo mayor que la suma de los pesos, son decisiones de la institución y no se rechazan.
- **Versiones que ya existen**: las reglas nuevas, los pesos y el mínimo por defecto se agregan a las versiones que todavía no los tienen, empezando por la 1, sin cambiar ningún otro valor. Hoy ninguna versión rige un periodo (los periodos llegan con F02-H01), así que agregarlos no rompe el principio VII.
- **Versión nueva**: copia los pesos, el mínimo y las severidades de la versión de origen, tal como F04-H02 copia el resto.
- **Sin cambios**: guardar una versión con los mismos valores no deja registro, como en F04-H02.
- **Severidad «sin severidad»**: una regla que es solo un parámetro no tiene severidad y no puede recibirla; una regla del motor siempre tiene una, dura o blanda, y no se le puede quitar.
- **Tronco común**: la excepción del mínimo para las asignaturas de tronco común (RN-19) es un dato de la asignatura (Datos maestros, carga 4), no de la versión; esta historia no la cambia.
- **Usuario sin permiso**: quien no tenga el permiso de escritura de «Parámetros y normativa» ve los pesos, el mínimo y las severidades en solo lectura, y el servidor responde «Acceso no autorizado» (403) si intenta cambiarlos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Cada versión de normativa DEBE tener los pesos de afinidad de RN-24 (título de cuarto nivel y de tercer nivel, cada uno cuando coincide el campo específico de la asignatura y cuando solo coincide el campo amplio; experiencia laboral relacionada; curso o capacitación relacionada, y el tope de los cursos) y el mínimo de afinidad de RN-19.
- **FR-002**: La versión 1 y las versiones que ya existen DEBEN quedar con los valores por defecto: cuarto nivel 3 en el campo específico y 2 si solo coincide el amplio; tercer nivel 2 y 1; experiencia 2; curso 1, con tope de 3; mínimo de afinidad 2. También DEBEN ganar las reglas del motor que les faltan (RN-12, RN-16, RN-17, RN-18, RN-19, RN-22, RN-25 y la de la oferta sin propuesta) con la severidad por defecto de la tabla 5.2 del modelo de dominio. Ningún otro valor DEBE cambiar.
- **FR-003**: Mientras una versión no rija ningún periodo, se DEBEN poder cambiar sus pesos de afinidad, su mínimo de afinidad y la severidad (dura o blanda) de sus reglas del motor, por la misma forma de cambiar una versión de F04-H02 (FR-010 de F04-H02).
- **FR-004**: Los pesos y el mínimo DEBEN ser números no negativos, con la misma precisión que los demás parámetros. Para cada nivel de título, el peso del campo amplio NO DEBE superar al del campo específico (RN-24: «los títulos cuentan más si coincide el campo específico»). Un valor inválido se rechaza entero, con el motivo en español y sin dejar la versión a medias.
- **FR-005**: Una regla que es solo un parámetro (RN-01, RN-06, RN-24) NO DEBE tener severidad; una regla del motor DEBE tener siempre una, dura o blanda. Un cambio que no cumpla esto se rechaza con el motivo en español.
- **FR-006**: Una versión que rige un periodo NO DEBE cambiar sus pesos, su mínimo ni sus severidades: el intento responde 409 con el mismo código y mensaje de F04-H02.
- **FR-007**: Una versión nueva DEBE copiar los pesos, el mínimo y las severidades de su versión de origen, y cambiarlos NO DEBE alterar ninguna otra versión.
- **FR-008**: La consulta del periodo DEBE decir qué versión lo rige, y la consulta de esa versión DEBE devolver sus pesos de afinidad, su mínimo y la severidad de cada regla, para que Distributivo los lea al calcular el puntaje y al validar (MD-02). La consulta propia de las reglas del periodo, desde el proceso de Distributivo, la define F07.
- **FR-009**: La pantalla «Normativa» DEBE mostrar los pesos, el mínimo y las severidades de la versión elegida, con nombres en español, y permitir editarlos a quien tenga el permiso de escritura cuando la versión no rige un periodo.
- **FR-010**: Cambiar pesos, mínimo o severidades DEBE exigir el permiso de escritura de «Parámetros y normativa», validado en el servidor (D-65, D-67, ADR-021), y cada cambio DEBE quedar registrado con quién, cuándo, qué campo, valor anterior y nuevo (principio VII).

### Key Entities

- **Versión de normativa**: como en F04-H02; gana sus pesos de afinidad, su mínimo de afinidad y la severidad editable de cada regla del motor.
- **Pesos de afinidad**: puntos que suma cada título, experiencia o curso relacionado con la asignatura (glosario §7.3, `PesosAfinidad`, RN-24).
- **Mínimo de afinidad**: puntaje desde el cual se sugiere un docente sin justificación (glosario §7.3, `MinimoAfinidad`, RN-19).
- **Regla** y **Severidad**: identificador RN-xx, parámetros y severidad dura o blanda; la lógica está en el código, las cifras y la severidad son datos (modelo de dominio §5.1, MD-08).
- **Cambio de valor**: como en F04-H02, también para los pesos, el mínimo y las severidades.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El Vicerrector académico cambia en pantalla un peso de afinidad y la severidad de una regla de una versión nueva en menos de 2 minutos, sin ayuda.
- **SC-002**: La versión 1 muestra los pesos por defecto de RN-24, su tope, el mínimo de afinidad y las 8 reglas del motor agregadas con su severidad por defecto, y el 100 % de sus demás valores sigue idéntico al de antes de esta historia.
- **SC-003**: El 100 % de los intentos de cambiar pesos, mínimo o severidades de una versión que rige un periodo se rechazan con 409, también llamando directamente al servidor.
- **SC-004**: Después de cambiar la severidad de una regla en una versión nueva, la consulta de los periodos asociados lleva a esa versión, y la consulta de la versión devuelve la severidad nueva, y la versión de origen la conserva.
- **SC-005**: Cada cambio de un peso, del mínimo o de una severidad aparece en el registro con quién, cuándo, el valor anterior y el nuevo.

## Assumptions

- Los pesos y el mínimo de afinidad son parámetros de la versión de normativa, como los de RN-01 a RN-15, y se versionan igual (modelo de dominio §5.1).
- El cálculo del puntaje de afinidad, las sugerencias y la justificación de una asignación bajo el mínimo son de Distributivo (MD-02, ADR-010; F06 y F07); esta historia solo guarda y entrega los valores.
- Quiénes cambian estos valores no cambia: el Administrador del sistema y el Vicerrector académico (D-65, D-67).
- Los valores por defecto que la ERS no da (el tope de los cursos, el mínimo y los pesos del campo amplio) los fijó el autor el 2026-10-20 y se ajustan con una versión nueva.
- Las severidades por defecto marcadas (I) en la tabla 5.2 del modelo de dominio se toman tal cual; se confirman al especificar F07 y F10 (MD-08) y, si cambian, se ajustan con una versión nueva.
- Estos parámetros se cambian en pantalla, no por archivo (ERS §8.3); la carga 7 por archivo (F01) sigue cubriendo solo la tabla de carga horaria.
- Los datos son ficticios o públicos.

## Dependencies

- F04-H02 (#41), cerrada: crear, editar y asociar versiones; el registro de cambios; la pantalla «Normativa» con edición.
- F04-H01 (#40), cerrada: la versión 1 y sus reglas.
- F05-H02 (#44), cerrada, y D-67: el permiso de escritura de «Parámetros y normativa».
- F06 y F07 (Distributivo) dependerán de esta historia para leer los pesos, el mínimo y las severidades del periodo; F07 prueba que el motor aplica la severidad que devuelve la consulta del periodo.
