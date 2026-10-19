# Feature Specification: Crear una versión nueva de la normativa

**Feature Branch**: `013-version-normativa-nueva`

**Created**: 2026-10-18

**Status**: Draft

**Input**: User description: "Especifica la historia F04-H02 · Crear una versión nueva de la normativa (issue #41), a partir de RF-19, el modelo de dominio de Normativa y lo que dejó F04-H01."

**Origen**: issue #41 · F04-H02 · sub-issue de F04 (Parametrizar y versionar la política) · milestone «Ola 1 · Datos listos» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:12-normativa`, `RF-19` · tamaño propuesto M · depende de #40 (F04-H01), cerrada. La siguen F04-H03 (#42) y F02-H01 (#32).

**Requerimientos que cubre**: RF-19, en la parte de versionar («al cambiar un parámetro en una versión nueva, los periodos anteriores no se ven afectados»), y la recomendación de la ERS 8.3 para la carga 7 («además del archivo, pueda editarse en pantalla y quede versionada por periodo»). **Decisiones tomadas en esta historia:** D-67 (el Vicerrector académico escribe «Parámetros y normativa»), que se propone en el plan. **Decisiones que aplica** (`docs/decisiones.md`): D-65 y D-66 (permisos de «Parámetros y normativa»), ADR-021 (permisos), ADR-016 (catálogo de errores), ADR-009 y MD-13 (la carga 7 por archivo entra como una versión nueva). **Principios de la constitución**: VI (las cifras son datos versionados) y VII (inmutabilidad y trazabilidad).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero cambiar un parámetro de la política en una versión nueva, para aplicar un cambio de la norma sin alterar los periodos anteriores.

**Punto de partida.** F04-H01 dejó la versión 1 de la normativa: la Política SENESCYT 2024, con las 22 filas de la tabla de carga horaria, 7 reglas con 29 parámetros y el tipo de IST Tipo 2. Se puede consultar en la pantalla «Normativa», pero no se puede cambiar: si la norma cambia (por ejemplo, las horas de clase del docente de medio tiempo, C-03), hoy no hay forma de reflejarlo. Esta historia permite **crear una versión nueva a partir de otra, cambiarla mientras no rija ningún periodo y protegerla en cuanto un periodo la use**, de modo que un cambio de la norma nunca altere lo que ya se decidió con la versión anterior.

## Clarifications

### Session 2026-10-18

- Q: ¿Quién crea y edita versiones, si la historia la pide el Vicerrector académico y D-65 da la escritura solo al Administrador? → A: El Vicerrector académico y el Administrador del sistema. El Vicerrector gana la escritura de «Parámetros y normativa»; amplía D-66 y se propone como D-67 en el plan.
- Q: ¿Cómo se prueba «una versión asociada a un periodo», si los periodos llegan con F02-H01? → A: Normativa ofrece en esta historia la operación de asociar una versión a un periodo, que F02-H01 usará al abrir el periodo. El periodo se nombra con su código y Normativa no lo valida contra Académico.
- Q: ¿Qué se puede cambiar en una versión sin periodo? → A: Las horas de las filas existentes, los valores de los parámetros existentes, el origen, el código y la vigencia. Agregar o quitar filas, perfiles o reglas, y cambiar el tipo de IST, quedan para cuando una norma nueva lo pida.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Cambiar un parámetro en una versión nueva (Priority: P1)

Como Vicerrector académico, quiero crear una versión nueva a partir de la vigente y cambiar en ella un parámetro de la política, para aplicar un cambio de la norma sin tocar la versión que rige los periodos anteriores (RF-19).

**Why this priority**: Es el núcleo de RF-19: sin versiones nuevas, la normativa es fija y un cambio de la norma obligaría a cambiar el sistema.

**Independent Test**: Con la versión 1 asociada a un periodo, crear la versión 2 a partir de ella, cambiar las horas de clase del docente de medio tiempo y comprobar que la versión 2 tiene el cambio y la versión 1 sigue idéntica.

**Acceptance Scenarios**:

```gherkin
Escenario: Versión nueva
  Dado la versión 1 asociada a un periodo anterior
  Cuando cambio las horas de clase de un docente de medio tiempo en una versión nueva
  Entonces se crea la versión 2 con el cambio
  Y el periodo anterior sigue con la versión 1 sin cambios
```

1. **Given** la versión 1, **When** creo una versión nueva a partir de ella, **Then** se crea la versión 2 con una copia exacta de su tabla de carga horaria, sus reglas y sus parámetros, con un origen y una vigencia propios, y queda registrada con quién y cuándo la creó.
2. **Given** la versión 2 sin periodo, **When** cambio las horas de clase del docente de medio tiempo, **Then** la versión 2 tiene el valor nuevo y la versión 1 sigue con el anterior.
3. **Given** la versión 1 asociada a un periodo anterior, **When** se crea y se cambia la versión 2, **Then** ese periodo sigue con la versión 1, sin cambios.
4. **Given** un cambio que rompe una regla de la fila (por ejemplo, horas de clase fuera de su mínimo y su máximo, o una suma mayor que la dedicación), **When** lo guardo, **Then** se rechaza, se dice qué está mal y la versión no cambia.

