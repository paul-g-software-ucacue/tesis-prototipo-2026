---

description: "Tareas de F05-H02: Roles con permisos y varios roles por usuario"
---

# Tasks: Roles con permisos y varios roles por usuario

**Input**: documentos de diseño en `specs/011-roles-permisos/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/roles.md](contracts/roles.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-16, D-59, D-62, D-63, D-64, D-65, ADR-014, ADR-015, ADR-016, ADR-018, ADR-019, ADR-020 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**:
- **Fundacional:** el contrato, el 403 común, los permisos en el token y el servicio Roles con la matriz D-16. Sin eso, ninguna historia funciona.
- **US1:** el administrador crea un rol y lo asigna.
- **US2:** solo lectura y 403 en cada operación (RNF-SEG-03).
- **US3:** varios roles y «Tus funciones».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Rutas relativas a la raíz. Abreviaturas:
  - `ROL` = `backend/servicios/roles/src`, `JROL` = `ROL/main/java/ec/mdd/roles`, `PROL` = `ROL/test/java/ec/mdd/roles` e `IROL` = `ROL/integrationTest/java/ec/mdd/roles`;
  - `USU` = `backend/servicios/usuarios/src`, `JUSU` = `USU/main/java/ec/mdd/usuarios` y `PUSU` = `USU/test/java/ec/mdd/usuarios`;
  - `PLAT` = `backend/plataforma`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; puertos en `puertos/`; aplicación sin infraestructura; adaptadores e `infraestructura/` con Spring. `ArquitecturaTest` y `ModulosTest` lo comprueban.
- **Entre servicios:** Usuarios solo usa `ec.mdd.roles.puertos.entrada`; Roles no usa nada de Usuarios (research §2).
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers y PostgreSQL; contrato con swagger-request-validator; 60 % de líneas por módulo (D-32).
- **Idioma:** identificadores del dominio, mensajes y JavaDoc de puertos y dominio en español (principio XII).
- **Formato:** `./gradlew spotlessApply` y `npm run formato` antes de cada commit.
- **Evidencia y commits:** la evidencia va en `specs/011-roles-permisos/evidencia.md`; los commits llevan `Refs #44`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/011-roles-permisos/evidencia.md` con estas secciones: entorno; escenario 1 (rol nuevo); escenario 2 (solo lectura y 403); escenario 3 (varios roles); equipo de demostración; notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV)

- [X] T002 Crear `contratos/roles/openapi.yaml` según contracts/roles.md: las 9 operaciones, con `x-mdd-permiso` en las de `/api/`, los esquemas y las respuestas 401, 403 `AccesoNoAutorizado`, 404, 409, 500 y 503.
  - `Rol.nombre`: «1 a 80»; `Rol.descripcion`: «hasta 300»; `Rol.permisos`: `minItems: 1`; `alcance` ∈ `TODO`, `PROPIO`; `tipo` ∈ `LECTURA`, `ESCRITURA`, `APROBACION`, `FIRMA`.
  - `consultarAutorizacion` en `/interno/roles/autorizaciones/{usuario}`, sin `x-mdd-permiso` (token de proceso, ADR-018).
- [X] T003 Cambiar `contratos/usuarios/openapi.yaml` (`listarUsuarios` con `x-mdd-permiso: USUARIOS_ROLES:LECTURA`; el 403 de `recibirEvento` pasa a `AccesoNoAutorizado`), `contratos/comun/seguridad.yaml` (`SesionActual.permisos`, opcional; la respuesta `AccesoNoAutorizado`) y `contratos/comun/errores.md` (las 5 filas de contracts/roles.md). Agregar a `backend/servicios/roles/build.gradle` `id 'mdd.contrato'` y `implementation 'org.springframework.boot:spring-boot-starter-jdbc'` (para `JdbcClient`). Regenerar las interfaces del backend y los tipos de la SPA: `generar:api` gana el contrato de roles (`src/api/generado/roles.d.ts`).

### Plataforma: el 403 común y los permisos como autoridades

