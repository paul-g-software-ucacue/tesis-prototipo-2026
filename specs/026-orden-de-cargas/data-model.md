# Data Model: Configurar las 10 cargas en su orden

## Qué cuenta como «tener datos» en cada carga (research §1 y §2)

Lo responde el traductor de cada carga con `tieneDatos()`, con una consulta de existencia en su repositorio.

| Nº | Carga | Dueño | Tiene datos si hay al menos… |
|---|---|---|---|
| 1 | Estructura orgánica | Académico | una unidad orgánica activa |
| 2 | Carreras | Académico | una carrera `VIGENTE` |
| 3 | Ciclos académicos aprobados | Académico | un periodo académico, en cualquier estado |
| 4 | Asignaturas y malla | Académico | una asignatura activa |
| 5 | Oferta del periodo | Académico | una asignatura-paralelo `OFERTADA` |
| 6 | Docentes contratados | Docentes | un docente `ACTIVO` o con `LICENCIA` |
| 7 | Horas según la política | Normativa | una versión de normativa (la 2024 viene precargada, F04-H01) |
| 8 | Cargos y autoridades | Docentes | un cargo `VIGENTE` |
| 9 | Proyectos y prácticas | Docentes | un proyecto, en cualquier estado |
| 10 | Matriculados por carrera | Académico | un registro de matriculados |

## Estado de la carga (calculado, no se guarda)

| Campo | Tipo | Regla |
|---|---|---|
| `codigo` | texto, `^[a-z_]{1,40}$` | El de la carga en el diccionario. |
| `numero` | entero 1–10 | Su orden en la ERS §8.3. |
| `estado` | `PENDIENTE`, `CARGADA` o `SIN_PROYECTOS` | `CARGADA` si su dueño tiene datos. `SIN_PROYECTOS` solo para la 9, si no tiene datos y hay al menos una confirmación «sin proyectos» que vale. Si no, `PENDIENTE`. |
| `faltan` | lista de `{numero, nombre}` | Las cargas de su `dependeDe` que no están `CARGADA`, en orden. Vacía si se puede subir. |
| `periodosSinProyectos` | lista de códigos de periodo | Solo en la 9: los periodos con una confirmación «sin proyectos» que vale. |

**Dependencias:**
- Una carga de la que se depende cuenta como cargada solo si está `CARGADA`. `SIN_PROYECTOS` no habilita a nadie, pero ninguna carga depende de la 9.
- Las dependencias salen de `dependeDe` del diccionario (FR-002); no hay otra copia.

## Confirmación «sin proyectos» (nueva, en el historial)

Es una entrada de `cargas.historial_carga` con `carga = 'proyectos_practicas'`.

| Campo | Regla |
|---|---|
| `resultado` | `SIN_PROYECTOS` (valor nuevo del `check`). |
| `periodo` | Columna nueva, `varchar(10)`, `^[0-9]{4}-(I\|II)$`. Obligatoria si el resultado es `SIN_PROYECTOS`; nula en los demás. |
| `archivo`, `hash` | Pasan a admitir nulo, solo cuando el resultado es `SIN_PROYECTOS`; en los demás siguen obligatorios. |
| `nuevos`, `cambiados`, `bajas_sin_aplicar` | 0. |
| `motivo` | Nulo (no es un rechazo). |
| `usuario`, `momento` | Como en toda entrada (principio VII). |

**Unicidad:** un índice único parcial `(carga, periodo) where resultado = 'SIN_PROYECTOS'`. La segunda confirmación del mismo periodo no inserta: devuelve la que ya estaba.

**Validez:** vale mientras Docentes diga que el periodo no tiene proyectos asignables (APROBADO o EN_EJECUCION, con una vigencia que se cruza con las fechas del periodo, F03-H05). Se calcula al leer; la entrada nunca se borra.

## Migración `V3__sin_proyectos.sql` (Cargas)

- `alter table cargas.historial_carga add column periodo varchar(10) check (periodo ~ '^[0-9]{4}-(I|II)$')`.
- Se reemplaza el `check` de `resultado` para admitir `SIN_PROYECTOS`.
- `archivo` y `hash` pasan a nulos, con un `check` nuevo: son nulos si y solo si el resultado es `SIN_PROYECTOS`. Lo mismo para `periodo`, al revés.
- El índice único parcial de arriba.

Las filas que ya existen siguen cumpliendo todo: tienen archivo y hash y no tienen periodo.

## Cambios en el lenguaje publicado (`:plataforma:lenguaje-cargas`)

| Tipo | Cambio |
|---|---|
| `TraductorDeCarga<T>` | Suma `boolean tieneDatos()`, abstracto. |
| `AplicadorDeCargas` | Suma `boolean tieneDatos(String carga)`. |
| `AplicadorPorTraductores` | Lo reparte al traductor de esa carga; una carga que no atiende lanza `IllegalArgumentException`, como `previsualizar`. |

## Cargas: tipos nuevos

| Capa | Tipo | Qué es |
|---|---|---|
| dominio | `OrdenDeLasCargas` | Con el diccionario y un `Predicate<String>` de «tiene datos», calcula `faltantes(carga)` y el estado de las 10. No conoce a los dueños. |
| dominio | `EstadoDeCarga`, `EstadoDeLaCarga` (enum), `CargaQueFalta` | Los valores del estado de arriba. |
| puertos.entrada | `ConsultarEstadoDeLasCargas`, `ConfirmarSinProyectos` | Los dos casos de uso nuevos. |
| puertos.entrada | `CargasAnterioresPendientes`, `PeriodoNoEncontrado`, `PeriodoConProyectos` | Las excepciones que el catálogo traduce (research §11). |
| puertos.salida | `FuenteDeDatos` | `LectorDeArchivo` renombrado (research §7). |
| puertos.salida | `ProyectosDelPeriodo` | `hayAsignables(periodo)`; lanza `PeriodoNoEncontrado`. |
| puertos.salida | `HistorialDeCargas` | Suma `sinProyectos()` (las confirmaciones) y `confirmarSinProyectos(periodo, quien)`, que devuelve la entrada nueva o la que ya estaba. |
