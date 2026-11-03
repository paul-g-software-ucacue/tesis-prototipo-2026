# Feature Specification: Cargar la configuración del flujo y del árbol de firmas

**Feature Branch**: `038-configuracion-flujo`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Especifica la historia F12-H01 · Cargar la configuración del flujo y del árbol de firmas (issue #56), a partir de RF-34, RF-58, ADR-004, ADR-007, D-14 y D-19, con las aclaraciones del autor.

**Origen**: issue #56 · F12-H01 · sub-issue de F12 (Flujo de etapas configurable con plazos, #11) · milestone «Ola 3 · Aprobado y firmado» · Ola del secuenciador MVP · tamaño L · depende de #45 (F05-H03), cerrada.

**Requerimientos que cubre**:
- **RF-34:** el flujo de estados es configurable: un esqueleto fijo y etapas opcionales, con su nombre, rol responsable, plazo y si admiten devolución.
- **RF-58:** el árbol de firmas: qué etapas exigen firma, qué rol firma, en qué orden y qué documento. Las etapas sin firma exigen una aprobación en el sistema.
- **ADR-007:** la configuración va en un archivo versionado por periodo, sin pantalla de configuración, y solo cambia en Planificación.
- **D-14 y ADR-004:** el árbol definitivo del prototipo: F1, F2 y F3 sobre el consolidado.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-13, D-14, D-19, D-63, D-65, D-72, D-74, ADR-004 y ADR-007. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), V (límites), VI (las reglas son datos), VII (trazabilidad), VIII (pruebas primero en el dominio), IX (seguridad y datos personales), XIII (catálogo de errores) y XIV (validación de entradas).

**Historia (tal como está en el issue)**: Como administrador del sistema, quiero cargar el archivo de configuración del flujo y del árbol de firmas del periodo, para activar las etapas opcionales con sus roles y plazos sin tocar el código.

**Punto de partida.** El distributivo se puede armar, validar y consolidar, pero nada dice por qué etapas pasa ni quién firma. Es la primera historia del servicio de Aprobación, que hoy está vacío.

Esta historia agrega:
- **La configuración del flujo:** con sus etapas, roles, plazos y firmas, por periodo.
- **El archivo:** una plantilla para descargar, que se edita y se sube.
- **La validación al cargar:** el esqueleto, las firmas y los firmantes.

El distributivo todavía no avanza por las etapas: eso es F12-H02.

## Clarifications

### Session 2026-11-02

- Q: ¿En qué formato va el archivo? → A: YAML, con una plantilla descargable de la sección 9.1, comentada en español. Se lee en modo seguro.
- Q: ¿Qué pasa con un periodo sin archivo cargado? → A: Rige la configuración por defecto de 9.1, y la pantalla lo dice. Nada se bloquea.
- Q: ¿Cómo se comprueba un rol firmante sin usuario? → A: Cada rol firmante necesita al menos un usuario, y el Responsable de consolidación debe estar designado en ese periodo. Si falta, se rechaza diciendo cuál.
- Q: ¿Cómo se expresa el plazo? → A: En días corridos desde que el distributivo llega a la etapa.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cargar una configuración válida (Priority: P1)

Como administrador, descargo la plantilla, la ajusto y la subo, para que el periodo tenga sus etapas, roles, plazos y firmas.

**Why this priority**: es la historia, y F12-H02 la necesita.

**Independent Test**: subir la plantilla sin cambios en un periodo en Planificación y verla vigente.

**Acceptance Scenarios**:

1. **Dado** el archivo por defecto de la sección 9.1, **cuando** lo cargo para un periodo en Planificación, **entonces** queda vigente con sus etapas, roles, plazos y las firmas F1, F2 y F3.
2. **Dada** la plantilla descargada, **cuando** la abro, **entonces** veo cada etapa y cada firma, con comentarios en español que explican qué se puede cambiar.
3. **Dado** un archivo con una etapa opcional activada o desactivada, **cuando** lo cargo, **entonces** queda vigente con ese cambio.
4. **Dado** un periodo con una configuración cargada, **cuando** cargo otra, **entonces** la nueva es la vigente y la anterior se conserva.
5. **Dada** una configuración cargada, **cuando** la consulto, **entonces** veo quién la cargó y cuándo.

---

### User Story 2 - Saber qué rige (Priority: P1)

Como administrador, veo la configuración que rige en un periodo, aunque nadie haya cargado una, para no adivinar.

**Why this priority**: sin archivo rige la de 9.1, y hay que poder verlo.

**Independent Test**: abrir un periodo sin archivo y ver que rige la configuración por defecto, con sus etapas y firmas.

**Acceptance Scenarios**:

