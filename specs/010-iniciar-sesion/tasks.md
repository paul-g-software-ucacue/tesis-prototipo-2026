---

description: "Tareas de F05-H01: Iniciar sesión"
---

# Tasks: Iniciar sesión

**Input**: documentos de diseño en `specs/010-iniciar-sesion/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/usuarios.md](contracts/usuarios.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-37, D-42, D-50, D-51, D-57, D-59, D-60, D-61, ADR-014, ADR-016, ADR-019 y ADR-020 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**: el contrato, la migración y el dominio son fundacionales. Las historias de usuario son las de la especificación; los requisitos de RNF-SEG-07 (D-60) se reparten donde encajan:
- **US1:** credenciales válidas, con los usuarios de la demo (D-59) y `autor.prueba` apagado en el equipo (D-57);
- **US2:** credenciales inválidas, con el bloqueo por intentos (FR-010);
- **US3:** acceso sin sesión, con el vencimiento por inactividad y la renovación (FR-011, ADR-020).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Rutas relativas a la raíz. `USU` abrevia `backend/servicios/usuarios/src`, `JAVA` abrevia `USU/main/java/ec/mdd/usuarios` y `PRUEBA` abrevia `USU/test/java/ec/mdd/usuarios`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; puertos en `puertos/`; aplicación sin infraestructura; adaptadores y `infraestructura/` con Spring. `ArquitecturaTest` lo comprueba.
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers y PostgreSQL; contrato con swagger-request-validator; 60 % de líneas por módulo (D-32).
- **Idioma:** identificadores del dominio y mensajes en español; JavaDoc en español en puertos y dominio (principio XII).
- **Secretos:** ninguna contraseña en el repositorio, una imagen o un registro; en local y en la CI solo `cambiar-en-local-demo`.
- **Formato:** `./gradlew spotlessApply` y `npm run formato` antes de cada commit. **Evidencia:** en `specs/010-iniciar-sesion/evidencia.md`. Commits con `Refs #43`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/010-iniciar-sesion/evidencia.md` con estas secciones:
  - entorno;
  - escenario 1 (credenciales válidas y usuarios de la demo);
  - escenario 2 (credenciales inválidas y bloqueo);
  - escenario 3 (acceso sin sesión e inactividad);
  - RNF-SEG-02 (D-61);
  - equipo de demostración;
  - notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV)

- [X] T002 Cambiar `contratos/usuarios/openapi.yaml` según contracts/usuarios.md:
  - en `iniciarSesion`, la respuesta `429` con `Error` (`USUARIOS_INICIO_BLOQUEADO`) y el encabezado `Retry-After` (entero, segundos);
  - la operación nueva `renovarSesion` (`POST /api/usuarios/sesion/renovacion`, `tags: [Sesion]`, con seguridad, sin cuerpo; `200` `TokenSesion`, `401` `NoAutenticado`, `500`, `503`).

  Cambiar `contratos/comun/seguridad.yaml`: `SesionActual.nombre` («string», **opcional**, «Nombre para mostrar (claim `nombre` del token)»), sin tocar `required: [usuario, roles]`. Agregar a `contratos/comun/errores.md` la fila de `USUARIOS_INICIO_BLOQUEADO` (429, «Demasiados intentos», «Espera unos minutos y vuelve a intentarlo.»).
- [X] T003 Regenerar las interfaces del backend (`./gradlew :servicios:usuarios:compileJava`, ADR-015) y los tipos de la SPA (`npm run generar:api`). Comprobar que compila y que los demás servicios con `SesionControlador` (cargas, generacion y dashboard) siguen compilando sin cambios, porque `nombre` es opcional.

### Persistencia

