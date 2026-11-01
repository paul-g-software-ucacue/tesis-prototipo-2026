# Feature Specification: Proponer un docente por asignatura-paralelo

**Feature Branch**: `031-proponer-docente`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Especifica la historia F06-H03 · Proponer un docente por asignatura-paralelo (issue #48), a partir de RF-24, RF-25, RF-56, D-19, RNF-EQU-01 y RN-19, con las seis aclaraciones del autor del 2026-10-31."

**Origen**: issue #48 · F06-H03 · sub-issue de F06 (Proponer un docente por asignatura con sugerencias por afinidad, #8) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:05-generacion`, `RF-24`, `RF-25` · tamaño propuesto L · depende de #47 (F06-H02) y #166 (F06-H05), cerradas · va antes que #51 (F07-H02), por decisión del autor.

**Requerimientos que cubre**:
- **RF-24:** cada coordinador propone un docente por asignatura y paralelo. Hay una sola propuesta (D-19).
- **RF-25:** el coordinador elige desde la sugerencia, con las horas reales del docente en otras carreras y unidades.
- **RF-56:** el distributivo se crea solo con las cargas obligatorias del periodo.
- **RN-19:** proponer bajo el mínimo exige justificación, salvo en tronco común.
- **RNF-EQU-01:** cada asignación registra su justificación: afinidad, disponibilidad o excepción.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-01, D-16, D-19, D-64, D-65, D-69, D-70, D-72, D-74, D-75, ADR-010 y ADR-023. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), VII (inmutabilidad y trazabilidad), IX (mínimo privilegio), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero proponer un docente para cada asignatura-paralelo de mi carrera, para armar el parcial de mi carrera.

**Punto de partida.** El coordinador ya ve los docentes sugeridos, pero no puede elegir a ninguno. El distributivo todavía no existe: el botón «Crear distributivo» solo avisa. Y como no hay asignaciones, todos los docentes tienen 0 h asignadas.

Esta historia agrega:
- **El distributivo del periodo,** con su versión 1 y un parcial por carrera.
- **La propuesta de un docente,** con su justificación.
- **Las horas reales** de cada docente, que reemplazan a las 0 h de F06-H02.
- **Los docentes bajo el mínimo,** en una sección aparte.

## Clarifications

### Session 2026-10-31

- Q: ¿Cómo nace el distributivo? → A: El Vicerrector pulsa «Crear distributivo» cuando el periodo está listo. Nacen la versión 1 y un parcial por carrera.
- Q: ¿Quién propone? → A: Solo el Coordinador de carrera, en su carrera. El Vicerrector crea y ve; sus ajustes llegan con la revisión (RF-35).
- Q: ¿Qué pasa con un docente sin horas suficientes? → A: La propuesta se guarda con un aviso. F07-H02 lo convierte en bloqueo.
- Q: ¿Cómo se llega a un docente bajo el mínimo? → A: En una sección desplegable «Docentes bajo el mínimo», debajo de los sugeridos, con su puntaje. Al elegir uno, la justificación escrita es obligatoria.
- Q: ¿Qué justificación lleva una propuesta en tronco común? → A: Sin texto: «afinidad» si alcanza el mínimo, y «disponibilidad» si no.
- Q: ¿Qué pasa si se retira una asignatura-paralelo con propuesta? → A: La propuesta se conserva, marcada «Paralelo retirado». No suma horas ni cuenta para el parcial, y el coordinador puede quitarla. Si vuelve a ofertarse, sigue valiendo.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Crear el distributivo (Priority: P1)

Como Vicerrector académico, quiero crear el distributivo de un periodo listo, para que las carreras empiecen a armar sus parciales (RF-56).

**Why this priority**: Sin distributivo no hay dónde guardar una propuesta.

**Independent Test**: Con un periodo listo, pulsar «Crear distributivo» y comprobar que el periodo tiene su versión 1 y un parcial por cada carrera con oferta.

**Acceptance Scenarios**:

1. **Given** un periodo listo en el tablero, **When** el Vicerrector pulsa «Crear distributivo», **Then** nacen el distributivo, su versión 1 editable y un parcial en elaboración por cada carrera con oferta.
2. **Given** un periodo que no está listo, **When** se intenta crear, **Then** el sistema lo rechaza con un error del catálogo que dice que revises lo que falta en el tablero.
3. **Given** un periodo con distributivo, **When** se intenta crear otra vez, **Then** el sistema lo rechaza, y el tablero dice que ya existe.
4. **Given** un usuario sin «Distributivo: escritura», **When** intenta crearlo, **Then** recibe «no autorizado».
5. **Given** el tablero de un periodo con distributivo, **When** se abre, **Then** dice que el distributivo ya está creado, en lugar de ofrecer crearlo.

