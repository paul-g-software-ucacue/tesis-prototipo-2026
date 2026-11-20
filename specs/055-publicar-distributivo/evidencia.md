# Evidencia: Publicar el distributivo aprobado

F18-H01 (#71). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-18 y el 2026-11-19, sobre la rama `055-publicar-distributivo`, antes del pull request.

## Pruebas y comprobaciones (T016 y T017)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 373 pruebas, 0 fallas | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 360 pruebas, 0 fallas. <br>• Validación OCS: dos publicaciones a la vez dejan una sola transición a PUBLICADO; la otra, `EtapaNoAdmitePublicacion` (`RepositorioProcesoIntegrationTest`, +1). <br>• `identidad`: con los tres destinos de la publicación, quien tiene DOCENTE y COORDINADOR_CARRERA recibe una; OPERADOR_DATOS, una; ni el autor, ni la otra Secretaría, ni un docente desactivado (`NotificacionesDesdeAprobacionIntegrationTest`, +1). | El mismo |
| Fallas en la corrida completa | 2, en módulos que esta rama no toca: `ClienteDeEventosLocalTest` (`plataforma:eventos`) y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (`datos`). La corrida coincidió con Vitest, que también falló por la carga. Repetidas solas, pasan | `:plataforma:eventos:test :apps:datos:integrationTest --rerun-tasks` |
| Cobertura de Validación OCS (60 %, D-32) | 86 % de instrucciones, con `jacocoTestCoverageVerification` en verde | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde, con ArchUnit y Spring Modulith sin reglas nuevas | `build` |
| Validación OCS | <br>• `ProcesoAprobacionTest` (+8): publicar desde APROBADO con la versión y el hash congelados; desde otra etapa y otra vez, `EtapaNoAdmitePublicacion`; sin el rol, `RolDeOtraEtapa`; las acciones en APROBADO y en PUBLICADO; los destinos de la publicación; registrar la resolución en PUBLICADO. <br>• `PublicarDistributivoServicioTest` (6, nueva): la transacción, el registro y la notificación; los rechazos sin guardar; el periodo cerrado y sin distributivo. <br>• `FlujoDelDistributivoServicioTest` (+1): publicado, el avance, enviar a revisión, devolver y aprobar responden con su error, sin registrar ni notificar, y nadie tiene acciones. <br>• `MensajesDeTransicionTest` (+1): los tres destinos, el título, el detalle y el enlace por rol. <br>• `PublicarContratoTest` (4, nueva) y `FlujoContratoTest` (+2): el 200 con la constancia, cada error, el 403 sin permiso y el 400 del periodo mal formado. <br>• `PermisosValidacionocsTest` y `ErroresValidacionocsTest`: el permiso `PUBLICACION:APROBACION` y el código nuevo. | `./gradlew :servicios:validacionocs:test` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 641 pruebas, 0 fallas. <br>• `EstadoDelFlujo.test.tsx` (+4): el botón solo con la acción, el diálogo con su texto y el foco en «Cancelar», el `POST` sin cuerpo, el 409 en el diálogo y la constancia. <br>• `Consolidado.test.tsx` y `Inconsistencias.test.tsx` (+1 cada una): «está publicada». <br>• `enlaces.test.ts` (+3): cada rol puede abrir el enlace de su notificación. | `vitest run` |
| SPA: Playwright | 134 pruebas, 0 fallas. `e2e/resolucion.spec.ts` (+1): la Secretaría publica con el teclado, y axe-core (WCAG 2.1 AA) no encuentra fallas con el diálogo abierto ni en PUBLICADO (SC-006) | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T016) | Los 36 del workflow, en su orden, sobre un perfil construido desde la rama. 35 pasaron a la primera; `probar_documento_firmable.sh` falló por el error del script de abajo y, corregido, pasa completo junto con `probar_rechazo_en_firma.sh` | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` | `koalaman/shellcheck:stable` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

22 de 22, del 0 al 21, sobre un perfil construido desde la rama.

| Paso | Resultado |
|---|---|
| 13. En EN_OCS | OK, y además: publicar responde 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION` |
| 16. Publicar (nuevo, US1) | OK. `vicerrectorado.demo` y `coordinacion.demo` reciben 403 sin dejar transición; `secretaria.demo` publica con `identidad` detenido y el token ya pedido: 200, PUBLICADO y la constancia (versión 1, `secretaria.demo`, sin acciones); una transición a PUBLICADO, `DISTRIBUTIVO_PUBLICADO` en la bitácora, y la segunda publicación, 409 |
| 17. Sin acciones (nuevo, US3) | OK. El estado del flujo de los 11 usuarios de la demo: sin acciones, o 403 a quien no lo lee. Proponer un docente y consolidar responden `GENERACION_VERSION_CONGELADA`; enviar a revisión, aprobar y registrar la resolución, el error de su etapa; sin transiciones nuevas |
| 18. Las bandejas | OK, con la publicación: una a cada usuario activo con DOCENTE, OPERADOR_DATOS o COORDINADOR_CARRERA, y a los cuatro de la demo (`datos.demo`, `coordinacion.demo`, `docente.demo` y `docente.coordinador.demo`) una sola; `secretaria.demo` no la recibe |
| 19. La caída de `identidad` (movida a la publicación, SC-003) | OK: la publicación respondió 200 sin error, y su notificación llegó a `docente.demo` una sola vez, 19 s después de levantar `identidad` (máximo, 120 s) |
| 20 y 21 | OK, sin cambios: la entrega repetida y F1 con `identidad` detenido |

