# Feature Specification: Reporte individual del docente en PDF y CSV

**Feature Branch**: `058-reporte-individual-docente`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "F20-H01"

**Origen**: issue #268 · historia (`tipo:historia`, `prioridad:must`, `RF-45`, `RF-48`, `modulo:08-reporting`) · feature F20 (Reportes general e individual en PDF y CSV, #259) · milestone «Ola 4 · Publicado» · servicio 08-reporting · tamaño M. Depende de F18-H01 (#71, publicar el distributivo), terminada. F20-H02 (#269, el reporte general) depende de esta historia. Es la primera historia de Reporting: hasta ahora el módulo solo tiene su esqueleto y su esquema vacío.

**Historia (tal como está en el issue)**: Como Rector, quiero obtener el reporte individual de distributivo de cada docente en PDF y en CSV, para presentar y revisar la carga de un docente.

**Requerimientos que cubre**:
- **RF-45:** generar el reporte individual de distributivo por docente. Su criterio en la ERS: «El reporte lista asignaturas, paralelos, actividades y horas por componente, y su total.»
- **RF-48:** exportar los reportes en PDF no editable y CSV; el XLSX queda fuera del prototipo (D-01). Su criterio: «Los dos formatos contienen los mismos datos.»
- **RNF-SEG-04:** prevenir la inyección de fórmulas en los CSV exportados.
- **Glosario:** «Reporte individual: distributivo de un docente: asignaturas, paralelos, actividades y horas por componente, en PDF y CSV» (Publicación y consulta). «Componente: clase, otras actividades de docencia, investigación, vinculación o gestión.» «Carga horaria: total de horas de un docente por componente en la versión.»
- **Mapa de contextos:** Publicación y consulta es conformista con Distributivo, Aprobación, Firma y Datos maestros: lee la versión publicada y los datos del docente, solo por llamada de lectura (MD-11).

**Decisiones que aplica**:
- D-01: reportería básica; reportes filtrados, XLSX, tablero e historial del docente quedan fuera (W).
- D-16 y D-63: la funcionalidad «Reportes» con permiso de lectura, que tienen el Rector, el Vicerrector académico, la Secretaría general académica, el Miembro del OCS y el Administrador.
- F18-H01 (spec 055): la versión publicada es la oficial del periodo, la que leen la consulta y las exportaciones.
- F18-H02 (spec 056, aclaración del 2026-11-20): durante una reforma, la versión publicada anterior sigue siendo la vigente hasta que se publique la de la reforma.
- F13-H02 (spec 046): el precedente del PDF/A-2b verificado con veraPDF y del pie con la versión y su hash de contenido.

**Principios de la constitución**: II, III, IV, V, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- El distributivo del periodo llega a PUBLICADO cuando la Secretaría general académica lo publica (F18-H01). Su versión publicada está congelada, con su hash de contenido, y es inmutable.
- La versión congelada ya tiene, por docente, sus asignaciones (asignaturas y paralelos que dicta, actividades no lectivas) y su carga horaria por componente.
- Durante la cadena de firmas y la revisión del OCS ya existe el PDF/A del consolidado, con el detalle por docente (F13-H02). No hay ningún reporte sobre la versión publicada.
- Reporting no tiene pantallas, operaciones ni datos propios.

Esta historia agrega:
- **El reporte individual:** para un docente de la versión publicada vigente del periodo, sus asignaturas y paralelos, sus actividades y sus horas por componente, con su total.
- **Las dos descargas:** el mismo reporte en PDF no editable y en CSV, con los mismos datos.
- **La pantalla «Reportes»:** para quien tiene «Reportes: lectura».

## Clarifications

### Session 2026-11-21

- Q: ¿De qué versión del distributivo sale el reporte individual: solo de la publicada, o también de la congelada en firma o en revisión del OCS? → A: Solo de la versión publicada vigente; durante una reforma, de la publicada anterior. Para la revisión del OCS está el PDF/A del consolidado (F13-H02).
- Q: ¿Qué tiene que cumplir el PDF para considerarse «no editable» (RF-48)? → A: PDF/A-2b sin campos de formulario ni contenido activo, con el periodo, la versión publicada y su hash de contenido en el pie de cada página. Sin cifrado.
- Q: ¿Cada descarga de un reporte individual (PDF o CSV) deja una entrada en la bitácora? → A: Sí, cada descarga, en los dos formatos: quién, cuándo, el periodo, la versión, el docente y el formato. Ver el reporte en pantalla no se registra.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Rector consulta el reporte individual de un docente (Priority: P1)

Como Rector, quiero pedir el reporte de un docente del distributivo publicado, para revisar su carga: qué dicta, qué otras actividades tiene y cuántas horas suma en cada componente.

