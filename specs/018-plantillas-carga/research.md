# Research: Descargar las plantillas de carga (F01-H01)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`c5e5e26`):
- **Cargas** (proceso `datos`, detrás de `/api/cargas/**` en el API Gateway): solo el esqueleto de la Ola 0, con el esquema `cargas` vacío (V1), la consulta de la sesión y sus pruebas de arquitectura.
- **Permisos alternativos** (D-70): una operación puede declarar en `x-mdd-permiso` una lista; `VerificadorDePermisos` comprueba cada uno por separado, y la SPA ya tiene `tieneAlguno` y `RutaConPermiso cualquieraDe` (F03-H01).
- **Los anexos de la ERS** están fuera del repositorio, en `MDD_artefactos_desarrollo/requerimientos_mdd/cargas`: `plantillas/` (10 CSV con su fila de encabezados), `ejemplos/` (10 CSV con filas coherentes entre sí), `Diccionario_datos_cargas_MDD.xlsx` y `scripts/build_diccionario.py`, que genera los tres desde una sola lista de datos: por carga, el archivo, el nombre, de qué cargas depende y sus campos (campo, descripción, tipo, longitud, obligatoriedad, formato, clave y validación), las convenciones y los catálogos.

## 1. Profundidad del modelo (principio II)

- **Decisión:** Ingesta de datos es de soporte. El dominio tiene lo justo para que la definición sea válida y se pueda probar sin dobles:
  - **`Carga`** (record): número de orden, código, nombre, archivo, las cargas de las que depende, obligatoriedad y sus columnas. Es el término del glosario («Carga»).
  - **`ColumnaDeCarga`** (record): campo, descripción, tipo, longitud máxima, obligatoriedad, formato o valores, clave, validación, ejemplo y, si lo tiene, el catálogo de sus valores.
  - **`Obligatoriedad`** (enum): `OBLIGATORIA`, `OBLIGATORIA_SALVO_SIN_PROYECTOS` (la 9, RF-18) y `RECOMENDADA` (la 10).
  - **`DiccionarioDeDatos`** (valor con las 10 cargas, las convenciones y los catálogos): valida que la definición esté completa (§3) y responde `carga(codigo)`.
  - **`PlantillaCarga`** (record, término del glosario): la plantilla de una carga, con su nombre de archivo y sus encabezados, que salen de la `Carga`.
- **Por qué:** las cargas no cambian en tiempo de ejecución, así que no hay agregado ni repositorio con escritura; basta un valor validado al arrancar.
- **Alternativa:** guardar la definición en tablas del esquema `cargas`. Agrega una migración y un repositorio para un dato que solo cambia con la ERS.

## 2. Una sola definición, como dato (aclaración 3, principio VI)

- **Decisión:** un archivo versionado `backend/servicios/cargas/src/main/resources/cargas/diccionario-de-datos.yaml`, transcrito una sola vez desde las listas de `build_diccionario.py` (ERS v2.1), con las 10 cargas y sus 115 columnas, las 13 convenciones y los 5 catálogos. Se lee al arrancar con SnakeYAML (`SafeConstructor`), como `usuarios-demo.yaml`; si no es válido (§3), el proceso no arranca y dice qué falta.
- La plantilla y el diccionario se generan de él. F01-H02 validará los archivos subidos con la misma definición.
- **Oráculo independiente:** las pruebas comparan los encabezados generados con una copia de las 10 primeras líneas de `cargas/plantillas/` de la ERS, guardada como recurso de prueba (`anexo-ers-encabezados.csv`: son solo nombres de columnas, sin datos). Así, un error al transcribir el YAML no pasa inadvertido.
- **Alternativas:** copiar al repositorio los 10 CSV y un diccionario ya armado (aclaración 3, descartada: F01-H02 repetiría las reglas); generar el YAML con el script de la ERS en cada build (el script escribe en rutas fijas fuera del repositorio y necesita `openpyxl`).

## 3. Lo que valida `DiccionarioDeDatos`

| Regla | Por qué |
|---|---|
| Exactamente 10 cargas, con los números 1 a 10, sin repetir, y códigos únicos `^[a-z_]{1,40}$` | ERS §8.3 |
| Cada carga depende solo de cargas anteriores | el orden de carga de §8.3 |
| Al menos una columna por carga; los campos no se repiten en la carga y son `^[a-z0-9_]+$` | «Encabezados: exactamente los nombres de la columna campo, en minúsculas» (convenciones) |
| Toda columna tiene tipo, obligatoriedad y un ejemplo no vacíos | RF-05 y el escenario del issue |
| El catálogo que nombra una columna existe | que el diccionario diga los valores (US2, escenario 2) |
| Ningún texto empieza con `=`, `+`, `-`, `@`, tabulador o retorno de carro | inyección de fórmulas en CSV (principio IX) |

