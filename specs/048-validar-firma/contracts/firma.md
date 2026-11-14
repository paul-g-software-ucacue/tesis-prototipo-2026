# Contrato: Firma

En `contratos/firma/openapi.yaml`, tag `FirmasPendientes`.

## `POST /api/firma/documentos/{documento}/firmas` · `subirFirma`

- **Permiso:** `x-mdd-permiso: DISTRIBUTIVO:FIRMA`.
- **Cuerpo:** `multipart/form-data`, con una parte `archivo` (`type: string`, `format: binary`): el PDF firmado.
- **Parámetro:** `documento`, un uuid, con el mismo `example` de `descargarArchivoVigente`.

| Respuesta | Cuándo |
|---|---|
| `201` `InformeDeValidacion` | Las cuatro comprobaciones aprobadas: la firma quedó registrada |
| `400` `SolicitudInvalida` | El uuid mal formado o sin la parte `archivo` |
| `400` `FIRMA_ARCHIVO_VACIO` | La parte `archivo` sin bytes |
| `401`, `403` | Los comunes. El 403 también para quien no está en turno, o si el documento no existe o no está generado (FR-002) |
| `413` `FIRMA_ARCHIVO_DEMASIADO_GRANDE` | Más de 10 MB. Más de 11 MB lo corta el proceso con `COMUN_CONTENIDO_DEMASIADO_GRANDE` |
| `422` | Se rechazó: el código de la primera comprobación rechazada. `detalles` lleva las cuatro, en orden: `campo` = la comprobación, `descripcion` = su resultado (research §8) |
| `500` | `FIRMA_ARCHIVO_NO_DISPONIBLE` (el vigente falta o no coincide) o el error interno |
| `502`, `504` | Usuarios no respondió al pedir la identificación |

### Esquemas

```yaml
InformeDeValidacion:
  type: object
  additionalProperties: false
  required: [id, documento, orden, aceptada, sha256Subido, subidoEn, comprobaciones]
  properties:
    id: { type: string, format: uuid }
    documento: { type: string, format: uuid }
    orden: { type: integer, format: int32, minimum: 1 }
    aceptada: { type: boolean }
    sha256Subido: { type: string, pattern: '^[0-9a-f]{64}$' }
    subidoEn: { type: string, format: date-time }
    comprobaciones:
      type: array
      minItems: 4
      maxItems: 4
      items: { $ref: '#/components/schemas/ResultadoDeComprobacion' }
    certificado: { $ref: '#/components/schemas/CertificadoDeLaFirma' }
ResultadoDeComprobacion:
  type: object
  additionalProperties: false
  required: [comprobacion, resultado]
  properties:
    comprobacion: { type: string, enum: [MISMO_DOCUMENTO, UNA_FIRMA_NUEVA, INTEGRIDAD, CONFIANZA] }
    resultado: { type: string, enum: [APROBADA, RECHAZADA, NO_EVALUADA] }
    motivo: { type: string, maxLength: 200 }
CertificadoDeLaFirma:
  type: object
  additionalProperties: false
  required: [nombre, emisor, serie, validoDesde, validoHasta]
  properties:
    nombre: { type: string, maxLength: 200 }
    emisor: { type: string, maxLength: 400 }
    serie: { type: string, pattern: '^[0-9a-f]{1,64}$' }
    validoDesde: { type: string, format: date-time }
    validoHasta: { type: string, format: date-time }
```

`FirmaPendiente` y `descargarArchivoVigente` no cambian: siguen al paso en turno y al archivo vigente nuevos (research §2).

## Catálogo (`contratos/comun/errores.md`, `ErroresFirma`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `FIRMA_NO_ES_LA_VERSION_VIGENTE` | 422 | No es la versión vigente | El PDF no es la versión vigente del documento. Descarga el PDF de «Pendientes de firma», fírmalo y súbelo sin cambiarlo. |
| `FIRMA_SIN_UNA_FIRMA_NUEVA` | 422 | Firma nueva no encontrada | El PDF debe tener exactamente una firma nueva, la tuya. Firma una sola vez el PDF descargado y súbelo. |
| `FIRMA_NO_INTEGRA` | 422 | Firma no íntegra | La firma nueva no corresponde al contenido del PDF: el archivo cambió después de firmarlo. Vuelve a firmar el PDF descargado. |
| `FIRMA_SIN_CONFIANZA` | 422 | Certificado no reconocido | El certificado de la firma no es de una entidad de certificación de confianza. Firma con un certificado de una entidad acreditada. |
| `FIRMA_ARCHIVO_VACIO` | 400 | Archivo vacío | El archivo está vacío. Sube el PDF firmado. |
| `FIRMA_ARCHIVO_DEMASIADO_GRANDE` | 413 | Archivo demasiado grande | El archivo pasa de 10 MB, el tamaño máximo de un PDF firmado. |

Cada fila con sus 8 columnas (principio XIII), como las de F13-H02.
