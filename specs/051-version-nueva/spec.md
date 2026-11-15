# Feature Specification: Crear una versión nueva y anular las firmas tras un rechazo en firma

**Feature Branch**: `051-version-nueva`

**Created**: 2026-11-15

**Status**: Draft

**Input**: User description: "F12-H04 · Crear una versión nueva y anular las firmas tras un rechazo en firma (#59). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md, docs/decisiones.md (D-01, D-83, D-98, D-99, D-100, D-101, D-103, D-104, D-105, ADR-004, ADR-007, ADR-018, MD-01, MD-04, MD-05) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §2 «Rechazo de un firmante → EN_REVISION_VRA, con nueva versión», tabla distributivo_version y §7 «Rechazo en firma»). Glosario (sdd/modelo_dominio.md): VersionDistributivo con estados EDITABLE, CONGELADA y ANULADA; crearVersionNueva() la ordena Aprobación (MD-04) y registra el motivo; anularFirmas() de DocumentoFirmable; PasoCadena con estado ANULADO; evento FirmasAnuladas de Firma; «Anular firmas: se conservan en el historial». Parte de F14-H04 (D-105): Firma registra el rechazo y publica RechazoEnFirma; Aprobación (CierreDeLaFirma) vuelve a la etapa activa anterior como sistema; aprobar otra vez da 409 VALIDACIONOCS_REQUIERE_VERSION_NUEVA mientras no haya versión nueva. Decisiones del autor: (1) la versión nueva se crea automáticamente al rechazar: cuando Aprobación recibe el rechazo, como el sistema, junto con la vuelta a EN_REVISION_VRA; la versión congelada pasa a ANULADA y se conserva, y la nueva queda EDITABLE con las mismas asignaciones y el motivo «rechazo en firma»; con la versión nueva el bloqueo de D-105 deja de aplicar; (2) Aprobación ordena las dos cosas: a Distributivo (Generación) crearVersionNueva() y a Firma anularFirmas() de la versión rechazada; Firma marca como anuladas las firmas aceptadas (F1, F2…) sin borrarlas y publica FirmasAnuladas; idempotente; (3) los parciales de la versión nueva quedan como estaban (ENVIADOS), con el consolidado y su informe: el Vicerrector puede aprobar otra vez de inmediato, lo que congela la versión nueva y genera un documento nuevo con su cadena de firmas desde F1, o devolver parciales (F12-H05) si hay que cambiar algo. La SPA muestra el número de la versión vigente y que la anterior quedó anulada por el rechazo; las firmas anuladas siguen consultables. Cada paso queda en la bitácora. Fuera de alcance: la devolución desde EN_OCS con firmas, la reforma (F18-H02), la vista de historial y comparación de versiones (RF-42), los ajustes del Vicerrector (RF-35) y las notificaciones (F16)."

**Origen**: issue #59 · historia (`tipo:historia`, `prioridad:must`, `RF-34`, `RF-59`) · feature F12 (#11) · milestone «Ola 3 · Aprobado y firmado» · servicio 06-validacion-ocs · tamaño M. Depende de F14-H04 (#65) y F13-H01 (#60), terminadas.

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero que un rechazo en firma cree una versión nueva y anule las firmas anteriores, para que nadie firme una versión distinta de la que se aprobó.

**Requerimientos que cubre**:
- **RF-59:** el versionado interno: tras un rechazo con firmas, una versión nueva y las firmas de la anterior invalidadas, conservadas en el historial. Su criterio en la ERS: «Una devolución tras la firma del Vicerrector obliga a recoger de nuevo las firmas sobre la nueva versión.»
- **RF-34:** la transición del rechazo queda auditada, y el flujo sigue con la versión nueva.

**Decisiones que aplica**:
- Diseño de firmas §2 y §7: el rechazo de un firmante lleva a EN_REVISION_VRA «con nueva versión»; la congelada pasa a ANULADA y se conserva.
- MD-01 y D-83: la versión es el agregado de todas las asignaciones; la nueva las copia.
- MD-04: Aprobación ordena a Distributivo congelar y crear la versión nueva. MD-05: Aprobación ordena a Firma; Firma no conoce el flujo.
- D-100 y D-101: aprobar congela la versión y genera su documento firmable con la cadena desde F1.
- D-105: el rechazo en firma y el bloqueo de aprobar sin versión nueva, que esta historia levanta.
- ADR-018: los avisos entre servicios se reintentan hasta entregarse.

