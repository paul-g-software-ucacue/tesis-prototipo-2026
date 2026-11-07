# Contrato: `deploy/probar_consulta_bitacora.sh`

Corre en el workflow `perfil-consolidado` después de `probar_bitacora.sh`, sobre el perfil consolidado levantado, con los usuarios de la demo. Usa la API por el API Gateway, como la SPA, y la base solo con consultas de solo lectura para comparar. Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime una clave ni un token.

| Paso | Qué hace | Qué comprueba | Requisito |
|---|---|---|---|
| 1 | Prepara un periodo con distributivo y una actividad de 4 horas (datos de prueba, como `probar_bitacora.sh`) | — | — |
| 2 | Con `vicerrectorado.demo`, consulta por `coordinacion.demo` y el día de hoy | Todas las entradas son de ese usuario y de hoy en hora de Ecuador, y son las mismas que da la consulta de solo lectura sobre `usuarios.bitacora` | US1-1, SC-001 |
| 3 | Con `admin.demo`, la misma consulta | El mismo resultado | US1-1, FR-007 |
| 4 | Consulta por servicio y acción, por identificador (la actividad), por periodo y combinados | Cada resultado cumple todos sus filtros; un filtro sin coincidencias da `total: 0` y 200 | US1-3, US1-4, US1-5 |
| 5 | Pide la página 2 con el `corte` de la 1, después de provocar una entrada nueva | La página 2 no repite entradas de la 1 y el total no cambia | FR-004, caso borde |
| 6 | Guarda la actividad de 4 horas con 4 horas, y después la cambia a 6 | Ninguna entrada nueva por el primer guardado; una de 4 a 6 por el segundo | US3-1, US3-2, FR-012 |
| 7 | Con `coordinacion.demo` y con un usuario de cada otro rol de la demo, consulta la bitácora | 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO` en todos; sin token, 401 | US2, SC-002 |
| 8 | Envía filtros inválidos (`hasta` antes de `desde`, `accion` sin `servicio`, `tamano=101`, `identificador` con espacio) | 400 `COMUN_SOLICITUD_INVALIDA` con el detalle de cada campo | FR-010 |
| 9 | Cuenta las entradas antes y después de los pasos 2 a 8 | Solo cambian por los cambios del paso 1, 5 y 6 | SC-005 |

Al terminar borra lo que creó, salvo las entradas de la bitácora, que no se pueden borrar.
