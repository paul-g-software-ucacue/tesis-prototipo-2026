# Contrato: Identidad (Usuarios)

## `GET /interno/usuarios/{usuario}/identificacion` (en `contratos/usuarios/openapi.yaml`)

- `operationId: consultarIdentificacion`.
- Solo entre procesos, con token de proceso (ADR-023). El API Gateway no enruta `/interno/`.
- La usa Firma para saber si quien pide es el docente de un cargo (research §2).

| Respuesta | Cuándo |
|---|---|
| 200 `{identificacion}` | La cuenta existe y tiene identificación (`^[0-9A-Z]{1,13}$`) |
| 404 `USUARIOS_SIN_IDENTIFICACION` | La cuenta no existe o no tiene identificación; el mismo cuerpo en los dos casos |
| 400, 401, 403, 500 | Los comunes |

## Catálogo

| Código | HTTP | Título | Descripción |
|---|---|---|---|
| `USUARIOS_SIN_IDENTIFICACION` | 404 | Sin identificación | La cuenta no tiene número de identificación. |

## Lo que no cambia

- `UsuarioResumen` (`listarUsuarios`) y la sesión no llevan la identificación (FR-013).
- El token no la lleva.

## `usuarios-demo.yaml`

`identificacion` es opcional. La tienen:
- `rectorado.demo`: `0109990010`;
- `vicerrectorado.demo`: `0109990028`.

Son los RECTOR y VICERRECTOR vigentes de `08_cargos_autoridades.csv`.