**Principios de la constitución**: II, IV, V, VII, VIII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-15

- Q: ¿Cuándo se crea la versión nueva? → A: Al rechazar, sin que nadie la pida: cuando Aprobación recibe el rechazo en firma, como el sistema, en el mismo paso que vuelve a EN_REVISION_VRA. Es lo que dice el criterio del issue.
- Q: ¿Cómo se anulan las firmas? → A: Las ordena Aprobación, como dice el glosario: a Distributivo, crear la versión nueva; a Firma, anular las firmas de la versión rechazada. Firma las marca anuladas sin borrarlas y avisa. Sirve igual para una devolución futura con firmas.
- Q: ¿En qué estado quedan los parciales de la versión nueva? → A: Como estaban: enviados, con el consolidado y su informe. El Vicerrector puede aprobar otra vez de inmediato o devolver parciales (F12-H05) si hay que cambiar algo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El rechazo en firma crea una versión nueva (Priority: P1)

Como Vicerrector académico, quiero que, al rechazarse en firma, la versión congelada quede anulada y exista una versión nueva editable con las mismas asignaciones, para seguir trabajando sin tocar la versión que se firmó.

**Why this priority**: es el criterio del issue. Sin la versión nueva, el distributivo rechazado se queda en EN_REVISION_VRA sin salida (D-105).

**Independent Test**: con F1 y F2 aceptadas, el Rector rechaza en F3; sin que nadie haga nada más, la versión 1 queda ANULADA, existe la versión 2 EDITABLE con las mismas asignaciones, y el distributivo está en EN_REVISION_VRA con la versión 2.

**Acceptance Scenarios**:

1. **Dado** una versión congelada con F1 y F2 aceptadas, **cuando** el Rector rechaza en firma con una observación, **entonces** el distributivo vuelve a EN_REVISION_VRA, **y** la versión congelada queda ANULADA y se conserva, **y** se crea una versión nueva EDITABLE con las mismas asignaciones. *(issue)*
2. **Dado** la versión nueva, **cuando** se compara con la anulada, **entonces** tiene las mismas propuestas, actividades no lectivas, justificaciones, parciales (enviados), consolidación e informe; su número es el siguiente y su motivo es «rechazo en firma».
3. **Dado** la versión anulada, **cuando** se consulta, **entonces** sigue igual que cuando se congeló, con su hash de contenido, y no admite cambios.
4. **Dado** el rechazo en F1, sin ninguna firma aceptada, **cuando** se registra, **entonces** también se anula la versión y se crea la nueva: la versión que se iba a firmar ya no es la que seguirá.
5. **Dado** que el aviso del rechazo se entrega dos veces o falla y se reintenta, **cuando** llega, **entonces** hay una sola versión nueva y una sola transición.
6. **Dado** la versión nueva creada, **cuando** se consulta la bitácora, **entonces** tiene la anulación de la versión y la creación de la nueva, con su motivo, por el sistema.

---

### User Story 2 - Las firmas de la versión rechazada quedan anuladas (Priority: P1)

Como Vicerrector académico, quiero que las firmas aceptadas de la versión rechazada queden anuladas y se conserven, para que nadie las tome como firmas de la versión nueva.

**Why this priority**: es la otra mitad del criterio del issue («F1 y F2 quedan anuladas y hay que recogerlas de nuevo sobre la versión nueva»).

**Independent Test**: tras el rechazo en F3, las firmas F1 y F2 del documento de la versión 1 están anuladas, con sus archivos y validaciones intactos; al aprobar la versión 2, su documento pide F1 otra vez.

**Acceptance Scenarios**:

1. **Dado** el rechazo con F1 y F2 aceptadas, **cuando** Aprobación ordena anular las firmas, **entonces** F1 y F2 quedan anuladas, **y** se conservan con quién, cuándo, el archivo y su validación. *(issue)*
2. **Dado** las firmas anuladas, **cuando** se consulta el documento de la versión anulada, **entonces** dice que está anulado y que sus firmas están anuladas; nadie está en turno.
3. **Dado** la versión nueva aprobada otra vez, **cuando** se genera su documento, **entonces** su cadena empieza en F1, sin ninguna firma de la versión anulada, **y** «Pendientes de firma» muestra solo el documento nuevo. *(issue)*
4. **Dado** que la orden de anular llega dos veces, **cuando** se procesa, **entonces** el resultado es el mismo y la bitácora tiene una sola anulación.
5. **Dado** la anulación, **cuando** se consulta la bitácora, **entonces** tiene la anulación de las firmas, con el documento, la versión, cuántas firmas y el motivo.

