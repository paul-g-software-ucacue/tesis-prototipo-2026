# Feature Specification: Generar el PDF/A del consolidado con su hash

**Feature Branch**: `046-pdfa-consolidado`

**Created**: 2026-11-11

**Status**: Draft

**Input**: User description: "F13-H02 · Generar el PDF/A del consolidado con su hash (#61). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md y docs/decisiones.md (ADR-006: Firma va en el proceso distributivo; ADR-007; ADR-008; D-88: el almacén de archivos; D-95; D-100) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §3 y §4: PDF/A-2b del consolidado al pasar a CONGELADO_EN_FIRMA, pie con el identificador de la versión y su hash de contenido, bloque con «Elaborado por», «Revisado por» y «Aprobado por» y un campo de firma vacío con nombre por firmante; archivos fuera de la base, nombrados por su hash, nunca se sobrescriben; tabla documento con sha256_original). Modelo de dominio: Firma reacciona a VersionCongelada (en proceso) y genera el DocumentoFirmable. Decisiones del autor: (1) se genera automáticamente al pasar a CONGELADO_EN_FIRMA (por aprobar el contenido o por el envío con la revisión desactivada), sin acción manual; (2) contenido: el resumen por carrera y los totales institucionales, y después el detalle por docente con sus asignaturas, actividades y horas por componente; (3) bajo cada rótulo del bloque de firmas van el cargo y el nombre de quien lo ocupa en el periodo, tomados de los cargos y autoridades cargados (Responsable de consolidación, Vicerrector académico, Rector, según el árbol de firmas de la configuración del flujo); (4) si la generación falla, la aprobación queda y la versión sigue congelada y en firma; la tarjeta del flujo dice «El PDF no se pudo generar» y el Responsable de consolidación tiene «Generar el PDF», idempotente; (5) el PDF debe pasar veraPDF como PDF/A; (6) quedan fuera: descargar y firmar (F14), anular por rechazo (F12-H04), los PDF parciales por carrera y su anexo, y notificar (F16-H01)."

**Origen**: issue #61 · historia (`tipo:historia`, `prioridad:must`, `RF-38`, `RNF-POR-01`) · feature F13 (#12) · milestone «Ola 3 · Aprobado y firmado» · servicio 10-firma · tamaño L. Depende de F13-H01 (#60) y de B00-S01 (#26), terminadas.

**Requerimientos que cubre**:
- **RF-38:** la firma se hace en orden y sobre la misma versión congelada. Esta historia genera el documento que se firmará; las firmas son de F14.
- **RNF-POR-01:** los archivos van fuera de la base de datos.

**Decisiones que aplica**: ADR-006 (Firma, el servicio 10, va en el proceso `distributivo`), ADR-007 (el alcance de las firmas en el prototipo), ADR-008 (las herramientas de PDF), D-88 (los archivos del negocio en un volumen, detrás de un puerto), D-95 (la transición la registra Aprobación) y D-100 (congelar la versión). Diseño de firmas §3 y §4. **Principios de la constitución**: II, IV, V, VII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-11

- Q: ¿Cuándo se genera el PDF? → A: Automáticamente, al pasar a CONGELADO_EN_FIRMA, sin acción manual (diseño de firmas §4.1; el modelo de dominio: Firma reacciona a la versión congelada).
- Q: ¿Qué contenido lleva? → A: El resumen por carrera y los totales institucionales, y después el detalle por docente con sus asignaturas, actividades y horas por componente.
- Q: ¿Qué va bajo cada rótulo del bloque de firmas? → A: El cargo y el nombre de quien lo ocupa en el periodo.
- Q: ¿Qué pasa si la generación falla? → A: La aprobación queda y la versión sigue congelada y en firma. La tarjeta del flujo dice «El PDF no se pudo generar» y el Responsable de consolidación tiene «Generar el PDF», que se puede repetir sin efectos.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El documento firmable se genera al congelar (Priority: P1)

