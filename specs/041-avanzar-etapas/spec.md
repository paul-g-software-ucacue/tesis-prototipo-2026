# Feature Specification: Avanzar el distributivo según el rol de la etapa

**Feature Branch**: `041-avanzar-etapas`

**Created**: 2026-11-07

**Status**: Draft

**Input**: User description: "F12-H02 · Avanzar el distributivo según el rol de la etapa (issue #57). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md y docs/decisiones.md; cada transición debe registrarse en la bitácora de F17-H01 (D-90)."

**Origen**: issue #57 · F12-H02 · sub-issue de F12 (Flujo de etapas configurable con plazos, #11) · milestone «Ola 3 · Aprobado y firmado» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `RF-34`, `RF-44` · servicio 06-validacion-ocs · tamaño propuesto L · depende de #56 (F12-H01) y #48 (F06-H03), las dos cerradas.

**Requerimientos que cubre**:
- **RF-34:** un flujo de estados configurable. Su criterio en la ERS: «Solo el rol de la etapa actual puede avanzar o devolver el distributivo, y cada transición queda auditada. […] Un cambio de configuración no afecta a un distributivo en curso.»
- **RF-44:** cada transición queda en la bitácora (F17-H01, D-90).

**Decisiones que aplica**: ADR-007 (esqueleto fijo y etapas opcionales en un archivo; el parcial de carrera se aprueba en el sistema y no se firma), D-89 (la configuración del flujo es un YAML, F12-H01), D-16 y D-64 (permisos y «solo lo propio»: el coordinador, solo su carrera), MD-04 (el estado del distributivo es de Aprobación; Distributivo solo conoce el estado de la versión y de cada parcial), D-90 y D-94 (bitácora). **Principios de la constitución**: III, IV, V, VII, IX, X, XIII y XIV.

**Historia (tal como está en el issue)**: Como Coordinador de carrera, quiero enviar el parcial de mi carrera y que solo el rol de cada etapa pueda avanzar el distributivo, para que cada etapa la mueva quien corresponde.

**Punto de partida.**
- F12-H01 dejó la configuración del flujo por periodo: las 9 etapas, cuáles están activas, su nombre, su rol, su plazo y si admiten devolución. Por defecto están activas BORRADOR_CARRERA, EN_CONSOLIDACION, EN_REVISION_VRA, CONGELADO_EN_FIRMA, EN_OCS, APROBADO y PUBLICADO.
- Generación ya tiene el parcial de cada carrera con su estado (en elaboración, enviado o devuelto), y un parcial enviado ya no admite cambios. Pero no hay ninguna operación que lo envíe, ni un estado del distributivo que avance.
- La bitácora (F17-H01) ya registra los cambios de los seis servicios.

## Clarifications

### Session 2026-11-07

- Q: ¿De qué contenido es el hash de una transición en BORRADOR_CARRERA, si la versión todavía no está congelada? → A: El SHA-256 de los datos del parcial (sus propuestas y actividades), en una forma fija, calculado al enviarlo. No se genera un PDF: el PDF parcial puede llegar con F13-H02 y referenciar el mismo contenido. El paso automático de etapa lleva el hash del distributivo completo: el de todos los parciales enviados, en la misma forma fija.
- Q: ¿Qué etapas avanza esta historia? → A: El envío del parcial, el paso automático a la siguiente etapa activa y la regla del rol como pieza común de las historias siguientes. Además, un avance genérico solo para las etapas opcionales sin historia propia: hoy, BORRADOR_UNIDADES si se activa. Las demás las avanza su historia, con la misma regla de rol.

Esta historia agrega el proceso de aprobación del distributivo: su estado actual por etapa, el envío del parcial de cada carrera, el paso a la siguiente etapa activa y la regla de que solo el rol de la etapa la mueve.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enviar el parcial de mi carrera (Priority: P1)

Como Coordinador de carrera, quiero enviar el parcial de mi carrera, para que pase a la siguiente etapa y ya no se pueda cambiar por error.

