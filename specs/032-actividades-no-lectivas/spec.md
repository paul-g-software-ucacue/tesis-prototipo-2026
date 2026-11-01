# Feature Specification: Asignar actividades no lectivas

**Feature Branch**: `032-actividades-no-lectivas`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Especifica la historia F06-H04 · Asignar actividades no lectivas (issue #49), a partir de RF-26, RF-21, RN-22, RN-25, C-12 y C-13, con las cuatro aclaraciones del autor."

**Origen**: issue #49 · F06-H04 · sub-issue de F06 (Proponer un docente por asignatura con sugerencias por afinidad, #8) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · tamaño M · depende de #48, cerrada.

**Requerimientos que cubre**:
- **RF-26:** asignar actividades no lectivas con horas semanales, cada una con su componente. Las comisiones quedan fuera (C-12).
- **RF-21:** el catálogo de actividades, fijo en el prototipo y sembrado con la versión de normativa.
- **RN-22:** solo se registran las actividades de los arts. 140 a 143 del RCE.
- **RN-25:** investigación, vinculación y tutoría de prácticas solo contra un proyecto aprobado y vigente.
- **C-13:** las horas de evaluación van en el distributivo.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-01, D-16, D-64, D-65, D-72, D-74, D-75, D-77, D-78, ADR-010 y ADR-023. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), VII (inmutabilidad y trazabilidad), IX (mínimo privilegio), XIII (catálogo de errores) y XIV (validación de las entradas). Mensajes en «tú» (D-74).

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero asignar horas de otras actividades de docencia, investigación, vinculación y gestión, para completar la carga horaria de cada docente.

**Punto de partida.** El coordinador ya propone docentes para las clases, y las horas asignadas salen de esas propuestas. Pero un docente también planifica, evalúa, investiga o coordina, y nada de eso se puede registrar. Por eso sus horas disponibles salen más altas de lo que son. El catálogo de actividades tampoco existe todavía.

Esta historia agrega:
- **El catálogo de actividades,** con 47 actividades de los arts. 140 a 143 del RCE.
- **La asignación de horas** de una actividad a un docente, en el parcial de la carrera.
- **Lo que exige la norma:** un proyecto para investigación, vinculación y tutoría de prácticas, y un cargo vigente para las actividades que son cargos.
- **Las horas reales completas:** las actividades cuentan en la disponibilidad y en las horas en otras carreras.

## Clarifications

### Session 2026-11-01

- Q: ¿Qué literales de los arts. 140 a 143 entran al catálogo? → A: Los literales concretos, cada uno con su componente. Quedan fuera 140 a) impartir clases, que son las propuestas; 143 i) comisiones (C-12); y los literales «Las demás que defina…» (140 p, 141 j, 142 j y 143 q), porque RN-22 solo admite las contempladas.
- Q: ¿Qué hace esta historia con RN-25? → A: Investigación, vinculación y tutoría de prácticas exigen elegir uno de los proyectos asignables del periodo en los que participa el docente. Sin proyecto no se guarda. El tope de 16 h queda para F07-H02.
- Q: ¿Cómo se manejan las horas de gestión? → A: Se registran a mano. Las actividades que son cargos (143 a a h) solo se pueden elegir si el docente tiene ese cargo vigente. El cálculo y los límites de horas quedan para F07.
- Q: ¿A quién le asigna un coordinador, y en qué parcial queda? → A: A cualquier docente activo, en el parcial de su carrera. Solo quien tiene esa carrera edita la actividad. Las horas restan disponibilidad en todas las carreras.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Asignar una actividad del catálogo (Priority: P1)

Como Coordinador de carrera, elijo un docente activo y una actividad del catálogo, y le asigno sus horas semanales, para completar su carga horaria.

**Why this priority**: es la historia. Sin ella, la carga de un docente solo tiene clases.

**Independent Test**: con un distributivo creado, asignar horas de planificación a un docente y verlas bajo «Otras actividades de docencia».

**Acceptance Scenarios**:

1. **Dado** el catálogo de la versión de normativa del periodo, **cuando** asigno horas de planificación a un docente, **entonces** la actividad queda en el componente «Otras actividades de docencia» y se refleja en las horas del docente.
2. **Dado** el formulario, **cuando** lo abro, **entonces** no ofrece comisiones, ni impartir clases, ni «Las demás que defina…».
3. **Dado** un docente con esa actividad en mi parcial, **cuando** la registro otra vez con otras horas, **entonces** queda una sola, con las horas nuevas, y el cambio queda registrado.
4. **Dada** una actividad de mi parcial, **cuando** la quito, **entonces** deja de contar y el cambio queda registrado.
5. **Dadas** unas horas vacías, en 0, negativas, con más de dos decimales o que no son un número, **cuando** guardo, **entonces** veo qué corregir y no se guarda nada.
6. **Dado** un docente sin horas suficientes, **cuando** guardo, **entonces** se guarda y veo el aviso (D-78).
7. **Dado** un periodo sin distributivo, o cerrado, o un parcial enviado, **cuando** intento guardar, **entonces** veo el motivo en palabras claras y no se guarda nada.
8. **Dado** un periodo cerrado, **cuando** intento quitar una actividad, **entonces** veo el motivo y no se quita.

