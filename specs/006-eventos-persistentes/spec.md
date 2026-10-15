# Feature Specification: Eventos persistentes entre procesos

**Feature Branch**: `006-eventos-persistentes`

**Created**: 2026-10-14

**Status**: Draft

**Input**: User description: "Especifica la historia B00-H05. Léela de su issue con gh issue list --search "B00-H05 in:title" y gh issue view <número>. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md y docs/decisiones.md"

**Origen**: issue #22 · B00-H05 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:transversal`, `RF-41`, `RF-44` · tamaño propuesto M · depende de #19 (B00-H02, cerrada) · bloquea a #69 (F17-H01, bitácora) y #67 (F16-H01, notificaciones internas).

**Requerimientos que cubre**: ADR-009 · MD-03 · RF-41 · RF-44. **Reglas que aplica**: principio V de la constitución («Fallos entre procesos»: evento de dominio en la transacción del servicio, registro persistente de publicaciones, oyente del mismo proceso, tarea programada de reenvío y receptor idempotente por el identificador del evento, sin colas ni broker), principio X (simplicidad), principio XIII (errores) y ADR-014 (tokens RS256 emitidos por Usuarios).

**Historia (tal como está en el issue)**: Como autor, quiero que los eventos hacia la bitácora y las notificaciones se guarden y se reintenten, para que no se pierda ninguna entrada aunque un proceso se caiga.

**Decisiones tomadas en esta historia:** D-47, D-48 y D-49.

## Clarifications

### Session 2026-10-14

- Q: ¿El receptor es un receptor de prueba en Usuarios o se adelanta un receptor mínimo real de la bitácora o de las notificaciones? → A: Un receptor de prueba en Usuarios, el servicio dueño de la bitácora, declarado en su contrato. El mecanismo común (publicación, entrega y reenvío, y la forma común de la recepción idempotente, FR-008) queda listo para que F17-H01 y F16-H01 lo usen; esta historia no adelanta reglas de la bitácora ni de las notificaciones (D-47).
- Q: ¿Cómo se publica el evento de prueba? → A: Con un endpoint de prueba en un servicio del proceso distributivo, apagado por defecto y encendido solo en el entorno de prueba, más pruebas automáticas y un script de punta a punta que detiene y vuelve a arrancar el proceso de identidad, como `deploy/probar_autenticacion.sh` y `deploy/probar_errores.sh` (D-48).
- Q: ¿Cómo se autentica el proceso que entrega ante el receptor? → A: Con un token de proceso que emite Usuarios para cada proceso, con la credencial del proceso en una variable de entorno, un rol que solo sirve para el receptor y una vida corta. La ruta del receptor no pasa por el API Gateway (D-49, con ADR-014).

> **Nota sobre el lenguaje de esta especificación.** Es una historia de la base técnica: su usuario es el autor y su objeto es el mecanismo de entrega que ya fija el principio V de la constitución. Los términos «publicación», «registro persistente de publicaciones», «oyente» y «tarea programada» vienen de ese principio; la especificación no elige ninguna tecnología nueva.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus tres escenarios son tres partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Un evento publicado en una transacción llega una vez al otro proceso (Priority: P1)

Como autor, quiero que un evento publicado dentro de la transacción de un servicio del proceso distributivo llegue al receptor del proceso de identidad, y que su publicación quede completada, para tener el camino que usarán la bitácora y las notificaciones (principio V, RF-41, RF-44).

**Why this priority**: Es el camino normal; las otras dos partes lo suponen.

**Independent Test**: Con los dos procesos en marcha, publicar un evento de prueba en una transacción de un servicio del proceso distributivo y comprobar que el receptor lo registra una sola vez y que la publicación queda completada.

**Acceptance Scenarios**:

```gherkin
Escenario: Entrega normal
  Dado un evento de prueba publicado en la transacción de un servicio del proceso distributivo
  Cuando el proceso de identidad está disponible
  Entonces el receptor lo recibe una vez
  Y la publicación queda completada
```

