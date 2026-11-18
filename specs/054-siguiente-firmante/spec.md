# Feature Specification: Notificar al siguiente firmante y ver la cadena de firmas

**Feature Branch**: `054-siguiente-firmante`

**Created**: 2026-11-17

**Status**: Draft

**Input**: User description: "F16-H02"

**Origen**: issue #68 · historia (`tipo:historia`, `prioridad:must`, `RF-41`) · feature F16 (#15) · milestone «Ola 3 · Aprobado y firmado» · servicio 07-notificaciones · tamaño propuesto S. Depende de F16-H01 (#67) y de F14-H02 (#63), las dos terminadas. Es la última historia de la feature F16.

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero que el siguiente firmante reciba una notificación cuando se acepta una firma, y ver quién está en turno, para que la cadena de firmas no se detenga.

**Requerimientos que cubre**:
- **RF-41:** notificaciones internas, dentro del MDD, a los involucrados en cada cambio de estado.
- **D-17:** la cadena de firmas, en orden.
- **Diseño de firmas** (`Diseno_firmas_cadena_aprobacion.docx`, «Firma de cada paso»): «El firmante en turno recibe la notificación y entra a "Pendientes de firma"». Al aceptar una firma, el MDD «registra la firma, guarda el archivo nuevo y notifica al siguiente firmante».

**Decisiones que aplica**:
- **D-108:** las notificaciones van por el evento de solicitud hacia Notificaciones, que guarda una por destinatario. La decisión ya preveía que F16-H02 publicara el mismo contenido desde Firma.
- **D-101 a D-104:** el documento firmable, el firmante esperado de cada paso fijado al generar, la validación de la firma y el aviso de la cadena completa.
- **D-105 y D-106:** un documento rechazado en firma queda cerrado, y la versión nueva anula sus firmas.
- **MD-05:** Firma no conoce el flujo.
- **MD-09:** «notificación», no «aviso».

**Principios de la constitución**: III, IV, V, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- **La cadena:** al entrar en firma, Firma genera el documento con su cadena de tres pasos:
  - F1, «Elaborado por», el Responsable de consolidación designado;
  - F2, «Revisado por», el Vicerrector;
  - F3, «Aprobado por», el Rector.
- **El firmante esperado** de cada paso queda fijado al generar. Es una persona, no un rol:
  - el designado del periodo se reconoce por su usuario;
  - el Vicerrector y el Rector, docentes de un cargo, se reconocen por la identificación de su cuenta.
- **Firmar:** el firmante en turno ve el documento en «Pendientes de firma», lo descarga, lo firma y lo sube. Si la firma se acepta, el turno pasa al paso siguiente.
- **Al entrar en firma,** F16-H01 ya notifica «El distributivo llegó a «En firma»» a quienes tienen el rol de Responsable de consolidación y al designado, con el enlace a «Pendientes de firma».
- **Después de F3,** el sistema pasa el distributivo a EN_OCS, y F16-H01 notifica a la Secretaría general académica.
- **Lo que falta:** nadie avisa a F2 ni a F3 cuando les llega el turno. Quien sigue el proceso tampoco ve, mientras la cadena está abierta, qué pasos están firmados y quién está en turno.

Esta historia agrega:
- **La notificación de firma pendiente:** cuando se acepta una firma, el firmante esperado del paso siguiente la recibe en su bandeja.
- **La cadena de firmas en «Consolidado del periodo»:** cada paso con su rótulo, el nombre del firmante, si está firmado o pendiente, y quién está en turno.

## Clarifications

### Session 2026-11-17

- Q: El firmante de F1, el designado, ya recibe por F16-H01 «El distributivo llegó a «En firma»», con el enlace a «Pendientes de firma». ¿F16-H02 le manda otra notificación? → A: No. F1 se queda con la de F16-H01, y F16-H02 notifica a F2 y a F3, cada uno cuando se acepta la firma anterior. Así el designado no recibe dos notificaciones por el mismo hecho.
- Q: ¿Dónde se ven los pasos que faltan y quién está en turno? → A: En «Consolidado del periodo», junto al documento firmable. Se ve cada paso con su rótulo, el nombre del firmante impreso en el PDF y su estado: firmado (con la fecha), en turno o pendiente. Lo ven quienes ya leen el documento firmable, como el Responsable de consolidación y el Vicerrector.

### Session 2026-11-18

