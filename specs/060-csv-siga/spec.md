# Feature Specification: Generar el CSV para SIGA con el layout configurable

**Feature Branch**: `060-csv-siga`

**Created**: 2026-11-22

**Status**: Ready

**Input**: User description: "F19-H01"

**Origen**: issue #266 · historia (`tipo:historia`, `prioridad:must`, `RF-52`, `modulo:08-reporting`) · feature F19 (Obtener el CSV para SIGA, #258) · milestone «Ola 4 · Publicado» · Ola del secuenciador: Incremento 1 · servicio 08-reporting · tamaño M · persona: Gabriela Cevallos. Depende de F20-H02 (#269, el reporte general, spec 059), terminada. La sigue F19-H02 (#267): validar antes de exportar y registrar la exportación.

**Historia (tal como está en el issue)**: Como Secretaria general académica, quiero generar el CSV consolidado por docente del distributivo publicado con el layout del archivo de mapeo, para entregarlo a SIGA con los mismos totales que el reporte general.

**Requerimientos que cubre**:
- **RF-52:** generar el CSV consolidado por docente del distributivo publicado, con un layout configurable (mapeo de columnas y catálogos) que permita adaptarlo al formato de SIGA cuando se conozca. Su criterio en la ERS: «El CSV contiene una fila por docente y actividad (o por docente, según el layout), con totales iguales a los del reporte RF-46.»
- **ADR-002 (D-04, cierre de V-01):** CSV propio con una fila por docente y actividad (clase por asignatura y paralelo, otras de docencia, investigación, vinculación y gestión); convenciones del instructivo de carga masiva de SENESCYT (códigos de catálogo, mayúsculas, «NA» para lo no aplicable, fechas `aaaa-mm-dd`, cédula como texto de 10 dígitos, UTF-8); el nombre, el orden y el catálogo de cada columna salen de un archivo de mapeo versionado, y adaptarlo a SIGA es editar ese archivo, no el código.
- **RNF-COM-02:** los CSV cumplen la RFC 4180, con UTF-8, encabezado obligatorio y fechas ISO 8601.
- **RNF-SEG-04:** prevenir la inyección de fórmulas en los CSV exportados.
- **RF-53 (no lo cubre):** la validación antes de exportar, la suma de verificación y el registro de la exportación son de F19-H02.
- **Glosario:** «Exportación a SIGA: el CSV por docente de la versión publicada, con su suma de verificación y su registro» (`ExportacionSiga`). «Layout de exportación: mapeo configurable de columnas y catálogos del CSV para SIGA» (`LayoutExportacion`). «Componente: clase, otras actividades de docencia, investigación, vinculación o gestión.» «Catálogo de actividades: actividades de los arts. 140 a 143 del RCE, cada una con su componente.» «Asignación: horas de un docente en un componente, con su justificación.» «Carga horaria: total de horas de un docente por componente en la versión.»
- **Mapa de contextos:** Publicación y consulta tiene una capa anticorrupción hacia SIGA: el CSV con el layout de ADR-002, por archivo. Lee la versión publicada solo por llamada de lectura (MD-11).

**Decisiones que aplica**:
- MD-11: publicar bloquea la versión; el CSV para SIGA se genera a pedido de la Secretaría sobre la versión publicada.
- D-01: el prototipo exporta PDF y CSV; no hay integración en línea con SIGA (C-11: no hay API).
- D-16 y D-63: la Secretaría general académica tiene «Publicación: aprobación» y «Reportes: lectura».
- D-115 y D-117 (F20-H01 y F20-H02): Reporting lee la publicación vigente de Aprobación y el contenido congelado por docente de Generación; el reporte general suma los totales institucionales en su dominio.
- D-113 (F18-H02): durante una reforma, la versión publicada anterior sigue vigente hasta que se publique la de la reforma.
- ADR-007 y F12-H01 (spec 038), como precedente: un archivo de configuración con una plantilla descargable comentada en español, que se valida al cargarlo y conserva las versiones anteriores.

