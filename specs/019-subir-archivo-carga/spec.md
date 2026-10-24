# Feature Specification: Subir un archivo y validar su estructura

**Feature Branch**: `019-subir-archivo-carga`

**Created**: 2026-10-23

**Status**: Draft

**Input**: User description: "Especifica la historia F01-H02 · Subir un archivo y validar su estructura (issue #28), a partir de RF-06, RF-07, RF-55, RF-57, el diccionario de datos de F01-H01 y sus convenciones."

**Origen**: issue #28 · F01-H02 · sub-issue de F01 (Cargar los 10 archivos con validación y vista previa) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:03-cargas`, `RF-06`, `RF-07` · tamaño propuesto M · depende de #27 (F01-H01), cerrada.

**Requerimientos que cubre**: RF-06 (subir archivos CSV o XLSX; criterio: «al cargar un archivo válido se muestra el número de registros leídos; un archivo de otro formato se rechaza con un mensaje»), RF-07 (validar la estructura de cada archivo; criterio: «un archivo con una columna faltante se rechaza indicando qué columna falta»), RF-55 (un botón de carga por entidad, con la plantilla de RF-05) y RF-57 (el archivo es una fuente de datos detrás de una interfaz, para que otra fuente no cambie las validaciones). **Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-70 (permisos alternativos), ADR-016 (catálogo de errores), ADR-021 (permisos). **Principios de la constitución**: II (Ingesta de datos es de soporte), IV (API primero), VI (las reglas son datos), VII (trazabilidad) y IX (mínimo privilegio, OWASP, solo datos ficticios).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero subir un CSV o XLSX de una carga y que se valide su estructura, para saber de inmediato si el archivo se puede leer.

**Punto de partida.** El MDD ya ofrece la plantilla y el diccionario de datos de las 10 cargas (F01-H01), con sus convenciones: UTF-8, coma o punto y coma, encabezados en minúsculas y en el orden de la plantilla, o XLSX con una hoja. Pero todavía no recibe archivos. Esta historia agrega la subida de un archivo por carga y responde de inmediato si se puede leer: cuántos registros tiene o por qué se rechaza. Revisar el contenido de cada fila, mostrar la vista previa y aplicar la carga vienen después (F01-H03 y F01-H04).

**Cómo queda tras las aclaraciones**: suben los archivos quienes tienen «Cargas: escritura»: Talento Humano y, con una decisión nueva (D-71), el Responsable de consolidación y el Vicerrector, que inician el proceso (ERS §8.3). Esta historia revisa solo la estructura del archivo; el archivo no se guarda y no se elige periodo.

## Clarifications

### Session 2026-10-23

- Q: ¿Quién sube los archivos y con qué permiso, si la historia la pide el Responsable de consolidación pero «Cargas: escritura» solo la tiene Talento Humano? → A: Quien tenga «Cargas: escritura». Como dice la ERS §8.3 («el rol que inicia el proceso, por defecto el Vicerrectorado o el Responsable de consolidación, ejecuta las cargas» y delega en Talento Humano), el Responsable de consolidación y el Vicerrector académico reciben «Cargas: lectura» y «Cargas: escritura». Es un cambio de la matriz D-65: una decisión nueva (D-71), que el plan propone.
- Q: ¿Qué parte de RF-07 entra en esta historia? → A: Solo la estructura del archivo: formato, codificación, separador, encabezados, hojas, tamaño, archivo vacío y número de registros, como piden los cuatro escenarios del issue. Los tipos, las longitudes y los obligatorios de cada fila van con F01-H03, junto al informe por fila.
- Q: ¿El archivo subido se guarda, y se sube para un periodo? → A: No se guarda: se lee, se valida y se responde. No se elige periodo. Guardar el archivo para la vista previa, el historial con su hash y el periodo de las cargas del periodo (3, 5, 9 y 10) es de F01-H04, que es donde se usan.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Subir un archivo válido (Priority: P1)

Como quien ejecuta las cargas, quiero subir el archivo de una carga, en CSV o XLSX, y ver cuántos registros se leyeron, para saber que el archivo se puede usar (RF-06).

**Why this priority**: Es el primer escenario del issue y la entrada de todas las cargas: sin leer el archivo no hay validación, vista previa ni confirmación.

**Independent Test**: Subir el archivo de docentes de los datos de prueba, en CSV y en XLSX; el sistema responde que es válido y cuántos registros tiene.

**Acceptance Scenarios**:

```gherkin
Escenario: Archivo válido
  Dado la plantilla de docentes completa
  Cuando subo el archivo en CSV o XLSX
  Entonces el sistema muestra el número de registros leídos
```