---

### User Story 2 - Vincular el proyecto (Priority: P1)

Como Coordinador de carrera, al asignar investigación, vinculación o tutoría de prácticas elijo el proyecto, para que esas horas respondan a un proyecto aprobado (RN-25).

**Why this priority**: es una regla dura de la norma, y guardar horas sin proyecto obligaría a corregirlas después.

**Independent Test**: asignar una actividad de investigación con y sin un proyecto del docente.

**Acceptance Scenarios**:

1. **Dado** un docente que participa en un proyecto de investigación asignable en el periodo, **cuando** asigno una actividad de investigación, **entonces** elijo ese proyecto y queda vinculado.
2. **Dado** un docente sin proyectos asignables, **cuando** elijo una actividad que pide proyecto, **entonces** el sistema dice que no tiene ninguno y no deja guardar.
3. **Dado** un proyecto del docente que no es del tipo de la actividad, o que no es asignable en el periodo, **cuando** guardo, **entonces** el servidor lo rechaza con un mensaje claro.
4. **Dado** un proyecto en el que el docente no participa, **cuando** guardo, **entonces** el servidor lo rechaza.
5. **Dada** una actividad que no pide proyecto, **cuando** abro el formulario, **entonces** no me pide uno.

---

### User Story 3 - Registrar horas de gestión (Priority: P1)

Como Coordinador de carrera, registro las horas de gestión de un docente, para que su carga refleje sus cargos y encargos.

**Why this priority**: RF-26 las incluye, y sin el control del cargo se podrían asignar horas de un cargo que el docente no tiene.

**Independent Test**: asignar «Coordinar carreras o programas» a un docente con y sin ese cargo vigente.

**Acceptance Scenarios**:

1. **Dado** un docente con el cargo vigente, **cuando** elijo la actividad de ese cargo, **entonces** se guarda en el componente «Gestión».
2. **Dado** un docente sin ese cargo vigente, **cuando** elijo la actividad, **entonces** el sistema dice que no tiene el cargo y no se guarda.
3. **Dada** una actividad de gestión que no es un cargo, como organizar eventos académicos, **cuando** la asigno, **entonces** se guarda sin pedir un cargo.

---

### User Story 4 - Ver las actividades de mi carrera (Priority: P1)

Como Coordinador de carrera, veo por docente las actividades de mi parcial y la suma por componente, para saber qué me falta completar.

**Why this priority**: sin esta vista, lo guardado no se puede revisar ni corregir.

**Independent Test**: asignar actividades a dos docentes y ver cada uno con sus sumas.

**Acceptance Scenarios**:

1. **Dados** dos docentes con actividades en mi parcial, **cuando** abro la pantalla, **entonces** veo a cada uno con sus actividades, sus horas, su proyecto y la suma por componente.
2. **Dado** un docente con actividades en otra carrera, **cuando** abro la pantalla, **entonces** veo solo las de mi carrera.
3. **Dado** quien lee el distributivo con alcance total, **cuando** abre la pantalla, **entonces** ve las de todas las carreras, sin poder cambiarlas.
4. **Dado** un periodo sin distributivo, **cuando** abro la pantalla, **entonces** lo dice y no ofrece asignar.

---

### User Story 5 - Ver las horas completas (Priority: P1)

Como Coordinador de carrera, veo en la sugerencia las horas que un docente ya tiene, también por sus actividades, para no proponerle más de lo que puede.

**Why this priority**: es lo que hace útil registrar las actividades, y otros coordinadores dependen de ese dato.

**Independent Test**: asignar actividades a un docente en una carrera y mirar sus horas desde otra.

**Acceptance Scenarios**:

1. **Dado** un docente con 10 h de actividades en otra carrera, **cuando** miro la sugerencia desde la mía, **entonces** esas 10 h salen en sus horas en otras carreras y restan de sus horas disponibles.
2. **Dado** un docente con actividades en mi carrera, **cuando** miro la sugerencia, **entonces** restan de sus horas disponibles, pero no salen como horas en otras carreras.
3. **Dada** una actividad quitada, **cuando** miro la sugerencia, **entonces** ya no cuenta.

