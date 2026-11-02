# Contrato: Normativa

## `consultarReglasDeAsignacion` (nueva, interna)

`GET /interno/normativa/periodos/{periodo}/reglas-de-asignacion`

- **Acceso:** solo entre procesos (ADR-023).
- **200:** `periodo`, `version`, `horasMaximasDocente` y `horasClaseConProyecto`.
- **404:** el periodo no tiene versión.
- **500:** a la versión le falta una cifra.