**Why this priority**: es el primer paso del flujo de aprobación; sin él, nada de la ola avanza.

**Independent Test**: con `coordinacion.demo`, enviar el parcial de su carrera; comprobar que ya no admite cambios y que la bitácora tiene la transición con quién, cuándo, desde qué estado, hacia cuál, la versión y el hash.

**Acceptance Scenarios**:

1. **Dado** el parcial de mi carrera en BORRADOR_CARRERA, **cuando** lo envío, **entonces** el parcial queda enviado y ya no se puede editar, **y** la transición queda en la bitácora con quién, cuándo, desde qué estado, hacia cuál, la versión y el hash. *(issue)*
2. **Dado** el parcial de mi carrera ya enviado, **cuando** intento proponer, asignar o quitar algo en él, **entonces** se rechaza con «Parcial cerrado: este parcial ya no admite cambios.» (`GENERACION_PARCIAL_NO_EDITABLE`, que ya existe).
3. **Dado** el parcial de otra carrera, **cuando** intento enviarlo, **entonces** se rechaza: el coordinador solo envía el suyo (D-64).
4. **Dado** el parcial de mi carrera ya enviado, **cuando** intento enviarlo otra vez, **entonces** se rechaza sin cambiar nada ni dejar otra entrada.

---

### User Story 2 - Pasar a la siguiente etapa cuando todas las carreras enviaron (Priority: P1)

Como Responsable de consolidación, quiero que el distributivo pase solo a la siguiente etapa cuando todas las carreras enviaron su parcial, para empezar a consolidar sin esperar a que alguien lo mueva.

**Why this priority**: es el segundo escenario del issue y lo que conecta el envío con F10-H03 (enviar a revisión).

**Independent Test**: con dos carreras, enviar el primer parcial y ver que el distributivo sigue en BORRADOR_CARRERA; enviar el último y ver que pasa a la siguiente etapa activa de la configuración del periodo, con su transición en la bitácora.

**Acceptance Scenarios**:

1. **Dado** todas las carreras con su parcial enviado, **cuando** se envía el último, **entonces** el distributivo pasa a la siguiente etapa activa de la configuración. *(issue)*
2. **Dado** una configuración con BORRADOR_UNIDADES desactivada y EN_CONSOLIDACION activa, **cuando** se envía el último parcial, **entonces** el distributivo pasa a EN_CONSOLIDACION.
3. **Dado** alguna carrera sin enviar, **cuando** otra envía el suyo, **entonces** el distributivo sigue en BORRADOR_CARRERA.
4. **Dado** el paso automático a la siguiente etapa, **cuando** ocurre, **entonces** deja su propia transición en la bitácora, con quién envió el último parcial.

---

### User Story 3 - Solo el rol de la etapa la mueve (Priority: P1)

Como institución, quiero que solo el rol responsable de la etapa actual pueda avanzar el distributivo, para que cada etapa la mueva quien corresponde (RF-34).

**Why this priority**: es el tercer escenario del issue y la regla que reutilizan las historias siguientes (F10-H03, F13-H01, F14, F15, F18).

**Independent Test**: con un coordinador de otra carrera o un usuario sin el rol, intentar enviar un parcial y recibir 403; con BORRADOR_UNIDADES activa, avanzarla con el Gestor de unidad y recibir 403 con cualquier otro rol.

**Acceptance Scenarios**:

1. **Dado** un usuario que no tiene el rol de la etapa actual, **cuando** intenta avanzar el distributivo, **entonces** recibe un error 403. *(issue)*
2. **Dado** un usuario con el rol de la etapa actual, **cuando** la avanza, **entonces** el distributivo pasa a la siguiente etapa activa y la transición queda en la bitácora.
3. **Dado** el rol de una etapa cambiado en la configuración del periodo, **cuando** se evalúa quién puede avanzarla, **entonces** se usa el rol de la configuración con la que empezó el distributivo, no uno cargado después.
4. **Dado** un usuario que no puede avanzar la etapa actual, **cuando** consulta el estado del distributivo, **entonces** no ve la acción de avanzar.
5. **Dado** el distributivo en BORRADOR_UNIDADES (activa en la configuración), **cuando** el Gestor de unidad lo avanza, **entonces** pasa a la siguiente etapa activa; **cuando** lo intenta otro rol, recibe 403.
6. **Dado** el distributivo en una etapa con historia propia (por ejemplo, EN_CONSOLIDACION), **cuando** alguien intenta el avance genérico, **entonces** se rechaza: esa etapa se avanza con su propia operación y sus condiciones.

