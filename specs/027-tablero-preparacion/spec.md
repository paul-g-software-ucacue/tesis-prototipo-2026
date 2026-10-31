# Feature Specification: Ver el tablero de preparación del periodo

**Feature Branch**: `027-tablero-preparacion`

**Created**: 2026-10-30

**Status**: Draft

**Input**: User description: "Especifica la historia F02-H02 · Ver el tablero de preparación del periodo (issue #33), a partir de RF-56 y del glosario «Tablero de preparación», con las cuatro aclaraciones del autor del 2026-10-30."

**Origen**: issue #33 · F02-H02 · sub-issue de F02 (Abrir el periodo con plazos y ver el tablero de preparación, #4) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · tamaño propuesto M · depende de #31 (F01-H05), cerrada. Es la última historia de la Ola 1.

**Requerimientos que cubre**:
- **RF-56:** un tablero de preparación del periodo con el estado de cada carga (pendiente, cargada o con errores), que habilita la construcción del distributivo solo cuando están las cargas obligatorias. Criterio: «Con la carga de docentes pendiente, el botón "Crear distributivo" está deshabilitado y el tablero indica qué falta».
- **Glosario §7, «Tablero de preparación»:** el estado de cada carga del periodo (pendiente, cargada o con errores) y qué falta para crear el distributivo (`PreparacionDelPeriodo`).
- **Glosario §7, «Carga obligatoria»:** las 9 primeras; la de proyectos admite la confirmación «sin proyectos». La 10, matriculados por carrera, es recomendada y no bloquea.

**Decisiones que aplica** (`docs/decisiones.md`): D-69 (los periodos se escriben con «Distributivo del periodo»), D-70 (permisos alternativos), D-71 (quién ejecuta las cargas), D-72 (validación de las entradas y catálogo de errores completo), D-74 (mensajes en «tú»), MD-13 y ADR-022 (cada carga tiene su servicio dueño, que dice si tiene datos). **Principios de la constitución 2.2.0**: III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), IX (mínimo privilegio), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero ver qué cargas faltan antes de crear el distributivo, para saber cuándo se puede empezar a construirlo.

**Punto de partida.** F01-H05 dejó en «Plantillas de carga» el estado de cada carga en todo el sistema: pendiente, cargada o «sin proyectos», y lo que le falta. Eso sirve a quien sube los archivos, pero no responde la pregunta del Vicerrector: **para este periodo**, ¿está todo listo para empezar el distributivo? Tres cargas son del periodo (la oferta, los proyectos y los matriculados), un intento rechazado no se ve como «con errores», y no hay un lugar que diga «se puede empezar» o «falta esto».

Esta historia agrega:
- **El tablero de un periodo:** el estado de cada carga en ese periodo, también «con errores».
- **Si el periodo está listo** para crear el distributivo, y qué falta si no.
- **El botón «Crear distributivo»,** habilitado solo cuando está listo. Por ahora no crea nada: el distributivo se construye en la Ola 2 (F06).

## Clarifications

### Session 2026-10-30

- Q: El distributivo todavía no existe en el sistema; se construye en la Ola 2 (F06). En esta historia, ¿qué hace «Crear distributivo»? → A: Solo se habilita. El servidor dice si el periodo está listo y qué falta, y el botón se habilita o deshabilita con eso. Al pulsarlo, por ahora, un aviso dice que el periodo está listo y que la construcción del distributivo llega con F06, que le pondrá la acción de verdad. No se crea ningún distributivo.
- Q: ¿Cuándo está una carga «con errores»? → A: Cuando no tiene datos y su último intento de confirmar, en el historial de cargas, fue rechazado: con errores en sus filas, con la vista previa desactualizada o por una regla de su servicio dueño. Sale del historial que ya existe (F01-H04), sin guardar nada nuevo; validar un archivo sigue sin dejar huella.
- Q: El tablero es de un periodo. ¿Qué cargas se miran solo en ese periodo? → A: Las de «datos del periodo» de la ERS §8.3. La oferta y los matriculados cuentan si hay datos de ese periodo. Los proyectos cuentan si alguno es asignable en él o si el periodo está confirmado «sin proyectos» (F01-H05). Las demás (estructura, carreras, ciclos, asignaturas, docentes, política y cargos) se miran como en F01-H05, en todo el sistema.
- Q: ¿Dónde va el tablero y quién lo ve? → A: En una pantalla nueva, «Preparación del periodo», con un selector de periodo. La ven quienes leen el distributivo o las cargas (D-70). «Crear distributivo» solo aparece para quien escribe el distributivo: hoy, el Vicerrector académico (D-69).

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias.

### User Story 1 - Ver qué falta en un periodo (Priority: P1)

Como Vicerrector académico, quiero elegir un periodo y ver el estado de cada carga en él, y qué falta para empezar el distributivo, para saber a quién pedir cada dato (RF-56).

