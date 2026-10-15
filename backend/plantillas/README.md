# Plantillas de servicio y de proceso

Las pantallas de la SPA no usan estas plantillas: cómo agregar una está en `frontend/README.md` (ADR-019).

Esqueletos que se copian cuando una historia crea un servicio o un proceso del MDD (ADR-012). No forman parte del build: no están en `settings.gradle`.

Las garantías de cada esqueleto (reglas de las cinco capas, API publicada, cobertura del 60 %, Spotless y SpotBugs) están en el contrato [`specs/001-repo-plantilla-ci/contracts/plantilla-servicio.md`](../../specs/001-repo-plantilla-ci/contracts/plantilla-servicio.md). La persistencia (un esquema y un Flyway por servicio, un rol por proceso) está en [`specs/002-compose-cuatro-procesos/contracts/persistencia.md`](../../specs/002-compose-cuatro-procesos/contracts/persistencia.md) (ADR-013).

**Nombres únicos:** ningún servicio ni proceso puede repetir el nombre de otro subproyecto. Gradle los identifica por grupo (`ec.mdd`) y nombre, y dos con el mismo nombre crean una dependencia circular.

## Crear un servicio

1. Copiar `plantillas/servicio/` a `servicios/<servicio>/`. El nombre va en español, sin número ni guiones (`normativa`, `validacionocs`).
2. En la copia, cambiar el paquete `ec.mdd.servicio` por `ec.mdd.<servicio>`, en las carpetas y en las líneas `package` e `import`.
3. Renombrar `ServicioFlywayConfig`, `ServicioFlywayConfigTest` y `ServicioEsquemaIntegrationTest` a `<Servicio>FlywayConfig`, `<Servicio>FlywayConfigTest` y `<Servicio>EsquemaIntegrationTest` (`NormativaFlywayConfig`, `ValidacionocsFlywayConfig`), y el método `flywayServicio` a `flyway<Servicio>`. Los nombres tienen que ser únicos en el proyecto.
4. Renombrar la carpeta `db/migration/servicio` a `db/migration/<servicio>` y el archivo `V1__esquema_servicio.sql` a `V1__esquema_<servicio>.sql`, y reemplazar el literal `servicio` en las clases, las pruebas y el comentario de la migración.
5. Agregar `include 'servicios:<servicio>'` en `backend/settings.gradle`.
6. Agregar `implementation project(':servicios:<servicio>')` en el `build.gradle` de su proceso, y el esquema `<servicio>` a nombre del rol del proceso en `deploy/postgres/01-esquemas.sh`.

## Crear un proceso

1. Copiar `plantillas/aplicacion/` a `apps/<proceso>/` (`identidad`, `datos`, `distributivo` o `salida`).
2. Renombrar `ProcesoAplicacion` a `<Proceso>Aplicacion`, también en `ModulosTest`. La clase se queda en el paquete `ec.mdd`.
3. En `application.yaml`, reemplazar `proceso` por el nombre del proceso (nombre de la aplicación y usuario de la base) y `DB_CLAVE_PROCESO` por `DB_CLAVE_<PROCESO>`.
4. Agregar `include 'apps:<proceso>'` en `backend/settings.gradle`.
5. Agregar el rol del proceso en `deploy/postgres/01-esquemas.sh`, su clave en `.env.ejemplo` y su contenedor en `compose.yaml`.

La seguridad (JWT RS256 contra el JWKS de Usuarios y el 401 uniforme) viene de `plataforma:seguridad`, que la plantilla ya declara (ADR-014). La traducción de errores (un solo `@RestControllerAdvice` y la página `/error`) viene de `plataforma:errores`, que la plantilla también declara (ADR-016). Las rutas que el proceso atiende sin token van en `mdd.seguridad.rutas-publicas`, con el formato `MÉTODO /ruta`; la salud de Actuator siempre es pública.

## Agregar un contrato a un servicio

Cuando la historia de un servicio agrega su primera API (principio IV, ADR-015):

1. Escribir `contratos/<servicio>/openapi.yaml` en OpenAPI 3.0.3, con `security: [bearer: []]` global y las respuestas de error con `$ref` a `../comun/errores.yaml` y `../comun/seguridad.yaml`. Revisarlo antes de escribir código.
2. Agregar los códigos de error nuevos a `contratos/comun/errores.md`, con el prefijo del servicio.
3. En el `build.gradle` del servicio, agregar `id 'mdd.contrato'`. Las interfaces y los modelos se generan en `ec.mdd.<servicio>.adaptadores.entrada.api`.
4. El controlador, en `adaptadores/entrada`, implementa la interfaz generada; si no cumple el contrato, no compila.
5. Por cada operación, una prueba de contrato con `@WebMvcTest`, `@ImportAutoConfiguration(SeguridadProcesoAutoConfiguracion.class)` y `ValidadorDeContrato.para("<servicio>")` de `plataforma:pruebas`, que valide cada respuesta (por ejemplo, `servicios/cargas/.../SesionContratoTest`).

