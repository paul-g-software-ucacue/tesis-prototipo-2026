# Research: Iniciar sesión (F05-H01)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`f0d9020`): el servicio Usuarios de B00-H03 (`IniciarSesionServicio`, `CargaUsuarioDePrueba`, `EmisorJwt`, `VerificadorArgon2`, tablas `usuario` y `usuario_rol`), la SPA de B00-H06 (`sesion/almacen.ts`, `api/cliente.ts`, `pantallas/IniciarSesion.tsx` e `Inicio.tsx`) y el equipo de demostración de B00-H08.

## 1. Roles: identificadores

| Rol (D-16) | Identificador | Origen |
|---|---|---|
| Administrador del sistema | `ADMINISTRADOR` | nuevo |
| Talento Humano / Operador de datos | `OPERADOR_DATOS` | nuevo |
| Coordinador de carrera | `COORDINADOR_CARRERA` | glosario (`sdd/modelo_dominio.md` §7) |
| Gestor de investigación o de vinculación | `GESTOR_UNIDAD` | glosario (incremento 1) |
| Responsable de consolidación | `RESPONSABLE_CONSOLIDACION` | glosario |
| Vicerrector académico | `VICERRECTOR` | nuevo |
| Rector | `RECTOR` | nuevo |
| Miembro del OCS | `MIEMBRO_OCS` | nuevo |
| Secretaría general académica | `SECRETARIA_GENERAL` | glosario (incremento 1) |
| Docente | `DOCENTE` | ya en uso (`autor.prueba`) |

- **Decisión:** estos 10 identificadores, en un enum de dominio `RolMdd` de Usuarios. El token y `SesionActual` siguen llevando texto (ADR-014), así que los procesos no cambian.
- **Por qué:** los del glosario se respetan tal cual; los demás siguen su forma (mayúsculas y guion bajo, en español).
- **Responsable de consolidación** se designa por periodo (D-13, F05-H03). Aquí es un rol fijo del usuario de la demo; F05-H03 decidirá cómo se asigna por periodo.

## 2. Usuarios de la demo (D-59)

- **Archivo:** `backend/servicios/usuarios/src/main/resources/usuarios-demo.yaml`, versionado, con `usuario`, `nombre` (nombre para mostrar) y `roles`. Sin contraseñas.
- **Usuarios:** uno por rol más uno con dos roles, todos ficticios y con el dominio de la demo en el nombre:

  | Usuario | Nombre para mostrar | Roles |
  |---|---|---|
  | `admin.demo` | Administración del sistema (demo) | ADMINISTRADOR |
  | `datos.demo` | Talento Humano (demo) | OPERADOR_DATOS |
  | `coordinacion.demo` | Coordinación de carrera (demo) | COORDINADOR_CARRERA |
  | `gestion.demo` | Gestión de investigación y vinculación (demo) | GESTOR_UNIDAD |
  | `consolidacion.demo` | Responsable de consolidación (demo) | RESPONSABLE_CONSOLIDACION |
  | `vicerrectorado.demo` | Vicerrectorado académico (demo) | VICERRECTOR |
  | `rectorado.demo` | Rectorado (demo) | RECTOR |
  | `ocs.demo` | Miembro del OCS (demo) | MIEMBRO_OCS |
  | `secretaria.demo` | Secretaría general académica (demo) | SECRETARIA_GENERAL |
  | `docente.demo` | Docente (demo) | DOCENTE |
  | `docente.coordinador.demo` | Docente y coordinador (demo) | DOCENTE, COORDINADOR_CARRERA |

- **Contraseñas por entorno:**
  - **Equipo de demostración:** un archivo `secretos/claves-demo` (`usuario=clave`, una por línea, 0400 del uid 10001) que genera `preparar.sh` con `openssl rand` y monta `compose.demo.yml` de solo lectura en `identidad`, como la llave JWT. Variable `MDD_USUARIOS_DEMO_CLAVES=/run/mdd/claves-demo`.
  - **Local y CI:** una contraseña ficticia común para todos, `MDD_USUARIOS_DEMO_CLAVE=cambiar-en-local-demo` en `.env.ejemplo` (Gitleaks la permite por el prefijo, B00-H07).
  - Sin archivo ni contraseña común: los usuarios de la demo no se crean, y se registra un aviso.
