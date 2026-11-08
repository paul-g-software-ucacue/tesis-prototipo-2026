# Contrato: el flujo del distributivo (`contratos/validacionocs/openapi.yaml`)

Tres operaciones nuevas, en la etiqueta `Flujo`. Se escriben en el contrato antes del código (principio IV).

## `GET /api/validacionocs/periodos/{periodo}/flujo` · `consultarFlujo`

`x-mdd-permiso: [DISTRIBUTIVO:LECTURA, OFERTA:LECTURA, PROYECTOS:LECTURA]` (cualquiera, D-70): el Responsable y el Vicerrector, el Coordinador de carrera y el Gestor de unidad, que necesita ver «Avanzar» en BORRADOR_UNIDADES (US3-5). El estado del flujo del distributivo del periodo.

`200` · `EstadoDelFlujo`:

| Campo | Tipo | Nota |
|---|---|---|
| `periodo` | `string`, `^[A-Za-z0-9._-]{1,20}$` | |
| `etapa` | `TipoDeEtapa` (enum de F12-H01) | La actual |
| `nombreDeEtapa` | `string` | El nombre de la configuración del proceso |
| `version` | `integer` | La vigente |
| `parciales` | lista de `{carrera, enviado: boolean, pendienteDeConfirmar: boolean, enviadoPor?, enviadoEn?}` | Ordenada por carrera. `pendienteDeConfirmar`: Generación lo marcó y la transición no se registró; reenviar lo completa (research §2) |
| `acciones` | lista de `ENVIAR_PARCIAL` o `AVANZAR` | Lo que **este** usuario puede hacer ahora (FR-014); vacía si nada. Con un parcial pendiente de confirmar, `ENVIAR_PARCIAL` sigue disponible para su coordinador |
| `transiciones` | lista de `TransicionDelFlujo` (`tipo`, `desde`, `hacia`, `carrera?`, `version`, `hash`, `quien`, `momento`) | En orden |

Errores: `401`, `403`, `404 VALIDACIONOCS_SIN_DISTRIBUTIVO` (y el 404 común si el periodo no existe), `500`, `503`.

## `POST /api/validacionocs/periodos/{periodo}/parciales/{carrera}/envio` · `enviarParcial`

`x-mdd-permiso: OFERTA:APROBACION`. `carrera`: `^[0-9A-Z_-]{1,20}$`. Sin cuerpo.

| Estado | Cuándo |
|---|---|
| `200` · `EstadoDelFlujo` | Enviado; si era el último, ya en la siguiente etapa |
| `403` `SEGURIDAD_ACCESO_NO_AUTORIZADO` | Sin el permiso, sin el rol de BORRADOR_CARRERA en la configuración del proceso, o de otra carrera (D-64). El mismo cuerpo en los tres casos |
| `403` `COMUN_CARRERA_SIN_INDICAR` | Coordinador sin carrera indicada |
| `404` `VALIDACIONOCS_SIN_DISTRIBUTIVO` | |
| `409` `VALIDACIONOCS_ETAPA_NO_ADMITE_ENVIO` | El distributivo ya no está en BORRADOR_CARRERA |
| `409` `VALIDACIONOCS_PARCIAL_YA_ENVIADO` | |
| `409` `VALIDACIONOCS_PERIODO_CERRADO` | |
| `401`, `500`, `503` | Como las demás |

## `POST /api/validacionocs/periodos/{periodo}/avance` · `avanzarEtapa`

Sin `x-mdd-permiso` propio: exige sesión, y el dominio exige el rol de la etapa actual (research §7). Sin cuerpo.

| Estado | Cuándo |
|---|---|
| `200` · `EstadoDelFlujo` | Avanzó a la siguiente etapa activa |
| `403` `SEGURIDAD_ACCESO_NO_AUTORIZADO` | Sin el rol de la etapa actual |
| `409` `VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA` | La etapa se avanza con su historia (incluida BORRADOR_CARRERA, que avanza con los envíos) |
| `404`, `409 VALIDACIONOCS_PERIODO_CERRADO`, `401`, `500`, `503` | Como arriba |

## Pruebas de contrato (`FlujoContratoTest`)

- Los tres códigos 200 cumplen el esquema.
- Cada error con su código y su estado; los tres 403 de `enviarParcial` con el mismo cuerpo, sin el rol ni la carrera.
- `carrera` y `periodo` inválidos: 400 `COMUN_SOLICITUD_INVALIDA` con su detalle.
- `consultarFlujo` responde 200 con cada uno de los tres permisos por separado, y 403 sin ninguno.
- `PermisosValidacionocsTest` suma `consultarFlujo` y `enviarParcial`.
