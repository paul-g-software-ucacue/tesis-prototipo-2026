# Feature Specification: Designar al Responsable de consolidación del periodo

**Feature Branch**: `016-responsable-consolidacion`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Especifica la historia F05-H03 · Designar al Responsable de consolidación del periodo (issue #45), a partir de D-13, ADR-003, RF-02 y lo que dejaron F05-H02 y F02-H01."

**Origen**: issue #45 · F05-H03 · sub-issue de F05 (Designar al Responsable de consolidación y aplicar los roles) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:02-roles`, `RF-02` · tamaño propuesto S · depende de #44 (F05-H02) y #32 (F02-H01), cerradas.

**Requerimientos que cubre**: D-13 y ADR-003 (el rol «Responsable de consolidación», que el Vicerrectorado asigna por periodo a un usuario, normalmente un docente con cargo de gestión; no es un cargo con horas propias), RF-02 (roles con permisos por funcionalidad) y la ERS §4, paso 1 de la preparación («el Vicerrectorado abre el periodo académico y designa al Responsable de consolidación»). **Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-62 (un cambio de permisos vale desde la próxima renovación de la sesión), D-63 (los 10 roles de D-16 son fijos), D-64 (alcance «solo lo propio»), D-69 (los periodos se escriben con «Distributivo del periodo»), ADR-021 (permisos en el token y en el contrato), ADR-016 (catálogo de errores), ADR-018 y D-49 (llamadas entre procesos con token de proceso). **Principios de la constitución**: V (límites entre servicios y procesos), VII (toda acción relevante deja registro) y IX (mínimo privilegio).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero designar por periodo al Responsable de consolidación, para que consolide y firme «Elaborado por» quien yo elija.

**Punto de partida.** F05-H02 dejó los 10 roles fijos de D-16, entre ellos «Responsable de consolidación» con sus permisos (leer el distributivo, la oferta y los proyectos; consolidar; firmar). Hoy ese rol se asigna como cualquier otro: de forma global, sin periodo, y solo lo puede hacer el Administrador del sistema, con «Usuarios y roles: escritura». F02-H01 dejó los periodos académicos con sus estados, que solo escribe el Vicerrector académico. Nada liga todavía el rol a un periodo, y el Vicerrector, que es quien lo designa según la ERS, no puede asignarlo. Esta historia permite que el Vicerrector designe, para cada periodo, al usuario que consolidará y firmará «Elaborado por», y que esa designación quede registrada.

## Clarifications

### Session 2026-10-21

- Q: ¿Con qué permiso designa el Vicerrector al Responsable de consolidación? → A: Con la escritura de «Distributivo del periodo», como los periodos (D-69): solo el Vicerrector académico. La matriz no cambia. El Administrador sigue asignando los demás roles en «Roles», pero no designa por periodo.
- Q: ¿Cómo se entiende «tiene ese rol solo en ese periodo»? → A: Hay un Responsable por periodo, que se reemplaza mientras el periodo no esté cerrado. El usuario recibe los permisos del rol mientras tenga una designación en un periodo no cerrado; las historias de consolidación y de firma limitan cada operación a los periodos que tiene designados, como «lo propio» (D-64).
- Q: ¿A quién se puede designar? → A: Solo a usuarios activos con el rol Docente (hoy la única marca de docente, hasta F03-H04), y nunca al propio Vicerrector que designa.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Designar al Responsable de consolidación de un periodo (Priority: P1)

Como Vicerrector académico, quiero designar a un usuario como Responsable de consolidación de un periodo, para que consolide el distributivo de ese periodo y firme «Elaborado por» (D-13, ADR-003).

**Why this priority**: Es el primer escenario del issue y el paso 1 de la preparación del periodo (ERS §4). Sin la designación, la etapa de consolidación (RF-32) y la firma F1 no tendrían quién las haga.

**Independent Test**: Con un periodo en Planificación, designar a un usuario, y comprobar que el periodo muestra su Responsable de consolidación, que el usuario recibe los permisos del rol y que la designación queda registrada.

**Acceptance Scenarios**:

```gherkin
Escenario: Designación
  Dado un periodo en Planificación y un usuario docente
  Cuando lo designo Responsable de consolidación
  Entonces el usuario tiene ese rol solo en ese periodo
  Y la designación queda en la bitácora
```

1. **Given** un periodo en Planificación sin Responsable de consolidación y un usuario que se puede designar, **When** el Vicerrector lo designa, **Then** el periodo queda con ese Responsable de consolidación y el usuario recibe los permisos del rol desde su próxima renovación de sesión (D-62). Las operaciones de consolidación y de firma que lleguen después solo le valdrán para ese periodo (D-64).
2. **Given** la designación hecha, **Then** queda registrado quién designó, a quién, para qué periodo y cuándo.
3. **Given** un usuario que no se puede designar, **When** el Vicerrector lo intenta, **Then** se rechaza con el motivo en español. Se puede designar solo a un usuario activo con el rol Docente, distinto de quien designa.
4. **Given** un periodo que no existe, **When** se intenta designar, **Then** se rechaza con «no encontrado».
5. **Given** un periodo con Responsable, no cerrado, **When** el Vicerrector designa a otro usuario, **Then** el nuevo reemplaza al anterior, que deja de tener los permisos del rol si no tiene otra designación en un periodo no cerrado.
6. **Given** que un periodo se cierra, **Then** su Responsable deja de tener los permisos del rol si no tiene otra designación en un periodo no cerrado; la designación se conserva como dato del periodo.

---

### User Story 2 - Quién designa (Priority: P1)

Como Vicerrector académico, quiero ser yo quien designa al Responsable de consolidación, sin pedírselo al Administrador del sistema, porque la ERS y ADR-003 dicen que lo designa el Vicerrectorado.

**Why this priority**: Hoy solo el Administrador asigna roles. Sin esta parte, la historia no se cumple tal como está escrita.

**Independent Test**: Designar con el Vicerrector académico funciona; con un usuario sin el permiso correspondiente, el servidor responde «Acceso no autorizado».

**Acceptance Scenarios**:

1. **Given** el Vicerrector académico, **When** designa al Responsable de consolidación de un periodo, **Then** la designación se acepta. El permiso es la escritura de «Distributivo del periodo», como para los periodos (D-69); el Administrador del sistema no designa.
2. **Given** un usuario sin ese permiso, **When** intenta designar, **Then** el servidor responde «Acceso no autorizado» (403) y no cambia nada.

---

### User Story 3 - Es un rol, no un cargo (Priority: P1)

Como Vicerrector académico, quiero que la designación no le sume horas al docente designado, porque el Responsable de consolidación es un rol del sistema y no un cargo con horas de gestión (ADR-003, RN-09).

**Why this priority**: Es el segundo escenario del issue: evita que el distributivo cuente horas de un cargo que la institución no tiene (C-22, RN-09).

**Independent Test**: La designación no se guarda como un cargo ni como una actividad del docente; solo como un rol en un periodo. El cálculo de horas, que llega con Distributivo (F07), la ignora.

**Acceptance Scenarios**:

```gherkin
Escenario: Es un rol, no un cargo
  Dado un docente designado Responsable de consolidación
  Cuando se calculan sus horas
  Entonces la designación no le suma horas de gestión
```

1. **Given** un docente designado, **Then** la designación no aparece entre sus cargos ni entre sus actividades, y no tiene horas.
2. **Given** el cálculo de horas de Distributivo (F07), **When** calcula las del docente designado, **Then** la designación no cuenta. F07 lo prueba con su propio cálculo y lo anota en sus dependencias.

---

### User Story 4 - Ver y designar en pantalla (Priority: P1)

Como Vicerrector académico, quiero ver y elegir al Responsable de consolidación de cada periodo en la pantalla «Periodos», junto con su estado y su versión de normativa, para hacer la preparación del periodo en un solo lugar.

**Why this priority**: La historia la pide el Vicerrector, que ya prepara el periodo en «Periodos» (F02-H01). Sin pantalla, la designación solo se podría hacer llamando al servidor.

**Independent Test**: En «Periodos», ver el Responsable de consolidación de cada periodo (o «Sin designar»), elegir uno de una lista y comprobar que queda; con un usuario sin escritura, verlo en solo lectura.

**Acceptance Scenarios**:

1. **Given** periodos con y sin Responsable de consolidación, **When** abro «Periodos», **Then** cada periodo muestra a su Responsable de consolidación o «Sin designar».
2. **Given** el permiso para designar, **When** elijo un usuario de la lista y confirmo, **Then** el periodo muestra al Responsable elegido.
3. **Given** un usuario sin el permiso para designar, **When** abre «Periodos», **Then** ve al Responsable de cada periodo, sin la acción de designar.

---

### Edge Cases

- **Periodo cerrado**: un periodo cerrado no admite cambios (RF-13, F02-H01): no se le designa ni se le cambia el Responsable.
- **El mismo usuario otra vez**: designar a quien ya es el Responsable del periodo no cambia nada ni deja un registro nuevo.
- **Varios periodos**: un mismo usuario puede ser Responsable de consolidación de varios periodos.
- **El Vicerrector no se designa a sí mismo**: la consolidación («Elaborado por») y la revisión («Revisado por») siguen en manos distintas (ADR-003, el árbol de firmas). Designarse a sí mismo se rechaza con el motivo.
- **Rol asignado a mano**: si el Administrador asignó el rol «Responsable de consolidación» de forma global con la pantalla «Roles» (F05-H02), esa asignación no es una designación de periodo y sigue valiendo, porque es una decisión del Administrador; la pantalla «Roles» no muestra las designaciones.
- **Periodo que se cierra**: al cerrarse, su Responsable conserva la designación como dato, pero deja de tener los permisos del rol si no tiene otra designación en un periodo no cerrado.
- **Periodo que todavía no llegó a Roles**: Roles conoce los periodos por los eventos de Académico. Si un periodo recién registrado todavía no llegó, designar responde «no encontrado» y la pantalla pide intentarlo en unos segundos. Si el proceso `datos` está caído, Roles sigue designando con la copia que ya tiene.
- **Sesión abierta del designado**: los permisos del rol le llegan desde su próxima renovación de sesión, como cualquier cambio de permisos (D-62).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir designar, para un periodo académico, a un usuario como Responsable de consolidación, y guardar la designación con el periodo, el usuario, quién designó y cuándo.
- **FR-002**: Cada periodo DEBE tener como mucho un Responsable de consolidación, que se reemplaza mientras el periodo no esté cerrado. Un usuario DEBE tener los permisos del rol «Responsable de consolidación» de la matriz D-16 mientras tenga una designación en un periodo no cerrado, y dejar de tenerlos cuando no tenga ninguna, desde su próxima renovación de sesión (D-62). Limitar cada operación de consolidación y de firma a sus periodos es de esas historias (D-64).
- **FR-003**: Designar DEBE exigir la escritura de «Distributivo del periodo», validada en el servidor (ADR-021); consultar las designaciones, su lectura. La matriz D-65 no cambia.
- **FR-004**: Solo se DEBE poder designar a un usuario con el rol Docente, y nunca a quien designa. Un usuario que no lo cumpla se rechaza con el motivo en español. Que esté activo no se comprueba al designar: un usuario inactivo no inicia sesión, así que la designación no le da nada (plan, research §2).
- **FR-005**: Un periodo cerrado NO DEBE admitir una designación ni un cambio de designación; un periodo que no existe se rechaza con «no encontrado».
- **FR-006**: Designar a quien ya es el Responsable del periodo NO DEBE cambiar nada ni dejar un registro nuevo.
- **FR-007**: La designación NO DEBE guardarse como un cargo ni como una actividad del docente, ni sumarle horas (ADR-003).
- **FR-008**: Cada designación y cada cambio de designación DEBE quedar registrado con quién, cuándo, el periodo, el Responsable anterior y el nuevo (principio VII).
- **FR-009**: El sistema DEBE ofrecer la consulta del Responsable de consolidación de un periodo, para que las historias de consolidación y de firma (RF-32, F1) sepan quién es.
- **FR-010**: La pantalla «Periodos» DEBE mostrar el Responsable de consolidación de cada periodo y permitir designarlo, a quien tenga el permiso, eligiendo de una lista de usuarios que se pueden designar.

### Key Entities

- **Responsable de consolidación**: rol que el Vicerrectorado designa por periodo para consolidar y firmar F1; es un rol, no un cargo, y no tiene horas propias (glosario §7.3, `RESPONSABLE_CONSOLIDACION`, Identidad y acceso).
- **Designación**: la relación entre un periodo académico y el usuario que es su Responsable de consolidación, con quién la hizo y cuándo.
- **Periodo académico**: como en F02-H01; su estado decide si se puede designar.
- **Usuario** y **Rol**: como en F05-H01 y F05-H02.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El Vicerrector académico designa al Responsable de consolidación de un periodo en menos de 1 minuto, sin ayuda.
- **SC-002**: Después de designarlo y de su próxima renovación de sesión, el usuario designado ve las pantallas y las acciones que el rol le permite, y antes de la designación no las veía.
- **SC-003**: El 100 % de los intentos de designar en un periodo cerrado, a un usuario que no se puede designar, o sin el permiso, se rechazan, también llamando directamente al servidor.
- **SC-004**: Cada designación aparece en el registro con quién, cuándo, el periodo, el Responsable anterior y el nuevo.

## Assumptions

- La designación es de Identidad y acceso (servicio Roles, glosario §7.3), y el periodo es de Académico: Roles conoce los periodos por los eventos que publica Académico y guarda una copia con su estado, sin leer sus tablas (principio V, ADR-018).
- El designado normalmente es un docente con cargo de gestión (ADR-003), pero el sistema no exige el cargo: los cargos llegan con F03-H05.
- La etapa de consolidación (RF-32), el informe sustentado y la firma F1 son de otras historias (F09, F10); esta historia solo deja quién es el Responsable de cada periodo y sus permisos.
- El cálculo de horas es de Distributivo (F06, F07); esta historia garantiza que la designación no es un cargo ni una actividad, y F07 comprueba que no suma horas.
- Las tres decisiones de Clarifications las fijó el autor el 2026-10-21.
- Los datos son ficticios.

## Dependencies

- F05-H02 (#44), cerrada: los roles fijos de D-16, la asignación de roles, el registro de cambios y los permisos en el token.
- F02-H01 (#32), cerrada: los periodos académicos con sus estados y la pantalla «Periodos».
- F05-H01 (#43), cerrada: los usuarios y la sesión.
- F09 (consolidación), F10 (firma F1) y F07 (horas) dependerán de esta historia para saber quién es el Responsable de cada periodo.
