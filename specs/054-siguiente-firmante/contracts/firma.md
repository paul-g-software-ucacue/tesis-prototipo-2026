# Contrato: la cadena en `consultarDocumentoFirmable`

F16-H02 (#68). `GET /api/firma/periodos/{periodo}/documento`, de `contratos/firma/openapi.yaml`. Cambia solo la respuesta; el permiso (`CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA`), los errores y el `POST` siguen igual.

## `EstadoDelDocumento` suma `cadena`

```yaml
cadena:
  description: >-
    Los pasos de la cadena de firmas con su estado, solo con GENERADO y sin anulación; vacía en
    cualquier otro caso (F16-H02).
  type: array
  maxItems: 10
  items:
    $ref: '#/components/schemas/PasoConEstado'
```

`cadena` es requerida, y es `[]` cuando no corresponde. Así, la SPA no distingue entre «falta» y «vacía».

## `PasoConEstado` (nuevo)

| Campo | Tipo | Requerido | Regla |
|---|---|---|---|
| `orden` | integer, ≥ 1 | Sí | |
| `rotulo` | string, ≤ 60 | Sí | «Elaborado por», «Revisado por» o «Aprobado por», como en `FirmaPendiente.rotulo` |
| `firmante` | string, ≤ 200 | No | El nombre impreso en el PDF; falta en un documento sin nombres guardados |
| `estado` | enum | Sí | `FIRMADO`, `EN_TURNO`, `PENDIENTE`, `RECHAZADO` |
| `firmadoEn` | date-time | Solo con `FIRMADO` | Cuando el MDD aceptó la firma |

`additionalProperties: false`. No lleva la identificación, el usuario ni datos del certificado (FR-016).

## Ejemplo: F1 firmada y F2 en turno

```json
{
  "periodo": "2026-I",
  "version": 1,
  "estado": "GENERADO",
  "documento": { "sha256": "…", "hashContenido": "…", "generadoPor": "sistema", "generadoEn": "2026-11-18T15:00:00Z" },
  "acciones": [],
  "cadena": [
    { "orden": 1, "rotulo": "Elaborado por", "firmante": "Nombre del designado", "estado": "FIRMADO", "firmadoEn": "2026-11-18T15:04:05Z" },
    { "orden": 2, "rotulo": "Revisado por", "firmante": "Nombre del Vicerrector", "estado": "EN_TURNO" },
    { "orden": 3, "rotulo": "Aprobado por", "firmante": "Nombre del Rector", "estado": "PENDIENTE" }
  ]
}
```

## Pruebas de contrato

`DocumentoControlador` responde con la cadena en estos casos:
- recién generado: F1 en turno;
- con F1 firmada: F1 firmada, F2 en turno y F3 pendiente;
- completa: las tres firmadas y ninguna en turno;
- rechazada en F2: F1 firmada, F2 rechazada y F3 pendiente;
- con anulación, en generación, fallido o sin generar: la cadena vacía.

`verificar:ruptura` no compara este contrato: solo compara el de Usuarios. La compatibilidad la comprueban las pruebas de contrato de `DocumentoControlador` y los tipos regenerados de la SPA.
