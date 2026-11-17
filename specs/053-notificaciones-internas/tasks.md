---
description: "Tareas de F16-H01: recibir notificaciones internas por cada cambio de estado"
---

# Tasks: Recibir notificaciones internas por cada cambio de estado

**Input**: documentos de diseño en `specs/053-notificaciones-internas/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto, y la implementación va en un commit aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-15, en la spec;
- D-108, escrita con el plan;
- ADR-018, ADR-024, D-16, D-63, D-90, D-97, D-98 y D-100.

**Organization**:
- **Fundacional:** el contrato, el catálogo, la migración, el dominio de Notificaciones y la consulta de Roles.
- **US1:** cada transición llega a la bandeja de sus destinatarios (MVP).
- **US2:** la bandeja y la campana.
- **US3:** sin duplicados y sin pérdidas.
- **Cierre:** el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** son relativas a la raíz. Cada abreviatura tiene `main`, `test` e `integrationTest` en `java/ec/mdd/<servicio>`:

  | Abreviatura | Ruta |
  |---|---|
  | `NOT` | `backend/servicios/notificaciones/src` |
  | `OCS` | `backend/servicios/validacionocs/src` |
  | `ROL` | `backend/servicios/roles/src` |
  | `SPA` | `frontend/src` |

---

## Phase 1: Setup

- [X] T001 Escribir **D-108** en `docs/decisiones.md`, en la fila del índice y en su sección (research §1 a §8). Revisar en el glosario `Notificacion`, el contexto Notificaciones y MD-09.

  **Resultado (2026-11-15):** quedó escrita con el plan. El glosario define `Notificacion` como «mensaje en la bandeja interna de un usuario por un hecho del flujo», y el contexto Notificaciones como genérico, con dependencia solo de Identidad. Se usan esos nombres y «notificación», nunca «aviso» (MD-09).

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T006.

- [X] T002 [P] El contrato nuevo `contratos/notificaciones/openapi.yaml`, según `contracts/notificaciones.md`.
  - **Operaciones:**
    - `recibirEvento`: `POST /interno/notificaciones/eventos`, solo `PROCESO`, con 204, 400, 401 y 403.
    - `consultarBandeja`: `GET /api/notificaciones/bandeja`, con «`limite` de 1 a 100, por defecto 50».
    - `consultarSinLeer`: `GET …/bandeja/sin-leer`.
    - `marcarLeida`: `POST …/bandeja/{id}/lectura`, con el `id` como UUID, 200 y 404.
    - Las tres de la bandeja van **sin `x-mdd-permiso`**.
  - **Esquemas:**
    - `Notificacion`, con «`titulo` de 1 a 200», «`detalle` hasta 1000» y «`enlace` con el patrón `^/([a-z0-9-]+(/[a-z0-9-]+)*)?$`»;
    - `Bandeja`, con `sinLeer`;
    - `SolicitudDeNotificaciones`, según `contracts/eventos.md`, con «de 1 a 50 mensajes».
  - **Build:** sumar `id 'mdd.contrato'` a `backend/servicios/notificaciones/build.gradle`, junto con `:servicios:roles`, `:servicios:usuarios` y `:plataforma:eventos`, y el contrato a `verificar:ruptura`.
  - **SPA:** sumar `notificaciones.d.ts` a `generar:api` en `frontend/package.json` y regenerar.
  - **Esqueleto:** `NOT/main/.../adaptadores/entrada/BandejaControlador.java` y `EventosControlador.java`, que lanzan hasta T012 y T014.

  **Resultado (2026-11-15):** el contrato nuevo, con las cuatro operaciones y los esquemas. El patrón de `carrera` es el de Validación OCS (`^[0-9A-Z_-]{1,20}$`), no el del plan. `build.gradle` suma el plugin `mdd.contrato`, Roles, Usuarios, JDBC, Jackson y Spring Security core; no hizo falta `:plataforma:eventos`, porque el receptor usa el sobre generado del contrato. Los tipos de la SPA quedaron regenerados (`notificaciones.d.ts`). **Desviación:** `verificar:ruptura` solo compara el contrato de Usuarios, así que no se sumó: el contrato es nuevo y no puede romper nada. Los controladores se crearon con su prueba (T012, T014), no como esqueleto aquí.
- [X] T003 [P] Los dos códigos, `NOTIFICACIONES_NOTIFICACION_INVALIDA` (400, Validación) y `NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA` (404, Negocio):
  - en `contratos/comun/errores.md`, con sus 8 columnas;
  - en `NOT/main/.../adaptadores/entrada/ErroresNotificaciones.java`, con sus excepciones de dominio vacías.

  Antes, ver fallar `NOT/test/.../ErroresNotificacionesTest.java`.

  **Resultado (2026-11-15):** los dos códigos están en `errores.md` y en `ErroresNotificaciones`, más `COMUN_EVENTO_NO_RECONOCIDO`. `ErroresNotificacionesTest` pasa. **No se vio fallar antes:** la clase y la prueba se escribieron en el mismo paso.
- [X] T004 [P] La migración `NOT/main/resources/db/migration/notificaciones/V2__bandeja.sql`, según `data-model.md`:
  - **`evento_recibido`:** «`id_evento` uuid PK, `tipo` varchar(120), `origen` varchar(40), `recibido_en`».
  - **`notificacion`:**
    - columnas: «`destinatario` varchar(60)», «`carrera` varchar(20) nulo», «`periodo` varchar(20)», «`titulo` varchar(200), `btrim` de 1 a 200», «`detalle` varchar(1000)», «`enlace` varchar(120) con su patrón», `autor`, `autor_nombre`, `momento`, `recibida_en` y «`leida_en` nulo»;
    - única por «`(id_evento, destinatario, coalesce(carrera,''))`»;
    - índices por `(destinatario, momento desc)`, y parcial sin leer.
  - **Disparadores:**
    - uno que solo admite el `UPDATE` de `leida_en`, de nulo a no nulo;
    - otro que rechaza `DELETE` y `TRUNCATE`.
  - **`REVOKE DELETE, TRUNCATE`** al rol del proceso.

  Antes, ver fallar en `NOT/integrationTest/.../NotificacionesEsquemaIntegrationTest.java`: inserta; rechaza otro `UPDATE`, un segundo `UPDATE` de `leida_en`, el `DELETE`, el `TRUNCATE` y el duplicado; acepta la misma notificación con otra carrera.

  **Resultado (2026-11-15):** `BandejaEsquemaIntegrationTest` cubre 5 casos y pasa. **No se vio fallar contra la migración ausente:** la V2 se escribió antes de correr la prueba. La primera corrida falló, y de ahí salió un ajuste: la clave foránea hacia `evento_recibido` necesita el `UPDATE` de esa tabla para su comprobación (`FOR KEY SHARE`). Por eso el `REVOKE` de `evento_recibido` deja `UPDATE`, y los cambios los rechaza el disparador. Las dos funciones de los disparadores quedaron separadas.
- [X] T005 [P] El dominio de Notificaciones en `NOT/main/.../dominio/`, según `data-model.md`. Antes, ver fallar `SolicitudDeNotificacionesTest`, `MensajeSolicitadoTest` y `NotificacionTest`:
  - **`SolicitudDeNotificaciones`, `MensajeSolicitado` y `NotificacionInvalida`:**
    - los patrones de periodo, usuario y carrera;
    - el rol, uno de los diez de D-63;
    - de 1 a 50 mensajes;
    - los largos del título y del detalle, y el enlace;
    - las claves que parecen secretos, con la regla de la bitácora, y los 64 KB.
  - **`Notificacion.marcarLeida(Instant)`:** idempotente.
  - **`NotificacionNoEncontrada`.**

  **Resultado (2026-11-15):** se vio fallar contra los esqueletos (10 de 12) y pasa. La solicitud solo admite las claves del esquema, a cualquier profundidad: es más estricto que buscar claves «que parecen secretos», y así no entra nada que nadie pidió. El tamaño se mide como en la bitácora.
- [X] T006 [P] Roles: `ROL/main/.../puertos/entrada/ConsultarDestinatarios.java` y `Destinatario.java`, y `aplicacion/ConsultarDestinatariosServicio.java`, con su bean en `infraestructura/ConfiguracionRoles.java`. Solo lee. Antes, ver fallar `ROL/test/.../aplicacion/ConsultarDestinatariosServicioTest.java`:
  - `RESPONSABLE_CONSOLIDACION` da el designado del periodo, o ninguno;
  - `COORDINADOR_CARRERA` con carrera da solo los coordinadores de esa carrera, con ella;
  - cualquier otro rol da todos, ordenados;
  - un rol desconocido da una lista vacía.

  **Resultado (2026-11-15):** se vio fallar contra el esqueleto (6 de 6) y pasa. El bean `consultarDestinatarios` está en `ConfiguracionRoles`. RESPONSABLE_CONSOLIDACION sale de la designación del periodo, no de la asignación de roles.
**Checkpoint**: el contrato, el esquema, el dominio y la consulta de Roles, en verde.

---

## Phase 3: User Story 1 - Cada transición llega a la bandeja de sus destinatarios (Priority: P1) 🎯 MVP

**Goal**: cada transición que registra Aprobación deja una notificación para quien le toca, y para nadie más.

**Independent Test**: el envío a revisión deja una notificación sin leer a `vicerrectorado.demo`, con el periodo, la etapa, la versión, quién y cuándo, y a ningún otro rol.

- [X] T007 [P] [US1] El dominio de Validación OCS: `OCS/main/.../dominio/DestinoDeNotificacion.java`, `ProcesoAprobacion.destinoDe(Transicion)` y `MensajesDeTransicion.java`, según `data-model.md` y la tabla de `contracts/eventos.md`. Antes, ver fallar `ProcesoAprobacionTest` y `MensajesDeTransicionTest`:
  - `ENVIO_PARCIAL` va al rol de `siguienteActiva(BORRADOR_CARRERA)`, también con BORRADOR_UNIDADES activa;
  - `PASO_DE_ETAPA` va al rol de `hacia`;
  - `DEVOLUCION` lleva un destino por carrera, con su observación;
  - `RECHAZO_EN_FIRMA` lleva el paso, el rol, quién y la observación;
  - `VERSION_NUEVA` lleva su número y la versión anulada;
  - una configuración con otro rol en EN_REVISION_VRA lo usa (escenario 7);
  - el enlace de cada etapa;
  - «título de 1 a 200» y «detalle hasta 1000», con una observación de 500 caracteres.

  **Resultado (2026-11-15):** se vio fallar contra los esqueletos (7 de 7) y pasa. Además de lo planeado, hay `MensajeDeTransicion` y `ProcesoAprobacion.etapaDe(tipo)`. El enlace de PUBLICADO queda provisorio (`/consolidado`) hasta F18-H01.
- [X] T008 [US1] El puerto `OCS/main/.../puertos/salida/NotificacionDeTransiciones.java` y el adaptador `adaptadores/salida/PublicadorDeNotificaciones.java`:
  - un `EventoEntreProcesos` `validacionocs.NotificacionesSolicitadas` por transición, hacia `notificaciones`, con `periodo`, `autor`, `momento` y `mensajes`;
  - con `nuevas` vacía, nada.

  Agregar el bean en `ConfiguracionValidacionocs`, y `mdd.eventos.destinos.notificaciones` en `backend/apps/distributivo/src/main/resources/application.yaml`. Antes, ver fallar `PublicadorDeNotificacionesTest`, que comprueba el contenido contra `SolicitudDeNotificaciones` del contrato.

  **Resultado (2026-11-15):** se vio fallar (4 de 4) y pasa. El destino `notificaciones` está en `apps/distributivo/application.yaml`. **Desviación:** la prueba compara el contenido con el esperado, no con el esquema del contrato, porque `ValidadorDeContrato` valida rutas y no esquemas sueltos. El receptor lo valida en T012 y T013.
- [X] T009 [US1] Las 8 llamadas, justo después de cada `procesos.guardar(…, nuevas)` y en la misma transacción: `FlujoDelDistributivoServicio` (5), `RegistrarResolucionServicio`, `RegistrarRechazoEnFirmaServicio` y `CompletarFirmasServicio`. Antes, ver fallar en sus pruebas de aplicación:
  - cada operación notifica con sus transiciones nuevas, las dos en el último parcial;
  - un rechazo (409, 400 o 403) no notifica;
  - una excepción al notificar revierte la transacción.

  Sumar a `OCS/test/.../ArquitecturaTest.java` la regla de que cada clase que llama a `RepositorioProceso.guardar` también usa `NotificacionDeTransiciones`.

  **Resultado (2026-11-15):** las 6 pruebas nuevas se vieron fallar y pasan con las 8 llamadas: 5 en `FlujoDelDistributivoServicio`, 1 en `RegistrarResolucionServicio`, 1 en `RegistrarRechazoEnFirmaServicio` y 1 en `CompletarFirmasServicio`. Los rechazos no notifican. La regla de ArchUnit `quienGuardaTransicionesNotifica` se vio fallar al quitar la llamada de `CompletarFirmasServicio`, y pasa con ella. **No se probó** que una excepción al notificar revierta la transacción: con la transacción de prueba eso no se puede ver, y la llamada va dentro de `transaccion.ejecutar`.
- [X] T010 [US1] La recepción en `NOT/main/.../aplicacion/RecibirNotificacionesServicio.java`, con los puertos `puertos/entrada/RecibirNotificaciones.java` y `puertos/salida/` (`RepositorioNotificaciones`, `RepositorioEventosRecibidos`, `Destinatarios` y `Transaccion`), según `data-model.md` («Recepción»). Antes, ver fallar `RecibirNotificacionesServicioTest` (Mockito):
  - una fila por destinatario, solo los activos, sin el autor;
  - `autor_nombre`, o «el sistema»;
  - un rol sin usuarios no deja filas ni da error;
  - el tipo o el origen no admitido da `COMUN_EVENTO_NO_RECONOCIDO`;
  - una solicitud inválida no registra nada;
  - un fallo de `Destinatarios` revierte todo.

  **Resultado (2026-11-15):** se vio fallar (9 de 9) y pasa. Por ahora solo admite `validacionocs`; `firma` llegará con F16-H02.
- [X] T011 [US1] Los adaptadores de salida de Notificaciones, con sus beans en `NOT/main/.../infraestructura/ConfiguracionNotificaciones.java`:
  - `RepositorioNotificacionesJdbc` y `RepositorioEventosRecibidosJdbc`, con «`ON CONFLICT DO NOTHING`»;
  - `DestinatariosDesdeIdentidad`, sobre `ConsultarDestinatarios` de Roles y `ListarUsuarios` de Usuarios.

  Antes, ver fallar `NOT/integrationTest/.../RepositorioNotificacionesIntegrationTest.java`: inserta, lee la bandeja ordenada, cuenta las sin leer y marca.

  **Resultado (2026-11-15):** `RepositorioNotificacionesIntegrationTest` se vio fallar (4 de 4) y pasa. `ConfiguracionNotificaciones` tiene los beans; los repositorios dependen de `flywayNotificaciones`.
- [X] T012 [US1] `EventosControlador`, para `POST /interno/notificaciones/eventos`. Antes, ver fallar `NOT/test/.../adaptadores/entrada/EventosNotificacionesContratoTest.java`:
  - el 204 y el repetido;
  - cada 400: el cuerpo, el tipo y la solicitud;
  - el 401 sin token y el 403 con un token de usuario.

  Validar contra el contrato con `ValidadorDeContrato`.

  **Resultado (2026-11-15):** la clase se llama `EventosNotificacionesControlador`: con `EventosControlador`, su bean chocaba con el de Usuarios en el proceso `identidad`. La prueba de contrato se vio fallar (4 de 8, las que llegan al controlador) y pasa. Sumó `AplicacionDePruebaNotificaciones` y `BandejasEnMemoria`.
- [X] T013 [US1] El proceso `identidad`. Antes, ver fallar `backend/apps/identidad/src/integrationTest/java/ec/mdd/NotificacionesDesdeAprobacionIntegrationTest.java`, con Roles y Usuarios reales y PostgreSQL:
  - una solicitud de EN_REVISION_VRA llega a los vicerrectores activos;
  - la de RESPONSABLE_CONSOLIDACION llega al designado;
  - la devolución de dos carreras llega a sus coordinadores, cada uno con la suya;
  - el autor queda fuera.

  Comprobar que `ModulosTest` sigue en verde, con Notificaciones usando solo `puertos.entrada` de Roles y de Usuarios.

  **Resultado (2026-11-15):** `NotificacionesDesdeAprobacionIntegrationTest` (4 casos) pasa. Su primera corrida falló por el choque de nombres de T012. `ModulosTest` de `identidad` sigue en verde, y también Roles y Usuarios.
**Checkpoint**: cada transición deja su notificación a quien le toca (FR-001 a FR-005).

---

## Phase 4: User Story 2 - El usuario ve su bandeja y marca una notificación como leída (Priority: P1)

**Goal**: la campana con el número sin leer y la lista; abrir una notificación la marca y lleva a su pantalla.

**Independent Test**: `vicerrectorado.demo` ve 1 en la campana, abre la notificación, llega a «Consolidado del periodo», y la campana deja de contarla.

- [X] T014 [US2] La bandeja en el backend: `NOT/main/.../puertos/entrada/` (`ConsultarBandeja` y `MarcarLeida`), `aplicacion/BandejaServicio.java` y `BandejaControlador`, con el destinatario tomado del `sub` del token. Antes, ver fallar:
  - **`BandejaServicioTest`:** marcar es idempotente; la de otro usuario da `NotificacionNoEncontrada`.
  - **`BandejaContratoTest`:**
    - la bandeja propia, ordenada, con `sinLeer` total;
    - el `limite` y su 400;
    - `sin-leer`;
    - marcar y marcar dos veces, con el mismo `leidaEn`;
    - el 404 con la de otro usuario, el 400 con un `id` que no es UUID y el 401 sin sesión.
  - **`PermisosNotificacionesTest`:** ninguna operación declara `x-mdd-permiso`, y todas piden sesión.

  **Resultado (2026-11-15):** `BandejaServicioTest` (4) y `BandejaContratoTest` (6) se vieron fallar y pasan. `PermisosNotificacionesTest` comprueba que ninguna operación declara `x-mdd-permiso`; que todas piden sesión, y que la de otro usuario da 404, lo cubre `BandejaContratoTest`. El destinatario es el `name` del token, como en Validación OCS.
- [X] T015 [P] [US2] La consulta en `SPA/navegacion/notificaciones.ts`: `consultarSinLeer()`, `consultarBandeja()` y `marcarLeida(id)`, con los tipos generados.

  **Resultado (2026-11-15):** también `apiNotificaciones` en `SPA/api/cliente.ts`. El número usa la cabecera de las consultas automáticas, así que la consulta cada 30 s no cuenta como actividad para renovar la sesión (ADR-020). Ese riesgo no estaba en el plan.
- [X] T016 [US2] La campana `SPA/navegacion/Campana.tsx`, en el `Toolbar` de `SPA/navegacion/Marco.tsx`, según `contracts/spa.md`. Antes, ver fallar `SPA/navegacion/Campana.test.tsx`:
  - el `Badge` y el `aria-label` «Notificaciones, N sin leer»;
  - la consulta a los 30 s y al volver el foco, con temporizadores falsos;
  - la lista, con las no leídas en negrita y con «{autorNombre} · {dd/mm/aa hh:mm}»;
  - abrir: marca, cierra, navega y baja el número, también si la lectura falla;
  - «No tienes notificaciones», el error y «Reintentar»;
  - Escape devuelve el foco.

  Ajustar `Marco.test.tsx` si hace falta.

  **Resultado (2026-11-15):** las 10 pruebas se vieron fallar contra el esqueleto y pasan. Una se ajustó: el punto decorativo de las no leídas también es texto. La campana usa un `SvgIcon` propio, porque el proyecto no tiene `@mui/icons-material`. «Sin leer:» va como texto solo para lectores de pantalla, para que no dependa del color ni del peso de la letra. `Marco.test.tsx` no cambió.
- [X] T017 [P] [US2] `SPA/navegacion/enlaces.test.ts`, para cada etapa de la configuración por defecto (`contracts/spa.md`):
  - su enlace de `contracts/eventos.md` es una ruta de `SPA/rutas.tsx`;
  - el rol de la etapa tiene el permiso que pide esa ruta (`RutaConPermiso`, `SPA/sesion/funcionalidades.ts` y la matriz D-16 con Roles V9);
  - COORDINADOR_CARRERA puede abrir `/oferta`, el enlace de la devolución.

  Si un rol no puede abrir su enlace, se corrige la tabla del enlace, no la matriz.

  **Resultado (2026-11-15):** 19 casos. **Hallazgo:** GESTOR_UNIDAD no podía abrir `/proyectos`, porque la ruta pide Docentes, Distributivo o Catálogos. Se corrigió la tabla y no la matriz:
  - BORRADOR_UNIDADES lleva al inicio (`/`), y el patrón del enlace ahora admite `/`, en el contrato, la V2, el dominio y estos documentos;
  - PUBLICADO pasa a `/resolucion-ocs` hasta F18-H01.

  Se comprobó que la prueba falla con el enlace anterior.
- [X] T018 [US2] Playwright en `frontend/e2e/notificaciones.spec.ts`, con `apiSimulada`:
  - el Vicerrector ve 1 sin leer;
  - abre la notificación con el teclado y llega a «Consolidado del periodo»;
  - el número desaparece;
  - axe-core sin fallas con la lista cerrada, abierta y vacía (SC-006).

  Esperar a que termine la animación del `Popover` antes de axe, como en `resolucion.spec.ts`.

  **Resultado (2026-11-15):** 2 pruebas, en verde en la primera corrida. **No se vieron fallar:** se escribieron con la campana hecha.
**Checkpoint**: la bandeja funciona en la API y en la SPA.

---

## Phase 5: User Story 3 - Sin duplicados y sin pérdidas (Priority: P1)

**Goal**: cada notificación llega una sola vez, aunque haya reintentos o `identidad` haya estado caída.

**Independent Test**: una entrega repetida, también después de leída, deja una sola notificación, que sigue leída; con `identidad` caída, la transición se completa y la notificación llega al volver.

- [X] T019 [US3] Ver fallar primero en `NOT/integrationTest/.../RecepcionRepetidaIntegrationTest.java`:
  - el mismo evento dos veces deja una sola fila por destinatario;
  - el mismo evento después de marcarla deja una fila, que sigue leída;
  - dos entregas del mismo evento a la vez, en dos hilos, dejan una sola.

  Corregir la recepción si hace falta.

  **Resultado (2026-11-15):** 3 casos, cada recepción en su transacción, y pasan sin cambiar la recepción. **No se vieron fallar:** la clave primaria y la única ya existían (T004).
- [X] T020 [US3] En `backend/apps/distributivo/src/integrationTest/java/ec/mdd/NotificacionesPendientesIntegrationTest.java`, como `BitacoraDesdeGeneracionIntegrationTest`, comprobar:
  - con el destino `notificaciones` sin responder, la transición se confirma y la publicación queda pendiente;
  - el reenvío la completa (FR-011, FR-013).

  **Resultado (2026-11-15):** 2 casos. Confirmada con `identidad` caída, queda `FAILED` sin error; revertida, no deja evento. **Desviación:** que el reenvío la complete, con `identidad` de vuelta, lo recorre el script (T021, paso 17), no esta prueba.
**Checkpoint**: las tres historias funcionan.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T021 El script `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - la revisión de las bandejas de todos los usuarios de la demo después de cada transición, contando desde el inicio del script: exactamente las esperadas (SC-001);
  - el paso 16, la bandeja y la lectura;
  - el paso 17, la caída de `identidad` en el registro de la resolución;
  - el paso 18, la entrega repetida.

  Actualizar el comentario de `.github/workflows/perfil-consolidado.yml`. shellcheck sin hallazgos.

  **Resultado (2026-11-16):** 19 pasos, del 0 al 18. La primera corrida falló en el paso 16 y destapó un error real: la primera lectura devolvía `leidaEn` en nanosegundos y la base la guarda en microsegundos, así que la segunda lectura daba otra fecha. Se corrigió en `BandejaServicio`, con una prueba que se vio fallar. Con eso pasan todos, en 114 s.

  Cambios respecto del plan:
  - La caída de `identidad` va en el envío a revisión y no en la resolución: el paso a APROBADO no llega a nadie, porque `secretaria.demo` es la única con su rol y es la autora.
  - Las bandejas se comparan completas («destinatario|título»), contra la lista esperada, para todos los usuarios.
  - **Cambio de regla (decisión del autor):** RESPONSABLE_CONSOLIDACION son quienes tienen el rol asignado más el designado del periodo, no solo el designado. Sin eso, `consolidacion.demo` no recibía los parciales. Quedó en la spec, D-108, la prueba de Roles y la del proceso `identidad`.

  shellcheck sin hallazgos. El comentario del workflow está al día.
