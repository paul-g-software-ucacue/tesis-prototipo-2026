# Evidencia: Reporte individual del docente en PDF y CSV

F20-H01 (#268). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-21, sobre la rama `058-reporte-individual-docente`, antes del pull request.

## Pruebas y comprobaciones (T029)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 558 pruebas, 0 fallas | `./gradlew build integrationTest --continue` |
| `integrationTest` completo | 383 pruebas, 0 fallas en los resultados finales. <br>• Generación: V10 (`GeneracionEsquemaIntegrationTest` +1: no se modifica y se borra con su versión); `ContenidoCongeladoIntegrationTest` (3, nueva): un solo guardado, vuelve igual, sin dedicación y la versión 1 con la 2 vigente. <br>• Validación OCS: `conPublicacion` (`RepositorioProcesoIntegrationTest` +1). <br>• Usuarios: V8, una entrada de `reporting` (`RepositorioBitacoraIntegrationTest` +1). <br>• Proceso `distributivo`: la orden de guardar el contenido llega a Generación después de confirmar el paso a firma, y no si se revierte (`DocumentoFirmableIntegrationTest` +1). <br>• Proceso `salida`: `eventos_salida.event_publication` (`EsquemasIntegrationTest`) y `RegistroDeDescargasIntegrationTest` (2, nueva): el evento en la transacción, y nada si se revierte. | El mismo |
| Fallas en la corrida completa | 1, en un módulo que esta rama no toca: la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (`datos`), la misma de F18-H02 y F12-H03. Repetida sola, pasa | `:apps:datos:integrationTest --tests '*ConfirmarLasDiezCargas*'` |
| Cobertura (60 % de instrucciones, D-32) | Reporting 93,0 %, Generación 91,6 %, Validación OCS 85,9 % y Usuarios 85,1 % | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde, con ArchUnit (las cinco capas de Reporting) y Spring Modulith (`ModulosTest` de `salida`) | `build` |
| Dominio de Reporting | <br>• `ReporteIndividualTest` (6, nueva): el orden, las sumas por componente, el hash distinto, filas que no suman su carga, un docente sin asignaciones con los cinco componentes en 0, y las horas con más de dos decimales o negativas. <br>• `EscritorCsvTest` (10, nueva): el BOM, `;`, CRLF y las seis filas de totales; los seis caracteres de fórmula; las comillas duplicadas; los mismos bytes para la misma versión pedida por otra persona; «sin dato». | `:servicios:reporting:test` |
| Aplicación, adaptadores y contrato de Reporting | <br>• `ConsultarReportesServicioTest` (6) y `DescargarReporteServicioTest` (5): la publicación más reciente primero, la reforma, el 404 frente a una versión sin contenido, y el registro dentro de la transacción después de generar el archivo; sin registro si la generación falla, y sin archivo si el registro falla. <br>• `GeneradorDeReportePdfaTest` (3): veraPDF 2B conforme, sin `AcroForm` ni anotaciones ni cifrado, el pie con la versión y «Página X de Y» en cada página de un docente con 40 filas, y el encabezado de la tabla repetido. <br>• `ClientesDeReportingTest` (5): las dos rutas internas, el 404 y el 500. <br>• `ReportingContratoTest` (5) y `DescargasContratoTest` (3): cada operación contra el contrato, los encabezados de las descargas, el 403, el 404, el 409, el 500 sin datos técnicos, el 502 y el 400. | El mismo |
| Generación | `CargaCongeladaTest` (5), `ContenidoCongeladoTest` (2), `GuardarContenidoCongeladoServicioTest` (6) e `InternoGeneracionContratoTest` (4) | `:servicios:generacion:test` |
| Validación OCS y Usuarios | `ConsultarPublicacionesServicioTest` (3) e `InternoValidacionocsContratoTest` (3); los ocho servicios de la bitácora en Usuarios (cuatro pruebas parametrizadas) | `:servicios:validacionocs:test :servicios:usuarios:test` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 677 pruebas, 0 fallas. `Reportes.test.tsx` (8, nueva): la publicación más reciente, sin publicaciones, la reforma, el buscador con el foco en el reporte, las dos descargas con el nombre del servidor, el error de una descarga, el aviso de importar la identificación como texto y el 404. `Marco.test.tsx` (+10): «Reportes» para los cinco roles con `REPORTES:LECTURA` y para ningún otro. `nombres.test.ts`: los ocho servicios de la bitácora | `vitest run` |
| SPA: Playwright | `reportes.spec.ts` (3, nueva): el Rector ve el reporte y descarga los dos archivos; el Coordinador no ve la opción y recibe el aviso; axe-core (WCAG 2.1 AA) sin fallas (SC-007). Con `bitacora.spec.ts` y `accesibilidad.spec.ts`: 10 de 10 | `mcr.microsoft.com/playwright:v1.64.0-noble`, sobre una copia del frontend con sus dependencias de glibc |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` y `preparar.sh`; la nota SC2015 de `probar_equipo.sh` ya estaba en `main` | `koalaman/shellcheck:v0.10.0` |

## El script (T028)

Sobre un perfil construido desde la rama: `docker compose up -d --build --wait`, con los siete servicios sanos.

### `probar_documento_firmable.sh`: 34 de 34, del 0 al 27 y de R1 a R6

| Paso | Resultado |
|---|---|
| R1. Sin publicación | OK: en EN_OCS, Reporting no lista 2026-I, el reporte responde 409 `REPORTING_SIN_PUBLICACION` y no hay entradas en la bitácora |
| R2. El reporte de la versión 1 | OK: el hash de la publicación; las filas suman el total; los cinco componentes son los de la carga horaria de Generación; el contenido se guardó al congelar (`tardio = false`) |
| R3. Los dos formatos | OK: el PDF es PDF/A-2b para veraPDF, sin campos, con su `Content-Disposition` y `no-store`; el CSV tiene las mismas filas, totales, versión y hash que el JSON; el CSV de `rectorado.demo` y el de `secretaria.demo` son idénticos; 8 entradas `REPORTE_INDIVIDUAL_DESCARGADO`, 4 con `CSV`, y ninguna por ver el reporte |
| R4. Acceso y errores | OK: 403 a `coordinacion.demo` y `docente.demo`; 404 `REPORTING_DOCENTE_SIN_DISTRIBUTIVO`; 400 `COMUN_SOLICITUD_INVALIDA` |
| R5. Durante la reforma | OK: versión 1 con `enReforma: true`; con una hora menos en un paralelo del docente, su CSV es idéntico byte a byte al de R3. Las horas se restauran |
| R6. La versión 2 | OK: el reporte sale de la versión 2, con su hash, sin reforma y con el contenido guardado al congelar |

Los pasos 0 a 27, que ya existían, siguen pasando.

## Mediciones (T031)

| Medición | Resultado |
|---|---|
| SC-005: cada descarga, en el perfil local | Con el docente de más filas de 2026-I: el PDF entre 0,058 y 0,068 s (22 730 bytes) y el CSV entre 0,021 y 0,026 s (2 976 bytes), en tres vueltas. La primera descarga después de reiniciar `salida`, en frío: 0,77 s. Todas, por debajo de 5 s |
| R-19: la memoria con PDFBox en `salida` | `salida` 264,6 MiB de 512 después de generar los reportes; el perfil completo, 1,79 GiB de 8. Cumple, sin alerta (`deploy/medir_memoria.sh`) |

El PDF real del perfil se revisó a la vista, con la primera página renderizada. Ahí se vio que los paralelos reales (`A_NOCTURNA`) y los códigos largos se partían en dos líneas. Se corrigieron los anchos de las columnas y se volvió a revisar.

## El recorrido del autor (T032)

Hecho por el autor el 2026-11-21 en el perfil local (`http://localhost:8081`), con 2026-I publicado (`MDD_CONSERVAR=PUBLICADO`):

| Paso | Resultado |
|---|---|
| §1. El reporte en pantalla | OK: `rectorado.demo` ve el reporte de un docente con sus cinco asignaturas, «No tiene actividades en esta versión», los cinco componentes y el total |
| §2. Las descargas | El PDF y el CSV se descargan. El CSV abre en columnas, con las tildes bien. **Hallazgo:** la hoja de cálculo mostró la cédula sin el cero inicial (`109990341`). El archivo la trae completa entre comillas; el autor eligió la opción A: un modal que aparece al descargar el CSV y pide importar la columna como texto (research §5, D-115). Primero fue un texto fijo bajo los botones, que pasaba desapercibido, y después un aviso en la página; el autor pidió un modal para este tipo de mensajes. El autor lo comprobó en el perfil local y funciona. La bitácora muestra las entradas `REPORTE_INDIVIDUAL_DESCARGADO` filtradas por Reporting, con el periodo, la versión y la identificación |
| §3. Sin permiso | OK: `coordinacion.demo` no ve «Reportes», y `/reportes` muestra «Acceso no autorizado» |
| §4. La reforma | Lo cubre el paso R5 del script |

**Solo con el teclado** (FR-016): automatizado en `reportes.spec.ts`, en Chromium, 12 de 12 en tres corridas. Se entra a «Reportes» por el menú con Enter; Tab va de «Periodo» a «Docente»; se escribe la cédula, flecha abajo y Enter; el foco pasa al nombre del docente; Tab llega a «Copiar la huella», «Descargar PDF» y «Descargar CSV», y Enter descarga cada uno; el modal del CSV toma el foco en «Entendido», Escape lo cierra y el foco vuelve a «Descargar CSV».

**Pendiente del autor:** el recorrido con un lector de pantalla (SC-007).

## Desviaciones del plan

1. **El contenido congelado cuelga de la versión** (`version_id`, con borrado en cascada), no de (periodo, número). La limpieza de los scripts recrea el distributivo de un periodo; un contenido guardado por número habría quedado de otra versión. Está en `data-model.md` y en D-115.
2. **Usuarios V8.** El check de servicios de `usuarios.bitacora` no admitía `reporting`. El plan solo sumaba el origen a las listas del código, y sin la migración cada entrada se habría rechazado. Lo encontró la revisión del script antes de correrlo. Está en D-115.
3. **Las descargas tienen su propia etiqueta en el contrato** (`Descargas`), así que hay dos controladores: `ReportesControlador` (US1) y `DescargasControlador` (US2). Cada historia compila y se prueba sola.
4. **El cálculo del contenido congelado es una función pura del dominio** (`ContenidoCongelado.de`), no un método extraído de `ContenidoCongeladoServicio`, que no cambia. Así no se toca el documento firmable de F13-H02.
5. **El script usa veraPDF** (ya estaba en el paso 4) en vez de `pdfinfo` y `qpdf`: verifica PDF/A y lista los campos de formulario sin instalar nada nuevo.
6. **R5 corrige la oferta con `vicerrectorado.demo`**, que tiene `DISTRIBUTIVO:ESCRITURA`, y restaura las horas después. Los pasos 25 a 27 recorren la versión 2 con las reglas del panel, y un paralelo con menos horas podía dejar una inconsistencia dura. Por eso R6 ya no compara las horas corregidas.
7. **`EmitirTokenDeProcesoServicioTest` no cambia:** las claves de proceso son un mapa de la configuración, así que `salida` no necesita código nuevo en Usuarios, solo su clave.
8. **La SPA también nombra a Reporting en la Bitácora** (`nombres.ts`): los tipos generados del contrato de Usuarios lo exigieron.
9. **El aviso del CSV, en un modal** (después del recorrido): al descargar el CSV, `DialogoInformativo` (nuevo, en `pantallas/comun`) dice que la columna «Identificación» se importa como texto. El foco va a «Entendido» y, al cerrar, vuelve a «Descargar CSV»: ese botón se deshabilita mientras descarga y perdía el foco, así que se le devuelve de forma explícita (lo encontró Playwright en Chromium; jsdom no lo reproducía). Lo prueban `Reportes.test.tsx` (+1) y `reportes.spec.ts`: el modal, el foco y axe-core con el modal abierto, después de que termina de aparecer, como en los demás diálogos. 45 de 45 en tres corridas de `--repeat-each=5`.

## Criterios de terminado

| Criterio | Estado |
|---|---|
| Los dos escenarios del issue (US1-1 y US2-1) | Cumplidos: R2 y R3 en el perfil real, y las pruebas de dominio, contrato y SPA |
| SC-001 a SC-004 y SC-006 | Cumplidos: R2, R3, `EscritorCsvTest`, `GeneradorDeReportePdfaTest` y R4 con `Marco.test.tsx` |
| SC-005 | Cumplido en la medición; falta el recorrido del autor para el minuto de punta a punta |
| SC-007 | axe-core sin fallas en Playwright y el recorrido solo con teclado automatizado; falta el recorrido con lector de pantalla (T032) |
| D-115 registrada | Sí, con los dos ajustes de la implementación |
| El glosario (`sdd/modelo_dominio.md` §7) | Hecho el 2026-11-21, fuera del repositorio: «Contenido congelado» (`CargaCongelada`) y «Reporte individual» sale de la versión publicada vigente (`ReporteIndividual`) |

## El equipo de demostración

Pendiente hasta después de fusionar: correr `deploy/equipo/preparar.sh` para que el `.env` del equipo gane `MDD_CLAVE_PROCESO_SALIDA` antes del despliegue (`compose.demo.yml` la exige), y verificar V10 de Generación, V8 de Usuarios y un reporte de 2026-I. El contenido de la versión publicada antes de esta historia quedará «tardío», lo que es esperado.
