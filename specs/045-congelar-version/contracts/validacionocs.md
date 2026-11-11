# Contrato: Validación OCS

Archivo: `contratos/validacionocs/openapi.yaml`.

## `POST /api/validacionocs/periodos/{periodo}/aprobacion` (nuevo)

- `operationId: aprobarContenido`, `tags: [Aprobacion]`, `x-mdd-permiso: DISTRIBUTIVO:APROBACION`.
- Sin cuerpo (sin comentario, decisión del autor).

| Respuesta | Cuándo |
|---|---|
| 200 `EstadoDelFlujo` | Aprobado: el distributivo ya está en CONGELADO_EN_FIRMA, con `congelacion` |
| 400 | Periodo mal formado |
| 401, 403 | Sin sesión, sin `DISTRIBUTIVO:APROBACION`, o sin el rol de EN_REVISION_VRA en la copia de la configuración (`RolDeOtraEtapa`, que el controlador traduce a 403, como en enviar y devolver) |
| 404 | Sin distributivo (`VALIDACIONOCS_SIN_DISTRIBUTIVO`) o periodo inexistente |
| 409 | Otra etapa (`VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION`) o periodo cerrado (`VALIDACIONOCS_PERIODO_CERRADO`) |
| 500, 502, 504 | Los comunes |

## `EstadoDelFlujo` (cambia)

- `acciones` admite `APROBAR_CONTENIDO`.
- `congelacion` (opcional): `{version: integer ≥ 1, hash: string (^[0-9a-f]{64}$), quien: string, momento: date-time}`. Está presente cuando el distributivo pasó a CONGELADO_EN_FIRMA.

## `enviarARevision` (cambia la descripción)

Si la siguiente etapa activa es CONGELADO_EN_FIRMA (EN_REVISION_VRA desactivada), congela la versión antes de avanzar (research §3).

## Catálogo (`contratos/comun/errores.md`)

| Código | HTTP | Mensaje |
|---|---|---|
| `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION` | 409 | El distributivo no está en revisión del Vicerrectorado: no se puede aprobar su contenido. |

## Bitácora

Una entrada `CONTENIDO_APROBADO` por aprobación, con periodo, versión y hash. Un intento rechazado no deja entrada.
