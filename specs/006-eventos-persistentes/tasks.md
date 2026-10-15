---

description: "Tareas de B00-H05: eventos persistentes entre procesos"
---

# Tasks: Eventos persistentes entre procesos

**Input**: documentos de diseño en `specs/006-eventos-persistentes/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas y el principio IV exige pruebas de contrato. En cada grupo, las pruebas se escriben antes que el código que las hace pasar.

**Decisiones**: D-47, D-48, D-49, ADR-014, ADR-016 y ADR-018 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**: por historia de usuario de la especificación:
- **US1:** entrega normal;
- **US2:** receptor caído y reenvío;
- **US3:** entrega repetida.

La seguridad del receptor a través del API Gateway y el cierre de punta a punta van en una fase transversal.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Las rutas son relativas a la raíz del repositorio. Gradle se ejecuta en `backend/` con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`; Docker y Testcontainers, con `sg docker -c '…'` mientras haga falta.

## Convenciones para todas las tareas

- **Paquete del módulo nuevo:** `ec.mdd.plataforma.eventos` (`plataforma:eventos`).
- **Capas:**
  - Generación publica por su puerto `puertos.salida.PublicadorDeEventos`; el adaptador `adaptadores.salida.PublicadorDeEventosSpring` usa `ApplicationEventPublisher`. Ni el dominio ni la aplicación conocen HTTP ni `plataforma:eventos`.
  - El receptor de Usuarios sigue el patrón de `SesionControlador`: el controlador implementa la interfaz generada y llama a un puerto de entrada.
- **Pruebas de contrato:** con `@WebMvcTest`, `@ImportAutoConfiguration({SeguridadProcesoAutoConfiguracion.class, ErroresAutoConfiguracion.class})`, `ValidadorDeContrato` y dobles escritos a mano. Mockito solo en la capa de aplicación (principio VIII).
- **Receptor simulado:** en las pruebas de `plataforma:eventos`, el servidor HTTP del JDK (`com.sun.net.httpserver.HttpServer`) en un puerto libre, que se puede detener y volver a levantar.
- **Cobertura:** JaCoCo cuenta solo la suite `test`. Lo que solo ejercitan las pruebas de integración necesita además una prueba unitaria pura.
- **Registro:** nunca escribe el `contenido` del evento, la clave del proceso ni un token. Cada prueba de registro lo comprueba con `OutputCaptureExtension`.
- **Herramientas en contenedor** (CLI de openapi-generator, shellcheck, actionlint y Gitleaks): sobre una copia en el directorio temporal de la sesión, nunca montando el repositorio con `:z`.
- **Scripts de Bash:**
  - con `set -euo pipefail`, las comprobaciones van dentro de un `if` (patrón `verificar` de `deploy/probar_errores.sh`);
  - las directivas de shellcheck para todo el archivo van antes del primer comando.
- **JavaDoc** en español, sin `@author`. **Formato:** `spotlessApply` antes de cada commit. **Evidencia:** en `specs/006-eventos-persistentes/evidencia.md`.

---

## Phase 1: Setup

- [X] T001 En `backend/settings.gradle`, agregar `'plataforma:eventos'` a la línea de `include` de `plataforma`.
- [X] T002 [P] Crear `specs/006-eventos-persistentes/evidencia.md` con estas secciones:
  - entorno;
  - contratos;
  - construcción y pruebas;
  - escenario 1 (entrega normal);
  - escenario 2 (receptor caído);
  - escenario 3 (entrega repetida);
  - seguridad;
  - punta a punta;
  - memoria (R-19);
  - notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

**Purpose**: contratos primero, esquemas, separación de tokens, el token de proceso y el núcleo de `plataforma:eventos` (ADR-018).

**⚠️ CRITICAL**: ninguna historia puede empezar sin esta fase.

### Contratos (principio IV: antes del código)

- [X] T003 [P] Crear `contratos/comun/eventos.yaml` con el esquema `EventoEntreProcesos` de data-model.md. Los cinco campos son obligatorios:
  - `idEvento`: `format: uuid`;
  - `tipo`: `pattern: '^[a-z]+\.[A-Z][A-Za-z]+$'`;
  - `origen`: `pattern: '^[a-z]+$'`;
  - `momento`: `format: date-time`;
  - `contenido`: `type: object`, con `additionalProperties: true`.

  Con `additionalProperties: false` en el sobre y la descripción de cada campo. La de `contenido` dice «sin contraseñas ni tokens».
