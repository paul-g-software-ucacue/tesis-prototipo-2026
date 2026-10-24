# Research: Subir un archivo y validar su estructura (F01-H02)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`b5cfa3c`):
- **Cargas** (proceso `datos`): el diccionario de datos de las 10 cargas (`DiccionarioDeDatos`, `Carga`, `ColumnaDeCarga`), validado al arrancar, y las tres operaciones de las plantillas con `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` (F01-H01). El esquema `cargas` sigue vacío.
- **La pantalla «Plantillas de carga»**, con las 10 cargas, sus descargas y el diccionario desplegable.
- **El catálogo de errores** (ADR-016): una `EntradaCatalogo` relaciona una excepción con su código y puede calcular encabezados; los detalles por campo solo los lleva `COMUN_SOLICITUD_INVALIDA`.
- **Los datos de prueba** de las 10 cargas están fuera del repositorio, en `MDD_artefactos_desarrollo/requerimientos_mdd/cargas/datos_prueba/`: 606 filas en total (24, 7, 2, 300, 165, 47, 22, 21, 11 y 7), unos 94 KB, sintéticos (RNF-SEG-06).

## 1. D-71: «Cargas» para quien inicia el proceso (aclaración 1)

- **Aceptada por el autor el 2026-10-23** y registrada en `docs/decisiones.md` con este texto (salvo el estado):

  > **D-71. El Responsable de consolidación y el Vicerrector académico ejecutan las cargas**
  >
  > - **Estado:** Propuesta · **Fecha:** 2026-10-23 · **Decidido por:** autor de la tesis (aclaración 1 de F01-H02)
  > - **Origen:** `specs/019-subir-archivo-carga/spec.md` (Clarifications) y este research §1
  > - **Modifica a:** D-65 (matriz D-16)
  >
  > **Decisión.** El Responsable de consolidación y el Vicerrector académico reciben «Cargas de datos: lectura» y «Cargas de datos: escritura», con alcance «todo». Talento Humano las conserva. Una migración nueva de Roles (`V7`) las agrega (D-63).
  >
  > **Por qué.** La ERS §8.3 dice que «el rol que inicia el proceso de construcción del distributivo (por defecto, el Vicerrectorado o el Responsable de consolidación) ejecuta las cargas» y delega en Talento Humano; la historia F01-H02 la pide el Responsable de consolidación. En D-65 solo Talento Humano tenía «Cargas: escritura», porque la matriz en prosa de D-16 lo nombraba a él («Cargas que se le deleguen»).
  >
  > **Alternativas.** Permisos alternativos de D-70 (`[CARGAS:ESCRITURA, CONSOLIDACION:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`): no cambia la matriz, pero autoriza la carga de datos con permisos de otras funcionalidades. Solo Talento Humano: la historia no se cumpliría.
  >
  > **Consecuencias.** Los dos roles ven «Plantillas de carga» también por «Cargas: lectura» y suben archivos. Al desplegar, los usuarios con esos roles reciben los permisos en su próximo inicio de sesión. La prueba de la matriz (`MatrizD16IntegrationTest`) y la tabla «Matriz D-16» se ponen al día.

- **Las plantillas** siguen con `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`: con D-71 el Responsable y el Vicerrector las descargan por los dos permisos, y nada cambia para los demás.

## 2. Profundidad del modelo (principio II) y la fuente de datos (RF-57)

- **Decisión:** Ingesta es de soporte. El dominio tiene lo justo para validar la estructura sin dobles:
  - **`TablaDeDatos`** (record): los encabezados y las filas, cada una como lista de textos, tal como los entrega una fuente. `registros()` cuenta las filas sin las vacías del final (spec, casos límite).
  - **`Carga.compararEncabezados(List<String>)`**: la lista de **`DiferenciaDeEncabezado`** (columna y `MotivoDeDiferencia`: `FALTA`, `SOBRA`, `REPETIDA`, `FUERA_DE_LUGAR`), vacía si son iguales. Los espacios al inicio y al final se ignoran.
  - **`EncabezadosDistintos`** y **`ArchivoSinRegistros`**: las excepciones de esas dos reglas.
- **La fuente de datos (RF-57):** el puerto de salida **`LectorDeArchivo`** recibe el nombre y el contenido y devuelve una `TablaDeDatos`, o lanza el motivo por el que no se puede leer. La regla de los encabezados y el conteo viven en el dominio, sin saber de dónde vino la tabla: otro adaptador (Moodle, SIGA) entregaría otra `TablaDeDatos` sin cambiar nada más.
- **Alternativa:** validar los encabezados en cada lector. Repite la regla en el CSV y en el XLSX, y la ataría al formato.

## 3. Formato, codificación y tamaño