1. **Dado** un periodo sin archivo, **cuando** abro la pantalla, **entonces** veo que rige la configuración por defecto, con sus etapas y firmas.
2. **Dado** un periodo con archivo, **cuando** abro la pantalla, **entonces** veo la que cargué, sin poder editarla ahí.
3. **Dado** un periodo que ya no está en Planificación, **cuando** abro la pantalla, **entonces** la veo, sin la opción de cargar otra.

---

### User Story 3 - Saber qué corregir (Priority: P1)

Como administrador, si el archivo tiene problemas quiero verlos todos, con qué corregir, para no subirlo una vez por cada error.

**Why this priority**: el issue lo pide, y un archivo mal cargado rompería el flujo más adelante.

**Independent Test**: subir un archivo con varios problemas y verlos todos, sin que cambie la vigente.

**Acceptance Scenarios**:

1. **Dado** un archivo sin la etapa EN_OCS, **cuando** lo cargo, **entonces** se rechaza indicando la etapa que falta.
2. **Dado** un archivo que desactiva una etapa del esqueleto, **cuando** lo cargo, **entonces** se rechaza indicando cuál.
3. **Dado** un archivo con una firma en una etapa desactivada, **cuando** lo cargo, **entonces** se rechaza indicando la firma y la etapa.
4. **Dado** un rol firmante sin usuario, **cuando** cargo el archivo, **entonces** se rechaza indicando el rol.
5. **Dado** un periodo sin Responsable de consolidación designado, **cuando** cargo un archivo con F1, **entonces** se rechaza y dice que falta designarlo.
6. **Dado** un archivo con varios problemas, **cuando** lo cargo, **entonces** veo todos.
7. **Dado** un archivo rechazado, **cuando** consulto la configuración, **entonces** sigue vigente la anterior.
8. **Dado** un archivo que no se puede leer, **cuando** lo cargo, **entonces** se rechaza con un mensaje claro, sin detalles técnicos.

---

### User Story 4 - Un distributivo en curso no cambia (Priority: P2)

Como institución, quiero que la configuración solo cambie en Planificación, para que un distributivo en curso no cambie de reglas.

**Why this priority**: el issue lo pide, pero depende de cargar.

**Independent Test**: intentar cargar en un periodo vigente y ver el rechazo, sin que cambie lo que rige.

**Acceptance Scenarios**:

1. **Dado** un periodo que ya no está en Planificación, **cuando** intento cargar una configuración nueva, **entonces** se rechaza y el distributivo en curso sigue con su configuración.
2. **Dado** un usuario sin el permiso del flujo, **cuando** intenta cargar o consultar, **entonces** no tiene acceso.

---

### Edge Cases

- **Archivo vacío o de más de 1 MB:** se rechaza, diciendo qué corregir.
- **YAML con anclas, alias o tipos especiales:** se rechaza sin interpretarlos.
- **Claves que no existen o repetidas:** se rechazan, para no ignorar un error de escritura en silencio.
- **Una etapa o un rol que no existe:** se rechaza, diciendo cuál.
- **Una etapa repetida:** se rechaza.
- **Plazo de 0 días, negativo o con decimales:** se rechaza.
- **El orden de las firmas repetido o con huecos:** se rechaza.
- **Una firma sobre un documento desconocido:** se rechaza.
- **Sin firmas:** se rechaza, porque el prototipo exige F1, F2 y F3 (D-14).
- **Una etapa activa sin rol o sin plazo:** se rechaza. PUBLICADO no necesita plazo.
- **Un periodo que no existe:** responde 404.
- **Roles o Académico no responden:** el error del catálogo, sin cargar nada.
- **Dos cargas a la vez:** las dos quedan, y vale la última.

## Requirements *(mandatory)*

### Functional Requirements

**La configuración**

- **FR-001**: Cada periodo DEBE tener una configuración que rige: la última cargada o, si no hay, la de la sección 9.1.
- **FR-002**: La configuración DEBE decir por cada etapa si está activa, su nombre, su rol responsable, su plazo y si admite devolución.
- **FR-003**: La configuración DEBE decir por cada firma su orden, su etiqueta, su rol, su etapa y su documento.
- **FR-004**: Las etapas del esqueleto (BORRADOR_CARRERA, CONGELADO_EN_FIRMA, EN_OCS, APROBADO y PUBLICADO) NO DEBEN poder quitarse ni desactivarse.
- **FR-005**: BORRADOR_UNIDADES, EN_CONSOLIDACION, EN_REVISION_VRA y REMITIDO_ZONAL DEBEN poder activarse o desactivarse.
- **FR-006**: El plazo DEBE ser un número entero de días corridos, mayor que 0, desde que el distributivo llega a la etapa.

**El archivo**

