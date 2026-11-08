# Contrato: `ParcialesDelDistributivo` (interfaz publicada de Generación, dentro del proceso)

Generación la publica en `ec.mdd.generacion.puertos.entrada` (`@NamedInterface("api")`). Solo la usa Validación OCS, en el mismo proceso `distributivo` (principio V). No tiene ruta HTTP: si los servicios se separan, se agrega en `/interno/generacion/…` con token de proceso (ADR-023).

| Método | Entrada | Salida | Errores |
|---|---|---|---|
| `estadoDelDistributivo` | `periodo` | `EstadoDelDistributivo(periodo, version, creadoEn, parciales: List<ParcialYSuEstado(carrera, estado)>)` | `DistributivoNoEncontrado` |
| `marcarParcialEnviado` | `periodo`, `carrera` | `ParcialMarcado(version, hash)` | `DistributivoNoEncontrado`, `ParcialNoEncontrado` |
| `hashDelDistributivo` | `periodo` | `String` (64 hex) | `DistributivoNoEncontrado` |

**Garantías:**
- `marcarParcialEnviado` bloquea la versión vigente y marca el parcial en una transacción de Generación; es idempotente (mismo hash si ya estaba enviado y no cambió nada) y no llama a `RegistroDeCambios` (D-95).
- Después de marcarlo, `proponer`, `asignar` y `quitar` sobre ese parcial responden `GENERACION_PARCIAL_NO_EDITABLE`, como ya hace `exigirEditable`.
- El hash: research §4. La misma entrada da siempre el mismo hash.

**Pruebas:** dominio de `ContenidoDelParcial` (hash fijo de un parcial de prueba; cambiar un dato lo cambia; el orden de las filas no lo cambia); aplicación con Mockito (idempotencia, sin `registrar`); integración con PostgreSQL (el estado queda `ENVIADO` y una propuesta posterior falla).