- **Carga (`CargaUsuariosDemo`, al arrancar):** por cada usuario del archivo con contraseña: si no existe, lo crea activo con el hash Argon2id; si existe, actualiza su nombre y sus roles y, si la contraseña del entorno ya no coincide con su hash, guarda el hash nuevo. Así el archivo de claves del equipo es la fuente de verdad y rotarlas no exige tocar la base.
- **Alternativas:** una migración de Flyway con los usuarios (dejaría hashes fijos en el repositorio); un comando de administración (manual, sin registro).

## 3. `autor.prueba` en el equipo de demostración (D-57)

- **Decisión:** `compose.demo.yml` pasa a `MDD_USUARIO_PRUEBA_HABILITADO: "false"` y deja de pasar `MDD_USUARIO_PRUEBA_CLAVE`. `CargaUsuarioDePrueba`, con la bandera en `false`, **desactiva** `autor.prueba` si existe (hoy solo deja de crearlo): en el PC del autor ya existe desde B00-H08.
- `probar_humo.sh` y `probar_equipo.sh` pasan a iniciar sesión con `docente.demo`, con su clave de `secretos/claves-demo`. En local y en la CI, `autor.prueba` sigue habilitado (D-37) y los scripts de B00-H03 a B00-H06 no cambian.

## 4. Nombre de usuario sin distinguir mayúsculas

- **Decisión:** el nombre se guarda en minúsculas y la búsqueda normaliza (`trim` y minúsculas, `Locale.ROOT`). Migración `V4__usuarios_demo.sql`: agrega `nombre_para_mostrar varchar(120)` (no nulo, con el nombre de usuario como valor para las filas existentes) y pasa `nombre_usuario` a minúsculas.
- La contraseña sí distingue mayúsculas (Argon2id compara bytes).

## 5. Bloqueo por intentos (D-60)

- **Dónde se cuenta:** tabla `intento_fallido` en el esquema `usuarios` (`nombre` normalizado como clave, `fallidos`, `bloqueado_hasta`, `actualizado_en`). Un solo proceso, `identidad`, la usa; sobrevive a un reinicio.
- **Regla (dominio, `PoliticaDeIntentos`):** con 5 fallidos seguidos, `bloqueado_hasta = ahora + 15 min`; un inicio correcto borra la fila; un intento durante el bloqueo no cuenta y no se verifica la contraseña. Los intentos se cuentan **por nombre intentado, exista o no**, así que el bloqueo no revela qué usuarios existen. Los tres valores son parámetros (`mdd.usuarios.intentos.maximo`, `bloqueo`), con 5 y PT15M por defecto.
- **Respuesta:** `429` con el código nuevo `USUARIOS_INICIO_BLOQUEADO` («Demasiados intentos», «Espera unos minutos y vuelve a intentarlo.»), sin decir cuánto falta ni si el usuario existe; encabezado `Retry-After` en segundos.
- **Concurrencia:** la actualización del contador es un `INSERT … ON CONFLICT … DO UPDATE` atómico.
- **Limpieza:** las filas sin bloqueo y sin actividad en 24 horas se borran al registrar un intento, para que nombres inventados no hagan crecer la tabla.
- **Alternativas:** contar solo usuarios existentes (revela cuáles existen); contar en memoria (se pierde al reiniciar); bloquear por IP (detrás de Caddy y del túnel, todas las peticiones vienen del mismo origen).

## 6. Sesión por inactividad (D-60)