Como Vicerrector académico, quiero que al congelar la versión se genere el PDF/A del consolidado, con el pie de la versión y su hash, para tener un documento oficial verificable sobre el que firmen los tres firmantes.

**Why this priority**: es el primer escenario del issue y lo que falta para que haya algo que firmar: hoy el distributivo llega a CONGELADO_EN_FIRMA sin documento.

**Independent Test**: congelar una versión aprobando el contenido; sin hacer nada más, el periodo tiene un documento firmable de esa versión que pasa la verificación de PDF/A, con la versión y el hash de contenido en el pie de cada página.

**Acceptance Scenarios**:

1. **Dado** una versión recién congelada, **cuando** el distributivo pasa a CONGELADO_EN_FIRMA, **entonces** se genera el documento firmable sin que nadie lo pida, y el PDF pasa la verificación de PDF/A. *(issue)*
2. **Dado** el PDF generado, **cuando** se mira el pie, **entonces** cada página muestra el identificador de la versión y su hash de contenido, el mismo de la congelación. *(issue)*
3. **Dado** el PDF generado, **cuando** se lee, **entonces** tiene primero el resumen por carrera y los totales institucionales, como el consolidado, y después el detalle por docente: sus asignaturas, sus actividades y sus horas por componente.
4. **Dado** que la versión se congeló por el envío a revisión con la revisión del Vicerrectorado desactivada, **cuando** pasa a CONGELADO_EN_FIRMA, **entonces** el documento se genera igual.
5. **Dado** una versión que ya tiene su documento, **cuando** algo vuelve a pedir generarlo, **entonces** no se crea otro: queda el mismo, con el mismo archivo.

---

### User Story 2 - El bloque de firmas (Priority: P1)

Como firmante, quiero que el PDF tenga un lugar para mi firma con mi cargo y mi nombre, en el orden en que firmamos, para firmar sin dudar dónde ni en qué turno.

**Why this priority**: es el segundo escenario del issue; sin los campos de firma el documento no sirve para F14.

**Independent Test**: abrir el PDF generado: al final tiene «Elaborado por», «Revisado por» y «Aprobado por», cada uno con el cargo, el nombre y un campo de firma vacío.

**Acceptance Scenarios**:

1. **Dado** el PDF generado, **cuando** lo abro, **entonces** tiene los rótulos «Elaborado por», «Revisado por» y «Aprobado por», en ese orden, con un campo de firma vacío para cada uno. *(issue)*
2. **Dado** el árbol de firmas de la configuración del distributivo, **cuando** se genera el PDF, **entonces** los rótulos, su orden y el rol de cada uno salen de esa configuración, la copia que regía al crearse el distributivo.
3. **Dado** cada rótulo, **cuando** se lee, **entonces** debajo están el cargo (Responsable de consolidación, Vicerrector académico, Rector) y el nombre de quien lo ocupa en el periodo.
4. **Dado** cada campo de firma, **cuando** se inspecciona el PDF, **entonces** tiene un nombre propio que dice a qué firmante corresponde, y está vacío.

---

### User Story 3 - El archivo no cambia (Priority: P1)

Como auditor, quiero que el PDF generado se guarde fuera de la base, nombrado por su hash y con su hash original registrado, para comprobar después que lo firmado es lo generado.

**Why this priority**: es el tercer escenario del issue y lo que permite verificar en F14 que nadie cambió el documento.

**Independent Test**: generar el PDF; el archivo está en el almacén de archivos, no en la base, nombrado por su SHA-256; la base registra ese SHA-256 como el original, y al recalcularlo sobre el archivo coincide.

**Acceptance Scenarios**:

1. **Dado** el PDF generado, **cuando** se guarda, **entonces** queda fuera de la base de datos, nombrado por su hash, con su SHA-256 original registrado. *(issue)*
2. **Dado** el documento registrado, **cuando** se consulta, **entonces** dice de qué periodo y versión es, su hash de contenido, su SHA-256 original, quién lo generó (el sistema o quien pidió el reintento) y cuándo.
3. **Dado** un archivo ya guardado, **cuando** algo intenta guardarlo otra vez con el mismo nombre, **entonces** no se sobrescribe.
4. **Dado** el documento generado, **cuando** se consulta la bitácora, **entonces** hay una entrada con el periodo, la versión, el hash de contenido y el SHA-256 del PDF.

---

### User Story 4 - Si falla, se reintenta (Priority: P2)

Como Responsable de consolidación, quiero saber si el PDF no se pudo generar y poder pedirlo otra vez, para que un fallo no deje el distributivo en firma sin documento.

**Why this priority**: la generación depende de datos de otros servicios (los nombres de los firmantes y de los docentes); un fallo no debe deshacer la aprobación del Vicerrector.

**Independent Test**: congelar con un firmante sin nombre; el distributivo queda en CONGELADO_EN_FIRMA, la tarjeta dice «El PDF no se pudo generar» con el motivo, y el Responsable de consolidación ve «Generar el PDF»; tras completar el dato, lo pulsa y el documento se genera.

**Acceptance Scenarios**:

1. **Dado** que la generación falla al congelar, **cuando** se consulta el flujo, **entonces** el distributivo sigue en CONGELADO_EN_FIRMA con la versión congelada y la tarjeta dice «El PDF no se pudo generar», con el motivo.
2. **Dado** el PDF sin generar, **cuando** el Responsable de consolidación abre «Consolidado del periodo», **entonces** ve «Generar el PDF»; ningún otro rol lo ve.
3. **Dado** que pulsa «Generar el PDF» y la causa ya se corrigió, **cuando** termina, **entonces** el documento queda generado y la tarjeta lo dice.
4. **Dado** un documento ya generado, **cuando** alguien pide generarlo otra vez, **entonces** responde con el mismo documento, sin crear otro ni dejar otra entrada en la bitácora.
5. **Dado** el distributivo en otra etapa, **cuando** alguien pide generar el PDF, **entonces** se rechaza con un error del catálogo y nada cambia.

---

### User Story 5 - La pantalla lo muestra (Priority: P3)

Como Vicerrector, Responsable de consolidación o firmante, quiero ver en la tarjeta del flujo que el documento firmable existe y cuál es su huella, para saber que está listo para firmar.

**Why this priority**: es informativo; la descarga y la firma son de F14.

**Independent Test**: con el PDF generado, la tarjeta del flujo de «Consolidado del periodo» dice que el documento está generado, cuándo y su huella abreviada.

**Acceptance Scenarios**:

1. **Dado** el PDF generado, **cuando** cualquiera con acceso abre «Consolidado del periodo», **entonces** la tarjeta del flujo dice «Documento firmable generado el <fecha>» con la huella del PDF abreviada y una acción para copiarla completa.
2. **Dado** que la generación está en curso o todavía no empezó, **cuando** se abre la pantalla, **entonces** la tarjeta dice que el documento se está generando, sin error.

---

### Edge Cases

- **Dos generaciones a la vez** (la automática y un reintento, o dos reintentos): queda un solo documento y una sola entrada en la bitácora.
- **Un firmante sin nombre** (el cargo sin titular vigente en el periodo, o el rol sin usuario): la generación falla con un motivo que nombra el cargo; se corrige el dato y se reintenta.
- **Un docente con asignación pero sin nombre en el catálogo de docentes:** el detalle usa el nombre con el que se guardó la asignación; no falla.
- **El archivo se guardó y el registro falló:** un reintento genera de nuevo. Como el PDF lleva su fecha de creación, el archivo nuevo tiene otro hash y otro nombre. El anterior queda en el almacén sin registrar: nunca se borra ni se sobrescribe (principio VII), y no es el documento firmable.
- **El almacén de archivos no responde:** la generación falla; la aprobación queda (FR-011).
- **La versión no está congelada** (por ejemplo, una prueba manual sobre otra etapa): no se genera nada.