- [X] T004 [P] En `contratos/comun/errores.md`, agregar la fila `COMUN_EVENTO_NO_RECONOCIDO` (400, «Evento no reconocido», «El receptor no admite este tipo de evento.») con el «cuándo» de contracts/entrega.md.
- [X] T005 En `contratos/usuarios/openapi.yaml`, agregar los tags `TokenProceso` y `Eventos`, uno por operación, y dos operaciones según contracts/entrega.md. Van con tags distintos porque, con `useTags` y `skipDefaultInterface`, cada tag genera una interfaz con todos sus métodos abstractos, y cada operación tiene su propio controlador:
  - `POST /interno/usuarios/token-proceso`:
    - tag `TokenProceso`, `operationId: emitirTokenDeProceso`, `security: []`;
    - cuerpo `CredencialesProceso`: `proceso` con `pattern: '^[a-z]+$'` y `clave` con `format: password`, los dos obligatorios y con `minLength: 1`;
    - respuestas 200 `TokenSesion`, 401 `Error` (`USUARIOS_CREDENCIALES_INVALIDAS`), 400 `SolicitudInvalida`, 500 y 503.
  - `POST /interno/usuarios/eventos`:
    - tag `Eventos`, `operationId: recibirEvento`, con `bearer`;
    - cuerpo `$ref: '../comun/eventos.yaml#/components/schemas/EventoEntreProcesos'`;
    - respuestas 204, 400 (`SolicitudInvalida` y la descripción de `COMUN_EVENTO_NO_RECONOCIDO`), 401 `NoAutenticado`, 403 sin cuerpo («hasta F05-H02»), 500 y 503.

  Actualizar el comentario de cabecera del contrato con D-47 y D-49.
- [X] T006 [P] En `contratos/generacion/openapi.yaml`, agregar el tag `EventosPrueba` y `POST /api/generacion/eventos-prueba`:
  - `operationId: publicarEventoDePrueba`, sin cuerpo;
  - respuestas 202 con `EventoPublicado` (`idEvento`, `format: uuid`, obligatorio), 401 `NoAutenticado`, 404 (`Error`, «apagado por defecto», D-48), 500 y 503.
- [X] T007 Validar los 8 archivos de `contratos/` con `openapitools/openapi-generator-cli:v7.25.0 validate --recommend`, sobre una copia en el directorio temporal. Anotar el resultado en la sección «contratos» de `specs/006-eventos-persistentes/evidencia.md`.

### Esquemas y configuración del entorno

- [X] T008 En `deploy/postgres/01-esquemas.sh`, agregar en cada bloque de proceso `CREATE SCHEMA eventos_<proceso> AUTHORIZATION <proceso>;` (`eventos_identidad`, `eventos_datos`, `eventos_distributivo` y `eventos_salida`). El comentario dice: «registro de publicaciones del proceso (ADR-018)». Actualizar el comentario de cabecera: hace falta `docker compose down -v` para aplicarlo en un volumen existente.
- [X] T009 [P] En `.env.ejemplo`, agregar con su comentario:
  - `MDD_CLAVE_PROCESO_DISTRIBUTIVO=cambiar-en-local-proceso-distributivo`, para el token de proceso (D-49, ADR-018);
  - `MDD_EVENTOS_PRUEBA_HABILITADO=true`, para el endpoint de prueba, solo en local y en la CI (D-48).

  Comprobar con Gitleaks local, sobre un clon, que no hay hallazgos.
- [X] T010 [P] En `backend/plataforma/errores-nucleo/`, agregar `COMUN_EVENTO_NO_RECONOCIDO` a `CodigosComunes` y a `TODOS`. `CodigosComunesTest` (con `VerificadorDeCatalogo`) sigue pasando con la fila de T004.

### Separación de tokens (`plataforma:seguridad`)

- [X] T011 Escribir primero, en `backend/plataforma/seguridad/src/test/java/ec/mdd/plataforma/seguridad/SeguridadProcesoTest.java`, con dos rutas de prueba en `AplicacionDePrueba` (`POST /interno/prueba/eventos` y `GET /api/prueba/recurso`) y `TokensDePrueba`, la tabla de contracts/entrega.md («Separación de tokens»):
  - token con `roles: [PROCESO]` en `/interno/**`: 2xx;
  - token de usuario en `/interno/**`: 403;
  - token de proceso en `/api/prueba/recurso`: 403;
  - token de usuario en `/api/prueba/recurso`: 2xx;
  - sin token o con un token vencido en `/interno/**`: el 401 uniforme (`SEGURIDAD_NO_AUTENTICADO`).
- [X] T012 En `SeguridadProcesoAutoConfiguracion`, después de las reglas de las rutas públicas:
  - `reglas.requestMatchers("/interno/**").hasRole("PROCESO")`;
  - `reglas.anyRequest()` con un `AuthorizationManager` que exige estar autenticado y no tener `ROLE_PROCESO`.

  Las rutas públicas siguen yendo antes, así que `POST /interno/usuarios/token-proceso` queda abierta si el proceso la lista. Actualizar el JavaDoc de la clase con ADR-018. Así pasa T011.

### Token de proceso (Usuarios, D-49)

