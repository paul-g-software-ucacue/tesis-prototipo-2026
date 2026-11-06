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
