# Data Model: Impedir una asignación que viola una regla dura

Sin tablas nuevas ni migraciones.

## Generación · dominio

### `ReglasDelPeriodo` (objeto de valor)

| Campo | Tipo | Regla |
|---|---|---|
| `horasMaximasDocente` | horas | RN-02, mayor que 0 |
| `horasClaseConProyecto` | horas | La fila de RN-13, mayor que 0 |

`limite(contratadas)`: el menor entre las horas contratadas y `horasMaximasDocente`.

### `Inconsistencia` (objeto de valor)

| Campo | Tipo | Regla |
|---|---|---|
| `regla` | `RN_02` o `RN_13` | La regla violada |
| `horasQueQuedan` | horas | Lo que todavía cabe, nunca menos de 0 |

### `MotorReglas` (servicio de dominio)

`evaluar(antes, despues, contratadas, conProyecto, reglas)`:

1. Si `despues.asignadas()` supera el límite y es mayor que `antes.asignadas()`: RN-02.
2. Si `conProyecto`, y `despues.clase()` supera `horasClaseConProyecto` y es mayor que `antes.clase()`: RN-13.
3. Si no, nada.

`conProyecto(tipos)`: si alguno es `INVESTIGACION` o `VINCULACION`.

### `VersionDistributivo`

- `conPropuesta(propuesta)`: las propuestas con esa, sin la anterior de su paralelo.
- `conActividad(actividad)`: las actividades con esa, sin la anterior con la misma clave.

### `Candidato`

Agrega `limite` y `motivo`: vacío, `HORAS` o `TOPE_DE_CLASE`.

## Normativa

Sin cambios de datos: lee `horas_maximas_docente` de RN-02 y `horas_clase_maximo` de la fila `DOCENTE_INVESTIGACION_VINCULACION`.

## Glosario (`sdd/modelo_dominio.md`)

- **Límite del docente:** término nuevo, `limite()`: el menor entre sus horas contratadas y el tope de RN-02.
- **`MotorReglas`:** se aclara que devuelve la inconsistencia comparando antes y después (D-83 no lo cambia).
