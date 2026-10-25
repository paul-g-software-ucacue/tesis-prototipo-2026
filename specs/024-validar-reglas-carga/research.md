# Research: Validar las reglas de los datos e informe por fila

Fase 0 del plan de F01-H03 (issue #29). Cada punto dice la decisión, por qué y qué se descartó.

## 1. El informe es una respuesta correcta, no un error

- **Decisión:** `validarArchivo` responde **200** también cuando las filas tienen errores. La respuesta `LecturaDeArchivo` agrega:
  - `valido`;
  - `errores` y `advertencias`, el total de cada uno;
  - `problemas`, los primeros 100.

  Los errores de estructura de F01-H02 siguen respondiendo 400 con su código del catálogo, como hoy.
- **Por qué:** la constitución, en el principio XIII («Lo que no es un error»), dice que «un archivo de carga con errores responde con el informe por fila (RF-09), no con un error HTTP». La pantalla lo muestra como un resultado y no como un fallo.
- **Descartado:**
  - Un 422 con el informe en `detalles`: el modelo común no tiene columnas para la fila, el valor ni la sugerencia.
  - Un código nuevo del catálogo: el archivo no falla, se informa.

## 2. El CSV, en una operación aparte

- **Decisión:** `descargarInforme`, `POST /api/cargas/plantillas/{codigo}/archivo/informe`, recibe el mismo archivo y responde `text/csv`. Usa la misma validación y no guarda nada.
- **Por qué:**
  - Una operación con dos tipos de respuesta según `Accept` complica las interfaces que genera openapi-generator y la prueba de contrato.
  - Con dos operaciones, cada una tiene su permiso, su ejemplo y su prueba.
  - La aclaración 3 pide que lo genere el servidor, sin guardar el archivo.
- **Descartado:** el mismo `validarArchivo` con `Accept: text/csv`.

## 3. Las reglas salen del diccionario, interpretado una sola vez

El diccionario tiene campos estructurados (`tipo`, `longitud`, `obligatorio`, `clave`, `catalogo`) y textos semiestructurados (`formato`). Una clase del dominio, `ReglasDeColumna`, lee cada columna una vez al cargar el diccionario y deja sus reglas listas. Cada patrón del `formato` que entiende está escrito una sola vez y probado contra todas las columnas del diccionario real (FR-012):

| En el diccionario | Regla |
|---|---|
| `tipo: Texto` y `longitud` | hasta ese largo |
| `tipo: Entero` | `\d+`, hasta su largo |
| `tipo: Decimal` | `\d+(\.\d+)?`, hasta su largo |
| `tipo: Fecha` | aaaa-mm-dd y una fecha que existe |
| `tipo: S/N` | `S` o `N` |
| `catalogo: X` | los valores del catálogo X |
| `formato: A, B, C` (códigos en mayúsculas) | una enumeración; `AGREGADO_1..3` se expande a `AGREGADO_1`, `AGREGADO_2` y `AGREGADO_3` |
| `formato: Igual catálogo que 02.modalidad` | los valores de esa otra columna |
| `formato: N dígitos` | exactamente N dígitos |
| `formato` de rango (`1–10`, `0,5–20`, `≥ 0`, `> 0`, `≤ 40`) | dentro del rango |
| `tipo: Lista (;)` y `formato` de códigos separados por `;` | cada elemento está en esa enumeración (`jornadas`) |
| `tipo: Lista (;) de «a\|b\|…»` | cada elemento tiene tantas partes como el `«…»`: 4 en `titulos_cuarto_nivel`, 3 en `participantes` |
| `formato` con la cláusula `parte: A, B` | esa parte está en la enumeración (`día`, `tipo`, `rol`) |
| `formato` con la cláusula `horas HH:MM` | las partes `inicio` y `fin` son una hora válida HH:MM (`franjas_horarias`) |
| `formato` con la cláusula `años decimal` | la parte `años` es un decimal (`experiencia_laboral`) |
| `formato` de varias cláusulas separadas por `; ` | cada cláusula se interpreta por separado |
| `obligatorio: Sí` / `No` / las 5 condiciones | ver research §4 |

- Un `formato` que no es ninguno de estos no se interpreta: por ejemplo `Ej.: …`, `TC = 40, MT = 20, TP ≤ 19`, `años decimal` o `horas HH:MM`. Una prueba lista exactamente esos formatos. Si el diccionario cambia y aparece un formato nuevo, la prueba falla y obliga a decidir (principio XIV: no se asume una regla que no esté definida).
- **Descartado:**
  - Reescribir las reglas en Java por carga: el mismo dato en dos lugares.
  - Interpretar la columna `validacion`, que está en prosa: queda para los dueños (aclaración 1).

## 4. Las 5 condiciones de obligatoriedad

Son las únicas reglas en prosa que esta historia interpreta, porque deciden si una celda puede quedar vacía:

| Texto del diccionario | Columna | Condición |
|---|---|---|
| `Sí si tipo_unidad = GESTION` | 1.`codigo_gestion_autorizada` | obligatoria si `tipo_unidad` es `GESTION` |
| `Sí si tipo_unidad = COMISION` | 1.`es_comision_permanente` | obligatoria si `tipo_unidad` es `COMISION` |
| `Sí si TITULAR` | 6.`categoria_escalafon` | obligatoria si `tipo_personal` es `TITULAR` |
| `Sí si OCASIONAL` | 6.`fin_contrato` | obligatoria si `tipo_personal` es `OCASIONAL` |
| `Sí si COORDINADOR_CARRERA` | 8.`codigo_carrera` | obligatoria si `codigo_cargo` es `COORDINADOR_CARRERA` |
| `Sí salvo coordinador de carrera` | 8.`codigo_unidad` | obligatoria si `codigo_cargo` no es `COORDINADOR_CARRERA` |

Están en `CondicionDeObligatoriedad`, con una prueba que compara cada texto con el diccionario real. Si aparece un texto que no es «Sí», «No» ni uno de estos, la carga del diccionario falla con su columna.

## 5. La cédula y su sugerencia

- **Decisión:** en la columna `numero_identificacion`, cuando `tipo_identificacion` es `CEDULA`, se exigen 10 dígitos y el dígito verificador (módulo 10), como `Identificacion` de Docentes.
  - Con 9 dígitos y válida con un `0` delante: el motivo es «Cédula inválida o sin cero inicial: le falta el 0 inicial.», con la sugerencia de la cédula con el cero.
  - En cualquier otro caso: «Cédula inválida o sin cero inicial: debe tener 10 dígitos y un dígito verificador válido.».
  - Un pasaporte, `[0-9A-Z]{1,13}`.
  - En las columnas que citan a un docente (cargas 8 y 9), la identificación se compara con los docentes del sistema (§6), sin repetir la regla.
- **Por qué:** es el criterio de RF-08. La regla de la provincia y el tercer dígito la aplica Docentes al confirmar (aclaración 1), así que aquí no se duplica.
- **Descartado:** reusar `Identificacion` de Docentes. Es un tipo del dominio de otro servicio, y `ModulosTest` lo prohíbe.

## 6. Las referencias, por los puertos publicados

- **Decisión:**
  - Las carreras, las asignaturas, las unidades y los periodos llegan por `ConsultarReferencias` de Académico, que ya publica sus códigos (F03-H04, F03-H05).
  - Los docentes llegan por un puerto nuevo de Docentes, `ConsultarIdentificaciones`, que devuelve solo las identificaciones registradas, sin datos personales.
  - Cargas los recibe por `ReferenciasDelSistema` (puerto de salida) y `ReferenciasDesdePuertos` (adaptador), y suma `implementation project(':servicios:academico')` y `project(':servicios:docentes')` a su `build.gradle`. Las referencias se piden una vez por archivo.
- **El destino de cada `FK`:**

  | `FK` en el diccionario | Referencia |
  |---|---|
  | `01.codigo_unidad` | unidades de Académico |
  | `02.codigo_carrera` | carreras |
  | `03.codigo_periodo` | periodos |
  | `04.codigo_asignatura` | asignaturas |
  | `06.numero_identificacion` | identificaciones de Docentes |
  | `codigo_unidad (mismo archivo)` | las claves del propio archivo y, además, las unidades de Académico |
  | `participantes` → 06 | la parte «identificación» de cada elemento |
  | `codigos_carrera`, `asignaturas_afines_declaradas` | cada elemento de la lista |

  Si aparece una `FK` sin destino conocido, el arranque falla: la prueba del diccionario la detecta.
- **Por qué:** el principio V, y es el mismo camino que ya usa Docentes.
- **Descartado:** `ConsultarDocentes.buscar`, que devuelve el `Docente` del dominio de Docentes.

## 7. Claves repetidas

La clave de cada carga sale de `clave: PK` (una columna) o `PK compuesta: a + b + c` (varias). El texto de la compuesta se lee como los nombres que siguen a `PK compuesta:`, y se traducen a sus columnas con una tabla pequeña probada contra el diccionario:

| Texto | Columnas |
|---|---|
| `periodo` | `codigo_periodo` |
| `asignatura` | `codigo_asignatura` |
| `paralelo` | `paralelo` |
| `versión` | `codigo_version_politica` |
| `tipo_ist` | `tipo_ist` |
| `perfil` | `perfil` |
| `identificación` | `numero_identificacion` |
| `cargo` | `codigo_cargo` |
| `fecha_designacion` | `fecha_designacion` |

La segunda fila con la misma clave es un error: «Esta clave ya está en la fila N.».

## 8. Errores y advertencias

- **Un error** rechaza el archivo.
- **Una advertencia** no lo rechaza. Son dos:
  - «Tiene espacios en los extremos: se quitan.»;
  - «Fila vacía: se ignora.», para una fila con todas sus celdas en blanco entre registros. El número de registros no la cuenta: cambia lo que hacía F01-H02, que sí la contaba.
- Hoy los lectores no recortan los valores (solo los encabezados), así que la advertencia se puede detectar. Las reglas se aplican al valor recortado.

## 9. El CSV y la inyección de fórmulas (RNF-SEG-04)

- **Columnas:** `fila,campo,valor,motivo,tipo,sugerencia`, en UTF-8 con BOM, para que Excel muestre bien las tildes, con separador coma y comillas según RFC 4180.
- **Neutralización:** se aplica a toda celda que empiece con `=`, `+`, `-`, `@`, tabulación o retorno de carro: se le antepone un apóstrofo, como recomienda OWASP para CSV injection. Vive en un `EscritorDeInforme` del adaptador.
- **Pruebas:** con los cuatro caracteres, con `=HYPERLINK(...)`, con un valor que tiene comas y comillas, y con una celda vacía.

## 10. Datos personales (principio IX)

- El informe devuelve el `valor` de cada problema, porque es el dato del propio archivo que hay que corregir, y solo se responde a quien lo subió (aclaración 3).
- No se guarda y no se registra en los logs.
- Los motivos no llevan valores; la cédula sugerida va en `sugerencia`.
- La descripción del contrato cambia: «los detalles de un error de estructura nunca traen valores del archivo; el informe por fila, sí».

## 11. Tamaño y tiempo

- El tope sigue en 5 MB, unas 30 000 filas de docentes.
- La validación lee las referencias una vez y revisa cada celda en memoria; no espera pasar de 2 s. SC-004 da 30 s.
- La respuesta JSON trae los primeros 100 problemas; el CSV, todos.
- `probar_reglas_carga.sh` mide un archivo de 5 MB y falla desde 30 s. La meta del plan es 10 s.

## 12. La pantalla

- **Al subir:** «Plantillas de carga» ya sube el archivo (F01-H02). Ahora muestra además:
  - si es válido o está rechazado;
  - los totales;
  - una tabla con los primeros 100 problemas: fila, campo, valor, motivo, tipo y sugerencia.
- **Descargar el informe:** el botón vuelve a enviar el mismo archivo, que la pantalla conserva en memoria, a `descargarInforme`, y guarda el CSV que llega (`informe-<carga>.csv`).
- **Accesibilidad:** la tabla tiene su título y los totales van en un `role="status"`.

## 13. Glosario

No cambia: «Informe de validación de la carga» ya dice «errores y advertencias por fila: número de fila, campo, valor y motivo» (`InformeValidacionCarga`). En el código, el informe es `InformeValidacionCarga` y cada problema, `ProblemaDeFila`.
