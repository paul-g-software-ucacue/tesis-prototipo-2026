# Plantillas de servicio y de proceso

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

La seguridad (JWT RS256 contra el JWKS de Usuarios y el 401 uniforme) viene de `plataforma:seguridad`, que la plantilla ya declara (ADR-014). Las rutas que el proceso atiende sin token van en `mdd.seguridad.rutas-publicas`, con el formato `MÉTODO /ruta`; la salud de Actuator siempre es pública.

## Agregar un contrato a un servicio

Cuando la historia de un servicio agrega su primera API (principio IV, ADR-015):

1. Escribir `contratos/<servicio>/openapi.yaml` en OpenAPI 3.0.3, con `security: [bearer: []]` global y las respuestas de error con `$ref` a `../comun/errores.yaml` y `../comun/seguridad.yaml`. Revisarlo antes de escribir código.
2. Agregar los códigos de error nuevos a `contratos/comun/errores.md`, con el prefijo del servicio.
3. En el `build.gradle` del servicio, agregar `id 'mdd.contrato'`. Las interfaces y los modelos se generan en `ec.mdd.<servicio>.adaptadores.entrada.api`.
4. El controlador, en `adaptadores/entrada`, implementa la interfaz generada; si no cumple el contrato, no compila.
5. Por cada operación, una prueba de contrato con `@WebMvcTest`, `@ImportAutoConfiguration(SeguridadProcesoAutoConfiguracion.class)` y `ValidadorDeContrato.para("<servicio>")` de `plataforma:pruebas`, que valide cada respuesta (por ejemplo, `servicios/cargas/.../SesionContratoTest`).
