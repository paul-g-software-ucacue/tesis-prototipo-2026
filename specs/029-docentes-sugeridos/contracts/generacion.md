# Contrato: cambios en `contratos/`

## `contratos/generacion/openapi.yaml`

### `consultarSugerencia` · `GET /api/generacion/periodos/{periodo}/oferta/{asignatura}/{paralelo}/sugerencia`

- **Permiso**: `x-mdd-permiso: [DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, los de `consultarAfinidadDelDocente` (D-70).
- **Parámetros**, con las reglas de Académico:
  - `periodo` (ruta): `^[A-Za-z0-9._-]{1,20}$`;
  - `asignatura` (ruta): `^[0-9A-Z-]{1,30}$`;
  - `paralelo` (ruta): `^[0-9A-Z_-]{1,20}$`.
- **200**: `Sugerencia`.
- **400**: `COMUN_SOLICITUD_INVALIDA` si no cumplen su formato.
- **404**: `GENERACION_PARALELO_NO_ENCONTRADO`.
- **409**: `GENERACION_PARALELO_RETIRADO` o `GENERACION_PERIODO_SIN_ABRIR`.
- **502**: `COMUN_PROCESO_CON_ERROR`. **504**: `COMUN_PROCESO_SIN_RESPUESTA`.
- **401, 403, 500, 503**: los comunes.

### Esquemas

- **`Sugerencia`:** `{ periodo, asignatura, nombre, paralelo, horasDelParalelo (number), troncoComun (boolean), version (entero), minimoAfinidad (number), fueraPorElMinimo (entero), candidatos: Candidato[] }`.
- **`Candidato`:** `{ identificacion, nombre, dedicacion, horasContratadas (entero), horasDisponibles (number), horasEnOtrasCarreras (number), horasSuficientes (boolean), puntaje: PuntajeAfinidad }`.
- **`Dedicacion`:** enum `TC`, `MT`, `TP`.
- **`PuntajeAfinidad`:** el de F06-H01, sin cambios.

## Rutas internas (ADR-023)

Con la etiqueta `Interno`, sin `x-mdd-permiso`: solo el rol `PROCESO`, por ruta.

| Contrato | Operación | Ruta | 200 | 404 |
|---|---|---|---|---|
| `contratos/academico` | `consultarParaleloDeLaOferta`, nueva | `GET /interno/academico/periodos/{codigo}/oferta/{asignatura}/{paralelo}` | `ParaleloDeLaOferta` | El periodo o el paralelo no existen |
| `contratos/docentes` | `consultarDocentesActivos`, nueva | `GET /interno/docentes/activos` | `DocenteActivo[]` | No aplica |
| `contratos/normativa` | `consultarPesosDelPeriodo`, ampliada | `GET /interno/normativa/periodos/{periodo}/pesos-de-afinidad` | `PesosDelPeriodo` con `minimoAfinidad` (obligatorio) | Sin versión |

- **`ParaleloDeLaOferta`:** `{ asignatura, nombre, carrera, paralelo, campoAmplio, campoEspecifico, troncoComun, horasSemanales (number, las efectivas), estado }`. Una retirada responde 200, con su estado.
- **`DocenteActivo`:** `{ identificacion, nombre, dedicacion, horasContratadas, formacion: FormacionDelDocente }`. Sin datos de contacto ni de contrato (principio IX).

## Catálogo (`contratos/comun/errores.md`), con sus 8 columnas (D-72) y en «tú» (D-74)

| Código | Estado | Título | Descripción | Clasificación |
|---|---|---|---|---|
| `GENERACION_PARALELO_NO_ENCONTRADO` | 404 | Asignatura-paralelo no encontrada | Esa asignatura-paralelo no está en la oferta del periodo. Revisa la oferta y elige otra. | Negocio |
| `GENERACION_PARALELO_RETIRADO` | 409 | Asignatura-paralelo retirada | Esta asignatura-paralelo se retiró de la oferta y no recibe sugerencias. Vuelve a ofertarla si hace falta. | Negocio |
