# Contrato: cambios en `contratos/cargas/openapi.yaml` y `contratos/comun/errores.md`

## Operaciones nuevas

### `consultarEstadoDeLasCargas` · `GET /api/cargas/estado`

- **Permiso**: `x-mdd-permiso: [CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`, el mismo que `listarCargas` (FR-016).
- **200**: `EstadoDeLasCargas`, con las 10 cargas en su orden.
- **401, 403, 500, 503**: los comunes.

### `confirmarSinProyectos` · `POST /api/cargas/sin-proyectos`

- **Permiso**: `x-mdd-permiso: CARGAS:ESCRITURA`, el mismo que `confirmarCarga` (D-71).
- **Cuerpo** (`application/json`), `ConfirmacionSinProyectos`:
  - `periodo`: obligatorio; `string`, `maxLength: 10`, `pattern: '^[0-9]{4}-(I|II)$'`.
  - Sin `additionalProperties: false`, como los demás contratos del proyecto: el servidor ignora los campos que no conoce.
- **201**: `EntradaDeHistorial`, la confirmación nueva.
- **200**: `EntradaDeHistorial`, la que ya estaba (FR-012).
- **400**:
  - `COMUN_SOLICITUD_INVALIDA` si el periodo falta o no cumple el formato;
  - `CARGAS_PERIODO_NO_ENCONTRADO` si no está en los ciclos académicos.
- **409**: `CARGAS_PERIODO_CON_PROYECTOS`.
- **401, 403, 500, 503**: los comunes.

## Operaciones que cambian

### `validarArchivo` y `confirmarCarga`

- **409** suma `CARGAS_CARGAS_ANTERIORES_PENDIENTES`. Sus `detalles`, uno por carga que falta, llevan:
  - `campo`: el código de esa carga;
  - `motivo`: su número y su nombre.

  Se responde antes de leer el archivo.

## Esquemas nuevos o que cambian

- **`EstadoDeLasCargas`:** `{ cargas: EstadoDeCarga[] }`, con 10 elementos.
- **`EstadoDeCarga`** (todos los campos obligatorios):
  - `numero`: entero, 1–10;
  - `codigo`: `^[a-z_]{1,40}$`;
  - `estado`: `EstadoDeLaCarga`;
  - `faltan`: `CargaQueFalta[]`;
  - `periodosSinProyectos`: `string[]`, con el patrón del periodo; vacío salvo en la 9.
- **`EstadoDeLaCarga`:** enum `PENDIENTE`, `CARGADA`, `SIN_PROYECTOS`.
- **`CargaQueFalta`:** `{ numero, codigo, nombre }`.
- **`EntradaDeHistorial`:**
  - `archivo` y `hash` dejan de ser obligatorios (no van en `SIN_PROYECTOS`);
  - suma `periodo`, opcional, con el patrón del periodo;
  - `ResultadoDeHistorial` suma `SIN_PROYECTOS`.
- **`ConfirmacionSinProyectos`:** `{ periodo }`, como arriba.

## Catálogo (`contratos/comun/errores.md`), con sus 8 columnas (D-72) y en «tú» (D-74)

| Código | Estado | Título | Descripción | Causa | Clasificación | Acción | Descripción técnica |
|---|---|---|---|---|---|---|---|
| `CARGAS_CARGAS_ANTERIORES_PENDIENTES` | 409 | Faltan cargas anteriores | Esta carga depende de otras que todavía no tienen datos. | Una carga de su `dependeDe` no tiene registros vigentes en su servicio dueño. | Negocio | Sube y confirma antes las cargas que faltan, o registra sus datos en su pantalla, y vuelve a intentarlo. | `OrdenDeLasCargas.faltantes` no vacío; los detalles nombran cada carga. |
| `CARGAS_PERIODO_NO_ENCONTRADO` | 400 | Periodo no encontrado | El periodo no está en los ciclos académicos. | El código tiene buen formato, pero Académico no tiene ese periodo. | Validación | Elige un periodo de la lista o carga antes sus ciclos académicos. | `ConsultarProyectos.listar` lanzó `DatoInvalido` en `periodo`. |
| `CARGAS_PERIODO_CON_PROYECTOS` | 409 | El periodo tiene proyectos | No puedes confirmar «sin proyectos»: el periodo tiene proyectos aprobados. | Al menos un proyecto es asignable en el periodo (F03-H05). | Negocio | Revisa los proyectos del periodo; si no deberían contar, corrige su estado o su vigencia. | `ProyectosDelPeriodo.hayAsignables` devolvió verdadero. |
