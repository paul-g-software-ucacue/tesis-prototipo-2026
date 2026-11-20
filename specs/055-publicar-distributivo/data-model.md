# Data Model: Publicar el distributivo aprobado

F18-H01 (#71). No hay tablas ni migraciones nuevas: la publicación es una transición de Aprobación, que ya se guarda en `validacionocs.transicion`, y la notificación es la de F16-H01.

## Validación OCS

### `ProcesoAprobacion` (agregado, existente)

Suma una operación y una consulta:

| Miembro | Regla |
|---|---|
| `publicar(quien, ahora)` | Exige `estado == APROBADO` (si no, `EtapaNoAdmitePublicacion`) y el rol de la etapa APROBADO de la copia de la configuración (si no, `RolDeOtraEtapa`, el 403 común). Pasa a PUBLICADO y devuelve una transición `PASO_DE_ETAPA` de APROBADO a PUBLICADO, con `quien`, `ahora`, y la versión y el hash de la congelación, como `registrarResolucion` |
| `publicacion()` | La transición a PUBLICADO, si existe |

Cambios en lo que ya existe:
- **`acciones(quien)`:** en APROBADO, con el rol de esa etapa, `[PUBLICAR]`. En PUBLICADO, ninguna, para nadie.
- **`destinoDe(transicion)`:** para el paso a PUBLICADO, tres destinos por rol, sin carrera: DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA (research §3).

### `AccionDelProceso` (existente)

Suma `PUBLICAR`.

### `EtapaNoAdmitePublicacion` (dominio, nueva)

`VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION` (409): publicar con el distributivo fuera de APROBADO.

### `MensajesDeTransicion` (existente)

Para el paso a PUBLICADO, un mensaje por destino:

| Campo | Valor |
|---|---|
| `titulo` | «El distributivo de {periodo} fue publicado» |
| `detalle` | «Versión {v}. Es la versión oficial del periodo.» |
| `enlace` | `/oferta` para COORDINADOR_CARRERA; `/` para DOCENTE y OPERADOR_DATOS (el de PUBLICADO deja de ser `/resolucion-ocs`) |

### `CambioDeFlujo.Accion` (existente)

Suma `DISTRIBUTIVO_PUBLICADO`: cambia, con `{estado: APROBADO}` antes y `{estado: PUBLICADO}` después, y `periodo`, `version` y `hash` en el detalle.

### `FlujoDelDistributivo` (puerto de entrada, existente)

Suma `publicacion`, opcional: la versión, quién y cuándo, de `ProcesoAprobacion.publicacion()`. La ve todo el que lee el estado del flujo.

## Lo que no cambia

- **Generación:** la versión publicada es la congelada que llegó a APROBADO, ya inmutable (D-100). No suma un estado (research §2).
- **Notificaciones:** los destinos por rol de F16-H01; la clave única por evento, destinatario y carrera deja una sola notificación a quien tiene dos de los roles.
- **Roles:** la matriz D-16 ya da `PUBLICACION:APROBACION` a la Secretaría general académica.

## SPA

- **`EstadoDelFlujo`:** la acción `PUBLICAR` con su diálogo, y la constancia de la publicación ([contracts/spa.md](contracts/spa.md)).
- **«Consolidado del periodo» y el panel de inconsistencias:** el aviso dice «publicada» con el flujo en PUBLICADO.
