# Feature Specification: Registrar los cargos y los proyectos aprobados

**Feature Branch**: `023-cargos-proyectos`

**Created**: 2026-10-25

**Status**: Draft

**Input**: User description: "Especifica la historia F03-H05 · Registrar los cargos y los proyectos aprobados (issue #39), a partir de RF-18, RN-20, RN-25, RN-12 y el diccionario de datos de las cargas 8 y 9, con las cuatro aclaraciones del autor del 2026-10-24."

**Origen**: issue #39 · F03-H05 · sub-issue de F03 (Mantener los datos maestros: estructura, mallas, oferta, docentes y proyectos) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:04-docentes`, `RF-18`, `RN-20`, `RN-25` · tamaño propuesto M · depende de #38 (F03-H04), cerrada.

**Requerimientos que cubre**:
- **RF-18:** los proyectos y actividades aprobados (investigación, vinculación, prácticas preprofesionales o servicio comunitario), con nombre, tipo, resolución del OCS, estado, vigencia, carreras, director, participantes y horas semanales aprobadas. Criterio: «Solo los proyectos aprobados y vigentes pueden recibir horas de investigación o vinculación».
- **RN-20:** para designar coordinadores y gestores se verifican sus requisitos (POL 5.4).
- **RN-25:** las horas de investigación, vinculación y tutoría de prácticas solo se asignan contra un proyecto aprobado. Aquí se deja saber si un proyecto es asignable; asignar las horas y el máximo de 16 h de docencia son del distributivo (F06).
- **RN-12:** un coordinador de carrera por carrera.
- **ERS §8.3:** las cargas 8 (cargos y autoridades) y 9 (proyectos y prácticas aprobados).

**Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-64 (permisos «solo lo propio»), D-70 (permisos alternativos), D-72 (validación de las entradas y catálogo de errores completo), ADR-016 (catálogo de errores), ADR-021 (permisos). **Principios de la constitución 2.2.0**: II (Datos maestros es de soporte), III (lenguaje ubicuo), IV (API primero), VI (las reglas son datos), VII (nada se borra y toda acción relevante deja registro), IX (mínimo privilegio y datos personales), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero tener los cargos de gestión y los proyectos aprobados del periodo, para que las horas de gestión, investigación y vinculación salgan de datos aprobados.

**Punto de partida.** Docentes ya guarda a cada docente con su ficha académica, su dedicación y su estado laboral (F03-H04). Académico tiene la estructura orgánica con sus unidades y las 21 gestiones autorizadas (F03-H01), las carreras (F03-H02) y los periodos académicos con sus fechas (F02-H01). Falta saber qué docente ocupa qué cargo, que es lo que da horas de gestión, y qué proyectos aprobados por el OCS existen, que son los únicos que pueden recibir horas de investigación, vinculación y prácticas. Esta historia permite registrar, consultar y corregir los cargos y los proyectos en pantalla, con las reglas de las cargas 8 y 9 del diccionario de datos. Las cargas por archivo (F01-H04) aplicarán después las mismas reglas.

**Cómo queda tras las aclaraciones**:
- Mantienen y consultan los cargos y los proyectos los mismos que mantienen y consultan los docentes. El Gestor de unidad, con «Proyectos: lectura» solo de lo propio, no los ve hasta F08 (D-64).
- Un coordinador o gestor sin título de cuarto nivel se registra con una advertencia que dice el requisito que falta. Un docente que no está activo, o que no es de tiempo completo, no puede recibir un cargo.
- Si un proyecto es asignable se decide contra un periodo.
- La confirmación «sin proyectos» del periodo queda para la confirmación de las cargas.

## Clarifications

### Session 2026-10-24

- Q: ¿Quién registra y corrige los cargos y los proyectos aprobados, y quién los consulta? → A: Como los docentes (D-70):
  - **Mantienen** quienes tienen «Docentes: escritura» (Talento Humano), «Catálogos y datos maestros: escritura» (Administrador del sistema) o «Distributivo del periodo: escritura» (Vicerrector académico).
  - **Consultan** quienes tienen «Docentes: lectura», «Catálogos y datos maestros: lectura» o «Distributivo del periodo: lectura». Entre ellos está el Responsable de consolidación.
  - El filtro «solo lo propio» del Gestor de unidad y la confirmación de las horas de sus proyectos quedan para F08. La matriz no cambia.
