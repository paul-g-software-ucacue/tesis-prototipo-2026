# Evidencia: Aprobar el contenido y congelar la versión

F13-H01 (#60). Cada sección dice qué se verificó, cómo y con qué resultado. Todas las comprobaciones son del 2026-11-10, sobre la rama `045-congelar-version`, antes del pull request.

## Pruebas y comprobaciones (T019)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 2 925 pruebas, 0 fallas. Generación 805, Validación OCS 342 | `./gradlew build` |
| `integrationTest` completo | 280 pruebas, 0 fallas, con `AprobacionIntegrationTest` (2) y `CongelacionIntegrationTest` (4) | `./gradlew integrationTest` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos. La más baja, Roles: 76,9 %. Generación 90,8 %, Validación OCS 82,7 % | `jacocoTestCoverageVerification` |
| Spotless y SpotBugs | En verde | `build` |
| Arquitectura | ArchUnit y Spring Modulith (`ModulosTest`) en verde: Validación OCS congela por la API publicada de Generación (`ParcialesDelDistributivo.congelarVersion`), sin leer sus tablas ni crear ciclos | `build` |
| `BitacoraSinBorradoContratoTest` | En verde | `./gradlew :servicios:usuarios:test --rerun` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 554 pruebas en 40 archivos, 0 fallas | `vitest run` |
| SPA: Playwright | 122 pruebas, 0 fallas (Chromium y Firefox), con la aprobación nueva y axe-core (WCAG 2.1 AA) sin fallas con el diálogo abierto y con la constancia | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T018) | Los 9 del workflow pasan, en su orden, sobre un perfil construido desde la rama: autenticación (4 s), errores (36 s), bitácora (60 s), consulta de la bitácora (18 s), avance (19 s), revisión (10 s), devolución (12 s), consolidado en revisión (41 s) y **aprobación (7/7, 41 s)** | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_aprobacion.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los commits de la rama | v8.30.1, `main..HEAD` |

Los scripts existentes no cambiaron (T018): sus aserciones de `acciones` son en EN_CONSOLIDACION, donde nada cambia, y el 409 `GENERACION_PARCIAL_NO_EDITABLE` sigue igual con la versión editable.

### `probar_aprobacion.sh` (contracts/demostracion.md)

| Paso | Resultado |
|---|---|
| 1. El consolidado listo y enviado a revisión | OK: 0 paralelos sin propuesta, 0 duras y 92 blandas justificadas; EN_REVISION_VRA, con el hash del envío |
| 2. Antes de aprobar (FR-005, FR-011) | OK: `coordinacion.demo`, `consolidacion.demo` y `rectorado.demo` (que tiene el permiso pero no el rol) no ven `APROBAR_CONTENIDO` y reciben 403; ninguna transición. `vicerrectorado.demo` sí la ve |
| 3. Aprobar (FR-001 a FR-004, SC-002) | OK: 200 en **35 ms** por la API; CONGELADO_EN_FIRMA; la versión CONGELADA en la base; `congelacion` con la versión, quién y **el mismo hash del envío a revisión**; `versionCongelada` verdadero en el consolidado y en el panel |
| 4. Una segunda aprobación (SC-004) | OK: 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION`; una sola transición desde EN_REVISION_VRA |
| 5. Las siete ediciones (FR-007, SC-001) | OK: proponer, quitar la propuesta, asignar y quitar una actividad, consolidar, justificar y adjuntar responden 409 `GENERACION_VERSION_CONGELADA`; la huella del contenido de la versión en la base (propuestas, actividades, estado y registro de cambios) es idéntica |
| 6. Consultar y descargar (FR-010) | OK: el consolidado y el panel responden 200; el PDF descargado tiene la huella del enviado; el flujo lleva la congelación |
| 7. La bitácora (FR-004, FR-009) | OK: una `CONTENIDO_APROBADO` de `vicerrectorado.demo` con la versión y el hash, de EN_REVISION_VRA a CONGELADO_EN_FIRMA; ninguna otra entrada de Generación o Validación OCS después de aprobar |

`MDD_CONSERVAR=1` hace solo el paso 1 y deja el distributivo en EN_REVISION_VRA, sin limpiar. Se negó con `MDD_GATEWAY=https://127.0.0.1` («solo se usa con el perfil local») y funcionó contra `http://localhost:8080`.

## Concurrencia (SC-004, research §7)

- **Dos aprobaciones a la vez** (`AprobacionIntegrationTest`, PostgreSQL real): una pasa y la otra recibe `EtapaNoAdmiteAprobacion`; queda una sola transición y una sola entrada.
- **Congelar y editar a la vez** (`CongelacionIntegrationTest`, PostgreSQL real):
  - una propuesta que llega con la versión ya bloqueada por la congelación espera y se rechaza con `VersionCongelada`, sin guardarse;
  - una congelación que llega con la versión bloqueada por una propuesta la espera, y su hash la incluye;
  - una lectura de la versión con `FOR SHARE`, como la de consolidar, justificar y adjuntar dentro de su transacción, hace esperar a la congelación.

