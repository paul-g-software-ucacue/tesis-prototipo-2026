# Contrato: `deploy/probar_avance.sh`

En `perfil-consolidado`, después de `probar_consulta_bitacora.sh`. Prepara los datos de prueba como los otros scripts, y los borra al terminar (salvo las entradas de la bitácora).

| Paso | Qué hace | Qué comprueba | Requisito |
|---|---|---|---|
| 1 | Cargas, periodo abierto y distributivo | El flujo en BORRADOR_CARRERA, todos los parciales pendientes | FR-001, FR-002 |
| 2 | `coordinacion.demo` envía el parcial de su carrera | 200; la oferta de su carrera responde `GENERACION_PARCIAL_NO_EDITABLE`; una entrada `PARCIAL_ENVIADO` con usuario, estados, versión y hash | US1-1, US1-2 |
| 3 | Lo envía otra vez | 409 `VALIDACIONOCS_PARCIAL_YA_ENVIADO` y ninguna entrada nueva | US1-4 |
| 4 | Envía el de otra carrera | 403, ninguna entrada | US1-3 |
| 5 | `vicerrectorado.demo` intenta el avance genérico | 409 `VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA` | US3-6 |
| 6 | Un usuario de otro rol intenta enviar | 403 | US3-1, SC-002 |
| 7 | Envía los parciales que faltan (cambiando la carrera del coordinador), los dos últimos a la vez | El distributivo en EN_CONSOLIDACION; una sola `ETAPA_AVANZADA` | US2-1, US2-2, SC-003, SC-004 |
| 8 | Compara el hash de la entrada con el de la transición en `consultarFlujo` | Iguales; el cálculo y su estabilidad los prueba el dominio de Generación (SC-006) | FR-011 |