- Q: ¿Cómo se verifican los requisitos del cargo (RN-20)? → A: Avisa y bloquea:
  - Un coordinador o un gestor cuyo docente no tiene ningún título de cuarto nivel en su ficha se registra, con una advertencia que dice el requisito que falta.
  - Un docente que no está activo, o cuya dedicación no es tiempo completo, no puede recibir ningún cargo (POL 5.4, diccionario de la carga 8).
  - Los años de experiencia no se verifican hasta que la normativa defina un mínimo (principio XIV: no se asume una regla sin definición funcional).
- Q: ¿Contra qué se decide si un proyecto es asignable? → A: Contra un periodo. Un proyecto es asignable en un periodo si está aprobado o en ejecución y su vigencia se cruza con las fechas del periodo. La consulta recibe el periodo y, de cada proyecto no asignable, dice por qué: su estado, su vigencia o los dos.
- Q: ¿La confirmación «sin proyectos» del periodo va en esta historia? → A: No. La resuelven las historias de confirmación de las cargas (F01-H04 y F01-H05). Esta historia solo registra, corrige y consulta.

### Session 2026-10-25

- Q: El diccionario solo dice que el código de un proyecto es «Texto, hasta 30». ¿Qué caracteres admite? → A: Mayúsculas, dígitos y guion, sin espacios, como los códigos de las carreras y las asignaturas. En pantalla se pasa a mayúsculas mientras se escribe. Cubre todos los códigos de los datos de prueba.
- Q: ¿De qué tipo son las horas semanales de cada participante? → A: Un decimal mayor que 0, como las horas aprobadas del proyecto. La suma de las horas de los participantes no puede pasar las aprobadas.

### Session 2026-10-25 (analyze)

- Q: ¿Cuántos decimales admiten las horas semanales, las aprobadas y las de cada participante? → A: Hasta dos, con hasta 5 caracteres en total: 2,5, 2,55, 99,75 y 999,9 pasan; 2,555 y 100,25 no.
- Q: ¿Qué hace el Gestor de unidad con los proyectos mientras no se pueda aplicar su «solo lo propio»? → A: No los ve hasta F08. «Proyectos y horas de participantes: lectura» deja de estar entre las lecturas de esta historia. Su alcance «solo lo propio» todavía no se puede aplicar, porque un proyecto no tiene unidad y un usuario no está vinculado a su docente, y D-64 pide no mostrar más que lo propio. El Responsable de consolidación sigue viéndolos con «Distributivo del periodo: lectura».

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Registrar el cargo de un docente (Priority: P1)

Como quien mantiene los datos de los docentes, quiero registrar el cargo de gestión que ocupa un docente, con la unidad o la carrera que dirige, sus fechas y su documento de designación, para que las horas de gestión salgan de una designación registrada (carga 8, RN-07, RN-08, RN-11).

**Why this priority**: Sin cargos registrados, el distributivo no sabe quién tiene horas de gestión.

**Independent Test**: Registrar al coordinador de una carrera, que es un docente activo a tiempo completo con maestría, y verlo en la lista de cargos.

**Acceptance Scenarios**:

1. **Given** un docente activo a tiempo completo con un título de cuarto nivel y una carrera sin coordinador vigente, **When** lo registro como coordinador de esa carrera con su fecha de designación y su resolución, **Then** el cargo queda vigente, sin advertencias, y el registro guarda quién y cuándo.
2. **Given** un docente inactivo, en licencia o con dedicación de medio tiempo o tiempo parcial, **When** le registro cualquier cargo, **Then** se rechaza con el motivo y no se guarda nada.
3. **Given** una carrera que ya tiene un coordinador vigente, **When** registro otro coordinador vigente para ella, **Then** se rechaza: cada carrera tiene un solo coordinador vigente (RN-12).
4. **Given** el mismo docente, el mismo cargo y la misma fecha de designación de un cargo ya registrado, **When** lo registro, **Then** se rechaza como repetido.
5. **Given** un cargo de gestor sin la unidad de su gestión, un miembro de comisión cuya unidad no es una comisión, una unidad o una carrera que no existe, o un fin de funciones anterior a la designación o a más de dos años de ella, **When** guardo, **Then** se rechaza con su campo y el motivo.

