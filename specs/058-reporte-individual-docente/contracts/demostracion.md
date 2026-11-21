# Contrato: la demostración en `deploy/probar_documento_firmable.sh`

F20-H01 (#268). El script ya recorre:
- la publicación de la versión 1 (F18-H01);
- la reforma, que crea la versión 2 (F18-H02);
- la publicación de la versión 2.

Esta historia le suma seis pasos de reportes, marcados R1 a R6, en esos tres momentos. Los números de los pasos que ya existen no cambian.

Los pasos usan funciones nuevas:
1. `bajar` descarga el PDF o el CSV de un docente, con sus cabeceras;
2. `pdf_sin_campos` pasa el PDF por veraPDF (la misma imagen del paso 4): conforme con PDF/A-2b y sin campos de formulario;
3. `csv_igual_al_json` compara el CSV con el JSON en Python: el BOM, CRLF, las filas, las horas, los totales, la versión y el hash.

**Ajuste al implementar:** el plan decía `pdfinfo` y `qpdf`. veraPDF ya está en el script y verifica las dos cosas, así que no se instala ninguna herramienta nueva.

## Antes de publicar (después del paso 12, en EN_OCS)

| Paso | Qué hace | Pasa si |
|---|---|---|
| R1. Sin publicación | `rectorado.demo` pide las publicaciones y el reporte de `2026-I` | Las publicaciones son `[]`; el reporte responde 409 `REPORTING_SIN_PUBLICACION`; la bitácora no tiene `REPORTE_INDIVIDUAL_DESCARGADO` |

## Con la versión 1 publicada (después del paso de la publicación)

| Paso | Qué hace | Pasa si |
|---|---|---|
| R2. El reporte (US1) | `rectorado.demo` pide los docentes de 2026-I y sus reportes hasta encontrar uno con asignaturas | Versión 1 y su hash, el de la publicación. Las filas suman su total, y las horas por componente son las de la carga horaria de Generación para esa versión. El contenido de la versión 1 se guardó al congelar (`tardio = false`) |
| R3. Los dos formatos (US2) | `rectorado.demo` y `secretaria.demo` descargan el PDF y el CSV, dos veces cada uno | El PDF es PDF/A sin formulario, con su `Content-Disposition` y `Cache-Control: no-store`. El CSV tiene las mismas filas y totales que el JSON, empieza con el BOM y usa `;`. El CSV de uno es idéntico byte a byte al del otro. Hay 8 entradas `REPORTE_INDIVIDUAL_DESCARGADO`, 4 con `CSV`; pedir el JSON en R2 no dejó ninguna |
| R4. Acceso y errores | `coordinacion.demo` y `docente.demo` piden el reporte; `rectorado.demo` pide un docente que no está y una identificación inválida | 403 los dos primeros; 404 `REPORTING_DOCENTE_SIN_DISTRIBUTIVO`; 400 `COMUN_SOLICITUD_INVALIDA` |

## Durante la reforma (después de iniciarla, antes de publicar la versión 2)

| Paso | Qué hace | Pasa si |
|---|---|---|
| R5. La versión 1 no cambia | `vicerrectorado.demo` (que tiene `DISTRIBUTIVO:ESCRITURA`) baja una hora a un paralelo del docente elegido en R2; `rectorado.demo` vuelve a descargar su CSV; después se restauran las horas | Las publicaciones dicen versión 1 con `enReforma: true`. El CSV es idéntico al de R3: las horas corregidas no aparecen (research §2) |

## Con la versión 2 publicada

| Paso | Qué hace | Pasa si |
|---|---|---|
| R6. La versión nueva | `rectorado.demo` pide el reporte del mismo docente | Versión 2 y su hash, sin reforma en curso, y el contenido de la versión 2 guardado al congelar |

**Ajuste al implementar:** R5 restaura las horas del paralelo, porque los pasos 25 a 27 recorren la versión 2 con las reglas del panel; un paralelo con menos horas podía dejar una inconsistencia dura. Por eso R6 ya no compara las horas corregidas.

## La caída de un proceso

Se prueba en las pruebas de integración de Reporting, no en el script: el script ya detiene `identidad` en otros pasos y cada reinicio alarga el job.
- **`distributivo` detenido:** el reporte responde 502 o 504, sin archivo.
- **Registro persistente lleno con `identidad` caído:** la descarga responde 200, y la entrada llega a la bitácora una sola vez cuando `identidad` vuelve.

## `MDD_CONSERVAR`

Sin cambios. `MDD_CONSERVAR=PUBLICADO` deja la versión 1 publicada, lista para el quickstart.

## Workflow

El script ya corre en el job `perfil-consolidado`. No necesita herramientas nuevas: veraPDF ya corre en Docker en el paso 4.