| Paso | Regla | Código |
|---|---|---|
| 1 | Más de 5 MB (`5 242 880` bytes) | `CARGAS_ARCHIVO_DEMASIADO_GRANDE` (413) |
| 2 | La extensión no es `.csv` ni `.xlsx` (sin distinguir mayúsculas) | `CARGAS_FORMATO_NO_ADMITIDO` (400) |
| 3 | Un `.xlsx` que no es un ZIP de Office Open XML (por ejemplo, un XLS antiguo renombrado) o un `.csv` que empieza con la firma de un PDF, un ZIP o un documento OLE2, o tiene bytes nulos | `CARGAS_FORMATO_NO_ADMITIDO` |
| 4 | Un CSV que no es UTF-8 válido (decodificador estricto); el BOM se acepta y se quita | `CARGAS_CODIFICACION_NO_ADMITIDA` (400) |
| 5 | Un XLSX con más de una hoja | `CARGAS_VARIAS_HOJAS` (400) |
| 6 | Comillas sin cerrar, una fila con otro número de valores que el encabezado, un XLSX cifrado o dañado | `CARGAS_ARCHIVO_ILEGIBLE` (400), con un detalle que dice la fila |
| 7 | Encabezados distintos de la plantilla | `CARGAS_ENCABEZADOS_DISTINTOS` (400), con un detalle por columna |
| 8 | Ningún registro | `CARGAS_ARCHIVO_SIN_REGISTROS` (400) |

- **Por qué la extensión:** es lo que el usuario ve y elige; el contenido se comprueba después (paso 3), para que un archivo renombrado no pase. Un CSV en Windows-1252 con solo caracteres ASCII es también UTF-8 válido, así que se acepta: lo que se pierde son las tildes, y esas fallan el paso 4.
- **El orden de los pasos** hace que cada archivo tenga un solo motivo, el primero que lo explica.
- **El tamaño:** el proceso `datos` acepta partes de hasta 10 MB (`spring.servlet.multipart.max-file-size`), y el servicio rechaza más de 5 MB con su código, que dice el máximo. Más de 10 MB lo corta Spring con 413; hoy ese 413 llega con `COMUN_SOLICITUD_INVALIDA`, porque `CodigosComunes.segunEstado` no tiene el 413. Se agrega **`COMUN_CONTENIDO_DEMASIADO_GRANDE`** (413) al catálogo común, para que cualquier proceso lo responda bien.
- **Alternativa:** limitar a 5 MB solo con Spring. El mensaje sería genérico y no diría el máximo.

## 4. Leer el CSV

- **Decisión:** un lector propio en el adaptador (`LectorCsv`), de unas 100 líneas: RFC 4180 con comillas dobles, comillas duplicadas y saltos de línea dentro de un valor; separador coma o punto y coma, el que aparezca primero fuera de comillas en la fila de encabezados (convenciones); fin de línea LF o CRLF.
- **Por qué:** las reglas son pocas y las fijan las convenciones; un lector propio no agrega una dependencia (principio X) y deja controlar los mensajes de cada error.
- **Alternativa:** Apache Commons CSV. Es pequeño y conocido, pero sería una dependencia más para lo que el JDK resuelve, y sus excepciones no dicen la fila en el formato que necesitamos.

## 5. Leer el XLSX

- **Decisión:** **Apache POI 5.5.1** (`poi-ooxml`), solo en el adaptador `LectorXlsx`, con la versión en `gradle.properties` (`versionApachePoi`). Lee la única hoja con `XSSFWorkbook` y convierte cada celda con `DataFormatter`, que devuelve el texto que muestra la celda: una cédula con formato de texto conserva su 0 inicial y una fecha sale como la muestra la hoja.
- **Por qué POI:** es el lector de referencia de OOXML, con protección contra XXE y contra los ZIP que se inflan de más (`ZipSecureFile`), y su `DataFormatter` da el «texto que muestra la celda» que pide la spec. Con el límite de 5 MB, cargar la hoja entera en memoria no pesa.
- **Versión:** la 6.0.0 salió el 19/10/2026, hace 4 días; la regla del proyecto pide versiones con más de dos semanas. La 5.5.1 es del 26/11/2025.
- **Riesgo:** POI trae sus dependencias (`poi`, `xmlbeans`, `commons-compress`, `commons-io`, `commons-collections4`, `commons-math3`, `SparseBitSet`, `curvesapi`, `log4j-api`). El SBOM y Grype de la etapa 4 las revisan; si alguna tiene una vulnerabilidad alta con versión corregida, se fija como Jackson y Tomcat en `gradle.properties`.
- **Alternativas:** `fastexcel-reader` 0.20.2 (más liviano, pero sin el formateo de celdas de `DataFormatter`); un lector propio con `java.util.zip` y StAX (más código, y el formato de números y fechas de las celdas no es trivial).

## 6. Los errores con detalles (ADR-016)