**Why this priority**: es el primer escenario del issue y el criterio de RF-45. Sin el contenido del reporte no hay nada que descargar.

**Independent Test**:
1. Con el distributivo de 2026-I publicado, `rectorado.demo` abre «Reportes».
2. Elige el periodo y busca a un docente por su nombre.
3. Ve su reporte: sus asignaturas y paralelos, sus actividades, sus horas por componente y su total, con la versión publicada de la que salen.

**Acceptance Scenarios**:

1. **Dado** el distributivo publicado del periodo, **cuando** pido el reporte de un docente, **entonces** lista sus asignaturas, paralelos, actividades y horas por componente, con su total. *(issue)*
2. **Dado** el reporte de un docente, **cuando** lo leo, **entonces** dice el periodo, la versión publicada, su hash de contenido y la fecha de publicación, y los datos del docente: su nombre, su identificación y su dedicación.
3. **Dado** un docente que dicta asignaturas en más de una carrera o tiene actividades en otras unidades, **cuando** pido su reporte, **entonces** incluye todas sus asignaciones de la versión, cada una con su carrera o su unidad, y los totales por componente las suman todas.
4. **Dado** el reporte de un docente, **cuando** comparo sus horas por componente y su total con su carga horaria en la versión publicada, **entonces** son las mismas, sin redondeos distintos.
5. **Dado** un periodo sin distributivo publicado, **cuando** pido su reporte por la API, **entonces** responde con un 409 del catálogo y no genera nada; la pantalla solo ofrece los periodos con versión publicada y, si no hay ninguno, dice que todavía no hay un distributivo publicado.
6. **Dado** el distributivo en reforma, **cuando** pido el reporte de un docente, **entonces** sale de la versión publicada vigente, la anterior a la reforma, y lo dice; los cambios en curso de la reforma no aparecen.
7. **Dado** un docente que no está en la versión publicada del periodo, **cuando** pido su reporte por la API, **entonces** recibo un 404 del catálogo que dice que ese docente no tiene distributivo en la versión publicada.
8. **Dado** un usuario sin «Reportes: lectura», por ejemplo un coordinador de carrera o un docente, **cuando** pide un reporte individual, **entonces** recibe el 403 común y la pantalla «Reportes» no aparece en su menú.

---

### User Story 2 - El Rector descarga el reporte en PDF y en CSV con los mismos datos (Priority: P1)

Como Rector, quiero descargar el reporte de un docente en PDF y en CSV, para presentarlo como un documento que no se puede alterar y para revisarlo en una hoja de cálculo.

**Why this priority**: es el segundo escenario del issue y el criterio de RF-48. El reporte sirve para presentar la carga de un docente fuera del sistema.

**Independent Test**:
1. Con el reporte de un docente en pantalla, `rectorado.demo` descarga el PDF y el CSV.
2. Abre el CSV en una hoja de cálculo y compara cada fila y cada total con el PDF: son los mismos.
3. El PDF pasa la verificación de PDF/A y no tiene campos que se puedan llenar.

**Acceptance Scenarios**:

1. **Dado** el reporte individual de un docente, **cuando** lo descargo en PDF y en CSV, **entonces** los dos contienen los mismos datos **y** el PDF no es editable. *(issue)*
2. **Dado** el PDF descargado, **cuando** se verifica, **entonces** cumple PDF/A-2b, no tiene campos de formulario ni contenido activo, y cada página muestra en el pie el periodo, la versión publicada y su hash de contenido.
3. **Dado** el CSV descargado, **cuando** lo abro en una hoja de cálculo, **entonces** cada asignación del PDF es una fila, con su carrera o unidad, su asignatura y paralelo o su actividad, su componente y sus horas, y los totales por componente y el total del docente están en filas marcadas como totales, con los mismos valores del PDF.
4. **Dado** un dato del docente o de una asignación que empieza con `=`, `+`, `-`, `@`, una tabulación o un retorno de carro, **cuando** se genera el CSV, **entonces** la celda se neutraliza para que la hoja de cálculo no la interprete como fórmula, y el PDF muestra el mismo texto.
5. **Dado** nombres con tildes y eñes, **cuando** abro el CSV en una hoja de cálculo, **entonces** se leen bien, sin caracteres extraños.
6. **Dado** el mismo docente y la misma versión publicada, **cuando** descargo el reporte dos veces, **entonces** los datos son idénticos; solo cambian la fecha y la persona que lo generó.
7. **Dado** una descarga de un reporte, **cuando** se completa, **entonces** queda una entrada en la bitácora con quién lo descargó, cuándo, el periodo, la versión, el docente y el formato.
8. **Dado** los archivos descargados, **cuando** miro su nombre, **entonces** dice que es el reporte individual, el periodo y el docente, sin espacios ni caracteres que un sistema de archivos rechace.

