# Feature Specification: Ver los docentes sugeridos para una asignatura

**Feature Branch**: `029-docentes-sugeridos`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Especifica la historia F06-H02 · Ver los docentes sugeridos para una asignatura (issue #47), a partir de RF-25, RN-19, D-01 y ADR-010, con las cuatro aclaraciones del autor del 2026-10-31."

**Origen**: issue #47 · F06-H02 · sub-issue de F06 (Proponer un docente por asignatura con sugerencias por afinidad, #8) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:05-generacion`, `RF-25`, `RN-19` · tamaño propuesto M · depende de #46 (F06-H01), cerrada.

**Requerimientos que cubre**:
- **RF-25:** el sistema sugiere y ordena los docentes para una asignatura según su puntaje de afinidad, su disponibilidad de horas y su dedicación, con el desglose del puntaje y las horas que ya tienen en otras carreras y unidades. La lista muestra primero a los de mayor puntaje y con horas disponibles.
- **RN-19:** solo se proponen docentes con puntaje igual o mayor al mínimo configurado, salvo en las asignaturas de tronco común. Esta historia cubre la lista; la justificación de asignar bajo el mínimo es de F07.
- **D-01:** las horas de un docente se miran en todas las carreras y unidades, no solo en la que asigna.
- **ADR-010 (MD-02):** Generación calcula el puntaje y arma la sugerencia, en el contexto Distributivo.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): ADR-006 (procesos), ADR-009 (fallos entre procesos), ADR-010, ADR-023 (consultas entre procesos), D-64 (alcance «solo lo propio»), D-70 (permisos alternativos), D-72 (validación y catálogo de errores), D-74 (mensajes en «tú»). **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), IX (mínimo privilegio y datos personales), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero ver los docentes sugeridos para una asignatura-paralelo, ordenados por afinidad y horas disponibles, para elegir rápido sin revisar fichas una por una.

**Punto de partida.** F06-H01 ya calcula el puntaje de un docente en cada asignatura de un periodo, pero solo desde la ficha de ese docente. Para elegir a quién proponer, el coordinador tendría que abrir las fichas una por una. Las asignaciones de horas todavía no existen: llegan con F06-H03 y F06-H04.

Esta historia agrega:
- **La sugerencia de una asignatura-paralelo:** los docentes activos que alcanzan el mínimo, ordenados, con su desglose y sus horas.
- **La acción «Sugerencia»** en cada asignatura-paralelo ofertada de la pantalla «Oferta».

**Lo que esta historia no puede demostrar todavía.** Como aún no hay asignaciones, hoy todo docente tiene 0 h asignadas. El escenario «Horas en otras carreras» se prueba aquí con datos simulados; con datos reales se demuestra al cerrar F06-H03.

## Clarifications

### Session 2026-10-31

- Q: Las asignaciones todavía no existen, ¿qué pasa con las horas en esta historia? → A: La sugerencia ya muestra las horas disponibles y las horas en otras carreras, con 0 h asignadas por ahora. El orden y la columna se prueban con datos simulados, y F06-H03 pone las horas reales.
- Q: ¿Qué pasa con un docente con buen puntaje pero sin horas? → A: Va al final de la lista, marcado. No se oculta, para que el coordinador vea que existe.
- Q: ¿De qué número se restan las horas asignadas? → A: De las horas contratadas, porque el tiempo parcial no tiene un valor fijo. El glosario «Horas disponibles» se precisa antes del código.
- Q: ¿Se filtra por la carrera del usuario? → A: No en esta historia. Hoy un usuario no está ligado a ninguna carrera, y la sugerencia solo lee lo que el coordinador ya puede ver. El filtro de «lo propio» (D-64) entra con F06-H03.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Ver los candidatos ordenados (Priority: P1)

Como Coordinador de carrera, quiero abrir la sugerencia de una asignatura-paralelo y ver primero a los docentes con mayor puntaje y con horas, con el desglose de cada uno, para elegir rápido (RF-25).

**Why this priority**: Es el primer escenario del issue y el valor de la historia.

**Independent Test**: Con un periodo abierto, su oferta y varios docentes con fichas distintas, abrir la sugerencia de un paralelo y comparar el orden y cada desglose con lo esperado.

