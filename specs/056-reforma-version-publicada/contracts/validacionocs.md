# Contrato: iniciar una reforma, en `contratos/validacionocs/openapi.yaml`

F18-H02 (#72).

## `iniciarReforma` (nueva)

`POST /api/validacionocs/periodos/{periodo}/reforma`, con el cuerpo `SolicitudDeReforma`.

- **Permiso:** `x-mdd-permiso: DISTRIBUTIVO:APROBACION`. Además, el rol de la etapa EN_REVISION_VRA de la copia de la configuración, que comprueba el agregado (por defecto, el Vicerrector académico).
- **`SolicitudDeReforma`:**

| Campo | Tipo | Regla |
|---|---|---|
| `motivo` | string, `maxLength: 500` | Obligatorio. De 10 a 500 caracteres sin los espacios de los extremos; lo comprueba el servidor |

`additionalProperties: false`.

- **Respuestas:**

| Código | Cuándo |
|---|---|
| 200 | Iniciada: el `EstadoDelFlujo` en la etapa de llegada (EN_CONSOLIDACION por defecto), con la versión nueva, `reforma` y la `publicacion` anterior |
| 400 | `VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO`: el motivo falta o no tiene de 10 a 500 caracteres. `COMUN_SOLICITUD_INVALIDA`: el cuerpo mal formado o el periodo mal formado |
| 401 | Sin sesión |
| 403 | `SEGURIDAD_ACCESO_NO_AUTORIZADO`: sin el permiso o sin el rol de la etapa |
| 404 | `VALIDACIONOCS_PERIODO_NO_ENCONTRADO` o `VALIDACIONOCS_SIN_DISTRIBUTIVO`, los que ya existen |
| 409 | `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA`: el distributivo no está en PUBLICADO, también en la segunda de dos reformas simultáneas. `VALIDACIONOCS_PERIODO_CERRADO` si el periodo está cerrado |
| 500 | El error técnico común, si Generación no tiene la versión publicada como vigente ni la siguiente de una reforma |
| 502 o 504 | Los comunes, si Académico no responde al consultar el estado del periodo, como en la publicación |

## `EstadoDelFlujo` (existente)

- `acciones` admite `INICIAR_REFORMA` (`AccionDelFlujo`).
- `TransicionDelFlujo.tipo` admite `REFORMA`.
- Suma `reforma`, opcional, `ReformaDelDistributivo`, mientras la reforma esté en curso:

| Campo | Tipo | Regla |
|---|---|---|
| `versionPublicada` | integer, ≥ 1 | La versión publicada que se reforma, vigente hasta que se publique la nueva |
| `version` | integer, ≥ 1 | La versión nueva, de la reforma |
| `motivo` | string, `maxLength: 500` | El motivo |
| `iniciadaPor` | string | Quien la inició |
| `iniciadaEn` | date-time | Cuándo |

`additionalProperties: false`. La ve todo el que lee el estado del flujo, como `publicacion`.

- `publicacion` sigue siendo la última publicación: durante la reforma, la anterior.
- `resolucion` se muestra solo si es de la versión de trabajo, como `congelacion`.

## El catálogo

En `contratos/comun/errores.md`, con sus 8 columnas:
- `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA`: 409, «Etapa sin reforma», «Solo se reforma un distributivo publicado.»; causa: iniciar una reforma fuera de PUBLICADO, también la segunda de dos simultáneas; Negocio; `EtapaNoAdmiteReforma`, de `ProcesoAprobacion`.
- `VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO`: 400, «Motivo no válido», «El motivo de la reforma tiene de 10 a 500 caracteres.»; Validación; acción: escribir por qué hay que cambiar el distributivo publicado; `MotivoDeReformaInvalido`.

## Pruebas de contrato

`ReformaContratoTest` (nueva):
- el 200 con la versión nueva, `reforma` y la `publicacion` anterior;
- cada error con su código: el 409 de la etapa, el 400 del motivo y el 403 sin el rol;
- el 403 sin el permiso, sin llamar al caso de uso;
- el 400 del cuerpo y del periodo mal formados.

`FlujoContratoTest` (+2): la acción `INICIAR_REFORMA` solo para el Vicerrector en PUBLICADO, y la reforma en curso con la publicación anterior para quien lee el flujo, de punta a punta con los procesos en memoria.

`PermisosValidacionocsTest` suma la operación con `DISTRIBUTIVO:APROBACION`.
