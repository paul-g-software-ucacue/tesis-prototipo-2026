# Contrato: Validación OCS (Aprobación)

## `GET /api/validacionocs/periodos/{periodo}/flujo` (`EstadoDelFlujo`, suma un campo)

```yaml
rechazoEnFirma:
  $ref: '#/components/schemas/RechazoEnFirmaDelFlujo'
```

`RechazoEnFirmaDelFlujo` (presente mientras la última transición es un rechazo en firma):

```yaml
type: object
additionalProperties: false
required: [orden, rol, por, en, observacion]
properties:
  orden: { type: integer, format: int32, minimum: 1 }
  rol: { type: string }
  por: { type: string }
  en: { type: string, format: date-time }
  observacion: { type: string }
```

`TransicionDelFlujo.tipo` suma `RECHAZO_EN_FIRMA`. Con un rechazo pendiente, `acciones` no trae `APROBAR_CONTENIDO`.

## `POST /api/validacionocs/periodos/{periodo}/aprobacion` (suma un 409)

| Estado | Código | Cuándo |
|---|---|---|
| 409 | `VALIDACIONOCS_REQUIERE_VERSION_NUEVA` | El distributivo volvió por un rechazo en firma y todavía no hay una versión nueva (F12-H04) |

El envío a revisión, cuando congela al avanzar (EN_REVISION_VRA desactivada), suma el mismo 409.

## Catálogo

| Código | Estado | Título | Descripción | Clasificación |
|---|---|---|---|---|
| `VALIDACIONOCS_REQUIERE_VERSION_NUEVA` | 409 | Hace falta una versión nueva | El documento se rechazó en firma. Para aprobarlo otra vez hace falta una versión nueva del distributivo. | Negocio |

## Oyente `CierreDeLaFirma.alRechazar(RechazoEnFirma)`

Después de confirmar el rechazo, en una transacción nueva. Llama a `RegistrarRechazoEnFirma`; idempotente. Fuera de CONGELADO_EN_FIRMA o con otra versión, no mueve nada y lo anota en el log.
