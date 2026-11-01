# Contrato: Generación

Cambios en `contratos/generacion/openapi.yaml`.

## `consultarCargaHoraria` (nueva)

`GET /api/generacion/periodos/{periodo}/docentes/{docente}/carga-horaria`

- **Permiso:** `[OFERTA:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, sin filtro de carrera (D-82).
- **200:** `CargaHoraria`.

| Campo | Requerido | Descripción |
|---|---|---|
| `docente` | Sí | Identificación |
| `nombreDocente` | Sí | Nombre |
| `horasContratadas` | Sí | Entero |
| `totales` | Sí | `HorasPorComponente` |
| `asignadas` | Sí | La suma de los cinco componentes |
| `disponibles` | Sí | Nunca menos de 0 |
| `exceso` | Sí | Lo que pasa de las contratadas; 0 si no se pasa |
| `proporcion` | No | `clase` y `otrasDocencia`; no viene si no hay horas en ninguna |
| `meta` | Sí | `clase` y `otrasDocencia`, de la normativa |

| Estado | Código |
|---|---|
| 400 | Periodo o docente con formato inválido |
| 401, 403 | Comunes |
| 409 | `GENERACION_DOCENTE_NO_ACTIVO`, `GENERACION_SIN_DISTRIBUTIVO`, también para un periodo que no existe, o `GENERACION_PERIODO_SIN_ABRIR` |
| 500, 502, 503, 504 | Comunes |

El estado de cada código es el que ya tiene en `contratos/comun/errores.md`.

## `HorasPorComponente` (cambia)

Agrega `clase`. Ahora son horas de todas las carreras.

## `ActividadesDelDocente` (cambia)

`horasPorComponente` pasa a `totales`, de tipo `HorasPorComponente`.

`consultarActividades` agrega las respuestas 502, 503 y 504, porque ahora lee la oferta.
