# Feature Specification: Ver los totales del docente al instante

**Feature Branch**: `033-totales-del-docente`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Especifica la historia F07-H01 · Ver los totales del docente al instante (issue #50), a partir de RF-27, RN-05 y D-01, con las cuatro aclaraciones del autor."

**Origen**: issue #50 · F07-H01 · sub-issue de F07 (Validar cada asignación al instante, con los totales del docente, #9) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · tamaño M

**Requerimientos que cubre**:
- **RF-27:** calcular los totales por docente y por componente, y el porcentaje 60/40. Se actualizan al guardar cada asignación.
- **RN-05:** la proporción de 60 % de clase y 40 % de otras actividades de docencia, solo para mostrarla. Exigir la planificación y la evaluación queda para F10-H01.
- **D-01:** los totales suman todas las carreras y unidades.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-01, D-72, D-74, D-75, D-77, D-78, ADR-010 y ADR-023. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), VIII (pruebas primero en el dominio), IX (mínimo privilegio), XIII (catálogo de errores) y XIV (validación de entradas).

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero ver los totales del docente por componente y su proporción 60/40 al guardar cada asignación, para saber si todavía le caben horas.

**Punto de partida.** El coordinador propone docentes en la oferta y asigna actividades en otra pantalla. Hoy solo ve una suma parcial: las actividades de su carrera, sin las clases y sin lo que el docente tiene en otras carreras. Para saber cuánto carga un docente tendría que sumar a mano.

Esta historia agrega:
- **Los totales del docente,** por los cinco componentes, de todas las carreras y unidades.
- **La proporción** de clase y de otras actividades de docencia, junto a la meta de la normativa.
- **Las horas que le quedan:** contratadas, asignadas y disponibles.
- **El panel en las dos pantallas,** al elegir al docente y al guardar.

## Clarifications

### Session 2026-11-01

- Q: ¿Dónde ve el coordinador los totales? → A: En las dos pantallas. Al guardar una propuesta en la oferta o una actividad en «Actividades no lectivas» aparece el panel del docente, y en Actividades la columna de horas pasa a mostrar sus totales de todas las carreras.
- Q: ¿De qué docentes puede verlos un coordinador? → A: De cualquier docente activo. Solo ve sumas por componente, sin el detalle de lo asignado en otras carreras.
- Q: ¿Cómo se muestra un docente que se aparta de 60/40? → A: Con los porcentajes y la meta, por ejemplo «Clase 67 % · Otras 33 % (meta 60/40)», sin marcarlo como error. La meta se lee de la normativa, y la justificación llega con F07-H03.
- Q: ¿Se revisa aquí que consten la planificación y la evaluación? → A: No. Es una regla global y queda para F10-H01.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver los totales al guardar (Priority: P1)

Como Coordinador de carrera, guardo una asignación y veo al momento los totales del docente, para saber si todavía le caben horas.

**Why this priority**: es la historia. Sin los totales, el coordinador asigna a ciegas.

**Independent Test**: con un docente que tiene horas en dos carreras, guardar una propuesta y una actividad, y comparar el panel con una suma aparte.

**Acceptance Scenarios**:

1. **Dado** un docente con asignaciones en dos carreras, **cuando** guardo una asignación nueva, **entonces** sus totales por clase, otras actividades de docencia, investigación, vinculación y gestión se actualizan.
2. **Dado** ese docente, **cuando** veo sus totales, **entonces** suman las horas de todas las carreras y unidades.
3. **Dado** un docente, **cuando** guardo una propuesta en la oferta, **entonces** veo su panel sin ir a otra pantalla.
4. **Dado** un docente, **cuando** guardo una actividad, **entonces** veo su panel sin ir a otra pantalla.
5. **Dada** una asignación, **cuando** la quito, **entonces** el panel y los totales dejan de contarla.
6. **Dada** una actividad que se registra otra vez con otras horas, **cuando** guardo, **entonces** los totales cuentan solo las horas nuevas.

---

### User Story 2 - Ver la proporción 60/40 (Priority: P1)

Como Coordinador de carrera, veo el porcentaje de clase y el de otras actividades de docencia junto a la meta, para saber cómo va la carga docente.

**Why this priority**: es el segundo escenario del issue, y RF-27 lo pide.

**Independent Test**: con 8 h de clase y 4 h de otras actividades de docencia, ver 67 % y 33 %, con la meta 60/40.

**Acceptance Scenarios**:

1. **Dado** un docente con horas de clase y de otras actividades de docencia, **cuando** veo sus totales, **entonces** veo el porcentaje de cada una y la meta.
2. **Dado** un docente que se aparta de la meta, **cuando** veo sus totales, **entonces** no se presenta como un error.
3. **Dado** un docente sin horas de clase ni de otras actividades de docencia, **cuando** veo sus totales, **entonces** se dice que todavía no hay proporción.
4. **Dado** un docente con horas de un solo componente, **cuando** veo sus totales, **entonces** veo 100 % y 0 %.
5. **Dadas** horas de investigación, vinculación o gestión, **cuando** veo la proporción, **entonces** no cambia.
6. **Dada** una versión de normativa con otra meta, **cuando** veo los totales de un periodo que la usa, **entonces** veo esa meta.