---

### Edge Cases

- **Dos coordinadores envían a la vez los dos últimos parciales:** el distributivo pasa una sola vez a la siguiente etapa, con una sola transición automática.
- **Un parcial vacío o con asignaturas sin docente:** se puede enviar; las asignaturas sin docente son inconsistencias del panel, que se resuelven al consolidar (F10-H03).
- **Una carrera sin propuestas todavía:** su parcial existe desde que se creó el distributivo; cuenta como no enviada y su coordinador puede enviarlo vacío.
- **Un periodo cerrado:** no se envía ni se avanza nada.
- **La configuración del periodo cambia después de crear el distributivo:** no afecta al distributivo en curso (criterio de RF-34).
- **El usuario pierde el rol durante la sesión:** pierde el permiso en la próxima renovación del token (D-62); el servidor valida el rol en cada envío.
- **Un coordinador sin carrera indicada:** se rechaza con el error del catálogo que pide indicarla (`COMUN_CARRERA_SIN_INDICAR`).

## Requirements *(mandatory)*

### Functional Requirements

**El proceso de aprobación**

- **FR-001**: Cada distributivo DEBE tener un estado actual del flujo: una de las etapas activas de la configuración de su periodo.
- **FR-002**: Al crearse, el distributivo DEBE quedar en BORRADOR_CARRERA, con el parcial de cada carrera como no enviado.
- **FR-003**: El distributivo DEBE usar la configuración del flujo vigente cuando se creó; una configuración cargada después no lo afecta (RF-34).

**Enviar el parcial**

- **FR-004**: El Coordinador de carrera DEBE poder enviar el parcial de su carrera mientras el distributivo está en BORRADOR_CARRERA.
- **FR-005**: Un parcial enviado NO DEBE admitir propuestas, asignaciones ni quitas, hasta que se devuelva (la devolución es de otra historia).
- **FR-006**: El coordinador solo DEBE poder enviar el parcial de su propia carrera (D-64).
- **FR-007**: Un parcial ya enviado NO DEBE poder enviarse de nuevo.

**Pasar de etapa**

