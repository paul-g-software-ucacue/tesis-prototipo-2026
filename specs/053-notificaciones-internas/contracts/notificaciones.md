# Contrato: Notificaciones (`contratos/notificaciones/openapi.yaml`, nuevo)

F16-H01 (#67). Lo que se publica en el contrato antes del código (principio IV). Las rutas `/api/notificaciones/**` las enruta ya el API Gateway hacia `identidad`.

## Operaciones

| operationId | Método y ruta | Seguridad | Respuestas |
|---|---|---|---|
| `recibirEvento` | `POST /interno/notificaciones/eventos` | Solo token de proceso (rol `PROCESO`, ADR-018 §6) | 204; 400 `COMUN_SOLICITUD_INVALIDA`, `COMUN_EVENTO_NO_RECONOCIDO`, `NOTIFICACIONES_NOTIFICACION_INVALIDA`; 401; 403 con un token de usuario |
| `consultarBandeja` | `GET /api/notificaciones/bandeja?limite=50` | Sesión, **sin `x-mdd-permiso`** (research §5) | 200 `Bandeja`; 400 si `limite` sale de 1..100; 401 |
| `consultarSinLeer` | `GET /api/notificaciones/bandeja/sin-leer` | Sesión | 200 `{ "sinLeer": 3 }`; 401 |
| `marcarLeida` | `POST /api/notificaciones/bandeja/{id}/lectura` | Sesión | 200 `Notificacion`; 404 `NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA` si no existe o es de otro usuario; 400 si el `id` no es un UUID; 401 |

**`recibirEvento`:**
- Admite solo el tipo `validacionocs.NotificacionesSolicitadas`, con `origen` = `validacionocs`.
- F16-H02 sumará `firma.NotificacionesSolicitadas`.
- El contenido es `SolicitudDeNotificaciones` ([eventos.md](eventos.md)).
- Un `idEvento` ya registrado responde 204 sin volver a crear nada.

**La bandeja:** el destinatario siempre es el `sub` del token. No hay un parámetro de usuario.

## Esquemas

```yaml
Notificacion:
  type: object
  required: [id, periodo, titulo, detalle, enlace, autor, autorNombre, momento, leida]
  properties:
    id: { type: string, format: uuid }
    periodo: { type: string, example: 2026-I }
    carrera: { type: string, description: Solo en una devolución. }
    titulo: { type: string, minLength: 1, maxLength: 200 }
    detalle: { type: string, maxLength: 1000 }
    enlace: { type: string, pattern: '^/([a-z0-9-]+(/[a-z0-9-]+)*)?$', example: /consolidado }
    autor: { type: string, description: El usuario que hizo la transición, o sistema. }
    autorNombre: { type: string, description: Su nombre al recibirla, o «el sistema». }
    momento: { type: string, format: date-time }
    leida: { type: boolean }
    leidaEn: { type: string, format: date-time }
Bandeja:
  type: object
  required: [notificaciones, sinLeer]
  properties:
    notificaciones: { type: array, items: { $ref: '#/components/schemas/Notificacion' } }
    sinLeer: { type: integer, minimum: 0, description: Todas las sin leer, no solo las de la página. }
SolicitudDeNotificaciones:   # el contenido del evento; ver eventos.md
```

La bandeja se ordena por `momento` descendente y luego por `id`.

## Errores nuevos (`contratos/comun/errores.md` y `ErroresNotificaciones`)

| Código | Estado | Título | Clasificación | Causa |
|---|---|---|---|---|
| `NOTIFICACIONES_NOTIFICACION_INVALIDA` | 400 | Notificación no válida | Validación | Una `SolicitudDeNotificaciones` que no cumple su esquema o sus invariantes: un largo, el enlace, un rol desconocido, una clave que parece un secreto o más de 64 KB. No registra nada, y el emisor deja la publicación pendiente. Solo lo recibe otro proceso. |
| `NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA` | 404 | Notificación no encontrada | Negocio | Marcar como leída una notificación que no existe o que es de otro usuario. No dice cuál de las dos (principio IX). |

Cada uno lleva las 8 columnas del catálogo: descripción, acción y descripción técnica.

## Pruebas de contrato

- **`EventosNotificacionesContratoTest`:**
  - el 204, el repetido y cada 400;
  - el 401 sin token y el 403 con un token de usuario.
- **`BandejaContratoTest`:**
  - la bandeja propia, ordenada, con `sinLeer`;
  - el `limite`;
  - marcar, y marcar dos veces;
  - el 404 con la de otro usuario y el 401 sin sesión.
- **`PermisosNotificacionesTest`:** `VerificadorDePermisos` sin operaciones con `x-mdd-permiso`. Comprueba que ninguna pide permiso y que todas piden sesión.
- **`ErroresNotificacionesTest`:** los dos códigos están en el catálogo.
- **`verificar:ruptura`:** el contrato es nuevo, así que no hay ruptura posible; se suma a la verificación.