---

### User Story 2 - Una versión en uso no se cambia (Priority: P1)

Como institución, quiero que una versión que ya rige un periodo no se pueda modificar, para que un periodo se valide siempre con las mismas reglas (principio VII).

**Why this priority**: Es la garantía de RF-19 y del modelo de dominio: una versión asociada a un periodo no cambia; cualquier ajuste crea una versión nueva.

**Independent Test**: Con una versión asociada a un periodo, intentar cambiar una de sus cifras, desde la pantalla o llamando directamente al servidor: se rechaza con un 409 que pide crear una versión nueva, y la versión no cambia.

**Acceptance Scenarios**:

```gherkin
Escenario: Versión en uso
  Dado una versión asociada a un periodo
  Cuando intento modificarla
  Entonces se rechaza con un error 409 que pide crear una versión nueva
```

1. **Given** la versión 1 asociada a un periodo, **When** intento cambiar una cifra, **Then** se rechaza con 409, un mensaje que pide crear una versión nueva, y la versión no cambia.
2. **Given** una versión asociada a un periodo, **When** la abro en la pantalla, **Then** se ve en solo lectura, con su periodo, y con la opción de crear una versión nueva a partir de ella.
3. **Given** una versión sin periodo, **When** la asocio al periodo `2025-1`, **Then** queda asociada y desde ese momento no se puede cambiar; **Given** el periodo `2025-1` ya asociado, **When** intento asociarle otra versión, **Then** se rechaza con 409.

---

### User Story 3 - Editar la tabla en pantalla (Priority: P1)

Como Vicerrector académico, quiero editar en pantalla la tabla de carga horaria de una versión nueva, para no depender de un archivo para un cambio pequeño (ERS 8.3, carga 7).

**Why this priority**: Es el tercer escenario del issue y la recomendación de la ERS: la carga 7 se puede editar en pantalla además de por archivo.

**Independent Test**: Con una versión nueva sin periodo, editar una fila de la tabla en la pantalla «Normativa» y comprobar que el cambio queda en esa versión con las mismas validaciones que tendría si viniera por archivo.

**Acceptance Scenarios**:

```gherkin
Escenario: Edición en pantalla
  Dado una versión nueva todavía sin periodo
  Cuando edito la tabla de carga horaria en pantalla
  Entonces el cambio queda en esa versión, igual que si viniera por archivo
```

1. **Given** una versión nueva sin periodo, **When** edito en pantalla una cifra de una fila de la tabla y guardo, **Then** el cambio queda en esa versión y se ve al volver a abrirla.
2. **Given** la misma edición, **When** se hace en pantalla o llega con la carga 7 por archivo (F01), **Then** pasa por las mismas reglas y deja el mismo resultado.
3. **Given** un valor inválido en la pantalla, **When** guardo, **Then** se ve el motivo junto al campo, en español, y no se guarda nada.

---

### Edge Cases