## Requirements *(mandatory)*

### Functional Requirements

**Generar**

- **FR-001**: Al pasar el distributivo a CONGELADO_EN_FIRMA, el sistema DEBE generar automáticamente el documento firmable del consolidado a partir de la versión congelada, sin acción del usuario, sea por aprobar el contenido o por el envío a revisión con la revisión desactivada.
- **FR-002**: El documento DEBE generarse conforme a PDF/A-2b. Las pruebas automáticas DEBEN verificarlo con veraPDF sobre un documento con el contenido completo de la prueba (165 paralelos) y en cada construcción.
- **FR-003**: Cada página DEBE llevar en el pie el identificador de la versión (periodo y número) y su hash de contenido, el registrado al congelar (D-100).
- **FR-004**: El documento DEBE tener, en este orden: el encabezado institucional con el periodo; el resumen por carrera (estado, paralelos con propuesta y horas por componente) y los totales institucionales; el detalle por docente con sus asignaturas-paralelo, sus actividades no lectivas y sus horas por componente; y el bloque de firmas.
- **FR-005**: Una versión DEBE tener un solo documento firmable: generar otra vez una versión que ya lo tiene devuelve el mismo, sin crear otro archivo, registro ni entrada.

**Bloque de firmas**

- **FR-006**: El bloque de firmas DEBE tener un rótulo por firmante del árbol de firmas de la copia de la configuración del distributivo («Elaborado por», «Revisado por», «Aprobado por»), en su orden.
- **FR-007**: Bajo cada rótulo DEBEN ir el cargo y el nombre de quien lo ocupa en el periodo, y un campo de firma vacío con un nombre que identifica al firmante.
- **FR-008**: Si a un firmante le falta el titular o el nombre, la generación DEBE fallar con un motivo que nombra el cargo.

**Archivo y registro**

- **FR-009**: El PDF DEBE guardarse fuera de la base de datos, nombrado por su SHA-256, sin sobrescribir nunca un archivo existente (RNF-POR-01).
- **FR-010**: El sistema DEBE registrar el documento con su periodo, su versión, su hash de contenido, su SHA-256 original, quién lo generó y cuándo, y la bitácora DEBE recibir **una** entrada con esos datos.

**Fallos y reintento**

- **FR-011**: Si la generación falla, la versión DEBE seguir congelada y el distributivo en CONGELADO_EN_FIRMA; la aprobación no se deshace. El fallo y su motivo DEBEN quedar disponibles para la pantalla.
- **FR-012**: El rol de la etapa CONGELADO_EN_FIRMA (el Responsable de consolidación) DEBE poder pedir «Generar el PDF» mientras la versión congelada no tenga documento; se rechaza en otra etapa, sin el rol o sin permiso.
- **FR-013**: Un fallo de generación NO DEBE dejar un documento registrado ni una entrada de documento generado en la bitácora.

**Pantalla**

- **FR-014**: En CONGELADO_EN_FIRMA, la tarjeta del flujo de «Consolidado del periodo» DEBE decir el estado del documento: generado (con la fecha y la huella del PDF abreviada, copiable completa), en generación, o «El PDF no se pudo generar» con el motivo.
- **FR-015**: «Generar el PDF» DEBE ofrecerse solo al rol de la etapa y solo sin documento.

**Errores**

- **FR-016**: Los errores nuevos DEBEN estar en el catálogo con sus 8 columnas (principio XIII).

### Key Entities