- [X] T013 Escribir primero, en `backend/servicios/usuarios/src/test/java/ec/mdd/usuarios/`:
  - `aplicacion/EmitirTokenDeProcesoServicioTest`:
    - con la clave correcta emite, con `sub` = `proceso:distributivo` y `roles` = `[PROCESO]`;
    - con una clave incorrecta, o con un proceso sin clave configurada, lanza `CredencialesInvalidas`;
    - la comparación no depende de la longitud de la clave recibida (las dos claves pasan por SHA-256 antes de `MessageDigest.isEqual`);
  - `adaptadores/salida/EmisorJwtTest`: agregar un caso: el token de proceso dura `mdd.usuarios.token-proceso.vigencia` (5 min por defecto) y lleva el `kid`;
  - `adaptadores/entrada/TokenProcesoContratoTest`: 200 con `TokenSesion`, 401 `USUARIOS_CREDENCIALES_INVALIDAS` (igual con una clave incorrecta y con un proceso desconocido) y 400 con `detalles` con los campos vacíos. Todas las respuestas pasan `ValidadorDeContrato`, y el registro no tiene la clave ni el token;
  - `infraestructura/ConfiguracionUsuariosTest`: `mdd.usuarios.procesos.<proceso>.clave` vacía no habilita el proceso.
- [X] T014 Implementar en Usuarios:
  - **el puerto de entrada** `puertos/entrada/EmitirTokenDeProceso` (`TokenEmitido emitir(String proceso, String clave)`);
  - **el caso de uso** `aplicacion/EmitirTokenDeProcesoServicio`;
  - **la configuración:** en `PropiedadesUsuarios`, `procesos` (mapa de nombre a clave) y `tokenProceso.vigencia` (`PT5M`);
  - **el emisor:** en `puertos/salida/EmisorDeTokens` y en `EmisorJwt`, `emitirParaProceso(String proceso)` con la vigencia propia;
  - **el controlador** `adaptadores/entrada/TokenProcesoControlador`, que implementa la interfaz generada `TokenProcesoApi`;
  - **el cableado** en `ConfiguracionUsuarios`.

  En `backend/apps/identidad/src/main/resources/application.yaml`:
  - agregar `POST /interno/usuarios/token-proceso` a `mdd.seguridad.rutas-publicas`;
  - agregar `mdd.usuarios.procesos.distributivo.clave: ${MDD_CLAVE_PROCESO_DISTRIBUTIVO:}`.

  Así pasa T013.

### Núcleo de `plataforma:eventos`

- [X] T015 Escribir primero, en `backend/plataforma/eventos/src/test/java/ec/mdd/plataforma/eventos/`:
  - `EventoEntreProcesosTest`:
    - `nuevo(tipo, origen, destino, contenido, reloj)` genera un UUID v4 y valida los patrones de `tipo` y `origen`;
    - el sobre serializado con el `JsonMapper` del registro y leído de vuelta conserva todos sus campos, incluido `destino` (lo necesita cada reenvío);
    - el cuerpo HTTP que arma `ClienteDeEventos` no tiene `destino` y valida contra `contratos/comun/eventos.yaml` (con `ValidadorDeContrato` o con el esquema).
  - `ProveedorDeTokenDeProcesoTest`, contra el servidor HTTP del JDK:
    - pide el token una vez y lo reutiliza;
    - lo renueva cuando le queda menos de 1 minuto;
    - `descartar()` obliga a pedir otro;
    - una respuesta distinta de 200 lanza `EntregaFallida`;
    - el registro no tiene la clave ni el token.
  - `ClienteDeEventosTest`, contra el servidor HTTP del JDK:
    - con 204, termina;
    - con 400, 500, un plazo vencido (el servidor no responde), conexión rechazada o host desconocido, lanza `EntregaFallida`;
    - con 401, descarta el token y reintenta una vez: si el reintento da 204, termina; si da 401, lanza `EntregaFallida`;
    - el cuerpo enviado es el sobre sin `destino`, con `Authorization: Bearer`;
    - un destino sin URL configurada lanza `EntregaFallida`;
    - con `mdd.eventos.clave` vacía no pide el token: lanza `EntregaFallida` y registra un WARN «sin clave de proceso».
  - `PropiedadesEventosTest`: el esquema `eventos_distributivo` pasa; `public`, `generacion` o vacío fallan al arrancar con un mensaje claro.
