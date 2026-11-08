# Contrato: las entradas de la bitácora del flujo

Validación OCS publica `validacionocs.EntradaBitacora` (F17-H01) con dos acciones nuevas. La bitácora ya admite el servicio `validacionocs`; no cambia su contrato.

| Acción | `usuario` | `periodo` | `objeto` | `anterior` | `nuevo` |
|---|---|---|---|---|---|
| `PARCIAL_ENVIADO` | El coordinador | El del distributivo | `{periodo, carrera, version, hash}` | `{estado: "BORRADOR_CARRERA", parcial: "PENDIENTE"}` | `{estado: "BORRADOR_CARRERA", parcial: "ENVIADO"}` |
| `ETAPA_AVANZADA` | Quien envió el último parcial, o quien avanzó | El del distributivo | `{periodo, version, hash}` | `{estado: "<desde>"}` | `{estado: "<hacia>"}` |

Las dos son de clase «cambia» (`anterior` y `nuevo`). Una transición, una entrada (SC-001, D-95). Un intento rechazado no publica nada (FR-013).
