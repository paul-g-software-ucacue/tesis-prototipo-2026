# Data Model: Subir un archivo y validar su estructura (F01-H02)

No hay tablas nuevas en `cargas`: el archivo se lee, se valida y se descarta (aclaración 3). La única migración es la de Roles para D-71.

## Roles: `V7__cargas_para_quien_inicia.sql` (D-71, aceptada)

| Rol | Funcionalidad | Tipo | Alcance |
|---|---|---|---|
| `RESPONSABLE_CONSOLIDACION` | `CARGAS` | `LECTURA` | `TODO` |
| `RESPONSABLE_CONSOLIDACION` | `CARGAS` | `ESCRITURA` | `TODO` |
| `VICERRECTOR` | `CARGAS` | `LECTURA` | `TODO` |
| `VICERRECTOR` | `CARGAS` | `ESCRITURA` | `TODO` |

Como `V5__vicerrector_escribe_normativa.sql`: `insert … select` por identificador del rol, con un comentario que cita D-71 y D-63.

## Dominio (`ec.mdd.cargas.dominio`)

| Tipo | Campos | Reglas |
|---|---|---|
| `TablaDeDatos` (record) | `encabezados`, `filas` (listas de textos) | copia las listas; `registros()` = filas sin las vacías del final (una fila vacía tiene todos sus valores en blanco) |
| `MotivoDeDiferencia` (enum) | `FALTA`, `SOBRA`, `REPETIDA`, `FUERA_DE_LUGAR` | — |
| `DiferenciaDeEncabezado` (record) | `columna`, `motivo` | — |
| `Carga.compararEncabezados(List<String>)` | — | ignora los espacios de los extremos; primero las repetidas, luego las que faltan y las que sobran, en el orden de la plantilla y del archivo; si el conjunto coincide, la primera fuera de lugar; vacía si son iguales |
| `Carga.leer(TablaDeDatos)` | — | lanza `EncabezadosDistintos` si hay diferencias y `ArchivoSinRegistros` si `registros()` es 0; devuelve el número de registros |
| `EncabezadosDistintos` | `diferencias` | — |
| `ArchivoSinRegistros` | — | — |

## Puertos

| Puerto | Tipo | Qué hace |
|---|---|---|
| `ValidarArchivo` | entrada | `validar(codigo, ArchivoSubido)`: `LecturaDeArchivo` (carga, nombre, formato, registros); lanza `CargaNoEncontrada`, `ArchivoDemasiadoGrande` y las del lector y del dominio |
| `LectorDeArchivo` | salida | `leer(nombre, contenido)`: `ArchivoLeido(Formato formato, TablaDeDatos tabla)`, con `Formato` `CSV` o `XLSX`; lanza `FormatoNoAdmitido`, `CodificacionNoAdmitida`, `VariasHojas` o `ArchivoIlegible` (con la fila, si la hay) |

`ArchivoSubido` (record del puerto de entrada): nombre (sin ruta, hasta 255) y contenido (bytes). `ArchivoDemasiadoGrande` lo lanza el servicio si el contenido pasa de 5 MB.

## Adaptadores

| Adaptador | Qué hace |
|---|---|
| `LectorPorFormato` (salida) | implementa `LectorDeArchivo`: la extensión y la firma del contenido eligen `LectorCsv` o `LectorXlsx` (research §3) |
| `LectorCsv` | UTF-8 estricto, BOM, separador `,` o `;`, RFC 4180 (research §4) |
| `LectorXlsx` | Apache POI 5.5.1, una hoja, `DataFormatter` (research §5) |
| `SubidaControlador` (entrada) | `validarArchivo`, `CARGAS:ESCRITURA` |
| `ErroresCargas` (entrada) | los 7 códigos `CARGAS_*`, dos con detalles (research §6) |

## Plataforma

- `EntradaCatalogo.conDetalles(…)` y su uso en `ManejadorDeErrores` (research §6).
- `CodigosComunes.CONTENIDO_DEMASIADO_GRANDE` (`COMUN_CONTENIDO_DEMASIADO_GRANDE`, 413) y su caso en `segunEstado`.
- `spring.servlet.multipart.max-file-size: 10MB` y `max-request-size: 10MB` en el proceso `datos`.
- `client_max_body_size 10m;` en `location /api/` de `frontend/nginx.conf`.
