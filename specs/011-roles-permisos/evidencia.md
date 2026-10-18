# Evidencia: F05-H02

Registro de las comprobaciones de los tres escenarios de la historia (issue #44): rol nuevo (RF-02), solo lectura y 403 en el servidor (RF-02, RNF-SEG-03) y varios roles (RF-03). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos y las contraseñas de local y de la CI son ficticios; las del equipo de demostración nunca salen del equipo.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest 5.0.4 y Playwright 1.64.0. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |
| Backend | `./gradlew check integrationTest` en verde | 442 pruebas en todos los módulos: 80 de Roles y 115 de Usuarios. Cobertura de líneas: Roles 100 %, Usuarios 93,8 %, `plataforma:seguridad` 98,6 %, `plataforma:pruebas` 92,3 %. |
| SPA | `npm run test -- --run --coverage` y `npm run test:e2e` en verde | 84 pruebas de Vitest con 97,87 % de líneas; 41 de Playwright en Chromium y Firefox. |

## Base: contrato, 403 común, matriz D-16 y permisos en el token

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Contrato primero (T002, T003) | confirmada | `contratos/roles/openapi.yaml` con 9 operaciones y `x-mdd-permiso` en las 8 de `/api/`; `listarUsuarios` en Usuarios; `SesionActual.permisos` y la respuesta `AccesoNoAutorizado`; 5 filas nuevas en `errores.md`. `openApiGenerate` genera las interfaces de Roles y `generar:api` los tipos de la SPA (`roles.d.ts`). | 2026-10-18 |
| 403 común en los procesos (T004, T005) | confirmada | `SeguridadProcesoTest` (35): los permisos del token son autoridades `PERMISO_<FUNCIONALIDAD>:<TIPO>` sin el alcance; sin el permiso, un token de usuario en `/interno/**` y uno de proceso fuera de él responden el mismo 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO`, comparado con el catálogo. `ControladorDeErroresTest`: un 403 que llega a `/error` también lleva ese cuerpo. | 2026-10-18 |
| 403 antes que 400 (T005) | corregido | Spring MVC lee y valida el cuerpo antes de `@PreAuthorize`: un usuario sin permiso con un cuerpo inválido recibía 400 con el detalle de los campos. `AutorizacionPrevia` evalúa el mismo `@PreAuthorize` antes de leer la petición. Sin ella, `dadoUnTokenSinElPermisoYUnCuerpoIlegible_…` falla: se comprobó quitándola. | 2026-10-18 |
| 403 en el API Gateway (T006) | confirmada | `SeguridadGatewayTest`: un token de usuario y uno de proceso en `/interno/**` reciben 403 con el cuerpo común y `Content-Type: application/json`. | 2026-10-18 |
| `VerificadorDePermisos` (T007) | confirmada | `VerificadorDePermisosTest` (4): lee las operaciones con `x-mdd-permiso`; acepta el código que las cumple; informa una operación sin anotación y otra con otro permiso, y no las correctas; falla si el contrato no tiene operaciones protegidas. | 2026-10-18 |
| Dominio de Roles (T008, T009) | confirmada | Sin dobles: `PermisoTest` (8), `RolTest` (8), `PermisosEfectivosTest` (4) y `ReglaDeAdministracionTest` (8). Identificador derivado del nombre (`Gestión  de  cargas!` → `GESTION_DE_CARGAS`, 80 caracteres → 64, «Auditor de Cargas.» = «Auditor de cargas»); unión con `TODO` sobre `PROPIO`; predefinido, asignado, nombre repetido (también «Administrador» contra `ADMINISTRADOR`) y sin administrador, también al editar un rol propio. | 2026-10-18 |
| Matriz D-16 en la base (T010, T011, SC-002) | confirmada | `MatrizD16IntegrationTest` (Testcontainers): cada uno de los 10 roles tiene **exactamente** los permisos y alcances de `data-model.md`, escritos a mano en la prueba; las 16 funcionalidades con sus tipos. `RolesEsquemaIntegrationTest`: todas las tablas en el esquema `roles`. | 2026-10-18 |
| Vuelta atrás segura (D-55) | confirmada | `RolesEsquemaIntegrationTest`: la versión anterior, que solo conoce V1, arranca sobre un esquema con V2 y V3 (Flyway las ignora como futuras). `usuarios.usuario_rol` no se borra. | 2026-10-18 |
| Repositorios JDBC (T012) | confirmada | `RepositoriosJdbcIntegrationTest` (6): funcionalidades en orden; guardar, editar y borrar un rol con sus permisos; asignar, quitar y contar; inicialización una sola vez, aunque después el usuario quede sin roles; registro con el detalle en `jsonb`. | 2026-10-18 |
| Autorización y roles iniciales (T013) | confirmada | `AutorizacionServiciosTest` (5, Mockito en los puertos): unión de los permisos de dos roles; sin roles, listas vacías; roles iniciales una sola vez, registrados como `INICIALIZAR`; un rol desconocido falla, dice cuál y no asigna nada. `AutorizacionContratoTest`: `GET /interno/roles/autorizaciones/{usuario}` responde 200 con token de proceso y el 403 común con uno de usuario, válidos contra el contrato. | 2026-10-18 |
| Permisos en el token (T014, D-62) | confirmada | `IniciarSesionServicioTest` y `RenovarSesionServicioTest`: el inicio de sesión y la renovación piden roles y permisos a `ProveedorDeAutorizacion`; la renovación de un token que decía solo `DOCENTE` emite uno con lo que Roles dice ahora. `EmisorJwtTest`: claim `permisos` ordenado; el token de proceso no lo lleva. `ModulosTest` de `identidad` en verde: Usuarios solo usa `ec.mdd.roles.puertos.entrada`. `identidad` arranca entero (`ErrorTecnicoIntegrationTest`, `EsquemasIntegrationTest`). | 2026-10-18 |
| Roles iniciales de la demo (T015, FR-009) | confirmada | `CargaUsuariosDemoTest` y `CargaUsuarioDePruebaTest`, con un doble escrito a mano: los roles iniciales se piden en cada arranque, también para usuarios que ya existían (el caso del equipo al desplegar F05-H02); los que el administrador cambió no se tocan; un rol desconocido detiene el arranque y dice cuál. Las pruebas de integración de Usuarios pasan con el doble. | 2026-10-18 |
| Sesión actual con permisos (T016) | confirmada | `SesionContratoTest`: `consultarSesion` devuelve `permisos` del token, ordenados, válido contra el contrato. | 2026-10-18 |

## Escenario 1: rol nuevo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Gestión y asignación (T017, FR-011) | confirmada | `GestionarRolesServicioTest` (9) y `AsignarRolesServicioTest` (7), Mockito en los puertos: crear, editar y borrar con las 4 reglas; asignar, también una lista vacía; cada cambio en el registro con quién y cuándo (`CREAR_ROL`, `EDITAR_ROL`, `BORRAR_ROL`, `ASIGNAR_ROL`, `QUITAR_ROL`), y nada si no cambió nada. | 2026-10-18 |
| API de Roles (T018) | confirmada | `RolesContratoTest` (11): cada operación contra el contrato; 409 de los 4 códigos (también `ROLES_SIN_ADMINISTRADOR` al editar y `ROLES_NOMBRE_REPETIDO` al crear «Administrador»); 400 con `permisos: []`, con un tipo que la funcionalidad no admite («Cargas de datos» no admite el permiso de firma.) y con un rol que no existe; 404 con el código común. `ErroresRolesTest`: el catálogo coincide con `errores.md`. | 2026-10-18 |
| Lista de usuarios (T019) | confirmada | `UsuariosContratoTest`: `usuario`, `nombre` y `activo`, ordenados, sin hashes, válido contra el contrato. `RepositorioUsuariosJpaIntegrationTest`: ordenados por nombre de usuario. | 2026-10-18 |
| Pantalla «Roles» (T020) | confirmada | `Roles.test.tsx` (12): la lista marca los de la matriz D-16; su detalle, en solo lectura; el formulario tiene un selector por cada tipo que admite cada funcionalidad y envía los permisos con su alcance; sin permisos, «El rol necesita al menos un permiso.» y no envía nada; los 409 y 400 con su título y su detalle, sin códigos; editar y borrar un rol propio; «Asignado a 2 usuarios. Quítaselo antes de borrarlo.» con el botón desactivado; la vista de usuarios asigna roles y muestra el 409 «Sin administrador». | 2026-10-18 |
| De punta a punta (T021) | confirmada | Perfil local (`docker compose up -d --build --wait`, sobre la base de F05-H01, que migró `roles` a V3) y `deploy/probar_autenticacion.sh`, paso 10: `admin.demo` crea «Auditor de roles» (201, `AUDITOR_DE_ROLES`) y se lo asigna a `docente.demo`; **la renovación de su sesión anterior ya trae `USUARIOS_ROLES:LECTURA` (D-62)** y su nueva sesión tiene `["AUDITOR_DE_ROLES","DOCENTE"]` con los permisos de los dos. | 2026-10-18 |
| Reinicio (SC-006, FR-009) | confirmada | En el perfil local: `admin.demo` crea «Rol de reinicio» y deja a `docente.demo` solo con él (le quita su rol inicial); después de `docker compose restart identidad`, `docente.demo` sigue con `["ROL_DE_REINICIO"]` y el rol, con 1 usuario. Después se restauró. | 2026-10-18 |

## Escenario 2: solo lectura y 403

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Una prueba de autorización por operación (T022, SC-003) | confirmada | `PermisosRolesTest` y `PermisosUsuariosTest` corren `VerificadorDePermisos` sobre sus contratos. Escritas antes de las anotaciones, fallaron en las 9 operaciones; con los `@PreAuthorize`, pasan; sin la de `borrarRol`, fallan solo en `DELETE /api/roles/AUDITOR_DE_CARGAS`. | 2026-10-18 |
| Proxy por clase (T022) | corregido | Con `@EnableMethodSecurity` por defecto, los controladores que implementan las interfaces generadas quedaban detrás de un proxy por interfaz y sus rutas respondían 404. `proxyTargetClass = true` en la plataforma, con una prueba de regresión en `SeguridadProcesoTest`. | 2026-10-18 |
| La SPA oculta lo que no se puede hacer (T023, SC-005) | confirmada | `permisos.test.ts` (4) y `Roles.test.tsx`: con solo lectura no aparecen crear, editar, borrar ni asignar; sin `USUARIOS_ROLES:LECTURA`, `/roles` muestra «Acceso no autorizado» sin pedir nada a `/api/roles` y el menú no ofrece «Roles»; un 403 de la API se muestra con su título y descripción, sin código ni estado. `mensajes.test.ts`: el texto coincide con `errores.md`. | 2026-10-18 |
| De punta a punta (T024) | confirmada | `probar_autenticacion.sh`, paso 11: con solo lectura, `GET /api/roles` 200; `POST /api/roles` 403 con el cuerpo común, **también con un cuerpo inválido**; el rol no se creó (404). Paso 12: `docente.demo` sin el rol recibe el 403 común en `GET /api/roles`. | 2026-10-18 |

## Escenario 3: varios roles

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| «Tus funciones» (T025, SC-004) | confirmada | `Inicio.test.tsx`: con los permisos de `docente.coordinador.demo`, exactamente 4 funcionalidades sin repetir: «Docentes» (lectura), «Oferta y propuesta de docentes» (lectura, escritura y aprobación · solo lo propio), «Distributivo del periodo» (lectura · solo lo propio) y «Mi distributivo» (lectura y escritura · solo lo propio); con alcances mezclados, el de cada tipo; sin permisos, «Todavía no tienes funciones asignadas.»; un rol creado se muestra como «Auditor de roles». | 2026-10-18 |
| Playwright y accesibilidad (T026) | confirmada | `e2e/roles.spec.ts` (4): el administrador crea un rol y se lo asigna a `docente.demo`; `docente.coordinador.demo` ve 4 funciones y no ve «Roles»; `docente.demo` en `/roles` ve «Acceso no autorizado» sin pedir `/api/roles` ni texto técnico; axe-core sin fallas WCAG 2.1 AA en la lista, el formulario y la pestaña Usuarios. axe encontró enlaces del menú lateral directamente en el `<ul>` (una falla anterior de la base de la SPA): cada entrada va ahora en su `<li>`. | 2026-10-18 |
| De punta a punta (T026) | confirmada | `probar_autenticacion.sh`, paso 12: `docente.coordinador.demo` trae exactamente los 7 permisos de Docente y de Coordinador de carrera, sin repetir. Al quitarle el rol a `docente.demo` (escenario 3.3), su nueva sesión ya no trae `USUARIOS_ROLES:LECTURA`; el rol de prueba se borra (204). Los 12 pasos en `OK`, dos corridas seguidas. `roles.registro_cambio` guarda cada cambio con `admin.demo`; `roles.inicializacion`, los 11 usuarios de la demo y `autor.prueba`. | 2026-10-18 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Otros scripts de punta a punta (T027) | confirmada | `probar_errores.sh`, `probar_eventos.sh` y `probar_spa.sh`: todos los pasos pasan. | 2026-10-18 |
| SPA (T027) | confirmada | `lint`, `formato:verificar`, `tipos` y `verificar:ruptura` (4 pasos) en verde. | 2026-10-18 |
| Gitleaks y shellcheck (T027) | confirmada | Gitleaks v8.30.1 sobre un clon en el scratchpad (montado con `:ro,Z`, nunca el repositorio): los 16 commits de la rama, sin fugas; en modo `dir`, solo los certificados de la CA de prueba de `firma`, que ya están en `main`. `shellcheck` sin observaciones en `probar_autenticacion.sh`. | 2026-10-18 |
| Primera corrida de la CI (T027) | corregido | Dos fallas. **`test-frontend`:** axe midió el contraste del diálogo «Rol nuevo» a mitad de su transición de opacidad (en local no pasaba); la prueba espera ahora `opacity: 1` antes de analizar, y pasó 12 veces seguidas. **`5 · imágenes`:** Grype publicó CVE-2026-77214 (alta) en `libexpat` 2.8.5-r0 de la base de Nginx, corregida en 2.9.0-r0: se actualiza con `apk upgrade`, como zlib en B00-H08; la imagen local queda con `libexpat-2.9.0-r0`. Las altas nuevas de OpenSSL 3.5.8 en `postgres` y `caddy` no tienen arreglo y no bloquean. Ninguna de las dos fallas venía del código de la historia. | 2026-10-18 |
| Intermitencia (T027) | anotada | Una corrida de `./gradlew check integrationTest` falló en `ClienteDeEventosTest` (`IOException` contra el receptor simulado) mientras corrían los scripts de Docker; el módulo no cambió en esta historia y pasó tres veces seguidas con `--rerun-tasks`, y `./gradlew check` completo volvió a pasar. | 2026-10-18 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Equipo simulado (T027) | confirmada | `deploy/equipo/probar_equipo.sh` con la versión de esta rama: pasos 0 a 11 en `OK`. La prueba de humo inicia sesión con `docente.demo`, que recibe sus roles iniciales al arrancar; el token sigue valiendo después de reiniciar `identidad`; memoria 1 326,7 MiB; la versión rota vuelve a la anterior. | 2026-10-18 |

## Notas

- `usuarios.usuario_rol` queda sin uso: se borrará en una historia posterior, cuando ya no haga falta volver a una versión anterior a F05-H02.
- El filtro de «solo lo propio» (D-64) no se aplica aquí: cada historia que trae carreras, unidades o docentes lo aplica y lo prueba.
- La operación de prueba de eventos de Generación (D-48) queda fuera de SC-003 (supuesto de la especificación).