- [X] T004 Prueba primero: en `PRUEBA/adaptadores/salida/UsuarioJpaTest.java` (Testcontainers), comprobar que después de las migraciones:
  - `usuario` tiene `nombre_para_mostrar` «varchar(120), no nulo»;
  - un `nombre_usuario` con mayúsculas guardado antes de V4 queda en minúsculas;
  - existe `intento_fallido` (`nombre` «varchar(64)» como clave primaria, `fallidos` «int», `bloqueado_hasta` «timestamptz, nulo» y `actualizado_en` «timestamptz»).
- [X] T005 Crear `USU/main/resources/db/migration/usuarios/V4__usuarios_demo.sql`: agrega `nombre_para_mostrar` (relleno con `nombre_usuario` en las filas existentes, después `not null`), pasa `nombre_usuario` a `lower(...)` y crea `intento_fallido`, con un comentario que cite D-59 y D-60. T004 pasa.

### Dominio

- [X] T006 [P] Prueba primero, en `PRUEBA/dominio/RolMddTest.java`: los 10 identificadores de research §1 (`ADMINISTRADOR`, `OPERADOR_DATOS`, `COORDINADOR_CARRERA`, `GESTOR_UNIDAD`, `RESPONSABLE_CONSOLIDACION`, `VICERRECTOR`, `RECTOR`, `MIEMBRO_OCS`, `SECRETARIA_GENERAL` y `DOCENTE`), y que un texto desconocido se rechaza.
- [X] T007 [P] Prueba primero, en `PRUEBA/dominio/UsuarioTest.java`: `Usuario.normalizar(" Coordinacion.Demo ")` da `coordinacion.demo`; el usuario guarda el nombre normalizado; `nombreParaMostrar` es obligatorio y no vacío; `toString` sigue sin el hash.
- [X] T008 Crear `JAVA/dominio/RolMdd.java` (enum con JavaDoc por rol, citando D-16 y el glosario) y cambiar `JAVA/dominio/Usuario.java`: campo `nombreParaMostrar`, normalización del nombre en el constructor (`trim` y minúsculas con `Locale.ROOT`) y el método estático `normalizar`. Ajustar `UsuarioJpa`, `RepositorioUsuariosJpa` (búsqueda por nombre normalizado) y `CargaUsuarioDePrueba` («autor.prueba» con nombre para mostrar «Autor de prueba»). T006 y T007 pasan.

**Checkpoint**: el contrato, la base y el dominio están listos; todo compila y las pruebas de B00-H03 siguen en verde.

---

## Phase 3: User Story 1 - Un usuario de la demo entra con sus credenciales (Priority: P1) 🎯 MVP

**Goal**: los 11 usuarios de la demo existen y entran con su nombre y solo sus roles; `autor.prueba` queda apagado en el equipo (escenario 1; FR-001 a FR-005 y FR-007; SC-001, SC-002, SC-005 y SC-006).

**Independent Test**: en el perfil local, `coordinacion.demo` y `vicerrectorado.demo` entran con `cambiar-en-local-demo` y la pantalla de inicio muestra su nombre y solo su rol.

- [X] T009 [P] [US1] Crear `USU/main/resources/usuarios-demo.yaml` con los 11 usuarios de research §2 (`usuario`, `nombre` y `roles`), sin contraseñas y con un comentario de cabecera que cite D-59 y diga que son ficticios.
- [X] T010 [US1] Prueba primero, en `PRUEBA/infraestructura/CargaUsuariosDemoTest.java` (Testcontainers):
  - con la clave común: crea los 11 activos, con hashes `$argon2id$`, su nombre y sus roles;
  - con un archivo `usuario=clave`: cada uno con su clave; un usuario sin línea en el archivo no se crea y se registra un aviso;
  - al volver a arrancar con otra clave: actualiza solo el hash, el nombre y los roles, sin duplicar;
  - sin archivo ni clave común: no crea nada y avisa;
  - nunca registra una clave (la prueba revisa el registro capturado).