---

### User Story 2 - Advertir los requisitos del cargo (Priority: P1)

Como quien registra un cargo, quiero que el sistema me advierta si al docente le falta un requisito del cargo, para saber qué completar antes de que la designación cuente (RN-20, POL 5.4).

**Why this priority**: Es el segundo escenario del issue.

**Independent Test**: Registrar como coordinador de carrera a un docente activo a tiempo completo sin títulos de cuarto nivel y ver la advertencia.

**Acceptance Scenarios**:

```gherkin
Escenario: Requisitos del cargo
  Dado un cargo de coordinador asignado a un docente sin título de cuarto nivel registrado
  Cuando se registra el cargo
  Entonces el sistema muestra una advertencia con el requisito que falta
```

1. **Given** un docente sin títulos de cuarto nivel en su ficha, **When** lo registro como coordinador o como gestor, **Then** el cargo se guarda y la respuesta trae la advertencia «Le falta un título de cuarto nivel registrado (RN-20)».
2. **Given** ese cargo ya registrado, **When** consulto los cargos, **Then** sigue mostrando su advertencia mientras al docente le falte el requisito, y deja de mostrarla cuando su ficha tenga un título de cuarto nivel.
3. **Given** un rector, un vicerrector o un miembro de comisión sin título de cuarto nivel, **When** lo registro, **Then** no hay advertencia: RN-20 habla de coordinadores y gestores.

---

### User Story 3 - Registrar un proyecto aprobado (Priority: P1)

Como quien mantiene los datos, quiero registrar un proyecto aprobado por el OCS con su tipo, su resolución, su vigencia, sus carreras, su director y sus participantes con sus horas, para que las horas de investigación, vinculación y prácticas salgan de un proyecto aprobado (RF-18, carga 9).

**Why this priority**: Sin proyectos no se pueden asignar horas de investigación, vinculación ni prácticas.

**Independent Test**: Registrar un proyecto de investigación en ejecución con dos participantes cuyas horas suman las aprobadas y verlo en la lista.

**Acceptance Scenarios**:

1. **Given** carreras y docentes registrados, **When** registro un proyecto con su resolución del OCS, su vigencia, su director y sus participantes, **Then** queda guardado y el registro guarda quién y cuándo.
2. **Given** un código de proyecto ya registrado, **When** registro, **Then** se rechaza como repetido.
3. **Given** participantes cuyas horas suman más que las horas semanales aprobadas, un participante o un director que no es un docente registrado, una carrera que no existe, un proyecto sin carreras o sin participantes, un fin de vigencia anterior al inicio o unas horas aprobadas que no son mayores que 0, **When** guardo, **Then** se rechaza con su campo (y, en las listas, su elemento) y el motivo.

---

### User Story 4 - Saber si un proyecto es asignable en un periodo (Priority: P1)

Como Responsable de consolidación, quiero ver qué proyectos son asignables en un periodo y, de los que no lo son, por qué, para que solo los aprobados y vigentes reciban horas (RF-18, RN-25).

**Why this priority**: Es el primer escenario del issue y el criterio de RF-18.

**Independent Test**: Consultar los proyectos de un periodo y ver como no asignables uno suspendido y uno cuya vigencia terminó antes del periodo.

**Acceptance Scenarios**:

```gherkin
Escenario: Proyecto no asignable
  Dado un proyecto no aprobado por el OCS o con la vigencia vencida
  Cuando se consulta para asignar horas
  Entonces aparece como no asignable
```

