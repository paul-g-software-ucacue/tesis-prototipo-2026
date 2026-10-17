# Evidencia: F05-H01

Registro de las comprobaciones de los tres escenarios de la historia (issue #43), del bloqueo y la inactividad de RNF-SEG-07 (D-60, ADR-020) y de RNF-SEG-02 (D-61). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. En local y en la CI, las contraseñas son ficticias; las del equipo de demostración nunca salen del equipo.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |
| Backend | `./gradlew check` en verde | 102 pruebas de Usuarios más las de la plataforma; las de integración con `:servicios:usuarios:integrationTest`. |

## Escenario 1: credenciales válidas y usuarios de la demo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Dominio (T006–T008) | confirmada | `RolMddTest` (3): los 10 identificadores de D-16 y el rechazo de uno desconocido. `UsuarioTest` (6): nombre normalizado (` Coordinacion.Demo ` → `coordinacion.demo`), nombre para mostrar obligatorio y `toString` sin el hash. | 2026-10-17 |
| Migración V4 (T004, T005) | confirmada | `MigracionUsuariosDemoIntegrationTest` (Testcontainers): migra hasta V3, inserta `Autor.Prueba` y, tras V4, queda `autor.prueba` con nombre para mostrar `Autor.Prueba`, la columna `not null` y `intento_fallido` con sus 4 columnas. `UsuariosEsquemaIntegrationTest`: todas las tablas en el esquema `usuarios`. | 2026-10-17 |
| Usuarios de la demo (T009–T011) | confirmada | `CargaUsuariosDemoTest` (9): el archivo versionado tiene 11 usuarios `.demo` sin repetir y los 10 roles; con la clave común los crea activos; con el archivo `usuario=clave` cada uno tiene la suya y uno sin línea no se crea; al volver a arrancar actualiza sin duplicar y, si nada cambió, no recalcula hashes; sin contraseñas no crea nada y avisa; un rol desconocido falla y dice cuál. El registro capturado nunca contiene una clave. `CargaUsuariosDemoIntegrationTest` (PostgreSQL y Argon2id real): 11 filas con `$argon2id$`, y al rotar la clave de `rectorado.demo` se actualiza su hash sin duplicar. | 2026-10-17 |
| `autor.prueba` desactivable (T012) | confirmada | `CargaUsuarioDePruebaTest` (5): con `habilitado=false` y el usuario existente, queda inactivo; si ya lo estaba, no se vuelve a guardar; con `habilitado=true`, igual que en B00-H03. | 2026-10-17 |
| Claims `nombre` e `inicio` (T013) | confirmada | `EmisorJwtTest` (3): el token de usuario lleva `nombre` e `inicio` (segundos), además de `sub` y `roles` ordenados; el de proceso no los lleva. | 2026-10-17 |
| Sesión actual con nombre (T014) | confirmada | `SesionContratoTest`: con un token de `docente.coordinador.demo`, `{"usuario":"docente.coordinador.demo","roles":["COORDINADOR_CARRERA","DOCENTE"],"nombre":"Docente y coordinador (demo)"}`, válido contra el contrato; sin claim `nombre`, el campo no aparece. | 2026-10-17 |

## Escenario 2: credenciales inválidas y bloqueo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Regla del bloqueo (T018, T019) | confirmada | `PoliticaDeIntentosTest` (7), sin dobles: de 1 a 4 fallos no bloquea; el quinto bloquea 15 minutos; los segundos restantes se redondean hacia arriba; al vencer, la cuenta vuelve a empezar; los parámetros se respetan y se rechazan los que no tienen sentido. | 2026-10-17 |
| Intentos en PostgreSQL (T020) | confirmada | `RepositorioIntentosJdbcIntegrationTest` (5): la cuenta sube; **10 fallos simultáneos cuentan los 10** (`INSERT … ON CONFLICT … DO UPDATE … RETURNING`); el bloqueo se guarda y se borra; la limpieza borra solo las filas de más de 24 horas sin bloqueo vigente. | 2026-10-17 |
| Inicio de sesión con bloqueo (T021) | confirmada | `IniciarSesionServicioTest` (11, Mockito en los puertos): un nombre bloqueado lanza `InicioBloqueado` sin buscar al usuario, sin verificar la contraseña y sin contar; un fallo cuenta el nombre normalizado, exista o no; el quinto bloquea; un acierto borra los intentos; los casos de B00-H03 siguen igual. | 2026-10-17 |
| 429 y catálogo (T022) | confirmada | `SesionContratoTest`: 429 con `Retry-After: 600` y `{"titulo":"Demasiados intentos","descripcion":"Espera unos minutos y vuelve a intentarlo.","codigo":"USUARIOS_INICIO_BLOQUEADO"}`, válido contra el contrato. `ErroresUsuariosTest`: el catálogo coincide con `contratos/comun/errores.md`. En la plataforma, `RegistroDeErroresTest` y `ManejadorDeErroresTest` cubren los encabezados calculados por la entrada del catálogo. | 2026-10-17 |

## Escenario 3: acceso sin sesión e inactividad

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Renovación (T025, T026) | confirmada | `RenovarSesionServicioTest` (4): con menos de 8 horas emite otro token con el mismo `inicio`; con 8 horas, un token de proceso o un usuario desactivado o borrado, `SesionNoRenovable`. `SesionContratoTest`: 200 `TokenSesion` con un token vigente; 401 `SEGURIDAD_NO_AUTENTICADO` con `WWW-Authenticate: Bearer` si no es renovable o no hay token, todo válido contra el contrato. | 2026-10-17 |

## RNF-SEG-02 (D-61)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- La respuesta 429 lleva `Retry-After`. Para eso, una entrada del catálogo de errores puede calcular encabezados con los datos de su excepción (`EntradaCatalogo.conEncabezados`, en `plataforma:errores-nucleo`), y el manejador común los agrega. La renovación rechazada usa lo mismo para responder como un token inválido: 401 `SEGURIDAD_NO_AUTENTICADO` con `WWW-Authenticate: Bearer`.
- La renovación vuelve a leer al usuario: uno desactivado no renueva, y los roles del token nuevo son los vigentes.
- Límite conocido (ADR-020): sin revocación, un token robado podría renovarse hasta las 8 horas desde el inicio de sesión.
