# Feature Specification: Recibir notificaciones internas por cada cambio de estado

**Feature Branch**: `053-notificaciones-internas`

**Created**: 2026-11-15

**Status**: Draft

**Input**: User description: "F16-H01"

**Origen**: issue #67 · historia (`tipo:historia`, `prioridad:must`, `RF-41`) · feature F16 (#15) · milestone «Ola 3 · Aprobado y firmado» · servicio 07-notificaciones · tamaño M. Depende de B00-H05 (#22), terminada. F16-H02 (#68, el aviso al siguiente firmante) y F18-H01 (#71, publicar) dependen de esta historia.

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero recibir en mi bandeja una notificación por cada cambio de estado que me toca, para dar seguimiento sin reuniones ni correos.

**Requerimientos que cubre**:
- **RF-41:** notificaciones internas, dentro del MDD, a los roles involucrados en cada cambio de estado. Su criterio en la ERS: «Cada transición genera la notificación a los destinatarios configurados. El envío por correo queda para una fase posterior, detrás de un adaptador (V-09).»
- **V-09 (opción B):** solo notificaciones dentro del MDD, sin correo.
- **ADR-009 y MD-03:** lo que no puede perderse, como las notificaciones, se publica como evento en la transacción del servicio y llega aunque el receptor esté caído.

**Decisiones que aplica**:
- ADR-018 y D-49: eventos entre procesos, sobre común, token de proceso y recepción idempotente.
- D-97, D-98 y D-100: enviar a revisión, devolver y aprobar el contenido dejaron sus notificaciones para esta historia.
- D-95: las transiciones las registra Aprobación.
- MD-09: «notificación», no «aviso».
- Glosario: `Notificacion`, «mensaje en la bandeja interna de un usuario por un hecho del flujo»; el contexto Notificaciones es genérico y solo depende de Identidad.

**Principios de la constitución**: III, IV, V, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- Aprobación registra cada transición del distributivo: el envío de un parcial, el paso de etapa, la devolución, el rechazo en firma y la versión nueva, con su entrada en la bitácora.
- La configuración del flujo de cada periodo da a cada etapa su rol responsable. Por defecto:
  - Propuesta de carrera: COORDINADOR_CARRERA.
  - Consolidación y En firma: RESPONSABLE_CONSOLIDACION.
  - Revisión del Vicerrectorado: VICERRECTOR.
  - Resolución del OCS, Aprobado y Publicado: SECRETARIA_GENERAL.
- El camino de los eventos entre procesos existe y está probado (B00-H05, F17-H01). Notificaciones vive en el proceso de identidad, como la bitácora.
- El servicio de Notificaciones está vacío: solo tiene su esquema.

Esta historia agrega:
- **La notificación de cada transición:** llega a quien tiene el rol de la etapa a la que llega el distributivo.
- **La bandeja:** una campana en la barra superior con las notificaciones sin leer.
- **Marcar como leída:** abrir una notificación la marca.
- **Que no se pierda ni se repita:** una notificación llega aunque el proceso de identidad esté caído, y llega una sola vez.

## Clarifications

### Session 2026-11-15

- Q: ¿A quién le llega la notificación de cada transición? → A: A los usuarios con el rol responsable de la etapa a la que llega el distributivo, según la configuración del flujo del periodo. Hay tres excepciones:
  - En una devolución a la carrera, solo a los coordinadores de las carreras devueltas.
  - Cuando el rol es RESPONSABLE_CONSOLIDACION, a quienes lo tienen asignado y, además, al designado en el periodo. *(Cambiado el 2026-11-15, durante T021: en la demo, `consolidacion.demo` tiene el rol fijo y es quien consolida, y `docente.demo` es el designado para firmar; con «solo el designado», `consolidacion.demo` no recibía los parciales. Lo decidió el autor.)*
  - En el envío de un parcial, donde el distributivo no cambia de etapa, al rol de la siguiente etapa activa, que es quien lo recibe.
- Q: ¿Qué transiciones notifican? → A: Todas las que registra Aprobación: el envío de un parcial, el paso de etapa, la devolución, el rechazo en firma y la versión nueva.
  - El paso de etapa incluye enviar a revisión, aprobar el contenido, llegar a EN_OCS por el sistema y llegar a APROBADO con la resolución.
  - El aviso al siguiente firmante es de F16-H02.
  - PUBLICADO usará este mismo camino en F18-H01.
- Q: ¿Dónde ve el usuario su bandeja? → A: En una campana, en la barra superior de todas las pantallas, con el número de notificaciones sin leer. La campana abre la lista. Abrir una notificación la marca como leída y lleva a la pantalla donde se ve el hecho.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cada transición llega a la bandeja de sus destinatarios (Priority: P1)

