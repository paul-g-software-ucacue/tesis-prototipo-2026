# Feature Specification: Devolver un parcial a su carrera con una observación

**Feature Branch**: `043-devolver-parcial`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "F12-H05 · Devolver un parcial a su carrera con una observación (issue #211). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md y docs/decisiones.md; seguir el proceso de aprobación de F12-H02 (ProcesoAprobacion, rol de la etapa desde la copia de la configuración) y la bitácora de F17-H01 (D-90, D-95). Decisiones del autor: (1) al devolver, el distributivo vuelve a BORRADOR_CARRERA; solo el parcial de esa carrera queda DEVUELTO y los demás siguen ENVIADO; cuando la carrera lo reenvía, el distributivo vuelve a EN_CONSOLIDACION con la lógica de F12-H02 y el consolidado queda desactualizado. (2) El coordinador ve la observación en el estado del flujo («Oferta del periodo» y «Consolidado del periodo»), además de la bitácora. (3) La notificación al coordinador (RF-41) se deja a F16-H01, como en D-97. Al cerrar, cronometrar SC-005 de F10-H03 (specs/042-enviar-revision/evidencia.md)."

**Origen**: issue #211 · F12-H05 · sub-issue de F12 (Flujo de etapas configurable con plazos, #11) · milestone «Ola 3 · Aprobado y firmado» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:06-validacion-ocs`, `RF-34`, `RF-36` · tamaño propuesto M · depende de #57 (F12-H02), cerrada. Sale de la especificación de F10-H03 (#55).

**Requerimientos que cubre**:
- **RF-34:** el flujo de estados configurable, «con devolución a cualquier etapa anterior y observaciones». Su criterio en la ERS: «Solo el rol de la etapa actual puede avanzar o devolver el distributivo, y cada transición queda auditada.» Cada etapa dice en la configuración si admite devolución.
- **RF-36:** las observaciones por etapa, «visibles para quien debe corregir». Su criterio en la ERS: «Una devolución sin observación no se permite.»
- **Modelo de dominio:** `ParcialCarrera` tiene los estados EN_ELABORACION, ENVIADO y DEVUELTO; «un parcial ENVIADO no se edita hasta que se devuelve». `ProcesoAprobacion`: «Devolver exige observación y solo va a una etapa anterior activa.» Aprobación ordena a Distributivo devolver un parcial (MD-04).

**Decisiones que aplica**: ADR-003 (el Responsable de consolidación), ADR-007 (esqueleto fijo y etapas opcionales, con `admite_devolucion` por etapa), D-16 (el permiso de aprobación incluye devolver) y D-64 (la carrera del coordinador), D-90, D-94 y D-95 (bitácora; la transición la registra Aprobación), D-97 (sin notificación hasta F16-H01). **Principios de la constitución**: III, IV, V, VII, IX, X, XIII y XIV.

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero devolver el parcial de una carrera con una observación, para que su coordinador corrija una inconsistencia dura antes de enviar a revisión.

**Punto de partida.**
- F12-H02 dejó el proceso de aprobación: la etapa del distributivo, el estado de envío de cada parcial, la regla de que solo el rol de la etapa la mueve y la transición con la versión y el hash en la bitácora. Cuando todas las carreras envían su parcial, el distributivo pasa a la siguiente etapa activa.
- Un parcial enviado no admite propuestas, asignaciones ni quitas «hasta que se devuelva»: F12-H02 dejó la devolución a esta historia.
- F10-H03 dejó el envío a revisión, que exige cero inconsistencias abiertas. En EN_CONSOLIDACION todos los parciales están enviados, así que una inconsistencia dura no se puede corregir: los datos de prueba y la demo se quedan ahí (211 duras en la demo).

## Clarifications

### Session 2026-11-08

- Q: Mientras la carrera corrige, ¿en qué etapa queda el distributivo? → A: Vuelve a BORRADOR_CARRERA. Solo los parciales devueltos quedan DEVUELTO; los demás siguen enviados. Cuando la última carrera devuelta reenvía, el distributivo pasa otra vez a la siguiente etapa activa, como en F12-H02.
- Q: ¿Dónde ve el coordinador la observación? → A: En el estado del flujo, que ya aparece en «Oferta del periodo» y en «Consolidado del periodo», además de la bitácora.
- Q: ¿Se notifica al coordinador? → A: No todavía: lo hará F16-H01, como en D-97.
- Q: ¿Cuántas carreras se devuelven en una devolución? → A: Una o varias a la vez, cada una con su observación. Si no, con duras en varias carreras habría que esperar a que la primera reenvíe para devolver la siguiente.

