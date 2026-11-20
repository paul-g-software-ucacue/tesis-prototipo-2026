# Feature Specification: Seguir los plazos de cada etapa

**Feature Branch**: `057-plazos-etapas`

**Created**: 2026-11-20

**Status**: Draft

**Input**: User description: "F12-H03" (se llamó sin descripción; el autor eligió la historia #58 entre las dos abiertas)

**Origen**: issue #58 · historia (`tipo:historia`, `prioridad:must`, `RF-34`, `RN-23`) · feature F12 (Flujo de etapas configurable con plazos, #11) · milestone «Ola 3 · Aprobado y firmado» · servicio 06-validacion-ocs · tamaño M. Depende de F12-H02 (#57) y F16-H01 (#67), las dos terminadas. Es la última historia abierta de la ola 3.

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero ver el plazo de cada etapa y recibir una notificación cuando vence, para publicar a tiempo.

**Requerimientos que cubre**:
- **RF-34:** cada etapa del flujo configurable tiene su rol responsable y su plazo.
- **RN-23:** «La distribución se realiza antes del inicio del periodo e incluye las actividades que comienzan en él. Se remite a SENESCYT al menos 30 días antes, con la aprobación previa del OCS» (POL 5.2).
- **Modelo de dominio:** `Plazo`, «fecha límite de una etapa. Al vencer genera una notificación»; el evento `PlazoDeEtapaVencido`, de Aprobación hacia Notificaciones, «vence el plazo de la etapa actual»; RN-23 queda fuera del motor de reglas y vive en Aprobación.
- **Modelo de dominio, pregunta 5 (§9.2):** «Plazo vencido (RN-23). Propuesta: solo genera una notificación; no bloquea la etapa.» El issue pide confirmarlo al especificar.
- **Glosario:** «Plazo: fecha límite de una etapa» (`Plazo`, Aprobación); «Etapa: estado del flujo con su rol, su plazo y si admite devolución» (`Etapa`).

**Decisiones que aplica**:
- D-89 (punto 4): el plazo son días corridos desde que el distributivo llega a la etapa.
- ADR-007: el esqueleto fijo y las etapas opcionales; la configuración del periodo solo cambia en Planificación.
- D-95: las transiciones las registra Aprobación; son las que dicen cuándo llegó el distributivo a cada etapa.
- D-108: Aprobación decide a quién y arma el mensaje; Notificaciones resuelve los usuarios; nunca se pierde una notificación ni llega dos veces (MD-03).
- D-16 y D-63: los permisos de la matriz y los diez roles.

**Principios de la constitución**: II, III, IV, V, VI, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- La configuración del flujo de cada periodo (F12-H01) ya fija, por cada etapa activa salvo PUBLICADO, un plazo en días corridos desde que el distributivo llega a ella. Por defecto: 15 días en BORRADOR_CARRERA, 5 en EN_CONSOLIDACION, 5 en EN_REVISION_VRA, 5 en CONGELADO_EN_FIRMA, 10 en EN_OCS y 3 en APROBADO.
- El proceso de aprobación guarda su copia de la configuración al arrancar (F12-H02), y cada transición registra cuándo llegó el distributivo a su etapa.
- Notificaciones (F16-H01) ya entrega en la campana los avisos de cada transición, por rol o por persona.
- Nadie usa todavía el plazo: ni se muestra, ni se revisa, ni avisa.

Esta historia agrega:
- **El plazo a la vista:** la fecha límite de la etapa actual, cuánto falta o cuánto lleva vencido, y el plazo de cada etapa activa, en la tarjeta del estado del flujo.
- **La revisión diaria de plazos:** cuando vence el plazo de la etapa actual, avisa al rol de la etapa y al Vicerrector, sin bloquear nada.
- **La advertencia de RN-23** al cargar una configuración cuyo último plazo queda a menos de 30 días del inicio del periodo.

## Clarifications

### Session 2026-11-20

- Q: Con el distributivo todavía en la misma etapa, ¿cada cuánto se repite el aviso del plazo vencido? → A: Una sola vez por llegada a la etapa. Si el distributivo vuelve a ella por una devolución, una versión nueva o una reforma, el plazo empieza de nuevo y puede avisar otra vez. La tarjeta sigue diciendo «Vencido hace N días».
- Q: Para la advertencia de RN-23, ¿desde qué fecha se cuentan los plazos para estimar el último? → A: Desde el día de la carga: la fecha de la carga más la suma de los plazos de las etapas activas hasta APROBADO, como si el flujo arrancara ese día. Con los plazos por defecto son 43 días.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recibir el aviso cuando vence el plazo de la etapa (Priority: P1)

Como Vicerrector académico, quiero recibir una notificación cuando vence el plazo de la etapa en la que está el distributivo, para intervenir antes de que se atrase la publicación.

**Why this priority**: es el primer escenario del issue y la razón de la historia: sin el aviso, un atraso solo se descubre preguntando.

**Independent Test**:
1. Con el distributivo en EN_CONSOLIDACION y su plazo de 5 días, se simula que pasan 6 días.
2. Corre la revisión diaria de plazos.
3. `vicerrectorado.demo` y quien consolida ven en su campana el aviso del plazo vencido, con la etapa, la fecha límite y los días de atraso; el distributivo sigue en EN_CONSOLIDACION y se puede enviar a revisión como siempre.

**Acceptance Scenarios**:

1. **Dado** una etapa cuyo plazo venció, **cuando** corre la revisión diaria de plazos, **entonces** el rol de la etapa y el Vicerrector reciben una notificación **y** la etapa no se bloquea. *(issue)*
2. **Dado** el aviso de un plazo vencido, **cuando** el destinatario lo abre, **entonces** dice el periodo, la etapa, la fecha límite y cuántos días lleva vencida, y lleva a la pantalla donde se ve el distributivo.
3. **Dado** un plazo vencido ya avisado, **cuando** la revisión vuelve a correr al día siguiente con el distributivo en la misma etapa, **entonces** no se avisa otra vez: el aviso es uno por llegada a la etapa, y la tarjeta sigue diciendo «Vencido hace N días». *(aclaración del 2026-11-20)*
4. **Dado** la etapa vencida, **cuando** su rol avanza o devuelve el distributivo, **entonces** la transición se acepta como siempre; el atraso no la impide ni la condiciona.
5. **Dado** el distributivo que vuelve a una etapa por una devolución o una reforma, **cuando** llega otra vez, **entonces** el plazo de esa etapa empieza de nuevo desde esa llegada, y puede volver a avisar si vence.
6. **Dado** una etapa cuyo plazo todavía no vence, o que vence hoy, **cuando** corre la revisión, **entonces** no se avisa nada.
7. **Dado** un usuario que es del rol de la etapa y además es Vicerrector, **cuando** vence el plazo, **entonces** recibe una sola notificación.
8. **Dado** el proceso de identidad detenido cuando corre la revisión, **cuando** vuelve, **entonces** el aviso llega una sola vez.
9. **Dado** el sistema detenido a la hora de la revisión, **cuando** vuelve a funcionar, **entonces** la revisión corre al arrancar y avisa los plazos que vencieron mientras estaba detenido, con sus días de atraso reales.

---

### User Story 2 - Ver el plazo de cada etapa (Priority: P1)

Como Vicerrector académico, o cualquier usuario que sigue el distributivo, quiero ver la fecha límite de la etapa actual y el plazo de cada etapa, para saber si el periodo va a tiempo.

**Why this priority**: es la primera mitad de la historia («quiero ver el plazo de cada etapa»); sin ella, el aviso llega sin contexto y nadie ve venir el vencimiento.

**Independent Test**:
1. Con el distributivo en EN_REVISION_VRA, cualquier usuario que lee el estado del flujo abre «Consolidado del periodo».
2. La tarjeta del estado del flujo muestra la fecha límite de EN_REVISION_VRA y cuántos días faltan, y la lista de etapas activas con su plazo en días.
3. Se simula que pasa la fecha límite: la tarjeta dice «Vencido hace N días».

**Acceptance Scenarios**:

1. **Dado** el distributivo en una etapa con plazo, **cuando** abro la tarjeta del estado del flujo, **entonces** veo cuándo llegó a la etapa, su fecha límite y cuántos días faltan.
2. **Dado** la etapa actual con la fecha límite hoy, **cuando** abro la tarjeta, **entonces** dice que vence hoy.
3. **Dado** la etapa actual vencida, **cuando** abro la tarjeta, **entonces** dice «Vencido hace N días», con un aviso que no depende solo del color.
4. **Dado** el distributivo de un periodo, **cuando** abro la tarjeta, **entonces** veo las etapas activas de su configuración, en orden, cada una con su plazo en días, y cuál es la actual.
5. **Dado** el distributivo en PUBLICADO, **cuando** abro la tarjeta, **entonces** no se muestra fecha límite, porque PUBLICADO no tiene plazo.
6. **Dado** la configuración del periodo cambiada después de que arrancó el flujo, **cuando** abro la tarjeta, **entonces** los plazos son los de la copia del proceso, no los de la configuración nueva.

---

### User Story 3 - La advertencia de RN-23 al cargar la configuración (Priority: P2)

Como administrador del sistema, quiero que al cargar la configuración del flujo el sistema me advierta si los plazos no dejan los 30 días que pide RN-23 antes del inicio del periodo, para ajustarlos antes de que arranque el flujo.

**Why this priority**: es el segundo escenario del issue. Previene el atraso en vez de avisarlo, pero no impide el trabajo: solo advierte.

**Independent Test**:
1. Con un periodo en Planificación que empieza dentro de 60 días, el administrador carga una configuración cuyos plazos suman 43 días.
2. La carga queda vigente y el sistema muestra la advertencia de RN-23, con la fecha estimada del último plazo, la fecha límite de RN-23 (30 días antes del inicio) y cuántos días faltan.
3. El administrador baja los plazos a 25 días, vuelve a cargar y ya no hay advertencia.

**Acceptance Scenarios**:

1. **Dado** una configuración cuyo último plazo queda a menos de 30 días del inicio del periodo, **cuando** la cargo, **entonces** el sistema muestra una advertencia por RN-23. *(issue)*
2. **Dado** el último plazo estimado como la fecha de la carga más la suma de los plazos de las etapas activas hasta APROBADO, **cuando** queda a 30 días o más del inicio del periodo, **entonces** no hay advertencia. *(aclaración del 2026-11-20)*
3. **Dado** la advertencia, **cuando** se muestra, **entonces** dice la fecha estimada del último plazo, la fecha que pide RN-23 (30 días antes del inicio del periodo), cuántos días faltan y qué hacer: acortar plazos o desactivar etapas opcionales.
4. **Dado** una configuración con la advertencia, **cuando** la cargo, **entonces** queda vigente igual: la advertencia no rechaza la carga.
5. **Dado** una configuración vigente cargada con la advertencia, **cuando** el administrador la consulta, **entonces** ve la advertencia con la que se cargó.
6. **Dado** un periodo cuyo inicio ya pasó o queda a menos de 30 días, **cuando** cargo cualquier configuración, **entonces** también recibo la advertencia.
7. **Dado** un archivo inválido, **cuando** lo cargo, **entonces** se rechaza con sus errores, como hoy; la advertencia solo se calcula sobre una configuración válida.

---

### Edge Cases

- **BORRADOR_CARRERA:** su rol es el coordinador de carrera; el aviso va solo a los coordinadores de las carreras que todavía no enviaron su parcial, no a los que ya enviaron.
- **CONGELADO_EN_FIRMA:** el aviso va al rol que la configuración pone en la etapa (por defecto, el Responsable de consolidación) y al Vicerrector; el firmante pendiente no recibe un aviso aparte (las firmas pendientes ya se notifican con F16-H02).
- **EN_REVISION_VRA vencida:** el rol de la etapa es el Vicerrector; recibe una sola notificación.
- **Sin usuarios activos con el rol de la etapa:** el aviso llega igual al Vicerrector.
- **Una etapa desactivada:** no tiene plazo ni se revisa; el distributivo no pasa por ella.
- **El plazo se cuenta en días corridos**, sin descontar fines de semana ni feriados (D-89), y la fecha límite es el día de llegada más el plazo. La etapa vence cuando termina ese día, en hora de Ecuador.
- **Varias revisiones el mismo día** (por un reinicio): el aviso no se duplica.
- **Una etapa que vence y se deja mientras el sistema está detenido:** al volver, se revisa solo la etapa actual; la que ya se dejó no se avisa.
- **Sin distributivo en el flujo**, o con el periodo cerrado: la revisión no avisa nada.
- **La configuración por defecto, sin archivo cargado:** no se carga, así que no muestra la advertencia de RN-23.

## Requirements *(mandatory)*

### Functional Requirements

**El plazo de la etapa**

- **FR-001**: La fecha límite de la etapa actual DEBE ser la fecha en que el distributivo llegó a ella más su plazo en días corridos, según la copia de la configuración del proceso.
- **FR-002**: Cada llegada a una etapa, también por una devolución, una versión nueva o una reforma, DEBE empezar su plazo de nuevo.
- **FR-003**: El plazo vencido NO DEBE bloquear la etapa ni condicionar ninguna transición: solo avisa.

**La revisión diaria y el aviso**

- **FR-004**: El sistema DEBE revisar una vez al día si venció el plazo de la etapa actual de cada distributivo en el flujo, y además al arrancar, para no perder un día en que estuvo detenido.
- **FR-005**: Cuando vence el plazo, el sistema DEBE notificar a los usuarios activos con el rol de la etapa, según la copia de la configuración del proceso (en BORRADOR_CARRERA, solo los coordinadores de las carreras que no enviaron su parcial), y a los usuarios activos con el rol de la etapa EN_REVISION_VRA de esa configuración (por defecto, VICERRECTOR), con una sola notificación por usuario (D-114 deja un caso conocido).
- **FR-006**: El aviso DEBE llegar una sola vez por llegada a la etapa, aunque la revisión corra varias veces o Notificaciones no responda en ese momento.
- **FR-007**: El aviso DEBE decir el periodo, la etapa, la fecha límite y los días de atraso, y llevar a la pantalla donde cada destinatario ve el distributivo.
- **FR-008**: Cada vencimiento detectado DEBE quedar registrado, con el periodo, la etapa, la llegada, la fecha límite y cuándo se detectó, y dejar una entrada en la bitácora como acción del sistema.

**Ver los plazos**

- **FR-009**: La tarjeta del estado del flujo, en las pantallas que la muestran («Consolidado del periodo», «Oferta y propuesta de docentes» y «Resolución del OCS»), DEBE mostrar a quien la lee la llegada a la etapa actual, su fecha límite y cuántos días faltan, o que vence hoy, o cuántos días lleva vencida.
- **FR-010**: La tarjeta DEBE mostrar las etapas activas de la copia de la configuración del proceso, en orden, cada una con su plazo en días, y marcar la actual.
- **FR-011**: El estado vencido DEBE distinguirse con texto, no solo con color, y la tarjeta DEBE cumplir WCAG 2.1 AA.

**La advertencia de RN-23**

- **FR-012**: Al cargar una configuración válida, el sistema DEBE estimar la fecha del último plazo como la fecha de la carga más la suma de los plazos de las etapas activas hasta APROBADO, y advertir si queda a menos de 30 días del inicio del periodo.
- **FR-013**: La advertencia NO DEBE rechazar la carga; la configuración queda vigente igual.
- **FR-014**: La advertencia DEBE decir la fecha estimada del último plazo, la fecha que pide RN-23, cuántos días faltan y qué se puede hacer, y DEBE conservarse con la versión cargada para verla al consultarla.
- **FR-015**: Los 30 días de RN-23 DEBEN ser un parámetro versionado por periodo, no una cifra fija (principio VI).

**Errores**

- **FR-016**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII). Las advertencias no son errores: llevan un código estable del contrato y un texto que dice qué hacer, como las advertencias de las cargas (plan, research §7).

