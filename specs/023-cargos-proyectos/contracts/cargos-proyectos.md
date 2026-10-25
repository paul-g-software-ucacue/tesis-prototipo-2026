# Contrato: cargos y proyectos (F03-H05)

`contratos/docentes/openapi.yaml` gana los tags `Cargos` y `Proyectos`, y `contratos/academico/openapi.yaml` amplía `consultarReferencias`. Se escriben antes del código (principio IV). El porqué está en [research.md](../research.md) §2 a §10.

## Esquemas nuevos de Docentes

| Esquema | Campos |
|---|---|
| `TipoDeCargo` | los 22 de research §3: `RECTOR`, `VICERRECTOR`, `COORDINADOR_ACADEMICO_INSTITUCIONAL`, `COORDINADOR_SEDE`, `COORDINADOR_CARRERA`, `G06_GESTOR_VINCULACION` … `G21_GESTOR_ESTADISTICAS` y `MIEMBRO_COMISION` |
| `EstadoDeCargo` | `VIGENTE`, `FINALIZADO` |
| `CodigoDeUnidad` | `^[A-Z0-9_-]{1,20}$` |
| `DatosDeCargo` | `unidad` (opcional), `carrera` (opcional), `fechaFin` (opcional), `documento` (1–60), `estado` |
| `CargoNuevo` | `docente`, `tipo` y `fechaDesignacion`, más los campos de `DatosDeCargo` al mismo nivel; `estado` es opcional y por omisión vale `VIGENTE` |
| `AdvertenciaDeCargo` | `codigo` (`CUARTO_NIVEL`) y `descripcion` («Le falta un título de cuarto nivel registrado (RN-20).») |
| `Cargo` | lo de `CargoNuevo` con `nombreDelDocente`, `nombreDeUnidad`, `nombreDeCarrera` y `advertencias` |
| `TipoDeProyecto` | `INVESTIGACION`, `VINCULACION`, `PRACTICAS_PREPROFESIONALES`, `SERVICIO_COMUNITARIO` |
| `EstadoDeProyecto` | `APROBADO`, `EN_EJECUCION`, `FINALIZADO`, `SUSPENDIDO` |
| `RolDeParticipante` | `DIRECTOR`, `INVESTIGADOR`, `DOCENTE_APOYO`, `TUTOR_PRACTICAS` |
| `HorasSemanales` | `number`, `> 0`, `≤ 999.9`, `multipleOf: 0.1` |
| `Participante` | `docente`, `rol` y `horas` |
| `DatosDeProyecto` | `nombre` (1–250), `tipo`, `carreras` (1–20), `resolucionOcs` (1–60), `fechaResolucion`, `estado`, `vigenciaInicio`, `vigenciaFin`, `director`, `participantes` (1–40) y `horasAprobadas` |
| `ProyectoNuevo` | `codigo` (`^[0-9A-Z-]{1,30}$`) y los campos de `DatosDeProyecto` al mismo nivel |
| `Proyecto` | lo de `ProyectoNuevo` con `nombreDelDirector`, los nombres de los participantes y las carreras, y, si se pidió un periodo, `asignable` y `motivos` (`ESTADO`, `VIGENCIA`) |
| `ReferenciasDeDocentes` | suma `unidades` (`codigo`, `nombre`, `tipo`, `gestionAutorizada`, `activa`) y `periodos` (`codigo`, `nombre`, `inicio`, `fin`) |

Ninguna lista usa `uniqueItems`: una carrera o un participante repetido da 400 con su posición (F03-H04).

## Operaciones (tags `Cargos` y `Proyectos`)

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarCargos` | `GET /api/docentes/cargos?estado=&docente=` | lectura | 200 (`Cargo[]`) · 400 |
| `registrarCargo` | `POST /api/docentes/cargos` | escritura | 201 (`Cargo`, con sus advertencias) · 400 · 409 `DOCENTES_CARGO_REPETIDO`, `DOCENTES_COORDINADOR_VIGENTE` o `DOCENTES_DOCENTE_NO_HABILITADO` |
| `corregirCargo` | `PUT /api/docentes/cargos/{docente}/{tipo}/{fechaDesignacion}` | escritura | 200 · 400 · 404 · 409 `DOCENTES_COORDINADOR_VIGENTE` o `DOCENTES_DOCENTE_NO_HABILITADO` |
| `listarProyectos` | `GET /api/docentes/proyectos?periodo=&tipo=&estado=` | lectura | 200 (`Proyecto[]`) · 400 (un periodo que no existe, en `detalles`) |
| `consultarProyecto` | `GET /api/docentes/proyectos/{codigo}?periodo=` | lectura | 200 · 400 · 404 |
| `registrarProyecto` | `POST /api/docentes/proyectos` | escritura | 201 · 400 · 409 `DOCENTES_PROYECTO_REPETIDO` |
| `corregirProyecto` | `PUT /api/docentes/proyectos/{codigo}` | escritura | 200 · 400 · 404 |

- **Escritura:** `[DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`.
- **Lectura:** `[DOCENTES:LECTURA, CATALOGOS:LECTURA, DISTRIBUTIVO:LECTURA, PROYECTOS:LECTURA]`.
- `consultarReferenciasDeDocentes` suma `PROYECTOS:LECTURA` (research §8).

## Académico

`ReferenciasDeAcademico` (respuesta de `consultarReferencias`) suma:
- `unidades`: `UnidadDeReferencia` con `codigo`, `nombre`, `tipo`, `gestionAutorizada` (opcional) y `activa`;
- `periodos`: `PeriodoDeReferencia` con `codigo`, `nombre`, `inicio` y `fin`.

Es un cambio compatible: solo agrega propiedades.

## Errores (`contratos/comun/errores.md`, D-72)

Los 4 códigos de research §10, con sus 8 columnas. Los datos inválidos responden `COMUN_SOLICITUD_INVALIDA` con un detalle por campo, como `fechaFin`, `participantes[2].horas` o `periodo`, y un motivo que dice qué hacer.