Esta historia agrega la devolución desde EN_CONSOLIDACION: el Responsable de consolidación devuelve uno o varios parciales con su observación, el distributivo vuelve a BORRADOR_CARRERA y esas carreras pueden corregir y reenviar.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Devolver parciales con su observación (Priority: P1)

Como Responsable de consolidación, quiero devolver el parcial de una o varias carreras, cada una con su observación, para que sus coordinadores corrijan las inconsistencias duras antes de enviar a revisión.

**Why this priority**: es el primer escenario del issue y lo único que deja corregir una inconsistencia dura después de que todas las carreras enviaron.

**Independent Test**: con el distributivo en EN_CONSOLIDACION, devolver dos carreras con su observación como Responsable de consolidación; comprobar que el distributivo queda en BORRADOR_CARRERA, que esas dos carreras están devueltas con su observación, que las demás siguen enviadas y que la bitácora tiene la devolución.

**Acceptance Scenarios**:

1. **Dado** el distributivo en EN_CONSOLIDACION con una inconsistencia dura en una carrera, **cuando** devuelvo el parcial de esa carrera con una observación, **entonces** el parcial queda DEVUELTO y su coordinador puede corregirlo y reenviarlo **y** la transición queda en la bitácora con la observación. *(issue)*
2. **Dado** el distributivo en EN_CONSOLIDACION, **cuando** devuelvo tres carreras, cada una con su observación, **entonces** las tres quedan devueltas, las demás siguen enviadas y el distributivo queda en BORRADOR_CARRERA con una sola transición.
3. **Dado** una devolución hecha, **cuando** se consulta el estado del flujo, **entonces** cada parcial devuelto dice quién lo devolvió, cuándo y su observación.
4. **Dado** un consolidado al día antes de devolver, **cuando** devuelvo un parcial, **entonces** el consolidado queda desactualizado: hay que consolidar otra vez antes de enviar a revisión.

---

### User Story 2 - Corregir y reenviar el parcial devuelto (Priority: P1)

Como Coordinador de carrera, quiero ver por qué me devolvieron el parcial, corregirlo y reenviarlo, para que el distributivo vuelva a consolidación.

**Why this priority**: sin esto la devolución no sirve; es el «puede corregirlo y reenviarlo» del issue.

**Independent Test**: con el parcial de su carrera devuelto, el coordinador ve la observación en «Oferta del periodo», cambia una propuesta, reenvía el parcial y, si era la última carrera devuelta, el distributivo vuelve a EN_CONSOLIDACION.

**Acceptance Scenarios**:

1. **Dado** el parcial de mi carrera devuelto, **cuando** abro «Oferta del periodo», **entonces** veo que está devuelto, quién lo devolvió, cuándo y la observación.
2. **Dado** el parcial de mi carrera devuelto, **cuando** cambio una propuesta o una actividad de mi carrera, **entonces** se guarda: un parcial devuelto se edita como uno en elaboración.
3. **Dado** dos carreras devueltas, **cuando** la primera reenvía, **entonces** el distributivo sigue en BORRADOR_CARRERA; **cuando** reenvía la segunda, **entonces** pasa a la siguiente etapa activa (por defecto, EN_CONSOLIDACION), en la misma operación (F12-H02).
4. **Dado** una carrera que no fue devuelta, **cuando** su coordinador intenta cambiar una propuesta, **entonces** se rechaza como antes: su parcial sigue enviado.

---

### User Story 3 - No devolver sin observación (Priority: P1)

Como institución, quiero que ninguna devolución se haga sin su observación, para que el coordinador sepa qué corregir (RF-36).

**Why this priority**: es el segundo escenario del issue y el criterio de RF-36.

**Independent Test**: intentar devolver una carrera sin observación, con una observación solo de espacios, con una muy corta y con una muy larga; recibir en cada caso un error de validación sin que nada cambie.

**Acceptance Scenarios**:

1. **Dado** el parcial de una carrera en EN_CONSOLIDACION, **cuando** intento devolverlo sin observación, **entonces** se rechaza. *(issue)*
2. **Dado** una devolución de tres carreras en la que una no tiene observación, **cuando** la envío, **entonces** se rechaza entera: ninguna carrera queda devuelta.
3. **Dado** una observación solo con espacios, de menos de 10 caracteres o de más de 500, **cuando** devuelvo, **entonces** se rechaza con un error de validación del campo.

---

### User Story 4 - Solo el rol de la etapa devuelve (Priority: P1)

Como institución, quiero que solo el rol de EN_CONSOLIDACION pueda devolver desde esa etapa, para que la etapa la mueva quien corresponde (RF-34).

**Why this priority**: es el tercer escenario del issue y la regla de F12-H02.

**Independent Test**: con cada rol que no es el de EN_CONSOLIDACION, intentar devolver y recibir 403; consultar el estado del flujo y no ver la acción.

