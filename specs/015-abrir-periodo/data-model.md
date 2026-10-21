# Data Model: Abrir el periodo académico (F02-H01)

Académico es de soporte (principio II, research §1): un `record` y un `enum` con las reglas, sin agregado elaborado ni eventos.

## Dominio (`ec.mdd.academico.dominio`)

| Tipo | Qué es | Reglas |
|---|---|---|
| `EstadoPeriodo` | `PLANIFICACION`, `EN_APROBACION`, `VIGENTE`, `CERRADO` (glosario §7.3, RF-13) | `siguiente()`: el estado que sigue; vacío en `CERRADO`. |
| `PeriodoAcademico` | `codigo`, `nombre`, `inicio`, `fin`, `estado` | Código `^[A-Za-z0-9._-]{1,20}$`; nombre no vacío, hasta 120 caracteres; `fin` posterior a `inicio`. Uno nuevo empieza en `PLANIFICACION`. |
| `PeriodoAcademico.corregido(nombre, inicio, fin)` | El periodo con sus datos nuevos y la lista de `CambioDeValor` | Lanza `PeriodoCerrado` en `CERRADO`. El código no cambia. |
| `PeriodoAcademico.conEstado(destino, tieneVersion)` | El periodo en el estado de destino | Mismo estado: sin cambios. `CERRADO`: `PeriodoCerrado`. Destino distinto del siguiente: `TransicionNoPermitida` (con el actual y el siguiente). Salir de `PLANIFICACION` sin versión: `PeriodoSinVersion`. |
| `DatoInvalido` | Un dato que no cumple su regla, con el campo y el motivo en español | Se traduce a 400 con un detalle por campo. |
| `CambioDeValor` | `campo`, `anterior`, `nuevo` | Como en Normativa, pero propio de Académico (los servicios no comparten clases, principio V). |

La versión de normativa **no** es parte del periodo: la guarda Normativa (research §2).

## Puertos

- **Entrada** (`puertos.entrada`, interfaz `api` para Cargas en F01):
  - `ConsultarPeriodos`: `todos()` y `porCodigo(codigo)`, cada uno con su versión (`PeriodoConsultado`: el periodo y un `OptionalInt`; no se llama `PeriodoConVersion` para no confundirlo con la excepción de Normativa);
  - `RegistrarPeriodo`, `CorregirPeriodo`, `AbrirPeriodo` y `CambiarEstadoPeriodo`, con el usuario de la sesión;
  - las excepciones que lanzan: `PeriodoNoEncontrado`, `CodigoRepetido`, `PeriodoConOtraVersion`, `VersionInexistente`.
- **Salida** (`puertos.salida`):
  - `RepositorioPeriodos`: `todos()`, `porCodigo`, `bloquear(codigo)` (`FOR UPDATE`), `registrar` y `guardar`;
  - `RegistroDeCambios`: `registrar(CambioDePeriodo)`;
  - `NormativaDelPeriodo`: `versionDe(codigo)` y `asociar(codigo, version, quien)` (research §4);
  - `Transaccion`: como en Normativa.

## Esquema (`academico`, `V2__periodos_academicos.sql`)

```text
periodo_academico
  codigo         varchar(20)  PK, check (codigo ~ '^[A-Za-z0-9._-]{1,20}$')
  nombre         varchar(120) not null, check (length(trim(nombre)) > 0)
  inicio         date         not null
  fin            date         not null, check (fin > inicio)
  estado         varchar(16)  not null, check (estado in ('PLANIFICACION','EN_APROBACION','VIGENTE','CERRADO'))
  registrado_en  timestamptz  not null
  registrado_por varchar(64)  not null

registro_cambio
  id       uuid         PK
  momento  timestamptz  not null
  usuario  varchar(64)  not null
  accion   varchar(30)  not null, check (accion in ('PERIODO_REGISTRADO','PERIODO_CORREGIDO','PERIODO_ABIERTO','PERIODO_ESTADO_CAMBIADO'))
  detalle  jsonb        not null
```

- Sin claves foráneas hacia Normativa: es otro esquema y otro servicio (principio V).
- Sin datos precargados: los periodos se registran a mano (aclaración del 2026-10-21).

## Transiciones

```text
PLANIFICACION ──(con versión)──▶ EN_APROBACION ──▶ VIGENTE ──▶ CERRADO
      ▲
      └── abrir: asociar la versión (solo aquí)
```

Ningún retroceso ni salto. En `CERRADO` no cambia nada: ni datos, ni estado, ni versión.
