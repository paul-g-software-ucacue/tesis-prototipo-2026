# Evidencia: Reporte general consolidado en PDF y CSV

F20-H02 (#269). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-21 y el 2026-11-22, sobre la rama `059-reporte-general-consolidado`, antes del pull request.

## Pruebas y comprobaciones (T025)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Build completo del backend | 3 543 pruebas unitarias y 384 de integración, 0 fallas en los resultados finales | `./gradlew build integrationTest jacocoTestReport --continue` |
| Fallas en la corrida completa | 2, en módulos que esta rama no toca, con el perfil y el script corriendo en la misma máquina: <br>• `ClienteDeEventosLocalTest` (`plataforma:eventos`): `HttpTimeoutException` al pedir el token a un servidor HTTP local de la prueba; <br>• la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (`datos`), la misma de F20-H01, F18-H02 y F12-H03. <br>Repetidas solas, pasan las dos | `:plataforma:eventos:test` y `:apps:datos:integrationTest --tests '*ConfirmarLasDiezCargas*'` |
| Cobertura (60 %, D-32) | Reporting 96,7 % de instrucciones (96,5 % de líneas); Generación 88,0 % (86,6 %) | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde, con `ArquitecturaTest` de Reporting | `build` |
| Generación (dominio central, TDD) | <br>• `DocenteDeLaVersionTest` (3, nueva): la dedicación y las horas de su carga congelada, sin dedicación, y sin horas se rechaza. <br>• `ContenidoCongeladoIntegrationTest` (+2 comprobaciones, PostgreSQL real): cada docente del listado trae la dedicación y las horas de su propia carga. <br>• `InternoGeneracionContratoTest`: `DocenteCongelado` con `dedicacion` y `horas`, y uno sin dedicación. <br>Las tres se vieron fallar antes del código | `:servicios:generacion:test` e `integrationTest` |
| Dominio de Reporting | <br>• `ReporteGeneralTest` (10, nueva): el orden alfabético del español, los totales exactos, el total general, un componente en 0, los decimales sin redondear, el hash distinto, el docente repetido, la versión vacía, sin dedicación, y totales que no suman. <br>• `HorasPorComponenteTest` (3, nueva), `DescargaDeReporteTest` (4, nueva). <br>• `EscritorCsvTest` (+9): las 14 columnas, la fila `TOTAL`, «sin dato», los seis caracteres de fórmula, los bytes idénticos y 300 docentes | `:servicios:reporting:test` |
| Aplicación, adaptadores y contrato de Reporting | <br>• `ConsultarReportesServicioTest` (+3) y `DescargarReporteServicioTest` (+5): la reforma, sin publicación, el contenido inconsistente, y el registro antes de entregar. <br>• `GeneradorDeReportePdfaTest` (+1): 300 docentes, veraPDF 2B, horizontal, sin `AcroForm` ni anotaciones ni cifrado, el pie y el encabezado en cada página, y «Total institucional» una sola vez en la última. <br>• `RegistroDeDescargasEventosTest` (2, nueva) y `RegistroDeDescargasIntegrationTest` (+1, PostgreSQL real): `REPORTE_GENERAL_DESCARGADO` sin identificación. <br>• `ClientesDeReportingTest` (+1): un docente sin horas es un 502. <br>• `ReportingContratoTest` (+2) y `DescargasContratoTest` (+2): las tres operaciones, sus encabezados, 403, 409, 500 sin datos técnicos, 502 y 400 | El mismo |
| SPA: tipos, ESLint y Prettier | En verde | `node:24-bookworm-slim` |
| SPA: Vitest | 681 pruebas, 0 fallas. `Reportes.test.tsx` (12): «General» propuesto, la tabla con su total institucional, sin publicaciones, la reforma, el foco en el buscador al pasar a «Por docente», el general sin volver a pedirse, y las descargas del general y su error. La primera corrida completa dio 19 fallas en pantallas ajenas, con Gradle y el build de Docker en paralelo; sola, 681 de 681 | `vitest run` |
| SPA: Playwright | `reportes.spec.ts`: 6 de 6 en Chromium. El Rector ve el general y lo descarga; el individual con «Por docente»; solo con el teclado (Tab al grupo, flecha a «Por docente», el foco en el buscador); el Coordinador sin acceso; axe-core sin fallas con «General» y con «Por docente» (SC-009) | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` | `koalaman/shellcheck:v0.10.0` |
| Gitleaks | Sin hallazgos en los archivos cambiados, con `.gitleaks.toml` | `ghcr.io/gitleaks/gitleaks:v8.30.1 dir` |

## El script (T015, T024)

Sobre un perfil construido desde la rama: `docker compose up -d --build --wait`, con los siete servicios sanos.

### `probar_documento_firmable.sh`: 36 de 36, del 0 al 27 y de R1 a R8

La primera corrida dio 34 de 36: R7 y R6 fallaron por dos errores del propio script, no del sistema. Se corrigieron y la segunda corrida pasó completa.
- **R7:** el lado derecho de `[[ … == $(jq …) ]]` iba sin comillas, y bash leía los `[` del JSON como patrón.
- **R6 y R7:** dentro de `map(. as $k | …)`, el punto era el nombre del componente y no el reporte.

| Paso | Resultado |
|---|---|
| R1 | En EN_OCS, el individual y el general responden 409 `REPORTING_SIN_PUBLICACION`, sin entradas en la bitácora |
| R7 | 46 docentes, los del listado y sin repetidos; cada fila es igual a su reporte individual; los totales son la suma de las filas; total institucional 660,00 h; pedir el JSON no dejó entrada |
| R8 | El PDF del general es PDF/A sin campos (veraPDF), con su nombre y `no-store`; el CSV es igual al JSON e idéntico entre `rectorado.demo` y `secretaria.demo`; 4 entradas `REPORTE_GENERAL_DESCARGADO`, 2 con `CSV`, sin identificación |
| R4 | `coordinacion.demo` y `docente.demo` reciben 403 en el general y en su PDF; un periodo de más de 20 caracteres, 400 |
| R5 | Durante la reforma, con la hora de un paralelo bajada, el CSV del general es idéntico al de R8 |
| R6 | Con la versión 2 publicada, el general sale de ella, con su hash, y sus totales son la suma de sus filas |
| R2, R3 y los pasos 0 a 27 | Sin cambios: OK |

## Mediciones (T027)

| Medición | Resultado |
|---|---|
| SC-007, con 300 docentes | El PDF/A, 604 ms (`GeneradorDeReportePdfaTest`); el CSV, 10 ms (`EscritorCsvTest`) |
| SC-007, de punta a punta en el perfil local | Con los 46 docentes de 2026-I: el PDF entre 0,15 y 0,30 s (45 079 bytes) y el CSV entre 0,04 y 0,05 s (9 513 bytes), en tres vueltas; el JSON, 0,02 s. En frío, la primera descarga después de reiniciar `salida`: 1,41 s |
| Conclusión de SC-007 | El costo fijo de una descarga (las dos lecturas internas, el registro y la red) queda por debajo de 0,3 s, y generar 300 docentes suma 0,6 s. Las dos partes juntas quedan muy por debajo de 10 s. Cumple |
| El PDF real | veraPDF 1.30.2: `isCompliant="true"`, sin campos de formulario. 2 páginas A4 horizontales |

**El PDF real se revisó a la vista,** con las páginas convertidas en imágenes. La primera versión partía «Investigación» a mitad de palabra; la segunda, «Tiempo completo». Se ajustaron los anchos (Dedicación 12 %, cada columna de horas 8,83 %) y la letra de los encabezados de horas (7,5 pt), y se volvió a revisar: nada se parte y el documento ocupa 2 páginas.

## El recorrido del autor (T028)

Hecho por el autor el 2026-11-22 en el perfil local (`http://localhost:8081`), con 2026-I publicado (`MDD_CONSERVAR=PUBLICADO`):

| Paso | Resultado |
|---|---|
| El reporte general en pantalla | OK: `rectorado.demo` ve 2026-I, versión 1, «General» propuesto, «46 docentes», la tabla en orden alfabético y la fila «Total institucional» |
| Contra el individual | OK: PERALTA MALDONADO JUAN ANDRES tiene 22,00 de clase y 22,00 de total, en «Por docente» y en su fila del general |
| El PDF | OK: horizontal, 2 páginas, el encabezado de la tabla repetido, nada partido, el pie con la versión, el hash y «Página X de 2», y «Total institucional» con 660,00 una sola vez. veraPDF: `isCompliant="true"` |
| El CSV | OK: el modal «CSV descargado» aparece; la hoja de cálculo lo abre en columnas, con 46 filas `DOCENTE` y la fila `TOTAL` («Total institucional», 660,00), con los mismos valores del PDF. La primera vez se abrió sin importar «Identificación» como texto, y la cédula se vio sin el cero inicial (`109990341`), como advierte el modal (D-115). Importada como texto en LibreOffice Calc («Importar texto»: UTF-8, Español (Ecuador), punto y coma, la columna «Identificación» como «Texto»), conserva el cero (`0109990341`) |
| La bitácora | OK: con el servicio Reporting, `REPORTE_GENERAL_DESCARGADO` de `rectorado.demo`, con el objeto `periodo: 2026-I · version: 1`, sin identificación, y el formato `PDF` o `CSV`. Las demás entradas son de las mediciones (T027) y del script |
| Sin permiso | OK: `coordinacion.demo` no ve «Reportes», y `/reportes` muestra «Acceso no autorizado» |
| La reforma | La cubre el paso R5 del script |

**Observación ajena a esta historia:** en «Resolución del OCS», «Descargar la resolución» abre un visor que dice «Failed to load PDF document». No es un error: el archivo es el de prueba que registra el script (56 bytes, `%PDF-1.7` y una línea de texto, sin páginas), con la misma huella `6b3c8418d948…` que muestra la pantalla. La descarga entrega exactamente lo que se registró.

**El lector de pantalla** (SC-009): el autor lo recorrió y funciona bien.

**Ajuste después del recorrido:** en la tabla de la pantalla, el encabezado «Otras actividades de docencia» en una sola línea dejaba poco ancho a los nombres, que ocupaban tres renglones. Los encabezados de horas ahora se parten en varias líneas, como en el PDF; las cifras y la dedicación no se parten, y la columna Docente tiene un ancho mínimo. `Reportes.test.tsx` (12 de 12) y `reportes.spec.ts` con axe-core (6 de 6) siguen en verde.

**Pendiente del autor:** comparar los totales con los del PDF/A del consolidado firmado (Riesgos del plan).

## Desviaciones del plan

1. **El orden de los docentes es el alfabético del español** (`Collator` es-EC, `ReporteGeneral.porNombre()`), no el de `String.compareTo`, que ponía «ÁLVAREZ» después de «PERALTA». Lo encontró `EscritorCsvTest`. Por eso R7 compara los docentes del general con los del listado como conjuntos: el listado sale con el orden de la base. Está en el contrato, en `data-model.md` y en `contracts/demostracion.md`.
2. **«Total institucional» con fondo gris y no en negrita:** solo la variante regular de DejaVu Sans está incrustada, y una negrita sin su fuente no sería PDF/A. Es el mismo estilo del total del individual. Está en `research.md` §3 y en `contracts/reporting.md`.
3. **`DescargaDeReporte.nombreDelArchivo()`:** el nombre del archivo sale del tipo de reporte, también en el individual, que antes lo armaba el servicio. No cambia el nombre.
4. **Las operaciones nuevas tuvieron una respuesta 501 provisional** mientras se escribían el dominio y la aplicación, porque el contrato generado obligaba a implementarlas para compilar. Las reemplazaron T013 y T021; no queda ninguna.
5. **T010 no se vio fallar antes del código:** el cliente de Generación y su prueba se escribieron juntos. Reporting es un servicio de soporte, no del dominio central.
6. **`TipoDeReporte.accion()` está en el dominio,** como dice `data-model.md`. La constante `RegistroDeDescargasEventos.ACCION` se quitó.

## Criterios de terminado

| Criterio | Estado |
|---|---|
| Los dos escenarios del issue | Cumplidos: R7 y R8 en el perfil real, y las pruebas de dominio, contrato y SPA |
| SC-001 a SC-006 y SC-008 | Cumplidos: R7, R8, R4, `ReporteGeneralTest`, `EscritorCsvTest` y `GeneradorDeReportePdfaTest` |
| SC-007 | Cumplido en la medición; falta el minuto de punta a punta en el recorrido del autor |
| SC-009 | Cumplido: axe-core sin fallas en Playwright, con los dos tipos de reporte, y el recorrido del autor con lector de pantalla |
| D-117 registrada | Sí |
| Horas estimadas de #269 | Confirmadas por el autor el 2026-11-21 (el token de `gh` no tiene `read:project`) |

## El equipo de demostración

Queda para después de fusionar.
