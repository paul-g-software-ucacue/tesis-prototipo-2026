# Contrato: Firma

En `contratos/firma/openapi.yaml`, etiqueta nueva `FirmasPendientes`. El API Gateway ya enruta `/api/firma/**` a `distributivo`.

## `GET /api/firma/pendientes`

- `operationId: listarFirmasPendientes`.
- `x-mdd-permiso: DISTRIBUTIVO:FIRMA`. Lo tienen RESPONSABLE_CONSOLIDACION (también el docente designado), VICERRECTOR y RECTOR.

| Respuesta | Cuándo |
|---|---|
| 200 `FirmaPendiente[]` | Los documentos generados cuyo paso en turno es del usuario; `[]` sin ninguno |
| 401, 403 | Los comunes |
| 500, 502, 504 | Los comunes; 502 o 504 si Usuarios no responde al pedir la identificación |

`FirmaPendiente`:
- `documento` (uuid);
- `periodo`;
- `version` (entero ≥ 1);
- `orden` (entero ≥ 1);
- `rotulo`: «Elaborado por», «Revisado por» o «Aprobado por»;
- `generadoEn` (date-time).

El orden de la lista es por periodo y versión.

## `GET /api/firma/documentos/{documento}/archivo`

- `operationId: descargarArchivoVigente`.
- `x-mdd-permiso: DISTRIBUTIVO:FIRMA`. Además, en el dominio, el usuario debe ser el firmante esperado del paso en turno.

| Respuesta | Cuándo |
|---|---|
| 200 `application/pdf` | El firmante en turno. Lleva `Content-Disposition: attachment; filename="distributivo-<periodo>-v<version>.pdf"` y `ETag: "<sha256>"` |
| 400 | `documento` no es un uuid |
| 401, 403 | Los comunes. 403 también si no es su turno, si el documento no existe o si no está generado, con el mismo cuerpo |
| 500 | `FIRMA_ARCHIVO_NO_DISPONIBLE`: el archivo falta o su SHA-256 no es el registrado; no se entrega nada |
| 502, 504 | Los comunes |

## Catálogo (`contratos/comun/errores.md`)

| Código | HTTP | Título | Descripción |
|---|---|---|---|
| `FIRMA_ARCHIVO_NO_DISPONIBLE` | 500 | Archivo no disponible | El archivo del documento firmable no está disponible. Avisa al administrador con el identificador del incidente. |

## Bitácora

Sin entradas nuevas: leer y descargar no cambian nada (F17-H01).
