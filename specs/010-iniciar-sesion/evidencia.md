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
| Inicio con nombre y roles en español (T016) | confirmada | Vitest: `Inicio.test.tsx` («Hola, Vicerrectorado académico (demo)» y «Vicerrector académico», sin `VICERRECTOR` en pantalla; sin `nombre`, «Hola, autor.prueba») y `roles.test.ts` (los 10 roles; uno desconocido, tal cual). Playwright en Chromium y Firefox: `docente.coordinador.demo` entra y ve «Hola, Docente y coordinador (demo)» y «Coordinador de carrera». | 2026-10-17 |
| Perfil local, los 11 usuarios (T015, T017, SC-001) | confirmada | `docker compose up -d --build --wait` y `deploy/probar_autenticacion.sh`: identidad registra «Usuarios de la demo: 11 creados y 0 actualizados»; el paso 7 pasa para los 11, cada uno con su nombre y **exactamente** los roles de `usuarios-demo.yaml` (`docente.coordinador.demo`: `["COORDINADOR_CARRERA","DOCENTE"]`). Los pasos 1 a 6 de B00-H03 siguen en `OK`, y también `probar_errores.sh`, `probar_eventos.sh` y `probar_spa.sh`. | 2026-10-17 |

## Escenario 2: credenciales inválidas y bloqueo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Regla del bloqueo (T018, T019) | confirmada | `PoliticaDeIntentosTest` (7), sin dobles: de 1 a 4 fallos no bloquea; el quinto bloquea 15 minutos; los segundos restantes se redondean hacia arriba; al vencer, la cuenta vuelve a empezar; los parámetros se respetan y se rechazan los que no tienen sentido. | 2026-10-17 |
| Intentos en PostgreSQL (T020) | confirmada | `RepositorioIntentosJdbcIntegrationTest` (5): la cuenta sube; **10 fallos simultáneos cuentan los 10** (`INSERT … ON CONFLICT … DO UPDATE … RETURNING`); el bloqueo se guarda y se borra; la limpieza borra solo las filas de más de 24 horas sin bloqueo vigente. | 2026-10-17 |
| Inicio de sesión con bloqueo (T021) | confirmada | `IniciarSesionServicioTest` (11, Mockito en los puertos): un nombre bloqueado lanza `InicioBloqueado` sin buscar al usuario, sin verificar la contraseña y sin contar; un fallo cuenta el nombre normalizado, exista o no; el quinto bloquea; un acierto borra los intentos; los casos de B00-H03 siguen igual. | 2026-10-17 |
| 429 y catálogo (T022) | confirmada | `SesionContratoTest`: 429 con `Retry-After: 600` y `{"titulo":"Demasiados intentos","descripcion":"Espera unos minutos y vuelve a intentarlo.","codigo":"USUARIOS_INICIO_BLOQUEADO"}`, válido contra el contrato. `ErroresUsuariosTest`: el catálogo coincide con `contratos/comun/errores.md`. En la plataforma, `RegistroDeErroresTest` y `ManejadorDeErroresTest` cubren los encabezados calculados por la entrada del catálogo. | 2026-10-17 |
| 429 en la SPA (T023) | confirmada | Vitest (`IniciarSesion.test.tsx`) y Playwright en los dos navegadores: «Demasiados intentos» y «Espera unos minutos y vuelve a intentarlo.», sin `USUARIOS_` ni `429` en pantalla y sin token. | 2026-10-17 |
| Bloqueo en el perfil (T024, SC-003, SC-007) | confirmada | `probar_autenticacion.sh`, paso 8: con `bloqueo-<aleatorio>` y con `ocs.demo`, 5 veces 401 y el sexto 429 `USUARIOS_INICIO_BLOQUEADO` con `Retry-After`; los cuerpos del 401 y del 429 son **idénticos** para el nombre inventado y para el que existe. El script borra después los intentos de `ocs.demo` y se pudo correr dos veces seguidas con todo en `OK`. | 2026-10-17 |