1. **Given** un proyecto aprobado o en ejecución cuya vigencia se cruza con el periodo, **When** consulto los proyectos del periodo, **Then** aparece como asignable.
2. **Given** un proyecto finalizado o suspendido, **When** consulto, **Then** aparece como no asignable por su estado.
3. **Given** un proyecto aprobado cuya vigencia termina antes de que empiece el periodo, o empieza después de que termine, **When** consulto, **Then** aparece como no asignable por su vigencia.
4. **Given** un periodo que no existe, **When** consulto, **Then** se responde que el periodo no existe.

---

### User Story 5 - Corregir y finalizar sin borrar (Priority: P2)

Como quien mantiene los datos, quiero corregir un cargo o un proyecto y darlo por finalizado sin borrarlo, para que los datos sigan siendo correctos y quede la historia de cada cambio (principio VII).

**Why this priority**: Las designaciones terminan y los proyectos cambian de estado; sin corrección los datos envejecen.

**Independent Test**: Finalizar un cargo con su fecha de fin y pasar un proyecto a suspendido; ver ambos cambios en el registro.

**Acceptance Scenarios**:

1. **Given** un cargo vigente, **When** lo finalizo con su fecha de fin, **Then** queda finalizado. Su carrera puede recibir otro coordinador vigente.
2. **Given** un proyecto, **When** corrijo sus participantes, su vigencia o su estado, **Then** se guardan las mismas reglas del registro y el cambio queda con el valor anterior y el nuevo.
3. **Given** cualquier cargo o proyecto, **When** busco cómo borrarlo, **Then** no existe esa acción: se finaliza.

---

### User Story 6 - Las pantallas de cargos y proyectos (Priority: P2)

Como Responsable de consolidación o como quien mantiene los datos, quiero consultar y mantener los cargos y los proyectos en pantalla, para trabajar sin archivos.

**Why this priority**: La historia se comprueba en el equipo de demostración por la pantalla.

**Independent Test**: Abrir los cargos, filtrar por vigentes, registrar uno; abrir los proyectos de un periodo y ver cuáles son asignables; sin permiso de escritura, verlos en solo lectura.

**Acceptance Scenarios**:

1. **Given** cargos registrados, **When** abro su pantalla, **Then** veo cada cargo con su docente, su unidad o carrera, sus fechas, su estado y su advertencia, si tiene.
2. **Given** proyectos registrados y un periodo elegido, **When** abro su pantalla, **Then** veo cada proyecto con su tipo, su estado, su vigencia, sus horas y si es asignable en ese periodo o por qué no.
3. **Given** el permiso de escritura, **When** registro o corrijo, **Then** cada campo solo admite lo suyo mientras se escribe. Al guardar, cada campo con un problema queda marcado con un motivo que dice qué hacer, antes de enviar nada (principio XIV).
4. **Given** un usuario sin permiso de escritura, **When** abre las pantallas, **Then** las ve en solo lectura, sin acciones, y la pantalla dice por qué.

---

### Edge Cases

Las reglas de cada campo salen del diccionario de datos de las cargas 8 y 9 (principio XIV). Lo que el diccionario no define no se valida.

**Cargo (carga 8)**

| Campo | Tipo y caracteres | Largo | Regla |
|---|---|---|---|
| Docente | Identificación de un docente registrado | Hasta 13 | Activo y de tiempo completo (POL 5.4). |
| Cargo | Uno del catálogo de cargos | Hasta 40 | Rector, vicerrector, coordinador académico institucional, coordinador de sede, coordinador de carrera, uno por cada gestión autorizada G06 a G21 de POL 6, o miembro de comisión. |
| Unidad | Código de una unidad activa de la estructura orgánica: mayúsculas, dígitos, guion y guion bajo | Hasta 20 | Obligatoria salvo para el coordinador de carrera. La de un gestor es la unidad de su gestión autorizada; la de un miembro de comisión, una comisión. |
| Carrera | Código de una carrera de Académico | Hasta 20 | Obligatoria solo para el coordinador de carrera. Un solo coordinador vigente por carrera (RN-12). |
| Fecha de designación | Fecha aaaa-mm-dd | 10 | Obligatoria. |
| Fecha de fin | Fecha aaaa-mm-dd | 10 | Opcional. Posterior a la designación y hasta dos años después (POL 5.4). |
| Documento de designación | Texto | Hasta 60 | Obligatorio: la resolución o la acción de personal. |
| Estado | Vigente o finalizado | — | Un cargo nuevo queda vigente. |

