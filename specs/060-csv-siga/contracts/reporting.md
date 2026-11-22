# Contrato: el CSV para SIGA y el layout en la API pública de Reporting

F19-H01 (#266). Se amplía `contratos/reporting/openapi.yaml` antes del código (principio IV), con el tag nuevo `Siga`. Las ocho operaciones de F20-H01 y F20-H02 no cambian.

**Común a las cuatro operaciones nuevas:**
- **El permiso:** `x-mdd-permiso: [PUBLICACION:APROBACION]` y `@PreAuthorize("hasAuthority('PERMISO_PUBLICACION:APROBACION')")` (research §9).
- **401 y 403:** los de `seguridad.yaml`. Rector, Vicerrector, Administrador, coordinación y docente reciben 403.
- **500:** el interno común, sin trazas.
- **`Cache-Control: no-store`** en las respuestas con datos.

## `generarCsvSiga`

`GET /api/reporting/periodos/{periodo}/csv-siga`

- **El parámetro:** `periodo`, `#/components/parameters/Periodo`.
- **200:** `text/csv; charset=UTF-8`, con el esquema `type: string, format: binary`, y `Content-Disposition: attachment; filename="siga-{periodo}-v{version}.csv"`.
  - **La descripción del contrato** resume el formato (research §4): una fila por docente y actividad, las columnas del layout vigente, RFC 4180 con coma, CRLF y sin BOM, `NA`, mayúsculas, horas con punto y dos decimales, y fórmulas neutralizadas.
  - **Antes de responder**, deja la entrada `CSV_SIGA_GENERADO` en la bitácora. Si el registro falla, no se entrega nada.
- **400:** `COMUN_SOLICITUD_INVALIDA`, si el periodo no cumple su patrón.
- **409:** `REPORTING_SIN_PUBLICACION` (`#/components/responses/SinPublicacion`). No se genera nada ni queda entrada.
- **500:** `REPORTING_CONTENIDO_INCONSISTENTE` (`#/components/responses/ErrorDelReporte`). Ocurre si el hash es distinto, si falta el contenido, si hay un docente repetido o si los totales no cuadran con el reporte general. No se entrega nada.
- **502 y 504:** los comunes, si `distributivo` falla o no responde.

## `consultarLayoutsSiga`

`GET /api/reporting/layout-siga`

**200:** `LayoutsSiga`, un esquema nuevo con `additionalProperties: false`:

| Campo | Tipo | Regla |
|---|---|---|
| `vigente` | `LayoutSiga` | El último cargado o, si no hay, el por defecto |
| `anteriores` | arreglo de `LayoutSiga` | Los demás cargados, del más reciente al más antiguo; vacío si no hay. El por defecto no se repite aquí |

`LayoutSiga`, también nuevo:

| Campo | Tipo | Regla |
|---|---|---|
| `numero` | integer, `minimum: 0` | 0 si es el por defecto |
| `porDefecto` | boolean | |
| `cargadoPor` | string | Ausente en el por defecto |
| `cargadoEn` | string, date-time | Ausente en el por defecto |
| `sha256` | string, `^[0-9a-f]{64}$` | |
| `columnas` | arreglo de `ColumnaDelLayoutSiga`, `minItems: 1`, `maxItems: 30` | En orden |

`ColumnaDelLayoutSiga`:

| Campo | Tipo | Regla |
|---|---|---|
| `nombre` | string | `^[A-Za-z][A-Za-z0-9_]{0,63}$` |
| `campo` | `CampoDelLayoutSiga` | Un `enum` con las 15 claves de research §6 |
| `traduccion` | arreglo de `{codigo, valor}` | Opcional, en el orden del catálogo |

## `descargarPlantillaLayoutSiga`

`GET /api/reporting/layout-siga/plantilla.yaml`

**200:** `text/yaml; charset=UTF-8`, con `Content-Disposition: attachment; filename="layout-siga-plantilla.yaml"`. Es el recurso `layout-siga-por-defecto.yaml`, comentado en español. Lo que se descarga es el layout por defecto, aunque haya otro cargado.

## `cargarLayoutSiga`

`PUT /api/reporting/layout-siga`, con `multipart/form-data` y el campo `archivo` (`format: binary`), obligatorio.

- **200:** el `LayoutSiga` nuevo, ya vigente, con el número siguiente. En la misma transacción que la fila, deja la entrada `LAYOUT_SIGA_CARGADO` en la bitácora.
- **400:**
  - `REPORTING_LAYOUT_INVALIDO`, con un `detalle` por problema, hasta 50. `campo` es `columnas[N]` (con el nombre, si lo tiene) o `archivo`, y `descripcion` dice la regla y qué hacer.
  - `REPORTING_ARCHIVO_ILEGIBLE`, si no es UTF-8, no es YAML o tiene anclas, alias o etiquetas. Si se conoce, dice «Revisa la línea N.».
  - `COMUN_SOLICITUD_INVALIDA`, si falta `archivo`.
- **413:** `REPORTING_ARCHIVO_DEMASIADO_GRANDE`, si pasa de 256 KB.

Un archivo rechazado no deja fila ni entrada en la bitácora, y el layout vigente no cambia.

## El catálogo (`contratos/comun/errores.md` y `ErroresReporting`)

**Los códigos nuevos:**

| Código | Estado | Título | Mensaje | Causa | Clasificación | Acción | Descripción técnica |
|---|---|---|---|---|---|---|---|
| `REPORTING_LAYOUT_INVALIDO` | 400 | Archivo de mapeo con problemas | Corrige lo que se indica en cada columna y vuelve a cargar el archivo. El layout vigente no cambió. | Cargar un archivo de mapeo que no cumple las reglas de FR-015 (F19-H01) | Validación | Corregir el archivo, partiendo de la plantilla | `LayoutInvalido`, de `GestionarLayoutSigaServicio`; lo traduce `ErroresReporting`, con un detalle por `ProblemaDelLayout` |
| `REPORTING_ARCHIVO_ILEGIBLE` | 400 | Archivo no válido | No se pudo leer el archivo de mapeo. Parte de la plantilla y vuelve a cargarlo. | El archivo no es UTF-8 ni YAML, o tiene anclas, alias o etiquetas (F19-H01) | Validación | Partir de la plantilla | `ArchivoIlegible`, de `LectorDeLayoutYaml`. El mensaje del lector solo va al log |
| `REPORTING_ARCHIVO_DEMASIADO_GRANDE` | 413 | Archivo demasiado grande | El archivo de mapeo pasa de 256 KB. Parte de la plantilla y vuelve a cargarlo. | Cargar un archivo de más de 256 KB (F19-H01) | Validación | Partir de la plantilla | Lo comprueba el controlador antes de leerlo |

**Los códigos que se amplían:**
- **`REPORTING_SIN_PUBLICACION`:** la causa suma «generar el CSV para SIGA».
- **`REPORTING_CONTENIDO_INCONSISTENTE`:** la causa suma «o, en el CSV para SIGA, la suma de las filas de un docente en un componente, o la de todas, no es la del reporte general de la misma versión». La descripción técnica suma `FilasParaSiga.de`, con el docente y el componente en el log, nunca en la respuesta.

## Los esquemas nuevos

`LayoutsSiga`, `LayoutSiga`, `ColumnaDelLayoutSiga` y `CampoDelLayoutSiga`.

## Las pruebas de contrato

`SigaContratoTest`, con swagger-request-validator, como `DescargasContratoTest`:
- **Las cuatro operaciones** con sus respuestas de éxito.
- **Los errores:** 409, 500 (sin trazas ni hashes en el cuerpo), 400 (por cada uno de sus códigos) y 413.
- **Los permisos:** 403 con `REPORTES:LECTURA` sin `PUBLICACION:APROBACION`.
- **Las cabeceras:** `Content-Type`, `Content-Disposition` y `Cache-Control`.
