# Evidencia: F01-H03

Registro de las comprobaciones de la historia (issue #29): validar las reglas de cada fila de las 10 cargas con el diccionario de datos y las referencias del sistema, y el informe por fila en la respuesta y en CSV (RF-07, RF-08, RF-09, RNF-SEG-04; D-71, D-72). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, intérprete del diccionario y puerto de Docentes

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El contrato: `validarArchivo` con el informe (`valido`, `errores`, `advertencias` y `problemas` con `maxItems: 100`), `TipoDeProblema`, `ProblemaDeFila` y `descargarInforme` (`text/csv`, `CARGAS:ESCRITURA`) | El YAML carga; los modelos de Java y los tipos de la SPA se generan; `verificar:ruptura` pasa (la respuesta solo agrega propiedades) | `contratos/cargas/openapi.yaml`, `npm run generar:api`, `npm run verificar:ruptura` | 2026-10-25 |
| Las reglas de cada columna, del diccionario real | `ReglasDeColumnaTest`: 83 casos que pasan, no pasan o quedan en el límite (largo, entero, decimal, rango `1–10`, `0,5–20`, `≥ 0`, `> 0`, `≤ 40`, fecha que existe, S/N, catálogo y enumeración con `AGREGADO_1..3`, `Igual catálogo que 02.modalidad`, `N dígitos`, «Mayúsculas», código de unidad, periodo, correo, jornadas, franjas HH:MM, títulos con 4 partes, experiencia con años decimales, participantes con su rol). **Los formatos que no se interpretan son exactamente 8** (`Ej.: …`, `Como consta en SENESCYT` y `TC = 40, MT = 20, TP ≤ 19`): uno nuevo hace fallar la prueba | `./gradlew :servicios:cargas:test --tests '*ReglasDeColumnaTest*'` | 2026-10-25 |
| Las 5 condiciones de obligatoriedad, las claves y los destinos de las referencias | `CondicionDeObligatoriedadTest` (las 6 columnas con la fila que cumple y la que no; un texto desconocido detiene el diccionario con su columna), `ClaveDeCargaTest` (3 simples y 3 compuestas; la 10 sin clave; un nombre desconocido falla) y `ReferenciaDeColumnaTest` (11 `FK`, el padre en el mismo archivo y el participante por su parte «identificación»; un destino desconocido falla) | `./gradlew :servicios:cargas:test --tests '*dominio*'` | 2026-10-25 |
| Docentes publica sus identificaciones | `ConsultarIdentificacionesServicioTest` (Mockito): las 47 de prueba, de cualquier estado laboral, y ninguna sin docentes. Docentes: todas sus pruebas pasan, 81,7 % de líneas | `./gradlew :servicios:docentes:check` | 2026-10-25 |

## Reglas de cada fila (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| **La cédula sin el cero inicial** (escenario del issue) | `109990010` en la fila 2 da «Cédula inválida o sin cero inicial: le falta el 0 inicial.» con la sugerencia `0109990010`; con el dígito verificador mal, el otro motivo y sin sugerencia; un pasaporte `AB123456` pasa, `ab123` y uno de 14 no | `ValidadorDeFilasTest` | 2026-10-25 |
| Problemas por fila y campo | Una fila con una fecha `2026-02-30`, una dedicación `XX` y un nombre de 61: tres errores en la fila 5, en el orden de las columnas; los obligatorios vacíos, el de un titular y el de un ocasional con su condición; la clave repetida simple (fila 49 → «ya está en la fila 2») y compuesta (fila 167 → fila 3); espacios en los extremos: advertencia y el valor recortado pasa; una fila vacía intermedia: advertencia y no cuenta; 150 problemas: los totales y los primeros 100; **ningún motivo repite su valor** | `ValidadorDeFilasTest` | 2026-10-25 |
| **Los 10 archivos de prueba no tienen errores ni advertencias** (SC-001) | Con las referencias de las cargas 01 a 06 de prueba, los 10 se validan sin ningún problema (la carga 07, con la fila 22 completada, ver Notas) | `ValidadorDeFilasTest.losDiezArchivosDePruebaNoTienenErrores` | 2026-10-25 |
| Las pruebas fallan si la regla se rompe | Aceptar cualquier cédula hace fallar `ValidadorDeFilasTest`; quitar la comprobación de rango hace fallar 8 casos de `ReglasDeColumnaTest` (mutaciones revertidas) | `./gradlew :servicios:cargas:test` | 2026-10-25 |

## Referencias (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| **Una asignatura con una carrera que no existe** (escenario del issue) | El error en la fila 11 y en `codigo_carrera`: «No existe la carrera en el sistema: regístrela o cárguela antes.»; también un periodo, una unidad y un docente; en una lista, «El elemento 2 no existe en el sistema como carrera» y el participante 2 como docente; el padre de una unidad en el mismo archivo pasa sin unidades en el sistema, y uno que no está en ninguno de los dos no | `ValidadorDeFilasTest` | 2026-10-25 |
| Las referencias por los puertos publicados | `ReferenciasDesdePuertosTest` con dobles escritos a mano de `ConsultarReferencias` y `ConsultarIdentificaciones`: traduce los códigos, activos o no. `ModulosTest` pasa: Cargas usa solo `puertos.entrada` de Académico y de Docentes | `./gradlew :apps:datos:test` | 2026-10-25 |

## Informe (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El servicio | `ValidarArchivoServicioTest` (Mockito): el informe en el resultado; una fila con errores no lanza; los errores de estructura pasan tal cual **sin pedir las referencias** (FR-009); las referencias se piden una vez por archivo | `./gradlew :servicios:cargas:test --tests '*ValidarArchivoServicioTest*'` | 2026-10-25 |
| El CSV neutralizado (RNF-SEG-04, SC-003) | `EscritorDeInformeTest`: `fila,campo,valor,motivo,tipo,sugerencia` en UTF-8 con BOM, con la sugerencia de la cédula; `=1+1`, `+57`, `-1`, `@SUMA(A1)`, tabulación y retorno de carro con un apóstrofo delante; `=HYPERLINK("x")` y una sugerencia `-2` neutralizados y entre comillas; RFC 4180; sin problemas, solo los encabezados | `./gradlew :servicios:cargas:test --tests '*EscritorDeInformeTest*'` | 2026-10-25 |
| El contrato y los permisos | `SubidaContratoTest`: el CSV de docentes, 200 con `valido` y sin problemas; la cédula sin cero, 200 con el problema y la sugerencia, **sin la cédula en los logs**; `descargarInforme` responde `text/csv` con `informe-docentes.csv` y `'=1+1`; cada respuesta valida contra el contrato. `PermisosCargasTest`: las dos operaciones exigen `CARGAS:ESCRITURA`; con `CARGAS:LECTURA` en `descargarInforme` falla (mutación revertida). Cargas: 253 pruebas sin fallas, 97,3 % de líneas | `./gradlew :servicios:cargas:check` | 2026-10-25 |

## Reintentar (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Corregir y reintentar | Un archivo rechazado y después el corregido: el segundo informe es solo el del archivo nuevo, sin errores. El servicio y el controlador no tienen estado entre peticiones | `SubidaContratoTest.dadoUnArchivoRechazado_cuandoSeSubeElCorregido_entoncesSeValidaDesdeCero` | 2026-10-25 |

## Pantalla (US5)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| La pantalla con el informe (Vitest) | `Plantillas.test.tsx`, 18 pruebas: válido sin problemas («es válido: 47 registros leídos.», sin tabla); rechazado («está rechazado: 1 error y 1 advertencia. Corríjalo y vuelva a subirlo.» en un `role="status"` y la tabla con la fila, el campo, el valor, el motivo, el tipo en español y la sugerencia); solo advertencias («es válido, con 1 advertencia»); 150 problemas («Se muestran 100 de 150; el informe los trae todos.» y 100 filas); «Descargar el informe» envía el mismo archivo, y su falla muestra el error. La SPA: 234 pruebas sin fallas, 93,8 % de líneas | `npx vitest run --coverage` | 2026-10-25 |
| En el navegador, con axe (Playwright) | `e2e/plantillas.spec.ts`, 6 de 6: la cédula sin el cero se ve con su sugerencia; el informe se descarga como `informe-docentes.csv` con sus columnas, con el token y la parte `archivo`; axe-core sin fallas WCAG 2.1 AA con la tabla de problemas a la vista | `npx playwright test e2e/plantillas.spec.ts` | 2026-10-25 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `probar_reglas_carga.sh` de punta a punta | 10 de 10 pasos: la asignatura que cita la carrera registrada es válida y la que cita una que no existe da el error en la fila 3 y en `codigo_carrera`; tres docentes ficticios válidos; la cédula sin el cero, la fecha imposible y la dedicación `XX`, 3 errores en su fila y su campo con la cédula sugerida; `descargarInforme` responde `text/csv`, `informe-docentes.csv` y `'=1+1`; el rechazado y después el corregido, desde cero; `consolidacion.demo` valida y `docente.demo` recibe 403 en las dos operaciones; nada guardado. Pasó en 5 corridas, dos de ellas seguidas al final, y la base quedó sin nada con el prefijo (0 carreras, 0 unidades, 0 registros). Ver la nota sobre la primera corrida | `deploy/probar_reglas_carga.sh`, perfil reconstruido con el builder `mdd-limpio` | 2026-10-25 |
| **Un archivo de casi 5 MB** (SC-004) | 5 241 703 bytes y 11 649 docentes en 285 a 763 ms por el gateway; la meta del plan es 10 s y la prueba falla desde 30 s | `deploy/probar_reglas_carga.sh`, paso 9 | 2026-10-25 |
| Los 14 scripts de punta a punta | Todos terminan en 0 con el perfil reconstruido, también `probar_subida.sh` (sus archivos de carreras ahora traen su informe, y la subida sigue respondiendo 200) | `deploy/probar_*.sh` | 2026-10-25 |
| Backend y SPA completos | Backend: 1303 pruebas sin fallas (`./gradlew build`, con Testcontainers); SPA: 234 pruebas de Vitest y 89 de 89 de Playwright | `./gradlew build`, `npx vitest run --coverage`, `npx playwright test` | 2026-10-25 |
| Sin datos en los logs (principio IX) | Ningún valor de prueba (las cédulas generadas, `NOEXISTE`, `=1+1`, `HYPERLINK`) aparece en los logs del gateway ni de `datos`: 0 coincidencias | `docker compose logs gateway datos \| grep -cE …` | 2026-10-25 |
| shellcheck y Gitleaks | shellcheck v0.11.0 sin observaciones en los 20 scripts, sobre una copia en el scratchpad; Gitleaks v8.30.1 sobre un clon de la rama: 62 commits, sin filtraciones | `koalaman/shellcheck-alpine:v0.11.0`, `ghcr.io/gitleaks/gitleaks:v8.30.1` | 2026-10-25 |
| El equipo de demostración con esta rama | `probar_equipo.sh` pasa todos sus pasos: despliegue, sin cambios, el token tras reiniciar identidad, el túnel, las claves, la vuelta atrás (126 s con `ESPERA_SALUD=60`) y el primer despliegue roto; 1372,1 MiB después de iniciar sesión (alerta: 7168) | `BUILDX_BUILDER=mdd-limpio deploy/equipo/probar_equipo.sh` | 2026-10-25 |
| Criterios de terminado de la constitución 2.2.0 (§6) | Los escenarios del issue pasan; el contrato está publicado y verificado; `ArquitecturaTest` y `ModulosTest` pasan; no hay códigos nuevos en el catálogo (un archivo con errores en sus filas responde 200, principio XIII) y los de estructura siguen cubiertos; las reglas de tipo, caracteres y largo de cada columna están probadas con casos válidos, inválidos y en el límite (principio XIV; ver la nota sobre la pantalla); cada motivo dice qué hacer; el JavaDoc de los puertos y del dominio está al día; SpotBugs en verde; el glosario no cambia | Esta evidencia | 2026-10-25 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El despliegue de `d360209` (PR #152) | El despliegue `demostracion` 7324088158 terminó en `success`, «Sano en 53 s»; el proyecto `mdd` corre desde `/opt/mdd/versiones/d360209c27dd/` con sus 8 contenedores sanos y las 6 imágenes de la aplicación con la etiqueta `…-d360209c27dd`; el código publicado (`index-Ce9xmrpC.js`) trae «Descargar el informe» | API de despliegues de GitHub, `docker ps` | 2026-10-26 |
| Una referencia que no existe, con `datos.demo` | `02_carreras.csv` de prueba, con la sede `MATRIZ`: rechazado con 7 errores en `codigo_unidad_sede` («No existe la unidad en el sistema»), porque la sede matriz del equipo se llama `001`. Con `001`: «02_carreras.csv es válido: 7 registros leídos.» | Capturas del autor | 2026-10-26 |
| 300 problemas y el límite de 100 en pantalla | `04_asignaturas_malla.csv`: rechazado con 300 errores en `codigo_carrera`, porque sus carreras no están registradas en el equipo; la pantalla dice «Se muestran 100 de 300; el informe los trae todos.» | Captura del autor | 2026-10-26 |
| **La cédula sin el cero inicial** (escenario del issue) | `06_docentes.csv` editado: las cédulas de 9 dígitos de las filas 3 y 4 dan «Cédula inválida o sin cero inicial: le falta el 0 inicial.» con su cédula sugerida; una de 8 dígitos en la fila 2, el otro motivo y sin sugerencia; la dedicación `XX`, los valores permitidos; `campo_amplio_3n` y `campo_especifico_3n` sin sus ceros (Calc los quitó al guardar) dan «2 dígitos» y «Escriba exactamente 3 dígitos.»: 189 errores, «Se muestran 100 de 189» | Captura del autor | 2026-10-26 |
| Corregir y reintentar | El archivo corregido: solo quedan los 47 errores de `codigo_carrera_base`, uno por docente; los de las cédulas desaparecen | Captura del autor | 2026-10-26 |

## Notas

- **La carga 07 de prueba.** La fila 22 (perfil `DOCENTE_INVESTIGACION_VINCULACION`) dejaba vacías `horas_otras_docencia` y `horas_gestion`, que el diccionario marca obligatorias. Por decisión del autor (análisis, 2026-10-25) se completaron con 0 y 0, que salen de la dedicación de 40 h (16 + 0 + 0 + 24). La copia de la ERS (`requerimientos_mdd/cargas/datos_prueba/`) sigue con las celdas vacías, y `V2__politica_2024.sql` de Normativa guarda `null` en esas dos columnas: no se tocaron.
- **Los formatos de texto.** Cuatro formatos de texto del diccionario definen caracteres («Mayúsculas», «Mayúsculas, dígitos, «-» o «_»», «AAAA-I o AAAA-II» y «usuario@dominio») y se validan (principio XIV). Un nombre en minúsculas es un error, por decisión del autor (implementación, 2026-10-25).
- **La primera corrida de `probar_reglas_carga.sh`.** Justo después de levantar el perfil, los pasos 2 a 6 fallaron y el 9 dio 413: el archivo grande pasaba de 5 MiB, porque el script contaba caracteres y no bytes de UTF-8 (corregido). De los pasos 2 a 6 no hay diagnóstico: el script aún no imprimía la respuesta. Desde entonces imprime el estado y el cuerpo de cada falla, y no se repitió en 4 corridas más, una de ellas con `datos` recién reiniciado; los logs del gateway y de `datos` no tienen errores de ese momento.
- **La validación en la pantalla (principio XIV).** Lo que el usuario ingresa es un archivo: la pantalla revisa su tamaño (5 MB) y su extensión (`.csv`, `.xlsx`), y las reglas de las filas viven solo en el servidor, en un solo intérprete del diccionario (FR-012), para no repetirlas en dos lugares. El autor estuvo de acuerdo (2026-10-25).
- **Cargar en orden, antes de F01-H04.** Validar un archivo no guarda nada (FR-008): las unidades de la carga 01 y las carreras de la 02 no existen en el sistema hasta que F01-H04 confirme la carga. Por eso, en el equipo, las cargas 02, 04 y 06 de prueba dan referencias que no existen, salvo lo registrado a mano. Es lo que pide la aclaración 2; el motivo «regístrela o cárguela antes» se precisará en F01-H04, donde cargar sí guarda.
- **Calc y los ceros iniciales.** Al abrir un CSV, Calc convierte a número los códigos como `001`, `04` o `041` y les quita los ceros. Hay que importar las columnas como «Texto» o editar el CSV en un editor de texto. Las reglas de dígitos del diccionario lo detectan.