---

### User Story 3 - Ver si le caben horas antes de asignar (Priority: P2)

Como Coordinador de carrera, elijo a un docente activo y veo cuántas horas le quedan, antes de guardar nada.

**Why this priority**: es el motivo de la historia, «saber si todavía le caben horas», pero el coordinador ya puede guardar sin esto.

**Independent Test**: elegir en el formulario a un docente sin asignaciones en mi carrera y ver sus horas contratadas, asignadas y disponibles.

**Acceptance Scenarios**:

1. **Dado** el formulario de actividades, **cuando** elijo a un docente, **entonces** veo su panel antes de guardar.
2. **Dada** la propuesta de un paralelo, **cuando** elijo a un docente de la sugerencia, **entonces** veo su panel antes de guardar.
3. **Dado** un docente activo sin asignaciones en mi carrera, **cuando** lo elijo, **entonces** veo sus totales, y los de otras carreras solo como sumas.
4. **Dado** un docente con más horas de las contratadas, **cuando** veo su panel, **entonces** dice por cuántas horas se pasó, y sus disponibles son 0 (D-78).

---

### User Story 4 - Ver los totales en la lista de actividades (Priority: P2)

Como Coordinador de carrera, veo los totales de cada docente en la lista de actividades, para comparar sin abrir a cada uno.

**Why this priority**: es útil, pero el panel ya resuelve el caso principal.

**Independent Test**: un docente con actividades en dos carreras muestra en la lista los totales de las dos, no solo los de mi carrera.

**Acceptance Scenarios**:

1. **Dado** un docente de mi parcial, **cuando** veo la lista, **entonces** la columna de horas muestra sus totales de todas las carreras, con las clases.
2. **Dado** un docente con horas en otra carrera, **cuando** veo la lista, **entonces** veo sus actividades solo de mi carrera, y las de la otra solo en la suma.
3. **Dado** un usuario que solo lee la oferta, **cuando** ve la lista, **entonces** ve los mismos totales, sin acciones.

---

### User Story 5 - Lo que no se puede mostrar (Priority: P3)

Como usuario, veo un motivo claro si no se pueden calcular los totales, y guardar una asignación no falla por eso.

**Why this priority**: son casos de borde, que no deben confundir ni impedir el trabajo.

**Independent Test**: con el servicio de datos detenido, pedir los totales y ver el mensaje.

**Acceptance Scenarios**:

1. **Dado** un periodo sin distributivo, **cuando** pido los totales, **entonces** veo el motivo en palabras claras.
2. **Dado** un docente que no existe o no está activo, **cuando** pido sus totales, **entonces** veo el motivo en palabras claras.
3. **Dado** un servicio caído, **cuando** pido los totales, **entonces** veo un mensaje claro y puedo volver a intentarlo.
4. **Dada** una asignación recién guardada, **cuando** los totales no se pueden cargar, **entonces** la asignación sigue guardada y el aviso lo aclara.
5. **Dado** un usuario sin permiso de leer la oferta, **cuando** pide los totales, **entonces** no los ve.

---

### Edge Cases

- Una propuesta de un paralelo retirado no cuenta en ningún total.
- Si cambian las horas de un paralelo, los totales usan las horas actuales.
- Las horas con dos decimales se suman sin perder precisión (D-77).
- Con 1 h de clase y 2 h de otras, los porcentajes redondeados son 33 % y 67 %: siempre suman 100.
- Un docente con 0 horas contratadas no tiene disponibles, y lo asignado cuenta como exceso.
- Las horas de un parcial enviado siguen contando en los totales.
- Un docente que dejó de estar activo y tiene asignaciones sigue contando en la lista de su parcial, aunque ya no se ofrezca para elegir.
- En un periodo cerrado los totales se pueden consultar.

## Requirements *(mandatory)*

### Functional Requirements

**Los totales**

- **FR-001**: El sistema DEBE calcular, para un docente y un periodo, sus horas por los cinco componentes: clase, otras actividades de docencia, investigación, vinculación y gestión.
- **FR-002**: Los totales DEBEN sumar todas las carreras y unidades de la versión del distributivo (D-01).
- **FR-003**: Las horas de clase DEBEN ser las horas actuales de los paralelos propuestos, sin los retirados.
- **FR-004**: Las horas de los demás componentes DEBEN salir de las actividades no lectivas.
- **FR-005**: Los totales DEBEN coincidir con las horas que usan la sugerencia y los avisos: una sola forma de contar.
- **FR-006**: El sistema DEBE mostrar las horas contratadas, las asignadas y las disponibles, nunca menos de 0 (D-75).
- **FR-007**: Si las asignadas superan las contratadas, el sistema DEBE decir por cuántas horas. No es un error hasta F07-H02.

**La proporción**

