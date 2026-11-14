# Feature Specification: Ver mis firmas pendientes y descargar el PDF vigente

**Feature Branch**: `047-firmas-pendientes`

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "F14-H01 · Ver mis firmas pendientes y descargar el PDF vigente (#62). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md, docs/decisiones.md (D-101, D-100, D-88, MD-05, ADR-003, ADR-007) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §5.1 y §9: «Pendientes de firma», GET /firmas/pendientes, descargar el PDF vigente). Decisiones del autor: (1) pantalla nueva «Pendientes de firma» en el menú, con una fila por documento en turno del usuario: periodo, versión, rótulo del paso («Elaborado por», «Revisado por», «Aprobado por»), fecha de generación y «Descargar el PDF»; (2) «en turno» es la persona, no el rol: para F1, el Responsable designado del periodo (ADR-003); para F2 y F3, quien ocupa el cargo vigente de Vicerrector y de Rector, los mismos nombres que imprime el PDF (D-101); un usuario con el rol que no es esa persona no lo ve; (3) solo el firmante en turno descarga el PDF vigente, los demás reciben 403 del catálogo; (4) el PDF vigente es el último archivo de la cadena: hoy, sin firmas todavía (F14-H02), el original generado en F13-H02, con su SHA-256; un documento FALLIDO o EN_CURSO no aparece. Fuera de alcance: subir y validar la firma (F14-H02, F14-H03), rechazar en firma (F14-H04), notificaciones y el estado de la cadena para otros (F16-H01, F16-H02), descargar el PDF desde «Consolidado del periodo»."

**Origen**: issue #62 · historia (`tipo:historia`, `prioridad:must`, `RF-38`, `RF-58`) · feature F14 (#13) · milestone «Ola 3 · Aprobado y firmado» · servicio 10-firma · tamaño S. Depende de F13-H02 (#61), terminada.

**Requerimientos que cubre**:
- **RF-38:** la firma se hace en orden, sobre la misma versión congelada. Esta historia muestra a cada firmante el documento de su turno y le entrega el archivo que debe firmar.
- **RF-58:** el árbol de firmas configurable. El turno sale del orden de ese árbol.

**Decisiones que aplica**:
- D-101: el documento firmable, sus firmantes y su archivo nombrado por su hash.
- D-100: la versión congelada.
- D-88: los archivos fuera de la base.
- MD-05: Firma no conoce el flujo.
- ADR-003: el Responsable de consolidación es una designación por periodo.
- ADR-007: el alcance de las firmas en el prototipo.

Diseño de firmas §5.1 y §9. **Principios de la constitución**: II, IV, V, VII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-14

- Q: ¿Dónde ve el firmante sus documentos pendientes? → A: En una pantalla nueva, «Pendientes de firma», con su entrada en el menú. Cada documento en su turno es una fila con el periodo, la versión, el rótulo del paso, la fecha de generación y «Descargar el PDF».
- Q: ¿Quién está «en turno»: el rol o la persona? → A: La persona.
  - Para «Elaborado por», el Responsable designado del periodo.
  - Para «Revisado por» y «Aprobado por», quien ocupa el cargo vigente de Vicerrector y de Rector.
  
  Son los mismos que imprime el PDF. Otro usuario con el mismo rol no lo ve.
- Q: ¿Quién descarga el PDF vigente? → A: Solo el firmante en turno. Los demás reciben el 403 común del catálogo.
- Q: Las cuentas no guardan la cédula, así que no se sabe qué usuario es el docente que ocupa un cargo. ¿Cómo se reconoce a esa persona? → A: La cuenta de usuario guarda el número de identificación de la persona. En turno está el usuario cuya identificación es la del docente del cargo. Los usuarios de la demostración reciben la identificación del cargo que representan en los datos de prueba. F14-H03 la usará también para comparar con el certificado.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El firmante en turno ve su documento y lo descarga (Priority: P1)

Como Responsable de consolidación, quiero ver en «Pendientes de firma» el documento que me toca firmar y descargarlo, para firmarlo con FirmaEC en mi computadora.

