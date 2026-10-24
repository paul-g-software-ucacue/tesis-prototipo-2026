# Contrato: Académico · carreras, mallas y asignaturas (F03-H02)

Cambios en `contratos/academico/openapi.yaml`, antes del código (principio IV). El porqué está en [research.md](../research.md) §5 a §7.

## Esquemas nuevos

| Esquema | Campos |
|---|---|
| `NivelDeFormacion`, `Modalidad`, `Jornada`, `UnidadDeOrganizacionCurricular`, `EstadoDeCarrera`, `EstadoDeMalla` | enums del diccionario |
| `CampoAmplio` | `codigo` (`^(0[0-9]|10)$`), `nombre` |
| `ResumenDeMalla` | `codigo`, `estado`, `asignaturas` (activas), `horasSemanalesClase` |
| `Carrera` | `codigo` (`^[0-9A-Z-]{1,20}$`), `nombre`, `siglas` (`^[0-9A-Z]{1,10}$`), `sede`, `nivelFormacion`, `modalidad`, `jornadas` (1 a 4, sin repetir), `campoAmplio`, `campoEspecifico` (`^[0-9]{3}$`), `numeroCiclos` (1–10), `estado`, `mallas` (`ResumenDeMalla`); opcional `campoDetallado` (`^[0-9]{4}$`) |
| `CarreraNueva` / `DatosDeCarrera` | los de `Carrera` sin `estado` ni `mallas`; `DatosDeCarrera` tampoco `codigo` |
| `EstadoDeCarreraNuevo` | `estado` |
| `MallaNueva` | `codigo` (`^[0-9A-Z-]{1,20}$`) |
| `EstadoDeMallaNuevo` | `estado` |
| `Asignatura` | `codigo` (`^[0-9A-Z-]{1,30}$`), `carrera`, `malla`, `nombre`, `ciclo`, `unidadOrganizacionCurricular`, `horasSemanalesClase` (0,5–20), `troncoComun`, `campoAmplio` (`CampoAmplio`, con su nombre), `campoEspecifico`, `palabrasClave` (al menos 2), `activa`; opcionales `horasPracticasSemanales`, `creditos` |
| `AsignaturaNueva` | `codigo`, `carrera`, `malla`, `nombre`, `ciclo`, `unidadOrganizacionCurricular`, `horasSemanalesClase`, `troncoComun`, `campoAmplio` (código), `campoEspecifico`, `palabrasClave` — todos obligatorios; opcionales las prácticas y los créditos |
| `DatosDeAsignatura` | los de `AsignaturaNueva` sin `codigo`, `carrera` ni `malla` |
| `EstadoDeAsignatura` | `activa` |

## Operaciones nuevas (tag `Carreras`)

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarCamposAmplios` | `GET /api/academico/campos-amplios` | lectura | 200 |
| `listarCarreras` | `GET /api/academico/carreras` | lectura | 200 |
| `registrarCarrera` | `POST /api/academico/carreras` | escritura | 201 · 400 · 409 `ACADEMICO_CARRERA_REPETIDA` o `ACADEMICO_SIGLAS_REPETIDAS` |
| `consultarCarrera` | `GET /api/academico/carreras/{codigo}` | lectura | 200 · 404 |
| `corregirCarrera` | `PUT /api/academico/carreras/{codigo}` | escritura | 200 · 400 · 404 · 409 `ACADEMICO_SIGLAS_REPETIDAS` |
| `cambiarEstadoCarrera` | `PUT /api/academico/carreras/{codigo}/estado` | escritura | 200 · 400 · 404 |
| `registrarMalla` | `POST /api/academico/carreras/{codigo}/mallas` | escritura | 201 · 400 · 404 · 409 `ACADEMICO_MALLA_REPETIDA` |
| `cambiarEstadoMalla` | `PUT /api/academico/mallas/{codigo}/estado` | escritura | 200 · 400 · 404 |
| `listarAsignaturasDeCarrera` | `GET /api/academico/carreras/{codigo}/asignaturas` | lectura | 200 · 404 |
| `registrarAsignatura` | `POST /api/academico/asignaturas` | escritura | 201 · 400 · 409 `ACADEMICO_ASIGNATURA_REPETIDA` |
| `consultarAsignatura` | `GET /api/academico/asignaturas/{codigo}` | lectura | 200 · 404 |
| `corregirAsignatura` | `PUT /api/academico/asignaturas/{codigo}` | escritura | 200 · 400 · 404 |
| `cambiarEstadoAsignatura` | `PUT /api/academico/asignaturas/{codigo}/estado` | escritura | 200 · 400 · 404 |

- **Lectura:** `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`. **Escritura:** `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]` (D-70).
- Todas responden también 401, 403, 500 y 503 comunes. El 404 es `COMUN_RECURSO_NO_ENCONTRADO`.

## Errores nuevos (`contratos/comun/errores.md`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `ACADEMICO_CARRERA_REPETIDA` | 409 | Carrera repetida | Ya existe una carrera con ese código. Revisa el código SENESCYT. |
| `ACADEMICO_SIGLAS_REPETIDAS` | 409 | Siglas repetidas | Otra carrera ya usa esas siglas. Elige otras. |
| `ACADEMICO_MALLA_REPETIDA` | 409 | Malla repetida | Ya existe una malla con ese código de versión. Elige otro. |
| `ACADEMICO_ASIGNATURA_REPETIDA` | 409 | Asignatura repetida | Ya existe una asignatura con ese código. Elige otro. |
