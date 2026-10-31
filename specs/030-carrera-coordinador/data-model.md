# Data Model: Indicar la carrera del coordinador

## Esquema `roles`

### `carrera_de_coordinador` (tabla nueva, `V8__carrera_del_coordinador.sql`)

| Columna | Tipo | Regla |
|---|---|---|
| `nombre_usuario` | `varchar(64)` | Llave primaria: una carrera por coordinador. Normalizado, como en `asignacion_rol`. |
| `carrera` | `varchar(20)`, el largo de `CodigoDeCarrera` | Sin llave foránea: es de otro servicio (principio V). |
| `indicada_en` | `timestamptz` | |
| `indicada_por` | `varchar(64)` | |

### `registro_cambio` (cambia)

La restricción de `accion` suma `INDICAR_CARRERA` y `QUITAR_CARRERA`.

## Roles

- **`AsignarCarrera`** (puerto de entrada):
  - `indicar(usuario, carrera, quien)`: exige el rol; reemplaza la que tenga;
  - `quitar(usuario, quien)`: falla si no tiene;
  - `carreras()`: por usuario, para `listarRolesDeUsuarios`.
- **`RepositorioCarrerasDeCoordinadores`** (puerto de salida).
- **`AsignarRolesServicio`** (cambia): recibe `Transaccion`; al quitar el rol, borra la carrera y lo registra, en la misma transacción.
- **`Autorizacion`** (cambia): suma `carrera`, opcional.

## Usuarios

- **`AutorizacionDeUsuario`** (cambia): suma `carrera`, opcional.
- **`EmisorJwt`** (cambia): el reclamo `carrera`, solo si existe.

## `:plataforma:seguridad`

- **`AutoridadesDelToken`** (cambia): suma `ALCANCE_PROPIO_<FUNCIONALIDAD>:<TIPO>` por cada permiso con `:PROPIO`, y `CARRERA_<código>` si el token trae carrera.

## `:plataforma:errores-nucleo`

- **`AlcanceDeCarrera.propia(Collection<String> autoridades)`:** `Optional<String>`. Vacío: ve todas. Con valor: solo esa. Lanza `CarreraSinIndicar` si es solo «lo propio» y no tiene carrera.
- **`CarreraSinIndicar`:** responde `COMUN_CARRERA_SIN_INDICAR`, con su catálogo `ErroresDeAlcance` en `:plataforma:errores`.

## Académico

- **`OfertaDelPeriodo.deCarrera(carrera)`:** la misma oferta, con solo los paralelos de esa carrera.
- **`ConsultarAsignaturasOfertadas.delPeriodo(periodo, carrera)`** (cambia): `Optional<String> carrera`.

## Generación

- **`ConsultarSugerencia.sugerencia(periodo, asignatura, paralelo, carrera)`** y **`ConsultarAfinidad.afinidad(identificacion, periodo, carrera)`** (cambian): `Optional<String> carrera`.
- **`DatosParaAfinidad.asignaturasOfertadas(periodo, carrera)`** (cambia).
- **`OtraCarrera`** (excepción de `puertos.entrada`).

## Transiciones

Sin carrera → con carrera, al indicarla. Con carrera → sin carrera, al quitarla o al quitar el rol.