**Why this priority**: es el primer escenario del issue y el primer paso de la cadena de firmas. Hoy el PDF existe, pero nadie lo puede bajar desde el sistema.

**Independent Test**:
1. Con el distributivo en CONGELADO_EN_FIRMA y su documento generado, el Responsable designado del periodo entra a «Pendientes de firma».
2. Ve una fila con el periodo, la versión, «Elaborado por» y la fecha de generación.
3. Pulsa «Descargar el PDF». El archivo descargado tiene el SHA-256 registrado del documento.

**Acceptance Scenarios**:

1. **Dado** un documento firmable cuyo paso en turno es F1 y yo soy su firmante, **cuando** abro «Pendientes de firma», **entonces** veo el documento y puedo descargar su archivo vigente. *(issue)*
2. **Dado** la fila del documento, **cuando** la leo, **entonces** dice el periodo, el número de versión, el rótulo de mi paso («Elaborado por») y la fecha en que se generó el documento.
3. **Dado** que pulso «Descargar el PDF», **cuando** termina la descarga, **entonces** el archivo es el vigente del documento: su SHA-256 es el registrado. Hoy, sin firmas todavía, es el original.
4. **Dado** que no tengo ningún documento en turno, **cuando** abro «Pendientes de firma», **entonces** la pantalla dice que no tengo firmas pendientes, sin error.

---

### User Story 2 - Quien no está en turno no lo ve ni lo descarga (Priority: P1)

Como Vicerrector académico, quiero que solo me aparezca un documento cuando sea mi turno, para no firmar fuera de orden.

**Why this priority**: es el segundo escenario del issue. Sin él, la cadena se podría firmar fuera de orden (RF-38).

**Independent Test**: con el paso en turno en F1:
- el Vicerrector y el Rector abren «Pendientes de firma» y no ven el documento;
- si piden la descarga directamente, reciben el 403 común;
- otro usuario con el rol de Responsable de consolidación que no es el designado tampoco lo ve ni lo descarga.

**Acceptance Scenarios**:

1. **Dado** un documento cuyo paso en turno es F1, **cuando** el Vicerrector abre «Pendientes de firma», **entonces** el documento no aparece como pendiente suyo. *(issue)*
2. **Dado** un documento cuyo paso en turno es F1, **cuando** el Vicerrector o el Rector piden su archivo, **entonces** se rechaza con el 403 común del catálogo y no se entrega nada.
3. **Dado** un usuario con el rol de Responsable de consolidación que no es el designado del periodo, **cuando** abre «Pendientes de firma» o pide el archivo, **entonces** no ve el documento y recibe el 403 común.
4. **Dado** un documento sin generar, en curso o con la generación fallida, **cuando** cualquiera abre «Pendientes de firma», **entonces** el documento no aparece y su archivo no se entrega.

---

### User Story 3 - Las autoridades se reconocen por su identificación (Priority: P1)

Como Rector o Vicerrector académico, quiero que el sistema me reconozca como quien ocupa mi cargo, para que mis firmas pendientes me lleguen a mí y no a cualquiera con el rol.

**Why this priority**: sin el vínculo entre la cuenta y la persona del cargo, no hay forma de decidir quién está en turno en F2 y F3. Este vínculo también lo usará F14-H03 para comparar con el certificado.

**Independent Test**:
1. En la demostración, la cuenta del Vicerrectorado tiene la identificación del docente que ocupa el cargo vigente de Vicerrector en los datos de prueba.
2. Con el paso en turno en F2, esa cuenta ve el documento.
3. Una cuenta con el rol VICERRECTOR y otra identificación no lo ve.

**Acceptance Scenarios**:

