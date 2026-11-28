# Contrato: las rutas internas que cambian

F19-H02 (#267). Son rutas entre procesos (ADR-023), con el token de proceso; el API Gateway no las enruta. Los contratos se cambian antes del código (principio IV).

## Firma · `consultarFirmasDeLaVersion` (nueva)

`GET /interno/firma/periodos/{periodo}/versiones/{numero}/firmas` · tag `Interno`

- **Los parámetros:**
  - `periodo`: `^[A-Za-z0-9._-]{1,20}$`, como en `consultarDocumentoFirmable`;
  - `numero`: entero ≥ 1.
- **La descripción:** «Solo entre procesos, con el token de proceso: lo usa Reporting para validar el CSV para SIGA (F19-H02, D-120). La cadena de firmas del documento CONSOLIDADO de esa versión, con el estado de cada paso. Firma solo informa: la regla de qué es una versión firmada es de quien la usa.»
- **200:** `FirmasDeLaVersion`, un esquema nuevo con `additionalProperties: false`:

  | Campo | Tipo | Regla |
  |---|---|---|
  | `periodo` | string | El pedido |
  | `version` | integer | ≥ 1 |
  | `hashContenido` | string | `^[0-9a-f]{64}$`: el hash de la versión congelada del documento |
  | `pasos` | arreglo de `PasoDeLaVersion` | Por orden; `minItems: 0` |
  | `anulada` | boolean | Sus firmas se anularon |
  | `rechazada` | boolean | Se rechazó en firma |

  `PasoDeLaVersion` (`additionalProperties: false`):
  - `orden`: integer ≥ 1;
  - `estado`: `enum: [FIRMADO, PENDIENTE, RECHAZADO]`.

  No lleva rótulos, nombres ni firmantes (principio IX): Reporting no los necesita.
- **404:** `COMUN_RECURSO_NO_ENCONTRADO`, si la versión no tiene documento.
- **400, 401, 403 (sin el rol `PROCESO`) y 500:** los comunes.

**Las pruebas:**
- `InternoFirmaContratoTest` (nueva): el 200 con cada estado, el 404 y el 403 con un token de usuario.
- `DocumentoFirmableServicioTest`: por versión, y sin documento.

## Generación · `ContenidoCongeladoCompleto` (amplía)

`GET /interno/generacion/periodos/{periodo}/versiones/{numero}/contenido`

| Campo nuevo | Tipo | Regla |
|---|---|---|
| `inconsistenciasAbiertas` | `InconsistenciasAlCongelar` (esquema nuevo: `duras` y `blandas`, integer ≥ 0, los dos obligatorios, `additionalProperties: false`) | Opcional. Las duras y las blandas sin justificar de la versión cuando se guardó su contenido. Falta si se guardó antes de F19-H02, o tarde, cuando la versión ya no era la vigente |

**La descripción** suma: «Las inconsistencias abiertas se fijan al guardar el contenido, no se recalculan (F19-H02, D-120).»

**Las pruebas:**
- `InternoGeneracionContratoTest`: con el campo y sin él.
- `ContenidoCongeladoIntegrationTest` (PostgreSQL real): las columnas, la regla de las dos vacías o las dos llenas, y el campo en la ruta.
