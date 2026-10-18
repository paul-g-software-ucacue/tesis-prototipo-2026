# Feature Specification: Roles con permisos y varios roles por usuario

**Feature Branch**: `011-roles-permisos`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "Especifica la historia F05-H02 · Roles con permisos y varios roles por usuario (issue #44) con sus escenarios, a partir del ERS (RF-02, RF-03, RNF-SEG-03 y la matriz de permisos D-16) y de lo que ya dejó F05-H01."

**Origen**: issue #44 · F05-H02 · sub-issue de #7 (F05 · Designar al Responsable de consolidación y aplicar los roles) · milestone «Ola 1 · Datos listos» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:02-roles`, `RF-02`, `RF-03` · tamaño propuesto L · depende de #43 (F05-H01), cerrada.

**Requerimientos que cubre**: RF-02 · RF-03 · RNF-SEG-03 (ERS, `requerimientos_mdd/ERS_MDD_Requerimientos.md`) y la matriz de permisos por rol de D-16 (ERS, sección 3). **Decisiones tomadas en esta historia:** D-62, D-63 y D-64. **Decisiones que aplica** (`docs/decisiones.md`): D-16 (matriz de permisos), D-59 (usuarios de la demo), ADR-014 (token con los roles), ADR-016 (catálogo de errores), ADR-019 (base del frontend) y ADR-020 (renovación del token).

**Historia (tal como está en el issue)**: Como administrador del sistema, quiero crear roles con permisos de lectura, escritura, aprobación y firma, y asignar varios a un usuario, para que cada persona actúe solo según la matriz de permisos.

**Punto de partida.** F05-H01 dejó 11 usuarios de la demo, uno por cada rol de D-16 y uno con dos roles. Cada usuario entra con sus roles en la sesión y la pantalla de inicio los muestra en español. Pero hoy **los roles no dan ni quitan nada**: son una etiqueta fija en el código, ningún rol tiene permisos y cualquier usuario con sesión puede usar cualquier operación. Además, un acceso denegado responde sin cuerpo. Esta historia convierte la matriz D-16 en permisos que el sistema **aplica en el servidor**, deja que el administrador cree roles nuevos y los asigne (uno o varios por usuario), y da al acceso denegado el mensaje del modelo de errores común.

