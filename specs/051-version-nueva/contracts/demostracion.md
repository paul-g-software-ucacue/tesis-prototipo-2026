# Contrato: Demostración

`deploy/probar_rechazo_en_firma.sh`, con los pasos de F14-H04 cambiados (research §9):

| Paso | Comprueba |
|---|---|
| 0 a 5 | Como en F14-H04 |
| 6 | En menos de 30 s, sin que nadie haga nada: EN_REVISION_VRA por `sistema`; la transición `VERSION_NUEVA`; v1 ANULADA y v2 EDITABLE, con el motivo; las mismas propuestas, actividades, justificaciones, parciales, consolidación e informe; `firma.anulacion_de_firmas` con 1 firma anulada (SC-001, SC-002) |
| 7 | El documento v1 cerrado: subir y rechazar otra vez, 403; nadie tiene pendientes |
| 8 | `vicerrectorado.demo` aprueba la v2: CONGELADO_EN_FIRMA con la v2, su documento generado y `docente.demo` con F1 de la v2 pendiente, sin la v1 (SC-004) |
| 9 | F1 de la v1 conservada; la bitácora con el rechazo, la versión nueva y la anulación |

`MDD_CONSERVAR=1` se detiene después del paso 2, como en F14-H04. La limpieza borra también `firma.anulacion_de_firmas`.

En la demo, 2026-I está en EN_CONSOLIDACION. La comprobación es solo de lectura:
- la V8 de `generacion`, la V7 de `firma` y la V6 de `validacionocs`;
- `firma.anulacion_de_firmas` vacía y ninguna versión ANULADA.
