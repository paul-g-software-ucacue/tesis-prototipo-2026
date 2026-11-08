# Contrato: la entrada de la bitácora de una devolución

Validación OCS publica `validacionocs.EntradaBitacora` (F17-H01) con una acción nueva. La bitácora ya admite el servicio `validacionocs` y acciones libres, así que su contrato no cambia.

| Acción | `usuario` | `periodo` | `objeto` | `anterior` | `nuevo` |
|---|---|---|---|---|---|
| `DISTRIBUTIVO_DEVUELTO` | El Responsable de consolidación | El del distributivo | `{periodo, version, hash, devueltos: {<carrera>: <observación>, …}}` | `{estado: "EN_CONSOLIDACION"}` | `{estado: "BORRADOR_CARRERA"}` |

- Es de clase «cambia», con `anterior` y `nuevo`.
- Una devolución deja una entrada, aunque devuelva varias carreras (D-95).
- La pantalla «Bitácora» muestra `devueltos` anidado, con una línea por carrera (`nombres.ts`, `lineas`).
- Un intento rechazado no publica nada (FR-013).
- El reenvío de cada carrera deja su `PARCIAL_ENVIADO`, y el último, su `ETAPA_AVANZADA`, como en F12-H02.
