# Evidencia: Registrar cada cambio en la bitácora

F17-H01 (issue #69). Cada sección dice qué se verificó, cómo y con qué resultado.

## Todo cambio guardado pasa por el registro de cambios (T020, FR-002 y FR-005)

**Cómo:** se recorrieron los servicios de `aplicacion/` de los seis servicios y se listó cada llamada que escribe en un repositorio (`guardar`, `registrar`, `insertar`, `quitar`, `reemplazar`, `borrar`, `crear`, `agregar`, `inicializar`), con la acción que registra en la misma transacción. Después se revisaron los traductores de `aplicacion/cargas/`.

### Casos de uso que escriben

| Servicio | Caso de uso | Escribe | Acción que registra |
|---|---|---|---|
| Generación | `CrearDistributivoServicio` | `distributivos.crear` | `DISTRIBUTIVO_CREADO` |
| Generación | `ProponerDocenteServicio` | `guardarPropuesta`, `quitarPropuesta`, `agregarParcial` | `PROPUESTA_GUARDADA`, `PROPUESTA_QUITADA` |
| Generación | `AsignarActividadServicio` | `guardarActividad`, `quitarActividad`, `agregarParcial` | `ACTIVIDAD_GUARDADA`, `ACTIVIDAD_QUITADA` |
| Generación | `JustificarInconsistenciaServicio` | `justificaciones.guardar` | `INCONSISTENCIA_JUSTIFICADA` |
| Generación | `ConsolidarServicio` | `guardarConsolidacion` | `DISTRIBUTIVO_CONSOLIDADO` |
| Generación | `InformeSustentadoServicio` | `almacen.guardar`, `guardarInforme` | `INFORME_ADJUNTADO` |
| Académico | `RegistrarPeriodoServicio` | `repositorio.registrar` | `PERIODO_REGISTRADO` |
| Académico | `CorregirPeriodoServicio` | `repositorio.guardar` | `PERIODO_CORREGIDO` |
| Académico | `CambiarEstadoPeriodoServicio` | `repositorio.guardar` | `PERIODO_ESTADO_CAMBIADO` |
| Académico | `AbrirPeriodoServicio` | (Normativa asocia la versión) | `PERIODO_ABIERTO` |
| Académico | `MantenerEstructuraServicio` | `registrar`, `guardar` | `UNIDAD_REGISTRADA`, `UNIDAD_CORREGIDA`, `UNIDAD_INACTIVADA`, `UNIDAD_REACTIVADA` |
| Académico | `MantenerCarrerasServicio` | `registrar`, `guardar` | `CARRERA_*`, `MALLA_*`, `ASIGNATURA_REGISTRADA`, `ASIGNATURA_CORREGIDA`, `ASIGNATURA_INACTIVADA`, `ASIGNATURA_REACTIVADA` |
| Académico | `MantenerOfertaServicio` | `registrar`, `guardar` | `ASIGNATURA_PARALELO_*` |
| Académico | `MantenerMatriculasServicio` | `registrar`, `guardar` | `MATRICULA_REGISTRADA`, `MATRICULA_CORREGIDA` |
| Docentes | `MantenerDocentesServicio` | `registrar`, `guardar` | `DOCENTE_REGISTRADO`, `DOCENTE_CORREGIDO`, `DOCENTE_ESTADO_CAMBIADO` |
| Docentes | `MantenerCargosServicio` | `registrar`, `guardar` | `CARGO_REGISTRADO`, `CARGO_CORREGIDO` |
| Docentes | `MantenerProyectosServicio` | `registrar`, `guardar` | `PROYECTO_REGISTRADO`, `PROYECTO_CORREGIDO` |
| Normativa | `CrearVersionServicio` | `repositorio.guardar` | `VERSION_CREADA` |
| Normativa | `EditarVersionServicio` | `repositorio.guardar` | `VERSION_EDITADA` |
| Normativa | `AsociarVersionAPeriodoServicio` | `repositorio.guardar` | `VERSION_ASOCIADA` |
| Roles | `GestionarRolesServicio` | `roles.guardar`, `roles.borrar` | `CREAR_ROL`, `EDITAR_ROL`, `BORRAR_ROL` |
| Roles | `AsignarRolesServicio` | `asignaciones.reemplazar`, `carreras.quitar` | `ASIGNAR_ROL`, `QUITAR_ROL`, `QUITAR_CARRERA` |
| Roles | `AsignarRolesInicialesServicio` | `asignaciones.inicializar` | `INICIALIZAR` |
| Roles | `AsignarCarreraServicio` | `carreras.guardar`, `carreras.quitar` | `INDICAR_CARRERA`, `QUITAR_CARRERA` |
| Roles | `DesignarResponsableServicio` | `designaciones.guardar` | `DESIGNAR_RESPONSABLE` |
| Validación OCS | `CargarConfiguracionServicio` | `repositorio.guardar` | `CONFIGURACION_FLUJO_CARGADA` |

### Escrituras que no son un cambio del negocio

| Dónde | Qué escribe | Por qué no va a la bitácora |
|---|---|---|
| Generación, `agregarParcial` | El parcial de una carrera, la primera vez que recibe una propuesta o una actividad | Es parte del mismo cambio, que ya se registra con su propuesta o su actividad. |
| Roles, `RecibirEventosServicio` | `evento_recibido` y la copia de los periodos de Académico | Es la réplica de un evento: el cambio ya lo registró Académico (`PERIODO_*`). |
| Usuarios, `RegistrarEventoRecibidoServicio` | `evento_recibido` y la propia entrada | Es la bitácora. |
| Usuarios, `IniciarSesionServicio` | Intentos fallidos de inicio de sesión | Protección de la cuenta, no un dato del negocio; fuera del alcance de D-90. |
| Normativa, migraciones `V2`, `V4`, `V5` y `V6` | La versión 1 y sus ediciones, con el usuario `sistema`, directo en `registro_cambio` | No pasan por el servicio: son datos iniciales de la base, anteriores a la bitácora. |
| Generación, `PublicarEventoDePruebaServicio` | `evento_prueba` | Se retira en T027. |

### Las cargas de archivo

Los traductores de `academico/aplicacion/cargas/`, `docentes/aplicacion/cargas/` y `normativa/aplicacion/cargas/` no escriben en los repositorios: aplican cada fila con los servicios de mantenimiento (`registrar`, `corregir`, `cambiarEstado`, `ofertar`, `crear`, `editar`), con el usuario que confirmó. No se corrigió ninguno.

Las pruebas de integración de los aplicadores lo comprueban: `AplicadorAcademicoIntegrationTest` (estructura y carreras) y `AplicadorDocentesIntegrationTest` (cargas 06, 08 y 09) afirman que cada fila del registro local tiene su entrada publicada, con el usuario que confirmó la carga. La de Docentes afirma además que ninguna entrada lleva el correo ni el teléfono de la carga 06 (D-91).

### Todo registro corre en una transacción

`@ApplicationModuleListener` es un oyente transaccional: un evento publicado fuera de una transacción no se guarda en el registro de publicaciones, y se perdería sin error. Al revisar cada llamada a `registrar` se encontraron cuatro sin transacción, todas en Roles: `crear`, `editar` y `borrar` un rol (`GestionarRolesServicio`) y los roles iniciales (`AsignarRolesInicialesServicio`). Desde T017 corren en `Transaccion`, con su prueba (`crearEditarYBorrar_cuandoGuardan_entoncesGuardanYRegistranDentroDeUnaTransaccion`). Validación OCS no tenía transacción y suma la suya (T018). Las demás llamadas ya corrían dentro de `transaccion.ejecutar`.

## Pruebas y comprobaciones (T030)

Todas el 2026-11-06, sobre la rama `039-bitacora-cambios`.

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 2 563 pruebas, 0 fallas | `./gradlew test --rerun` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos. La más baja, Roles: 76,7 %. Generación 90,6 %, Académico 84,6 %, Docentes 82,1 %, Normativa 79,6 %, Validación OCS 92,9 %, Usuarios 89,0 %, `:plataforma:eventos` 96,0 % | `jacocoTestReport jacocoTestCoverageVerification`, JaCoCo regenerado |
| `integrationTest` completo | 240 pruebas, 0 fallas | `./gradlew integrationTest --rerun`, como el job de la CI |
| Spotless y SpotBugs | En verde | `spotlessCheck spotbugsMain` |
| Los 28 scripts de `perfil-consolidado` | Los 28 pasan, en el orden del workflow, sobre un perfil nuevo (`down -v` y `up --wait`) | `probar_bitacora.sh` en el lugar de `probar_eventos.sh`; después, `probar_actividades.sh` y otra vez `probar_bitacora.sh`, para comprobar que deja la base lista |
| `deploy/equipo/probar_equipo.sh` | Todos sus pasos pasan (0 a 12) | Con el perfil abajo, como en la CI |
| shellcheck | 0 hallazgos en los 43 scripts de `deploy/` | v0.11.0, con una copia nueva en el scratchpad |
| Gitleaks | Sin fugas en 98 commits | v8.30.1 en modo `git`, sobre un clon, con `.gitleaks.toml` |

### `probar_bitacora.sh` (contracts/demostracion.md)

| Paso | Resultado |
|---|---|
| 1. Cargas, periodo abierto y distributivo | OK |
| 2. Actividad de 4 a 6 horas (US1-1) | OK: una entrada con el usuario, el periodo, `anterior.horas = 4` y `nuevo.horas = 6` |
| 3. Agregar y quitar (US1-2, US1-3) | OK |
| 4. Regla dura (US1-4) | OK: 409 `GENERACION_HORAS_EXCEDIDAS` y ninguna entrada |
| 5. Un cambio en cada servicio (SC-001) | OK: Académico, Docentes, Normativa, Roles y Validación OCS |
| 6. Cargas (FR-002) | OK: 600 filas aplicadas por `datos.demo`, 600 entradas |
| 7. Identidad detenido (SC-002, FR-008) | OK: 10 cambios sin error y 10 publicaciones pendientes; las 10 entradas llegan una vez antes de 75 s |
| 8. Entrega repetida (FR-007) | OK |
| 9. Modificar la bitácora (SC-005) | OK: `UPDATE`, `DELETE` y `TRUNCATE` fallan con `identidad` y con el superusuario |
| 10. Secretos y datos personales (SC-004, FR-014) | OK: 0 claves, 0 valores secretos, 0 teléfonos |
| 11. Demora (SC-003) | OK: 0,112 s con identidad y 0,109 s sin ella |

### La carga de casi 5 MB (SC-004 de F01-H04)

Cada fila de una carga publica ahora su entrada en la misma transacción. `ConfirmarLasDiezCargasIntegrationTest` confirma 10 568 docentes en **13,0 s** sola (la medición de `specs/025-confirmar-carga` fue 15,5 s) y en **32,9 s** dentro de `integrationTest` completo, con los módulos en paralelo; el límite es 60 s. Una corrida que juntó `check` e `integrationTest` en un solo Gradle llegó a 76,9 s por la carga de la máquina: la CI los corre en jobs separados.

### Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_bitacora.sh`, arriba, y las pruebas de US1 a US4 |
| Contrato publicado y verificado | `EntradaBitacora` y el receptor en `contratos/usuarios/openapi.yaml`, con `EventosContratoTest` y `BitacoraSinBorradoContratoTest`; Generación sin `eventos-prueba` |
| Prueba de arquitectura | ArchUnit y Spring Modulith de cada módulo y de las tres aplicaciones, en verde |
| Errores en el catálogo | `USUARIOS_ENTRADA_BITACORA_INVALIDA` con sus 8 columnas, cubierto por la prueba de contrato del receptor |
| Validaciones de entrada (principio XIV) | El contenido del evento en el contrato y en `EntradaBitacora`, con casos válidos e inválidos. No hay formularios nuevos |
| JavaDoc | `ReceptorLocal`, `EntradaBitacora`, `RepositorioBitacora`, los registros de cambio y los puertos `RegistroDeCambios` (T029) |
| Seguridad | SpotBugs y Gitleaks en verde; Grype en el pull request |
| Glosario | Sin términos nuevos (T001); D-92 no agrega ninguno |

### Pull request (#193)

La primera corrida en borrador falló en «5 · imágenes». Grype encontró GO-2026-6860, una vulnerabilidad alta en `golang.org/x/net` de la imagen de Caddy, y la falla no venía de esta historia. Se corrigió aparte en el #194 (D-93), que también subió `klauspost/compress` en Caddy, `apt` en las imágenes Java y `musl` en la del frontend. Con `main` traído a la rama (`c79f86d`), las 12 comprobaciones del #193 quedaron en verde. Eso incluye `test-integracion`, `sca`, «5 · imágenes» y `perfil-consolidado` (17 min), que corren con el PR ya listo.
