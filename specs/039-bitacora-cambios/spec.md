# Feature Specification: Registrar cada cambio en la bitácora

**Feature Branch**: `039-bitacora-cambios`

**Created**: 2026-11-05

**Status**: Draft

**Input**: User description: "Especifica la historia F17-H01 · Registrar cada cambio en la bitácora (issue #69). Léela de su issue con gh issue view 69. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md y docs/decisiones.md (en especial D-47, D-48, ADR-018 y D-89)."

**Origen**: issue #69 · F17-H01 · sub-issue de F17 (Bitácora de auditoría que no se puede borrar, #16) · milestone «Ola 3 · Aprobado y firmado» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:01-usuarios`, `RF-44` · tamaño propuesto M · depende de #22 (B00-H05), cerrada.

**Requerimientos que cubre**:
- **RF-44:** una bitácora de auditoría con quién, qué, cuándo, el valor anterior y el valor nuevo. Su criterio en la ERS: «cada cambio en una asignación aparece en la bitácora y no puede eliminarse». Consultarla es F17-H02.
- **RNF-INT-02:** toda la auditoría se conserva por el tiempo que fije la normativa de archivo (supuesto de la ERS: 5 años).
- **ADR-009:** lo que no puede perderse se publica como evento dentro de la transacción del servicio, y llega aunque el receptor esté caído.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-90 (el alcance de esta historia), D-91 (datos personales de un docente), D-47 y D-48 (el receptor y el evento de prueba que esta historia puede retirar), D-49 y ADR-018 (eventos entre procesos, sobre común, token de proceso y recepción idempotente), ADR-021 (permisos), D-72, D-74 y ADR-016 (catálogo de errores). **Principios de la constitución 2.2.0**: III (lenguaje ubicuo), IV (API primero), V (límites y fallos entre procesos), VII (inmutabilidad y trazabilidad), VIII (pruebas), IX (seguridad y datos personales), X (simplicidad y alcance) y XIII (errores).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero que cada cambio quede en una bitácora que no se puede borrar, para probar quién cambió, aprobó y firmó cada versión.

**Punto de partida.** El camino de los eventos entre procesos ya existe y está probado (B00-H05): un servicio publica un evento en su transacción, el evento llega a Usuarios aunque el proceso de identidad haya estado caído, y Usuarios lo registra una sola vez. Hoy solo viaja un evento de prueba. Además, cada servicio con datos guarda su propio registro de cambios, en la misma operación que el cambio: el distributivo, los periodos y los datos maestros académicos, los docentes con sus cargos y proyectos, la normativa y los roles. La configuración del flujo guarda quién la cargó y cuándo. Pero cada registro está aislado en su servicio, no hay una bitácora única, y no todos guardan el valor anterior y el nuevo.

Esta historia agrega:
- **La bitácora:** un registro único, solo de inserción, en el servicio de Usuarios.
- **Las entradas:** quién, qué, cuándo, el objeto, el valor anterior y el valor nuevo.
- **La entrega segura:** cada cambio viaja con el camino de B00-H05, así que no se pierde si el proceso de identidad está caído.

Consultar la bitácora en pantalla es F17-H02.

## Clarifications

### Session 2026-11-05

- Q: ¿Qué cambios registra la bitácora en esta historia? → A: Todos los que hoy registra cada servicio en su propio registro de cambios, más la carga de la configuración del flujo (D-90). Se eligió primero un alcance menor, con la premisa de que el distributivo no tenía registro; al planificar se vio que sí lo tenía y que todos los servicios ya pasan por un registro propio, así que registrar todo cuesta casi lo mismo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Un cambio queda registrado con su valor anterior y nuevo (Priority: P1)

Como Vicerrector académico, quiero que cada cambio quede en la bitácora con quién lo hizo, qué cambió, cuándo, y el valor anterior y el nuevo, para poder probarlo después.

**Why this priority**: es la historia, y las siguientes de la Ola 3 (avanzar etapas, congelar, firmar, publicar) registran sus transiciones en ella.

**Independent Test**: cambiar una asignación de 4 a 6 horas y encontrar en la bitácora una entrada con el usuario, el momento, la asignación, 4 como valor anterior y 6 como valor nuevo.

**Acceptance Scenarios**:

1. **Dado** una asignación de 4 horas, **cuando** un coordinador la cambia a 6 horas, **entonces** la bitácora registra quién, qué, cuándo, el valor anterior y el valor nuevo.
2. **Dado** una asignación nueva, **cuando** un coordinador la crea, **entonces** la bitácora la registra sin valor anterior.
3. **Dado** una asignación, **cuando** un coordinador la quita, **entonces** la bitácora la registra sin valor nuevo.
4. **Dado** un cambio rechazado (por ejemplo, una regla dura violada), **cuando** se intenta, **entonces** no queda ninguna entrada, porque nada cambió.

---

