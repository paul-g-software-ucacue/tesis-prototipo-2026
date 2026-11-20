# Data Model: Iniciar una reforma de la versión publicada

F18-H02 (#72). Dos migraciones pequeñas: Validación OCS V8 (el tipo `REFORMA` y su tabla) y Generación V9 (el motivo `REFORMA`). La notificación es la de F16-H01.

## Validación OCS

### `ProcesoAprobacion` (agregado, existente)

| Miembro | Regla |
|---|---|
| `validarReforma(quien)` | Sin cambiar nada: exige `estado == PUBLICADO` (si no, `EtapaNoAdmiteReforma`) y el rol de la etapa EN_REVISION_VRA de la copia de la configuración (si no, `RolDeOtraEtapa`). Lo usa el caso de uso antes de pedir la versión a Generación |
| `iniciarReforma(quien, motivo, nueva, hash, ahora)` | Las mismas reglas. Registra una transición `REFORMA` de PUBLICADO a EN_CONSOLIDACION (o la siguiente etapa activa desde ella), con la versión `nueva`, su `hash`, `quien`, `ahora` y la `Reforma` (versión publicada y motivo). Devuelve la transición |
| `reformaEnCurso()` | La última `REFORMA`, si no hay una transición a PUBLICADO después |

Cambios en lo que ya existe:
- **`acciones(quien)`:** en PUBLICADO, `[INICIAR_REFORMA]` a quien tiene el rol de EN_REVISION_VRA; ninguna a los demás.
- **`destinoDe(transicion)`:** para la `REFORMA`, el rol de la etapa de llegada y DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, sin carrera.
- **`publicacion()`** no cambia: la última transición a PUBLICADO es la versión publicada vigente.

### `Reforma` (dominio, nuevo)

| Campo | Regla |
|---|---|
| `versionPublicada` | ≥ 1; la versión que estaba publicada |
| `motivo` | `Observacion`: de 10 a 500 caracteres sin los espacios de los extremos |

### `Transicion` (existente)

Suma `Optional<Reforma> reforma`, presente solo si el tipo es `REFORMA`, como `rechazo` con `RECHAZO_EN_FIRMA`.

### `TipoDeTransicion`, `AccionDelProceso` (existentes)

Suman `REFORMA` e `INICIAR_REFORMA`.

### Errores (dominio, nuevos)

- `EtapaNoAdmiteReforma` → `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA` (409).
- `MotivoDeReformaInvalido` → `VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO` (400).

### `MensajesDeTransicion` (existente)

Para la `REFORMA`, un mensaje por destino:

| Campo | Valor |
|---|---|
| `titulo` | «Se inició una reforma del distributivo de {periodo}» |
| `detalle` | «Versión {nueva}, reforma de la versión {publicada}. Motivo: «{motivo}». La versión {publicada} sigue vigente hasta que se publique la reforma.» |
| `enlace` | El de la etapa de llegada para su rol (`/consolidado`); `/oferta` para COORDINADOR_CARRERA; `/` para DOCENTE y OPERADOR_DATOS |

### `CambioDeFlujo.Accion` (existente)

Suma `REFORMA_INICIADA`: cambia, con `{estado: PUBLICADO}` antes y `{estado: <llegada>}` después, y `periodo`, `versionPublicada`, `version`, `hash` y `motivo` en el detalle.

### `FlujoDelDistributivo` (puerto de entrada, existente)

- Suma `reforma`, opcional, mientras esté en curso: la versión publicada, la nueva, el motivo, quién y cuándo.
- `publicacion` sigue siendo la última publicación: durante la reforma, la anterior.
- La resolución se muestra solo si es de la versión de trabajo, como la congelación.

### Puerto de salida `Distributivo` (existente)

`crearVersionNueva(periodo, version, motivo, momento)`: suma el motivo, `RECHAZO_EN_FIRMA` o `REFORMA`, y quién la crea.

### Migración V8

| Cambio | Detalle |
|---|---|
| `transicion.tipo` | El `check` suma `REFORMA` |
| `reforma` (tabla nueva) | `transicion_id` (clave, foránea a `transicion`, en cascada), `periodo`, `version_publicada` (≥ 1), `motivo` (`varchar(500)`, de 10 a 500 sin los extremos) |

## Generación

### `MotivoDeVersion` (interfaz publicada, existente)

Suma `REFORMA`.

### `crearVersionNueva` (caso de uso, existente)

| Motivo | La anterior | La nueva | Idempotente si |
|---|---|---|---|
| `RECHAZO_EN_FIRMA` | ANULADA (como hoy) | EDITABLE, copia todo | la vigente es la siguiente y la anterior está ANULADA |
| `REFORMA` | Sigue CONGELADA | EDITABLE, copia todo | la vigente es la siguiente, con motivo `REFORMA`, y la anterior sigue CONGELADA |

Quien la crea: `sistema` en el rechazo; quien inició la reforma en la reforma.

### Migración V9

El `check` de `version_distributivo.motivo` suma `REFORMA`.

## Lo que no cambia

- **Firma:** la versión nueva tiene su documento al aprobarse (D-101), con su cadena desde F1. La publicada anterior conserva su documento y sus firmas; nada se anula.
- **La resolución:** única por versión (D-107), así que la versión nueva lleva la suya.
- **Notificaciones:** los destinos por rol de F16-H01; la clave única deja una notificación por usuario.
- **Roles:** la matriz D-16 ya da `DISTRIBUTIVO:APROBACION` al Vicerrector.

## SPA

- **`EstadoDelFlujo`:** la acción `INICIAR_REFORMA` con su diálogo (el motivo) y la constancia de la reforma en curso; la publicación anterior, «vigente hasta que se publique la reforma» ([contracts/spa.md](contracts/spa.md)).