---

### User Story 2 - Proponer un docente (Priority: P1)

Como Coordinador de carrera, quiero elegir un docente desde la sugerencia, para dejar resuelta esa asignatura-paralelo (RF-24).

**Why this priority**: Es el primer escenario del issue y el valor de la historia.

**Independent Test**: Con un distributivo creado, abrir la sugerencia de un paralelo, proponer un candidato y verlo en la fila de la oferta.

**Acceptance Scenarios**:

```gherkin
Escenario: Una propuesta por asignatura-paralelo
  Dado una asignatura-paralelo sin propuesta
  Cuando propongo a un docente con afinidad suficiente
  Entonces queda una sola propuesta para esa asignatura-paralelo, con la justificación "afinidad"
```

1. **Given** una asignatura-paralelo sin propuesta y un candidato que alcanza el mínimo, **When** lo propongo, **Then** queda una sola propuesta, con la justificación «afinidad», y la fila de la oferta la muestra.
2. **Given** una asignatura-paralelo con propuesta, **When** propongo a otro docente, **Then** la nueva reemplaza a la anterior: nunca hay dos, y el cambio queda registrado.
3. **Given** una propuesta, **When** el coordinador la quita, **Then** la asignatura-paralelo vuelve a quedar sin propuesta, y queda registrado.
4. **Given** un docente al que no le alcanzan las horas, **When** lo propongo, **Then** la propuesta se guarda, y un aviso dice que no le alcanzan.
5. **Given** un docente que dejó de estar activo, **When** se intenta proponer, **Then** el sistema lo rechaza con un error del catálogo.
6. **Given** un periodo sin distributivo, **When** el coordinador abre la sugerencia, **Then** la ve, pero el sistema dice que todavía no se puede proponer.
7. **Given** una asignatura-paralelo de otra carrera, **When** el coordinador intenta proponer, **Then** recibe «no autorizado», sin datos.
8. **Given** un usuario sin «Oferta y propuesta de docentes: escritura», como el Vicerrector, **When** abre la sugerencia, **Then** ve la lista sin la opción de proponer.
9. **Given** una asignatura-paralelo retirada, **When** se intenta proponer, **Then** el sistema lo rechaza con el error que ya existe.

---

### User Story 3 - Justificar la propuesta (Priority: P1)

Como Coordinador de carrera, quiero que cada propuesta diga por qué se hizo, para que la elección se pueda explicar (RN-19, RNF-EQU-01).

**Why this priority**: Es el segundo escenario del issue.

**Independent Test**: Proponer a un docente bajo el mínimo con y sin texto, y a uno de tronco común con y sin puntaje suficiente.

**Acceptance Scenarios**:

```gherkin
Escenario: Docente bajo el mínimo
  Dado un docente con puntaje bajo el mínimo de afinidad
  Cuando lo propongo
  Entonces el sistema exige una justificación escrita antes de guardar
```

1. **Given** una asignatura sin tronco común, **When** abro la sugerencia, **Then** una sección cerrada «Docentes bajo el mínimo» lista a los que no lo alcanzan, con su puntaje y sus horas.
2. **Given** un docente bajo el mínimo, **When** lo propongo sin texto, **Then** no se guarda y el sistema pide la justificación.
3. **Given** un docente bajo el mínimo, **When** lo propongo con texto, **Then** se guarda con la justificación «excepción» y ese texto.
4. **Given** un candidato que alcanza el mínimo, **When** lo propongo, **Then** no se pide texto, y queda como «afinidad».
5. **Given** una asignatura de tronco común y un docente con el mínimo, **When** lo propongo, **Then** queda como «afinidad», sin texto.
6. **Given** una asignatura de tronco común y un docente bajo el mínimo, **When** lo propongo, **Then** queda como «disponibilidad», sin texto.
7. **Given** una propuesta guardada, **When** veo su fila, **Then** veo su justificación y, si es una excepción, su texto.
8. **Given** una llamada que manda texto en una propuesta que no lo pide, **When** se guarda, **Then** el texto no se conserva: la justificación la decide el sistema.

---

### User Story 4 - Ver el parcial de mi carrera (Priority: P1)

Como Coordinador de carrera, quiero ver cuánto me falta, para saber cuándo termino mi parcial.

**Why this priority**: Es el tercer escenario del issue.

**Independent Test**: Proponer en todos los paralelos de una carrera y ver que el parcial dice que está completo.

**Acceptance Scenarios**:

```gherkin
Escenario: Parcial completo
  Dado todas las asignaturas-paralelo de la oferta de mi carrera
  Cuando termino de proponer
  Entonces cada asignatura-paralelo tiene un docente propuesto
```

