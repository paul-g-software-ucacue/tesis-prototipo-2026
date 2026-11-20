# Contrato: publicar, en `contratos/validacionocs/openapi.yaml`

F18-H01 (#71).

## `publicarDistributivo` (nueva)

`POST /api/validacionocs/periodos/{periodo}/publicacion`, sin cuerpo.

- **Permiso:** `x-mdd-permiso: PUBLICACION:APROBACION`. Además, el rol de la etapa APROBADO de la copia de la configuración, que comprueba el agregado (por defecto, la Secretaría general académica).
- **Respuestas:**

| Código | Cuándo |
|---|---|
| 200 | Publicado: el `EstadoDelFlujo` en PUBLICADO, con `publicacion` y sin acciones |
| 400 | `COMUN_SOLICITUD_INVALIDA`: el periodo mal formado |
| 401 | Sin sesión |
| 403 | `SEGURIDAD_ACCESO_NO_AUTORIZADO`: sin el permiso o sin el rol de la etapa |
| 404 | `VALIDACIONOCS_PERIODO_NO_ENCONTRADO` o `VALIDACIONOCS_SIN_DISTRIBUTIVO`, los que ya existen |
| 409 | `VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION`: el distributivo no está en APROBADO, también en PUBLICADO y en la segunda de dos publicaciones simultáneas. `VALIDACIONOCS_PERIODO_CERRADO` si el periodo está cerrado |

## `EstadoDelFlujo` (existente)

- `acciones` admite `PUBLICAR` (`AccionDelFlujo`).
- Suma `publicacion`, opcional, `PublicacionDelDistributivo`:

| Campo | Tipo | Regla |
|---|---|---|
| `version` | integer, ≥ 1 | La versión publicada |
| `publicadaPor` | string | Quien publicó |
| `publicadaEn` | date-time | Cuándo |

`additionalProperties: false`. A diferencia de `resolucion`, la ve todo el que lee el estado del flujo: publicar no es un dato reservado.

## El catálogo

`VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION` en `contratos/comun/errores.md`, con sus 8 columnas:
- 409, «Etapa sin publicación»;
- «El distributivo solo se publica cuando está aprobado.»;
- causa: publicar fuera de APROBADO, también la segunda de dos simultáneas;
- Negocio; `EtapaNoAdmitePublicacion`, de `ProcesoAprobacion`; lo traduce `ErroresValidacionocs`.

## Pruebas de contrato

`PublicarContratoTest` (nueva):
- el 200 con la constancia y sin acciones;
- el 409 en EN_OCS y en PUBLICADO;
- el 403 de un usuario sin el permiso y de uno con el permiso pero sin el rol de la etapa;
- el 400 de un periodo mal formado.

`FlujoContratoTest` (+2): la acción `PUBLICAR` para la Secretaría en APROBADO, y la constancia `publicacion` en PUBLICADO para quien lee el flujo.

`PermisosValidacionocsTest` suma la operación con `PUBLICACION:APROBACION`.