- [X] T004 [P] Prueba primero, en `PLAT/seguridad/src/test/java/ec/mdd/plataforma/seguridad/SeguridadProcesoTest.java`:
  - un token con `permisos: ["USUARIOS_ROLES:LECTURA", "OFERTA:LECTURA:PROPIO"]` da las autoridades `PERMISO_USUARIOS_ROLES:LECTURA` y `PERMISO_OFERTA:LECTURA`, además de `ROLE_<rol>`;
  - un método con `@PreAuthorize` sin el permiso responde 403 con el cuerpo `SEGURIDAD_ACCESO_NO_AUTORIZADO`, igual que un token de usuario en `/interno/**` y uno de proceso fuera de él;
  - en `PLAT/errores/src/test/java/ec/mdd/plataforma/errores/ControladorDeErroresTest.java`, la prueba `dadoUn403DeLaSeguridad_…entoncesNoLlevaCuerpo` pasa a esperar el cuerpo `SEGURIDAD_ACCESO_NO_AUTORIZADO` (el 403 que llega a `/error`).
- [X] T005 Agregar `SEGURIDAD_ACCESO_NO_AUTORIZADO` a `PLAT/errores-nucleo/…/CodigosComunes.java`, con su fila del catálogo. En `PLAT/seguridad`:
  - crear `RespuestaAccesoNoAutorizado.java` (un `AccessDeniedHandler` con cuerpo fijo y sin detalles, como `RespuestaNoAutenticado`);
  - en `SeguridadProcesoAutoConfiguracion`: un conversor que una `roles` y `permisos`, `@EnableMethodSecurity` y el manejador de acceso denegado.

  En `PLAT/errores/…/ControladorDeErrores.java`, el 403 responde ese cuerpo (`CodigosComunes.segunEstado(403)`) y su JavaDoc deja de decir «sin cuerpo». Actualizar los comentarios «hasta F05-H02» de `Usuario.java` y `CargaUsuarioDePrueba.java`; **no** el de `V2__usuarios.sql`, porque Flyway compara su suma de verificación.

  T004 pasa.
- [X] T006 [P] Prueba primero, en `backend/gateway/src/test/…`: una ruta fuera de `/api/**` con un token válido responde 403 con el mismo cuerpo. Después configurar el manejador de acceso denegado en `backend/gateway/src/main/java/ec/mdd/gateway/SeguridadGatewayConfig.java`. T006 pasa.
- [X] T007 Prueba primero, en `PLAT/pruebas/src/test/…`: `VerificadorDePermisos` lee un contrato de prueba con dos operaciones con `x-mdd-permiso` y una sin él. Contra una aplicación de prueba con `@PreAuthorize`, informa una operación sin anotación o con el permiso equivocado, y acepta las correctas. Después crear `PLAT/pruebas/src/main/java/ec/mdd/plataforma/pruebas/VerificadorDePermisos.java`: por cada operación con `x-mdd-permiso`, un token con todos los demás permisos espera 403 con el cuerpo común, y uno con ese permiso espera algo distinto de 403. T007 pasa.

### Dominio de Roles

- [X] T008 [P] Prueba primero, en `PROL/dominio/`, sin dobles:
  - `TipoDePermiso` y `Alcance` (valores);
  - un `Permiso` con un tipo que su funcionalidad no admite se rechaza;
  - `Rol`: nombre de 1 a 80 con al menos una letra o un dígito, descripción hasta 300, al menos un permiso;
  - identificador derivado del nombre (data-model.md): `Auditor de cargas` → `AUDITOR_DE_CARGAS`; `Gestión  de  cargas!` → `GESTION_DE_CARGAS`; un nombre de 80 caracteres da uno de 64; «Auditor de Cargas.» da el mismo que «Auditor de cargas»;
  - `PermisosEfectivos`: unión de varios roles, el alcance `TODO` gana a `PROPIO`, orden alfabético y la forma de texto `FUNCIONALIDAD:TIPO[:PROPIO]`;
  - `ReglaDeAdministracion`: un rol predefinido no se edita ni se borra; uno asignado no se borra; un nombre cuyo identificador derivado ya existe se rechaza, también contra los de D-16 («Administrador» → `ADMINISTRADOR`); un cambio de los roles de un usuario, o la edición de un rol propio, que deja sin `USUARIOS_ROLES:ESCRITURA`, o se lo quita a quien lo hace, se rechaza.
