# Feature Specification: Aprobar el contenido y congelar la versión

**Feature Branch**: `045-congelar-version`

**Created**: 2026-11-09

**Status**: Draft

**Input**: User description: "F13-H01 · Aprobar el contenido y congelar la versión (#60). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md y docs/decisiones.md (D-95, D-97, D-98, D-99, MD-04) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §2: EN_REVISION_VRA, «Aprobar contenido», al aprobar se congela la versión). Decisiones del autor: (1) solo el rol VICERRECTOR, en EN_REVISION_VRA, aprueba; el distributivo pasa a CONGELADO_EN_FIRMA y la versión vigente queda CONGELADA con su hash de contenido (el SHA-256 que ya usan las transiciones), registrado en la transición y en la bitácora; (2) la pantalla pide un diálogo de confirmación que dice que la versión N queda congelada y que todo cambio posterior exigirá una versión nueva, con «Aprobar y congelar» y «Cancelar»; (3) sin comentario al aprobar; (4) después de congelar, la tarjeta del flujo en «Consolidado del periodo» muestra «Versión N congelada por <usuario> el <fecha>» y la huella SHA-256 abreviada con opción de copiarla completa; las acciones de edición desaparecen con un aviso de versión congelada; (5) cualquier edición de una versión congelada (propuestas, actividades, consolidar, justificar, informe) responde 409 del catálogo y no cambia nada; (6) no notifica todavía (F16-H01); los ajustes del Vicerrector (RF-35), devolver desde EN_REVISION_VRA y el PDF/A (F13-H02) quedan fuera."

**Origen**: issue #60 · historia (`tipo:historia`, `prioridad:must`, `modulo:05-generacion`, `RF-38`, `RF-40`, `RF-59`) · feature F13 (#12) · milestone «Ola 3 · Aprobado y firmado». Depende de F10-H03 (#55), terminada.

**Requerimientos que cubre**:
- **RF-38:** la firma se hace en orden y sobre la misma versión congelada. Esta historia congela la versión; el PDF/A (F13-H02) y las firmas (F14) vienen después.
- **RF-40:** la versión aprobada queda bloqueada: no hay acciones de edición sobre ella.
- **RF-59:** el versionado interno; la firma se aplica siempre sobre una versión congelada, y un cambio posterior exige una versión nueva.

**Decisiones que aplica**: MD-04 (Aprobación ordena a Distributivo), D-95 (la transición la registra Aprobación; Generación no duplica el cambio de estado), D-97, D-98 y D-99 (desde EN_REVISION_VRA el consolidado no cambia). Diseño de firmas §2: en EN_REVISION_VRA actúa el Vicerrector, «Aprobar contenido», y al aprobar se congela la versión. **Principios de la constitución**: IV, V, VII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-09

- Q: ¿Qué pide la pantalla antes de congelar? → A: Un diálogo de confirmación que dice que la versión N queda congelada y que todo cambio posterior exigirá una versión nueva, con «Aprobar y congelar» y «Cancelar».
- Q: ¿El Vicerrector deja un comentario al aprobar? → A: No. La aprobación queda con quién, cuándo, la versión y la huella.
- Q: ¿Qué muestra la tarjeta del flujo después de congelar? → A: «Versión N congelada por <usuario> el <fecha>» y la huella SHA-256 abreviada, con opción de copiarla completa. Las acciones de edición desaparecen y aparece un aviso de versión congelada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Aprobar y congelar (Priority: P1)

Como Vicerrector académico, quiero aprobar el contenido y que la versión quede congelada, para que los tres firmantes firmen exactamente lo que aprobé.

**Why this priority**: es el primer escenario del issue y el paso que falta en el flujo: hoy el distributivo llega a EN_REVISION_VRA y ahí se queda.

**Independent Test**: con el distributivo en EN_REVISION_VRA, entrar como Vicerrector, abrir «Consolidado del periodo», pulsar «Aprobar el contenido» y confirmar; el distributivo pasa a CONGELADO_EN_FIRMA, la versión queda congelada con su huella, y la bitácora tiene una entrada con quién, cuándo, la versión y la huella.

**Acceptance Scenarios**:

