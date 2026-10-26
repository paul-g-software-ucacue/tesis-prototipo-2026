# Feature Specification: Vista previa y confirmación de la carga

**Feature Branch**: `025-confirmar-carga`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Especifica la historia F01-H04 · Vista previa y confirmación de la carga (issue #30), a partir de RF-10, RF-55, ADR-009 y MD-13, con las cuatro aclaraciones del autor del 2026-10-26."

**Origen**: issue #30 · F01-H04 · sub-issue de F01 (Cargar los 10 archivos con validación y vista previa) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:03-cargas`, `RF-10`, `RF-55` · tamaño propuesto L · depende de #29 (F01-H03) y #35 (F03-H01), cerradas.

**Requerimientos que cubre**:
- **RF-10:** mostrar una vista previa y pedir confirmación antes de guardar. La carga se confirma completa o no se aplica, y queda en un historial con el usuario, la fecha, el archivo, el hash y el resultado. Criterio: «Si se cancela, no se persiste ningún dato. El historial muestra cada carga».
- **RF-55:** cada carga muestra los registros nuevos, modificados y dados de baja antes de confirmarse. Criterio: «Al cargar docentes, el sistema muestra cuántos registros son nuevos, cuántos cambian y cuántos tienen errores. Sin confirmación no se modifica ningún dato».
- **MD-13 (ADR-009):** cada carga pertenece a un solo servicio dueño. Cargas valida y muestra la vista previa; al confirmar, envía el lote completo al dueño, que lo aplica entero o nada en su propia transacción. El hash del archivo es la clave de idempotencia.

**Decisiones que aplica** (`docs/decisiones.md`): D-71 (quién ejecuta las cargas), D-70 (permisos alternativos), D-72 (validación de las entradas y catálogo de errores completo), MD-13 y ADR-009. **Principios de la constitución 2.2.0**: III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), VII (inmutabilidad y trazabilidad), IX (datos personales), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero ver qué registros son nuevos, cuáles cambian y cuáles se dan de baja antes de confirmar, para aplicar solo lo que reviso.

**Punto de partida.** Hoy «Plantillas de carga» valida la estructura del archivo de cualquiera de las 10 cargas (F01-H02) y cada fila con el diccionario de datos y las referencias del sistema (F01-H03). Responde un informe por fila, pero no guarda nada. Por eso, en el equipo de demostración, subir en orden las cargas 01 y 02 no sirve: la carga 02 no encuentra las unidades de la 01, porque esas unidades nunca se guardaron.

Esta historia agrega lo que falta:
- **La vista previa:** qué registros son nuevos, cuáles cambian y cuáles se dan de baja, frente a lo que ya está en el sistema.
- **La confirmación:** el servicio dueño de cada carga aplica el archivo completo o nada.
- **El historial** de las cargas confirmadas.

Con eso, cargar en orden funciona.

## Clarifications

### Session 2026-10-26

- Q: ¿Qué cargas se aplican en esta historia? → A: Las 10, cada una en su servicio dueño (MD-13):

  | Carga | Dueño |
  |---|---|
  | 01 estructura orgánica, 02 carreras, 03 ciclos académicos, 04 asignaturas y malla, 05 oferta del periodo, 10 matriculados por carrera | Académico |
  | 06 docentes, 08 cargos y autoridades, 09 proyectos y prácticas | Docentes |
  | 07 política de carga horaria | Normativa |

  El plan las reparte en incrementos que se prueban por separado.
- Q: ¿Qué pasa con los registros que se dan de baja, los que están en el sistema pero no vienen en el archivo? → A: La vista previa los cuenta y los lista, pero confirmar no los toca: la baja se hace a mano en su pantalla. Así un archivo parcial no inactiva datos por error.
- Q: ¿Contra qué se compara el archivo? → A: Contra lo que hay en el sistema en ese momento, también lo registrado a mano en las pantallas. Es coherente con cómo F01-H03 comprueba las referencias.
- Q: ¿Se puede confirmar un archivo con errores en sus filas? → A: No. Con algún error no se puede confirmar hasta corregirlo, porque la carga se aplica completa o nada (RF-10). Las advertencias no lo impiden.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Ver la vista previa (Priority: P1)

Como quien ejecuta las cargas, quiero ver, antes de confirmar, cuántos registros del archivo son nuevos, cuántos cambian, cuántos se dan de baja y cuántos tienen errores, y cuáles son, para decidir si confirmo (RF-10, RF-55).