Como Vicerrector académico, quiero que cada cambio de estado que me toca llegue a mi bandeja, para dar seguimiento sin reuniones ni correos.

**Why this priority**: es el primer escenario del issue y la razón de la historia. D-97, D-98 y D-100 dejaron sus notificaciones para aquí, y F16-H02 y F18-H01 la usan.

**Independent Test**:
1. Con el distributivo en EN_CONSOLIDACION, el Responsable de consolidación lo envía a revisión.
2. `vicerrectorado.demo` ve una notificación sin leer en su campana, con el periodo, la etapa y quién lo envió.
3. Ningún otro rol la recibe.

**Acceptance Scenarios**:

1. **Dado** una transición del distributivo, **cuando** se completa, **entonces** cada destinatario configurado recibe una notificación en su bandeja. *(issue)*
2. **Dado** el envío a revisión, **cuando** se completa, **entonces** reciben la notificación los usuarios con el rol de EN_REVISION_VRA, por defecto el Vicerrector, con el periodo, la etapa de llegada, la versión, quién la hizo y cuándo.
3. **Dado** una devolución de dos carreras desde EN_CONSOLIDACION, **cuando** se completa, **entonces**:
   - el coordinador de cada carrera devuelta recibe una notificación con la observación de su carrera;
   - los coordinadores de las demás carreras no la reciben.
4. **Dado** el envío del parcial de una carrera, **cuando** se completa, **entonces** el Responsable de consolidación (con el rol asignado o designado en el periodo) recibe una notificación con la carrera.
   - Si era el último parcial, recibe además la del paso a EN_CONSOLIDACION.
5. **Dado** un rechazo en firma, **cuando** el distributivo vuelve a EN_REVISION_VRA, **entonces** el Vicerrector recibe una notificación con el paso que rechazó, el rol de quien lo rechazó y su observación.
   - La versión nueva que sigue al rechazo le llega en otra notificación, con su número.
6. **Dado** la cadena de firmas completa, **cuando** el sistema pasa el distributivo a EN_OCS, **entonces** los usuarios con el rol de EN_OCS, por defecto la Secretaría general académica, reciben la notificación, con «el sistema» como autor.
7. **Dado** un periodo cuya configuración del flujo da otro rol a una etapa, **cuando** el distributivo llega a ella, **entonces** la notificación la reciben los usuarios de ese rol, no los del rol por defecto.
8. **Dado** que el usuario que hizo la transición tiene también el rol de destino, **cuando** se completa, **entonces** él no recibe su propia notificación; los demás usuarios de ese rol sí.
9. **Dado** un intento de transición rechazado (un 409, un 400 o un 403), **cuando** ocurre, **entonces** nadie recibe una notificación, porque nada cambió.

---

### User Story 2 - El usuario ve su bandeja y marca una notificación como leída (Priority: P1)

Como destinatario, quiero ver en la campana cuántas notificaciones tengo sin leer y abrirlas, para saber qué me toca hacer.

**Why this priority**: es el segundo escenario del issue. Sin la bandeja, la notificación no le sirve a nadie.

**Independent Test**:
1. `vicerrectorado.demo`, con una notificación sin leer, ve un 1 en la campana.
2. Abre la lista y la notificación.
3. Llega a «Consolidado del periodo».
4. La campana ya no muestra el número, y la notificación sigue en la lista, como leída.

**Acceptance Scenarios**:

1. **Dado** una notificación sin leer, **cuando** la abro, **entonces** queda marcada como leída. *(issue)*
2. **Dado** varias notificaciones, **cuando** abro la campana, **entonces**:
   - veo mis notificaciones, de la más nueva a la más antigua;
   - las no leídas se distinguen de las leídas;
   - cada una dice qué pasó, en qué periodo, quién y cuándo.
3. **Dado** notificaciones sin leer, **cuando** navego por cualquier pantalla, **entonces** la campana muestra cuántas son.
   - Cuando llega una nueva, el número se actualiza solo, sin recargar la página.
4. **Dado** una notificación, **cuando** la abro, **entonces** me lleva a la pantalla donde se ve el hecho. Por ejemplo, «Consolidado del periodo» o «Resolución del OCS».
5. **Dado** una notificación ya leída, **cuando** la abro otra vez, **entonces** sigue leída y nada más cambia.
6. **Dado** otro usuario, **cuando** intenta ver o marcar mis notificaciones, **entonces** no puede: cada usuario ve y marca solo las suyas.
7. **Dado** que no tengo notificaciones, **cuando** abro la campana, **entonces** veo un mensaje que dice que no hay notificaciones.
8. **Dado** la campana, **cuando** la uso solo con el teclado o con un lector de pantalla, **entonces** puedo abrir la lista y cada notificación, y se anuncia el número sin leer (WCAG 2.1 AA).

