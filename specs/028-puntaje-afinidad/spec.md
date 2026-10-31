# Feature Specification: Calcular el puntaje de afinidad con su desglose

**Feature Branch**: `028-puntaje-afinidad`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Especifica la historia F06-H01 · Calcular el puntaje de afinidad con su desglose (issue #46), a partir de RN-19, RN-24, RF-17 y ADR-010, con las cuatro aclaraciones del autor del 2026-10-31."

**Origen**: issue #46 · F06-H01 · sub-issue de F06 (Proponer un docente por asignatura con sugerencias por afinidad, #8) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:05-generacion`, `RF-17`, `RN-19`, `RN-24` · tamaño propuesto L · depende de #38 (F03-H04) y #42 (F04-H03), cerradas. Es la primera historia de la Ola 2.

**Requerimientos que cubre**:
- **RN-24:** los pesos de afinidad son parametrizables: título de cuarto nivel en el campo de la asignatura, título de tercer nivel en el campo, experiencia relacionada y curso relacionado (con tope). Los títulos cuentan más si coincide el campo específico que si solo coincide el campo amplio.
- **RN-19:** la afinidad docente-asignatura se calcula con la formación de la ficha del docente frente al campo de conocimiento de la asignatura. Esta historia cubre el cálculo; el mínimo de afinidad y su justificación son de F06-H02 y F07.
- **RF-17:** para un docente se muestra su formación y, por cada asignatura, su puntaje de afinidad y de qué título, curso o experiencia proviene.
- **ADR-010 (MD-02):** el puntaje lo calcula el servicio 5, Generación, en el contexto Distributivo, con la ficha de Docentes y los pesos de la versión de normativa del periodo. La pantalla de RF-17 lo consulta en Generación.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): ADR-006 (procesos), ADR-009 (fallos entre procesos), ADR-010, D-49 y ADR-018 (entre procesos), D-68 (pesos por defecto), D-70 (permisos alternativos), D-72 (validación y catálogo de errores), D-74 (mensajes en «tú»). **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), IX (mínimo privilegio y datos personales), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero ver el puntaje de afinidad de un docente para una asignatura y de dónde sale, para proponer al docente mejor formado.

**Punto de partida.** La ficha académica de cada docente, los pesos de afinidad de cada versión de normativa y los campos de cada asignatura ya están en el sistema, pero nadie los cruza. Generación, el servicio donde se construirá el distributivo, todavía no tiene lógica de negocio.

Esta historia agrega:
- **El cálculo del puntaje de afinidad,** con su desglose, en Generación.
- **La consulta del puntaje de un docente** en las asignaturas de la oferta de un periodo.
- **La vista en la ficha del docente,** en la pantalla «Docentes».

## Clarifications

### Session 2026-10-31

- Q: ¿Cuándo un curso o una experiencia está relacionada con la asignatura? → A: Si su campo específico es el de la asignatura. Las palabras clave de la asignatura no suman en esta historia, aunque RN-19 las nombra.
- Q: Si un docente tiene dos títulos que coinciden con la asignatura, ¿suman los dos? → A: Sí. Cada título, experiencia o curso relacionado suma, como dice el glosario («Pesos de afinidad»). Solo los cursos tienen tope.
- Q: ¿Qué asignaturas muestra la ficha? → A: Las de la oferta del periodo elegido, del mayor puntaje al menor.
- Q: ¿Qué pasa con un periodo que todavía no está abierto? → A: No se calcula. Un error del catálogo dice que lo abras con su versión de normativa: el puntaje siempre sale de la versión que rige ese periodo.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Calcular el puntaje de un docente para una asignatura (Priority: P1)

Como Coordinador de carrera, quiero que el puntaje de afinidad salga de la ficha del docente y de los pesos de la normativa del periodo, y que diga de dónde sale cada punto, para confiar en él (RN-19, RN-24).

**Why this priority**: Son los tres primeros escenarios del issue: la regla central de la historia.

**Independent Test**: Con pesos conocidos y una ficha de prueba, calcular el puntaje para una asignatura y comparar el total y cada línea del desglose con la suma hecha a mano.

**Acceptance Scenarios**:

