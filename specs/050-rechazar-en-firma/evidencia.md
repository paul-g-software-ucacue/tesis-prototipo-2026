# Evidencia: Rechazar en firma con una observación

F14-H04 (#65). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-15, sobre la rama `050-rechazar-en-firma`, antes del pull request.

## Pruebas y comprobaciones (T018 a T021)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 180 pruebas | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 318 pruebas. Firma: la V6 con el rol del proceso (`DocumentoFirmableEsquemaIntegrationTest`, +3) y `registrarRechazo` contra PostgreSQL, con la carrera entre una subida y un rechazo en dos hilos (`RepositorioDocumentosIntegrationTest`, +4). Validación OCS: la V5 y la transición con su rechazo (+1 y +1). `apps:distributivo`: `DocumentoFirmableIntegrationTest` rechaza en F2 con F1 aceptada y comprueba el aviso una sola vez | El mismo |
| Fallas en la corrida completa | 3, en módulos que esta rama no toca, y las tres pasan corridas solas: `ClienteDeEventosLocalTest` (5/5) y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (9/9), las de siempre, y `EntregaIntegrationTest` de `plataforma:eventos` (3/3), que agotó su espera mientras Vitest corría a la vez | Cada una con `--rerun` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Firma 81,8 %, Validación OCS 82,9 %; la más baja, Roles, 77,2 % | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde: ArchUnit y Spring Modulith. Firma sigue sin conocer el flujo: el evento `RechazoEnFirma` va en su API publicada y Aprobación lo escucha (MD-05) | `build`, `ArquitecturaTest`, `ModulosTest` |
| El dominio de Firma | `RechazoDelFirmanteTest`: vacía, de 9 y de 501 caracteres, recortada. `FirmasDelDocumentoTest` (+2): con rechazo, nadie en turno | `./gradlew :servicios:firma:test` |
| El rechazo | `RechazarEnFirmaServicioTest` (5): fuera de turno (403, antes de mirar la observación), la observación inválida, el registro con su bitácora y su aviso en la transacción, y la carrera perdida | El mismo |
| La vuelta a revisión | `ProcesoAprobacionTest` (+7): a la etapa activa anterior por `sistema`, idempotente, en otra etapa o con otra versión no cambia nada, y aprobar, enviar a revisión o avanzar con un rechazo pendiente lanza `RequiereVersionNueva`. `TransicionTest` (3) y `RegistrarRechazoEnFirmaServicioTest` (3) | `./gradlew :servicios:validacionocs:test` |
| Contrato y catálogo | `RechazarEnFirmaContratoTest` (6): 201, el 400 del catálogo, el 400 común, los dos 403 y el 401. `FlujoContratoTest` (+3): `rechazoEnFirma` en el estado del flujo y el 409. `ErroresFirmaTest`, `ErroresValidacionocsTest` y `PermisosFirmaTest` (seis operaciones) | Las mismas |
| SPA: tipos, ESLint y Prettier | En verde, también después de corregir los textos | En `node:24-alpine` |
| SPA: Vitest | 583 pruebas, 0 fallas: `FirmasPendientes.test.tsx` (+5), `EstadoDelFlujo.test.tsx` (+2), y una en `Consolidado.test.tsx` y otra en `Inconsistencias.test.tsx` por el aviso de la versión congelada | `vitest run` |
| SPA: Playwright | 129 pruebas, 0 fallas, antes de corregir los textos; `e2e/firmas-pendientes.spec.ts` rechaza con el teclado, con axe-core (WCAG 2.1 AA) sin fallas con el diálogo abierto y después. Después de corregirlos, los archivos de las tres pantallas tocadas: 14/14 | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T019, SC-005) | Los 36 del workflow pasan, en su orden, sobre un perfil construido desde la rama; `probar_rechazo_en_firma.sh` en 48 s y `probar_documento_firmable.sh` en 73 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_rechazo_en_firma.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los commits de la rama | `zricethezav/gitleaks:v8.30.1`, `--log-opts="main..HEAD"` |

### `probar_rechazo_en_firma.sh` (contracts/demostracion.md)

10/10 en la primera corrida, en 61 s, sobre un perfil construido desde la rama. Dentro del workflow, otra vez 10/10, en 48 s.

| Paso | Resultado |
|---|---|
| 0. La base vacía | OK: sin estructura, distributivos ni procesos de aprobación |
| 1. Aprobado y generado | OK: el consolidado aprobado por `vicerrectorado.demo` y el documento generado por el sistema |
| 2. F1 aceptada | OK: `docente.demo` firma F1 y se acepta; F2, de `vicerrectorado.demo`, en turno |
| 3. Fuera de turno (SC-001) | OK: `rectorado.demo`, `docente.demo` (ya firmó) y `consolidacion.demo` reciben 403 al rechazar, sin dejar fila |
| 4. La observación (SC-001) | OK: sin observación, con una corta o sin el campo, 400 y sin fila |
| 5. El rechazo (SC-001) | OK: 201 con el paso 2 |
| 6. La vuelta a revisión (SC-002) | OK: EN_REVISION_VRA por `sistema` unos 1 s después del rechazo (límite 30 s), con el rechazo en el flujo y sin «Aprobar el contenido» |
| 7. El documento cerrado (SC-004) | OK: subir y rechazar otra vez, 403; nadie tiene pendientes |
| 8. Aprobar otra vez (SC-004) | OK: 409 `VALIDACIONOCS_REQUIERE_VERSION_NUEVA`; sigue en EN_REVISION_VRA |
| 9. Lo guardado | OK: F1 intacta, el rechazo en Firma y en Aprobación, y la bitácora de los dos servicios |

## Recorrido a mano (T019, SC-003)

