# Contrato: Cargas · plantillas (F01-H01)

Cambios en `contratos/cargas/openapi.yaml`, antes del código (principio IV). El porqué está en [research.md](../research.md) §6 a §8.

## Esquemas nuevos

| Esquema | Campos |
|---|---|
| `Obligatoriedad` | enum `OBLIGATORIA`, `OBLIGATORIA_SALVO_SIN_PROYECTOS`, `RECOMENDADA` |
| `ValorDeCatalogo` | `codigo`, `descripcion` |
| `ColumnaDeCarga` | `campo`, `descripcion`, `tipo`, `obligatorio`, `ejemplo`; opcionales `longitud` (entero), `formato`, `clave`, `validacion`, `valores` (lista de `ValorDeCatalogo`, si la columna tiene catálogo) |
| `Carga` | `numero` (1 a 10), `codigo` (`^[a-z_]{1,40}$`), `nombre`, `archivo`, `dependeDe` (números), `obligatoriedad`, `columnas` |
| `Convencion` | `tema`, `regla` |
| `DiccionarioDeDatos` | `convenciones`, `cargas` |

## Parámetro nuevo

`CodigoDeCarga`, en la ruta: `^[a-z_]{1,40}$`, ejemplo `estructura_organica`.

## Operaciones nuevas (tag `Plantillas`)

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarCargas` | `GET /api/cargas/plantillas` | `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` | 200 `DiccionarioDeDatos` |
| `descargarPlantilla` | `GET /api/cargas/plantillas/{codigo}/plantilla` | `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` | 200 `text/csv` con `Content-Disposition` · 404 |
| `descargarDiccionario` | `GET /api/cargas/plantillas/{codigo}/diccionario` | `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` | 200 `text/csv` con `Content-Disposition` · 404 |

- Todas responden también 401, 403, 500 y 503 comunes, en `application/json`.
- El 404 es `COMUN_RECURSO_NO_ENCONTRADO` (`CargaNoEncontrada`), como `UnidadNoEncontrada` en Académico.
- El `text/csv` es `type: string`. El encabezado `Content-Disposition` se declara en la respuesta 200: `attachment; filename="01_estructura_organica.csv"`.
- Una ruta con un código que no cumple el patrón responde 400 `COMUN_SOLICITUD_INVALIDA`.

## Errores

Sin códigos nuevos en `contratos/comun/errores.md`.

## SPA

- `generar:api` agrega `contratos/cargas/openapi.yaml` → `src/api/generado/cargas.d.ts`.
- `apiCargas` en `src/api/cliente.ts`, con el mismo token y la misma actividad que los demás clientes.
- `verificar:ruptura` cubre el contrato de cargas como los demás.