---

### User Story 2 - Con el receptor caído, la publicación espera y se reenvía (Priority: P1)

Como autor, quiero que, si el proceso de identidad está detenido, la transacción del servicio se confirme igual y la publicación quede pendiente, y que una tarea programada la reenvíe cuando el proceso vuelva, para que no se pierda ninguna entrada de la bitácora ni ninguna notificación aunque un proceso se caiga (objetivo 2 de la constitución).

**Why this priority**: Es la razón de la historia: sin el reenvío, una caída del proceso de identidad dejaría huecos en la bitácora (F17-H01, escenario «Proceso de identidad caído»).

**Independent Test**: Detener el proceso de identidad, publicar un evento, comprobar que la transacción se confirma y que la publicación queda pendiente; volver a arrancar el proceso y comprobar que la tarea programada reenvía la publicación, que el receptor la registra y que la publicación queda completada.

**Acceptance Scenarios**:

```gherkin
Escenario: Receptor caído
  Dado el proceso de identidad detenido
  Cuando se publica un evento
  Entonces la transacción del servicio se confirma igual
  Y la publicación queda pendiente
  Cuando el proceso de identidad vuelve
  Entonces la tarea programada reenvía la publicación
  Y el receptor la registra
```

---

### User Story 3 - Un evento repetido no se registra dos veces (Priority: P2)

Como autor, quiero que el receptor no registre dos veces un evento que llega otra vez con el mismo identificador, para que los reenvíos no dupliquen entradas de la bitácora ni notificaciones (principio V, F16-H01 «Sin duplicados»).

**Why this priority**: Los reenvíos de la parte 2 pueden entregar un evento que el receptor ya registró (por ejemplo, si se perdió su respuesta); sin esta parte, el reenvío produciría duplicados.

**Independent Test**: Entregar al receptor el mismo evento dos o más veces con el mismo identificador y comprobar que queda registrado una sola vez y que cada entrega se confirma.

**Acceptance Scenarios**:

```gherkin
Escenario: Entrega repetida
  Dado un evento que el receptor ya registró
  Cuando llega otra vez con el mismo identificador
  Entonces el receptor no lo registra dos veces
```

---

### Edge Cases

- **La transacción del servicio se revierte**: no queda publicación y no se entrega nada; un evento solo existe si su transacción se confirmó.
- **El receptor responde con un error o no responde dentro del plazo de espera**: la publicación queda pendiente igual que con el proceso detenido; la transacción del servicio ya está confirmada y no cambia.
- **El receptor registró el evento, pero su respuesta se perdió**: la publicación queda pendiente, se reenvía y el receptor la confirma sin registrarla de nuevo (parte 3).
- **El proceso que publica se detiene después de confirmar y antes de entregar**: la publicación sigue guardada como pendiente y la tarea programada la reenvía cuando el proceso vuelve.
- **Dos entregas del mismo evento llegan a la vez** (el oyente y la tarea programada, o dos reenvíos): el receptor lo registra una sola vez.
- **Varias publicaciones pendientes cuando el receptor vuelve**: se reenvían todas; ninguna se pierde ni se registra dos veces.
- **El receptor está caído mucho tiempo**: la publicación sigue pendiente y se sigue reenviando en cada ciclo de la tarea programada; no se descarta.
- **Un evento con un contenido que el receptor no reconoce o que no cumple su contrato**: el receptor lo rechaza con el modelo de errores común (principio XIII) y la publicación queda pendiente; el registro lo deja ver para corregirlo.
- **Entrega sin token válido o con un token de usuario**: el receptor la rechaza (401 o 403) y no registra el evento; la publicación sigue pendiente.
- **El token de proceso vence entre dos reenvíos**: el proceso pide uno nuevo antes de entregar; un token vencido no deja la publicación perdida, solo pendiente.
- **El endpoint de prueba apagado**: responde como una ruta inexistente (404 con el modelo común), en cualquier entorno distinto del de prueba.
- **Datos sensibles**: el evento y los registros de fallos no llevan contraseñas ni tokens, y el prototipo solo usa datos ficticios (principio IX; F17-H01 «Sin datos sensibles»).