**Acceptance Scenarios**:

```gherkin
Escenario: Orden de la sugerencia
  Dado una asignatura-paralelo de mi carrera
  Cuando abro la sugerencia
  Entonces aparecen primero los docentes con mayor puntaje y con horas disponibles
  Y cada uno muestra el desglose de su puntaje
```

1. **Given** varios docentes activos que alcanzan el mínimo y tienen horas, **When** abro la sugerencia, **Then** aparecen del mayor puntaje al menor.
2. **Given** dos docentes con el mismo puntaje, **When** abro la sugerencia, **Then** va primero el que tiene más horas disponibles y, si también empatan, por nombre.
3. **Given** un docente con buen puntaje al que no le alcanzan las horas para ese paralelo, **When** abro la sugerencia, **Then** aparece después de todos los que sí tienen horas, marcado «Sin horas suficientes».
4. **Given** un candidato de la lista, **When** lo despliego, **Then** veo qué título, experiencia o curso aportó cada parte, con su peso, como en la ficha.
5. **Given** un docente en licencia o inactivo, **When** abro la sugerencia, **Then** no aparece, aunque tenga el mejor puntaje.
6. **Given** un docente activo de otra carrera base, **When** abro la sugerencia, **Then** puede aparecer: la carrera base no lo excluye.
7. **Given** la sugerencia abierta, **When** la miro, **Then** cada candidato muestra su nombre, su dedicación, sus horas contratadas y sus horas disponibles, y ningún dato de contacto.
8. **Given** una asignatura-paralelo retirada de la oferta, **When** veo su fila en «Oferta», **Then** no ofrece la sugerencia, y pedirla responde un error del catálogo.

---

### User Story 2 - Dejar fuera a quien no alcanza el mínimo (Priority: P1)

Como Coordinador de carrera, quiero que solo aparezcan los docentes que alcanzan el mínimo de afinidad, salvo en tronco común, para no revisar perfiles que no corresponden (RN-19).

**Why this priority**: Es el tercer escenario del issue y una regla de la normativa.

**Independent Test**: Con un mínimo conocido, abrir la sugerencia de una asignatura sin tronco común y otra con él, y comparar quién aparece en cada una.

**Acceptance Scenarios**:

```gherkin
Escenario: Bajo el mínimo
  Dado un docente con puntaje bajo el mínimo de afinidad
  Cuando abro la sugerencia de una asignatura que no es de tronco común
  Entonces ese docente no aparece entre los sugeridos
```

1. **Given** un docente bajo el mínimo y una asignatura que no es de tronco común, **When** abro la sugerencia, **Then** no aparece, y una línea dice cuántos docentes quedaron fuera por el mínimo, sin nombrarlos.
2. **Given** un docente con un puntaje exactamente igual al mínimo, **When** abro la sugerencia, **Then** aparece.
3. **Given** una asignatura de tronco común, **When** abro la sugerencia, **Then** aparecen todos los docentes activos, también los de 0 puntos, en el mismo orden.
4. **Given** una asignatura sin tronco común en la que ningún docente alcanza el mínimo, **When** abro la sugerencia, **Then** el sistema lo dice y no muestra una lista vacía sin explicación.
5. **Given** dos periodos abiertos con versiones de mínimos distintos, **When** abro la sugerencia en cada uno, **Then** cada una usa el mínimo de la versión que rige su periodo.

---

### User Story 3 - Ver las horas que ya tiene en otras carreras (Priority: P1)

Como Coordinador de carrera, quiero ver cuántas horas tiene cada candidato en otras carreras y unidades, para no contar con horas que no están libres (D-01).

**Why this priority**: Es el segundo escenario del issue. Evita ofrecer a un docente que otra carrera ya ocupó.

**Independent Test**: Con horas asignadas simuladas para un docente, comprobar sus horas disponibles, su lugar en la lista y la cifra mostrada. Con datos reales, al cerrar F06-H03.

**Acceptance Scenarios**:

