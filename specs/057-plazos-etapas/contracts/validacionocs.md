# Contrato: los plazos en `contratos/validacionocs/openapi.yaml`

F12-H03 (#58). No hay operaciones nuevas: la revisión diaria no tiene ruta (research §3, §9). Cambian dos esquemas de respuesta.

## `EstadoDelFlujo` (existente, `consultarFlujoDelDistributivo`)

Suma dos campos opcionales. Los ve todo el que lee el estado del flujo; el permiso no cambia.

### `plazo`, `PlazoDeLaEtapa`

Presente en toda etapa salvo PUBLICADO.

| Campo | Tipo | Regla |
|---|---|---|
| `etapa` | `TipoDeEtapa` | La etapa actual |
| `llegada` | string, date-time | Cuándo llegó el distributivo a la etapa (research §2) |
| `dias` | integer, ≥ 1 | El plazo de la etapa, en días corridos |
| `fechaLimite` | string, date | La fecha de la llegada en Ecuador más `dias` |
| `diasRestantes` | integer | `fechaLimite − hoy`, con «hoy» en Ecuador según el servidor; negativo si venció |
| `estado` | string, enum `A_TIEMPO`, `VENCE_HOY`, `VENCIDO` | `diasRestantes` > 0, = 0 o < 0 |

`additionalProperties: false`.

### `etapas`, lista de `EtapaDelDistributivo` (nuevo)

`EtapaDelFlujo`, de `ConfiguracionDelFlujo`, exige además `esqueleto`, `activa`, `rol` y `admiteDevolucion`, que la tarjeta no necesita: un esquema propio evita mostrar los roles a todo el que lee el flujo.

Las etapas **activas** de la configuración del distributivo, en orden.

| Campo | Tipo | Regla |
|---|---|---|
| `etapa` | `TipoDeEtapa` | |
| `nombre` | string | El nombre en la configuración |
| `plazoDias` | integer, ≥ 1 | Ausente en PUBLICADO |
| `actual` | boolean | Si es la etapa actual |

`additionalProperties: false`.

## `ConfiguracionDelFlujo` (existente, `consultarConfiguracionDelFlujo` y `cargarConfiguracionDelFlujo`)

Suma `advertencias`, una lista de `AdvertenciaDelFlujo`, con 0 o 1 elementos.
- **En la carga:** la advertencia calculada.
- **En la consulta:** la que se guardó con la carga vigente.
- **Con la configuración por defecto:** siempre vacía.

| Campo | Tipo | Regla |
|---|---|---|
| `codigo` | string, enum `RN23_MARGEN_INSUFICIENTE`, `RN23_SIN_NORMATIVA` | Estable; no es un código del catálogo de errores (research §7) |
| `mensaje` | string | En español, con qué hacer. Por ejemplo: «Con estos plazos, el distributivo se aprobaría el 2027-03-18, después del 2027-03-01, que es 30 días antes del inicio del periodo (RN-23). Acorta plazos o desactiva etapas opcionales.» |
| `ultimoPlazo` | string, date | La fecha de la carga más la suma de los plazos hasta APROBADO |
| `inicioDelPeriodo` | string, date | |
| `diasAntesDelInicio` | integer, ≥ 0 | Ausente con `RN23_SIN_NORMATIVA` |
| `fechaDeRemision` | string, date | Ausente con `RN23_SIN_NORMATIVA` |
| `diasDeMargen` | integer | Negativo; ausente con `RN23_SIN_NORMATIVA` |

`additionalProperties: false`. La carga con advertencia responde **200**: la configuración queda vigente (FR-013).

Respuestas nuevas de `cargarConfiguracionDelFlujo`: el **502** y el **504** comunes, también si Normativa no responde (antes, solo si fallaba Académico o Roles). No hay códigos nuevos en el catálogo.

## La bitácora

Suma la acción `PLAZO_VENCIDO`, de `validacionocs`, por `sistema`:
- **objeto:** el periodo;
- **detalle:** `{periodo, etapa}`;
- **nuevo:** `{fechaLimite, diasDeAtraso}`.

`CONFIGURACION_FLUJO_CARGADA` suma `advertencia` (el código) en `nuevo` cuando la hay.

## Las notificaciones

El evento es `validacionocs.NotificacionesSolicitadas`, sin cambios en `contratos/notificaciones`:
- `autor: "sistema"`; `momento` es cuándo se detectó el vencimiento.
- Un mensaje por destino (research §4): el rol de la etapa, por carrera en BORRADOR_CARRERA, y el rol de EN_REVISION_VRA.
- Título: «Venció el plazo de <nombre de la etapa>». Enlace: el de la etapa, o `/consolidado` para el Vicerrector.

## Configuración del proceso

`application.yaml` de `distributivo`:

```yaml
# La revisión diaria de plazos (F12-H03, D-114): un cron de Spring en hora de Ecuador; además corre
# al arrancar.
mdd.validacionocs.plazos.revision: ${MDD_REVISION_DE_PLAZOS:0 0 7 * * *}
```

## Pruebas de contrato

- `FlujoContratoTest` (existente): `plazo` en los tres estados, ausente en PUBLICADO; `etapas` con la actual marcada.
- `ConfiguracionContratoTest` (existente):
  - la carga con `RN23_MARGEN_INSUFICIENTE` responde 200 y la consulta la devuelve;
  - `RN23_SIN_NORMATIVA`;
  - sin advertencia con margen;
  - el 504 si Normativa no responde.
- `PermisosValidacionocsTest`: sin cambios, porque no hay operaciones nuevas.
