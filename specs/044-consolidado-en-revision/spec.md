# Feature Specification: El consolidado no cambia durante la revisión

**Feature Branch**: `044-consolidado-en-revision`

**Created**: 2026-11-08

**Status**: Draft

**Input**: User description: "#215 · Generación permite consolidar, justificar y cambiar el informe después de enviar a revisión (defecto de F10-H03). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md y docs/decisiones.md (D-95, D-97, D-98, MD-04). Decisiones del autor: (1) se bloquea desde EN_REVISION_VRA en adelante; antes, todo sigue como hoy (en BORRADOR_CARRERA y EN_CONSOLIDACION se puede consolidar); (2) se bloquean las tres cosas: consolidar, justificar o quitar la justificación de una blanda, y adjuntar o reemplazar el informe sustentado; descargar el informe sigue permitido; (3) en «Consolidado del periodo» y en el panel desaparecen esos botones y aparece el aviso «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.». El servidor responde 409 del catálogo y nada cambia."

**Origen**: issue #215 · defecto (`tipo:defecto`, `prioridad:must`, `modulo:05-generacion`, `RF-32`, `RF-34`) · milestone «Ola 3 · Aprobado y firmado». Sale de F10-H03 (#55); lo encontró el autor al comprobar F12-H05 (#211) el 2026-11-08 en el perfil local.

**Requerimientos que cubre**:
- **RF-32:** el consolidado con su informe sustentado. Lo que se envía a revisión es lo que se consolidó.
- **RF-34:** el flujo de estados; cada etapa tiene su responsable, y la consolidación es la del Responsable de consolidación.
- **D-97:** enviar a revisión exige el consolidado al día, su informe y cero abiertas. La confirmación de F10-H03 promete: «Ya no se podrá consolidar ni justificar en esta etapa».

**Decisiones que aplica**: MD-04 (Aprobación ordena a Distributivo), D-95 (la transición la registra Aprobación), D-97 y D-98. **Principios de la constitución**: IV, V, VII, IX, XIII y XIV.

**El defecto (tal como está en el issue)**: con el distributivo en EN_REVISION_VRA, el Responsable de consolidación sigue viendo «Volver a consolidar». Generación solo rechaza consolidar si el periodo está cerrado: no sabe en qué etapa está el distributivo. Lo mismo vale para justificar una blanda y para el informe.

**Por qué importa**: el Vicerrector revisa el consolidado que se le envió, con su informe y sus justificaciones. Si cambian mientras lo revisa, recibe algo distinto de lo enviado, y la huella del informe de la entrada `CONSOLIDADO_ENVIADO_A_REVISION` deja de coincidir con el vigente.

## Clarifications

### Session 2026-11-08

- Q: ¿Desde qué etapa se bloquea? → A: Desde EN_REVISION_VRA en adelante. En BORRADOR_CARRERA y EN_CONSOLIDACION todo sigue como hoy, también consolidar para ver cómo va.
- Q: ¿Qué se bloquea? → A: Las tres cosas que forman lo que recibe el Vicerrector: consolidar, justificar una blanda (o cambiar su justificación) y adjuntar o reemplazar el informe. Consultar el consolidado, el panel y descargar el informe siguen permitidos.
- Q: ¿Qué ve el Responsable? → A: No ve los botones de esas tres acciones, y en su lugar un aviso: «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.»

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lo enviado a revisión no cambia (Priority: P1)

Como Vicerrector académico, quiero que el consolidado que recibí no cambie mientras lo reviso, para aprobar exactamente lo que se envió.

**Why this priority**: es el defecto: hoy el consolidado, las justificaciones y el informe pueden cambiar durante la revisión.

**Independent Test**: con el distributivo en EN_REVISION_VRA, intentar consolidar, justificar una blanda y adjuntar un informe como Responsable de consolidación; recibir en cada caso un 409 y comprobar que nada cambió.

**Acceptance Scenarios**:

1. **Dado** el distributivo en EN_REVISION_VRA o en una etapa posterior, **cuando** el Responsable intenta consolidar, **entonces** se rechaza con un 409 del catálogo y la consolidación vigente sigue siendo la misma. *(issue)*
2. **Dado** el distributivo en EN_REVISION_VRA, **cuando** el Responsable intenta justificar una blanda o cambiar una justificación, **entonces** se rechaza con un 409 y el panel no cambia. *(issue)*
3. **Dado** el distributivo en EN_REVISION_VRA, **cuando** el Responsable intenta adjuntar o reemplazar el informe, **entonces** se rechaza con un 409 y el informe vigente es el mismo que se envió. *(issue)*
4. **Dado** el distributivo en EN_REVISION_VRA, **cuando** cualquiera con permiso consulta el consolidado, el panel o descarga el informe, **entonces** lo puede hacer como antes.
5. **Dado** un intento rechazado, **cuando** se consulta la bitácora, **entonces** no dejó ninguna entrada.

---

### User Story 2 - La pantalla no lo ofrece (Priority: P1)

Como Responsable de consolidación, quiero que la pantalla no me ofrezca lo que ya no puedo hacer, y que me diga por qué, para no intentarlo y recibir un error.

**Why this priority**: es el segundo escenario del issue; el botón visible fue lo que reveló el defecto.

**Independent Test**: con el distributivo en EN_REVISION_VRA, abrir «Consolidado del periodo» y el panel como Responsable: no aparecen «Volver a consolidar», «Adjuntar el informe» ni justificar, y aparece el aviso.

**Acceptance Scenarios**:

1. **Dado** el distributivo fuera de BORRADOR_CARRERA y EN_CONSOLIDACION, **cuando** el Responsable abre «Consolidado del periodo», **entonces** no ve «Consolidar» o «Volver a consolidar» ni «Adjuntar el informe», y ve el aviso «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.». *(issue)*
2. **Dado** lo mismo, **cuando** abre el panel de inconsistencias, **entonces** no ve la acción de justificar y ve el mismo aviso.
3. **Dado** lo mismo, **cuando** abre el consolidado, **entonces** sigue viendo el informe con su botón para descargarlo.
4. **Dado** el distributivo en BORRADOR_CARRERA o EN_CONSOLIDACION, **cuando** abre las mismas pantallas, **entonces** ve los botones como hoy y no ve el aviso.

---

### User Story 3 - Antes de la revisión, todo sigue igual (Priority: P1)

Como Responsable de consolidación, quiero seguir consolidando, justificando y adjuntando el informe mientras el distributivo no se envió a revisión, para preparar el envío como hasta ahora.

**Why this priority**: el arreglo no debe romper F10-H01, F10-H02, F10-H03 ni F12-H05.

**Independent Test**: en BORRADOR_CARRERA y en EN_CONSOLIDACION, consolidar, justificar una blanda y adjuntar un informe responden como hoy; tras una devolución (F12-H05), el distributivo vuelve a BORRADOR_CARRERA y se puede consolidar otra vez.

**Acceptance Scenarios**:

1. **Dado** el distributivo en BORRADOR_CARRERA o EN_CONSOLIDACION, **cuando** el Responsable consolida, justifica o adjunta el informe, **entonces** funciona como hoy.
2. **Dado** un periodo sin distributivo o sin proceso de aprobación todavía, **cuando** el Responsable consolida, **entonces** se comporta como hoy.

---

### Edge Cases

- **Enviar a revisión y consolidar a la vez:** pasa uno; si pasó el envío, el consolidar se rechaza; si pasó el consolidar, el envío ve la consolidación nueva (es la que se envía). Nunca queda una consolidación posterior al envío.
- **Justificar mientras se envía a revisión:** igual que el caso anterior.
- **Una etapa posterior (CONGELADO_EN_FIRMA, EN_OCS, APROBADO, PUBLICADO):** también se rechaza.
- **Una devolución futura desde EN_REVISION_VRA** (historias de esa etapa): si el distributivo vuelve a una etapa anterior a la revisión, se puede consolidar otra vez.
- **Un periodo cerrado:** se sigue rechazando como hoy, con su error propio.
- **Un usuario sin permiso:** recibe el 403 de siempre, antes que este 409.

## Requirements *(mandatory)*

### Functional Requirements

**Bloqueo**