---

### Edge Cases

- **Un docente con solo actividades no lectivas** (sin asignaturas): el reporte lista sus actividades y la sección de asignaturas dice que no tiene; el CSV no tiene filas de asignaturas.
- **Un docente con solo asignaturas:** lo mismo con las actividades.
- **Un componente sin horas:** aparece en los totales con 0, en el PDF y en el CSV, para que los dos tengan siempre los cinco componentes.
- **Horas con decimales** (hasta dos, D-77): se muestran con dos decimales en el PDF y en el CSV, sin redondear el total por separado.
- **Un docente desactivado después de publicar:** su reporte se sigue generando, porque la versión publicada no cambia.
- **Un docente con un nombre muy largo o una asignatura con un nombre muy largo:** el PDF lo parte en varias líneas sin cortar el texto ni salirse de la página.
- **Un docente con muchas asignaciones:** el PDF pasa a una página nueva y repite los encabezados de la tabla; el pie con la versión y el hash está en cada página.
- **Dos periodos con distributivo publicado:** cada reporte sale del periodo elegido; la pantalla propone por defecto el de la publicación más reciente.
- **La reforma se publica mientras el Rector mira un reporte:** el reporte que ya ve dice su versión; el siguiente que pida sale de la nueva versión publicada.
- **Un usuario con «Reportes: lectura» que además es docente:** puede pedir el reporte de cualquier docente, por su permiso de reportes; la consulta de su propio distributivo como docente es otra historia (F21).
- **El proceso del que Reporting lee la versión no responde:** el reporte no se genera, la pantalla lo dice con el error del catálogo y no queda ningún archivo a medias.

## Requirements *(mandatory)*

### Functional Requirements

**El reporte**

- **FR-001**: El sistema DEBE generar el reporte individual de un docente a partir de la versión publicada vigente del periodo elegido, y solo de ella: ni de una versión en curso, ni de una histórica, ni de una congelada que todavía no se publicó.
- **FR-002**: El reporte DEBE incluir el periodo, la versión publicada, su hash de contenido y su fecha de publicación; los datos del docente (nombre, identificación y dedicación); y quién lo generó y cuándo.
- **FR-003**: El reporte DEBE listar las asignaturas y paralelos que dicta el docente en la versión, cada uno con su carrera y sus horas de clase, y sus actividades no lectivas, cada una con su componente, su unidad o proyecto cuando lo tiene, y sus horas.
- **FR-004**: El reporte DEBE mostrar las horas del docente en cada uno de los cinco componentes (clase, otras actividades de docencia, investigación, vinculación y gestión), incluidos los que suman 0, y su total, con los mismos valores de su carga horaria en la versión publicada.
- **FR-005**: Pedir el reporte de un periodo sin versión publicada DEBE responder con un 409 del catálogo; pedirlo para un docente que no está en la versión publicada DEBE responder con un 404 del catálogo. En los dos casos no se genera nada.

**Los formatos**

- **FR-006**: El reporte DEBE poder descargarse en PDF y en CSV, con los mismos datos: las mismas asignaciones, las mismas horas y los mismos totales.
- **FR-007**: El PDF DEBE cumplir PDF/A-2b, sin campos de formulario ni contenido activo, con el periodo, la versión publicada y su hash de contenido en el pie de cada página. Las pruebas automáticas DEBEN verificarlo con veraPDF.
- **FR-008**: El CSV DEBE tener una fila por asignación y filas marcadas como totales para cada componente y para el total del docente; sus columnas DEBEN tener encabezados en español y leerse bien con tildes y eñes en una hoja de cálculo.
- **FR-009**: El CSV DEBE neutralizar toda celda que empiece con `=`, `+`, `-`, `@`, una tabulación o un retorno de carro, para que no se interprete como fórmula (RNF-SEG-04).
- **FR-010**: Dos reportes del mismo docente y la misma versión DEBEN tener los mismos datos; solo pueden cambiar quién lo generó y cuándo.

**Acceso y trazabilidad**

- **FR-011**: Solo quien tiene «Reportes: lectura» en la matriz D-16 DEBE poder pedir y descargar un reporte individual, de cualquier docente de la versión publicada. Cualquier otro usuario DEBE recibir el 403 común.
- **FR-012**: Cada descarga, en PDF o en CSV, DEBE dejar una entrada en la bitácora con quién, cuándo, el periodo, la versión, el docente y el formato (aclaración del 2026-11-21). Ver el reporte en pantalla no deja entrada.
- **FR-013**: Reporting NO DEBE cambiar nada del distributivo, de la versión ni del proceso de aprobación: solo lee.

**Pantalla y errores**

