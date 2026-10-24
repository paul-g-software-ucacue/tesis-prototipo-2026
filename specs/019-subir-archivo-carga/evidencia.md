# Evidencia: F01-H02

Registro de las comprobaciones de la historia (issue #28): subir el CSV o XLSX de una carga y validar su estructura, con D-71 (RF-06, RF-07, RF-55, RF-57). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: plataforma, D-71, contrato y dominio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T001 · Apache POI 5.5.1 | ✅ `poi-ooxml` trae `poi`, `poi-ooxml-lite`, `xmlbeans` 5.3.0, `commons-compress` 1.28.0, `commons-io` 2.21.0, `commons-collections4` 4.5.0, `commons-math3` 3.6.1, `SparseBitSet` 1.3, `curvesapi` 1.08 y `log4j-api` (2.25.5, que Spring Boot envía a SLF4J) | `./gradlew :servicios:cargas:dependencies` | 2026-10-23 |
| T002 · `EntradaCatalogo.conDetalles` y `COMUN_CONTENIDO_DEMASIADO_GRANDE` | ✅ 2 pruebas nuevas con un servidor real: una entrada con detalles responde su código y sus detalles; un `MaxUploadSizeExceededException` responde 413 con su código común (antes, `COMUN_SOLICITUD_INVALIDA`). Mutación: sin el caso 413 en `segunEstado`, la prueba falla; restaurado | `ManejadorDeErroresTest`, `RespuestaErrorTest` | 2026-10-23 |
| T003 · D-71 en Roles (`V7`) | ✅ `MatrizD16IntegrationTest` falló antes de la migración y pasa después: el Responsable de consolidación y el Vicerrector tienen `CARGAS:LECTURA` y `CARGAS:ESCRITURA`. `AutorizacionServiciosTest` y `AutorizacionEnMemoria` no fijan esos permisos: sin cambios | `MatrizD16IntegrationTest` | 2026-10-23 |
| T004 · Contrato: `validarArchivo`, 2 esquemas, 7 códigos `CARGAS_*` | ✅ Generada `SubidaApi` (`@RequestPart MultipartFile archivo`) y `cargas.d.ts`; `verificar:ruptura` en verde | `contratos/cargas/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-23 |
| T005 · Dominio, con las pruebas primero | ✅ 12 pruebas, sin dobles, que no compilaban antes: filas vacías al final y en medio, encabezados iguales con espacios, `FALTA`, `SOBRA`, `REPETIDA`, `FUERA_DE_LUGAR` con su posición, `leer`. Un archivo totalmente vacío da `ArchivoSinRegistros` y no «encabezados distintos» (agregado con su prueba, que falló primero) | `TablaDeDatosTest` (4), `EncabezadosTest` (8) | 2026-10-23 |

## Archivo válido (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T006 · Datos de prueba de la ERS | ✅ Los 10 CSV de `MDD_artefactos_desarrollo/requerimientos_mdd/cargas/datos_prueba/`, sintéticos (RNF-SEG-06), copiados a `CAR/test/resources/cargas/datos-de-prueba/`. Tienen CRLF y no tienen BOM | `datos-de-prueba/` | 2026-10-23 |
| T006 · `LectorCsv` | ✅ Los 10 archivos se leen con los encabezados de su plantilla y su número de registros (24, 7, 2, 300, 165, 47, 22, 21, 11 y 7); LF y CRLF, con BOM, dan lo mismo; `;` se detecta; comillas con coma, comillas duplicadas y salto de línea; líneas vacías y de solo separadores son filas vacías. Una primera versión de la prueba convertía LF a CRLF sobre un archivo que ya tenía CRLF; se corrigió la prueba, no el lector | `LectorCsvTest` | 2026-10-23 |
| T007 · `LectorXlsx` | ✅ 3 pruebas: los 10 archivos convertidos a XLSX dan la misma tabla que el CSV; una cédula de diez dígitos con formato `0000000000` conserva el 0, una fecha sale `2026-04-06` y un decimal `2.5`; filas con sus últimas celdas vacías tienen el ancho de los encabezados | `LectorXlsxTest` | 2026-10-23 |
| T008 · `LectorPorFormato` | ✅ `.csv`, `.CSV` y `.xlsx` eligen su lector | `LectorPorFormatoTest` | 2026-10-23 |
| T009 · `ValidarArchivoServicio` con Mockito | ✅ 5 pruebas: lectura válida con el nombre saneado (sin ruta, hasta 255 con su extensión); carga inexistente y más de 5 MB sin leer; los errores pasan tal cual | `ValidarArchivoServicioTest` | 2026-10-23 |
| T010 · Contrato de la subida | ✅ 5 pruebas: el CSV de docentes, 200 con 47; el de matriculados en XLSX, 200 con 7; sin la parte `archivo`, 400; carga inexistente, 404. `ErroresCargasTest` contra `errores.md`. **El de 300 filas (`04_asignaturas_malla.csv`) se valida en unos 30 ms**, frente a la meta de 1 s | `SubidaContratoTest`, `ErroresCargasTest` | 2026-10-23 |
| T011 · Permisos | ✅ `validarArchivo` exige `CARGAS:ESCRITURA`. `VerificadorDePermisos` mandaba los cuerpos en JSON y la operación respondía 415: ahora manda un archivo en cada parte de un cuerpo `multipart/form-data`, con su prueba primero (falló antes del cambio) | `PermisosCargasTest`, `VerificadorDePermisosTest` | 2026-10-23 |
| T011 · Límites | ✅ `spring.servlet.multipart` a 10 MB en `datos` y `client_max_body_size 10m` en `location /api/` de nginx | `application.yaml`, `frontend/nginx.conf` | 2026-10-23 |

## Archivo que no se puede leer (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T012 · Rechazos en los lectores | ✅ Fallaron antes de las firmas: un `.csv` que es PDF, ZIP, OLE2 o tiene un byte nulo, y un `.xlsx` que es texto o un XLS antiguo, dan `FormatoNoAdmitido`; un XLSX cifrado, `ArchivoIlegible` («protegido con contraseña»), y uno dañado, «dañado». Pasaron a la primera, porque llegaron con T006 y T007: Windows-1252, comillas sin cerrar y filas de otro ancho (con su fila), dos hojas, un valor fuera de las columnas, `.pdf` o sin extensión. Mutación: sin la regla del ancho, su prueba falla; restaurado | `LectorCsvTest`, `LectorXlsxTest`, `LectorPorFormatoTest` | 2026-10-23 |
| T013 · Rechazos por la API | ✅ Cada caso con su estado y su código, validados contra el contrato; el detalle de un archivo ilegible dice la fila; uno de carreras sin `codigo_carrera` y con `extra` trae `codigo_carrera: Falta en el archivo.` y `extra: No está en la plantilla.`; **la cédula ficticia `0999999999` no aparece ni en la respuesta ni en la salida del proceso**. Pasaron a la primera porque el catálogo llegó con T010. Mutación: sin los detalles de encabezados, la prueba falla; restaurado | `SubidaContratoTest` (9) | 2026-10-23 |
| Cargas: todas las pruebas del módulo | ✅ 96 pruebas, 0 fallas; ArchUnit, Spotless y SpotBugs en verde | `./gradlew :servicios:cargas:check` | 2026-10-23 |

## Pantalla (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| T014 · «Subir archivo» con Vitest | ✅ 4 pruebas nuevas (13 en la pantalla), que fallaron antes del botón: subir muestra «06_docentes.csv: 47 registros leídos.»; un rechazo muestra el título y «codigo_carrera: Falta en el archivo.»; más de 5 MB se rechaza sin enviar; sin `CARGAS:ESCRITURA` no hay botón. En jsdom, `FormData` y `File` son de jsdom y el `Request` de Node no los acepta, así que `subirArchivo` se simula en Vitest y el pedido real se prueba en Playwright | `Plantillas.test.tsx` | 2026-10-23 |
| T015 · Playwright y axe | ✅ 2 pruebas nuevas: el navegador manda `multipart/form-data` con la parte `archivo` (`filename="06_docentes.csv"`) y el token, y muestra los 47 registros; un archivo de carreras sin una columna muestra cuál falta; axe-core sin fallas WCAG 2.1 AA con el resultado y con el error a la vista | `e2e/plantillas.spec.ts` | 2026-10-23 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Perfil reconstruido con el código de la rama | ✅ Con el builder `docker-container` limpio (`mdd-limpio`), por la instantánea rota de BuildKit del daemon; `roles` en V7 | `docker compose build --builder mdd-limpio` | 2026-10-23 |
| T016 · `probar_subida.sh`, dos corridas seguidas | ✅ 10 de 10 pasos en cada una, a la primera: el CSV y el XLSX de carreras, 2 registros; uno de 22 000 registros (unos 2 MB) por el frontend (nginx); uno con punto y coma; sin `codigo_carrera`, 400 con su columna; columna de más, Windows-1252, sin registros, PDF, 6 MB (413 `CARGAS_ARCHIVO_DEMASIADO_GRANDE`) y 11 MB (413 `COMUN_CONTENIDO_DEMASIADO_GRANDE`); `consolidacion.demo` y `vicerrectorado.demo` suben (D-71); `coordinacion.demo` y `docente.demo`, 403; una carga inexistente, 404 | salida del script | 2026-10-23 |
| `shellcheck` del script | ✅ Sin observaciones | `koalaman/shellcheck:stable` | 2026-10-23 |
| T017 · Backend: `./gradlew check integrationTest` | ✅ 845 pruebas (748 unitarias y 97 de integración), 0 fallas. La primera corrida falló en `spotlessJavaCheck` de Roles (el formato de `MatrizD16IntegrationTest`); corregido con `spotlessApply` | `backend/**/build/test-results` | 2026-10-23 |
| T017 · SPA | ✅ lint, formato, tipos, `verificar:ruptura` y 67 de Playwright. Vitest: 146 pruebas, 95,1 % de líneas, con `--testTimeout=15000` desde la línea de comandos por la carga del PC (como en F01-H01) | `npx vitest run --coverage` | 2026-10-23 |
| T017 · Los 9 scripts del perfil | ✅ autenticacion, errores, eventos, normativa, periodos, spa, estructura, plantillas y subida: todos los pasos pasan | `deploy/probar_*.sh` | 2026-10-23 |
| T017 · `probar_equipo.sh` | ✅ 0 a 11: todos pasan, a la primera; memoria 1 381,7 MiB (F01-H01: 1 365,4 MiB; POI suma unos 16 MiB). Con `BUILDX_BUILDER=mdd-limpio`; el builder se borró después | `deploy/equipo/probar_equipo.sh` | 2026-10-23 |
| T017 · Gitleaks sobre un clon en el scratchpad | ✅ 13 commits de la rama, sin fugas | gitleaks v8.30.1, `--log-opts=origin/main..HEAD` | 2026-10-23 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
