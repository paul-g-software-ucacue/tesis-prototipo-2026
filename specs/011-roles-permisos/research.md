# Research: Roles con permisos y varios roles por usuario (F05-H02)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`de92c3d`):
- el servicio Usuarios de F05-H01: `usuario` y `usuario_rol`, `CargaUsuariosDemo`, `EmisorJwt` con `roles`, `nombre` e `inicio`, y `RenovarSesionServicio`;
- el esqueleto vacío del servicio Roles (`backend/servicios/roles`, esquema `roles` con solo V1), ensamblado en el proceso `identidad`;
- `plataforma:seguridad`, que convierte el claim `roles` en autoridades `ROLE_<rol>`, separa `/interno/**` (token de proceso) del resto y responde los 403 sin cuerpo;
- la SPA con `SesionActual` (`usuario`, `nombre`, `roles`) y los roles en español (`sesion/roles.ts`).

## 1. Quién es dueño de qué

| Dato | Servicio | Esquema |
|---|---|---|
| Credenciales, nombre para mostrar, activo e intentos | Usuarios | `usuarios` |
| Catálogo de funcionalidades, roles, permisos y asignaciones de roles a usuarios | **Roles** | `roles` |

- **Decisión:** las asignaciones pasan de `usuarios.usuario_rol` a Roles (`roles.asignacion`). Usuarios deja de leer y escribir `usuario_rol`, y el dominio `Usuario` pierde `roles`.
- **`usuario_rol` no se borra en esta historia:** si la versión nueva falla y el equipo vuelve a la anterior (D-55), la anterior todavía lee esa tabla. Se borra en una historia posterior, cuando ya no haga falta volver a una versión de antes de F05-H02.
- **Por qué:** la ERS 8.5 da a Roles «roles múltiples por usuario y permisos por funcionalidad» (servicio 2), y el principio V prohíbe que un servicio lea las tablas de otro.
- **Alternativa:** dejar las asignaciones en Usuarios y solo los roles en Roles (dos servicios para una sola regla, y Usuarios tendría que validar roles que no conoce).

## 2. Cómo se hablan Usuarios y Roles

- **Decisión:** dentro del proceso `identidad`, Usuarios llama a Roles por su interfaz de aplicación publicada (`ec.mdd.roles.puertos.entrada`), como dice el principio V. `ModulosTest` (Spring Modulith) ya permite usar solo `puertos.entrada` de otro servicio.
  - **`ConsultarAutorizacion`:** roles y permisos efectivos de un usuario. Lo usan el inicio de sesión y la renovación.
  - **`AsignarRolesIniciales`:** da los roles iniciales a un usuario una sola vez. Lo usan `CargaUsuariosDemo` y `CargaUsuarioDePrueba`.
- Del lado de Usuarios, un puerto de salida propio (`ProveedorDeAutorizacion`) con un adaptador que delega en esos dos puertos de entrada. Usuarios no ve las clases internas de Roles.
- **Una sola dirección:** Usuarios → Roles. Roles no llama a Usuarios: al asignar, no comprueba que el usuario exista (la pantalla solo ofrece usuarios existentes; una asignación a un nombre inexistente no da acceso a nadie). Así no hay dependencia circular.
- `Consultar autorización` también queda en el contrato de Roles como operación interna (`GET /interno/roles/autorizaciones/{usuario}`, solo con el token de proceso, ADR-018), para el día en que Roles corra en otro proceso.
- **Alternativa:** que Usuarios lea las tablas de Roles (prohibido por el principio V); un evento por cada cambio (más piezas, y la autorización tiene que ser una consulta síncrona al iniciar sesión).

## 3. Cómo llegan los permisos a cada proceso (D-62)

- **Decisión:** el token lleva un claim nuevo, `permisos`: la lista de los permisos efectivos del usuario, en el formato `FUNCIONALIDAD:TIPO` o `FUNCIONALIDAD:TIPO:PROPIO` (por ejemplo, `USUARIOS_ROLES:ESCRITURA` y `OFERTA:LECTURA:PROPIO`). El claim `roles` sigue, con los identificadores de sus roles.
  - Usuarios lo arma al iniciar sesión y al renovar, con `ConsultarAutorizacion`. Así, un cambio vale desde la próxima renovación (D-62).
  - `plataforma:seguridad` convierte cada permiso en la autoridad `PERMISO_FUNCIONALIDAD:TIPO`, con o sin `PROPIO`: el alcance no cambia **si** puede, solo **qué** puede ver (D-64).
  - El alcance queda disponible en el token para las historias que filtren «lo propio».
