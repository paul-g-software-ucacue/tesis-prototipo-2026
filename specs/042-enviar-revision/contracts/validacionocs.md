# Contrato: enviar a revisión (`contratos/validacionocs/openapi.yaml`)

Una operación nueva en la etiqueta `Aprobacion`, y un valor nuevo en `acciones`. Se escriben en el contrato antes del código (principio IV).

## `POST /api/validacionocs/periodos/{periodo}/revision` · `enviarARevision`

`x-mdd-permiso: CONSOLIDACION:APROBACION` (research §3). `periodo`: `^[A-Za-z0-9._-]{1,20}$`. Sin cuerpo.

| Estado | Cuándo |
|---|---|
| `200` · `EstadoDelFlujo` | Enviado: el distributivo ya está en la siguiente etapa activa (EN_REVISION_VRA por defecto) |
| `403` `SEGURIDAD_ACCESO_NO_AUTORIZADO` | Sin el permiso o sin el rol de EN_CONSOLIDACION en la configuración del proceso. El mismo cuerpo en los dos casos |
| `404` `VALIDACIONOCS_SIN_DISTRIBUTIVO` | |
| `409` `VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION` | El distributivo no está en EN_CONSOLIDACION (también el segundo de dos envíos simultáneos) |
| `409` `VALIDACIONOCS_PARCIALES_PENDIENTES` | Algún parcial del proceso sin enviar (research §4) |
| `409` `VALIDACIONOCS_SIN_CONSOLIDAR` | |
| `409` `VALIDACIONOCS_CONSOLIDADO_DESACTUALIZADO` | |
| `409` `VALIDACIONOCS_SIN_INFORME` | |
| `409` `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS` | Con `detalles: [{campo: "inconsistencias", descripcion: "Quedan 3: 2 duras y 1 blanda sin justificar."}]` |
| `409` `VALIDACIONOCS_PERIODO_CERRADO` | |
| `400` `COMUN_SOLICITUD_INVALIDA` | `periodo` no cumple su patrón |
| `401`, `500`, `502`, `504` | Como las demás; 502 o 504 si Datos no responde al evaluar el panel |

Los 409 se comprueban en el orden de research §8.

## `consultarFlujo` y `EstadoDelFlujo`

`acciones` suma `ENVIAR_A_REVISION`: el usuario tiene el rol de EN_CONSOLIDACION y el distributivo está en esa etapa, aunque falten condiciones.

## Pruebas de contrato (`FlujoContratoTest`)

- El 200 cumple el esquema `EstadoDelFlujo` con la etapa nueva.
- Cada 409 con su código y su estado; `INCONSISTENCIAS_ABIERTAS` con su detalle.
- El 403 con el mismo cuerpo sin el permiso y sin el rol.
- `periodo` inválido: 400 con su detalle.
- `PermisosValidacionocsTest` suma `enviarARevision` con `CONSOLIDACION:APROBACION`.