- Q: Si el firmante esperado de F2 o F3 no tiene ninguna cuenta activa con su identificación, ¿quién recibe la notificación de firma pendiente? → A: Nadie. La firma se acepta igual, no es un error y no hay respaldo por rol: el firmante esperado es una persona fija (D-102). La cadena en «Consolidado del periodo» muestra quién está en turno.
- Q: En la cadena, ¿qué fecha y hora se muestran en un paso firmado: cuando el MDD aceptó la firma subida, o la que trae la firma dentro del PDF? → A: Cuando el MDD aceptó la firma subida. No se lee la fecha de la firma dentro del PDF.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El siguiente firmante recibe su notificación (Priority: P1)

Como Vicerrector académico, quiero que el siguiente firmante reciba una notificación cuando se acepta una firma, para que la cadena de firmas no se detenga.

**Why this priority**: es el primer escenario del issue y la razón de la historia. Sin ella, el firmante de F2 o de F3 solo se entera si entra por su cuenta a «Pendientes de firma».

**Independent Test**:
1. Con el documento en firma, el designado sube F1 y se acepta.
2. El Vicerrector, firmante de F2, ve en su campana una notificación sin leer de firma pendiente, con el periodo, la versión y el paso.
3. La abre y llega a «Pendientes de firma», donde está su documento.
4. Nadie más la recibe.

**Acceptance Scenarios**:

1. **Dado** F1 recién aceptada, **cuando** se registra la firma, **entonces** el firmante de F2 recibe una notificación de firma pendiente. *(issue)*
2. **Dado** F2 recién aceptada, **cuando** se registra la firma, **entonces** el firmante de F3, el Rector, recibe una notificación de firma pendiente.
3. **Dado** una notificación de firma pendiente, **cuando** el destinatario la lee, **entonces** dice:
   - qué paso le toca firmar, con su rótulo («Revisado por» o «Aprobado por»);
   - el periodo y la versión;
   - quién firmó el paso anterior y cuándo.
4. **Dado** una notificación de firma pendiente, **cuando** el destinatario la abre, **entonces** llega a «Pendientes de firma», donde está el documento en su turno.
5. **Dado** F3 recién aceptada, **cuando** se registra la firma, **entonces** nadie recibe una notificación de firma pendiente, porque no hay siguiente paso. La Secretaría general académica recibe la del paso a EN_OCS, que ya da F16-H01.
6. **Dado** una firma rechazada por la validación (por ejemplo, el PDF no es el mismo documento), **cuando** se responde, **entonces** nadie recibe una notificación de firma pendiente, porque el turno no cambió.
7. **Dado** un rechazo en firma, **cuando** se registra, **entonces** nadie recibe una notificación de firma pendiente: el documento queda cerrado y nadie está en turno.
8. **Dado** el firmante de F2, **cuando** recibe su notificación, **entonces** la recibe solo él, la persona del paso, y no los demás usuarios que tienen el rol de Vicerrector.

---

### User Story 2 - Ver la cadena de firmas y quién está en turno (Priority: P1)

Como Vicerrector académico, quiero ver qué pasos de la cadena están firmados y quién está en turno, para saber dónde está detenida la cadena sin preguntar.

**Why this priority**: es el segundo escenario del issue. Sin ella, quien sigue el proceso no sabe a quién reclamarle la firma que falta.

**Independent Test**:
1. Con F1 aceptada, el Vicerrector abre «Consolidado del periodo».
2. Ve los tres pasos:
   - F1, firmado por el designado, con la fecha;
   - F2, en turno, con su propio nombre;
   - F3, pendiente, con el nombre del Rector.

**Acceptance Scenarios**:

1. **Dado** un documento firmable en CONGELADO_EN_FIRMA, **cuando** abro la lista de firmas pendientes, **entonces** veo qué pasos faltan y quién está en turno. *(issue)*
2. **Dado** la cadena, **cuando** la veo, **entonces** cada paso muestra:
   - su orden y su rótulo, como en el PDF;
   - el nombre del firmante esperado, el mismo que imprime el PDF;
   - su estado: firmado (con la fecha y la hora en que el MDD aceptó la firma), en turno o pendiente.
3. **Dado** un documento recién generado, sin firmas, **cuando** veo la cadena, **entonces** F1 está en turno y F2 y F3 están pendientes.
4. **Dado** la cadena completa, **cuando** la veo, **entonces** los tres pasos están firmados y nadie está en turno.
5. **Dado** un documento rechazado en firma, **cuando** veo la cadena, **entonces**:
   - nadie está en turno;
   - el paso rechazado lo dice;
   - las firmas aceptadas antes del rechazo siguen como firmadas.
6. **Dado** una firma recién aceptada, **cuando** vuelvo a abrir «Consolidado del periodo», **entonces** la cadena ya muestra el paso firmado y el nuevo en turno.
7. **Dado** un usuario que no lee el documento firmable, como un coordinador de carrera, **cuando** intenta ver la cadena, **entonces** no puede: recibe el mismo 403 que hoy al consultar el documento.
8. **Dado** la cadena, **cuando** la recorro solo con el teclado o con un lector de pantalla, **entonces** se entiende el estado de cada paso sin depender del color (WCAG 2.1 AA).