- **FR-014**: La pantalla «Reportes» DEBE aparecer en el menú solo a quien tiene «Reportes: lectura», permitir elegir entre los periodos con versión publicada (por defecto, el de la publicación más reciente; research §6 del plan) y buscar a un docente de la versión publicada por su nombre o su identificación, mostrar su reporte y ofrecer «Descargar PDF» y «Descargar CSV».
- **FR-015**: Si ningún periodo tiene versión publicada, la pantalla DEBE decirlo y no ofrecer docentes ni descargas.
- **FR-016**: La pantalla DEBE cumplir WCAG 2.1 AA.
- **FR-017**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII). Si la versión publicada no se puede leer, el reporte DEBE fallar con un error del catálogo, sin generar un archivo incompleto.

### Key Entities

- **Versión publicada** (de Distributivo y Aprobación, existente): la versión congelada que llegó a PUBLICADO y es la vigente del periodo. Reporting la lee; no la cambia.
- **Carga horaria del docente** (de Distributivo, existente): sus horas por componente en la versión y su total.
- **Asignación** (de Distributivo, existente): horas de un docente en un componente, como una asignatura y paralelo que dicta o una actividad no lectiva.
- **Docente** (de Datos maestros, existente): su nombre, su identificación y su dedicación.
- **Reporte individual** (nuevo, de Publicación y consulta): el distributivo de un docente en una versión publicada, con sus asignaciones, sus horas por componente y su total, y el periodo, la versión y su hash. Se genera a pedido; no se guarda.
- **Entrada de bitácora** (existente): la de cada descarga.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Para cada docente de la versión publicada de los datos de prueba, los totales por componente y el total de su reporte son iguales a su carga horaria en esa versión (100 % de los docentes).
- **SC-002**: Para cada docente de la versión publicada de los datos de prueba, el PDF y el CSV de su reporte tienen las mismas asignaciones y los mismos totales, comparados de forma automática (100 % de los docentes).
- **SC-003**: El 100 % de los PDF generados pasan la verificación de PDF/A-2b y no tienen campos de formulario.
- **SC-004**: Ninguna celda del CSV se interpreta como fórmula al abrirlo en una hoja de cálculo, probado con datos que empiezan con cada uno de los caracteres peligrosos.
- **SC-005**: El Rector obtiene el reporte de un docente y lo descarga en los dos formatos en menos de 1 minuto desde que abre «Reportes»; cada descarga tarda menos de 5 segundos con los datos de prueba.
- **SC-006**: Ningún rol sin «Reportes: lectura» de la demo obtiene un reporte, por la pantalla ni por la API.
- **SC-007**: La pantalla no tiene fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **Qué versión:** solo la versión publicada vigente del periodo (aclaración del 2026-11-21), como dice el escenario del issue («Dado el distributivo publicado del periodo») y el milestone «Ola 4 · Publicado». Si el distributivo del periodo nunca se publicó, aunque esté en CONGELADO_EN_FIRMA, EN_OCS o APROBADO, se aplica el 409 de FR-005. Antes de publicar, el OCS ya tiene el PDF/A del consolidado con el detalle por docente (F13-H02), que es el documento que se presenta en EN_OCS. Durante una reforma, la vigente es la publicada anterior (aclaración del 2026-11-20 de F18-H02).
- **«PDF no editable»** (aclaración del 2026-11-21): un documento PDF/A-2b, sin campos de formulario ni contenido activo, con la versión y su hash en el pie, como el consolidado de F13-H02. No se cifra el PDF para restringir permisos, porque PDF/A no admite cifrado y esa restricción se quita fácilmente; la integridad la da el hash de contenido de la versión en el pie.
- **Quién puede:** los cinco roles con «Reportes: lectura» en la matriz D-16 (Rector, Vicerrector académico, Secretaría general académica, Miembro del OCS y Administrador), sobre cualquier docente. No hace falta cambiar la matriz.
- **La identificación del docente:** la que ya tiene en Datos maestros; con datos ficticios (principio IX).
- **El reporte no se guarda:** se genera a pedido a partir de la versión publicada, que es inmutable, así que dos reportes de la misma versión tienen los mismos datos. Lo que queda es la entrada de la bitácora de cada descarga.
- **El orden:** las asignaturas por carrera, asignatura y paralelo; las actividades por componente y actividad. El mismo orden en el PDF y en el CSV.
- **Fuera de alcance:**
  - el reporte general consolidado (F20-H02, RF-46);
  - la consulta del docente de su propio distributivo y el acuse de recibo (F21, RF-51);
  - el CSV para SIGA (F19, RF-52 y RF-53);
  - los reportes filtrados, el XLSX, el historial del docente y los reportes de versiones históricas (W, D-01);
  - firmar el reporte individual.
