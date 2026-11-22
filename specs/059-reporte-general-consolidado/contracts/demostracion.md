# Contrato: la demostración en `deploy/probar_documento_firmable.sh`

F20-H02 (#269). El script ya tiene los pasos R1 a R6 del reporte individual (spec 058). Esta historia:
- suma dos pasos, **R7** y **R8**, después de R3, con la versión 1 publicada;
- amplía **R1**, **R4**, **R5** y **R6** con el reporte general.

Los números de los demás pasos no cambian.

**Las funciones que se generalizan:**
- **`bajar`:** recibe la ruta del reporte, en lugar de la identificación. El individual usa `ruta_del_reporte <identificación>` y el general, la nueva `ruta_del_general`.
- **`csv_igual_al_json`:** recibe el tipo de reporte. Para el general compara, en Python, cada fila `DOCENTE` y la fila `TOTAL` con el JSON, además del BOM, CRLF, la versión y el hash.
- **`pdf_sin_campos`:** sin cambios.

## R1 · Sin publicación (amplía)

| Qué suma | Pasa si |
|---|---|
| `rectorado.demo` pide el reporte general de `2026-I` en EN_OCS | 409 `REPORTING_SIN_PUBLICACION`; la bitácora no tiene `REPORTE_GENERAL_DESCARGADO` |

## Con la versión 1 publicada (después de R3)

| Paso | Qué hace | Pasa si |
|---|---|---|
| R7. El reporte general (US1) | `rectorado.demo` pide el reporte general de 2026-I, los docentes del periodo y el reporte individual de cada uno | Versión 1 y el hash de la publicación. **Los docentes:** los mismos del listado, sin repetidos (SC-001). **Ajuste al implementar:** se comparan como conjuntos, porque el general ordena en orden alfabético del español (D-117) y el listado sale con el orden de la base. **Cada fila:** igual a las horas por componente y al total de su reporte individual (SC-002). **Los totales:** la suma de las filas en cada componente, y el total general es la suma de los cinco (SC-003). **La bitácora:** pedir el JSON no dejó entrada |
| R8. Los dos formatos (US2) | `rectorado.demo` y `secretaria.demo` descargan el PDF y el CSV del general | **El PDF:** PDF/A sin formulario, con `Content-Disposition` `reporte-general-2026-I.pdf` y `Cache-Control: no-store`. **El CSV:** las mismas filas y los mismos totales que el JSON (SC-004), con el BOM y `;`; el de un usuario es idéntico byte a byte al del otro. **La bitácora:** 4 entradas `REPORTE_GENERAL_DESCARGADO`, 2 con `CSV`, con el objeto `{periodo, version}` sin identificación |

## R4 · Acceso (amplía)

| Qué suma | Pasa si |
|---|---|
| `coordinacion.demo` y `docente.demo` piden el reporte general y su PDF; `rectorado.demo` pide el de un periodo con un formato inválido | 403 los cuatro pedidos; 400 `COMUN_SOLICITUD_INVALIDA` (SC-008) |

## R5 · Durante la reforma (amplía)

| Qué suma | Pasa si |
|---|---|
| Con la hora del paralelo ya bajada, `rectorado.demo` vuelve a descargar el CSV del general | Es idéntico al de R8: la versión 1 no cambia |

## R6 · La versión nueva (amplía)

| Qué suma | Pasa si |
|---|---|
| `rectorado.demo` pide el reporte general | Versión 2 y su hash; los totales son la suma de sus filas |

## La caída de un proceso

Como en F20-H01, se prueba en las pruebas de integración de Reporting, no en el script.

## Workflow

Sin cambios: el script ya corre en el job `perfil-consolidado`, con veraPDF en Docker.
