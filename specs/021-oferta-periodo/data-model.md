# Data Model: Mantener la oferta del periodo (F03-H03)

Lo que agrega la historia al esquema `academico` y al dominio de Académico. Las reglas y su porqué están en [research.md](research.md) §2 a §4.

## Tablas (`V5__oferta_del_periodo.sql`)

### `asignatura_paralelo`

| Columna | Tipo | Reglas |
|---|---|---|
| `periodo` | `varchar(20)` | FK → `periodo_academico(codigo)` |
| `asignatura` | `varchar(30)` | FK → `asignatura(codigo)` |
| `paralelo` | `varchar(20)` | `^[0-9A-Z_-]{1,20}$` |
| `jornada` | `varchar(15)` | `MATUTINA`, `VESPERTINA`, `NOCTURNA`, `INTENSIVA` |
| `modalidad` | `varchar(15)` | las 6 de la carga 2 |
| `sede` | `varchar(20)` nulo | FK → `unidad_organica(codigo)`; nulo = la de la carrera |
| `estudiantes` | `integer` | 0 a 9999 |
| `horas_semanales` | `numeric(4,2)` nulo | 0,5 a 20; nulo = las de la asignatura |
| `estado` | `varchar(10)` | `OFERTADA`, `RETIRADA` |
| `registrada_en`, `registrada_por` | `timestamptz`, `varchar(64)` | quién y cuándo |

Clave primaria `(periodo, asignatura, paralelo)`. Índice por `periodo` para la consulta.

### `registro_cambio`

El `check` de `accion` gana `ASIGNATURA_PARALELO_REGISTRADA`, `ASIGNATURA_PARALELO_CORREGIDA`, `ASIGNATURA_PARALELO_RETIRADA` y `ASIGNATURA_PARALELO_REOFERTADA`.

## Dominio (`ec.mdd.academico.dominio`)

| Tipo | Qué es |
|---|---|
| `EstadoDeAsignaturaParalelo` | `OFERTADA`, `RETIRADA` |
| `AsignaturaParalelo` | *record*: `periodo`, `asignatura`, `paralelo`, `jornada` (`Jornada`), `modalidad` (`Modalidad`), `sede` (opcional), `estudiantes`, `horasIndicadas` (opcional, `BigDecimal`), `estado`; `nueva(...)`, `corregida(...)` → `Correccion(asignaturaParalelo, cambios)`, `conEstado(...)` |
| `OfertaDelPeriodo` | el periodo, sus asignaturas-paralelo y los `DatosMaestros`; `ofertar(...)`, `corregir(...)`, `retirar(...)`, `volverAOfertar(...)`; `horasDe(asignaturaParalelo)` (las efectivas), `horasACubrir()` por asignatura, por carrera y del periodo |
| `HorasACubrir` | *record* con los totales: del periodo, por carrera y por asignatura, con el número de paralelos ofertados |

Reutiliza `Jornada`, `Modalidad`, `Cambios`, `DatoInvalido` y `DatosMaestros` de F03-H02 y `PeriodoAcademico` y `EstadoPeriodo` de F02-H01.

## Puertos

- **Entrada:** `ConsultarOferta` (la oferta de un periodo con sus totales; una asignatura-paralelo), `MantenerOferta` (ofertar, corregir, cambiar el estado); excepciones `OfertaCerrada`, `AsignaturaParaleloRepetida`, `AsignaturaParaleloNoEncontrada`; `DatosDeAsignaturaParalelo`.
- **Salida:** `RepositorioOferta` (`delPeriodo`, `bloquear`, `registrar`, `guardar`). Usa también `RepositorioPeriodos`, `RepositorioCarreras` y `RepositorioEstructura`, que ya existen, y `RegistroDeCambios`.
