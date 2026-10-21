# Feature Specification: Abrir el periodo académico con su versión de normativa

**Feature Branch**: `015-abrir-periodo`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Especifica la historia F02-H01 · Abrir el periodo académico con su versión de normativa (issue #32), a partir de RF-13, RF-19, la pregunta 4 del modelo de dominio y lo que dejaron F04-H01, F04-H02 y F04-H03."

**Origen**: issue #32 · F02-H01 · sub-issue de F02 (Abrir el periodo con plazos y ver el tablero de preparación) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:11-academico`, `RF-13`, `RF-19` · tamaño propuesto M · depende de #41 (F04-H02), cerrada.

**Requerimientos que cubre**: RF-13 (los periodos académicos con estados Planificación, En aprobación, Vigente y Cerrado; un periodo cerrado no admite cambios), RF-19 en la parte «cada periodo se asocia a una versión de normativa», la pregunta 4 del modelo de dominio §9.2 («quién cambia el estado del periodo: el Vicerrectorado, a mano, en Datos maestros») y la ERS §4, paso 1 de la preparación («el Vicerrectorado abre el periodo académico… y selecciona la versión de normativa vigente»). **Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-67 (el Vicerrector académico escribe «Parámetros y normativa»), ADR-021 (permisos), ADR-016 (catálogo de errores), D-35 (el esquema de Académico gana sus tablas con sus historias), MD-10 (se dice «periodo académico», no «ciclo»). **Principios de la constitución**: VII (una versión de normativa en uso no cambia; toda acción relevante deja registro).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero abrir el periodo académico y asociarle la versión de normativa vigente, para que el distributivo se arme con las reglas correctas.

**Punto de partida.** El servicio Académico no tiene todavía ningún dato de negocio: su esquema está vacío (D-35). Normativa ya sabe asociar una versión a un periodo, nombrado por su código, y consultar qué versión lo rige (F04-H02): una vez asociada, la versión no cambia, y un periodo no puede pasar a otra versión. Pero ese código hoy no corresponde a ningún periodo que exista: nadie crea periodos, ni guarda su estado. La carga 3 por archivo (ciclos académicos aprobados: código, fechas de inicio y fin, semanas) llega con F01, que todavía no existe. Esta historia crea el periodo académico en Académico, con su estado, y permite abrirlo eligiendo su versión de normativa y hacerlo avanzar por sus estados.

## Clarifications

### Session 2026-10-21

- Q: La carga 3 (F01) todavía no existe. ¿De dónde sale el periodo en esta historia? → A: Se registra a mano en la pantalla «Periodos», con código, nombre y fechas de inicio y fin, y empieza en Planificación sin versión. Cuando llegue F01, la carga 3 usará el mismo registro.
- Q: ¿Quién registra, abre y avanza los periodos, y con qué permiso de la matriz D-65? → A: El Vicerrector académico, con la escritura de «Distributivo del periodo», que solo tiene él; la consulta, con la lectura de «Distributivo del periodo». La matriz no cambia. El Administrador del sistema no escribe periodos.
- Q: Además del orden, ¿se puede retroceder o cada paso exige algo más? → A: Solo hacia adelante, un paso a la vez y sin retroceder. Salir de Planificación exige versión de normativa. Lo que dependa del distributivo (por ejemplo, estar aprobado para pasar a Vigente) lo agregan las historias de Aprobación.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Abrir el periodo con su versión de normativa (Priority: P1)

Como Vicerrector académico, quiero abrir un periodo académico en Planificación y elegir su versión de normativa, para que el distributivo de ese periodo se valide con las reglas correctas (RF-13, RF-19).

**Why this priority**: Es el primer escenario del issue y el paso 1 de la preparación del periodo (ERS §4). Sin periodo con versión, Distributivo no sabría con qué reglas validar (F06, F07), y la asociación de Normativa quedaría sin un periodo real detrás.

**Independent Test**: Tener un periodo en Planificación, abrirlo eligiendo una versión de normativa sin periodo, y comprobar que el periodo muestra esa versión, que Normativa dice que esa versión lo rige y que la versión ya no se puede editar.

**Acceptance Scenarios**:

```gherkin
Escenario: Abrir el periodo
  Dado un periodo de la carga 3 en estado Planificación
  Cuando lo abro y elijo la versión de normativa
  Entonces el periodo queda asociado a esa versión
  Y esa versión de normativa ya no se puede modificar
```

1. **Given** un periodo en Planificación sin versión de normativa, **When** lo abro y elijo una versión, **Then** el periodo queda asociado a esa versión, la consulta del periodo en Normativa devuelve esa versión, y editar esa versión responde 409 con el mensaje que pide crear una versión nueva.
2. **Given** un periodo que ya tiene versión, **When** intento abrirlo con otra versión, **Then** se rechaza con el motivo en español y sigue con la que tenía; abrirlo otra vez con la misma no cambia nada.
3. **Given** una versión que ya rige otro periodo, **When** la elijo para este periodo, **Then** se acepta: varias versiones pueden regir periodos distintos y una versión puede regir varios periodos.
4. **Given** un periodo que no está en Planificación (y que por eso ya tiene versión), **When** intento abrirlo con otra versión, **Then** se rechaza con el motivo en español; abrirlo con la misma no cambia nada, como en el escenario 2.
5. **Given** que el periodo quedó abierto, **Then** queda registrado quién lo abrió, cuándo y con qué versión.

---

### User Story 2 - Avanzar el estado del periodo (Priority: P1)

Como Vicerrector académico, quiero cambiar el estado del periodo de Planificación a En aprobación, a Vigente y a Cerrado, en ese orden, para que el resto del sistema sepa en qué etapa está (RF-13, pregunta 4).

**Why this priority**: Es el segundo escenario del issue. Otras partes dependen del estado: la configuración del flujo solo cambia en Planificación (RF-34) y el tablero muestra el estado del periodo (F02-H03).

**Independent Test**: Con un periodo abierto, avanzarlo paso a paso hasta Cerrado y comprobar que cada paso queda; intentar saltar un estado o retroceder y comprobar el rechazo.

**Acceptance Scenarios**:

```gherkin
Escenario: Estados del periodo
  Dado un periodo abierto
  Cuando el Vicerrectorado cambia su estado
  Entonces solo pasa de Planificación a En aprobación, a Vigente y a Cerrado, en ese orden
```

1. **Given** un periodo abierto en Planificación, **When** lo paso a En aprobación, luego a Vigente y luego a Cerrado, **Then** cada cambio queda y se registra con quién, cuándo, el estado anterior y el nuevo.
2. **Given** un periodo en Planificación, **When** intento pasarlo directamente a Vigente o a Cerrado, **Then** se rechaza con el motivo en español y sigue en Planificación.
3. **Given** un periodo en En aprobación o en Vigente, **When** intento volverlo a un estado anterior, **Then** se rechaza con el motivo en español.
4. **Given** un periodo en Planificación sin versión de normativa, **When** intento pasarlo a En aprobación, **Then** se rechaza con el motivo en español: el periodo se abre antes de avanzar.

---

### User Story 3 - Un periodo cerrado no cambia (Priority: P1)

Como Vicerrector académico, quiero que un periodo cerrado no admita cambios, para que lo que se aprobó y se cumplió en ese periodo quede como fue (RF-13, principio VII).

**Why this priority**: Es el tercer escenario del issue y el criterio de aceptación de RF-13.

**Independent Test**: Cerrar un periodo e intentar cambiar su nombre, sus fechas, su estado y su versión de normativa; todos se rechazan y el periodo sigue igual.

**Acceptance Scenarios**:

```gherkin
Escenario: Periodo cerrado
  Dado un periodo en estado Cerrado
  Cuando alguien intenta cambiar sus datos
  Entonces el cambio se rechaza
```

1. **Given** un periodo en Cerrado, **When** alguien intenta cambiar su nombre o sus fechas, **Then** se rechaza con 409 y un mensaje en español, y el periodo no cambia.
2. **Given** un periodo en Cerrado, **When** alguien intenta cambiar su estado, **Then** se rechaza: Cerrado es el último estado.
3. **Given** un periodo en Cerrado, **When** se consulta, **Then** se ven todos sus datos, su estado y su versión de normativa, en solo lectura.

---

### User Story 4 - Ver y registrar los periodos en pantalla (Priority: P1)

Como Vicerrector académico, quiero ver los periodos académicos en una pantalla «Periodos», con su estado y su versión de normativa, y hacer allí lo de las historias anteriores, para no depender de llamar al servidor.

**Why this priority**: La historia la pide el Vicerrector, que trabaja en la interfaz. Sin pantalla, los escenarios solo se podrían probar llamando al servidor.

**Independent Test**: Entrar como Vicerrector, ver la lista de periodos, registrar uno, abrirlo eligiendo la versión de una lista y avanzar su estado; entrar con un usuario sin permiso de escritura y ver lo mismo en solo lectura.

**Acceptance Scenarios**:

1. **Given** periodos registrados, **When** abro «Periodos», **Then** veo cada uno con su código, nombre, fechas, estado y versión de normativa (o «sin versión»), en español.
2. **Given** un periodo en Planificación y el permiso de escritura, **When** elijo una versión de normativa de la lista y confirmo, **Then** el periodo muestra la versión y la pantalla «Normativa» la muestra como en uso.
3. **Given** el permiso de escritura, **When** avanzo el estado, **Then** la pantalla solo ofrece el estado siguiente y pide confirmar antes de cerrar.
4. **Given** un usuario sin permiso de escritura, **When** abre «Periodos», **Then** ve los periodos en solo lectura, sin acciones.

---

### Edge Cases

- **De dónde sale el periodo**: se registra a mano en «Periodos», con código, nombre y fechas de inicio y fin. Cuando llegue F01, la carga 3 usará el mismo registro; ninguno llega precargado.
- **Código del periodo**: es la clave que comparten Académico y Normativa; DEBE cumplir el mismo formato que ya acepta Normativa (letras, números, punto, guion y guion bajo, hasta 20 caracteres) y ser único. Un código repetido se rechaza.
- **Fechas**: la fecha de fin DEBE ser posterior a la de inicio. Que dos periodos se solapen no se rechaza: puede haber periodos en paralelo.
- **Versión que no existe**: elegir un número de versión que no existe se rechaza con el motivo junto a la versión elegida, y el periodo sigue sin versión. «No encontrado» queda para un periodo que no existe.
- **La asociación falla**: si Normativa no puede guardar la asociación, el periodo no se abre (sigue sin versión), se informa el error y no queda `PERIODO_ABIERTO` en el registro: Académico solo registra después de que Normativa confirma.
- **Abrir dos veces a la vez**: si dos personas abren el mismo periodo con versiones distintas al mismo tiempo, una gana y la otra recibe el rechazo de «el periodo ya tiene versión».
- **Versión con periodos asociados en Normativa sin periodo en Académico**: hoy ninguna versión rige un periodo (el equipo tiene 0 asociaciones el 2026-10-21, y las pruebas de punta a punta borran las suyas). Si existieran, no se migran: Académico empieza vacío.
- **Datos que no son el estado**: nombre y fechas se pueden corregir mientras el periodo no esté Cerrado; el código no cambia nunca, porque Normativa y las demás partes lo usan como referencia.
- **Usuario sin permiso**: quien no tenga el permiso de escritura ve los periodos en solo lectura y el servidor responde «Acceso no autorizado» (403) si intenta cambiarlos. El permiso de escritura es el de «Distributivo del periodo», que solo tiene el Vicerrector académico (D-65); el Administrador del sistema no escribe periodos. El de lectura es el de «Distributivo del periodo»: lo tienen, entre otros, el Rector, el Responsable de consolidación y el Coordinador de carrera.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE guardar periodos académicos con código único, nombre, fecha de inicio, fecha de fin, estado (Planificación, En aprobación, Vigente o Cerrado) y la versión de normativa que los rige, si ya la tienen. Un periodo nuevo empieza en Planificación y sin versión.
- **FR-002**: Abrir un periodo DEBE asociarle una versión de normativa existente, solo mientras está en Planificación. La asociación DEBE quedar en Normativa, que es la dueña (AsignacionNormativa), de modo que la consulta del periodo en Normativa y la del periodo en Académico digan la misma versión.
- **FR-003**: Desde que un periodo tiene versión, esa versión NO DEBE poder modificarse (lo garantiza Normativa, F04-H02), y el periodo NO DEBE poder pasar a otra versión. Abrirlo otra vez con la misma versión no cambia nada.
- **FR-004**: El estado del periodo DEBE avanzar solo un paso a la vez y en este orden: Planificación → En aprobación → Vigente → Cerrado. Salir de Planificación DEBE exigir que el periodo tenga versión de normativa. Cualquier otro cambio de estado se rechaza con el motivo en español.
- **FR-005**: Un periodo en Cerrado NO DEBE admitir ningún cambio: ni de datos, ni de estado, ni de versión. El intento responde 409 con un código del catálogo de errores y un mensaje en español.
- **FR-006**: El código, el nombre y las fechas DEBEN validarse: código con el formato que acepta Normativa y único; nombre no vacío y de hasta 120 caracteres; fin posterior al inicio. Un valor inválido se rechaza entero, con el motivo en español, sin dejar el periodo a medias.
- **FR-007**: Registrar un periodo, corregir sus datos, abrirlo y cambiar su estado DEBEN exigir la escritura de «Distributivo del periodo», validada en el servidor (ADR-021); consultar los periodos DEBE exigir su lectura. La matriz D-65 no cambia. Abrir el periodo no pide además la escritura de «Parámetros y normativa»: Académico se la pide a Normativa dentro del mismo proceso (plan, research §4).
- **FR-008**: Cada registro, corrección, apertura y cambio de estado DEBE quedar registrado con quién, cuándo, qué cambió, el valor anterior y el nuevo (principio VII).
- **FR-009**: El sistema DEBE ofrecer la consulta de un periodo por su código y la lista de periodos, con su estado y su versión, para que otras partes (el tablero de F02-H02 y F02-H03, Distributivo, Aprobación por RF-34) sepan en qué estado está y qué normativa lo rige.
- **FR-010**: La pantalla «Periodos» DEBE mostrar la lista de periodos con su código, nombre, fechas, estado y versión, y permitir a quien tenga el permiso de escritura registrar un periodo, corregirlo, abrirlo eligiendo una versión de la lista de versiones de normativa, y avanzar su estado, pidiendo confirmación antes de cerrarlo.

### Key Entities

- **Periodo académico**: lapso para el que se arma un distributivo (glosario §7.3, `PeriodoAcademico`, Datos maestros). Tiene código, nombre, fechas, estado y la versión de normativa que lo rige.
- **Estado del periodo**: Planificación, En aprobación, Vigente o Cerrado (`EstadoPeriodo`, RF-13).
- **Asignación normativa**: el periodo, nombrado por su código, al que se asocia una versión de normativa; la guarda Normativa (glosario §7.3, `AsignacionNormativa`, F04-H02).
- **Versión de normativa**: como en F04-H02 y F04-H03; desde que rige un periodo no cambia.
- **Cambio de periodo**: registro de quién, cuándo y qué cambió (dato, versión o estado), con el valor anterior y el nuevo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El Vicerrector académico abre un periodo eligiendo su versión de normativa y lo pasa a En aprobación en menos de 2 minutos, sin ayuda.
- **SC-002**: Después de abrir un periodo, el 100 % de los intentos de editar su versión de normativa se rechazan, y la consulta del periodo en Normativa y en Académico devuelve la misma versión.
- **SC-003**: El 100 % de los cambios de estado fuera del orden Planificación → En aprobación → Vigente → Cerrado se rechazan, también llamando directamente al servidor.
- **SC-004**: El 100 % de los intentos de cambiar un periodo cerrado se rechazan, y sus datos siguen idénticos.
- **SC-005**: Cada registro, corrección, apertura y cambio de estado aparece en el registro con quién, cuándo, el valor anterior y el nuevo.

## Assumptions

- El periodo es un dato de Datos maestros, en el servicio Académico (modelo de dominio §3, «F02 (periodo)»), y la versión que lo rige la guarda Normativa (AsignacionNormativa). Académico la pide por la API de Normativa que ya existe (F04-H02).
- «Abrir el periodo» es asociarle su versión de normativa mientras está en Planificación; el periodo existe antes de abrirse.
- Los requisitos para pasar a Vigente o a Cerrado que dependen del distributivo (por ejemplo, estar aprobado o publicado) llegarán con las historias de Aprobación; aquí solo se exige el orden y, para salir de Planificación, la versión de normativa.
- Las semanas del periodo (carga 3) se derivan de sus fechas y no se guardan aparte.
- Los plazos del periodo (RN-23, 30 días antes) y el tablero de preparación son de otras historias de F02; el Responsable de consolidación (D-13) se designa en otra historia.
- Quién escribe periodos lo fijó el autor el 2026-10-21: el Vicerrector académico, por la pregunta 4 del modelo de dominio y el paso 1 de la preparación (ERS §4), con la escritura de «Distributivo del periodo» de D-65.
- Los datos son ficticios.

## Dependencies

- F04-H02 (#41), cerrada: asociar una versión a un periodo y consultar qué versión lo rige, en Normativa; una versión en uso no cambia.
- F04-H03 (#42), cerrada: las versiones tienen sus pesos, su mínimo y sus severidades.
- F05-H02 (#44), cerrada, y D-65: la matriz de permisos.
- F01 (carga 3) usará los periodos de esta historia; F02-H02 y F02-H03 (tablero y estado del periodo), F06 y F07 (Distributivo) y RF-34 (configuración del flujo) dependerán del estado del periodo y de su versión.
