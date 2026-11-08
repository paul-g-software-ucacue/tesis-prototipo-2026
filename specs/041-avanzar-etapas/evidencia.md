# Evidencia: Avanzar el distributivo según el rol de la etapa

F12-H02 (issue #57). Cada sección dice qué se verificó, cómo y con qué resultado. Todas las comprobaciones son del 2026-11-07, sobre la rama `041-avanzar-etapas`, antes del pull request.

## Pruebas y comprobaciones (T020)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 2 749 pruebas, 0 fallas | `./gradlew build` |
| `integrationTest` completo | 261 pruebas, 0 fallas | `./gradlew integrationTest` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos. La más baja, Roles: 76,9 %. Validación OCS 83,1 %, Generación 90,6 %, Académico 84,7 %, Usuarios 84,4 %, Docentes 82,1 %, Normativa 79,6 % | `jacocoTestReport jacocoTestCoverageVerification` |
| Spotless y SpotBugs | En verde | `spotlessCheck spotbugsMain` |
| Arquitectura | ArchUnit y Spring Modulith en verde: Validación OCS usa solo `ec.mdd.generacion.puertos.entrada`, la interfaz publicada de Generación | Dentro de `build` |
| SPA: tipos, ESLint y Prettier | En verde | `tsc --noEmit` de los tres `tsconfig`, `eslint . --max-warnings 0`, `prettier --check .`, en `node:24-alpine` |
| SPA: Vitest | 522 pruebas en 39 archivos, 0 fallas | `vitest run` |
| SPA: Playwright | 19 casos de `oferta`, `actividades`, `cargos-proyectos` y `consolidado` en Chromium, incluido el envío del parcial, con axe-core (WCAG 2.1 AA) sin fallas | `playwright test … --project=chromium`, en `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Los 30 scripts de `perfil-consolidado` | Los 30 pasan en el orden del workflow sobre un perfil construido desde la rama, con `probar_avance.sh` después de `probar_consulta_bitacora.sh` | Uno tras otro, como el workflow. `probar_avance.sh` falló la primera vez (ver abajo) y se corrió otra vez, dos veces seguidas, con `probar_consolidado.sh` y `probar_flujo.sh` después: todos en verde |
| shellcheck | 0 hallazgos en los scripts de `deploy/` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 91 archivos cambiados ni en el historial de la rama | v8.30.1 con `.gitleaks.toml`: modo `dir` sobre una copia de los archivos cambiados, y modo `git` con `main..HEAD` |

### `probar_avance.sh` (contracts/demostracion.md)

| Paso | Resultado |
|---|---|
| 1. Cargas, periodo abierto y distributivo (FR-001, FR-002) | OK: BORRADOR_CARRERA, 7 parciales pendientes, versión 1 |
| 2. El coordinador envía su parcial (US1-1, US1-2) | OK: 200; asignar una actividad en su carrera responde 409 `GENERACION_PARCIAL_NO_EDITABLE`; una entrada `PARCIAL_ENVIADO` con el usuario, BORRADOR_CARRERA/PENDIENTE → ENVIADO, la versión 1 y un hash de 64 caracteres |
| 3. Enviarlo otra vez (US1-4) | OK: 409 `VALIDACIONOCS_PARCIAL_YA_ENVIADO`, sin entrada nueva |
| 4. El parcial de otra carrera (US1-3) | OK: 403 sin nombrar la carrera, sin entrada |
| 5. El avance genérico en BORRADOR_CARRERA (US3-6) | OK: 409 `VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA` |
| 6. Los 9 usuarios de los demás roles (US3-1, SC-002) | OK: 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO`, sin entrada |
| 7. Los que faltan, los dos últimos a la vez (US2-1, US2-2, SC-003, SC-004) | OK: los dos con 200; EN_CONSOLIDACION; 7 `PARCIAL_ENVIADO` y una sola `ETAPA_AVANZADA` (BORRADOR_CARRERA → EN_CONSOLIDACION) |
| 8. El hash de la entrada y el de la transición (FR-011) | OK: iguales |

### Un fallo que encontró la corrida

En la primera corrida, `probar_avance.sh` terminó con 1 sin imprimir nada: la lista de carreras no terminaba en salto de línea, `read` devolvía 1 y `set -e` cortaba el script. Pasaba antes del `trap`, sin haber creado nada, y los scripts siguientes pasaron. Se agregó el salto de línea.

## El 403 de los roles (SC-002)

`FlujoContratoTest` lee los permisos de cada rol de la migración de Roles (`V2__matriz_d16.sql`) y arma su token como lo arma Usuarios. Los 9 roles que no son el de la etapa reciben 403 al enviar en BORRADOR_CARRERA y al avanzar en BORRADOR_UNIDADES; el Coordinador de carrera y el Gestor de unidad, 200. `PermisosValidacionocsTest` comprueba los `@PreAuthorize` de las 5 operaciones contra su `x-mdd-permiso`.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| Un servicio por caso de uso | Un solo `FlujoDelDistributivoServicio` implementa `ConsultarFlujo`, `EnviarParcial` y `AvanzarEtapa`, con `EtapasAlCrearse` | Comparten la carga y la creación del proceso (principio XI) |
| `EstadoDelDistributivo` con el `EstadoDelParcial` de Generación | Un booleano `enviado` | La interfaz publicada no puede exponer el dominio de Generación (Spring Modulith) |
| Sin código para una carrera sin parcial | `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO` (404) en el catálogo, el contrato y research §10 | Sin él habría sido un 500 |
| `frontend/e2e/flujo-envio.spec.ts` | El caso en `frontend/e2e/oferta.spec.ts` | Reutiliza la simulación de Académico y de la oferta |
| Borrar las tres tablas del proceso en los scripts | Solo `validacionocs.proceso_aprobacion` | Las otras dos se borran en cascada |

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_avance.sh`, arriba; `FlujoContratoTest`, `FlujoDelDistributivoServicioTest`, `ProcesoAprobacionTest`, `RepositorioProcesoIntegrationTest` (dos envíos a la vez, un solo paso de etapa), `EstadoDelFlujo.test.tsx` y `e2e/oferta.spec.ts` |
| Contrato publicado y verificado | `consultarFlujo`, `enviarParcial` y `avanzarEtapa` (etiqueta `Aprobacion`) en `contratos/validacionocs/openapi.yaml`, con `FlujoContratoTest` y `PermisosValidacionocsTest` |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo | Seis códigos `VALIDACIONOCS_*` en `contratos/comun/errores.md` y `ErroresValidacionocs`, verificados por `ErroresValidacionocsTest` |
| Validaciones de entrada (principio XIV) | El periodo y la carrera con su patrón en el contrato (400 `COMUN_SOLICITUD_INVALIDA`); el hash y la versión, en `Transicion` |
| JavaDoc | `ProcesoAprobacion`, `Transicion`, `ParcialesDelDistributivo`, `ContenidoDelParcial` y los servicios (T019); `marcarParcialEnviado` dice que no registra en Generación (D-95) |
| Seguridad | `@PreAuthorize` por operación; el rol de la etapa y la carrera en el dominio, con el mismo 403 que sin permiso; SQL parametrizado; `FOR UPDATE` para un solo paso de etapa; SpotBugs y Gitleaks en verde |
| Bitácora (D-90, D-95) | Una entrada por transición, la registra Aprobación, con la versión y el hash |
| Decisión registrada | D-95, antes de implementar |

## Pendiente (T021)

- La comprobación del autor con `quickstart.md` §4, con el cronómetro de SC-005 (enviar el parcial en menos de 30 s).
- El pull request, su fusión con la aprobación del autor, el despliegue en el equipo de demostración (con la migración `V3` en `validacionocs`) y el PR de evidencia con `Closes #57`.
