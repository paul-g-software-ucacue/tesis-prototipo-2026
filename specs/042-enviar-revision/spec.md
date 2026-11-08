# Feature Specification: Enviar a revisión con cero inconsistencias abiertas

**Feature Branch**: `042-enviar-revision`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "F10-H03 · Enviar a revisión con cero inconsistencias abiertas (issue #55). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md y docs/decisiones.md; la transición a EN_REVISION_VRA debe registrarse en la bitácora de F17-H01 (D-90, D-95) y seguir el proceso de aprobación de F12-H02. La notificación al Vicerrector depende de F16-H01, que aún no existe."

**Origen**: issue #55 · F10-H03 · sub-issue de F10 (Consolidar, con el panel de inconsistencias y el informe sustentado, #10) · milestone «Ola 3 · Aprobado y firmado» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:06-validacion-ocs`, `RF-29`, `RF-34` · tamaño propuesto S · depende de #54 (F10-H02) y #57 (F12-H02), las dos cerradas.

**Requerimientos que cubre**:
- **RF-29:** las inconsistencias globales. Su criterio en la ERS: «El panel de inconsistencias está vacío antes de que se habilite enviar a revisión.»
- **RF-34:** el flujo de estados configurable; solo el rol de la etapa actual la avanza y cada transición queda auditada.
- **RF-32:** el consolidado con su informe sustentado. F10-H02 dejó para esta historia exigir el informe al enviar a revisión.
- **MD-07:** una inconsistencia blanda justificada deja de estar abierta. Enviar a revisión exige 0 duras y 0 blandas sin justificar.

**Decisiones que aplica**: ADR-003 (el Responsable de consolidación, designado por el Vicerrectorado), ADR-007 (esqueleto fijo y etapas opcionales), D-16 y D-64 (permisos), D-85 (el panel), D-90, D-94 y D-95 (bitácora; la transición la registra Aprobación con la versión y el hash). **Principios de la constitución**: III, IV, V, VII, IX, X, XIII y XIV.

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero enviar el consolidado a revisión del Vicerrectorado solo cuando no queden inconsistencias abiertas, para que el Vicerrector reciba un distributivo que cumple la política.

**Punto de partida.**
- F12-H02 dejó el proceso de aprobación: la etapa actual del distributivo, la regla de que solo el rol de la etapa la mueve y la transición con la versión y el hash en la bitácora. Cuando todas las carreras envían su parcial, el distributivo pasa a EN_CONSOLIDACION. El avance genérico rechaza EN_CONSOLIDACION, porque esa etapa se avanza con su propia operación, que es esta historia.
- F10-H01 dejó el panel de inconsistencias, calculado al abrirlo: las duras y las blandas, y la justificación de las blandas.
- F10-H02 dejó la consolidación (quién y cuándo, y si quedó desactualizada) y el informe sustentado en PDF.
- Nada lleva todavía el distributivo de EN_CONSOLIDACION a EN_REVISION_VRA.

## Clarifications

### Session 2026-11-08