1. **Dado** el distributivo en EN_REVISION_VRA, **cuando** el Vicerrector aprueba el contenido y confirma, **entonces** la versión vigente queda CONGELADA con su hash de contenido **y** el distributivo pasa a CONGELADO_EN_FIRMA. *(issue)*
2. **Dado** lo mismo, **cuando** se aprueba, **entonces** la transición y la bitácora registran una sola entrada con quién, cuándo, desde EN_REVISION_VRA hacia CONGELADO_EN_FIRMA, la versión y la huella.
3. **Dado** lo mismo, **cuando** se aprueba, **entonces** la huella registrada es la misma que se registró al enviar a revisión (nada cambió durante la revisión, D-99).
4. **Dado** el diálogo de confirmación abierto, **cuando** el Vicerrector pulsa «Cancelar», **entonces** nada cambia.
5. **Dado** el distributivo en otra etapa (BORRADOR_CARRERA, EN_CONSOLIDACION, CONGELADO_EN_FIRMA o posterior), **cuando** el Vicerrector intenta aprobar el contenido, **entonces** se rechaza con un 409 del catálogo y nada cambia.
6. **Dado** un usuario sin el rol VICERRECTOR (por ejemplo, el Rector, que tiene el permiso pero no es el rol de la etapa), **cuando** intenta aprobar el contenido, **entonces** se rechaza con un 403 y nada cambia.

---

### User Story 2 - La versión congelada no cambia (Priority: P1)

Como firmante, quiero que la versión congelada no cambie, para firmar lo mismo que aprobó el Vicerrector.

**Why this priority**: es el segundo escenario del issue y la razón de congelar (RF-40).

**Independent Test**: con la versión congelada, intentar por la API guardar o quitar una propuesta, cambiar una actividad, consolidar, justificar una blanda y adjuntar el informe; cada intento responde 409 y la huella sigue siendo la misma.

**Acceptance Scenarios**:

1. **Dado** una versión congelada, **cuando** alguien intenta editar una asignación (guardar o quitar una propuesta, cambiar una actividad no lectiva), **entonces** se rechaza con un 409 del catálogo y nada cambia. *(issue)*
2. **Dado** una versión congelada, **cuando** alguien intenta consolidar, justificar una blanda o adjuntar el informe, **entonces** se rechaza con un 409 y nada cambia.
3. **Dado** un intento rechazado, **cuando** se consulta la bitácora, **entonces** no dejó ninguna entrada.
4. **Dado** una versión congelada, **cuando** cualquiera con permiso consulta el distributivo, el consolidado, el panel o descarga el informe, **entonces** lo puede hacer como antes.

---

### User Story 3 - La pantalla lo muestra (Priority: P2)

Como Vicerrector, Responsable de consolidación o coordinador, quiero ver que la versión está congelada, quién la congeló, cuándo y con qué huella, para saber qué se va a firmar y no intentar cambiarlo.

**Why this priority**: el segundo escenario del issue pide que no haya acciones de edición disponibles; la huella visible permite comprobar después que se firmó lo mismo.

**Independent Test**: con el distributivo en EN_REVISION_VRA, el Vicerrector ve «Aprobar el contenido»; los demás no. Tras congelar, la tarjeta del flujo muestra la versión, quién, cuándo y la huella abreviada, que se puede copiar completa; no hay acciones de edición y aparece el aviso.

**Acceptance Scenarios**:

1. **Dado** el distributivo en EN_REVISION_VRA, **cuando** el Vicerrector abre «Consolidado del periodo», **entonces** ve «Aprobar el contenido» en la tarjeta del flujo; ningún otro rol lo ve.
2. **Dado** que el Vicerrector pulsa «Aprobar el contenido», **cuando** se abre el diálogo, **entonces** dice que la versión N queda congelada y que todo cambio posterior exigirá una versión nueva, con «Aprobar y congelar» y «Cancelar».
3. **Dado** una versión congelada, **cuando** cualquiera con acceso abre «Consolidado del periodo», **entonces** la tarjeta del flujo muestra «Versión N congelada por <usuario> el <fecha>» y la huella abreviada, con una acción para copiarla completa.
4. **Dado** una versión congelada, **cuando** se abren «Consolidado del periodo» o el panel de inconsistencias, **entonces** no hay acciones de edición y aparece el aviso «La versión N está congelada para la firma: ya no cambia.».
5. **Dado** una versión congelada, **cuando** un coordinador abre las pantallas de propuestas y actividades, **entonces** no hay acciones de edición, como ya pasa con un parcial enviado.

