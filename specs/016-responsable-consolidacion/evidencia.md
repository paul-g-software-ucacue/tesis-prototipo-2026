# Evidencia: F05-H03

Registro de las comprobaciones de la historia (issue #45): designar por periodo al Responsable de consolidación, con sus permisos mientras el periodo no esté cerrado, y los periodos que Académico avisa a Roles por eventos (D-13, ADR-003, ADR-018). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, dominio, migración y eventos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Contrato primero (T002) | confirmada | `contratos/roles/openapi.yaml`: `listarResponsables` (`DISTRIBUTIVO:LECTURA`), `designarResponsable` y `listarDocentes` (`DISTRIBUTIVO:ESCRITURA`), y `recibirEvento` en `/interno/roles/eventos`; `ROLES_PERIODO_CERRADO` en `errores.md`. `openApiGenerate`, `tipos` y `verificar:ruptura` en verde. | 2026-10-21 |
| Dominio (T003, T004) | confirmada | `EstadoConocidoTest` (2, los 16 pares), `PeriodoConocidoTest` (2) y `DesignacionTest` (7), sin dobles: la copia nunca retrocede; periodo cerrado, no docente y quien designa se rechazan con su motivo; el mismo usuario no cambia nada. Fallaron antes del código (no compilaban). | 2026-10-21 |
| Esquema V6 (T005, T006) | confirmada | `RolesEsquemaIntegrationTest` (+1): las tres tablas, sus `check`, la clave foránea de la designación y la acción `DESIGNAR_RESPONSABLE`. Falló sin V6 («bad SQL grammar») y pasó con `V6__responsable_de_consolidacion.sql`. | 2026-10-21 |
| Repositorios (T007) | confirmada | `DesignacionesJdbcIntegrationTest` (5): la copia se guarda y se actualiza; una designación por periodo, que se reemplaza; `periodosAbiertosDe` solo trae los no cerrados; el candado del periodo; el evento repetido no es nuevo. Fallaron antes de los adaptadores (no compilaban). | 2026-10-21 |
| Académico avisa (T008) | confirmada | `RegistrarPeriodoServicioTest` (+1) y `CambiarEstadoPeriodoServicioTest` (+1): `AvisosDelPeriodo.actualizado` se llama **dentro** de la transacción; un código repetido y el mismo estado no avisan. Fallaron antes (no compilaban). El adaptador publica `academico.PeriodoActualizado` con `{codigo, estado}` hacia `roles`. | 2026-10-21 |
| Eventos en `datos` (T009) | confirmada | `EsquemasIntegrationTest` del proceso `datos` con `eventos_datos.event_publication`: falló sin `plataforma:eventos` (el contexto no arrancaba) y pasó con su configuración, como `distributivo`. `MDD_CLAVE_PROCESO_DATOS` en `.env.ejemplo` y en la configuración de `identidad`. `AbrirPeriodoIntegrationTest` sigue en verde. | 2026-10-21 |
| Receptor de Roles (T010) | confirmada | `RecibirEventosServicioTest` (5, Mockito) y `EventosRolesContratoTest` (3): 204 con token de proceso; un repetido no cambia nada; uno viejo no hace retroceder la copia; otro tipo o un estado desconocido dan 400 `COMUN_EVENTO_NO_RECONOCIDO`; 403 con un token de usuario. Fallaron antes (no compilaban). | 2026-10-21 |
| Catálogo con códigos comunes compartidos | agregada | Con el receptor, `identidad` dejó de arrancar: Usuarios y Roles registraban los dos `COMUN_EVENTO_NO_RECONOCIDO` y `RegistroDeErrores` no admitía un código repetido. `RegistroDeErroresTest` (+2): un código `COMUN_` con la misma definición se puede compartir; con otra definición, sigue fallando. Falló antes del cambio. | 2026-10-21 |
| Clave en el equipo (T011) | confirmada | `probar_equipo.sh`, paso 11 (+1 comprobación): con un `.env` sin la clave, `preparar.sh` la agrega y las demás líneas quedan iguales (comprobado también a mano en el scratchpad). `compose.demo.yml` la pasa a `datos` (obligatoria) y a `identidad`. `shellcheck` sin observaciones en los dos scripts. | 2026-10-21 |

## Escenario «Designación» (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Designar (T012) | confirmada | `DesignarResponsableServicioTest` (7, Mockito): bloquea el periodo, guarda y registra `DESIGNAR_RESPONSABLE` con el anterior y el nuevo; el mismo usuario no registra; periodo desconocido, cerrado y no docente se rechazan sin registrar; `docentes()` solo trae a los usuarios con el rol Docente. Fallaron antes (no compilaban). | 2026-10-21 |
| El rol en el token (T013, SC-002) | confirmada | `AutorizacionServiciosTest` (+4): con una designación en un periodo abierto, el docente recibe `RESPONSABLE_CONSOLIDACION` y sus permisos; con su único periodo cerrado, no; el rol asignado a mano se conserva; un usuario sin roles pero designado también lo recibe. Fallaron antes (no compilaban). `MatrizD16IntegrationTest` en verde. | 2026-10-21 |
| Contrato (T014) | confirmada | `ResponsablesContratoTest` (6): 200 con el `Responsable`; 400 con el detalle en `usuario`; 404 con un periodo que Roles no conoce; 409 `ROLES_PERIODO_CERRADO`; `listarResponsables` y `listarDocentes`. `ErroresRolesTest` con el código nuevo. Fallaron antes (no compilaban). | 2026-10-21 |
| Proceso `identidad` completo (T015) | confirmada | `ResponsableDeConsolidacionIntegrationTest`: pide un token de proceso de `datos`, avisa por HTTP que un periodo se registró (204), designa a un docente y la autorización interna trae `RESPONSABLE_CONSOLIDACION` y `CONSOLIDACION:ESCRITURA`; avisa el cierre (204) y el rol sale. El puerto se elige antes de arrancar, para que el JWKS apunte al mismo proceso y no al perfil local. | 2026-10-21 |

## Quién designa (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Permisos (T016) | confirmada | `PermisosRolesTest` con las 11 operaciones de `/api/roles` (las 3 nuevas con `DISTRIBUTIVO`), `VerificadorDePermisos` en verde; `ResponsablesContratoTest` (+1): el Administrador, con solo `USUARIOS_ROLES:ESCRITURA`, recibe 403 y no designa. Antes de las operaciones, la prueba fallaba (8 esperadas, 11 en el contrato). | 2026-10-21 |

## Es un rol, no un cargo (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| No es un cargo (T017, FR-007) | confirmada | La designación solo vive en `roles.designacion_responsable`: no hay cargos ni actividades en el sistema todavía (F03-H05, F07), y ninguna tabla de horas la referencia. F07 lo comprueba con su cálculo (ver «Notas»). | 2026-10-21 |

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

- **Es un rol, no un cargo (FR-007).** Cuando F07 calcule las horas, su prueba debe comprobar que un docente designado Responsable de consolidación no suma horas de gestión por la designación (ADR-003).
- **El periodo `001` del equipo.** Es anterior al despliegue y Roles no lo conoce: designar en él responde «no encontrado» (research §1).
- **Códigos comunes compartidos.** `RegistroDeErrores` admite ahora que dos servicios de un mismo proceso traduzcan su excepción al mismo código `COMUN_` con la misma definición; los códigos propios de cada servicio siguen sin poder repetirse.