**Principios de la constitución**: I, III, IV, V, VI, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- El distributivo del periodo llega a PUBLICADO cuando la Secretaría lo publica (F18-H01). Su versión publicada está congelada, con su hash, y no cambia.
- Al congelar se guarda el contenido por docente: sus datos, sus asignaturas y paralelos, sus actividades y su carga horaria por componente (F20-H01).
- El reporte general (F20-H02) muestra a cada docente de la versión publicada con sus horas por componente y los totales institucionales, en PDF y en CSV. Su CSV está hecho para leerse en una hoja de cálculo, no para SIGA.
- No hay un CSV para SIGA ni un archivo de mapeo.

Esta historia agrega:
- **El CSV para SIGA:** de la versión publicada vigente del periodo, una fila por docente y actividad, con las convenciones de SENESCYT, y sus totales comprobados contra el reporte general.
- **El layout por defecto:** el archivo de mapeo del prototipo, con el nombre, el orden y el catálogo de cada columna.
- **Otro layout sin tocar código:** cargar un archivo de mapeo nuevo, validarlo y que el CSV siguiente salga con sus columnas.

## Clarifications

### Session 2026-11-22

- Q: ¿Quién debe poder cargar un archivo de mapeo nuevo para el CSV de SIGA? → A: Solo la Secretaría general académica, la misma que genera el CSV (por «Publicación: aprobación»).
- Q: ¿Cada CSV para SIGA generado deja ya una entrada en la bitácora en esta historia? → A: Sí, antes de entregarlo, con quién, cuándo, el periodo, la versión publicada y la versión del layout de exportación. El registro `ExportacionSiga` con la suma de verificación sigue siendo de F19-H02.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - La Secretaría genera el CSV para SIGA del distributivo publicado (Priority: P1)

Como Secretaria general académica, quiero generar el CSV para SIGA del distributivo publicado, para entregarlo con una fila por docente y actividad y con los mismos totales que el reporte general.

**Why this priority**: es el primer escenario del issue y el criterio de RF-52. Sin el archivo no hay nada que entregar.

**Independent Test**:
1. Con el distributivo de 2026-I publicado y el layout por defecto, `secretaria.demo` abre la pantalla del periodo y genera el CSV para SIGA.
2. Abre el archivo: tiene una fila por cada asignatura y paralelo de cada docente y una por cada una de sus actividades.
3. Suma las horas por docente y por componente y las compara con el reporte general de la misma versión: son iguales.

**Acceptance Scenarios**:

1. **Dado** el distributivo publicado del periodo, **cuando** genero el CSV para SIGA, **entonces** tiene una fila por docente y actividad, y sus totales por docente y por componente son iguales a los del reporte general. *(issue)*
2. **Dado** un docente con clase en dos paralelos de una asignatura y una actividad de investigación, **cuando** genero el CSV, **entonces** tiene tres filas: una por cada paralelo, con el código de la asignatura y el paralelo, y una de investigación, con el código de la actividad del catálogo y «NA» en la asignatura y el paralelo.
3. **Dado** el CSV generado, **cuando** sumo sus horas, **entonces** la suma de cada docente en cada componente es igual a su fila del reporte general, y la suma de todas las filas en cada componente es igual al total institucional de ese componente.
4. **Dado** el distributivo en reforma, **cuando** genero el CSV, **entonces** sale de la versión publicada vigente, la anterior a la reforma; los cambios en curso de la reforma no aparecen.
5. **Dado** un periodo sin distributivo publicado, **cuando** pido su CSV para SIGA por la API, **entonces** responde con el 409 del catálogo que ya usan los reportes y no genera nada; la pantalla solo lo ofrece para periodos con versión publicada.
6. **Dado** un usuario sin el permiso de generar el CSV para SIGA, por ejemplo el Rector o un coordinador de carrera, **cuando** lo pide, **entonces** recibe el 403 común, y la pantalla no le ofrece la opción.
7. **Dados** la misma versión publicada y el mismo layout, **cuando** genero el CSV dos veces, **entonces** los dos archivos son idénticos, byte a byte.
8. **Dado** un CSV para SIGA generado, **cuando** se entrega, **entonces** antes de entregarlo queda una entrada en la bitácora con quién lo generó, cuándo, el periodo, la versión publicada y la versión del layout de exportación. Si no se puede registrar, el archivo no se entrega.

