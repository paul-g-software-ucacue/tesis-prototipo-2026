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
| `probar_preparacion.sh` de punta a punta | 6 de 6 pasos en cada una de sus 3 corridas completas (una antes del corte de la nota y dos después, en la misma base, con otros scripts entre ellas): sin docentes, con un archivo de docentes con errores y 2026-I «sin proyectos», no está listo, docentes «con errores» con `CARGAS_CARGA_CON_ERRORES` y faltan docentes, cargos y abrirlo; con las 10, solo le falta abrirse; abierto con la política de prueba, listo; 2026-II le faltan la oferta y abrirse; 2027-I y 2026-III, 400; sin periodo, el que empieza más tarde; `coordinacion.demo` lo ve y `docente.demo` recibe 403. Al terminar, la base sin nada de prueba (0/0/0/0/0/0/0) | `deploy/probar_preparacion.sh`, perfil reconstruido con el builder `mdd-limpio` | 2026-10-30 |
| Los 17 scripts de punta a punta | Todos terminan en 0 | `deploy/probar_*.sh` | 2026-10-30 |
| Backend y SPA completos | Backend: 1473 pruebas sin fallas (1440 antes de la historia). Líneas: Cargas 93,3 %, Académico 81,8 %, Docentes 79,7 %, `lenguaje-cargas` 95,3 %. SPA: 274 de Vitest (93,9 % de líneas) y 94 de 94 de Playwright | `./gradlew build integrationTest`, `npx vitest run --coverage`, `npx playwright test` | 2026-10-30 |
| Sin datos en los logs (principio IX) | Ningún valor de prueba en los logs del gateway ni de `datos`: 0 coincidencias | `docker compose logs gateway datos \| grep -cE …` | 2026-10-30 |
| shellcheck y Gitleaks | shellcheck v0.11.0 sin observaciones en `probar_preparacion.sh`; Gitleaks v8.30.1 sobre un clon de la rama: sin filtraciones | `koalaman/shellcheck-alpine:v0.11.0`, `ghcr.io/gitleaks/gitleaks:v8.30.1` | 2026-10-30 |
| El equipo de demostración con esta rama | `probar_equipo.sh` pasa todos sus pasos (código 0): despliegue, sin cambios, el token tras reiniciar identidad, el túnel, las claves, la vuelta atrás (131 s con `ESPERA_SALUD=60`) y el primer despliegue roto; 1396,5 MiB después de iniciar sesión (alerta: 7168). El equipo de demostración siguió con sus 8 contenedores sanos | `BUILDX_BUILDER=mdd-limpio deploy/equipo/probar_equipo.sh` | 2026-10-30 |
| Criterios de terminado de la constitución 2.2.0 (§6) | Los 2 escenarios del issue pasan; contrato publicado y verificado; `ArquitecturaTest` y `ModulosTest` pasan; sin códigos de error nuevos; el periodo validado en contrato, servidor y pantalla con casos válidos e inválidos; mensajes en «tú»; JavaDoc al día; SpotBugs en verde; el glosario no cambia | Esta evidencia | 2026-10-30 |

## Notas

- **Un corte en la verificación.** La PC se reinició en medio de los scripts, y la limpieza del que corría no llegó a ejecutarse: quedaron datos de prueba en la base del perfil local. Se vació solo ese volumen (`tesis-prototipo-2026_datos-postgres`) y se repitió la corrida completa; el del equipo de demostración (`mdd_datos-postgres`) no se tocó. El equipo volvió solo después del reinicio.
- **El periodo abierto y el cerrado.** Los dos supuestos de la spec (un periodo tiene que estar abierto para estar listo, y uno cerrado nunca lo está) los aprobó el autor el 2026-10-30.
- **curl otra vez en las imágenes.** En la CI del PR #158, Grype encontró 7 vulnerabilidades nuevas de curl y libcurl 8.22.0-r0, corregidas en 8.23.0-r0: 2 críticas (CVE-2026-86411, CVE-2026-87866) y 5 altas (CVE-2026-86409, CVE-2026-86410, CVE-2026-86412, CVE-2026-92386, CVE-2026-92387). La imagen del frontend ya trae 8.23.0. Las imágenes oficiales de PostgreSQL 18.6 y Caddy 2.11.7, también las más recientes (2026-10-21, comprobado con `docker pull`), siguen en 8.22.0: tienen una excepción temporal en `.grype.yaml` hasta el 2026-11-16, aprobada por el autor, con el túnel apagado mientras dure.
- **El código del periodo (defecto de F01-H05 y de esta historia).** La comprobación del autor mostró un periodo con el código `003`, registrado en la pantalla «Periodos». Cargas validaba el periodo con el formato del archivo de la carga 3 (`AAAA-I` o `AAAA-II`, hasta 10), más estricto que el de Académico (`[A-Za-z0-9._-]`, hasta 20): ese periodo no se podía elegir en el tablero ni confirmar «sin proyectos». Se corrigió en Cargas, que usa la regla de Académico, su dueño: el contrato, la pantalla y la migración V4 de `cargas.historial_carga`. La regla del archivo de la carga 3 no cambia. `probar_preparacion.sh` suma un paso con un periodo registrado en la pantalla.