- Q: El issue pide que el Vicerrector reciba una notificación, pero Notificaciones (F16-H01) todavía no existe. ¿Qué hace esta historia con el aviso? → A: Lo deja a F16-H01. Esta historia registra la transición en la bitácora, y F16-H01 notificará cada cambio de estado, incluido este. Así lo hizo F12-H02.
- Q: Además de cero inconsistencias abiertas, ¿qué más exige enviar a revisión? → A: Haber consolidado, que el consolidado no esté desactualizado y que tenga su informe sustentado. Cada falta tiene su propio error.
- Q: En EN_CONSOLIDACION todos los parciales están enviados y no admiten cambios, y devolver un parcial (RF-36) no tiene historia. ¿Cómo se corrige una inconsistencia dura en esa etapa? → A: Queda fuera de esta historia, con un issue nuevo para devolver un parcial a su carrera con una observación (#211, F12-H05). Esta historia solo hace el envío con sus condiciones, y su prueba usa un distributivo sin inconsistencias duras.

Esta historia agrega el envío a revisión: la operación que lleva el distributivo de EN_CONSOLIDACION a la siguiente etapa activa (por defecto, EN_REVISION_VRA), solo si está listo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enviar el consolidado a revisión (Priority: P1)

Como Responsable de consolidación, quiero enviar el consolidado a revisión del Vicerrectorado cuando está listo, para que el Vicerrector lo revise.

**Why this priority**: es el segundo escenario del issue y lo único que hoy saca al distributivo de EN_CONSOLIDACION.

**Independent Test**: con un distributivo en EN_CONSOLIDACION, consolidado, con su informe y el panel sin abiertas (en las pruebas automáticas, research §10), enviarlo a revisión con el rol de Responsable de consolidación; comprobar que queda en EN_REVISION_VRA y que la bitácora tiene la transición con quién, cuándo, desde qué estado, hacia cuál, la versión y el hash.

**Acceptance Scenarios**:

1. **Dado** un consolidado con el panel vacío, consolidado al día y con su informe, **cuando** lo envío a revisión, **entonces** pasa a EN_REVISION_VRA **y** la transición queda en la bitácora con quién, cuándo, desde qué estado, hacia cuál, la versión y el hash. *(issue)*
2. **Dado** blandas abiertas que ya justifiqué en el panel, **cuando** lo envío a revisión, **entonces** se envía: una blanda justificada no está abierta (MD-07).
3. **Dado** EN_REVISION_VRA desactivada en la configuración del distributivo, **cuando** lo envío, **entonces** pasa a la siguiente etapa activa.
4. **Dado** el distributivo ya enviado a revisión, **cuando** lo intento enviar otra vez, **entonces** se rechaza sin cambiar nada ni dejar otra entrada.

---

### User Story 2 - No enviar con inconsistencias abiertas (Priority: P1)

Como institución, quiero que no se pueda enviar a revisión con inconsistencias abiertas, para que el Vicerrector reciba un distributivo que cumple la política (RF-29, MD-07).

**Why this priority**: es el primer escenario del issue y la razón de la historia.

**Independent Test**: con una inconsistencia dura o una blanda sin justificar en el panel, intentar enviar y recibir 409 con cuántas quedan; justificar la blanda o quitar la dura y volver a intentar.

**Acceptance Scenarios**:

1. **Dado** un consolidado con una inconsistencia abierta, **cuando** intento enviarlo a revisión, **entonces** el envío se rechaza con un error 409 que dice cuántas inconsistencias quedan. *(issue)*
2. **Dado** 2 duras y 1 blanda sin justificar, **cuando** intento enviarlo, **entonces** el error dice que quedan 3: 2 duras y 1 blanda sin justificar.
3. **Dado** una inconsistencia que aparece entre que abro la pantalla y envío, **cuando** envío, **entonces** se rechaza: el panel se evalúa en el momento del envío, no con lo que vi.

---

### User Story 3 - No enviar sin consolidar ni sin informe (Priority: P1)

Como Vicerrector, quiero recibir el consolidado al día y con su informe sustentado, para revisar lo que el Responsable consolidó y no una versión vieja (RF-32).

**Why this priority**: lo dejó F10-H02 para esta historia; sin esto, el Vicerrector podría recibir un consolidado viejo o sin sustento.

**Independent Test**: intentar enviar sin haber consolidado, con el consolidado desactualizado y sin informe, y recibir en cada caso su 409; consolidar, adjuntar el informe y enviar.

**Acceptance Scenarios**:

1. **Dado** un distributivo que nunca se consolidó, **cuando** intento enviarlo, **entonces** se rechaza con un error que pide consolidar.
2. **Dado** un consolidado desactualizado, porque una propuesta o una actividad cambió después de consolidar, **cuando** intento enviarlo, **entonces** se rechaza con un error que pide consolidar otra vez.
3. **Dado** un consolidado sin informe sustentado, **cuando** intento enviarlo, **entonces** se rechaza con un error que pide adjuntarlo.
4. **Dado** un consolidado que no cumple ninguna condición, **cuando** abro la pantalla del consolidado, **entonces** veo todas las que faltan antes de intentar enviar.

---

### User Story 4 - Solo el Responsable de consolidación envía (Priority: P1)

Como institución, quiero que solo el rol de EN_CONSOLIDACION pueda enviar a revisión, para que la etapa la mueva quien corresponde (RF-34).

**Why this priority**: es la regla de F12-H02 aplicada a esta etapa.

**Independent Test**: con cada rol que no es el de EN_CONSOLIDACION, intentar enviar y recibir 403; consultar el estado del flujo y no ver la acción.

**Acceptance Scenarios**:

1. **Dado** un usuario sin el rol de EN_CONSOLIDACION en la configuración del distributivo, **cuando** intenta enviar a revisión, **entonces** recibe 403, sin saber qué rol falta.
2. **Dado** el Responsable de consolidación, **cuando** consulta el estado del flujo en EN_CONSOLIDACION, **entonces** ve la acción de enviar a revisión; otro rol no la ve.
3. **Dado** el distributivo en otra etapa (BORRADOR_CARRERA, EN_REVISION_VRA o posterior), **cuando** el Responsable intenta enviar a revisión, **entonces** se rechaza con 409: solo se envía desde EN_CONSOLIDACION.

---

### Edge Cases

- **Dos envíos a la vez:** el distributivo pasa una sola vez, con una sola transición.
- **Una inconsistencia blanda justificada después de consolidar:** no deja desactualizado el consolidado (F10-H02, FR-013), y deja de contar como abierta.
- **Un informe reemplazado:** vale el vigente.
- **Reglas que el panel todavía no evalúa** (`sinEvaluar`): no bloquean; el envío solo mira las inconsistencias que el panel calcula.
- **Un periodo cerrado:** no se envía.
- **Generación no responde** al evaluar el panel o el consolidado: el envío se rechaza con el error del catálogo, sin pasar de etapa ni dejar transición.
- **EN_CONSOLIDACION desactivada:** con el último parcial, F12-H02 pasa el distributivo directamente a la siguiente etapa activa, y esta historia no aplica.
- **Una inconsistencia dura en EN_CONSOLIDACION:** no se puede corregir mientras devolver un parcial no exista (#211, F12-H05); el distributivo espera en EN_CONSOLIDACION y la pantalla dice qué falta.
- **El usuario pierde el rol durante la sesión:** pierde el permiso en la próxima renovación del token (D-62); el servidor valida el rol en cada envío.

## Requirements *(mandatory)*

### Functional Requirements

**Enviar a revisión**

- **FR-001**: El Responsable de consolidación DEBE poder enviar a revisión el distributivo del periodo cuando está en EN_CONSOLIDACION.
- **FR-002**: Al enviarlo, el distributivo DEBE pasar a la siguiente etapa activa de su configuración (por defecto, EN_REVISION_VRA), en la misma respuesta.
- **FR-003**: Solo un usuario con el rol de EN_CONSOLIDACION en la configuración del distributivo DEBE poder enviarlo (regla de F12-H02). Cualquier otro recibe `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403), sin saber qué rol falta.
- **FR-004**: Fuera de EN_CONSOLIDACION, el envío a revisión DEBE rechazarse con un 409 del catálogo.

**Condiciones**

- **FR-005**: El envío DEBE rechazarse con 409 si quedan inconsistencias abiertas: duras, o blandas sin justificar (MD-07). El mensaje DEBE decir cuántas quedan, y cuántas son duras y cuántas blandas.
- **FR-006**: El envío DEBE rechazarse con 409 si el distributivo no se consolidó, o si el consolidado está desactualizado.
- **FR-007**: El envío DEBE rechazarse con 409 si el consolidado no tiene informe sustentado.
- **FR-008**: Las condiciones DEBEN evaluarse en el momento del envío, con el panel y el consolidado calculados entonces, no con lo que se vio en pantalla.
- **FR-009**: Cada condición que falla DEBE tener su propio error en el catálogo, con su mensaje y la acción que la corrige (principio XIII).

**Auditoría**

- **FR-010**: El envío DEBE quedar en la bitácora como una transición, con quién, cuándo, desde qué estado, hacia cuál, la versión del distributivo y el hash (RF-44, D-90, D-95). El hash es el del distributivo completo, en la misma forma fija que el paso de etapa de F12-H02.
- **FR-011**: El envío DEBE quedar en el historial de transiciones del distributivo.
- **FR-012**: Un intento rechazado (por rol, por etapa, por una condición o por periodo cerrado) NO DEBE dejar transición ni entrada.

**Consulta**

- **FR-013**: Quien consulta el estado del flujo en EN_CONSOLIDACION DEBE ver la acción de enviar a revisión solo si tiene el rol de la etapa.
- **FR-014**: La pantalla del consolidado DEBE mostrar al Responsable cuáles condiciones faltan para enviar (consolidar, consolidar otra vez, adjuntar el informe, inconsistencias abiertas con su número) y permitir enviar solo cuando no falta ninguna.

### Key Entities

- **Proceso de aprobación** (`ProcesoAprobacion`, de F12-H02): su etapa actual y el historial de transiciones. Esta historia le agrega la transición de EN_CONSOLIDACION a la siguiente etapa activa.
- **Transición** (`Transicion`, de F12-H02): desde qué estado, hacia cuál, quién, cuándo, la versión y el hash.
- **Panel de inconsistencias** (de F10-H01): las inconsistencias de la versión vigente, con su severidad y si están abiertas o justificadas.
- **Consolidado** (de F10-H02): quién y cuándo consolidó, si quedó desactualizado y el informe sustentado vigente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los intentos de enviar con al menos una inconsistencia abierta se rechazan, y el número que dice el error coincide con el del panel.
- **SC-002**: Con todas las condiciones cumplidas, el distributivo está en la siguiente etapa activa en la misma respuesta del envío, y la bitácora tiene exactamente una transición con los seis datos de RF-44.
- **SC-003**: El 100 % de los intentos de enviar con un rol que no es el de EN_CONSOLIDACION reciben 403: los 9 roles de la matriz D-16 que no son el de la etapa.
- **SC-004**: Con dos envíos simultáneos, el distributivo pasa de etapa exactamente una vez.
- **SC-005**: Con el consolidado listo, el Responsable de consolidación lo envía a revisión en menos de 30 segundos desde que abre la pantalla del consolidado.
- **SC-006**: Con un consolidado que no está listo, el Responsable ve en la pantalla todas las condiciones que faltan sin necesidad de intentar el envío.

## Assumptions

- **Notificaciones:** avisar al Vicerrector es de F16-H01, que notificará cada cambio de estado a partir de las transiciones. Esta historia no notifica. El tercer «Entonces» del issue queda cubierto cuando llegue F16-H01.
- **Quién es el Responsable de consolidación:** el usuario con ese rol (ADR-003), el mismo que consolida y adjunta en F10-H02.
- **El hash:** el del distributivo completo, en la forma fija de F12-H02. El PDF del consolidado, con su propio hash, llega con F13-H02.
- **El informe:** cualquier informe vigente adjuntado a la versión; esta historia no revisa su contenido.
- **Las condiciones que mira:** solo las inconsistencias que el panel calcula hoy; una regla que el panel no evalúa todavía no bloquea.
- **Correcciones en EN_CONSOLIDACION:** devolver un parcial a su carrera (RF-36) es de #211 (F12-H05). Mientras no exista, los datos de prueba no llegan al panel vacío (tienen duras): el envío que pasa se prueba con un distributivo sin inconsistencias duras en las pruebas automáticas, y la demostración muestra los rechazos con el número real del panel (research §10).

## Out of Scope

- Notificar al Vicerrector (F16-H01).
- Devolver un parcial o el distributivo a una etapa anterior, con su observación (RF-36; #211, F12-H05).
- Lo que pasa en EN_REVISION_VRA: los ajustes del Vicerrector (RF-35) y aprobar y congelar (F13-H01).
- Generar el PDF del consolidado (F13-H02).
- Los plazos de la etapa (F12-H03).