---

### User Story 2 - El CSV sigue las convenciones de SENESCYT (Priority: P1)

Como Secretaria general académica, quiero que el CSV siga las convenciones de carga masiva de SENESCYT, para que se pueda cargar en SIGA con el menor retrabajo posible.

**Why this priority**: es el segundo escenario del issue y la regla 2 de ADR-002. Un archivo con etiquetas, minúsculas o cédulas sin el cero inicial no sirve para una carga masiva.

**Independent Test**: generar el CSV de la versión publicada de los datos de prueba y revisarlo con un validador de esquema: cada columna cumple su tipo y su formato.

**Acceptance Scenarios**:

1. **Dado** el CSV generado, **cuando** lo reviso, **entonces** usa códigos de catálogo, texto en mayúsculas, «NA» para lo no aplicable, fechas `aaaa-mm-dd`, la cédula como texto de 10 dígitos y UTF-8. *(issue)*
2. **Dada** una cédula que empieza con 0, **cuando** genero el CSV, **entonces** la columna tiene los 10 dígitos, con el 0 inicial.
3. **Dado** un nombre con tildes y eñes, **cuando** genero el CSV, **entonces** sale en mayúsculas y conserva las tildes y las eñes (por ejemplo, «PEÑA ÁLVAREZ JOSÉ»), en UTF-8.
4. **Dadas** las horas de una asignación con decimales, **cuando** genero el CSV, **entonces** salen con punto decimal y dos decimales, sin separador de miles.
5. **Dado** el CSV, **cuando** lo leo con un lector de la RFC 4180, **entonces** tiene encabezado en la primera fila, separa las columnas con coma y pone entre comillas dobles los valores que contienen una coma, una comilla o un salto de línea.
6. **Dado** un dato de un docente que empieza con `=`, `+`, `-`, `@`, una tabulación o un retorno de carro, **cuando** se genera el CSV, **entonces** la celda se neutraliza como en los reportes, para que una hoja de cálculo no la interprete como fórmula.

---

### User Story 3 - Cambiar el layout sin tocar código (Priority: P2)

Como Secretaria general académica, quiero cargar un archivo de mapeo con otro nombre y otro orden de columnas, para adaptar el CSV al formato de SIGA cuando se conozca, sin cambiar el sistema.

**Why this priority**: es el tercer escenario del issue y la regla 3 de ADR-002. Con el layout por defecto el CSV ya se puede entregar; esto lo hace adaptable.

**Independent Test**:
1. Se descarga la plantilla del archivo de mapeo: es el layout por defecto, comentado en español.
2. Se le cambian los nombres de dos columnas, se invierte el orden de otras dos y se quita una opcional; se carga.
3. La Secretaría genera el CSV: sale con las columnas del mapeo nuevo, en su orden, y con los mismos totales.

**Acceptance Scenarios**:

1. **Dado** un archivo de mapeo con otro nombre y otro orden de columnas, **cuando** lo cargo y genero el CSV, **entonces** el CSV sale con las columnas del mapeo nuevo. *(issue)*
2. **Dado** un archivo de mapeo que traduce los códigos de un catálogo (por ejemplo, el componente `CLASE` como `01`), **cuando** genero el CSV, **entonces** esa columna lleva los códigos traducidos.
3. **Dado** un archivo de mapeo con un campo de origen que no existe, dos columnas con el mismo nombre, una columna sin nombre, una traducción de catálogo que no cubre todos sus códigos, o sin las columnas obligatorias (la identificación del docente, el componente y las horas), **cuando** lo cargo, **entonces** se rechaza con un mensaje que dice qué columna y qué regla falla, y el layout vigente no cambia.
4. **Dado** un archivo de mapeo cargado, **cuando** lo consulto, **entonces** veo cuál está vigente, quién lo cargó y cuándo, y las versiones anteriores se conservan.
5. **Dado** que nunca se cargó un archivo de mapeo, **cuando** genero el CSV, **entonces** rige el layout por defecto, y la pantalla lo dice.
6. **Dado** un archivo de mapeo cargado, **cuando** se carga, **entonces** queda una entrada en la bitácora con quién, cuándo y la versión del layout de exportación.

