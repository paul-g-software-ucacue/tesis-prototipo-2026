# Evidencia: F04-H02

Registro de las comprobaciones de la historia (issue #41): crear una versión nueva de la normativa, editarla mientras no rija un periodo y asociarla a un periodo (RF-19). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son la tabla pública de la política; las contraseñas de local y de la CI son ficticias.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |
| Backend | `./gradlew check integrationTest` en verde | 524 pruebas en todo el backend. Normativa: 68 unitarias y de contrato, con 71,2 % de líneas, y 18 de integración. Roles: 11 de integración. Plataforma de errores: 17, con 77,2 %. |
| SPA | Vitest y Playwright en verde | 98 pruebas de Vitest con 95,59 % de líneas; 49 de Playwright. |

## Base: contrato, dominio, persistencia y permiso

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Contrato primero (T002) | confirmada | `contratos/normativa/openapi.yaml` gana `crearVersion`, `editarVersion`, `asociarPeriodo` (`PARAMETROS:ESCRITURA`) y `consultarPeriodo` (`PARAMETROS:LECTURA`), con `copiadaDe` y `periodos` en las versiones. `errores.md` gana `NORMATIVA_VERSION_EN_USO`, `NORMATIVA_CODIGO_REPETIDO` y `NORMATIVA_PERIODO_CON_VERSION`. `openApiGenerate` genera `VersionesApi` y `PeriodosApi`; `generar:api`, los tipos de la SPA. Los cuerpos se escribieron sin `allOf`, para que el generador dé clases simples, y `origen` admite 120 caracteres, como su columna. | 2026-10-18 |
| Dominio (T003, T004) | confirmada | Sin dobles: `VersionNormativaTest` (14), `FilaCargaHorariaTest` (7) y `TablaCargaHorariaTest` (5). `copiaDe` copia la tabla, las reglas y el tipo de IST y registra `VersionNormativaCreada`. `editada` devuelve un `CambioDeValor` por valor que cambia y ninguno si nada cambia (también con 40 y 40.00). Una fila fuera de rango lanza `FilaInvalida` con su perfil, su tipo y el motivo «Las horas de clase (13) no están entre el mínimo y el máximo (8 a 12)». Una fila, una regla o un parámetro de más o de menos lanza `CambioNoPermitido`. Una versión asociada lanza `VersionEnUso`. Las pruebas se escribieron antes que el código y no compilaban. | 2026-10-18 |
| Parámetros válidos | agregada | No estaba en las tareas: la base guarda los parámetros como `numeric(10, 2)` y redondearía sin aviso. `editada` rechaza un valor negativo, con más de dos decimales o desde 100 000 000, con `CambioNoPermitido` y su campo. | 2026-10-18 |
| Catálogo (T005) | confirmada | `ErroresNormativaTest` (2): los tres 409 coinciden con `errores.md`; cada excepción se sumó en la tarea que la creó. | 2026-10-18 |
| Persistencia (T006, T007) | confirmada | `V3__versiones_y_periodos.sql`. Testcontainers: `VersionesNuevasJdbcIntegrationTest` (6): una copia de la 1 se guarda y se reconstruye igual, valor por valor; una edición reemplaza las filas, y si falla a mitad no queda nada cambiado; dos creaciones simultáneas obtienen 2 y 3; una segunda transacción espera el `FOR UPDATE`; un periodo con otra versión viola la restricción única. `RegistroDeCambiosJdbcIntegrationTest` (1): las tres acciones con su detalle en `jsonb`. `PoliticaDe2024IntegrationTest` (4) sigue en verde. | 2026-10-18 |
| Los candados importan | confirmada | Se quitaron el candado de la numeración y el `FOR UPDATE`: fallaron las dos pruebas de concurrencia. Se volvió a poner el código. | 2026-10-18 |
| Vuelta atrás segura (D-55) | confirmada | `NormativaEsquemaIntegrationTest`: la versión anterior, que conoce V1 y V2, arranca sobre el esquema con V3. | 2026-10-18 |
| Permiso del Vicerrector (T008, D-67) | confirmada | `MatrizD16IntegrationTest` con `PARAMETROS:ESCRITURA` en la fila de `VICERRECTOR`: falló antes de la migración y pasó con `V5__vicerrector_escribe_normativa.sql`. Las 11 pruebas de integración de Roles en verde. | 2026-10-18 |

## Escenario «Versión nueva» (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Aplicación (T009) | confirmada | Mockito: `CrearVersionServicioTest` (3) y `EditarVersionServicioTest` (6). El número se pide antes de comprobar el código, porque su candado ordena las creaciones. Un código repetido o una versión en uso no guardan ni registran. Una edición sin cambios no guarda ni registra. | 2026-10-18 |
| API (T010) | confirmada | `NormativaContratoTest` (16) contra el contrato: crear da 201 con la copia y el usuario del token; una versión de origen que no existe, 404; un código repetido, 409 `NORMATIVA_CODIGO_REPETIDO`; un código en blanco, 400 con su campo; editar, 200; una fila fuera de rango, 400 con el campo `tablaCargaHoraria[DOCENTE_TC/TODOS]` y el motivo en español; una fila de menos, 400 «no se quitan filas». Las pruebas de 405 de F04-H01 se reemplazaron por estas. | 2026-10-18 |
| Permisos (T010, FR-008) | confirmada | `PermisosNormativaTest`: `VerificadorDePermisos` sobre las seis operaciones. Se quitó el `@PreAuthorize` de `asociarPeriodo` y falló en esa operación («sin el permiso respondió 400, no 403»); se volvió a poner. | 2026-10-18 |
| De punta a punta (T011, SC-002) | confirmada | `probar_normativa.sh`, paso 5: `vicerrectorado.demo` asocia la 1 a `prueba-1273524475`, crea la versión 2 a partir de ella (201 en 0,082 s), cambia las horas de clase de `DOCENTE_MT` a 11 (200 en 0,176 s), y la 1 es idéntica a la de antes, sin contar sus periodos. Los dos tiempos están por debajo de la meta de 200 ms del plan. | 2026-10-18 |

## Escenario «Versión en uso» (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Asociar a un periodo (T012) | confirmada | `AsociarVersionAPeriodoServicioTest` (5): bloquea el periodo y después la versión, guarda y registra `VERSION_ASOCIADA`; la misma versión otra vez no cambia nada; otra versión da `PeriodoConVersion`; una versión ya en uso puede regir otro periodo. `ConsultarNormativaServicioTest` (5) gana `periodo`. `AsociarVersionAPeriodo`, `PeriodoConVersion` y `PeriodoSinVersion` están en `puertos.entrada`, para Académico (F02-H01); `ModulosTest` en verde. | 2026-10-18 |
| API (T013) | confirmada | `NormativaContratoTest`: editar una versión en uso da 409 `NORMATIVA_VERSION_EN_USO` con «Crea una versión nueva a partir de ella»; asociar da 200 y la versión muestra su periodo; un periodo con otra versión, 409 `NORMATIVA_PERIODO_CON_VERSION`; un periodo sin versión, 404; un código de periodo mal formado (`2025 1`), 400. | 2026-10-18 |
| Hueco de la plataforma | corregido | El código de periodo mal formado respondía 500: las interfaces generadas llevan `@Validated`, Spring valida la ruta antes del controlador y lanza `ConstraintViolationException`, que `ManejadorDeErrores` no traducía. Ahora responde 400 `COMUN_SOLICITUD_INVALIDA` con el campo y «no tiene el formato esperado». `ManejadorDeErroresTest` ganó la prueba; falló con 500 antes de la corrección. Vale para todos los servicios. | 2026-10-18 |
| Concurrencia (T014) | confirmada | `VersionEnUsoIntegrationTest` (2), con los servicios reales: una edición que llega mientras otra transacción asocia la versión espera y la rechaza con `VersionEnUso`, y la versión no cambia; dos ediciones a la vez se guardan en orden, con dos `VERSION_EDITADA`, y queda la última. Sin el `FOR UPDATE` fallan las dos. | 2026-10-18 |
| De punta a punta (T015, SC-003) | confirmada | `probar_normativa.sh`: paso 6, editar la 1, que rige el periodo de prueba, da 409 `NORMATIVA_VERSION_EN_USO` y la 1 sigue idéntica; paso 7, asociar la versión 2 al mismo periodo da 409 `NORMATIVA_PERIODO_CON_VERSION`, el periodo sigue con la 1, y `docente.demo` recibe el 403 común al crear. | 2026-10-18 |

## Escenario «Edición en pantalla» (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Pantalla (T016, FR-007) | confirmada | `Edicion.test.tsx` (8): abre con la versión de número más alto; crear envía `copiarDe` con la elegida y deja elegida la nueva; editar envía la versión completa, sin las horas vacías; un 400 muestra el motivo en la fila, sin códigos ni el nombre del campo; un 409 se muestra con su título; «Cancelar» no pide nada; asociar envía la versión; una versión con periodo muestra «Rige: 2025-1», sin «Editar»; con solo lectura no hay acciones. Las 6 pruebas de F04-H01 siguen en verde. Las 8 nuevas fallaron antes del código. | 2026-10-18 |
| Playwright y accesibilidad (T017) | confirmada | `e2e/normativa.spec.ts` (8): `vicerrectorado.demo` cambia a 5 las horas de clase del docente de medio tiempo y ve el valor nuevo; un valor fuera de rango deja el motivo como descripción accesible del campo; axe-core sin fallas WCAG 2.1 AA en la edición y en los dos diálogos, esperando la opacidad 1 del diálogo. | 2026-10-18 |
| Una sola forma de editar (T018, FR-010) | confirmada | `editarVersion` es la única entrada para cambiar una versión: la pantalla la usa y la carga 7 por archivo de F01 la usará con la tabla completa. Lo dicen el JavaDoc de `EditarVersion` y la descripción de la operación en el contrato. | 2026-10-18 |
| Principio VI (T018) | confirmada | Se buscaron las cifras de la política en el Java de Normativa y en `frontend/src/pantallas/normativa`. Solo aparecen en comentarios (los rangos de RN-06 en `TipoIst`, de F04-H01, y «40.00» en el controlador) y en longitudes de campos del contrato (40, 20). Las cifras siguen solo en `V2__politica_2024.sql` y en las pruebas. | 2026-10-18 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend (T019) | confirmada | `./gradlew check integrationTest`: 524 pruebas, ninguna falla. | 2026-10-18 |
| SPA (T019) | confirmada | `lint`, `formato:verificar`, `tipos` y `verificar:ruptura` en verde; 98 pruebas de Vitest con 95,59 % de líneas; 49 de Playwright. | 2026-10-18 |
| Scripts (T019) | confirmada | Perfil local reconstruido con la rama (`normativa` en V3, `roles` en V5). `probar_normativa.sh` dos veces seguidas, todos los pasos en `OK`. Después de cada corrida la base tiene 1 versión, 0 periodos y 1 registro, así que la limpieza solo borró lo suyo. `probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh` y `probar_spa.sh`: todos los pasos pasan. | 2026-10-18 |
| Gitleaks y shellcheck (T019) | confirmada | Gitleaks v8.30.1 sobre un clon en el scratchpad, montado con `:ro,Z` (nunca el repositorio): 9 commits, sin fugas. `shellcheck` sin observaciones en `probar_normativa.sh`. | 2026-10-18 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Equipo simulado (T019) | confirmada | `deploy/equipo/probar_equipo.sh` con la versión de esta rama: pasos 0 a 11 en `OK`; memoria 1 354,6 MiB; la versión rota vuelve a la anterior. | 2026-10-18 |

## Notas

- **Transacción como puerto.** El plan decía `TransactionOperations` «como en Roles». En Roles la transacción vive en el repositorio. Aquí la aplicación necesita la comprobación, el cambio y el registro juntos, así que recibe el puerto `Transaccion`, implementado en `TransaccionSpring`. Así la aplicación no depende de Spring y se prueba con una transacción que solo ejecuta.
- **Código repetido con concurrencia.** Dos creaciones con el mismo código se ordenan por el candado de la numeración. Si una edición cambia el código al mismo tiempo que otra versión se crea con ese código, la restricción `unique` de la base lo impide y la segunda responde 500, no 409. Es un caso raro; se deja anotado.
- **El evento `VersionNormativaCreada`** queda en `registro_cambio` como `VERSION_CREADA`; su envío a otros procesos llega con su primer consumidor.
- **Asignación normativa** quedó en el glosario §7.3 de `sdd/modelo_dominio.md` (fuera del repositorio), con una copia anterior en `modelo_dominio.md.antes-F04-H02`.