1. **Given** un parcial a medias, **When** abro la oferta, **Then** veo cuántas asignaturas-paralelo tienen propuesta, de cuántas ofertadas.
2. **Given** todas las ofertadas con propuesta, **When** abro la oferta, **Then** el parcial dice que está completo.
3. **Given** una asignatura-paralelo retirada con propuesta, **When** abro la oferta, **Then** se ve marcada «Paralelo retirado», no cuenta para el parcial, y el coordinador puede quitarla.
4. **Given** esa asignatura-paralelo otra vez ofertada, **When** abro la oferta, **Then** su propuesta sigue valiendo y vuelve a contar.
5. **Given** una asignatura-paralelo nueva en la oferta, **When** abro la oferta, **Then** cuenta como pendiente.

---

### User Story 5 - Ver las horas reales (Priority: P1)

Como Coordinador de carrera, quiero ver las horas que ya tiene cada docente, para no contar con horas que no están libres (D-01).

**Why this priority**: Cierra el escenario «Horas en otras carreras» de F06-H02, que hasta ahora solo se probó con datos simulados.

**Independent Test**: Proponer a un docente en una carrera, abrir la sugerencia en otra y ver sus horas.

**Acceptance Scenarios**:

1. **Given** un docente propuesto en otra carrera, **When** aparece en mi sugerencia, **Then** veo esas horas en otras carreras y unidades, y sus horas disponibles bajan.
2. **Given** un docente propuesto en mi carrera, **When** aparece en la sugerencia de otro paralelo, **Then** sus horas disponibles bajan, pero no cuentan como otras carreras.
3. **Given** la propuesta de la misma asignatura-paralelo que estoy viendo, **When** aparece ese docente, **Then** esas horas no le restan: se pueden reemplazar.
4. **Given** una propuesta de un paralelo retirado, **When** se calculan las horas, **Then** no cuenta.

---

### Edge Cases

- **Dos coordinadores de la misma carrera proponen a la vez en la misma asignatura-paralelo:** queda una sola propuesta, la última.
- **Dos coordinadores proponen al mismo docente a la vez:** las dos se guardan. El control con bloqueo es de F07-H02.
- **Se corrigen las horas del paralelo después de proponer:** la propuesta cuenta las horas efectivas del momento de calcular, no una copia.
- **Una carrera con oferta nueva después de crear el distributivo:** recibe su parcial con la primera propuesta.
- **El docente pasa a licencia después de proponerlo:** la propuesta se conserva; marcarlo es una inconsistencia de F07.
- **Una justificación que supera el largo o solo tiene espacios:** error del catálogo con el campo.
- **Si Cargas, Académico, Normativa o Docentes no responden:** error del catálogo en un plazo acotado; no se guarda nada.
- **Un periodo cerrado:** no admite crear ni proponer.

## Requirements *(mandatory)*

### Functional Requirements

**El distributivo**

- **FR-001**: Quien tiene «Distributivo: escritura» MUST poder crear el distributivo de un periodo listo en el tablero. Nacen la versión 1, editable, y un parcial en elaboración por cada carrera con oferta.
- **FR-002**: Un periodo MUST tener como mucho un distributivo. Crear otro MUST responder un error del catálogo.
- **FR-003**: Crear sobre un periodo que no está listo MUST responder un error del catálogo que diga que lo revises en el tablero.
- **FR-004**: El tablero MUST decir si el distributivo ya está creado, y dejar de ofrecer crearlo.

**La propuesta**

- **FR-005**: Quien tiene «Oferta y propuesta de docentes: escritura» MUST poder proponer un docente activo para una asignatura-paralelo ofertada de su carrera.
- **FR-006**: Cada asignatura-paralelo MUST tener como mucho una propuesta. Proponer otra vez MUST reemplazarla.
- **FR-007**: Quien propone MUST poder quitar una propuesta de su carrera.
- **FR-008**: Una propuesta MUST guardar el docente, la justificación, quién y cuándo. Cada cambio MUST quedar registrado (principio VII).
- **FR-009**: Las horas de una propuesta MUST salir de las horas efectivas del paralelo.
- **FR-010**: Proponer MUST seguir funcionando, con un aviso, si al docente no le alcanzan las horas.
- **FR-011**: Proponer MUST rechazarse con un error del catálogo si el periodo no tiene distributivo, si el docente no está activo o si el periodo está cerrado.
- **FR-012**: Una asignatura-paralelo de otra carrera MUST responder «no autorizado», sin datos.

**La justificación**

