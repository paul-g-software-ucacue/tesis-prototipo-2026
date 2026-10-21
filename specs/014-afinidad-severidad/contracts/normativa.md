# Contrato: Normativa (cambios de F04-H03)

## `contratos/normativa/openapi.yaml`

Sin operaciones nuevas. Cambian dos esquemas:

| Esquema | Campo | Cambio |
|---|---|---|
| `Regla` | `identificador` | `pattern: '^R[NF]-[0-9]{2}$'` (RF-24). |
| `Regla` | `parametros` | Sigue obligatorio, pero puede venir vacío (sin `minItems`). |
| `ReglaEditada` | `identificador` | El mismo `pattern`. |
| `ReglaEditada` | `parametros` | Puede venir vacío. |
| `ReglaEditada` | `severidad` | **Nuevo**, opcional, `$ref: Severidad`. Ausente: no cambia. Presente en una regla que solo da parámetros: 400. |

La descripción de `editarVersion` agrega que la severidad de las reglas del motor también se edita y que la carga 7 por archivo la omite.

## Errores

Sin códigos nuevos en `contratos/comun/errores.md`:

| Caso | Respuesta |
|---|---|
| Severidad en RN-01, RN-06 o RN-24 | 400 `COMUN_SOLICITUD_INVALIDA`, campo `reglas[RN-24].severidad` |
| Peso del campo amplio mayor que el del específico | 400 `COMUN_SOLICITUD_INVALIDA`, campo `reglas[RN-24].cuarto_nivel_campo_amplio` (o `tercer_nivel_…`) |
| Peso o mínimo negativo, con más de dos decimales o demasiado grande | 400, como cualquier parámetro (F04-H02) |
| Cambiar pesos, mínimo o severidad de una versión en uso | 409 `NORMATIVA_VERSION_EN_USO` (F04-H02) |
| Sin `PARAMETROS:ESCRITURA` | 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO` |

## La consulta del periodo (FR-008, clarificación 3)

`consultarPeriodo` devuelve la versión que rige el periodo, y `consultarVersion`, sus reglas con la severidad, los pesos y el mínimo. El motor de F07 decide cómo las pide desde el proceso `distributivo` (research §7).
