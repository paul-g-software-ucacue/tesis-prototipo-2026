# Research: Eventos persistentes entre procesos (B00-H05)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. La versión de Spring Modulith es la del proyecto (2.1.1); lo que se afirma de su API se comprobó en los JAR publicados de la 2.1.1 (`spring-modulith-events-api`, `-core` y `-jdbc`).

## 1. Registro persistente de publicaciones

**Decisión.** El registro de Spring Modulith con JDBC, solo en los procesos que publican (en esta historia, distributivo), con estos módulos y no con el *starter*:
- `spring-modulith-events-jdbc` (repositorio JDBC y su DDL v2);
- `spring-modulith-events-jackson` (serializa el evento con el Jackson 3 del proceso);
- `spring-modulith-events-api` (`@ApplicationModuleListener`, `FailedEventPublications`).

`spring-modulith-starter-jdbc` arrastra `spring-modulith-starter-core`, y con él `spring-modulith-core` y ArchUnit en el *runtime* de la imagen, que solo hacen falta en las pruebas (`ModulosTest`). Los tres módulos elegidos dependen solo de Spring, Spring Boot, SLF4J y JSpecify.

La configuración del registro:
- `spring.modulith.events.jdbc.schema: eventos_<proceso>`: el registro vive en un esquema propio del proceso, no en el de un servicio (§2);
- `spring.modulith.events.jdbc.schema-initialization.enabled: false` (el valor por defecto): el esquema lo migra Flyway (§2);
- `spring.modulith.events.completion-mode: update` (el valor por defecto): una publicación completada queda con su `completion_date` y su estado `COMPLETED`, que es lo que comprueba el escenario «Entrega normal»;
- `spring.modulith.events.republish-outstanding-events-on-restart: false`: el reenvío lo hace la tarea programada (§4), también después de un reinicio.

**Por qué.** Es el mecanismo que fija la constitución (principio V y §3) y no agrega colas ni broker (principio X). La publicación se guarda en la misma transacción que el cambio del servicio: si la transacción se revierte, no queda publicación (FR-001).

**Alternativas.**
- *Starter* JDBC: suma `spring-modulith-core` y ArchUnit al *runtime*, con más SBOM y más memoria sin ganar nada.
- Registro JPA: el proceso distributivo no tiene JPA, y el JDBC basta.
- Una tabla de salida (*outbox*) propia: repetiría lo que ya trae Spring Modulith.

## 2. Esquema del registro y su migración

**Decisión.**
- `deploy/postgres/01-esquemas.sh` crea un esquema `eventos_<proceso>` por cada uno de los 4 procesos, con el rol del proceso como dueño.
- `plataforma:eventos` tiene su propio Flyway (`flywayEventos`), limitado a ese esquema y a `db/migration/eventos`, con el DDL v2 de PostgreSQL de Spring Modulith 2.1.1 (`event_publication` y sus dos índices).
- El nombre del esquema sale de `spring.modulith.events.jdbc.schema`, y la autoconfiguración falla al arrancar si falta o no cumple `eventos_[a-z]+`.

**Por qué.**
- El registro es infraestructura del proceso, no un dato de un servicio. Si viviera en `generacion`, las publicaciones de `firma` quedarían en el esquema de otro servicio (principio V).
- Un esquema por proceso respeta ADR-013: un rol por proceso, dueño de sus esquemas.
- Se crean los 4 de una vez porque `01-esquemas.sh` solo corre al crear el volumen. Así, cuando datos, salida o identidad publiquen (F16 y F17), no habrá que volver a borrar el volumen.

**Alternativas.**
- El registro en el esquema de un servicio: rompe el principio V.
- El esquema `public`: está cerrado desde ADR-013 (`REVOKE CREATE`).
- Que Flyway cree el esquema: los roles de los procesos no pueden crear esquemas en la base.

**Consecuencia.** El volumen local hay que recrearlo una vez (`docker compose down -v`). La CI (`perfil-consolidado`) y el VPS (B00-H08) parten de un volumen nuevo.

**Riesgo comprobado.** Al arrancar, el repositorio JDBC solo lee el tipo de base de los metadatos del `DataSource`. No consulta la tabla, porque el reenvío al arrancar está apagado. Por eso no depende del orden entre `flywayEventos` y el registro. La prueba de integración del proceso lo comprueba.

