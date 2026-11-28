# Contrato: la demostración en `deploy/probar_documento_firmable.sh`

F19-H02 (#267). El script ya tiene R1 a R10. Esta historia:
- amplía **R1**, **R4**, **R5** y **R9**;
- suma **R11**, después de R10, con la versión 1 publicada.

Los demás pasos no cambian. Por el flujo normal no se puede publicar una versión sin firmas ni con inconsistencias abiertas, así que el script no provoca ese rechazo; lo cubren las pruebas (research §10 y §11).

**Ajuste al implementar (2026-11-28):** `reporting.exportacion_siga` no admite borrar, así que las exportaciones de corridas anteriores se conservan. El script las cuenta desde que empezó la corrida (`exportada_en >= $inicio`), como la bitácora, y la API solo se usa para revisar el contenido de las más recientes. «La lista vacía» de R1 y «2 exportaciones» de R9 se leen así.

**Las funciones nuevas:**
- **`exportaciones_de_la_corrida`:** cuenta en la base las exportaciones del periodo desde `$inicio`.
- **`exportaciones_siga`:** pide `consultarExportacionesSiga` de un periodo y devuelve el JSON.

## R1 · Sin publicación (amplía)

| Qué suma | Pasa si |
|---|---|
| Después del 409 del CSV para SIGA en EN_OCS | No hay `EXPORTACION_SIGA_RECHAZADA` en la bitácora, y `exportaciones_siga 2026-I` da la lista vacía |

## R4 · Acceso (amplía)

| Qué suma | Pasa si |
|---|---|
| `rectorado.demo`, `coordinacion.demo` y `admin.demo` piden las exportaciones de 2026-I | 403 los tres (SC-008) |

## R9 · El CSV con el layout por defecto (amplía)

| Qué suma | Pasa si |
|---|---|
| Con los dos CSV que ya pide R9 | **La suma:** el encabezado `X-Suma-Verificacion` de cada uno es el `sha256sum` de su archivo, y es el mismo en los dos (SC-002, SC-005). **El registro:** `exportaciones_siga 2026-I` tiene 2 exportaciones nuevas, con la versión 1, el layout vigente, `siga-2026-I-v1.csv`, las filas del archivo y esa suma. **La bitácora:** las 2 entradas `CSV_SIGA_GENERADO` llevan en `nuevo` la suma y el `id` de su exportación (SC-003). No hay `EXPORTACION_SIGA_RECHAZADA` |

## R5 · Durante la reforma (amplía)

| Qué suma | Pasa si |
|---|---|
| Con el CSV que ya pide R5 | Su `X-Suma-Verificacion` es la de R9, y hay una exportación más de la versión 1 |

## R11 · El registro no cambia (nuevo, después de R10)

| Qué hace | Pasa si |
|---|---|
| Con `psql` en el contenedor de la base (la función `consultar_base`, como superusuario), intenta un `update` y un `delete` sobre `reporting.exportacion_siga` | Los dos fallan con `insufficient_privilege` (el disparador frena también al superusuario), y el número de filas no cambia (SC-006) |

La ruta interna de Firma no se pide desde el script, que no tiene token de proceso: que R9 exporte ya prueba, de punta a punta, que Reporting la lee y que la cadena de la versión 1 está completa.

## Las líneas `verificar`

```bash
verificar R9 "El CSV para SIGA de la versión 1: ... dos archivos idénticos con la misma suma de verificación, 2 exportaciones registradas y 2 entradas CSV_SIGA_GENERADO con la suma" csv_siga_por_defecto
verificar R11 "Las exportaciones a SIGA no se modifican ni se borran" exportacion_siga_inmutable
```
