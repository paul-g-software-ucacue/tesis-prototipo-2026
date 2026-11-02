# Data Model: Justificar una asignación con una regla blanda

Sin tablas nuevas ni migraciones.

## Generación · dominio

### `ReglasDelPeriodo`

Agrega `severidadAfinidad`: `DURA` o `BLANDA`, de RN-19.

### `ReglaDeAsignacion`

Agrega `RN_19`.

### `Inconsistencia`

| Campo | Tipo | Regla |
|---|---|---|
| `regla` | `ReglaDeAsignacion` | La regla |
| `severidad` | `DURA` o `BLANDA` | RN-02 y RN-13 son duras; RN-19, la de la normativa |
| `horasQueQuedan` | horas | 0 en RN-19 |

### `MotorReglas`

`evaluarAfinidad(puntaje, minimo, troncoComun, reglas)`:

1. Si es tronco común o alcanza el mínimo: nada.
2. Si no: `RN_19`, con la severidad de la normativa.

### `Propuesta`

`inconsistencia()`: `RN_19` justificada si la justificación es `EXCEPCION`; si no, vacío.

### `ResultadoDePropuesta` (puerto)

| Campo | Regla |
|---|---|
| `propuesta` | Vacía si no se guardó |
| `advertencia` | La inconsistencia blanda sin justificar; vacía si se guardó |

## Normativa

Sin cambios de datos: lee la severidad de RN-19.

## Glosario (`sdd/modelo_dominio.md`)

- **Advertencia:** término nuevo: lo que responde el sistema ante una inconsistencia blanda sin justificar. No guarda nada.