- [X] T016 Implementar `backend/plataforma/eventos/`:
  - **`build.gradle`:** `java-library` y `mdd.java-conventions`, más:
    - `api 'org.springframework.modulith:spring-modulith-events-api'`;
    - `implementation 'org.springframework.modulith:spring-modulith-events-jdbc'` y `'org.springframework.modulith:spring-modulith-events-jackson'`, sin el *starter* (research §1);
    - `implementation 'org.springframework:spring-web'`, `'org.springframework.boot:spring-boot-starter-jdbc'`, `'org.springframework.boot:spring-boot-starter-flyway'` y `runtimeOnly 'org.flywaydb:flyway-database-postgresql'`;
    - en la suite `test`: `project(':plataforma:pruebas')`;
    - excluir `org.slf4j:jcl-over-slf4j`, que trae `spring-modulith-events-core` y repite las clases de `commons-logging` que usa Spring 7. Comprobarlo con `./gradlew :plataforma:eventos:dependencies --configuration runtimeClasspath` y anotarlo en la evidencia (research §1).
  - **`EventoEntreProcesos`:** record con `idEvento`, `tipo`, `origen`, `destino`, `momento` y `contenido` (`Map<String, Object>`), sin `@JsonIgnore`: el registro guarda el sobre completo. `ClienteDeEventos` envía un cuerpo aparte (`CuerpoEvento`, sin `destino`), con la forma de `comun/eventos.yaml`.
  - **`PropiedadesEventos`:** `mdd.eventos` con `plazo` (`PT5S`), `reenvio.intervalo` (`PT30S`), `destinos`, `tokenProceso` (URL), `proceso` y `clave`. Valida el nombre del esquema de `spring.modulith.events.jdbc.schema` con `^eventos_[a-z]+$`.
  - **`EntregaFallida`:** la excepción de una entrega que no se confirmó.
  - **`ProveedorDeTokenDeProceso`** y **`ClienteDeEventos`:** `RestClient` sobre `JdkClientHttpRequestFactory`, con los plazos de conexión y de lectura.
  - **La migración** `src/main/resources/db/migration/eventos/V1__registro_de_publicaciones.sql`: el DDL v2 de PostgreSQL de Spring Modulith 2.1.1 (`event_publication` y sus dos índices), con un comentario de su origen.

  Así pasa T015.