- [X] T009 Crear en `JROL/dominio/` `Funcionalidad`, `TipoDePermiso`, `Alcance`, `Permiso`, `Rol`, `PermisosEfectivos` y `ReglaDeAdministracion`, con JavaDoc en español que cite D-16, D-63, D-64 y D-65. T008 pasa.

### Persistencia de Roles

- [X] T010 Crear `ROL/main/resources/db/migration/roles/V2__matriz_d16.sql`: `funcionalidad`, `funcionalidad_tipo`, `rol` y `rol_permiso`, con las 16 funcionalidades, sus tipos y los 10 roles de D-16 (`predefinido = true`), con los permisos y alcances de la tabla «Matriz D-16» de data-model.md. Crear `V3__asignaciones_de_rol.sql`: `asignacion_rol`, `inicializacion` y `registro_cambio`. Los tipos y longitudes, según data-model.md.
- [X] T011 Prueba primero, en `IROL/MatrizD16IntegrationTest.java` (Testcontainers): después de migrar, cada uno de los 10 roles tiene **exactamente** los permisos y alcances de data-model.md, escritos en la prueba como tabla esperada (SC-002), y las 16 funcionalidades tienen sus tipos. `RolesEsquemaIntegrationTest` sigue en verde, con las tablas nuevas.
- [X] T012 Prueba primero, en `IROL/RepositoriosJdbcIntegrationTest.java`:
  - guardar, leer, editar y borrar un rol con sus permisos;
  - asignar y quitar roles (`asignacion_rol`);
  - contar usuarios por rol;
  - inicialización una sola vez;
  - registro de cambios.

  Crear en `JROL/puertos/salida/` `RepositorioRoles`, `RepositorioAsignacionesDeRol` y `RegistroDeCambios`, y en `JROL/adaptadores/salida/` sus implementaciones con `JdbcClient`. T011 y T012 pasan.

### Autorización en la sesión (ADR-021, D-62)

- [X] T013 Prueba primero, en `PROL/aplicacion/` (Mockito en los puertos):
  - `ConsultarAutorizacionServicio` devuelve los roles y los permisos efectivos de un usuario, y vacíos si no tiene;
  - `AsignarRolesInicialesServicio` asigna solo la primera vez (si ya hay `inicializacion`, no hace nada aunque el usuario no tenga roles), rechaza un rol desconocido y registra el cambio como `INICIALIZAR`.

  Crear los puertos de entrada `JROL/puertos/entrada/ConsultarAutorizacion.java` y `AsignarRolesIniciales.java` (con registros de valor propios), sus servicios y `JROL/infraestructura/ConfiguracionRoles.java`. Exponer `consultarAutorizacion` en `JROL/adaptadores/entrada/AutorizacionControlador.java` (`/interno/`). T013 pasa.
- [X] T014 Prueba primero, en `PUSU/aplicacion/IniciarSesionServicioTest.java`, `RenovarSesionServicioTest.java` y `PUSU/adaptadores/salida/EmisorJwtTest.java`:
  - el inicio de sesión y la renovación piden la autorización a `ProveedorDeAutorizacion` y emiten un token con `roles` y `permisos` desde ahí (D-62);
  - el token de proceso no los lleva.

  Después:
  - crear el puerto `JUSU/puertos/salida/ProveedorDeAutorizacion.java` y su adaptador `JUSU/adaptadores/salida/AutorizacionDesdeRoles.java`, que delega en `ConsultarAutorizacion` y `AsignarRolesIniciales`;
  - quitar `roles` de `Usuario` y el mapeo de `usuario_rol` de `UsuarioJpa` (la tabla **no** se borra, research §1);
  - retirar `RolMdd`;
  - agregar `implementation project(':servicios:roles')` a `backend/servicios/usuarios/build.gradle`;
  - ajustar `USU/integrationTest/java/ec/mdd/usuarios/RepositorioUsuariosJpaIntegrationTest.java`, porque `Usuario` ya no tiene roles.

  T014 pasa, y `ModulosTest` de `identidad` y `:servicios:usuarios:integrationTest` siguen en verde.
