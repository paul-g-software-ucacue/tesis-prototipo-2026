# Contrato: Académico · estructura orgánica (F03-H01)

Cambios en `contratos/academico/openapi.yaml`, antes del código (principio IV). El porqué está en [research.md](../research.md) §5 y §7.

## Esquemas nuevos

| Esquema | Campos |
|---|---|
| `TipoDeUnidad` | enum de los 8 tipos |
| `GestionAutorizada` | `codigo` (`^G(0[1-9]|1[0-9]|2[01])$`), `nombre` |
| `UnidadOrganica` | `codigo` (`^[A-Z0-9_-]{1,20}$`), `nombre` (1 a 150), `tipo`, `activa`; opcionales `padre`, `gestionAutorizada`, `comisionPermanente` |
| `UnidadNueva` | `codigo`, `nombre`, `tipo`; opcionales `padre`, `gestionAutorizada`, `comisionPermanente` |
| `DatosDeUnidad` | `nombre`; opcionales `padre`, `gestionAutorizada`, `comisionPermanente` |
| `EstadoDeUnidad` | `activa` (boolean) |

## Operaciones nuevas

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarGestionesAutorizadas` | `GET /api/academico/gestiones-autorizadas` | `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` | 200 |
| `listarUnidades` | `GET /api/academico/unidades` | `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` | 200 |
| `consultarUnidad` | `GET /api/academico/unidades/{codigo}` | `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` | 200 · 404 |
| `registrarUnidad` | `POST /api/academico/unidades` | `CATALOGOS:ESCRITURA` | 201 · 400 · 409 `ACADEMICO_UNIDAD_REPETIDA` o `ACADEMICO_SEDE_MATRIZ_EXISTENTE` |
| `corregirUnidad` | `PUT /api/academico/unidades/{codigo}` | `CATALOGOS:ESCRITURA` | 200 · 400 · 404 |
| `cambiarEstadoUnidad` | `PUT /api/academico/unidades/{codigo}/estado` | `CATALOGOS:ESCRITURA` | 200 · 400 · 404 · 409 `ACADEMICO_UNIDAD_CON_DEPENDIENTES` |

Todas responden también 401, 403, 500 y 503 comunes. `x-mdd-permiso` como lista es D-70.

## Errores nuevos (`contratos/comun/errores.md`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `ACADEMICO_UNIDAD_REPETIDA` | 409 | Unidad repetida | Ya existe una unidad con ese código. Elige otro. |
| `ACADEMICO_SEDE_MATRIZ_EXISTENTE` | 409 | Sede matriz existente | La institución ya tiene su sede matriz. Registra las demás como sede, extensión o campus. |
| `ACADEMICO_UNIDAD_CON_DEPENDIENTES` | 409 | Unidad con dependientes | La unidad tiene unidades activas que dependen de ella. Inactívalas antes. |