1. **Given** el archivo de una carga con los encabezados de su plantilla y sus filas, en CSV con coma o con punto y coma, **When** lo subo en esa carga, **Then** el sistema responde que se puede leer y muestra el número de registros (las filas de datos, sin el encabezado ni las filas vacías del final).
2. **Given** el mismo contenido en XLSX, con una hoja, **When** lo subo, **Then** el resultado es el mismo número de registros.
3. **Given** un CSV guardado desde la plantilla descargada (con su marca de UTF-8 al inicio), **When** lo subo, **Then** se lee igual que uno sin esa marca.

---

### User Story 2 - Rechazar un archivo que no se puede leer (Priority: P1)

Como quien ejecuta las cargas, quiero que un archivo con otro formato, otra codificación o encabezados distintos se rechace con un mensaje que diga qué corregir, para arreglarlo sin adivinar (RF-06, RF-07).

**Why this priority**: Son los otros tres escenarios del issue. Un archivo mal formado que pasara llegaría roto a las cargas siguientes.

**Independent Test**: Subir un PDF, un CSV en Windows-1252 y un archivo de carreras sin la columna `codigo_carrera`; cada uno se rechaza con su motivo en español.

**Acceptance Scenarios**:

```gherkin
Escenario: Formato no admitido
  Dado un archivo que no es CSV ni XLSX
  Cuando lo subo
  Entonces se rechaza con un mensaje que indica los formatos admitidos

Escenario: Columna faltante
  Dado un archivo de carreras sin la columna de código SENESCYT
  Cuando lo subo
  Entonces se rechaza indicando qué columna falta

Escenario: Codificación incorrecta
  Dado un CSV que no está en UTF-8
  Cuando lo subo
  Entonces se rechaza indicando la codificación esperada
```

1. **Given** un archivo que no es CSV ni XLSX (por ejemplo, un PDF o un XLS antiguo), **When** lo subo, **Then** se rechaza diciendo que solo se admiten CSV y XLSX.
2. **Given** un archivo al que le falta una columna de la plantilla, **When** lo subo, **Then** se rechaza nombrando cada columna que falta.
3. **Given** un archivo con una columna que la plantilla no tiene, o con las columnas en otro orden, **When** lo subo, **Then** se rechaza nombrando la columna sobrante o la primera fuera de lugar.
4. **Given** un CSV que no está en UTF-8, **When** lo subo, **Then** se rechaza diciendo que se espera UTF-8.
5. **Given** un archivo sin registros (solo el encabezado, o vacío), **When** lo subo, **Then** se rechaza diciendo que no tiene registros.
6. **Given** un XLSX con más de una hoja, **When** lo subo, **Then** se rechaza diciendo que debe tener una sola hoja.

---

### User Story 3 - Subir desde la pantalla (Priority: P1)

Como quien ejecuta las cargas, quiero un botón «Subir archivo» en cada carga, en la pantalla de las cargas, para elegir el archivo y ver el resultado sin salir de ella (RF-55).

**Why this priority**: Sin pantalla, subir un archivo exigiría llamar al servidor.

**Independent Test**: En la pantalla, pulsar «Subir archivo» en «Docentes contratados», elegir el CSV de prueba y ver «47 registros leídos»; con un archivo de carreras sin una columna, ver el motivo.

**Acceptance Scenarios**:

1. **Given** quien tiene el permiso de subir, **When** abre la pantalla de las cargas, **Then** cada una de las 10 tiene «Subir archivo», junto a su plantilla y su diccionario.
2. **Given** un archivo elegido, **When** lo subo, **Then** veo el nombre del archivo y el número de registros leídos, o el motivo del rechazo.
3. **Given** quien solo puede descargar las plantillas, **When** abre la pantalla, **Then** no ve «Subir archivo».

---

### Edge Cases

