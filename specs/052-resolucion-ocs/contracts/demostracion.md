# Contrato: Demostración

`deploy/probar_documento_firmable.sh` suma tres pasos después del 12 (EN_OCS por el sistema):

| Paso | Comprueba |
|---|---|
| 13 | El avance genérico desde EN_OCS: 409 `VALIDACIONOCS_REQUIERE_RESOLUCION`. `vicerrectorado.demo` y `rectorado.demo` reciben 403 al registrar. Sin fila en `resolucion_ocs` |
| 14 | `secretaria.demo` con número vacío, fecha futura, fecha anterior a EN_OCS, un archivo que no es PDF y uno de más de 10 MB: su 400 o 413, sin fila |
| 15 | `secretaria.demo` registra: 200, APROBADO, la transición por `secretaria.demo` con la resolución, el PDF descargado idéntico y `RESOLUCION_REGISTRADA` en la bitácora |

`MDD_CONSERVAR=EN_OCS` (nuevo) se detiene después del paso 12, con el distributivo en EN_OCS, para el recorrido a mano.

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es solo de lectura: la V7 de `validacionocs` y `resolucion_ocs` vacía.
