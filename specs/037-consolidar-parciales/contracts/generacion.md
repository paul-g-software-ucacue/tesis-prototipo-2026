# Contrato: Generación

Lectura: `CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA`. Escritura: `CONSOLIDACION:ESCRITURA`.

## `GET /api/generacion/periodos/{periodo}/consolidado`

```json
{
  "periodo": "2026-I",
  "creado": true,
  "modificable": true,
  "version": 1,
  "consolidacion": { "por": "consolidacion.demo", "en": "2026-11-02T15:00:00Z" },
  "desactualizado": false,
  "carreras": [
    {
      "carrera": "TDS",
      "estado": "EN_ELABORACION",
      "paralelos": 12,
      "propuestos": 9,
      "completo": false,
      "horas": { "clase": 40, "otrasDocencia": 12, "investigacion": 4, "vinculacion": 0, "gestion": 2 }
    }
  ],
  "totales": { "clase": 40, "otrasDocencia": 12, "investigacion": 4, "vinculacion": 0, "gestion": 2 },
  "abiertas": { "duras": 3, "blandas": 1 },
  "informe": {
    "nombre": "informe.pdf",
    "tamano": 204800,
    "huella": "…",
    "por": "consolidacion.demo",
    "en": "2026-11-02T15:10:00Z"
  }
}
```

Sin distributivo: 200 con `creado: false`, sin `version` y con las listas vacías. Sin consolidar: sin `consolidacion`. Sin informe: sin `informe`.

Errores: 401, 403, 404 `GENERACION_PERIODO_NO_ENCONTRADO`, 502 y 504.

## `POST /api/generacion/periodos/{periodo}/consolidado`

Responde 200 con el consolidado.

| Error | Cuándo |
|---|---|
| 409 `GENERACION_SIN_DISTRIBUTIVO` | no hay distributivo |
| 409 `GENERACION_PERIODO_CERRADO` | periodo cerrado |
| 404 `GENERACION_PERIODO_NO_ENCONTRADO` | el periodo no existe |
| 502 y 504 | otro proceso no responde |

## `PUT /api/generacion/periodos/{periodo}/consolidado/informe`

`multipart/form-data` con `archivo`. Responde 200 con el informe.

| Error | Cuándo |
|---|---|
| 409 `GENERACION_SIN_CONSOLIDAR` | no se consolidó |
| 400 `GENERACION_INFORME_NO_ES_PDF` | no es un PDF, o está vacío |
| 413 `GENERACION_INFORME_DEMASIADO_GRANDE` | más de 10 MB |
| 409 `GENERACION_SIN_DISTRIBUTIVO`, `GENERACION_PERIODO_CERRADO`, 404, 502 y 504 | como arriba |

## `GET /api/generacion/periodos/{periodo}/consolidado/informe`

Responde `application/pdf`, con `Content-Disposition: attachment; filename="informe-sustentado-<periodo>.pdf"`.

| Error | Cuándo |
|---|---|
| 404 `GENERACION_INFORME_NO_ENCONTRADO` | no hay informe |
| 500 `GENERACION_INFORME_ILEGIBLE` | el archivo falta o no coincide con su huella |
| 404 `GENERACION_PERIODO_NO_ENCONTRADO` | el periodo no existe |

## Errores nuevos

| Código | HTTP | Título | Mensaje |
|---|---|---|---|
| `GENERACION_SIN_CONSOLIDAR` | 409 | Sin consolidar | Primero consolida el periodo, y después adjunta el informe. |
| `GENERACION_INFORME_NO_ES_PDF` | 400 | El archivo no es un PDF | Adjunta el informe en PDF. Si lo tienes en otro formato, expórtalo a PDF. |
| `GENERACION_INFORME_DEMASIADO_GRANDE` | 413 | Informe demasiado grande | El informe pasa de 10 MB. Reduce su tamaño y vuelve a adjuntarlo. |
| `GENERACION_INFORME_NO_ENCONTRADO` | 404 | Sin informe | Este periodo todavía no tiene informe sustentado. |
| `GENERACION_INFORME_ILEGIBLE` | 500 | Informe no disponible | No se pudo leer el informe guardado. Vuelve a adjuntarlo o avisa al administrador. |

`PermisosGeneracionTest` pasa de 14 a 18 operaciones.