## 3. Sobre del evento entre procesos

**Decisión.** El servicio publica un `EventoEntreProcesos` de `plataforma:eventos`, que es el sobre que viaja por HTTP:
- `idEvento` (UUID v4, generado al publicar);
- `tipo` (`<servicio>.<NombreDelEvento>`);
- `origen` (el servicio que lo publica);
- `destino` (el servicio receptor);
- `momento` (instante de la publicación);
- `contenido` (objeto JSON del tipo; sin contraseñas ni tokens).

El esquema público es `EventoEntreProcesos` en `contratos/comun/eventos.yaml`, sin `destino`, que solo sirve al emisor para elegir la URL. El servicio no conoce HTTP: publica por su puerto de salida `PublicadorDeEventos`, cuyo adaptador usa el `ApplicationEventPublisher` de Spring.

**Por qué.**
- El identificador lo genera quien publica. Así es el mismo en cada reenvío, que es lo que necesita el receptor idempotente (FR-007).
- El identificador interno de la publicación de Spring Modulith no llega al oyente.
- Un solo sobre, con un oyente común, evita un oyente y un cliente HTTP por cada tipo de evento.

**Alternativas.** Un evento de dominio por tipo, con su propio oyente y cliente: con F16 y F17 serían varios oyentes iguales. El identificador de Spring Modulith: no está disponible en el oyente.

## 4. Entrega y reenvío

**Decisión.**
- **Oyente:** `OyenteEntreProcesos`, en la autoconfiguración de `plataforma:eventos`, con `@ApplicationModuleListener`. Se ejecuta de forma asíncrona después de confirmar la transacción, en una transacción nueva. Spring Modulith activa `@Async` solo.
- **Cliente:** `ClienteDeEventos`, con `RestClient` sobre el `HttpClient` del JDK y plazos de conexión y de lectura (`mdd.eventos.plazo`, 5 s por defecto). La URL de cada destino sale de `mdd.eventos.destinos.<servicio>`.
  - 2xx: el oyente termina y la publicación queda `COMPLETED`.
  - Cualquier otra respuesta, un plazo vencido o un error de conexión: lanza una excepción y la publicación queda `FAILED`.
  - Cada fallo se registra en WARN con `idEvento`, `tipo`, `destino` y la causa resumida (estado HTTP o clase de la excepción), sin el contenido (FR-006).
- **Reenvío:** `ReenvioDePublicaciones`, con `@Scheduled(fixedDelay = mdd.eventos.reenvio.intervalo)`, 30 s por defecto. Llama a `FailedEventPublications.resubmit(ResubmissionOptions.defaults())` y registra en INFO cada publicación que reenvía, con `idEvento` y `tipo`, desde el filtro de las opciones. `@EnableScheduling` lo activa la autoconfiguración.
- **Proceso caído a mitad de una entrega:** las publicaciones que quedaron `PUBLISHED` o `PROCESSING` pasan a `FAILED` con el monitor de publicaciones atascadas de Spring Modulith 2.1. Se configura con `spring.modulith.events.staleness.published`, `.processing` y `.resubmitted`, en 2 minutos, más que el plazo de espera. Después la tarea las reenvía. Cubre el caso límite «El proceso que publica se detiene después de confirmar».
- **Sin máximo de intentos:** una publicación `FAILED` se reenvía en cada ciclo hasta que se confirma (supuestos de la especificación).

**Por qué.** Es el reparto del principio V: el oyente del mismo proceso hace la llamada, y una tarea programada reenvía lo pendiente. La transacción del servicio no espera la llamada (FR-003). `FAILED` y el monitor de atascadas son la forma de Spring Modulith 2.x de separar «falló» de «en curso». Así, la tarea no reenvía una entrega que todavía está en marcha.

**Alternativas.**
- `IncompleteEventPublications.resubmitIncompletePublicationsOlderThan`: reenvía también las que están en curso, y con eso duplica entregas sin necesidad.
- `republish-outstanding-events-on-restart`: solo actúa al arrancar, y no cubre al receptor caído con el emisor en marcha.
- Reintentos dentro del oyente (Spring Retry): ocupan hilos y no sobreviven a un reinicio.

