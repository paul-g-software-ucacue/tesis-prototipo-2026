# Contrato: Generación

## API publicada en el proceso: `ParcialesDelDistributivo` (cambia)

```java
/**
 * Congela la versión vigente por orden de Aprobación (F13-H01, MD-04), con la versión bloqueada.
 * Idempotente: si ya estaba congelada, devuelve el mismo hash sin cambiar nada. No registra en
 * el registro de cambios de Generación (D-95).
 * @throws DistributivoNoEncontrado si el periodo no tiene distributivo
 */
VersionCongeladaEnGeneracion congelarVersion(String periodo);
```

## HTTP: `contratos/generacion/openapi.yaml`

- **`ConsolidadoDelPeriodo` y `PanelDeInconsistencias`:** campo requerido `versionCongelada` (boolean), si la versión vigente está congelada. Falso sin distributivo.
- **409 `GENERACION_VERSION_CONGELADA`** en las descripciones de:
  - `proponerDocente` y `quitarPropuesta`;
  - `asignarActividad` y `quitarActividad`;
  - `consolidar`, `justificarInconsistencia` y `adjuntarInformeSustentado`.

## Catálogo (`contratos/comun/errores.md`)

| Código | HTTP | Mensaje | Detalles |
|---|---|---|---|
| `GENERACION_VERSION_CONGELADA` | 409 | La versión del distributivo ya se aprobó y está congelada para la firma: un cambio exige una versión nueva. | `version` |
