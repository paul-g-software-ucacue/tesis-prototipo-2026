# Contrato: `deploy/probar_devolucion.sh`

Va en `perfil-consolidado`, después de `probar_revision.sh`. Prepara los datos de prueba como los otros scripts y los borra al terminar, salvo las entradas de la bitácora.

| Paso | Qué hace | Qué comprueba | Requisito |
|---|---|---|---|
| 1 | Cargas, periodo abierto y distributivo; una propuesta en dos carreras; los parciales de todas las carreras enviados | EN_CONSOLIDACION; `consolidacion.demo` ve `DEVOLVER_PARCIALES` y `vicerrectorado.demo` no | US4-2, FR-016 |
| 2 | Los 9 roles que no son el de la etapa intentan devolver | 403 con el mismo cuerpo | US4-1, SC-003 |
| 3 | `consolidacion.demo` devuelve sin observación, con una de 9 caracteres, con una solo de espacios, con una carrera repetida y con dos carreras de las que una no tiene observación | 400 con su código, y ningún parcial devuelto | US3, SC-001 |
| 4 | Devuelve una carrera inexistente junto con una válida | 404 `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO`; ninguna devuelta | FR-005 |
| 5 | Devuelve dos carreras con su observación | 200; BORRADOR_CARRERA; las dos con `devolucion` y su observación, las demás enviadas; una entrada `DISTRIBUTIVO_DEVUELTO` con las dos observaciones | US1-1, US1-2, US1-3, SC-002 |
| 6 | Devuelve otra vez | 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION`, sin entrada | US4-3 |
| 7 | El coordinador cambia una propuesta en una carrera devuelta y en una no devuelta | 200 en la devuelta; 409 `GENERACION_PARCIAL_NO_EDITABLE` en la otra | US2-2, US2-4 |
| 8 | Reenvía la primera carrera devuelta, después la segunda | BORRADOR_CARRERA tras la primera; EN_CONSOLIDACION tras la segunda, con su `ETAPA_AVANZADA` | US2-3, FR-010 |
| 9 | Revisa la bitácora y las transiciones | Una sola `DISTRIBUTIVO_DEVUELTO`; ningún rechazo dejó transición ni entrada | FR-011, FR-013 |
