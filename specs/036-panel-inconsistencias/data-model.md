# Data Model: Ver el panel de inconsistencias globales

## Generación · dominio

### `TipoDeInconsistencia`

| Tipo | Regla | Objeto | Datos |
|---|---|---|---|
| `DOCENTE_SIN_CLASE` | RN-17 | docente | — |
| `PARALELO_SIN_PROPUESTA` | RF-24 | asignatura-paralelo | horas |
| `COORDINADORES_DE_LA_CARRERA` | RN-12 | carrera | cuántos hay |
| `GESTORES_FUERA_DE_RANGO` | RN-10 | periodo | número, mínimo y máximo |
| `HORAS_SOBRE_LA_MALLA` | RN-18 | carrera | los paralelos, con sus horas y las de la malla |
| `SOBRECARGA` | RN-02 | docente | horas y límite |
| `PROPORCION` | RN-05 | docente | porcentajes, meta y margen |
| `ACTIVIDADES_OBLIGATORIAS` | RN-05 | docente | las actividades que faltan |

Además, `EXCEPCION_DE_AFINIDAD` (RN-19), solo como justificada, leída de la propuesta.

### Quién corrige cada una

| Tipo | Quién corrige | Pantalla | Indicación |
|---|---|---|---|
| `DOCENTE_SIN_CLASE` | Coordinador de la carrera | `OFERTA` | Proponle al menos un paralelo. |
| `PARALELO_SIN_PROPUESTA` | Coordinador de la carrera | `OFERTA` | Propón un docente para este paralelo. |
| `COORDINADORES_DE_LA_CARRERA` | Vicerrectorado | `CARGOS` | Deja un solo coordinador vigente en esta carrera. |
| `GESTORES_FUERA_DE_RANGO` | Vicerrectorado | `CARGOS` | Revisa los gestores designados, o justifica el número. |
| `HORAS_SOBRE_LA_MALLA` | Vicerrectorado | `OFERTA` | Baja las horas de estos paralelos a las de su asignatura. |
| `SOBRECARGA` | Coordinador de la carrera | `ACTIVIDADES` | Baja sus horas hasta su límite. |
| `PROPORCION` | Coordinador de la carrera | `ACTIVIDADES` | Ajusta sus horas, o justifica la diferencia. |
| `ACTIVIDADES_OBLIGATORIAS` | Coordinador de la carrera | `ACTIVIDADES` | Asígnale la actividad que falta, o justifica. |
| `EXCEPCION_DE_AFINIDAD` | — | `OFERTA` | Ya está justificada en su propuesta. |

Las indicaciones son redacción inicial y se ajustan en T016. Los cargos también los puede corregir el Operador de datos: el texto nombra al Vicerrectorado para no dar dos responsables.

### `InconsistenciaGlobal`

`tipo`, `severidad`, `objeto`, `datos`. `regla()` sale del tipo.

### `ObjetoAfectado`

| Clase | Clave | Ejemplo |
|---|---|---|
| `DOCENTE` | identificación | `0109990010` |
| `PARALELO` | `asignatura/paralelo` | `POO-204/A` |
| `CARRERA` | código | `IST-023` |
| `PERIODO` | código | `2026-I` |

También lleva el nombre que se muestra.

### `ReglasDelPanel`

Severidad por regla, `horasMaximasDocente`, `metaClase`, `metaOtrasDocencia`, `margenPuntos`, `gestoresMinimo`, `gestoresMaximo`, `actividadesObligatorias`.

Sin una severidad o un parámetro, no se construye (principio VI).

### `CargoVigente`

`docente`, `tipo`, `carrera` opcional.

### Reglas de `MotorReglas.evaluarVersion`

| Comprobación | Incumple si |
|---|---|
| RN-17 | un docente activo tiene 0 h de clase en paralelos no retirados |
| RF-24 | un paralelo no retirado no tiene propuesta |
| RN-12 | una carrera con oferta no retirada no tiene exactamente 1 coordinador vigente |
| RN-10 | los cargos G06 a G21 vigentes están fuera del rango |
| RN-18 | un paralelo no retirado tiene horas sobre las de su asignatura |
| Sobrecarga | el total de un docente activo pasa su límite |
| RN-05, porcentajes | `|clase − meta| > margen`, entre docentes con horas de clase u otras de docencia |
| RN-05, actividades | un docente con clase no tiene todas las obligatorias |

## Generación · persistencia (`V5`)

### `justificacion_inconsistencia`

| Columna | Tipo | Regla |
|---|---|---|
| `id` | uuid | clave |
| `version_id` | uuid | referencia a la versión, en cascada |
| `tipo` | varchar(30) | uno de los tipos justificables |
| `objeto` | varchar(60) | clave del objeto |
| `texto` | varchar(500) | 10 a 500 caracteres |
| `justificada_por` | varchar(64) | |
| `justificada_en` | timestamptz | |

Solo de inserción. Índice por `(version_id, tipo, objeto, justificada_en desc)`.

`registro_cambio.accion`: pasa a `varchar(30)` y admite `INCONSISTENCIA_JUSTIFICADA`, con tipo, objeto y versión, sin el texto.

## Normativa (`V6`)

- `parametro_regla`: `('RN-05', 'margen_puntos', 5)` en cada versión.
- `actividad_catalogo.obligatoria boolean not null default false`, `true` en `140-b` y `140-g`.
- `registro_cambio` de Normativa: una fila por versión, como D-68.

## Estados

```text
Abierta ──justificar (blanda)──► Justificada
   ▲                                  │
   └──────── la dura nunca ───────────┘ (una justificación no cierra una dura)
Corregida: deja de existir, porque se calcula.
```