---

### User Story 3 - Sin duplicados y sin pérdidas (Priority: P1)

Como destinatario, quiero que cada notificación llegue una sola vez, aunque haya reintentos o el proceso de identidad haya estado caído, para confiar en la bandeja.

**Why this priority**: es el tercer escenario del issue. Las notificaciones están entre lo que no puede perderse (ADR-009, MD-03).

**Independent Test**:
1. Se detiene el proceso de identidad y se hace una transición.
2. Se vuelve a levantar el proceso.
3. El destinatario tiene la notificación una sola vez, aunque la entrega se haya reintentado.

**Acceptance Scenarios**:

1. **Dado** una notificación que llega dos veces por un reintento, **cuando** el servicio la recibe por segunda vez, **entonces** la bandeja muestra una sola. *(issue)*
2. **Dado** el proceso de identidad detenido, **cuando** se completa una transición, **entonces**:
   - la transición se guarda igual, y quien la hizo no ve ningún error por las notificaciones;
   - cuando el proceso vuelve, la notificación llega a sus destinatarios.
3. **Dado** una notificación repetida que llega después de que el destinatario la leyó, **cuando** se recibe, **entonces** sigue siendo una sola, y sigue leída.

---

### Edge Cases

- **Un rol de destino sin usuarios:** la transición se completa y no hay notificación. No es un error.
- **El Responsable de consolidación no designado:** la reciben solo quienes tienen el rol asignado.
- **Un coordinador sin carrera indicada:** no recibe la notificación de una devolución.
- **Un usuario con el rol de destino que está desactivado:** no la recibe.
- **Un usuario que recibe el rol después de la transición:** no ve las notificaciones anteriores. Los destinatarios se fijan cuando la notificación se recibe.
- **Un usuario que pierde el rol después:** conserva las notificaciones que ya recibió. Solo él las ve.
- **Una sola operación que produce dos transiciones** (el último parcial y el paso a EN_CONSOLIDACION):
  - cada una da su notificación;
  - el Responsable de consolidación recibe dos.
- **Dos coordinadores de la misma carrera devuelta:** cada uno recibe la suya, con la observación de esa carrera. Un coordinador tiene una sola carrera (D-64).
- **Un tipo de transición que Notificaciones no conoce:** se rechaza con el error del catálogo y queda pendiente para su reenvío. No se pierde.
- **Muchas notificaciones:** la lista muestra las más recientes primero. El número sin leer cuenta todas.
- **Datos sensibles:** una notificación no lleva contraseñas, tokens ni datos personales de un docente; solo el hecho del flujo. Las observaciones son las mismas que ya ve el destinatario en el estado del flujo.

## Requirements *(mandatory)*

### Functional Requirements

**Qué se notifica y a quién**

- **FR-001**: Cada transición que registra Aprobación DEBE generar notificaciones para sus destinatarios:
  - el envío de un parcial;
  - el paso de etapa, que incluye enviar a revisión, aprobar el contenido, llegar a EN_OCS y llegar a APROBADO;
  - la devolución;
  - el rechazo en firma;
  - la versión nueva.
- **FR-002**: Los destinatarios DEBEN ser los usuarios activos con el rol responsable de la etapa a la que llega el distributivo, según la configuración del flujo vigente en el periodo, con estas reglas:
  - **(a)** En una devolución a la carrera, solo los coordinadores de las carreras devueltas.
  - **(b)** Si el rol es RESPONSABLE_CONSOLIDACION, quienes lo tienen asignado y, además, el designado en el periodo.
  - **(c)** En el envío de un parcial, el rol de la siguiente etapa activa.
  - **(d)** Nunca el usuario que hizo la transición.
- **FR-003**: Cada notificación DEBE decir qué pasó, con el nombre de la etapa que da la configuración del flujo. También DEBE decir:
  - el periodo;
  - la versión;
  - quién hizo la transición («el sistema» si la hizo el sistema);
  - cuándo.
  
  Según el tipo, DEBE incluir además:
  - en una devolución, la observación de la carrera del destinatario;
  - en un rechazo en firma, el paso, el rol de quien rechazó y su observación;
  - en el envío de un parcial, la carrera.
- **FR-004**: Un intento rechazado NO DEBE generar notificaciones.
- **FR-005**: La notificación DEBE generarse si la transición se guardó, y solo en ese caso: ambas van juntas.