> **Alcance respecto de F05-H03.** Designar al Responsable de consolidación **de un periodo** es de F05-H03 (#45). Esta historia trata los roles como permanentes del usuario; la designación por periodo se apoyará en ellos.

## Clarifications

### Session 2026-10-17

- Q: Si el administrador le quita un rol o un permiso a un usuario con la sesión abierta, ¿cuándo lo pierde? → A: En la próxima renovación de su sesión, como máximo 30 minutos después (ADR-020), o al volver a entrar; los procesos siguen validando sin consultar a Roles en cada petición (D-62).
- Q: ¿El administrador puede cambiar los 10 roles de D-16? → A: No: son fijos, se ven y se asignan, pero no se editan ni se borran; para otra combinación se crea un rol nuevo (D-63).
- Q: ¿Qué hace esta historia con «lo propio» (carrera, unidad o persona)? → A: Registra y muestra qué permisos de cada rol son «solo lo propio»; el filtro de datos lo aplica cada historia que trae carreras, unidades o docentes (D-64).

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus tres escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - El administrador crea un rol y se lo asigna a un usuario (Priority: P1)

Como administrador del sistema, quiero crear un rol eligiendo, para cada funcionalidad del MDD, si da lectura, escritura, aprobación o firma, y asignárselo a un usuario, para adaptar los permisos a lo que la institución decida sin cambiar el sistema (RF-02).

**Why this priority**: Es el núcleo de RF-02: sin roles con permisos, ninguna de las pantallas de las olas siguientes puede restringir quién hace qué.

**Independent Test**: Con el usuario administrador de la demo, crear un rol nuevo con algunos permisos, asignárselo a un usuario de la demo y comprobar que, al volver a entrar, ese usuario tiene exactamente esos permisos además de los de sus otros roles.

**Acceptance Scenarios**:

```gherkin
Escenario: Rol nuevo
  Dado los permisos de la matriz D-16
  Cuando creo un rol y se lo asigno a un usuario
  Entonces el rol queda guardado y el usuario tiene sus permisos
```

1. **Given** el administrador con sesión, **When** crea el rol «Auditor de cargas» con lectura de Cargas y de Docentes, **Then** el rol aparece en la lista de roles con esos dos permisos y nada más.
2. **Given** el rol «Auditor de cargas», **When** el administrador se lo asigna a `docente.demo`, **Then** en su próxima sesión `docente.demo` tiene los permisos de Docente y los de «Auditor de cargas».
3. **Given** un rol sin ningún permiso, **When** el administrador intenta guardarlo, **Then** el sistema lo rechaza y dice que el rol necesita al menos un permiso.
4. **Given** un rol con el mismo nombre que otro existente, **When** el administrador intenta guardarlo, **Then** el sistema lo rechaza y dice que el nombre ya existe.

---

### User Story 2 - Un usuario no puede hacer lo que su rol no permite (Priority: P1)

Como institución, quiero que un usuario que intenta hacer algo que sus roles no permiten reciba «Acceso no autorizado», aunque llame directamente al servidor, para aplicar el mínimo privilegio (RF-02, RNF-SEG-03).

**Why this priority**: Es el criterio de aceptación de RF-02 y el requisito de seguridad RNF-SEG-03: el permiso se valida en el servidor, no solo ocultando botones.

**Independent Test**: Con un usuario cuyo rol solo tiene lectura de una funcionalidad, intentar una operación de escritura de esa funcionalidad, tanto desde la pantalla como llamando directamente al servidor: en los dos casos, «Acceso no autorizado» con estado 403 y nada cambia.

**Acceptance Scenarios**:

```gherkin
Escenario: Solo lectura
  Dado un usuario con un rol de solo lectura
  Cuando intenta editar
  Entonces recibe "Acceso no autorizado" con estado 403
```

1. **Given** un usuario cuyo único rol tiene solo lectura de «Usuarios y roles», **When** ve la lista de roles, **Then** la ve, pero sin las acciones de crear, editar ni asignar.
2. **Given** ese mismo usuario, **When** llama directamente al servidor para crear un rol, **Then** recibe 403 con el título «Acceso no autorizado» y una descripción en español, y el rol no se crea.
3. **Given** un usuario sin ningún permiso sobre «Usuarios y roles» (por ejemplo, `docente.demo`), **When** abre la dirección de la pantalla de roles, **Then** ve «Acceso no autorizado» en lugar de la pantalla, sin datos de los roles.

---

### User Story 3 - Un usuario con varios roles tiene las funciones de todos (Priority: P1)

Como docente que también coordina una carrera, quiero ver y usar las funciones de mis dos roles en una sola sesión, para no necesitar dos usuarios (RF-03).

**Why this priority**: Es el criterio de aceptación de RF-03 y un caso real de la institución (DT HU-003, escenario 3).

**Independent Test**: Iniciar sesión con `docente.coordinador.demo` y comprobar que tiene la unión de los permisos de Docente y de Coordinador de carrera, y que la pantalla de inicio muestra las funciones de los dos roles.

**Acceptance Scenarios**:

```gherkin
Escenario: Varios roles
  Dado un usuario con los roles Docente y Coordinador de carrera
  Cuando inicia sesión
  Entonces ve las funciones de los dos roles
```

1. **Given** `docente.coordinador.demo`, **When** inicia sesión, **Then** la pantalla de inicio muestra sus funciones: las de Docente y las de Coordinador de carrera, sin repetir ninguna.
2. **Given** dos roles que dan permisos distintos sobre la misma funcionalidad (por ejemplo, lectura en uno y escritura en otro), **When** el usuario tiene los dos, **Then** tiene los dos permisos.
3. **Given** el administrador que le quita a un usuario uno de sus dos roles, **When** el usuario vuelve a entrar, **Then** solo tiene los permisos del rol que le queda.

---

### Edge Cases

- **Cambio de permisos con la sesión abierta**: un usuario con sesión al que el administrador le quita un rol o un permiso lo conserva hasta la próxima renovación de su sesión (como máximo 30 minutos, ADR-020) o hasta que vuelva a entrar; desde entonces ya no lo tiene (D-62). Lo mismo al agregarle un rol: lo recibe en la próxima renovación.
- **Quedarse sin administrador**: el administrador no puede, ni al cambiar los roles de un usuario ni al editar un rol, quitarse a sí mismo el permiso de escritura sobre «Usuarios y roles» ni dejar el sistema sin ningún usuario que lo tenga por alguno de sus roles; el sistema lo impide y explica por qué.
- **Rol asignado que se borra**: un rol creado por el administrador que todavía está asignado no se puede borrar; la pantalla dice a cuántos usuarios está asignado.
- **Roles de D-16**: los 10 roles de la matriz D-16 existen desde el principio con sus permisos; se ven y se asignan, pero no se editan ni se borran. Si el administrador intenta cambiarlos, el sistema lo impide y sugiere crear un rol nuevo (D-63).
- **«Propio»**: D-16 dice que varios roles solo ven o escriben «lo propio» (su carrera, unidad o persona), pero todavía no existen carreras, unidades ni docentes en el MDD. Cada permiso registra su alcance, **todo** o **solo lo propio**, y la pantalla lo muestra; el filtro de datos lo aplica cada historia que trae esos datos (D-64). Si dos roles dan el mismo permiso con alcances distintos, vale el más amplio.
- **Usuario sin roles**: un usuario al que se le quitan todos los roles puede entrar, pero no tiene ninguna función; la pantalla de inicio lo dice.
- **Usuarios de la demo al volver a arrancar**: las asignaciones de roles que hizo el administrador no se pierden cuando el sistema se reinicia o se despliega una versión nueva; el archivo de la demo (D-59) solo da los roles iniciales.
- **Acceso denegado en cualquier proceso**: el 403 tiene el mismo cuerpo del modelo de errores común en todos los procesos y en el API Gateway, sin decir qué permiso faltó.
- **Firma**: el permiso de firma existe en el modelo y en la matriz, pero firmar es de historias posteriores (F11); aquí solo se guarda y se muestra.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE tener un catálogo de **funcionalidades** del MDD, tomado de la matriz D-16 y de las features del backlog (por ejemplo: Usuarios y roles, Parámetros y normativa, Cargas, Docentes, Oferta y propuesta, Proyectos, Consolidación, Distributivo, Reportes, Bitácora, Resolución del OCS y Publicación), cada una con los tipos de permiso que tienen sentido para ella entre **lectura, escritura, aprobación y firma**.
- **FR-002**: Un **rol** DEBE tener un nombre único (dos nombres que solo cambian en mayúsculas, tildes o signos son el mismo), una descripción y al menos un permiso; cada permiso es una funcionalidad, un tipo de permiso y un alcance (**todo** o **solo lo propio**, D-64).
- **FR-003**: Los **10 roles de D-16** DEBEN existir desde el principio, con los permisos y alcances de la matriz, y no se pueden editar ni borrar (D-63); los 11 usuarios de la demo DEBEN tener sus roles de F05-H01.
- **FR-004**: El administrador DEBE poder crear roles, verlos todos, editar y borrar los que creó, y asignar y quitar cualquier rol a los usuarios, con las reglas de los casos límite (sin roles vacíos ni nombres repetidos, sin borrar un rol asignado, sin tocar los de D-16 y sin quedarse sin administrador).
- **FR-005**: Un usuario DEBE poder tener **varios roles**; sus permisos son la **unión** de los permisos de todos sus roles, y ante el mismo permiso con alcances distintos vale el más amplio (RF-03).
- **FR-006**: Toda operación del servidor que cambia, aprueba o firma algo, y toda consulta de datos restringidos, DEBE exigir el permiso correspondiente y validarlo **en el servidor** (RNF-SEG-03); sin ese permiso, responde 403.
- **FR-007**: El 403 DEBE tener un código del catálogo de errores y el título «Acceso no autorizado», con una descripción en español, igual en todos los procesos y en el API Gateway, sin decir qué permiso faltó (ADR-016).
- **FR-008**: La pantalla de inicio DEBE mostrar las funciones que el usuario puede usar según la unión de sus permisos; la SPA DEBE ocultar o desactivar las acciones que el usuario no puede hacer y mostrar «Acceso no autorizado» si abre una pantalla sin permiso.
- **FR-009**: Las asignaciones de roles hechas por el administrador DEBEN conservarse al reiniciar o al desplegar; los usuarios de la demo solo reciben sus roles iniciales cuando se crean.
- **FR-010**: Un cambio en los roles de un usuario, o en los permisos de un rol, DEBE valer para sus sesiones abiertas desde la próxima renovación (como máximo 30 minutos, ADR-020) o al volver a entrar (D-62).
- **FR-011**: Cada creación, cambio o borrado de un rol, y cada asignación o retiro de un rol a un usuario, DEBE quedar registrado con quién lo hizo y cuándo, para la bitácora (RF de bitácora, en historias posteriores).

### Key Entities

- **Funcionalidad**: parte del MDD sobre la que se dan permisos (por ejemplo, Cargas); tiene un nombre en español y los tipos de permiso que admite.
- **Tipo de permiso**: lectura, escritura, aprobación (incluye devolver) o firma (RF-02).
- **Permiso**: una funcionalidad con un tipo de permiso y un alcance (todo o solo lo propio).
- **Rol**: nombre único, descripción y un conjunto de permisos; puede ser uno de los 10 de D-16 (fijo) o uno creado por el administrador (editable).
- **Asignación de rol**: un usuario con un rol; un usuario puede tener varias. No es la «asignación» de horas del distributivo (glosario, §8).
- **Permisos efectivos**: la unión de los permisos de todos los roles de un usuario.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El administrador crea un rol con 3 permisos y se lo asigna a un usuario en menos de 2 minutos, sin ayuda.
- **SC-002**: Para cada uno de los 10 roles de D-16, un usuario con ese rol tiene exactamente los permisos de su fila de la matriz: ni uno más, ni uno menos.
- **SC-003**: El 100 % de las operaciones del servidor que cambian datos rechaza con 403 a un usuario sin el permiso, aunque llame directamente sin pasar por la pantalla; lo comprueba una prueba de autorización por cada operación (RNF-SEG-03).
- **SC-004**: `docente.coordinador.demo` ve en la pantalla de inicio las funciones de Docente y de Coordinador de carrera, sin repetidas.
- **SC-005**: Un acceso denegado muestra «Acceso no autorizado» en español en el 100 % de los casos, sin códigos internos, estados HTTP ni el nombre del permiso que faltó.
- **SC-006**: Después de reiniciar el sistema o desplegar una versión nueva, el 100 % de las asignaciones de roles y de los roles creados por el administrador siguen iguales.

## Assumptions

- El administrador es el usuario de la demo `admin.demo` (rol Administrador del sistema), que en D-16 escribe usuarios y roles. Crear o desactivar usuarios no es de esta historia: solo se asignan roles a los usuarios que ya existen.
- El catálogo de funcionalidades es fijo en esta historia (lo define el sistema, no el administrador); las historias siguientes agregan las suyas cuando traen pantallas nuevas, con los permisos de D-16 para los 10 roles.
- Como todavía no hay pantallas de F01 a F04, la protección se demuestra con las operaciones que existen: las de roles y asignaciones de roles de esta historia, y las ya existentes de las sesiones; cada historia siguiente protege las suyas con este mismo mecanismo.
- La operación de prueba de eventos de Generación (D-48) queda fuera de FR-006 y SC-003: está apagado por defecto, no se enciende en el equipo de demostración y no toca datos del negocio.
- «Aprobación» incluye devolver con observaciones (D-16, columna «Aprobación o devolución»).
- La firma del distributivo (F1, F2 y F3, D-19) es de historias posteriores; el permiso de firma solo se guarda y se muestra.
- Los nombres para mostrar de los roles y de las funcionalidades están en español; el identificador interno de los 10 roles de D-16 es el de F05-H01.
- Los datos son ficticios.

## Dependencies

- F05-H01 (#43), cerrada: usuarios de la demo, sesión con roles y renovación del token.
- Modelo de errores común (ADR-016) para el 403 y base de la SPA (ADR-019) para las pantallas.