### User Story 2 - La bitácora no se puede borrar ni modificar (Priority: P1)

Como Vicerrector académico, quiero que nadie pueda borrar ni cambiar una entrada, para que la bitácora sirva como prueba.

**Why this priority**: sin esto la bitácora no prueba nada (criterio de RF-44).

**Independent Test**: revisar que ninguna operación del sistema permite borrar o modificar una entrada, y que un intento directo sobre los datos guardados también falla.

**Acceptance Scenarios**:

1. **Dado** una entrada de la bitácora, **cuando** cualquier usuario intenta borrarla o modificarla, **entonces** no existe ninguna operación que lo permita.
2. **Dado** una entrada de la bitácora, **cuando** se intenta borrarla o modificarla directamente en el almacén de datos con la cuenta del sistema, **entonces** el intento se rechaza.

---

### User Story 3 - No se pierde nada si el proceso de identidad se cae (Priority: P1)

Como Vicerrector académico, quiero que los cambios hechos mientras el proceso de identidad está detenido lleguen a la bitácora cuando vuelve, para que no falte ninguno.

**Why this priority**: es lo que distingue la bitácora de un registro cualquiera (objetivo 2 de la tesis, principio V).

**Independent Test**: detener el proceso de identidad, hacer varios cambios, volver a levantarlo y ver que la bitácora tiene todas las entradas, una sola vez cada una.

**Acceptance Scenarios**:

1. **Dado** el proceso de identidad detenido mientras se hacen cambios, **cuando** vuelve, **entonces** la bitácora tiene todas las entradas de ese lapso.
2. **Dado** un cambio cuya entrada llega más de una vez por los reenvíos, **cuando** se registra, **entonces** la bitácora la tiene una sola vez.
3. **Dado** el proceso de identidad detenido, **cuando** un usuario hace un cambio, **entonces** el cambio se guarda igual y el usuario no ve ningún error por la bitácora.

---

### User Story 4 - Sin datos sensibles (Priority: P1)

Como institución, quiero que la bitácora no guarde contraseñas ni tokens, para no exponer credenciales a quien la lea (RNF-SEG-06, LOPDP).

**Why this priority**: el issue lo pide y es un requisito de seguridad.

**Independent Test**: hacer cambios de todos los tipos registrados, incluidos los de usuarios y roles, y revisar que ninguna entrada contiene una contraseña ni un token.

**Acceptance Scenarios**:

1. **Dado** cualquier entrada de la bitácora, **cuando** se revisa, **entonces** no contiene contraseñas ni tokens.

---

### Edge Cases

- **Dos cambios a la vez sobre lo mismo:** cada uno deja su entrada, con el valor anterior que leyó su propia transacción. Esta historia no agrega bloqueos: el orden lo decide cada servicio, como hoy (Generación bloquea la versión al guardar, D-83).
- **Una entrada repetida por un reenvío:** se registra una sola vez.
- **Un tipo de cambio que la bitácora no conoce:** se rechaza con el error del catálogo, y queda pendiente hasta que se corrija; no se pierde.
- **Un cambio que entra por una carga de archivo** (por ejemplo, los docentes de la carga 4): cada fila aplicada deja su entrada, como si se hubiera hecho en pantalla, con el usuario que confirmó la carga. Una carga rechazada no deja entradas, porque no cambió nada.
- **Un valor con datos personales** (los nombres, el correo, el teléfono o la condición especial de un docente): la entrada dice qué campo cambió, sin sus valores (D-91). La identificación del docente sí se guarda, porque dice a quién se refiere el cambio.
- **El proceso que publica se cae a mitad de la entrega:** la entrada se reenvía cuando vuelve, y no se duplica.

## Requirements *(mandatory)*

### Functional Requirements

**Qué se registra**

- **FR-001**: Cada cambio dentro del alcance (FR-002) DEBE dejar exactamente una entrada en la bitácora.
- **FR-002**: La bitácora DEBE registrar todos los cambios de (D-90):
  - **El distributivo:** crearlo; proponer, cambiar y quitar un docente en una asignatura-paralelo; asignar, cambiar y quitar una actividad no lectiva; guardar o cambiar una justificación; consolidar; adjuntar o reemplazar el informe sustentado.
  - **Académico:** periodos (registrar, corregir, abrir con su normativa, cambiar de estado), unidades de la estructura orgánica, carreras, mallas, asignaturas, la oferta del periodo y los matriculados.
  - **Docentes:** docentes, cargos y proyectos.
  - **La normativa:** crear una versión, cambiar sus valores y asociarla a un periodo.
  - **Los roles:** crear, cambiar y borrar un rol; asignar o quitar roles a un usuario; indicar o quitar la carrera de un coordinador; designar o reemplazar al Responsable de consolidación.
  - **El flujo:** cargar una configuración del flujo y del árbol de firmas.
  - **Las cargas de archivo:** cada cambio que una carga confirmada aplica en los puntos anteriores.
