# Data Model: Designar al Responsable de consolidación (F05-H03)

## Dominio de Roles (`ec.mdd.roles.dominio`)

| Tipo | Qué es | Reglas |
|---|---|---|
| `EstadoConocido` | El estado de un periodo según la copia de Roles: `PLANIFICACION`, `EN_APROBACION`, `VIGENTE`, `CERRADO` | `masAvanzado(otro)`: los estados solo avanzan, así que gana el posterior (research §1). `cerrado()`. |
| `PeriodoConocido` | `codigo` y `estado`, copiados de Académico | Se actualiza con `conEstado(otro)`, que nunca retrocede. |
| `Designacion` | `periodo`, `usuario`, `designadoPor`, `designadoEn` | `de(periodo, usuario, quien, rolesDelUsuario, anterior)`: el periodo no está cerrado (`PeriodoCerrado`); el usuario tiene `DOCENTE` y no es quien designa (`DesignacionInvalida`, con el campo `usuario` y el motivo en español); con el mismo usuario que la anterior, sin cambios. |

## Puertos de Roles

- **Entrada** (interfaz `api`): `DesignarResponsable`, `ConsultarResponsables` (para la pantalla y para F09 y F10), `ConsultarDocentes` y `RegistrarPeriodoConocido` (el receptor del evento).
- **Salida:** `RepositorioDesignaciones` (`todas()`, `dePeriodo`, `guardar`, `bloquearPeriodo`, `periodosConDesignacionAbierta(usuario)`), `RepositorioPeriodosConocidos` y `RepositorioEventosRecibidos` (`registrarSiEsNuevo(idEvento)`).

## Esquema `roles` (migración nueva)

```text
periodo_conocido
  codigo         varchar(20)  PK, check (codigo ~ '^[A-Za-z0-9._-]{1,20}$')
  estado         varchar(16)  not null, check (estado in ('PLANIFICACION','EN_APROBACION','VIGENTE','CERRADO'))
  actualizado_en timestamptz  not null

designacion_responsable
  periodo        varchar(20)  PK, references periodo_conocido (codigo)
  usuario        varchar(64)  not null
  designado_en   timestamptz  not null
  designado_por  varchar(64)  not null

evento_recibido
  id_evento      uuid         PK
  tipo           varchar(80)  not null
  origen         varchar(40)  not null
  recibido_en    timestamptz  not null

registro_cambio.accion: se agrega 'DESIGNAR_RESPONSABLE'
```

- La clave foránea a `periodo_conocido` es dentro del mismo esquema: la copia es de Roles.
- Sin datos precargados (research §1).

## Académico (`ec.mdd.academico`)

- **Puerto de salida `AvisosDelPeriodo`**: `actualizado(PeriodoAcademico)`.
- **Evento** `academico.PeriodoActualizado`, origen `academico`, destino `roles`, contenido `{codigo, estado}`; lo publican `RegistrarPeriodoServicio` y `CambiarEstadoPeriodoServicio` dentro de su transacción.

## Token

`ConsultarAutorizacion` = roles asignados ∪ `{RESPONSABLE_CONSOLIDACION}` si hay al menos una designación del usuario en un periodo cuya copia no está cerrada.