---

### Edge Cases

- **Otra carrera:** quitar o cambiar una actividad de otra carrera responde «no autorizado», sin datos.
- **Sin carrera indicada:** un coordinador sin carrera recibe el aviso que ya existe, sin datos.
- **Dos registros iguales:** la misma actividad y el mismo docente, con otro proyecto, son dos actividades distintas. Con el mismo proyecto, la segunda reemplaza a la primera.
- **Una carrera sin parcial:** recibe su parcial con la primera actividad, como con la primera propuesta.
- **Un docente que deja de estar activo:** sus actividades se conservan y se siguen viendo; no se le pueden asignar nuevas.
- **Un proyecto o un cargo que deja de valer:** lo guardado se conserva. Volver a validarlo es de F07-H02.
- **Otro servicio caído:** se ve un mensaje claro y no se guarda nada.
- **Un parcial sin propuestas:** puede tener actividades, y su avance de propuestas no cambia.
- **Horas decimales:** se aceptan hasta dos decimales, como en las propuestas (D-77).

## Requirements *(mandatory)*

### Functional Requirements

**El catálogo**

- **FR-001**: El sistema MUST tener un catálogo fijo de 47 actividades, una por cada literal concreto de los arts. 140 a 143 del RCE, con el texto de la política de marzo de 2024 y su componente: 14 de otras actividades de docencia, 9 de investigación, 9 de vinculación y 15 de gestión.
- **FR-002**: El catálogo MUST NOT incluir impartir clases (140 a), comisiones (143 i, C-12) ni «Las demás que defina…» (140 p, 141 j, 142 j y 143 q).
- **FR-003**: El catálogo MUST llegar sembrado con la versión de normativa, y cada periodo MUST usar el de la versión que lo rige (principio VI).
- **FR-004**: Una actividad fuera del catálogo MUST rechazarse en el servidor con un error del catálogo de errores (RN-22).

**Asignar**

- **FR-005**: Quien tiene «Oferta y propuesta de docentes: escritura» MUST poder asignar a un docente activo una actividad con sus horas semanales, en el parcial de su carrera.
- **FR-006**: Las horas MUST ser mayores que 0 y como mucho 40, con hasta dos decimales, y validarse en la pantalla y en el servidor (principio XIV).
- **FR-007**: Cada parcial MUST tener como mucho una actividad por docente, actividad y proyecto. Registrarla otra vez MUST reemplazar sus horas.
- **FR-008**: Quien asigna MUST poder quitar una actividad de su carrera, salvo con el periodo cerrado o el parcial que no admite cambios.
- **FR-009**: Una actividad MUST guardar el docente, la actividad, las horas, el proyecto si lo tiene, quién y cuándo. Cada cambio MUST quedar registrado (principio VII).
- **FR-010**: Asignar MUST rechazarse con un error del catálogo si el periodo no tiene distributivo, está cerrado o no tiene versión de normativa, si el parcial no admite cambios o si el docente no está activo.
- **FR-011**: Sin horas suficientes, la actividad MUST guardarse, con un aviso (D-78).
- **FR-012**: Una actividad de otra carrera MUST responder «no autorizado», sin datos.

**El proyecto**

- **FR-013**: Las actividades de investigación, las de vinculación y la tutoría de prácticas (140 f) MUST exigir un proyecto. Las demás MUST NOT llevarlo.
- **FR-014**: El proyecto MUST ser asignable en el periodo, MUST tener al docente entre sus participantes y MUST ser de un tipo que corresponda: investigación para el art. 141; vinculación, prácticas preprofesionales o servicio comunitario para el art. 142; prácticas preprofesionales o servicio comunitario para 140 f.
- **FR-015**: La pantalla MUST ofrecer solo los proyectos que cumplen FR-014 y, si no hay ninguno, decirlo. El servidor MUST volver a comprobarlo (RN-25).

**El cargo**

- **FR-016**: Las actividades que son cargos MUST exigir que el docente tenga vigente el cargo que les corresponde: 143 a, rector o vicerrector; 143 b, coordinador académico institucional; 143 c, coordinador de sede; 143 d, coordinador de carrera; 143 e, gestor de investigación; 143 f, gestor de vinculación; 143 g, gestor de idiomas; 143 h, gestor de formación integral.
- **FR-017**: Las demás actividades de gestión MUST NOT pedir un cargo.

**Ver**

