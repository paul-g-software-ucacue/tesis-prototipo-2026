---

description: "Tareas de B00-H03: autenticación JWT de punta a punta"
---

# Tasks: Autenticación JWT de punta a punta

**Input**: documentos de diseño en `specs/003-autenticacion-jwt/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas y el principio IV exige pruebas de contrato. En cada grupo, las pruebas se escriben antes que el código que las hace pasar.

**Decisiones**: D-34, D-36, D-37, D-38, D-39, ADR-013, ADR-014 y ADR-015 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**: por historia de usuario de la especificación: US1, sin un token válido no se entra; US2, un usuario autenticado llega a cualquier proceso con sus roles; US3, la memoria después de iniciar sesión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Las rutas son relativas a la raíz del repositorio. Gradle se ejecuta en `backend/` con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`; Docker y Testcontainers, con `sg docker -c '…'` hasta que el autor vuelva a iniciar sesión.

## Convenciones para todas las tareas

- **Capas** (`ReglasCapas`): los adaptadores solo los usa la infraestructura, y ninguna capa usa la infraestructura. Por eso la infraestructura crea los adaptadores con métodos `@Bean` y les pasa tipos que no son de la infraestructura (`JwtEncoder`, `JWKSet`, `Duration`, `String`). Los adaptadores no llevan `@Component` ni leen las clases de propiedades.
- **Paquetes:**
  - `ec.mdd.plataforma.seguridad` y `ec.mdd.plataforma.pruebas`, para que Spring Modulith los vea como un solo módulo `plataforma` en cada proceso;
  - el código generado, en `ec.mdd.<servicio>.adaptadores.entrada.api`.
- **Mensajes del 401** (contracts/autenticacion.md):
  - `SEGURIDAD_NO_AUTENTICADO`: «Sesión no válida» / «Inicia sesión de nuevo para continuar.»;
  - `USUARIOS_CREDENCIALES_INVALIDAS`: «Credenciales inválidas» / «El usuario o la contraseña no son correctos.».
- **Llaves de prueba:** siempre generadas en memoria; nunca un `.pem` en el repositorio (Gitleaks).
- **JavaDoc** en español, sin `@author` (principio XII). **Formato:** `spotlessApply` antes de cada commit (D-33).
- **Evidencia:** cada comprobación va a `specs/003-autenticacion-jwt/evidencia.md`. **Valores:** solo ficticios (principio IX).

---

## Phase 1: Setup

**Purpose**: versiones, módulos nuevos y archivo de evidencia.

- [X] T001 Agregar a `backend/gradle.properties`, cada una con su motivo:
  - `versionBouncyCastle=1.86` («Argon2PasswordEncoder la necesita y no está en el BOM de Spring Boot; research §7»);
  - `versionOpenApiGenerator=7.25.0` («la 7.26.0 tiene una semana; research §11»);
  - `versionValidadorOpenApi=3.0.0` («openapi-request-validator-core, antes swagger-request-validator; research §11»).
- [X] T002 En `backend/buildSrc/build.gradle`, agregar `implementation "org.openapitools:openapi-generator-gradle-plugin:${propiedades.getProperty('versionOpenApiGenerator')}"`, que se resuelve desde `gradlePluginPortal()`, con un comentario «para mdd.contrato (ADR-015)».
- [X] T003 En `backend/settings.gradle`, agregar `include 'plataforma:seguridad', 'plataforma:pruebas'`, después de `plataforma:arquitectura`.
- [X] T004 [P] Crear `specs/003-autenticacion-jwt/evidencia.md` con estas secciones:
  - entorno;
  - contratos;
  - construcción y pruebas;
  - escenario 1 (sin token);
  - escenario 2 (token válido);
  - escenario 3 (token alterado o vencido);
  - escenario 4 (R-19 después de iniciar sesión);
  - búsquedas de secretos;
  - notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

**Purpose**: contratos primero, generación, seguridad compartida de los procesos y contexto de construcción de las imágenes (ADR-014, ADR-015).

**⚠️ CRITICAL**: ninguna historia puede empezar sin esta fase.

### Contratos (principio IV: antes del código)

- [X] T005 [P] Crear `contratos/comun/errores.yaml` (OpenAPI 3.0.3, solo `components`):
  - `Error`, con `titulo`, `descripcion` y `codigo` obligatorios, y `detalles` (lista de `DetalleError`) e `idIncidente` opcionales;
  - `DetalleError`, con `campo` y `descripcion`.

  Cada campo lleva la descripción del principio XIII.