**Why this priority**: Es el primer escenario del issue y lo que da sentido a confirmar.

**Independent Test**: Registrar a mano algunos docentes, subir un archivo de docentes con uno nuevo, uno con la dedicación cambiada y sin uno de los registrados, y ver 1 nuevo, 1 que cambia y 1 que se da de baja.

**Acceptance Scenarios**:

```gherkin
Escenario: Vista previa
  Dado una carga de docentes válida y una carga anterior del mismo periodo
  Cuando veo la vista previa
  Entonces el sistema muestra cuántos registros son nuevos, cuántos cambian, cuántos se dan de baja y cuántos tienen errores
```

1. **Given** un archivo válido de cualquiera de las 10 cargas, **When** lo subo, **Then** veo los totales de nuevos, de los que cambian, de los que se dan de baja y de los que no cambian. También veo las listas de los tres primeros grupos, con la clave de cada registro.
2. **Given** un registro que cambia, **When** veo la vista previa, **Then** veo qué campos cambian, con el valor de ahora y el del archivo.
3. **Given** un archivo con errores en sus filas, **When** lo subo, **Then** la vista previa dice cuántas filas tienen errores, el informe de F01-H03 sigue a la vista y no puedo confirmar.

---

### User Story 2 - Confirmar (Priority: P1)

Como quien ejecuta las cargas, quiero confirmar la carga revisada y que su dueño la aplique completa o nada, para que los datos queden en el sistema y la carga siguiente los encuentre (RF-10, MD-13).

**Why this priority**: Es el tercer escenario del issue y lo que permite cargar en orden.

**Independent Test**: Confirmar la carga 01 de prueba y después subir la 02: sus unidades ya existen y la 02 queda válida.

**Acceptance Scenarios**:

```gherkin
Escenario: Confirmar
  Dado la vista previa de una carga
  Cuando confirmo
  Entonces el servicio dueño de la entidad aplica la carga completa, o no aplica nada si falla
  Y el historial registra el usuario, la fecha, el archivo, el hash y el resultado
```

1. **Given** la vista previa de un archivo sin errores, **When** confirmo, **Then** el dueño registra los nuevos y cambia los que cambian, y veo cuántos aplicó de cada grupo. Los que se dan de baja quedan como estaban.
2. **Given** un archivo que una regla del dueño rechaza al confirmar (por ejemplo, un segundo coordinador vigente de la misma carrera, RN-12), **When** confirmo, **Then** no se aplica ninguna fila y veo la fila, el campo y el motivo. El historial registra el rechazo.
3. **Given** que los datos del sistema cambiaron entre la vista previa y la confirmación (por ejemplo, alguien registró a mano un docente del archivo), **When** confirmo, **Then** no se aplica nada y veo que debo revisar la vista previa de nuevo.
4. **Given** las cargas 01 a 10 de prueba, **When** las confirmo en su orden, **Then** cada una queda aplicada y la siguiente encuentra sus referencias.

---

### User Story 3 - Cancelar (Priority: P1)

Como quien ejecuta las cargas, quiero cancelar después de ver la vista previa sin que cambie nada (RF-10).

**Why this priority**: Es el segundo escenario del issue.

**Acceptance Scenarios**:

```gherkin
Escenario: Cancelar
  Dado la vista previa de una carga
  Cuando cancelo
  Entonces no se modifica ningún dato
```

1. **Given** la vista previa, **When** cancelo o cierro la pantalla, **Then** ningún dato cambia y el historial no registra nada.

---

### User Story 4 - Confirmar el mismo archivo otra vez (Priority: P1)

Como quien ejecuta las cargas, quiero que confirmar dos veces el mismo archivo no duplique nada (MD-13).

**Why this priority**: Es el cuarto escenario del issue.

**Acceptance Scenarios**:

```gherkin
Escenario: Confirmación repetida
  Dado una carga ya confirmada
  Cuando se confirma otra vez el mismo archivo, con el mismo hash
  Entonces no se duplica ningún registro
```

1. **Given** un archivo ya confirmado en la misma carga, **When** lo confirmo de nuevo, **Then** no se aplica otra vez, veo que ya estaba confirmado y cuándo, y el historial registra el intento como repetido.

---

### User Story 5 - Ver el historial (Priority: P2)