## Comprobación a mano en el perfil local (T018, SC-003, SC-006)

Pendiente del autor, con `quickstart.md` §3: el perfil local quedó con 2026-I en EN_REVISION_VRA (`MDD_CONSERVAR=1`).

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| Congelar al aprobar y en el envío a revisión con EN_REVISION_VRA desactivada | También al enviar el último parcial y en el avance genérico, si llegan a CONGELADO_EN_FIRMA | FR-016 dice que ningún distributivo entra en CONGELADO_EN_FIRMA con una versión editable; con EN_CONSOLIDACION y EN_REVISION_VRA desactivadas, esos dos pasos también llegan. Antes de congelar, el dominio valida sin cambiar nada (`validarAvance`, `validarRevision` con las condiciones) |
| `AprobacionIntegrationTest` con Generación en el mismo proceso | Generación es un doble que congela y cuenta, como en las pruebas de integración anteriores de Validación OCS; la congelación real y su bloqueo, en `CongelacionIntegrationTest` de Generación (T013) | Así cada prueba usa el repositorio real de su servicio |
| `versionCongelada` en las pruebas de `ConsultarConsolidado` y del panel | En `EnRevision.versionCongelada` (con `EnRevisionServicioTest`) y en los dos controladores, como `enRevision` (#215) | La regla queda en un solo lugar; los registros del dominio no cambian |
| Revisar que las siete ediciones comprueban la versión bloqueada (T010) | Quitar una propuesta y quitar una actividad no la comprobaban: ahora lo hacen dentro de su transacción | Si no, una quita podía llegar después de congelar |
| Ver fallar cada prueba antes del código | Así en el dominio, la aplicación, los controladores, los contratos y la SPA. La prueba del avance genérico hacia la firma y las cuatro de `CongelacionIntegrationTest` se escribieron después del código; pasaron a la primera | Se cuenta tal como pasó |
| El diálogo con `autoFocus` en «Cancelar» | Una referencia que enfoca «Cancelar» al montarse | ESLint (`jsx-a11y/no-autofocus`) no admite `autoFocus` |

**Limitación conocida.** Con EN_REVISION_VRA desactivada, una devolución simultánea al envío a revisión puede dejar la versión congelada con el distributivo de vuelta en BORRADOR_CARRERA: la configuración por defecto tiene la revisión activa, y crear la versión nueva es de F12-H04 y F18-H02.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_aprobacion.sh`; `ProcesoAprobacionTest`, `FlujoDelDistributivoServicioTest`, `FlujoContratoTest`, `PermisosValidacionocsTest`, `AprobacionIntegrationTest`; `VersionDistributivoTest`, `ParcialesDelDistributivoServicioTest`, `ProponerDocenteServicioTest`, `AsignarActividadServicioTest`, `ConsolidarServicioTest`, `JustificarInconsistenciaServicioTest`, `InformeSustentadoServicioTest`, `EnRevisionServicioTest`, las pruebas de contrato de Generación y `CongelacionIntegrationTest`; `EstadoDelFlujo.test.tsx`, `DialogoDeAprobacion.test.tsx`, `Consolidado.test.tsx`, `Inconsistencias.test.tsx` y `e2e/consolidado.spec.ts` |
| Contrato publicado y verificado | `aprobarContenido`, `congelacion` y `APROBAR_CONTENIDO` en `contratos/validacionocs/openapi.yaml`; `versionCongelada` y el 409 en las siete operaciones de `contratos/generacion/openapi.yaml`; con las pruebas de contrato |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo | `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION` y `GENERACION_VERSION_CONGELADA` (con la versión en los detalles) en `contratos/comun/errores.md`, `ErroresValidacionocs` y `ErroresGeneracion`, verificados por sus pruebas |
| JavaDoc | `congelarVersion`, `VersionCongeladaEnGeneracion`, `VersionCongelada`, `exigirEditable`, `AprobarContenido` (por qué dos transacciones y por qué idempotente), `validarAprobacion`, `aprobarContenido`, `congelaAlAvanzar`, `congelacion`, `Distributivo.congelar`, `DistributivoDesdeGeneracion`, `EnRevision.versionCongelada` y `EtapaNoAdmiteAprobacion` (T020) |
| Seguridad | El permiso `DISTRIBUTIVO:APROBACION` y el rol de la etapa en el servidor; la SPA solo refleja; SQL parametrizado; SpotBugs y Gitleaks en verde |
| Decisión registrada | D-100, antes de implementar |