---

### Edge Cases

- **Un docente con solo actividades o solo asignaturas:** tiene solo esas filas; sus totales en los otros componentes son 0, como en el reporte general.
- **Un docente con dos asignaciones de la misma actividad del catálogo** (por ejemplo, dos proyectos de investigación): una fila por asignación; sus horas se suman en los totales.
- **Un docente con pasaporte en lugar de cédula:** la identificación sale como texto, tal como está registrada; la regla de los 10 dígitos aplica solo a la cédula.
- **Un docente sin tipo de identificación** en el contenido congelado (no estaba activo al congelar, o la versión se congeló antes de esta historia): el tipo sale «NA» y la identificación, tal como está registrada.
- **Un dato opcional vacío** (por ejemplo, la dedicación de un docente que no la tiene registrada): sale «NA», como lo no aplicable.
- **Un nombre con una coma o comillas:** va entre comillas dobles y con las comillas internas duplicadas (RFC 4180).
- **Horas con decimales** (hasta dos, D-77): la suma por docente y componente se compara con el reporte general sin redondear cada fila por separado.
- **Los totales no cuadran con el reporte general** (no debería pasar, porque salen del mismo contenido congelado): el CSV no se entrega y responde con un error del catálogo.
- **El contenido guardado no coincide con el hash de la versión publicada:** el CSV no se genera y responde con el error del catálogo, como los reportes.
- **Un docente desactivado o en licencia después de publicar:** sigue en el CSV, porque la versión publicada no cambia.
- **Se carga un layout nuevo mientras alguien genera el CSV:** el archivo en curso sale completo con el layout con el que empezó; el siguiente sale con el nuevo.
- **El layout traduce un catálogo y el contenido trae un código que la traducción no tiene:** no puede pasar, porque al cargar se exige que la traducción cubra todos los códigos de ese catálogo.
- **Un layout sin una columna opcional** (por ejemplo, la fecha de publicación): el CSV sale sin ella; los totales se siguen comprobando con las columnas obligatorias.
- **El proceso del que se lee la versión no responde:** el CSV no se genera, la pantalla lo dice con el error del catálogo y no queda un archivo a medias.

## Requirements *(mandatory)*

### Functional Requirements

**El CSV**

- **FR-001**: El sistema DEBE generar el CSV para SIGA a partir de la versión publicada vigente del periodo elegido, y solo de ella: ni de una versión en curso, ni de una histórica, ni de una congelada que todavía no se publicó.
- **FR-002**: El CSV DEBE tener una fila por docente y actividad: una por cada asignatura y paralelo de clase, y una por cada asignación de otras actividades de docencia, investigación, vinculación o gestión, de todos los docentes de la versión publicada.
- **FR-003**: Las filas DEBEN ir ordenadas por el nombre del docente y su identificación, y dentro de cada docente por componente (clase, otras actividades de docencia, investigación, vinculación y gestión) y por código.
- **FR-004**: El layout por defecto DEBE tener, al menos, estas columnas: el código del periodo, el tipo y el número de identificación del docente, sus apellidos y nombres, su dedicación, el componente, el código de la actividad del catálogo, el código de la carrera, el código de la asignatura, el paralelo, las horas semanales, la versión publicada y su fecha de publicación. Lo que no aplica a una fila (por ejemplo, la asignatura en una actividad de gestión) DEBE salir «NA».
- **FR-005**: Antes de entregar el CSV, el sistema DEBE comprobar que la suma de sus horas por docente y por componente es igual a la del reporte general de la misma versión. Si no lo es, NO DEBE entregar el archivo y DEBE responder con un error del catálogo.
- **FR-006**: Pedir el CSV de un periodo sin versión publicada DEBE responder con el 409 del catálogo que usan los reportes, sin generar nada.
- **FR-007**: Con la misma versión publicada y el mismo layout, el CSV DEBE ser idéntico byte a byte, para que su suma de verificación (F19-H02) sea estable.

