# Feature Specification: Reporte general consolidado en PDF y CSV

**Feature Branch**: `059-reporte-general-consolidado`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "F20-H02"

**Origen**: issue #269 · historia (`tipo:historia`, `prioridad:must`, `RF-46`, `RF-48`, `modulo:08-reporting`) · feature F20 (Reportes general e individual en PDF y CSV, #259) · milestone «Ola 4 · Publicado» · servicio 08-reporting · tamaño M. Depende de F20-H01 (#268, el reporte individual, spec 058), terminada. Es la última historia de F20.

**Historia (tal como está en el issue)**: Como Rector, quiero obtener el reporte general consolidado institucional en PDF y en CSV, para presentar el distributivo completo ante el OCS.

**Requerimientos que cubre**:
- **RF-46:** generar el reporte general consolidado institucional. Su criterio en la ERS: «Incluye a todos los docentes activos del periodo.»
- **RF-48:** exportar los reportes en PDF no editable y CSV; el XLSX queda fuera del prototipo (D-01). Su criterio: «Los dos formatos contienen los mismos datos.»
- **RNF-SEG-04:** prevenir la inyección de fórmulas en los CSV exportados.
- **RF-52 (relación, no lo cubre):** el CSV para SIGA de F19 debe tener «totales iguales a los del reporte RF-46». Los totales institucionales de esta historia son la referencia de esa comparación.
- **Glosario:** «Reporte general: distributivo consolidado de la institución en PDF y CSV» (Publicación y consulta). «Componente: clase, otras actividades de docencia, investigación, vinculación o gestión.» «Carga horaria: total de horas de un docente por componente en la versión.» «Estado laboral: activo, en licencia o inactivo. Solo un docente activo recibe asignaciones.»
- **Mapa de contextos:** Publicación y consulta es conformista con Distributivo, Aprobación, Firma y Datos maestros: lee la versión publicada y los datos del docente, solo por llamada de lectura (MD-11).

**Decisiones que aplica**:
- D-01: reportería básica; reportes filtrados, XLSX, tablero e historial del docente quedan fuera (W).
- D-16 y D-63: la funcionalidad «Reportes» con permiso de lectura, que tienen el Rector, el Vicerrector académico, la Secretaría general académica, el Miembro del OCS y el Administrador.
- D-115 (F20-H01, spec 058): Reporting lee la publicación vigente de Aprobación y el contenido congelado de Generación; el contenido por docente se guarda al congelar y no cambia después de publicar; el PDF es PDF/A-2b sin campos ni contenido activo, con la versión y su hash en el pie; el CSV neutraliza las fórmulas; cada descarga queda en la bitácora antes de responder.
- F18-H01 (spec 055): la versión publicada es la oficial del periodo.
- F18-H02 (spec 056, aclaración del 2026-11-20): durante una reforma, la versión publicada anterior sigue siendo la vigente hasta que se publique la de la reforma.

**Principios de la constitución**: II, III, IV, V, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- El distributivo del periodo llega a PUBLICADO cuando la Secretaría general académica lo publica (F18-H01). Su versión publicada está congelada, con su hash de contenido, y es inmutable.
- Al congelar, se guarda el contenido de la versión por docente: sus datos, sus asignaturas y paralelos, sus actividades y su carga horaria por componente (F20-H01).
- La pantalla «Reportes» ya existe para quien tiene «Reportes: lectura»: elige un periodo con versión publicada, busca un docente, muestra su reporte individual y lo descarga en PDF y CSV (F20-H01).
- No hay un reporte de toda la institución sobre la versión publicada. Lo más parecido es el PDF/A del consolidado (F13-H02), que se genera al congelar, se firma y se presenta en la revisión del OCS, pero no tiene CSV.

Esta historia agrega:
- **El reporte general:** para la versión publicada vigente del periodo, cada docente con sus horas por componente y su total, y los totales institucionales.
- **Las dos descargas:** el mismo reporte en PDF no editable y en CSV, con los mismos datos y los mismos totales.
- **En la pantalla «Reportes»:** el reporte general del periodo elegido, junto al individual.