- **Crear a partir de qué versión**: una versión nueva se crea copiando otra versión existente (por defecto, la de número más alto). El número de la nueva es el siguiente disponible.
- **Dos versiones nuevas a la vez**: dos personas que crean una versión nueva al mismo tiempo obtienen números distintos; ninguna pisa a la otra.
- **Cambios simultáneos sobre la misma versión**: si dos personas editan la misma versión sin periodo, gana la última que guarda, y ambas ediciones quedan en el registro.
- **Versión 1**: sigue la misma regla que cualquier otra versión: si está asociada a un periodo, no se cambia. Para cambiar la política 2024 sin perderla, lo normal es crear la versión 2 a partir de ella.
- **Vigencia y origen**: la versión nueva lleva su propio origen (por ejemplo, «Reforma de la política 2025») y su fecha de vigencia; el código es único.
- **Severidades y afinidad**: cambiar la severidad de una regla o los pesos de afinidad no es de esta historia (F04-H03); en una versión nueva se copian tal cual.
- **Periodo ya asociado**: un periodo tiene una sola versión; asociarle otra se rechaza con 409, y cambiar la versión de un periodo ya abierto no es de esta historia.
- **Una versión, varios periodos**: una misma versión puede regir varios periodos; basta uno para que no se pueda cambiar.
- **Usuario sin permiso**: quien no tenga el permiso de escritura de «Parámetros y normativa» no ve las acciones de crear ni editar, y el servidor responde «Acceso no autorizado» (403) si las intenta.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Se DEBE poder crear una versión nueva de la normativa a partir de una versión existente, con una copia exacta de su tabla de carga horaria, sus reglas, sus parámetros y su tipo de IST, el siguiente número disponible, un código único, un origen y una fecha de vigencia propios.
- **FR-002**: Mientras una versión no esté asociada a ningún periodo, se DEBEN poder cambiar las horas de las filas existentes de su tabla de carga horaria, los valores de los parámetros existentes de sus reglas, su origen, su código y su vigencia. Agregar o quitar filas, perfiles o reglas, cambiar el tipo de IST y cambiar severidades no son de esta historia.
- **FR-003**: Cada cambio DEBE cumplir las reglas de la tabla de carga horaria (las mismas de la versión 1: horas no negativas, clase entre su mínimo y su máximo, la suma sin superar la dedicación, un perfil sin repetir para el mismo tipo de IST); un cambio que no las cumple se rechaza entero, con el motivo en español.
- **FR-004**: Una versión asociada a un periodo NO DEBE cambiar: cualquier intento responde 409 con un código del catálogo y un mensaje que pide crear una versión nueva (ADR-016).
- **FR-005**: Crear o cambiar una versión NO DEBE alterar ninguna otra versión ni los periodos asociados a ellas.
- **FR-006**: Normativa DEBE ofrecer la operación de asociar una versión a un periodo, nombrado por su código (la que F02-H01 usará al abrir el periodo), y la de consultar qué versión tiene un periodo. Un periodo tiene una sola versión: asociarle otra se rechaza con 409. Desde que una versión tiene un periodo, no cambia (FR-004).
- **FR-007**: La pantalla «Normativa» DEBE permitir, a quien tenga el permiso de escritura, crear una versión nueva a partir de la elegida y editar en pantalla la tabla de carga horaria y los parámetros de una versión sin periodo; una versión asociada a un periodo se ve en solo lectura.
- **FR-008**: Crear y cambiar versiones, y asociarlas a un periodo, DEBEN exigir el permiso de escritura de «Parámetros y normativa», validado en el servidor (RNF-SEG-03, ADR-021). Lo tienen el Administrador del sistema y el Vicerrector académico (D-65, ampliada por D-67).
- **FR-009**: Crear una versión y cada cambio sobre ella DEBEN quedar registrados con quién, cuándo y qué cambió (valor anterior y nuevo), para la bitácora (principio VII).
- **FR-010**: Una edición hecha en pantalla y la misma edición llegada por la carga 7 (F01) DEBEN pasar por las mismas reglas y dejar el mismo resultado: hay una sola forma de cambiar una versión.

### Key Entities

- **Versión de normativa**: conjunto numerado de parámetros de RN-01 a RN-26 que rige un periodo; no cambia una vez asociada a un periodo (glosario §7.3). Tiene número, código, origen, vigencia, tipo de IST, tabla de carga horaria y reglas, y sabe de qué versión se copió.
- **Asignación normativa**: el periodo, nombrado por su código, al que se asocia una versión; un periodo tiene una sola versión vigente (modelo de dominio §5.1, `AsignacionNormativa`). Una versión puede tener varias.
- **Tabla de carga horaria** y **Perfil**: como en F04-H01 (glosario §7.3).
- **Regla** y **Parámetro**: como en F04-H01; aquí se cambian sus valores.
- **Cambio de versión**: quién, cuándo, qué versión y qué valores cambiaron (para la bitácora).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El Vicerrector académico crea la versión 2 y cambia las horas de clase del docente de medio tiempo en menos de 2 minutos, sin ayuda.
- **SC-002**: Después de crear y cambiar una versión nueva, el 100 % de los valores de la versión anterior sigue idéntico, valor por valor.
- **SC-003**: El 100 % de los intentos de cambiar una versión asociada a un periodo se rechazan con 409, también llamando directamente al servidor, y la versión no cambia.
- **SC-004**: El 100 % de los cambios que rompen una regla de la tabla se rechazan con el motivo en español, y ninguno deja la versión a medias.
- **SC-005**: Cada versión creada y cada cambio aparecen en el registro con quién, cuándo y qué cambió.

## Assumptions

- La versión nueva se crea a partir de otra versión existente; no se crea una versión vacía.
- El código de un periodo es el que usará Académico (por ejemplo, `2025-1`); Normativa no comprueba que exista, porque los periodos llegan con F02-H01 y un servicio no lee las tablas de otro (principio V).
- No se bloquea la edición de una versión mientras alguien la edita: el prototipo tiene pocos editores, y cada cambio queda en el registro.
- Una versión sin periodo se puede cambiar varias veces; deja de cambiar cuando se asocia a un periodo (F02-H01 hace esa asociación al abrir el periodo).
- Cambiar severidades y pesos de afinidad es de F04-H03; la carga 7 por archivo es de F01 y usará la misma forma de cambiar una versión.
- No hay un estado «borrador» ni «publicada»: una versión sin periodo se puede elegir para un periodo y se puede cambiar; las dos cosas no se dan a la vez.
- Los datos son ficticios o públicos.

## Dependencies

- F04-H01 (#40), cerrada: la versión 1, la consulta y la pantalla «Normativa».
- F05-H02 (#44), cerrada: los permisos por funcionalidad (`PARAMETROS`, D-65, D-66, ADR-021).
- Modelo de errores común (ADR-016) y base de la SPA (ADR-019).
- F02-H01 (#32) dependerá de esta historia para asociar versiones a periodos.