**Las convenciones de SENESCYT**

- **FR-008**: Los valores de catálogo (dedicación, componente, actividad, carrera y tipo de identificación) DEBEN salir como códigos, no como etiquetas.
- **FR-009**: El texto DEBE salir en mayúsculas, conservando las tildes y las eñes.
- **FR-010**: La cédula DEBE salir como texto de 10 dígitos, con el 0 inicial; las fechas, como `aaaa-mm-dd`; las horas, con punto decimal, dos decimales y sin separador de miles.
- **FR-011**: El archivo DEBE cumplir la RFC 4180 (encabezado en la primera fila, coma como separador y comillas dobles cuando hacen falta) y estar en UTF-8. El separador y la codificación son fijos; el layout no los cambia.
- **FR-012**: El CSV DEBE neutralizar toda celda que empiece con `=`, `+`, `-`, `@`, una tabulación o un retorno de carro, como los reportes (RNF-SEG-04).

**El layout**

- **FR-013**: El sistema DEBE tener un layout por defecto, que rige mientras no se cargue otro, y ofrecerlo como plantilla descargable, comentada en español, que explique cada columna y qué se puede cambiar.
- **FR-014**: El archivo de mapeo DEBE definir, por cada columna, su nombre en el encabezado, su posición, el campo del que sale (de una lista cerrada de campos disponibles) y, si corresponde, la traducción de los códigos de su catálogo.
- **FR-015**: Al cargar un archivo de mapeo, el sistema DEBE validarlo y rechazarlo, diciendo qué columna y qué regla falla, si: no se puede leer; un campo de origen no existe; dos columnas tienen el mismo nombre; una columna no tiene nombre o su nombre no cumple las reglas de caracteres y largo; una traducción de catálogo no cubre todos sus códigos; o faltan las columnas obligatorias (la identificación del docente, el componente y las horas). Un archivo rechazado NO DEBE cambiar el layout vigente.
- **FR-016**: Un archivo de mapeo válido DEBE quedar vigente para el siguiente CSV que se genere. Las versiones anteriores DEBEN conservarse, con quién las cargó y cuándo.
- **FR-017**: Cargar un archivo de mapeo DEBE dejar una entrada en la bitácora con quién, cuándo y la versión del layout de exportación.
- **FR-018**: Adaptar el CSV a otro formato DEBE ser cargar otro archivo de mapeo, sin cambiar el código ni volver a desplegar.

**Acceso y trazabilidad**

- **FR-019**: Solo quien tiene «Publicación: aprobación» en la matriz D-16, es decir, la Secretaría general académica, DEBE poder generar el CSV para SIGA. Cualquier otro usuario DEBE recibir el 403 común.
- **FR-020**: Solo la Secretaría general académica, por «Publicación: aprobación», DEBE poder cargar un archivo de mapeo, descargar la plantilla y consultar el layout vigente y los anteriores (aclaración del 2026-11-22). Cualquier otro usuario, el Administrador incluido, DEBE recibir el 403 común.
- **FR-021**: Cada CSV generado DEBE dejar una entrada en la bitácora con quién, cuándo, el periodo, la versión publicada y la versión del layout de exportación, registrada antes de entregar el archivo, como en los reportes (aclaración del 2026-11-22). Si la entrada no se puede registrar, el archivo NO DEBE entregarse. Esta entrada no es la `ExportacionSiga` con la suma de verificación, que es de F19-H02.
- **FR-022**: Reporting NO DEBE cambiar nada del distributivo, de la versión ni del proceso de aprobación: solo lee.

