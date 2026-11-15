# Contrato: Firma

En `contratos/firma/openapi.yaml`.

## `subirFirma` (de F14-H02, ampliado)

- **`ResultadoDeComprobacion.comprobacion`:** suma `VIGENCIA`, `ORDEN` y `FIRMANTE`.
- **`ResultadoValidacion.comprobaciones`:** `minItems: 7`, `maxItems: 7`.
- **El 422 suma** `FIRMA_CERTIFICADO_NO_VIGENTE`, `FIRMA_FUERA_DE_ORDEN` y `FIRMA_FIRMANTE_EQUIVOCADO`, con las siete comprobaciones en los detalles.
- **Un 409 nuevo,** `FIRMA_PASO_SIN_NOMBRE`: el paso en turno no tiene guardado su nombre impreso (un documento anterior a F14-H03).
- **La descripción:** suma las tres comprobaciones y que, con la última firma, la cadena queda completa.

## Catálogo (`contratos/comun/errores.md`, `ErroresFirma`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `FIRMA_CERTIFICADO_NO_VIGENTE` | 422 | Certificado no vigente | El certificado de la firma no está vigente. Firma con un certificado vigente y vuelve a subir el PDF. |
| `FIRMA_FUERA_DE_ORDEN` | 422 | Firma fuera de orden | La firma es de la persona de un paso posterior. Primero firma la persona del paso en turno. |
| `FIRMA_FIRMANTE_EQUIVOCADO` | 422 | Firmante equivocado | El certificado no es de la persona que el documento indica para este paso. Firma con tu propio certificado. |
| `FIRMA_PASO_SIN_NOMBRE` | 409 | Paso sin firmante registrado | El documento no tiene registrado el nombre de quien firma este paso. Avisa al Responsable de consolidación para generarlo otra vez. |

Cada fila con sus 8 columnas (principio XIII).
