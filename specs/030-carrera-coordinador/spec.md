# Feature Specification: Indicar la carrera del coordinador

**Feature Branch**: `030-carrera-coordinador`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Especifica la historia F06-H05 · Indicar la carrera del coordinador (issue #166), a partir de D-16, D-64, RF-02 y RF-03, con las tres aclaraciones del autor del 2026-10-31."

**Origen**: issue #166 · F06-H05 · sub-issue de F06 (Proponer un docente por asignatura con sugerencias por afinidad, #8) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:02-roles` · tamaño propuesto M · depende de #44 (F05-H02) y #47 (F06-H02), cerradas · la necesita #48 (F06-H03). Salió de la especificación de F06-H03, por decisión del autor.

**Requerimientos que cubre**:
- **D-16:** el Coordinador de carrera trabaja con su carrera: la matriz le da «Oferta y propuesta de docentes» y «Distributivo del periodo» solo sobre lo propio.
- **D-64:** cada permiso tiene un alcance, y cada historia que trae los datos aplica el filtro de «solo lo propio».
- **RF-02 y RF-03:** los roles, sus permisos y sus alcances; con varios roles, vale el alcance más amplio.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-16, D-62 (los cambios valen desde la próxima renovación de la sesión), D-64, D-65 (la matriz), D-70 (permisos alternativos), D-72 (validación y catálogo de errores), D-74 (mensajes en «tú»), ADR-018 y ADR-023 (entre procesos). **Principios de la constitución 2.2.0**: III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), IX (mínimo privilegio y datos personales), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Administrador del sistema, quiero indicar la carrera de cada Coordinador de carrera, para que cada coordinador vea y prepare solo lo de su carrera.

**Punto de partida.** El sistema sabe qué permisos de un usuario son «solo lo propio», pero no sabe qué es lo propio de un coordinador: un usuario no está ligado a ninguna carrera. Por eso hoy cualquier coordinador lee la oferta de todas las carreras y puede pedir la sugerencia de cualquiera. F06-H03 va a guardar propuestas, y ahí eso ya no se puede dejar abierto.

Esta historia agrega:
- **La carrera del coordinador,** que el Administrador indica en «Roles de usuarios».
- **El filtro de «solo lo propio»** en la oferta, en la sugerencia y en la afinidad de la ficha del docente.
- **Un aviso** para el coordinador que todavía no tiene carrera.

## Clarifications

### Session 2026-10-31

- Q: ¿Cuántas carreras puede tener un coordinador? → A: Una sola.
- Q: ¿Qué ve un coordinador sin carrera indicada? → A: Ni oferta ni sugerencias, y un aviso le dice que pida que indiquen su carrera. Un dato sin configurar no abre todo.
- Q: ¿También se filtra «Afinidad por asignatura» en la ficha del docente? → A: Sí. El coordinador ve el puntaje solo en las asignaturas de su carrera.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Indicar la carrera (Priority: P1)

Como Administrador del sistema, quiero indicar la carrera de un coordinador, para que el sistema sepa qué es lo suyo.

**Why this priority**: Es el primer escenario del issue. Sin este dato no hay filtro.

**Independent Test**: Indicar una carrera a un coordinador, volver a abrir la pantalla y verla; iniciar sesión como ese coordinador y comprobar que trabaja con ella.

**Acceptance Scenarios**:

```gherkin
Escenario: Indicar la carrera del coordinador
  Dado un usuario con el rol Coordinador de carrera
  Cuando le indico su carrera en «Roles de usuarios»
  Entonces esa carrera queda registrada y, en su próxima sesión, el coordinador trabaja con ella
```

1. **Given** un usuario con el rol Coordinador de carrera, **When** el Administrador elige su carrera de la lista y guarda, **Then** la pantalla la muestra junto a sus roles.
2. **Given** un coordinador con carrera, **When** el Administrador elige otra, **Then** queda la nueva: un coordinador tiene una sola.
3. **Given** un usuario sin el rol Coordinador de carrera, **When** se le intenta indicar una carrera, **Then** el sistema lo rechaza con un error del catálogo, y la pantalla no lo ofrece.
4. **Given** un coordinador con carrera, **When** el Administrador le quita ese rol, **Then** su carrera se borra.
5. **Given** la lista de carreras de la pantalla, **When** el Administrador elige, **Then** solo ve las carreras vigentes. Un código con un formato inválido se rechaza con un error del catálogo.
6. **Given** un usuario sin «Usuarios y roles: escritura», **When** intenta indicar una carrera, **Then** recibe «no autorizado».
7. **Given** una carrera indicada, **When** se guarda, **Then** el cambio queda registrado, con quién y cuándo.

---