- La clave de un cargo es el docente, el cargo y la fecha de designación. Un docente puede tener más de un cargo; que sus horas sumen bien lo revisa el distributivo.
- **Vigente** es el estado del cargo: lo decide quien lo mantiene, al finalizarlo. Las fechas las usa el distributivo para saber en qué periodos da horas.
- **Advertencias:** se calculan cada vez que se consulta el cargo; no se guardan ni bloquean nada.
  - **Requisitos (RN-20):** un coordinador (académico institucional, de sede o de carrera) o un gestor (G06 a G21) cuyo docente no tiene ningún título de cuarto nivel en su ficha. El cargo se guarda.
  - **Fecha de fin vencida:** un cargo vigente cuya fecha de fin ya pasó. Hay que finalizarlo.
  - **Docente que dejó de estar habilitado:** el docente de un cargo vigente ya no está activo o ya no es de tiempo completo. Al registrar o corregir el cargo, eso sigue siendo un rechazo.
- **Coordinador académico institucional:** se registra. El aviso de RN-09, que dice que el tipo de IST no lo prevé, lo da el motor (F06), igual que el número de gestores por tipo de IST (RN-10).

**Proyecto aprobado (carga 9)**

| Campo | Tipo y caracteres | Largo | Regla |
|---|---|---|---|
| Código | Mayúsculas, dígitos y guion, sin espacios (aclaración 5) | Hasta 30 | Obligatorio y único. No cambia. |
| Nombre | Texto libre | Hasta 250 | Obligatorio. |
| Tipo | Investigación, vinculación, prácticas preprofesionales o servicio comunitario | — | Obligatorio (POL 5.3). |
| Carreras | Lista de códigos de carreras de Académico | De 1 a 20, que cubren los 200 caracteres del archivo | Sin repetir. |
| Resolución del OCS | Texto | Hasta 60 | Obligatoria (RN-13, RN-25). |
| Fecha de la resolución | Fecha aaaa-mm-dd | 10 | Obligatoria. |
| Estado | Aprobado, en ejecución, finalizado o suspendido | — | Obligatorio. |
| Vigencia | Fechas de inicio y fin, aaaa-mm-dd | 10 cada una | Obligatorias; el fin es posterior al inicio. |
| Director | Identificación de un docente registrado | Hasta 13 | Obligatorio. |
| Participantes | Lista de docente, rol y horas semanales | De 1 a 40, que cubren los 1000 caracteres del archivo | Docentes registrados, sin repetir. El rol es director, investigador, docente de apoyo o tutor de prácticas. Las horas siguen la regla de las horas aprobadas (aclaraciones 6 y 7). |
| Horas semanales aprobadas | Decimal con hasta dos decimales | Hasta 5 caracteres | Mayor que 0 (aclaración 7). La suma de las horas de los participantes no la pasa. |

