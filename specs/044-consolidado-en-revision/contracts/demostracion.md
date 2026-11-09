# Contrato: `deploy/probar_consolidado_en_revision.sh`

Va en `perfil-consolidado`, después de `probar_devolucion.sh`. Prepara los datos de prueba como los otros scripts y los borra al terminar, salvo las entradas de la bitácora.

| Paso | Qué hace | Qué comprueba | Requisito |
|---|---|---|---|
| 1 | Cargas, 2026-I abierto, distributivo; en cada uno de los 165 paralelos, el primer candidato de la sugerencia que se puede proponer, prefiriendo docentes sin clase | 0 paralelos sin propuesta; el panel con 0 duras | — |
| 2 | Envía los parciales; consolida y adjunta el informe **en EN_CONSOLIDACION** | Las tres responden como hoy; `enRevision` falso | US3, FR-005 |
| 3 | Justifica las blandas abiertas y envía a revisión | 200 y EN_REVISION_VRA: el envío que pasa, con datos reales | F10-H03 |
| 4 | Consolida, justifica (cambia una justificación) y adjunta otro informe | 409 `GENERACION_CONSOLIDADO_EN_REVISION` en las tres; la consolidación, la justificación y la huella del informe, iguales | US1, SC-001 |
| 5 | Consulta el consolidado y el panel, y descarga el informe | 200; `enRevision` verdadero en las dos consultas; el PDF descargado es el que se envió | FR-004 |
| 6 | Revisa la bitácora | Ningún rechazo dejó entrada | FR-007 |