```gherkin
Escenario: Título en el campo específico
  Dado un docente con un título de cuarto nivel en el campo específico de la asignatura
  Cuando se calcula su afinidad
  Entonces el título suma el peso del cuarto nivel
  Y el desglose indica qué título lo aportó

Escenario: Solo coincide el campo amplio
  Dado un docente cuyo título solo coincide en el campo amplio
  Cuando se calcula su afinidad
  Entonces ese título suma menos que si coincidiera el campo específico

Escenario: Tope de cursos
  Dado un docente con más cursos relacionados que el tope
  Cuando se calcula su afinidad
  Entonces los cursos no suman más que el tope de la versión de normativa
```

1. **Given** un docente con un título de cuarto nivel en el campo específico de la asignatura, **When** se calcula su afinidad, **Then** ese título suma el peso «cuarto nivel en el campo específico», y el desglose lo nombra con ese peso.
2. **Given** un título, de tercer o de cuarto nivel, que solo coincide en el campo amplio, **When** se calcula, **Then** suma el peso del campo amplio de su nivel, menor que el del específico.
3. **Given** un docente con dos títulos que coinciden, **When** se calcula, **Then** suman los dos, y el desglose muestra cada uno.
4. **Given** un docente con una experiencia y un curso en el campo específico de la asignatura, **When** se calcula, **Then** cada uno suma su peso.
5. **Given** un curso o una experiencia que solo coincide en el campo amplio, **When** se calcula, **Then** no suma.
6. **Given** un docente con más cursos relacionados de los que permite el tope, **When** se calcula, **Then** la suma de los cursos no pasa del tope de la versión, y el desglose dice que se aplicó.
7. **Given** una formación que no coincide con la asignatura, **When** se calcula, **Then** suma 0 y no está en el desglose.
8. **Given** dos periodos abiertos con versiones de normativa de pesos distintos, **When** se calcula en cada uno, **Then** cada puntaje usa los pesos de la versión que rige su periodo.

---

### User Story 2 - Ver el puntaje en la ficha del docente (Priority: P1)

Como Coordinador de carrera, quiero abrir la ficha de un docente, elegir un periodo y ver su puntaje en cada asignatura de la oferta, con el desglose, para decidir a quién proponer (RF-17).

**Why this priority**: Es el cuarto escenario del issue y lo que el usuario ve.

**Independent Test**: Con un periodo abierto y su oferta, abrir la ficha de un docente y ver las asignaturas ordenadas por puntaje; desplegar una y ver su desglose.

**Acceptance Scenarios**:

```gherkin
Escenario: Ficha del docente
  Dado la ficha de un docente en Docentes
  Cuando la abro
  Entonces veo su puntaje por asignatura con el desglose, calculado por Generación
```

1. **Given** la ficha de un docente y un periodo abierto con oferta, **When** la abro y elijo el periodo, **Then** veo cada asignatura de la oferta una sola vez, con su puntaje, del mayor al menor.
2. **Given** esa lista, **When** despliego una asignatura, **Then** veo qué título, experiencia o curso aportó cada parte, con su peso.
3. **Given** un periodo sin abrir, **When** lo elijo, **Then** el sistema dice que lo abras con su versión de normativa, y no muestra puntajes.
4. **Given** un periodo abierto sin oferta, **When** lo elijo, **Then** el sistema dice que el periodo todavía no tiene oferta.
5. **Given** un usuario que no puede leer los docentes, **When** pide el puntaje, **Then** recibe «no autorizado».

---

### Edge Cases

- **Un docente sin formación que coincida con ninguna asignatura:** se muestran las asignaturas con puntaje 0.
- **Un docente que no existe:** el sistema responde el error del catálogo de Generación.
- **Un periodo que no existe o con un código mal escrito:** el sistema lo rechaza con la regla de Académico (F02-H02).
- **Un periodo cerrado:** se calcula con su versión, porque sigue rigiéndolo.
- **Asignaturas de la oferta en varios paralelos:** cuentan una vez: el puntaje es por asignatura.
- **Si Docentes, Académico o Normativa no responden:** el sistema muestra el error del catálogo, con qué hacer, en un plazo acotado; no un puntaje parcial.
- **La ficha cambia:** el puntaje se calcula en cada consulta, así que siempre refleja la ficha de ese momento.
- **Asignaturas afines declaradas en la ficha:** no suman todavía: suman cuando el coordinador las valida (glosario), y esa validación no existe.

## Requirements *(mandatory)*

### Functional Requirements

**El cálculo**

