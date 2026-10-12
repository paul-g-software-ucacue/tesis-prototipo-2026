# Modelo de datos (fase 1): B00-H02

No hay datos de negocio. El modelo describe los elementos del despliegue que nombra la especificación (Key Entities), con sus reglas de validación.

## Contenedor

| Contenedor | Imagen | Depende de | Puerto en el anfitrión | Límite de memoria | Sano cuando |
|---|---|---|---|---|---|
| `postgres` | `postgres:18.6-alpine` | — | ninguno | 512 MiB | `pg_isready -h 127.0.0.1 -U postgres -d mdd` responde |
| `identidad` | construida desde `apps:identidad` | `postgres` sano | ninguno | 512 MiB | `/actuator/health/readiness` = `UP` (incluye la base) |
| `datos` | construida desde `apps:datos` | `postgres` sano | ninguno | 512 MiB | ídem |
| `distributivo` | construida desde `apps:distributivo` | `postgres` sano | ninguno | 512 MiB | ídem |
| `salida` | construida desde `apps:salida` | `postgres` sano | ninguno | 512 MiB | ídem |
| `gateway` | construida desde `gateway` | — | `8080` | 384 MiB | `/actuator/health/readiness` = `UP` |

**Validaciones:**

- Son exactamente 6 contenedores (FR-002).
- Todos tienen un *healthcheck* (FR-003) y un límite de memoria (FR-006). La suma de los límites es 2 944 MiB.
- Cada JVM recibe `-XX:MaxRAMPercentage=60` (FR-006).

## Proceso

| Proceso | Rol de la base | Servicios (ADR-006) |
|---|---|---|
| identidad | `identidad` | usuarios, roles, notificaciones |
| datos | `datos` | cargas, docentes, academico, normativa |
| distributivo | `distributivo` | generacion, validacionocs, firma |
| salida | `salida` | reporting, dashboard |

**Validaciones:**

- `ModulosTest` pasa, es decir, sus servicios no usan clases internas de otros.
- `EsquemasIntegrationTest` pasa.
- El proceso se conecta con su rol.

## Servicio (D-35)

| Atributo | Regla |
|---|---|
| Subproyecto | `:servicios:<servicio>`, creado desde `backend/plantillas/servicio` |
| Paquete | `ec.mdd.<servicio>` |
| Esquema | `<servicio>`, igual al nombre del servicio |
| Migraciones | `classpath:db/migration/<servicio>`; la primera, `V1__esquema_<servicio>.sql`, no crea tablas de negocio |
| Configuración | `ec.mdd.<servicio>.infraestructura.<Servicio>FlywayConfig`: un bean `Flyway` con `schemas=<servicio>`, que migra al arrancar el proceso |

**Validaciones:**

- La prueba unitaria confirma que su `Flyway` usa solo su esquema y su carpeta.
- `<Servicio>EsquemaIntegrationTest` confirma que, migrado solo, todas sus tablas quedan en su esquema.
- `ArquitecturaTest` pasa.
- La cobertura es del 60 % o más.

## Esquema

| Atributo | Regla |
|---|---|
| Nombre | El del servicio |
| Dueño | El rol del proceso que contiene al servicio |
| Contenido después de B00-H02 | Solo `flyway_schema_history`, el registro de migraciones del servicio |
| Regla | Ninguna tabla de un servicio queda fuera de su esquema; no hay tablas en `public` (FR-009, FR-011, SC-003) |

## Rol de la base

| Atributo | Regla |
|---|---|
| Nombre | El del proceso |
| Contraseña | La de `.env.ejemplo` (ficticia) o la de `.env` |
| Privilegios | Dueño de los esquemas de sus servicios. Sin `CREATE` en `public` ni en los esquemas de otros procesos. |

## Medición de R-19 (FR-008)

| Campo | Contenido |
|---|---|
| Fecha y hora | ISO 8601 |
| Commit | SHA corto del commit medido |
| Entorno | `ubuntu-latest` (Docker) o local (Docker y versión) |
| Por contenedor | Nombre, consumo (MiB) y límite (MiB) |
| Total | MiB y GiB |
| Resultado | `< 8 GiB` (cumple) o `≥ 8 GiB` (falla); además, `alerta R-19` si es mayor que 7 GiB (D-36) |
| Calentamiento | Número de llamadas a la salud por proceso (20) y espera antes de medir (30 s) |