- **Tamaño:** el rol con más permisos (Administrador: lectura de todo y escritura de 4) da unos 20 permisos, alrededor de 500 bytes más en el token. Aceptable para un encabezado HTTP.
- **Por qué:** mantiene la validación sin estado de ADR-014 y ADR-020; ningún proceso consulta a Roles en cada petición.
- **Alternativas:** consultar a Roles en cada petición (acopla los 4 procesos a `identidad`); un catálogo rol → permisos en la plataforma (no admite roles creados por el administrador).

## 4. Cómo se protege cada operación (RNF-SEG-03)

- **Decisión, en dos partes:**
  1. **En el contrato:** cada operación protegida declara su permiso con la extensión `x-mdd-permiso: USUARIOS_ROLES:ESCRITURA` (contrato primero, principio IV). Una operación sin la extensión solo exige sesión.
  2. **En el código:** el controlador anota el método con `@PreAuthorize("hasAuthority('PERMISO_USUARIOS_ROLES:ESCRITURA')")` (Spring Security, *method security*, activada en `plataforma:seguridad`).
- **La prueba que los une:** `plataforma:pruebas` gana `VerificadorDePermisos`. Lee el contrato del servicio y, por cada operación con `x-mdd-permiso`, llama con un token que tiene todos los permisos **menos** ese y espera 403; con ese permiso, espera algo distinto de 403. Si un controlador olvida la anotación o pone otra, la prueba falla. Es la «prueba de autorización por endpoint» de RNF-SEG-03.
- **Por qué:** el permiso queda escrito en el contrato, que es la fuente de verdad (ADR-015), y la prueba impide que contrato y código se separen.
- **Alternativas:** reglas por ruta en la cadena de filtros (se separan del contrato); un filtro que lea el contrato en ejecución (más magia, y el generador no lleva la extensión al código).

## 5. El 403 con el modelo de errores común (ADR-016)

