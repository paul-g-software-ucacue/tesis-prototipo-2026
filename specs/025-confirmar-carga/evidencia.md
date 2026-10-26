# Evidencia: F01-H04

Registro de las comprobaciones de la historia (issue #30): la vista previa de cada carga frente a lo que hay en el sistema, la confirmación que el servicio dueño aplica completa o nada, y el historial de las cargas (RF-10, RF-55, MD-13; ADR-022, D-74). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: decisión, contrato, motor del lote e historial

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| ADR-022 y D-74 | Registrados en `docs/decisiones.md`, con la aprobación del autor del 2026-10-26. ADR-022: el lenguaje publicado en `:plataforma:lenguaje-cargas` (ver la nota del nombre). D-74: los mensajes en «tú» | `docs/decisiones.md` | 2026-10-26 |
| El contrato y los 3 códigos | `VistaPrevia`, `RegistroDeLaVista`, `CambioDeCampo`, `confirmarCarga` (`huella` `^[0-9a-f]{64}$`), `listarHistorial` y `EntradaDeHistorial`; `CARGAS_CARGA_CON_ERRORES`, `CARGAS_VISTA_PREVIA_DESACTUALIZADA` y `CARGAS_CARGA_RECHAZADA` con sus 8 columnas. Se generan los modelos de Java y los tipos de la SPA; `verificar:ruptura` pasa | `contratos/cargas/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-26 |
| El motor del lote, sin dobles | `MotorDelLoteTest`, 9 de 9: nuevos, los que cambian con sus campos, las bajas dentro del alcance (la de otro periodo no cuenta) y los iguales; la huella estable al reordenar y distinta con otro valor; con la misma huella aplica 1 nuevo y 1 cambio, sin tocar bajas ni iguales, y bloquea una vez; con otra huella, `VistaPreviaDesactualizada` y nada aplicado; una regla del dueño en la fila 3 da `RechazoDelLote` con esa fila y deshace también la fila 2; una clave repetida, con la fila de la primera; 100 de cada tipo; el SHA-256 | `./gradlew :plataforma:lenguaje-cargas:check` | 2026-10-26 |
| El historial (V2) | `HistorialDeCargasJdbcIntegrationTest`, 7 de 7 (Testcontainers): guarda y lee cada resultado, del más reciente al más antiguo; la primera `APLICADA` de un hash; la base rechaza un hash que no es hexadecimal, un resultado desconocido, un total negativo, un rechazo sin motivo, una aplicada con motivo y una carga en mayúsculas. `CargasEsquemaIntegrationTest` ve `cargas.historial_carga` | `./gradlew :servicios:cargas:integrationTest` | 2026-10-26 |
| Confirmar e historial en Cargas (Mockito) | `ConfirmarCargaServicioTest` (6): aplicada con su lote (el hash SHA-256 de los bytes y las filas) y su entrada; con errores, `CargaConErrores` sin llamar al dueño y su entrada `RECHAZADA` con `CON_ERRORES`; repetida sin llamar al dueño, con la fecha de la primera; rechazada por el dueño y desactualizada, con su motivo. `ValidarArchivoServicioTest` (7): la vista previa es la del dueño; con errores, solo `filasConErrores`, sin llamar al dueño. `DuenosDeLasCargasTest`: una carga sin dueño, con dos o fuera del diccionario detiene el arranque | `./gradlew :servicios:cargas:test` | 2026-10-26 |
| El contrato, los permisos y el catálogo | `ConfirmacionContratoTest`, 7 de 7, con un dueño y un historial en memoria: la vista previa con sus totales y su huella; confirmar dos veces da `APLICADA` y `REPETIDA` (con la fecha de la primera), y al subirla otra vez, 0 nuevos y 7 iguales; el historial en orden; los tres 409 con su código, el rechazo con `fila 2` y el texto del catálogo del dueño; una huella que no es hexadecimal, 400; un historial de una carga que no existe, 404; cada respuesta valida contra el contrato. `PermisosCargasTest` con las 2 operaciones nuevas; `ErroresCargasTest`, 10 códigos, y un rechazo sin código del dueño no muestra el mensaje técnico. Cargas: 270 pruebas sin fallas | `./gradlew :servicios:cargas:test` | 2026-10-26 |

## Académico A: estructura y carreras

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los traductores de la 01 y la 02 (Mockito) | `TraductoresAcademicoATest`, 5 de 5: los padres antes que los hijos; una unidad nueva inactiva se registra y cambia de estado; una igual no cambia, y una inactivada cambia solo de estado, sin corregirla; el tipo de una unidad no cambia y un `DatoInvalido` se rechaza con su fila y su campo; una carrera con otro nombre y otras jornadas cambia en esos dos campos, y `MATUTINA;NOCTURNA` con `05` ciclos es igual a la registrada; una carrera nueva en cierre se registra y cambia de estado | `./gradlew :servicios:academico:test` | 2026-10-26 |
| **La 01 y después la 02, aplicadas** (PostgreSQL) | `AplicadorAcademicoIntegrationTest`, 4 de 4: 24 unidades y 7 carreras, con el registro de cambios de `datos.demo`, y subidas otra vez, todo igual; **una segunda sede matriz rechaza la fila 26 con `SedeMatrizExistente` y no aplica nada, ni las 25 filas anteriores** (SC-003); una carrera que cambia se corrige y la que no viene queda como estaba (1 cambiada, 1 baja sin aplicar); la misma huella aplicada dos veces da `VistaPreviaDesactualizada` la segunda | `./gradlew :servicios:academico:integrationTest` | 2026-10-26 |
| Los límites del proceso | `ModulosTest` pasa: Académico usa `ec.mdd.plataforma.cargas` (su interfaz nombrada) y Cargas no conoce sus tipos | `./gradlew :apps:datos:test` | 2026-10-26 |

## Académico B: ciclos, asignaturas, oferta y matriculados

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los matriculados (V6) | `MatriculaTest` (6, sin dobles): un total de 0, 1 y 99 999 pasa, y -1 y 100 000 no; la fuente hasta 60; el periodo y la carrera, obligatorios. `RepositorioMatriculasJdbcIntegrationTest` (2): se guardan y se leen por periodo, y la base exige el periodo y la carrera. `AcademicoEsquemaIntegrationTest` ve `academico.matricula` | `./gradlew :servicios:academico:test :servicios:academico:integrationTest` | 2026-10-26 |
| Los traductores de la 03, 04, 05 y 10 (Mockito) | `TraductoresAcademicoBTest`, 4 de 4: en los periodos, las semanas, la resolución y el estado del archivo no cuentan, y una fecha de fin sí; en las asignaturas, `3` y `3.0` son iguales, la malla que no existe se registra antes y una vez, y la carrera o la malla no cambian (`DatoInvalido` en su campo); en la oferta, las bajas solo en los periodos del archivo y las franjas no cuentan; en los matriculados, la clave es `periodo · carrera` | `./gradlew :servicios:academico:test` | 2026-10-26 |
| **Las 6 cargas de Académico en su orden** (PostgreSQL) | `AplicadorAcademicoIntegrationTest`, 5 de 5: la 01, 02, 03, 04, 05 y 10 de prueba dan 24, 7, 2, 300, 165 y 7 nuevos, con 12 mallas, y Roles recibe el aviso de los 2 periodos; **subidas otra vez, ninguna tiene nuevos, cambios ni bajas** (SC-002) | `./gradlew :servicios:academico:integrationTest` | 2026-10-26 |

## Docentes: docentes, cargos y proyectos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los traductores de la 06, 08 y 09 (Mockito) | `TraductoresDocentesTest`, 5 de 5: **los 47 docentes, los 21 cargos y los 11 proyectos del archivo de prueba son iguales a los que construyen las pruebas de Docentes desde el mismo archivo** (la traducción es fiel, con la ficha académica y las listas); pasar un docente a licencia cambia solo el estado y se aplica con `cambiarEstado`; el tipo de identificación no cambia; un `DatoInvalido` se rechaza con su campo; un cargo nuevo se registra con `MantenerCargos` | `./gradlew :servicios:docentes:test` | 2026-10-26 |
| **La 06, 08 y 09 en su orden** (PostgreSQL) | `AplicadorDocentesIntegrationTest`, 3 de 3: 47 docentes, 21 cargos y 11 proyectos aplicados, y subidos otra vez, todos iguales; **un segundo coordinador vigente de la misma carrera rechaza la fila 23 con `CoordinadorVigente` (RN-12) sin aplicar ninguno de los 21 cargos**; un cargo para un docente de medio tiempo, `DocenteNoHabilitado`, sin cambios | `./gradlew :servicios:docentes:integrationTest` | 2026-10-26 |

## Normativa: la política

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Las 10 en orden

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantalla

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- **D-74.** El plan reservaba D-74 para el índice de ADR-022. Como el autor aprobó dos decisiones en el plan, ADR-022 es el módulo `:plataforma:lenguaje-cargas` y D-74 es el trato de «tú» en los mensajes (research §10).
- **El nombre del módulo.** El plan lo llamaba `:plataforma:cargas`, pero Gradle trata como el mismo artefacto a dos proyectos con el mismo nombre, y `:servicios:cargas` ya se llama así: la compilación daba una dependencia circular. El módulo es `:plataforma:lenguaje-cargas`; su paquete sigue siendo `ec.mdd.plataforma.cargas`.