- [X] T006 [P] Crear `contratos/comun/errores.md`, el catálogo de códigos, con una tabla `Código | Estado | Título | Descripción | Cuándo`:
  - `SEGURIDAD_NO_AUTENTICADO` y `USUARIOS_CREDENCIALES_INVALIDAS` (research §5);
  - una nota: «B00-H04 completa el catálogo y no renombra estos códigos (D-38)».
- [X] T007 [P] Crear `contratos/comun/seguridad.yaml` (OpenAPI 3.0.3, solo `components`):
  - `securitySchemes.bearer` (`type: http`, `scheme: bearer`, `bearerFormat: JWT`);
  - el esquema `SesionActual` (`usuario` texto y `roles` lista de textos, los dos obligatorios);
  - la respuesta `NoAutenticado` (401, con el encabezado `WWW-Authenticate` y el cuerpo `$ref: './errores.yaml#/components/schemas/Error'`).
- [X] T008 Crear `contratos/usuarios/openapi.yaml` según [contracts/api.md](contracts/api.md):
  - `security: [bearer: []]` global;
  - `POST /api/usuarios/sesion` (`iniciarSesion`, `security: []`, `CredencialesSesion` → `200 TokenSesion` y `401 Error`);
  - `GET /api/usuarios/sesion` (`consultarSesion` → `200 SesionActual` y `401 NoAutenticado`);
  - `GET /api/usuarios/jwks` (`consultarJwks`, `security: []` → `200 Jwks`).

  Tags `Sesion` y `Jwks`, y `$ref` a `../comun/*.yaml`.
- [X] T009 [P] Crear `contratos/cargas/openapi.yaml`, `contratos/generacion/openapi.yaml` y `contratos/dashboard/openapi.yaml`, cada uno con `GET /api/<servicio>/sesion` (`consultarSesion`, tag `Sesion` → `200 SesionActual` y `401 NoAutenticado`) y `security: [bearer: []]`.
- [X] T010 Validar los 7 archivos de `contratos/` con el CLI de openapi-generator 7.25.0, en contenedor (`openapitools/openapi-generator-cli:v7.25.0 validate -i …`). Montar una **copia** de `contratos/` en el directorio temporal de la sesión, nunca el repositorio con `:z` (memoria del proyecto). Anotar el resultado en la sección «contratos» de `specs/003-autenticacion-jwt/evidencia.md`.

### Generación y módulos de plataforma

- [X] T011 Crear `backend/buildSrc/src/main/groovy/mdd.contrato.gradle`:
  - aplica `org.openapi.generator`;
  - configura `openApiGenerate` con `generatorName 'spring'`, `inputSpec` en `${rootDir}/../contratos/${project.name}/openapi.yaml`, `outputDir` en `build/generated/openapi`, `apiPackage` y `modelPackage` en `ec.mdd.${project.name}.adaptadores.entrada.api`, y las `configOptions` de research §11;
  - agrega `build/generated/openapi/src/main/java` a `sourceSets.main` y hace que `compileJava` dependa de `openApiGenerate`;
  - en `dependencies`: `implementation 'org.springframework:spring-web'` y `'com.fasterxml.jackson.core:jackson-annotations'`;
  - en la suite `test`: `implementation 'org.springframework.boot:spring-boot-starter-webmvc-test'`, `'org.springframework.security:spring-security-test'`, `project(':plataforma:pruebas')` y `project(':plataforma:seguridad')`. Así las pruebas de contrato tienen el 401 con el cuerpo `NoAutenticado`: el `@WebMvcTest` importa `SeguridadProcesoAutoConfiguracion` y usa un `jwk-set-uri` ficticio, que no se llama porque los tokens vienen de `jwt()`;
  - excluye `**/adaptadores/entrada/api/**` de `jacocoTestReport` y `jacocoTestCoverageVerification`.

  Comentario de cabecera: «interfaces generadas desde contratos/ (principio IV, ADR-015)».
- [X] T012 Crear `backend/plataforma/pruebas/` (`build.gradle` con `java-library` y `mdd.java-conventions`), un apoyo solo para pruebas, sin dependencias de servlet ni de WebFlux:
  - `api 'com.nimbusds:nimbus-jose-jwt'`, `api "com.atlassian.oai:openapi-request-validator-core:${versionValidadorOpenApi}"` y `compileOnly 'org.springframework:spring-test'`;
  - `src/main/java/ec/mdd/plataforma/pruebas/TokensDePrueba.java`: genera un par RSA en memoria y firma tokens válidos, vencidos, de otra llave, con otro emisor, HS256 y `alg: none`, más un token alterado (cambia un carácter de la firma);
  - `ServidorJwks.java`: sirve el JWKS de un `TokensDePrueba` con `com.sun.net.httpserver.HttpServer` en un puerto libre;
  - `ValidadorDeContrato.java`: convierte un `MvcResult` en `SimpleRequest` y `SimpleResponse` y lo valida con `OpenApiInteractionValidator` contra `contratos/<servicio>/openapi.yaml`; falla con el informe del validador.

  JavaDoc en las tres clases. Para cumplir el 60 %, una prueba propia de cada clase en `src/test/java`:
  - `TokensDePrueba`: cada tipo de token tiene el algoritmo, el emisor y el vencimiento esperados;
  - `ServidorJwks`: el JWKS servido trae la llave pública y ninguna parte privada;
  - `ValidadorDeContrato`: una respuesta válida pasa y una inválida falla, contra un contrato pequeño de prueba en `src/test/resources`.