- [X] T015 Prueba primero, en `PUSU/infraestructura/CargaUsuariosDemoTest.java` y `CargaUsuarioDePruebaTest.java`: al crear o actualizar un usuario de la demo, o `autor.prueba`, se piden sus roles iniciales del archivo; un rol desconocido falla y dice cuál; ya no se actualizan roles en cada arranque (FR-009). Después cambiar `CargaUsuariosDemo` y `CargaUsuarioDePrueba`, y ajustar `USU/integrationTest/java/ec/mdd/usuarios/CargaUsuariosDemoIntegrationTest.java` y `CargaUsuarioDePruebaIntegrationTest.java` con un `ProveedorDeAutorizacion` falso escrito a mano (sin Mockito: son adaptadores) que anota los roles iniciales pedidos. T015 pasa.
- [X] T016 Prueba primero, en `PUSU/adaptadores/entrada/SesionContratoTest.java`: `consultarSesion` devuelve `permisos` del token, válido contra el contrato. Después cambiar `SesionControlador`. T016 pasa.

**Checkpoint**: el contrato, el 403 común, la matriz D-16 en la base y los permisos en el token están listos; `./gradlew check` y las pruebas de integración de Roles y Usuarios en verde.

---

## Phase 3: User Story 1 - El administrador crea un rol y se lo asigna a un usuario (Priority: P1) 🎯 MVP

**Goal**: `admin.demo` crea, edita y borra roles propios y asigna varios a un usuario; los 10 de D-16 son fijos (escenario 1; FR-002 a FR-004, FR-009 y FR-011; SC-001 y SC-006).

**Independent Test**: con `admin.demo`, crear un rol con 3 permisos, asignárselo a `docente.demo` y comprobar que su nueva sesión trae esos permisos.

- [X] T017 [US1] Prueba primero, en `PROL/aplicacion/GestionarRolesServicioTest.java` y `AsignarRolesServicioTest.java` (Mockito):
  - crear, editar y borrar con las reglas de `ReglaDeAdministracion`: `RolPredefinido`, `RolAsignado`, `NombreRepetido` y `SinAdministrador`;
  - editar un rol propio que es la única fuente de `USUARIOS_ROLES:ESCRITURA` y quitarle ese permiso da `SinAdministrador`;
  - asignar el conjunto de roles de un usuario (vacío también) y rechazar un rol desconocido;
  - cada cambio queda en `RegistroDeCambios`, con quién y cuándo (FR-011): `CREAR_ROL`, `EDITAR_ROL`, `BORRAR_ROL`, `ASIGNAR_ROL` y `QUITAR_ROL`.

  Crear los puertos `GestionarRoles` y `AsignarRoles`, sus servicios y las excepciones. T017 pasa.
- [X] T018 [US1] Prueba primero, en `PROL/adaptadores/entrada/RolesContratoTest.java`: cada operación de `/api/roles` con un token con los permisos, contra el contrato, incluidos los 409 de los 4 códigos (también `ROLES_SIN_ADMINISTRADOR` en `editarRol` y `ROLES_NOMBRE_REPETIDO` al crear «Administrador»), el 400 de un permiso con un tipo que la funcionalidad no admite y el 400 de `permisos: []`. `listarRolesDeUsuarios` (`GET /api/roles/usuarios`) y `asignarRoles` (`PUT /api/roles/usuarios/{usuario}`) devuelven `RolesDeUsuario`. Agregar `ErroresRolesTest`, que compara el catálogo con `errores.md`. Después crear `JROL/adaptadores/entrada/RolesControlador.java` y `ErroresRoles.java`. T018 pasa.
- [X] T019 [P] [US1] Prueba primero, en `PUSU/adaptadores/entrada/UsuariosContratoTest.java`: `listarUsuarios` devuelve `usuario`, `nombre` y `activo`, ordenados, sin hashes, válido contra el contrato. Después agregar la consulta al puerto de Usuarios y su controlador. T019 pasa.
- [X] T020 [P] [US1] Prueba primero, en `frontend/src/pantallas/roles/*.test.tsx`, con la API simulada:
  - la lista de roles muestra cuáles son de D-16;
  - el detalle de un rol de D-16 muestra su matriz en solo lectura;
  - el formulario crea un rol con una casilla por cada tipo que admite cada funcionalidad y su alcance, y muestra los 409 y 400 del servidor;
  - sin ningún permiso marcado, el formulario no deja guardar y dice «El rol necesita al menos un permiso.»;
  - un rol propio con `usuariosAsignados > 0` muestra «Asignado a N usuarios. Quítaselo antes de borrarlo.» y no deja borrarlo;
  - la vista de usuarios asigna y quita roles.

  Crear `frontend/src/pantallas/roles/` (`Roles.tsx`, `RolFormulario.tsx`, `RolesDeUsuarios.tsx`), `frontend/src/sesion/funcionalidades.ts` (nombres en español), la ruta `/roles` en `rutas.tsx` y la entrada «Roles» en `navegacion/Marco.tsx`. T020 pasa.