- [X] T017 Agregar a `plataforma:eventos` la autoconfiguración `EventosAutoConfiguracion`, registrada en `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, con:
  - el bean `flywayEventos` (`@Bean(initMethod = "migrate")`), limitado al esquema `spring.modulith.events.jdbc.schema` y a `db/migration/eventos`, como `UsuariosFlywayConfig`;
  - `PropiedadesEventos`, `ProveedorDeTokenDeProceso` y `ClienteDeEventos`.

  Escribir primero `EventosAutoConfiguracionTest` con `ApplicationContextRunner`: arma los beans con la configuración mínima, también con la clave vacía (el proceso arranca y las entregas fallan, data-model.md), y falla sin el esquema o con uno inválido.

**Checkpoint**: los contratos están validados, los tokens separados, Usuarios emite tokens de proceso y el núcleo del emisor está probado contra un receptor simulado.

---

## Phase 3: User Story 1 - Un evento publicado en una transacción llega una vez al otro proceso (Priority: P1) 🎯 MVP

**Goal**: un evento de prueba publicado en la transacción de Generación llega a Usuarios, que lo registra, y la publicación queda `COMPLETED`. Una transacción revertida no deja publicación (escenario «Entrega normal»; FR-001 a FR-004, FR-009, FR-010; SC-001, SC-005).

**Independent Test**: `./gradlew :plataforma:eventos:integrationTest :servicios:usuarios:test :servicios:generacion:test` en verde, y el paso 1 de `deploy/probar_eventos.sh` en `OK`.

### Tests for User Story 1 ⚠️

- [ ] T018 [P] [US1] Escribir primero, en Usuarios:
  - `aplicacion/RegistrarEventoRecibidoServicioTest`:
    - un evento `generacion.EventoDePrueba` nuevo se registra una vez y aplica su manejador;
    - un tipo sin manejador lanza `EventoNoReconocido` y no registra nada;
  - `adaptadores/entrada/EventosContratoTest`:
    - 204 con un token `PROCESO`;
    - 400 `COMUN_EVENTO_NO_RECONOCIDO` con un tipo desconocido;
    - 400 `COMUN_SOLICITUD_INVALIDA` con un `idEvento` que no es UUID o sin `tipo`;
    - 401 sin token;
    - 403 con un token de usuario.

    Las respuestas con cuerpo pasan `ValidadorDeContrato`.
  - `ErroresUsuariosTest`: `EventoNoReconocido` tiene el código `COMUN_EVENTO_NO_RECONOCIDO` y 400.
- [ ] T019 [P] [US1] Escribir primero, en Generación:
  - `aplicacion/PublicarEventoDePruebaServicioTest`: guarda la fila y publica un sobre con `tipo` = `generacion.EventoDePrueba`, `origen` = `generacion`, `destino` = `usuarios`, el mismo `idEvento` y el contenido ficticio de data-model.md;
  - `adaptadores/entrada/EventosPruebaContratoTest`:
    - con `mdd.generacion.eventos-prueba.habilitado=true`: 202 con `idEvento`, y 401 sin token;
    - sin la propiedad: 404 `COMUN_RECURSO_NO_ENCONTRADO`.

    Las respuestas pasan `ValidadorDeContrato`.
- [ ] T020 [US1] Escribir primero `backend/plataforma/eventos/src/integrationTest/java/ec/mdd/plataforma/eventos/EntregaIntegrationTest.java`:
  - `@SpringBootTest` con una aplicación de prueba, PostgreSQL 18.6 de Testcontainers con el esquema `eventos_prueba` creado al iniciar el contenedor, y el receptor simulado, que atiende los dos endpoints: el del token de proceso y el de los eventos;
  - un servicio de prueba transaccional inserta una fila y publica un `EventoEntreProcesos`;
  - comprueba que:
    - el receptor recibe el sobre una vez, con el token;
    - la publicación en `eventos_prueba.event_publication` queda `COMPLETED` en menos de 10 s (Awaitility o un sondeo con plazo);
    - si el servicio lanza una excepción después de publicar, no queda ni la fila ni la publicación, y el receptor no recibe nada (SC-005);
    - el registro no tiene el contenido.

### Implementation for User Story 1

- [ ] T021 [US1] Implementar en `plataforma:eventos` el `OyenteEntreProcesos` (`@ApplicationModuleListener void entregar(EventoEntreProcesos)`), que llama a `ClienteDeEventos`. Ante `EntregaFallida`, registra WARN con `idEvento`, `tipo`, `destino` y la causa resumida, y vuelve a lanzar la excepción para que la publicación quede `FAILED`. Agregarlo a `EventosAutoConfiguracion` y documentar en su JavaDoc por qué es asíncrono y transaccional. Así pasa T020.
- [ ] T022 [US1] Implementar el receptor en Usuarios:
  - **la migración** `src/main/resources/db/migration/usuarios/V3__evento_recibido.sql`, con la tabla de data-model.md y un comentario: «nadie la modifica ni la borra»;
  - **el puerto de entrada** `puertos/entrada/RegistrarEventoRecibido`, con el record `EventoRecibido`;
  - **el puerto de salida** `puertos/salida/RepositorioEventosRecibidos` (`boolean registrarSiEsNuevo(EventoRecibido)`);
  - **el caso de uso** `aplicacion/RegistrarEventoRecibidoServicio`, transaccional, con el manejador de `generacion.EventoDePrueba` (sin efecto, D-47);
  - **la excepción** `aplicacion/EventoNoReconocido`;
  - **el repositorio** `adaptadores/salida/RepositorioEventosRecibidosJdbc`, con `JdbcClient` e `INSERT … ON CONFLICT (id_evento) DO NOTHING`, que devuelve si insertó;
  - **el controlador** `adaptadores/entrada/EventosControlador`, que implementa `EventosApi#recibirEvento` y responde 204;
  - **el catálogo:** `EventoNoReconocido` en `ErroresUsuarios`, con la definición de `CodigosComunes`;
  - **el cableado** en `ConfiguracionUsuarios`.

  Así pasa T018. En `UsuariosEsquemaIntegrationTest`, comprobar que existe `usuarios.evento_recibido` con su clave primaria.
- [ ] T023 [US1] Implementar el evento de prueba en Generación:
  - **la migración** `src/main/resources/db/migration/generacion/V2__evento_prueba.sql`;
  - **el puerto de entrada** `puertos/entrada/PublicarEventoDePrueba`;
  - **los puertos de salida** `puertos/salida/RepositorioEventosDePrueba` y `PublicadorDeEventos`;
  - **el caso de uso** `aplicacion/PublicarEventoDePruebaServicio`, transaccional;
  - **los adaptadores** `adaptadores/salida/RepositorioEventosDePruebaJdbc` (`JdbcClient`) y `PublicadorDeEventosSpring`;
  - **el controlador** `adaptadores/entrada/EventosPruebaControlador`, con `@ConditionalOnProperty(name = "mdd.generacion.eventos-prueba.habilitado", havingValue = "true")`;
  - **el cableado** en una configuración de `infraestructura`.

  En `backend/servicios/generacion/build.gradle`:
  - `implementation project(':plataforma:eventos')`, para el sobre;
  - `implementation 'org.springframework.boot:spring-boot-starter-jdbc'`.

  Así pasa T019. En `GeneracionEsquemaIntegrationTest`, comprobar que existe `generacion.evento_prueba`.