- [X] T013 [P] Crear `backend/plataforma/seguridad/build.gradle` (`java-library` y `mdd.java-conventions`):
  - `api 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'` e `implementation 'org.springframework.boot:spring-boot-starter-webmvc'`;
  - en la suite `test`: `project(':plataforma:pruebas')` y `'org.springframework.security:spring-security-test'`.

  Comentario de cabecera: «seguridad servlet compartida por los 4 procesos (ADR-014, research §6)».
- [X] T014 Escribir primero `backend/plataforma/seguridad/src/test/java/ec/mdd/plataforma/seguridad/SeguridadProcesoTest.java`:
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)` con una aplicación de prueba en el mismo paquete, que tiene un controlador `GET /api/prueba/sesion` que devuelve el `sub` y las autoridades;
  - `ServidorJwks` como JWKS, por `@DynamicPropertySource`;
  - comprueba que sin token, encabezado mal formado, token mal formado, alterado, vencido, de otra llave, otro emisor, HS256 y `alg: none` dan 401 con **el mismo cuerpo, byte por byte**, `Content-Type: application/json` y `WWW-Authenticate` igual a `Bearer`;
  - comprueba que un token válido llega con autoridades `ROLE_COORDINADOR_CARRERA` y `ROLE_DOCENTE`, y que `/actuator/health` y una ruta agregada a `mdd.seguridad.rutas-publicas` pasan sin token;
  - con `POST /api/prueba/sesion` como ruta pública, comprueba que `POST` pasa sin token y `GET` a la misma ruta responde 401.

  Al principio no compila: faltan las clases de T015.
- [X] T015 Implementar en `backend/plataforma/seguridad/src/main/java/ec/mdd/plataforma/seguridad/`:
  - `PropiedadesSeguridad`, un `@ConfigurationProperties("mdd.seguridad")` con `emisor` (por defecto `mdd-usuarios`) y `rutasPublicas`. Cada ruta pública tiene el formato `MÉTODO /ruta`, con `*` para cualquier método (por ejemplo, `POST /api/usuarios/sesion`), porque en identidad la misma ruta es pública con `POST` y protegida con `GET`;
  - `RespuestaNoAutenticado`, un `AuthenticationEntryPoint` que escribe el cuerpo constante de contracts/autenticacion.md y `WWW-Authenticate: Bearer`, y registra la causa en DEBUG sin el token;
  - `SeguridadProcesoAutoConfiguracion`, un `@AutoConfiguration` con:
    - `JwtDecoder` (`NimbusJwtDecoder.withJwkSetUri(...).jwsAlgorithm(RS256)` con `JwtValidators.createDefaultWithIssuer(emisor)`, `@ConditionalOnMissingBean`);
    - `JwtAuthenticationConverter`, con el claim `roles` y el prefijo `ROLE_`;
    - un `SecurityFilterChain` *stateless*, sin CSRF, con `permitAll` para `/actuator/health/**` y `rutasPublicas`, `authenticated` para lo demás, y `oauth2ResourceServer` con el punto de entrada propio.

  Registrar la autoconfiguración en `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. JavaDoc del contrato y del porqué del 401 uniforme. Así T014 pasa.
- [X] T016 Contexto de construcción en la raíz del repositorio (ADR-015):
  - crear `.dockerignore` en la raíz, que excluye todo (`*`) y deja pasar `backend/` y `contratos/`, salvo `**/build/` y `**/.gradle/`;
  - borrar `backend/.dockerignore`;
  - en `deploy/docker/java.Dockerfile`: `COPY backend/ /src/backend/` y `COPY contratos/ /src/contratos/`, `WORKDIR /src/backend` y el mismo `RUN` de compilación, con el comentario actualizado;
  - en `compose.yaml`: `context: .` y `dockerfile: deploy/docker/java.Dockerfile` en los 5 componentes Java.
- [X] T017 Agregar `implementation project(':plataforma:seguridad')` a `backend/plantillas/aplicacion/build.gradle` y a `backend/apps/{identidad,datos,distributivo,salida}/build.gradle`. En sus `application.yaml`, agregar `spring.security.oauth2.resourceserver.jwt.jwk-set-uri: ${MDD_JWKS_URI:http://identidad:8080/api/usuarios/jwks}`. Actualizar `backend/plantillas/README.md` (crear un proceso: la seguridad viene de `plataforma:seguridad`).
- [X] T018 Ejecutar `./gradlew spotlessApply build integrationTest`. Todo tiene que quedar en verde, incluidos los `EsquemasIntegrationTest` con la seguridad en el contexto y la cobertura del 60 % en `plataforma:seguridad` y `plataforma:pruebas`. Después, `docker compose up -d --wait --build`, para construir con el contexto nuevo y no reutilizar las imágenes de B00-H02: los 6 contenedores sanos sin token (FR-008, SC-006). Anotar los dos resultados en `specs/003-autenticacion-jwt/evidencia.md`.

**Checkpoint**: los contratos están escritos, el código se genera, los procesos rechazan peticiones sin un token válido y el perfil sigue levantándose con un comando.

---

## Phase 3: User Story 1 - Sin un token válido no se entra (Priority: P1) 🎯 MVP

**Goal**: el API Gateway rechaza con el 401 uniforme toda petición a `/api/**` sin un token válido, y cada proceso hace lo mismo por su cuenta (escenarios «Petición sin token» y «Token alterado o vencido»; FR-003, FR-005, FR-006, FR-008).

**Independent Test**: `./gradlew :gateway:test :plataforma:seguridad:test` en verde: las mismas causas de 401 con el mismo cuerpo en el API Gateway y en los procesos.

### Tests for User Story 1 ⚠️

- [X] T019 [US1] Escribir primero `backend/gateway/src/test/java/ec/mdd/gateway/SeguridadGatewayTest.java`:
  - `@SpringBootTest(webEnvironment = RANDOM_PORT)` y `WebTestClient`;
  - `ServidorJwks` como JWKS;
  - un *backend* falso (`HttpServer` del JDK) como destino de `/api/cargas/**`, que guarda el encabezado `Authorization` recibido.

  Comprueba que:
  - las causas de T014 dan 401 con el mismo cuerpo y `WWW-Authenticate: Bearer`, sin llegar al *backend*;
  - `POST /api/usuarios/sesion` y `/actuator/health` pasan sin token;
  - un token válido llega al *backend* con el mismo `Authorization`.

### Implementation for User Story 1

- [X] T020 [US1] En `backend/gateway/build.gradle`, agregar `implementation 'org.springframework.boot:spring-boot-starter-security-oauth2-resource-server'` y, en la suite `test`, `project(':plataforma:pruebas')` y `'org.springframework.security:spring-security-test'`.
- [X] T021 [US1] Implementar en `backend/gateway/src/main/java/ec/mdd/gateway/`:
  - `RespuestaNoAutenticadoReactiva`, un `ServerAuthenticationEntryPoint` con el mismo cuerpo y encabezado que la versión servlet;
  - `SeguridadGatewayConfig`, con:
    - un `SecurityWebFilterChain` sin CSRF, con `permitAll` para `POST /api/usuarios/sesion` y `/actuator/health/**`, `authenticated` para `/api/**` y `denyAll` para lo demás;
    - un `ReactiveJwtDecoder` (`NimbusReactiveJwtDecoder.withJwkSetUri(...).jwsAlgorithm(RS256)`, con validación del emisor `mdd.seguridad.emisor`).

  JavaDoc del porqué de la doble validación (ADR-014).
- [X] T022 [US1] En `backend/gateway/src/main/resources/application.yaml`:
  - las rutas `spring.cloud.gateway.server.webflux.routes` de research §10, con destinos `${MDD_URI_IDENTIDAD:http://identidad:8080}`, `${MDD_URI_DATOS:http://datos:8080}`, `${MDD_URI_DISTRIBUTIVO:http://distributivo:8080}` y `${MDD_URI_SALIDA:http://salida:8080}`;
  - `jwk-set-uri` igual que los procesos, y `mdd.seguridad.emisor: mdd-usuarios`.

  Ejecutar `./gradlew :gateway:build`: T019 y `GatewayAplicacionTest` tienen que pasar, con el 60 % de cobertura.

**Checkpoint**: sin un token válido no se entra, ni por el API Gateway ni directo a un proceso.

---

## Phase 4: User Story 2 - Un usuario autenticado llega a cualquier proceso con sus roles (Priority: P1)

**Goal**: Usuarios emite el token en un inicio de sesión mínimo (D-37) y publica el JWKS; la «sesión actual» de los 4 procesos devuelve el usuario y sus roles (escenario «Token válido»; FR-001, FR-002, FR-004, FR-007, FR-009, FR-010).

**Independent Test**: `deploy/probar_autenticacion.sh` termina con 0 contra el perfil levantado (quickstart §2).

### Tests for User Story 2 ⚠️

- [X] T023 [P] [US2] Escribir primero las pruebas unitarias de Usuarios en `backend/servicios/usuarios/src/test/java/ec/mdd/usuarios/`:
  - `dominio/UsuarioTest`: un usuario activo puede iniciar sesión y uno inactivo no;
  - `aplicacion/IniciarSesionServicioTest`, con Mockito en los 3 puertos de salida: credenciales correctas emiten un token; usuario inexistente, inactivo o contraseña incorrecta lanzan `CredencialesInvalidas`; con un usuario inexistente, igual se llama al verificador.
- [X] T024 [P] [US2] Escribir primero `backend/servicios/usuarios/src/test/java/ec/mdd/usuarios/adaptadores/salida/`:
  - `VerificadorArgon2Test`: el hash no es la contraseña y la verifica; otra contraseña no;
  - `EmisorJwtTest`, con un `Clock` fijo y un par de llaves de `TokensDePrueba`: RS256, `kid`, `iss`, `sub`, `roles` y `exp = iat + vigencia`; un usuario sin roles da `roles: []`.

  Y `infraestructura/LlavesDeFirmaTest`: sin archivo, genera una llave RSA de 2048 bits; con un PEM escrito por la prueba en un directorio temporal, lo carga; el JWKS no contiene partes privadas.
- [X] T025 [P] [US2] Escribir primero `backend/servicios/usuarios/src/test/java/ec/mdd/usuarios/adaptadores/entrada/SesionContratoTest.java`, con `@WebMvcTest` y `SeguridadProcesoAutoConfiguracion` importada, con las rutas públicas de identidad (T011). Valida con `ValidadorDeContrato` contra `contratos/usuarios/openapi.yaml`:
  - inicio de sesión 200 y 401, con `IniciarSesionDePrueba`: un doble escrito a mano que implementa el puerto `IniciarSesion`, sin Mockito, porque el principio VIII solo admite Mockito en la capa de aplicación;
  - sesión actual 200 (`jwt()` de spring-security-test con roles) y 401;
  - JWKS 200.
- [X] T026 [P] [US2] Escribir primero `backend/servicios/usuarios/src/integrationTest/java/ec/mdd/usuarios/RepositorioUsuariosJpaIntegrationTest.java`, con PostgreSQL 18.6 de Testcontainers y el Flyway de Usuarios: guarda y lee un usuario con sus roles, y `nombre_usuario` es único. `UsuariosEsquemaIntegrationTest` sigue pasando, ahora con las tablas de V2 en el esquema `usuarios`.
- [X] T027 [P] [US2] Escribir primero `SesionContratoTest` en `backend/servicios/{cargas,generacion,dashboard}/src/test/java/ec/mdd/<servicio>/adaptadores/entrada/`, con `@WebMvcTest` y `SeguridadProcesoAutoConfiguracion` importada (T011): sesión actual 200 con `jwt()` (roles ordenados), 200 con un usuario sin roles (`roles: []`) y 401, validados contra el contrato de su servicio.

### Implementation for User Story 2

- [X] T028 [US2] En `backend/servicios/usuarios/build.gradle`, agregar `id 'mdd.contrato'`, `implementation 'org.springframework.boot:spring-boot-starter-data-jpa'`, `'org.springframework.security:spring-security-crypto'`, `'org.springframework.security:spring-security-oauth2-jose'`, `'org.springframework.security:spring-security-oauth2-resource-server'` y `"org.bouncycastle:bcprov-jdk18on:${versionBouncyCastle}"`.

  En `cargas`, `generacion` y `dashboard`, agregar `id 'mdd.contrato'` e `implementation 'org.springframework.security:spring-security-oauth2-resource-server'`.
- [X] T029 [US2] Dominio y puertos de Usuarios:
  - `dominio/Usuario.java`: `nombreUsuario`, `claveHash`, `activo` y `roles` (inmutable), con `puedeIniciarSesion()`;
  - `puertos/entrada/IniciarSesion.java`: `TokenEmitido iniciar(String usuario, String clave)`, con los registros `TokenEmitido(String valor, Instant venceEn)`;
  - `puertos/salida/RepositorioUsuarios.java` (`Optional<Usuario> buscarPorNombre`, `void guardar`), `VerificadorDeClaves.java` (`boolean coincide`, `String calcularHash`) y `EmisorDeTokens.java` (`TokenEmitido emitir(Usuario)`).

  JavaDoc del contrato de cada uno. Así compila T023.
- [X] T030 [US2] Aplicación de Usuarios:
  - `aplicacion/CredencialesInvalidas.java`, error de aplicación sin HTTP y sin decir qué falló;
  - `aplicacion/IniciarSesionServicio.java`, que implementa `IniciarSesion`. Si el usuario no existe, calcula igual un hash con `VerificadorDeClaves`, para que el tiempo de respuesta no revele qué usuarios existen.

  Así pasa T023.
- [X] T031 [US2] Persistencia de Usuarios:
  - `src/main/resources/db/migration/usuarios/V2__usuarios.sql`, con las tablas de data-model.md;
  - `adaptadores/salida/UsuarioJpa.java` (`@Entity`, `@Table(schema = "usuarios", name = "usuario")`, roles con `@ElementCollection` en `usuario_rol`), `UsuarioJpaRepositorio.java` (Spring Data) y `RepositorioUsuariosJpa.java`, que implementa el puerto y traduce a `Usuario`.

  Si Hibernate valida el esquema antes de que corra `flywayUsuarios`, agregar en la infraestructura un `EntityManagerFactoryDependsOnPostProcessor` hacia ese bean. Así pasa T026.

  JaCoCo solo cuenta la suite `test` (`ci.yml`). Por eso, agregar `adaptadores/salida/UsuarioJpaTest`, una prueba unitaria pura de la conversión `UsuarioJpa` ↔ `Usuario`, sin simular el repositorio (principio VIII).
- [X] T032 [US2] Adaptadores de salida de seguridad de Usuarios:
  - `adaptadores/salida/VerificadorArgon2.java`, que recibe un `PasswordEncoder`;
  - `adaptadores/salida/EmisorJwt.java`, que recibe `JwtEncoder`, `Clock`, `Duration vigencia`, `String emisor` y `String kid`.

  Infraestructura:
  - `infraestructura/LlavesDeFirma.java` (research §2): PEM de `mdd.usuarios.llave-privada` o efímera, con aviso en WARN, `kid` y `JWKSet` público;
  - `infraestructura/PropiedadesUsuarios.java` (`mdd.usuarios`: `token.vigencia`, por defecto `PT30M`; `llave-privada`; `usuario-prueba.habilitado` y `usuario-prueba.clave`);
  - `infraestructura/ConfiguracionUsuarios.java`, con los `@Bean` de `Clock`, `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`, `JwtEncoder` (`NimbusJwtEncoder`), `JWKSet` y los adaptadores y el servicio.

  Así pasa T024.
- [X] T033 [US2] Adaptadores de entrada de Usuarios:
  - `adaptadores/entrada/SesionControlador.java`, que implementa la `SesionApi` generada. Inicio de sesión: llama a `IniciarSesion`; con `CredencialesInvalidas`, responde 401 con el `Error` generado y `USUARIOS_CREDENCIALES_INVALIDAS`. Sesión actual: lee el `Jwt` del `SecurityContext` y devuelve el usuario y los roles ordenados.
  - `JwksControlador.java`, que implementa `JwksApi` con el `JWKSet` público.

  Datos vacíos responden como credenciales inválidas (research §5). Así pasa T025.
- [X] T034 [US2] `backend/servicios/usuarios/src/main/java/ec/mdd/usuarios/infraestructura/CargaUsuarioDePrueba.java`: un `ApplicationRunner`, activo solo con `mdd.usuarios.usuario-prueba.habilitado=true`, que crea `autor.prueba`, activo y con `DOCENTE` y `COORDINADOR_CARRERA`, si no existe, con la contraseña de `usuario-prueba.clave` en Argon2id. Si la contraseña está vacía, no crea nada y lo avisa. Nunca registra la contraseña. Prueba en `src/integrationTest/java/ec/mdd/usuarios/CargaUsuarioDePruebaIntegrationTest.java`: crea el usuario una sola vez y no lo duplica. Además, `src/test/java/ec/mdd/usuarios/infraestructura/CargaUsuarioDePruebaTest.java`, con un doble escrito a mano de `RepositorioUsuarios` (sin Mockito): deshabilitado no crea nada, con la contraseña vacía no crea nada, y habilitado crea el usuario una vez. También `ConfiguracionUsuariosTest`, que llama directamente a los métodos `@Bean`.
- [X] T035 [P] [US2] `adaptadores/entrada/SesionControlador.java` en `cargas`, `generacion` y `dashboard`: implementa su `SesionApi` y lee el `Jwt` del `SecurityContext`. Así pasa T027.
- [ ] T036 [US2] Configuración del proceso identidad en `backend/apps/identidad/src/main/resources/application.yaml`:
  - `jwk-set-uri: ${MDD_JWKS_URI:http://localhost:8080/api/usuarios/jwks}`;
  - `mdd.seguridad.rutas-publicas` con `POST /api/usuarios/sesion` y `GET /api/usuarios/jwks` (formato de T015);
  - `mdd.usuarios.usuario-prueba.habilitado: ${MDD_USUARIO_PRUEBA_HABILITADO:false}` y `mdd.usuarios.usuario-prueba.clave: ${MDD_USUARIO_PRUEBA_CLAVE:}`;
  - `mdd.usuarios.llave-privada: ${MDD_LLAVE_PRIVADA:}`;
  - `spring.jpa.hibernate.ddl-auto: validate` y `spring.jpa.open-in-view: false`.
- [ ] T037 [US2] En `.env.ejemplo`, agregar `MDD_USUARIO_PRUEBA_HABILITADO=true` y `MDD_USUARIO_PRUEBA_CLAVE=cambiar-en-local-usuario-prueba`, con un comentario: «usuario de prueba autor.prueba; solo para local (research §8)».
- [ ] T038 [US2] Crear `deploy/probar_autenticacion.sh`, ejecutable, con `set -euo pipefail`, según [contracts/medicion-r19-sesion.md](contracts/medicion-r19-sesion.md):
  - los 6 pasos con `curl` y `jq`, con la contraseña tomada de `.env.ejemplo` y de `.env` si existe;
  - una línea `OK` o `FALLA` por paso, sin imprimir el token ni la contraseña;
  - código de salida 0 o 1.

  Validar con shellcheck, sobre una copia en el directorio temporal.
- [ ] T039 [US2] Ejecutar `./gradlew spotlessApply build integrationTest` (todo en verde, 60 % por módulo). Después, `docker compose build && docker compose up -d --wait` y `deploy/probar_autenticacion.sh`, que tiene que dar los 6 `OK`. Anotar en `specs/003-autenticacion-jwt/evidencia.md` las pruebas por módulo, la cobertura y la salida del script, en las secciones de los escenarios 1 a 3. Revisar en especial la cobertura de `servicios:usuarios` y `plataforma:pruebas` (60 % o más con solo la suite `test`).

**Checkpoint**: la autenticación funciona de punta a punta en los 4 procesos.

---

## Phase 5: User Story 3 - Sigue cabiendo en la memoria después de iniciar sesión (Priority: P2)

**Goal**: medir R-19 después de iniciar sesión y compararlo con B00-H02 (escenario 4; FR-011; D-34, D-36).

**Independent Test**: `deploy/medir_memoria.sh --con-sesion` termina con 0 y deja la tabla «después de iniciar sesión».

### Implementation for User Story 3

- [ ] T040 [US3] En `deploy/medir_memoria.sh`, agregar la opción `--con-sesion`:
  - inicia sesión con el usuario de prueba a través del API Gateway, y si falla sale con 2;
  - suma al calentamiento 20 llamadas a la «sesión actual» de cada proceso;
  - el título pasa a «Medición de R-19 después de iniciar sesión».

  Sin la opción, el comportamiento no cambia. Validar con shellcheck, sobre una copia.
- [ ] T041 [US3] En `.github/workflows/perfil-consolidado.yml`:
  - agregar `contratos/**` al filtro `paths`;
  - después de `up --wait`, agregar el paso «Autenticación de punta a punta» (`deploy/probar_autenticacion.sh`, con `shell: bash`);
  - cambiar la medición a `deploy/medir_memoria.sh --con-sesion`.

  Validar con actionlint 1.7.12, sobre una copia.
- [ ] T042 [US3] Medir en local, `deploy/medir_memoria.sh --con-sesion`, y pegar la tabla en `specs/003-autenticacion-jwt/evidencia.md` con la diferencia respecto de los 931,6 MiB de B00-H02. Si identidad pasa del 85 % de su límite de 512 MiB, anotarlo y proponer al autor un ajuste de límites, que no requiere una decisión nueva (D-36).
- [ ] T043 [US3] Cuando el pull request quede listo para revisión (T046), esperar el run de `perfil-consolidado` y pegar en `specs/003-autenticacion-jwt/evidencia.md` la salida de `probar_autenticacion.sh` y la tabla de la medición (entorno: `ubuntu-latest`), con el enlace al run.

**Checkpoint**: R-19 está medido después de iniciar sesión.

---

## Phase 6: Polish & cierre

- [ ] T044 Búsquedas de secretos (quickstart §4) sobre el perfil levantado, después de `probar_autenticacion.sh`. En `docker compose logs`, 0 apariciones del token obtenido, 0 de la contraseña de prueba y 0 de `PRIVATE KEY`. `git grep -n 'BEGIN .*PRIVATE KEY'` no debe dar resultados. Gitleaks local con `.gitleaks.toml`, sobre un clon en el directorio temporal: `no leaks found`. Anotar en `specs/003-autenticacion-jwt/evidencia.md`.
- [ ] T045 Revisar el JavaDoc (principio XII) de `plataforma/seguridad`, `plataforma/pruebas`, la configuración del API Gateway, y el dominio, los puertos y la infraestructura de Usuarios. Además:
  - en `backend/plantillas/README.md`, explicar cómo agregar un contrato a un servicio (`id 'mdd.contrato'` y `contratos/<servicio>/openapi.yaml`);
  - ejecutar `spotlessApply`;
  - comprobar con `git status` que no hay archivos temporales.
- [ ] T046 Hacer commit y push, y abrir el pull request de `003-autenticacion-jwt` hacia `main` **en borrador**, con la plantilla y `Refs #20`. Esperar a que terminen los runs del borrador y recién entonces marcarlo como listo. Comprobar que `2 · test-integracion`, `4 · sbom-backend` y `4 · sca` corren y pasan, y que `3 · secretos` y `perfil-consolidado` pasan. Si Grype marca una dependencia nueva, como BouncyCastle o Nimbus, con versión corregida, fijar esa versión como en B00-H02 y anotarlo.
- [ ] T047 Completar `specs/003-autenticacion-jwt/evidencia.md` y la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**. Después, ejecutar `perfil-consolidado` sobre `main` y abrir un pull request de evidencia con `Closes #20`, como en B00-H02. Borrar las ramas fusionadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: sin dependencias.
- **Foundational (fase 2)**:
  - depende de la fase 1;
  - los contratos (T005 a T010) van antes que `mdd.contrato` (T011);
  - T014 va antes que T015;
  - T016 y T017 van antes que T018, que cierra la fase.
- **US1 (fase 3)**: depende de la fase 2. T019 va antes que T021 y T022.
- **US2 (fase 4)**:
  - depende de la fase 2; no depende de US1, salvo para la prueba de punta a punta (T039), que pasa por el API Gateway de US1;
  - cada prueba (T023 a T027) va antes que su implementación (T029 a T035).
- **US3 (fase 5)**: depende de US2, porque necesita el inicio de sesión. T043 depende de T046.
- **Cierre (fase 6)**: depende de US1, US2 y US3 (salvo T043, que se completa entre T046 y T047).

### Parallel Opportunities

- Fase 1: T004.
- Fase 2: T005, T006, T007 y T009 (contratos en archivos distintos); T013 con T012.
- US2: las pruebas T023 a T027; T035 con T029 a T034.
- Como trabajas solo (límite de 1 en «Implementar»), [P] indica orden libre, no trabajo simultáneo.

---

## Parallel Example: User Story 2

```bash
Task: "UsuarioTest e IniciarSesionServicioTest en backend/servicios/usuarios/src/test/java/ec/mdd/usuarios/"
Task: "VerificadorArgon2Test, EmisorJwtTest y LlavesDeFirmaTest en backend/servicios/usuarios/src/test/java/ec/mdd/usuarios/"
Task: "SesionContratoTest en backend/servicios/{cargas,generacion,dashboard}/src/test/java/"
```

---

## Implementation Strategy

### MVP (US1)

1. Fases 1 y 2: contratos, generación, seguridad de los procesos y perfil sano.
2. Fase 3: el API Gateway también rechaza sin un token válido.
3. **Validar**: `./gradlew :gateway:test :plataforma:seguridad:test`.

### Incremental

1. **US2:** el inicio de sesión de Usuarios y la «sesión actual» de los 4 procesos; prueba de punta a punta.
2. **US3:** la medición de R-19 después de iniciar sesión, en local y en el runner.
3. **Cierre:** búsquedas de secretos, pull request, evidencia y fusión.

---

## Notes

- Los contratos (T005 a T009) se revisan antes de escribir código: si hay que cambiar un contrato durante la implementación, se cambia primero el archivo y después el código (principio IV).
- Las llaves de las pruebas se generan siempre en memoria (`TokensDePrueba`); el archivo PEM de T024 se escribe en un directorio temporal de la prueba.
- Para las herramientas en contenedor (CLI de openapi-generator, shellcheck, actionlint y Gitleaks), copiar los archivos al directorio temporal de la sesión y montar esa copia, nunca el repositorio con `:z`.
- Hacer commit por tarea o por grupo lógico en `003-autenticacion-jwt`, con `Refs #20`.
