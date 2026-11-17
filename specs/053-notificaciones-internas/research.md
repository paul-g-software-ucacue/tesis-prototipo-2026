# Research: Recibir notificaciones internas por cada cambio de estado

Plan de F16-H01 (issue #67). Cada sección cierra una pregunta del diseño con la decisión, su porqué y lo que se descartó.

## 0. Lo que ya existe

| Pieza | Dónde | Qué hace hoy |
|---|---|---|
| Transiciones | `ProcesoAprobacion` + `RepositorioProceso.guardar(proceso, nuevas)` en Validación OCS | Las 8 operaciones que cambian el flujo pasan por `procesos.guardar(…, nuevas)` en su transacción: `FlujoDelDistributivoServicio` (5), `RegistrarResolucionServicio`, `RegistrarRechazoEnFirmaServicio` y `CompletarFirmasServicio`. |
| Etapas del proceso | `EtapaDelProceso(tipo, activa, nombre, rol, admiteDevolucion)` en el agregado | Es la copia de la configuración con la que empezó el distributivo. `siguienteActiva(etapa)` ya existe. |
| Publicar hacia otro proceso | `PublicadorDeCambios` (Validación OCS → `usuarios`) | Publica un `EventoEntreProcesos` en la transacción (ADR-018). El destino `usuarios` está en `apps/distributivo/application.yaml`. |
| Receptor idempotente | `usuarios.evento_recibido` + `/interno/usuarios/eventos` | Es el patrón que se copia (ADR-018 §4). |
| Entrega local | `ReceptorLocal` (ADR-024) | Dentro de `identidad`, sin HTTP. |
| Notificaciones | `servicios/notificaciones`, en el proceso `identidad` | Solo tiene el esquema y la configuración de Flyway. No tiene contrato ni rutas. |
| Gateway | `backend/gateway/application.yaml` | Ya enruta `/api/notificaciones/**` a `identidad`. |
| Roles en el proceso | `roles.puertos.entrada` (`@NamedInterface("api")`) | Tiene los roles de cada usuario (`AsignarRoles.rolesDeUsuarios`), la carrera de cada coordinador (`AsignarCarrera.carreras`) y el Responsable del periodo (`ConsultarResponsable`). |
| Usuarios en el proceso | `usuarios.puertos.entrada.ListarUsuarios` | Da `usuario`, `nombre` y `activo`. |
| Ruta solo con sesión | `/api/usuarios/sesion/renovacion` | Es una operación sin `x-mdd-permiso`: basta con la sesión. |

## 1. Quién publica y cuándo

- **Decision:**
  - Validación OCS suma el puerto de salida `NotificacionDeTransiciones.notificar(proceso, nuevas)`.
  - Cada una de las 8 operaciones lo llama justo después de `procesos.guardar(…, nuevas)`, dentro de la misma transacción.
  - El adaptador `PublicadorDeNotificaciones` publica un `EventoEntreProcesos` de tipo `validacionocs.NotificacionesSolicitadas` hacia `notificaciones` por cada transición nueva.
- **Rationale:**
  - Notificar es una regla del flujo. Que la llamada se vea en cada caso de uso es explícito, igual que el registro de cambios.
  - Va en la misma transacción: si la transición se revierte, la notificación también (FR-005). Con `identidad` caída queda pendiente (FR-011, FR-013).
- **Alternatives considered:**
  - Publicar dentro de `RepositorioProcesoJdbc.guardar`: mezcla la persistencia con una regla.
  - Escuchar `CambioDeFlujo`: no todas las acciones son transiciones (por ejemplo, `CONFIGURACION_FLUJO_CARGADA`).

## 2. Quién decide los destinatarios

- **Decision:** se reparte entre los dos lados.
  - **Validación OCS** decide el *destino* con una regla de dominio, `ProcesoAprobacion.destinoDe(transicion)`:
    - el rol de la etapa `hacia`, sacado de `EtapaDelProceso`;
    - en `ENVIO_PARCIAL`, el rol de `siguienteActiva(BORRADOR_CARRERA)`;
    - en `DEVOLUCION`, `COORDINADOR_CARRERA` con las carreras devueltas.
  - **Notificaciones** resuelve el destino a usuarios dentro de `identidad`, con un caso de uso nuevo de Roles, `ConsultarDestinatarios.destinatarios(rol, periodo, carreras)`:
    - `RESPONSABLE_CONSOLIDACION` da quienes tienen el rol asignado y, además, el designado del periodo (cambiado durante T021, con el autor);
    - `COORDINADOR_CARRERA` con carreras da los coordinadores de esas carreras, con su carrera;
    - cualquier otro rol da todos los usuarios que lo tienen.
  - Después, Notificaciones deja solo los usuarios activos (`ListarUsuarios`) y quita al autor (FR-002 d).
- **Rationale:**
  - El glosario dice que Notificaciones recibe «notificar a estos roles o usuarios este hecho» y solo depende de Identidad.
  - Las designaciones y las carreras son de Roles, que vive en el mismo proceso.
  - Validación OCS ya conoce el rol de cada etapa.
  - Los destinatarios se fijan al recibir, como dice el supuesto de la spec.
- **Alternatives considered:**
  - Que Validación OCS resuelva los usuarios: tendría que consultar a Roles por HTTP (ADR-023) en la transacción del usuario.
  - Que Notificaciones lea la configuración del flujo: dependería de Aprobación y formaría un ciclo con el glosario.

## 3. El contenido del evento

- **Decision:** `{periodo, autor, momento, mensajes: [{destino: {rol, carrera?}, titulo, detalle, enlace}]}`.
  - Hay un mensaje por destino. Una devolución lleva uno por carrera, con su observación (edge case de dos carreras).
  - Validación OCS arma `titulo` y `detalle` con los datos de FR-003:
    - el nombre de la etapa de la configuración, la versión y la carrera;
    - la observación, o el rechazo (paso, rol, quién y observación).
  - `enlace` es una ruta relativa de la SPA, según la etapa de llegada:
    - BORRADOR_CARRERA → `/oferta`;
    - EN_CONSOLIDACION y EN_REVISION_VRA → `/consolidado`;
    - CONGELADO_EN_FIRMA → `/firmas-pendientes`;
    - EN_OCS y APROBADO → `/resolucion-ocs`.
    - Notificaciones la valida con un patrón (`^/([a-z0-9-]+(/[a-z0-9-]+)*)?$`).
- **Rationale:** Notificaciones es genérico (glosario): guarda y muestra mensajes, sin conocer el flujo. F16-H02 y F18-H01 reutilizan el mismo tipo de contenido.
- **Alternatives considered:**
  - Datos estructurados que la SPA convierte en texto: Notificaciones tendría que conocer cada tipo de transición.
  - Una clave de pantalla en lugar de una ruta: es una tabla más que mantener en dos lados.
- **Seguridad (FR-014):** el receptor rechaza claves que parezcan secretos, con la regla de la bitácora (F17-H01 §5), y textos de más de 1 000 caracteres. El error es `NOTIFICACIONES_NOTIFICACION_INVALIDA` (400, código nuevo del catálogo).

## 4. El receptor y las tablas

- **Decision:** dos migraciones de Notificaciones.
  - **`V2`, tabla `notificaciones.evento_recibido`:** el patrón de ADR-018.
  - **`V2`, tabla `notificaciones.notificacion`:**
    - columnas: `id` uuid, `id_evento`, `destinatario`, `carrera` nulo, `periodo`, `titulo`, `detalle`, `enlace`, `autor`, `autor_nombre`, `momento`, `recibida_en`, `leida_en` nulo;
    - única por `(id_evento, destinatario, coalesce(carrera,''))`;
    - índice por `(destinatario, momento desc)`.
  - **Protección:**
    - un disparador solo permite el `UPDATE` de `leida_en`, de nulo a una fecha;
    - `REVOKE DELETE, TRUNCATE` (FR-009).
  - **La ruta:** `POST /interno/notificaciones/eventos`, solo con el rol `PROCESO`. Responde 204, también a un evento repetido (FR-012). En una sola transacción registra `evento_recibido` y, si el evento es nuevo, las filas.
  - **Si Roles o Usuarios fallan:** la respuesta es 5xx y el evento se reenvía (ADR-018 §3).
- **`autor_nombre`:** sale de `ListarUsuarios` al recibir. Si el autor es `sistema`, se guarda «el sistema» (el autor prefirió ver nombres a ver roles; F12-H04).

## 5. La bandeja: API y permiso

- **Decision:** tres operaciones en el contrato nuevo, `contratos/notificaciones/openapi.yaml`, **sin `x-mdd-permiso`**: basta con la sesión, como `renovarSesion`.
  - `GET /api/notificaciones/bandeja?limite=50`: las del usuario del token, de la más nueva a la más antigua, con `sinLeer`.
  - `GET /api/notificaciones/bandeja/sin-leer`: solo el número, para la campana.
  - `POST /api/notificaciones/bandeja/{id}/lectura`:
    - marca la notificación como leída y responde 200 con ella; marcar una ya leída es idempotente;
    - con el `id` de otro usuario, responde 404 `NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA`, sin revelar que existe (FR-008).
- **Rationale:** cada usuario solo ve lo suyo, sea cual sea su rol. Un permiso que tienen los diez roles no protege nada (principio X).
- **Alternatives considered:** `NOTIFICACIONES:LECTURA` para los diez roles: es una migración de Roles más, sin beneficio.

## 6. La SPA

- **La campana:** `navegacion/Campana.tsx` en el `Toolbar` de `Marco.tsx`, con un `Badge` del número sin leer y `aria-label` «Notificaciones, N sin leer».
- **La actualización:** consulta `sin-leer` cada 30 s y al volver el foco a la ventana (SC-004: menos de 1 minuto).
- **La lista:** un `Popover` con las notificaciones; las no leídas, en negrita y con un punto.
- **Abrir una:**
  - hace `POST …/lectura`;
  - navega al `enlace`;
  - actualiza el número.
- **Sin notificaciones:** dice «No tienes notificaciones».
- **Pruebas:** Vitest y Playwright con axe-core (SC-006).

## 7. Rutas de entrega

| Proceso | Cambio |
|---|---|
| `distributivo` | `mdd.eventos.destinos.notificaciones: ${MDD_URI_IDENTIDAD}/interno/notificaciones/eventos` |
| `identidad` | Notificaciones suma `:plataforma:eventos` y su ruta `/interno/notificaciones/eventos`, más las dependencias de `roles` y `usuarios` (puertos de entrada). |

## 8. La demostración

- **Decision:** `probar_documento_firmable.sh` revisa las bandejas después de cada transición de la cadena y suma tres pasos: la bandeja y la lectura, la caída de `identidad` y la entrega repetida ([contracts/demostracion.md](contracts/demostracion.md)).
- **Rationale:** es el único script que recorre todas las transiciones, de la propuesta de carrera a APROBADO, y el workflow `perfil-consolidado` ya lo corre. La caída y la repetición siguen la forma de `probar_bitacora.sh`.
- **Alternatives considered:** un script nuevo; tendría que preparar la misma cadena otra vez y sumaría unos 90 s al workflow.

## 9. Lo que no cambia

- La matriz D-16: la bandeja no pide permiso (§5).
- Las transiciones y la bitácora: marcar como leída no deja entrada (supuesto de la spec).
- El API Gateway: ya enruta `/api/notificaciones/**`.
- El glosario: ya define `Notificacion` y el contexto Notificaciones.