- **FR-001**: Con el distributivo en EN_REVISION_VRA o en una etapa posterior, consolidar DEBE rechazarse con un 409 del catálogo, sin cambiar nada.
- **FR-002**: En las mismas etapas, justificar una inconsistencia blanda, o cambiar su justificación, DEBE rechazarse con un 409 del catálogo, sin cambiar nada.
- **FR-003**: En las mismas etapas, adjuntar o reemplazar el informe sustentado DEBE rechazarse con un 409 del catálogo, sin cambiar nada.
- **FR-004**: Consultar el consolidado y el panel, y descargar el informe, DEBEN seguir permitidos en cualquier etapa.
- **FR-005**: En BORRADOR_CARRERA y EN_CONSOLIDACION, y sin proceso de aprobación todavía, las tres acciones DEBEN funcionar como hoy.
- **FR-006**: La etapa DEBE comprobarse en el servidor, en el momento de la acción, de modo que una acción que llega después del envío a revisión se rechace aunque la pantalla se haya abierto antes.
- **FR-007**: Un intento rechazado NO DEBE dejar entrada en la bitácora ni en el registro de cambios.

**Pantalla**

- **FR-008**: Fuera de BORRADOR_CARRERA y EN_CONSOLIDACION, «Consolidado del periodo» NO DEBE mostrar «Consolidar», «Volver a consolidar» ni «Adjuntar el informe», y el panel NO DEBE mostrar la acción de justificar.
- **FR-009**: En su lugar, las dos pantallas DEBEN mostrar el aviso «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.».
- **FR-010**: El informe vigente DEBE seguir visible con su descarga.

**Errores**

- **FR-011**: El error nuevo DEBE estar en el catálogo, con sus 8 columnas (principio XIII), y decir que el consolidado ya se envió a revisión.

### Key Entities

- **Consolidado** (de F10-H02): la consolidación, el informe sustentado y si quedó desactualizado.
- **Panel de inconsistencias** (de F10-H01): las inconsistencias con sus justificaciones.
- **Proceso de aprobación** (de F12-H02): la etapa del distributivo, que hoy solo conoce Aprobación.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los intentos de consolidar, justificar o cambiar el informe con el distributivo en EN_REVISION_VRA o después se rechazan, y lo que se envió a revisión (la consolidación, las justificaciones y la huella del informe) es idéntico antes y después del intento.
- **SC-002**: En BORRADOR_CARRERA y EN_CONSOLIDACION, el 100 % de los escenarios de F10-H01, F10-H02, F10-H03 y F12-H05 siguen pasando sin cambios.
- **SC-003**: Durante la revisión, el Responsable ve el aviso y ninguna de las tres acciones en las dos pantallas, sin necesidad de intentar ninguna.
- **SC-004**: Con un envío a revisión y una consolidación simultáneos, nunca queda una consolidación con fecha posterior al envío.

## Assumptions

- **«Desde EN_REVISION_VRA en adelante»** quiere decir cualquier etapa después de EN_CONSOLIDACION en el orden del flujo. Si EN_REVISION_VRA está desactivada, la regla se aplica a la siguiente etapa activa, que es a la que se envía el distributivo.
- **El aviso es uno solo** para todas las etapas posteriores. Dice «revisión del Vicerrectorado» porque es la primera; las etapas de firma tendrán sus propias pantallas.
- **Las propuestas y las actividades** ya no se editan en esas etapas, porque los parciales están enviados (F12-H02); esta historia no las toca.
- **Las devoluciones desde EN_REVISION_VRA o la firma** son de otras historias (F13-H01, F12-H04, F14-H04). Si devuelven el distributivo a una etapa anterior, esta regla deja de bloquear.
- **Sin migración de datos visible para el usuario:** el distributivo de 2026-I de la demo está en EN_CONSOLIDACION, así que nada cambia para él al desplegar.

## Out of Scope

- Bloquear consolidar en BORRADOR_CARRERA (se decidió mantenerlo).
- Lo que hace el Vicerrector en EN_REVISION_VRA: ajustes (RF-35), aprobar y congelar (F13-H01).
- Devolver desde EN_REVISION_VRA.
