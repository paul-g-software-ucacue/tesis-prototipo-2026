---

description: "Tareas de B00-H04: modelo de errores común y catálogo de códigos"
---

# Tasks: Modelo de errores común y catálogo de códigos

**Input**: documentos de diseño en `specs/004-modelo-errores/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas y el principio IV exige pruebas de contrato. En cada grupo, las pruebas se escriben antes que el código que las hace pasar.

**Decisiones**: D-38, D-40, D-41, D-42, ADR-014, ADR-015 y ADR-016 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**: por historia de usuario de la especificación: US1, error reconocido; US2, error técnico inesperado; US3, error de entrada. El API Gateway (D-40) y las pruebas de punta a punta van en una fase transversal.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Las rutas son relativas a la raíz del repositorio. Gradle se ejecuta en `backend/` con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`; Docker y Testcontainers, con `sg docker -c '…'` mientras haga falta.

## Convenciones para todas las tareas

- **Paquetes:**
  - `ec.mdd.plataforma.errores.nucleo` (`plataforma:errores-nucleo`, sin servlet ni WebFlux);
  - `ec.mdd.plataforma.errores` (`plataforma:errores`, servlet).
- **Capas:** el dominio y la aplicación no conocen el catálogo ni HTTP. `Errores<Servicio>` vive en `adaptadores/entrada`, con `@Component` (research §2).
- **Pruebas de contrato:** con `@WebMvcTest`, `@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})` y dobles escritos a mano. Mockito solo en la capa de aplicación (principio VIII).
- **Cobertura:** JaCoCo cuenta solo la suite `test`. Lo que solo ejercitan las pruebas de integración necesita además una prueba unitaria pura.
- **Herramientas en contenedor** (CLI de openapi-generator, shellcheck, actionlint y Gitleaks): sobre una copia en el directorio temporal de la sesión, nunca montando el repositorio con `:z`.
- **Scripts de Bash:**
  - con `set -euo pipefail`, las comprobaciones van dentro de un `if` (patrón `verificar` de `deploy/probar_autenticacion.sh`);
  - las directivas de shellcheck para todo el archivo van antes del primer comando.
- **JavaDoc** en español, sin `@author`. **Formato:** `spotlessApply` antes de cada commit. **Evidencia:** en `specs/004-modelo-errores/evidencia.md`.

---

## Phase 1: Setup

- [ ] T001 En `backend/settings.gradle`, agregar `'plataforma:errores-nucleo'` y `'plataforma:errores'` a la línea de `include` de `plataforma`.
- [ ] T002 [P] Crear `specs/004-modelo-errores/evidencia.md` con estas secciones:
  - entorno;
  - contratos;
  - construcción y pruebas;
  - escenario 1 (error reconocido);
  - escenario 2 (error técnico);
  - escenario 3 (error de entrada);
  - API Gateway;
  - punta a punta;
  - notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

**Purpose**: contratos primero, el núcleo, el mecanismo de los procesos y la generación con validación (ADR-016).

**⚠️ CRITICAL**: ninguna historia puede empezar sin esta fase.

### Contratos (principio IV: antes del código)

- [ ] T003 [P] En `contratos/comun/errores.md`, agregar las 8 filas `COMUN_` de research §4, con título, descripción accionable y cuándo:
  - la descripción de `COMUN_ERROR_INTERNO` es «Ocurrió un error inesperado. Intenta de nuevo en unos minutos; si el problema sigue, informa el identificador del incidente.»;
  - la de `COMUN_SOLICITUD_INVALIDA` es «Revisa los campos marcados y vuelve a intentarlo.».

  Actualizar la nota final: los códigos `COMUN_` y `SEGURIDAD_` son de la plataforma.
- [ ] T004 [P] En `contratos/comun/errores.yaml`, agregar en `components.responses` las respuestas `SolicitudInvalida` (400), `ErrorInterno` (500) y `ServicioNoDisponible` (503). Las tres usan el esquema `Error`, con su descripción y el código que llevan.
- [ ] T005 En los contratos de `usuarios`, `cargas`, `generacion` y `dashboard`:
  - cada operación declara `'500': $ref: '../comun/errores.yaml#/components/responses/ErrorInterno'` y `'503': $ref: '../comun/errores.yaml#/components/responses/ServicioNoDisponible'`, porque el API Gateway puede darlo en cualquiera (D-40);
  - en `contratos/usuarios/openapi.yaml`, `POST /api/usuarios/sesion` agrega `'400': $ref: '../comun/errores.yaml#/components/responses/SolicitudInvalida'`, y `CredencialesSesion.usuario` y `clave` llevan `minLength: 1` (D-42).
