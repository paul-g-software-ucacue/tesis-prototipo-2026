# Contrato: las rutas internas que cambian

F19-H01 (#266). Son las rutas entre procesos (ADR-023), con el token de proceso. El API Gateway no las enruta. Los dos contratos se cambian antes del código (principio IV).

## Generación · `consultarContenidoCongelado` (nueva)

`GET /interno/generacion/periodos/{periodo}/versiones/{numero}/contenido` · tag `Interno`

- **Los parámetros:** `periodo` y `numero`, iguales a los de `consultarCargasCongeladas`.
- **La descripción:** «Solo entre procesos, con el token de proceso: lo usa Reporting para el CSV para SIGA (F19-H01, D-119). El contenido completo de cada docente de una versión congelada, con el mismo cálculo tardío de las otras dos rutas.»
- **200:** `ContenidoCongeladoCompleto`, un esquema nuevo con `additionalProperties: false`:

  | Campo | Tipo | Regla |
  |---|---|---|
  | `hash` | string | `^[0-9a-f]{64}$` |
  | `cargas` | arreglo de `CargaCongeladaDelDocente` (existente) | Una por docente, por nombre y luego por identificación. Cada una repite el `hash` de la versión |

- **400, 401, 403 y 500:** los comunes.
- **404:** `COMUN_RECURSO_NO_ENCONTRADO`, si la versión no existe o no está congelada.

**Las pruebas:**
- `InternoGeneracionContratoTest` suma la operación.
- `ContenidoCongeladoIntegrationTest`, con PostgreSQL real, comprueba que las cargas de la ruta nueva son las mismas de la ruta por docente.

## Generación · `DatosDelDocenteCongelado` (amplía)

| Campo nuevo | Tipo | Regla |
|---|---|---|
| `tipoIdentificacion` | `TipoDeIdentificacion` (`enum: [CEDULA, PASAPORTE]`, esquema nuevo) | Opcional. Falta si el docente ya no estaba activo al congelar, o si el contenido se guardó antes de F19-H01 |

Afecta también a `consultarCargaCongelada`. El reporte individual no usa el campo.

## Docentes · `DocenteActivo` (amplía)

`GET /interno/docentes/activos`

| Campo nuevo | Tipo | Regla |
|---|---|---|
| `tipoIdentificacion` | `enum: [CEDULA, PASAPORTE]` | Obligatorio; se suma a `required` |

Generación es el único consumidor de esta ruta y se actualiza en el mismo cambio. Los dos procesos salen de la misma versión del repositorio (Riesgos del plan).

**Las pruebas:**
- `InternoDocentesContratoTest`: el campo nuevo.
- `RepositorioDocentesJdbcIntegrationTest`: el tipo en la consulta de los activos.
- Generación: el cliente lee el tipo, y una respuesta sin él es un 502 (principio XIV).
