# Contrato: la vista previa, la confirmación y el historial (F01-H04)

`contratos/cargas/openapi.yaml` cambia antes del código (principio IV). El porqué está en [research.md](../research.md) §2, §3, §9 y §11.

## `validarArchivo` (amplía la de F01-H03)

**200** `LecturaDeArchivo`, que agrega `vistaPrevia` (`VistaPrevia`):

| Campo | Tipo |
|---|---|
| `filasConErrores` | entero ≥ 0 |
| `nuevos`, `cambian`, `bajas`, `iguales` | enteros ≥ 0; ausentes si hay errores |
| `registros` | `RegistroDeLaVista[]`, hasta 100 de cada tipo, sin los iguales |
| `huella` | texto de 64 caracteres hexadecimales; ausente si hay errores |

`RegistroDeLaVista`: `clave` (texto), `tipo` (`NUEVO`, `CAMBIA`, `BAJA`), `fila` (entero ≥ 2, ausente en una baja) y `cambios` (`[{campo, ahora, archivo}]`, solo en `CAMBIA`).

## `confirmarCarga` (nueva)

`POST /api/cargas/plantillas/{codigo}/archivo/confirmacion`, con `x-mdd-permiso: CARGAS:ESCRITURA` y `multipart/form-data` con `archivo` y `huella`.

- **200** `ResultadoDeConfirmacion`:
  - `resultado`: `APLICADA` o `REPETIDA`;
  - `nuevos`, `cambiados` y `bajasSinAplicar`;
  - `confirmadaEl` (fecha y hora; en una repetida, la de la primera).
- **409**, con los códigos nuevos:
  - `CARGAS_CARGA_CON_ERRORES`;
  - `CARGAS_VISTA_PREVIA_DESACTUALIZADA`;
  - `CARGAS_CARGA_RECHAZADA`, con un detalle por rechazo: `campo` es `fila N · campo` y `descripcion`, el motivo del dueño.
- **400**, **404** y **413**: como `validarArchivo`.

## `listarHistorial` (nueva)

`GET /api/cargas/plantillas/{codigo}/historial`, con `x-mdd-permiso: [CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`.

- **200** `EntradaDeHistorial[]`, de la más reciente a la más antigua: `usuario`, `momento`, `archivo`, `hash`, `resultado` (`APLICADA`, `RECHAZADA`, `REPETIDA`), `nuevos`, `cambiados`, `bajasSinAplicar` y `motivo` (código, opcional).
- **404:** la carga no existe.

## Catálogo

Los 3 códigos nuevos, con sus 8 columnas en `contratos/comun/errores.md` (D-72).