---

### Edge Cases

- **Dos aprobaciones a la vez** (dos pestañas, o dos clics): pasa una; la otra se rechaza porque el distributivo ya no está en EN_REVISION_VRA. Queda una sola transición y una sola entrada en la bitácora.
- **Una aprobación que se corta a mitad** (la versión quedó congelada pero la etapa no avanzó): volver a aprobar la completa, sin congelar dos veces ni dejar dos entradas. Mientras tanto, la pantalla muestra el aviso de versión congelada y sigue ofreciendo «Aprobar el contenido» al Vicerrector, pero no la constancia, que sale de la transición.
- **Una edición que llega mientras se aprueba:** desde EN_REVISION_VRA el consolidado ya no cambia (D-99) y los parciales están enviados; nada puede quedar con fecha posterior a la congelación.
- **La pantalla abierta antes de congelar:** si alguien intenta editar con una pantalla vieja, el servidor rechaza con el 409.
- **La etapa EN_REVISION_VRA desactivada en la configuración:** el envío a revisión pasa directo a CONGELADO_EN_FIRMA, y la versión se congela en ese mismo envío: nunca se llega a la firma con una versión editable (RF-59).
- **Un periodo sin distributivo o sin proceso de aprobación:** aprobar se rechaza con el error que ya existe para eso.

## Requirements *(mandatory)*

### Functional Requirements

**Aprobar y congelar**

- **FR-001**: El Vicerrector DEBE poder aprobar el contenido del distributivo cuando está en EN_REVISION_VRA.
- **FR-002**: Al aprobar, la versión vigente DEBE quedar CONGELADA con su hash de contenido, y el distributivo DEBE pasar a CONGELADO_EN_FIRMA, las dos cosas o ninguna (si se corta a mitad, volver a aprobar lo completa).
- **FR-003**: El hash de contenido DEBE ser el SHA-256 del contenido del distributivo que ya registran las transiciones del flujo.
- **FR-004**: La transición DEBE registrar quién, cuándo, desde EN_REVISION_VRA hacia CONGELADO_EN_FIRMA, la versión y el hash, y la bitácora DEBE recibir **una** entrada con esos datos (D-95).
- **FR-005**: Aprobar DEBE rechazarse sin cambiar nada: con un 409 del catálogo si el distributivo no está en EN_REVISION_VRA, y con un 403 (acceso denegado, como al enviar o devolver un parcial) si quien lo pide no tiene el permiso o no tiene el rol de la etapa, VICERRECTOR.
- **FR-006**: Aprobar NO DEBE pedir un comentario.
- **FR-016**: Si la configuración desactiva EN_REVISION_VRA, el envío a revisión, que pasa directo a CONGELADO_EN_FIRMA, DEBE congelar la versión de la misma forma: ningún distributivo entra en CONGELADO_EN_FIRMA con una versión editable.

**Versión congelada**

- **FR-007**: Con la versión vigente congelada, toda edición de su contenido DEBE rechazarse con un 409 del catálogo, sin cambiar nada: guardar o quitar una propuesta, cambiar una actividad no lectiva, consolidar, justificar una blanda y adjuntar o reemplazar el informe.
- **FR-008**: La comprobación DEBE hacerse en el servidor, en el momento de la acción, aunque la pantalla se haya abierto antes de congelar.
- **FR-009**: Un intento rechazado NO DEBE dejar entrada en la bitácora ni en el registro de cambios.
- **FR-010**: Consultar el distributivo, el consolidado y el panel, y descargar el informe, DEBEN seguir permitidos.

**Pantalla**

