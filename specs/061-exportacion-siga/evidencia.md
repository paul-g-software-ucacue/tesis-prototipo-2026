# Evidencia: Validar antes de exportar y registrar la exportación

F19-H02 (#267). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-28, sobre la rama `061-exportacion-siga` (commit `ebbb82a` y siguientes), antes del pull request.

## Pruebas y comprobaciones (T029)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Build completo del backend | 4138 pruebas unitarias y de integración (4 omitidas), 0 fallas | `./gradlew build integrationTest jacocoTestReport --continue` |
| Cobertura (60 %, D-32) | Reporting 92,3 % de líneas (92,6 % de instrucciones); Generación 86,1 % (87,6 %); Firma 82,0 % (83,5 %) | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde. La primera corrida de los módulos cambiados falló solo en `:servicios:firma:spotlessJavaCheck`; con `spotlessApply`, en verde. `ArquitecturaTest` y `ApplicationModules.verify()` confirman que Reporting no usa clases de Firma, Generación ni Aprobación (FR-023) | `check` |
| `BitacoraSinBorradoContratoTest` (Usuarios), la lección del PR #289 | En verde con `--rerun-tasks`: ninguna descripción de escritura nombra la bitácora | `:servicios:usuarios:test --rerun-tasks` |
| Firma (dominio central, TDD) | <br>• `DocumentoFirmableServicioTest` (+3): las firmas de la versión pedida aunque la vigente sea otra (D-113), y vacío sin documento. <br>• `CadenaConEstadoTest` (+1): con las firmas anuladas, los pasos conservan su estado y la anulación va aparte. <br>• `InternoFirmaContratoTest` (6, nueva): FIRMADO, PENDIENTE (del en turno), RECHAZADO, anulada, 404, 400, 403 con token de usuario y 401 | `:servicios:firma:check` |
| Generación (dominio central, TDD) | <br>• `GuardarContenidoCongeladoServicioTest` (+7): las abiertas del panel de la misma versión; vacías con el panel de otra o sin distributivo; el guardado tardío; el fallo del panel al congelar se propaga y en el tardío guarda sin ellas; la consulta completa las trae. <br>• `ContenidoCongeladoIntegrationTest` (+3, PostgreSQL real): las columnas de V11, el `check` de las dos juntas y el disparador contra el `update`. <br>• `InternoGeneracionContratoTest` (+1): `inconsistenciasAbiertas` presente y ausente | `:servicios:generacion:check` e `integrationTest` |
| Dominio de Reporting | <br>• `CondicionesDeExportacionTest` (10): cada motivo, una cadena de un paso, el documento de otra versión, anulada y rechazada, el contenido sin registro, varios motivos en orden y las reglas de los datos. <br>• `SumaDeVerificacionTest` (3): el vector conocido de `abc`, el vacío y el formato. <br>• `ExportacionSigaTest` (3) | `:servicios:reporting:test` |
| Aplicación, adaptadores y contrato de Reporting | <br>• `GenerarCsvSigaServicioTest` (16): el rechazo sin exportar y su entrada en una transacción; el rechazo sigue aunque la entrada no se guarde; sin publicación no pide firmas; Firma sin respuesta; la reforma; el registro y la entrada juntos antes de entregar; el guardado que falla; dos exportaciones con la misma suma; 300 docentes. <br>• `ConsultarExportacionesSigaServicioTest` (2). <br>• `ClientesDeReportingTest` (+4): Firma (200, 404, 500 y sin hash) y las inconsistencias del contenido. <br>• `ExportacionesSigaJdbcIntegrationTest` (3, PostgreSQL real): el orden, el periodo, el disparador y dos guardados concurrentes. <br>• `RegistroEnBitacoraEventosTest` (+2) y `RegistroEnBitacoraIntegrationTest` (+1, `apps/salida`): `CSV_SIGA_GENERADO` con la suma y `EXPORTACION_SIGA_RECHAZADA`, sin hashes ni datos personales. <br>• `SigaContratoTest` (+3) y `ErroresReportingTest`: `X-Suma-Verificacion` igual al SHA-256 del cuerpo, el 409 con un detalle por condición, la lista con datos, vacía, 400 y 403 | El mismo |
| SPA: tipos, ESLint y Prettier | En verde | `node:24-bookworm-slim` |
| SPA: Vitest | 703 pruebas en 48 archivos. `Siga.test.tsx` (17, +6): el aviso, el 409 con sus detalles, la suma con la línea de `certutil`/`sha256sum`, copiar y su falla, y la lista con su orden y sus columnas | `vitest run` |
| SPA: Playwright | `siga.spec.ts`: 5 de 5 en Chromium; el nuevo compara la suma con el SHA-256 del archivo descargado y pasa axe-core con la lista vacía y con datos | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Gitleaks | Sin hallazgos en los 3 commits de `main..HEAD`, con `.gitleaks.toml` | `ghcr.io/gitleaks/gitleaks:v8.30.1 git` |

## El script

Sobre un perfil construido desde la rama: con la aprobación del autor, `docker compose down -v`, `docker compose build datos distributivo salida frontend` y `docker compose up -d --wait`, con los siete contenedores sanos.

### `probar_documento_firmable.sh`: 39 de 39, del 0 al 27 y de R1 a R11, a la primera

| Paso | Resultado |
|---|---|
| R1 | En EN_OCS, el 409 `REPORTING_SIN_PUBLICACION`, sin exportaciones de la corrida ni `EXPORTACION_SIGA_RECHAZADA` |
| R9 | `X-Suma-Verificacion` igual a `sha256sum` e igual en los dos archivos (SC-002, SC-005); 2 exportaciones registradas, las más recientes de la lista, con la versión 1, el layout, el nombre, las 165 filas y la suma; 2 entradas `CSV_SIGA_GENERADO` con `sha256` y `exportacion` (SC-003). Que la exportación pase prueba de punta a punta que Firma da la cadena completa y que la versión 1 se congeló con 0 duras y 0 blandas |
| R4 | `rectorado.demo`, `coordinacion.demo` y `admin.demo`, 403 en las exportaciones (SC-008) |
| R5 | Durante la reforma, la misma suma que en R9 y una exportación más |
| R11 | `update` y `delete` sobre `reporting.exportacion_siga`, como superusuario, fallan con «no se puede modificar ni borrar», y el número de filas no cambia (SC-006) |
| Los demás pasos | Sin cambios: OK |

Al terminar quedaron 4 exportaciones con 2 sumas distintas (la plantilla, en R9 y R5; el layout de R10) y sus 4 entradas `CSV_SIGA_GENERADO`. Una segunda corrida con `MDD_CONSERVAR=PUBLICADO` también pasó y dejó la versión 1 de 2026-I publicada, para el recorrido.

## Las mediciones (T031, SC-007)

| Qué | Resultado |
|---|---|
| 300 docentes, en `GenerarCsvSigaServicioTest` (los puertos simulados y `ExportadorSigaCsv` real), con la validación, la suma y el registro | 144 ms |
| De punta a punta en el perfil local, por el API Gateway (`curl -w '%{time_total}'`, cinco pedidos): las lecturas internas (Aprobación, Firma y Generación), la validación, el archivo, la suma, el registro y la entrada | 34 a 39 ms |
| La Secretaría, desde que abre la pantalla hasta tener la suma (menos de 1 minuto) | 218 ms, solo con el teclado, en el perfil real (T032) |

SC-007 (menos de 10 s con 300 docentes, y menos de 1 minuto para la Secretaría) se cumple con amplio margen.

## El recorrido (T032)

Lo hizo Claude, a pedido del autor, el 2026-11-28, sobre el perfil local con la versión 1 de 2026-I publicada (`MDD_CONSERVAR=PUBLICADO`), **sin API simulada**: Playwright (`mcr.microsoft.com/playwright:v1.64.0-noble`, Chromium, 1366 px) contra la SPA del perfil (`localhost:8081`). La clave de demostración pasó solo por una variable de entorno. El archivo de Playwright fue temporal y no se versiona.

| Qué | Resultado |
|---|---|
| Ingreso y navegación con el teclado | `secretaria.demo` entra con Enter; el enlace «Exportación a SIGA» y «Generar CSV para SIGA», con el foco y Enter |
| El aviso (FR-019a) | Visible antes del botón, como `role="note"` |
| La suma | La que muestra la pantalla es el SHA-256 del archivo descargado; debajo, la línea de `certutil` y `sha256sum` con `siga-2026-I-v1.csv` |
| Copiar | Con Enter, «Se copió la suma de verificación.», y el portapapeles tiene la suma |
| La lista | Pasa de 12 a 13 exportaciones; la nueva, arriba, con la suma |
| axe-core (WCAG 2.1 AA) con los datos reales | Sin fallas (SC-009) |
| La bitácora | Comprobada por el script (R9): una entrada `CSV_SIGA_GENERADO` por exportación, con la suma |
| El lector de pantalla | **No se hizo:** necesita a una persona. Queda para el autor, junto con el de F19-H01 |

## Las desviaciones

- **Pruebas que pasaron sin verse fallar:** `CadenaConEstadoTest` con las firmas anuladas, y el 404 de `ClientesDeReportingTest` para Firma. Fijan comportamiento que ya existía y del que dependen la ruta interna y el cliente.
- **Pruebas y código juntos:** `InternoFirmaControlador` y `RechazoDeExportacion` se escribieron con sus pruebas, que no compilaban sin ellos; se vieron fallar solo por compilación.
- **El mensaje del 409 (research §8):** `VerificadorDeCatalogo` exige la descripción del catálogo, así que los motivos van en `detalles`, como `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS`. Quedan ajustados el catálogo, D-120, research y los contratos.
- **Dónde van las inconsistencias en Generación:** en el repositorio y en `ContenidoCompleto`, no en el record `ContenidoCongelado` (data-model, «Ajustes al implementar»). Si el panel falla al congelar, se propaga para reintentar; en el guardado tardío, el contenido se guarda sin ellas.
- **El controlador provisional:** `consultarExportacionesSiga` respondió 501 mientras se hacía la US1, porque la interfaz generada `SigaApi` exige todas sus operaciones; T025 lo reemplazó.
- **El 504 de Firma en el cliente:** no tiene prueba propia en `ClientesDeReportingTest`; el plazo lo prueba la plataforma (`ConsultasEntreProcesos`), y el caso de uso prueba la propagación.
- **El script:** como `reporting.exportacion_siga` no admite borrar, las exportaciones se cuentan desde que empieza la corrida (contracts/demostracion.md).
- **El caso límite «La conexión se corta después de registrar»** se cumple por diseño: el registro se guarda antes de responder (T023). No tiene una prueba propia.
- **El glosario (T003):** el `.md` y el `.docx` están actualizados (el `.docx` con `python-docx` en `python:3.13-slim`, copiando el formato de la fila «Exportación a SIGA»); los originales quedan como `modelo_dominio.md.antes-F19-H02` y `modelo_dominio.docx.antes-F19-H02`.

## Los criterios de terminado

| Criterio | Estado |
|---|---|
| SC-001 a SC-006, SC-008 y SC-009 | Cumplidos: pruebas automáticas y R1, R4, R5, R9 y R11 |
| SC-007 | Cumplido: 144 ms con 300 docentes; 34 a 39 ms de punta a punta; 218 ms desde que se abre la pantalla |
| El recorrido (`quickstart.md` §1 a §3) | Hecho por Claude en el perfil real. El lector de pantalla queda para el autor |
| El equipo de demostración | Pendiente: después de fusionar |