- **FR-008**: Cuando todas las carreras con parcial en la versión vigente lo enviaron, el distributivo DEBE pasar a la siguiente etapa activa de su configuración, en la misma operación que el último envío.
- **FR-009**: Solo un usuario con el rol responsable de la etapa actual, según la configuración del distributivo, DEBE poder avanzarla. Cualquier otro recibe `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403), sin saber qué rol falta.
- **FR-010**: El avance genérico DEBE aplicarse solo a las etapas opcionales sin historia propia: hoy, BORRADOR_UNIDADES. En las demás etapas DEBE rechazarse: EN_CONSOLIDACION (F10-H03), EN_REVISION_VRA (F13-H01), CONGELADO_EN_FIRMA (F14-H03), EN_OCS (F15-H01) y APROBADO (F18-H01) las avanza su historia, con la regla de rol de FR-009.

**Auditoría**

- **FR-011**: Cada transición, el envío de un parcial y el paso de etapa, DEBE quedar en la bitácora con quién, cuándo, desde qué estado, hacia cuál, la versión del distributivo y el hash (RF-44, D-90). El hash es el SHA-256 de los datos que se aprueban en esa transición, en una forma fija: al enviar un parcial, sus propuestas y actividades; al pasar de etapa, las de todos los parciales enviados. La misma forma da el mismo hash para el mismo contenido.
- **FR-012**: El distributivo DEBE conservar el historial de sus transiciones, en orden.
- **FR-013**: Un intento rechazado (por rol, por carrera, por estado o por periodo cerrado) NO DEBE dejar transición ni entrada.

**Consulta y errores**

- **FR-014**: Quien lee el distributivo, la oferta o los proyectos DEBE poder ver su etapa actual, el estado de envío de cada parcial y la acción que le corresponde. Un parcial marcado en Generación sin su transición registrada se muestra como pendiente de confirmar, con la acción de reenviar (research §2).
- **FR-015**: Cada error nuevo DEBE estar en el catálogo, con sus 8 columnas (principio XIII).

### Key Entities

- **Proceso de aprobación** (`ProcesoAprobacion`, modelo de dominio §5): uno por distributivo. Su estado actual, el estado de envío de cada carrera en BORRADOR_CARRERA, la configuración con la que empezó y el historial de transiciones.
- **Transición** (`Transicion`): desde qué estado, hacia cuál, quién, cuándo, la versión y el hash. También va a la bitácora.
- **Etapa** (`Etapa`, de F12-H01): un estado del flujo con su rol, su plazo y si admite devolución.
- **Parcial de carrera** (`ParcialCarrera`, de Generación): la parte del distributivo que arma una carrera, con su estado (en elaboración, enviado o devuelto).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las transiciones (envíos de parcial y pasos de etapa) quedan en la bitácora exactamente una vez, con los seis datos de RF-44.
- **SC-002**: El 100 % de los intentos de enviar o avanzar con un rol que no es el de la etapa actual reciben 403: los 9 roles de la matriz D-16 que no son el de la etapa, en cada operación.
- **SC-003**: Con todas las carreras enviadas, el distributivo está en la siguiente etapa activa en la misma respuesta del último envío, sin un paso manual.
- **SC-004**: Con dos envíos simultáneos de los dos últimos parciales, el distributivo pasa de etapa exactamente una vez.
- **SC-005**: Un coordinador envía el parcial de su carrera en menos de 30 segundos desde que abre la pantalla del distributivo.
- **SC-006**: Calcular dos veces el hash del mismo contenido da el mismo resultado, y cambiar un solo dato del parcial lo cambia.

## Assumptions

- **Qué carreras cuentan:** las que tienen parcial en la versión vigente. Generación crea uno por cada carrera con oferta al crear el distributivo, y otro si una carrera recibe su primera propuesta después (research §6).
- **Devolver:** devolver un parcial o una etapa (RF-36) no es de esta historia; esta solo deja el estado DEVUELTO del parcial sin usar.
- **Notificaciones:** avisar del envío o del paso de etapa es F16-H01. Esta historia no notifica.
- **Plazos:** seguir los plazos de cada etapa es F12-H03.
- **El parcial no se firma** en el prototipo: el coordinador lo aprueba en el sistema (ADR-007).
- **La versión:** el número de la versión vigente del distributivo, que ya existe en Generación.
- **El hash no es el del PDF:** el PDF parcial y el del consolidado, con su propio hash, llegan con F13-H02. El hash de esta historia identifica el contenido de datos que se aprobó.
- **BORRADOR_UNIDADES:** hoy está desactivada por defecto; el avance genérico existe para cuando se active. Las unidades no tienen parcial propio en el prototipo, así que esa etapa no tiene envíos: su rol la avanza entera.
- **Contenido incompleto:** se puede enviar un parcial con asignaturas sin docente; el panel de inconsistencias las muestra y F10-H03 exige cero abiertas antes de enviar a revisión.

## Out of Scope

- Devolver un parcial o el distributivo a una etapa anterior (RF-36).
- Las condiciones propias de cada etapa posterior: enviar a revisión con cero inconsistencias (F10-H03), aprobar y congelar (F13-H01), completar la cadena de firmas (F14-H03), registrar la resolución (F15-H01) y publicar (F18-H01).
- Generar el PDF del parcial o del consolidado (F13-H02).
- Notificaciones (F16-H01) y plazos (F12-H03).
- Remitir a la instancia zonal (REMITIDO_ZONAL, desactivada en el prototipo).
