# Feature Specification: Rechazar en firma con una observación

**Feature Branch**: `050-rechazar-en-firma`

**Created**: 2026-11-15

**Status**: Draft

**Input**: User description: "F14-H04 · Rechazar en firma con una observación (#65). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md, docs/decisiones.md (D-23, D-98, D-100, D-101, D-102, D-103, D-104, ADR-004, ADR-007, ADR-017, ADR-018, MD-05) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §2 «Rechazo de un firmante → EN_REVISION_VRA», §7 «Rechazo en firma» y §9). Parte de F14-H01 a F14-H03: el firmante en turno (FirmanteEnTurno), «Pendientes de firma», la subida con sus siete comprobaciones, validacion_firma, CadenaDeFirmasCompleta y CierreDeLaFirma. Decisiones del autor: (1) el rechazo lo registra Firma: una acción sobre el documento, solo para el firmante en turno con la regla de F14-H01 (a los demás, el 403 común); Firma guarda el rechazo con su observación y quién y cuándo, y publica un evento en la misma transacción; Aprobación lo escucha después de confirmar y pasa el distributivo de CONGELADO_EN_FIRMA a EN_REVISION_VRA como el sistema, con la observación, idempotente, como CadenaDeFirmasCompleta (MD-05: Firma no conoce el flujo); (2) cualquiera en turno puede rechazar (F1, F2 o F3), no solo el Rector; (3) después del rechazo, el documento queda cerrado: no acepta más subidas ni rechazos y nadie está en turno; aprobar el contenido otra vez desde EN_REVISION_VRA se bloquea con un error del catálogo hasta que exista la versión nueva (F12-H04, #59). La observación es obligatoria, de 10 a 500 caracteres como en D-98 (RF-36: un rechazo sin observación no se permite). El Vicerrector ve la observación del rechazo en el estado del flujo. Cada rechazo queda en la bitácora. La SPA: un botón «Rechazar» en cada fila de «Pendientes de firma» con la observación. Fuera de alcance: la versión nueva, la anulación de la versión congelada y de las firmas (F12-H04), la devolución desde EN_REVISION_VRA o EN_OCS, las notificaciones (F16) y la resolución del OCS (F15)."

**Origen**: issue #65 · historia (`tipo:historia`, `prioridad:must`, `RF-59`, `RF-36`) · feature F14 (#13) · milestone «Ola 3 · Aprobado y firmado» · servicio 10-firma · tamaño S. Depende de F14-H01 (#62), terminada. Es la última historia de F14.

**Historia (tal como está en el issue)**: Como Rector, quiero rechazar en firma el documento escribiendo una observación, para que se corrija antes de que yo firme.

