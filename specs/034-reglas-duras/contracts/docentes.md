# Contrato: Docentes

## `consultarDocentesConProyecto` (nueva, interna)

`GET /interno/docentes/con-proyecto?periodo=`

- **Acceso:** solo entre procesos (ADR-023).
- **200:** una lista con `docente` y `tipos`: los tipos de sus proyectos asignables en el periodo. Sin datos de contacto (principio IX).