## Requirements *(mandatory)*

### Functional Requirements

**Publicación**

- **FR-001**: Un servicio DEBE poder publicar un evento dentro de su propia transacción. La publicación DEBE quedar guardada de forma persistente en esa misma transacción: si la transacción se confirma, la publicación existe; si se revierte, no (principio V).
- **FR-002**: Cada evento DEBE llevar un identificador único, su tipo, el servicio que lo publica, el momento y su contenido. NO DEBE llevar contraseñas ni tokens.

**Entrega**

- **FR-003**: Después de confirmar la transacción, un oyente del mismo proceso DEBE entregar el evento al receptor del otro proceso por HTTP, según el contrato del receptor y con un plazo de espera (principio V). La transacción del servicio NO DEBE esperar la entrega ni depender de su resultado.
- **FR-004**: Si el receptor confirma la entrega, la publicación DEBE quedar completada. Si el receptor no está disponible, no responde dentro del plazo o responde con un error, la publicación DEBE quedar pendiente.

**Reenvío**

- **FR-005**: Una tarea programada del proceso que publica DEBE reenviar las publicaciones pendientes, cada cierto intervalo configurable, hasta que el receptor las confirme. Esto incluye las publicaciones que quedaron pendientes porque el propio proceso se reinició.
- **FR-006**: Cada fallo de entrega y cada reenvío DEBEN quedar en el registro del proceso con el identificador del evento y su tipo, sin el contenido del evento.

**Recepción idempotente**

- **FR-007**: El receptor DEBE registrar cada evento una sola vez por su identificador. Si el evento llega otra vez con el mismo identificador, incluso en dos entregas simultáneas, DEBE confirmar la entrega sin registrarlo de nuevo (principio V).
- **FR-008**: La publicación, la entrega y el reenvío DEBEN ser un mecanismo común, que F17-H01 (bitácora) y F16-H01 (notificaciones) usen sin repetirlo. La recepción idempotente DEBE seguir una forma común y documentada, igual en cada receptor: el mismo sobre del contrato, una tabla de eventos recibidos en el esquema del servicio con el identificador como clave, el registro en la misma transacción que el efecto del evento y una ruta entre procesos protegida por el token de proceso. El registro de cada receptor queda en su servicio, porque va en su transacción y en su esquema (principio V).

**Demostración**

- **FR-009**: Los escenarios se demuestran con un evento de prueba que publica un servicio del proceso distributivo y un receptor de prueba en Usuarios, en el proceso de identidad. El receptor DEBE estar declarado en el contrato de Usuarios antes de implementarse (principio IV) y registrar los eventos que recibe en el esquema de Usuarios. NO DEBE adelantar reglas de la bitácora ni de las notificaciones (D-47).
- **FR-010**: El evento de prueba se publica con un endpoint de prueba de un servicio del proceso distributivo, declarado en su contrato. El endpoint DEBE estar apagado por defecto y encenderse solo en el entorno de prueba; apagado, no existe para quien lo llame. Un script de punta a punta DEBE recorrer los tres escenarios con los procesos levantados: publica con identidad en marcha, detiene identidad, publica, comprueba que la publicación queda pendiente, vuelve a arrancar identidad y comprueba el reenvío y el registro único (D-48).

**Seguridad**

- **FR-011**: Las entregas y los reenvíos no tienen el token de ningún usuario, porque ocurren después de la petición. El proceso que entrega DEBE autenticarse con un token de proceso que emite Usuarios, con la credencial del proceso guardada fuera del repositorio, un rol que solo permite entregar eventos al receptor y una vida corta. El receptor DEBE rechazar con el 401 uniforme de ADR-014 una entrega sin token válido, y con 403 la de un token sin ese rol; un token de proceso NO DEBE servir para ninguna otra ruta. Si Usuarios no está disponible para emitir el token, la entrega falla y la publicación queda pendiente (D-49).
- **FR-012**: La ruta del receptor NO DEBE estar expuesta por el API Gateway; solo se llama entre procesos.