## Clarifications

### Session 2026-11-21

- Q: ¿«Todos los docentes activos del periodo» (RF-46) son los que están en la versión publicada, o también los activos de Datos maestros sin ninguna asignación? → A: Los docentes de la versión publicada. Todos estaban activos cuando se les asignó algo, porque solo un docente activo recibe asignaciones. Salen del contenido que se guarda al congelar y no cambian después de publicar. Un docente activo sin carga no aparece.
- Q: ¿El reporte general lleva un resumen de horas por carrera y componente, como el PDF/A del consolidado? → A: No. Lleva solo la tabla por docente y los totales institucionales, en el PDF y en el CSV. El resumen por carrera ya está en el PDF/A del consolidado (F13-H02).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Rector consulta el reporte general del periodo (Priority: P1)

Como Rector, quiero ver el reporte general del distributivo publicado, para revisar la carga de toda la institución antes de presentarla ante el OCS: cada docente con sus horas por componente y los totales institucionales.

**Why this priority**: es el primer escenario del issue y el criterio de RF-46. Sin el contenido del reporte no hay nada que descargar.

**Independent Test**:
1. Con el distributivo de 2026-I publicado, `rectorado.demo` abre «Reportes» y elige el periodo.
2. Abre el reporte general.
3. Ve a cada docente del distributivo publicado con sus horas por componente y su total, y al final los totales institucionales, con la versión publicada de la que salen.

**Acceptance Scenarios**:

1. **Dado** el distributivo publicado del periodo, **cuando** pido el reporte general, **entonces** incluye a todos los docentes de la versión publicada (aclaración del 2026-11-21), con sus horas por componente y los totales institucionales. *(issue)*
2. **Dado** el reporte general, **cuando** lo leo, **entonces** dice el periodo, la versión publicada, su hash de contenido y la fecha de publicación, y cuántos docentes incluye.
3. **Dado** el reporte general, **cuando** leo la fila de un docente, **entonces** tiene su identificación, su nombre, su dedicación, sus horas en cada uno de los cinco componentes y su total, con los mismos valores de su carga horaria en la versión publicada y de su reporte individual.
4. **Dado** el reporte general, **cuando** sumo las horas de todos los docentes en cada componente, **entonces** el resultado es igual al total institucional de ese componente, y la suma de los cinco totales es igual al total institucional general.
5. **Dado** el reporte general, **cuando** lo leo, **entonces** tiene solo la tabla por docente y los totales institucionales, sin un resumen por carrera (aclaración del 2026-11-21).
6. **Dado** un periodo sin distributivo publicado, **cuando** pido su reporte general por la API, **entonces** responde con el 409 del catálogo y no genera nada; la pantalla solo ofrece los periodos con versión publicada.
7. **Dado** el distributivo en reforma, **cuando** pido el reporte general, **entonces** sale de la versión publicada vigente, la anterior a la reforma, y lo dice; los cambios en curso de la reforma no aparecen.
8. **Dado** un usuario sin «Reportes: lectura», por ejemplo un coordinador de carrera o un docente, **cuando** pide el reporte general, **entonces** recibe el 403 común.

---

### User Story 2 - El Rector descarga el reporte general en PDF y en CSV con los mismos datos (Priority: P1)

Como Rector, quiero descargar el reporte general en PDF y en CSV, para presentarlo ante el OCS como un documento que no se puede alterar y para revisarlo o cruzarlo en una hoja de cálculo.

**Why this priority**: es el segundo escenario del issue y el criterio de RF-48. El reporte sirve para presentar el distributivo completo fuera del sistema.

**Independent Test**:
1. Con el reporte general en pantalla, `rectorado.demo` descarga el PDF y el CSV.
2. Abre el CSV en una hoja de cálculo y compara cada docente y cada total con el PDF: son los mismos.
3. El PDF pasa la verificación de PDF/A y no tiene campos que se puedan llenar.

**Acceptance Scenarios**:

1. **Dado** el reporte general, **cuando** lo descargo en PDF y en CSV, **entonces** los dos contienen los mismos datos y los mismos totales. *(issue)*
2. **Dado** el PDF descargado, **cuando** se verifica, **entonces** cumple PDF/A-2b, no tiene campos de formulario ni contenido activo, y cada página muestra en el pie el periodo, la versión publicada, su hash de contenido y el número de página.
3. **Dado** el CSV descargado, **cuando** lo abro en una hoja de cálculo, **entonces** cada docente del PDF es una fila, con su identificación, su nombre, su dedicación, sus horas en cada componente y su total, y los totales institucionales están en una fila marcada como total, con los mismos valores del PDF.
4. **Dado** un dato de un docente que empieza con `=`, `+`, `-`, `@`, una tabulación o un retorno de carro, **cuando** se genera el CSV, **entonces** la celda se neutraliza para que la hoja de cálculo no la interprete como fórmula, y el PDF muestra el mismo texto.
5. **Dado** nombres con tildes y eñes, **cuando** abro el CSV en una hoja de cálculo, **entonces** se leen bien, sin caracteres extraños.
6. **Dado** la misma versión publicada, **cuando** descargo el reporte general dos veces, **entonces** los datos son idénticos; solo cambian, en el PDF, la fecha y la persona que lo generó.
7. **Dado** una descarga del reporte general, **cuando** se completa, **entonces** queda una entrada en la bitácora con quién lo descargó, cuándo, el periodo, la versión y el formato.
8. **Dado** los archivos descargados, **cuando** miro su nombre, **entonces** dice que es el reporte general y el periodo, sin espacios ni caracteres que un sistema de archivos rechace.

---

### Edge Cases

- **Muchos docentes:** el PDF pasa a una página nueva cuando hace falta y repite el encabezado de la tabla; los totales institucionales van al final, una sola vez; el pie con la versión y el hash está en cada página.
- **Un componente sin horas en toda la institución:** aparece en los totales institucionales con 0, en el PDF y en el CSV, para que los dos tengan siempre los cinco componentes.
- **Un docente con solo actividades o solo asignaturas:** aparece con 0 en los componentes que no tiene.
- **Horas con decimales** (hasta dos, D-77): se muestran con dos decimales en el PDF y en el CSV; los totales institucionales son la suma exacta de las filas, sin redondear cada total por separado.
- **Un docente que dicta en varias carreras:** aparece una sola vez, con todas sus horas sumadas.
- **Un docente desactivado o en licencia después de publicar:** sigue en el reporte, porque la versión publicada no cambia.
- **Dos docentes con el mismo nombre:** aparecen los dos, distinguidos por su identificación; el orden es por nombre y, a igual nombre, por identificación.
- **Un nombre muy largo:** el PDF lo parte en varias líneas sin cortar el texto ni salirse de la página.
- **Dos periodos con distributivo publicado:** el reporte sale del periodo elegido en la pantalla.
- **La reforma se publica mientras el Rector mira el reporte general:** el que ya ve dice su versión; el siguiente que pida sale de la nueva versión publicada.
- **El proceso del que Reporting lee la versión no responde:** el reporte no se genera, la pantalla lo dice con el error del catálogo y no queda ningún archivo a medias.
- **El contenido guardado no coincide con el hash de la versión publicada:** el reporte no se genera y responde con el error del catálogo, como el individual.

## Requirements *(mandatory)*

### Functional Requirements

**El reporte**

