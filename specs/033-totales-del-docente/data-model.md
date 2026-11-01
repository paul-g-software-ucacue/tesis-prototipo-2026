# Data Model: Ver los totales del docente al instante

Sin tablas nuevas ni migraciones. Todo se calcula en cada consulta.

## Generación · dominio

### `CargaHoraria` (objeto de valor calculado)

| Campo | Tipo | Regla |
|---|---|---|
| `clase` | horas | Las horas actuales de sus propuestas, sin paralelos retirados, en todas las carreras |
| `otrasDocencia` | horas | Sus actividades de ese componente, en todas las carreras |
| `investigacion` | horas | Igual |
| `vinculacion` | horas | Igual |
| `gestion` | horas | Igual |

Operaciones: `asignadas()`, la suma de las cinco; `disponibles(contratadas)`, nunca menos de 0 (D-75); `exceso(contratadas)`, nunca menos de 0; `proporcion()`.

### `Proporcion` (objeto de valor)

| Campo | Tipo | Regla |
|---|---|---|
| `clase` | entero 0–100 | Redondeado al entero más cercano |
| `otrasDocencia` | entero 0–100 | 100 menos `clase` |

`Proporcion.de(clase, otras)` es vacío si las dos son 0.

### `MetaDeProporcion` (objeto de valor)

`clase` y `otrasDocencia`, los parámetros de RN-05. Se muestran tal como vienen de la normativa.

## Normativa

Sin cambios de datos: lee `porcentaje_clase` y `porcentaje_otras_docencia` de RN-05 en la versión que rige el periodo.

## Glosario (`sdd/modelo_dominio.md`)

- **Proporción 60/40:** agrega el término de código `Proporcion`.
- **Meta de la proporción:** término nuevo, `MetaDeProporcion`.
- **`CargaHoraria`:** se aclara que hasta F07-H02 puede superar las horas contratadas (D-78).