**Valores para SC-003.** Con el intervalo de 30 s, una publicación `FAILED` se reenvía en el primer ciclo después de que identidad vuelve, en menos de 30 s más el plazo. Ver el §6 sobre el token después de un reinicio de identidad.

## 5. Recepción idempotente

**Decisión.**
- Cada servicio receptor tiene en su esquema la tabla `evento_recibido`, con la misma forma:
  - `id_evento` (UUID, clave primaria);
  - `tipo`, `origen` y `momento`;
  - `recibido_en`.
- El receptor la escribe con `INSERT … ON CONFLICT (id_evento) DO NOTHING`, en la misma transacción que el efecto del evento: en esta historia ninguno, porque el receptor es de prueba (D-47); con F17, la entrada de la bitácora.
  - Si se insertó una fila, aplica el efecto.
  - Si no (el evento ya estaba registrado), no hace nada.
  - En los dos casos responde 204.
- Un `tipo` sin manejador responde 400 `COMUN_EVENTO_NO_RECONOCIDO`, y la publicación queda `FAILED` (caso límite de la especificación).
- La parte común (FR-008) es:
  - el sobre del contrato (`comun/eventos.yaml`), del que se generan los modelos;
  - la regla de seguridad de `/interno/**` (§7);
  - el código común `COMUN_EVENTO_NO_RECONOCIDO`;
  - la forma de la tabla y su prueba, documentadas en `backend/plantillas/README.md`.

  El SQL de cada receptor vive en su adaptador, porque debe ir en la transacción de su servicio y en su esquema.

**Por qué.**
- La clave primaria hace la idempotencia atómica, también con dos entregas simultáneas (SC-004), sin bloqueos explícitos ni lecturas previas.
- Responder 204 también al repetido cierra la publicación del emisor cuando se perdió la primera respuesta (caso límite).

**Alternativas.**
- Leer antes de insertar: tiene una carrera con dos entregas simultáneas.
- Un componente común que construya el SQL con el nombre del esquema: necesita SQL dinámico (FindSecBugs) y sacaría la inserción de la transacción del servicio.
- 201 la primera vez y 200 al repetir: al emisor no le cambia nada.

## 6. Token de proceso (D-49)

**Decisión.**
- **Emisión:** `POST /interno/usuarios/token-proceso`, en el contrato de Usuarios, sin token de usuario. El cuerpo trae `{proceso, clave}`.
  - Usuarios compara la clave con la de `mdd.usuarios.procesos.<proceso>.clave` en tiempo constante, con `MessageDigest.isEqual` sobre SHA-256.
  - Si coincide, emite con su llave RS256 (`EmisorJwt`) un JWT con `sub` = `proceso:<proceso>`, `roles` = `[PROCESO]` y una vigencia de `mdd.usuarios.token-proceso.vigencia` (5 min por defecto).
  - Un proceso sin clave configurada, o una clave incorrecta, responde 401 `USUARIOS_CREDENCIALES_INVALIDAS`, con la misma respuesta en los dos casos.
- **Credenciales:** una variable por proceso, `MDD_CLAVE_PROCESO_<PROCESO>`, presente en identidad y en el proceso que publica. En `.env.ejemplo` va un valor ficticio `cambiar-en-local-…`, como las claves de la base; en el VPS la configura B00-H08. En esta historia solo hace falta la de distributivo.
- **Uso en el emisor:** `ProveedorDeTokenDeProceso` pide el token, lo guarda en memoria y lo renueva cuando le queda menos de 1 minuto.
  - Si el receptor responde 401, descarta el token, pide uno nuevo y reintenta la entrega una sola vez.
  - Es necesario porque, si identidad se reinicia con la llave efímera (ADR-014), el token anterior deja de valer. Sin el reintento, el reenvío tardaría un ciclo más (SC-003).
  - Si Usuarios no responde, la entrega falla y la publicación queda `FAILED` (FR-011).

**Por qué.**
- Reutiliza la llave, el formato y la validación de ADR-014. Para el receptor, el token de proceso es un JWT más.
- La clave del proceso es un secreto aleatorio del entorno, no una contraseña de una persona: con una comparación en tiempo constante basta, sin Argon2.
- Con la vida corta, un token filtrado se vence pronto.