## 4. El ejemplo de cada columna (caso límite)

- **Decisión:** el ejemplo de cada columna es el de la primera fila de `cargas/ejemplos/` con un valor en ella, como la columna «Ejemplo» del diccionario de la ERS, que toma la primera fila. Para las 6 columnas opcionales sin ningún valor en los ejemplos se escribe uno coherente con su formato, y el YAML lo marca con un comentario:

  | Carga · columna | Ejemplo |
  |---|---|
  | 03 · `fecha_limite_envio_senescyt` | `2026-03-07` (aaaa-mm-dd; 30 días antes del inicio del ejemplo, `2026-04-06`, RN-23) |
  | 04 · `creditos` | `3.5` |
  | 05 · `codigo_unidad_sede` | `MATRIZ` |
  | 05 · `horas_semanales` | `4` |
  | 06 · `telefono` | `0999999999` (ficticio) |
  | 06 · `registro_senescyt_3n` | `1031-2020-0000001` (ficticio) |

- Los ejemplos son ficticios (RNF-SEG-06): son los de la ERS, con cédulas de dígito verificador válido.

## 5. Los catálogos en el diccionario (US2, escenario 2)

- **Decisión:** los 5 catálogos de la hoja «Catálogos» de la ERS (gestiones autorizadas, cargos, perfiles de carga, campos amplios CINE y tipo de IST) van en el YAML, y una columna los nombra con `catalogo`. En el CSV del diccionario y en la pantalla, la celda «Formato o valores permitidos» de esa columna lista sus valores con su descripción («G01 Rector; G02 Vicerrector; …»). Ninguno pasa de 21 valores.
- Son una copia para documentar: las 21 gestiones las tiene también Académico (F03-H01). Cuál de las dos copias manda al validar es de F01-H02 (MD-13: el dueño de la entidad aplica la carga).
- **Alternativa:** una descarga aparte con los catálogos. Agrega un archivo más para unos pocos valores.

## 6. El formato de los archivos

- **Plantilla:** CSV en UTF-8 **con BOM**, separador coma, fin de línea CRLF (RFC 4180, como dicen las convenciones), una sola fila: los encabezados. El BOM es para SC-003: sin él, Excel abre el CSV como Windows-1252 y rompe las tildes y la «ñ». F01-H02 debe aceptar el BOM al leer.
- **Diccionario:** el mismo formato. Una fila de encabezados (`numero, campo, descripcion, tipo, longitud_maxima, obligatorio, formato_o_valores, clave, validacion, ejemplo`) y una fila por columna, en el orden de la plantilla. Después, una fila vacía y el bloque de convenciones (`tema, regla`), que son las mismas en todas las cargas. El diccionario es para leerlo en una hoja de cálculo; nada lo vuelve a leer, porque F01-H02 usa la definición.
- **Nombres:** `01_estructura_organica.csv` y `01_estructura_organica_diccionario.csv`, en `Content-Disposition: attachment`.
- **Comillas:** un valor con coma, comillas o salto de línea va entre comillas dobles, con las internas duplicadas. El escritor CSV, además, antepone un apóstrofo a un valor que empiece como fórmula (principio IX), aunque la definición ya los prohíbe (§3).
- **Riesgo conocido:** Excel con la configuración regional en punto y coma abre un CSV con comas en una sola columna. La comprobación de SC-003 se hace en LibreOffice Calc, en el equipo del autor, y la evidencia lo dice. Las convenciones ya dicen que el sistema también aceptará «;» al cargar.

## 7. Quién descarga: D-70, sin decisiones nuevas (aclaración 1)

- **Decisión:** las 3 operaciones declaran `x-mdd-permiso: [CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`, con `hasAnyAuthority` en el controlador. Como `AutoridadesDelToken` quita el alcance `:PROPIO`, el Coordinador de carrera (`DISTRIBUTIVO:LECTURA:PROPIO`) también descarga, como dice la aclaración. No cambia la matriz.
- Con la matriz D-65 actual descargan: el Administrador y Talento Humano (Cargas); el Responsable de consolidación, el Vicerrector, el Rector, el Coordinador de carrera, el OCS y Secretaría general (Distributivo). No descargan el Gestor ni el Docente.
- Descargar no cambia nada, así que no deja registro (principio VII habla de acciones relevantes).

