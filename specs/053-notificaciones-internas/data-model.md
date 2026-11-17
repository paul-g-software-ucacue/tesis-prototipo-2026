# Data Model: Recibir notificaciones internas por cada cambio de estado

F16-H01 (#67). Lo nuevo y lo que cambia. Lo que no se menciona no cambia.

## Validación OCS (proceso `distributivo`)

### `DestinoDeNotificacion` (dominio, nuevo)

A quién toca un mensaje. Es un record con dos campos:
- `rol`: el identificador de uno de los diez roles de D-63.
- `carrera`: `Optional<String>`; solo con `COORDINADOR_CARRERA` en una devolución.

### `ProcesoAprobacion.destinoDe(Transicion)` (dominio, nuevo)

Devuelve `List<DestinoDeNotificacion>`: un destino por mensaje.

| Tipo de transición | Destinos |
|---|---|
| `ENVIO_PARCIAL` | El rol de `siguienteActiva(BORRADOR_CARRERA)` |
| `PASO_DE_ETAPA` | El rol de la etapa `hacia` |
| `DEVOLUCION` | `COORDINADOR_CARRERA`, uno por carrera de `observaciones` |
| `RECHAZO_EN_FIRMA` | El rol de la etapa `hacia` |
| `VERSION_NUEVA` | El rol de la etapa `hacia`, la misma de la que partió |

El rol sale de `EtapaDelProceso`, la copia de la configuración del periodo (principio VI).

### `MensajesDeTransicion` (dominio, nuevo)

Arma, por destino, el título, el detalle y el enlace de [contracts/eventos.md](contracts/eventos.md):
- con el nombre de la etapa de la copia de la configuración, la versión, la carrera, la observación o el rechazo;
- el título tiene de 1 a 200 caracteres y el detalle de 0 a 1 000;
- el enlace sale de una tabla fija por etapa de llegada.

### `NotificacionDeTransiciones` (puerto de salida, nuevo)

```java
@FunctionalInterface
interface NotificacionDeTransiciones {
    /** En la transacción en curso; con {@code nuevas} vacía no hace nada. */
    void notificar(ProcesoAprobacion proceso, List<Transicion> nuevas);
}
```

`PublicadorDeNotificaciones` lo implementa. Publica un `EventoEntreProcesos` por transición: tipo `validacionocs.NotificacionesSolicitadas`, destino `notificaciones`.

## Roles (proceso `identidad`)

### `ConsultarDestinatarios` (puerto de entrada, nuevo)

```java
interface ConsultarDestinatarios {
    /**
     * @param rol uno de los diez roles; uno desconocido da una lista vacía
     * @param periodo el código del periodo, para el Responsable de consolidación
     * @param carrera con COORDINADOR_CARRERA: solo los coordinadores de esa carrera
     * @return los usuarios con su carrera, ordenados por usuario
     */
    List<Destinatario> destinatarios(String rol, String periodo, Optional<String> carrera);
}
record Destinatario(String usuario, Optional<String> carrera) {}
```

| Rol | Resultado |
|---|---|
| `RESPONSABLE_CONSOLIDACION` | Quienes tienen el rol asignado y, además, el designado del periodo, una sola vez |
| `COORDINADOR_CARRERA` con carrera | Los usuarios con ese rol cuya carrera indicada es esa |
| `COORDINADOR_CARRERA` sin carrera, o cualquier otro rol | Todos los usuarios con el rol |

Solo lee: no cambia nada ni deja entrada en el registro de cambios.

## Notificaciones (proceso `identidad`)

### Tabla `notificaciones.evento_recibido` (V2)

| Columna | Tipo | Regla |
|---|---|---|
| `id_evento` | uuid | PK |
| `tipo` | varchar(120) | no nulo |
| `origen` | varchar(40) | no nulo |
| `recibido_en` | timestamptz | no nulo |

Es el mismo patrón de `usuarios.evento_recibido` (ADR-018 §4).

### Tabla `notificaciones.notificacion` (V2)

| Columna | Tipo | Regla |
|---|---|---|
| `id` | uuid | PK, la genera el servicio |
| `id_evento` | uuid | FK a `evento_recibido` |
| `destinatario` | varchar(60) | no nulo: el usuario |
| `carrera` | varchar(20) | nulo salvo en una devolución |
| `periodo` | varchar(20) | no nulo |
| `titulo` | varchar(200) | no nulo, `btrim` de 1 a 200 |
| `detalle` | varchar(1000) | no nulo; puede estar vacío |
| `enlace` | varchar(120) | no nulo, `^/([a-z0-9-]+(/[a-z0-9-]+)*)?$` |
| `autor` | varchar(60) | no nulo: el usuario, o `sistema` |
| `autor_nombre` | varchar(120) | no nulo: el nombre para mostrar al recibir, o «el sistema» |
| `momento` | timestamptz | no nulo: cuándo ocurrió el hecho |
| `recibida_en` | timestamptz | no nulo |
| `leida_en` | timestamptz | nulo hasta que se lee |

- **Única** por `(id_evento, destinatario, coalesce(carrera, ''))`: FR-012 y el edge case de dos carreras.
- **Índice** por `(destinatario, momento desc)`, y un índice parcial por `destinatario` con `leida_en is null`, para el número sin leer.
- **Disparador `BEFORE UPDATE`:** rechaza cualquier cambio que no sea `leida_en` de nulo a no nulo.
- **Disparadores `BEFORE DELETE` y `BEFORE TRUNCATE`:** los rechazan (FR-009).
- **`REVOKE DELETE, TRUNCATE`** al rol del proceso `identidad`.

### `Notificacion` (dominio, nuevo)

- Los campos de la tabla.
- `marcarLeida(Instant)`: devuelve la misma si ya estaba leída (FR-008); si no, la devuelve con `leidaEn`.

### `SolicitudDeNotificaciones` y `MensajeSolicitado` (dominio, nuevos)

El contenido del evento, validado. Si algo falla, lanza `NotificacionInvalida` (400):

- `periodo` cumple el patrón de periodo.
- `autor` cumple el patrón de usuario.
- `momento` es un instante.
- `mensajes` lleva de 1 a 50.
- Cada mensaje cumple:
  - `destino.rol` es uno de los diez roles;
  - `destino.carrera`, si va, cumple el patrón de carrera;
  - el título, el detalle y el enlace, como en la tabla.
- Ninguna clave parece un secreto: es la regla de la bitácora (F17-H01 research §5).
- El evento completo pesa hasta 64 KB.

### Recepción (`RecibirNotificacionesServicio`)

Todo ocurre en una sola transacción de Notificaciones:

1. Registra `id_evento` con `ON CONFLICT DO NOTHING`. Si ya estaba, termina con 204.
2. Valida la solicitud.
3. Por cada mensaje:
   1. pide los destinatarios a Roles;
   2. deja solo los activos, con `ListarUsuarios`;
   3. quita al `autor`;
   4. inserta una fila por destinatario con `ON CONFLICT DO NOTHING`.
4. Resuelve `autor_nombre`.

Si Roles o Usuarios fallan, la transacción se revierte, la respuesta es 500 y el emisor reenvía.

### Estados de una notificación

```text
SIN_LEER (leida_en nulo) ── abrir ──> LEIDA (leida_en = momento de la primera lectura)
LEIDA ── abrir ──> LEIDA (sin cambios)
```

## SPA

`navegacion/notificaciones.ts` tiene `consultarSinLeer()`, `consultarBandeja()` y `marcarLeida(id)`, con los tipos generados de `contratos/notificaciones/openapi.yaml` (`generar:api`).
