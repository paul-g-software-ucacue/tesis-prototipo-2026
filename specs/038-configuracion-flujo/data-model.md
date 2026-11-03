# Data Model: Cargar la configuración del flujo y del árbol de firmas

## Dominio

### `TipoDeEtapa`

`BORRADOR_CARRERA`, `BORRADOR_UNIDADES`, `EN_CONSOLIDACION`, `EN_REVISION_VRA`, `CONGELADO_EN_FIRMA`, `EN_OCS`, `REMITIDO_ZONAL`, `APROBADO` y `PUBLICADO`, en ese orden. Cada uno sabe si es del esqueleto.

### `Etapa`

`tipo`, `activa`, `nombre`, `rol`, `plazoDias` (vacío en PUBLICADO), `admiteDevolucion`.

### `PasoFirma`

`orden`, `etiqueta`, `rol`, `etapa`, `documento`.

### `ConfiguracionFlujo`

Las 9 etapas y sus firmas. Reglas:

| Regla | Problema |
|---|---|
| Falta una etapa | «Falta la etapa EN_OCS.» |
| Etapa desconocida | «La etapa X no existe.» |
| Esqueleto desactivado | «La etapa EN_OCS no se puede desactivar.» |
| Etapa sin nombre o sin rol, aunque esté desactivada (FR-002) | Dice cuál y qué falta |
| Etapa activa sin plazo, salvo PUBLICADO | Dice cuál |
| Nombre de más de 80 caracteres o etiqueta de más de 40 | Dice cuál |
| Plazo que no es un entero de 1 a 9999 | Dice la etapa |
| Rol desconocido | Dice dónde |
| Sin firmas | «Falta el árbol de firmas.» |
| Orden repetido, o con huecos desde 1 | Dice el orden |
| Firma en una etapa inactiva | «La firma Elaborado está en EN_OCS, que no está activa.» |
| Documento desconocido | Dice la firma |
| Clave desconocida o repetida (la dice el lector, con su línea) | Dice la clave |

Documentos: `CONSOLIDADO`, `PARCIAL_CARRERA` y `RESOLUCION_OCS` (RF-58). El prototipo firma el consolidado.

### `Firmantes`

`rolesConUsuario`, `responsableDesignado`. Con ellos: «El rol RECTOR no tiene usuarios.» y «Falta designar al Responsable de consolidación del periodo.»

### `ConfiguracionVigente`

`periodo`, `configuracion`, `carga` (vacía si rige la de por defecto) y `modificable`. `CargaGuardada` lleva `numero`, `contenido`, `por` y `en`.

## Persistencia (`V2`)

### `configuracion_flujo`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | uuid | clave |
| `periodo` | varchar(20) | sin referencia: Académico es otro esquema |
| `contenido` | text | el archivo tal como se subió |
| `cargada_por` | varchar(64) | |
| `cargada_en` | timestamptz | |

Solo de inserción. Índice por `(periodo, cargada_en desc)`.