- **Documento firmable** (nuevo, de Firma): el PDF/A de una versión congelada. Su tipo (en el prototipo, CONSOLIDADO), el periodo, la versión, el hash de contenido de la versión, el SHA-256 original del archivo, quién lo generó y cuándo. En F14 recibirá su archivo vigente y sus firmas.
- **Generación del documento:** el último resultado de generar el documento de una versión: en curso, fallida con su motivo, o generada.
- **Versión congelada** (de F13-H01): su número y su hash de contenido.
- **Árbol de firmas** (de F12-H01): el orden, la etiqueta y el rol de cada firmante del consolidado, en la copia de la configuración del distributivo.
- **Firmante del periodo:** el cargo y el nombre de quien lo ocupa: el Rector y el Vicerrector académico, de los cargos de autoridad vigentes; el Responsable de consolidación, el usuario con ese rol (ADR-003).
- **Huella del PDF:** el SHA-256 del archivo. No es el hash de contenido: este identifica la versión, aquella identifica el archivo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los documentos generados pasan la verificación de PDF/A-2b.
- **SC-002**: El hash de contenido del pie es igual al de la congelación en el 100 % de los casos.
- **SC-003**: El documento queda generado en menos de 30 segundos desde que el distributivo pasa a CONGELADO_EN_FIRMA, con el distributivo completo de la prueba (165 paralelos).
- **SC-004**: Recalcular el SHA-256 del archivo guardado da el original registrado en el 100 % de los casos, y el nombre del archivo es ese hash.
- **SC-005**: Con dos pedidos de generación simultáneos queda exactamente un documento y una entrada en la bitácora.
- **SC-006**: Después de un fallo, el Responsable de consolidación genera el documento en menos de 1 minuto desde que abre la pantalla, una vez corregida la causa.
- **SC-007**: Los escenarios de F13-H01, F12-H05 y #215 siguen pasando sin cambios.

## Assumptions

- **Los firmantes** salen del árbol de firmas de la copia de la configuración del distributivo (la plantilla por defecto: Elaborado → Responsable de consolidación, Revisado → Vicerrector, Aprobado → Rector). El nombre del Rector y del Vicerrector académico sale de sus cargos de autoridad vigentes en el periodo; el del Responsable de consolidación, del usuario con ese rol, que designa el Vicerrectorado (ADR-003), porque no es un cargo de la carga de autoridades.
- **El rótulo** es la etiqueta de la configuración seguida de «por» («Elaborado» → «Elaborado por»).
- **El hash de contenido** del pie es el de la transición de congelación (D-100); el SHA-256 del PDF es otro, el del archivo.
- **El almacén** es el de D-88 (un volumen detrás de un puerto). Si el puerto pasa a la plataforma o Firma tiene el suyo se decide en el plan.
- **La generación** no cambia la etapa: el distributivo ya está en CONGELADO_EN_FIRMA cuando empieza. Generar el documento no es una transición.
- **Quién genera:** la generación automática queda a nombre del sistema; un reintento, a nombre de quien lo pidió.
- **La verificación de PDF/A** corre en las pruebas, no en el proceso: veraPDF necesita JAXB 2.3 y Spring Boot trae JAXB 4 (D-46, hallazgo 5). La conformidad se garantiza por construcción (fuente incrustada, perfil de color, metadatos XMP) y se verifica en cada construcción.
- **Sin descarga todavía:** ver y descargar el PDF es de F14-H01 (#62). Esta historia solo dice que existe y su huella.
- **La demo:** 2026-I está en EN_CONSOLIDACION con 1 propuesta de 165 paralelos, así que no llega a CONGELADO_EN_FIRMA. El recorrido completo se prueba en el perfil local y en la CI.

## Out of Scope

- Descargar el PDF y firmarlo (F14-H01 a F14-H04).
- Anular el documento y las firmas tras un rechazo, y la versión nueva (F12-H04).
- Los PDF parciales por carrera y su anexo con la lista de parciales y sus hashes.
- Notificar a los firmantes (F16-H01).
- El sello de tiempo y la validación a largo plazo (ADR-007).
