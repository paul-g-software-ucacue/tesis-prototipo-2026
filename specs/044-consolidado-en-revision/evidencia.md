# Evidencia: El consolidado no cambia durante la revisión

Defecto #215. Cada sección dice qué se verificó, cómo y con qué resultado. Todas las comprobaciones son del 2026-11-09, sobre la rama `044-consolidado-en-revision`, antes del pull request.

## Pruebas y comprobaciones (T010, T013)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 2 859 pruebas, 0 fallas | `./gradlew build` |
| `integrationTest` completo | 274 pruebas, 0 fallas, con `EtapaDelConsolidadoIntegrationTest` (2) | `./gradlew integrationTest` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos. La más baja, Roles: 76,9 %. Generación 90,8 %, Validación OCS 82,7 % | `jacocoTestReport jacocoTestCoverageVerification` |
| Spotless y SpotBugs | En verde | `spotlessCheck spotbugsMain` |
| Arquitectura | ArchUnit y Spring Modulith (`ModulosTest`) en verde: Validación OCS implementa `generacion.puertos.requeridos.EtapaDelConsolidado`, publicado como interfaz nombrada, sin ciclo | `build` y `:apps:distributivo:test` (T002) |
| `BitacoraSinBorradoContratoTest` | En verde | `./gradlew :servicios:usuarios:test --rerun` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 541 pruebas en 39 archivos, 0 fallas | `vitest run` |
| SPA: Playwright | `consolidado` e `inconsistencias` en Chromium, con el consolidado en revisión y axe-core (WCAG 2.1 AA) sin fallas | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` | `probar_avance.sh`, `probar_revision.sh`, `probar_devolucion.sh`, `probar_consolidado_en_revision.sh` (6/6, **46 s**) y los 25 que siguen (de `probar_spa.sh` a `probar_flujo.sh`) pasan, en su orden, sobre un perfil construido desde la rama | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_consolidado_en_revision.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 7 commits de la rama | v8.30.1 con `.gitleaks.toml`, `main..HEAD` |

### `probar_consolidado_en_revision.sh` (contracts/demostracion.md)

| Paso | Resultado |
|---|---|
| 1. Los 165 paralelos con un docente de la sugerencia | OK: 0 paralelos sin propuesta; el panel con 0 duras y 92 blandas |
| 2. En EN_CONSOLIDACION, consolidar y adjuntar (US3, FR-005) | OK: 200 en los dos; `enRevision` falso |
| 3. Justificar las blandas y enviar a revisión | OK: EN_REVISION_VRA. **Es el primer envío a revisión que pasa en la CI**, con datos reales |
| 4. En revisión, consolidar, justificar y adjuntar (US1, SC-001) | OK: 409 `GENERACION_CONSOLIDADO_EN_REVISION` en las tres; la consolidación, las justificaciones y la huella del informe, idénticas |
| 5. Consultar y descargar (FR-004) | OK: `enRevision` verdadero en el consolidado y en el panel; el PDF descargado tiene la huella del enviado |
| 6. Sin rastro (FR-007) | OK: ninguna entrada de consolidar, justificar ni adjuntar después del envío |

## Sin carrera con el envío (SC-004)

`EtapaDelConsolidadoIntegrationTest`, con PostgreSQL real:
- una transacción pregunta la etapa (`FOR SHARE`) y tarda 1,5 s en confirmar;
- un envío a revisión simultáneo espera;
- su transición queda con un momento posterior al último instante antes de que la primera confirmara.

Antes de mover el momento dentro de la transacción bloqueada, la prueba fallaba: el envío esperaba, pero su momento era anterior.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| `enRevision` dentro de los registros del dominio `ConsolidadoDelPeriodo` y `PanelDeInconsistencias` | Un caso de uso pequeño, `EnRevision` (puerto de entrada de Generación, con `EnRevisionServicio`), que usan los dos controladores y las tres operaciones | Los dos registros se construyen en una docena de pruebas; así la regla queda en un solo lugar y el dominio no cambia |
| `ConsolidadoEnRevision` en `dominio/` | En `puertos/entrada/`, junto a `SinConsolidar` y `PeriodoCerrado` | Es donde Generación pone los errores de su aplicación |
| Ver fallar cada prueba antes del código | Las pruebas de T006 se escribieron antes que el código, pero se corrieron con el código ya escrito; las de contrato de T007 pasaron a la primera | Se cuenta tal como pasó |
| Playwright del panel en revisión | Solo Vitest para el panel (`Inconsistencias.test.tsx`); Playwright cubre el consolidado | El panel solo oculta un botón y muestra el mismo aviso |

**Notas del análisis.** «Quitar la justificación» no existe como operación: `justificarInconsistencia` guarda o cambia (A4). El aviso es el mismo en todas las etapas posteriores a la consolidación, aunque nombre la revisión del Vicerrectorado (A5).

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_consolidado_en_revision.sh`; `ConsolidarServicioTest`, `JustificarInconsistenciaServicioTest`, `InformeSustentadoServicioTest`, `EnRevisionServicioTest`, `TipoDeEtapaTest`, `EtapaDelConsolidadoServicioTest`, `EtapaDelConsolidadoIntegrationTest`, `ConsolidadoContratoTest`, `PanelContratoTest`, `Consolidado.test.tsx`, `Inconsistencias.test.tsx` y `e2e/consolidado.spec.ts` |
| Contrato publicado y verificado | `enRevision` y el 409 en `contratos/generacion/openapi.yaml`, con las pruebas de contrato |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde, con la interfaz nombrada `requeridos` |
| Errores en el catálogo | `GENERACION_CONSOLIDADO_EN_REVISION` en `contratos/comun/errores.md` y `ErroresGeneracion`, verificado por `ErroresGeneracionTest` |
| JavaDoc | El puerto requerido, el caso de uso y el servicio de Aprobación, el adaptador, `EnRevision`, la excepción, la regla de etapas y las tres operaciones (T012) |
| Seguridad | La regla en el servidor; la SPA solo la refleja; SQL parametrizado; SpotBugs y Gitleaks en verde |
| Decisión registrada | D-99, antes de implementar |

## Pendiente (T014)

- El pull request y su fusión con la aprobación del autor.
- El despliegue en el equipo de demostración y la comprobación del autor: en la demo, 2026-I está en Consolidación, así que «Volver a consolidar», «Adjuntar el informe» y «Justificar» siguen, y no aparece el aviso.
- El PR de evidencia con `Closes #215`.
