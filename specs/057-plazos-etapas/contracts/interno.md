# Contrato: las rutas internas de Académico y de Normativa

F12-H03 (#58). Las dos viven en el proceso `datos`. Solo se llaman entre procesos, con el token de proceso (ADR-023), y el API Gateway no las enruta.

## Académico: `consultarEstadoDelPeriodo` (existente)

`GET /interno/academico/periodos/{codigo}/estado`. El esquema `EstadoDelPeriodo` suma `inicio`:

| Campo | Tipo | Regla |
|---|---|---|
| `periodo` | string | Existente |
| `estado` | `EstadoPeriodo` | Existente |
| `inicio` | string, date | **Nuevo.** El primer día del periodo, el de `PeriodoAcademico` |

`required: [periodo, estado, inicio]`.
- Es un cambio compatible: el cliente de Validación OCS lee `inicio` como opcional, para tolerar un `datos` anterior durante el despliegue (D-55).
- Ningún otro consumidor lo usa hoy.
- `inicio` no es un dato personal (principio IX).

**Pruebas:** `InternoAcademicoContratoTest`, con el `inicio` del periodo.

## Normativa: `consultarRemisionDelPeriodo` (nueva)

`GET /interno/normativa/periodos/{periodo}/remision`.

- **Descripción:** «Solo entre procesos, con el token de proceso: lo usa Aprobación para la advertencia de RN-23 al cargar la configuración del flujo (F12-H03, D-114, ADR-023). Sale de la versión que rige el periodo. Un periodo sin versión responde 404.»
- **200**, el esquema `RemisionDelPeriodo`:

| Campo | Tipo | Regla |
|---|---|---|
| `version` | integer, ≥ 1 | La versión de normativa que rige el periodo |
| `diasAntesDelInicio` | integer, ≥ 0 | `RN-23.dias_antes_del_inicio` de esa versión |

`additionalProperties: false`.

- **Errores:** 400, 401, 403 y 500, los comunes. El 404 es `NoEncontrada`, la respuesta que ya usan las demás rutas internas de Normativa para un periodo sin versión.

**El cliente,** `DatosDesdeProcesos.diasDeRemision`, usa `ConsultasEntreProcesos`:
- un 404 es un resultado vacío (`RN23_SIN_NORMATIVA`);
- un error o un plazo vencido de la consulta, el 502 o el 504 común.

**Pruebas:**
- `InternoNormativaContratoTest`, con versión y sin versión;
- de la migración V7: cada versión tiene RN-23 con 30, y una versión nueva lo copia.
