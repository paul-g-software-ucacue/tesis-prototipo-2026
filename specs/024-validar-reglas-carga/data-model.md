# Data Model: Validar las reglas de los datos e informe por fila

No hay tablas nuevas: el archivo, el informe y el resultado no se guardan (FR-008). Todo vive en memoria durante la validación de un archivo.

## Dominio de Cargas

| Tipo | Qué es | Reglas |
|---|---|---|
| `ReglasDeColumna` | Las reglas de una columna de una carga, interpretadas una vez del diccionario (research §3). | El tipo, el largo, la enumeración o el patrón, el rango, las partes de una lista y la obligatoriedad. Un `formato` no reconocido no da regla. |
| `CondicionDeObligatoriedad` | «Sí», «No» o una de las 5 condiciones (research §4). | `exige(fila)` dice si la celda puede quedar vacía en esa fila. Un texto desconocido hace fallar la carga del diccionario. |
| `ClaveDeCarga` | Las columnas que forman la clave de una carga (research §7). | Una o varias; la segunda fila con la misma clave es un error. |
| `ReferenciaDeColumna` | El destino de una `FK`: unidades, carreras, periodos, asignaturas o docentes, y si también vale el mismo archivo (research §6). | Una `FK` sin destino conocido hace fallar la carga del diccionario. |
| `ReferenciasDelSistema` (valor) | Los códigos que existen al validar: unidades, carreras, periodos, asignaturas e identificaciones de docentes. | Se piden una vez por archivo. |
| `ProblemaDeFila` | `fila` (el encabezado es la 1), `campo`, `valor`, `motivo`, `tipo` (`ERROR` o `ADVERTENCIA`) y `sugerencia` opcional. | El motivo dice qué hacer y no repite el valor. |
| `InformeValidacionCarga` | El resultado de un archivo: `registros`, los problemas en orden de fila y de columna, y los totales. | `valido()` si no tiene errores. `primeros(100)` para la respuesta JSON. |
| `ValidadorDeFilas` | Recorre la tabla leída y devuelve el informe. | Aplica a cada celda las reglas de su columna, la cédula, la clave y las referencias. |

## Puertos

| Puerto | Servicio | Qué ofrece |
|---|---|---|
| `ValidarArchivo` (entrada, amplía el de F01-H02) | Cargas | `validar(codigo, archivo)` devuelve `ArchivoValidado`, que ahora trae el informe. |
| `ReferenciasDelSistema` (salida, nuevo) | Cargas | Los códigos que existen en Académico y en Docentes. |
| `ConsultarIdentificaciones` (entrada, nuevo) | Docentes | `Set<String> identificaciones()`: solo las identificaciones registradas (research §6). |
| `ConsultarReferencias` (entrada, ya existe) | Académico | Las carreras, las asignaturas, las unidades y los periodos (F03-H04, F03-H05). |
