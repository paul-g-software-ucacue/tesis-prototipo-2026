# Feature Specification: Validar las reglas de los datos e informe por fila

**Feature Branch**: `024-validar-reglas-carga`

**Created**: 2026-10-25

**Status**: Draft

**Input**: User description: "Especifica la historia F01-H03 · Validar las reglas de los datos e informe por fila (issue #29), a partir de RF-08, RF-09, RNF-SEG-04 y el diccionario de datos de las 10 cargas, con las cuatro aclaraciones del autor del 2026-10-25."

**Origen**: issue #29 · F01-H03 · sub-issue de F01 (Cargar los 10 archivos con validación y vista previa) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:03-cargas`, `RF-08`, `RF-09` · tamaño propuesto L · depende de #28 (F01-H02), cerrada.

**Requerimientos que cubre**:
- **RF-08:** validar las reglas de los datos: la cédula como texto de 10 dígitos con su dígito verificador (o un pasaporte), los duplicados, la integridad referencial (por ejemplo, asignatura → carrera), las fechas y los valores de catálogo. Criterio: «`301740718` se marca "cédula inválida o sin cero inicial" y se sugiere `0301740718`. Una carrera inexistente se informa en su fila». El mapeo configurable de etiqueta a código queda para F01-H05 (aclaración 4).
- **RF-09:** un informe de errores y advertencias por fila, descargable en CSV, y corregir y reintentar. Criterio: «El informe lista número de fila, campo, valor y motivo».
- **RF-07, la parte de las filas:** los tipos, las longitudes y los obligatorios de cada fila, que F01-H02 dejó para esta historia.
- **RNF-SEG-04:** el informe neutraliza la inyección de fórmulas en el CSV exportado.

**Decisiones que aplica** (`docs/decisiones.md`): D-65 y D-71 (quién sube los archivos), D-70 (permisos alternativos), D-72 (validación de las entradas y catálogo de errores completo), D-73 (los 22 cargos), MD-13 (la carga se aplica en el servicio dueño). **Principios de la constitución 2.2.0**: III (lenguaje ubicuo), IV (API primero), V (límites entre servicios), VI (las reglas son datos), IX (datos personales), XIII (errores del catálogo completo) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero que se validen las reglas de cada fila y descargar un informe de errores, para corregir el archivo y volver a intentar.

**Punto de partida.** Hoy el MDD recibe el archivo de una carga y revisa su estructura: el formato, la codificación, el separador, los encabezados, las hojas, el tamaño y si tiene registros (F01-H02). Pero no mira las filas: una cédula sin el cero inicial, una fecha imposible o una carrera que no existe pasan sin aviso. Esta historia revisa cada fila con las reglas del diccionario de datos, que ya está en el sistema (F01-H01), y con las referencias que ya existen. Responde con un informe por fila, que también se descarga en CSV. El archivo sigue sin guardarse: corregir y reintentar es volver a subirlo.

**Cómo queda tras las aclaraciones**:
- Cargas revisa en las 10 cargas lo que fija el diccionario de cada columna, y que existan las referencias en el sistema. Las reglas propias de cada servicio dueño, como un coordinador vigente por carrera, las aplica el dueño al confirmar (F01-H04).
- Una referencia existe si ya está en el sistema: registrada a mano o cargada y confirmada antes.
- El informe CSV lo genera el servidor, con las celdas neutralizadas.
- Solo se aceptan los códigos exactos del catálogo; el mapeo de etiquetas queda para F01-H05.

## Clarifications

### Session 2026-10-25

- Q: ¿Qué reglas revisa la carga en cada fila, y dónde viven? → A: Las que fija el diccionario de datos y las referencias:
  - En las 10 cargas, Cargas revisa el tipo, el largo, si es obligatorio, el catálogo, la fecha, el decimal, la cédula con su sugerencia y la clave repetida dentro del archivo.
  - También revisa que existan en el sistema las referencias: carreras, asignaturas, unidades, periodos y docentes.
  - Las reglas propias de cada servicio dueño las aplica el dueño al confirmar la carga (F01-H04, MD-13). Por ejemplo: un coordinador vigente por carrera, la suma de horas de los participantes o el docente activo a tiempo completo de un cargo.
