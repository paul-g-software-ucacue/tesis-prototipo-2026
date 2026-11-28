# Contrato: la exportación a SIGA en la API pública de Reporting

F19-H02 (#267). Los cambios en `contratos/reporting/openapi.yaml` (OpenAPI 3.0.3, tag `Siga`), antes del código (principio IV). El API Gateway ya enruta `/api/reporting/**`.

## `generarCsvSiga` (cambia)

`GET /api/reporting/periodos/{periodo}/csv-siga` · `x-mdd-permiso: [PUBLICACION:APROBACION]`

- **La descripción** suma:
  - «Antes de generar, valida que la versión publicada vigente tenga todas sus firmas sobre el documento de esa versión y ninguna inconsistencia abierta registrada al congelarla; si no, responde 409 REPORTING_VERSION_NO_EXPORTABLE y deja la entrada EXPORTACION_SIGA_RECHAZADA.»
  - «Cada archivo entregado es una Exportación a SIGA: su registro y su entrada CSV_SIGA_GENERADO, con la suma de verificación, se guardan juntos antes de responder (F19-H02, D-120).»
- **200:** suma el encabezado `X-Suma-Verificacion` (`components/headers/SumaVerificacion`):
  - tipo string, `pattern: '^[0-9a-f]{64}$'`;
  - la descripción: «El SHA-256 de los bytes del cuerpo, en hexadecimal en minúsculas. Es el del registro de la exportación.».
- **409:** pasa a una respuesta propia (`components/responses/NoExportable`), con la descripción «Sin distributivo publicado (REPORTING_SIN_PUBLICACION), o la versión publicada no tiene todas sus firmas o tiene inconsistencias abiertas (REPORTING_VERSION_NO_EXPORTABLE).».
- **Los demás** no cambian. El 502 y el 504 cubren ahora también a Firma.

## `consultarExportacionesSiga` (nueva)

`GET /api/reporting/periodos/{periodo}/exportaciones-siga` · `x-mdd-permiso: [PUBLICACION:APROBACION]`

- **El resumen:** «Devuelve las exportaciones a SIGA del periodo, de la más reciente a la más antigua.»
- **200:** `ExportacionesSiga`, con `Cache-Control: no-store`. Un periodo sin exportaciones, o sin publicación, da la lista vacía.
- **400** (el periodo no cumple su patrón), **401**, **403** y **500:** los comunes.

## Los esquemas nuevos

`ExportacionesSiga` (`additionalProperties: false`, `required: [periodo, exportaciones]`):

| Campo | Tipo |
|---|---|
| `periodo` | string |
| `exportaciones` | arreglo de `ExportacionSiga` |

`ExportacionSiga` (`additionalProperties: false`, todos obligatorios):

| Campo | Tipo | Regla |
|---|---|---|
| `id` | string | `format: uuid` |
| `version` | integer | ≥ 1 |
| `layout` | integer | ≥ 0; 0 es el layout por defecto |
| `nombre` | string | `^siga-[A-Za-z0-9._-]{1,20}-v[0-9]+\.csv$` |
| `filas` | integer | ≥ 0 |
| `sumaVerificacion` | string | `^[0-9a-f]{64}$` |
| `exportadaPor` | string | El usuario |
| `exportadaEn` | string | `format: date-time` |

No lleva el hash de la versión: la pantalla no lo muestra, y queda en la base y en el log.

## El catálogo (`contratos/comun/errores.md` y `ErroresReporting`)

| Código | Estado | Clasificación | Lo nuevo |
|---|---|---|---|
| `REPORTING_VERSION_NO_EXPORTABLE` | 409 | Negocio | Código nuevo, con todos los campos de research §8 |
| `COMUN_PROCESO_CON_ERROR` y `COMUN_PROCESO_SIN_RESPUESTA` | 502 y 504 | Sistema | Amplían la causa: Firma, al validar el CSV para SIGA |

## Las pruebas de contrato

`SigaContratoTest`:
- el 200 del CSV con `X-Suma-Verificacion`, que coincide con el SHA-256 del cuerpo;
- los dos 409 con su código;
- `consultarExportacionesSiga`: con datos, vacía, 400, y 403 sin `PUBLICACION:APROBACION`.

`ErroresReportingTest` y la prueba del catálogo suman el código nuevo. Un detalle por condición que falla, con sus números, y nunca hashes ni nombres; la descripción es la del catálogo.
