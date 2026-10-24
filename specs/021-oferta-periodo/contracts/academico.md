# Contrato: Académico · oferta del periodo (F03-H03)

Cambios en `contratos/academico/openapi.yaml`, antes del código (principio IV). El porqué está en [research.md](../research.md) §5 a §7.

## Esquemas nuevos

| Esquema | Campos |
|---|---|
| `CodigoDeParalelo` | `^[0-9A-Z_-]{1,20}$`, ejemplo `A_NOCTURNA` |
| `EstadoDeAsignaturaParalelo` | `OFERTADA`, `RETIRADA` |
| `AsignaturaParalelo` | `periodo`, `asignatura`, `nombre` (de la asignatura), `carrera`, `malla`, `ciclo`, `paralelo`, `jornada`, `modalidad`, `sede` (la efectiva), `sedeIndicada` (si no es la de la carrera), `estudiantes`, `horasSemanales` (las efectivas), `horasIndicadas` (si no son las de la asignatura), `horasDeLaAsignatura`, `porEncimaDeLaAsignatura`, `troncoComun`, `campoAmplio` (`CampoAmplio`, con su nombre), `campoEspecifico`, `palabrasClave`, `estado` |
| `AsignaturaParaleloNueva` | `asignatura`, `paralelo`, `jornada`, `modalidad`, `estudiantes` (0–9999); opcionales `sede`, `horasSemanales` (0,5–20) |
| `DatosDeAsignaturaParalelo` | `jornada`, `modalidad`, `estudiantes`; opcionales `sede`, `horasSemanales` (omitidos = los de la carrera y la asignatura) |
| `EstadoDeAsignaturaParaleloNuevo` | `estado` |
| `HorasDeAsignatura` | `asignatura`, `nombre`, `ciclo`, `paralelos` (ofertados), `horasACubrir` |
| `HorasDeCarrera` | `carrera`, `nombre`, `horasACubrir`, `asignaturas` (`HorasDeAsignatura`, por ciclo y código) |
| `OfertaDelPeriodo` | `periodo`, `estadoDelPeriodo`, `modificable`, `horasACubrir`, `carreras` (`HorasDeCarrera`), `asignaturasParalelo` (`AsignaturaParalelo`, por carrera, ciclo, asignatura y paralelo) |

## Operaciones nuevas (tag `Oferta`)

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `consultarOferta` | `GET /api/academico/periodos/{codigo}/oferta` | lectura | 200 · 404 |
| `ofertarAsignatura` | `POST /api/academico/periodos/{codigo}/oferta` | escritura | 201 · 400 · 404 · 409 `ACADEMICO_ASIGNATURA_PARALELO_REPETIDA` o `ACADEMICO_OFERTA_CERRADA` |
| `consultarAsignaturaParalelo` | `GET /api/academico/periodos/{codigo}/oferta/{asignatura}/{paralelo}` | lectura | 200 · 404 |
| `corregirAsignaturaParalelo` | `PUT /api/academico/periodos/{codigo}/oferta/{asignatura}/{paralelo}` | escritura | 200 · 400 · 404 · 409 `ACADEMICO_OFERTA_CERRADA` |
| `cambiarEstadoAsignaturaParalelo` | `PUT /api/academico/periodos/{codigo}/oferta/{asignatura}/{paralelo}/estado` | escritura | 200 · 400 · 404 · 409 `ACADEMICO_OFERTA_CERRADA` |

- **Lectura:** `[OFERTA:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`. **Escritura:** `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]` (D-70).
- Un 400 trae `COMUN_SOLICITUD_INVALIDA` con un detalle por campo (`asignatura`, `jornada`, `sede`, `horasSemanales`, …).

## Errores nuevos (`contratos/comun/errores.md`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `ACADEMICO_ASIGNATURA_PARALELO_REPETIDA` | 409 | Asignatura-paralelo repetida | Esa asignatura ya está en ese paralelo del periodo. Si está retirada, vuelve a ofertarla. |
| `ACADEMICO_OFERTA_CERRADA` | 409 | Oferta cerrada | La oferta solo cambia mientras el periodo está en Planificación. |
