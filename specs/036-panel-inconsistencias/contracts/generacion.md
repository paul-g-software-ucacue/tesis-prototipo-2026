# Contrato: Generación

## `GET /api/generacion/periodos/{periodo}/inconsistencias`

Permiso: `CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA`.

```json
{
  "periodo": "2026-I",
  "creado": true,
  "modificable": true,
  "version": 1,
  "abiertas": { "duras": 3, "blandas": 1 },
  "inconsistencias": [
    {
      "tipo": "PARALELO_SIN_PROPUESTA",
      "regla": "RF-24",
      "severidad": "DURA",
      "estado": "ABIERTA",
      "objeto": { "clase": "PARALELO", "clave": "POO-204/A", "nombre": "Programación · A" },
      "detalle": "Falta el docente de 4 h de clase.",
      "indicacion": "El coordinador de la carrera debe proponer un docente.",
      "pantalla": "OFERTA"
    }
  ],
  "sinEvaluar": ["RN-03", "RN-04", "RN-07", "RN-08", "RN-09", "RN-11", "RN-14", "RN-15", "RN-16"]
}
```

- Una justificada agrega `justificacion { texto, por, en }`.
- Sin distributivo: 200 con `creado: false` y sin inconsistencias.
- Errores: 401, 403, 404 `GENERACION_PERIODO_NO_ENCONTRADO`, 409 `GENERACION_PERIODO_SIN_ABRIR`, 502 y 504.
- El 409 no debería ocurrir, porque un distributivo nace de un periodo abierto. Queda como protección si la versión no se puede leer.

## `PUT /api/generacion/periodos/{periodo}/inconsistencias/justificacion`

Permiso: `CONSOLIDACION:ESCRITURA`.

```json
{ "tipo": "PROPORCION", "objeto": "0109990010", "texto": "Tiene horas de clase de tronco común." }
```

Responde 200 con la inconsistencia justificada.

| Error | Cuándo |
|---|---|
| 400 `texto` | largo inválido, o solo espacios |
| 409 `GENERACION_INCONSISTENCIA_NO_ENCONTRADA` | ya no existe |
| 409 `GENERACION_INCONSISTENCIA_DURA` | es dura |
| 409 `GENERACION_PERIODO_CERRADO` | periodo cerrado |
| 409 `GENERACION_SIN_DISTRIBUTIVO` | no hay distributivo |

## Errores nuevos

| Código | HTTP | Título | Mensaje |
|---|---|---|---|
| `GENERACION_INCONSISTENCIA_NO_ENCONTRADA` | 409 | Inconsistencia resuelta | Esta inconsistencia ya no existe, quizá porque alguien la corrigió. Actualiza el panel. |
| `GENERACION_INCONSISTENCIA_DURA` | 409 | No se puede justificar | La normativa del periodo no admite excepciones para esta regla. Corrígela en su pantalla. |

`PermisosGeneracionTest` pasa de 12 a 14 operaciones.
