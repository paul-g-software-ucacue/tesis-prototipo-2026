# Contrato: la versión nueva de una reforma, en la interfaz publicada de Generación

F18-H02 (#72). Es la interfaz entre módulos que Aprobación usa (MD-04): no hay operación HTTP nueva.

## `MotivoDeVersion` (existente)

Suma `REFORMA`. Su `check` en `generacion.version_distributivo` también (V9).

## `crearVersionNueva(periodo, version, motivo, quien, momento)` (existente)

Suma `quien`: `sistema` en el rechazo en firma, como hoy; quien inició la reforma, en la reforma.

| Motivo | `version` | Si la vigente es `version` y está CONGELADA | Si la vigente es `version + 1` (reintento) | Si no |
|---|---|---|---|---|
| `RECHAZO_EN_FIRMA` | La rechazada | La pasa a ANULADA y crea la siguiente | La devuelve si `version` está ANULADA | Vacío |
| `REFORMA` | La publicada | La deja CONGELADA y crea la siguiente | La devuelve si su motivo es `REFORMA` y `version` sigue CONGELADA | Vacío |

En los dos casos, la nueva es EDITABLE y copia, con la versión bloqueada y en una transacción, los parciales con su estado, las propuestas, las actividades, las justificaciones, la consolidación y el informe (D-106). Devuelve el número y el hash de la nueva.

## Pruebas

- `ParcialesDelDistributivoServicioTest` (+3): la reforma deja la anterior CONGELADA, el reintento devuelve la misma, y una versión que no es la vigente da vacío.
- `VersionNuevaIntegrationTest` (+2), en PostgreSQL: la copia de la reforma, con la anterior CONGELADA y el motivo `REFORMA`, y dos pedidos a la vez dejan una sola versión.
