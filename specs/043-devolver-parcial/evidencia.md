# Evidencia: Devolver un parcial a su carrera con una observación

F12-H05 (issue #211). Cada sección dice qué se verificó, cómo y con qué resultado. Todas las comprobaciones son del 2026-11-08, sobre la rama `043-devolver-parcial`, antes del pull request.

## Pruebas y comprobaciones (T022)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 2 832 pruebas, 0 fallas | `./gradlew build` |
| `integrationTest` completo | 272 pruebas, 0 fallas, con `DevolucionIntegrationTest` (5), `MigracionDevolucionIntegrationTest` (1) y `DevolucionDeParcialesIntegrationTest` (3) | `./gradlew integrationTest` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos. La más baja, Roles: 76,9 %. Validación OCS 83,4 %, Generación 90,7 % | `jacocoTestReport jacocoTestCoverageVerification` |
| Spotless y SpotBugs | En verde | `spotlessCheck spotbugsMain` |
| Arquitectura | ArchUnit y Spring Modulith en verde: Validación OCS sigue usando solo `ec.mdd.generacion.puertos.entrada` | Dentro de `build` |
| `BitacoraSinBorradoContratoTest` | En verde: la descripción de `devolverParciales` no nombra la bitácora | `./gradlew :servicios:usuarios:test --rerun` |
| SPA: tipos, ESLint y Prettier | En verde | `npm run tipos`, `eslint . --max-warnings 0`, `prettier --check .`, en `node:24-alpine` |
| SPA: Vitest | 537 pruebas en 39 archivos, 0 fallas | `vitest run` |
| SPA: Playwright | 15 casos de `consolidado`, `oferta` y `flujo` en Chromium, con la devolución, el parcial devuelto en la oferta y axe-core (WCAG 2.1 AA) sin fallas | `playwright test …`, en `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` | `probar_avance.sh` (8/8), `probar_revision.sh` (7/7), `probar_devolucion.sh` (9/9) y los 25 que siguen en el workflow (de `probar_spa.sh` a `probar_flujo.sh`) pasan, en su orden, sobre un perfil construido desde la rama. Los 4 anteriores (autenticación, errores y bitácora) no cambian con esta historia y no se repitieron | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_devolucion.sh` y `probar_revision.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 9 commits de la rama | v8.30.1 con `.gitleaks.toml`, modo `git` con `main..HEAD` |

### `probar_devolucion.sh` (contracts/demostracion.md)

| Paso | Resultado |
|---|---|
| 1. Cargas, una propuesta y los 7 parciales enviados (US4-2, FR-016) | OK: EN_CONSOLIDACION; `consolidacion.demo` ve `ENVIAR_A_REVISION` y `DEVOLVER_PARCIALES`, `vicerrectorado.demo` nada |
| 2. Los 9 usuarios de los demás roles (US4-1, SC-003) | OK: 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO`, el mismo cuerpo, sin nombrar el rol |
| 3. Sin observación, de 9 caracteres, solo espacios, corta sin sus espacios, carrera repetida y una de dos inválida (US3, SC-001) | OK: 400 con su código en cada caso; ninguna devolución guardada ni parcial devuelto |
| 4. Una carrera que no existe junto con una válida (FR-005) | OK: 404 `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO`; ninguna devuelta |
| 5. Dos carreras con su observación (US1-1, US1-2, US1-3, SC-002) | OK: BORRADOR_CARRERA; las dos con `devolucion` y su observación, las otras 5 enviadas; una entrada `DISTRIBUTIVO_DEVUELTO` con las dos observaciones |
| 6. Devolver otra vez (US4-3) | OK: 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION` |
| 7. Corregir (US2-2, US2-4) | OK: la carrera devuelta quita una propuesta; la no devuelta recibe 409 `GENERACION_PARCIAL_NO_EDITABLE` |
| 8. Reenviar (US2-3, FR-010) | OK: BORRADOR_CARRERA tras la primera; EN_CONSOLIDACION tras la segunda, con su `ETAPA_AVANZADA` |
| 9. Sin rastro (FR-011, FR-013) | OK: una sola devolución guardada y una sola entrada |

## SC-005 de F10-H03 (T020, SC-007)

Con la sugerencia de docentes, el distributivo de 2026-I **sí llega a un consolidado listo** en el perfil local. Un script en el scratchpad, fuera del repositorio, hizo esto por la API:
- propuso en cada uno de los 165 paralelos al primer candidato que se puede proponer, con horas suficientes y todavía sin clase; si no había, al primero que se puede proponer;
- quedaron 0 paralelos sin propuesta y 46 docentes con clase; el panel quedó con **0 duras** y 92 blandas, todas de RN-05;
- justificó las 92 blandas, envió los 7 parciales, consolidó y adjuntó un PDF mínimo: el consolidado quedó con 0 abiertas, al día y con informe.

**Pendiente:** cronometrar «Enviar a revisión» en la pantalla y anotarlo en `specs/042-enviar-revision/evidencia.md`.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| El botón «Devolver parciales» y su diálogo en `Consolidado.tsx` (`contracts/spa.md`) | En `EstadoDelFlujo`, junto a «Enviar a revisión», con el diálogo en `comun/DialogoDeDevolucion.tsx`; sus pruebas, en `EstadoDelFlujo.test.tsx` | Ahí viven los botones de cada acción del flujo, y las carreras enviadas salen del mismo flujo. Se ve igual en «Consolidado del periodo» |
| El objeto de valor `DevolucionDeParcial` con el texto | `Observacion`, del glosario (§5 y §7), con el texto; `DevolucionDeParcial` junta la carrera con su `Observacion`. En el contrato, `ParcialADevolver` | Principio III: el término ya existía (T001) |
| `probar_revision.sh` sin cambios | Su paso 1 espera ahora las dos acciones del Responsable | Desde esta historia también ve `DEVOLVER_PARCIALES` |
| Ver fallar cada prueba antes del código | T011 y T015 pasaron a la primera | Prueban lo que ya habían hecho T002, T003, T005 y T006: el contrato, la validación y el marcado en Generación |
| La prueba de Generación de T014 con una propuesta guardada | Comprueba que el parcial devuelto queda editable y que devolver no es un cambio de asignaciones; guardar y quitar una propuesta en el parcial devuelto, y el 409 en el que no se devolvió, los prueba `probar_devolucion.sh`, paso 7 | La prueba de integración usa el repositorio, no el caso de uso de proponer |

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_devolucion.sh`, arriba; `ProcesoAprobacionTest`, `ObservacionTest`, `FlujoDelDistributivoServicioTest`, `FlujoContratoTest`, `DevolucionIntegrationTest`, `ParcialesDelDistributivoServicioTest`, `DevolucionDeParcialesIntegrationTest`, `EstadoDelFlujo.test.tsx`, `e2e/consolidado.spec.ts` y `e2e/oferta.spec.ts` |
| Contrato publicado y verificado | `devolverParciales`, `DEVOLVER_PARCIALES`, `DEVOLUCION` y `ParcialDelFlujo.devolucion` en `contratos/validacionocs/openapi.yaml`, con `FlujoContratoTest` y `PermisosValidacionocsTest` |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo | Cinco códigos `VALIDACIONOCS_*` en `contratos/comun/errores.md` y `ErroresValidacionocs`, verificados por `ErroresValidacionocsTest`; los que nombran una carrera, con su detalle |
| Validaciones de entrada (principio XIV) | Patrones, longitudes y de 1 a 50 carreras en el contrato (400 `COMUN_SOLICITUD_INVALIDA`); la observación sin espacios y la carrera repetida en el dominio |
| Migración | `V4__devolucion.sql`, probada sobre un proceso guardado como lo guardaba F12-H02 (`MigracionDevolucionIntegrationTest`) |
| JavaDoc | `validarDevolucion` y `devolver` (dice que no notifica, D-98), `Observacion`, `DevolucionDeParcial`, `DevolverParciales`, `ParcialesDelDistributivo.marcarParcialesDevueltos` y `EstadoDeEnvio` (T021) |
| Seguridad | `@PreAuthorize` con `CONSOLIDACION:APROBACION`; el rol de la etapa en el dominio, con el mismo 403; SQL parametrizado; el bloqueo `FOR UPDATE` de F12-H02; SpotBugs y Gitleaks en verde |
| Bitácora (D-90, D-95) | Una entrada `DISTRIBUTIVO_DEVUELTO` por devolución, con la versión, el hash y cada observación |
| Decisión registrada | D-98, antes de implementar |

## Pendiente (T023)

- El pull request y su fusión con la aprobación del autor.
- El despliegue en el equipo de demostración, con la migración `V4`, y la comprobación del autor con `quickstart.md` §3 y §4, con una carrera, incluido SC-005 (devolver en menos de 1 minuto).
- El PR de evidencia con `Closes #211`.