- **FR-011**: En EN_REVISION_VRA, la tarjeta del flujo de «Consolidado del periodo» DEBE ofrecer «Aprobar el contenido» solo al Vicerrector.
- **FR-012**: Antes de aprobar, la pantalla DEBE pedir confirmación con un diálogo que diga que la versión N queda congelada y que todo cambio posterior exigirá una versión nueva, con «Aprobar y congelar» y «Cancelar».
- **FR-013**: Con la versión congelada, la tarjeta del flujo DEBE mostrar «Versión N congelada por <usuario> el <fecha>» y la huella abreviada, con una acción para copiar la huella completa.
- **FR-014**: Con la versión congelada, ninguna pantalla DEBE ofrecer acciones de edición, y «Consolidado del periodo» y el panel DEBEN mostrar el aviso «La versión N está congelada para la firma: ya no cambia.».

**Errores**

- **FR-015**: Los errores nuevos DEBEN estar en el catálogo con sus 8 columnas (principio XIII): el de una versión congelada dice que la versión ya se aprobó y que un cambio exige una versión nueva.

### Key Entities

- **Versión del distributivo** (de F09/F10): su número y su estado (EDITABLE, CONGELADA o ANULADA, RF-59). Al congelarse solo cambia de estado; quién, cuándo y la huella quedan en la transición (D-95).
- **Proceso de aprobación** (de F12-H02): la etapa del distributivo y sus transiciones; la nueva va de EN_REVISION_VRA a CONGELADO_EN_FIRMA.
- **Transición** (de F12-H02): quién, cuándo, desde, hacia, la versión y el hash. La de EN_REVISION_VRA a CONGELADO_EN_FIRMA es la constancia de la congelación.
- **Huella de contenido:** el SHA-256 del contenido de la versión, el mismo que firmarán los tres firmantes sobre el PDF/A de F13-H02. «Huella» es el nombre que ve el usuario; los contratos, las transiciones y la bitácora lo llaman «hash». Son lo mismo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con la versión congelada, el 100 % de los intentos de edición (propuestas, actividades, consolidar, justificar e informe) se rechazan, y la huella del contenido es idéntica antes y después de cada intento.
- **SC-002**: La huella de la congelación es igual a la registrada al enviar a revisión en el 100 % de los casos.
- **SC-003**: El Vicerrector aprueba y congela en menos de 1 minuto desde que abre «Consolidado del periodo».
- **SC-004**: Con dos aprobaciones simultáneas, queda exactamente una transición y una entrada en la bitácora.
- **SC-005**: Los escenarios de F10-H03, F12-H05 y #215 siguen pasando sin cambios.
- **SC-006**: Tras congelar, cualquiera con acceso ve en la tarjeta del flujo la versión, quién, cuándo y la huella, sin abrir la bitácora.

## Assumptions

- **El Vicerrector** es el usuario con el rol VICERRECTOR, el responsable de la etapa EN_REVISION_VRA en la configuración del flujo (`vicerrectorado.demo` en la demo).
- **La siguiente etapa** es CONGELADO_EN_FIRMA: es parte del esqueleto fijo del flujo y no se puede desactivar. EN_REVISION_VRA sí se puede desactivar; entonces congela el envío a revisión (FR-016).
- **Sin firmas todavía:** CONGELADO_EN_FIRMA queda a la espera del PDF/A (F13-H02) y de las firmas (F14). Nadie actúa en esa etapa en esta historia.
- **Las propuestas y las actividades** ya no se editan desde que los parciales se enviaron (F12-H02), y el consolidado ya no cambia desde EN_REVISION_VRA (D-99). La congelación agrega una regla propia de la versión, que no depende de la etapa: vale aunque el distributivo vuelva atrás sin una versión nueva.
- **El aviso de versión congelada** reemplaza al de la revisión (#215) cuando la versión está congelada.
- **La demo:** 2026-I está en EN_CONSOLIDACION con 1 propuesta de 165 paralelos, así que no llega a EN_REVISION_VRA sin proponer los demás. El recorrido completo se prueba en el perfil local y en la CI.

## Out of Scope

- Los ajustes del Vicerrector durante la revisión (RF-35).
- Devolver desde EN_REVISION_VRA.
- El PDF/A del consolidado (F13-H02) y las firmas (F14).
- Crear una versión nueva tras un rechazo en firma (F12-H04) o por reforma (F18-H02).
- Notificar la aprobación (F16-H01).