**La bandeja**

- **FR-006**: Cada usuario con sesión DEBE poder ver sus notificaciones, de la más nueva a la más antigua, distinguiendo las leídas de las no leídas. Solo DEBE ver las suyas.
- **FR-007**: La barra superior de la SPA DEBE mostrar en todas las pantallas una campana con el número de notificaciones sin leer.
  - El número DEBE actualizarse solo, sin recargar la página, en menos de un minuto desde que la notificación llega a la bandeja.
- **FR-008**: Abrir una notificación DEBE marcarla como leída y llevar al usuario a la pantalla donde se ve el hecho.
  - Marcar una ya leída no cambia nada.
  - Un usuario NO DEBE poder marcar la notificación de otro.
- **FR-009**: Las notificaciones NO DEBEN poder borrarse ni editarse desde el sistema. Solo cambia si están leídas.
- **FR-010**: La campana y la lista DEBEN cumplir WCAG 2.1 AA: se usan con el teclado y anuncian el número sin leer.

**Que no se pierda ni se repita**

- **FR-011**: Una notificación de una transición hecha con el proceso de identidad detenido DEBE llegar cuando vuelva, sin intervención manual.
- **FR-012**: Una misma notificación recibida varias veces DEBE quedar una sola vez en cada bandeja, sin volver a marcarse como no leída.
- **FR-013**: Que Notificaciones no responda NO DEBE impedir ni demorar la transición del usuario, ni mostrarle un error.

**Seguridad y errores**

- **FR-014**: Una notificación NO DEBE contener contraseñas, tokens, secretos ni datos personales de un docente.
- **FR-015**: Solo los procesos del sistema DEBEN poder crear notificaciones; ningún usuario puede crearlas.
- **FR-016**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII).

### Key Entities

- **Notificación** (`Notificacion`, glosario): un mensaje en la bandeja interna de un usuario por un hecho del flujo.
  - Tiene el destinatario y el hecho: qué pasó, el periodo, la versión, quién y cuándo.
  - Según el tipo, lleva también la carrera o la observación.
  - Dice si está leída y desde cuándo.
  - Su contenido no cambia; solo cambia si está leída.
- **Bandeja**: las notificaciones de un usuario. Solo él las ve.
- **Transición** (de Aprobación): el hecho que origina las notificaciones. Ya existe.
- **Etapa y su rol responsable** (de la configuración del flujo): deciden los destinatarios. Ya existen.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En un recorrido que lleva el distributivo desde la propuesta de carrera hasta APROBADO, cada transición deja su notificación en la bandeja de cada destinatario, sin faltar ninguna ni sobrar ninguna. Se comprueba en un script de punta a punta con los procesos levantados.
- **SC-002**: Con el proceso de identidad detenido durante una transición, el destinatario la tiene en su bandeja una sola vez antes de 2 minutos desde que el proceso vuelve.
- **SC-003**: Una entrega repetida deja el 100 % de las bandejas con una sola notificación por transición y destinatario.
- **SC-004**: Desde que la notificación llega a la bandeja, el destinatario la ve en la campana en menos de 1 minuto, sin recargar la página.
- **SC-005**: El autor abre una notificación desde la campana y llega a la pantalla del hecho en menos de 10 segundos. Al volver, la campana ya no la cuenta.
- **SC-006**: La campana y la lista no tienen fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **Destinatarios:** se fijan cuando Notificaciones recibe el hecho, con los roles, los usuarios activos y la carrera de cada coordinador que tiene en ese momento el proceso de identidad, y con la configuración del flujo del periodo.
- **Sin otros canales:** no hay correo (V-09), ni notificaciones del navegador, ni preferencias por usuario para silenciar notificaciones.
- **Sin acciones en bloque:** no hay «marcar todas como leídas» ni borrado; la historia solo pide marcar al abrir.
- **La bitácora:** marcar como leída no deja entrada; no es un cambio del distributivo ni de los datos maestros (D-90).
- **Conservación:** las notificaciones se conservan sin purga, como la bitácora; si hace falta, una historia futura la agrega.
- **Fuera de alcance:**
  - el aviso al siguiente firmante cuando se acepta una firma (F16-H02);
  - publicar y notificar a docentes, Talento Humano y coordinaciones (F18-H01);
  - el aviso de un plazo vencido (F12-H03);
  - las notificaciones de cambios que no son transiciones del distributivo, como una carga confirmada o un cambio de roles.
- **El permiso:** ver la bandeja propia lo puede todo usuario con sesión, sea cual sea su rol. El plan decide si se expresa como un permiso de la matriz D-16 que tienen los diez roles.