- **Asignable en un periodo:** un proyecto está aprobado o en ejecución y su vigencia (inicio y fin, inclusive) se cruza con las fechas del periodo. Si no es asignable, se dice por qué: su estado, su vigencia o los dos.
- La duración «típica de 1 a 2 años» del diccionario es una descripción, no una regla, y no se valida.
- **Datos personales:** los cargos y los proyectos nombran docentes por su identificación. Los datos de prueba del repositorio y del equipo de demostración son ficticios.
- **Usuario sin permiso:** quien no tiene ningún permiso de escritura de los tres ve los cargos y los proyectos en solo lectura. Si intenta cambiar algo, el servidor responde «Acceso no autorizado» (403). Quien no tiene ninguno de los tres de lectura no los ve; tampoco quien solo tiene «Proyectos y horas de participantes: lectura» (aclaración 8).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE guardar cada cargo con su docente, su cargo, su unidad o su carrera, su fecha de designación, su fecha de fin, su documento de designación y su estado, como en la carga 8.
- **FR-002**: El sistema DEBE guardar cada proyecto aprobado con su código, su nombre, su tipo, sus carreras, su resolución del OCS con su fecha, su estado, su vigencia, su director, sus participantes con su rol y sus horas semanales, y sus horas semanales aprobadas, como en la carga 9 (RF-18).
- **FR-003**: Se DEBE poder registrar, consultar y corregir los cargos y los proyectos. Un cargo se finaliza y un proyecto cambia de estado; nada se borra (principio VII). La clave de un cargo y el código de un proyecto no cambian.
- **FR-004**: Cada campo DEBE cumplir las reglas de tipo, caracteres y largo de los casos límite, en el servidor y en la pantalla (principio XIV). Cada regla se prueba con casos válidos e inválidos, también en sus límites.
- **FR-005**: Un cargo DEBE rechazarse si su docente no está activo o no es de tiempo completo, si repite la clave de otro, si da a una carrera un segundo coordinador vigente (RN-12) o si su unidad no corresponde a su cargo.
- **FR-006**: Registrar o corregir un coordinador o un gestor cuyo docente no tiene un título de cuarto nivel DEBE guardar el cargo y devolver una advertencia con el requisito que falta (RN-20, aclaración 2). La consulta de los cargos DEBE mostrar esa advertencia mientras el requisito siga faltando. También DEBE advertir de un cargo vigente con la fecha de fin vencida y de uno cuyo docente dejó de estar activo a tiempo completo.
- **FR-007**: Un proyecto DEBE rechazarse si repite el código de otro, si sus participantes suman más horas que las aprobadas o si su director, sus participantes o sus carreras no existen.
- **FR-008**: Las unidades, las carreras y las fechas de los periodos DEBEN venir de Académico, y los docentes con su estado, su dedicación y sus títulos de cuarto nivel, de Docentes.
- **FR-009**: La consulta de los proyectos de un periodo DEBE decir de cada uno si es asignable y, si no, por qué: su estado, su vigencia o los dos (aclaración 3).
- **FR-010**: Cada dato inválido DEBE rechazarse entero, con su campo (y, en las listas, su elemento) y un motivo que dice qué hacer. Cada rechazo DEBE responder con un código del catálogo de errores, con sus 8 columnas llenas (principio XIII, D-72). No se guarda nada a medias.
- **FR-011**: Mantener cargos y proyectos DEBE exigir «Docentes: escritura», «Catálogos y datos maestros: escritura» o «Distributivo del periodo: escritura». Consultarlos DEBE exigir «Docentes: lectura», «Catálogos y datos maestros: lectura» o «Distributivo del periodo: lectura»; «Proyectos y horas de participantes: lectura» no basta hasta F08 (D-64). El servidor lo valida (ADR-021, D-70, aclaraciones 1 y 8).
- **FR-012**: Cada registro, corrección, finalización y cambio de estado DEBE quedar registrado con quién, cuándo, qué cambió, el valor anterior y el nuevo (principio VII).
- **FR-013**: Las pantallas DEBEN listar los cargos, con filtros por estado y por docente, y los proyectos de un periodo, con su condición de asignable. También DEBEN permitir a quien tiene el permiso registrar, corregir y finalizar, con la validación en pantalla del principio XIV.
- **FR-014**: Los catálogos de las cargas 8 y 9 (cargos, estados del cargo, tipos de proyecto, estados del proyecto y roles de participante) DEBEN ser los del diccionario de datos y vivir en un solo lugar (principio VI).

### Key Entities