- [X] T011 [US1] Crear `JAVA/infraestructura/CargaUsuariosDemo.java` (un `ApplicationRunner` como `CargaUsuarioDePrueba`), con `PropiedadesUsuarios.Demo` (`claves`: ruta; `clave`: texto) y el puerto `RepositorioUsuarios` (agregar `actualizar` si hace falta). Lee el YAML con SnakeYAML (`org.yaml.snakeyaml`, la que ya trae Spring Boot) y no pisa nada si no hay contraseña. Registrar el bean en `ConfiguracionUsuarios`. T010 pasa.
- [X] T012 [US1] Prueba primero, en `PRUEBA/infraestructura/CargaUsuarioDePruebaTest.java`: con `habilitado=false` y `autor.prueba` ya en la base, queda `activo=false` (D-57); con `habilitado=true`, sigue igual que hoy. Después cambiar `CargaUsuarioDePrueba` para que desactive. T012 pasa.
- [X] T013 [US1] Prueba primero, en `PRUEBA/adaptadores/salida/EmisorJwtTest.java`: el token lleva el claim `nombre` y el claim `inicio` (segundos), y `sub` y `roles` como hoy. Después cambiar `EmisorJwt` y el puerto `EmisorDeTokens`. T013 pasa.
- [X] T014 [US1] Prueba primero, en `PRUEBA/adaptadores/entrada/SesionContratoTest.java`: `consultarSesion` con un token de `docente.coordinador.demo` devuelve `usuario`, `nombre` y `roles` ordenados, válido contra el contrato. Después cambiar `SesionControlador.consultarSesion` para rellenar `nombre` desde el claim. T014 pasa.
- [X] T015 [US1] Configurar `backend/apps/identidad/src/main/resources/application.yaml` (`mdd.usuarios.demo.claves: ${MDD_USUARIOS_DEMO_CLAVES:}`, `mdd.usuarios.demo.clave: ${MDD_USUARIOS_DEMO_CLAVE:}`) y `.env.ejemplo` (`MDD_USUARIOS_DEMO_CLAVE=cambiar-en-local-demo`, con un comentario que cite D-59).
- [X] T016 [P] [US1] Prueba primero, en `frontend/src/pantallas/Inicio.test.tsx`: «Hola, Vicerrectorado académico (demo)» con el `nombre` de la sesión, y «Hola, autor.prueba» si no viene `nombre`; los roles con sus nombres en español («Coordinador de carrera», «Docente»…). Crear `frontend/src/sesion/roles.ts` (los 10 identificadores → nombre en español; un identificador desconocido se muestra tal cual) y cambiar `frontend/src/pantallas/Inicio.tsx`. T016 pasa.
- [X] T017 [US1] Agregar a `deploy/probar_autenticacion.sh` un paso que recorre los 11 usuarios de `usuarios-demo.yaml` (leído con `yq`): cada uno inicia sesión con la clave común y `GET /api/usuarios/sesion` devuelve su `nombre` y **exactamente** los roles del archivo (SC-001). Correrlo con el perfil local (`docker compose up -d --build --wait`) y anotar el resultado en la evidencia.

**Checkpoint**: los usuarios de la demo entran con su nombre y sus roles en local.

---

## Phase 4: User Story 2 - Un error de credenciales no revela cuál dato falló (Priority: P1)

**Goal**: el mismo mensaje para usuario inexistente, desactivado o contraseña incorrecta, y el bloqueo tras 5 intentos fallidos que tampoco revela si el usuario existe (escenario 2; FR-006 y FR-010; SC-003 y SC-007).

**Independent Test**: con un nombre inventado y con uno existente, 5 intentos fallidos llevan al sexto a 429 `USUARIOS_INICIO_BLOQUEADO`; el mensaje es el mismo en los dos casos.

