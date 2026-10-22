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
| Columna y diálogo (T018, FR-010) | confirmada | `Periodos.test.tsx` (+3, Vitest con la API simulada): la columna «Responsable de consolidación» con el usuario o «Sin designar»; sin escritura, sin la acción; «Designar responsable 2026-2» con la lista de docentes, que no aparece en un periodo cerrado; el 400 muestra su motivo, el 409 su título y el 404 «El periodo todavía no está disponible para designar. Inténtalo en unos segundos.». Fallaron las 3 antes del código. `tipos` y `lint` en verde. | 2026-10-21 |
| Playwright y axe (T019, SC-001) | confirmada | `e2e/periodos.spec.ts` (+2, Chromium): el Vicerrector designa a `docente.demo` y la fila lo muestra; axe-core sin fallas WCAG 2.1 AA en «Designar al Responsable de consolidación de 2026-2». | 2026-10-21 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| De punta a punta (T020, SC-002 y SC-003) | confirmada | Perfil local reconstruido con la rama (`datos`, `identidad` y `frontend`). `deploy/probar_periodos.sh` dos veces seguidas: los 10 pasos en `OK`. En el paso 10, el aviso del periodo llegó a Roles en menos de 1 s; `rectorado.demo` y el propio Vicerrector dan 400; `docente.demo` queda designado (designar tardó 0,088 y 0,025 s) y su sesión trae `CONSOLIDACION:ESCRITURA`; cerrado el periodo, su sesión nueva ya no lo trae; designar en el periodo cerrado da 409 `ROLES_PERIODO_CERRADO`; un `DESIGNAR_RESPONSABLE` en el registro de Roles. Después de las dos corridas, la base quedó como estaba: 0 periodos, 0 copias, 0 designaciones, 0 eventos recibidos y 0 publicaciones en `eventos_datos`. `shellcheck` sin observaciones. | 2026-10-21 |
| Backend (T021) | confirmada | `./gradlew check integrationTest jacocoTestReport`: 590 pruebas unitarias y 90 de integración en todo el backend, ninguna falla. Roles con 72,5 % de líneas y Académico con 77,9 %. | 2026-10-21 |
| SPA (T021) | confirmada | `lint`, `formato:verificar`, `tipos` y `verificar:ruptura` en verde; 115 pruebas de Vitest con 95,4 % de líneas; 57 de Playwright. | 2026-10-21 |
| Scripts (T021) | confirmada | Con el perfil reconstruido: `probar_autenticacion.sh` (38 `OK`), `probar_errores.sh` (4), `probar_eventos.sh` (6), `probar_spa.sh` (5), `probar_normativa.sh` (24) y `probar_periodos.sh` (28). | 2026-10-21 |
| Gitleaks y shellcheck (T021) | confirmada | Gitleaks v8.30.1 sobre un clon en el scratchpad, montado con `:ro,Z`: 12 commits, sin fugas. `shellcheck` sin observaciones en `probar_periodos.sh`, `preparar.sh` y `probar_equipo.sh`. | 2026-10-21 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Clave del proceso `datos` (T022) | confirmada | Antes de fusionar, el autor corrió `sudo deploy/equipo/preparar.sh` desde la rama: «Ya existe /opt/mdd/.env: no se toca, salvo las claves que falten» y «Agregada MDD_CLAVE_PROCESO_DATOS a /opt/mdd/.env»; 0 contraseñas nuevas. | 2026-10-21 |
| Despliegue automático (T022) | confirmada | Al fusionar #127 (`2c80942`), el CD publicó las 9 imágenes y `mdd-actualizar.timer` desplegó `2c80942c3a4d` (anterior: `861e9ae0a0d1`): «Sano en 68 s» y la prueba de humo en `OK`. `roles` en V6 y `eventos_datos` con su registro de publicaciones; la copia de periodos de Roles empezó vacía. | 2026-10-21 |
| Designación en el equipo (T022, SC-001) | confirmada | Con `vicerrectorado.demo` en `https://mdd.localhost`, el autor registró el periodo `002` («2026-3») a las 21:56:49 y designó Responsable a `docente.coordinador.demo` a las 21:57:40 (51 s después de registrarlo), lo abrió con la versión 1 y lo reemplazó por `docente.demo` a las 21:57:58; después lo pasó a En aprobación y a Vigente. En la base del equipo: dos `DESIGNAR_RESPONSABLE` (el segundo con el anterior y el nuevo); 3 publicaciones de `datos` en `COMPLETED` y 3 eventos recibidos en Roles; la copia de `002` en Vigente a la misma hora que el cambio en Académico. La pantalla muestra a `docente.demo` como Responsable de `002` y «Sin designar» en `001`, anterior al despliegue. El autor no informó si entró con `docente.demo`; que su sesión gana los permisos se comprobó en el perfil (paso 10 de `probar_periodos.sh`) y en `ResponsableDeConsolidacionIntegrationTest`. | 2026-10-21 |
| Equipo simulado (T021) | confirmada | `deploy/equipo/probar_equipo.sh` con la versión de esta rama: pasos 0 a 11 en `OK`, a la primera; memoria 1 380,0 MiB; la versión rota vuelve a la anterior en 126 s; en el paso 11, `preparar.sh` agrega `MDD_CLAVE_PROCESO_DATOS` a un `.env` que no la tenía y deja igual el resto. | 2026-10-21 |

## Notas

- **Es un rol, no un cargo (FR-007).** Cuando F07 calcule las horas, su prueba debe comprobar que un docente designado Responsable de consolidación no suma horas de gestión por la designación (ADR-003).
- **El periodo `001` del equipo.** Es anterior al despliegue y Roles no lo conoce: designar en él responde «no encontrado» (research §1).
- **Códigos comunes compartidos.** `RegistroDeErrores` admite ahora que dos servicios de un mismo proceso traduzcan su excepción al mismo código `COMUN_` con la misma definición; los códigos propios de cada servicio siguen sin poder repetirse.