- Q: ¿Contra qué se comprueba que una referencia existe? → A: Contra lo que ya hay en el sistema: lo registrado a mano o cargado y confirmado antes. Es coherente con el orden de las cargas (F01-H05): primero las carreras y después las asignaturas.
- Q: ¿Cómo se descarga el informe de errores en CSV, si el archivo subido no se guarda? → A: Lo genera el servidor. La misma validación responde en CSV cuando se pide, con las celdas neutralizadas contra la inyección de fórmulas (RNF-SEG-04). La pantalla vuelve a enviar el archivo para descargarlo, que tampoco se guarda.
- Q: ¿El mapeo configurable de etiqueta a código (RF-08) va en esta historia? → A: No. Esta historia acepta solo los códigos exactos del catálogo, como dicen las convenciones del diccionario, y el informe dice los valores permitidos. El mapeo editable va con F01-H05 («Configurar las 10 cargas en su orden»).

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Revisar las reglas de cada fila (Priority: P1)

Como quien ejecuta las cargas, quiero que cada fila del archivo se revise con las reglas de su carga, para saber qué datos están mal antes de confirmar (RF-08, RF-07).

**Why this priority**: Sin esta revisión, un dato malo solo se descubriría al confirmar o, peor, en el distributivo.

**Independent Test**: Subir el archivo de docentes de prueba con tres filas alteradas (una cédula sin el cero inicial, una fecha imposible y una dedicación que no existe) y ver las tres en el informe, cada una con su fila y su campo.

**Acceptance Scenarios**:

```gherkin
Escenario: Cédula sin cero inicial
  Dado una fila de docente con una cédula de 9 dígitos a la que le falta el cero inicial
  Cuando se valida la carga
  Entonces la fila se marca "cédula inválida o sin cero inicial"
  Y se sugiere la cédula con el cero inicial
```

1. **Given** el archivo de una carga sin errores, **When** lo subo, **Then** es válido, con su número de registros y sin errores.
2. **Given** una fila con un valor fuera del catálogo, un texto más largo que su columna, una fecha que no es aaaa-mm-dd o no existe, un número que no es número, o un obligatorio vacío, **When** se valida, **Then** cada problema se informa en su fila y su campo, con un motivo que dice qué hacer, y el archivo queda rechazado.
3. **Given** dos filas con la misma clave de la carga, **When** se valida, **Then** la segunda se informa como repetida, y el motivo dice la fila donde está la primera.

---

### User Story 2 - Referencias que no existen (Priority: P1)

Como quien ejecuta las cargas, quiero saber qué filas citan algo que no existe en el sistema, para cargar antes lo que falta o corregir el código (RF-08).

**Why this priority**: Es el segundo escenario del issue y la integridad referencial que pide RF-08.

**Independent Test**: Subir asignaturas que citan una carrera que no está registrada y ver el error en la fila de esa asignatura.

**Acceptance Scenarios**:

```gherkin
Escenario: Referencia inexistente
  Dado una asignatura que cita una carrera que no existe
  Cuando se valida la carga
  Entonces el error se informa en la fila de esa asignatura
```

1. **Given** una fila que cita una carrera, una asignatura, una unidad, un periodo o un docente que no está en el sistema, **When** se valida, **Then** el error se informa en esa fila y ese campo, y el motivo dice qué falta.
2. **Given** una lista de referencias en una celda (como las carreras de un proyecto), **When** una no existe, **Then** el motivo dice cuál.
3. **Given** una unidad de la carga 1 cuyo padre está en el mismo archivo, **When** se valida, **Then** el padre cuenta como existente, como dice el diccionario («mismo archivo»).

---

### User Story 3 - Descargar el informe (Priority: P1)

