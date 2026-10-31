# Contrato: cambios en `contratos/`

## `contratos/generacion/openapi.yaml`

### `consultarAfinidadDelDocente` · `GET /api/generacion/docentes/{identificacion}/afinidad`

- **Permiso**: `x-mdd-permiso: [DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, los de `consultarDocente` (D-70).
- **Parámetros**:
  - `identificacion` (ruta): `^[0-9A-Z]{1,13}$`, `NumeroDeIdentificacion` del contrato de Docentes;
  - `periodo` (query, obligatorio): `^[A-Za-z0-9._-]{1,20}$`, la regla de Académico.
- **200**: `AfinidadDelDocente`.
- **400**: `COMUN_SOLICITUD_INVALIDA` si no cumplen su formato.
- **404**: `GENERACION_PERIODO_NO_ENCONTRADO` o `GENERACION_DOCENTE_NO_ENCONTRADO`.
- **409**: `GENERACION_PERIODO_SIN_ABRIR`.
- **502**: `COMUN_PROCESO_CON_ERROR`. **504**: `COMUN_PROCESO_SIN_RESPUESTA`.
- **401, 403, 500, 503**: los comunes.

### Esquemas

- **`AfinidadDelDocente`:** `{ periodo, version (entero), puntajes: PuntajeAfinidad[] }`, del mayor puntaje al menor y, a igual puntaje, por nombre. Vacía si el periodo no tiene oferta.
- **`PuntajeAfinidad`:** `{ asignatura, nombre, total (number), topeAplicado (boolean), desglose: AporteDeAfinidad[] }`.
- **`AporteDeAfinidad`:** `{ tipo, nombre, coincidencia, aporte (number) }`.
- **`TipoDeAporte`:** enum `TERCER_NIVEL`, `CUARTO_NIVEL`, `EXPERIENCIA`, `CURSO`.
- **`CoincidenciaDeCampo`:** enum `CAMPO_ESPECIFICO`, `CAMPO_AMPLIO`.

## Rutas internas (ADR-023 propuesto)

Con la etiqueta `Interno`, sin `x-mdd-permiso`: solo el rol `PROCESO`, por ruta.

| Contrato | Operación | Ruta | 200 | 404 |
|---|---|---|---|---|
| `contratos/academico` | `consultarAsignaturasOfertadas` | `GET /interno/academico/periodos/{codigo}/asignaturas-ofertadas` | `AsignaturaOfertada[]` | El periodo no existe |
| `contratos/normativa` | `consultarPesosDelPeriodo` | `GET /interno/normativa/periodos/{periodo}/pesos-de-afinidad` | `PesosDelPeriodo` | Sin versión |
| `contratos/docentes` | `consultarFormacion` | `GET /interno/docentes/{identificacion}/formacion` | `FormacionDelDocente` | El docente no existe |

## Catálogo (`contratos/comun/errores.md`), con sus 8 columnas (D-72) y en «tú» (D-74)

| Código | Estado | Título | Descripción | Clasificación |
|---|---|---|---|---|
| `GENERACION_PERIODO_NO_ENCONTRADO` | 404 | Periodo no encontrado | El periodo no está en los ciclos académicos. Elige otro. | Negocio |
| `GENERACION_PERIODO_SIN_ABRIR` | 409 | Periodo sin abrir | El periodo todavía no tiene su versión de normativa. Ábrelo en «Periodos» para calcular la afinidad. | Negocio |
| `GENERACION_DOCENTE_NO_ENCONTRADO` | 404 | Docente no encontrado | No hay un docente con esa identificación. Revísala y vuelve a intentarlo. | Negocio |
| `COMUN_PROCESO_CON_ERROR` | 502 | Servicio con problemas | Una parte del sistema tuvo un problema y no se pudo completar la consulta. Intenta de nuevo en unos minutos. | Sistema |
| `COMUN_PROCESO_SIN_RESPUESTA` | 504 | Servicio sin respuesta | Una parte del sistema tardó demasiado en responder. Intenta de nuevo en unos minutos. | Sistema |
