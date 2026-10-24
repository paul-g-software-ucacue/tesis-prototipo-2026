# Data Model: Mantener los docentes con su ficha académica (F03-H04)

Las reglas de cada campo están en [research.md](research.md) §3; aquí, lo que se guarda y sus relaciones.

## Dominio (`ec.mdd.docentes.dominio`)

| Tipo | Clase | Contenido |
|---|---|---|
| `Docente` | *record* | `identificacion` (`Identificacion`), `nombres` (`NombresDelDocente`), `correo`, `telefono` (opcional), `tipoDePersonal`, `categoria`, `dedicacion`, `horasContratadas`, `fechaDeIngreso`, `inicioDeContrato`, `finDeContrato` (opcional), `condicionEspecial`, `carreraBase`, `ficha` (`FichaAcademica`), `estado` (`EstadoLaboral`). `nuevo(…)` lo deja `ACTIVO`; `corregido(…)` conserva la identificación y el estado; `conEstado(…)`; `cambiosDesde(anterior)` |
| `Identificacion` | *record* | `tipo` (`TipoDeIdentificacion`), `numero`; valida la cédula (módulo 10) o el pasaporte |
| `NombresDelDocente` | *record* | `primerApellido`, `segundoApellido`, `primerNombre`, `segundoNombre`; en mayúsculas; `completo()` («PERALTA MALDONADO JUAN ANDRES») |
| `FichaAcademica` | *record* | `tercerNivel` (`TituloTercerNivel`), `cuartoNivel` (lista de `TituloCuartoNivel`), `cursos` (lista de `Curso`), `experiencia` (lista de `ExperienciaLaboral`), `asignaturasAfines` (lista de códigos); cada lista hasta 20 |
| `TituloTercerNivel` | *record* | `titulo`, `registroSenescyt` (opcional), `campo` (`CampoConocimiento`) |
| `TituloCuartoNivel` | *record* | `titulo`, `tipo` (`TipoDeTituloDeCuartoNivel`), `campoEspecifico`, `anio` |
| `Curso` | *record* | `nombre`, `campoEspecifico`, `horas`, `anio` |
| `ExperienciaLaboral` | *record* | `cargo`, `institucion`, `campoEspecifico`, `anios` (`BigDecimal`) |
| `CampoConocimiento` | *record* | `amplio`, `especifico`; el específico empieza por el amplio |
| `Referencias` | *record* | los códigos de carreras, asignaturas y campos amplios que existen, con sus nombres; `verificar(Docente)` lanza `DatoInvalido` en `carreraBase`, `ficha.asignaturasAfines[i]` o `ficha.tercerNivel.campoAmplio` |
| `BusquedaDeDocentes` | *record* | `texto`, `carrera`, `estado` (todos opcionales); `aplicaA(Docente)` sin mayúsculas ni tildes |
| Enumeraciones | `enum` | `TipoDeIdentificacion` (`CEDULA`, `PASAPORTE`), `TipoPersonal` (`TITULAR`, `OCASIONAL`, `INVITADO`, `HONORARIO`), `CategoriaDeEscalafon` (`AUXILIAR_1`, `AUXILIAR_2`, `AGREGADO_1`–`3`, `PRINCIPAL_1`–`3`, `NO_APLICA`), `Dedicacion` (`TC`, `MT`, `TP`), `CondicionEspecial` (`CUIDADOR_SUSTITUTO`, `MATERNIDAD_LACTANCIA`, `APOYO_SIES`, `NINGUNA`), `TipoDeTituloDeCuartoNivel` (`MAESTRIA`, `ESPECIALIZACION`, `DOCTORADO`), `EstadoLaboral` (`ACTIVO`, `LICENCIA`, `INACTIVO`) |
| `DatoInvalido` | excepción | `campo` y motivo (400) |
| `CambioDeValor`, `Cambios` | *record* | campo, valor anterior y nuevo, como en Académico |

## Puertos y aplicación

| Tipo | Paquete | Contenido |
|---|---|---|
| `ConsultarDocentes` | `docentes.puertos.entrada` | `buscar(BusquedaDeDocentes)`, `docente(identificacion)`, `referencias()` |
| `MantenerDocentes` | `docentes.puertos.entrada` | `registrar(Docente)`, `corregir(identificacion, Docente)`, `cambiarEstado(identificacion, EstadoLaboral)` |
| `DocenteNoEncontrado`, `DocenteRepetido`, `CorreoRepetido` | `docentes.puertos.entrada` | 404, 409, 409 |
| `RepositorioDocentes` | `docentes.puertos.salida` | `bloquear()`, `todos()`, `buscar(identificacion)`, `existeCorreo(correo, salvo)`, `guardar(Docente)`, `actualizar(Docente)` |
| `RegistroDeCambios` | `docentes.puertos.salida` | `registrar(accion, detalle)` |
| `ReferenciasAcademicas` | `docentes.puertos.salida` | `referencias()` → `Referencias` |
| `ConsultarDocentesServicio`, `MantenerDocentesServicio` | `docentes.aplicacion` | orquestan: bloquear, leer, decidir con el dominio, guardar y registrar |

## Académico: el puerto publicado nuevo

| Tipo | Paquete | Contenido |
|---|---|---|
| `ConsultarReferencias` | `academico.puertos.entrada` | `referencias()` → `ReferenciasDeAcademico` |
| `ReferenciasDeAcademico` | `academico.puertos.entrada` | `carreras` (`CarreraDeReferencia`: código, nombre, siglas), `asignaturas` (`AsignaturaDeReferencia`: código, nombre, carrera), `camposAmplios` (`CampoAmplioDeReferencia`: código, nombre) |
| `ConsultarReferenciasServicio` | `academico.aplicacion` | los arma con los repositorios de carreras ya existentes |

## Persistencia: `docentes.V2__docentes.sql`

```text
docente
  identificacion       varchar(13)  PK
  tipo_identificacion  varchar(10)  check in (CEDULA, PASAPORTE)
  primer_apellido      varchar(60)  not null
  segundo_apellido     varchar(60)
  primer_nombre        varchar(60)  not null
  segundo_nombre       varchar(60)
  correo_institucional varchar(100) not null    unique index on lower(…)
  telefono             varchar(10)
  tipo_personal        varchar(10)  check in (…)
  categoria_escalafon  varchar(12)  check in (…)
  dedicacion           char(2)      check in (TC, MT, TP)
  horas_contratadas    smallint     check (1..40)
  fecha_ingreso        date         not null
  inicio_contrato      date         not null
  fin_contrato         date         check (fin_contrato > inicio_contrato)
  condicion_especial   varchar(25)  check in (…)
  carrera_base         varchar(20)  not null    (código de Académico, sin FK entre esquemas)
  titulo_tercer_nivel  varchar(150) not null
  registro_senescyt    varchar(30)
  campo_amplio         char(2)      not null
  campo_especifico     char(3)      check (left(campo_especifico, 2) = campo_amplio)
  titulos_cuarto_nivel jsonb        not null default '[]'
  cursos               jsonb        not null default '[]'
  experiencia          jsonb        not null default '[]'
  asignaturas_afines   jsonb        not null default '[]'
  estado               varchar(10)  check in (ACTIVO, LICENCIA, INACTIVO)

registro_cambio
  id uuid PK · momento timestamptz · usuario varchar(64)
  accion varchar(30) check in (DOCENTE_REGISTRADO, DOCENTE_CORREGIDO, DOCENTE_ESTADO_CAMBIADO)
  detalle jsonb
```

## Estados

Cualquier estado pasa a cualquier otro; pedir el mismo estado no cambia nada y no se registra. Ninguno borra al docente.