- **FR-001**: El sistema DEBE generar el reporte general a partir de la versión publicada vigente del periodo elegido, y solo de ella: ni de una versión en curso, ni de una histórica, ni de una congelada que todavía no se publicó.
- **FR-002**: El reporte DEBE incluir el periodo, la versión publicada, su hash de contenido, su fecha de publicación y el número de docentes que incluye; el PDF, además, quién lo generó y cuándo.
- **FR-003**: El reporte DEBE incluir a todos los docentes de la versión publicada, es decir, los que tienen al menos una asignatura o una actividad en ella, una vez cada uno y ordenados por nombre y luego por identificación (aclaración del 2026-11-21). Un docente activo sin asignaciones en la versión no aparece.
- **FR-003a**: El reporte NO DEBE llevar un resumen por carrera ni el detalle de asignaturas y actividades: solo la fila de cada docente y los totales institucionales (aclaración del 2026-11-21).
- **FR-004**: Por cada docente, el reporte DEBE mostrar su identificación, su nombre, su dedicación, sus horas en cada uno de los cinco componentes (clase, otras actividades de docencia, investigación, vinculación y gestión), incluidos los que suman 0, y su total, con los mismos valores de su carga horaria en la versión publicada.
- **FR-005**: El reporte DEBE mostrar los totales institucionales: la suma de las horas de todos los docentes en cada componente y el total general, iguales a la suma exacta de las filas.
- **FR-006**: Pedir el reporte general de un periodo sin versión publicada DEBE responder con el 409 del catálogo que ya usa el reporte individual, sin generar nada.

**Los formatos**

- **FR-007**: El reporte general DEBE poder descargarse en PDF y en CSV, con los mismos datos: los mismos docentes, las mismas horas y los mismos totales.
- **FR-008**: El PDF DEBE cumplir PDF/A-2b, sin campos de formulario ni contenido activo, con el periodo, la versión publicada, su hash de contenido y el número de página en el pie de cada página, y con el encabezado de la tabla repetido en cada página. Las pruebas automáticas DEBEN verificarlo con veraPDF.
- **FR-009**: El CSV DEBE tener una fila por docente y una fila marcada como total institucional; sus columnas DEBEN tener encabezados en español, usar el mismo formato de archivo que el CSV del reporte individual y leerse bien con tildes y eñes en una hoja de cálculo.
- **FR-010**: El CSV DEBE neutralizar toda celda que empiece con `=`, `+`, `-`, `@`, una tabulación o un retorno de carro, para que no se interprete como fórmula (RNF-SEG-04).
- **FR-011**: Dos reportes generales de la misma versión DEBEN tener los mismos datos; los CSV DEBEN ser idénticos y los PDF solo pueden cambiar en quién lo generó y cuándo.

**Acceso y trazabilidad**

- **FR-012**: Solo quien tiene «Reportes: lectura» en la matriz D-16 DEBE poder pedir y descargar el reporte general. Cualquier otro usuario DEBE recibir el 403 común.
- **FR-013**: Cada descarga, en PDF o en CSV, DEBE dejar una entrada en la bitácora con quién, cuándo, el periodo, la versión y el formato, registrada antes de entregar el archivo, como en el reporte individual. Ver el reporte en pantalla no deja entrada.
- **FR-014**: Reporting NO DEBE cambiar nada del distributivo, de la versión ni del proceso de aprobación: solo lee.

**Pantalla y errores**

- **FR-015**: La pantalla «Reportes» DEBE ofrecer, para el periodo elegido, el reporte general junto al individual: mostrarlo con sus totales institucionales y ofrecer «Descargar PDF» y «Descargar CSV».
- **FR-016**: Si ningún periodo tiene versión publicada, la pantalla DEBE seguir diciéndolo y no ofrecer el reporte general.
- **FR-017**: La pantalla DEBE cumplir WCAG 2.1 AA, también con la tabla de todos los docentes.
- **FR-018**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII). Si la versión publicada no se puede leer o su contenido no coincide con su hash, el reporte DEBE fallar con un error del catálogo, sin generar un archivo incompleto.

### Key Entities