- [X] T018 [P] [US2] Prueba primero, en `PRUEBA/dominio/PoliticaDeIntentosTest.java` (sin dobles, con un reloj fijo): de 1 a 4 fallos, no bloquea; el quinto bloquea 15 minutos; durante el bloqueo, `estaBloqueado` es verdadero; al vencer, un fallo vuelve a 1; los valores salen de los parámetros.
- [X] T019 [US2] Crear `JAVA/dominio/PoliticaDeIntentos.java` (regla pura, con `java.time.Clock`) y el registro de valor `EstadoDeIntentos` (`fallidos`, `bloqueadoHasta`). T018 pasa.
- [X] T020 [US2] Prueba primero, en `PRUEBA/adaptadores/salida/RepositorioIntentosJdbcTest.java` (Testcontainers): leer, registrar un fallo (atómico con `INSERT … ON CONFLICT … DO UPDATE`), bloquear, borrar al acertar y limpiar las filas sin bloqueo ni actividad por 24 horas. Crear el puerto `JAVA/puertos/salida/RepositorioIntentos.java` y `JAVA/adaptadores/salida/RepositorioIntentosJdbc.java`. T020 pasa.
- [X] T021 [US2] Prueba primero, en `PRUEBA/aplicacion/IniciarSesionServicioTest.java` (Mockito en los puertos): un nombre bloqueado lanza `InicioBloqueado` sin verificar la contraseña ni distinguir si existe; un fallo registra el intento con el nombre normalizado; el quinto fallo deja el nombre bloqueado; un acierto borra los intentos; los casos de B00-H03 siguen igual. Después cambiar `IniciarSesionServicio` y crear `JAVA/aplicacion/InicioBloqueado.java`. T021 pasa.
- [X] T022 [US2] Prueba primero, en `PRUEBA/adaptadores/entrada/ErroresUsuariosTest.java` y `SesionContratoTest.java`: `USUARIOS_INICIO_BLOQUEADO` en el catálogo coincide con `errores.md`, y `iniciarSesion` responde 429 con el modelo `Error` y `Retry-After`, válido contra el contrato. Después agregar el código a `ErroresUsuarios` y la traducción de `InicioBloqueado` (ADR-016). T022 pasa.
- [X] T023 [P] [US2] Prueba primero, en `frontend/src/pantallas/IniciarSesion.test.tsx`: con un 429 de la API simulada, la pantalla muestra «Demasiados intentos» y la descripción del servidor, sin texto técnico, igual que cualquier error (ADR-019). Ajustar solo si falla.
- [X] T024 [US2] Agregar a `deploy/probar_autenticacion.sh`: con un nombre inventado (`bloqueo-<aleatorio>`), 5 intentos con 401 y el sexto con 429 `USUARIOS_INICIO_BLOQUEADO`; con `ocs.demo` (existe y ningún otro script lo usa), el mismo resultado y el mismo mensaje. Nunca se bloquea `autor.prueba` ni `docente.coordinador.demo`, que usan los demás pasos del perfil. Correrlo y anotarlo.

**Checkpoint**: los errores de credenciales y el bloqueo no permiten saber qué usuarios existen.

---

## Phase 5: User Story 3 - Sin sesión, toda ruta interna lleva al inicio de sesión (Priority: P1)

**Goal**: sin sesión o con la sesión vencida por inactividad, la SPA lleva al inicio de sesión y después a la ruta pedida; mientras se trabaja, la sesión se renueva hasta 8 horas (escenario 3; FR-009 y FR-011; SC-004 y SC-008; ADR-020).

**Independent Test**: sin sesión, `/inicio` lleva al inicio de sesión y, al entrar, de vuelta a `/inicio`. Con actividad simulada, la sesión pasa los 30 minutos; sin actividad, vence a los 30.

