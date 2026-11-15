# Contrato: Firma

## `POST /api/firma/documentos/{documento}/rechazos` (nuevo, `rechazarEnFirma`)

Permiso `DISTRIBUTIVO:FIRMA`, el mismo de la subida.

**Solicitud** (`application/json`, `SolicitudDeRechazo`):

```yaml
type: object
additionalProperties: false
required: [observacion]
properties:
  observacion:
    type: string
    maxLength: 500
```

**Respuestas:**

| Estado | Cuándo | Cuerpo |
|---|---|---|
| 201 | Rechazo registrado | `RechazoEnFirma`: `documento`, `periodo`, `version`, `orden`, `rotulo` («Aprobado por»), `rechazadoPor`, `rechazadoEn`, `observacion` |
| 400 | El cuerpo no cumple el contrato | `COMUN_SOLICITUD_INVALIDA` |
| 400 | La observación, recortada, tiene menos de 10 caracteres | `FIRMA_OBSERVACION_INVALIDA` |
| 401 | Sin sesión | `SEGURIDAD_NO_AUTENTICADO` |
| 403 | Sin el permiso, fuera de turno, documento rechazado o con la cadena completa, o que no existe | `SEGURIDAD_ACCESO_NO_AUTORIZADO`, el 403 común de la subida (`SinTurnoDeFirma`); no dice cuál |

El turno se comprueba antes que la observación: a quien no está en turno no se le dice nada del cuerpo.

## `POST /api/firma/documentos/{documento}/firmas` (cambia la descripción)

Con el documento rechazado no hay nadie en turno: el 403 común (`SinTurnoDeFirma`). Una subida aceptada que llega a registrarse después de un rechazo se registra como rechazada por «mismo documento» (la regla de la subida tardía de F14-H02).

## `GET /api/firma/pendientes` (sin cambio de esquema)

Un documento rechazado no aparece.

## Catálogo (`contratos/comun/errores.md`, `ErroresFirma`)

| Código | Estado | Título | Descripción | Clasificación |
|---|---|---|---|---|
| `FIRMA_OBSERVACION_INVALIDA` | 400 | Observación no válida | Para rechazar en firma escribe una observación de 10 a 500 caracteres. | Validación |

## Evento `RechazoEnFirma` (API publicada de Firma)

`periodo`, `version`, `orden`, `rol`, `rechazadoPor`, `rechazadoEn`, `observacion`. Se publica en la transacción del rechazo; el registro de publicaciones lo reenvía (ADR-018).
