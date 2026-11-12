# Contrato: Firma

Archivo nuevo: `contratos/firma/openapi.yaml` (etiqueta `DocumentoFirmable`). El API Gateway ya enruta `/api/firma/**` a `distributivo`.

## API publicada en el proceso: `PrepararDocumentoFirmable` (interfaz nombrada `preparacion`)

```java
/**
 * Aprobación entrega la cadena y pide el documento firmable de la versión congelada (MD-05,
 * F13-H02). Registra la solicitud y genera; es idempotente. Un fallo de generación queda
 * registrado con su motivo y no se relanza (D-101).
 */
void preparar(SolicitudDeDocumento solicitud);
```

## `GET /api/firma/periodos/{periodo}/documento`

- `operationId: consultarDocumentoFirmable`.
- `x-mdd-permiso`: `DISTRIBUTIVO:LECTURA`, `CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA`, los mismos que leen el consolidado.

| Respuesta | Cuándo |
|---|---|
| 200 `EstadoDelDocumento` | Siempre; sin solicitud de Aprobación, SIN_GENERAR sin versión |
| 400, 401, 403 | Los comunes |

`EstadoDelDocumento`:
- `periodo`;
- `version` (entero ≥ 1), opcional: falta con SIN_GENERAR;
- `estado`: `SIN_GENERAR` | `EN_CURSO` | `FALLIDA` | `GENERADO`;
- `motivo` (solo con FALLIDA, hasta 200 caracteres);
- `documento` (solo con GENERADO): `{sha256 (64 hex), hashContenido (64 hex), generadoPor, generadoEn (date-time)}`;
- `acciones`: `[]` o `[GENERAR]`.

`GENERAR` aparece solo para el rol de la etapa que llegó en la solicitud, con la generación FALLIDA.

## `POST /api/firma/periodos/{periodo}/documento`

- `operationId: generarDocumentoFirmable`, sin cuerpo.
- `x-mdd-permiso: CONSOLIDACION:APROBACION`; en el dominio, el rol de la etapa que llegó en la solicitud (403 si no lo tiene).
- Genera en la misma petición.

| Respuesta | Cuándo |
|---|---|
| 200 `EstadoDelDocumento` con GENERADO | Generado ahora o ya existía (idempotente) |
| 400, 401, 403 | Los comunes; 403 también sin el rol de la etapa |
| 409 | Aprobación no pidió el documento (`FIRMA_ETAPA_NO_ADMITE_GENERACION`) o la generación falló (`FIRMA_GENERACION_FALLIDA`, con el motivo en `detalles`) |
| 500, 502, 504 | Los comunes |

## Catálogo (`contratos/comun/errores.md`)

| Código | HTTP | Título | Descripción |
|---|---|---|---|
| `FIRMA_ETAPA_NO_ADMITE_GENERACION` | 409 | Etapa sin documento firmable | El documento firmable se genera cuando el distributivo está en firma. |
| `FIRMA_GENERACION_FALLIDA` | 409 | El PDF no se pudo generar | El documento firmable no se pudo generar. Corrige lo que dice el detalle y vuelve a intentarlo. |

## Bitácora

Una entrada `DOCUMENTO_FIRMABLE_GENERADO`:
- servicio `firma`;
- usuario `sistema` o el del reintento;
- objeto `{periodo, version, hashContenido, sha256}`;
- `nuevo` `{estado: GENERADO}`.

Un fallo o un pedido repetido no deja entrada.