- [ ] T021 [US1] Agregar a `deploy/probar_autenticacion.sh`, paso 10: `admin.demo` crea «Auditor de roles» con solo `USUARIOS_ROLES:LECTURA` y se lo asigna a `docente.demo`, y la nueva sesión de `docente.demo` trae ese permiso. Correrlo en el perfil y anotarlo.

**Checkpoint**: la administración de roles funciona por la API y por la pantalla.

---

## Phase 4: User Story 2 - Un usuario no puede hacer lo que su rol no permite (Priority: P1)

**Goal**: toda operación protegida responde 403 «Acceso no autorizado» sin el permiso, aunque se llame directamente al servidor, y la SPA no muestra lo que no se puede hacer (escenario 2; FR-006 a FR-008; SC-003 y SC-005).

**Independent Test**: con un usuario de solo lectura de «Usuarios y roles», ver los roles, pero recibir 403 al crear uno, desde la pantalla y por la API.

- [ ] T022 [US2] Prueba primero: `PROL/adaptadores/entrada/PermisosRolesTest.java` y `PUSU/adaptadores/entrada/PermisosUsuariosTest.java` corren `VerificadorDePermisos` sobre sus contratos (SC-003) y fallan mientras falten las anotaciones. Después poner `@PreAuthorize("hasAuthority('PERMISO_<permiso>')")` en cada método de `RolesControlador` y en `listarUsuarios`, según su `x-mdd-permiso`. T022 pasa; comprobar que vuelve a fallar si se quita una anotación.
- [X] T023 [P] [US2] Prueba primero, en `frontend/src/sesion/permisos.test.ts` y `RutaConPermiso.test.tsx`:
  - `tienePermiso` con y sin el permiso, y con alcance «propio»;
  - sin `USUARIOS_ROLES:LECTURA`, `/roles` muestra «Acceso no autorizado» sin pedir los roles a la API;
  - con solo lectura, la pantalla no muestra crear, editar, borrar ni asignar;
  - un 403 de la API se muestra con su título y descripción, sin el código (ADR-019).

  Crear `frontend/src/sesion/permisos.ts` y `RutaConPermiso.tsx`, y usarlos en la pantalla de roles y en el menú. T023 pasa.
