# Evidencia: F02-H02

Registro de las comprobaciones de la historia (issue #33): el tablero de preparación de un periodo, con el estado de cada carga en él, lo que falta y si se puede crear el distributivo (RF-56). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Línea de base

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `main` en verde | `336077f` (#157); antes, en #156, las 12 comprobaciones en verde, 1440 pruebas de backend sin fallas y los 16 scripts de `deploy/probar_*.sh` terminando en 0 | PR #156 y #157, `specs/026-orden-de-cargas/evidencia.md` | 2026-10-30 |

## Base: contrato, lenguaje publicado y dominio del tablero

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El contrato | `consultarPreparacionDelPeriodo` (`GET /api/cargas/preparacion`, `periodo` opcional con el patrón de la carga 3), con `PreparacionDelPeriodo`, `PeriodoParaElegir`, `CargaDelPeriodo`, `LoQueFalta` y `TipoDeLoQueFalta`; `CON_ERRORES` en `EstadoDeLaCarga`. Sin códigos nuevos: la causa de `CARGAS_PERIODO_NO_ENCONTRADO` suma el tablero | `contratos/cargas/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-30 |
| `tieneDatosEn` en el lenguaje publicado | `AplicadorPorTraductoresTest`, 5 de 5: por defecto es `tieneDatos()`; un traductor que lo reemplaza responde por periodo; una carga que no atiende es `IllegalArgumentException` | `./gradlew :plataforma:lenguaje-cargas:check` | 2026-10-30 |
| `PreparacionDelPeriodo`, sin dobles y con el diccionario real | 10 de 10: con todo y el periodo abierto, listo; con docentes pendiente, falta docentes; sin matriculados, sigue listo; sin datos y con un rechazo, «con errores» con su motivo; con datos y un rechazo, cargada; proyectos «sin proyectos» no falta; sin proyectos ni la confirmación, falta; el periodo sin abrir y el cerrado van después de las cargas; sin periodos, `SIN_PERIODOS`; `OrdenDeLasCargas` nunca da `CON_ERRORES` | `PreparacionDelPeriodoTest` | 2026-10-30 |

## Los dueños y Académico

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| La oferta y los matriculados, por periodo | `AplicadorAcademicoIntegrationTest`: los de 2026-I cuentan para 2026-I y no para 2026-II; una oferta retirada no cuenta; una carga que no depende del periodo responde como `tieneDatos()` | `./gradlew :servicios:academico:integrationTest` | 2026-10-30 |
| Los periodos abiertos o cerrados | `ConsultarReferenciasServicioTest`: un periodo con su versión de normativa está abierto, sin ella no, y uno cerrado está cerrado. `PeriodoDeReferencia` suma `abierto` y `cerrado`; Docentes los ignora | `./gradlew :servicios:academico:test :servicios:docentes:test` | 2026-10-30 |

## El tablero en Cargas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los periodos para elegir | `ReferenciasDesdePuertosTest`: del que empieza más tarde al más antiguo, con si están abiertos o cerrados | `./gradlew :servicios:cargas:test` | 2026-10-30 |
| El caso de uso | `ConsultarPreparacionServicioTest` (Mockito), 6 de 6: sin periodo, el que empieza más tarde, con la lista; listo con todo y abierto, preguntando los datos de ese periodo; uno que no existe es `PeriodoNoEncontrado`; sin periodos, `SIN_PERIODOS`; «con errores» del último intento, sin contar «sin proyectos»; los proyectos por sus asignables o por «sin proyectos», sin preguntarle al dueño | `./gradlew :servicios:cargas:test` | 2026-10-30 |
| El contrato y los permisos | `PreparacionContratoTest`: el 200 validado contra el contrato, sin periodo (2026-II, sin abrir), listo con 2026-I, y con docentes pendiente; 2027-I, 400 `CARGAS_PERIODO_NO_ENCONTRADO`; `2026-III`, `26-I`, `2026-i`, `20260-IIII` y `2026-I'`, 400 `COMUN_SOLICITUD_INVALIDA`; 2026-I y 2026-II válidos. `PermisosCargasTest` con la operación nueva. Cargas: 333 pruebas sin fallas | `./gradlew :servicios:cargas:test` | 2026-10-30 |
| El proceso `datos` real | `ConfirmarLasDiezCargasIntegrationTest`, 9 de 9: con las cargas 01 a 05, sin docentes, el tablero de 2026-I no está listo y le faltan docentes, cargos, proyectos y abrirse (la política viene precargada); con las 10, solo le falta abrirse; abierto, listo y todas cargadas; 2026-II, sin oferta ni matriculados propios, tiene la oferta pendiente y le falta abrirse; el tablero en **27 ms** (SC-003: menos de 3 s). `ModulosTest` en verde | `./gradlew :apps:datos:integrationTest test` | 2026-10-30 |

## Pantalla

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Vitest | `Preparacion.test.tsx`, 12 de 12: con docentes pendiente, «Crear distributivo» deshabilitado con lo que falta como descripción accesible; con las obligatorias, habilitado aunque falten los matriculados, y al pulsarlo el aviso de F06; cada estado, con el motivo de «con errores»; si faltan los proyectos, la opción de «sin proyectos»; el periodo sin abrir y el cerrado; al elegir otro periodo, pide su tablero; sin periodos, pide los ciclos académicos; sin «Distributivo: escritura», sin botón; el menú con cada permiso y sin ellos; el error del servidor sin un tablero falso. SPA: 274 pruebas, 93,9 % de líneas | `npx vitest run --coverage` | 2026-10-30 |
| Playwright con axe | `e2e/preparacion.spec.ts`, 2 de 2, sin fallas WCAG 2.1 AA: el botón deshabilitado con su descripción, y el aviso al pulsarlo. Suite completa: 94 de 94 | `npx playwright test` | 2026-10-30 |
| lint, tipos y formato | Sin observaciones | `npm run lint`, `npm run tipos`, `npm run formato:verificar` | 2026-10-30 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