**Alternativas.**
- Un usuario «proceso» en la tabla de usuarios, con el inicio de sesión normal: mezcla cuentas técnicas con personas en la gestión de usuarios (F05).
- OAuth2 *client credentials* con Spring Authorization Server: es una dependencia y un servidor nuevos para un solo caso.
- Guardar el hash de la clave en identidad: protege poco, porque el valor en claro igual está en el entorno del emisor.

## 7. Rutas internas y separación de tokens

**Decisión.**
- Las rutas entre procesos usan el prefijo `/interno/<servicio>/…`. El API Gateway solo enruta `/api/**`, y su seguridad ya niega todo lo demás (`anyExchange().denyAll()` en `SeguridadGatewayConfig`). Así, `/interno/**` nunca llega a un proceso desde fuera: sin token responde el 401 uniforme y con token 403 (FR-012). El API Gateway no cambia.
- En `plataforma:seguridad`:
  - `/interno/**` exige el rol `PROCESO`: un token de usuario responde 403, y sin token o con un token no válido responde el 401 uniforme;
  - cualquier otra ruta no pública exige estar autenticado y no tener el rol `PROCESO`: un token de proceso responde 403 fuera de `/interno/**`;
  - `POST /interno/usuarios/token-proceso` se agrega a `mdd.seguridad.rutas-publicas` de identidad.
- El 403 responde sin cuerpo, como ya hace `ControladorDeErrores`. El cuerpo con el modelo común llega con F05-H02.

**Por qué.** Es la condición de D-49: un token de proceso no sirve para otras rutas, y un usuario no puede escribir en el receptor. Un prefijo distinto deja la regla en un solo lugar y fuera del API Gateway, sin listas de rutas en dos lados.

**Alternativas.**
- Rutas `/api/usuarios/interno/...` con un predicado de exclusión en el API Gateway: la regla quedaría en dos lugares.
- Un puerto HTTP aparte para lo interno: es más configuración y no cambia la seguridad dentro de la red de Docker.

## 8. Evento de prueba en el proceso distributivo (D-48)

**Decisión.**
- **Servicio:** Generación, que ya tiene contrato y sesión actual (D-39).
- **Operación:** `POST /api/generacion/eventos-prueba`, con token de usuario y sin cuerpo. Responde 202 con `{idEvento}`.
- **Caso de uso `PublicarEventoDePrueba`:** en una transacción, inserta una fila en `generacion.evento_prueba` (`id_evento`, `momento`) y publica `generacion.EventoDePrueba` hacia `usuarios`, con un contenido ficticio (`{"nota": "evento de prueba B00-H05"}`).
  - La fila es el cambio del servicio que se confirma aunque identidad esté caído (escenario 2).
  - La prueba de reversión fuerza un fallo después de publicar y comprueba que no quedan ni fila ni publicación (SC-005).
- **Encendido:** el controlador solo existe con `mdd.generacion.eventos-prueba.habilitado=true` (`@ConditionalOnProperty`), que sale de `MDD_EVENTOS_PRUEBA_HABILITADO`, falso por defecto y verdadero en `.env.ejemplo`, como el usuario de prueba.
  - Apagado, la ruta no existe y responde 404 `COMUN_RECURSO_NO_ENCONTRADO`.
  - B00-H08 no lo enciende en el VPS.
- **Receptor de prueba (D-47):** `POST /interno/usuarios/eventos` en Usuarios. Solo tiene manejador para `generacion.EventoDePrueba`, y lo registra en `usuarios.evento_recibido`, sin otro efecto. F17-H01 agrega sus tipos en el mismo receptor.

**Por qué.** Es lo mínimo para recorrer los escenarios con los procesos reales, sin inventar reglas de negocio (D-47, D-48).

**Alternativas.** Publicar desde un `CommandLineRunner`: no se puede repetir a pedido desde el script. Validación OCS o Firma: no tienen contrato todavía.

## 9. Prueba de punta a punta y CI

**Decisión.** `deploy/probar_eventos.sh`, con el estilo de `probar_errores.sh`, sobre el perfil levantado:
1. **Entrega normal:**
   - inicia sesión y publica a través del API Gateway;
   - en menos de 10 s, comprueba con `docker compose exec -T postgres psql` que hay 1 fila del `idEvento` en `usuarios.evento_recibido` y que la publicación está `COMPLETED`.
