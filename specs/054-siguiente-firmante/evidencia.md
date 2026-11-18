# Evidencia: Notificar al siguiente firmante y ver la cadena de firmas

F16-H02 (#68). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-18, sobre la rama `054-siguiente-firmante`, antes del pull request.

## Pruebas y comprobaciones (T018 a T020)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 343 pruebas, 0 fallas | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 353 pruebas. <br>• `identidad`: la solicitud de Firma llega a la cuenta de la identificación, con Usuarios real; sin cuenta o con la cuenta desactivada, el evento queda recibido y no hay notificación (`NotificacionesDesdeFirmaIntegrationTest`, 4). <br>• `distributivo`: confirmada con `identidad` caído, la publicación queda pendiente; revertida, no deja nada (`FirmaPendienteSinIdentidadIntegrationTest`, 2). <br>• Notificaciones: la misma solicitud de Firma dos veces (`RecepcionRepetidaIntegrationTest`). <br>• Usuarios: la búsqueda por identificación en PostgreSQL (`RepositorioUsuariosJpaIntegrationTest`). | El mismo |
| Fallas en la corrida completa | 1: la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest`, la intermitente de siempre, en `datos`, que esta rama no toca. Pasa sola | El módulo otra vez |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Firma 81,8 %, Notificaciones 78,9 % y Usuarios 84,8 % | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde. ArchUnit, con una regla nueva en Firma: no depende de Aprobación ni de Notificaciones (MD-05). Spring Modulith: Notificaciones usa solo los puertos de entrada de Roles y Usuarios | `build`, `ArquitecturaTest`, `ModulosTest` |
| Firma | <br>• `CadenaConEstadoTest` (8): el estado de cada paso recién generado, con F1 firmada, completo, rechazado en F2, anulado y sin firmantes; el paso siguiente tras F1, F2 y F3. <br>• `SubirFirmaServicioTest` (+4): F1 notifica a F2 en la transacción, F2 a F3, un fallo al notificar sale de la transacción, y el momento en microsegundos. Las de F3, la validación rechazada y la ganada por otra subida verifican que no notifica. <br>• `PublicadorDeFirmaPendienteTest` (2). <br>• `DocumentoFirmableServicioTest` (+1) y `DocumentoContratoTest` (+3): la cadena, la anulación y el 403. | `./gradlew :servicios:firma:test` |
| Notificaciones | <br>• `SolicitudDeNotificacionesTest` (+5): los tres destinos, dos claves a la vez, `carrera` sin `rol`, los largos y la identificación inválida sin repetir su valor. <br>• `DestinatariosDesdeIdentidadTest` (3, nueva): por rol, por usuario y por identificación, solo con la cuenta activa. <br>• `RecibirNotificacionesServicioTest` (+2) y `EventosNotificacionesContratoTest` (+2): el origen `firma`, sin cuenta, el 204 y el 403 con un token de usuario. | `./gradlew :servicios:notificaciones:test` |
| Usuarios | `ConsultarCuentaPorIdentificacionServicioTest` (3): con cuenta activa, inactiva y sin cuenta | `./gradlew :servicios:usuarios:test` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 632 pruebas, 0 fallas. <br>• `DocumentoFirmable.test.tsx` (+6): los estados en texto, el paso en turno en negrita, el rechazado, sin cadena, otra versión y la firma nueva al volver a abrir. <br>• `enlaces.test.ts` (+2): el Vicerrector y el Rector abren `/firmas-pendientes`. | `vitest run` |
| SPA: Playwright | 133 pruebas, 0 fallas. `e2e/consolidado.spec.ts` (+1): el Vicerrector ve la cadena con F2 en turno, y axe-core (WCAG 2.1 AA) no encuentra fallas (SC-006) | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T018) | Los 36 del workflow, en su orden, sobre un perfil construido desde la rama. 35 pasaron a la primera; `probar_documento_firmable.sh` falló por el error de precisión de abajo y, corregido, pasa completo junto con `probar_rechazo_en_firma.sh` | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en los dos scripts de la firma | `koalaman/shellcheck:v0.11.0` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

20 de 20, del 0 al 19, en 147 s, sobre un perfil construido desde la rama.

| Paso | Resultado |
|---|---|
| 2. El documento generado | OK, y además: la cadena con F1 en turno y F2 y F3 pendientes, con sus nombres impresos; `coordinacion.demo` recibe 403 |
| 11. Las tres firmas | OK. Después de cada firma, la cadena con el paso firmado, su `firmadoEn` igual al `subidoEn` de la subida, el siguiente en turno y el resto pendiente; al final, las tres firmadas |
| 16. Las bandejas | OK, con las dos nuevas: «Tienes una firma pendiente: «Revisado por»» para `vicerrectorado.demo` y «…«Aprobado por»» para `rectorado.demo`. `docente.demo` no recibe una de firma pendiente, y tras F3 nadie |
| 19. F1 con `identidad` detenido (nuevo, SC-002) | OK: la firma se aceptó, y la notificación de F2 llegó una sola vez, 32 s después de levantar `identidad` (máximo, 120 s), con el detalle de F1 y `docente.demo` como autor |

`MDD_CONSERVAR=F2` (nuevo) se detiene con F1 aceptada y F2 en turno, para el recorrido a mano.

### `probar_rechazo_en_firma.sh`

Pasa completo. La cadena: F1 firmada y F2 en turno (paso 2); vacía con las firmas de la versión 1 anuladas (paso 6); y la versión 2 desde F1 en turno, sin las firmas de la 1 (paso 8).

**El error que encontró el script.** La primera corrida del workflow falló el paso 11. La subida respondía `subidoEn` con nanosegundos (`…09.609956638Z`), y PostgreSQL lo **redondea** a microsegundos al guardarlo (`…09.609957Z`). La cadena mostraba entonces otra hora que la respuesta de la subida. Es el mismo error que F16-H01 encontró con `leidaEn`, esta vez en la subida de F14-H02. Ahora `SubirFirmaServicio` trunca el momento a microsegundos, con una prueba que se vio fallar antes del cambio.

## Recorrido a mano (T018, SC-004, SC-005)

Hecho por el autor el 2026-11-18, con `quickstart.md` §3, sobre un perfil limpio construido desde la rama, después de `MDD_CONSERVAR=F2 deploy/probar_documento_firmable.sh`.

| Comprobación | Resultado |
|---|---|
| La notificación (US1) | `vicerrectorado.demo` tuvo «Tienes una firma pendiente: «Revisado por»», sin leer, con «Periodo 2026-I, versión 1. «Elaborado por» la firmó Docente (demo).» y «Docente (demo)» como autor |
| A su documento (SC-004) | Al abrirla, llegó a «Pendientes de firma», con el paso «Revisado por» de 2026-I |
| La cadena (SC-005) | En «Consolidado del periodo», en la tarjeta del documento: F1 firmada con su fecha, F2 en turno y en negrita, F3 pendiente, con los nombres impresos de los datos de prueba |
| Quién la ve | `consolidacion.demo` vio la misma cadena; `coordinacion.demo` no tiene la pantalla |
| F1 | `docente.demo` solo tiene «El distributivo llegó a «En firma»», de F16-H01 |
| Limpieza | El perfil se bajó con `down -v` |

Sin hallazgos.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| El patrón del usuario del destino (`^[a-z0-9._-]{1,64}$`) | De 1 a 64 caracteres, sin patrón | Análisis C1: Usuarios solo fija el largo (`varchar(64)`), y el principio XIV prohíbe suponer un formato |
| `verificar:ruptura` en los dos contratos | Solo compara el de Usuarios; la compatibilidad la prueban los contratos de cada lado | Análisis I1 |
| `EstadoDelDocumento.cadena` con la clase del dominio | Con su propio tipo, `EstadoDelDocumento.Paso` | El puerto de entrada es la interfaz publicada del módulo; no expone el dominio |
| La subida conserva la hora del reloj | La trunca a microsegundos | El script lo destapó (arriba) |
| Recorrer la cadena con el teclado (FR-015) | La lista no tiene elementos interactivos, así que no hay nada que recorrer con Tab; un lector de pantalla la lee como lista, con su nombre, y cada estado se dice con texto | Lo que pide FR-015 es que el estado no dependa del color |
| Ver fallar cada prueba antes del código | Así en la mayoría. No se vieron fallar: <br>• contra los esqueletos, las que esperaban listas o cadenas vacías: los destinos inválidos (T005), tres de `CadenaConEstadoTest` (T006), «sin cuenta» de Usuarios (T004), la anulación y el 403 del documento (T012) y dos de la SPA (T013); <br>• escritas con el código hecho: dos de contrato de Notificaciones (T009), `NotificacionesDesdeFirmaIntegrationTest` (T010), las de `distributivo` (T015) y Playwright (T014). | Se cuenta tal como pasó |

**Limitación conocida.** La falla intermitente de la corrida completa es de un módulo que esta rama no toca y depende de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 2, 11, 16 y 19) y `probar_rechazo_en_firma.sh` (pasos 2, 6 y 8).<br>• Backend: las pruebas de Firma, Notificaciones y Usuarios de la primera tabla, con `NotificacionesDesdeFirmaIntegrationTest` y `FirmaPendienteSinIdentidadIntegrationTest`.<br>• SPA: `DocumentoFirmable.test.tsx`, `enlaces.test.ts` y `e2e/consolidado.spec.ts`. |
| Contrato publicado y verificado | `contratos/notificaciones/openapi.yaml` (el destino por persona) y `contratos/firma/openapi.yaml` (`cadena` y `PasoConEstado`), con sus pruebas de contrato |
| Prueba de arquitectura | ArchUnit, con la regla nueva de Firma, y Spring Modulith en verde |
| Errores en el catálogo | Sin códigos nuevos: el destino inválido es `NOTIFICACIONES_NOTIFICACION_INVALIDA` |
| JavaDoc | Por qué publica Firma, el destino por persona, sin respaldo por rol, la identificación que no se guarda ni se repite y la regla del estado de cada paso (T020) |
| Seguridad | La cadena, con el permiso de hoy. El destino por persona solo lo crea un proceso con su token (403 con uno de usuario). La identificación no se muestra, no se guarda en la notificación y no se repite en los errores; viaja en el evento, como ya estaba en Firma desde D-102 |
| Decisión registrada | D-110, antes de implementar |

## Equipo de demostración (T022)

| Comprobación | Resultado |
|---|---|
| Fusión | PR #246 (`2ba0736`), con las 12 comprobaciones en verde en la primera corrida |
| Despliegue | `cd` exitoso en la primera corrida; `mdd-actualizar` tomó `2ba0736` y dice «Sano en 56 s»; los 8 contenedores sanos con las imágenes `*-2ba0736bdcf2` |
| Sin migraciones | Esta historia no cambia el esquema de ningún servicio |
| Nada firmado todavía | Ningún documento firmable ni notificación de firma pendiente, y ninguna publicación pendiente en `eventos_distributivo`: 2026-I sigue en EN_CONSOLIDACION |
| Memoria | Dos minutos después del arranque, `identidad` usa 489 de sus 768 MiB (64 %) y `distributivo`, 235 de 512 (D-109) |

En la demo no se llega a la firma: 2026-I está en consolidación. La notificación de firma pendiente y la cadena las cubren el script en la CI y el recorrido del autor en el perfil local.
