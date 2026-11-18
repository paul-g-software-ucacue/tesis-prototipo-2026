# Contrato: el evento `firma.NotificacionesSolicitadas` y el destino por persona

F16-H02 (#68). Viaja en el sobre común `EventoEntreProcesos` (`contratos/comun/eventos.yaml`, ADR-018 §2), de `firma` (proceso `distributivo`) a `notificaciones` (proceso `identidad`), por `POST /interno/notificaciones/eventos`, la ruta de F16-H01.

- Se publica uno por firma aceptada que tiene un paso siguiente distinto de F1, en la transacción que la registra (research §1).
- El `idEvento` lo genera Firma y no cambia en los reenvíos.
- El destino ya está configurado en `apps/distributivo`: `mdd.eventos.destinos.notificaciones`, que usa Validación OCS desde F16-H01.

## Contenido (`SolicitudDeNotificaciones`, el mismo esquema)

```json
{
  "periodo": "2026-I",
  "autor": "docente.demo",
  "momento": "2026-11-18T15:04:05Z",
  "mensajes": [
    {
      "destino": { "identificacion": "0102030405" },
      "titulo": "Tienes una firma pendiente: «Revisado por»",
      "detalle": "Periodo 2026-I, versión 1. «Elaborado por» la firmó Nombre del designado.",
      "enlace": "/firmas-pendientes"
    }
  ]
}
```

La identificación del ejemplo es ficticia.

- `autor`: quien subió la firma aceptada.
- `momento`: cuándo la aceptó el MDD.
- Un solo mensaje por evento: el siguiente paso tiene un solo firmante esperado.

## El destino, en `contratos/notificaciones/openapi.yaml`

`DestinoDelMensaje` pasa a `oneOf` con tres esquemas, cada uno con `additionalProperties: false`:

| Esquema | Requeridos | Opcionales | Patrón |
|---|---|---|---|
| `DestinoPorRol` | `rol` | `carrera` | El de F16-H01 |
| `DestinoPorUsuario` | `usuario` | — | `minLength: 1`, `maxLength: 64`, sin patrón |
| `DestinoPorIdentificacion` | `identificacion` | — | `^[0-9A-Z]{1,13}$` |

- El cambio es compatible con lo que ya publica Validación OCS: un destino con `rol` sigue siendo válido.
- Un destino con dos de las tres claves, o con `carrera` sin `rol`, da 400 `NOTIFICACIONES_NOTIFICACION_INVALIDA`, sin repetir el valor recibido.

## Lo que hace Notificaciones

| Destino | Destinatarios |
|---|---|
| `{rol, carrera?}` | Como en F16-H01 |
| `{usuario}` | Esa cuenta, si existe y está activa |
| `{identificacion}` | La cuenta activa con esa identificación (`ConsultarCuentaPorIdentificacion` de Usuarios), si existe |

En todos los casos:
- se quita al autor;
- si no queda nadie, el evento se registra como recibido y no hay notificación: no es un error (FR-002 d);
- `ORIGENES` admite `firma`, y el tipo tiene que empezar con el origen: `firma.NotificacionesSolicitadas`.