**Pantalla y errores**

- **FR-023**: La pantalla DEBE ofrecer a quien tiene el permiso, para el periodo elegido con versión publicada, «Generar CSV para SIGA», y decir con qué layout se generará (el por defecto o la versión cargada, con su fecha).
- **FR-024**: La pantalla DEBE permitir a la Secretaría general académica descargar la plantilla, cargar un archivo nuevo, ver el resultado de la validación y consultar el layout vigente y los anteriores.
- **FR-025**: Las pantallas DEBEN cumplir WCAG 2.1 AA.
- **FR-026**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII). Si la versión publicada no se puede leer, si su contenido no coincide con su hash o si los totales no cuadran, el CSV DEBE fallar con un error del catálogo, sin entregar un archivo incompleto.

### Key Entities

- **Versión publicada** (de Distributivo y Aprobación, existente): la versión congelada que llegó a PUBLICADO y es la vigente del periodo. Reporting la lee; no la cambia.
- **Contenido congelado por docente** (de Distributivo, existente desde F20-H01): por cada docente, sus datos, sus asignaturas y paralelos, sus actividades y su carga horaria por componente. Desde esta historia guarda también el tipo de identificación, que da Docentes al congelar.
- **Reporte general** (de Publicación y consulta, existente desde F20-H02): la referencia de los totales por docente y por componente.
- **Layout de exportación** (`LayoutExportacion`, nuevo): el mapeo de columnas y catálogos del CSV para SIGA. Hay uno por defecto; cada archivo de mapeo cargado es una versión nueva, con quién la cargó y cuándo. La última válida es la vigente.
- **Columna del layout** (nueva, parte del layout): su nombre, su posición, su campo de origen y, si tiene, la traducción de los códigos de su catálogo.
- **CSV para SIGA** (nuevo, de Publicación y consulta): el archivo de una versión publicada con un layout, una fila por docente y actividad. Se genera a pedido; en esta historia no se guarda, pero cada generación queda en la bitácora. Su registro como **Exportación a SIGA** (`ExportacionSiga`), con la suma de verificación, es de F19-H02.
- **Entrada de bitácora** (existente): la de cada carga de un archivo de mapeo y la de cada CSV generado, con su propia acción para distinguirlas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con la versión publicada de los datos de prueba, el CSV tiene el 100 % de las asignaturas y paralelos y de las actividades de la versión, cada una en una sola fila.
- **SC-002**: Para el 100 % de los docentes, la suma de sus horas en el CSV es igual a su fila del reporte general en cada componente, y la suma de todas las filas es igual a cada total institucional, comparado de forma automática.
- **SC-003**: El 100 % de las filas del CSV de los datos de prueba pasan un validador de esquema con las convenciones de SENESCYT y la RFC 4180: códigos de catálogo, mayúsculas, «NA», fechas `aaaa-mm-dd`, cédula de 10 dígitos y UTF-8.
- **SC-004**: Con un archivo de mapeo que cambia nombres y orden de columnas, el CSV siguiente sale con las columnas nuevas sin cambiar el código ni volver a desplegar, y con los mismos totales.
- **SC-005**: El 100 % de los archivos de mapeo inválidos de las pruebas (uno por cada regla de FR-015) se rechazan con un mensaje que dice la columna y la regla, y el layout vigente no cambia.
- **SC-006**: Dos CSV de la misma versión y el mismo layout son idénticos byte a byte.
- **SC-007**: Ninguna celda del CSV se interpreta como fórmula en una hoja de cálculo, probado con datos que empiezan con cada uno de los caracteres peligrosos.
- **SC-008**: La Secretaría genera el CSV en menos de 1 minuto desde que abre la pantalla; la generación tarda menos de 10 segundos con un distributivo de 300 docentes.
- **SC-009**: Ningún rol sin el permiso obtiene el CSV ni carga un archivo de mapeo, por la pantalla ni por la API.
- **SC-010**: Las pantallas no tienen fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **Qué versión:** solo la versión publicada vigente del periodo, como los reportes y como dice el escenario del issue («Dado el distributivo publicado del periodo»). Durante una reforma, la vigente es la publicada anterior (D-113).
- **Una fila por docente y actividad:** la regla 1 de ADR-002. «Actividad» se lee como cada asignatura y paralelo de clase y cada asignación de los otros componentes. La variante de una fila por docente («o por docente, según el layout», RF-52) necesita un mapeo de agregación que ADR-002 deja para cuando SIGA lo exija.
- **Los totales:** el CSV no lleva filas de totales, porque es un archivo para una carga masiva. Los totales se obtienen sumando sus filas, y el sistema los comprueba contra el reporte general antes de entregar el archivo.
- **Quién genera:** la Secretaría general académica, por «Publicación: aprobación» (D-16). MD-11 dice que el CSV se genera «a pedido de la Secretaría». Los demás roles con «Reportes: lectura» ya tienen el CSV del reporte general. La misma Secretaría carga el archivo de mapeo, porque el escenario del issue lo pone en el mismo actor («cuando lo cargo y genero el CSV») y es quien entrega el archivo a SIGA; a diferencia de la configuración del flujo (F12-H01), no lo carga el Administrador. No hace falta cambiar la matriz.
- **El formato del archivo:** la RFC 4180 con coma y punto decimal (RNF-COM-02 y las convenciones del diccionario de datos), no el `;` y la coma decimal del CSV de los reportes (D-115), que está hecho para abrirse en una hoja de cálculo. El separador, la codificación y la neutralización de fórmulas no son configurables.
- **El archivo de mapeo:** sigue el precedente de la configuración del flujo (F12-H01): un archivo de texto con una plantilla descargable comentada en español, que se valida al cargarlo y conserva las versiones anteriores. Es uno para la institución, no uno por periodo; el formato exacto lo decide el plan.
- **Los catálogos:** sin traducción, cada catálogo sale con el código interno del sistema (por ejemplo, `TC`, `CLASE` o `140-B`), en mayúsculas como todo el texto (FR-009). La traducción a los códigos de SIGA se agrega en el archivo de mapeo cuando se conozcan. En esta historia, el archivo de mapeo admite traducción solo en el tipo de identificación, la dedicación y el componente, cuyos códigos fija el sistema. La de las actividades y las carreras queda para cuando se conozca el formato de SIGA (D-119).
- **El layout no es el oficial de SIGA:** es la limitación declarada de ADR-002. Nadie entrega el formato de importación de SIGA; el CSV se carga en SIGA fuera del MDD.
- **El tamaño:** el prototipo trabaja con decenas de docentes por periodo en los datos de prueba; SC-008 pide un margen de 300 docentes, como el reporte general.
- **La bitácora:** cada CSV generado queda registrado antes de entregarse, como las descargas de los reportes (D-115), con su propia acción. Lo que añade F19-H02 es el registro de la exportación (`ExportacionSiga`) con la suma de verificación y la validación previa; no repite esta entrada.
- **Datos ficticios:** las identificaciones y los nombres de la demo son ficticios (principio IX).
- **Fuera de alcance:**
  - validar que la versión esté firmada y sin inconsistencias antes de exportar, calcular la suma de verificación y registrar la exportación (`ExportacionSiga`) con el usuario, la fecha y la versión (F19-H02, RF-53); la entrada de la bitácora de cada CSV generado sí es de esta historia (FR-021);
  - el mapeo de agregación con una fila por docente (ADR-002, consecuencias);
  - la traducción de los códigos de actividad y de carrera en el archivo de mapeo;
  - enviar el archivo a SIGA: no hay API (C-11);
  - la matriz para la coordinación zonal o SENESCYT y la alerta de los 30 días (RF-54, W);
  - el XLSX (D-01);
  - el CSV de versiones históricas o de una versión que todavía no se publicó.