**Requerimientos que cubre**:
- **RF-36:** las observaciones, «visibles para quien debe corregir». Su criterio en la ERS: «Una devolución sin observación no se permite.»
- **RF-59:** la parte del rechazo: el documento vuelve a revisión del Vicerrectorado. La versión nueva y la anulación de las firmas son de F12-H04 (#59).

**Decisiones que aplica**:
- Diseño de firmas §2 y §7: el rechazo de un firmante lleva el distributivo a EN_REVISION_VRA y exige una observación.
- MD-05: Firma no conoce el flujo; avisa a Aprobación.
- D-98: la observación de 10 a 500 caracteres, visible en el estado del flujo.
- D-102 y D-104: el firmante en turno y el aviso de Firma a Aprobación.
- ADR-018: los avisos entre servicios se reintentan hasta entregarse.

**Principios de la constitución**: II, IV, V, VII, VIII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-15

- Q: ¿Quién registra el rechazo y cómo llega a Aprobación? → A: Firma, como una acción sobre el documento, con la misma regla de turno que la subida. Guarda el rechazo y avisa a Aprobación, que pasa el distributivo a EN_REVISION_VRA como el sistema, igual que con la cadena completa (MD-05).
- Q: ¿Quién puede rechazar? → A: Cualquier firmante en turno: F1, F2 o F3. El issue dice «el firmante en turno»; la historia nombra al Rector como ejemplo.
- Q: Después del rechazo, ¿qué pasa si el Vicerrector aprueba otra vez? → A: Se bloquea con un error del catálogo hasta que exista la versión nueva (F12-H04). El documento rechazado queda cerrado: no acepta más subidas ni rechazos, y nadie está en turno.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El firmante en turno rechaza con una observación (Priority: P1)

Como firmante en turno (Responsable de consolidación, Vicerrector o Rector), quiero rechazar el documento escribiendo una observación, para que se corrija antes de firmarlo.

**Why this priority**: son los dos escenarios del issue. Sin el rechazo, un firmante que encuentra un error solo puede no firmar, y el distributivo se queda en firma sin salida.

**Independent Test**: con el paso F3 en turno, el Rector rechaza sin observación (no se permite) y luego con una observación (se registra); el documento queda cerrado y el distributivo vuelve a EN_REVISION_VRA.

**Acceptance Scenarios**:

1. **Dado** que soy el firmante en turno, **cuando** rechazo sin escribir una observación, **entonces** el rechazo no se permite y se pide la observación. *(issue)*
2. **Dado** que soy el firmante en turno, **cuando** rechazo con una observación, **entonces** se registra el rechazo en firma, **y** el distributivo vuelve a revisión del Vicerrectorado. *(issue)*
3. **Dado** una observación de menos de 10 o de más de 500 caracteres, **cuando** rechazo, **entonces** no se permite y se dice el largo admitido.
4. **Dado** que no soy el firmante en turno (otro firmante, alguien con el rol pero sin la designación, o alguien sin el permiso de firma), **cuando** intento rechazar, **entonces** recibo el mismo error de acceso que al subir fuera de turno, y nada cambia.
5. **Dado** el rechazo registrado, **cuando** se consulta el documento, **entonces** queda cerrado: nadie está en turno, «Pendientes de firma» ya no lo muestra, y una subida o un segundo rechazo reciben el error de acceso común.
6. **Dado** el rechazo registrado, **cuando** se consulta la bitácora, **entonces** tiene el rechazo, con quién, cuándo, el paso, la versión y la observación.
7. **Dado** F1 o F1 y F2 ya aceptadas, **cuando** el firmante en turno rechaza, **entonces** el rechazo se registra igual; las firmas aceptadas se conservan, sin anularse (F12-H04).

---

### User Story 2 - El distributivo vuelve a revisión con la observación a la vista (Priority: P1)

Como Vicerrector académico, quiero que el distributivo vuelva a mi revisión con la observación del firmante a la vista, para saber qué corregir.

**Why this priority**: es la segunda mitad del segundo escenario del issue. Sin ella, el rechazo queda registrado pero el flujo no se entera.

**Independent Test**: tras el rechazo, el distributivo está en EN_REVISION_VRA con la transición del sistema, el Vicerrector ve la observación en el estado del flujo, y aprobar el contenido otra vez se rechaza con un error del catálogo.

**Acceptance Scenarios**:

1. **Dado** el rechazo registrado, **cuando** se consulta el flujo, **entonces** el distributivo está en EN_REVISION_VRA, con la transición de CONGELADO_EN_FIRMA a EN_REVISION_VRA registrada por el sistema, con la versión y la observación, y en la bitácora.
2. **Dado** el distributivo devuelto por un rechazo en firma, **cuando** el Vicerrector consulta el estado del flujo, **entonces** ve la observación, quién rechazó y en qué paso.
3. **Dado** el distributivo devuelto por un rechazo en firma, **cuando** el Vicerrector aprueba el contenido otra vez, **entonces** se rechaza con un error del catálogo que dice que hace falta una versión nueva, y el distributivo sigue en EN_REVISION_VRA.
4. **Dado** que el aviso a Aprobación falla después de registrar el rechazo, **cuando** se reintenta, **entonces** el distributivo llega a EN_REVISION_VRA una sola vez, sin repetir el rechazo ni duplicar la transición.
5. **Dado** un distributivo que pasó por firma sin rechazo, **cuando** se consulta el flujo, **entonces** nada cambia respecto de F14-H03.

---

### User Story 3 - Rechazar desde «Pendientes de firma» (Priority: P2)

Como firmante en turno, quiero un botón «Rechazar» junto a «Subir el PDF firmado», donde escribo la observación, para rechazar sin salir de la pantalla.

**Why this priority**: la acción ya se puede hacer por la API; la pantalla la pone al alcance del firmante.

**Independent Test**: en «Pendientes de firma», el firmante en turno abre «Rechazar», intenta confirmar sin observación (no se permite), escribe una y confirma; ve el aviso de rechazo registrado y la fila desaparece.

**Acceptance Scenarios**:

1. **Dado** una fila de «Pendientes de firma», **cuando** el firmante en turno elige «Rechazar», **entonces** se le pide la observación, con su largo admitido, y una confirmación.
2. **Dado** la observación vacía o fuera de largo, **cuando** confirma, **entonces** la pantalla no lo deja y dice por qué; el servidor lo comprueba igual.
3. **Dado** el rechazo registrado, **cuando** vuelve a la lista, **entonces** ve un aviso de rechazo registrado y la fila ya no está.
4. **Dado** la pantalla, **cuando** se revisa con un lector de pantalla y el teclado, **entonces** el rechazo se puede hacer completo y cumple WCAG 2.1 AA, como la subida.

---

### Edge Cases

- **Rechazo y subida a la vez del mismo firmante**: gana la que se registra primero. Si gana la firma, el rechazo llega fuera de turno; si gana el rechazo, la subida se rechaza porque el documento está cerrado. Nunca quedan las dos.
- **Una validación en curso cuando llega el rechazo**: al registrar la firma se vuelve a comprobar el documento (F14-H02); si ya está rechazado, la subida no se acepta.
- **El último paso rechaza**: el distributivo vuelve a EN_REVISION_VRA; no pasa a EN_OCS.
- **El aviso llega con el distributivo fuera de CONGELADO_EN_FIRMA o con otra versión**: Aprobación no lo mueve y lo deja registrado, sin reintentar para siempre (como en F14-H03).
- **La observación con espacios al inicio o al final**: se recortan antes de medir el largo, como en D-98.
- **El documento con la cadena completa**: ya no hay nadie en turno; un rechazo recibe el error de acceso común.
- **El Vicerrector, después del rechazo**: hasta F12-H04 no puede aprobar otra vez; puede consultar y ver la observación. La salida de ese estado llega con la versión nueva.

## Requirements *(mandatory)*

### Functional Requirements

**El rechazo**

- **FR-001**: El firmante en turno DEBE poder rechazar el documento con una observación. El turno es el de F14-H01: el primer paso sin firma aceptada y su firmante esperado.
- **FR-002**: La observación DEBE ser obligatoria, de 10 a 500 caracteres después de recortar los espacios del inicio y del final (RF-36, D-98). Si falta o no cabe, el rechazo no se registra y se responde con un error del catálogo.
- **FR-003**: Quien no está en turno, o no tiene el permiso de firma, DEBE recibir el mismo error de acceso que al subir fuera de turno, sin que nada cambie y sin revelar si el documento existe.
- **FR-004**: El rechazo DEBE guardarse con el documento, el paso, la versión, quién, cuándo y la observación. No se modifica ni se borra después (principio VII).
- **FR-005**: Un documento rechazado DEBE quedar cerrado: nadie está en turno, no aparece en «Pendientes de firma», y una subida o un segundo rechazo se responden con el error de acceso común, el mismo de fuera de turno, sin cambiar nada.
- **FR-006**: El rechazo y una subida del mismo documento a la vez NO DEBEN quedar registrados los dos: se registra el primero y el otro se rechaza.
- **FR-007**: Las firmas aceptadas antes del rechazo y el archivo vigente se conservan sin cambios. Anularlas es de F12-H04.
- **FR-008**: Cada rechazo DEBE quedar en la bitácora, con quién, cuándo, el paso, la versión y la observación.

**El aviso y el flujo**

- **FR-009**: Con el rechazo registrado, Firma DEBE avisar a Aprobación que el documento se rechazó, con el periodo, la versión, el paso, quién y la observación, sin conocer el flujo (MD-05).
- **FR-010**: Aprobación DEBE pasar el distributivo de CONGELADO_EN_FIRMA a EN_REVISION_VRA, o a la etapa activa anterior si la configuración del distributivo la desactiva, como una transición del sistema, con la versión y la observación, y registrarla en la bitácora.
- **FR-011**: El aviso DEBE llegar aunque Aprobación falle un momento, una sola vez: el rechazo no se repite y la transición no se duplica.
- **FR-012**: Si el distributivo ya no está en CONGELADO_EN_FIRMA o la versión no es la congelada, Aprobación NO DEBE moverlo, y lo DEBE dejar registrado sin reintentar para siempre.
- **FR-013**: El estado del flujo DEBE mostrar al Vicerrector la observación del rechazo, quién rechazó y en qué paso, mientras el distributivo siga en la etapa a la que volvió por ese rechazo.
- **FR-014**: Aprobar el contenido, o enviar a revisión cuando eso congela la versión (EN_REVISION_VRA desactivada), DEBE rechazarse con un error del catálogo si el distributivo volvió por un rechazo en firma y no hay una versión nueva (F12-H04).

**Pantalla, errores y seguridad**

- **FR-015**: «Pendientes de firma» DEBE ofrecer «Rechazar» en cada fila, con la observación, su largo admitido y una confirmación. Muestra el resultado y quita la fila. Cumple WCAG 2.1 AA.
- **FR-016**: Los errores nuevos DEBEN estar en el catálogo con sus 8 columnas (principio XIII).
- **FR-017**: El turno, la observación y el estado del documento DEBEN comprobarse en el servidor; la pantalla solo ayuda.

### Key Entities

- **Rechazo en firma** (nuevo): el documento, el paso, la versión, quién, cuándo y la observación. Uno por documento como máximo; cierra el documento.
- **Aviso de rechazo en firma** (nuevo): de Firma a Aprobación, con el periodo, la versión, el paso, quién y la observación.
- **Transición a EN_REVISION_VRA** (de Aprobación): la del sistema al rechazarse en firma, con la observación.
- **Firmante en turno** (de F14-H01): un documento rechazado no tiene.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En las pruebas, el 100 % de los rechazos sin observación, con una fuera de largo o de quien no está en turno se rechazan sin cambiar nada, y el 100 % de los del firmante en turno con una observación válida se registran.
- **SC-002**: Con el rechazo registrado, el distributivo está en EN_REVISION_VRA en menos de 30 segundos, sin que nadie lo mueva.
- **SC-003**: Un firmante en turno rechaza desde «Pendientes de firma» en menos de 1 minuto, sin salir de la pantalla.
- **SC-004**: Después de un rechazo, el 100 % de las subidas, de los segundos rechazos y de las aprobaciones del contenido sin versión nueva se rechazan con su error del catálogo.
- **SC-005**: Los escenarios de F14-H03, F14-H02, F14-H01, F13-H02, F13-H01, F12-H05 y #215 siguen pasando.

## Assumptions

- **El permiso** para rechazar es el mismo que para subir la firma: el de firmar el distributivo. No hace falta uno nuevo.
- **La observación** sigue la regla de D-98: 10 a 500 caracteres, recortada.
- **El aviso de rechazo** usa los eventos persistentes entre servicios del mismo proceso, como el aviso de cadena completa de F14-H03, y se entrega después de confirmar el rechazo.
- **La transición a EN_REVISION_VRA** la registra Aprobación como «sistema», con un tipo propio de rechazo en firma, distinto de la devolución de D-98.
- **El documento rechazado** no se descarga desde «Pendientes de firma»: como en F14-H01, solo descarga el firmante en turno, y ya no hay. Se conserva con sus archivos.
- **Hasta F12-H04**, un distributivo rechazado en firma se queda en EN_REVISION_VRA. En la demo, 2026-I está en consolidación y no llega a firma.
- **La notificación** al Vicerrector es de F16-H01; aquí la observación se ve en el estado del flujo y en la bitácora.

## Out of Scope

- Crear la versión nueva, anular la versión congelada y las firmas, y recoger las firmas otra vez (F12-H04).
- Devolver desde EN_REVISION_VRA o desde EN_OCS.
- Notificar el rechazo (F16).
- Registrar la resolución del OCS (F15).
- Deshacer un rechazo.