Como quien ejecuta las cargas, quiero descargar el informe en CSV, para corregir el archivo fila por fila en mi hoja de cálculo (RF-09, RNF-SEG-04).

**Why this priority**: Es el tercer escenario del issue.

**Independent Test**: Validar un archivo con errores y advertencias, descargar el informe y abrirlo: una línea por problema, con fila, campo, valor y motivo, y ninguna celda que empiece con `=`, `+`, `-` o `@` sin neutralizar.

**Acceptance Scenarios**:

```gherkin
Escenario: Informe descargable
  Dado una carga con errores y advertencias
  Cuando descargo el informe
  Entonces es un CSV con el número de fila, el campo, el valor y el motivo
  Y ninguna celda del informe empieza con =, +, - o @ sin neutralizar
```

1. **Given** un valor que empieza con `=`, `+`, `-` o `@` (como `=HYPERLINK(...)`, o un número negativo), **When** se descarga el informe, **Then** esa celda empieza con un apóstrofo y la hoja de cálculo no la ejecuta.
2. **Given** una carga sin errores ni advertencias, **When** se pide el informe, **Then** el CSV trae solo los encabezados.

---

### User Story 4 - Corregir y reintentar (Priority: P1)

Como quien ejecuta las cargas, quiero subir el archivo corregido y que se valide de nuevo desde cero, para no arrastrar el resultado anterior (RF-09).

**Why this priority**: Es el cuarto escenario del issue.

**Acceptance Scenarios**:

```gherkin
Escenario: Corregir y reintentar
  Dado una carga rechazada por errores
  Cuando subo el archivo corregido
  Entonces se valida de nuevo desde cero
```

1. **Given** un archivo rechazado, **When** subo su versión corregida, **Then** el informe es solo el del archivo nuevo. El servidor no guarda nada de un intento a otro.

---

### User Story 5 - Ver el informe en pantalla (Priority: P2)

Como quien ejecuta las cargas, quiero ver en la pantalla de la carga cuántos errores y advertencias tiene el archivo y cuáles son, para decidir si lo corrijo o si ya puede seguir (FR-011).

**Why this priority**: La historia se comprueba en el equipo de demostración por la pantalla.

**Acceptance Scenarios**:

1. **Given** un archivo con errores, **When** lo subo en «Plantillas de carga», **Then** veo que está rechazado, cuántos errores y advertencias tiene y una tabla con la fila, el campo, el valor y el motivo. Hay un botón para descargar el informe completo.
2. **Given** un archivo con solo advertencias, **When** lo subo, **Then** veo que es válido y sus advertencias.
3. **Given** un archivo de varios miles de problemas, **When** lo subo, **Then** la pantalla muestra los primeros 100 y dice cuántos hay. El informe descargado los trae todos.

---

### Edge Cases

Las reglas de cada tipo de columna salen del diccionario de datos (principio XIV):

| Tipo en el diccionario | Regla |
|---|---|
| Texto | Hasta su `longitud`. Sin espacios en los extremos: si los tiene, se quitan con una advertencia. |
| Entero | Solo dígitos, hasta su `longitud`. |
| Decimal | Dígitos con punto decimal (convención «Decimales»), sin separador de miles, hasta su `longitud`. |
| Rango | Si el `formato` de un entero o un decimal es un rango (`1–10`, `0,5–20`, `≥ 0`, `> 0`, `≤ 40`), el valor está dentro de él. Un `formato` que no es un rango, como `TC = 40, MT = 20, TP ≤ 19`, es una regla de coherencia del dueño y no se interpreta aquí. |
| Fecha | aaaa-mm-dd y una fecha que existe (convención «Fechas»). |
| Catálogo | Uno de los valores permitidos de su catálogo o de su `formato`, con el código exacto en mayúsculas. El motivo dice los valores permitidos (o los primeros, si son muchos). |
| S/N | `S` o `N`. |
| Lista (;) | Elementos separados por `;`. Si cada uno tiene partes separadas por `\|`, cada elemento tiene el número de partes del formato. |
| Obligatorio | «Sí»: no puede estar vacío. «No»: puede estarlo. Las 5 condiciones del diccionario («Sí si TITULAR», «Sí si OCASIONAL», «Sí si COORDINADOR_CARRERA», «Sí salvo coordinador de carrera», «Sí si tipo_unidad = GESTION» o «COMISION») se aplican como dicen. |