Como quien ejecuta las cargas, o quien lee las cargas o el distributivo, quiero ver el historial de cada carga, para saber qué se cargó, quién lo hizo y cuándo (RF-10).

**Why this priority**: RF-10 pide que el historial muestre cada carga, y el tablero de preparación (F02-H02, RF-56) lo usará.

**Acceptance Scenarios**:

1. **Given** cargas confirmadas, rechazadas y repetidas, **When** abro el historial de una carga, **Then** veo cada una con el usuario, la fecha, el archivo, su hash, el resultado y cuántos registros aplicó, de la más reciente a la más antigua.

---

### Edge Cases

- **Las bajas:** se cuentan y se listan, pero no se aplican (aclaración 2). En las cargas por periodo (05 oferta y 10 matriculados), las bajas se buscan solo en los periodos que trae el archivo.
- **Qué cuenta como cambio:** solo los campos que trae el archivo. Un campo que no está en la plantilla, como el estado de una carrera registrado a mano, no cambia por la carga. Un valor igual al de ahora no es cambio, aunque se escriba distinto en el archivo, por ejemplo con espacios en los extremos.
- **Carga 07, la política:** el dueño es Normativa.
  - Las filas de un código de versión que no existe crean esa versión.
  - Las de una versión que existe la cambian, si no está asociada a un periodo.
  - Si está asociada a un periodo, el dueño rechaza la confirmación con su motivo, porque una versión en uso no cambia (principio VII).
- **Reglas de los dueños:** al confirmar, cada dueño aplica sus propias reglas, como un coordinador vigente por carrera o la suma de horas de un proyecto (aclaración 1 de F01-H03). Si una falla, no se aplica nada y el motivo dice la fila del archivo.
- **Idempotencia:** el hash de un archivo es su huella (SHA-256 de su contenido). Confirmar de nuevo el mismo archivo en la misma carga no lo aplica otra vez, aunque los datos hayan cambiado a mano después. Para volver a aplicarlo hay que cambiar el archivo.
- **Lo que se revisó es lo que se aplica:** la confirmación aplica exactamente la vista previa que se vio. Si los datos del sistema cambiaron entre las dos, no se aplica nada.
- **Sin estado entre pasos:** el servidor no guarda el archivo entre la vista previa y la confirmación. Para confirmar, la pantalla vuelve a enviar el mismo archivo, como al descargar el informe (F01-H03).
- **Datos personales:** la vista previa de las cargas 06, 08 y 09 muestra identificaciones y nombres de los docentes. Se devuelve solo a quien subió el archivo y no se registra en los logs. El historial guarda el nombre del archivo, su hash y los totales, no el contenido (principio IX).
- **Permisos:** confirmar exige «Cargas: escritura», como subir (D-71). Leer el historial exige «Cargas: lectura» o «Distributivo del periodo: lectura», como las plantillas (D-70).
- **Las referencias de F01-H03:** el motivo «regístrela o cárguela antes» pasa a «regístrela o confirme antes su carga», porque cargar es confirmar.
- **Tamaño:** el mismo máximo de 5 MB. La vista previa trae los totales y los primeros 100 de cada grupo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Al subir un archivo de cualquiera de las 10 cargas, el sistema DEBE comparar sus registros, por la clave de la carga, con los del servicio dueño. DEBE informar cuántos son nuevos, cuántos cambian, cuántos se dan de baja, cuántos no cambian y cuántas filas tienen errores.
- **FR-002**: La vista previa DEBE listar los primeros 100 registros de cada grupo con su clave y, para los que cambian, los campos que cambian, con el valor de ahora y el del archivo.
- **FR-003**: Un archivo con algún error en sus filas NO DEBE poder confirmarse. Las advertencias no lo impiden.
- **FR-004**: Confirmar DEBE enviar el lote completo al servicio dueño de la carga, que lo aplica entero o nada en su propia transacción (MD-13):
  - registra los nuevos;
  - cambia los que cambian;
  - no toca los que se dan de baja ni los que no cambian.