- **FR-001**: El sistema MUST calcular el puntaje de afinidad de un docente para una asignatura como la suma de lo que aporta cada título, experiencia y curso relacionado de su ficha, con los pesos de la versión de normativa del periodo.
- **FR-002**: Un título MUST sumar el peso del campo específico de su nivel si su campo específico es el de la asignatura, o el del campo amplio de su nivel si solo coincide el campo amplio. Si no coincide, no suma.
- **FR-003**: Una experiencia o un curso MUST sumar su peso solo si su campo específico es el de la asignatura.
- **FR-004**: Cada título, experiencia o curso que coincide MUST sumar. La suma de los cursos MUST NOT pasar del tope de la versión de normativa.
- **FR-005**: El puntaje MUST traer su desglose: por cada elemento que suma, su tipo, su nombre y su aporte, y si el tope de cursos se aplicó.
- **FR-006**: Los pesos MUST salir de la versión de normativa que rige el periodo; ningún peso se escribe en el código (principio VI).
- **FR-007**: Las palabras clave de la asignatura, las asignaturas afines declaradas y el tronco común MUST NOT cambiar el puntaje en esta historia.

**La consulta**

- **FR-008**: El sistema MUST ofrecer, para un docente y un periodo, su puntaje con el desglose en cada asignatura de la oferta del periodo, una sola vez por asignatura, del mayor puntaje al menor; a igual puntaje, por nombre.
- **FR-009**: Sobre un periodo sin versión de normativa, la consulta MUST responder un error del catálogo que diga que lo abras. No calcula con otra versión.
- **FR-010**: Un docente o un periodo que no existen MUST responderse con un error del catálogo.
- **FR-011**: El identificador del docente y el código del periodo MUST validarse por tipo, caracteres y largo, en el servidor y en la pantalla (principio XIV).

**La pantalla**

- **FR-012**: La ficha del docente en «Docentes» MUST ofrecer elegir un periodo y mostrar la lista de FR-008, con el desglose de cada asignatura desplegable.
- **FR-013**: Sin periodo abierto, o sin oferta, la pantalla MUST decir qué falta, sin un puntaje falso.

**Transversales**

- **FR-014**: Consultar el puntaje MUST requerir el mismo permiso que consultar el docente (D-70).
- **FR-015**: Si un servicio del que depende el cálculo no responde, el sistema MUST responder un error del catálogo dentro del plazo de espera de ADR-009, sin un puntaje parcial.
- **FR-016**: Todo mensaje nuevo MUST salir del catálogo, con sus 8 columnas, en «tú», sin detalles técnicos (D-72, D-74).
- **FR-017**: El puntaje MUST NOT guardarse: se calcula en cada consulta. La ficha del docente MUST NOT ir a los registros de log (principio IX).

### Key Entities

- **Puntaje de afinidad** (`PuntajeAfinidad`): el total de un docente para una asignatura, con su desglose.
- **Calculadora de afinidad** (`CalculadoraAfinidad`, MD-02): el servicio de dominio de Generación que lo calcula.
- **Línea del desglose:** un título, experiencia o curso que suma, con su aporte.
- **Pesos de afinidad** y **Ficha académica:** ya existen en Normativa y Docentes; Generación solo los lee.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En los casos de prueba, el 100 % de los puntajes y sus desgloses coinciden con la suma hecha a mano.
- **SC-002**: Ningún peso de afinidad está escrito en el código: cambiar un peso en una versión de normativa cambia el puntaje de sus periodos.
- **SC-003**: La lista de un docente en la oferta de un periodo (165 paralelos de prueba) se ve en menos de 3 segundos (con holgura).
- **SC-004**: Si un servicio del que depende no responde, el usuario ve un mensaje claro en menos de 10 segundos (con holgura), sin puntajes parciales.
- **SC-005**: Ningún dato de la ficha aparece en los registros de log.

## Assumptions

- La ficha guarda solo el campo específico de los títulos de cuarto nivel, los cursos y la experiencia; su campo amplio son los primeros dígitos del específico, como en el resto del sistema.
- Quien puede leer un docente puede ver su puntaje. El filtro «solo lo propio» del coordinador sigue fuera, como en F03-H05.
- Un periodo cerrado sigue teniendo su versión, así que se calcula.
- El selector de periodos de la ficha ofrece los periodos que el usuario puede ver.
- Los datos de prueba son ficticios (repositorio público).

## Out of Scope

- Ordenar los docentes sugeridos para una asignatura (F06-H02).
- El mínimo de afinidad como inconsistencia y su justificación (F07).
- Proponer un docente (F06-H03).
- Validar las asignaturas afines declaradas.
- Las palabras clave y la exención de tronco común.
- Guardar el puntaje.
