# Data Model: Aprobar el contenido y congelar la versión

Sin migraciones (research §5).

## Generación

### VersionDistributivo (existe)

| Campo | Cambio |
|---|---|
| `estado` (`EDITABLE`, `CONGELADA`, `ANULADA`) | Pasa de `EDITABLE` a `CONGELADA` por orden de Aprobación. No vuelve atrás en esta historia. La columna ya admite `CONGELADA` (V3). |

**Regla nueva:** `exigirEditable(carrera)` rechaza primero una versión `CONGELADA` con `VersionCongelada`; después, como hoy, una versión que no es `EDITABLE` o un parcial enviado, con `ParcialNoEditable`.

### VersionCongelada (excepción nueva, dominio)

409 `GENERACION_VERSION_CONGELADA`, con el número de la versión en `detalles`. La lanzan:
- proponer y quitar una propuesta, asignar y quitar una actividad (`exigirEditable`);
- consolidar, justificar y adjuntar el informe, que comprueban la versión vigente dentro de su transacción, antes que `EnRevision`.

### VersionCongeladaEnGeneracion (registro nuevo, `puertos.entrada`)

Lo que `ParcialesDelDistributivo.congelarVersion(periodo)` le devuelve a Aprobación:

| Campo | Tipo | Regla |
|---|---|---|
| `version` | int ≥ 1 | El número de la versión vigente |
| `hash` | String (64 hex) | `hashDelDistributivo` de esa versión |

## Validación OCS

### ProcesoAprobacion (existe)

- **`validarAprobacion(quien)`**:
  - lanza `RolDeOtraEtapa` si `quien` no tiene el rol de EN_REVISION_VRA en la copia de la configuración;
  - lanza `EtapaNoAdmiteAprobacion` si el estado no es EN_REVISION_VRA.
- **`aprobarContenido(quien, version, hash, ahora)`**: valida y agrega una transición `PASO_DE_ETAPA` de EN_REVISION_VRA a `siguienteActiva` (CONGELADO_EN_FIRMA).
- **`congelaAlAvanzar()`**: si la siguiente activa desde el estado es CONGELADO_EN_FIRMA (research §3).
- **`acciones(quien)`**: en EN_REVISION_VRA, con su rol, devuelve `[APROBAR_CONTENIDO]`.
- **`congelacion()`**: la última transición `PASO_DE_ETAPA` con `hacia == CONGELADO_EN_FIRMA`, si hay.

### Transicion (existe, sin cambios)

La aprobación es un `PASO_DE_ETAPA`. Desde EN_REVISION_VRA, quién, cuándo, la versión y el hash ya son columnas.

### AccionDelProceso

Valor nuevo: `APROBAR_CONTENIDO`.

### CambioDeFlujo.Accion

Valor nuevo: `CONTENIDO_APROBADO`, para un `PASO_DE_ETAPA` desde EN_REVISION_VRA. Su detalle lleva el periodo, la versión y el hash. Antes: `{estado: EN_REVISION_VRA}`; después: `{estado: CONGELADO_EN_FIRMA}`.

### EtapaNoAdmiteAprobacion (excepción nueva)

409 `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION`.

### FlujoDelDistributivo (existe)

Campo nuevo `congelacion` (opcional), con `version`, `hash`, `quien` y `momento`, tomados de `ProcesoAprobacion.congelacion()`.

### Distributivo (puerto de salida, existe)

Método nuevo `congelar(periodo)`, que devuelve `VersionCongeladaEnAprobacion(version, hash)`. Lo implementa `DistributivoDesdeGeneracion` con `ParcialesDelDistributivo.congelarVersion`.

## Estados

```
EN_REVISION_VRA ──aprobarContenido (VICERRECTOR)──▶ CONGELADO_EN_FIRMA
    versión EDITABLE ─────────────────────────────▶ versión CONGELADA (mismo hash)

EN_CONSOLIDACION ──enviarARevision, con EN_REVISION_VRA desactivada──▶ CONGELADO_EN_FIRMA
    versión EDITABLE ─────────────────────────────▶ versión CONGELADA
```