### User Story 2 - Trabajar solo con su carrera (Priority: P1)

Como Coordinador de carrera, quiero ver solo lo de mi carrera, para no confundirme ni tocar lo de otra (D-16, D-64).

**Why this priority**: Son los escenarios segundo y tercero del issue: la razón de la historia.

**Independent Test**: Con dos carreras en la oferta y un coordinador de una de ellas, abrir la oferta, pedir una sugerencia de cada carrera y abrir la afinidad de un docente.

**Acceptance Scenarios**:

```gherkin
Escenario: Solo la oferta de su carrera
  Dado un coordinador con su carrera indicada
  Cuando abre la oferta de un periodo
  Entonces ve solo las asignaturas-paralelo de su carrera

Escenario: Sugerencia de otra carrera
  Dado un coordinador con su carrera indicada
  Cuando pide la sugerencia de una asignatura-paralelo de otra carrera
  Entonces el sistema la rechaza
```

1. **Given** un coordinador con su carrera, **When** abre la oferta, **Then** ve solo esa carrera, y las horas a cubrir son las de su carrera, no las del periodo.
2. **Given** un coordinador con su carrera, **When** pide la sugerencia de un paralelo de su carrera, **Then** la recibe, también con candidatos de otra carrera base.
3. **Given** un coordinador con su carrera, **When** pide la sugerencia de un paralelo de otra carrera, **Then** recibe «no autorizado» del catálogo, sin ningún dato de ese paralelo.
4. **Given** un coordinador con su carrera, **When** abre la ficha de un docente y elige un periodo, **Then** ve el puntaje solo en las asignaturas de su carrera.
5. **Given** un periodo sin oferta en su carrera, **When** la abre, **Then** el sistema dice que no hay oferta para mostrar en ese periodo.

---

### User Story 3 - Sin carrera no se ve nada (Priority: P1)

Como Administrador del sistema, quiero que un coordinador sin carrera no vea datos de otras, para que un olvido no abra todo.

**Why this priority**: Es una regla de seguridad que decidió el autor.

**Independent Test**: Con un coordinador sin carrera, abrir la oferta y la ficha de un docente.

**Acceptance Scenarios**:

1. **Given** un coordinador sin carrera indicada, **When** abre la oferta, **Then** no ve ninguna asignatura-paralelo, y un aviso le dice que pida que indiquen su carrera.
2. **Given** un coordinador sin carrera indicada, **When** pide una sugerencia, **Then** recibe «no autorizado» del catálogo.
3. **Given** un coordinador sin carrera indicada, **When** abre la ficha de un docente y elige un periodo, **Then** no ve puntajes, y el mismo aviso se lo dice.

---

### User Story 4 - Quien alcanza todo sigue viendo todo (Priority: P1)

Como Vicerrector académico o Responsable de consolidación, quiero seguir viendo todas las carreras, porque mi permiso alcanza todos los datos.

**Why this priority**: Es el cuarto escenario del issue. Evita romper lo que ya funciona.

**Independent Test**: Con un usuario con alcance total, abrir la oferta y pedir una sugerencia de cualquier carrera.

**Acceptance Scenarios**:

```gherkin
Escenario: Permiso sobre todo
  Dado un usuario cuyo permiso alcanza todos los datos
  Cuando abre la oferta de un periodo
  Entonces ve todas las carreras, sin necesitar una carrera propia
```

1. **Given** un usuario con alcance total, **When** abre la oferta o pide una sugerencia, **Then** ve todas las carreras.
2. **Given** un usuario con el rol Coordinador de carrera y otro rol con alcance total sobre el mismo dato, **When** abre la oferta, **Then** ve todas las carreras: vale el alcance más amplio (RF-03).

---

### Edge Cases

- **Le cambian la carrera con la sesión abierta:** vale desde la próxima renovación de la sesión, como los roles (D-62).
- **Su carrera deja de estar vigente:** la carrera indicada se conserva, y sigue viendo lo suyo.
- **Dos coordinadores con la misma carrera:** el sistema lo permite. La regla de un solo coordinador vigente por carrera es del cargo, en Docentes.
- **Un código con buen formato que no es de ninguna carrera:** se guarda, pero no abre nada: el coordinador ve su carrera sin oferta. Solo se puede llegar ahí llamando a la API a mano, con permiso de Administrador (D-76).
- **Una sugerencia con el código de otra carrera mal escrito:** 400, como cualquier código inválido.
- **Una asignatura-paralelo o un periodo que no existen:** el coordinador recibe el mismo error que hasta ahora.

## Requirements *(mandatory)*

### Functional Requirements

**La carrera del coordinador**