- **Cédula:** una identificación de tipo CEDULA tiene 10 dígitos y su dígito verificador (módulo 10).
  - Si tiene 9 dígitos y con un 0 delante es válida, se marca «cédula inválida o sin cero inicial» y se sugiere la cédula con el cero.
  - Un pasaporte tiene de 1 a 13 letras mayúsculas o dígitos.
  - El motivo nunca repite la cédula. El informe sí la trae en la columna `valor`, porque es el dato del propio archivo que se corrige.
- **Clave repetida:** la clave de cada carga (`PK` o `PK compuesta` en el diccionario) no se repite dentro del archivo.
- **Referencias:** cada `FK` del diccionario se compara con lo que ya existe en el sistema:
  - las carreras, asignaturas, unidades y periodos, de Académico;
  - los docentes, de Docentes.
  - El padre de una unidad de la carga 1 también puede estar en el mismo archivo.
  - La carga 7 no tiene referencias.
- **Errores y advertencias:**
  - Un **error** impide continuar: el archivo queda rechazado.
  - Una **advertencia** no lo impide. Por ahora hay dos: los espacios en los extremos de un valor, que se quitan, y una fila completamente vacía, que se ignora.
- **Sin estado:** el servidor no guarda el archivo, el informe ni el resultado. Volver a subir es validar desde cero.
- **Tamaño:** el mismo máximo de 5 MB de F01-H02. El informe JSON trae los primeros 100 problemas y el total; el CSV, todos.
- **Datos personales:** el informe se devuelve solo a quien subió el archivo. Ni los logs ni los mensajes de error del servidor llevan los valores de las filas (principio IX).
- **Usuario sin permiso:** validar exige «Cargas: escritura», como subir el archivo en F01-H02 (D-71). Sin él, «Acceso no autorizado» (403).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Al subir el archivo de una carga, el sistema DEBE revisar cada fila con las reglas de su carga del diccionario de datos: el tipo, el largo, si es obligatorio (con sus 5 condiciones), el catálogo, el formato de fecha y de número, y las listas (principio XIV).
- **FR-002**: Una identificación de tipo cédula DEBE tener 10 dígitos y su dígito verificador. A una de 9 dígitos que es válida con un 0 delante, el informe DEBE marcarla «cédula inválida o sin cero inicial» y sugerir la cédula con el cero.
- **FR-003**: La clave de cada carga DEBE ser única dentro del archivo. La fila repetida se informa con la fila de la primera.
- **FR-004**: Cada referencia del diccionario (`FK`) DEBE existir en el sistema; el padre de una unidad de la carga 1 también puede estar en el mismo archivo. Las referencias vienen de Académico y de Docentes por sus puertos publicados (principio V).
- **FR-005**: El resultado DEBE traer, por cada problema, la fila del archivo (contando el encabezado como fila 1), el campo, el valor recibido, el motivo y si es un error o una advertencia. También el total de errores y de advertencias, y si el archivo es válido (sin errores) o está rechazado.
- **FR-006**: Cada motivo DEBE decir qué hacer, en español, y salir de las reglas de su columna. Un valor fuera de catálogo DEBE decir los valores permitidos.
- **FR-007**: El informe DEBE poder pedirse en CSV, con las columnas `fila`, `campo`, `valor`, `motivo` y `tipo`, en UTF-8. Toda celda que empiece con `=`, `+`, `-`, `@`, tabulación o retorno de carro DEBE neutralizarse con un apóstrofo delante (RNF-SEG-04).
- **FR-008**: El servidor NO DEBE guardar el archivo, el informe ni el resultado. Cada envío se valida desde cero (escenario «Corregir y reintentar»).
- **FR-009**: Los errores de estructura de F01-H02 (formato, codificación, encabezados, tamaño, registros) DEBEN seguir respondiendo como hoy, antes de revisar las filas.
- **FR-010**: Validar las filas DEBE exigir «Cargas: escritura», como la subida de F01-H02, validado en el servidor (ADR-021, D-71).
- **FR-011**: La pantalla «Plantillas de carga» DEBE mostrar, al subir un archivo, si es válido o está rechazado, cuántos errores y advertencias tiene y los primeros 100 problemas. También DEBE permitir descargar el informe completo en CSV.
- **FR-012**: Las reglas DEBEN salir del diccionario de datos, en un solo lugar (principio VI). Cuando una regla del diccionario no es leíble por máquina (las 5 condiciones de obligatoriedad, los rangos del formato), su traducción DEBE quedar escrita una sola vez y probada contra el texto del diccionario.