- [X] T025 [US3] Prueba primero, en `PRUEBA/aplicacion/RenovarSesionServicioTest.java` (Mockito): con un token vigente y `inicio` hace menos de 8 horas, emite uno nuevo con el mismo `inicio`; con `inicio` hace más de 8 horas, lanza la excepción que termina en 401; un token de proceso (`roles: [PROCESO]`) se rechaza. Crear `JAVA/puertos/entrada/RenovarSesion.java`, `JAVA/aplicacion/RenovarSesionServicio.java` y la propiedad `mdd.usuarios.token.maximo` (PT8H) en `PropiedadesUsuarios`. T025 pasa.
- [X] T026 [US3] Prueba primero, en `PRUEBA/adaptadores/entrada/SesionContratoTest.java`: `renovarSesion` con un token vigente devuelve 200 `TokenSesion` con `venceEn` 30 minutos después; sin token, o pasadas las 8 horas, 401 `SEGURIDAD_NO_AUTENTICADO`. Después implementar `renovarSesion` en `SesionControlador`. Comprobar que el API Gateway la enruta y la protege igual que `consultarSesion`. T026 pasa.
- [X] T027 [P] [US3] Prueba primero, en `frontend/src/sesion/renovacion.test.ts` (temporizadores falsos de Vitest):
  - sin actividad, no llama a la renovación y la sesión se cierra a los 30 minutos con «sesión terminada»;
  - con actividad después de la última renovación y menos de 20 minutos restantes, llama una sola vez y reprograma el vencimiento con el `venceEn` nuevo;
  - si la renovación responde 401, la sesión se cierra como terminada;
  - después de una renovación, sin actividad del usuario, no hay otra renovación y la sesión vence a los 30 minutos de la última actividad: la petición de renovación **no** cuenta como actividad (H1 del análisis);
  - nunca escribe el token fuera de la memoria (D-51).
- [X] T028 [US3] Crear `frontend/src/sesion/renovacion.ts` y cambiar `frontend/src/sesion/almacen.ts` (registro de `ultimaActividad` y `ultimaRenovacion`), `frontend/src/api/cliente.ts` (marca actividad solo en las peticiones sin el encabezado interno `X-MDD-Automatica`; `renovacion.ts` lo envía en la suya) y `SesionProvider` (escucha la navegación, el teclado y el ratón con `passive` y los quita al cerrar). T027 pasa, y las pruebas de `sesion.test.tsx` y `cliente.test.ts` siguen en verde.
- [X] T029 [US3] Prueba con Playwright, en `frontend/e2e/sesion.spec.ts`: sin sesión, `/inicio` lleva a `/iniciar-sesion`; al entrar con un usuario de la demo de la API simulada, vuelve a `/inicio` con su nombre. Agregar a `deploy/probar_autenticacion.sh` la renovación con un token vigente (200) y con un token alterado (401). Correr todo y anotarlo.

**Checkpoint**: el acceso sin sesión y la inactividad funcionan; RNF-SEG-07 queda cubierto.

---

## Phase 6: Equipo de demostración y cierre

- [X] T030 Cambiar el equipo de demostración (D-57, D-59):
  - `deploy/equipo/compose.demo.yml`: `identidad` con `MDD_USUARIO_PRUEBA_HABILITADO: "false"`, sin `MDD_USUARIO_PRUEBA_CLAVE`, y con `MDD_USUARIOS_DEMO_CLAVES: /run/mdd/claves-demo` y el archivo `${MDD_SECRETOS:-/opt/mdd/secretos}/claves-demo` montado `:ro,Z`;
  - `deploy/equipo/preparar.sh`: crea `secretos/claves-demo` si no existe y **agrega** una línea `usuario=clave` (`openssl rand -hex 16`) por cada usuario de `usuarios-demo.yaml` (leído con `yq` o `grep`) que todavía no la tenga, sin tocar las existentes ni mostrar las claves; dueño 10001, grupo `mdd`, modo **0440** (0444 solo con `--no-interactivo`);
  - `deploy/equipo/probar_humo.sh`: los pasos 3 y 4 con `docente.demo` y su clave de `secretos/claves-demo`, y un paso nuevo en el que `autor.prueba` recibe 401;
  - `deploy/equipo/probar_equipo.sh`: verificar lo mismo, y que `claves-demo` tiene modo 0440 y la prueba de humo puede leerlo.

  Correr `probar_equipo.sh` en el PC (pasos 0 a 10 y los nuevos en `OK`) y anotarlo.