1. **Dado** una cuenta con la identificación del docente que ocupa el cargo vigente de Vicerrector, **cuando** el paso en turno es «Revisado por», **entonces** esa cuenta ve el documento en «Pendientes de firma» y puede descargarlo.
2. **Dado** una cuenta con el rol VICERRECTOR sin identificación, o con otra, **cuando** el paso en turno es «Revisado por», **entonces** no ve el documento y su descarga se rechaza con el 403 común.
3. **Dado** dos cuentas, **cuando** se intenta darles la misma identificación, **entonces** se rechaza: una identificación corresponde a una sola cuenta.
4. **Dado** los usuarios de la demostración, **cuando** se cargan, **entonces** las cuentas del Rectorado y del Vicerrectorado tienen la identificación de los docentes que ocupan esos cargos en los datos de prueba.

---

### Edge Cases

- **El paso en turno no tiene a nadie**: el cargo quedó sin titular, o ninguna cuenta tiene esa identificación. El documento no aparece para nadie y nadie lo descarga, sin error. El dato se corrige fuera de esta historia.
- **Cambia la designación o el cargo después de generar el PDF**: en turno sigue la persona que imprime el PDF, fijada al generarlo, para que la firma coincida con el nombre impreso. Rehacer el documento con otros firmantes es de F12-H04.
- **Varios periodos en firma a la vez**: la pantalla muestra una fila por cada documento en turno del usuario.
- **El archivo vigente falta o no coincide con su SHA-256 registrado**: la descarga falla con un error del catálogo y no entrega un archivo distinto del registrado.
- **El usuario ocupa dos pasos de la misma cadena** (por ejemplo, la misma persona designada y con el cargo): solo ve el paso en turno; el siguiente le aparecerá cuando llegue su turno (F14-H03).
- **El distributivo ya no está en CONGELADO_EN_FIRMA**: hoy no hay salida de esa etapa. Cuando exista (F14-H04, F12-H04), el documento dejará de aparecer.

## Requirements *(mandatory)*

### Functional Requirements

**Firmas pendientes**

- **FR-001**: El sistema DEBE ofrecer «Pendientes de firma» como entrada del menú a quien puede firmar algún paso del árbol de firmas.
- **FR-002**: «Pendientes de firma» DEBE mostrar, para el usuario que la abre, un documento por fila: solo los documentos firmables generados cuyo paso en turno le corresponde. Cada fila lleva el periodo, el número de versión, el rótulo del paso y la fecha de generación.
- **FR-003**: Sin documentos en turno, la pantalla DEBE decirlo con un mensaje, sin error.
- **FR-004**: Un documento sin generar, en curso o con la generación fallida NO DEBE aparecer en «Pendientes de firma».

**Turno y firmante**

- **FR-005**: El paso en turno de un documento DEBE ser el primero del árbol de firmas que todavía no tiene firma. Hoy, sin firmas, es siempre el primero («Elaborado por»).
- **FR-006**: El firmante esperado de cada paso DEBE quedar fijado al generar el documento, y es la misma persona cuyo nombre imprime el PDF:
  - para el Responsable de consolidación, el usuario designado del periodo;
  - para el Vicerrector y el Rector, el docente que ocupa el cargo vigente, reconocido por su identificación.
- **FR-007**: Un usuario DEBE estar en turno solo si es el firmante esperado del paso en turno. Para el Vicerrector y el Rector, eso es tener la identificación de ese docente. Tener el rol del paso no basta.

**Descarga**

- **FR-008**: El firmante en turno DEBE poder descargar el archivo vigente del documento: el último de su cadena. Hoy, sin firmas, es el original generado en F13-H02.
- **FR-009**: El archivo entregado DEBE tener el SHA-256 registrado como vigente. Si el archivo falta o no coincide, la descarga DEBE fallar con un error del catálogo, sin entregar nada.
- **FR-010**: Cualquier otro usuario que pida el archivo DEBE recibir el 403 común del catálogo, sin saber si el documento existe ni de quién es el turno. Esto incluye a otro firmante de la cadena fuera de su turno y a otro usuario con el mismo rol.

**Identificación de la cuenta**

- **FR-011**: Una cuenta de usuario DEBE poder guardar el número de identificación de la persona, con el formato de los datos de docentes. Ninguna otra cuenta DEBE tener la misma identificación.
- **FR-012**: Los usuarios de la demostración DEBEN cargarse con la identificación de quien representan en los datos de prueba: el Rectorado y el Vicerrectorado, la de los docentes con esos cargos vigentes. La carga DEBE seguir siendo idempotente.
- **FR-013**: La identificación NO DEBE aparecer en los tokens ni en las respuestas que no la necesitan. Es un dato personal.