---

### User Story 3 - El Vicerrector sigue con la versión nueva (Priority: P1)

Como Vicerrector académico, quiero ver que la versión anterior quedó anulada por el rechazo y poder aprobar la versión nueva o devolver parciales, para que el distributivo vuelva a firma.

**Why this priority**: sin esto la versión nueva existe pero el flujo no avanza; levanta el bloqueo de D-105.

**Independent Test**: en «Consolidado del periodo», el Vicerrector ve la versión 2, el aviso de que la versión 1 se anuló por el rechazo (con la observación), y «Aprobar el contenido»; aprueba y la versión 2 queda congelada, con su documento y F1 en turno.

**Acceptance Scenarios**:

1. **Dado** la versión nueva, **cuando** el Vicerrector consulta el consolidado y el estado del flujo, **entonces** ve el número de la versión vigente y que la anterior quedó anulada por el rechazo en firma, con la observación, quién rechazó y en qué paso.
2. **Dado** la versión nueva, **cuando** el Vicerrector aprueba el contenido, **entonces** ya no recibe el error de versión nueva: la versión nueva queda congelada, el distributivo pasa a CONGELADO_EN_FIRMA y se genera su documento.
3. **Dado** la versión nueva, **cuando** el Vicerrector devuelve parciales (F12-H05), **entonces** la devolución funciona igual que sin rechazo, sobre la versión nueva.
4. **Dado** que la versión nueva no se pudo crear (el aviso todavía no llegó o falló), **cuando** el Vicerrector aprueba, **entonces** sigue recibiendo el error de versión nueva de D-105.
5. **Dado** un distributivo que pasó por firma sin rechazo, **cuando** se consulta, **entonces** nada cambia respecto de F14-H03.

---

### Edge Cases

- **Mientras se crea la versión nueva**, el consolidado sigue sin admitir cambios: desde EN_REVISION_VRA el consolidado no cambia (D-99), ni en la versión anulada ni en la nueva, hasta que se devuelvan parciales.
- **El aviso llega con el distributivo fuera de CONGELADO_EN_FIRMA o con otra versión**: no se crea versión ni se anulan firmas; queda registrado sin reintentar para siempre (como en F14-H04).
- **La anulación de firmas falla después de crear la versión**: se reintenta hasta completarse; la versión nueva no se crea dos veces.
- **El documento de la versión anulada** sigue rechazado y cerrado: no acepta subidas ni rechazos. Su archivo vigente y sus firmas se pueden consultar.
- **Un segundo rechazo**, sobre el documento de la versión nueva: crea la versión siguiente y anula las firmas de la nueva, igual que el primero.
- **La versión nueva antes de aprobarla**: no tiene documento firmable; la consulta del documento del periodo muestra el de la versión anulada, como anulado, hasta que se apruebe otra vez.
- **Las inconsistencias y su panel** se calculan sobre la versión vigente, la nueva, con las mismas justificaciones.

## Requirements *(mandatory)*

### Functional Requirements

**La versión nueva**

- **FR-001**: Al recibir un rechazo en firma de la versión congelada, Aprobación DEBE ordenar a Distributivo, como el sistema, crear la versión nueva, en el mismo procesamiento que vuelve a la etapa activa anterior (D-105).
- **FR-002**: Distributivo DEBE pasar la versión congelada a ANULADA y crear la versión siguiente EDITABLE, con el motivo «rechazo en firma», quién (el sistema) y cuándo.
- **FR-003**: La versión nueva DEBE tener las mismas asignaciones que la anulada: propuestas, actividades no lectivas, justificaciones de inconsistencias, parciales con su estado, consolidación e informe sustentado.
- **FR-004**: La versión anulada NO DEBE cambiar ni borrarse: conserva su contenido, su hash de contenido y su congelación (principio VII).
- **FR-005**: Crear la versión nueva DEBE ser idempotente: un aviso repetido o reintentado no crea otra versión ni otra transición.
- **FR-006**: Una vez creada la versión nueva, aprobar el contenido o enviar a revisión cuando eso congela DEBE funcionar como antes del rechazo, sobre la versión nueva; el error de versión nueva de D-105 solo aplica mientras la versión rechazada sigue siendo la vigente.

**La anulación de las firmas**