- [X] T031 Evidencia de RNF-SEG-02 (D-61, FR-008): en el perfil local, `select nombre_usuario, left(clave_hash, 10) from usuarios.usuario` muestra solo `$argon2id$`; no hay tablas de tokens; la prueba de B00-H06 de que el token no está en el almacenamiento del navegador sigue en verde; y los permisos de `/opt/mdd` del equipo (de la evidencia de B00-H08). Anotarlo en la sección RNF-SEG-02.
- [X] T032 Correr `./gradlew check` del backend, `npm run lint`, `npm run formato:verificar`, `npm run tipos`, `npm run test -- --run --coverage` y `npm run test:e2e` del frontend, `shellcheck` sobre los scripts cambiados, actionlint y Gitleaks (`--text HEAD`) sobre un clon. Todo en verde, con el 60 % de líneas. Hacer commit y push, y abrir el pull request de `010-iniciar-sesion` en borrador, con `Refs #43`; marcarlo listo cuando terminen los runs del borrador. Comprobar los 10 checks, `perfil-consolidado` (con los pasos nuevos de `probar_autenticacion.sh` y del equipo simulado) y `5 · imágenes`.
- [ ] T033 Completar `evidencia.md` y la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**. Después:
  - antes de fusionar, el autor corre `sudo deploy/equipo/preparar.sh` desde la rama, que solo crea `secretos/claves-demo` (si no, el despliegue falla y vuelve atrás; quickstart §4);
  - esperar el despliegue automático («Sano en N s» y `success` en *Deployments*);
  - en `https://mdd.localhost`, entrar con `vicerrectorado.demo` en menos de 1 minuto (SC-002, comprobación manual) y comprobar que `autor.prueba` ya no entra (SC-006);
  - abrir el pull request de evidencia con `Closes #43` y borrar las ramas fusionadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: T001 es libre.
- **Foundational (fase 2)**: T002 antes que T003; T004 antes que T005; T006 y T007 antes que T008; T008 después de T005.
- **US1 (fase 3)**: depende de la fase 2. T009 es libre; T010 antes que T011; T012, T013 y T014 después de T008; T015 después de T011; T016 depende de T003; T017 cierra la fase.
- **US2 (fase 4)**: depende de la fase 2. T018 antes que T019; T020 después de T005; T021 depende de T019 y T020; T022 después de T021; T023 depende de T003; T024 cierra la fase.
- **US3 (fase 5)**: depende de la fase 2 y de T013 (claim `inicio`). T025 antes que T026; T027 antes que T028; T029 cierra la fase.
- **Cierre (fase 6)**: T030 depende de US1; T031 es libre desde T017; T032 y T033 dependen de todo.

### Parallel Opportunities

- Fase 2: T006 y T007.
- US1: T009 y T016; US2: T018 y T023; US3: T027.
- US2 y US3 en paralelo con US1 desde la fase 2, salvo T013 (US3 lo necesita).
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fases 2 y US1)

1. Contrato, migración y dominio.
2. Usuarios de la demo con su nombre y sus roles, y la pantalla de inicio.
3. **Validar:** `coordinacion.demo` y `vicerrectorado.demo` entran en el perfil local.

### Incremental

1. **US2:** el bloqueo por intentos.
2. **US3:** la renovación y la inactividad (ADR-020).
3. **Cierre:** el equipo de demostración, la evidencia, el pull request y la fusión.

---

## Notes

- Los usuarios de la demo no tienen permisos todavía: qué puede hacer cada rol es de F05-H02 (#44).
- En local y en la CI, `autor.prueba` sigue habilitado (D-37); los scripts de punta a punta de B00-H03 a B00-H06 no cambian.
- Hacer commit por tarea o por grupo lógico en `010-iniciar-sesion`, con `Refs #43`.
