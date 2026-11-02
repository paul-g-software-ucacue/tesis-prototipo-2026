# Contrato: Docentes

## `GET /interno/docentes/cargos-vigentes`

Solo entre procesos. Los cargos con estado `VIGENTE`, sin datos personales (principio IX).

```json
[ { "docente": "0109990010", "tipo": "COORDINADOR_CARRERA", "carrera": "IST-023" } ]
```

`carrera` solo va en los cargos que la tienen.