- [ ] T024 [US2] Agregar a `deploy/probar_autenticacion.sh`, paso 11: con la sesión de `docente.demo` del paso 10, `GET /api/roles` da 200 y `POST /api/roles` da 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO`, con el cuerpo común y sin crear el rol. También `docente.demo` sin el rol (sesión nueva) da 403 en `GET /api/roles`. Correrlo y anotarlo.

**Checkpoint**: el mínimo privilegio se valida en el servidor y se refleja en la SPA.

---

## Phase 5: User Story 3 - Un usuario con varios roles tiene las funciones de todos (Priority: P1)

**Goal**: la unión de los permisos de varios roles, visible en la pantalla de inicio (escenario 3; FR-005 y FR-008; SC-004).

**Independent Test**: `docente.coordinador.demo` entra y «Tus funciones» muestra las de Docente y las de Coordinador de carrera, sin repetir.

- [ ] T025 [P] [US3] Prueba primero, en `frontend/src/pantallas/Inicio.test.tsx` y `frontend/src/sesion/roles.test.ts`:
  - «Tus funciones» con los permisos de `docente.coordinador.demo` muestra exactamente 4 funcionalidades, sin repetir: «Mi distributivo» (lectura y escritura, solo lo propio), «Oferta y propuesta de docentes» (lectura, escritura y aprobación, solo lo propio), «Distributivo del periodo» (lectura, solo lo propio) y «Docentes» (lectura);
  - un usuario sin permisos ve «Todavía no tienes funciones asignadas.»;
  - `nombreDelRol` muestra un rol fuera de D-16 con su identificador legible: `AUDITOR_DE_ROLES` → «Auditor de roles» (research §9).

  Después cambiar `frontend/src/pantallas/Inicio.tsx` y `frontend/src/sesion/roles.ts`. T025 pasa.
- [ ] T026 [US3] Prueba con Playwright, en `frontend/e2e/roles.spec.ts`, con la API simulada: `admin.demo` crea un rol y lo asigna; `docente.coordinador.demo` ve las funciones de sus dos roles; `docente.demo` abre `/roles` y ve «Acceso no autorizado». Agregar a `deploy/probar_autenticacion.sh`, paso 12: el token de `docente.coordinador.demo` trae en `permisos` la unión de Docente y Coordinador de carrera, sin repetidos. Al final, el script le quita el rol a `docente.demo`, comprueba que su nueva sesión ya no trae `USUARIOS_ROLES:LECTURA` (escenario 3.3) y borra «Auditor de roles», para poder repetirse. Correr todo y anotarlo.

**Checkpoint**: los tres escenarios pasan de punta a punta.

---

## Phase 6: Cierre

- [ ] T027 Correr `./gradlew check`, `:servicios:roles:integrationTest` y `:servicios:usuarios:integrationTest`; en el frontend, `npm run lint`, `formato:verificar`, `tipos`, `test -- --run --coverage`, `test:e2e` y `verificar:ruptura`. Además, `shellcheck` sobre los scripts cambiados y Gitleaks (`--text HEAD`) sobre un clon. Correr también `probar_errores.sh`, `probar_eventos.sh`, `probar_spa.sh` y `deploy/equipo/probar_equipo.sh`. Todo en verde, con el 60 % de líneas. Hacer commit y push, abrir el PR en borrador con `Refs #44` y marcarlo listo **después** de que terminen sus runs. Si la concurrencia cancela el run de `ready_for_review`, relanzarlo. Comprobar los 10 checks, `perfil-consolidado` y `5 · imágenes`.
- [ ] T028 Completar `evidencia.md` y la «Definición de terminado» del PR. Fusionarlo con *squash* **con la aprobación del autor**, con la rama al día con `main`. Después:
  - esperar el despliegue automático en el PC («Sano en N s»);
  - el autor entra con `admin.demo`, crea un rol y lo asigna (SC-001), y se comprueba que sigue después de reiniciar `identidad` (SC-006);
  - abrir el PR de evidencia con `Closes #44` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup (fase 1)**: T001 es libre.
- **Foundational (fase 2)**:
  - T002 y T003 antes que todo lo demás;
  - T004 antes que T005; T006 y T007 después de T005;
  - T008 antes que T009;
  - T010 antes que T011 y T012;
  - T013 después de T009 y T012;
  - T014 depende de T013; T015 y T016, de T014.
- **US1 (fase 3)**: depende de la fase 2. T017 antes que T018; T019 es libre; T020 depende de T003; T021 cierra la fase.
- **US2 (fase 4)**: depende de T007, T018 y T019. T022 y T023 en orden libre; T024 depende de T021.
- **US3 (fase 5)**: depende de T016. T025 es libre; T026 depende de T020, T023 y T024.
- **Cierre (fase 6)**: T027 y T028 dependen de todo.

### Parallel Opportunities

- Fase 2: T004 y T008; T006 y T007, una vez hecho T005.
- US1: T019 y T020; US2: T023; US3: T025.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, el 403 común, la matriz D-16 y los permisos en el token.
2. La administración de roles por la API y por la pantalla.
3. **Validar:** `admin.demo` crea un rol y se lo asigna a `docente.demo`.

### Incremental

1. **US2:** `@PreAuthorize` y `VerificadorDePermisos` en cada operación; la SPA oculta lo que no se puede hacer.
2. **US3:** «Tus funciones».
3. **Cierre:** evidencia, PR, fusión y despliegue.

---

## Notes

- El filtro de «solo lo propio» (D-64) no se implementa aquí: cada historia de F03 en adelante lo aplica y lo prueba.
- `usuarios.usuario_rol` queda sin uso; se borra en una historia posterior, cuando ya no haga falta volver a una versión anterior a F05-H02.
- Hacer commit por tarea o por grupo lógico en `011-roles-permisos`, con `Refs #44`.