- [ ] T006 Validar los 7 archivos de `contratos/` con `openapitools/openapi-generator-cli:v7.25.0 validate --recommend`, sobre una copia en el directorio temporal. Anotar el resultado en la sección «contratos» de `specs/004-modelo-errores/evidencia.md`.

### `plataforma:errores-nucleo`

- [ ] T007 Escribir primero las pruebas de `backend/plataforma/errores-nucleo/src/test/java/ec/mdd/plataforma/errores/nucleo/`:
  - `SaneadorDeTrazasTest`: una traza con un JWT, `Bearer abc.def`, `clave=secreta` y `password=x` en el mensaje de la excepción y de su causa queda con `[oculto]` en los 4 lugares, y conserva la clase, las líneas de la traza y el `Caused by`;
  - `RegistroDeErroresTest`: encuentra la entrada por la clase y por su superclase registrada más cercana; da vacío si no hay; falla con un mensaje claro si dos catálogos registran el mismo código o la misma excepción;
  - `IncidentesTest`: dos identificadores seguidos son distintos y tienen forma de UUID v4;
  - `RespuestaErrorTest`: las fábricas `deCatalogo`, `conDetalles` y `deIncidente` dejan nulos los campos que no corresponden.
- [ ] T008 Implementar en `backend/plataforma/errores-nucleo/`:
  - `build.gradle`, con `java-library` y `mdd.java-conventions`, más `api 'com.fasterxml.jackson.core:jackson-annotations'`;
  - **el modelo:** `RespuestaError` (record con `@JsonInclude(NON_NULL)`) y `DetalleRespuesta` (record);
  - **el catálogo:**
    - `DefinicionDeCodigo` (record: `codigo`, `estado`, `titulo` y `descripcion`, con validación del formato del código y de que el estado sea 4xx o 5xx);
    - `EntradaCatalogo` (record: `excepcion` y `definicion`, con la fábrica `de(clase, codigo, estado, titulo, descripcion)`);
    - `CatalogoDeErrores` (interfaz con `List<EntradaCatalogo> entradas()`);
    - `RegistroDeErrores`, que reúne los catálogos, valida los repetidos y busca por clase o superclase;
  - `CodigosComunes`, con las definiciones de los 8 códigos `COMUN_` y de `SEGURIDAD_NO_AUTENTICADO`, y una lista `TODOS`;
  - `SaneadorDeTrazas`, que convierte la excepción con sus causas a texto y la sanea;
  - `Incidentes`, que genera el `idIncidente`.

  JavaDoc del porqué de cada pieza (ADR-016). Así pasa T007.

### Verificación del catálogo y mecanismo de los procesos

- [ ] T009 [P] Agregar en `backend/plataforma/pruebas/` la dependencia `api project(':plataforma:errores-nucleo')` y la clase `VerificadorDeCatalogo`:
  - `verificar(CatalogoDeErrores)` y `verificar(List<DefinicionDeCodigo>)` leen la tabla de `contratos/comun/errores.md` y fallan si un código no está, o si su estado, título o descripción no coinciden;
  - tiene su prueba, con un `.md` de prueba en `src/test/resources`: una entrada que coincide pasa; una con otro título falla y nombra el código.
