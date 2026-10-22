# Contrato: Roles · Responsable de consolidación (F05-H03)

Cambios en `contratos/roles/openapi.yaml`, antes del código (principio IV). El porqué está en [research.md](../research.md) §4.

## Esquemas nuevos

| Esquema | Campos |
|---|---|
| `Responsable` | `periodo` (`^[A-Za-z0-9._-]{1,20}$`), `usuario`, `designadoEn` (`date-time`) |
| `Designacion` | `usuario` (1 a 64) |

## Operaciones nuevas

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarResponsables` | `GET /api/roles/responsables` | `DISTRIBUTIVO:LECTURA` | 200 lista de `Responsable`, por periodo |
| `designarResponsable` | `PUT /api/roles/responsables/{periodo}` | `DISTRIBUTIVO:ESCRITURA` | 200 `Responsable` · 400 (detalle en `usuario`) · 404 · 409 `ROLES_PERIODO_CERRADO` |
| `listarDocentes` | `GET /api/roles/docentes` | `DISTRIBUTIVO:ESCRITURA` | 200 lista de nombres de usuario |
| `recibirEvento` | `POST /interno/roles/eventos` | (token de proceso) | 204 · 400 `COMUN_EVENTO_NO_RECONOCIDO` |

Todas responden también 401, 403, 500 y 503 de `contratos/comun/`. `recibirEvento` usa el sobre `EventoEntreProcesos` de `contratos/comun/eventos.yaml`.

## Error nuevo (`contratos/comun/errores.md`)

| Código | Estado | Título | Descripción |
|---|---|---|---|
| `ROLES_PERIODO_CERRADO` | 409 | Periodo cerrado | El periodo está cerrado y ya no admite cambios. |

## Evento nuevo

`academico.PeriodoActualizado`: `{ "codigo": "2026-2", "estado": "PLANIFICACION" }`. Lo publica Académico (proceso `datos`) al registrar un periodo y en cada cambio de estado; lo recibe Roles.