**Acceptance Scenarios**:

1. **Dado** un usuario sin el rol de EN_CONSOLIDACION, **cuando** intenta devolver un parcial, **entonces** recibe un error 403, sin saber qué rol falta. *(issue)*
2. **Dado** el Responsable de consolidación, **cuando** consulta el estado del flujo en EN_CONSOLIDACION, **entonces** ve la acción de devolver; otro rol no la ve.
3. **Dado** el distributivo en otra etapa, **cuando** el Responsable intenta devolver, **entonces** se rechaza con 409: esta historia solo devuelve desde EN_CONSOLIDACION.
4. **Dado** EN_CONSOLIDACION configurada sin devolución (`admite_devolucion: false`), **cuando** el Responsable intenta devolver, **entonces** se rechaza con 409 y la acción no aparece.

---

### Edge Cases

- **Una carrera repetida en la misma devolución:** se rechaza como un error de validación.
- **Una devolución sin carreras:** se rechaza como un error de validación.
- **Una carrera que no tiene parcial en el distributivo:** se rechaza con el error de parcial no encontrado, y no se devuelve ninguna.
- **Devolver todas las carreras:** se permite; el distributivo vuelve a BORRADOR_CARRERA con todos los parciales devueltos.
- **Dos devoluciones a la vez:** pasa una sola; la otra se rechaza porque el distributivo ya no está en EN_CONSOLIDACION.
- **Una devolución y un envío a revisión a la vez:** pasa solo uno de los dos; el otro se rechaza por la etapa.
- **BORRADOR_UNIDADES activa:** cuando la última carrera devuelta reenvía, el distributivo pasa a la siguiente etapa activa, que es BORRADOR_UNIDADES, como en F12-H02.
- **Un periodo cerrado:** no se devuelve.
- **Generación no responde** al devolver: no se devuelve nada, el distributivo sigue en EN_CONSOLIDACION y no queda transición ni entrada. Si Generación ya había marcado algún parcial, el estado del flujo lo muestra como pendiente de confirmar, como el envío de F12-H02.
- **El coordinador reenvía sin cambiar nada:** se permite; el consolidado ya quedó desactualizado al devolver, así que hay que consolidar otra vez.
- **Una segunda devolución de la misma carrera** después de reenviarla: se permite; el estado del flujo muestra la observación de la última devolución, y la bitácora conserva todas.
- **El usuario pierde el rol durante la sesión:** pierde el permiso en la próxima renovación del token (D-62); el servidor valida el rol en cada devolución.

## Requirements *(mandatory)*

### Functional Requirements

**Devolver**