- **Cargo**: puesto institucional que un docente ocupa y que da horas de gestión: rector, vicerrector, coordinadores y gestores (glosario, `Cargo`, Datos maestros). Su clave es el docente, el cargo y la fecha de designación.
- **Gestión autorizada**: una de las 21 de POL 6 (`GestionAutorizada`, de Académico). El cargo de cada gestor corresponde a una de las gestiones G06 a G21.
- **Unidad orgánica** y **Carrera**: las de Académico, que dirige o coordina el cargo.
- **Proyecto aprobado**: proyecto de investigación, de vinculación o práctica preprofesional o de servicio comunitario aprobado por el OCS, con su resolución, su vigencia, sus participantes y sus horas semanales aprobadas (glosario, `Proyecto`).
- **Participante**: un docente en un proyecto, con su rol y sus horas semanales.
- **Periodo académico**: el de Académico, con sus fechas, contra el que se decide si un proyecto es asignable.
- **Docente**: el de F03-H04, con su estado laboral, su dedicación y los títulos de cuarto nivel de su ficha.
- **Cambio de los datos maestros**: registro de quién, cuándo y qué cambió, con el valor anterior y el nuevo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quien mantiene los datos registra el cargo de un docente, o consulta si un proyecto es asignable en un periodo, en menos de 1 minuto y sin ayuda.
- **SC-002**: Los 21 cargos y los 11 proyectos de los datos de prueba de las cargas 8 y 9 se registran todos con las mismas reglas, sobre los docentes, las unidades, las carreras y los periodos de prueba.
- **SC-003**: En los proyectos de un periodo, el 100 % de los finalizados, los suspendidos y los de vigencia que no se cruza con el periodo aparecen como no asignables, con su motivo, y el 100 % de los aprobados o en ejecución con vigencia que se cruza aparecen como asignables.
- **SC-004**: Registrar a un coordinador o un gestor sin título de cuarto nivel muestra siempre la advertencia con el requisito que falta; a un rector, un vicerrector o un miembro de comisión, nunca.
- **SC-005**: El 100 % de los intentos de guardar un dato fuera de las reglas de las cargas 8 y 9 se rechazan con su motivo, también llamando directamente al servidor. Cada rechazo lleva un código del catálogo de errores.
- **SC-006**: Cada cambio de un cargo o de un proyecto aparece en el registro con quién, cuándo, el valor anterior y el nuevo.

## Assumptions

- Las reglas de cada campo son las del diccionario de datos de las cargas 8 y 9 (F01-H01). Si la ERS cambia, cambian allí.
- Los cargos de gestor son uno por cada gestión autorizada G06 a G21, con el código de la gestión y su nombre, como en los datos de prueba (por ejemplo, `G07_GESTOR_INVESTIGACION` o `G10_COORDINADOR_CALIDAD`). Su unidad es la unidad de esa gestión en la estructura orgánica.
- RN-20 alcanza a los coordinadores y a los gestores. El rector y el vicerrector tienen sus propios requisitos en la ley, y el miembro de comisión no tiene horas (C-12): ninguno lleva advertencia.
- Que el docente esté activo y sea de tiempo completo se exige a todo cargo, como pide el diccionario para la carga 8 (POL 5.4).
- Un cargo vale por sus fechas. En qué periodos da horas lo decide el distributivo, no esta historia.
- RN-12 pide un coordinador por cada carrera ofertada. Aquí solo se impide el segundo coordinador vigente; que no falte ninguno lo revisa la validación del distributivo (F06).
- Que el director figure entre los participantes como director no está definido en el diccionario, y no se valida.
- Los periodos y sus fechas vienen de Académico; un proyecto no pertenece a un periodo, sino que es asignable o no en cada uno.
- Registrar por pantalla es la vía de esta historia. Las cargas 8 y 9 por archivo se aplican con F01-H04 sobre las mismas reglas, y la confirmación «sin proyectos» con F01-H04 y F01-H05.
- Fuera de alcance:
  - las cargas 8 y 9 por archivo y la confirmación «sin proyectos» (F01-H04, F01-H05);
  - el cálculo de las horas de gestión por cargo y el máximo de 16 h de docencia con proyecto (motor, F06);
  - el número de gestores por tipo de IST (RN-10) y el aviso del coordinador académico institucional (RN-09);
  - el filtro «solo lo propio» del Gestor de unidad y la confirmación de las horas de los proyectos (F08);
  - las comisiones con horas (C-12);
  - los años de experiencia de RN-20.
