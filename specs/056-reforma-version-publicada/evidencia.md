# Evidencia: Iniciar una reforma de la versión publicada

F18-H02 (#72). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-20, sobre la rama `056-reforma-version-publicada`, antes del pull request.

## Pruebas y comprobaciones (T018 y T019)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 409 pruebas, 0 fallas | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 368 pruebas. <br>• Validación OCS: la tabla `reforma` (V8); la reforma se guarda y se lee con su motivo y la publicada vigente; dos reformas a la vez dejan una sola (`ValidacionocsEsquemaIntegrationTest` +1, `RepositorioProcesoIntegrationTest` +2). <br>• Generación: el motivo `REFORMA` (V9); la copia de la reforma deja la publicada CONGELADA; dos pedidos a la vez dejan una sola versión; un rechazo en firma durante la reforma anula la versión 2 y no la 1 (`GeneracionEsquemaIntegrationTest` +1, `VersionNuevaIntegrationTest` +3). <br>• `identidad`: con los destinos de la reforma, una notificación a cada uno y ninguna al autor (`NotificacionesDesdeAprobacionIntegrationTest` +1). | El mismo |
| Fallas en la corrida completa | 2, en módulos que esta rama no toca: la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (`datos`) y `EntregaIntegrationTest` (`plataforma:eventos`). Repetidas solas, pasan | `:apps:datos:integrationTest :plataforma:eventos:integrationTest --rerun-tasks` |
| Cobertura (60 % de instrucciones, D-32) | Validación OCS 85 % y Generación 91 %, con su verificación en verde | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde, con ArchUnit y Spring Modulith sin reglas nuevas | `build` |
| Validación OCS | <br>• `ProcesoAprobacionTest` (+17): iniciar la reforma desde PUBLICADO, validarla sin cambiar nada, otro rol, las 8 etapas, otro rol en la configuración, EN_CONSOLIDACION desactivada, la reforma de una reforma, las acciones en PUBLICADO y los destinos. <br>• `IniciarReformaServicioTest` (7, nueva): el orden con Generación fuera de la transacción, el motivo inválido sin pedir nada, el rechazo del agregado, sin proceso, Generación sin la versión, la segunda de dos y el periodo cerrado. <br>• `FlujoDelDistributivoServicioTest` (+2) y `MensajesDeTransicionTest` (+1). <br>• `ReformaContratoTest` (4, nueva) y `FlujoContratoTest` (+2, de punta a punta): el 200 con la versión nueva, la reforma y la publicación anterior, cada error y el 403. <br>• `ErroresValidacionocsTest` y `PermisosValidacionocsTest`: los dos códigos y el permiso. | `./gradlew :servicios:validacionocs:test` |
| Generación | `ParcialesDelDistributivoServicioTest` (+3): la reforma con quien la inició, el reintento y los casos que no corresponden | `./gradlew :servicios:generacion:test` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 649 pruebas, 0 fallas. <br>• `EstadoDelFlujo.test.tsx` (+4): el botón solo con la acción; el diálogo, su texto, el foco y el motivo corto que no se envía; el `POST` con el motivo; el 409 en el diálogo; la reforma en curso y la publicada vigente. <br>• `enlaces.test.ts` (+4): cada rol puede abrir el enlace de la reforma. | `vitest run` |
| SPA: Playwright | 135 pruebas, 0 fallas. `e2e/consolidado.spec.ts` (+1): el Vicerrector inicia la reforma con el teclado; axe-core (WCAG 2.1 AA) sin fallas con el diálogo abierto y después (SC-006) | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T018) | Los 36 del workflow, en su orden, sobre un perfil construido desde la rama: pasan todos en la primera corrida. `probar_documento_firmable.sh` tardó 188 s, y los 36, 872 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` | `koalaman/shellcheck:stable` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

28 de 28, del 0 al 27, sobre un perfil construido desde la rama.

| Paso | Resultado |
|---|---|
| 15. La resolución | OK, y además: en APROBADO, la reforma responde 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA` |
| 17. Publicado | OK: nadie tiene acciones salvo `vicerrectorado.demo`, que tiene `INICIAR_REFORMA` |
| 22. Iniciar la reforma (nuevo, US1) | OK. `secretaria.demo` y `rectorado.demo` reciben 403, y un motivo vacío o corto, 400, sin dejar transición; `vicerrectorado.demo` la inicia con `identidad` detenido y el token ya pedido: 200 en EN_CONSOLIDACION con la versión 2, la reforma y la publicación de la 1; en Generación, la 2 EDITABLE con motivo `REFORMA` y la 1 CONGELADA; `REFORMA_INICIADA` en la bitácora; la segunda, 409 |
| 23. La publicada vigente (nuevo, US2) | OK: para la Secretaría, el Responsable y la coordinación, la publicación es la 1 y la reforma dice la 1, la 2, el motivo y quién |
| 24. La notificación (nuevo, US3, SC-004) | OK: una a `consolidacion.demo`, `docente.demo`, `datos.demo`, `coordinacion.demo` y `docente.coordinador.demo`, ninguna a `vicerrectorado.demo`; la de `docente.demo` llegó 28 s después de levantar `identidad` (máximo, 120 s) |
| 25. El ciclo de la versión 2 (nuevo, SC-002) | OK: devolver la primera carrera, reenviarla, consolidar, adjuntar otro informe, revisión y aprobación; la versión 2 congelada, con su documento y la cadena desde F1 |
| 26. Las firmas, la resolución y la publicación (nuevo) | OK: F1, F2 y F3 aceptadas sobre la versión 2, EN_OCS, la resolución `RPC-SO-12-No.124-2026` y la publicación; la versión 2 es la publicada, sin reforma en curso |
| 27. La versión 1, histórica (nuevo) | OK: las dos versiones CONGELADAS, cada una con sus tres firmas aceptadas, su documento y su resolución, y ninguna anulación |