## Registrar un error de un servicio

Cuando una historia agrega un error de dominio o de aplicación (principio XIII, ADR-016; `specs/004-modelo-errores/contracts/catalogo.md`):

1. La excepción vive en el dominio o en la aplicación, como clase Java común, sin HTTP. Su mensaje no lleva datos personales, tokens ni contraseñas.
2. Agregar su fila en `contratos/comun/errores.md`, con un código con el prefijo del servicio.
3. Registrarla en `adaptadores/entrada/Errores<Servicio>.java` (`@Component`, implementa `CatalogoDeErrores`) con `EntradaCatalogo.de(...)` y los mismos textos de la fila.
4. Declarar la respuesta en el contrato de la operación que puede producirla.
5. Pruebas: `VerificadorDeCatalogo.verificar(new Errores<Servicio>())`, y la respuesta en la prueba de contrato. `@WebMvcTest` no escanea los `@Component` comunes, así que la prueba de contrato importa `Errores<Servicio>` (`@Import`).

El advice único de cada proceso (`plataforma:errores`) traduce la excepción con su código; ningún controlador ni caso de uso repite esa traducción.

## Publicar un evento hacia otro proceso

Para lo que no puede perderse, como la bitácora y las notificaciones (principio V, ADR-018; `specs/006-eventos-persistentes/contracts/entrega.md`):

1. El servicio publica por un puerto de salida propio (por ejemplo, `PublicadorDeEventos`). Su adaptador en `adaptadores/salida` arma un `EventoEntreProcesos` de `plataforma:eventos` (`idEvento` nuevo, `tipo` `<servicio>.<NombreDelEvento>`, `origen`, `destino`, `momento` y `contenido` sin contraseñas ni tokens) y lo publica con `ApplicationEventPublisher`, **dentro de la transacción** del caso de uso. Si la transacción se revierte, no queda publicación. Ver `servicios/generacion/.../PublicadorDeEventosSpring`.
2. El servicio declara `implementation project(':plataforma:eventos')`, y su proceso también.
3. En el `application.yaml` del proceso:
   - `spring.modulith.events.jdbc.schema: eventos_<proceso>` (el esquema ya existe en `deploy/postgres/01-esquemas.sh`);
   - la vigilancia de publicaciones atascadas (`spring.modulith.events.staleness.*`);
   - `mdd.eventos.destinos.<servicio destino>` con la URL de su receptor, `mdd.eventos.token-proceso`, `mdd.eventos.proceso` y `mdd.eventos.clave: ${MDD_CLAVE_PROCESO_<PROCESO>:}`.

   Copiar el bloque de `apps/distributivo`.
4. La clave del proceso va en `.env.ejemplo`, con un valor ficticio, y en `mdd.usuarios.procesos.<proceso>.clave` de identidad.

`plataforma:eventos` entrega cada evento con un oyente asíncrono, después de confirmar la transacción, con plazo de espera y el token de proceso, y reenvía las publicaciones fallidas cada 30 s, sin máximo de intentos.

## Recibir eventos de otro proceso

La recepción sigue la misma forma en cada servicio receptor (FR-008, ADR-018); el SQL vive en el servicio porque va en su transacción y en su esquema:

1. En el contrato del servicio, `POST /interno/<servicio>/eventos` con el cuerpo `$ref: '../comun/eventos.yaml#/components/schemas/EventoEntreProcesos'` y las respuestas 204, 400, 401, 403, 500 y 503 (ver `contratos/usuarios/openapi.yaml`). Las rutas `/interno/**` solo admiten el token de proceso y el API Gateway no las enruta.
2. Una migración con la tabla `evento_recibido` en el esquema del servicio:

   ```sql
   create table evento_recibido (
       id_evento uuid primary key,
       tipo text not null,
       origen text not null,
       momento timestamptz not null,
       recibido_en timestamptz not null default now()
   );
   ```

3. Un repositorio que registra con `INSERT … ON CONFLICT (id_evento) DO NOTHING` y devuelve si insertó (`RepositorioEventosRecibidosJdbc` de Usuarios), y un caso de uso `@Transactional` que aplica el efecto del tipo **solo si insertó**, en la misma transacción. Un tipo sin manejador lanza una excepción registrada con `CodigosComunes.EVENTO_NO_RECONOCIDO`.
4. El controlador responde 204 también a un evento repetido, para que el emisor cierre su publicación.
5. Pruebas: el contrato del receptor (204, 400, 401 y 403) y una prueba de integración con entregas repetidas y simultáneas (`RepositorioEventosRecibidosJdbcIntegrationTest`).
