# Data Model: Mantener carreras, mallas y asignaturas (F03-H02)

Migración `V4__carreras_mallas_asignaturas.sql` en el esquema `academico` ([research.md](research.md) §2 a §6).

## Tablas

### `campo_amplio` (precargada con los 11 de CINE)

| Columna | Tipo | Reglas |
|---|---|---|
| `codigo` | `char(2)` | clave primaria, `00` a `10` |
| `nombre` | `varchar(80)` | no nulo |

### `carrera`

| Columna | Tipo | Reglas |
|---|---|---|
| `codigo` | `varchar(20)` | clave primaria, `^[0-9A-Z-]+$` |
| `nombre` | `varchar(150)` | no vacío |
| `siglas` | `varchar(10)` | índice único, `^[0-9A-Z]+$` |
| `sede` | `varchar(20)` | clave foránea a `unidad_organica(codigo)` |
| `nivel_formacion` | `varchar(40)` | `check` de los 3 niveles |
| `modalidad` | `varchar(15)` | `check` de las 6 modalidades |
| `jornadas` | `varchar(15)[]` | al menos una, cada una de las 4 |
| `campo_amplio` | `char(2)` | clave foránea a `campo_amplio` |
| `campo_especifico` | `char(3)` | `check`: empieza por `campo_amplio` |
| `campo_detallado` | `char(4)` | nulo, o empieza por `campo_especifico` |
| `numero_ciclos` | `smallint` | 1 a 10 |
| `estado` | `varchar(12)` | `VIGENTE`, `NO_VIGENTE`, `EN_CIERRE` |
| `registrada_en`, `registrada_por` | | como en `unidad_organica` |

### `malla`

| Columna | Tipo | Reglas |
|---|---|---|
| `codigo` | `varchar(20)` | clave primaria, `^[0-9A-Z-]+$` |
| `carrera` | `varchar(20)` | clave foránea a `carrera`; único con `codigo`, para la clave de `asignatura` |
| `estado` | `varchar(10)` | `VIGENTE`, `DEROGADA` |
| `registrada_en`, `registrada_por` | | |

### `asignatura`

| Columna | Tipo | Reglas |
|---|---|---|
| `codigo` | `varchar(30)` | clave primaria, `^[0-9A-Z-]+$` |
| `carrera`, `malla` | `varchar(20)` | clave foránea `(malla, carrera)` a `malla(codigo, carrera)`: la malla es de esa carrera |
| `nombre` | `varchar(150)` | no vacío |
| `ciclo` | `smallint` | 1 a 10 |
| `unidad_organizacion_curricular` | `varchar(25)` | `BASICA`, `PROFESIONAL`, `INTEGRACION_CURRICULAR` |
| `horas_semanales_clase` | `numeric(4,2)` | 0,5 a 20 |
| `horas_practicas_semanales` | `numeric(4,2)` | nulo o 0 a 20 |
| `creditos` | `numeric(4,2)` | nulo o mayor que 0 |
| `tronco_comun` | `boolean` | no nulo |
| `campo_amplio` | `char(2)` | clave foránea a `campo_amplio` |
| `campo_especifico` | `char(3)` | empieza por `campo_amplio` |
| `palabras_clave` | `varchar(300)[]` | al menos 2 |
| `activa` | `boolean` | |
| `registrada_en`, `registrada_por` | | |

### `registro_cambio`

Se recrea `registro_cambio_accion_check` con las 8 acciones de antes y las 9 nuevas (research §6).

## Dominio (`ec.mdd.academico.dominio`)

| Tipo | Qué es |
|---|---|
| `NivelDeFormacion`, `Modalidad`, `Jornada`, `UnidadDeOrganizacionCurricular`, `EstadoDeCarrera`, `EstadoDeMalla` | enumeraciones, iguales al diccionario de datos (una prueba lo compara) |
| `CampoConocimiento` | record: amplio y específico, con la regla del prefijo |
| `CampoAmplio` | record: código y nombre, de la tabla |
| `Carrera` | record con sus reglas; `nueva`, `corregida` → `Correccion`, `conEstado` |
| `Malla` | record: código, carrera, estado; `nueva`, `conEstado` |
| `Asignatura` | record con sus reglas; `nueva`, `corregida`, `conEstado` |
| `DatosMaestros` | las reglas entre entidades de research §4: `registrarCarrera`, `corregirCarrera`, `registrarMalla`, `cambiarEstadoMalla`, `registrarAsignatura`, `corregirAsignatura`, `cambiarEstadoAsignatura` |

Las reglas de una sola entidad lanzan `DatoInvalido` con su campo; los repetidos los decide la aplicación con el repositorio (409, research §4).

## Puertos

| Puerto | Tipo |
|---|---|
| `ConsultarCarreras` | entrada: campos amplios, carreras con sus mallas y sus horas, una carrera, las asignaturas de una carrera, una asignatura |
| `MantenerCarreras` | entrada: las 8 escrituras |
| `CarreraRepetida`, `SiglasRepetidas`, `MallaRepetida`, `AsignaturaRepetida`, `CarreraNoEncontrada`, `MallaNoEncontrada`, `AsignaturaNoEncontrada` | excepciones de entrada |
| `RepositorioCarreras` | salida: `bloquear`, lecturas y escrituras de las tres tablas, `camposAmplios` |
