# Contrato: Generación (Distributivo), interfaz publicada

Sin cambios en `contratos/generacion/openapi.yaml`: crear la versión nueva no es una operación de la API (FR-017). La pide Aprobación por la interfaz publicada de Generación, en el mismo proceso.

## `ParcialesDelDistributivo.crearVersionNueva`

```java
/**
 * Anula la versión congelada y crea la siguiente con sus asignaciones, por orden de Aprobación
 * (F12-H04, MD-04). Bloquea la versión vigente durante su transacción. Idempotente.
 */
Optional<VersionNuevaEnGeneracion> crearVersionNueva(
        String periodo, int anulada, MotivoDeVersion motivo, Instant momento);

record VersionNuevaEnGeneracion(int numero, String hash) {}

/** En la interfaz publicada, no en el dominio: lo usa Aprobación (principio V). */
enum MotivoDeVersion { RECHAZO_EN_FIRMA }
```

| Situación | Resultado |
|---|---|
| La vigente es `anulada` y está CONGELADA | `anulada` pasa a ANULADA; la siguiente, EDITABLE, con todo copiado; su número y su hash |
| La vigente es `anulada + 1`, creada por `motivo`, y `anulada` está ANULADA | La misma versión, sin cambiar nada |
| Otra | Vacío |
| Sin distributivo | `DistributivoNoEncontrado` |

No anota nada en el registro de cambios de Generación (D-95): la transición la registra Aprobación.

## Consultas

- `GET /api/generacion/periodos/{periodo}/consolidado` e `inconsistencias`: sin cambios en el contrato. `version` es el número de la versión vigente, la nueva; `versionCongelada` es falso mientras la nueva no se apruebe.
