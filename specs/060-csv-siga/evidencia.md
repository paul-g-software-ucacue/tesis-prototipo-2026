# Evidencia: Generar el CSV para SIGA con el layout configurable

F19-H01 (#266). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-22, sobre la rama `060-csv-siga`, antes del pull request.

## Pruebas y comprobaciones (T035)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Build completo del backend | 4 078 pruebas unitarias y de integración, 0 fallas. La primera corrida falló solo en `:apps:salida:spotlessJavaCheck` (el formato de `RegistroEnBitacoraIntegrationTest`); con `spotlessApply`, `:apps:salida:check` en verde | `./gradlew build integrationTest jacocoTestReport --continue` |
| Cobertura (60 %, D-32) | Reporting 93,7 % de instrucciones (93,6 % de líneas); Generación 87,6 % (86,2 %); Docentes 82,5 % (82,1 %) | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde. `ArquitecturaTest` de Reporting suma la regla «SnakeYAML solo en adaptadores»; `ApplicationModules.verify()` y ArchUnit confirman que Reporting no depende de clases de Generación ni de Aprobación (FR-022) | `build` |
| Docentes | `InternoDocentesContratoTest`: cada activo trae `tipoIdentificacion`. El repositorio ya cargaba el tipo en `Identificacion`, así que solo cambió el mapeo | `:servicios:docentes:test` |
| Generación (dominio central, TDD) | <br>• `ContenidoCongeladoTest`: el tipo del docente activo pasa a la carga congelada; uno que no está activo queda sin tipo. <br>• `DatosDesdeProcesoDatosTest`: el cliente lee el tipo, y un activo sin tipo falla. <br>• `ContenidoCongeladoIntegrationTest` (PostgreSQL real): la clave `tipoIdentificacion` vuelve igual, falta en un contenido sin ella, y `cargas(periodo, numero)` da las mismas cargas que de a una. <br>• `InternoGeneracionContratoTest`: `consultarContenidoCongelado` (200 y 404) y el tipo en `DatosDelDocenteCongelado` | `:servicios:generacion:test` e `integrationTest` |
| Dominio de Reporting | <br>• `LayoutExportacionTest` (11) y `ColumnaDelLayoutTest` (6): cada regla de FR-015, todos los problemas juntos, el tope de 50, 30 y 31 columnas, 64 y 65 caracteres, las obligatorias, la traducción incompleta, con un código de más, `NA` y repetida. <br>• `FilasParaSigaTest` (9): el escenario 2 de la US1, el orden, dos asignaciones de la misma actividad, totales distintos, un docente que falta o sobra, el hash, el repetido, los 47 docentes de prueba sin redondear y el valor tipado de los 15 campos. <br>• `CeldaCsvTest` (2); `EscritorCsvTest` sin cambios | `:servicios:reporting:test` |
| Aplicación, adaptadores y contrato de Reporting | <br>• `GenerarCsvSigaServicioTest` (8): una sola lectura de la publicación y del layout, sin publicación, sin contenido, el registro antes de entregar, la exportación que falla, la reforma y 300 docentes. <br>• `GestionarLayoutSigaServicioTest` (7): el vigente por defecto y cargado, la plantilla, la carga en una transacción, el archivo inválido o ilegible sin guardar nada. <br>• `LectorDeLayoutYamlTest` (7), `PlantillaDelLayoutTest` (1), `ExportadorSigaCsvTest` (8), `ClientesDeReportingTest` (+3), `RegistroEnBitacoraEventosTest` (+2). <br>• `LayoutsGuardadosJdbcIntegrationTest` (2, PostgreSQL real): las versiones, el vigente y el disparador contra `update` y `delete`. <br>• `RegistroEnBitacoraIntegrationTest` (+2): `CSV_SIGA_GENERADO` y `LAYOUT_SIGA_CARGADO` en `eventos_salida`. <br>• `SigaContratoTest` (7) y `ErroresReportingTest` (2, nueva): las cuatro operaciones, 409, 500 sin datos técnicos, 502, 504, 400 con sus tres códigos, 413 y 403 sin `PUBLICACION:APROBACION` | El mismo |
| SPA: tipos, ESLint y Prettier | En verde | `node:24-bookworm-slim` |
| SPA: Vitest | `Siga.test.tsx` (11) y `Marco.test.tsx` (27, +5): los estados, el layout por defecto y cargado, la reforma, la descarga del CSV y de la plantilla, el 409, la carga válida, los problemas con el foco en su título, el archivo de más de 256 KB sin enviarse, las versiones anteriores; el menú solo para `SECRETARIA_GENERAL` | `vitest run` |
| SPA: Playwright | `siga.spec.ts`: 4 de 4 en Chromium. La Secretaría genera el CSV y descarga la plantilla; carga un layout con problemas y uno válido; las versiones anteriores; el Rector sin la opción; axe-core sin fallas en los tres estados (SC-010) | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Gitleaks | Sin hallazgos en los archivos cambiados, con `.gitleaks.toml` | `ghcr.io/gitleaks/gitleaks:v8.30.1 dir` |

## El script

Sobre un perfil construido desde la rama: `docker compose build datos distributivo salida frontend` y `docker compose up -d --wait`, con los siete servicios sanos. La base del perfil tenía datos de una corrida anterior; con la aprobación del autor se borró su volumen (`docker compose down -v`) antes de correrlo.

### `probar_documento_firmable.sh`: 38 de 38, del 0 al 27 y de R1 a R10, a la primera

| Paso | Resultado |
|---|---|
| R1 | En EN_OCS, el CSV para SIGA responde 409 `REPORTING_SIN_PUBLICACION`, sin `CSV_SIGA_GENERADO` |
| R9 | Con la plantilla cargada como versión 1 del layout de exportación: `siga-2026-I-v1.csv`, `no-store`, sin BOM y con CRLF; 165 filas, una por cada asignatura y actividad de los 46 reportes individuales (SC-001); las sumas por docente y componente, y por componente, iguales al general (SC-002); las convenciones de SENESCYT (SC-003); dos archivos idénticos (SC-006); 2 entradas `CSV_SIGA_GENERADO` con la versión 1 y el layout |
| R10 | La plantilla descargada es el recurso; el layout nuevo (dos nombres, la asignatura y el paralelo invertidos, sin la fecha, el componente en 01…05) da la versión 2 y `LAYOUT_SIGA_CARGADO`; uno inválido da 400 `REPORTING_LAYOUT_INVALIDO` con 2 detalles y el vigente no cambia (SC-005); el CSV sale con las columnas nuevas y los mismos totales (SC-004). Al terminar, la plantilla otra vez |
| R4 | `rectorado.demo`, `coordinacion.demo` y `admin.demo` reciben 403 en el CSV y al cargar el layout, sin layout nuevo ni entradas (SC-009) |
| R5 | Durante la reforma, el CSV para SIGA es idéntico al de R9 |
| Los demás pasos | Sin cambios: OK |

Una segunda corrida con `MDD_CONSERVAR=PUBLICADO` pasó 26 de 26 y dejó la versión 1 de 2026-I publicada, para el recorrido del autor.

## Las mediciones (T037, SC-008)

| Qué | Resultado |
|---|---|
| 300 docentes, en `GenerarCsvSigaServicioTest` (los puertos de lectura simulados y `ExportadorSigaCsv` real) | 55 ms |
| De punta a punta en el perfil local, 46 docentes, por el API Gateway (`curl -w '%{time_total}'`, cinco pedidos): las tres lecturas internas, la comprobación, el archivo y el registro en la bitácora | 42 a 65 ms |
| La Secretaría, desde que abre la pantalla hasta tener el archivo (menos de 1 minuto) | Pendiente: el recorrido del autor (T038) |

SC-008 (menos de 10 s con 300 docentes) se cumple con amplio margen.

## Las desviaciones

- **Las convenciones de SENESCYT (T027):** las pruebas pasaron sin verse fallar, porque el adaptador `ExportadorSigaCsv` ya las aplicaba desde T021. Se escribieron después del código, en contra del orden del principio VIII.
- **La pantalla (T025, T032):** las pruebas de Vitest y Playwright se escribieron junto con el componente y pasaron a la primera, sin verse fallar antes.
- **El orden de US3:** la interfaz generada `SigaApi` exige las cuatro operaciones en el mismo controlador, así que el servicio y la bitácora del layout (T029, T030) se hicieron antes de cerrar el controlador (T024, T031).
- **La carga del archivo en la pantalla:** sigue el patrón de «Flujo y firmas» (un botón que abre el selector y carga al elegir), sin un segundo botón «Cargar»; `contracts/spa.md` quedó ajustado.
- **El glosario (T003):** el `.md` está actualizado; el `.docx` queda pendiente, porque en el equipo no hay con qué editarlo.

## Los criterios de terminado

| Criterio | Estado |
|---|---|
| SC-001 a SC-007, SC-009 y SC-010 | Cumplidos: pruebas automáticas y R9, R10, R4 y R5 |
| SC-008 | Cumplido en la generación; falta medir el recorrido de la Secretaría (T038) |
| El recorrido del autor (`quickstart.md` §1 a §3, teclado, lector de pantalla y LibreOffice Calc) | Pendiente (T038) |
| El equipo de demostración | Pendiente: después de fusionar |
