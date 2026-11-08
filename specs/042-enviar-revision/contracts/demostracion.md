# Contrato: `deploy/probar_revision.sh`

Va en `perfil-consolidado`, después de `probar_avance.sh`. Prepara los datos de prueba como los otros scripts y los borra al terminar, salvo las entradas de la bitácora. El envío que pasa no se puede alcanzar con los datos de prueba (research §10): lo cubren las pruebas de integración.

| Paso | Qué hace | Qué comprueba | Requisito |
|---|---|---|---|
| 1 | Cargas, periodo abierto y distributivo; una propuesta que deja una blanda abierta (`PROPORCION` o `ACTIVIDADES_OBLIGATORIAS`, como `probar_panel.sh`); los parciales de todas las carreras enviados | El flujo en EN_CONSOLIDACION; `consolidacion.demo` ve `ENVIAR_A_REVISION` y `vicerrectorado.demo` no | US4-2, FR-013 |
| 2 | Los 9 roles que no son el de la etapa intentan enviar | 403 con el mismo cuerpo | US4-1, SC-003 |
| 3 | `consolidacion.demo` envía sin haber consolidado | 409 `VALIDACIONOCS_SIN_CONSOLIDAR` | US3-1 |
| 4 | Consolida y envía | 409 `VALIDACIONOCS_SIN_INFORME` | US3-3 |
| 5 | Adjunta un PDF mínimo y envía | 409 `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS`; el número del detalle es `abiertas.duras + abiertas.blandas` del panel, con duras y blandas por separado | US2-1, US2-2, SC-001 |
| 6 | Justifica una blanda y envía | El número baja en uno | US1-2, MD-07 |
| 7 | Revisa la bitácora y las transiciones | Ningún rechazo dejó transición ni entrada; el distributivo sigue en EN_CONSOLIDACION | FR-012 |
