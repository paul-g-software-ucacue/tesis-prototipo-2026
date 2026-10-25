# Data Model: Registrar los cargos y los proyectos aprobados

Esquema `docentes`, migración `V3__cargos_y_proyectos.sql`. No hay claves foráneas a otros esquemas (principio V): las unidades, las carreras y los periodos se validan contra `consultarReferencias` de Académico al guardar ([research.md](research.md) §2).

## Dominio

### `Cargo` (record)

| Campo | Tipo | Regla |
|---|---|---|
| `docente` | `String` | Identificación de un docente registrado; activo y de tiempo completo (`DocenteNoHabilitado` si no). |
| `tipo` | `TipoDeCargo` | Uno de los 22 del catálogo `CARGOS` (research §3). |
| `unidad` | `String` o nulo | `[A-Z0-9_-]{1,20}`; obligatoria y que corresponda al cargo, según la tabla de research §4. |
| `carrera` | `String` o nulo | `[0-9A-Z-]{1,20}`; obligatoria solo para `COORDINADOR_CARRERA`; una carrera de Académico. |
| `fechaDesignacion` | `LocalDate` | Obligatoria. |
| `fechaFin` | `LocalDate` o nulo | Posterior a la designación y no más de 2 años después (POL 5.4). |
| `documento` | `String` | Texto de 1 a 60 caracteres, sin espacios en los extremos. |
| `estado` | `EstadoDeCargo` | `VIGENTE` o `FINALIZADO`; un cargo nuevo, `VIGENTE`. |

- Clave: `(docente, tipo, fechaDesignacion)`; no cambia al corregir.
- `TipoDeCargo` sabe qué gestión autorizada tiene cada cargo de gestor (`G06` a `G21`), si es coordinador o gestor (`exigeCuartoNivel()`, para RN-20) y qué unidad pide.
- `advertencias(Docente, LocalDate hoy)` devuelve `List<AdvertenciaDeCargo>`, con tres valores posibles (research §5):
  - `CUARTO_NIVEL`: el cargo exige cuarto nivel y la ficha del docente no tiene ningún título de cuarto nivel;
  - `FECHA_FIN_VENCIDA`: el cargo está vigente y su fecha de fin es anterior a `hoy`;
  - `DOCENTE_NO_HABILITADO`: el cargo está vigente y su docente ya no está activo o ya no es de tiempo completo.

### `Proyecto` (record)

| Campo | Tipo | Regla |
|---|---|---|
| `codigo` | `String` | `[0-9A-Z-]{1,30}`; único; no cambia. |
| `nombre` | `String` | Texto de 1 a 250 caracteres. |
| `tipo` | `TipoDeProyecto` | `INVESTIGACION`, `VINCULACION`, `PRACTICAS_PREPROFESIONALES` o `SERVICIO_COMUNITARIO`. |
| `carreras` | `List<String>` | Al menos una; sin repetir; cada una de Académico. |
| `resolucionOcs` | `String` | Texto de 1 a 60 caracteres. |
| `fechaResolucion` | `LocalDate` | Obligatoria. |
| `estado` | `EstadoDeProyecto` | `APROBADO`, `EN_EJECUCION`, `FINALIZADO` o `SUSPENDIDO`. |
| `vigenciaInicio`, `vigenciaFin` | `LocalDate` | Obligatorias; el fin es posterior al inicio. |
| `director` | `String` | Identificación de un docente registrado. |
| `participantes` | `List<Participante>` | Al menos uno, sin repetir docente; la suma de sus horas no pasa `horasAprobadas`. |
| `horasAprobadas` | `BigDecimal` | Mayor que 0, con hasta dos decimales y hasta 5 caracteres sin ceros de más (aclaración 7). |

- `Participante(docente, RolDeParticipante rol, BigDecimal horas)`: el docente está registrado; el rol es `DIRECTOR`, `INVESTIGADOR`, `DOCENTE_APOYO` o `TUTOR_PRACTICAS`; las horas siguen la misma regla que `horasAprobadas`.
- `asignableEn(LocalDate inicio, LocalDate fin)` devuelve un `Asignabilidad(boolean asignable, Set<MotivoNoAsignable> motivos)`, con los motivos `ESTADO` y `VIGENCIA` (research §7).

### `Referencias` (amplía la de F03-H04)

Suma las unidades, con su tipo, su gestión y si está activa, y los periodos, con sus fechas. También suma `unidad(codigo)` y `periodo(codigo)`; un código que no existe es un `DatoInvalido` de su campo.

## Base de datos (`V3__cargos_y_proyectos.sql`)

```text
cargo
  docente            varchar(13)  not null
  tipo               varchar(40)  not null  check (tipo in (<los 22>))
  fecha_designacion  date         not null
  unidad             varchar(20)
  carrera            varchar(20)
  fecha_fin          date                    check (fecha_fin > fecha_designacion
                                                    and fecha_fin <= fecha_designacion + interval '2 years')
  documento          varchar(60)  not null
  estado             varchar(10)  not null  check (estado in ('VIGENTE', 'FINALIZADO'))
  registrado_en      timestamptz  not null
  registrado_por     varchar(64)  not null
  primary key (docente, tipo, fecha_designacion)
  check (tipo = 'COORDINADOR_CARRERA' and carrera is not null or tipo <> 'COORDINADOR_CARRERA' and unidad is not null)
unique index cargo_coordinador_vigente on cargo (carrera)
  where tipo = 'COORDINADOR_CARRERA' and estado = 'VIGENTE'                -- RN-12

proyecto
  codigo             varchar(30)  primary key check (codigo ~ '^[0-9A-Z-]{1,30}$')
  nombre             varchar(250) not null
  tipo               varchar(30)  not null  check (tipo in (<los 4>))
  carreras           jsonb        not null  -- ["550613A01-P-0301", …]
  resolucion_ocs     varchar(60)  not null
  fecha_resolucion   date         not null
  estado             varchar(15)  not null  check (estado in (<los 4>))
  vigencia_inicio    date         not null
  vigencia_fin       date         not null  check (vigencia_fin > vigencia_inicio)
  director           varchar(13)  not null
  participantes      jsonb        not null  -- [{"docente", "rol", "horas"}, …]
  horas_aprobadas    numeric(5,2) not null  check (horas_aprobadas > 0 and horas_aprobadas < 1000)
  registrado_en      timestamptz  not null
  registrado_por     varchar(64)  not null

registro_cambio.accion   -- el check suma CARGO_REGISTRADO, CARGO_CORREGIDO, PROYECTO_REGISTRADO, PROYECTO_CORREGIDO
```

- `cargo.docente` y `proyecto.director` tienen clave foránea a `docente (identificacion)`, porque están en el mismo esquema. Los docentes de `participantes`, que están en `jsonb`, no la tienen. Todos se validan además en el servicio, dentro de la misma transacción, para responder con su campo y su motivo y no con un error de la base.

## Transiciones

- **Cargo:** `VIGENTE → FINALIZADO` al corregirlo con su fecha de fin. También `FINALIZADO → VIGENTE`, si se corrige un error de captura y no da a la carrera un segundo coordinador. Nunca se borra.
- **Proyecto:** cualquier estado a cualquier otro al corregirlo, porque el diccionario no fija un orden. Nunca se borra.