```gherkin
Escenario: Horas en otras carreras
  Dado un docente con horas asignadas en otra carrera
  Cuando aparece en la sugerencia
  Entonces veo cuántas horas ya tiene en otras carreras y unidades
```

1. **Given** un docente con horas asignadas en otras carreras o unidades, **When** aparece en la sugerencia, **Then** veo esas horas, y sus horas disponibles son sus horas contratadas menos sus horas asignadas.
2. **Given** un docente con más horas asignadas que contratadas, **When** aparece, **Then** sus horas disponibles son 0, nunca un número negativo.
3. **Given** el estado actual, sin asignaciones, **When** abro la sugerencia, **Then** cada docente muestra 0 h en otras carreras y unidades, y sus horas disponibles son sus horas contratadas.

---

### Edge Cases

- **Un periodo sin abrir:** se responde el error del catálogo que ya existe, con qué hacer. No se calcula con otra versión.
- **Un periodo cerrado:** se calcula con su versión, como en F06-H01.
- **Una asignatura-paralelo o un periodo que no existen, o con un código mal escrito:** error del catálogo, con la regla de Académico.
- **Ningún docente activo en la institución:** el sistema lo dice.
- **Un docente bajo el mínimo y sin horas:** no aparece; primero se aplica el mínimo.
- **Dos paralelos de la misma asignatura:** el puntaje es el mismo, pero el lugar en la lista puede cambiar si sus horas son distintas.
- **Si Docentes, Académico o Normativa no responden:** el sistema muestra el error del catálogo, con qué hacer, en un plazo acotado; no una lista parcial.
- **La ficha cambia:** la sugerencia se calcula en cada consulta, así que siempre refleja la ficha de ese momento.
- **Una ficha con datos incompletos:** un docente activo tiene siempre su tercer nivel; lo demás puede faltar y no suma.

## Requirements *(mandatory)*

### Functional Requirements

**Los candidatos**

- **FR-001**: El sistema MUST ofrecer, para una asignatura-paralelo ofertada de un periodo, su sugerencia: la lista de candidatos con su puntaje de afinidad y su desglose, calculados como en F06-H01.
- **FR-002**: Un candidato MUST ser un docente en estado laboral Activo, de cualquier carrera base. Un docente en licencia o inactivo MUST NOT aparecer.
- **FR-003**: En una asignatura que no es de tronco común, un docente con puntaje menor que el mínimo de afinidad MUST NOT aparecer. Con un puntaje igual al mínimo, sí aparece.
- **FR-004**: En una asignatura de tronco común, el mínimo MUST NOT aplicarse: aparecen todos los docentes activos.
- **FR-005**: El mínimo MUST salir de la versión de normativa que rige el periodo; ningún valor se escribe en el código (principio VI).
- **FR-006**: La sugerencia MUST decir cuántos docentes activos quedaron fuera por el mínimo, sin nombrarlos.

**Las horas**

- **FR-007**: Las horas disponibles de un docente MUST ser sus horas contratadas menos sus horas asignadas en todas las carreras y unidades, y nunca menos de 0.
- **FR-008**: Cada candidato MUST mostrar sus horas en otras carreras y unidades.
- **FR-009**: Mientras no existan asignaciones, las horas asignadas MUST ser 0. Cuando F06-H03 las traiga, MUST bastar con cambiar de dónde salen, sin cambiar la sugerencia ni su pantalla.

**El orden**

- **FR-010**: La lista MUST mostrar primero a los candidatos cuyas horas disponibles alcanzan para las horas del paralelo, y después a los demás, marcados «Sin horas suficientes».
- **FR-011**: Dentro de cada grupo, MUST ordenar del mayor puntaje al menor; a igual puntaje, más horas disponibles primero; y después por nombre.

**La consulta**

- **FR-012**: Sobre un periodo sin versión de normativa, la consulta MUST responder el error del catálogo que dice que lo abras.
- **FR-013**: Un periodo o una asignatura-paralelo que no existen, y una asignatura-paralelo retirada, MUST responderse con un error del catálogo.
- **FR-014**: El periodo, la asignatura y el paralelo MUST validarse por tipo, caracteres y largo en el servidor (principio XIV). La pantalla no tiene campos: los códigos salen de la fila de la oferta.