- **FR-007**: El administrador DEBE poder descargar una plantilla con la configuración por defecto, comentada en español.
- **FR-008**: La plantilla, cargada sin cambios y con sus firmantes asignados, DEBE ser válida.
- **FR-009**: El archivo se DEBE leer en modo seguro: sin anclas, alias ni tipos especiales.
- **FR-010**: Un archivo DEBE pesar hasta 1 MB.
- **FR-011**: Una clave desconocida o repetida DEBE rechazar el archivo.

**La validación**

- **FR-012**: Una firma en una etapa desactivada DEBE rechazar el archivo.
- **FR-013**: Cada rol firmante DEBE tener al menos un usuario.
- **FR-014**: Si firma el Responsable de consolidación, DEBE estar designado en ese periodo.
- **FR-015**: Un archivo inválido DEBE responder todos sus problemas, cada uno diciendo qué corregir.
- **FR-016**: Un archivo inválido NO DEBE cambiar lo que rige.

**Versiones y periodo**

- **FR-017**: Cargar DEBE crear una versión nueva, y las anteriores DEBEN conservarse.
- **FR-018**: Cada carga DEBE quedar registrada con quién y cuándo. La fila de la carga es ese registro, hasta que llegue la bitácora (F17).
- **FR-019**: Solo un periodo en Planificación DEBE admitir una carga.
- **FR-020**: La consulta DEBE decir si rige una cargada o la de por defecto.

**Acceso y errores**

- **FR-021**: Solo quien tiene el permiso del flujo DEBE poder cargar. Hoy, el Administrador.
- **FR-022**: Solo quien tiene el permiso del flujo DEBE poder consultar y descargar la plantilla.
- **FR-023**: Cada error nuevo DEBE estar en el catálogo, con un mensaje claro y accionable (principio XIII).
- **FR-024**: Si otro proceso no responde, el sistema DEBE dar el error del catálogo, sin cargar nada.

### Key Entities

- **Configuración del flujo**: etapas y árbol de firmas de un periodo, con su número de versión, quién la cargó y cuándo.
- **Etapa**: estado del distributivo con su nombre, rol responsable, plazo y si admite devolución. Puede estar activa o no.
- **Paso de firma**: orden, etiqueta (Elaborado, Revisado, Aprobado), rol, etapa y documento.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La plantilla sin cambios se carga a la primera, con sus firmantes asignados.
- **SC-002**: Cada uno de los 4 escenarios del issue responde lo que dice, y un archivo con 5 problemas los muestra todos.
- **SC-003**: Consultar la configuración y cargar un archivo tardan menos de 3 segundos cada uno.
- **SC-004**: Un administrador descarga la plantilla, cambia un plazo y la carga en menos de 3 minutos, sin ayuda.
- **SC-005**: Con otro proceso detenido, responde el error del catálogo en menos de 10 segundos.
- **SC-006**: Las reglas de la configuración tienen al menos 80 % de cobertura.

## Assumptions

- **Firmas:** F1 Elaborado por el Responsable de consolidación, F2 Revisado por el Vicerrector y F3 Aprobado por el Rector, sobre el consolidado, en CONGELADO_EN_FIRMA.
- **Etapas activas por defecto:** EN_CONSOLIDACION y EN_REVISION_VRA. BORRADOR_UNIDADES queda desactivada, porque las unidades no tienen parcial (F10-H02), y REMITIDO_ZONAL también, como en el prototipo.
- **Plazos por defecto:** la plantilla trae valores iniciales, porque la ERS no los fija: 15 días en BORRADOR_CARRERA, 5 en EN_CONSOLIDACION, 5 en EN_REVISION_VRA, 5 en CONGELADO_EN_FIRMA, 10 en EN_OCS y 3 en APROBADO. El administrador los ajusta.
- **La de por defecto no se valida contra los usuarios:** rige sin cargarse. Sus firmantes se exigen al cargar un archivo.
- **Roles:** los 10 roles fijos de D-63.
- **Planificación con distributivo:** se puede cargar mientras el periodo esté en Planificación, aunque tenga distributivo, porque todavía no avanza por las etapas.
- **Un cambio no afecta a un distributivo en curso:** cada periodo usa su configuración, y solo Planificación la cambia. Fijarla al arrancar el flujo es de F12-H02.
- **Leer:** solo el Administrador tiene hoy el permiso del flujo. Las demás historias leerán la configuración por dentro.

## Out of Scope

- Avanzar el distributivo por las etapas (F12-H02).
- Los plazos y sus avisos (F12-H03).
- Firmar (F14).
- La votación del OCS.
- Editar la configuración en pantalla.
- Firmas por carrera.
- REMITIDO_ZONAL activo: se puede configurar, pero el flujo no lo usará en el prototipo.