- **FR-005**: El servicio dueño DEBE aplicar al confirmar sus propias reglas, además de las del diccionario. Si una falla, no se aplica nada y la respuesta dice la fila del archivo, el campo y el motivo, con su código del catálogo.
- **FR-006**: La confirmación DEBE aplicar la misma vista previa que se revisó. Si los datos del sistema cambiaron desde entonces, NO DEBE aplicar nada y DEBE pedir revisar la vista previa de nuevo.
- **FR-007**: Confirmar de nuevo un archivo con el mismo hash en la misma carga NO DEBE aplicarlo otra vez. DEBE decir cuándo se confirmó.
- **FR-008**: Cada intento de confirmar DEBE quedar en el historial de cargas: la carga, el usuario, la fecha y hora, el nombre del archivo, su hash, el resultado (aplicada, rechazada o repetida) y los totales aplicados. NO DEBE guardar el contenido del archivo (principio IX).
- **FR-009**: Cancelar la vista previa NO DEBE cambiar ningún dato ni registrar nada.
- **FR-010**: El historial de cada carga DEBE poder consultarse, de la más reciente a la más antigua.
- **FR-011**: Confirmar DEBE exigir «Cargas: escritura» (D-71) y leer el historial, «Cargas: lectura» o «Distributivo del periodo: lectura» (D-70), validados en el servidor.
- **FR-012**: Cada servicio dueño DEBE traducir las columnas de su carga a sus datos en un solo lugar, probado con los archivos de prueba de la ERS (principio VI).
- **FR-013**: La pantalla «Plantillas de carga» DEBE mostrar la vista previa al subir un archivo, con «Confirmar» habilitado solo si no tiene errores y «Cancelar». Después de confirmar, DEBE mostrar el resultado y permitir ver el historial de la carga.
- **FR-014**: El motivo de una referencia que no existe (F01-H03) DEBE decir «regístrela o confirme antes su carga».
- **FR-015**: Cada código de error nuevo DEBE estar en el catálogo con sus 8 columnas (D-72) y sus mensajes DEBEN decir qué hacer.

### Key Entities

- **Carga** y **Diccionario de datos**: los de F01-H01.
- **Vista previa**: los registros nuevos, los que cambian, con sus campos, los que se dan de baja y los que no cambian, frente a lo que hay en el sistema (glosario, `VistaPrevia`).
- **Confirmar una carga**: aplicar la carga completa, o nada, en el servicio dueño (glosario, `confirmar()`, MD-13).
- **Historial de cargas**: cada intento de confirmar, con el archivo, la fecha, el usuario, el hash y el resultado (glosario, `HistorialCargas`).
- **Lote de la carga**: los registros del archivo traducidos a los datos del dueño, con la fila de cada uno, que el dueño aplica en su transacción.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Las 10 cargas de prueba, confirmadas en su orden sobre un sistema sin esos datos, quedan aplicadas sin ningún error, y cada una encuentra las referencias de las anteriores.
- **SC-002**: Volver a subir cualquiera de las 10 cargas de prueba después de confirmarla da 0 nuevos, 0 que cambian y 0 que se dan de baja. Confirmarla otra vez no cambia ningún dato.
- **SC-003**: En el 100 % de las confirmaciones rechazadas, por una regla del dueño o porque los datos cambiaron, ningún dato del archivo queda aplicado.
- **SC-004**: La vista previa de un archivo de 5 MB, el máximo, responde en menos de 30 segundos, y su confirmación en menos de 60.
- **SC-005**: Quien ejecuta las cargas hace las 10 de prueba en orden, revisando cada vista previa, en menos de 30 minutos y sin ayuda.

## Assumptions

- **Las claves:** las de F01-H03, que salen del diccionario (`PK` o `PK compuesta`). La carga 10 no tiene clave en el diccionario: sus registros se comparan por periodo y carrera, sus dos referencias.
- **Matriculados:** la carga 10 necesita en Académico un lugar para los totales de estudiantes por carrera y periodo, sin datos personales (glosario «Matriculados»). Hoy no lo tiene.
- **Lo que cambia la carga:** cada columna de la plantilla corresponde a un dato del dueño. Las columnas que el dueño no guarda todavía, porque son para historias futuras, se validan (F01-H03) pero no cuentan como cambio. Las enumera el plan.
- **Hash:** SHA-256 del contenido del archivo. El mismo contenido con otro nombre es el mismo archivo.
- **Rendimiento:** con holgura, porque el AS-IS del ISTA tarda 3 semanas en preparar el distributivo.
- **Fuera de alcance:**
  - aplicar las bajas (aclaración 2);
  - el orden obligatorio de las 10 cargas, el «sin proyectos» y el mapeo configurable de etiquetas (F01-H05);
  - el tablero de preparación del periodo (F02-H02, RF-56);
  - las reimportaciones incrementales por clave natural (RF-11, Won't).
