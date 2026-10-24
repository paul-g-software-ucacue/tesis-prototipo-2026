# Evidencia: F01-H01

Registro de las comprobaciones de la historia (issue #27): descargar la plantilla de cada una de las 10 cargas y su diccionario de datos, generados de una sola definición, con los permisos alternativos de D-70 (RF-05, ERS §8.3). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, dominio y definición, lectura y servicio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T002 · Contrato: 3 operaciones, 6 esquemas, sin códigos nuevos | ✅ Generados `PlantillasApi` (las descargas, `ResponseEntity<String>` con `produces = text/csv, application/json`) y `cargas.d.ts`; `verificar:ruptura` en verde | `contratos/cargas/openapi.yaml` | 2026-10-23 |
| T003 · Dominio, con las pruebas primero | ✅ 18 pruebas, sin dobles, que no compilaban antes del dominio: cada regla de data-model.md con la carga y la columna en el mensaje, y sin fórmulas | `DiccionarioDeDatosTest` (13), `ColumnaDeCargaTest` (5) | 2026-10-23 |
| T004 · Oráculo de los anexos de la ERS | ✅ La primera línea de los 10 CSV de `MDD_artefactos_desarrollo/requerimientos_mdd/cargas/plantillas/`, con el nombre del archivo delante (solo nombres de columnas) | `CAR/test/resources/cargas/anexo-ers-encabezados.csv` | 2026-10-23 |
| T005 · `diccionario-de-datos.yaml` y `DiccionarioYaml` | ✅ 7 pruebas: 10 cargas con 7, 12, 8, 14, 9, 26, 14, 8, 12 y 5 columnas (115), **iguales al oráculo**; obligatoriedad y «Depende de» de la ERS; 13 convenciones y 5 catálogos (21 gestiones, 11 perfiles); los 6 ejemplos escritos a mano; sin «hoja Catálogos»; un YAML con un error falla con la carga y la columna. El YAML se transcribió con un script de una sola vez desde las listas `L`, `CONV` y `CAT` de `build_diccionario.py`, que además comprobó que los encabezados de `ejemplos/` coinciden | `DiccionarioYamlTest` | 2026-10-23 |
| Catálogo de cargos de la ERS incompleto | ⚠️ La hoja «Catálogos» da los cargos de gestión como un rango, «G06_GESTOR_VINCULACION … G21_GESTOR_ESTADISTICAS»: no lista los 16 códigos. Se transcribió tal cual, como un valor con la descripción de la ERS; los códigos exactos los decide F03-H05 o F01-H02 | `diccionario-de-datos.yaml`, catálogo `CARGOS` | 2026-10-23 |
| Nombre de la carga 7 | La ERS §8.3 la llama «Cargar horas según la política de contratación» y el diccionario, «Horas según la política de contratación»: se usa el del diccionario. El código, del nombre de su archivo, es `politica_carga_horaria` | `diccionario-de-datos.yaml` | 2026-10-23 |
| T006 · `ConsultarPlantillasServicio` con Mockito | ✅ 2 pruebas: lee la fuente una sola vez; un código que no es de una carga lanza `CargaNoEncontrada`. El método se llama `carga(codigo)` (tasks decía `plantilla`), porque devuelve la carga, de la que salen la plantilla y el diccionario | `ConsultarPlantillasServicioTest` | 2026-10-23 |

## Plantilla de una carga (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T007 · `EscritorCsv` | ✅ 4 pruebas, que no compilaban antes: BOM, coma y CRLF; comillas con las internas duplicadas; apóstrofo delante de `=`, `+`, `-` y `@`; tildes y «ñ» en UTF-8 | `EscritorCsvTest` | 2026-10-23 |
| T008 · Contrato de la plantilla | ✅ `listarCargas` con 10 cargas y 115 columnas; las 10 plantillas: `text/csv;charset=UTF-8`, `attachment; filename="NN_<codigo>.csv"`, el BOM y una línea **igual al oráculo**; 404 `COMUN_RECURSO_NO_ENCONTRADO` y 400 con otro formato; **con `Accept: text/csv`, el 404 y el 403 llegan como el `Error` común en JSON, no como 406** (el riesgo de research §8 no se dio) | `PlantillasContratoTest` | 2026-10-23 |
| T009 · Permisos | ✅ Las 3 operaciones exigen `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`. Mutación: con solo `hasAuthority('PERMISO_CARGAS:LECTURA')` la prueba falla; restaurado | `PermisosCargasTest` | 2026-10-23 |
| `SesionContratoTest` | ⚠️→✅ Con `@WebMvcTest` sin controlador cargaba también `PlantillasControlador` sin su caso de uso; ahora se limita a `SesionControlador` | `SesionContratoTest` | 2026-10-23 |

## Diccionario (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T010 · Contrato del diccionario | ✅ 3 pruebas: los 10 diccionarios con su nombre, el BOM, el encabezado, una fila por columna en el orden de la plantilla, una fila vacía, `tema,regla` y 13 convenciones; en la estructura orgánica, `codigo_gestion_autorizada` con «Sí si tipo_unidad = GESTION» y «G01 Rector; G02 Vicerrector; …», y las descripciones con tildes en UTF-8; `listarCargas` trae sus 21 `valores`; 404 | `PlantillasContratoTest` (8 en total) | 2026-10-23 |
| Cargas: todas las pruebas del módulo | ✅ 47 pruebas, 0 fallas; cobertura de líneas del módulo 100 %; ArchUnit, Spotless y SpotBugs en verde | `./gradlew :servicios:cargas:check` | 2026-10-23 |

## Pantalla «Plantillas de carga» (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T011 · La pantalla, con Vitest | ✅ 9 pruebas, que fallaron antes de la pantalla: las 10 cargas en orden con «—», «Sí, o sin proyectos» y «Recomendada»; el diccionario desplegable con los valores del catálogo; las convenciones plegadas; las dos descargas con el token y el nombre de `Content-Disposition`; un 503 muestra su título; el menú con `CARGAS:LECTURA`, `DISTRIBUTIVO:LECTURA` o `DISTRIBUTIVO:LECTURA:PROPIO`; sin ninguno, «Acceso no autorizado» | `Plantillas.test.tsx` | 2026-10-23 |
| T012 · Playwright y axe | ✅ 3 pruebas nuevas: `consolidacion.demo` descarga `06_docentes.csv` (evento `download`, 26 encabezados) y `01_estructura_organica_diccionario.csv`; `gestion.demo` no ve la entrada; sin fallas WCAG 2.1 AA en la tabla, con un diccionario desplegado y con las convenciones. Toda la suite: 65 pasan | `e2e/plantillas.spec.ts` | 2026-10-23 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
