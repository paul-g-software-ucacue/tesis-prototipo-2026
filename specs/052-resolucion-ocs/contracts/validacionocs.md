# Contrato: Validación OCS (Aprobación)

## `POST /api/validacionocs/periodos/{periodo}/resolucion` (`registrarResolucion`, nueva)

`x-mdd-permiso: RESOLUCION_OCS:ESCRITURA`. `multipart/form-data`:

| Parte | Regla |
|---|---|
| `numero` | Texto, de 1 a 60 caracteres sin los espacios de los extremos |
| `fecha` | `date` (AAAA-MM-DD) |
| `archivo` | El PDF, hasta 10 MB |

| Estado | Código | Cuándo |
|---|---|---|
| 200 | — | Registrada: el estado del flujo en APROBADO, con `resolucion` |
| 400 | `COMUN_SOLICITUD_INVALIDA` | Falta una parte o la fecha no es una fecha; la parte, en los detalles |
| 400 | `VALIDACIONOCS_NUMERO_DE_RESOLUCION_INVALIDO` | El número, recortado, vacío o de más de 60 |
| 400 | `VALIDACIONOCS_FECHA_DE_RESOLUCION_FUERA_DE_RANGO` | Futura o anterior al día de entrada en EN_OCS |
| 400 | `VALIDACIONOCS_RESOLUCION_NO_ES_PDF` | Vacío o no es un PDF por su contenido |
| 413 | `VALIDACIONOCS_RESOLUCION_DEMASIADO_GRANDE` | Pasa de 10 MB |
| 403 | `SEGURIDAD_ACCESO_NO_AUTORIZADO` | Sin el permiso o sin el rol de EN_OCS |
| 404 | `VALIDACIONOCS_SIN_DISTRIBUTIVO` | Sin distributivo |
| 409 | `VALIDACIONOCS_ETAPA_NO_ADMITE_RESOLUCION` | Fuera de EN_OCS (también el segundo de dos a la vez) |
| 409 | `VALIDACIONOCS_PERIODO_CERRADO` | Periodo cerrado |

## `GET /api/validacionocs/periodos/{periodo}/resolucion/archivo` (`descargarResolucion`, nueva)

`x-mdd-permiso: RESOLUCION_OCS:LECTURA` (D-16; Roles V9 lo suma a VICERRECTOR, RECTOR y MIEMBRO_OCS). 200 `application/pdf`, con `Content-Disposition: attachment` y el nombre guardado. Sin el permiso, 403 común.

| Estado | Código | Cuándo |
|---|---|---|
| 404 | `VALIDACIONOCS_RESOLUCION_NO_ENCONTRADA` | El periodo no tiene resolución |
| 500 | `VALIDACIONOCS_RESOLUCION_ILEGIBLE` | El archivo falta o no coincide con su huella |

## `POST …/avance` (suma un 409)

| Estado | Código | Cuándo |
|---|---|---|
| 409 | `VALIDACIONOCS_REQUIERE_RESOLUCION` | Desde EN_OCS: a APROBADO solo se pasa registrando la resolución |

## `GET …/flujo` (`EstadoDelFlujo`)

- `resolucion` (`ResolucionDelOcs`): `numero`, `fecha`, `registradaPor`, `registradaEn`, `sha256`, `nombre`, `tamano`. Presente desde que se registra, y solo para quien tiene `RESOLUCION_OCS:LECTURA`.
- `AccionDelProceso` suma `REGISTRAR_RESOLUCION`.

## Catálogo (8 columnas en `contratos/comun/errores.md`)

| Código | Estado | Título | Descripción | Clasificación |
|---|---|---|---|---|
| `VALIDACIONOCS_REQUIERE_RESOLUCION` | 409 | Falta la resolución | Para aprobar el distributivo registra la resolución del OCS: número, fecha y PDF. | Negocio |
| `VALIDACIONOCS_ETAPA_NO_ADMITE_RESOLUCION` | 409 | Etapa sin resolución | La resolución del OCS solo se registra con el distributivo en la etapa del OCS. | Negocio |
| `VALIDACIONOCS_NUMERO_DE_RESOLUCION_INVALIDO` | 400 | Número no válido | Escribe el número de la resolución, de hasta 60 caracteres. | Validación |
| `VALIDACIONOCS_FECHA_DE_RESOLUCION_FUERA_DE_RANGO` | 400 | Fecha fuera de rango | La fecha de la resolución va desde el día en que el distributivo llegó al OCS hasta hoy. | Validación |
| `VALIDACIONOCS_RESOLUCION_NO_ES_PDF` | 400 | El archivo no es un PDF | Adjunta la resolución en PDF. | Validación |
| `VALIDACIONOCS_RESOLUCION_DEMASIADO_GRANDE` | 413 | Resolución demasiado grande | La resolución pasa de 10 MB. Reduce su tamaño y vuelve a adjuntarla. | Validación |
| `VALIDACIONOCS_RESOLUCION_NO_ENCONTRADA` | 404 | Sin resolución | Este periodo todavía no tiene resolución del OCS. | Negocio |
| `VALIDACIONOCS_RESOLUCION_ILEGIBLE` | 500 | Resolución no disponible | No se pudo leer el PDF guardado de la resolución. Avisa al administrador. | Técnico |