**Why this priority**: Es el primer escenario del issue y la razón del tablero.

**Independent Test**: Con todas las cargas obligatorias salvo los docentes, abrir «Preparación del periodo», elegir el periodo y ver «Docentes contratados» como pendiente y la lista de lo que falta con esa carga.

**Acceptance Scenarios**:

```gherkin
Escenario: Falta una carga obligatoria
  Dado un periodo con la carga de docentes pendiente
  Cuando abro el tablero de preparación
  Entonces el botón "Crear distributivo" está deshabilitado
  Y el tablero indica qué carga falta
```

1. **Given** un periodo con todas las cargas obligatorias salvo la de docentes, **When** abro el tablero de ese periodo, **Then** veo las 10 cargas con su estado, la de docentes «Pendiente», y el tablero dice que falta la carga de docentes.
2. **Given** un periodo sin oferta propia y con oferta en otro periodo, **When** abro su tablero, **Then** la oferta está «Pendiente» en este periodo.
3. **Given** un periodo con matriculados de otro periodo pero no del suyo, **When** abro su tablero, **Then** los matriculados están «Pendiente», pero no bloquean, porque son recomendados.
4. **Given** un periodo sin proyectos asignables y sin confirmar «sin proyectos», **When** abro su tablero, **Then** los proyectos están «Pendiente» y el tablero dice que faltan los proyectos, o confirmarlo «sin proyectos».
5. **Given** un periodo confirmado «sin proyectos», **When** abro su tablero, **Then** los proyectos dicen «Sin proyectos» y no faltan.
6. **Given** una carga sin datos cuyo último intento de confirmar fue rechazado, **When** abro el tablero, **Then** esa carga dice «Con errores» y su motivo, y cuenta como lo que falta.

---

### User Story 2 - Saber cuándo se puede empezar (Priority: P1)

Como Vicerrector académico, quiero que «Crear distributivo» se habilite solo cuando el periodo tiene todas sus cargas obligatorias, para no empezar con datos incompletos (RF-56).

**Why this priority**: Es el segundo escenario del issue.

**Independent Test**: Con las 9 cargas obligatorias del periodo, ver «Crear distributivo» habilitado y, al pulsarlo, el aviso de que el periodo está listo y de que la construcción llega con F06.

**Acceptance Scenarios**:

```gherkin
Escenario: Cargas completas
  Dado un periodo con las cargas obligatorias confirmadas
  Cuando abro el tablero de preparación
  Entonces el botón "Crear distributivo" está habilitado
  Y cada carga muestra su estado: pendiente, cargada o con errores
```

1. **Given** un periodo abierto con sus 9 cargas obligatorias (la de proyectos, o «sin proyectos»), **When** abro su tablero con el permiso de escribir el distributivo, **Then** «Crear distributivo» está habilitado, aunque falten los matriculados.
2. **Given** un periodo listo, **When** pulso «Crear distributivo», **Then** veo un aviso de que el periodo está listo y de que la construcción del distributivo llega en la Ola 2. No se crea nada.
3. **Given** un periodo con alguna carga obligatoria pendiente o con errores, **When** abro su tablero, **Then** «Crear distributivo» está deshabilitado, y su descripción dice qué falta.
4. **Given** un periodo con todas sus cargas obligatorias pero que todavía no está abierto (sin su versión de normativa, F02-H01), **When** abro su tablero, **Then** «Crear distributivo» está deshabilitado y lo que falta dice que hay que abrir el periodo.
5. **Given** un usuario que lee el distributivo o las cargas, pero no escribe el distributivo, **When** abre el tablero, **Then** ve el estado y lo que falta, pero no el botón «Crear distributivo».

---

### Edge Cases

- **Un periodo que no está en los ciclos académicos:** el servidor responde un error del catálogo; la pantalla solo ofrece periodos que existen.
- **Un periodo con un código mal escrito,** por ejemplo pedido por la API: el servidor lo rechaza por su formato (principio XIV).
- **Sin periodos cargados:** la pantalla dice que primero hay que cargar los ciclos académicos, y no muestra el tablero.
- **Un periodo cerrado:** el tablero se muestra, pero no está listo: un periodo cerrado no admite un distributivo nuevo.
- **Una carga con datos y un último intento rechazado:** cuenta como «Cargada»; el intento rechazado no borra lo que ya entró.
- **Si la consulta de los datos de una carga falla:** la pantalla muestra el error del catálogo con qué hacer, no un estado falso.

## Requirements *(mandatory)*

### Functional Requirements

**El tablero**

