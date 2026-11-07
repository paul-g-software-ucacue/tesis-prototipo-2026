# Contrato: guardados sin cambios (D-94)

Ningún contrato de API cambia: las seis operaciones responden lo mismo que hoy, con éxito, y con el elemento que había. Cambia solo lo que queda registrado.

| Operación | Respuesta si nada cambió | Registro de cambios | Bitácora |
|---|---|---|---|
| Guardar una propuesta (Generación) | La propuesta que había, sin advertencias nuevas | Nada | Nada |
| Asignar una actividad no lectiva (Generación) | La actividad que había, con su `asignadaEn` y su `asignadaPor` | Nada | Nada |
| Justificar una inconsistencia (Generación) | La entrada del panel con la justificación que había | Nada | Nada |
| Corregir una matrícula (Académico, desde la carga 10) | La fila cuenta como aplicada en el informe de la carga | Nada | Nada |
| Editar un rol (Roles) | El rol con sus usuarios | Nada | Nada |
| Indicar la carrera de un coordinador (Roles) | Sin cuerpo, como hoy | Nada | Nada |

**Las validaciones van primero.** Una solicitud inválida, sin permiso o que viola una regla responde su error como hoy, aunque los valores sean los mismos: la comparación se hace después de validar.

**Texto de una justificación.** Si una propuesta o una inconsistencia cambia solo el texto de su justificación, hay cambio: se guarda y la entrada lleva `objeto.textoCambiado = true`. El texto nunca va a la bitácora (F17-H01).

## Pruebas

- **Aplicación (Mockito), por acción:** guardar con los mismos datos no llama a `guardar…` ni a `RegistroDeCambios.registrar`, y devuelve el elemento que había; con un dato distinto, los llama una vez. Para la propuesta y la inconsistencia, también el caso de solo el texto, con `textoCambiado`.
- **Integración de la carga 10:** confirmar dos veces una matrícula con los mismos valores deja una sola entrada.
- **Punta a punta:** `probar_consulta_bitacora.sh`, paso 6.
