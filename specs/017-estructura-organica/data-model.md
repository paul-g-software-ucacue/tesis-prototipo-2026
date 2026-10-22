# Data Model: Mantener la estructura orgánica (F03-H01)

## Dominio (`ec.mdd.academico.dominio`)

| Tipo | Qué es | Reglas |
|---|---|---|
| `TipoDeUnidad` | `SEDE_MATRIZ`, `SEDE`, `EXTENSION`, `CAMPUS`, `GOBIERNO`, `UNIDAD_ACADEMICA`, `GESTION`, `COMISION` | `esSede()` para los cuatro primeros. |
| `GestionAutorizada` | `codigo` (`G01`…`G21`) y `nombre` | Solo las del catálogo precargado. |
| `UnidadOrganica` | `codigo`, `nombre`, `tipo`, `padre` (código o vacío), `gestionAutorizada` (código o vacío), `comisionPermanente` (o vacío), `activa` | Reglas de su tipo (research §3); `corregida(nombre, padre, gestion, permanente)` no cambia código ni tipo; `conEstado(activa)`. |
| `EstructuraOrganica` | Todas las unidades | `registrar(unidad, catalogo)`, `corregir(...)`, `inactivar(codigo)`, `reactivar(codigo)`: padre existe y activo, sin ciclos, una sede matriz, sin dependientes activos al inactivar. Lanza `DatoInvalido` (400) o las excepciones de §5 de research. |

## Esquema `academico` (`V3__estructura_organica.sql`)

```text
gestion_autorizada
  codigo   varchar(3)   PK, check (codigo ~ '^G(0[1-9]|1[0-9]|2[01])$')
  nombre   varchar(150) not null
  -- los 21 de POL 6, precargados

unidad_organica
  codigo               varchar(20)  PK, check (codigo ~ '^[A-Z0-9_-]{1,20}$')
  nombre               varchar(150) not null, check (length(trim(nombre)) > 0)
  tipo                 varchar(20)  not null, check (tipo in (...los 8...))
  padre                varchar(20)  references unidad_organica (codigo)
  gestion_autorizada   varchar(3)   references gestion_autorizada (codigo)
  comision_permanente  boolean
  activa               boolean      not null
  registrada_en        timestamptz  not null
  registrada_por       varchar(64)  not null
  check ((tipo = 'SEDE_MATRIZ') = (padre is null))
  check ((tipo = 'GESTION') = (gestion_autorizada is not null))
  check ((tipo = 'COMISION') = (comision_permanente is not null))
  unique index una_sede_matriz on unidad_organica ((true)) where tipo = 'SEDE_MATRIZ'

registro_cambio.accion: + UNIDAD_REGISTRADA, UNIDAD_CORREGIDA, UNIDAD_INACTIVADA, UNIDAD_REACTIVADA
```

## Puertos

- **Entrada:** `ConsultarEstructura` (`unidades()`, `unidad(codigo)`, `gestionesAutorizadas()`) y `MantenerEstructura` (`registrar`, `corregir`, `cambiarEstado`), con el usuario de la sesión. F03-H02, F03-H04 y F01 usarán `ConsultarEstructura`.
- **Salida:** `RepositorioEstructura` (`todas()`, `catalogo()`, `bloquear()` —un candado de la estructura, para que dos cambios no se crucen—, `registrar`, `guardar`), y el `RegistroDeCambios` y la `Transaccion` que ya existen.