### Key Entities

- **Plazo** (`Plazo`): la fecha límite de la etapa actual, calculada con la llegada y los días de la etapa.
- **Etapa** (`Etapa`, de F12-H01, existente): su plazo en días; PUBLICADO no tiene.
- **Plazo de etapa vencido** (`PlazoDeEtapaVencido`): el hecho de que venció la etapa actual de un distributivo, con el periodo, la etapa, la llegada, la fecha límite y cuándo se detectó. Hay uno por llegada a la etapa, y es lo que impide repetir el aviso.
- **Advertencia de RN-23**: la fecha estimada del último plazo, la fecha de RN-23 y los días que faltan, guardada con la versión de la configuración cargada.
- **Configuración del flujo** (`ConfiguracionFlujo`, existente) y **proceso de aprobación** (`ProcesoAprobacion`, existente), con su copia de la configuración.
- **Notificación** (existente, F16-H01).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con el paso de los días simulado, cada etapa con plazo que vence produce su aviso al rol de la etapa y al Vicerrector, y ninguna etapa que vence hoy o más tarde lo produce. Se comprueba en un script de punta a punta.
- **SC-002**: Después de un vencimiento, cada destinatario tiene exactamente una notificación de ese vencimiento, aunque la revisión corra tres veces el mismo día o varios días seguidos con el distributivo en la misma etapa.
- **SC-003**: Con la etapa vencida, el rol de la etapa avanza o devuelve el distributivo en el 100 % de los intentos válidos, como sin vencimiento.
- **SC-004**: Con el proceso de identidad detenido al correr la revisión, el aviso llega una sola vez antes de 2 minutos desde que el proceso vuelve.
- **SC-005**: Cualquier usuario que sigue el distributivo sabe la fecha límite y si la etapa está vencida mirando la tarjeta, en menos de 10 segundos y sin abrir la bitácora.
- **SC-006**: Una configuración cuyo último plazo queda a menos de 30 días del inicio del periodo muestra la advertencia en el 100 % de las cargas, y una con 30 días o más no la muestra nunca.
- **SC-007**: La tarjeta y la advertencia no tienen fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **El plazo vencido solo avisa:** confirma la propuesta de la pregunta 5 del modelo de dominio y el escenario del issue. No bloquea la etapa ni cambia ningún estado.
- **A quién:** el rol de la etapa, según la copia de la configuración del proceso (las mismas reglas de D-108: RESPONSABLE_CONSOLIDACION son quienes tienen el rol y el designado del periodo), y el Vicerrector, como dice el issue: el rol de EN_REVISION_VRA en la configuración, que por defecto es VICERRECTOR (principio VI, como D-113). No hay autor que excluir, porque el aviso lo genera el sistema.
- **Cuándo:** la revisión corre una vez al día, a una hora fija de la mañana en hora de Ecuador, configurable, y además al arrancar.
- **Dónde se ve:** en la tarjeta del estado del flujo que ya existe; no hay una pantalla nueva de plazos.
- **Quién ve la tarjeta:** los mismos que hoy leen el estado del flujo; los permisos de la matriz D-16 no cambian.
- **El último plazo de RN-23:** el de APROBADO, la última etapa con plazo. RN-23 pide la aprobación del OCS 30 días antes del inicio; la publicación llega al terminar APROBADO. Se cuenta desde el día de la carga, como si el flujo arrancara ese día (aclaración del 2026-11-20); una devolución o una reforma pueden alargarlo, y eso no se estima.
- **La demo:** el sistema no tiene una fecha simulada. Para mostrar un vencimiento sin esperar días, la prueba de punta a punta corre hacia atrás las transiciones de sus datos de prueba y reinicia el proceso, que revisa los plazos al arrancar (plan, research §9).
- **Fuera de alcance:**
  - avisos antes de que venza el plazo (recordatorios de «vence mañana»);
  - bloquear o escalar una etapa vencida;
  - plazos por carrera o por persona;
  - el correo (V-09);
  - la vista del estado del periodo del Vicerrector (F02-H03), que podrá usar estos plazos;
  - la remisión a SENESCYT.