`MDD_CONSERVAR=PUBLICADO` (nuevo) se detiene después del paso 17 con la versión 1 publicada.

## Recorrido a mano (T018, SC-005, SC-006)

Hecho por el autor el 2026-11-20, con `quickstart.md` §3, sobre un perfil limpio construido desde la rama, después de `MDD_CONSERVAR=PUBLICADO deploy/probar_documento_firmable.sh`.

| Comprobación | Resultado |
|---|---|
| Iniciar la reforma (US1, SC-005) | `vicerrectorado.demo`, en «Consolidado del periodo», vio «Publicado · versión 1» con «Iniciar una reforma»; el diálogo dijo qué pasaría y a quiénes se notificaría, con el contador del motivo; al confirmar, la tarjeta pasó a «Consolidación · versión 2» |
| La reforma en curso (US2) | La tarjeta mostró «Reforma de la versión 1 en curso: versión 2, iniciada por vicerrectorado.demo…», el motivo y «Versión 1 publicada por secretaria.demo… Vigente hasta que se publique la reforma.», también en la oferta de la coordinación |
| El ciclo (FR-004) | `consolidacion.demo` y `docente.demo` (el designado) pueden devolver parciales y enviar a revisión |
| La notificación (US3) | En la base, una por usuario: `consolidacion.demo` y `docente.demo` a `/consolidado`, `coordinacion.demo` a `/oferta`, `datos.demo` al inicio; la campana del Vicerrector no la tiene |
| Limpieza | El perfil se bajó con `down -v` |

**Comportamiento conocido.** `docente.coordinador.demo`, con DOCENTE y COORDINADOR_CARRERA, recibe una sola notificación, con el enlace al inicio y no a la oferta: Notificaciones guarda la primera de las dos (la clave única de F16-H01), y la de DOCENTE llega antes. Pasa lo mismo con la publicación de F18-H01. Elegir el enlace por la prioridad del rol sería un cambio en Notificaciones.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| `crearVersionNueva` del puerto `Distributivo` con el motivo | Un método aparte, `crearVersionDeReforma` | Validación OCS no usa el enum de Generación en su puerto, y así el rechazo en firma no cambia |
| La congelación del estado del flujo, solo de la versión de trabajo | Solo la resolución se filtra en el servicio | El rechazo en firma depende de recibir la congelación anterior; la SPA ya la muestra solo si es de la versión de trabajo |
| `FlujoDelDistributivo.reforma` en T012 | Adelantado a T009 | La prueba de contrato del controlador lo necesita |
| `bloquearVigente` bloquea la fila de la vigente | Bloquea antes la del distributivo y lee la vigente en otra sentencia | **Lo encontró la prueba de dos reformas a la vez:** la segunda, al soltarse el bloqueo, seguía con la versión 1, todavía CONGELADA, e intentaba crear la 2 otra vez; la restricción única lo frenaba con un error. En el rechazo no se notaba, porque la anterior ya estaba ANULADA |
| `MDD_CONSERVAR=PUBLICADO` después del paso 21 | Después del 17 | En ese modo F1 se sube sin la caída de `identidad`, así que el paso 21 no tiene qué comprobar |
| Ver fallar cada prueba antes del código | Así en la mayoría. No se vieron fallar: <br>• contra el esqueleto: crear la versión de la reforma (bastaba pasar `quien`), el 403 sin permiso del controlador, «sin la acción no hay botón» y «publicado sin reforma» del servicio; <br>• por reglas que ya existían: los enlaces (T015) y Notificaciones con los destinos de la reforma (T016); <br>• escrita con la pantalla hecha: Playwright (T011). | Se cuenta tal como pasó |

**Limitaciones conocidas.**
- Las dos fallas intermitentes de la corrida completa son de módulos que esta rama no toca y dependen de la carga del equipo.
- Los docentes y Talento Humano llegan al inicio desde la notificación, sin otra pantalla que abrir, hasta F19.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 15, 17 y 22 a 27).<br>• Backend: las pruebas de Validación OCS y Generación de la primera tabla, con `RepositorioProcesoIntegrationTest`, `VersionNuevaIntegrationTest` y `NotificacionesDesdeAprobacionIntegrationTest`.<br>• SPA: `EstadoDelFlujo.test.tsx`, `enlaces.test.ts` y `e2e/consolidado.spec.ts`. |
| Contrato publicado y verificado | `contratos/validacionocs/openapi.yaml`: `iniciarReforma`, `SolicitudDeReforma`, `INICIAR_REFORMA`, `REFORMA` y `ReformaDelDistributivo`, con sus pruebas de contrato |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo | `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA` (409) y `VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO` (400), en `contratos/comun/errores.md` y en `ErroresValidacionocs` |
| JavaDoc | Por qué la publicada no se anula, cuál es la vigente, el orden con Generación, el bloqueo del distributivo y a quién se notifica (T020) |
| Seguridad | Iniciar la reforma exige `DISTRIBUTIVO:APROBACION` (matriz D-16) y el rol de EN_REVISION_VRA de la copia de la configuración; sin uno de los dos, el 403 común. El motivo se valida en el servidor, de 10 a 500 caracteres |
| Decisión registrada | D-113, antes de implementar |

## Equipo de demostración (T022)

Pendiente: después de fusionar.
