# Contrato: la demostración en `deploy/probar_documento_firmable.sh`

F19-H01 (#266). El script ya tiene los pasos R1 a R8 de los reportes (specs 058 y 059). Esta historia:
- suma **R9** y **R10**, después de R8, con la versión 1 publicada;
- amplía **R1**, **R4** y **R5**.

Los números de los demás pasos no cambian.

**Al empezar,** el script deja el layout vigente en un estado conocido: carga la plantilla por defecto como un layout más. Así, R9 no depende de lo que cargó una corrida anterior. La tabla no admite `delete` (principio VII), así que no se limpia: se agrega encima.

**Las funciones nuevas:**
- **`csv_siga_igual_al_general`:** en Python, con el módulo `csv` (RFC 4180), compara un CSV para SIGA con el JSON del reporte general. Suma las horas por docente y componente, y todas por componente, con `Decimal`.
- **`cargar_layout`:** sube un YAML con `curl --form` y devuelve el estado y el cuerpo.

## R1 · Sin publicación (amplía)

| Qué suma | Pasa si |
|---|---|
| `secretaria.demo` pide el CSV para SIGA de `2026-I` en EN_OCS | 409 `REPORTING_SIN_PUBLICACION`; la bitácora no tiene `CSV_SIGA_GENERADO` |

## Con la versión 1 publicada (después de R8)

| Paso | Qué hace | Pasa si |
|---|---|---|
| R9. El CSV con el layout por defecto (US1 y US2) | `secretaria.demo` genera el CSV para SIGA de 2026-I dos veces y pide el reporte general | **El archivo:** `Content-Disposition` `siga-2026-I-v1.csv`, `Cache-Control: no-store`, sin BOM y con CRLF. **Las filas:** una por cada asignatura y paralelo y por cada actividad de los reportes individuales (SC-001). **Los totales:** por docente y componente, y por componente, son los del general (SC-002). **Las convenciones** (SC-003): cédulas `^[0-9]{10}$`, mayúsculas, `NA` en la asignatura de una actividad, horas `^[0-9]+\.[0-9]{2}$` y fecha `^[0-9]{4}-[0-9]{2}-[0-9]{2}$`. **Los dos archivos** son idénticos byte a byte (SC-006). **La bitácora:** 2 entradas `CSV_SIGA_GENERADO` con `{periodo: 2026-I, version: 1}` y el número de la versión del layout de exportación vigente que da `consultarLayoutsSiga`. **El encabezado:** el del layout vigente; con la carga inicial de la plantilla (R10, T033), son las 13 columnas de FR-004 en su orden |
| R10. Otro layout (US3) | `secretaria.demo` descarga la plantilla, le cambia dos nombres, invierte dos columnas, quita `fecha_publicacion` y traduce `componente` a `01`…`05`, y la carga. Después carga uno con un campo inexistente y dos nombres repetidos, y genera el CSV | **La plantilla** es igual al recurso del repositorio. **El válido:** 200, con el número siguiente, y deja `LAYOUT_SIGA_CARGADO`. **El inválido:** 400 `REPORTING_LAYOUT_INVALIDO` con 2 detalles, y el vigente sigue siendo el anterior (SC-005). **El CSV** sale con las columnas nuevas, en su orden, sin la fecha y con `01`…`05`; sus totales siguen siendo los del general (SC-004). Al terminar, el script vuelve a cargar la plantilla |

## R4 · Acceso (amplía)

| Qué suma | Pasa si |
|---|---|
| `rectorado.demo`, `coordinacion.demo` y `admin.demo` piden el CSV para SIGA y cargan un layout válido | 403 los seis pedidos, sin entrada en la bitácora ni layout nuevo (SC-009) |

## R5 · Durante la reforma (amplía)

| Qué suma | Pasa si |
|---|---|
| Con la reforma en curso y la hora del paralelo ya bajada, `secretaria.demo` vuelve a generar el CSV para SIGA | Es idéntico al de R9: sale de la versión 1 publicada (escenario 4 de la US1) |

## La línea `verificar`

```bash
verificar R9 "El CSV para SIGA de la versión 1: una fila por asignatura y actividad, los totales del general, las convenciones de SENESCYT, dos archivos idénticos y 2 entradas CSV_SIGA_GENERADO" csv_siga_por_defecto
verificar R10 "Un layout nuevo cambia las columnas sin tocar el código; uno inválido se rechaza con 2 problemas y no cambia el vigente" layout_siga_nuevo
```
