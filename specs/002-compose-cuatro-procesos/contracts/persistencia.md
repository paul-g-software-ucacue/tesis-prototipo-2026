# Contrato: un esquema por servicio

Es lo que garantiza la persistencia de cada servicio desde B00-H02 (escenario 3; FR-009 a FR-011; D-35; ADR-013).

## Por servicio

```text
servicios/<servicio>/
├── src/main/java/ec/mdd/<servicio>/infraestructura/<Servicio>FlywayConfig.java
├── src/main/resources/db/migration/<servicio>/V1__esquema_<servicio>.sql
├── src/test/java/ec/mdd/<servicio>/<Servicio>FlywayConfigTest.java
└── src/integrationTest/java/ec/mdd/<servicio>/<Servicio>EsquemaIntegrationTest.java
```

| Pieza | Contrato |
|---|---|
| `<Servicio>FlywayConfig` | Un `@Configuration` con un bean `Flyway flyway<Servicio>(DataSource)` y `initMethod = "migrate"`. Configuración: `schemas("<servicio>")`, `locations("classpath:db/migration/<servicio>")` y registro de migraciones `flyway_schema_history` dentro del esquema. JavaDoc en español. |
| `V1__esquema_<servicio>.sql` | Primera migración. No crea tablas de negocio (D-35); deja constancia de que el esquema es del servicio. Las migraciones siguientes las agregan las historias del servicio (`V2__…`). |
| `<Servicio>FlywayConfigTest` | Sin base de datos: comprueba que el `Flyway` configurado tiene como único esquema `<servicio>` y como única ubicación `classpath:db/migration/<servicio>`. |
| `<Servicio>EsquemaIntegrationTest` (suite `integrationTest`) | Contra un PostgreSQL 18.6 vacío de Testcontainers, sin contexto de Spring: migra **solo** este servicio con su `<Servicio>FlywayConfig` y comprueba en `information_schema.tables` que todas las tablas creadas están en `<servicio>`. Como la base solo tiene las migraciones de este servicio, cualquier tabla fuera de su esquema es suya: así se atribuye sin ambigüedad (FR-011). |

**Reglas:**

- Un servicio nunca escribe migraciones en la carpeta ni en el esquema de otro.
- El nombre de la clase de configuración es único en todo el proyecto (`<Servicio>FlywayConfig`).

## Por proceso

| Pieza | Contrato |
|---|---|
| Conexión | Un solo `DataSource` por proceso, con el rol del proceso (`SPRING_DATASOURCE_USERNAME`). |
| Arranque | Al iniciar, el proceso ejecuta el `migrate` de cada servicio que ensambla. |
| `EsquemasIntegrationTest` (suite `integrationTest`) | Con un PostgreSQL 18.6 de Testcontainers, levanta el contexto completo del proceso, de modo que los servicios conviven y migran juntos. Toma los esquemas esperados de los beans `Flyway` y comprueba en `information_schema.tables` tres cosas: (1) cada esquema esperado tiene su `flyway_schema_history`; (2) no hay tablas en `public`; (3) no hay tablas en esquemas que no pertenezcan a un servicio del proceso. No puede atribuir una tabla a un servicio dentro del mismo proceso; eso lo hace la prueba de cada servicio. |

## En PostgreSQL (`deploy/postgres/01-esquemas.sh`)

| Rol | Contraseña | Esquemas de los que es dueño |
|---|---|---|
| `identidad` | `DB_CLAVE_IDENTIDAD` | `usuarios`, `roles` y `notificaciones` |
| `datos` | `DB_CLAVE_DATOS` | `cargas`, `docentes`, `academico` y `normativa` |
| `distributivo` | `DB_CLAVE_DISTRIBUTIVO` | `generacion`, `validacionocs` y `firma` |
| `salida` | `DB_CLAVE_SALIDA` | `reporting` y `dashboard` |

- Se revoca `CREATE` en `public` para `PUBLIC`, que ya es el valor por defecto desde PostgreSQL 15 y se deja explícito.
- Un rol no tiene privilegios sobre los esquemas de otro proceso.

## Garantías

1. Después de arrancar, cada esquema contiene solo el registro de migraciones de su servicio, y nada más hasta que su historia agregue tablas (escenario 3, SC-003).
2. Un proceso no puede crear ni leer tablas en los esquemas de otro proceso: lo impide la base.
3. Una migración de un servicio que cree una tabla en el esquema de otro servicio, sea del mismo proceso o no, hace fallar `<Servicio>EsquemaIntegrationTest` de ese servicio.
4. Una segunda ejecución no vuelve a aplicar migraciones (SC-004).