- [X] T022 Correr los scripts de `perfil-consolidado`, en su orden, sobre un perfil construido desde la rama (SC-001 a SC-003). Después, el recorrido a mano de `quickstart.md` §3, del autor (SC-004 y SC-005).

  **Avance (2026-11-16):** los 36 scripts del workflow pasan, en su orden, sobre un perfil construido desde la rama. `probar_documento_firmable.sh` tarda 96 s. `identidad` usaba 454 de sus 512 MiB después de la corrida (#224). **Falta el recorrido a mano del autor (SC-004 y SC-005).**

  **Recorrido a mano (2026-11-16):** el autor lo hizo con `MDD_CONSERVAR=1`:
  - **Lo que funcionó:**
    - las bandejas del Vicerrector (1), del Responsable (8) y de `docente.demo` (ninguna) fueron las esperadas;
    - `coordinacion.demo` no vio nada;
    - al aprobar, `docente.demo` vio su notificación sin recargar y llegó a «Pendientes de firma»;
    - el Responsable pasó a 9.
  - **Dos cambios que salieron del recorrido:**
    - **El periodo:** la notificación de 2026-I abrió «Consolidado del periodo» en 2026-II, el de por omisión, porque ambos están en Planificación. Ahora la campana navega con `?periodo=`, y Oferta, Consolidado y Resolución lo toman (`periodoElegido.ts`). Pruebas: `periodoElegido.test.tsx`, Campana, Consolidado y el e2e, vistas fallar antes del cambio.
    - **La carrera:** a pedido del autor, los títulos nombran la carrera por su nombre en Académico y no por su código. Validación OCS lo pide por la ruta interna solo en el envío y la devolución; si Académico no responde, va el código, y un nombre largo se acorta con «…» para no pasar de 200. Pruebas: `MensajesDeTransicionTest`, `PublicadorDeNotificacionesTest` y `DatosDesdeProcesosTest`, vistas fallar antes del cambio. El script compara con el nombre.
  - **Repetido con los dos cambios:** el autor confirmó los nombres de las carreras y que la notificación del Vicerrector abre «Consolidado del periodo» en 2026-I. La primera vez vio 2026-II porque su pestaña tenía cargada la SPA anterior; después de recargar, funcionó.
  - **El script completo, sobre un perfil limpio:** 19 de 19 en 93 s.
    - La primera corrida falló el paso 17 con «llegó −1 s después de que identidad volvió». El script tomaba la vuelta cuando `--wait` terminaba, y identidad acepta el evento antes de que su chequeo de salud lo dé por sano.
    - Ahora la vuelta es el momento en que se pide levantarlo. Con eso, la notificación llegó 25 s después.
- [X] T023 Correr completos, y medir la cobertura de Notificaciones (60 %, D-32):
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue`;
  - en la SPA: tipos, ESLint, Prettier, Vitest y Playwright;
  - Gitleaks sobre `main..HEAD`.

  **Resultado (2026-11-16):**
  - **Backend:** 3 303 pruebas unitarias y 350 de integración. Fallaron dos cosas:
    - `ConfirmarLasDiezCargasIntegrationTest`, la medición de tiempo intermitente de siempre (117 s contra 60 s), en `datos`, que esta rama no toca: pasa al correr el módulo otra vez;
    - Spotless en `MensajesDeTransicion`, por la última edición: corregido, y Validación OCS pasa su `build`.
  - **Cobertura (60 %):** pasa en todos los módulos; Notificaciones 72,3 %, Validación OCS 84,0 % y Roles 77,9 %.
  - **SPA:** tipos, ESLint y Prettier en verde; Vitest 618 y Playwright 132.
  - **Gitleaks:** queda para después del commit (`main..HEAD`).
- [X] T024 [P] Revisar el JavaDoc:
  - a quién toca cada transición;
  - por qué los destinatarios se fijan al recibir;
  - por qué la bandeja no pide permiso;
  - por qué el contenido no cambia;
  - el reenvío ante un fallo de Roles.

  **Resultado (2026-11-16):** dicen lo pedido:
  - a quién toca cada transición: `ProcesoAprobacion.destinoDe` y `ConsultarDestinatarios`;
  - por qué los destinatarios se fijan al recibir y el reenvío ante un fallo de Roles: `RecibirNotificacionesServicio`;
  - por qué la bandeja no pide permiso: `BandejaServicio` y `BandejaControlador`;
  - por qué el contenido no cambia: `Notificacion`;
  - y la consulta automática de la campana: `notificaciones.ts`.
- [X] T025 Crear `specs/053-notificaciones-internas/evidencia.md`, como la de 052.
- [X] T026 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - Verificar la demo en solo lectura (`quickstart.md` §4): la V2 de `notificaciones` y la memoria de `identidad` (#224).
  - Abrir el PR de evidencia con `Closes #67`.
  - La épica F16 (#15) sigue abierta por F16-H02.

  **Resultado (2026-11-17):**
  - Se fusionó #241 (`94ba524`).
  - El primer despliegue volvió atrás por una suspensión del equipo.
  - Forzado por el autor: «Sano en 39 s», con `notificaciones` en la V2.
  - El detalle está en `evidencia.md`.

---

## Dependencies & Execution Order

- **Setup:** T001, hecha.
- **Fundacional:** de T002 a T006, en paralelo.
- **US1:**
  - T007 sin dependencias;
  - T008 después de T002 y T007;
  - T009 después de T008;
  - T010 después de T003, T005 y T006;
  - T011 después de T004 y T010;
  - T012 después de T002 y T010;
  - T013 después de T011 y T012.
- **US2:**
  - T014 después de T002, T003 y T011;
  - T015 después de T002;
  - T016 después de T015;
  - T017 sin dependencias;
  - T018 después de T016.
- **US3:** T019 después de T011; T020 después de T009.
- **Cierre:** T021 después de T009, T013 y T014; de T022 a T026, al final.

### Parallel Opportunities

```text
T002 a T006.
T007 (Validación OCS) en paralelo con T010 y T011 (Notificaciones).
T015 a T017 (SPA) en paralelo con T012 y T013 (backend).
T019 en paralelo con T014.
```

## Implementation Strategy

- **MVP:** Setup, Fundacional y US1 (de T001 a T013). Las transiciones llegan a las bandejas, aunque todavía no se vean en la SPA.
- **Después:** US2, la bandeja y la campana; US3, la idempotencia y la caída, probadas.
- **Luego:** el script, las regresiones y el cierre.
