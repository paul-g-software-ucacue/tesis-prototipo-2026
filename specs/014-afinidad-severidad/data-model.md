# Data Model: Ajustar pesos y mínimo de afinidad y la severidad de las reglas (F04-H03)

Parte del modelo de F04-H02 ([data-model.md](../013-version-normativa-nueva/data-model.md)). Solo se lista lo que cambia.

## Dominio (modelo de dominio §5.1)

| Elemento | Tipo | Cambio |
|---|---|---|
| `Regla` | Objeto de valor | El identificador admite `RN-xx` y `RF-xx`. Exige parámetros **o** severidad, no al menos un parámetro (research §2). Gana `delMotor()`: tiene severidad. |
| `PesosAfinidad` | Objeto de valor, nuevo | Los 7 parámetros de RN-24. Invariante: para cada nivel, el peso del campo amplio no supera al del específico (research §4). |
| `MinimoAfinidad` | Objeto de valor, nuevo | El parámetro `minimo_afinidad` de RN-19. |
| `VersionNormativa` | Agregado | Gana `pesosAfinidad()` y `minimoAfinidad()`. `editada` también compara y cambia la severidad de cada regla (research §3). |
| `CambiosDeVersion` | Objeto de valor | Gana la severidad pedida por regla, opcional. |
| `CambioDeValor` | Objeto de valor | Sin cambios: el campo de una severidad es `reglas[RN-05].severidad`. |

**Reglas de una versión después de esta historia (16):**

| Grupo | Reglas | Severidad |
|---|---|---|
| Solo parámetros | RN-01, RN-06, RN-24 | ninguna |
| Del motor, ya estaban | RN-02, RN-13 | dura |
| | RN-05, RN-09, RN-10 | blanda |
| Del motor, nuevas | RN-12, RN-17, RN-18, RN-22, RN-25, RF-24 | dura |
| | RN-16, RN-19 | blanda |

Los 8 parámetros nuevos y sus valores por defecto están en research §1.

## Tablas (`V4__afinidad_y_reglas_del_motor.sql`)

- `regla.identificador`: el `check` pasa a `'^R[NF]-[0-9]{2}$'`.
- Para cada versión que no las tiene: las 9 reglas nuevas en `regla`, y los 8 parámetros en `parametro_regla`.
- En `registro_cambio`: un `VERSION_EDITADA` de `sistema` por versión completada, con un cambio por regla y por parámetro agregado (anterior `null`).
- Sin columnas ni tablas nuevas.
