# Feature Specification: Iniciar una reforma de la versión publicada

**Feature Branch**: `056-reforma-version-publicada`

**Created**: 2026-11-20

**Status**: Draft

**Input**: User description: "F18-H02"

**Origen**: issue #72 · historia (`tipo:historia`, `prioridad:must`, `RF-40`) · feature F18 (#17) · milestone «Ola 3 · Aprobado y firmado» · servicio 06-validacion-ocs · tamaño M. Depende de F18-H01 (#71), terminada. Es la última historia de F18 y de la ola 3.

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero iniciar una reforma cuando hay que cambiar un distributivo publicado, para corregirlo con un ciclo de aprobación completo.

**Requerimientos que cubre**:
- **RF-40:** bloquear la versión aprobada y firmada; cualquier cambio posterior crea una reforma con un nuevo ciclo de aprobación.
- **Diseño de firmas §2 y §7:** «Reforma después de publicar: se crea una versión nueva marcada como "reforma", con la misma cadena completa. La versión publicada anterior queda como histórica.» El diagrama de estados del distributivo: `PUBLICADO --> EN_CONSOLIDACION : Reforma (nueva versión, cadena completa)`.
- **Modelo de dominio:** `VersionDistributivo`, «una versión nueva copia las asignaciones de la anterior y registra el motivo (ajuste, devolución, reforma o rechazo en firma)»; `ProcesoAprobacion`, «una versión publicada solo cambia con una reforma», con la operación `iniciarReforma()`. El diagrama de estados de la versión: una versión CONGELADA «se publica y queda inmutable (histórica si luego hay reforma)».
- **Glosario:** «Reforma: cambio después de publicar. Crea una versión nueva con la cadena completa de firmas» (`iniciarReforma()`, Aprobación).

**Decisiones que aplica**:
- D-95: las transiciones las registra Aprobación.
- D-100 y D-101: la versión se congela al aprobar el contenido, y sobre ella se genera el documento y se firma.
- D-104: la cadena de firmas completa lleva a EN_OCS.
- D-106: la versión nueva la crea Generación por orden de Aprobación, copiando la anterior con su motivo.
- D-107: la resolución del OCS es única por versión aprobada, «deja lugar a la reforma».
- D-108, D-110 y D-111: las notificaciones de cada transición, por rol o por persona, y la publicación.
- D-16 y D-63: los permisos de la matriz y los diez roles.

**Principios de la constitución**: II, III, IV, V, VI, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- El distributivo publicado (F18-H01) está en PUBLICADO, la última etapa del esqueleto fijo (ADR-007), sin ninguna acción para nadie.
- La versión publicada es la versión congelada que llegó a APROBADO, con sus tres firmas válidas y su resolución del OCS.
- Generación ya sabe crear una versión nueva que copia la anterior, con su motivo (D-106), para el rechazo en firma. En ese caso la anterior queda ANULADA y Firma anula sus firmas.
- Desde EN_CONSOLIDACION, quien consolida puede devolver parciales a las carreras (F12-H05), volver a consolidar y enviar a revisión; desde ahí, el ciclo completo ya existe: aprobar el contenido, la cadena de firmas, la resolución del OCS y la publicación.

Esta historia agrega:
- **Iniciar una reforma:** el Vicerrector, con el distributivo publicado, inicia una reforma con su motivo, y el distributivo vuelve a un ciclo de aprobación completo con una versión nueva, marcada como reforma.
- **La versión publicada anterior, histórica:** conserva sus firmas, su resolución y su publicación; no se anula.
- **La reforma en el estado del flujo y en la bitácora**, y su notificación.

## Clarifications

### Session 2026-11-20

- Q: ¿A qué etapa vuelve el distributivo y quién hace los cambios? → A: A EN_CONSOLIDACION, como el diagrama de estados del diseño. Quien consolida devuelve a cada carrera lo que tiene que cambiar (F12-H05), vuelve a consolidar y envía a revisión; desde ahí sigue el ciclo de siempre. Si EN_CONSOLIDACION está desactivada en la configuración del periodo, a la siguiente etapa activa.
- Q: Mientras dura la reforma, ¿la versión publicada anterior sigue siendo la oficial del periodo? → A: Sí. Sigue siendo la publicada vigente hasta que se publique la versión de la reforma; recién entonces queda como histórica. El periodo nunca se queda sin un distributivo oficial.
- Q: ¿A quién se notifica al iniciar una reforma? → A: Al rol de la etapa a la que vuelve el distributivo (por defecto, el Responsable de consolidación, que empieza a trabajar) y, además, a los docentes, Talento Humano y las coordinaciones que recibieron la publicación, porque el distributivo que se les avisó va a cambiar. Una sola notificación por usuario, nunca a quien inició la reforma.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El Vicerrector inicia una reforma (Priority: P1)

