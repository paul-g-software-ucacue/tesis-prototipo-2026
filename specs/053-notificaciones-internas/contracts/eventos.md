# Contrato: el evento `validacionocs.NotificacionesSolicitadas`

F16-H01 (#67). Viaja en el sobre común `EventoEntreProcesos` (`contratos/comun/eventos.yaml`, ADR-018 §2), de `validacionocs` (proceso `distributivo`) a `notificaciones` (proceso `identidad`), por `POST /interno/notificaciones/eventos`.

- Se publica uno por transición, en la transacción que la guarda (research §1).
- El `idEvento` lo genera Validación OCS y no cambia en los reenvíos.

## Contenido (`SolicitudDeNotificaciones`)

```json
{
  "periodo": "2026-I",
  "autor": "consolidacion.demo",
  "momento": "2026-11-15T15:04:05Z",
  "mensajes": [
    {
      "destino": { "rol": "VICERRECTOR" },
      "titulo": "El distributivo llegó a «Revisión del Vicerrectorado»",
      "detalle": "Periodo 2026-I, versión 1.",
      "enlace": "/consolidado"
    }
  ]
}
```

- `destino.carrera` solo va en una devolución.
- El rol `RESPONSABLE_CONSOLIDACION` se resuelve a quienes lo tienen asignado y al designado del `periodo`.
- Notificaciones no interpreta el título ni el detalle: los guarda tal como llegan (research §3).
- Las reglas son las de [data-model.md](../data-model.md) (`SolicitudDeNotificaciones`).

## Los mensajes de cada transición

`{etapa}` es el nombre de la etapa en la copia de la configuración del proceso, por ejemplo «Revisión del Vicerrectorado». `{v}` es la versión de la transición.

`{carrera}` es el nombre de la carrera en Académico, por ejemplo «DESARROLLO DE SOFTWARE»:
- Validación OCS lo pide por la ruta interna `GET /interno/academico/carreras` del proceso datos (ADR-023), solo cuando un mensaje nombra una carrera.
- Si Académico no responde o no tiene esa carrera, el título lleva el código; la transición nunca falla por eso.
- Si el título pasa de 200 caracteres (un nombre de 150 con una etapa de 80, en una devolución), el nombre se acorta con «…».
- El destino sigue llevando el código de la carrera.

| Transición | Destino | Título | Detalle | Enlace |
|---|---|---|---|---|
| `ENVIO_PARCIAL` | Rol de `siguienteActiva(BORRADOR_CARRERA)` | La carrera {carrera} envió su parcial | Periodo {p}, versión {v}. | El de esa etapa |
| `PASO_DE_ETAPA` hacia X | Rol de X | El distributivo llegó a «{etapa X}» | Periodo {p}, versión {v}. | El de X |
| `DEVOLUCION` | `COORDINADOR_CARRERA` con {carrera}, un mensaje por carrera | El parcial de {carrera} fue devuelto a «{etapa}» | Observación: «{observación de esa carrera}» | `/oferta` |
| `RECHAZO_EN_FIRMA` | Rol de la etapa `hacia` | La firma F{orden} fue rechazada: el distributivo volvió a «{etapa}» | Rechazó el paso F{orden} ({rol}): «{observación}» | `/consolidado` |
| `VERSION_NUEVA` | Rol de la etapa `hacia` | Se creó la versión {v} del distributivo | La versión {v-1} quedó anulada por el rechazo en firma. | `/consolidado` |

**Enlace por etapa de llegada:**

| Etapa de llegada | Enlace |
|---|---|
| BORRADOR_CARRERA | `/oferta` |
| BORRADOR_UNIDADES | `/` (el Gestor de unidad no tiene una pantalla del distributivo) |
| EN_CONSOLIDACION, EN_REVISION_VRA, REMITIDO_ZONAL | `/consolidado` |
| CONGELADO_EN_FIRMA | `/firmas-pendientes` |
| EN_OCS, APROBADO | `/resolucion-ocs` |
| PUBLICADO | `/resolucion-ocs`, provisorio: lo fija F18-H01 |

Una prueba de la SPA comprueba dos cosas para cada etapa de la configuración por defecto:
- que el enlace es una ruta de `rutas.tsx`;
- que el rol de la etapa tiene el permiso que esa ruta pide.

El detalle del rechazo nombra el paso y el rol, no el nombre de usuario de quien rechazó: Validación OCS no conoce el nombre para mostrar, y el autor de la transición es «el sistema».

## Ejemplo: la devolución de dos carreras

```json
{
  "periodo": "2026-I", "autor": "consolidacion.demo", "momento": "…",
  "mensajes": [
    { "destino": { "rol": "COORDINADOR_CARRERA", "carrera": "TI" },
      "titulo": "El parcial de TECNOLOGÍAS DE LA INFORMACIÓN fue devuelto a «Propuesta de carrera»",
      "detalle": "Observación: «Falta el docente de Redes II»", "enlace": "/oferta" },
    { "destino": { "rol": "COORDINADOR_CARRERA", "carrera": "ADM" },
      "titulo": "El parcial de ADMINISTRACIÓN fue devuelto a «Propuesta de carrera»",
      "detalle": "Observación: «Revisar las horas de tutoría»", "enlace": "/oferta" }
  ]
}
```

## Configuración

- `apps/distributivo/application.yaml`: `mdd.eventos.destinos.notificaciones: ${MDD_URI_IDENTIDAD:http://identidad:8080}/interno/notificaciones/eventos`.
- La clave de proceso de `distributivo` ya existe.