- [ ] T010 Escribir primero las pruebas de `backend/plataforma/errores/src/test/java/ec/mdd/plataforma/errores/`:
  - `ManejadorDeErroresTest`: `@SpringBootTest(RANDOM_PORT)` con una aplicación de prueba sin seguridad, un controlador de prueba y `OutputCaptureExtension`. Comprueba que:
    - el error de dominio de prueba `fixtures.dominio.HorasExcedidas`, registrado en un catálogo de prueba como `PRUEBA_HORAS_EXCEDIDAS` con 409 (D-41), responde 409 con su título, descripción y código, sin `detalles` ni `idIncidente`;
    - una subclase de una excepción registrada usa la entrada de la superclase;
    - otra excepción de dominio sin registrar responde 500 `COMUN_ERROR_INTERNO`, y el registro dice «sin código en el catálogo»;
    - un `IllegalStateException("fallo con eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ4In0.firma y clave=secreta")` responde 500 con `idIncidente`, sin «IllegalStateException», «ec.mdd», «eyJ» ni «secreta» en el cuerpo, y el registro tiene el `idIncidente`, la clase, la traza y `[oculto]`, pero no el JWT ni la clave;
    - un cuerpo con 2 campos inválidos responde 400 `COMUN_SOLICITUD_INVALIDA` con 2 `detalles` en español, y un campo vacío dice «es obligatorio»;
    - un JSON mal formado responde 400 `COMUN_CUERPO_ILEGIBLE`, sin el mensaje del analizador;
    - una ruta inexistente, un método no permitido, un `Accept` no soportado y un tipo de contenido no soportado responden 404, 405, 406 y 415 con sus códigos comunes;
    - ninguna respuesta tiene `timestamp`, `path`, `trace`, `status` ni `error`.
  - `ManejadorDeErroresSeguridadTest`: con `plataforma:seguridad` y `jwt()`, un controlador que lanza `AccessDeniedException` responde 403, no 500.
  - `ControladorDeErroresTest`: unitaria, con `MockHttpServletRequest` y los atributos `jakarta.servlet.error.*`: 404 → `COMUN_RECURSO_NO_ENCONTRADO`; 500 con una excepción → `COMUN_ERROR_INTERNO` con `idIncidente` y el incidente en el registro.
  - `CodigosComunesTest`: `VerificadorDeCatalogo.verificar(CodigosComunes.TODOS)` pasa.
