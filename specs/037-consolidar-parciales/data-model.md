# Data Model: Consolidar los parciales con el informe sustentado

## Generación · dominio

### `ArchivoPdf`

`nombre`, `contenido`, `huella` (SHA-256 en hexadecimal). Se construye con `ArchivoPdf.de`:

| Regla | Error |
|---|---|
| Vacío, sin `%PDF-` al inicio o sin `%%EOF` al final | `InformeNoEsPdf` |
| Más de 10 MB | `InformeDemasiadoGrande` |

El nombre se limpia para mostrarlo: sin ruta, sin caracteres de control, hasta 150 caracteres.

### `InformeSustentado`

`nombre`, `tamano`, `huella`, `clave`, `por`, `en`. El vigente es el último de la versión.

### `Consolidacion`

`por`, `en`. La vigente es la última de la versión.

### `ConsolidadoDelPeriodo`

| Campo | Qué es |
|---|---|
| `periodo`, `modificable` | |
| `numero` | la versión; ausente sin distributivo |
| `consolidacion` | la última; vacía si no se consolidó |
| `desactualizado` | hay un cambio de asignaciones posterior |
| `carreras` | carrera, estado, avance y horas por componente |
| `totales` | horas por componente de toda la versión |
| `abiertas` | duras y blandas, del panel |
| `informe` | el vigente; vacío si no hay |

`desactualizado`: `consolidacion` presente y `ultimoCambio` posterior a ella. Sin consolidar, siempre falso.

## Generación · persistencia (`V6`)

### `consolidacion`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | uuid | clave |
| `version_id` | uuid | referencia a la versión, en cascada |
| `consolidada_por` | varchar(64) | |
| `consolidada_en` | timestamptz | |

### `informe_sustentado`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | uuid | clave |
| `version_id` | uuid | referencia a la versión, en cascada |
| `nombre` | varchar(150) | para mostrar |
| `tamano` | integer | 1 a 10 485 760 |
| `huella` | char(64) | SHA-256 |
| `clave` | varchar(36) | la del almacén |
| `adjuntado_por` | varchar(64) | |
| `adjuntado_en` | timestamptz | |

Las dos son solo de inserción, con índice por `(version_id, fecha desc)`.

`registro_cambio.accion` admite `DISTRIBUTIVO_CONSOLIDADO` e `INFORME_ADJUNTADO`.

## Almacén

`<raíz>/<clave>`, sin subcarpetas. La raíz sale de `mdd.archivos.raiz`, con `/var/mdd/archivos` en los contenedores.