- **FR-001**: El Responsable de consolidación DEBE poder devolver, en una sola operación, el parcial de una o varias carreras del distributivo cuando está en EN_CONSOLIDACION, cada una con su observación.
- **FR-002**: Al devolver, cada parcial devuelto DEBE quedar DEVUELTO, los demás DEBEN seguir enviados y el distributivo DEBE pasar a BORRADOR_CARRERA, en la misma respuesta y con una sola transición.
- **FR-003**: Solo un usuario con el rol de EN_CONSOLIDACION en la configuración del distributivo DEBE poder devolver (regla de F12-H02). Cualquier otro recibe `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403), sin saber qué rol falta.
- **FR-004**: La devolución DEBE rechazarse con un 409 del catálogo si el distributivo no está en EN_CONSOLIDACION o si EN_CONSOLIDACION no admite devolución en la configuración del distributivo.
- **FR-005**: La devolución DEBE ser todo o nada: si una carrera no se puede devolver, ninguna se devuelve.
- **FR-006**: Devolver DEBE dejar el consolidado desactualizado.

**Observación**

- **FR-007**: Cada carrera devuelta DEBE llevar una observación de 10 a 500 caracteres; una observación solo con espacios cuenta como vacía. Sin ella, la devolución se rechaza con un error de validación del campo (RF-36).
- **FR-008**: Una carrera repetida o una devolución sin carreras DEBEN rechazarse con un error de validación.

**Corregir y reenviar**

- **FR-009**: Un parcial devuelto DEBE admitir propuestas, asignaciones y quitas de su carrera, como uno en elaboración.
- **FR-010**: El coordinador DEBE poder reenviar el parcial devuelto de su carrera con la operación de enviar de F12-H02. Cuando todos los parciales vuelven a estar enviados, el distributivo DEBE pasar a la siguiente etapa activa, en la misma operación.

**Auditoría**

- **FR-011**: La devolución DEBE quedar en la bitácora como una transición, con quién, cuándo, desde qué estado, hacia cuál, la versión y el hash (RF-44, D-90, D-95), y con cada carrera devuelta y su observación.
- **FR-012**: La devolución DEBE quedar en el historial de transiciones del distributivo.
- **FR-013**: Un intento rechazado (por rol, por etapa, por configuración, por validación o por periodo cerrado) NO DEBE dejar transición ni entrada, ni devolver ningún parcial.
- **FR-014**: La devolución no crea una versión nueva del distributivo: no hay firmas que anular (RF-59 aplica después de firmar).

**Consulta y errores**

- **FR-015**: El estado del flujo DEBE mostrar, en cada parcial devuelto, quién lo devolvió, cuándo y la observación de su última devolución, a quien puede leer el flujo.
- **FR-016**: Quien consulta el estado del flujo en EN_CONSOLIDACION DEBE ver la acción de devolver solo si tiene el rol de la etapa y la etapa admite devolución.
- **FR-017**: La pantalla «Consolidado del periodo» DEBE permitir al Responsable elegir una o varias carreras y escribir la observación de cada una antes de confirmar la devolución.
- **FR-018**: Cada error nuevo DEBE estar en el catálogo, con sus 8 columnas (principio XIII).

### Key Entities

- **Proceso de aprobación** (`ProcesoAprobacion`, de F12-H02): su etapa, el estado de envío de cada parcial y el historial de transiciones. Esta historia le agrega la devolución desde EN_CONSOLIDACION.
- **Transición** (`Transicion`, de F12-H02): desde qué estado, hacia cuál, quién, cuándo, la versión y el hash. La devolución agrega las carreras devueltas con su observación.
- **Observación**: el texto que explica qué corregir, de una carrera en una devolución, con quién y cuándo.
- **Parcial de carrera** (`ParcialCarrera`, de Generación): la parte de una carrera, con su estado (en elaboración, enviado o devuelto).
- **Etapa** (`Etapa`, de F12-H01): con su rol y si admite devolución.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los intentos de devolver con alguna carrera sin una observación válida se rechazan, sin devolver ninguna carrera.
- **SC-002**: Tras una devolución, el distributivo está en BORRADOR_CARRERA en la misma respuesta, con exactamente una transición en la bitácora que lleva las carreras y sus observaciones; cuando la última carrera devuelta reenvía, vuelve a la siguiente etapa activa con exactamente una transición de paso.
- **SC-003**: El 100 % de los intentos de devolver con un rol que no es el de EN_CONSOLIDACION reciben 403: los 9 roles de la matriz D-16 que no son el de la etapa.
- **SC-004**: Con dos devoluciones simultáneas, el distributivo sale de EN_CONSOLIDACION exactamente una vez.
- **SC-005**: El Responsable de consolidación devuelve una carrera con su observación en menos de 1 minuto desde que abre «Consolidado del periodo».
- **SC-006**: El coordinador ve la observación de su parcial devuelto en «Oferta del periodo» sin abrir la bitácora.
- **SC-007**: Con esta historia, SC-005 de F10-H03 (enviar a revisión en menos de 30 segundos con el consolidado listo) se cronometra y se anota en su evidencia, o queda explicado por qué los datos de prueba aún no llegan a un consolidado listo.

## Assumptions

- **Notificaciones:** avisar al coordinador es de F16-H01 (D-97). Esta historia no notifica; el coordinador ve la observación en el estado del flujo.
- **Solo desde EN_CONSOLIDACION:** devolver desde EN_REVISION_VRA, desde la firma o desde el OCS es de las historias de esas etapas (F13-H01, F12-H04, F14-H04). Esta historia solo devuelve desde EN_CONSOLIDACION a BORRADOR_CARRERA, la etapa anterior activa del esqueleto.
- **Sin versión nueva:** en EN_CONSOLIDACION no hay firmas; la versión del distributivo no cambia (RF-59 aplica después de firmar, F12-H04).
- **La longitud de la observación:** de 10 a 500 caracteres, como la justificación de una blanda (F10-H01).
- **Las unidades:** no tienen parcial propio (BORRADOR_UNIDADES desactivada por defecto); solo se devuelven parciales de carrera.
- **Corregir las duras de la demo:** devolver no corrige nada por sí solo; las carreras deben proponer y asignar lo que falta. Si los datos de prueba tienen más duras de las que es razonable corregir a mano, el plan decide cómo llegar a un consolidado listo para medir SC-005 de F10-H03 (SC-007).

## Out of Scope

- Notificar al coordinador (F16-H01).
- Devolver desde otras etapas (EN_REVISION_VRA, CONGELADO_EN_FIRMA, EN_OCS) y crear una versión nueva con anulación de firmas (F13-H01, F12-H04, F14-H04).
- Observaciones por docente o por asignatura (RF-36): esta historia guarda una observación por carrera devuelta.
- Los plazos de la etapa (F12-H03).