- [ ] T024 [US1] Ensamblar en distributivo:
  - **`backend/apps/distributivo/build.gradle`:** `implementation project(':plataforma:eventos')`;
  - **`application.yaml`:** las propiedades de data-model.md («Configuración nueva»):
    - `spring.modulith.events.jdbc.schema`;
    - `mdd.eventos.*`, con las URL a partir de `${MDD_URI_IDENTIDAD:http://identidad:8080}`;
    - `mdd.eventos.clave: ${MDD_CLAVE_PROCESO_DISTRIBUTIVO:}`;
    - `mdd.generacion.eventos-prueba.habilitado: ${MDD_EVENTOS_PRUEBA_HABILITADO:false}`;
  - **las pruebas del proceso:**
    - `ModulosTest` sigue pasando;
    - en `EsquemasIntegrationTest`, comprobar que existe `eventos_distributivo.event_publication` (con el superusuario de Testcontainers, `flywayEventos` crea el esquema solo);
  - **el arranque:** que no consulta `event_publication` antes de la migración (research §2, riesgo comprobado).
- [ ] T025 [US1] Crear `deploy/probar_eventos.sh`, ejecutable, con el patrón `verificar` de `deploy/probar_errores.sh`, con los pasos 0 y 1 de contracts/demostracion.md:
  - inicia sesión;
  - publica;
  - con `docker compose exec -T postgres psql -U postgres -d mdd -tAc`, comprueba en menos de 10 s 1 fila en `usuarios.evento_recibido` y la publicación `COMPLETED`.

  Una función del script, que usan todos los pasos, busca la publicación de un `idEvento` en `eventos_distributivo.event_publication` por `serialized_event LIKE '%<idEvento>%'`.

  El `idEvento` se valida con una expresión regular de UUID antes de usarlo en el SQL. Validar con shellcheck, sobre una copia.

**Checkpoint**: un evento publicado llega una vez y su publicación queda completada.

---

## Phase 4: User Story 2 - Con el receptor caído, la publicación espera y se reenvía (Priority: P1)

**Goal**: con identidad detenida, la transacción se confirma y la publicación queda `FAILED`. Una tarea programada la reenvía cuando identidad vuelve, también después de un reinicio de identidad con una llave nueva y de un emisor caído a mitad de una entrega (escenario «Receptor caído»; FR-004 a FR-006; SC-002, SC-003).

**Independent Test**: `ReenvioIntegrationTest` en verde, y los pasos 2 y 3 de `deploy/probar_eventos.sh` en `OK`.

### Tests for User Story 2 ⚠️

- [X] T026 [P] [US2] Escribir primero `backend/plataforma/eventos/src/test/java/ec/mdd/plataforma/eventos/ReenvioDePublicacionesTest`. Con un `FailedEventPublications` escrito a mano, comprueba que:
  - cada ejecución llama a `resubmit` con las opciones por defecto;
  - el filtro registra en INFO cada publicación con `idEvento` y `tipo`, sin el contenido.
- [ ] T027 [US2] Escribir primero `backend/plataforma/eventos/src/integrationTest/java/ec/mdd/plataforma/eventos/ReenvioIntegrationTest.java`, con la misma base de T020, `mdd.eventos.reenvio.intervalo=PT2S` y la vigilancia de atascadas en pocos segundos. Comprueba que:
  - **receptor detenido:**
    - la transacción se confirma (la fila existe);
    - la publicación queda `FAILED`;
    - el registro tiene el WARN con el `idEvento`;
  - **receptor de vuelta:** en menos de 2 ciclos la publicación queda `COMPLETED` y el receptor la recibió;
  - **varias pendientes:** 3 publicaciones con el receptor detenido quedan todas `COMPLETED` cuando vuelve;
  - **atascadas:** una publicación `PROCESSING` vieja (el emisor se cayó a mitad de la entrega) y una `PUBLISHED` vieja (se cayó justo después de confirmar, antes de que corriera el oyente), insertadas a mano, pasan a `FAILED` y después se completan (FR-005);
  - **token rechazado:** si el receptor responde 401 al token guardado y 204 al nuevo, la entrega se completa en el mismo intento;
  - **Usuarios sin poder emitir el token:** si el endpoint del token responde 503, la publicación queda `FAILED`, y se completa cuando vuelve a responder (FR-011).

### Implementation for User Story 2

- [ ] T028 [US2] Implementar `ReenvioDePublicaciones` en `plataforma:eventos`: `@Scheduled(fixedDelayString = "${mdd.eventos.reenvio.intervalo:PT30S}")`, que llama a `FailedEventPublications.resubmit(ResubmissionOptions.defaults().withFilter(…))`. Agregar `@EnableScheduling` a `EventosAutoConfiguracion`. Así pasan T026 y T027.
- [ ] T029 [US2] En `backend/apps/distributivo/src/main/resources/application.yaml`, agregar con su comentario:
  - `spring.modulith.events.staleness.published`, `.processing` y `.resubmitted`, en `PT2M`;
  - `spring.modulith.events.republish-outstanding-events-on-restart: false`;
  - `spring.modulith.events.completion-mode: update`.