Como Vicerrector académico, quiero iniciar una reforma de un distributivo publicado, con su motivo, para corregirlo con un ciclo de aprobación completo.

**Why this priority**: es el escenario del issue. Sin la reforma, un distributivo publicado no se puede cambiar nunca.

**Independent Test**:
1. Con el distributivo en PUBLICADO, `vicerrectorado.demo` abre la pantalla del distributivo.
2. Pulsa «Iniciar una reforma», escribe el motivo y confirma.
3. El distributivo vuelve a EN_CONSOLIDACION con la versión nueva, marcada como reforma, y quien consolida puede devolver parciales a las carreras.

**Acceptance Scenarios**:

1. **Dado** un distributivo publicado, **cuando** inicio una reforma, **entonces** se crea una versión nueva marcada como reforma, con la cadena completa de firmas, **y** la versión publicada anterior queda como histórica cuando se publica la de la reforma. *(issue; aclaración del 2026-11-20)*
2. **Dado** el distributivo publicado, **cuando** inicio una reforma, **entonces** la versión nueva copia todo el contenido de la publicada (propuestas, actividades, justificaciones, la consolidación y el informe), y el distributivo vuelve a EN_CONSOLIDACION.
3. **Dado** la reforma iniciada, **cuando** quien consolida abre el distributivo, **entonces** puede devolver parciales a las carreras que tienen que cambiar, volver a consolidar y enviar a revisión, como en cualquier consolidación.
4. **Dado** una reforma iniciada, **cuando** el distributivo vuelve a pasar por el ciclo, **entonces** pasa por todas sus etapas activas: revisión, aprobación del contenido, la cadena completa de firmas F1, F2 y F3 sobre la versión nueva, la resolución del OCS y la publicación.
5. **Dado** el distributivo en cualquier etapa que no es PUBLICADO, **cuando** intento iniciar una reforma, **entonces** se rechaza con un 409 del catálogo que dice por qué, y nada cambia.
6. **Dado** un usuario que no es el Vicerrector, por ejemplo la Secretaría general académica, **cuando** intenta iniciar una reforma, **entonces** recibe el 403 común y nada cambia.
7. **Dado** una reforma sin motivo, o con un motivo de menos de 10 o más de 500 caracteres, **cuando** la inicio, **entonces** se rechaza con un 400 del catálogo, y nada cambia.
8. **Dado** una reforma iniciada, **cuando** se registra, **entonces** queda una entrada en la bitácora con quién la inició, cuándo, el periodo, la versión publicada, la versión nueva y el motivo.
9. **Dado** dos reformas iniciadas a la vez sobre el mismo distributivo, **cuando** se registran, **entonces** solo una crea la versión nueva; la otra recibe el 409 de la etapa.

---

### User Story 2 - La versión publicada anterior queda como histórica (Priority: P1)

Como cualquier usuario que sigue el distributivo, quiero ver que hay una reforma en curso y cuál fue la versión publicada, para saber qué versión rige y qué se está cambiando.

**Why this priority**: es la segunda parte del escenario del issue; sin ella, la reforma borraría de la vista la versión oficial.

**Independent Test**:
1. Con la reforma iniciada, cualquier usuario que lee el estado del flujo lo abre.
2. Ve que hay una reforma en curso, con su motivo, quién la inició y cuándo, y la versión publicada anterior con su constancia.

**Acceptance Scenarios**:

1. **Dado** una reforma iniciada, **cuando** cualquiera que lee el estado del flujo lo abre, **entonces** ve la reforma en curso (quién, cuándo y el motivo) y la versión publicada anterior, con quién la publicó y cuándo.
2. **Dado** la versión publicada anterior, **cuando** se inicia la reforma, **entonces** conserva sus tres firmas, su documento, su resolución del OCS y su publicación; no se anula, y nada de eso se borra ni se cambia.
3. **Dado** la reforma en curso, **cuando** alguien consulta cuál es la versión publicada vigente del periodo, **entonces** es la versión publicada anterior, que sigue siendo la oficial hasta que se publique la de la reforma. *(aclaración del 2026-11-20)*
4. **Dado** la versión nueva de la reforma publicada, **cuando** se completa, **entonces** pasa a ser la versión publicada del periodo, y la anterior queda como histórica.

---

### User Story 3 - Los involucrados reciben la notificación de la reforma (Priority: P2)