- **FR-007**: Aprobación DEBE ordenar a Firma anular las firmas de la versión rechazada.
- **FR-008**: Firma DEBE marcar como anuladas las firmas aceptadas del documento de esa versión, sin borrarlas ni cambiar sus archivos ni sus validaciones, y el documento como anulado.
- **FR-009**: Anular DEBE ser idempotente, y DEBE avisar que las firmas se anularon, con el periodo, la versión y cuántas.
- **FR-010**: El documento anulado NO DEBE tener a nadie en turno ni aparecer en «Pendientes de firma».
- **FR-011**: El documento de la versión nueva, al aprobarla, DEBE empezar su cadena en F1, sin firmas de la versión anulada.

**Fiabilidad y auditoría**

- **FR-012**: Si una de las dos órdenes falla, DEBE reintentarse hasta completarse, sin repetir la que ya se cumplió.
- **FR-013**: Si el distributivo ya no está en la etapa a la que volvió por ese rechazo, o la versión del aviso no es la congelada, NO DEBE crearse versión ni anularse firmas, y DEBE quedar registrado sin reintentar para siempre.
- **FR-014**: La bitácora DEBE tener la anulación de la versión y la creación de la nueva (con su motivo), y la anulación de las firmas (con el documento, la versión y cuántas).

**Pantalla, errores y seguridad**

- **FR-015**: «Consolidado del periodo» y el estado del flujo DEBEN mostrar el número de la versión vigente y que la anterior quedó anulada por el rechazo en firma, con la observación, quién rechazó y en qué paso; «Aprobar el contenido» vuelve a ofrecerse a quien puede aprobar.
- **FR-016**: Los errores nuevos, si los hay, DEBEN estar en el catálogo con sus 8 columnas (principio XIII).
- **FR-017**: Crear la versión y anular las firmas DEBEN ser órdenes internas del sistema: ningún usuario las pide por la API.

### Key Entities

- **Versión del distributivo** (de Distributivo): número, estado (EDITABLE, CONGELADA, ANULADA), motivo de su creación, quién y cuándo. La anulada se conserva; la nueva copia las asignaciones.
- **Anulación de firmas** (nueva, de Firma): el documento, la versión, cuándo, por qué y cuántas firmas. Las firmas anuladas se conservan.
- **Aviso de firmas anuladas** (nuevo): de Firma, con el periodo, la versión y cuántas.
- **Rechazo en firma** (de F14-H04): dispara la versión nueva y la anulación.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el rechazo registrado, en menos de 30 segundos y sin que nadie haga nada, la versión rechazada está ANULADA, existe la versión nueva EDITABLE y las firmas de la rechazada están anuladas.
- **SC-002**: En las pruebas, el 100 % de las asignaciones de la versión anulada están en la nueva, y la anulada no cambia.
- **SC-003**: El 100 % de los avisos repetidos o reintentados terminan con una sola versión nueva y una sola anulación.
- **SC-004**: Después del rechazo, el Vicerrector aprueba la versión nueva y el documento nuevo tiene F1 en turno, sin firmas de la versión anulada.
- **SC-005**: Los escenarios de F14-H04, F14-H03, F14-H02, F14-H01, F13-H02, F13-H01, F12-H05 y #215 siguen pasando.

## Assumptions

- **Las dos órdenes** las cumple el mismo proceso que recibe el rechazo, con los eventos persistentes entre servicios (ADR-018), como el aviso de cadena completa de F14-H03.
- **El motivo** de la versión nueva es «rechazo en firma»; los otros motivos del glosario (ajuste, devolución, reforma) son de otras historias.
- **La versión 1** de los periodos existentes no tiene motivo: es la inicial.
- **La consolidación copiada** sigue válida: la versión nueva tiene el mismo contenido, así que no queda desactualizada.
- **El hash de contenido** de la versión nueva se calcula al congelarla, como en D-100; puede coincidir con el de la anulada si nada cambió, y el documento nuevo se distingue por su versión.
- **Las firmas anuladas** se consultan en el estado del documento de la versión anulada y en la bitácora; la vista de historial de versiones es de RF-42.
- **En la demo**, 2026-I está en consolidación y no llega a firma.

## Out of Scope

- La devolución desde EN_OCS con firmas.
- La reforma después de publicar (F18-H02).
- La vista de historial y la comparación de versiones (RF-42).
- Los ajustes del Vicerrector en EN_REVISION_VRA (RF-35).
- Notificar la versión nueva o la anulación (F16).
