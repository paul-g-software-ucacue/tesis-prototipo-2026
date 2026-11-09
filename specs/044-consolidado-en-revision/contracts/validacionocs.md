# Contrato: Validación OCS implementa el puerto de Generación

Sin cambios en `contratos/validacionocs/openapi.yaml`.

| Pieza | Qué hace |
|---|---|
| `puertos.entrada.ConsultarEtapaDelConsolidado` | `admiteCambios(periodo, distributivoCreadoEn)` |
| `aplicacion.EtapaDelConsolidadoServicio` | Lee el estado con `RepositorioProceso.estadoCompartido`; sin proceso o con el de otro distributivo, `true`; si no, `TipoDeEtapa.admiteCambiosEnElConsolidado()` |
| `adaptadores.entrada.EtapaParaGeneracion` | Implementa `generacion.puertos.requeridos.EtapaDelConsolidado` y delega |
| `RepositorioProcesoJdbc.estadoCompartido` | `SELECT estado, distributivo_creado_en FROM validacionocs.proceso_aprobacion WHERE periodo = :periodo FOR SHARE` |

Y en `FlujoDelDistributivoServicio.enviarARevision`, el momento de la transición se toma dentro de la transacción bloqueada (research §2).

**Pruebas:**
- `TipoDeEtapaTest`: las 9 etapas.
- `EtapaDelConsolidadoServicioTest`: cada etapa, sin proceso y con el proceso de otro distributivo.
- Integración (`EtapaDelConsolidadoIntegrationTest`):
  - el bloqueo compartido retrasa un envío a revisión;
  - la transición queda con un momento posterior a la liberación del bloqueo.
- `ModulosTest`: sin ciclos.