### Key Entities

- **Carga** y **Diccionario de datos**: los de F01-H01 (glosario), con las columnas, sus tipos, sus largos, su obligatoriedad, sus catálogos y sus claves.
- **Informe de validación de la carga**: errores y advertencias por fila, con su número de fila, su campo, su valor y su motivo (glosario, `InformeValidacionCarga`).
- **Problema de una fila**: un error o una advertencia, con su fila, su campo, su valor, su motivo y, si la hay, una sugerencia (como la cédula con el cero inicial).
- **Referencias del sistema**: las carreras, asignaturas, unidades y periodos de Académico, y los docentes de Docentes, que existen en el momento de validar.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los 10 archivos de los datos de prueba, con lo que cada uno cita ya registrado en el sistema, se validan sin ningún error.
- **SC-002**: En el archivo de docentes de prueba con una cédula sin el cero inicial, el informe marca esa fila y sugiere la cédula correcta en el 100 % de las veces.
- **SC-003**: El 100 % de las celdas del CSV descargado que empiezan con `=`, `+`, `-` o `@` salen neutralizadas.
- **SC-004**: Un archivo de 5 MB, el máximo, se valida y responde su informe en menos de 30 segundos.
- **SC-005**: Quien ejecuta la carga encuentra en el informe la fila y el campo de cada problema, y corrige y vuelve a subir el archivo sin ayuda, en menos de 5 minutos para un archivo de la ERS con 3 errores.

## Assumptions

- Las reglas de cada columna son las del diccionario de datos (F01-H01). Si la ERS cambia, cambian allí.
- **Referencias:** hoy, sin F01-H04, en el sistema solo existe lo registrado a mano en las pantallas de Académico y de Docentes. Con F01-H04, también lo cargado y confirmado.
- **Reglas de los dueños:** las que no están en el diccionario como tipo, largo, obligatoriedad, catálogo o clave las aplica el servicio dueño al confirmar (F01-H04, MD-13). La columna `validacion` del diccionario está en prosa y no se interpreta aquí, salvo lo que esta especificación nombra. Por ejemplo: un coordinador por carrera (RN-12), el docente activo a tiempo completo de un cargo, la suma de horas de un proyecto, la cédula con la provincia válida o que el contrato cubra el periodo.
- **Sugerencias:** solo la cédula sin el cero inicial tiene sugerencia; las demás reglas dicen qué hacer en el motivo.
- **Advertencias:** el diccionario no define advertencias. Las dos de esta historia no rechazan ningún dato: avisan de algo que el sistema tolera y ajusta (los espacios en los extremos y una fila vacía), para que el archivo quede limpio la próxima vez. El escenario «Informe descargable» las pide.
- **Fuera de alcance:**
  - la vista previa, la confirmación y el historial (F01-H04);
  - el orden de las 10 cargas, el «sin proyectos» y el mapeo configurable de etiquetas (F01-H05);
  - el tablero de preparación (F02-H02).