- **FR-003**: Cada entrada DEBE decir quién hizo el cambio, qué acción fue, cuándo ocurrió, el objeto (el elemento y, si aplica, su periodo), el valor anterior y el valor nuevo. El valor anterior falta al crear y el nuevo falta al quitar.
- **FR-004**: Un intento rechazado (por validación, por permisos o por una regla) NO DEBE dejar entrada.
- **FR-005**: La entrada DEBE registrarse solo si el cambio se guardó, y siempre que se guardó: ambos van juntos.

**Que no se pierda ni se repita**

- **FR-006**: Un cambio hecho con el proceso de identidad detenido DEBE llegar a la bitácora cuando vuelva, sin intervención manual.
- **FR-007**: Una misma entrada recibida varias veces DEBE quedar una sola vez.
- **FR-008**: Que la bitácora no responda NO DEBE impedir ni demorar el cambio del usuario, ni mostrarle un error.

**Que no se pueda borrar**

- **FR-009**: NO DEBE existir ninguna operación que borre o modifique una entrada.
- **FR-010**: El almacén de la bitácora DEBE rechazar un borrado o una modificación, aunque se intente con la cuenta del servicio.

**Seguridad**

- **FR-011**: Una entrada NO DEBE contener contraseñas, tokens ni secretos.
- **FR-014**: Una entrada NO DEBE contener los valores de los datos personales de un docente que no hacen falta para la auditoría: nombres y apellidos, correo, teléfono y condición especial. De esos campos DEBE decir que cambiaron, sin el valor anterior ni el nuevo (D-91, RNF-SEG-06).
- **FR-012**: Solo los procesos del sistema DEBEN poder escribir en la bitácora; ningún usuario puede escribir en ella directamente.
- **FR-013**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII).

### Key Entities

- **Entrada de la bitácora** (`EntradaBitacora`, glosario §7.6 y lenguaje publicado §4): un hecho que ya ocurrió. Tiene quién, qué, cuándo, el objeto, el valor anterior y el valor nuevo, más el servicio de origen y el periodo. No cambia nunca.
- **Acción**: el «qué» de la entrada, con el nombre que ya usa el registro de cambios de cada servicio (por ejemplo, `ACTIVIDAD_GUARDADA`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las acciones dentro del alcance llegan a la bitácora exactamente una vez: cada servicio, de punta a punta con los procesos reales, y cada acción, en las pruebas de su servicio.
- **SC-002**: Con el proceso de identidad detenido durante al menos 2 minutos y al menos 10 cambios en ese lapso, la bitácora tiene las 10 entradas en menos de 2 minutos después de que vuelve.
- **SC-003**: El tiempo que tarda un cambio del usuario no sube más de 0,5 segundos por registrarlo en la bitácora, con el proceso de identidad arriba o abajo.
- **SC-004**: Ninguna entrada contiene una contraseña ni un token, en una revisión de todas las entradas generadas por las pruebas.
- **SC-005**: Todos los intentos de borrar o modificar una entrada, por la API o directamente sobre los datos, fallan.

## Assumptions

- **Firmas:** el escenario del issue dice «mientras se aceptan firmas», pero firmar llega con F14. Esta historia lo demuestra con los cambios que existen hoy (las asignaciones), y cada historia que agregue una acción (firmar, avanzar etapa, congelar, publicar) publica su propia entrada sobre el mismo camino.
- **Sin carga histórica:** los cambios hechos antes de esta historia no se copian a la bitácora. Los registros locales que ya existen se conservan como están.
- **Mismo valor:** la bitácora recibe lo que cada servicio ya considera un cambio, sin una regla nueva.
- **Quién:** el usuario que inició la acción, por su nombre de usuario. Los datos de la demo son ficticios (D-06).
- **Retención:** las entradas se conservan siempre y no hay purga, lo que cumple los 5 años de RNF-INT-02.
- **El evento de prueba de B00-H05:** esta historia puede retirar el receptor y el evento de prueba (D-47), porque el camino queda probado con entradas reales.
- **Consultar:** no hay pantalla ni operación de consulta para usuarios; eso es F17-H02. La evidencia se revisa con las pruebas y con consultas de solo lectura.

## Out of Scope

- Consultar y filtrar la bitácora (F17-H02).
- Registrar firmas, transiciones de etapa, congelamientos, resolución del OCS y publicación: cada historia lo agrega (F12-H02, F13, F14, F15, F18).
- Notificaciones (F16-H01).
- El historial de los archivos de carga subidos (validados, rechazados o repetidos): lo que una carga cambia se registra en el servicio dueño, pero subir o rechazar un archivo no cambia datos.
- Iniciar y renovar sesión: no cambian datos del negocio.
- Copiar a la bitácora los cambios anteriores a esta historia.
- Exportar la bitácora o firmarla.
