# Data Model: Asignar actividades no lectivas

## Normativa · `normativa.actividad_catalogo` (migración V5)

| Campo | Tipo | Regla |
|---|---|---|
| `version_id` | uuid | Llave foránea a `version_normativa`. Parte de la llave primaria. |
| `codigo` | varchar(6) | `^14[0-3]-[a-p]$`. Parte de la llave primaria. |
| `orden` | int | El orden de la norma. |
| `componente` | varchar(14) | `OTRAS_DOCENCIA`, `INVESTIGACION`, `VINCULACION` o `GESTION`. |
| `nombre` | varchar(500) | El texto literal. |
| `tipos_de_proyecto` | text[] | Vacía si no exige proyecto. |
| `cargos` | text[] | Vacía si no exige cargo. |

- Una versión nueva copia las filas de la anterior.
- Una actividad no exige proyecto y cargo a la vez: `check`.

## Generación · dominio

- **`Componente`**: `CLASE`, `OTRAS_DOCENCIA`, `INVESTIGACION`, `VINCULACION`, `GESTION`.
- **`ActividadDelCatalogo`** (objeto de valor): `codigo`, `nombre`, `componente`, `tiposDeProyecto`, `cargos`. Con `exigir(proyecto, proyectosYCargos)` y `disponibilidad(proyectosYCargos)`.
- **`ProyectosYCargosDelDocente`** (objeto de valor): sus `proyectos` y sus `cargos`.
- **`ActividadNoLectiva`** (entidad de `VersionDistributivo`): `carrera`, `docente`, `nombreDocente`, `actividad`, `nombreActividad`, `componente`, `proyecto`, `nombreProyecto`, `horas`, `asignadaEn`, `asignadaPor`.
- **`VersionDistributivo`** suma `actividades` y `actividadDe(carrera, docente, actividad, proyecto)`.
- **`HorasDelPeriodo.calcular`** recibe también las actividades.
- **`SumaPorComponente`**: las horas de un docente en cada componente, pura.

## Generación · `generacion.actividad_no_lectiva` (migración V4)

| Campo | Tipo | Regla |
|---|---|---|
| `id` | uuid | Llave primaria. |
| `version_id`, `carrera` | | Llave foránea a `parcial_carrera`. |
| `docente` | varchar(13) | |
| `nombre_docente` | varchar(200) | |
| `actividad` | varchar(6) | Sin llave foránea: es de otro esquema (principio V). |
| `nombre_actividad` | varchar(500) | |
| `componente` | varchar(14) | `check` con los 4 valores. |
| `proyecto` | varchar(30) | Opcional. |
| `nombre_proyecto` | varchar(200) | Opcional, y con `proyecto`: `check`. |
| `horas` | numeric(5,2) | `check (horas > 0 and horas <= 40)`. |
| `asignada_en` | timestamptz | |
| `asignada_por` | varchar(64) | |

- Índice único: `(version_id, carrera, docente, actividad, coalesce(proyecto, ''))`.
- Índice para las horas: `(version_id, docente)`.
- `registro_cambio.accion` suma `ACTIVIDAD_GUARDADA` y `ACTIVIDAD_QUITADA`.

## Estados

Una actividad no tiene estados: existe o se quita. El parcial y la versión siguen las reglas de F06-H03.