- **Versión publicada** (de Distributivo y Aprobación, existente): la versión congelada que llegó a PUBLICADO y es la vigente del periodo. Reporting la lee; no la cambia.
- **Contenido congelado por docente** (de Distributivo, existente desde F20-H01): por cada docente de la versión, sus datos, sus asignaciones y su carga horaria por componente, guardado al congelar.
- **Carga horaria del docente** (de Distributivo, existente): sus horas por componente en la versión y su total.
- **Totales institucionales** (nuevo, del reporte): la suma de la carga horaria de todos los docentes de la versión en cada componente y en total.
- **Reporte general** (nuevo, de Publicación y consulta): el distributivo consolidado de la institución en una versión publicada, con cada docente, sus horas por componente y su total, y los totales institucionales, con el periodo, la versión y su hash. Se genera a pedido; no se guarda.
- **Entrada de bitácora** (existente): la de cada descarga.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con la versión publicada de los datos de prueba, el reporte general incluye al 100 % de los docentes que corresponden (FR-003), cada uno una sola vez.
- **SC-002**: Para el 100 % de los docentes del reporte general, sus horas por componente y su total son iguales a los de su reporte individual de la misma versión.
- **SC-003**: Los totales institucionales del reporte general son iguales a la suma de las filas de sus docentes, en los cinco componentes y en el total, en el PDF y en el CSV, comparados de forma automática.
- **SC-004**: El PDF y el CSV del reporte general tienen los mismos docentes, las mismas horas y los mismos totales, comparados de forma automática.
- **SC-005**: El 100 % de los PDF generados pasan la verificación de PDF/A-2b y no tienen campos de formulario.
- **SC-006**: Ninguna celda del CSV se interpreta como fórmula al abrirlo en una hoja de cálculo, probado con datos que empiezan con cada uno de los caracteres peligrosos.
- **SC-007**: El Rector obtiene el reporte general y lo descarga en los dos formatos en menos de 1 minuto desde que abre «Reportes»; cada descarga tarda menos de 10 segundos con un distributivo de 300 docentes.
- **SC-008**: Ningún rol sin «Reportes: lectura» de la demo obtiene el reporte general, por la pantalla ni por la API.
- **SC-009**: La pantalla no tiene fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **Qué versión:** solo la versión publicada vigente del periodo, como el reporte individual (aclaración del 2026-11-21 de F20-H01) y como dice el escenario del issue («Dado el distributivo publicado del periodo»). Durante una reforma, la vigente es la publicada anterior (aclaración del 2026-11-20 de F18-H02).
- **Qué docentes** (aclaración del 2026-11-21): «todos los docentes activos del periodo» (RF-46) se lee como todos los docentes de la versión publicada. Solo un docente activo recibe asignaciones, así que todos estaban activos cuando se armó el distributivo. Si después lo desactivan, sigue en el reporte, porque la versión no cambia.
- **Qué datos por docente:** sus horas por componente y su total, como dice el issue. El detalle de sus asignaturas, paralelos y actividades está en su reporte individual; el reporte general no lo repite.
- **«PDF no editable»:** el mismo criterio del reporte individual (aclaración del 2026-11-21 de F20-H01): PDF/A-2b sin campos de formulario ni contenido activo, con la versión y su hash en el pie, sin cifrado.
- **El formato del CSV:** el mismo del reporte individual (D-115): la misma codificación, el mismo separador y el mismo formato de horas, para que los dos se abran igual en una hoja de cálculo.
- **Quién puede:** los cinco roles con «Reportes: lectura» en la matriz D-16. No hace falta cambiar la matriz.
- **La bitácora:** cada descarga queda registrada como las del reporte individual (aclaración del 2026-11-21 de F20-H01), con su propia acción para distinguirla.
- **El reporte no se guarda:** se genera a pedido a partir del contenido guardado de la versión publicada, que es inmutable.
- **El tamaño:** el prototipo trabaja con decenas de docentes por periodo en los datos de prueba; SC-007 pide un margen de 300 docentes.
- **Datos ficticios:** las identificaciones y los nombres de la demo son ficticios (principio IX).
- **Fuera de alcance:**
  - el detalle por docente de asignaturas y actividades (está en el reporte individual, F20-H01);
  - el resumen por carrera (está en el PDF/A del consolidado, F13-H02);
  - los docentes activos sin asignaciones en la versión publicada;
  - el CSV para SIGA (F19, RF-52 y RF-53), aunque sus totales deberán coincidir con los de este reporte;
  - la consulta del docente de su propio distributivo y el acuse de recibo (F21, RF-51);
  - los reportes filtrados (por carrera, por dedicación o por componente), el XLSX y los reportes de versiones históricas (W, D-01);
  - firmar el reporte general: el documento firmado que se presenta al OCS es el PDF/A del consolidado (F13-H02).