---

### User Story 3 - Sin duplicados y sin pérdidas (Priority: P2)

Como firmante, quiero que la notificación de mi turno llegue una sola vez, aunque haya reintentos o el proceso de identidad haya estado caído, para confiar en la bandeja.

**Why this priority**: lo exige ADR-009 para las notificaciones (MD-03). El camino ya lo garantiza F16-H01; esta historia lo extiende a Firma y lo comprueba.

**Independent Test**:
1. Se detiene el proceso de identidad y se acepta F1.
2. El designado ve su firma aceptada, sin ningún error.
3. Se levanta el proceso de identidad.
4. El firmante de F2 tiene la notificación una sola vez.

**Acceptance Scenarios**:

1. **Dado** el proceso de identidad detenido, **cuando** se acepta una firma, **entonces**:
   - la firma se registra igual, y quien la subió no ve ningún error por la notificación;
   - cuando el proceso vuelve, el siguiente firmante recibe la notificación.
2. **Dado** una notificación de firma pendiente entregada dos veces por un reintento, **cuando** se recibe por segunda vez, **entonces** la bandeja muestra una sola.
3. **Dado** una firma aceptada, **cuando** la firma no llega a guardarse, **entonces** nadie recibe la notificación: las dos van juntas.

---

### Edge Cases

- **El firmante de un paso sin cuenta de usuario:** si ninguna cuenta activa tiene la identificación del firmante esperado, la firma se acepta igual y nadie recibe la notificación (FR-002 d). No es un error, y no se notifica al rol en su lugar. La cadena muestra quién está en turno, por su nombre.
- **Dos cuentas con la misma identificación:** no pueden existir. Usuarios no admite una identificación repetida (D-102), así que cada firmante esperado tiene a lo sumo una cuenta.
- **El firmante esperado con la cuenta desactivada:** no recibe la notificación.
- **El Vicerrector o el Rector cambian de cargo durante la firma:** el firmante esperado es el que quedó fijado al generar el documento (D-102). La notificación va a esa persona, y la cadena muestra su nombre.
- **El designado es también el Responsable con el rol asignado:** F1 sigue recibiendo solo la notificación de F16-H01.
- **Una firma aceptada que, al volver a subirla, se rechaza por repetida:** no cambia el turno y no notifica.
- **La versión nueva después de un rechazo en firma (F12-H04):** al volver a firma, F1 recibe la notificación de F16-H01. La cadena muestra solo el documento de la versión vigente, con sus firmas desde cero; las firmas anuladas de la versión anterior no aparecen como firmadas.
- **Un documento todavía sin generar, en generación o fallido:** no hay cadena que mostrar. La pantalla sigue diciendo lo que dice hoy.
- **Un documento generado antes de que existieran los firmantes esperados:** no tiene turno (F14-H01). La cadena muestra los pasos sin firmante ni turno.
- **Datos sensibles:** la notificación y la cadena muestran los nombres que ya imprime el PDF. No muestran la identificación de nadie, ni el certificado, ni datos de la firma más allá de la fecha.

## Requirements *(mandatory)*

### Functional Requirements

**La notificación de firma pendiente**

- **FR-001**: Cuando se acepta la firma de un paso y la cadena tiene un paso siguiente, el sistema DEBE notificar al firmante esperado de ese paso siguiente.
- **FR-002**: El destinatario DEBE ser la persona del paso, no el rol:
  - **(a)** si el firmante esperado se reconoce por su usuario, ese usuario;
  - **(b)** si se reconoce por su identificación, la cuenta activa con esa identificación, que es única;
  - **(c)** nunca otro usuario por tener el mismo rol;
  - **(d)** si ninguna cuenta activa corresponde al firmante esperado, nadie: la firma se acepta igual, sin error y sin respaldo por rol.
- **FR-003**: La notificación DEBE decir:
  - el rótulo del paso que le toca firmar;
  - el periodo y la versión;
  - quién firmó el paso anterior y cuándo, con el momento en que el MDD aceptó esa firma.
- **FR-004**: La notificación DEBE llevar a «Pendientes de firma».
- **FR-005**: NO DEBEN generar una notificación de firma pendiente:
  - la última firma de la cadena;
  - una firma rechazada por la validación;
  - un rechazo en firma.
- **FR-006**: El firmante de F1 NO DEBE recibir una notificación de firma pendiente de esta historia: le basta la del paso a «En firma» de F16-H01.

**Que no se pierda ni se repita**