- **FR-001**: Quien tiene «Usuarios y roles: escritura» MUST poder indicar, cambiar y quitar la carrera de un usuario con el rol Coordinador de carrera.
- **FR-002**: Un coordinador MUST tener como mucho una carrera.
- **FR-003**: Solo un usuario con el rol Coordinador de carrera MUST poder tener carrera. Al quitarle ese rol, su carrera MUST borrarse.
- **FR-004**: La pantalla MUST ofrecer solo las carreras vigentes de Académico. El servidor no consulta a Académico al guardar (D-76).
- **FR-005**: El código de la carrera MUST validarse por tipo, caracteres y largo, en el servidor; en la pantalla se elige de una lista (principio XIV).
- **FR-006**: Cada cambio MUST quedar registrado, con quién y cuándo.
- **FR-007**: El cambio MUST valer desde la próxima renovación de la sesión del coordinador (D-62).
- **FR-008**: «Roles de usuarios» MUST mostrar la carrera de cada coordinador, y ofrecer elegirla a quien puede cambiarla.

**El filtro**

- **FR-009**: Un usuario cuyo permiso sobre la oferta es solo «lo propio» MUST ver solo la oferta de su carrera, con las horas a cubrir de esa carrera.
- **FR-010**: Ese usuario MUST NOT recibir la sugerencia de una asignatura-paralelo de otra carrera: responde un error de «no autorizado» del catálogo.
- **FR-011**: Ese usuario MUST ver la afinidad de un docente solo en las asignaturas de su carrera.
- **FR-012**: Sin carrera indicada, ese usuario MUST NOT ver oferta, sugerencias ni puntajes, y la pantalla MUST decir que pida que indiquen su carrera.
- **FR-013**: Quien tiene ese permiso con alcance total MUST ver todas las carreras, sin carrera propia. Si tiene los dos alcances, MUST valer el más amplio (RF-03).
- **FR-014**: El filtro MUST aplicarse en el servidor: la pantalla solo muestra lo que recibe.

**Transversales**

- **FR-015**: Los candidatos de una sugerencia MUST NOT filtrarse por carrera: un docente de otra carrera base puede aparecer (D-01).
- **FR-016**: Si un servicio del que depende la sugerencia o la afinidad no responde, el sistema MUST seguir respondiendo el error del catálogo de F06-H01.
- **FR-017**: Todo error nuevo MUST salir del catálogo, con sus 8 columnas, en «tú», sin detalles técnicos (D-72, D-74).

### Key Entities

- **Carrera del coordinador:** la carrera que el Administrador indica a un usuario con el rol Coordinador de carrera. Una por coordinador. Término nuevo del glosario.
- **Alcance** y **Permisos efectivos:** ya existen; esta historia los aplica por primera vez a un dato.
- **Oferta, sugerencia y afinidad:** ya existen; se filtran por carrera.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En los casos de prueba con dos o más carreras, un coordinador no recibe en el 100 % de sus consultas ningún dato de oferta, sugerencia ni afinidad de otra carrera.
- **SC-002**: Un coordinador sin carrera no recibe ningún dato de oferta, sugerencia ni afinidad, y siempre ve qué hacer.
- **SC-003**: Los usuarios con alcance total ven lo mismo que antes de esta historia, en todas las pruebas existentes.
- **SC-004**: Indicar la carrera de un coordinador toma menos de 1 minuto (con holgura).
- **SC-005**: Abrir la oferta filtrada tarda menos de 3 segundos con los 165 paralelos de prueba (con holgura).

## Assumptions

- La indica quien tiene «Usuarios y roles: escritura»: hoy, el Administrador del sistema. En la demo, `admin.demo`.
- El filtro solo se aplica a quien tiene un permiso sobre la oferta y lo tiene solo con alcance «lo propio». Quien no tiene ningún permiso sobre la oferta, como Talento Humano, sigue viendo la afinidad como hoy: no es un coordinador.
- En la demo, `coordinacion.demo` deja de ver la oferta hasta que `admin.demo` le indique su carrera. Es un cambio esperado.
- Los permisos alternativos de la oferta se miran juntos (D-70): con cualquiera de ellos con alcance total, el usuario ve todo.
- El filtro cubre lo que el coordinador prepara: oferta, sugerencia y afinidad. Los catálogos (carreras, mallas y estructura) se siguen leyendo completos, porque no son la preparación de otra carrera.
- No se liga el usuario con el docente ni con su cargo: la carrera del coordinador es un dato de Roles.
- Los datos de prueba son ficticios (repositorio público).

## Out of Scope

- Ligar el usuario con el docente o con su cargo.
- Varias carreras por coordinador.
- Filtrar los catálogos.
- Proponer un docente (F06-H03).
- Gestionar usuarios.