- **Separador**: coma (RFC 4180) o punto y coma, como dicen las convenciones; el sistema lo detecta en la fila de encabezados. Otro separador no deja reconocer los encabezados y se rechaza por columnas faltantes.
- **Comillas**: un valor entre comillas dobles puede tener el separador, comillas duplicadas o saltos de línea; no parte la fila. Unas comillas sin cerrar hacen que el archivo no se pueda leer.
- **Encabezados**: se comparan como los escribe la plantilla, en minúsculas; los espacios al inicio o al final se ignoran. Una columna repetida se rechaza.
- **Filas con otro número de columnas**: una fila de datos con más o menos valores que el encabezado hace que el archivo no se pueda leer, con su número de fila.
- **Filas vacías**: las del final no cuentan como registros; una fila vacía entre registros sí se cuenta y su contenido lo revisa F01-H03.
- **Tamaño**: un archivo de más de 5 MB se rechaza con el tamaño máximo. Los datos de prueba más grandes son de decenas de kilobytes.
- **XLSX**: la hoja tiene los encabezados en la primera fila; las celdas de fecha o número se leen como el texto que muestran, para no perder el 0 inicial de una cédula.
- **El nombre del archivo** no importa: lo que cuenta es la carga en que se sube.
- **Contenido de las filas** (tipos, longitudes, obligatorios, catálogos, duplicados): no se revisa en esta historia. Va con F01-H03, junto al informe por fila (aclaración 2).
- **Usuario sin permiso**: quien no tiene «Cargas: escritura» no ve el botón, y el servidor responde «Acceso no autorizado» (403). Quien solo puede descargar las plantillas (por ejemplo, un coordinador) sigue viéndolas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE recibir un archivo para una de las 10 cargas del diccionario de datos y responder si se puede leer.
- **FR-002**: El sistema DEBE admitir CSV y XLSX, y rechazar cualquier otro formato diciendo cuáles se admiten.
- **FR-003**: Un CSV DEBE estar en UTF-8, con o sin la marca de UTF-8 al inicio; si no lo está, se rechaza diciendo que se espera UTF-8.
- **FR-004**: El sistema DEBE detectar si el CSV usa coma o punto y coma, y leer las comillas dobles según RFC 4180.
- **FR-005**: Los encabezados DEBEN ser exactamente las columnas de la plantilla de la carga, en su orden; si no, el archivo se rechaza nombrando las columnas que faltan, las que sobran, las repetidas o la primera fuera de lugar.
- **FR-006**: Un XLSX DEBE tener una sola hoja, con los encabezados en la primera fila, y leerse con las mismas reglas que un CSV.
- **FR-007**: Un archivo válido DEBE informar el número de registros leídos; un archivo sin registros se rechaza.
- **FR-008**: Un archivo de más de 5 MB DEBE rechazarse diciendo el tamaño máximo.
- **FR-009**: Cada rechazo DEBE decir el motivo en español, con un código del catálogo de errores (ADR-016), y no dejar nada guardado a medias.
- **FR-010**: La lectura del archivo DEBE quedar detrás de una interfaz de fuente de datos, para que otra fuente (Moodle, SIGA) pueda entregar los mismos registros sin cambiar las validaciones (RF-57).
- **FR-011**: Subir un archivo DEBE exigir un permiso, validado en el servidor (ADR-021). Lo DEBE poder hacer quien tenga «Cargas: escritura»: Talento Humano y, con D-71, el Responsable de consolidación y el Vicerrector académico (aclaración 1); sin él, 403.
- **FR-012**: La pantalla de las cargas DEBE ofrecer «Subir archivo» en cada carga a quien tenga el permiso, y mostrar el número de registros leídos o el motivo del rechazo.
- **FR-013**: El archivo NO DEBE guardarse: se lee, se valida y se descarta, y la subida no pide periodo (aclaración 3). Por eso no deja registro: no cambia ningún dato (principio VII); el historial de las cargas confirmadas es de F01-H04.

### Key Entities

- **Carga**: una de las 10 entidades del diccionario de datos (glosario, `Carga`); el archivo se sube en una de ellas.
- **El archivo de una carga**: el CSV o XLSX que se sube, con su nombre, su formato y su tamaño; es la fuente de datos de la carga (el glosario define «Carga» como ese archivo; aquí no se le da otro nombre).
- **Resultado de la lectura**: si el archivo se puede leer y cuántos registros tiene, o el motivo del rechazo.
- **Plantilla de carga** y **Diccionario de datos**: las columnas esperadas y su orden (F01-H01).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quien ejecuta las cargas sube el archivo de docentes de los datos de prueba (47 registros) y ve el número de registros leídos en menos de 5 segundos desde que lo elige.
- **SC-002**: El 100 % de los archivos de prueba de las 10 cargas, en CSV y en XLSX, se leen con su número de registros correcto.
- **SC-003**: El 100 % de los archivos con otro formato, otra codificación, una columna que falta o sobra, o sin registros se rechazan con su motivo, también llamando directamente al servidor.
- **SC-004**: El 100 % de los intentos de subir sin el permiso se rechazan en el servidor.

## Assumptions

- Las 10 cargas, sus columnas y su orden son los del diccionario de datos de F01-H01; no se repiten aquí.
- Se sube un archivo por vez; «uno o varios archivos» de RF-06 se cumple subiendo varios, uno tras otro.
- 5 MB alcanza para una institución del tamaño del caso: los datos de prueba tienen decenas de filas por carga.
- Las celdas de un XLSX se leen como texto, como las de un CSV; convertir tipos es de F01-H03.
- Fuera de alcance: las reglas de los datos y el informe por fila (F01-H03), la vista previa, la confirmación y el historial (F01-H04), el orden de las cargas y el tablero (F01-H05, F02-H02), el adaptador de Moodle o SIGA.