Como usuario a quien le toca la siguiente etapa, quiero recibir una notificación cuando se inicia una reforma, para empezar a corregir.

**Why this priority**: RF-41 pide notificar cada cambio de estado; sin ella, nadie sabe que el distributivo volvió al ciclo.

**Independent Test**:
1. `vicerrectorado.demo` inicia la reforma.
2. Quienes corresponde ven en su campana la notificación de la reforma, con el motivo; el Vicerrector que la inició no la recibe.

**Acceptance Scenarios**:

1. **Dado** una reforma iniciada, **cuando** se completa, **entonces** reciben una notificación interna el rol de la etapa a la que vuelve (por defecto, el Responsable de consolidación) y los docentes, Talento Humano y las coordinaciones. *(aclaración del 2026-11-20)*
2. **Dado** un usuario con más de uno de esos roles, **cuando** se inicia la reforma, **entonces** recibe una sola notificación.
3. **Dado** la notificación de la reforma, **cuando** el destinatario la lee, **entonces** dice que se inició una reforma del distributivo, el periodo, la versión nueva, el motivo, quién la inició y cuándo.
4. **Dado** el Vicerrector que inició la reforma, **cuando** se completa, **entonces** no recibe su propia notificación.
5. **Dado** el proceso de identidad detenido, **cuando** se inicia la reforma, **entonces** la reforma se completa sin error, y la notificación llega cuando el proceso vuelve, una sola vez.

---

### Edge Cases

- **Una reforma de una reforma:** la versión nueva de una reforma, ya publicada, se puede reformar otra vez; cada reforma crea su propia versión nueva, marcada como reforma.
- **Un fallo a medias:** si la versión nueva se crea y el proceso no alcanza a registrar la transición, un reintento no crea otra versión: la creación es idempotente, como en D-106.
- **EN_CONSOLIDACION desactivada** en la configuración del periodo (ADR-007): el distributivo va a la siguiente etapa activa del ciclo.
- **Una carrera que no cambia:** quien consolida no le devuelve el parcial; su contenido sigue el de la versión publicada, copiado en la nueva.
- **Una reforma que termina sin cambios:** el ciclo se completa igual; la versión nueva se publica aunque su contenido sea el mismo.
- **Un rechazo en firma durante la reforma (F12-H04):** anula la versión nueva de la reforma y crea otra, como hoy; la versión publicada anterior no se toca.
- **El periodo cerrado:** no se puede iniciar una reforma; es el error que ya existe para un periodo cerrado.
- **Varios Vicerrectores:** cualquiera con el rol puede iniciar la reforma; el que la inició no recibe su notificación, los demás sí si les corresponde.

## Requirements *(mandatory)*

### Functional Requirements

**Iniciar la reforma**

- **FR-001**: El sistema DEBE permitir iniciar una reforma del distributivo de un periodo solo cuando está en PUBLICADO, y solo al Vicerrector académico: quien tiene «Distributivo: aprobación» en la matriz D-16 y el rol que aprueba el contenido en la configuración del periodo.
- **FR-002**: Iniciar una reforma DEBE exigir un motivo de 10 a 500 caracteres, como la observación de una devolución.
- **FR-003**: Iniciar una reforma DEBE crear una versión nueva del distributivo, editable, que copia todo el contenido de la versión publicada y está marcada como reforma, con su motivo, quién y cuándo.
- **FR-004**: Iniciar una reforma DEBE devolver el distributivo a EN_CONSOLIDACION (o, si está desactivada, a la siguiente etapa activa), con la versión nueva como la versión en trabajo. Desde ahí, quien consolida DEBE poder devolver parciales, consolidar y enviar a revisión como en cualquier consolidación.
- **FR-005**: La versión nueva DEBE recorrer el ciclo completo: aprobación del contenido, su propio documento firmable con la cadena completa de firmas desde F1, su propia resolución del OCS y su publicación.
- **FR-006**: La versión publicada anterior DEBE seguir siendo la publicada vigente del periodo mientras dura la reforma, y quedar como histórica cuando se publica la versión de la reforma. En los dos casos conserva su contenido, sus firmas, su documento, su resolución y su publicación, sin anularse.
- **FR-007**: Iniciar una reforma en otra etapa DEBE rechazarse con un 409 de un código del catálogo que diga por qué, y no cambiar nada. Un usuario sin el permiso o sin el rol DEBE recibir el 403 común. Un motivo que no cumple FR-002, el 400 de su código del catálogo.
- **FR-008**: Dos reformas a la vez del mismo distributivo DEBEN dejar una sola versión nueva y una sola transición.
- **FR-009**: Cada reforma DEBE dejar una entrada en la bitácora con quién, cuándo, el periodo, la versión publicada, la versión nueva y el motivo.