- [ ] T030 [US2] Extender `deploy/probar_eventos.sh` con los pasos 2 y 3 de contracts/demostracion.md:
  - `docker compose stop identidad` y publicar con el token del paso 0, que el API Gateway y distributivo validan con el JWKS en caché (contracts/demostracion.md); si responde 401, el script lo dice;
  - comprobar la fila en `generacion.evento_prueba` y la publicación no `COMPLETED`;
  - `docker compose start identidad` y esperar a que esté sano;
  - en menos de 75 s, comprobar 1 fila en `evento_recibido` y la publicación `COMPLETED`.

  Validar con shellcheck, sobre una copia.

**Checkpoint**: ninguna publicación se pierde con el receptor caído.

---

## Phase 5: User Story 3 - Un evento repetido no se registra dos veces (Priority: P2)

**Goal**: el receptor registra cada `idEvento` una sola vez, con entregas seguidas o simultáneas, y confirma todas (escenario «Entrega repetida»; FR-007; SC-004).

**Independent Test**: `RepositorioEventosRecibidosJdbcIntegrationTest` en verde, y el paso 4 de `deploy/probar_eventos.sh` en `OK`.

### Tests for User Story 3 ⚠️

- [ ] T031 [P] [US3] Escribir primero `backend/servicios/usuarios/src/integrationTest/java/ec/mdd/usuarios/RepositorioEventosRecibidosJdbcIntegrationTest.java`, contra PostgreSQL de Testcontainers con las migraciones de Usuarios:
  - el mismo evento registrado 5 veces seguidas da `true` una vez y `false` 4 veces, y queda 1 fila;
  - 2 hilos que lo registran a la vez, sincronizados con un `CountDownLatch`, dan exactamente un `true` y 1 fila;
  - la fila conserva el `recibido_en` de la primera vez.
- [ ] T032 [P] [US3] En `RegistrarEventoRecibidoServicioTest`, agregar un caso: un evento repetido no vuelve a aplicar el manejador. En `EventosContratoTest`, agregar otro: el repetido también responde 204.

### Implementation for User Story 3

- [ ] T033 [US3] Si T031 o T032 encuentran un hueco (por ejemplo, una carrera entre la inserción y el efecto), corregirlo en `RepositorioEventosRecibidosJdbc` o en `RegistrarEventoRecibidoServicio` con su prueba. Si no hay nada que corregir, anotar en la evidencia que el escenario pasa sin cambios.
- [ ] T034 [US3] Extender `deploy/probar_eventos.sh` con el paso 4 de contracts/demostracion.md:
  - `UPDATE eventos_distributivo.event_publication SET status = 'FAILED', completion_date = NULL` en la publicación del paso 1, buscándola con la función de T025;
  - en menos de 75 s, comprobar la publicación `COMPLETED` y 1 sola fila en `evento_recibido`.

  Validar con shellcheck, sobre una copia.

**Checkpoint**: los reenvíos no duplican registros.

---

## Phase 6: Transversal - seguridad del receptor y punta a punta

**Purpose**: el receptor no se alcanza desde fuera, y los tres escenarios pasan sobre el perfil levantado (FR-011, FR-012, SC-006, SC-007).

- [ ] T035 Escribir primero, en `backend/gateway/src/test/java/ec/mdd/gateway/SeguridadGatewayTest.java`, dos casos más:
  - `POST /interno/usuarios/eventos` sin token responde el 401 uniforme;
  - con un token válido responde 403, y el destino simulado no recibe la petición.

  Si ya pasan sin cambios (por `denyAll`), anotarlo en la evidencia.
- [ ] T036 Extender `deploy/probar_eventos.sh` con el paso 5 de contracts/demostracion.md:
  - vuelve a iniciar sesión, porque el token del paso 0 dejó de valer cuando identidad se reinició;
  - `POST /interno/usuarios/eventos` a través del API Gateway, sin token y con el de usuario: 401 y 403;
  - `usuarios.evento_recibido` no cambia.

  Validar con shellcheck, sobre una copia.
- [ ] T037 En `.github/workflows/perfil-consolidado.yml`, agregar el paso «Eventos de punta a punta» (`deploy/probar_eventos.sh`, con `shell: bash`) después de «Errores de punta a punta» y antes de la medición. Validar con actionlint, sobre una copia.
- [ ] T038 En `backend/plantillas/README.md`, agregar dos secciones:
  - **«Publicar un evento hacia otro proceso»:**
    - el puerto `PublicadorDeEventos` y el sobre;
    - `plataforma:eventos` en el proceso, con su esquema y su clave de proceso;
    - el destino en `mdd.eventos.destinos`;
  - **«Recibir eventos de otro proceso»:**
    - la tabla `evento_recibido` con su DDL;
    - el `INSERT … ON CONFLICT DO NOTHING` en la transacción del efecto;
    - la ruta `/interno/<servicio>/eventos` en el contrato;
    - la prueba de entregas repetidas y simultáneas.

  Con referencias a ADR-018 y a contracts/entrega.md.