**Límites**

- **FR-013**: NO DEBE haber colas ni broker (principio X). Ningún servicio DEBE leer ni escribir las tablas de otro (principio V): el receptor guarda los eventos que registró en su propio esquema.
- **FR-014**: Las pruebas automáticas DEBEN cubrir los tres escenarios, incluida la caída del receptor y su vuelta, con datos ficticios (principio VIII).

### Key Entities

- **Evento**: un hecho que un servicio publica en su transacción para otro proceso, con su identificador único, su tipo, el servicio de origen, el momento y su contenido.
- **Publicación**: el registro persistente de un evento en el proceso que lo publica, con su estado: pendiente o completada.
- **Entrega**: un intento de enviar una publicación al receptor, hecho por el oyente o por la tarea programada; termina en confirmación o en fallo.
- **Token de proceso**: credencial de vida corta que Usuarios emite para un proceso del MDD, con un rol que solo permite entregar eventos al receptor.
- **Evento recibido**: el registro del receptor que guarda el identificador de cada evento que ya registró, para no registrarlo dos veces.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con los dos procesos en marcha, un evento publicado queda registrado una sola vez en el receptor y su publicación queda completada en menos de 10 segundos.
- **SC-002**: Con el proceso de identidad detenido, el 100 % de las transacciones que publican eventos se confirman, y el 100 % de las publicaciones de ese lapso quedan pendientes.
- **SC-003**: Después de que el proceso de identidad vuelve, el 100 % de las publicaciones pendientes quedan registradas y completadas en menos de dos ciclos de la tarea programada.
- **SC-004**: Un mismo evento entregado N veces, seguidas o simultáneas, queda registrado exactamente una vez, y las N entregas se confirman.
- **SC-005**: Ninguna transacción que se revierte deja una publicación, comprobado con al menos un caso automático.
- **SC-006**: Una entrega sin token, con un token vencido o con el token de un usuario responde 401 o 403 y no registra el evento; a través del API Gateway, la ruta del receptor nunca llega al proceso: responde 401 sin token y 403 con token.
- **SC-007**: La memoria del perfil consolidado sigue por debajo de la alerta de 7 GiB de R-19 con el mecanismo activo (D-36).

## Assumptions

- El emisor de la demostración es un servicio del proceso distributivo y el receptor está en el proceso de identidad, como dicen los escenarios. El mecanismo vale para cualquier par de procesos.
- La bitácora está en Usuarios (F17-H01, servicio 01-usuarios) y las notificaciones en Notificaciones (F16-H01, servicio 07-notificaciones); los dos están en el proceso de identidad (ADR-006).
- El registro persistente de publicaciones es el de Spring Modulith que fija la constitución (principio V y sección 3); dónde se guarda y cómo se configura se decide en el plan.
- El intervalo de la tarea programada y el plazo de espera son parámetros de configuración con un valor por defecto; el plan fija los valores.
- Los reenvíos no tienen un número máximo de intentos: una publicación pendiente se reenvía hasta que el receptor la confirma.
- Hoy no hay llamadas HTTP entre procesos; esta historia trae la primera. Como la entrega ocurre fuera de la petición del usuario, un fallo de entrega no llega al usuario como error: deja la publicación pendiente. Los errores 502 y 504 entre procesos que dejaron pendientes ADR-016 y D-40 llegan con la primera llamada que responda a un usuario.
- La credencial de cada proceso es un secreto del entorno, como las claves de la base: en local y en la CI viene de `.env.ejemplo`, con un valor ficticio, y en el VPS la configura B00-H08 (principio IX). Cómo se emite el token de proceso lo detalla el plan sobre ADR-014.
- Solo se usan datos ficticios (principios VIII y IX).
