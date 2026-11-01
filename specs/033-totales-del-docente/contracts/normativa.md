# Contrato: Normativa

Cambios en `contratos/normativa/openapi.yaml`.

## `consultarProporcionDelPeriodo` (nueva, interna)

`GET /interno/normativa/periodos/{periodo}/proporcion`

- **Acceso:** solo entre procesos, con el token de proceso (ADR-023).
- **200:** `version`, `clase` y `otrasDocencia`.
- **404:** el periodo no tiene versión.