- **FR-001**: El sistema MUST ofrecer, para un periodo de los ciclos académicos, el estado de cada una de las 10 cargas en ese periodo: «Pendiente», «Cargada», «Con errores» o, la de proyectos, «Sin proyectos».
- **FR-002**: La oferta del periodo y los matriculados por carrera MUST contar como cargados solo si hay datos vigentes de ese periodo. Los proyectos, si al menos uno es asignable en el periodo o si está confirmado «sin proyectos». Las demás cargas, como en F01-H05: si su servicio dueño tiene datos vigentes en todo el sistema.
- **FR-003**: Una carga sin datos cuyo último intento de confirmar fue rechazado MUST estar «Con errores», con el motivo del rechazo. Una carga con datos MUST estar «Cargada», aunque su último intento haya sido rechazado.
- **FR-004**: El tablero MUST decir qué falta para crear el distributivo:
  - cada carga obligatoria (de la 1 a la 9) que no esté «Cargada» (ni «Sin proyectos», la 9);
  - que el periodo no está abierto, si todavía no tiene su versión de normativa;
  - que el periodo está cerrado, si lo está.
- **FR-005**: El periodo MUST estar «listo» solo cuando no falta nada de FR-004. Los matriculados (carga recomendada) MUST NOT impedirlo.

**«Crear distributivo»**

- **FR-006**: «Crear distributivo» MUST aparecer solo para quien tiene el permiso de escribir el distributivo, y MUST estar habilitado solo si el periodo está listo. Deshabilitado, su descripción accesible dice qué falta.
- **FR-007**: En esta historia, pulsar «Crear distributivo» MUST mostrar un aviso de que el periodo está listo y de que la construcción del distributivo llega con F06 (Ola 2), sin crear ni cambiar nada.

**La pantalla**

- **FR-008**: El sistema MUST ofrecer la pantalla «Preparación del periodo», con un selector de los periodos de los ciclos académicos; al abrirla elige el periodo que empieza más tarde.
- **FR-009**: La pantalla MUST mostrar, por carga, su número, su nombre, si es obligatoria y su estado; y, aparte, la lista de lo que falta o que el periodo está listo.

**Transversales**

- **FR-010**: Consultar el tablero MUST requerir leer el distributivo o leer las cargas (D-70).
- **FR-011**: El periodo pedido MUST validarse por tipo, caracteres y largo con la regla de Académico, su dueño (letras, números, punto, guion y guion bajo, hasta 20), en el servidor y en la pantalla. La regla de `codigo_periodo` del diccionario (`AAAA-I` o `AAAA-II`) es la del archivo de la carga 3: un periodo registrado en su pantalla puede tener otro código. Un periodo que no existe MUST responderse con un error del catálogo.
- **FR-012**: Todo mensaje nuevo al usuario MUST salir del catálogo de errores, en «tú», sin detalles técnicos, y decir qué hacer (D-72, D-74).
- **FR-013**: El tablero MUST NOT mostrar datos personales: solo dice si hay datos, no cuáles.

### Key Entities

- **Preparación del periodo** (glosario «Tablero de preparación», `PreparacionDelPeriodo`): el periodo, si está abierto, el estado de cada carga en él, lo que falta y si está listo. Se calcula en cada consulta; no se guarda.
- **Estado de la carga** (F01-H05): suma «Con errores», con el motivo del último rechazo.
- **Lo que falta:** cada carga obligatoria que no está cargada, y el periodo sin abrir o cerrado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con la carga de docentes pendiente, el tablero dice que falta y «Crear distributivo» está deshabilitado el 100 % de las veces.
- **SC-002**: Con las 9 cargas obligatorias del periodo y el periodo abierto, «Crear distributivo» está habilitado, aunque falten los matriculados.
- **SC-003**: El Vicerrector sabe si puede empezar el distributivo mirando una sola pantalla, que responde en menos de 3 segundos (con holgura).
- **SC-004**: Ninguna carga muestra un estado de otro periodo: la oferta, los proyectos y los matriculados de un periodo no cuentan para otro.
- **SC-005**: Ningún dato personal aparece en el tablero, en sus mensajes ni en los registros de log.

## Assumptions

- «Abierto» es tener su versión de normativa (F02-H01): sin ella, el motor del distributivo no tiene reglas. Es una condición del tablero, además de las cargas, porque el glosario dice que el tablero muestra «qué falta para crear el distributivo».
- Un periodo cerrado no está listo: el glosario dice que un periodo cerrado no cambia.
- El «último intento» es la entrada más reciente del historial de esa carga, sin contar las confirmaciones «sin proyectos».
- Los periodos del selector son los de los ciclos académicos, en cualquier estado.
- Quien escribe el distributivo hoy es el Vicerrector académico (D-69); si después otro rol lo tiene, ve el botón sin cambiar nada.
- Los datos de prueba son ficticios (repositorio público).

## Out of Scope

- Crear el distributivo y su ciclo de vida (F06, F12).
- Los plazos por etapa del periodo y la vista del estado del periodo (F02-H03, Ola 4).
- Subir o confirmar cargas desde el tablero: eso sigue en «Plantillas de carga».
- Guardar el resultado de las validaciones.