- **FR-018**: Una pantalla «Actividades no lectivas» MUST mostrar, por docente, las actividades, sus horas, su proyecto y la suma por componente. Es lo que refleja las horas del docente hasta que F07-H01 traiga sus totales.
- **FR-019**: El coordinador MUST ver solo lo de su carrera (D-64). Quien tiene alcance total MUST ver todas las carreras.
- **FR-020**: Solo quien escribe MUST ver las acciones de asignar, cambiar y quitar.

**Las horas del docente**

- **FR-021**: Las horas asignadas MUST sumar las propuestas y las actividades de todas las carreras (D-75). Esto modifica FR-018 de F06-H03, que solo contaba propuestas.
- **FR-022**: Las horas en otras carreras MUST incluir las actividades de los parciales de otras carreras.

**La oferta**

- **FR-023**: Quien puede proponer pero no cambiar la oferta MUST NOT ver el aviso «Solo lectura: tu rol no cambia la oferta». MUST ver un texto que diga que no cambia la oferta, pero sí propone docentes.

**Corrección de F06-H03**

- **FR-024**: Quitar una propuesta con el periodo cerrado MUST rechazarse con el error del catálogo. Corrige F06-H03, donde solo se bloqueaba proponer.

**Transversales**

- **FR-025**: Todo filtro por carrera MUST aplicarse en el servidor (D-64).
- **FR-026**: Si un servicio del que depende no responde, el sistema MUST responder un error del catálogo dentro del plazo de ADR-009, sin guardar nada.
- **FR-027**: Todo error nuevo MUST salir del catálogo, con sus 8 columnas, en «tú», sin detalles técnicos (D-72, D-74).

### Key Entities

- **Catálogo de actividades**: las 47 actividades, con su código, su literal, su texto, su componente y lo que exigen: nada, un proyecto o un cargo. Es parte de la versión de normativa.
- **Actividad no lectiva**: horas de un docente en una actividad del catálogo, en el parcial de una carrera, con su proyecto si lo pide, quién y cuándo. Junto con la propuesta, es una asignación.
- **Componente**: clase, otras actividades de docencia, investigación, vinculación o gestión.
- **Proyecto** y **Cargo**: ya existen en Docentes; aquí solo se consultan.
- **Parcial de carrera**: el mismo de F06-H03; ahora también guarda actividades.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El catálogo tiene exactamente las 47 actividades, comparadas con una lista hecha aparte desde el texto de los artículos, y ninguna es de comisiones.
- **SC-002**: En los casos de prueba, ninguna actividad que pide proyecto o cargo queda guardada sin él.
- **SC-003**: Ningún parcial tiene dos veces la misma actividad con el mismo docente y el mismo proyecto.
- **SC-004**: Las horas disponibles y en otras carreras, con propuestas y actividades juntas, coinciden con una suma hecha aparte en el 100 % de los casos de prueba.
- **SC-005**: Un coordinador asigna una actividad en menos de 2 minutos sin ayuda (RNF-USA-01).
- **SC-006**: Guardar una actividad responde en menos de 3 segundos, con holgura.
- **SC-007**: Con un servicio caído, el usuario ve un mensaje claro en menos de 10 segundos, con holgura, y no se guarda nada.

## Assumptions

- Se reutiliza el permiso «Oferta y propuesta de docentes: escritura» con alcance propio: el coordinador ya lo tiene y no hace falta un permiso nuevo. Leer exige los mismos permisos que los parciales.
- El Vicerrector solo ve: sus ajustes llegan con la revisión (RF-35).
- La evaluación (140 g) es una actividad más (C-13). Exigir que planificación y evaluación consten (RN-05) es una validación de F07.
- 143 a incluye ser integrante del Órgano Colegiado Superior, que no es un cargo registrado: en esta historia pide rector o vicerrector.
- Los gestores G10 a G21 no tienen un literal propio: sus horas se registran como 143 j, encargo de funciones o responsabilidades.
- 140 k, proyectos de vinculación articulados a la docencia, es una actividad de docencia y no pide proyecto: RN-25 solo nombra vinculación del art. 142 y la tutoría de prácticas.
- Las actividades se ven en su pantalla: no aparecen en las filas de la oferta ni cambian «N de M propuestas».
- La carrera de la actividad es la del coordinador, no la del docente.

## Out of Scope

- Los totales completos del docente y la proporción 60/40 (F07-H01).
- Las reglas duras, el tope de 16 h y volver a validar lo guardado (F07-H02).
- Calcular las horas de gestión desde el cargo y la tabla de carga horaria.
- Las comisiones (C-12).
- Habilitar o deshabilitar actividades por periodo (RF-21 completo).
- Enviar o devolver el parcial.
- El reemplazo por maternidad o lactancia (RN-16).
