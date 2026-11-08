# Contrato: la entrada de la bitácora del envío a revisión

Validación OCS publica `validacionocs.EntradaBitacora` (F17-H01) con una acción nueva. La bitácora ya admite el servicio `validacionocs`, así que su contrato no cambia.

| Acción | `usuario` | `periodo` | `objeto` | `anterior` | `nuevo` |
|---|---|---|---|---|---|
| `CONSOLIDADO_ENVIADO_A_REVISION` | El Responsable de consolidación | El del distributivo | `{periodo, version, hash, informe}` | `{estado: "EN_CONSOLIDACION"}` | `{estado: "<hacia>"}` |

- Es de clase «cambia», con `anterior` y `nuevo`.
- Una transición deja una entrada (D-95).
- Un intento rechazado no publica nada (FR-012).
- `informe` es la huella SHA-256 del informe vigente.
