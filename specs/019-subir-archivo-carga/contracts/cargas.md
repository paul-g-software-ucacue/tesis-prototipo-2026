# Contrato: Cargas · subir un archivo (F01-H02)

Cambios en `contratos/cargas/openapi.yaml` y en el catálogo común, antes del código (principio IV). El porqué está en [research.md](../research.md) §3, §6 y §7.

## Esquemas nuevos

| Esquema | Campos |
|---|---|
| `FormatoDeArchivo` | enum `CSV`, `XLSX` |
| `LecturaDeArchivo` | `carga` (código), `archivo` (nombre, hasta 255), `formato`, `registros` (entero ≥ 1) |

## Operación nueva (tag `Subida`)

| Operación | Ruta | `x-mdd-permiso` | Cuerpo | Respuestas |
|---|---|---|---|---|
| `validarArchivo` | `POST /api/cargas/plantillas/{codigo}/archivo` | `CARGAS:ESCRITURA` | `multipart/form-data`, parte `archivo` (`type: string, format: binary`) | 200 `LecturaDeArchivo` · 400 · 404 · 413 |

- 400: `CARGAS_FORMATO_NO_ADMITIDO`, `CARGAS_CODIFICACION_NO_ADMITIDA`, `CARGAS_VARIAS_HOJAS`, `CARGAS_ARCHIVO_ILEGIBLE` (con detalle), `CARGAS_ENCABEZADOS_DISTINTOS` (con detalles), `CARGAS_ARCHIVO_SIN_REGISTROS`; o `COMUN_SOLICITUD_INVALIDA` sin la parte `archivo` o con un código que no cumple el patrón.
- 404: `COMUN_RECURSO_NO_ENCONTRADO`, para una carga que no existe.
- 413: `CARGAS_ARCHIVO_DEMASIADO_GRANDE` (más de 5 MB) o `COMUN_CONTENIDO_DEMASIADO_GRANDE` (más de 10 MB, lo corta el proceso).
- También 401, 403, 500 y 503 comunes.

## Errores nuevos (`contratos/comun/errores.md`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `CARGAS_FORMATO_NO_ADMITIDO` | 400 | Formato no admitido | Solo se admiten archivos CSV y XLSX. Descarga la plantilla de la carga y guárdala en uno de esos formatos. |
| `CARGAS_CODIFICACION_NO_ADMITIDA` | 400 | Codificación no admitida | El CSV tiene que estar en UTF-8. Guárdalo como «CSV UTF-8» y vuelve a subirlo. |
| `CARGAS_VARIAS_HOJAS` | 400 | Varias hojas | El archivo XLSX tiene que tener una sola hoja, con los encabezados en la primera fila. |
| `CARGAS_ARCHIVO_ILEGIBLE` | 400 | Archivo ilegible | El archivo no se puede leer. Revisa el detalle, corrígelo y vuelve a subirlo. |
| `CARGAS_ENCABEZADOS_DISTINTOS` | 400 | Encabezados distintos de la plantilla | Las columnas del archivo no son las de la plantilla de la carga, en su orden. Revisa las columnas marcadas. |
| `CARGAS_ARCHIVO_SIN_REGISTROS` | 400 | Archivo sin registros | El archivo no tiene registros debajo de los encabezados. |
| `CARGAS_ARCHIVO_DEMASIADO_GRANDE` | 413 | Archivo demasiado grande | El archivo pasa de 5 MB, el tamaño máximo de una carga. |
| `COMUN_CONTENIDO_DEMASIADO_GRANDE` | 413 | Contenido demasiado grande | El contenido enviado supera el tamaño máximo que admite el sistema. |

Los detalles de `CARGAS_ENCABEZADOS_DISTINTOS` llevan `campo` = la columna y `descripcion` = «Falta en el archivo.», «No está en la plantilla.», «Está repetida.» o «Está fuera de lugar: va en la posición N.». El de `CARGAS_ARCHIVO_ILEGIBLE` lleva `campo` = `archivo` y la fila en la descripción. Nunca llevan valores del archivo.

## SPA

- `apiCargas.POST('/api/cargas/plantillas/{codigo}/archivo')` con `FormData`.
- `traducirError` ya muestra el título, la descripción y los detalles.
