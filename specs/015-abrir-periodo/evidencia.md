# Evidencia: F02-H01

Registro de las comprobaciones de la historia (issue #32): registrar el periodo académico, abrirlo con su versión de normativa y hacerlo avanzar por sus estados, hasta cerrado (RF-13, RF-19, D-69). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, dominio, migración y repositorio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Contrato primero (T003) | confirmada | `contratos/academico/openapi.yaml`, nuevo: 6 operaciones con su `x-mdd-permiso` (`DISTRIBUTIVO:LECTURA` o `ESCRITURA`), 6 esquemas sin `allOf`; los 5 códigos `ACADEMICO_*` en `errores.md`; la descripción de `asociarPeriodo` de Normativa cita D-69. `openApiGenerate` y `generar:api` (con `academico.d.ts`) regeneran; `tipos` y `verificar:ruptura` en verde. El catálogo tiene descripciones fijas, así que `ACADEMICO_TRANSICION_NO_PERMITIDA` no dice el estado actual: se ajustaron `errores.md`, research §5 y T019. | 2026-10-21 |
| Dominio (T004, T005) | confirmada | `EstadoPeriodoTest` (2) y `PeriodoAcademicoTest` (9), sin dobles: el orden de los estados; las validaciones del código, del nombre (en blanco y más de 120) y de las fechas, con su campo y su motivo; `corregido` con sus cambios y el rechazo de un periodo cerrado. Fallaron antes del código (no compilaban). | 2026-10-21 |
| Esquema (T006a, T006) | confirmada | `AcademicoEsquemaIntegrationTest` (2), con Testcontainers: las dos tablas de V2 y sus `check` (código, nombre en blanco, `fin <= inicio`, estado, acción y código repetido). Fallaron sin V2 («bad SQL grammar», tablas inexistentes) y pasaron con `V2__periodos_academicos.sql`. | 2026-10-21 |
| Repositorio y registro (T007, T008) | confirmada | `RepositorioPeriodosJdbcIntegrationTest` (5): registra y lee igual, con quién y cuándo; un código repetido no se guarda (`registrar` devuelve falso, sin consulta previa); lista por inicio descendente; guarda los datos y el estado; `bloquear` toma el candado (`FOR UPDATE NOWAIT` desde otra conexión falla). `RegistroDeCambiosJdbcIntegrationTest` (1): las cuatro acciones con su detalle en `jsonb`. Fallaron antes de los adaptadores (no compilaban). | 2026-10-21 |
| Adaptador hacia Normativa (T009, T010) | confirmada | `NormativaDesdePuertosTest` (4), con dobles escritos a mano de los puertos de Normativa: sin asignación da vacío; `PeriodoConVersion` → `PeriodoConOtraVersion`; `VersionNoEncontrada` → `VersionInexistente`. El adaptador solo importa `ec.mdd.normativa.puertos.entrada`. | 2026-10-21 |
| Límites del proceso (T011) | confirmada | `ModulosTest` del proceso `datos` en verde con la dependencia nueva. Con una referencia a `ec.mdd.normativa.dominio.VersionNormativa` agregada a mano en el adaptador, falló con `Violations`; se quitó y volvió a verde. | 2026-10-21 |
| Consulta y controlador (T012, T013) | confirmada | `ConsultarPeriodosServicioTest` (3, Mockito). `AcademicoContratoTest`: listar y consultar validan contra el contrato, con y sin `version`; un código que no existe da 404 `COMUN_RECURSO_NO_ENCONTRADO`. `PermisosAcademicoTest`: las 6 operaciones exigen su permiso. `ErroresAcademicoTest`: los 5 códigos coinciden con `errores.md`. | 2026-10-21 |

## Escenario «Abrir el periodo» (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Registrar y abrir (T014) | confirmada | `RegistrarPeriodoServicioTest` (3) y `AbrirPeriodoServicioTest` (8), con Mockito: abrir llama a Normativa **fuera** de la transacción y registra `PERIODO_ABIERTO` **dentro** (una transacción de prueba que sabe si está dentro); cerrado, misma versión, otra versión (también en En aprobación), versión inexistente y rechazo de Normativa, sin registro. Fallaron antes de los servicios (no compilaban). | 2026-10-21 |
| Contrato de registrar y abrir (T015) | confirmada | `AcademicoContratoTest` (+8): 201 en Planificación sin `version`; 409 `ACADEMICO_CODIGO_REPETIDO`; 400 con el campo `fin` o `nombre`; abrir da 200 con `version`; 409 `ACADEMICO_PERIODO_CON_VERSION`; 400 con el campo `version` («La versión de normativa 9 no existe»); 404. Las 8 fallaron antes (501). | 2026-10-21 |
| Con la Normativa real (T016, SC-002) | confirmada | `AbrirPeriodoIntegrationTest` (4), el proceso `datos` completo con Testcontainers: abrir deja la versión en Normativa, la consulta de Académico dice la misma, y editarla lanza `VersionEnUso`; `VERSION_ASOCIADA` y `PERIODO_ABIERTO` con el mismo usuario; dos periodos con la misma versión; si falla el registro de Académico, la asociación de Normativa queda y abrir otra vez no cambia nada; dos aperturas simultáneas con versiones distintas dejan una y la otra recibe `PeriodoConOtraVersion`. Las 4 pasaron a la primera, porque el código ya estaba; con la llamada a Normativa movida a mano dentro de la transacción de Académico, falló la del registro, y al restaurarla volvió a verde. La base se descarta al terminar, así que la prueba no borra lo que crea. | 2026-10-21 |

## Escenario «Estados del periodo» (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Orden de los estados (T017, SC-003) | confirmada | `PeriodoAcademicoTest` (+3), sin dobles: los 16 pares de origen y destino (el mismo estado no cambia; desde Cerrado, `PeriodoCerrado`; el siguiente cambia; los demás, `TransicionNoPermitida` con el actual y el pedido); salir de Planificación sin versión da `PeriodoSinVersion`; saltar a Vigente sin versión da `TransicionNoPermitida`. Fallaron antes de `conEstado` (no compilaban). | 2026-10-21 |
| Cambiar el estado (T018) | confirmada | `CambiarEstadoPeriodoServicioTest` (6), con Mockito: consulta a Normativa fuera de la transacción, bloquea, guarda y registra `PERIODO_ESTADO_CAMBIADO` con el anterior y el nuevo; sin versión, salto, retroceso y el mismo estado no guardan ni registran; 404. Fallaron antes del servicio (no compilaban). | 2026-10-21 |
| Contrato del cambio de estado (T019) | confirmada | `AcademicoContratoTest` (+5): 200 a En aprobación con versión; 409 `ACADEMICO_PERIODO_SIN_VERSION`; 409 `ACADEMICO_TRANSICION_NO_PERMITIDA` al saltar a Vigente y al volver de Vigente a Planificación, con la descripción del catálogo; 400 con un estado que no es del contrato. Fallaron 4 antes (501); la del 400 ya pasaba, porque la valida el contrato antes del controlador. | 2026-10-21 |

## Escenario «Periodo cerrado» (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantalla «Periodos» (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
