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
| SPA: Vitest | 538 pruebas en 39 archivos, 0 fallas | `vitest run` |
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

**Cronometrado por el autor: 15 s** (meta: menos de 30 s), anotado en `specs/042-enviar-revision/evidencia.md`. Fue el primer envío a revisión que pasa con datos reales.

## Comprobación del autor en el perfil local (quickstart §3)

Sobre el consolidado listo de la sección anterior, el 2026-11-08:

| Paso | Resultado |
|---|---|
| 1–3. Devolver CONTABILIDAD como `consolidacion.demo` | OK: con «corta», error en el campo y «Devolver» deshabilitado; con la observación, «Propuesta de carrera», CONTABILIDAD «devuelto por consolidacion.demo el 8/11/26, 20:08» con la observación debajo, las otras 6 enviadas y sin los botones |
| **SC-005: devolver en menos de 1 minuto** | **Cumplido, 30 s**, desde abrir «Consolidado del periodo» |
| 4–5. `coordinacion.demo` reenvía sin cambios | OK: vuelve a «Consolidación»; el consolidado sigue al día (consolidado a las 19:22) y con 0 abiertas |
| 7. Bitácora | OK: `DISTRIBUTIVO_DEVUELTO` a las 20:08:15, de EN_CONSOLIDACION a BORRADOR_CARRERA, con la observación de `550411C02-P-0301`; después `PARCIAL_ENVIADO` y `ETAPA_AVANZADA` a las 20:10:03 |
| 8. `vicerrectorado.demo` en el consolidado | OK: ve la etapa, sin «Devolver parciales» |
| Enviar a revisión al final | OK: «Revisión del Vicerrectorado», en 15 s |

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| El botón «Devolver parciales» y su diálogo en `Consolidado.tsx` (`contracts/spa.md`) | En `EstadoDelFlujo`, junto a «Enviar a revisión», con el diálogo en `comun/DialogoDeDevolucion.tsx`; sus pruebas, en `EstadoDelFlujo.test.tsx` | Ahí viven los botones de cada acción del flujo, y las carreras enviadas salen del mismo flujo. Se ve igual en «Consolidado del periodo» |
| El objeto de valor `DevolucionDeParcial` con el texto | `Observacion`, del glosario (§5 y §7), con el texto; `DevolucionDeParcial` junta la carrera con su `Observacion`. En el contrato, `ParcialADevolver` | Principio III: el término ya existía (T001) |
| `probar_revision.sh` sin cambios | Su paso 1 espera ahora las dos acciones del Responsable | Desde esta historia también ve `DEVOLVER_PARCIALES` |
| Ver fallar cada prueba antes del código | T011 y T015 pasaron a la primera | Prueban lo que ya habían hecho T002, T003, T005 y T006: el contrato, la validación y el marcado en Generación |
| La prueba de Generación de T014 con una propuesta guardada | Comprueba que el parcial devuelto queda editable y que devolver no es un cambio de asignaciones; guardar y quitar una propuesta en el parcial devuelto, y el 409 en el que no se devolvió, los prueba `probar_devolucion.sh`, paso 7 | La prueba de integración usa el repositorio, no el caso de uso de proponer |

## Un hallazgo de la comprobación, ya corregido

En el diálogo «Devolver parciales» y en el estado del flujo, las carreras aparecían con su código (`550411C02-P-0301`), que el usuario no reconoce. Ahora se muestran con su nombre de Académico, como la tabla del consolidado, y con el código solo si no se puede leer el nombre. Vale también para la confirmación de enviar el parcial. Lo comprueba una prueba nueva en `EstadoDelFlujo.test.tsx`; las de `e2e/oferta.spec.ts` buscan ahora el nombre.

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

## El equipo de demostración (T023)

| Qué | Resultado | Fuente | Fecha |
|---|---|---|---|
| Fusión (#214) | `753f7a6`, con las 12 comprobaciones de CI en verde, `perfil-consolidado` incluido | GitHub | 2026-11-08 |
| Despliegue | `demostracion` exitoso a las 20:51; «Sano en 75 s», con la prueba de humo en verde; los 8 contenedores sanos con las imágenes `753f7a610150` | GitHub, `journalctl` y `docker ps` | 2026-11-08 |
| La migración `V4` | Exitosa; el proceso de 2026-I, que ya existía, quedó con `admiteDevolucion: true` en EN_CONSOLIDACION | `flyway_schema_history` y consulta de solo lectura | 2026-11-08 |

Comprobación del autor con `quickstart.md` §3 y §4, en 2026-I, con TECNOLOGIA SUPERIOR EN CONTROL DE INCENDIOS Y OPERACIONES DE RESCATE (`551021B01-D-0301`), la carrera que tenía asignada `coordinacion.demo`:

| Paso | Resultado |
|---|---|
| Devolver como `consolidacion.demo` | OK, a las 21:15:53: «Propuesta de carrera»; la carrera «devuelto por consolidacion.demo» con su observación; las otras 6 enviadas |
| Corregir como `coordinacion.demo` | OK: propuso un docente en un paralelo (`PROPUESTA_GUARDADA` a las 21:18:25), y en el parcial enviado no habría podido |
| Reenviar | OK, a las 21:18:31: `PARCIAL_ENVIADO` y `ETAPA_AVANZADA`, de vuelta en «Consolidación» |
| La bitácora | OK: `DISTRIBUTIVO_DEVUELTO`, después `PROPUESTA_GUARDADA`, `PARCIAL_ENVIADO` y `ETAPA_AVANZADA`. En la base, una `DEVOLUCION` de EN_CONSOLIDACION a BORRADOR_CARRERA con la observación de la carrera, y el `ENVIO_PARCIAL` y el `PASO_DE_ETAPA` del reenvío |
| Consolidar otra vez | OK: tras el cambio, el autor volvió a consolidar a las 21:23:44. El panel pasó de 211 duras y 1 blanda a 209 duras y 3 blandas: el paralelo cubierto y el docente con clase dejaron de ser duras, y la propuesta dejó dos blandas nuevas. «Enviar a revisión» sigue deshabilitado con «Quedan 212 inconsistencias abiertas», y «Devolver parciales» vuelve a estar disponible |

**SC-005 (devolver en menos de 1 minuto):** cumplido en el perfil local, **30 s** (sección «Comprobación del autor en el perfil local»). En la demo no se cronometró.

En la demo, 2026-I queda en EN_CONSOLIDACION, con la carrera reenviada y una propuesta más.

## Lo que queda fuera

- **#215:** Generación permite consolidar, justificar y cambiar el informe después de enviar a revisión. Lo encontró el autor en esta comprobación; es de F10-H03.
