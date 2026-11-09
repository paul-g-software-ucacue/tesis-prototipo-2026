# Data Model: El consolidado no cambia durante la revisión

Sin tablas ni migraciones nuevas. El proceso de aprobación es el de F12-H02; el consolidado, el panel y el informe, los de F10-H01 y F10-H02.

## Aprobación (Validación OCS, dominio)

| Elemento | Regla |
|---|---|
| `TipoDeEtapa.admiteCambiosEnElConsolidado()` | Verdadera para BORRADOR_CARRERA, BORRADOR_UNIDADES y EN_CONSOLIDACION; falsa desde EN_REVISION_VRA en adelante (orden del flujo) |

## El puerto que Generación requiere (`ec.mdd.generacion.puertos.requeridos`, `@NamedInterface("requeridos")`)

| Método | Contrato |
|---|---|
| `EtapaDelConsolidado.admiteCambios(String periodo, Instant distributivoCreadoEn)` | Verdadero si el distributivo admite cambios en su consolidado: sin proceso de aprobación, con el proceso de un distributivo anterior, o con la etapa en una que admite cambios. Dentro de una transacción, deja la fila del proceso con un bloqueo compartido hasta que termina (research §2) |

Lo implementa Validación OCS:
- `adaptadores.entrada.EtapaParaGeneracion`, que delega en
- `puertos.entrada.ConsultarEtapaDelConsolidado`, implementado por
- `aplicacion.EtapaDelConsolidadoServicio`, que usa
- `RepositorioProceso.estadoCompartido(periodo)`: `SELECT estado, distributivo_creado_en … FOR SHARE`.

## Generación

| Elemento | Cambio |
|---|---|
| `ConsolidadoEnRevision` (dominio) | Excepción nueva; responde `GENERACION_CONSOLIDADO_EN_REVISION` (409) |
| `ConsolidarServicio.consolidar` | Dentro de su transacción, antes de guardar: si el puerto dice que no admite cambios, `ConsolidadoEnRevision` |
| `JustificarInconsistenciaServicio.justificar` | Igual |
| `InformeSustentadoServicio.adjuntar` | Igual, antes de guardar el archivo y su fila |
| `ConsolidadoDelPeriodo` | Suma `enRevision` |
| El panel (`PanelDeInconsistencias` en la respuesta) | Suma `enRevision` |

## Validación OCS: envío a revisión (F10-H03)

`FlujoDelDistributivoServicio.enviarARevision` toma el momento de la transición dentro de la transacción bloqueada (research §2).

## Estados

```text
BORRADOR_CARRERA ─┐
BORRADOR_UNIDADES ├── el consolidado admite cambios (consolidar, justificar, informe)
EN_CONSOLIDACION ─┘
EN_REVISION_VRA ──┐
CONGELADO_EN_FIRMA│
EN_OCS            ├── no admite: 409 GENERACION_CONSOLIDADO_EN_REVISION; se consulta y se descarga
REMITIDO_ZONAL    │
APROBADO          │
PUBLICADO ────────┘
```
