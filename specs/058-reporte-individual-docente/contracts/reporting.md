# Contrato: la API pública de Reporting

F20-H01 (#268). Un contrato nuevo, `contratos/reporting/openapi.yaml` (OpenAPI 3.0.3), escrito antes del código (principio IV). El API Gateway ya enruta `/api/reporting/**` al proceso `salida`.

**Común a todas las rutas:**
- `x-mdd-permiso: REPORTES:LECTURA`: el Rector, el Vicerrector académico, la Secretaría general académica, el Miembro del OCS y el Administrador.
- **Errores comunes:**
  - 400 `COMUN_SOLICITUD_INVALIDA`, por una ruta con un formato inválido;
  - 401 y 403, los de `seguridad.yaml`;
  - 502 `COMUN_PROCESO_CON_ERROR` y 504 `COMUN_PROCESO_SIN_RESPUESTA`, si Aprobación o Generación no responden;
  - 500, el común.
- **Parámetros de ruta:**
  - `periodo`: el esquema `CodigoDePeriodo` de Académico (por referencia).
  - `identificacion`: `NumeroDeIdentificacion` de Docentes (`^[0-9A-Z]{1,13}$`, cédula o pasaporte), por referencia.

## `listarPublicaciones`

`GET /api/reporting/publicaciones`

**200:** un arreglo de `Publicacion`, de la más reciente a la más antigua por `publicadaEn`; vacío si no hay ninguna.

| Campo | Tipo | Regla |
|---|---|---|
| `periodo` | string | El código del periodo |
| `version` | integer, ≥ 1 | La versión publicada vigente |
| `hash` | string, 64 hex | Su hash de contenido |
| `publicadaEn` | string, date-time | La transición a PUBLICADO |
| `publicadaPor` | string | Quién publicó |
| `enReforma` | boolean | Si hay una reforma en curso; la versión sigue siendo la vigente |

## `listarDocentesDelPeriodo`

`GET /api/reporting/periodos/{periodo}/docentes`

**200:** `DocentesDelPeriodo`:
- `publicacion`: una `Publicacion`;
- `docentes`: un arreglo de `{identificacion, nombre}` por nombre y, a igual nombre, por identificación.

**409:** `REPORTING_SIN_PUBLICACION`.

## `consultarReporteIndividual`

`GET /api/reporting/periodos/{periodo}/docentes/{identificacion}/reporte`

**200:** `ReporteIndividual`. No deja entrada en la bitácora (FR-012).

| Campo | Tipo | Regla |
|---|---|---|
| `publicacion` | `Publicacion` | |
| `docente` | objeto | `identificacion`, `nombre`, `dedicacion` (`TC`, `MT`, `TP` o ausente), `horasContratadas` (opcional) |
| `asignaturas` | arreglo | `carrera`, `codigo`, `nombre`, `paralelo`, `horas` |
| `actividades` | arreglo | `componente`, `codigo`, `nombre`, `unidad` (opcional), `horas` |
| `horas` | `HorasPorComponente` | `clase`, `otrasDocencia`, `investigacion`, `vinculacion`, `gestion` y `total` |
| `generadoPor` | string | El usuario del token |
| `generadoEn` | string, date-time | |

Las horas son `number` con hasta dos decimales, ≥ 0 (el esquema `Horas` que ya usa Generación).

**Errores:**
- **404:** `REPORTING_DOCENTE_SIN_DISTRIBUTIVO`.
- **409:** `REPORTING_SIN_PUBLICACION`.
- **500:** `REPORTING_CONTENIDO_INCONSISTENTE`.

## `descargarReporteIndividualPdf` y `descargarReporteIndividualCsv`

`GET /api/reporting/periodos/{periodo}/docentes/{identificacion}/reporte.pdf` y `GET …/reporte.csv`

**200:**
- **Cuerpo:** `application/pdf` o `text/csv; charset=UTF-8`, `format: binary`.
- **`Content-Disposition`:** `attachment; filename="reporte-individual-<periodo>-<identificacion>.pdf"` (o `.csv`). El periodo y la identificación ya cumplen sus patrones, así que el nombre no tiene espacios ni caracteres peligrosos.
- **`Cache-Control: no-store`:** lleva datos personales.
- Cada respuesta 200 deja su entrada `REPORTE_INDIVIDUAL_DESCARGADO` en la bitácora, registrada antes de responder ([data-model.md](../data-model.md)).

**Errores:** los mismos de `consultarReporteIndividual`. Si el registro de la descarga falla, es el 500 común, sin archivo.

## Los errores nuevos del catálogo

En `contratos/comun/errores.md`, con sus ocho columnas:

| Código | Estado | Título | Descripción (lo que ve el usuario) | Clasificación |
|---|---|---|---|---|
| `REPORTING_SIN_PUBLICACION` | 409 | Sin distributivo publicado | El periodo todavía no tiene un distributivo publicado. Los reportes salen de la versión publicada. | Negocio |
| `REPORTING_DOCENTE_SIN_DISTRIBUTIVO` | 404 | Docente sin distributivo | El docente no tiene asignaciones en la versión publicada de este periodo. | Negocio |
| `REPORTING_CONTENIDO_INCONSISTENTE` | 500 | Reporte no disponible | No se pudo generar el reporte. Avisa al administrador con el identificador del incidente. | Sistema |

**Descripción técnica del tercero:** el hash de la publicación de Aprobación no es el del contenido congelado de Generación (research §3). Lo detecta `ReporteIndividual.de` y lo traduce `ErroresReporting`. No se entrega nada.

## Pruebas

**`ReportingContratoTest`**, con los puertos de entrada simulados, prueba para cada operación:
- el 200 contra el esquema;
- el 403 sin `REPORTES:LECTURA`;
- cada error con su código.

Además:
- los encabezados de las descargas;
- que una respuesta 500 no tiene trazas (principio VIII).
