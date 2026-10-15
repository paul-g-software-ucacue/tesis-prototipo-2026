# Data Model: Eventos persistentes entre procesos (B00-H05)

No hay entidades de negocio. Las estructuras son técnicas: el sobre del evento, la publicación del emisor, el registro del receptor, la fila de la transacción de prueba y el token de proceso. Los detalles están en [research.md](research.md).

## Evento entre procesos (sobre)

Lo publica un servicio en su transacción y viaja por HTTP al receptor. Clase `EventoEntreProcesos` de `plataforma:eventos`; esquema público `EventoEntreProcesos` en `contratos/comun/eventos.yaml`.

| Campo | Tipo | Reglas |
|---|---|---|
| `idEvento` | UUID | Obligatorio. Lo genera quien publica (UUID v4) y no cambia en los reenvíos. Es la clave de idempotencia del receptor. |
| `tipo` | texto | Obligatorio. `<servicio>.<NombreDelEvento>`, con el patrón `^[a-z]+\.[A-Z][A-Za-z]+$`; por ejemplo, `generacion.EventoDePrueba`. |
| `origen` | texto | Obligatorio. Nombre del servicio que publica, con el patrón `^[a-z]+$`. |
| `momento` | fecha y hora (ISO 8601, UTC) | Obligatorio. Instante de la publicación. |
| `contenido` | objeto JSON | Obligatorio; puede estar vacío (`{}`). Lo define cada tipo. NO DEBE llevar contraseñas ni tokens (FR-002). |
| `destino` | texto | Solo en el emisor: elige la URL de `mdd.eventos.destinos.<destino>`. Se guarda con el sobre en el registro de publicaciones, para que cada reenvío sepa a dónde ir, pero no viaja en el cuerpo HTTP: `ClienteDeEventos` arma un cuerpo aparte sin él. |

## Publicación (emisor)

La guarda Spring Modulith en `eventos_<proceso>.event_publication`, con el DDL v2 de PostgreSQL de la 2.1.1. La migra `flywayEventos` (`db/migration/eventos/V1__registro_de_publicaciones.sql`).

| Columna | Uso |
|---|---|
| `id` | Identificador de la publicación (no es el `idEvento`). |
| `listener_id` | El oyente `OyenteEntreProcesos.entregar`. |
| `event_type`, `serialized_event` | Clase del sobre y el sobre en JSON. |
| `publication_date` | Momento en que se confirmó la transacción que publicó. |
| `completion_date` | Momento de la confirmación del receptor; vacío mientras no se completa. |
| `status` | `PUBLISHED`, `PROCESSING`, `COMPLETED`, `FAILED` o `RESUBMITTED`. |
| `completion_attempts`, `last_resubmission_date` | Intentos y último reenvío. |

**Estados** («pendiente» en la especificación es cualquier estado distinto de `COMPLETED`):

```text
            confirmación de la transacción
                        │
                        ▼
                   PUBLISHED ──(oyente)──► PROCESSING ──2xx──► COMPLETED
                        │                     │
            atascada > 2 min       error, plazo o atascada > 2 min
                        ▼                     ▼
                      FAILED ◄──────────── FAILED
                        │
               tarea programada (cada 30 s)
                        ▼
                   RESUBMITTED ──2xx──► COMPLETED
                        │
                error o atascada > 2 min ──► FAILED
```

- Una transacción revertida no crea la publicación (FR-001).
- No hay máximo de intentos: una publicación `FAILED` se reenvía en cada ciclo.
- Las completadas se conservan; purgarlas queda fuera del alcance.

## Evento recibido (receptor)

Tabla `evento_recibido` en el esquema de cada servicio receptor. En esta historia, `usuarios.evento_recibido` (`V3__evento_recibido.sql` de Usuarios).

| Columna | Tipo | Reglas |
|---|---|---|
| `id_evento` | `uuid` | Clave primaria: un evento se registra una sola vez (FR-007). |
| `tipo` | `text` | No nulo. |
| `origen` | `text` | No nulo. |
| `momento` | `timestamptz` | No nulo; el del sobre. |
| `recibido_en` | `timestamptz` | No nulo, `DEFAULT now()`; la primera recepción. |

- Se escribe con `INSERT … ON CONFLICT (id_evento) DO NOTHING`, en la misma transacción que el efecto del evento.
- Ninguna operación la modifica ni la borra.
- No guarda el `contenido`: el efecto de cada tipo decide qué conserva.

## Evento de prueba (Generación)

Tabla `generacion.evento_prueba` (`V2__evento_prueba.sql` de Generación). Es el cambio del servicio que se confirma en la transacción que publica (escenario 2).

| Columna | Tipo | Reglas |
|---|---|---|
| `id_evento` | `uuid` | Clave primaria; el mismo `idEvento` del sobre. |
| `momento` | `timestamptz` | No nulo. |

Contenido del evento `generacion.EventoDePrueba`: `{"nota": "evento de prueba B00-H05"}`. Es ficticio.

## Token de proceso

JWT RS256 que emite Usuarios con la misma llave y el mismo formato de ADR-014.

| Claim | Valor |
|---|---|
| `iss` | `mdd-usuarios` |
| `sub` | `proceso:<proceso>`, por ejemplo `proceso:distributivo` |
| `roles` | `["PROCESO"]` |
| `iat`, `exp` | Vigencia de `mdd.usuarios.token-proceso.vigencia`, 5 min por defecto |

**Credencial del proceso:** `mdd.usuarios.procesos.<proceso>.clave` en identidad, y `mdd.eventos.proceso` con `mdd.eventos.clave` en el emisor. Las dos salen de `MDD_CLAVE_PROCESO_<PROCESO>`. Un proceso sin clave configurada no puede pedir tokens. En el emisor, una clave vacía no impide arrancar: cada entrega falla con un WARN «sin clave de proceso» y la publicación queda `FAILED` hasta que se configure.

## Configuración nueva

| Propiedad | Dónde | Por defecto | Variable |
|---|---|---|---|
| `spring.modulith.events.jdbc.schema` | distributivo | `eventos_distributivo` | — |
| `spring.modulith.events.staleness.published`, `.processing`, `.resubmitted` | distributivo | `PT2M` | — |
| `mdd.eventos.plazo` | distributivo | `PT5S` | — |
| `mdd.eventos.reenvio.intervalo` | distributivo | `PT30S` | — |
| `mdd.eventos.destinos.usuarios` | distributivo | `http://identidad:8080/interno/usuarios/eventos` | `MDD_URI_IDENTIDAD` (base) |
| `mdd.eventos.token-proceso` | distributivo | `http://identidad:8080/interno/usuarios/token-proceso` | `MDD_URI_IDENTIDAD` (base) |
| `mdd.eventos.proceso`, `mdd.eventos.clave` | distributivo | `distributivo`, vacía | `MDD_CLAVE_PROCESO_DISTRIBUTIVO` |
| `mdd.usuarios.procesos.distributivo.clave` | identidad | vacía | `MDD_CLAVE_PROCESO_DISTRIBUTIVO` |
| `mdd.usuarios.token-proceso.vigencia` | identidad | `PT5M` | — |
| `mdd.generacion.eventos-prueba.habilitado` | distributivo | `false` | `MDD_EVENTOS_PRUEBA_HABILITADO` |