- [ ] T011 Implementar `backend/plataforma/errores/`:
  - **`build.gradle`:** `java-library` y `mdd.java-conventions`, más:
    - `api project(':plataforma:errores-nucleo')`;
    - `implementation 'org.springframework.boot:spring-boot-starter-webmvc'` y `'org.springframework.boot:spring-boot-starter-validation'`;
    - en la suite `test`: `project(':plataforma:pruebas')`, `project(':plataforma:seguridad')` y `'org.springframework.security:spring-security-test'`.
  - **`ManejadorDeErrores`:** el `@RestControllerAdvice`, que extiende `ResponseEntityExceptionHandler`:
    - `handleExceptionInternal` aplica la correspondencia entre estado y código de `data-model.md`;
    - `handleMethodArgumentNotValid` y `handleHandlerMethodValidationException` responden con `detalles`;
    - `@ExceptionHandler(Exception.class)` busca en el `RegistroDeErrores` y, si no encuentra nada, registra el incidente;
    - vuelve a lanzar `AuthenticationException` y `AccessDeniedException`.
  - **`DescripcionDeValidacion`:** la descripción en español de cada restricción (research §7).
  - **`ControladorDeErrores`:** implementa `ErrorController` en `${server.error.path:/error}`.
  - **`ErroresAutoConfiguracion`:** `@AutoConfiguration` con `beforeName` de la autoconfiguración de errores de Spring Boot 4 (obtener el nombre del JAR, como en B00-H03). Arma el `RegistroDeErrores` con todos los `CatalogoDeErrores` del contexto, el advice y el controlador de errores. Se registra en `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

  JavaDoc del contrato. Así pasa T010.
- [ ] T012 En `backend/plataforma/seguridad/`:
  - en `SeguridadProcesoAutoConfiguracion`, agregar `reglas.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` antes de las demás reglas, con un comentario: la página de error se puede escribir; una petición directa a `/error` sigue pidiendo token;
  - en `SeguridadProcesoTest`, agregar:
    - un caso: `GET /error` directo sin token responde 401;
    - una prueba que compare el cuerpo del 401 con `CodigosComunes.SEGURIDAD_NO_AUTENTICADO`, y otra con `VerificadorDeCatalogo`;
  - agregar `testImplementation project(':plataforma:errores-nucleo')`.
- [ ] T013 En `backend/buildSrc/src/main/groovy/mdd.contrato.gradle`:
  - `useBeanValidation: 'true'`, y quitar el comentario que la dejaba para B00-H04;
  - `implementation 'org.springframework.boot:spring-boot-starter-validation'` y `implementation project(':plataforma:errores-nucleo')`;
  - en la suite `test`: `implementation project(':plataforma:errores')`.

  Regenerar y comprobar que `CredencialesSesion` tiene `@NotNull` y `@Size(min = 1)`.
- [ ] T014 Agregar `implementation project(':plataforma:errores')` a `backend/plantillas/aplicacion/build.gradle` y a `backend/apps/{identidad,datos,distributivo,salida}/build.gradle`. En las pruebas de contrato de `cargas`, `generacion` y `dashboard`, agregar `ErroresAutoConfiguracion` al `@ImportAutoConfiguration`.
- [ ] T015 Ejecutar `./gradlew spotlessApply build integrationTest`. Todo en verde, con el 60 % de cobertura en `plataforma:errores-nucleo` y `plataforma:errores`. Las pruebas de B00-H03 siguen pasando, salvo las de Usuarios que cambian en US1 a US3. Anotar en `specs/004-modelo-errores/evidencia.md`.

**Checkpoint**: el mecanismo funciona con el error de dominio de prueba, y los procesos lo tienen.

---

## Phase 3: User Story 1 - Un error reconocido llega con su código del catálogo (Priority: P1) 🎯 MVP

**Goal**: los errores registrados se traducen con su código del catálogo, y las credenciales inválidas pasan al mecanismo común (escenario «Error reconocido»; FR-004, FR-005; D-38, D-41).

**Independent Test**: `./gradlew :plataforma:errores:test :servicios:usuarios:test` en verde: el error de dominio de prueba y las credenciales inválidas, con su código y su estado.

### Tests for User Story 1 ⚠️

- [ ] T016 [US1] Escribir primero, en `backend/servicios/usuarios/src/test/java/ec/mdd/usuarios/adaptadores/entrada/`:
  - `ErroresUsuariosTest`: `VerificadorDeCatalogo.verificar(new ErroresUsuarios())` pasa, y la entrada de `CredencialesInvalidas` tiene 401;
  - en `SesionContratoTest`, agregar `ErroresAutoConfiguracion` al `@ImportAutoConfiguration`; el caso de credenciales inválidas sigue respondiendo 401 `USUARIOS_CREDENCIALES_INVALIDAS` y pasa la validación del contrato.

### Implementation for User Story 1

- [ ] T017 [US1] Crear `backend/servicios/usuarios/src/main/java/ec/mdd/usuarios/adaptadores/entrada/ErroresUsuarios.java`, con `@Component` e implementando `CatalogoDeErrores`, que registra `CredencialesInvalidas` con los textos de `errores.md`. Borrar el `@ExceptionHandler` de `SesionControlador` y la parte de su JavaDoc que lo explicaba. Así pasa T016.

**Checkpoint**: un error reconocido llega con su código del catálogo, sin traducciones en los controladores.

---

## Phase 4: User Story 2 - Un error técnico inesperado no filtra detalles (Priority: P1)

**Goal**: un fallo técnico responde 500 con `idIncidente` y sin detalles, y el registro guarda la causa saneada (escenario «Error técnico inesperado»; FR-006 a FR-008).

**Independent Test**: `ErrorTecnicoIntegrationTest` de identidad en verde: con la base detenida, 500 con `idIncidente` y el mismo id en el registro.

### Tests for User Story 2 ⚠️

- [ ] T018 [P] [US2] En `SesionContratoTest` de Usuarios, agregar un caso: el doble del caso de uso lanza una excepción técnica con un JWT en el mensaje, y la respuesta es 500 `COMUN_ERROR_INTERNO` con `idIncidente`, sin el JWT ni el nombre de la clase, y pasa la validación del contrato.
- [ ] T019 [US2] Crear `backend/apps/identidad/src/integrationTest/java/ec/mdd/ErrorTecnicoIntegrationTest.java`:
  - `@SpringBootTest(RANDOM_PORT)` con PostgreSQL 18.6 de Testcontainers (`@ServiceConnection`), `spring.datasource.hikari.connection-timeout=2000` y `OutputCaptureExtension`;
  - detiene el contenedor y hace `POST /api/usuarios/sesion` con credenciales completas;
  - comprueba un 500 `COMUN_ERROR_INTERNO` con `idIncidente`, un cuerpo sin «SQL», «Exception», «org.» ni «jdbc», y que el registro tiene el mismo `idIncidente` y la causa de conexión.

### Implementation for User Story 2

- [ ] T020 [US2] Si T018 o T019 encuentran un hueco en el mecanismo (por ejemplo, una excepción de acceso a datos que Spring traduce antes de llegar al advice), corregirlo en `plataforma:errores` con su prueba. Si no hay nada que corregir, anotar en `specs/004-modelo-errores/evidencia.md` que el escenario pasa sin cambios.

**Checkpoint**: un fallo técnico no filtra detalles y se puede rastrear por su `idIncidente`.

---

## Phase 5: User Story 3 - Un error de entrada dice qué campo falló (Priority: P2)

**Goal**: un campo obligatorio vacío responde 400 con `detalles`, también en el inicio de sesión (escenario «Error de entrada»; FR-009; D-42).

**Independent Test**: los casos 400 de `SesionContratoTest` en verde y válidos contra el contrato.

### Tests for User Story 3 ⚠️

- [ ] T021 [US3] En `SesionContratoTest` de Usuarios, agregar dos casos:
  - `{"usuario":""}` responde 400 `COMUN_SOLICITUD_INVALIDA` con 2 `detalles`: `usuario` y `clave`, los dos «es obligatorio»;
  - `{"usuario":"autor.prueba","clave":""}` responde 400 con 1 detalle.

  Las respuestas pasan la validación del contrato.

### Implementation for User Story 3

- [ ] T022 [US3] Actualizar `specs/003-autenticacion-jwt/contracts/api.md` («datos vacíos dan el mismo 401») con una nota de D-42. `IniciarSesionServicio` y su JavaDoc no cambian: el caso de uso conserva su defensa ante nulos y vacíos, y sus pruebas siguen igual, porque la validación del contrato es del adaptador. T021 pasa con la validación de T013.

**Checkpoint**: un error de entrada dice qué campo falló y qué le falta.

---

## Phase 6: Transversal - API Gateway (D-40) y punta a punta

**Purpose**: los errores propios del API Gateway con el modelo común, y la comprobación sobre el perfil levantado (FR-010, FR-012, SC-001, SC-005, SC-006).

- [ ] T023 Escribir primero `backend/gateway/src/test/java/ec/mdd/gateway/ManejadorDeErroresGatewayTest.java`, como `SeguridadGatewayTest`:
  - con un token válido, `GET /api/inexistente/x` responde 404 `COMUN_RECURSO_NO_ENCONTRADO`;
  - con `MDD_URI_SALIDA` apuntando a un puerto cerrado, `GET /api/dashboard/sesion` responde 503 `COMUN_SERVICIO_NO_DISPONIBLE`, sin la dirección ni el puerto;
  - sin token, la misma ruta sigue respondiendo el 401 de ADR-014;
  - ninguna respuesta tiene `timestamp`, `path`, `requestId` ni `error`.

  Una prueba unitaria del manejador cubre el 500 con `idIncidente` ante una excepción cualquiera. Otra compara el cuerpo del 401 de `RespuestaNoAutenticadoReactiva` con `CodigosComunes.SEGURIDAD_NO_AUTENTICADO`.
- [ ] T024 Implementar `backend/gateway/src/main/java/ec/mdd/gateway/ManejadorDeErroresGateway.java`, un `ErrorWebExceptionHandler` con `@Order(-2)`, según research §10. Usa `RespuestaError`, `CodigosComunes`, `Incidentes` y `SaneadorDeTrazas` de `plataforma:errores-nucleo`; escribe el JSON con el `ObjectMapper` del contexto. Agregar `implementation project(':plataforma:errores-nucleo')` a `backend/gateway/build.gradle`. Así pasa T023, con el 60 % de cobertura.
- [ ] T025 Crear `deploy/probar_errores.sh`, ejecutable, con el patrón `verificar`, contra el perfil levantado y a través del API Gateway:
  1. inicio de sesión con `{"usuario":""}`: 400 `COMUN_SOLICITUD_INVALIDA` con 2 `detalles`;
  2. con token, `GET /api/cargas/no-existe`: 404 `COMUN_RECURSO_NO_ENCONTRADO`;
  3. con token, `DELETE /api/cargas/sesion`: 405 `COMUN_METODO_NO_PERMITIDO`;
  4. con `docker compose stop salida`, `GET /api/dashboard/sesion`: 503 `COMUN_SERVICIO_NO_DISPONIBLE`; después, `docker compose start salida` y esperar a que quede sano;
  5. ninguna de estas respuestas tiene `timestamp`, `path`, `trace` ni `requestId`.

  Termina con 0 o 1 y no imprime el token. Validar con shellcheck, sobre una copia.
- [ ] T026 En `.github/workflows/perfil-consolidado.yml`, agregar el paso «Errores de punta a punta» (`deploy/probar_errores.sh`, con `shell: bash`) después de la autenticación y antes de la medición. Validar con actionlint, sobre una copia.
- [ ] T027 Ejecutar `./gradlew spotlessApply build integrationTest` (todo en verde) y `docker compose up -d --wait --build`. Después:
  - `deploy/probar_autenticacion.sh` sigue con los 6 pasos en `OK` (SC-005);
  - `deploy/probar_errores.sh` da todos los pasos en `OK`;
  - el escenario 2 del quickstart §2: con `postgres` detenido, el inicio de sesión responde 500 con `idIncidente`, y ese id aparece en `docker compose logs identidad` con la causa, sin el token ni la contraseña.

  Anotar las respuestas y la línea del registro en `specs/004-modelo-errores/evidencia.md`.

---

## Phase 7: Polish & cierre

- [ ] T028 Revisar el JavaDoc de `plataforma:errores-nucleo`, `plataforma:errores`, `ManejadorDeErroresGateway` y `ErroresUsuarios`. En `backend/plantillas/README.md`, explicar cómo registrar un error de un servicio (contracts/catalogo.md). Ejecutar `spotlessApply` y comprobar con `git status` que no hay archivos temporales. Correr Gitleaks local sobre un clon: `no leaks found`.
- [ ] T029 Hacer commit y push, y abrir el pull request de `004-modelo-errores` hacia `main` **en borrador**, con `Refs #21`. Esperar a que terminen los runs del borrador y recién entonces marcarlo como listo. Comprobar que `2 · test-integracion`, `4 · sbom-backend`, `4 · sca` y `perfil-consolidado` corren y pasan; si Grype marca una dependencia nueva con versión corregida (por ejemplo, de Hibernate Validator), fijarla como en B00-H02 y anotarlo. Pegar en la evidencia la medición de R-19 del run.
- [ ] T030 Completar `specs/004-modelo-errores/evidencia.md` y la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**. Después, ejecutar `perfil-consolidado` sobre `main` y abrir el pull request de evidencia con `Closes #21`, como en B00-H03. Borrar las ramas fusionadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: sin dependencias.
- **Foundational (fase 2)**:
  - los contratos (T003 a T006) van antes que el código;
  - T007 va antes que T008, y T010 antes que T011;
  - T009 depende de T008;
  - T012 a T014 dependen de T011;
  - T015 cierra la fase.
