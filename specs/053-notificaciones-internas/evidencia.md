# Evidencia: Recibir notificaciones internas por cada cambio de estado

F16-H01 (#67). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-16, sobre la rama `053-notificaciones-internas`, antes del pull request.

## Pruebas y comprobaciones (T022 a T024)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 303 pruebas, 0 fallas, en la corrida completa | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 350 pruebas. <br>• Notificaciones: V2 con sus reglas y permisos (`BandejaEsquemaIntegrationTest`), la bandeja, el número y la lectura (`RepositorioNotificacionesIntegrationTest`), y la misma entrega dos veces sin duplicar (`RecepcionRepetidaIntegrationTest`). <br>• `identidad`: de la solicitud a las bandejas, con Roles y Usuarios reales (`NotificacionesDesdeAprobacionIntegrationTest`, 4). <br>• `distributivo`: la transición guarda su solicitud en el registro de publicaciones, en la misma transacción (`NotificacionesPendientesIntegrationTest`). | El mismo |
| Fallas en la corrida completa | 1: la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (117 s contra 60 s), la intermitente de siempre, en `datos`, que esta rama no toca. Pasa con su módulo completo | El módulo otra vez |
| Después del recorrido a mano | Los dos cambios que salieron del recorrido tocan Validación OCS y la SPA. Se volvieron a correr completos: <br>• `:servicios:validacionocs:check` e `integrationTest`; <br>• `:apps:distributivo:test` e `integrationTest`; <br>• la SPA. <br>Todo en verde; Validación OCS tiene 443 pruebas unitarias | `./gradlew :servicios:validacionocs:check :servicios:validacionocs:integrationTest :apps:distributivo:test :apps:distributivo:integrationTest` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: <br>• Notificaciones, 72,3 %; <br>• Roles, 77,9 %; <br>• Validación OCS, 84,1 % después de los cambios | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde. ArchUnit, con una regla nueva en Validación OCS: quien guarda transiciones las notifica (`quienGuardaTransicionesNotifica`). Spring Modulith: Notificaciones depende solo de los puertos de entrada de Roles y Usuarios | `build`, `ArquitecturaTest`, `ModulosTest` |
| Notificaciones | <br>• `SolicitudDeNotificacionesTest` y `NotificacionTest`: las reglas del contenido y la lectura que se marca una vez. <br>• `RecibirNotificacionesServicioTest` (9): los destinatarios sin el autor, la carrera y el origen desconocido. <br>• `BandejaServicioTest` (5). <br>• `EventosNotificacionesContratoTest`, `BandejaContratoTest` (6), `ErroresNotificacionesTest` y `PermisosNotificacionesTest`. | `./gradlew :servicios:notificaciones:test` |
| Roles | `ConsultarDestinatariosServicioTest` (7): el rol, la carrera del coordinador y el Responsable, que son quienes tienen el rol más el designado del periodo | `./gradlew :servicios:roles:test` |
| Validación OCS | <br>• `MensajesDeTransicionTest`: el destino, el título, el detalle y el enlace de cada transición; el nombre de la carrera, con el código si falta; y un título que no pasa de 200. <br>• `PublicadorDeNotificacionesTest`: un evento por transición, el nombre de la carrera, y el código si Académico no responde. <br>• `DatosDesdeProcesosTest` (+2): los nombres de las carreras. <br>• Las pruebas de los cuatro servicios que guardan transiciones: cada uno notifica las nuevas. | `./gradlew :servicios:validacionocs:test` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 624 pruebas, 0 fallas. <br>• `Campana.test.tsx` (10): el número y su nombre accesible, la consulta cada 30 s sin contar como actividad, la lista, abrir con el periodo, el error y el reintento. <br>• `enlaces.test.ts` (19): cada enlace es una ruta, y el rol de su etapa puede abrirla. <br>• `periodoElegido.test.tsx` (5) y `Consolidado.test.tsx` (+1): el periodo de la dirección. | `vitest run` |
| SPA: Playwright | 132 pruebas, 0 fallas. `e2e/notificaciones.spec.ts` (2): el Vicerrector abre su notificación con el teclado y llega a «Consolidado del periodo» con su periodo; Escape cierra la lista. axe-core (WCAG 2.1 AA) sin fallas con la lista cerrada, abierta y vacía (SC-006) | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T022) | Los 36 del workflow pasan, en su orden, sobre un perfil construido desde la rama. Corrieron antes de los dos cambios del recorrido; después se repitió `probar_documento_firmable.sh`, el único que revisa los títulos | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` | `koalaman/shellcheck:v0.11.0` |
| Memoria | `identidad` usaba 454 de sus 512 MiB después de los 36 scripts (#224) | `docker stats` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

19 de 19 en 93 s, sobre un perfil limpio construido desde la rama, con los dos cambios del recorrido.

| Paso | Resultado |
|---|---|
| 0 a 15 | OK, como en F15-H01. Ahora el envío a revisión se hace con `identidad` detenido (SC-002): el Responsable recibe 200 y el distributivo pasa a EN_REVISION_VRA |
| 16. Las bandejas (SC-001, SC-003) | OK. <br>• Cada usuario de la demo tiene exactamente las notificaciones de cada transición: el Responsable, los parciales con el nombre de cada carrera y «Consolidación»; el Vicerrector, «Revisión del Vicerrectorado»; el Responsable y `docente.demo`, «En firma»; la Secretaría, «Resolución del OCS». <br>• Nadie recibe la suya: el autor no está. <br>• EN_OCS dice «el sistema». <br>• `vicerrectorado.demo` marca la suya, el número baja, y la de otro le da 404. |
| 17. La caída de `identidad` (SC-002) | OK: la notificación del envío llegó 25 s después de levantar `identidad` (máximo, 120 s) |
| 18. La entrega repetida (SC-002) | OK: la publicación vuelve a FAILED, el reenvío se completa y no se duplica nada; la notificación sigue leída |

**La primera corrida completa falló el paso 17, por el script.** Dijo «llegó −1 s después de que identidad volvió». El script tomaba la vuelta cuando `docker compose up --wait` terminaba. Pero `identidad` acepta el evento antes de que su chequeo de salud lo dé por sano. Ahora la vuelta es el momento en que se pide levantarlo.

## Recorrido a mano (T022, SC-004, SC-005)

Hecho por el autor el 2026-11-16, con `quickstart.md` §3, sobre un perfil construido desde la rama, después de `MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh`.

| Comprobación | Resultado |
|---|---|
| Las bandejas | El Vicerrector con 1, el Responsable con 8, `docente.demo` y `coordinacion.demo` sin ninguna, como se esperaba |
| La lectura | La notificación sin leer en negrita y con el punto; abierta, deja de contar y sigue en la lista |
| Sin recargar (SC-004) | Al aprobar el Vicerrector, la campana de `docente.demo` pasó a 1 sin recargar. No se cronometró; la consulta cada 30 s lo acota. El Responsable pasó a 9 |
| A la pantalla del hecho (SC-005) | `docente.demo` llegó a «Pendientes de firma» con su firma. El Vicerrector llegó a «Consolidado del periodo» en 2026-I, con el estado del flujo y «Aprobar el contenido» |
| Aislamiento | `coordinacion.demo` no ve las notificaciones de los demás |
| Limpieza | El perfil se bajó con `down -v` |

**Dos hallazgos del recorrido, corregidos en la rama y vistos otra vez por el autor:**
- **El periodo.** La notificación de 2026-I abrió «Consolidado del periodo» en 2026-II. Los dos periodos están en Planificación, y la pantalla elegía el más reciente. Ahora la campana navega con `?periodo=` de la notificación, y «Oferta del periodo», «Consolidado del periodo» y «Resolución del OCS» lo toman (`periodoElegido.ts`). La segunda vez el autor también vio 2026-II, pero su pestaña tenía cargada la SPA anterior; al recargarla, funcionó.
- **La carrera.** Los títulos decían el código de la carrera («La carrera 550732E01-D-0301 envió su parcial»), y el autor pidió el nombre. Validación OCS lo pide a Académico por la ruta interna, solo en el envío y en la devolución. Esas dos operaciones ya le piden a Académico el estado del periodo. Si Académico no responde, el título lleva el código y la transición sigue. Un nombre de 150 caracteres con una etapa de 80 se acorta con «…» para no pasar de 200.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| RESPONSABLE_CONSOLIDACION recibe solo el designado del periodo | Quienes tienen el rol asignado y, además, el designado | Sin eso, `consolidacion.demo`, que consolida, no recibía nada. Lo decidió el autor durante T021; quedó en la spec, D-108 y el modelo de datos |
| BORRADOR_UNIDADES enlaza a `/proyectos` | Enlaza al inicio (`/`), y el patrón del enlace admite `/` | `enlaces.test.ts` mostró que el Gestor de unidad no puede abrir `/proyectos` |
| PUBLICADO sin enlace definido | `/resolucion-ocs`, provisorio hasta F18-H01 | La pantalla de la publicación no existe todavía |
| `leidaEn` tal como lo da el reloj | Truncado a microsegundos | El script vio que marcar dos veces devolvía otra fecha: la primera respuesta traía nanosegundos y la base guarda microsegundos. `BandejaServicioTest` lo vio fallar antes de corregirlo |
| Los títulos con el código de la carrera | Con su nombre en Académico | Lo pidió el autor en el recorrido |
| El enlace sin periodo | La SPA agrega `?periodo=` al navegar; el enlace del contrato no cambia | Hallazgo del recorrido (SC-005) |
| Ver fallar cada prueba antes del código | Así en la mayoría. No se vieron fallar: <br>• T003 y T004 (el contrato y el catálogo), escritos con su código; <br>• T018 y T019 (las pruebas de integración entre procesos), escritas con el código hecho. <br>Comprobados a mano: <br>• la regla de ArchUnit, quitando una llamada; <br>• `enlaces.test.ts`, volviendo al enlace anterior. | Se cuenta tal como pasó |

**Limitación conocida.** La falla intermitente de la corrida completa es de un módulo que esta rama no toca y depende de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 16 a 18).<br>• Backend: las pruebas de Notificaciones, Roles y Validación OCS de la primera tabla, con `NotificacionesDesdeAprobacionIntegrationTest` y `NotificacionesPendientesIntegrationTest`.<br>• SPA: `Campana.test.tsx`, `enlaces.test.ts`, `periodoElegido.test.tsx` y `e2e/notificaciones.spec.ts`. |
| Contrato publicado y verificado | `contratos/notificaciones/openapi.yaml` (nuevo): la bandeja, el número sin leer, marcar como leída y la ruta interna de los eventos; con sus pruebas de contrato |
| Prueba de arquitectura | ArchUnit, con la regla nueva, y Spring Modulith en verde |
| Errores en el catálogo | `NOTIFICACIONES_NOTIFICACION_INVALIDA` y `NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA` en `contratos/comun/errores.md`, verificados por `ErroresNotificacionesTest` |
| JavaDoc | Quién decide qué (D-108), la entrega persistente y repetible, la bandeja por destinatario y el nombre de la carrera (T024) |
| Seguridad | La bandeja solo pide sesión y responde por el usuario del token; la de otro, 404. La notificación no se borra ni se reescribe: solo `leida_en`, una vez, con un disparador y el `REVOKE`. La consulta automática no extiende la sesión (ADR-020) |
| Decisión registrada | D-108, antes de implementar, puesta al día con el Responsable, el nombre de la carrera y el periodo en la navegación |

## Equipo de demostración (T026)

Pendiente: se verifica después de fusionar, en solo lectura (`quickstart.md` §4), y va en el PR de evidencia.