**Mostrar**

- **FR-010**: El estado del flujo DEBE mostrar, a quien lo lee, la reforma en curso (quién, cuándo y el motivo) y la versión publicada anterior con su constancia, y la acción «Iniciar una reforma» solo a quien puede iniciarla.
- **FR-011**: La tarjeta del estado del flujo, en las pantallas que la muestran («Consolidado del periodo», «Oferta y propuesta de docentes» y «Resolución del OCS»), DEBE distinguir la versión de la reforma de la versión publicada anterior.

**Notificar**

- **FR-012**: La reforma DEBE notificar al rol de la etapa a la que vuelve el distributivo y a las cuentas activas con DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, con una sola notificación por usuario y nunca a quien la inició, con el motivo. La notificación DEBE generarse si la reforma se guardó, y solo en ese caso; que Notificaciones no responda NO DEBE impedir la reforma.

**Pantalla, seguridad y errores**

- **FR-013**: La pantalla del Vicerrector DEBE ofrecer «Iniciar una reforma» en PUBLICADO, con una confirmación que pida el motivo y diga qué pasa: se crea una versión nueva, el distributivo vuelve a consolidación, la versión publicada sigue vigente hasta que se publique la nueva, y a quiénes se notificará.
- **FR-014**: La pantalla y la confirmación DEBEN cumplir WCAG 2.1 AA.
- **FR-015**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII).

### Key Entities

- **Proceso de aprobación** (de Aprobación, existente): su etapa pasa de PUBLICADO a la etapa de regreso.
- **Transición de reforma**: quién, cuándo, el motivo, la versión publicada y la versión nueva.
- **Versión del distributivo** (de Distributivo, existente): la nueva es EDITABLE, con el motivo «reforma»; la publicada anterior sigue CONGELADA, como histórica.
- **Versión publicada anterior**: la que se publicó antes de la reforma, con sus firmas, su documento, su resolución y su publicación. Es la vigente hasta que se publica la de la reforma; después, histórica.
- **Notificación** (existente, F16-H01).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En un recorrido que lleva el distributivo hasta PUBLICADO, el Vicerrector inicia una reforma y el distributivo vuelve al ciclo con una versión nueva marcada como reforma; iniciarla otra vez, desde otra etapa o sin motivo se rechaza en el 100 % de los casos sin cambiar nada. Se comprueba en un script de punta a punta.
- **SC-002**: La versión nueva recorre el ciclo completo hasta publicarse, con tres firmas válidas sobre la versión nueva y su propia resolución; las tres firmas, el documento y la resolución de la versión anterior siguen intactos.
- **SC-003**: Después de la reforma, el Responsable de consolidación (las cuentas activas con el rol y el designado del periodo) y cada cuenta activa con DOCENTE, OPERADOR_DATOS o COORDINADOR_CARRERA tienen exactamente una notificación de la reforma, y quien la inició no la tiene.
- **SC-004**: Con el proceso de identidad detenido al iniciar la reforma, la reforma se completa sin error, y la notificación llega una sola vez antes de 2 minutos desde que el proceso vuelve.
- **SC-005**: El Vicerrector inicia una reforma en menos de 1 minuto, motivo y confirmación incluidos.
- **SC-006**: La pantalla y la confirmación no tienen fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **Quién la inicia:** el Vicerrector académico, como dice el issue: el permiso «Distributivo: aprobación» de la matriz D-16 y el rol que aprueba el contenido en la configuración del periodo, que por defecto es el Vicerrector. Hacen falta los dos, como en las demás operaciones del flujo.
- **El motivo:** obligatorio, de 10 a 500 caracteres, como la observación de una devolución (D-98, RF-36).
- **Dónde:** en «Consolidado del periodo», la pantalla del Vicerrector que ya muestra la tarjeta del estado del flujo.
- **La versión publicada anterior:** no se anula; las firmas y la resolución siguen valiendo para esa versión. Firma no anula nada.
- **La versión nueva:** se crea como en D-106, copiando todo, con el motivo «reforma». La consolidación copiada no queda desactualizada.
- **La configuración del flujo:** la reforma usa la copia de la configuración del periodo que ya tiene el proceso; no se vuelve a leer.
- **Fuera de alcance:**
  - el ajuste del Vicerrector en EN_REVISION_VRA (RF-35);
  - la consulta del distributivo publicado y las exportaciones (F19);
  - el CSV para SIGA;
  - una pantalla del historial de versiones; esta historia solo muestra la versión publicada anterior en el estado del flujo.
