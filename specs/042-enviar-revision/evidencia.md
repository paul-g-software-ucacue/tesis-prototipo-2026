# Evidencia: Enviar a revisión con cero inconsistencias abiertas

F10-H03 (issue #55). Cada sección dice qué se verificó, cómo y con qué resultado. Todas las comprobaciones son del 2026-11-08, sobre la rama `042-enviar-revision`, antes del pull request.

## Pruebas y comprobaciones (T018)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 2 786 pruebas, 0 fallas | `./gradlew build` |
| `integrationTest` completo | 263 pruebas, 0 fallas, con `EnvioARevisionIntegrationTest` (2) | `./gradlew integrationTest` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos. La más baja, Roles: 76,9 %. Validación OCS 83,8 %, Generación 90,6 % | `jacocoTestReport jacocoTestCoverageVerification` |
| Spotless y SpotBugs | En verde | `spotlessCheck spotbugsMain` |
| Arquitectura | ArchUnit y Spring Modulith en verde: Validación OCS sigue usando solo `ec.mdd.generacion.puertos.entrada` | Dentro de `build` y `:apps:distributivo:test` |
| `BitacoraSinBorradoContratoTest` | En verde: la descripción de `enviarARevision` no nombra la bitácora | `./gradlew :servicios:usuarios:test --rerun` (R1 del análisis) |
| SPA: tipos, ESLint y Prettier | En verde | `npm run tipos`, `eslint . --max-warnings 0`, `prettier --check .`, en `node:24-alpine` |
| SPA: Vitest | 532 pruebas en 39 archivos, 0 fallas | `vitest run` |
| SPA: Playwright | 13 casos de `consolidado`, `oferta` y `flujo` en Chromium, con el envío a revisión y axe-core (WCAG 2.1 AA) sin fallas | `playwright test …`, en `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` | `probar_avance.sh`, `probar_revision.sh` y los 25 que siguen en el workflow (de `probar_spa.sh` a `probar_flujo.sh`) pasan, en su orden, sobre un perfil construido desde la rama. Los 4 anteriores (autenticación, errores y bitácora) no cambian con esta historia y no se repitieron | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_revision.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 8 commits de la rama | v8.30.1 con `.gitleaks.toml`, modo `git` con `main..HEAD` |

### `probar_revision.sh` (contracts/demostracion.md)

| Paso | Resultado |
|---|---|
| 1. Cargas, una propuesta y los 7 parciales enviados (US4-2, FR-013) | OK: EN_CONSOLIDACION; `consolidacion.demo` ve `ENVIAR_A_REVISION` y `vicerrectorado.demo` no |
| 2. Los 9 usuarios de los demás roles (US4-1, SC-003) | OK: 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO`, el mismo cuerpo, sin nombrar el rol |
| 3. Sin consolidar (US3-1) | OK: 409 `VALIDACIONOCS_SIN_CONSOLIDAR` |
| 4. Consolidado, sin informe (US3-3) | OK: 409 `VALIDACIONOCS_SIN_INFORME` |
| 5. Con el informe (US2-1, US2-2, SC-001) | OK: el panel tiene 209 duras y 2 blandas; 409 `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS` con «Quedan 211: 209 duras y 2 blandas sin justificar.» |
| 6. Justificar una blanda (US1-2, MD-07) | OK: el número baja a 210 |
| 7. Sin rastro (FR-012) | OK: ninguna transición desde EN_CONSOLIDACION ni entrada `CONSOLIDADO_ENVIADO_A_REVISION`; sigue en EN_CONSOLIDACION |

## El envío que pasa (research §10)

Con los datos de prueba, el panel de 2026-I tiene 209 inconsistencias duras: paralelos sin propuesta y docentes sin clase. En EN_CONSOLIDACION no se pueden corregir mientras no exista devolver un parcial (#211, F12-H05), así que ni el script ni la demo llegan al panel vacío.

El envío que pasa lo prueba `EnvioARevisionIntegrationTest`, con PostgreSQL real, el servicio, el repositorio y la transacción reales y un Generación doble con el consolidado listo:
- el distributivo queda en EN_REVISION_VRA, con su transición guardada desde EN_CONSOLIDACION;
- deja una sola entrada `CONSOLIDADO_ENVIADO_A_REVISION`;
- con dos envíos a la vez, un solo paso, y el otro recibe `EtapaNoAdmiteRevision` (SC-004).

**SC-005 (enviar en menos de 30 s con el consolidado listo): pendiente hasta F12-H05 (#211).** No hay un consolidado listo en los datos de prueba ni en la demo.

## El 403 de los roles (SC-003)

`FlujoContratoTest` lee los permisos de cada rol de `V2__matriz_d16.sql` y arma su token como lo arma Usuarios. Los 9 roles que no son el Responsable de consolidación reciben 403 con el mismo cuerpo, sin el rol que falta, y el Responsable, 200. `ProcesoAprobacionTest` comprueba que vale el rol de la copia de la configuración, también si EN_CONSOLIDACION tiene otro rol.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| «Hay parciales sin enviar» desde el flujo (`contracts/spa.md`) | Desde el estado de cada carrera en el consolidado | Es el mismo dato, y la pantalla no necesita otra consulta |
| Ver fallar cada prueba antes del código | T008, T010, T012 y T014 pasaron a la primera | Prueban lo que ya habían hecho T003, T005, T006, T009, T011 y T013: el catálogo, el agregado y el servicio |
| Playwright: recargar la página para pasar a «listo» | «Volver a consolidar» | Recargar pierde la sesión, que vive en memoria |

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_revision.sh`, arriba; `ProcesoAprobacionTest`, `FlujoDelDistributivoServicioTest`, `FlujoContratoTest`, `EnvioARevisionIntegrationTest`, `ParcialesDelDistributivoServicioTest`, `EstadoDelFlujo.test.tsx`, `Consolidado.test.tsx` y `e2e/consolidado.spec.ts` |
| Contrato publicado y verificado | `enviarARevision` y `ENVIAR_A_REVISION` en `contratos/validacionocs/openapi.yaml`, con `FlujoContratoTest` y `PermisosValidacionocsTest` |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo | Seis códigos `VALIDACIONOCS_*` en `contratos/comun/errores.md` y `ErroresValidacionocs`, verificados por `ErroresValidacionocsTest`; el de inconsistencias, con su detalle en singular y plural |
| Validaciones de entrada (principio XIV) | El periodo con su patrón en el contrato (400 `COMUN_SOLICITUD_INVALIDA`); la operación no tiene cuerpo |
| JavaDoc | `validarRevision`, `enviarARevision` (dice que no notifica, D-97), `CondicionesDeRevision` en los dos servicios, `ParcialesDelDistributivo.condicionesDeRevision` y `FlujoDelDistributivoServicio` (T017) |
| Seguridad | `@PreAuthorize` con `CONSOLIDACION:APROBACION`; el rol de la etapa en el dominio, con el mismo 403; SQL parametrizado; el bloqueo `FOR UPDATE` de F12-H02 para un solo paso; SpotBugs y Gitleaks en verde |
| Bitácora (D-90, D-95) | Una entrada por transición, con la versión, el hash y la huella del informe |
| Decisión registrada | D-97, antes de implementar |

## El equipo de demostración (T019)

| Qué | Resultado | Fuente | Fecha |
|---|---|---|---|
| Fusión (#212) | `fb53d36`, con las 12 comprobaciones de CI en verde, `perfil-consolidado` incluido | GitHub | 2026-11-08 |
| Despliegue | `demostracion` exitoso a las 14:37; «Sano en 83 s», con la prueba de humo en verde; los 8 contenedores sanos con las imágenes `fb53d36a0b7e` | GitHub, `journalctl` y `docker ps` | 2026-11-08 |

Para llegar a EN_CONSOLIDACION, el autor envió los 6 parciales que faltaban con `coordinacion.demo`. Como `admin.demo` le cambió la carrera antes de cada envío, porque la demo tiene un solo coordinador. Con el último, a las 17:44:57, la bitácora tiene un `PARCIAL_ENVIADO` y una `ETAPA_AVANZADA` de BORRADOR_CARRERA a EN_CONSOLIDACION, versión 1.

Comprobación del autor con `quickstart.md` §3, en 2026-I:

| Paso | Resultado |
|---|---|
| 1. `consolidacion.demo` en «Consolidado del periodo» | OK: «Consolidación · versión 1», etapa EN_CONSOLIDACION, los 7 parciales enviados; **Enviar a revisión** deshabilitado, con lo que falta debajo |
| 2. Consolidar | OK: `DISTRIBUTIVO_CONSOLIDADO` a las 17:52:16; la lista ya no pide consolidar |
| 3. Adjuntar el informe | OK: `INFORME_ADJUNTADO` a las 17:58:14; solo queda «Quedan 212 inconsistencias abiertas…», el mismo número que el panel (211 duras y 1 blanda), con el enlace |
| 4. `vicerrectorado.demo` en «Consolidado del periodo» | OK: ve la etapa y los parciales, sin el botón |
| 5. El 409 por la API | No se hizo en la demo. Lo comprueba `probar_revision.sh`, paso 5, en el perfil local |
| 6. La bitácora | OK: ninguna `CONSOLIDADO_ENVIADO_A_REVISION`; en la base, ninguna transición desde EN_CONSOLIDACION y la etapa sigue en EN_CONSOLIDACION |

**Un intento fallido al adjuntar, sin defecto.** Los dos primeros intentos de adjuntar mostraron «Servicio no disponible». El autor eligió un PDF de la Papelera y el navegador no pudo leerlo: cortó la conexión sin enviar el archivo. El API Gateway registró `AbortedException` y Generación, el cuerpo vacío (`EOFException`). Con un PDF de otra carpeta se adjuntó a la primera.

**SC-005 (enviar a revisión en menos de 30 s con el consolidado listo): pendiente hasta F12-H05 (#211).** Con las 211 duras de la demo, el envío no pasa.

En la demo, 2026-I queda en EN_CONSOLIDACION con todos los parciales enviados: las carreras están en solo lectura en ese periodo hasta que se pueda devolver un parcial (#211).
