# Research: Justificar una asignación con una regla blanda

## 1. La respuesta (FR-006)

- **Decisión:** `proponerDocente` responde 200 con `ResultadoDePropuesta`: `guardada`, y `propuesta` o `advertencia`.
- **Por qué:** la constitución: «una inconsistencia blanda se devuelve en una respuesta correcta, con su advertencia». El autor lo confirmó.
- **Consecuencia:** un 200 ya no significa guardado. La pantalla y los scripts miran `guardada`.
- **Alternativas:** 409 con su código: es más simple, pero contradice la constitución. Un estado 202: dice «en proceso», y aquí no queda nada en proceso.

## 2. Dónde entra RN-19

- **Decisión:** `MotorReglas.evaluarAfinidad(puntaje, minimo, troncoComun, reglas)`, después de RN-02 y RN-13.
- **Por qué:** una regla dura violada no se resuelve con un texto (FR-002).
- **El orden en el servicio:** las reglas duras corren en la transacción, con la versión bloqueada (D-83). `Justificacion.para` queda después, porque hoy corre antes y pediría un texto inútil.

## 3. La propuesta provisional

- **Decisión:** para evaluar las horas se usa la propuesta con una justificación provisional, y se reemplaza antes de guardar.
- **Por qué:** `conPropuesta` solo mira el docente y el paralelo. Así no se duplica la cuenta (FR-004 de F07-H02).

## 4. La severidad (principio VI)

- **Decisión:** `consultarReglasDeAsignacion` agrega `severidadAfinidad`. `ReglasDelPeriodo` la lleva.
- **Sin severidad:** Normativa falla y Generación responde con el error entre procesos: no se guarda (FR-018).

## 5. El texto (FR-009, FR-010)

- **Vacío o solo espacios:** advertencia, porque falta.
- **De 1 a 9 o más de 500 caracteres:** `JustificacionObligatoria`, un error del campo `texto`, como hoy.
- **Por qué:** escribir poco es corregible en el campo; no escribir nada es la regla.

## 6. La sugerencia (FR-017)

- **Decisión:** `Sugerencia.armar` pone el motivo `RN_19` a quien está bajo el mínimo si la severidad es dura y no es tronco común. El motivo de horas va primero, como en el guardado.
- **`motivoParaNoProponer`:** agrega `AFINIDAD`.

## 7. Justificada (FR-013, MD-07)

- **Decisión:** `Propuesta.inconsistencia()`, derivada del tipo: `excepción` da `RN_19` justificada. En el contrato, `inconsistencia` opcional.
- **Por qué:** no se guarda nada nuevo: una excepción es, por definición, una inconsistencia blanda justificada. F10-H01 la leerá.

## 8. La pantalla (FR-019, FR-020)

- **Decisión:** en `useEnvio`, `proponerDocente` deja el resultado en una referencia. Si no se guardó, abre `DialogoDeJustificacion`, con la advertencia como aviso, y no cierra.
- **`DialogoDeJustificacion`:** muestra el aviso con severidad `warning`, no `error`.
- **Lo escrito:** sigue en el campo si vuelve un error (FR-021).

## 9. 60/40 (D-85)

- **Decisión:** se justifica en F10-H01, con el panel. D-85 lo deja escrito, y `specs/033-totales-del-docente/spec.md` agrega la nota.
- **Por qué:** es una regla global. Mientras se arma la carga, casi todos se apartan de la meta.

## 10. Las pruebas

- **Motor:** sin dobles, tabla de casos.
- **Punta a punta:** `deploy/probar_justificacion.sh`, con otra versión de normativa con RN-19 dura.
- **Ya existentes:** `probar_propuestas.sh`, `probar_reglas_duras.sh`, `probar_actividades.sh` y `probar_totales.sh` proponen: un 200 ya no basta, y deben mirar `guardada`.