**La pantalla**

- **FR-015**: Cada asignatura-paralelo ofertada de «Oferta» MUST ofrecer la acción «Sugerencia» a quien puede usarla. Una retirada, no.
- **FR-016**: La sugerencia MUST mostrar, por candidato, su nombre, su dedicación, sus horas contratadas, sus horas disponibles, sus horas en otras carreras y unidades y su puntaje, con el desglose desplegable. MUST NOT mostrar datos de contacto ni su identificación.
- **FR-017**: La sugerencia MUST solo mostrar: no propone a nadie.
- **FR-018**: Sin candidatos, la pantalla MUST decir por qué, y no una lista vacía.

**Transversales**

- **FR-019**: Consultar la sugerencia MUST requerir el mismo permiso que consultar el puntaje de un docente (D-70).
- **FR-020**: Si un servicio del que depende no responde, el sistema MUST responder un error del catálogo dentro del plazo de espera de ADR-009, sin una lista parcial.
- **FR-021**: Todo error nuevo MUST salir del catálogo, con sus 8 columnas, en «tú», sin detalles técnicos (D-72, D-74).
- **FR-022**: La sugerencia MUST NOT guardarse: se calcula en cada consulta. La ficha de un docente MUST NOT ir a los registros de log (principio IX).
- **FR-023**: Entre procesos MUST viajar solo lo que la sugerencia necesita: ningún correo, teléfono, contrato ni dato personal de más (principio IX).

### Key Entities

- **Sugerencia** (`Sugerencia`): la lista ordenada de candidatos de una asignatura-paralelo en un periodo, con cuántos quedaron fuera por el mínimo. No se guarda.
- **Candidato:** un docente de la lista, con su puntaje, su desglose y sus horas.
- **Horas disponibles:** horas contratadas menos horas asignadas, nunca menos de 0 (D-75).
- **Mínimo de afinidad** (`MinimoAfinidad`), **Puntaje de afinidad** (`PuntajeAfinidad`) y **Tronco común:** ya existen; Generación solo los lee o los reusa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En los casos de prueba, el 100 % de las listas coincide con el orden y los candidatos hechos aparte, sin usar el código de Generación.
- **SC-002**: Ningún mínimo de afinidad está escrito en el código: cambiarlo en una versión de normativa cambia quién aparece en sus periodos.
- **SC-003**: La sugerencia de un paralelo, con los 47 docentes de prueba (46 activos), se ve en menos de 3 segundos (con holgura).
- **SC-004**: Si un servicio del que depende no responde, el usuario ve un mensaje claro en menos de 10 segundos (con holgura), sin una lista parcial.
- **SC-005**: Ningún dato de la ficha ni de contacto aparece en los registros de log.
- **SC-006**: En una prueba con el coordinador, encontrar a un candidato para un paralelo y ver su desglose toma menos de 2 minutos (RNF-USA-01), sin abrir ninguna ficha.

## Assumptions

- Quien puede leer el puntaje de un docente puede ver la sugerencia. Mientras el usuario no esté ligado a una carrera, un coordinador puede abrir la sugerencia de cualquier asignatura-paralelo: el filtro de «lo propio» (D-64) queda pendiente para F06-H03, que además escribe.
- «Sin horas suficientes» cubre al docente con 0 h y al que tiene menos horas que las del paralelo, porque tampoco se le podría asignar.
- La dedicación se muestra, pero no ordena por sí sola: RF-25 la nombra, y ya pesa en las horas contratadas, de las que salen las horas disponibles.
- Las horas contratadas son las del registro del docente, sin revisar que cuadren con su dedicación: esa revisión es de la validación del distributivo (F07).
- Un periodo cerrado sigue teniendo su versión, así que se calcula.
- Los datos de prueba son ficticios (repositorio público).

## Out of Scope

- Proponer un docente (F06-H03).
- Las asignaciones reales y las actividades no lectivas (F06-H03, F06-H04).
- Bloquear, marcar inconsistencias o justificar (F07).
- Filtrar por la carrera del usuario (F06-H03).
- Las palabras clave y las asignaturas afines declaradas.
- Guardar la sugerencia.