`MDD_CONSERVAR=APROBADO` (nuevo) se detiene con la resolución registrada, para el recorrido a mano.

**El error que encontró el script.** En la primera corrida del workflow fallaron los pasos 18 y 20. `esperadas()` nombraba a los cuatro destinatarios de la demo, pero la publicación también le llegó a `autor.prueba`, un usuario activo con DOCENTE y COORDINADOR_CARRERA que dejan `probar_autenticacion.sh` y otros scripts. Notificaciones hizo lo correcto: avisa a todos los usuarios activos de esos roles. Ahora los destinatarios de la publicación salen de la base, y el paso 18 exige aparte que a los cuatro de la demo les llegue una vez. El 20 falló solo porque el 18 cortó antes de la lectura del Vicerrector. Además, el paso 18 espera las bandejas hasta 120 s, porque la notificación de la publicación llega después de la vuelta de `identidad`.

**Lo que encontró la CI.** En el PR, después de actualizar con `main`, fallaron los pasos 12 y 16 (la publicación sí respondió 200 con PUBLICADO). Los dos esperaban 15 s entradas de la bitácora que viajan a `identidad` por eventos justo después de detenerlo: la de la publicación (paso 16) y la de F1, subida con `identidad` detenido desde F16-H02 (paso 12). Con el reenvío, pueden tardar hasta 120 s; en local y en las corridas anteriores llegaron antes de los 15 s. Ahora esas esperas usan `ESPERA_REENVIO`, que termina en cuanto la entrada llega.

## Recorrido a mano (T016, SC-005, SC-006)

Hecho por el autor el 2026-11-19, con `quickstart.md` §3, sobre el perfil construido desde la rama, después de `MDD_CONSERVAR=APROBADO deploy/probar_documento_firmable.sh`.

| Comprobación | Resultado |
|---|---|
| Publicar (US1, SC-005) | `secretaria.demo`, en «Resolución del OCS», publicó con el diálogo; la tarjeta mostró «Publicado · versión 1» y «Versión 1 publicada por secretaria.demo el 19/11/26, 19:40», sin el botón |
| La notificación (US2) | `coordinacion.demo`, `docente.demo` y `datos.demo` tuvieron «El distributivo de 2026-I fue publicado»; `secretaria.demo`, no. `coordinacion.demo`, al abrirla, llegó a «Oferta del periodo» con la constancia en la tarjeta del flujo |
| Sin acciones (US3) | «Consolidado del periodo», como `consolidacion.demo`: «La versión 1 está publicada: ya no cambia.», la constancia y ninguna acción |
| Limpieza | El perfil se bajó con `down -v` |

**Hallazgo del recorrido.** Con el enlace al inicio para los tres roles (la decisión del plan), el clic en la notificación no llevaba a ninguna parte visible: el usuario ya estaba en el inicio. El autor eligió un enlace por rol. COORDINADOR_CARRERA va a `/oferta`, cuya tarjeta del flujo muestra la constancia. DOCENTE y OPERADOR_DATOS siguen yendo al inicio, porque hasta F19 no pueden abrir otra pantalla del distributivo. Se cambió `MensajesDeTransicion`, con la prueba vista fallar antes, y `enlaces.test.ts`; se actualizaron D-111, la spec, research §4, data-model y contracts/spa.md; y se repitió el recorrido con el servicio reconstruido.

