# Contrato: el reporte general en la API pública de Reporting

F20-H02 (#269). Se amplía `contratos/reporting/openapi.yaml`, antes del código (principio IV). Las cinco operaciones de F20-H01 no cambian.

**Común a las tres operaciones nuevas** (lo mismo que en las de F20-H01, spec 058):
- `x-mdd-permiso: [REPORTES:LECTURA]` y `@PreAuthorize("hasAuthority('PERMISO_REPORTES:LECTURA')")`;
- el parámetro `periodo`: `#/components/parameters/Periodo`;
- **400:** `COMUN_SOLICITUD_INVALIDA`, si el periodo no cumple su patrón;
- **401 y 403:** los de `seguridad.yaml`;
- **409:** `REPORTING_SIN_PUBLICACION` (`#/components/responses/SinPublicacion`);
- **500:** `REPORTING_CONTENIDO_INCONSISTENTE`, por un hash distinto, una versión sin contenido o un docente repetido, o el interno (`#/components/responses/ErrorDelReporte`). No se entrega nada;
- **502 y 504:** los comunes, si `distributivo` falla o no responde.

No hay 404: el reporte general no pide un docente.

## `consultarReporteGeneral`

`GET /api/reporting/periodos/{periodo}/reporte-general` · tag `Reportes`

**200:** `ReporteGeneral` (esquema nuevo). No deja entrada en la bitácora.

| Campo | Tipo | Regla |
|---|---|---|
| `publicacion` | `Publicacion` | La vigente del periodo |
| `docentes` | arreglo de `FilaDelDocente` | Por nombre, en orden alfabético del español (`Collator` es-EC: «ÁLVAREZ» antes que «PERALTA»), y luego por identificación |
| `totales` | `HorasPorComponente` | Los totales institucionales; `total` es el total general |
| `generadoPor` | string | Quién lo pidió |
| `generadoEn` | string, date-time | Cuándo |

`FilaDelDocente` (esquema nuevo, `additionalProperties: false`):

| Campo | Tipo | Regla |
|---|---|---|
| `identificacion` | `NumeroDeIdentificacion` | Obligatoria |
| `nombre` | string | Obligatorio |
| `dedicacion` | `Dedicacion` | Opcional: falta si el docente no estaba activo al congelar |
| `horas` | `HorasPorComponente` | Obligatorias, con su `total` |

El número de docentes es el largo de `docentes`; no se repite en otro campo.

## `descargarReporteGeneralPdf`

`GET /api/reporting/periodos/{periodo}/reporte-general.pdf` · tag `Descargas`

**200:** `application/pdf`, el PDF/A-2b.
- **Encabezados:**
  - `Content-Disposition: attachment; filename="reporte-general-<periodo>.pdf"`;
  - `Cache-Control: no-store`.
- **Bitácora:** cada descarga deja la entrada `REPORTE_GENERAL_DESCARGADO`, registrada antes de responder (D-115, D-117). Si el registro falla, no se entrega nada.

**El documento** (research §3):
- A4 horizontal, sin `AcroForm`, sin anotaciones, sin JavaScript y sin cifrado.
- **Encabezado:** «Reporte general del distributivo», el periodo, «Versión N», el hash, «Publicada el …», «N docentes», y «Generado por … el …».
- **Tabla:** las columnas Identificación, Docente, Dedicación, Clase, Otras actividades de docencia, Investigación, Vinculación, Gestión y Total.
  - El encabezado de las columnas se repite en cada página.
  - La última fila es «Total institucional», una sola vez. **Ajuste al implementar:** se distingue con el fondo gris, como el total del individual, y no en negrita: solo la variante regular de DejaVu Sans está incrustada, y una negrita sin su fuente no sería PDF/A.
- **Pie de cada página:** «Periodo P · Versión N · Hash H · Página X de Y».
- **Horas:** con dos decimales y coma decimal (`12,50`), como el individual.

## `descargarReporteGeneralCsv`

`GET /api/reporting/periodos/{periodo}/reporte-general.csv` · tag `Descargas`

**200:** `text/csv; charset=UTF-8`.
- **Encabezados:** los mismos, con `reporte-general-<periodo>.csv`.
- **Bitácora:** la misma entrada, con `formato: CSV`.

### CSV

El formato de archivo del individual (D-115 §5): UTF-8 con BOM, separador `;`, fin de línea CRLF, comillas dobles en cada celda de texto (las comillas internas se duplican), y horas con dos decimales y coma decimal, sin comillas.

| # | Columna | Fila `DOCENTE` | Fila `TOTAL` |
|---|---|---|---|
| 1 | `Periodo` | el código | igual |
| 2 | `Versión` | el número | igual |
| 3 | `Hash de contenido` | el hash | igual |
| 4 | `Publicada el` | `yyyy-MM-dd HH:mm`, hora de Ecuador, como el individual | igual |
| 5 | `Tipo` | `DOCENTE` | `TOTAL` |
| 6 | `Identificación` | la del docente | vacía |
| 7 | `Docente` | apellidos y nombres | `Total institucional` |
| 8 | `Dedicación` | «Tiempo completo», «Medio tiempo», «Tiempo parcial» o «sin dato» | vacía |
| 9–13 | `Clase`, `Otras actividades de docencia`, `Investigación`, `Vinculación`, `Gestión` | sus horas | los totales institucionales |
| 14 | `Total` | su total | el total general |

- **Las filas:** una por docente, en el orden del PDF, y la fila `TOTAL` al final, una sola vez.
- **Fórmulas (RNF-SEG-04):** toda celda de texto que empiece con `=`, `+`, `-`, `@`, tabulación o retorno de carro lleva un apóstrofo delante. Las horas son números y nunca son negativas.
- **Lo que no lleva:** quién lo generó ni cuándo. Dos CSV de la misma versión son idénticos byte a byte.

## Lo que comprueban las pruebas de contrato

`ReportingContratoTest` y `DescargasContratoTest`:
- cada operación contra el contrato, con el validador de OpenAPI que ya usan;
- los encabezados de las dos descargas;
- 403 sin el permiso;
- 409 sin publicación;
- 500 sin datos técnicos;
- 502;
- 400 con un periodo inválido.

## `contratos/comun/errores.md`

Sin códigos nuevos. Se actualizan:
- `REPORTING_SIN_PUBLICACION`: suma las tres operaciones nuevas;
- `REPORTING_CONTENIDO_INCONSISTENTE`: suma las operaciones nuevas y la causa «un docente repetido en el contenido de la versión».