2. **Receptor caído:**
   - `docker compose stop identidad` y publica con el token que ya tenía, que el API Gateway sigue validando con el JWKS en caché;
   - comprueba que la fila de `generacion.evento_prueba` existe (la transacción se confirmó) y que la publicación no está `COMPLETED`;
   - `docker compose start identidad`, espera a que esté sano, y en menos de 2 ciclos comprueba 1 fila en `evento_recibido` y la publicación `COMPLETED`.
3. **Entrega repetida:**
   - vuelve a marcar `FAILED` la publicación del paso 1 (`UPDATE … SET status = 'FAILED', completion_date = NULL`), como si se hubiera perdido la respuesta;
   - espera el reenvío y comprueba que la publicación vuelve a `COMPLETED` y que `evento_recibido` sigue con 1 fila.
4. **Seguridad:**
   - `POST /interno/usuarios/eventos` a través del API Gateway responde 401 sin token y 403 con el token de usuario, sin llegar a identidad;
   - con el token de usuario, `docker compose exec` no puede llegar al receptor (las imágenes no tienen curl), así que el 401 y el 403 los cubren las pruebas automáticas.

El workflow `perfil-consolidado` agrega el paso «Eventos de punta a punta» después de `probar_errores.sh`.

**Por qué.** Es como B00-H03 y B00-H04. Consultar la base desde el script evita un endpoint de estado solo para la prueba. Volver a marcar la publicación es la forma más directa de reproducir una entrega repetida real.

## 10. Pruebas automáticas (principio VIII)

- **`plataforma:eventos`, suite `test`:**
  - `ClienteDeEventos` contra un servidor HTTP del JDK (`com.sun.net.httpserver`): 2xx, 4xx, 5xx, plazo vencido, conexión rechazada y 401 con renovación del token;
  - `ProveedorDeTokenDeProceso`: caché y renovación;
  - el registro sin contenido, con `OutputCaptureExtension`;
  - la validación del nombre del esquema.
- **`plataforma:eventos`, suite `integrationTest`** (Testcontainers con PostgreSQL y el servidor HTTP del JDK como receptor):
  - entrega normal: `COMPLETED`;
  - receptor caído: `FAILED`, y después de levantarlo, el reenvío deja `COMPLETED`;
  - una transacción revertida no deja publicación;
  - el monitor de atascadas pasa a `FAILED` una publicación `PROCESSING` vieja.
- **Usuarios:**
  - el token de proceso (emisión, clave incorrecta, proceso sin clave y vigencia);
  - el contrato de `/interno/usuarios/eventos` y `/interno/usuarios/token-proceso` con `ValidadorDeContrato`;
  - la integración de `evento_recibido`: una sola fila con N entregas seguidas y con 2 simultáneas;
  - el tipo desconocido responde 400.
- **`plataforma:seguridad`:** token de usuario en `/interno/**` (403), token de proceso fuera de `/interno/**` (403), y sin token (401 uniforme).
- **Generación:** el endpoint apagado responde 404 y el encendido 202, con la prueba de contrato; la reversión, en la integración.
- **Procesos:** `ModulosTest` sigue pasando. La integración de distributivo arranca con el registro y `flywayEventos`.

Sin Mockito fuera de la capa de aplicación (DS-2). 60 % por módulo (D-32).

## 11. Memoria (R-19, SC-007)

Distributivo suma `spring-modulith-events-*`, el *pool* asíncrono por defecto de Spring Boot (8 hilos) y una tarea programada. No cambia los límites de `compose.yaml` (512 MiB por proceso). La medición de R-19 del PR de evidencia lo comprueba (D-34). Si distributivo pasara de su límite, se ajusta `spring.task.execution.pool.max-size`.

## 12. ADR-018

La entrega entre procesos es una decisión que afecta a varias features: F16, F17 y cualquier evento futuro. Abarca:
- el registro por proceso;
- el sobre y su contrato;
- las rutas `/interno/**`;
- el token de proceso, que modifica ADR-014;
- la recepción idempotente.

Por eso se registra como **ADR-018** (constitución §6, paso 4), en estado «Propuesta» hasta que el autor la apruebe.
