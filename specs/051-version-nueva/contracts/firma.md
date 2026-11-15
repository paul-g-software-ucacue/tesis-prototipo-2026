# Contrato: Firma

## `GET /api/firma/periodos/{periodo}/documento` (`EstadoDelDocumento`, suma un campo)

```yaml
anulacion:
  $ref: '#/components/schemas/AnulacionDeFirmas'
```

`AnulacionDeFirmas` (presente si las firmas del documento de esa versión se anularon):

```yaml
type: object
additionalProperties: false
required: [motivo, firmasAnuladas, anuladaEn]
properties:
  motivo: { type: string, enum: [RECHAZO_EN_FIRMA] }
  firmasAnuladas: { type: integer, format: int32, minimum: 0 }
  anuladaEn: { type: string, format: date-time }
```

La consulta muestra la última versión del periodo con documento: la anulada, hasta que se genere el de la versión nueva.

## API publicada (mismo proceso)

```java
/** Anula las firmas del documento de una versión (F12-H04, RF-59). Idempotente. */
void anular(String periodo, int version, MotivoDeAnulacion motivo, Instant momento);

/** El mismo valor del contrato. */
enum MotivoDeAnulacion { RECHAZO_EN_FIRMA }

/** El evento, en la misma transacción que la anulación. */
record FirmasAnuladas(
        String periodo, int version, int anuladas, MotivoDeAnulacion motivo, Instant momento) {}
```

| Situación | Resultado |
|---|---|
| Documento generado sin anulación | Fila en `anulacion_de_firmas`, bitácora `FIRMAS_ANULADAS`, evento `FirmasAnuladas` |
| Ya anulado | Nada |
| Sin documento para esa versión | Nada; queda en el log |

Con la anulación, nadie está en turno: «Pendientes de firma», la descarga, la subida y el rechazo lo tratan como el documento rechazado (403 común).

## Bitácora

| Acción | Detalle |
|---|---|
| `FIRMAS_ANULADAS` | `periodo`, `version`, `firmasAnuladas`, `motivo`; por `sistema` |