- **Decisión:** `EntradaCatalogo` gana una forma con detalles, como ya tiene con encabezados: `EntradaCatalogo.conDetalles(excepcion, definicion, error -> List<DetalleRespuesta>)`. `ManejadorDeErrores` los pone en la respuesta junto al código de la entrada. Así `CARGAS_ENCABEZADOS_DISTINTOS` dice cada columna (`{campo: "codigo_carrera", descripcion: "Falta en el archivo."}`) y `CARGAS_ARCHIVO_ILEGIBLE`, la fila (`{campo: "archivo", descripcion: "La fila 7 tiene 5 valores y la plantilla, 12."}`).
- **Por qué:** el modelo `Error` ya tiene `detalles` (principio XIII); solo `COMUN_SOLICITUD_INVALIDA` los llenaba. El título y la descripción siguen fijos en el catálogo; lo variable va en los detalles, que nunca llevan valores del archivo, solo nombres de columna y números de fila.
- **Alternativa:** responder `COMUN_SOLICITUD_INVALIDA` con los detalles. Perdería el código propio, y su descripción («Revisa los campos marcados») no habla de un archivo.
- Es una ampliación del catálogo dentro de ADR-016, sin cambiar sus reglas: no se registra como decisión.

## 7. El contrato (principio IV)

- **Decisión:** una operación nueva en `contratos/cargas/openapi.yaml`, `validarArchivo`, `POST /api/cargas/plantillas/{codigo}/archivo`, `multipart/form-data` con la parte `archivo`, `x-mdd-permiso: CARGAS:ESCRITURA`. Responde 200 con `LecturaDeArchivo` (`carga`, `archivo`, `formato` `CSV` o `XLSX`, `registros`). Errores: 400 con los 6 códigos de §3, 404 `COMUN_RECURSO_NO_ENCONTRADO`, 413.
- **Por qué esa ruta:** el archivo se arma con la plantilla de la carga, y la ruta queda junto a la plantilla y al diccionario. `POST`, aunque no guarde nada, porque recibe un cuerpo.
- **Alternativa:** `/api/cargas/{codigo}/archivo`: el patrón del código chocaría con `/api/cargas/plantillas` y `/api/cargas/sesion`.

## 8. El nombre del archivo y los datos

- El nombre que manda el navegador solo se usa para la extensión y para devolverlo en la respuesta: se quita cualquier ruta y se recorta a 255 caracteres. No se guarda ni se registra.
- El contenido no se guarda (aclaración 3) ni se escribe en los registros del proceso: ni filas ni valores, porque una carga de docentes trae cédulas (principio IX, LOPDP).

## 9. La pantalla

- **Decisión:** en «Plantillas de carga», cada fila gana un botón «Subir archivo» para quien tiene «Cargas: escritura», que abre el selector de archivos del navegador (`accept=".csv,.xlsx"`). El resultado aparece debajo de la tabla: «`06_docentes.csv`: 47 registros leídos» (éxito) o el título del error con sus detalles. Antes de enviar, la SPA rechaza un archivo de más de 5 MB con el mismo texto del servidor, para no subirlo en vano.
- **El nombre de la pantalla** se mantiene: «Plantillas de carga». F01-H05 (configurar las 10 cargas en su orden) es la que la convierte en la pantalla de las cargas.
- **Alternativa:** una pantalla «Subir archivo» aparte. Separaría la plantilla del archivo que se arma con ella.

## 10. Punta a punta

- **Decisión:** `deploy/probar_subida.sh`, con el estilo de `probar_plantillas.sh`. Arma los archivos en una carpeta temporal con datos ficticios: un CSV de carreras, el mismo con punto y coma, el mismo en XLSX (con `python3 -I` y `zipfile`, sin binarios en el repositorio), uno sin `codigo_carrera`, uno en Windows-1252, uno con una columna de más, uno sin registros, un PDF mínimo y uno de 6 MB. Pasos:
  1. `datos.demo` sube el CSV y el XLSX: 200 con el mismo número de registros;
  2. el de punto y coma, también;
  3. cada archivo inválido da su código, y el de la columna faltante dice `codigo_carrera`;
  4. `consolidacion.demo` y `vicerrectorado.demo` suben (D-71);
  5. `coordinacion.demo` (solo descarga) y `docente.demo` reciben 403;
  6. una carga que no existe, 404.

  No crea nada. Se agrega a `perfil-consolidado.yml`.
- **Las pruebas del adaptador** usan una copia de los 10 CSV de los datos de prueba de la ERS, en los recursos de prueba de Cargas (sintéticos, RNF-SEG-06), y los convierten a XLSX con POI dentro de la prueba: cada uno se lee con su número de filas (SC-002).

## 11. Glosario (principio III)

- La spec usa «Carga», «Plantilla de carga» y «Diccionario de datos», del glosario. El código nuevo necesita **`TablaDeDatos`** (lo que entrega una fuente), que no es un término del negocio sino del puerto de RF-57, como `FuenteDelDiccionario`: no entra al glosario. La spec habla de «el archivo de una carga», sin darle otro nombre, porque el glosario ya define «Carga» como ese archivo; «Resultado de la lectura» describe la respuesta, sin término propio, y en el código es `LecturaDeArchivo`, un modelo del contrato.
- **Sin cambios en el glosario.**