Hecho por el autor el 2026-11-15, con `quickstart.md` §3, sobre un perfil construido desde la rama, después de `MDD_CONSERVAR=1 deploy/probar_rechazo_en_firma.sh`.

| Comprobación | Resultado |
|---|---|
| El diálogo | «Rechazar en firma» se queda desactivado sin observación y con menos de 10 caracteres |
| El rechazo (SC-003) | `vicerrectorado.demo` rechazó con una observación desde «Pendientes de firma» en 45 s; salió «Rechazo registrado» con la observación y la lista quedó vacía |
| El estado del flujo | En «Consolidado del periodo», EN_REVISION_VRA con el aviso «Rechazado en firma»: «Paso 2 · VICERRECTOR», quién, cuándo y la observación, «Para aprobar otra vez hace falta una versión nueva.» y sin «Aprobar el contenido» |
| El siguiente firmante | `rectorado.demo`: «No tienes firmas pendientes.» |
| Limpieza | El perfil se bajó con `down -v` |

**Hallazgos del recorrido, corregidos en la rama:**
1. **El aviso empezaba por el rótulo del paso.** Decía «Revisado por, 2026-I, versión 1: …», que no se entiende. El aviso y el diálogo dicen ahora «2026-I, versión 1 (paso «Revisado por»): …».
2. **El aviso de la versión congelada seguía diciendo «congelada para la firma».** Después del rechazo el documento ya no está en firma. Con un rechazo pendiente en el flujo, `AvisoDeVersionCongelada` dice «La versión 1 sigue congelada: para cambiarla hace falta una versión nueva.», en «Consolidado del periodo» y en el panel de inconsistencias. Si el flujo no se puede leer, queda el aviso anterior.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| El objeto de valor de Firma `RechazoEnFirma` | Se llama `RechazoDelFirmante` | El glosario usa `RechazoEnFirma` para el evento hacia Aprobación (§6), que va en la API publicada de Firma; dos clases con el mismo nombre en el módulo confundirían |
| La operación nueva en `DocumentoControlador` | En `FirmasPendientesControlador`, el de la subida | Es el controlador de «Pendientes de firma» y de la subida, la otra acción del firmante en turno |
| V5 de Validación OCS: solo el `check` y la tabla nueva | También ensancha `transicion.tipo` de `varchar(15)` a `varchar(20)` | `RECHAZO_EN_FIRMA` tiene 16 caracteres |
| `validacionocs.rechazo_en_firma` sin reescrituras por permisos | Solo de inserción por el código, sin `revoke` | Validación OCS no revoca permisos en ninguna de sus tablas, como `observacion_de_devolucion` (V4). En Firma sí se revoca (V6) |
| La preparación del script | Copiada de `probar_documento_firmable.sh` | Como en los demás scripts: cada uno se corre solo |
| Ver fallar cada prueba antes del código | Así en el dominio, la aplicación, el repositorio, el esquema, el contrato y Vitest. T010 no tiene prueba nueva: la subida tardía ya cubre el documento rechazado, con T008 contra PostgreSQL. En T013, el 409 y el campo ausente pasaron al escribirse (el bloqueo ya estaba en el dominio). En T011, el 400 común y el 401 ya pasaban contra el esqueleto. `DocumentoFirmableIntegrationTest` (T014) y la prueba de Playwright (T017) se escribieron con el código ya hecho | Se cuenta tal como pasó |

**Limitación conocida.** Las tres fallas de la corrida completa son de módulos que esta rama no toca y pasan solas: dependen de la carga del equipo. `EntregaIntegrationTest` es nueva en la lista.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_rechazo_en_firma.sh` (pasos 0 a 9); `RechazoDelFirmanteTest`, `FirmasDelDocumentoTest`, `RechazarEnFirmaServicioTest`, `RechazarEnFirmaContratoTest`, `RepositorioDocumentosIntegrationTest`, `DocumentoFirmableEsquemaIntegrationTest`, `ProcesoAprobacionTest`, `TransicionTest`, `RegistrarRechazoEnFirmaServicioTest`, `RepositorioProcesoIntegrationTest`, `ValidacionocsEsquemaIntegrationTest`, `FlujoContratoTest`; `DocumentoFirmableIntegrationTest`; `FirmasPendientes.test.tsx`, `EstadoDelFlujo.test.tsx`, `Consolidado.test.tsx`, `Inconsistencias.test.tsx` y `e2e/firmas-pendientes.spec.ts` |
| Contrato publicado y verificado | `POST /api/firma/documentos/{documento}/rechazos` en `contratos/firma/openapi.yaml`; `rechazoEnFirma`, `RECHAZO_EN_FIRMA` y el 409 en `contratos/validacionocs/openapi.yaml`; con sus pruebas de contrato y `verificar:ruptura` |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde; Aprobación depende de Firma, nunca al revés |
| Errores en el catálogo | `FIRMA_OBSERVACION_INVALIDA` y `VALIDACIONOCS_REQUIERE_VERSION_NUEVA` en `contratos/comun/errores.md`, `ErroresFirma` y `ErroresValidacionocs`, verificados por sus pruebas |
| JavaDoc | El cierre del documento, la carrera con la subida, el orden turno → observación, la etapa activa anterior, la idempotencia, el bloqueo hasta la versión nueva y el evento (T021) |
| Seguridad | El turno y la observación, en el servidor; la SPA solo los muestra. El permiso `DISTRIBUTIVO:FIRMA` en la operación nueva. La generación bloqueada al registrar, para que una subida y un rechazo no ganen los dos. `firma.rechazo_en_firma` sin reescrituras; cada rechazo en la bitácora de los dos servicios |
| Decisión registrada | D-105, antes de implementar |

## Equipo de demostración (T023)

Pendiente: se completa después de fusionar, solo de lectura (`quickstart.md` §4).