- **Decisión:**
  - el token dura **30 minutos** (`mdd.usuarios.token.vigencia`, ya existe) y lleva el claim `inicio` (instante del inicio de sesión, `auth_time` de OpenID);
  - **renovación:** `POST /api/usuarios/sesion/renovacion`, con el token vigente, devuelve un token nuevo con el mismo `inicio` y otros 30 minutos, salvo que `inicio` + 8 horas ya haya pasado (`mdd.usuarios.token.maximo`, PT8H): entonces responde 401 `SEGURIDAD_NO_AUTENTICADO` y la sesión termina;
  - **la SPA** registra la actividad del usuario (navegación, teclado, ratón y cada petición a la API) y, si hubo actividad desde la última renovación y al token le quedan menos de 20 minutos, lo renueva una vez. Sin actividad, no renueva: el token vence a los 30 minutos, y el temporizador de `almacen.ts` cierra la sesión con «sesión terminada» (ya existe).
- **Por qué:** mantiene el token solo en memoria (D-51) y los procesos siguen validando sin estado con el JWKS (ADR-014). No hace falta una tabla de sesiones.
- **Límite conocido:** un token robado podría renovarse hasta las 8 horas; no hay revocación en el prototipo. Se anota en la evidencia.
- **Alternativas:** *refresh token* en una cookie `HttpOnly` (rompe D-51 y obliga a CSRF); tabla de sesiones con última actividad (estado en el servidor y una consulta por petición).

## 7. Contrato (principio IV, ADR-015)

Cambia `contratos/usuarios/openapi.yaml` antes del código:
- `POST /api/usuarios/sesion`: nueva respuesta `429` (`USUARIOS_INICIO_BLOQUEADO`, con `Retry-After`);
- nueva operación `renovarSesion` (`POST /api/usuarios/sesion/renovacion`, `200` con `TokenSesion`, `401`);
- `SesionActual` (en `contratos/comun/seguridad.yaml`) gana `nombre` **opcional**, el nombre para mostrar, tomado del claim `nombre` del token. Opcional para no romper los «sesión actual» de los demás procesos (D-39): cada servicio tiene su propio `SesionControlador`, y solo el de Usuarios lo rellena; los demás quedan igual.

`contratos/comun/errores.md` y `ErroresUsuarios` suman `USUARIOS_INICIO_BLOQUEADO` (ADR-016).

## 8. SPA

- **Inicio:** «Hola, <nombre>» (con el nombre de usuario si no viene `nombre`), y sus roles con nombres en español (`COORDINADOR_CARRERA` → «Coordinador de carrera») en un solo módulo `sesion/roles.ts`.
- **Inicio de sesión:** el 429 se muestra con el título y la descripción del servidor, como cualquier error (ADR-019); el formulario sigue igual.
- **Renovación:** `sesion/renovacion.ts`, con la regla de §6 y pruebas con temporizadores falsos de Vitest.

## 9. RNF-SEG-02 (D-61)

Sin código nuevo. La evidencia muestra: la columna `clave_hash` con hashes `$argon2id$`; que la SPA no guarda el token (prueba de B00-H06 en Playwright); que no hay tablas de tokens; y los permisos de `/opt/mdd` del equipo (B00-H08).

## 10. Pruebas

- **Dominio:** `PoliticaDeIntentos` y `Usuario` (nombre normalizado), sin dobles.
- **Aplicación:** `IniciarSesionServicio` con bloqueo y `RenovarSesionServicio`, con Mockito en los puertos de salida (principio VIII).
- **Adaptadores:** el repositorio de intentos y la carga de la demo con Testcontainers y PostgreSQL; la renovación y el 429 con las pruebas de contrato (swagger-request-validator).
- **SPA:** Vitest para la renovación, el inicio con nombre y roles y el 429; Playwright para el inicio de sesión de un usuario de la demo y la vuelta a la ruta pedida.
- **Punta a punta:** `deploy/probar_autenticacion.sh` suma: un usuario de la demo entra con sus roles; 5 intentos fallidos y el sexto bloqueado; renovación. `probar_equipo.sh` comprueba `autor.prueba` rechazado y `docente.demo` aceptado en el equipo.