- **US1 (fase 3)**: depende de la fase 2. T016 va antes que T017.
- **US2 (fase 4)**: depende de la fase 2 y de US1 (comparten `SesionContratoTest`). T018 y T019 van antes que T020.
- **US3 (fase 5)**: depende de la fase 2 y de US1. T021 va antes que T022.
- **Transversal (fase 6)**: T023 y T024 dependen de la fase 2; T025 a T027 dependen de US1 a US3 y de T024.
- **Cierre (fase 7)**: depende de todo lo anterior.

### Parallel Opportunities

- Fase 1: T002.
- Fase 2: T003 y T004 (archivos distintos); T009 con T010.
- US2: T018 con T019.
- Fase 6: T023 y T024 se pueden hacer en cuanto termina la fase 2, en paralelo con US1 a US3.
- Como trabajas solo (límite de 1 en «Implementar»), [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (US1)

1. Fases 1 y 2: contratos, núcleo, mecanismo de los procesos y generación con validación.
2. Fase 3: credenciales inválidas por el mecanismo común.
3. **Validar**: `./gradlew :plataforma:errores:test :servicios:usuarios:test`.

### Incremental

1. **US2:** el error técnico con la base detenida.
2. **US3:** el 400 con `detalles` en el inicio de sesión.
3. **Transversal:** el API Gateway y las pruebas de punta a punta.
4. **Cierre:** pull request, evidencia y fusión.

---

## Notes

- Los contratos (T003 a T005) se revisan antes de escribir código: si hay que cambiarlos durante la implementación, se cambia primero el archivo y después el código (principio IV).
- Las respuestas de error que vienen de un proceso pasan por el API Gateway sin cambios; el manejador del API Gateway solo traduce sus propios errores.
- Hacer commit por tarea o por grupo lógico en `004-modelo-errores`, con `Refs #21`.