- **FR-013**: La justificación MUST decidirla el servidor: «afinidad» si el docente alcanza el mínimo; «disponibilidad» si no lo alcanza en tronco común; «excepción» si no lo alcanza fuera de tronco común.
- **FR-014**: Una excepción MUST exigir un texto de 10 a 500 caracteres. Sin él, no se guarda. Los otros dos casos MUST NOT conservar texto.
- **FR-015**: El texto MUST validarse por tipo y largo, en el servidor y en la pantalla (principio XIV).

**La sugerencia**

- **FR-016**: La sugerencia MUST traer a los docentes bajo el mínimo, con su puntaje y sus horas, aparte de los candidatos. Esto modifica FR-006 de F06-H02, que solo los contaba.
- **FR-017**: La pantalla MUST mostrarlos en una sección cerrada «Docentes bajo el mínimo».
- **FR-018**: Las horas asignadas MUST salir de las propuestas del distributivo del periodo, en todas las carreras, sin las de paralelos retirados y sin la propuesta del paralelo que se consulta.
- **FR-019**: Las horas en otras carreras MUST ser las propuestas de carreras distintas de la del paralelo.

**El parcial**

- **FR-020**: La oferta MUST mostrar en cada fila el docente propuesto y su justificación, y por carrera cuántas ofertadas tienen propuesta, de cuántas.
- **FR-021**: El parcial MUST decir que está completo cuando todas sus ofertadas tienen propuesta.
- **FR-022**: Una propuesta de un paralelo retirado MUST verse marcada «Paralelo retirado», MUST NOT contar y MUST poder quitarse. Si el paralelo vuelve a ofertarse, MUST volver a contar.

**Transversales**

- **FR-023**: Todo filtro por carrera MUST aplicarse en el servidor, con la carrera del coordinador (D-64).
- **FR-024**: Si un servicio del que depende no responde, el sistema MUST responder un error del catálogo dentro del plazo de ADR-009, sin guardar nada.
- **FR-025**: Todo error nuevo MUST salir del catálogo, con sus 8 columnas, en «tú», sin detalles técnicos (D-72, D-74).
- **FR-026**: Ninguna justificación ni ficha MUST ir a los registros de log (principio IX).

### Key Entities

- **Distributivo:** uno por periodo. Término ya definido en el glosario.
- **Versión del distributivo:** el estado del contenido; esta historia solo crea la versión 1, editable.
- **Parcial de carrera:** la parte de una carrera, en elaboración.
- **Propuesta:** el docente de una asignatura-paralelo, con su justificación. Una sola.
- **Justificación:** afinidad, disponibilidad o excepción; la excepción lleva texto.
- **Horas disponibles:** ya definidas (D-75); ahora con asignaciones reales.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En los casos de prueba, ninguna asignatura-paralelo tiene más de una propuesta, también al reemplazar.
- **SC-002**: El 100 % de las propuestas tiene su justificación, y toda excepción tiene su texto.
- **SC-003**: Un coordinador completa el parcial de una carrera de prueba, y el sistema dice que está completo.
- **SC-004**: Las horas en otras carreras coinciden con una suma hecha aparte en el 100 % de los casos de prueba: cierra el escenario pendiente de F06-H02.
- **SC-005**: Un coordinador propone un docente para una asignatura en menos de 2 minutos sin ayuda, con varios candidatos (RNF-USA-01).
- **SC-006**: Proponer responde en menos de 3 segundos, y crear el distributivo en menos de 5 (con holgura).
- **SC-007**: Con un servicio caído, el usuario ve un mensaje claro en menos de 10 segundos (con holgura), y no se guarda nada.

## Assumptions

- Propone solo quien tiene «Oferta y propuesta de docentes: escritura»: hoy, el Coordinador de carrera, siempre sobre lo propio.
- Un coordinador sin carrera recibe el aviso de F06-H05.
- Se puede proponer mientras la versión esté editable, el parcial en elaboración y el periodo no esté cerrado. Enviar el parcial es de otra historia.
- Una carrera que oferta después de crear el distributivo recibe su parcial con la primera propuesta.
- Los 10 caracteres mínimos de la excepción evitan textos vacíos de sentido; son un valor razonable, no una regla de normativa.
- Mientras no exista F07-H02, nada impide pasar de las horas contratadas: solo hay un aviso.
- La demo tiene un docente y una carrera con oferta: las horas en otras carreras y varios candidatos se prueban con el script. Para verlos en la demo hacen falta más docentes y otra carrera con oferta, que registra el autor.
- Los datos de prueba son ficticios (repositorio público).

## Out of Scope

- Bloquear por reglas duras y el caso de dos coordinadores a la vez (F07-H02).
- Las inconsistencias y sus justificaciones (F07).
- Actividades no lectivas (F06-H04).
- Enviar o devolver el parcial.
- Versiones nuevas, clonar un distributivo y firmas.
- Que el Vicerrector proponga o ajuste (RF-35).