En la bandeja del recorrido quedaron también notificaciones de corridas anteriores del script, porque la limpieza de los scripts no borra las notificaciones (F16-H01). No afectan al resultado: cada script compara solo las que llegaron desde que empezó.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| Un controlador propio, `PublicacionControlador` | La operación vive en `FlujoControlador` | El contrato la pone en la etiqueta Aprobación, que genera una sola interfaz, `AprobacionApi` |
| El enlace de la notificación, el inicio para los tres roles | `/oferta` para COORDINADOR_CARRERA; el inicio para DOCENTE y OPERADOR_DATOS | El hallazgo del recorrido (arriba), por decisión del autor |
| Registrar la resolución en PUBLICADO, en `RegistrarResolucionServicioTest` | En `ProcesoAprobacionTest`, con el agregado real | `RegistrarResolucionServicioTest` simula el agregado, y ahí no probaría la regla |
| Las guardas `case … when` en `destinoDe` y en `MensajesDeTransicion` | Un `if` antes del `switch` | Java no admite guardas sobre constantes de enum |
| El paso 18 espera las bandejas 30 s | Hasta `ESPERA_REENVIO` (120 s) | La caída de `identidad` pasó a la publicación, que se hace después; su notificación llega cuando vuelve |
| Ver fallar cada prueba antes del código | Así en la mayoría. No se vieron fallar: <br>• por reglas que ya existían: dos publicaciones a la vez (el bloqueo de F15-H01, T007), Notificaciones con los tres roles (T012) y cada operación en PUBLICADO (T014); <br>• contra el esqueleto o el código anterior: la acción `PUBLICAR` en `FlujoContratoTest`, el 403 y el 400 de `PublicarContratoTest` (T006), «sin la acción no hay botón» (T008) y `enlaces.test.ts` (T011); <br>• escritas con la pantalla hecha: Playwright (T009). | Se cuenta tal como pasó |

**Limitaciones conocidas.**
- Los docentes y Talento Humano llegan al inicio desde la notificación, sin otra pantalla que abrir, hasta F19.
- Las dos fallas intermitentes de la corrida completa son de módulos que esta rama no toca y dependen de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 13 y 16 a 19).<br>• Backend: las pruebas de Validación OCS de la primera tabla, con `RepositorioProcesoIntegrationTest` y `NotificacionesDesdeAprobacionIntegrationTest`.<br>• SPA: `EstadoDelFlujo.test.tsx`, `Consolidado.test.tsx`, `Inconsistencias.test.tsx`, `enlaces.test.ts` y `e2e/resolucion.spec.ts`. |
| Contrato publicado y verificado | `contratos/validacionocs/openapi.yaml`: la operación `publicarDistributivo`, la acción `PUBLICAR` y `EstadoDelFlujo.publicacion`, con sus pruebas de contrato |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo | `VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION` (409), en `contratos/comun/errores.md` y en `ErroresValidacionocs` |
| JavaDoc | Por qué publica Aprobación, qué es la versión publicada, a quién se notifica y por qué no al rol de la etapa PUBLICADO (T018) |
| Seguridad | Publicar exige el permiso `PUBLICACION:APROBACION` (matriz D-16) y el rol de la etapa APROBADO de la copia de la configuración; sin uno de los dos, el 403 común. La constancia solo dice la versión, quién y cuándo |
| Decisión registrada | D-111, antes de implementar; actualizada con el enlace por rol después del recorrido |

## Equipo de demostración (T020)

| Comprobación | Resultado |
|---|---|
| Antes de fusionar | «5 · imágenes» falló por vulnerabilidades nuevas, ajenas a esta rama: PostgreSQL 18.6 y, en el frontend, `openssl` y `nginx`. El PR #249 (`90d1476`) subió PostgreSQL a 18.7 y el frontend a nginx mainline (D-112); la demo lo tomó a las 00:22 del 2026-11-20, «Sano en 58 s». Al actualizar la rama con `main`, la CI encontró las esperas de la bitácora de los pasos 12 y 16 (arriba) |
| Fusión | PR #248 (`9e05c83`), con las 12 comprobaciones en verde |
| Despliegue | `cd` exitoso; `mdd-actualizar` tomó `9e05c83` y dice «Sano en 55 s», con las cinco comprobaciones del proxy en OK; los 8 contenedores sanos con las imágenes `*-9e05c8355ef5` |
| Sin migraciones | Esta historia no cambia el esquema de ningún servicio. PostgreSQL responde 18.7, con los mismos datos |
| Nada publicado todavía | 2026-I sigue en EN_CONSOLIDACION: ninguna transición a PUBLICADO, ninguna notificación de la publicación y ninguna publicación pendiente en `eventos_distributivo` |
| El frontend | La imagen desplegada trae nginx 1.31.7 (D-112), comprobado en un contenedor aparte de la imagen, sin tocar el de la demo |
| Memoria | Unos 45 s después del arranque, `identidad` usa 492 de sus 768 MiB (64 %) y `distributivo`, 235 de 512 (D-109) |

En la demo no se llega a la publicación: 2026-I está en consolidación. La publicación, la notificación y la versión sin acciones las cubren el script en la CI y el recorrido del autor en el perfil local.