- [ ] T039 Ejecutar `./gradlew spotlessApply build integrationTest`: todo en verde, con el 60 % de cobertura en `plataforma:eventos`, `plataforma:seguridad`, Usuarios y Generación. Después, desde un volumen nuevo, `docker compose down -v && docker compose up -d --wait --build`, y comprobar que:
  - `deploy/probar_autenticacion.sh` y `deploy/probar_errores.sh` siguen todo en `OK`;
  - `deploy/probar_eventos.sh` da los pasos 0 a 5 en `OK`;
  - `docker compose logs distributivo` tiene los WARN de la caída con `idEvento` y sin el contenido, la clave ni el token.

  Anotar las salidas y las líneas del registro en `specs/006-eventos-persistentes/evidencia.md`.

---

## Phase 7: Polish & cierre

- [ ] T040 Revisar el JavaDoc de `plataforma:eventos`, de la separación de tokens, del token de proceso, del receptor y del evento de prueba. Comprobar con `git status` que no hay archivos temporales. Correr Gitleaks local sobre un clon: `no leaks found`.
- [ ] T041 Hacer commit y push, y abrir el pull request de `006-eventos-persistentes` hacia `main` **en borrador**, con `Refs #22`. Esperar a que terminen los runs del borrador y recién entonces marcarlo como listo. Comprobar que corren y pasan:
  - `2 · test-integracion`;
  - `4 · sbom-backend` y `4 · sca`. Si Grype marca una dependencia nueva de Spring Modulith con versión corregida, fijarla como en B00-H02 y anotarlo;
  - `perfil-consolidado`. Pegar en la evidencia la medición de R-19 del run (SC-007).
- [ ] T042 Completar `specs/006-eventos-persistentes/evidencia.md` y la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**. Después:
  - ejecutar `perfil-consolidado` sobre `main` y abrir el pull request de evidencia con `Closes #22`, como en B00-H04;
  - borrar las ramas fusionadas;
  - en el mismo pull request de evidencia, marcar T019 a T022 en `specs/005-spike-firmaec/tasks.md`, que quedaron sin marcar en B00-S01.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: sin dependencias.
- **Foundational (fase 2)**:
  - los contratos (T003 a T007) van antes que el código;
  - T008 a T010 son independientes entre sí;
  - T011 va antes que T012, T013 antes que T014 (T014 depende de T005 y T012), y T015 antes que T016 y T017.
- **US1 (fase 3)**: depende de la fase 2.
  - T018 y T019 van antes que T022 y T023;
  - T020 va antes que T021;
  - T024 depende de T021 y T023;
  - T025 depende de T022 a T024.
- **US2 (fase 4)**: depende de US1.
  - T026 y T027 van antes que T028;
  - T029 depende de T024;
  - T030 depende de T025 y T028.
- **US3 (fase 5)**: depende de T022 (el receptor). T031 y T032 van antes que T033. T034 depende de T030.
- **Transversal (fase 6)**: T035 es independiente; T036 depende de T034; T037 depende de T036; T039 cierra la fase.
- **Cierre (fase 7)**: depende de todo lo anterior.

### Parallel Opportunities

- Fase 1: T002.
- Fase 2: T003, T004 y T006 (archivos distintos); T008, T009 y T010; T011 y T013 y T015, porque son pruebas de módulos distintos.
- US1: T018 con T019.
- US2: T026 con T027.
- US3: T031 con T032, en cuanto exista el receptor (T022), en paralelo con US2.
- Fase 6: T035 y T038 en cualquier momento después de la fase 2.
- Como trabajas solo (límite de 1 en «Implementar»), [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (US1)

1. Fases 1 y 2: contratos, esquemas, separación de tokens, token de proceso y núcleo del emisor.
2. Fase 3: receptor, evento de prueba, oyente y ensamblado en distributivo.
3. **Validar:** `EntregaIntegrationTest` y el paso 1 de `deploy/probar_eventos.sh`.

### Incremental

1. **US2:** el reenvío con el receptor caído.
2. **US3:** las entregas repetidas y simultáneas.
3. **Transversal:** la seguridad a través del API Gateway, el workflow y la documentación.
4. **Cierre:** pull request, evidencia con R-19 y fusión.

---

## Notes

- Los contratos (T003 a T006) se revisan antes de escribir código: si hay que cambiarlos durante la implementación, se cambia primero el archivo y después el código (principio IV).
- El volumen local de PostgreSQL se recrea una vez para tener los esquemas `eventos_<proceso>` (quickstart).
- Hacer commit por tarea o por grupo lógico en `006-eventos-persistentes`, con `Refs #22`.
