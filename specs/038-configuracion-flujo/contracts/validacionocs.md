# Contrato: Validación OCS

Archivo nuevo: `contratos/validacionocs/openapi.yaml`.

## `GET /api/validacionocs/periodos/{periodo}/configuracion-flujo`

Permiso: `FLUJO_FIRMAS:LECTURA`.

```json
{
  "periodo": "2026-I",
  "porDefecto": false,
  "version": 2,
  "cargada": { "por": "admin.demo", "en": "2026-11-02T15:00:00Z" },
  "modificable": true,
  "etapas": [
    {
      "tipo": "EN_OCS",
      "esqueleto": true,
      "activa": true,
      "nombre": "En OCS",
      "rol": "SECRETARIA_GENERAL",
      "plazoDias": 10,
      "admiteDevolucion": true
    }
  ],
  "firmas": [
    {
      "orden": 1,
      "etiqueta": "Elaborado",
      "rol": "RESPONSABLE_CONSOLIDACION",
      "etapa": "CONGELADO_EN_FIRMA",
      "documento": "CONSOLIDADO"
    }
  ]
}
```

Sin archivo: `porDefecto: true`, sin `version` ni `cargada`. Errores: 401, 403, 404 `VALIDACIONOCS_PERIODO_NO_ENCONTRADO`, 502 y 504.

## `PUT /api/validacionocs/periodos/{periodo}/configuracion-flujo`

Permiso: `FLUJO_FIRMAS:ESCRITURA`. `multipart/form-data` con `archivo`. Responde 200 con la configuración vigente.

| Error | Cuándo |
|---|---|
| 400 `VALIDACIONOCS_CONFIGURACION_INVALIDA` | el archivo tiene problemas: uno por detalle |
| 400 `VALIDACIONOCS_ARCHIVO_ILEGIBLE` | vacío, no es YAML o UTF-8, o trae anclas, alias o tipos; con «Revisa la línea N.» si se conoce |
| 413 `VALIDACIONOCS_ARCHIVO_DEMASIADO_GRANDE` | más de 1 MB |
| 409 `VALIDACIONOCS_PERIODO_SIN_PLANIFICACION` | el periodo ya no está en Planificación |
| 404 `VALIDACIONOCS_PERIODO_NO_ENCONTRADO` | el periodo no existe |
| 502 y 504 | otro proceso no responde |

## `GET /api/validacionocs/plantilla-flujo`

Permiso: `FLUJO_FIRMAS:LECTURA`. Responde `text/yaml`, con `Content-Disposition: attachment; filename="flujo-por-defecto.yaml"`.

## Errores nuevos

| Código | HTTP | Título | Mensaje |
|---|---|---|---|
| `VALIDACIONOCS_CONFIGURACION_INVALIDA` | 400 | Configuración con problemas | Corrige lo que se indica y vuelve a cargar el archivo. |
| `VALIDACIONOCS_ARCHIVO_ILEGIBLE` | 400 | Archivo no válido | No se pudo leer el archivo. Parte de la plantilla y vuelve a cargarlo. |
| `VALIDACIONOCS_ARCHIVO_DEMASIADO_GRANDE` | 413 | Archivo demasiado grande | El archivo pasa de 1 MB. Parte de la plantilla y vuelve a cargarlo. |
| `VALIDACIONOCS_PERIODO_SIN_PLANIFICACION` | 409 | Periodo en curso | La configuración solo cambia mientras el periodo está en Planificación. |
| `VALIDACIONOCS_PERIODO_NO_ENCONTRADO` | 404 | Periodo no encontrado | El periodo no está en los ciclos académicos. Elige otro. |
