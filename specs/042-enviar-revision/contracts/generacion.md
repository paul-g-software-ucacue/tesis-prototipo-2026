# Contrato: `ParcialesDelDistributivo.condicionesDeRevision` (interfaz publicada de Generación)

Un método nuevo en la interfaz de F12-H02 (`ec.mdd.generacion.puertos.entrada`, `@NamedInterface("api")`). Solo lo usa Validación OCS, en el mismo proceso `distributivo`. No tiene ruta HTTP.

| Método | Entrada | Salida | Errores |
|---|---|---|---|
| `condicionesDeRevision` | `periodo` | `CondicionesDeRevision(version, consolidado, desactualizado, informe: Optional<String>, duras, blandas)` | `DistributivoNoEncontrado`; los fallos de Datos al armar el panel, como en `consultarConsolidado` |

**Garantías:**
- Sale de `ConsultarConsolidado`: el mismo panel y el mismo consolidado que ve la pantalla, calculados en ese momento (FR-008, SC-001).
- `consolidado` es verdadero si la versión vigente tiene al menos una consolidación. `desactualizado` es el mismo campo del consolidado. `informe` es la huella del informe vigente. `duras` y `blandas` son `abiertas` del panel.
- No guarda nada ni abre una transacción de escritura.

**Pruebas:** aplicación con Mockito: las cuatro condiciones, en los dos sentidos cada una, a partir de un `ConsolidadoDelPeriodo`; sin distributivo, `DistributivoNoEncontrado`.