## Escenario 3: acceso sin sesión e inactividad

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Renovación (T025, T026) | confirmada | `RenovarSesionServicioTest` (4): con menos de 8 horas emite otro token con el mismo `inicio`; con 8 horas, un token de proceso o un usuario desactivado o borrado, `SesionNoRenovable`. `SesionContratoTest`: 200 `TokenSesion` con un token vigente; 401 `SEGURIDAD_NO_AUTENTICADO` con `WWW-Authenticate: Bearer` si no es renovable o no hay token, todo válido contra el contrato. | 2026-10-17 |
| Renovación en la SPA (T027, T028) | confirmada | `renovacion.test.ts` (9, temporizadores falsos): sin actividad no renueva y cierra a los 30 minutos como «sesión terminada»; con actividad renueva una vez al entrar en los últimos 20 minutos; dos actividades seguidas, una sola renovación; **después de una renovación, sin actividad, no hay otra y la sesión vence**; la petición de renovación sale sin el encabezado interno y no cuenta como actividad; una petición del usuario sí cuenta; un 401 cierra la sesión; un fallo de red la deja hasta su vencimiento; el token renovado tampoco llega al almacenamiento. `sesion.test.tsx`: una tecla, un clic o volver atrás en el navegador renuevan; sin sesión, la actividad no pide nada. Cobertura de la SPA: 99,5 % de líneas. | 2026-10-17 |
| Acceso sin sesión y renovación de punta a punta (T029, SC-004) | confirmada | Playwright: sin sesión, `/` lleva a `/iniciar-sesion?destino=%2F` y, al entrar, vuelve a `/` (37 pruebas en Chromium y Firefox, todas en verde). `probar_autenticacion.sh`, paso 9: la renovación con el token de `docente.demo` devuelve 200 y el token nuevo sirve en la sesión actual; con un token alterado, el 401 uniforme. | 2026-10-17 |

## RNF-SEG-02 (D-61)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Contraseñas solo como hash (T031, FR-008) | confirmada | En el perfil local, `select nombre_usuario, left(clave_hash, 10) from usuarios.usuario`: las 12 filas (los 11 de la demo y `autor.prueba`) empiezan con `$argon2id$`. | 2026-10-17 |
| Sin tokens guardados (T031) | confirmada | Las tablas del esquema `usuarios` son `evento_recibido`, `flyway_schema_history`, `intento_fallido`, `usuario` y `usuario_rol`: ninguna guarda tokens. En la SPA, el token solo vive en memoria: las pruebas de B00-H06 siguen en verde (Vitest y Playwright) y hay una nueva para el token renovado. | 2026-10-17 |
| Secretos del equipo (T031) | confirmada | Los de B00-H08 (`/opt/mdd` 0750, `secretos` 0700, `jwt.pem` 0400 del uid 10001), más `claves-demo` con dueño 10001, grupo `mdd` y modo 0440 (T030). Ninguna contraseña de la demo está en el repositorio ni en una imagen: `usuarios-demo.yaml` no tiene contraseñas y en local se usa la clave ficticia `cambiar-en-local-demo`. | 2026-10-17 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Equipo simulado (T030) | confirmada | `deploy/equipo/probar_equipo.sh`: pasos 0 a 11 en `OK`. La prueba de humo inicia sesión con `docente.demo` y comprueba que `autor.prueba` no entra; la llave persistente y el sitio del túnel se prueban con `docente.demo`; memoria 1 374 MiB; vuelta atrás en 149 s. Paso 11: `claves-demo` tiene una contraseña por usuario de `usuarios-demo.yaml` y, si falta una, `preparar.sh` la agrega sin cambiar las demás. | 2026-10-17 |
| SELinux y el montaje de `claves-demo` (T030) | corregido | La primera corrida falló en los pasos 1, 4, 6 y 7 porque `docente.demo` no entraba. Causa, comprobada con un contenedor de prueba: Compose ignora `selinux: Z` en la sintaxis larga del montaje (la etiqueta quedó `user_home_t` y el uid 10001 recibió «Permission denied»). Se volvió a la sintaxis corta `:ro,Z`, como `jwt.pem`. Para que un `claves-demo` que falta no se convierta en una carpeta, `desplegar.sh` revisa con `docker compose config --format json` que existan los archivos montados y, si no, no levanta la versión y vuelve atrás. | 2026-10-17 |
| Herramientas (T032) | confirmada | ShellCheck sin hallazgos en `probar_autenticacion.sh`, `desplegar.sh`, `preparar.sh`, `probar_humo.sh` y `probar_equipo.sh`. Gitleaks 8.30.1 (`--text HEAD`, todo el historial) sin hallazgos, después de permitir las rutas `/run/mdd/`: la regla de claves tomaba `MDD_USUARIOS_DEMO_CLAVES: /run/mdd/claves-demo` por una contraseña. `npm run verificar:ruptura`: los 4 pasos en `OK`. | 2026-10-17 |

## Notas

- La respuesta 429 lleva `Retry-After`. Para eso, una entrada del catálogo de errores puede calcular encabezados con los datos de su excepción (`EntradaCatalogo.conEncabezados`, en `plataforma:errores-nucleo`), y el manejador común los agrega. La renovación rechazada usa lo mismo para responder como un token inválido: 401 `SEGURIDAD_NO_AUTENTICADO` con `WWW-Authenticate: Bearer`.
- La renovación vuelve a leer al usuario: uno desactivado no renueva, y los roles del token nuevo son los vigentes.
- Límite conocido (ADR-020): sin revocación, un token robado podría renovarse hasta las 8 horas desde el inicio de sesión.
