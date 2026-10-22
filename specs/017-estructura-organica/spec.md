# Feature Specification: Mantener la estructura orgánica

**Feature Branch**: `017-estructura-organica`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Especifica la historia F03-H01 · Mantener la estructura orgánica (issue #35), a partir de RF-12, C-12, V-02, RN-10, la carga 01 y lo que dejaron F02-H01 y F05-H03."

**Origen**: issue #35 · F03-H01 · sub-issue de F03 (Mantener los datos maestros: estructura, mallas, oferta, docentes y proyectos) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:11-academico`, `RF-12` · tamaño propuesto M · depende de #23 (B00-H06), cerrada.

**Requerimientos que cubre**: RF-12 (crear, consultar, editar e inactivar la estructura orgánica: sedes y campus, carreras, coordinaciones y las 21 gestiones autorizadas; las comisiones solo como dato informativo, sin horas), C-12 y D-08 (las comisiones quedan fuera de alcance como fuente de horas), V-02 y D-07 (las 21 gestiones de POL 6 y la estructura de los datos de prueba son el orgánico de referencia), RN-10 (solo se admiten las 21 gestiones autorizadas) y la ERS §8.3, carga 1. **Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-69 (los periodos se escriben con «Distributivo del periodo»), ADR-021 (permisos), ADR-016 (catálogo de errores). **Principios de la constitución**: II (Académico es de soporte), VI (las cifras son datos), VII (toda acción relevante deja registro) y IX (mínimo privilegio).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero mantener sedes, carreras, unidades y las 21 gestiones autorizadas, para que el distributivo use la estructura vigente de la institución.

**Cómo queda tras las aclaraciones**: la estructura la mantiene el Administrador del sistema y la consultan, entre otros, el Responsable de consolidación y el Vicerrector; las carreras van con F03-H02.

**Punto de partida.** Académico ya tiene los periodos académicos (F02-H01) y avisa de ellos a Roles (F05-H03), pero no tiene la estructura orgánica: ni sedes, ni unidades, ni gestiones. La carga 1 por archivo, que la traería, es de F01, que todavía no existe. Sin la estructura, las mallas, los docentes, los cargos y la confirmación de las cargas (F03-H02, F03-H04, F03-H05 y F01-H04) no tienen a qué unidad apuntar. Esta historia permite registrar la estructura orgánica unidad por unidad, con su jerarquía, y garantiza que las gestiones sean solo las 21 autorizadas y que las comisiones queden como dato informativo.

## Clarifications

### Session 2026-10-21

- Q: ¿Quién mantiene la estructura orgánica y con qué permiso? → A: El Administrador del sistema, con «Catálogos y datos maestros: escritura», como dicen la matriz D-65 y la ERS §8.3 («Parametrización: Administrador / Vicerrectorado»); no cambia quién escribe. La leen quienes la usan: quien tenga «Distributivo del periodo: lectura» (entre ellos el Vicerrector académico y el Responsable de consolidación) o «Catálogos y datos maestros: lectura». Que una consulta admita cualquiera de los dos permisos es una decisión nueva (D-70), que el plan propone.
- Q: ¿Qué significa el criterio de RF-12 «sin permiso de administrador»? → A: Nadie registra una gestión fuera de las 21, ni con permiso de administrador: V-02, D-07 y RN-10 solo admiten las 21 de POL 6. El catálogo es un dato precargado y, si la norma cambia, se amplía con una migración. Se documenta como la interpretación de RF-12.
- Q: ¿Qué unidades pueden llevar una gestión autorizada? (al implementar: la carga 01 de prueba tiene el Rectorado con G01 y el Vicerrectorado con G02, ambos de tipo Gobierno) → A: Como dice el diccionario de la carga 01: una gestión la exige, una comisión no puede llevarla (C-12) y los demás tipos pueden llevarla o no.
- Q: ¿Las carreras entran en esta historia? → A: No. Esta historia mantiene las unidades de la carga 01, incluidas las coordinaciones de carrera. Las carreras de la carga 02 van con las mallas (F03-H02), que las necesitan, y se anota en el issue #36.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Mantener las unidades de la estructura (Priority: P1)

Como Administrador del sistema, que mantiene la estructura orgánica (aclaración 1), quiero registrar, consultar, corregir e inactivar las unidades de la institución (sedes, extensiones y campus, órganos de gobierno, unidades académicas y coordinaciones de carrera, gestiones y comisiones), cada una con la unidad de la que depende, para que el distributivo use la estructura vigente (RF-12, carga 1).

**Why this priority**: Es la base de toda la historia y de las que la siguen: las mallas, los docentes y los cargos apuntan a una unidad.

**Independent Test**: Registrar una sede matriz, un órgano de gobierno que depende de ella y una coordinación de carrera que depende del órgano; consultarlas en su jerarquía; corregir el nombre de una; inactivar otra; y comprobar que cada acción queda registrada.

**Acceptance Scenarios**:

1. **Given** una unidad con código, nombre, tipo y la unidad de la que depende, **When** la registro, **Then** queda activa en la estructura, debajo de su unidad padre, y el registro guarda quién y cuándo.
2. **Given** una unidad registrada, **When** corrijo su nombre o la unidad de la que depende, **Then** queda corregida y el registro guarda el valor anterior y el nuevo.
3. **Given** una unidad activa, **When** la inactivo, **Then** sigue en la estructura como inactiva y no se ofrece para lo nuevo (por ejemplo, como padre de otra unidad); se puede volver a activar si su padre está activo.
4. **Given** un padre que no existe, o uno que haría que la unidad dependa de sí misma o de una de las que dependen de ella, **When** guardo, **Then** se rechaza con el motivo en español.
5. **Given** un código que ya usa otra unidad, **When** registro, **Then** se rechaza: el código no se repite ni cambia después.

---

### User Story 2 - Solo las 21 gestiones autorizadas (Priority: P1)

Como quien mantiene la estructura orgánica, quiero que una gestión solo se pueda registrar como una de las 21 gestiones autorizadas de POL 6, para que el distributivo solo dé horas de gestión a las que la norma admite (RN-10, V-02).

**Why this priority**: Es el primer escenario del issue: una gestión fuera del catálogo daría horas que la norma no reconoce.

**Independent Test**: Registrar una gestión con un código del catálogo (por ejemplo, G07 Investigación) se acepta; con un código fuera de él, o sin código, se rechaza.

**Acceptance Scenarios**:

```gherkin
Escenario: Gestión fuera del catálogo
  Dado un usuario sin permiso de administrador
  Cuando intenta crear una gestión que no está entre las 21 autorizadas
  Entonces el sistema lo rechaza
```

Según la aclaración 2, también se rechaza con el permiso de administrador.

1. **Given** una unidad de tipo gestión con un código de gestión autorizada del catálogo (G01 a G21), **When** la registro, **Then** se acepta y muestra el nombre de esa gestión autorizada.
2. **Given** una unidad de tipo gestión sin código, o con uno que no está en el catálogo, **When** la registro, **Then** se rechaza con el motivo en español.
3. **Given** el catálogo de las 21 gestiones, **When** lo consulto, **Then** veo los 21 códigos con su nombre, y nadie lo cambia desde la pantalla, ni el Administrador: solo se amplía con una migración si cambia la norma (aclaración 2).

---

### User Story 3 - Comisión informativa (Priority: P1)

Como quien mantiene la estructura orgánica, quiero registrar las comisiones como dato informativo, sin horas, para conservar el orgánico completo sin que el distributivo les asigne horas (C-12, D-08).

**Why this priority**: Es el segundo escenario del issue: las comisiones están fuera de alcance como fuente de horas.

**Independent Test**: Registrar una comisión con su indicación de si es permanente; aparece en la estructura marcada como informativa, y no puede tener una gestión autorizada ni horas.

**Acceptance Scenarios**:

```gherkin
Escenario: Comisión informativa
  Dado una comisión de la estructura orgánica
  Cuando se registra
  Entonces queda como dato informativo, sin horas
```

1. **Given** una unidad de tipo comisión con su indicación de permanente o no, **When** la registro, **Then** queda en la estructura marcada como informativa, sin horas.
2. **Given** una comisión sin la indicación de permanente, o con un código de gestión autorizada, **When** la registro, **Then** se rechaza con el motivo en español.

---

### User Story 4 - La estructura en pantalla (Priority: P1)

Como Administrador del sistema, o como Responsable de consolidación o Vicerrector que la consultan, quiero verla en una pantalla «Estructura orgánica», en su jerarquía y con su tipo y su estado, y hacer allí lo de las historias anteriores, para no depender de un archivo ni de llamar al servidor.

**Why this priority**: La carga por archivo llega con F01; sin pantalla, la estructura solo se podría registrar llamando al servidor.

**Independent Test**: Ver la estructura en su jerarquía, registrar una unidad eligiendo su padre y su tipo, inactivarla; con un usuario sin escritura, ver lo mismo en solo lectura.

**Acceptance Scenarios**:

1. **Given** una estructura registrada, **When** abro «Estructura orgánica», **Then** veo cada unidad debajo de la que depende, con su código, nombre, tipo en español, estado y, si es gestión, su gestión autorizada; las comisiones se distinguen como informativas.
2. **Given** el permiso de escritura, **When** registro, corrijo o inactivo una unidad, **Then** el cambio se ve en la estructura; un valor inválido muestra su motivo junto al campo.
3. **Given** un usuario sin el permiso de escritura, **When** abre «Estructura orgánica», **Then** la ve en solo lectura, sin acciones.

---

### Edge Cases

- **Raíz de la estructura**: la sede matriz no depende de ninguna unidad; las demás unidades sí. Hay una sola sede matriz.
- **Ciclos**: una unidad no puede depender de sí misma ni de una que dependa de ella, directa o indirectamente.
- **Inactivar una unidad con dependientes**: si tiene unidades activas debajo, se rechaza con 409 y el motivo; primero se inactivan las de abajo. Así ninguna unidad activa queda debajo de una inactiva. Nunca se borra una unidad.
- **Tipo de unidad**: uno de SEDE_MATRIZ, SEDE, EXTENSION, CAMPUS, GOBIERNO, UNIDAD_ACADEMICA, GESTION o COMISION, mostrado en español. Una gestión siempre lleva gestión autorizada, una comisión nunca, y los demás tipos pueden llevarla (por ejemplo, el Rectorado, G01); solo una comisión lleva la indicación de permanente.
- **Dos unidades con la misma gestión autorizada**: se permite, porque RN-10 cuenta docentes con cargo, no unidades. La carga de prueba usa una unidad por gestión.
- **Código**: letras mayúsculas, dígitos, guion y guion bajo, hasta 20 caracteres; único; no cambia después.
- **Usuario sin permiso**: quien no tenga el permiso de escritura ve la estructura en solo lectura y el servidor responde «Acceso no autorizado» (403) si intenta cambiarla. Escribe solo quien tiene «Catálogos y datos maestros: escritura» (hoy, el Administrador del sistema); leen quienes tienen «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura» (aclaración 1).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE guardar las unidades de la estructura orgánica con código único, nombre, tipo, la unidad de la que dependen (salvo la sede matriz), estado (activa o inactiva) y, según el tipo, su gestión autorizada o la indicación de comisión permanente.
- **FR-002**: Se DEBE poder registrar, consultar, corregir (nombre, padre y los datos de su tipo) e inactivar o reactivar una unidad; el código y el tipo no cambian después. Ninguna unidad se borra.
- **FR-003**: La unidad padre DEBE existir y estar activa, y NO DEBE formarse un ciclo. Solo la sede matriz no tiene padre, y hay una sola.
- **FR-004**: Una unidad de tipo gestión DEBE llevar uno de los 21 códigos de gestión autorizada de POL 6; una comisión NO DEBE llevar ninguno; los demás tipos PUEDEN llevar uno, siempre del catálogo (ver US2).
- **FR-005**: El catálogo de las 21 gestiones autorizadas DEBE estar disponible para consultar, con su código y su nombre, como dato y no como código (principio VI).
- **FR-006**: Una unidad de tipo comisión DEBE llevar la indicación de si es permanente y quedar como dato informativo, sin gestión autorizada ni horas (C-12).
- **FR-007**: Cada dato inválido DEBE rechazarse entero, con el motivo en español y sin dejar la estructura a medias.
- **FR-008**: Mantener la estructura DEBE exigir «Catálogos y datos maestros: escritura», validada en el servidor (ADR-021); consultarla DEBE exigir «Distributivo del periodo: lectura» o «Catálogos y datos maestros: lectura».
- **FR-009**: Cada registro, corrección, inactivación y reactivación DEBE quedar registrado con quién, cuándo, qué cambió, el valor anterior y el nuevo (principio VII).
- **FR-010**: El sistema DEBE ofrecer la consulta de la estructura y de una unidad por su código, para que las mallas, los docentes, los cargos y las cargas (F03-H02, F03-H04, F03-H05, F01) apunten a ella.
- **FR-011**: La pantalla «Estructura orgánica» DEBE mostrar la estructura en su jerarquía y permitir a quien tenga el permiso registrar, corregir, inactivar y reactivar unidades, eligiendo el padre y, si corresponde, la gestión autorizada de listas.

### Key Entities

- **Unidad**: unidad orgánica de la estructura (glosario §7.3, `Unidad`, Datos maestros), con su código, nombre, tipo, padre y estado.
- **Sede**: sede matriz, sede, extensión o campus de la institución (`Sede`); es una unidad de esos tipos.
- **Gestión autorizada**: una de las 21 gestiones de POL 6 que dan horas de gestión (`GestionAutorizada`); una unidad de tipo gestión la representa.
- **Comisión**: dato informativo de la estructura orgánica; no tiene horas ni entra al distributivo (`Comision`).
- **Cambio de la estructura**: registro de quién, cuándo y qué cambió, con el valor anterior y el nuevo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quien mantiene la estructura registra una gestión, eligiendo su padre y su gestión autorizada, en menos de 1 minuto, sin ayuda.
- **SC-002**: La estructura de los datos de prueba de la carga 01 (24 unidades) se puede registrar completa, y se ve en la pantalla en su jerarquía.
- **SC-003**: El 100 % de los intentos de registrar una gestión fuera de las 21, una comisión con gestión autorizada, un ciclo o un código repetido se rechazan, también llamando directamente al servidor.
- **SC-004**: Cada cambio de la estructura aparece en el registro con quién, cuándo, el valor anterior y el nuevo.

## Assumptions

- La estructura orgánica es de Datos maestros, en el servicio Académico (modelo de dominio §3), como los periodos (F02-H01).
- Las 21 gestiones autorizadas son las de POL 6 (V-02, D-07), con los códigos G01 a G21 del diccionario de datos de las cargas; llegan precargadas con el servicio, como la política 2024 en Normativa.
- Las horas de gestión, los cargos y quién ocupa cada gestión son de otras historias (F03-H05, F07); esta historia solo guarda la estructura.
- La carga 1 por archivo (F01) usará el mismo registro de unidades que la pantalla.
- Las carreras (carga 02) no son parte de esta historia: van con F03-H02 (aclaración 3).
- Las tres aclaraciones las fijó el autor el 2026-10-21.
- Los datos son ficticios, salvo el catálogo de gestiones, que es público (POL 6).

## Dependencies

- B00-H06 (#23), cerrada: la SPA base.
- F02-H01 (#32) y F05-H03 (#45), cerradas: Académico con su contrato, su registro de cambios y su pantalla.
- F03-H02, F03-H04, F03-H05 y F01-H04 dependerán de esta historia para apuntar a una unidad.