- **FR-008**: El sistema DEBE calcular el porcentaje de clase y el de otras actividades de docencia, con la suma de las dos como base.
- **FR-009**: Investigación, vinculación y gestión NO DEBEN cambiar la proporción.
- **FR-010**: Los porcentajes DEBEN ser enteros y sumar 100.
- **FR-011**: Sin horas en ninguna de las dos, el sistema DEBE decir que todavía no hay proporción.
- **FR-012**: La meta DEBE leerse de la versión de normativa que rige el periodo (principio VI), no estar escrita en el sistema.
- **FR-013**: Apartarse de la meta NO DEBE mostrarse como error ni impedir nada.

**Dónde se ven**

- **FR-014**: Al guardar una propuesta, el sistema DEBE mostrar el panel del docente en la oferta.
- **FR-015**: Al guardar una actividad, el sistema DEBE mostrar el panel del docente en «Actividades no lectivas».
- **FR-016**: Al elegir un docente para proponerlo o para asignarle una actividad, el sistema DEBE mostrar su panel antes de guardar.
- **FR-017**: Al quitar una propuesta o una actividad, los totales que estén a la vista DEBEN actualizarse.
- **FR-018**: En la lista de «Actividades no lectivas», la columna de horas DEBE mostrar los totales del docente en todas las carreras, con la clase.

**Quién ve qué**

- **FR-019**: Los totales DEBEN verse con los mismos permisos que la oferta: leer la oferta, el distributivo o los catálogos. No hace falta un permiso nuevo.
- **FR-020**: Un coordinador DEBE poder ver los totales de cualquier docente activo.
- **FR-021**: Los totales NO DEBEN mostrar el detalle de lo asignado en otras carreras ni datos de contacto: solo sumas por componente (principio IX).
- **FR-022**: Quien solo lee, como el Vicerrector, DEBE ver los mismos totales, sin acciones.

**Validación y errores**

- **FR-023**: El periodo y el docente que llegan DEBEN validarse en la interfaz y en el servidor (principio XIV).
- **FR-024**: Sin distributivo, con un docente que no se puede consultar o sin normativa, el sistema DEBE responder con un motivo en palabras claras, y el catálogo de errores DEBE tener cada código nuevo con sus 8 columnas (principio XIII).
- **FR-025**: Con un servicio caído, al pedir los totales o la lista de actividades, el sistema DEBE responder con un mensaje claro, sin detalles técnicos, antes de 10 segundos.
- **FR-026**: Si la asignación se guardó pero los totales no cargan, DEBE quedar claro que sí se guardó.
- **FR-027**: Consultar los totales NO DEBE guardar nada ni dejar registro: es solo lectura.

### Key Entities

- **Carga horaria**: el total de un docente por componente en todas las carreras y unidades de la versión, con su proporción. Se calcula: no se guarda.
- **Asignación**: una propuesta o una actividad no lectiva. Es lo que se suma.
- **Componente**: clase, otras actividades de docencia, investigación, vinculación o gestión.
- **Meta de la proporción**: los dos porcentajes de RN-05 en la versión de normativa.
- **Horas disponibles**: contratadas menos asignadas, nunca menos de 0 (D-75).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los totales de docentes con horas en dos o más carreras coinciden con una suma hecha aparte en el 100 % de los casos de prueba.
- **SC-002**: Las horas asignadas del panel coinciden con las que usan la sugerencia y los avisos en el 100 % de los casos de prueba.
- **SC-003**: Los porcentajes suman 100 en todos los casos de prueba, incluidos los que no dan enteros.
- **SC-004**: El coordinador ve los totales actualizados en menos de 3 segundos después de guardar, con holgura.
- **SC-005**: Un coordinador dice si a un docente le caben 4 h más en menos de 30 segundos, sin sumar a mano.
- **SC-006**: Con un servicio caído, el usuario ve un mensaje claro en menos de 10 segundos, con holgura.
- **SC-007**: Ninguna respuesta de totales lleva asignaturas, actividades ni datos de contacto de otras carreras.

## Assumptions

- La base de la proporción es clase más otras actividades de docencia, como dice RN-05.
- Los porcentajes se redondean al entero más cercano; el de otras actividades es 100 menos el de clase, para que siempre sumen 100.
- Las horas contratadas y las disponibles se muestran porque son la respuesta a «¿le caben horas?» (D-75).
- Hasta F07-H02 se puede pasar de las horas con un aviso (D-78), así que el panel dice el exceso. Cuando bloquee, el exceso solo podrá venir de datos anteriores.
- Con esos permisos ven los totales el Administrador, el Coordinador de carrera, el Responsable de consolidación, el Vicerrector, el Rector, el OCS y la Secretaría General. Ya pueden leer el distributivo.
- La lista de actividades necesita la oferta para los totales. Sin el proceso `datos` muestra un mensaje claro en lugar de la lista.
- Un docente inactivo con asignaciones sigue contando en los totales.
- Sin distributivo no hay asignaciones: se dice el motivo, en lugar de mostrar ceros que parecen un cálculo.

## Out of Scope

- Bloquear las reglas duras, RN-02 y RN-13: F07-H02.
- Justificar una regla blanda: F07-H03.
- Exigir la planificación y la evaluación (RN-05): F10-H01.
- El panel de inconsistencias: F10-H01.
- La vista con el detalle del docente en cada unidad (RF-30, Won't).
- Las condiciones especiales y la tabla de carga horaria.