- **Decisión:** código nuevo de la plataforma, `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403, «Acceso no autorizado», «No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.»). En `CodigosComunes`, en `contratos/comun/errores.md` y en una respuesta común `AccesoNoAutorizado` de `contratos/comun/seguridad.yaml`.
  - **Procesos:** `plataforma:seguridad` gana `RespuestaAccesoNoAutorizado` (un `AccessDeniedHandler`), con el mismo cuerpo fijo para todas las causas: falta un permiso, un token de usuario en `/interno/**` o un token de proceso fuera de él. El `@PreAuthorize` lanza `AccessDeniedException`, que `ManejadorDeErrores` ya deja pasar hacia la seguridad.
  - **API Gateway:** su `denyAll()` para rutas fuera de `/api/**` responde el mismo cuerpo.
- Los contratos que hoy dicen «403 sin cuerpo hasta F05-H02» pasan a la respuesta común.

## 6. Catálogo de funcionalidades y la matriz D-16 (D-65)

- **Decisión:** 16 funcionalidades fijas, cada una con los tipos de permiso que admite, y los permisos de los 10 roles tomados de D-16. Están en [data-model.md](data-model.md#matriz-d-16), con la interpretación de cada celda de la matriz.
- **Cómo se guardan:** una migración de Roles (`V2__matriz_d16.sql`) inserta las funcionalidades, los 10 roles predefinidos y sus permisos. Cambiar la matriz exige una migración nueva y una decisión (D-63). Las historias siguientes agregan sus funcionalidades del mismo modo.
- **Interpretación de D-16:** la tabla del ERS está en prosa («Todo, incluida la bitácora», «Oferta propia y propuesta de docentes»). Pasarla a permisos concretos exige elegir. Por eso la interpretación queda registrada como D-65, aprobada por el autor.

## 7. Roles predefinidos y reglas de la administración

- **D-63:** los 10 roles de D-16 llevan `predefinido = true`; editarlos o borrarlos responde 409 `ROLES_ROL_PREDEFINIDO`.
- **Nombre repetido:** 409 `ROLES_NOMBRE_REPETIDO`.
- **Rol sin permisos:** 400 por la validación del contrato (`permisos` con `minItems: 1`).
- **Rol asignado:** borrarlo responde 409 `ROLES_ROL_ASIGNADO`, con la cantidad de usuarios en la descripción.
- **Sin administrador:** si un cambio de asignaciones dejaría sin ningún usuario con `USUARIOS_ROLES:ESCRITURA`, o le quitaría ese permiso a quien hace el cambio, responde 409 `ROLES_SIN_ADMINISTRADOR`. Esto no mira si el usuario está activo, que es dato de Usuarios: basta con que quede una asignación con ese permiso.
- **Identificador de un rol nuevo:** se deriva del nombre (`Auditor de cargas` → `AUDITOR_DE_CARGAS`), es único y no cambia aunque cambie el nombre, porque viaja en el token.
- **Registro (FR-011):** la tabla `registro_cambio` (momento, usuario, acción y detalle en JSON) guarda cada creación, edición, borrado, asignación y retiro. La bitácora de historias posteriores lo consumirá.

## 8. Roles iniciales de los usuarios de la demo (FR-009)

- **Decisión:** `AsignarRolesIniciales(usuario, roles)` asigna los roles **solo la primera vez** que se le pide para ese usuario, y lo deja anotado en `roles.inicializacion`. Si después el administrador cambia sus roles, incluso si se los quita todos, un nuevo arranque no los toca.
  - `CargaUsuariosDemo` deja de actualizar roles. Al crear o actualizar un usuario de la demo, pide sus roles iniciales del archivo, y Roles los ignora si ya se inicializó.
  - Lo mismo `CargaUsuarioDePrueba` para `autor.prueba`.
- **Equipo del autor:** al desplegar esta versión, Roles está vacío. Los 11 usuarios de la demo y `autor.prueba` reciben sus roles iniciales del archivo, que son los mismos que tenían en `usuario_rol`. No hace falta migrar datos entre esquemas.
- **El enum `RolMdd` de Usuarios** se retira: los roles válidos ya no son fijos. `CargaUsuariosDemo` valida los roles del archivo contra el catálogo de Roles por medio de `AsignarRolesIniciales`, que rechaza un rol desconocido.

## 9. Administración en la SPA

- **Pantalla «Roles»** (`/roles`), en el menú solo con `USUARIOS_ROLES:LECTURA`:
  - pestaña **Roles**: la lista con nombre, descripción y si es de D-16; el detalle muestra la matriz funcionalidad × tipo × alcance, en solo lectura para los de D-16;
  - **crear y editar** un rol: nombre, descripción y una tabla con una casilla por cada tipo que admite cada funcionalidad, más un selector de alcance (todo o solo lo propio);
  - pestaña **Usuarios**: la lista de usuarios (`GET /api/usuarios/usuarios`, nombre de usuario y nombre para mostrar) con sus roles, y un diálogo para asignar y quitar.
- **Sin `USUARIOS_ROLES:ESCRITURA`**, las acciones de crear, editar, borrar y asignar no aparecen.
- **Sin `USUARIOS_ROLES:LECTURA`**, la ruta muestra «Acceso no autorizado» (componente `RutaConPermiso`).
- **Inicio:** una sección «Tus funciones» lista las funcionalidades que el usuario puede usar, con sus tipos de permiso en español y sin repetir, a partir de `SesionActual.permisos`.
- **Permisos en la SPA:** `sesion/permisos.ts` (`tienePermiso`) y `sesion/funcionalidades.ts`, con los nombres en español. Solo deciden qué se muestra; el servidor decide qué se permite.
- `SesionActual` gana `permisos` (opcional), que Usuarios toma del token. Los demás procesos no cambian.

## 10. Lista de usuarios en Usuarios

- `GET /api/usuarios/usuarios` (`x-mdd-permiso: USUARIOS_ROLES:LECTURA`) devuelve `usuario`, `nombre` y `activo` de cada usuario, ordenados por nombre de usuario. Sin contraseñas ni hashes.
- Es la primera operación de Usuarios protegida por permiso. Su prueba usa el mismo `VerificadorDePermisos`.

## 11. Pruebas

- **Dominio de Roles,** sin dobles: permisos válidos por funcionalidad, unión de permisos con alcance (el más amplio gana), identificador derivado del nombre y las reglas de predefinido, asignado y sin administrador.
- **Aplicación,** con Mockito en los puertos: crear, editar, borrar, asignar, consultar autorización y roles iniciales una sola vez; registro de cambios.
- **Adaptadores con Testcontainers:**
  - repositorios JDBC de Roles;
  - V2 con la matriz D-16. Una prueba compara, rol por rol, los permisos de la base con la tabla de `data-model.md`, para SC-002.
- **Contrato:** las operaciones de Roles y la lista de usuarios, más `VerificadorDePermisos` en Roles y en Usuarios (403 con el cuerpo común).
- **Plataforma:**
  - `RespuestaAccesoNoAutorizado`;
  - la conversión de `permisos` en autoridades;
  - el 403 del API Gateway;
  - que `/interno/**` con un token de usuario responda el 403 con cuerpo.
- **SPA:**
  - Vitest para `tienePermiso`, «Tus funciones», la pantalla de roles con y sin escritura, y «Acceso no autorizado»;
  - Playwright para crear un rol y asignarlo con la API simulada.
- **Punta a punta:** `deploy/probar_autenticacion.sh` suma tres pasos:
  - `admin.demo` crea un rol, se lo asigna a `docente.demo`, y la nueva sesión de `docente.demo` trae el permiso;
  - un usuario con solo lectura recibe 403 al crear un rol;
  - `docente.coordinador.demo` trae los permisos de sus dos roles.

  Al final borra lo que creó, para poder repetirse.