## 8. El contrato (principio IV)

- **Decisión:** tres operaciones nuevas en `contratos/cargas/openapi.yaml` (detalle en [contracts/cargas.md](contracts/cargas.md)):
  - `listarCargas`: el diccionario completo en JSON, para la pantalla;
  - `descargarPlantilla` y `descargarDiccionario`: `text/csv` con `type: string`, para que el generador devuelva `ResponseEntity<String>` y el controlador ponga el BOM y el `Content-Disposition`.
- Una carga que no existe responde 404 con `COMUN_RECURSO_NO_ENCONTRADO`, como una unidad inexistente en Académico (F03-H01). No hace falta un código nuevo.
- Las respuestas de error declaran `application/json` junto al `text/csv`. Hay que comprobar que un 403 o un 404 con `Accept: text/csv` llegan como el `Error` común y no como 406. Lo cubre la prueba de contrato y, si falla, el plan se ajusta en las tareas.
- **Alternativa:** que la SPA arme los CSV desde el JSON. Repite el formato en dos lados, y «llamando directamente al servidor» (SC-004) no tendría qué descargar.

## 9. La pantalla «Plantillas de carga» (US3)

- **Decisión:** `frontend/src/pantallas/plantillas/`, en el menú como «Plantillas de carga», con la misma lista de permisos alternativos en la ruta y en el menú (`DESCARGA_DE_PLANTILLAS` en `funcionalidades.ts`).
  - Una tabla con las 10 cargas: número, nombre, «Depende de» (con los nombres de esas cargas), «Obligatoria» (Sí, «Sí, o sin proyectos» o Recomendada), columnas y dos botones: «Plantilla» y «Diccionario».
  - Cada fila se despliega y muestra su diccionario en una tabla. Arriba van las convenciones, plegadas.
- **Descargar con el token:** el token está solo en memoria (D-51), así que un enlace directo no lo llevaría. La SPA pide el CSV con `apiCargas` (`parseAs: 'blob'`), crea una URL `blob:` y hace clic en un `<a download>` con el nombre del archivo, que sale del `Content-Disposition`. La CSP de `nginx.conf` no lo restringe: la descarga no es una carga de recurso. Lo comprueba Playwright con el evento `download`.
- **Cliente nuevo:** `apiCargas` en `api/cliente.ts`, y `generar:api` genera `generado/cargas.d.ts`.

## 10. Punta a punta

- **Decisión:** `deploy/probar_plantillas.sh`, con el estilo de `probar_estructura.sh`:
  1. `consolidacion.demo` lista las 10 cargas con 115 columnas en total, y cada columna con tipo, obligatoriedad y ejemplo;
  2. descarga la plantilla de cada carga: 200, `text/csv`, el `Content-Disposition` con su nombre, el BOM y una sola línea;
  3. descarga el diccionario de la carga 1: tantas filas como columnas, más las convenciones;
  4. `datos.demo` (Cargas) y `coordinacion.demo` (Distributivo, propio) descargan; `gestion.demo` y `docente.demo` reciben 403;
  5. una carga inexistente da 404 `COMUN_RECURSO_NO_ENCONTRADO`.

  No crea nada, así que no necesita limpieza. Se agrega a `perfil-consolidado.yml`.

## 11. Glosario (principio III)

- La spec usa «diccionario de datos» y «columna», que aparecen en la definición de «Plantilla de carga» pero no son términos del glosario, y el código necesita `DiccionarioDeDatos` y `ColumnaDeCarga`. **Aprobada por el autor y aplicada el 2026-10-23** (copia `modelo_dominio.md.antes-F01-H01`): se agregó a la sección 7 de `sdd/modelo_dominio.md`, después de «Plantilla de carga»:

  | Término | Significado | En el código | Contexto | Fuente |
  |---|---|---|---|---|
  | Diccionario de datos | Descripción de las columnas de las 10 cargas: por cada una, su tipo, longitud, obligatoriedad, valores permitidos, clave, validación y un ejemplo, más las convenciones comunes. Es la misma definición con la que se validan las cargas. | `DiccionarioDeDatos`, `ColumnaDeCarga` | Ingesta | RF-05, ERS 8.3 |
