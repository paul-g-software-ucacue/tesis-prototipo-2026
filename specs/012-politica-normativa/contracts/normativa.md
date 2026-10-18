# Contrato: Normativa

Se escribe el contrato antes que el código (principio IV, ADR-015). Cada operación declara su permiso con `x-mdd-permiso` (ADR-021).

## `contratos/normativa/openapi.yaml` (nuevo)

| Operación | Ruta | `x-mdd-permiso` | Respuestas |
|---|---|---|---|
| `listarVersiones` | `GET /api/normativa/versiones` | `PARAMETROS:LECTURA` | 200 `[VersionResumen]`, ordenadas por número |
| `consultarVersion` | `GET /api/normativa/versiones/{numero}` | `PARAMETROS:LECTURA` | 200 `VersionNormativa` · 404 `COMUN_RECURSO_NO_ENCONTRADO` |

Las dos responden además 401 `NoAutenticado`, 403 `AccesoNoAutorizado`, 500 y 503. `numero` es un entero ≥ 1.

**Esquemas:**

| Esquema | Campos |
|---|---|
| `VersionResumen` | `numero`, `codigo`, `origen`, `vigenteDesde` (fecha), `tipoIst` (`T1`…`T5`) |
| `VersionNormativa` | los de `VersionResumen`, más `reglas` (`[Regla]`, por identificador) y `tablaCargaHoraria` (`[FilaCargaHoraria]`, en el orden de la política) |
| `Regla` | `identificador` (`RN-NN`), `severidad` (`DURA` o `BLANDA`, opcional), `parametros` (`[Parametro]`) |
| `Parametro` | `nombre`, `valor` (número) |
| `FilaCargaHoraria` | `aplicaA` (`TODOS` o `T1`…`T5`), `perfil`, `dedicacionTotal`, `horasClase`, `horasClaseMinimo`, `horasClaseMaximo`, `horasOtrasDocencia`, `horasGestion`, `horasInvestigacionVinculacionMaximo` (las tres últimas opcionales: se omiten si la política no las da), `severidad`, `referencia`, `observacion` (opcional) |

**Nombre:** `aplicaA` dice a qué tipo de IST aplica la fila; no se llama «alcance», que en el glosario es «todo o solo lo propio».

**Orden de la tabla:** el de la carga 7. Para que sea estable, la fila guarda su posición (`orden`) en la tabla `fila_carga_horaria` (data-model).

## Sin códigos de error nuevos

El 404 usa el código común `COMUN_RECURSO_NO_ENCONTRADO`; el 403, `SEGURIDAD_ACCESO_NO_AUTORIZADO` (ADR-021). No hay nada nuevo en `contratos/comun/errores.md`.