**Errores y seguridad**

- **FR-014**: Los errores nuevos DEBEN estar en el catálogo con sus 8 columnas (principio XIII).
- **FR-015**: El turno y el permiso DEBEN comprobarse en el servidor. La pantalla solo refleja lo que el servidor permite.

### Key Entities

- **Documento firmable** (de F13-H02): ahora también guarda su archivo vigente con su SHA-256 (hoy, el original) y el firmante esperado de cada paso.
- **Firmante esperado:** para cada paso del árbol de firmas de un documento, la persona que debe firmar. Es un usuario (el Responsable designado) o una identificación (la del docente del cargo). Queda fijado al generar el documento.
- **Paso en turno:** el primer paso del árbol sin firma. Las firmas son de F14-H02 y F14-H03.
- **Cuenta de usuario** (de Usuarios): ahora con el número de identificación de la persona, único.
- **Archivo vigente:** el último PDF de la cadena del documento, nombrado por su SHA-256. Nunca se sobrescribe (D-101).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El firmante en turno descarga el PDF en menos de 1 minuto desde que inicia sesión.
- **SC-002**: El 100 % de los archivos descargados tienen el SHA-256 registrado como vigente del documento.
- **SC-003**: En las pruebas, ningún usuario que no sea el firmante esperado del paso en turno ve el documento ni lo descarga. Se prueba con los otros dos firmantes, con otro usuario con el mismo rol y con uno sin rol de firma.
- **SC-004**: «Pendientes de firma» pasa la revisión automática de accesibilidad (WCAG 2.1 AA) sin fallas, con documentos y sin ellos.
- **SC-005**: Los escenarios de F13-H02, F13-H01, F12-H05 y #215 siguen pasando sin cambios.

## Assumptions

- **El primer paso**: sin la subida de firmas (F14-H02), el paso en turno es siempre el primero. La regla del turno se escribe para la cadena completa, y F14-H03 la ejercitará con los pasos siguientes.
- **El firmante esperado se fija al generar**, junto con el nombre impreso (D-101). Los documentos generados antes de esta historia no lo tienen, así que no aparecen como pendientes de nadie. En la demo no hay ninguno, porque 2026-I está en consolidación (research §1).
- **La identificación de la cuenta** se carga con los usuarios de la demostración. No hay pantalla de administración de usuarios que la edite: hoy las cuentas solo se listan. Editarla queda para cuando exista esa administración.
- **El Responsable designado** se reconoce por su cuenta: la designación ya es de un usuario (ADR-003). No necesita su identificación para esta historia. F14-H03 la pedirá para comparar con el certificado.
- **Quién ve la entrada del menú**: los usuarios con alguno de los roles del árbol de firmas, más el docente designado, que recibe los permisos del Responsable mientras dure su designación. Para los demás, la entrada no aparece y la ruta responde como las demás sin permiso.
- **El nombre del archivo descargado** lleva el periodo y la versión (por ejemplo, `distributivo-2026-I-v1.pdf`) para que el firmante lo reconozca. Su identidad es el SHA-256, no el nombre.
- **La descarga no deja entrada en la bitácora**: la bitácora registra los cambios, y descargar no cambia nada (F17-H01).
- **La demo**: 2026-I está en EN_CONSOLIDACION, así que no hay documentos en firma. El recorrido completo se prueba en el perfil local y en la CI.

## Out of Scope

- Subir el PDF firmado y validarlo (F14-H02, F14-H03).
- Rechazar en firma (F14-H04).
- Notificar al firmante en turno y mostrar a otros el estado de la cadena (F16-H01, F16-H02).
- Descargar el PDF desde «Consolidado del periodo» o por quien no está en turno.
- Administrar las cuentas de usuario y editar su identificación.
- Rehacer el documento si cambian los firmantes (F12-H04).
