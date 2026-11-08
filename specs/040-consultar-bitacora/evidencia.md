# Evidencia: Consultar la bitácora

F17-H02 (issue #70). Cada sección dice qué se verificó, cómo y con qué resultado. Todas las comprobaciones son del 2026-11-07, sobre la rama `040-consultar-bitacora`, antes del pull request.

## Pruebas y comprobaciones (T028)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 2 673 pruebas, 0 fallas | `./gradlew test --rerun` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos. La más baja, Roles: 76,9 %. Generación 90,6 %, Validación OCS 92,9 %, Académico 84,7 %, Usuarios 84,3 %, Docentes 82,1 %, Normativa 79,6 % | `jacocoTestReport jacocoTestCoverageVerification` |
| `integrationTest` completo | 253 pruebas, 0 fallas | `./gradlew integrationTest --rerun` |
| Spotless y SpotBugs | En verde | `spotlessCheck spotbugsMain` |
| SPA: tipos, ESLint y Prettier | En verde | `tsc --noEmit` de los tres `tsconfig`, `eslint . --max-warnings 0`, `prettier --check .`, en `node:24-alpine` |
| SPA: Vitest | 507 pruebas en 37 archivos, 0 fallas | `vitest run` |
| SPA: Playwright | Los 4 casos de `e2e/bitacora.spec.ts` en Chromium, con axe-core (WCAG 2.1 AA) sin fallas | `playwright test bitacora --project=chromium`, en `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Los 29 scripts de `perfil-consolidado` | Los 29 pasan, en el orden del workflow, sobre un perfil nuevo (`down -v` y `up --wait`), con `probar_consulta_bitacora.sh` después de `probar_bitacora.sh` | Uno tras otro, como el workflow. `deploy/equipo/probar_equipo.sh` no se corrió: va con el perfil abajo y esta historia no toca `deploy/equipo/` |
| `probar_bitacora.sh` dos veces seguidas (T025) | Los 11 pasos, las dos veces. La regla de D-94 no le quita entradas: sus cambios usan valores distintos | Sobre el mismo perfil |
| shellcheck | 0 hallazgos en `probar_consulta_bitacora.sh` y `probar_bitacora.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 61 archivos cambiados ni en el historial | v8.30.1 con `.gitleaks.toml`: modo `dir` sobre una copia de los archivos cambiados, y modo `git` sobre un clon |

### `probar_consulta_bitacora.sh` (contracts/demostracion.md)

| Paso | Resultado |
|---|---|
| 1. Cargas, periodo abierto, distributivo y actividad de 4 horas | OK |
| 2. Vicerrector por `coordinacion.demo` y hoy (US1-1, SC-001) | OK: 38 entradas, todas de ese usuario, las mismas que la consulta de solo lectura sobre la base hasta el corte |
| 3. Administrador, la misma consulta | OK: el mismo total |
| 4. Por servicio y acción, identificador, periodo, combinados y sin coincidencias (US1-3, US1-4, US1-5) | OK |
| 5. Página 2 con el corte de la 1 | OK: sin repetir entradas y con el mismo total, aunque llegó una entrada nueva |
| 6. De 4 a 4 y de 4 a 6 (US3-1, US3-2, FR-012) | OK: ninguna entrada por el primero; una de 4 a 6 por el segundo |
| 7. Coordinador y los otros 7 usuarios sin permiso (US2, SC-002) | OK: 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO` en las dos operaciones, sin el nombre del permiso; sin sesión, 401 |
| 8. Filtros inválidos (FR-010) | OK: 400 `COMUN_SOLICITUD_INVALIDA` con el detalle en `hasta`, `accion`, `tamano` e `identificador` |
| 9. Solo lectura (SC-005, FR-006) | OK: la bitácora no cambia durante los pasos 2 a 4 ni 7 y 8 |

### Un fallo que encontró el script

En su primera corrida, el paso 4 respondió 400 con `servicio=generacion`, un valor válido. Spring convierte un enumerado generado por su nombre Java (`GENERACION`) y no por el valor del contrato. La prueba de contrato solo probaba un servicio inválido. Se agregó la prueba con los seis valores (`dadoUnServicioDeLaLista_…`), que falló, y después `ServicioDeBitacoraDesdeTexto`, un `Converter` con `ServicioDeBitacora.fromValue`. Un valor fuera de la lista sigue respondiendo 400.

## La consulta con 10 000 entradas (SC-003)

`RepositorioBitacoraConsultaIntegrationTest` inserta 10 000 entradas y pide la primera página filtrada por usuario y fechas, y por identificador. La prueba completa, con una consulta de calentamiento y las dos medidas, tarda **0,154 s**; el límite es 2 s por consulta. No hizo falta el índice `(usuario, momento)` (research §5).

## Guardados sin cambios (D-94)

### Las seis acciones que faltaban (T018 a T023)

| Acción | Dónde se compara | Pruebas |
|---|---|---|
| `ACTIVIDAD_GUARDADA` | `ActividadNoLectiva.mismasHorasQue` (4 y 4.00 son iguales) | `AsignarActividadServicioTest`, `ActividadNoLectivaTest` |
| `PROPUESTA_GUARDADA` | `Propuesta.mismosDatosQue` (docente, tipo y texto); `otroTextoQue` marca `textoCambiado` | `ProponerDocenteServicioTest` |
| `INCONSISTENCIA_JUSTIFICADA` | `JustificacionDeInconsistencia.mismoTextoQue`; con otro texto, `textoCambiado` | `JustificarInconsistenciaServicioTest` |
| `MATRICULA_CORREGIDA` | `Matricula.equals` | `MantenerMatriculasServicioTest` |
| `EDITAR_ROL` | `Rol.mismosDatosQue` (permisos en cualquier orden) | `GestionarRolesServicioTest` |
| `INDICAR_CARRERA` | La carrera que ya tiene | `AsignarCarreraServicioTest` |

En todas, la comparación va después de las validaciones, y un guardado sin cambios no guarda nada ni llama a `RegistroDeCambios`.

Una prueba existente cambió con la regla: `unDocenteExcedido_cuandoSeVuelveAProponerIgual_entoncesGuarda` pasa a `…_entoncesSeAceptaSinGuardarNada`. La propuesta repetida sigue aceptándose, con el aviso de horas, pero ya no se guarda.

**Efecto en el consolidado.** `RepositorioConsolidacionJdbc` toma el último cambio de propuestas y actividades para saber si el consolidado quedó desactualizado. Un guardado de 4 a 4 ya no lo marca, y es lo correcto, porque nada cambió.

**La carga 10.** Al implementar se vio que la carga 10 nunca corrige una fila igual: `MotorDelLote` la marca `IGUAL` y solo llama a `corregir` con las `CAMBIA`. La regla de `MantenerMatriculasServicio` queda como resguardo de una llamada directa (research §6, corregido).

### Las que ya cumplían (T024)

| Servicio | Prueba de «sin cambios» |
|---|---|
| Académico | `CorregirPeriodoServicioTest`, `CambiarEstadoPeriodoServicioTest`, `AbrirPeriodoServicioTest`, `MantenerEstructuraServicioTest` (corrección y estado), `MantenerCarrerasServicioTest` (carrera y estado), `MantenerOfertaServicioTest` (corrección y estado) |
| Académico, agregadas en esta historia | `MantenerCarrerasServicioTest`: `dadaUnaMallaYaDerogada_…` y `dadaUnaAsignaturaConLosMismosDatosOElMismoEstado_…`, que pasan con el código actual |
| Docentes | `MantenerDocentesServicioTest` (corrección y estado), `MantenerCargosServicioTest`, `ProyectosServiciosTest` |
| Normativa | `EditarVersionServicioTest`, `AsociarVersionAPeriodoServicioTest` |
| Roles | `AsignarRolesServicioTest`, `DesignarResponsableServicioTest` |

## Desviaciones del plan

- **Validación en la SPA sin `zod` ni `react-hook-form`.** Se usaron funciones propias y `Campo` y `Eleccion` de `pantallas/comun/`, como los demás formularios de datos. Las reglas son las mismas del contrato.
- **axe-core en `bitacora.spec.ts`,** como `flujo.spec.ts`, en lugar de `accesibilidad.spec.ts`.
- **El momento en 24 horas.** `es-EC` usa 12 horas con «p. m.»; para una auditoría se fijó `hourCycle: 'h23'`.
- **Un `servicio` fuera de la lista responde 400 sin detalle.** La plataforma solo detalla los enumerados de un cuerpo (`ValorNoPermitido`). La SPA elige el servicio de una lista (contracts/usuarios.md).

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_consulta_bitacora.sh`, arriba; `BitacoraContratoTest`, `Bitacora.test.tsx` y `e2e/bitacora.spec.ts` para US1 y US2; las pruebas de T018 a T024 para US3 |
| Contrato publicado y verificado | `consultarBitacora` y `listarAccionesDeBitacora` en `contratos/usuarios/openapi.yaml`, con `BitacoraContratoTest` y `PermisosUsuariosTest`; `BitacoraSinBorradoContratoTest` sigue en verde |
| Prueba de arquitectura | ArchUnit y Spring Modulith de los módulos, en verde |
| Errores en el catálogo | Sin códigos nuevos. `FiltroDeBitacoraInvalido` en la fila de `COMUN_SOLICITUD_INVALIDA` de `contratos/comun/errores.md` |
| Validaciones de entrada (principio XIV) | Cada filtro en el contrato, en `FiltroDeBitacora` y `PaginaPedida`, y en `validacionDeBitacora.ts`, con casos en sus límites |
| JavaDoc | Los puertos, el caso de uso, el filtro, la página, el controlador, el conversor y las comparaciones de D-94 (T027) |
| Seguridad | `@PreAuthorize` con `BITACORA:LECTURA` y los permisos reales de la matriz D-16 en la prueba; SQL parametrizado (con comillas y `%` en la prueba de integración); SpotBugs y Gitleaks en verde |
| Glosario | Sin términos nuevos (T001) |
| Decisión registrada | D-94, antes de implementar |

## Comprobación del autor en el perfil local

El 2026-11-07, sobre el perfil consolidado construido desde la rama (`http://localhost:8081`), con los datos de prueba cargados con los pasos de preparación de `probar_consulta_bitacora.sh` (sin borrar al final).

| Parte | Resultado |
|---|---|
| A. Consulta (US1) | OK. Con `datos.demo`, Docentes, `CARGO_REGISTRADO` e identificador `0109990044`: 1 entrada, el cargo `COORDINADOR_CARRERA`, sin periodo. |
| B. Permisos (US2) | OK. `coordinacion.demo` no ve «Bitácora» en el menú, y `/bitacora` muestra «Acceso no autorizado». |
| C. Sin cambios (US3, D-94) | OK. La 140-b guardada otra vez con 4 horas no dejó fila en `generacion.registro_cambio` ni entrada en la bitácora; de 4 a 6, una de cada una (`horas` 4 → 6), en la bitácora 0,04 s después. |
| SC-004 | **2 minutos** la primera vez, incluido el tiempo de entender los filtros, que el autor no cuenta. No se da por cumplido con este número; se vuelve a medir en el equipo de demostración, ya conociendo la pantalla. |

**Ajuste de usabilidad que salió de la prueba.** En el filtro «Usuario» se escribió el nombre de una docente, que la bitácora no guarda (D-91). Con la aprobación del autor, el campo pasó a «Quién hizo el cambio», y las pistas de ese campo y de «Identificador» dicen qué va en cada uno (`contracts/spa.md`). Vitest suma una prueba de los dos textos.

**La tabla en una ventana angosta.** El resumen del objeto se partía en muchas líneas y empujaba «Ver valores» fuera de la vista. Con la aprobación del autor, ahora va en una sola línea con «…» (el texto completo, al pasar el puntero; el detalle, en «Ver valores»), y la columna «Usuario» pasó a «Quién». Vitest suma una prueba: 509 en total.

**Fuera de esta historia.** En «Asignar actividad», el docente se elige de una lista nativa sin búsqueda, igual que en Cargos y Proyectos. El autor pidió poder escribir y filtrar por nombre: va en un pull request aparte, porque esas pantallas son de otras historias.

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Comprobaciones del PR #199 | 12 de 12, con `probar_consulta_bitacora.sh` en `perfil-consolidado` | GitHub | 2026-11-07 |
| Despliegue de F17-H02 | `4ac135b982b7`, «Sano en 93 s», con los 8 contenedores sanos y la prueba de humo en verde (pasos 1 a 5); `demostracion` exitoso | `journalctl`, `docker ps` y GitHub | 2026-11-07 |
| Despliegue del #200 (selector de docentes) | `69f7377848c0`, «Sano en 87 s», con los 8 contenedores sanos y la prueba de humo en verde; `demostracion` exitoso | `journalctl`, `docker ps` y GitHub | 2026-11-07 |
| A y B. Consulta y permisos | El autor consulta la bitácora como Vicerrector, y `coordinacion.demo` no la ve | Comprobación del autor | 2026-11-07 |
| C. Sin cambios (D-94) | La 140-b, en 6 horas, guardada con 6: ninguna entrada. De 6 a 5: `ACTIVIDAD_GUARDADA` de `coordinacion.demo`, `{"horas": "6"}` → `{"horas": "5"}`, llegó en 0,124 s. La entrada de 4 a 4 de F17-H01 (12:02:14) se queda: la bitácora no se borra (FR-014). La bitácora pasó de 617 a 618 entradas | Comprobación del autor y consulta de solo lectura | 2026-11-07 |
| SC-004 | **30 segundos** para encontrar quién cambió la 140-b, ya conociendo la pantalla (límite: 1 minuto) | Comprobación del autor | 2026-11-07 |
| Selector de docentes (#200) | En «Asignar actividad», como `coordinacion.demo`, la lista se acota al escribir | Comprobación del autor | 2026-11-07 |

Para probar el selector, el autor entró primero como Vicerrector y no vio «Asignar actividad»: es lo esperado, porque solo el Coordinador de carrera escribe la oferta y la propuesta de su carrera (D-16).

## Pendiente

- **Rol perdido durante la sesión.** Lo cubren la D-62 y la prueba de la renovación del token: el permiso deja el token en la próxima renovación.
