# Data Model: Ver los docentes sugeridos para una asignatura

Nada se guarda: la sugerencia se calcula en cada consulta (FR-022). Sin migraciones.

Los puntajes, el mínimo y las horas son `BigDecimal`, comparados con `compareTo`; solo las horas contratadas son enteras.

## Dominio de Generación

### `Sugerencia` (objeto de valor)

| Campo | Regla |
|---|---|
| `periodo` | Código del periodo. |
| `paralelo` | La `AsignaturaParalelo` sugerida. |
| `version` | Número de la versión de normativa que rige el periodo. |
| `minimoAfinidad` | El de esa versión. |
| `candidatos` | Lista de `Candidato`, en el orden de abajo. |
| `fueraPorElMinimo` | Docentes activos que no alcanzan el mínimo. 0 en tronco común. |

`Sugerencia.armar(periodo, paralelo, pesos, docentes, horas)`, con el mínimo dentro de `PesosDelPeriodo`. Función pura:

1. Calcula el puntaje de cada docente con `CalculadoraAfinidad`.
2. Si la asignatura no es de tronco común, deja fuera a quien tenga un puntaje menor que el mínimo, y los cuenta. Igual al mínimo, se queda.
3. Pone primero a los que tienen horas suficientes.
4. Dentro de cada grupo: mayor puntaje, más horas disponibles, nombre.

### `Candidato` (objeto de valor)

| Campo | Regla |
|---|---|
| `identificacion` | Clave del docente; la pantalla no la muestra. |
| `nombre` | Apellidos y nombres. |
| `dedicacion` | `TC`, `MT` o `TP`. |
| `horasContratadas` | De 1 a 40. |
| `horas` | `HorasDelDocente`: las asignadas no van al contrato, y las de otras carreras sí. |
| `puntaje` | `PuntajeAfinidad` en la asignatura. |

`horasDisponibles()`: `horasContratadas − horas.asignadas`, nunca menos de 0.

`tieneHorasPara(horasDelParalelo)`: `horasDisponibles() >= horasDelParalelo`, con `compareTo`.

### `AsignaturaParalelo` (objeto de valor)

Propio de este contexto (principio V).

| Campo | Regla |
|---|---|
| `asignatura` | `AsignaturaOfertada`: código, nombre, campo amplio y específico. |
| `paralelo` | Código del paralelo. |
| `carrera` | Su carrera; define qué son «otras carreras». |
| `horasSemanales` | Decimal, de 0,5 a 20: las efectivas del paralelo. |
| `troncoComun` | Exime del mínimo. |
| `retirada` | Si salió de la oferta. |

### `DocenteActivo` (objeto de valor)

`identificacion`, `nombre`, `dedicacion`, `horasContratadas` y `FormacionDelDocente`.

### `HorasDelDocente` (objeto de valor)

`asignadas` y `enOtrasCarreras`, decimales no negativos. `enOtrasCarreras` no pasa de `asignadas`. Sin asignaciones, `HorasDelDocente.NINGUNA`: 0 y 0.

### Cambia

`PesosDelPeriodo` suma `minimoAfinidad`. F06-H01 no lo usa.

## Puertos de Generación

- **Entrada:** `ConsultarSugerencia.sugerencia(periodo, asignatura, paralelo)`.
- **Salida:** `DatosParaSugerencia`:
  - `paralelo(periodo, asignatura, paralelo)`: vacío si no existe;
  - `docentesActivos()`.
  - Los pesos salen del método `pesos` que ya existe en `DatosParaAfinidad`.
- **Salida:** `HorasAsignadas.delPeriodo(periodo, carrera)`: un mapa de identificación a `HorasDelDocente`; un docente que no está, 0 h.
- **Excepciones:** `ParaleloNoEncontrado`, `ParaleloRetirado`; se reusa `PeriodoSinAbrir`.

## Lo que traen los dueños

| Dueño | Dato nuevo | Campos |
|---|---|---|
| Académico | Una asignatura-paralelo | Asignatura, nombre, carrera, paralelo, campo amplio, campo específico, tronco común, horas semanales efectivas, estado. |
| Docentes | Los docentes activos | Identificación, nombre completo, dedicación, horas contratadas, formación. |
| Normativa | El mínimo | `minimoAfinidad`, en la respuesta de los pesos. |

## Transiciones

Ninguna: la historia solo lee.