- **FR-007**: La notificación DEBE generarse si la firma se registró, y solo en ese caso: ambas van juntas.
- **FR-008**: Una firma aceptada con el proceso de identidad detenido DEBE notificar al siguiente firmante cuando ese proceso vuelva, sin intervención manual.
- **FR-009**: Una misma notificación recibida varias veces DEBE quedar una sola vez en la bandeja, sin volver a marcarse como no leída.
- **FR-010**: Que Notificaciones no responda NO DEBE impedir ni demorar la firma, ni mostrarle un error a quien la sube.

**La cadena de firmas**

- **FR-011**: «Consolidado del periodo» DEBE mostrar, con el documento firmable generado, la cadena de firmas de la versión vigente. Para cada paso DEBE mostrar:
  - su orden y su rótulo, como en el PDF;
  - el nombre del firmante esperado, como lo imprime el PDF;
  - su estado: firmado (con la fecha y la hora), en turno, pendiente o rechazado.
  
  La fecha y la hora de un paso firmado DEBEN ser las del momento en que el MDD aceptó la firma subida, no las de la firma dentro del PDF.
- **FR-012**: A lo sumo un paso DEBE estar en turno, el primero sin firma. Con la cadena completa, un documento rechazado o un documento sin firmantes esperados, ninguno.
- **FR-013**: La cadena DEBEN poder verla quienes hoy leen el documento firmable, y nadie más. La regla de permisos no cambia.
- **FR-014**: La cadena DEBE reflejar la última firma aceptada en cuanto se vuelve a abrir la pantalla.
- **FR-015**: La cadena DEBE cumplir WCAG 2.1 AA: el estado de cada paso se dice con texto, no solo con color, y se recorre con el teclado.

**Seguridad y errores**

- **FR-016**: La notificación y la cadena NO DEBEN mostrar la identificación de una persona, su certificado ni datos de la firma más allá de la fecha.
- **FR-017**: Solo los procesos del sistema DEBEN poder crear notificaciones; ningún usuario puede crearlas.
- **FR-018**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII).

### Key Entities

- **Notificación** (`Notificacion`, glosario): ya existe (F16-H01). Esta historia agrega un destinatario que es una persona, no un rol.
- **Documento firmable y su cadena**: ya existen (F13-H02, F14-H01). Cada paso tiene su orden, su rótulo, el firmante esperado con su nombre y, si la tiene, su firma aceptada.
- **Paso de la cadena, visto por otros**: el orden, el rótulo, el nombre del firmante y su estado (firmado, en turno, pendiente o rechazado), con la fecha de la firma si está firmado.
- **Firmante esperado**: la persona de cada paso, fijada al generar (D-102). Se reconoce por su usuario o por la identificación de su cuenta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En un recorrido que firma F1, F2 y F3, el firmante de F2 y el de F3 reciben cada uno exactamente una notificación de firma pendiente, y ningún otro usuario la recibe. Se comprueba en un script de punta a punta con los procesos levantados.
- **SC-002**: Con el proceso de identidad detenido mientras se acepta una firma, el siguiente firmante tiene la notificación una sola vez antes de 2 minutos desde que el proceso vuelve.
- **SC-003**: Después de cada firma aceptada del recorrido, la cadena en «Consolidado del periodo» muestra el estado correcto de los tres pasos en el 100 % de los casos: firmados, en turno y pendientes.
- **SC-004**: El firmante abre la notificación desde la campana y llega a su documento en «Pendientes de firma» en menos de 10 segundos.
- **SC-005**: El Vicerrector identifica en «Consolidado del periodo» quién está en turno en menos de 10 segundos, sin abrir otra pantalla.
- **SC-006**: La cadena no tiene fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **El firmante esperado** es el que quedó fijado al generar el documento (D-102). La notificación y la cadena usan ese, aunque después cambie quién ocupa el cargo.
- **La cuenta de una identificación** se resuelve cuando Notificaciones recibe el hecho, con la cuenta que tiene en ese momento el proceso de identidad, si está activa.
- **Quién lee la cadena:** los mismos que hoy leen el estado del documento firmable. No hay permiso nuevo.
- **«Pendientes de firma» no cambia:** sigue mostrando solo los documentos en turno del usuario.
- **Sin recordatorios:** no se repite la notificación si el firmante tarda, ni hay plazos para firmar (F12-H03 trata los plazos).
- **Fuera de alcance:**
  - publicar y notificar a docentes, Talento Humano y coordinaciones (F18-H01);
  - la cadena en otras pantallas, como «Resolución del OCS»;
  - notificar a quien sigue el proceso por cada firma aceptada: el issue solo pide notificar al siguiente firmante.
