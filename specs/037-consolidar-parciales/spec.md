# Feature Specification: Consolidar los parciales con el informe sustentado

**Feature Branch**: `037-consolidar-parciales`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Especifica la historia F10-H02 · Consolidar los parciales con el informe sustentado (issue #54), a partir de RF-32, D-16, RNF-POR-01 y MD-04, con las aclaraciones del autor.

**Origen**: issue #54 · F10-H02 · sub-issue de F10 (Consolidar, con el panel de inconsistencias y el informe sustentado, #10) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · tamaño M · depende de #53 (F10-H01), cerrada.

**Requerimientos que cubre**:
- **RF-32:** el Responsable de consolidación consolida los distributivos parciales en el distributivo institucional y adjunta un informe sustentado. El consolidado incluye a todas las carreras y unidades, con su estado.
- **D-16:** la matriz de permisos: «Consolidación e informe sustentado».
- **RNF-POR-01:** el almacenamiento de archivos va separado de la base de datos.
- **MD-04:** el estado de los parciales es de Aprobación: Distributivo no lo decide.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-13, D-72, D-74, D-83, D-85, D-87 y ADR-023. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), VII (trazabilidad), VIII (pruebas primero en el dominio), IX (seguridad y datos personales), XIII (catálogo de errores) y XIV (validación de entradas).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero consolidar los parciales de todas las carreras y unidades y adjuntar el informe sustentado, para tener el distributivo institucional listo para revisión.

**Punto de partida.** Cada carrera arma su parcial, y el panel de inconsistencias (F10-H01) dice lo que falta. Pero no hay una vista institucional ni dónde dejar el informe. Enviar los parciales llega con F12-H02, en la Ola 3, así que hoy todos están en elaboración.

Esta historia agrega:
- **Consolidar:** deja constancia de quién y cuándo, y se puede repetir.
- **El consolidado:** todas las carreras con su estado, avance y horas, y los totales institucionales.
- **El informe sustentado:** un PDF asociado a la versión. Es la primera vez que el sistema guarda un archivo.

## Clarifications

### Session 2026-11-02

- Q: ¿Qué hace consolidar? → A: Marca, sin bloquear. Guarda quién y cuándo, se puede repetir y admite inconsistencias abiertas, que muestra. Las carreras siguen editando. Si algo cambia después, avisa que el consolidado quedó desactualizado y hay que repetirlo.
- Q: ¿Qué muestra el consolidado? → A: Cada carrera con su estado, avance y horas, y aparte los totales institucionales de investigación, vinculación y gestión, sin estado propio. Las unidades no tienen parcial todavía, y queda escrito.
- Q: ¿Cómo es el informe sustentado? → A: Un PDF de hasta 10 MB, guardado separado de la base, con su huella en la base. Las firmas de la Ola 3 reutilizarán ese almacén.
- Q: ¿Cuándo se adjunta, y se puede reemplazar? → A: Primero se consolida y después se adjunta. Reemplazarlo conserva el anterior y deja registro. Exigirlo para enviar a revisión es de F10-H03.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consolidar (Priority: P1)

Como Responsable de consolidación, consolido el periodo y veo todas las carreras con su estado, avance y horas, para saber cómo está el distributivo institucional.

**Why this priority**: es la historia, y sin consolidado no hay a qué adjuntar el informe.

**Independent Test**: con los parciales de las 7 carreras, consolidar y ver todas, con quién y cuándo.

**Acceptance Scenarios**:

1. **Dados** los parciales de las 7 carreras, **cuando** consolido, **entonces** el consolidado incluye a todas, cada una con su estado, su avance y sus horas.
2. **Dado** el consolidado, **cuando** lo veo, **entonces** veo aparte los totales institucionales de investigación, vinculación y gestión, y que las unidades todavía no tienen parcial propio.
3. **Dadas** inconsistencias abiertas, **cuando** consolido, **entonces** se consolida igual y el consolidado dice cuántas hay, con un enlace al panel.
4. **Dado** un periodo consolidado, **cuando** vuelvo a consolidar, **entonces** queda la fecha nueva y el registro conserva las dos.
5. **Dado** un periodo sin consolidar, **cuando** abro la pantalla, **entonces** veo el estado actual y la opción de consolidar.
6. **Dado** que consolidé, **cuando** un coordinador edita su parcial, **entonces** puede hacerlo, porque consolidar no bloquea.

---

### User Story 2 - Saber si sigue al día (Priority: P1)

Como Responsable de consolidación, veo un aviso si algo cambió después de consolidar, para no entregar un consolidado viejo.

**Why this priority**: consolidar no bloquea, así que sin este aviso el consolidado podría mentir.

**Independent Test**: consolidar, cambiar una propuesta y ver el aviso; volver a consolidar y que desaparezca.

**Acceptance Scenarios**:

1. **Dado** un consolidado, **cuando** cambia una propuesta o una actividad, **entonces** veo que quedó desactualizado y que hay que repetirlo.
2. **Dado** un consolidado desactualizado, **cuando** vuelvo a consolidar, **entonces** el aviso desaparece.
3. **Dado** un consolidado sin cambios después, **cuando** lo veo, **entonces** no hay aviso.
4. **Dado** un consolidado, **cuando** adjunto el informe o justifico una inconsistencia, **entonces** no queda desactualizado, porque no cambia una asignación.

---

### User Story 3 - Adjuntar el informe sustentado (Priority: P1)

Como Responsable de consolidación, adjunto el informe en PDF, para que quede asociado a esa versión del distributivo.

**Why this priority**: el issue lo pide, y F10-H03 lo necesitará.

**Independent Test**: adjuntar un PDF, verlo con su nombre, su tamaño, quién y cuándo, y descargarlo idéntico.

**Acceptance Scenarios**:

1. **Dado** el consolidado, **cuando** adjunto el informe, **entonces** queda asociado a esa versión, con su nombre, su tamaño, quién y cuándo.
2. **Dado** un informe adjunto, **cuando** lo descargo, **entonces** recibo exactamente el mismo archivo.
3. **Dado** un periodo sin consolidar, **cuando** intento adjuntar, **entonces** se rechaza y dice que primero hay que consolidar.
4. **Dado** un archivo que no es un PDF, aunque se llame `.pdf`, **cuando** lo adjunto, **entonces** se rechaza y dice qué corregir.
5. **Dado** un archivo de más de 10 MB, **cuando** lo adjunto, **entonces** se rechaza y dice el límite.
6. **Dado** un informe adjunto, **cuando** adjunto otro, **entonces** el nuevo es el vigente, el anterior se conserva y queda registro.

---

### User Story 4 - Quién puede hacerlo (Priority: P2)

Como institución, quiero que solo el Responsable de consolidación consolide y adjunte, y que el Vicerrectorado pueda seguirlo, para respetar la matriz de permisos.

**Why this priority**: el issue lo pide, pero depende de lo anterior.

**Independent Test**: un coordinador recibe 403 al consolidar; el Vicerrectorado lee y descarga, sin consolidar.

**Acceptance Scenarios**:

1. **Dado** un Coordinador de carrera, **cuando** intenta consolidar, **entonces** recibe un error 403.
2. **Dado** un Coordinador de carrera, **cuando** intenta adjuntar o descargar el informe, **entonces** recibe un error 403.
3. **Dado** el Vicerrectorado, **cuando** abre el consolidado, **entonces** lo ve y descarga el informe, sin poder consolidar ni adjuntar.
4. **Dado** el Administrador del sistema, **cuando** abre el consolidado, **entonces** lo lee, como en el panel (D-87).

---

### Edge Cases

- **Periodo sin distributivo:** la pantalla lo avisa, sin error, y no ofrece consolidar.
- **Periodo cerrado:** se lee y se descarga, pero no se consolida ni se adjunta.
- **Carrera sin propuestas:** sale con su estado, 0 de avance y 0 h.
- **Paralelo retirado:** no cuenta para el avance ni las horas.
- **Dos personas consolidan a la vez:** las dos quedan en el registro, y vale la última.
- **Archivo vacío:** se rechaza.
- **Nombre del archivo:** se guarda solo para mostrarlo. Nunca decide dónde se guarda, y no sirve para salir de la carpeta.
- **El archivo falta o no coincide con su huella al descargar:** el error del catálogo, nunca un archivo dañado.
- **Falla el guardado del archivo:** no queda registrado como adjunto.
- **Un proceso no responde:** el error del catálogo, sin presentar el consolidado como vacío.
- **Un cambio en la oferta:** la oferta es de Académico, y no marca el consolidado como desactualizado (ver Assumptions).

## Requirements *(mandatory)*

### Functional Requirements

**Consolidar**

- **FR-001**: El Responsable de consolidación DEBE poder consolidar la versión vigente de un periodo.
- **FR-002**: Consolidar DEBE guardar quién y cuándo, y cada vez DEBE quedar en el registro.
- **FR-003**: Consolidar se DEBE poder repetir, y vale la última vez.
- **FR-004**: Consolidar NO DEBE cambiar el estado de los parciales ni impedir editarlos (MD-04).
- **FR-005**: Consolidar DEBE admitir inconsistencias abiertas.
- **FR-006**: Un periodo sin distributivo o cerrado NO DEBE poder consolidarse.

**El consolidado**

- **FR-007**: El consolidado DEBE mostrar todas las carreras con parcial, con su estado, su avance y sus horas.
- **FR-008**: El consolidado DEBE mostrar los totales institucionales de investigación, vinculación y gestión, y los de clase y otras actividades de docencia.
- **FR-009**: El consolidado DEBE decir que las unidades todavía no tienen parcial propio.
- **FR-010**: El consolidado DEBE decir cuántas inconsistencias abiertas hay, por severidad, con un enlace al panel.
- **FR-011**: El consolidado se DEBE calcular al abrirlo. Solo se guardan quién y cuándo.
- **FR-012**: Si una propuesta o una actividad cambió después de consolidar, el consolidado DEBE decir que quedó desactualizado.
- **FR-013**: Adjuntar el informe o justificar una inconsistencia NO DEBE dejarlo desactualizado.

**El informe sustentado**

- **FR-014**: El Responsable de consolidación DEBE poder adjuntar un PDF de hasta 10 MB a una versión consolidada.
- **FR-015**: El sistema DEBE comprobar por su contenido que el archivo es un PDF.
- **FR-016**: El archivo DEBE guardarse separado de la base de datos, con su huella en la base (RNF-POR-01).
- **FR-017**: El nombre recibido NO DEBE decidir dónde se guarda el archivo.
- **FR-018**: Adjuntar otro informe DEBE dejarlo como vigente, conservando el anterior y su registro.
- **FR-019**: Quien puede leer el consolidado DEBE poder descargar el informe vigente, idéntico al adjuntado.
- **FR-020**: Al descargar, el sistema DEBE comprobar la huella, y no entregar un archivo que no coincida.
- **FR-021**: El consolidado DEBE mostrar el nombre, el tamaño, quién y cuándo del informe vigente.
- **FR-022**: Si el archivo no se pudo guardar, NO DEBE quedar registrado como adjunto.

**Acceso y errores**

- **FR-023**: Solo el Responsable de consolidación DEBE poder consolidar y adjuntar.
- **FR-024**: El Vicerrectorado y el Administrador DEBEN poder leer el consolidado y descargar el informe. Un coordinador NO DEBE poder.
- **FR-025**: Cada error nuevo DEBE estar en el catálogo, con un mensaje claro y accionable (principio XIII).
- **FR-026**: Si otro proceso no responde, el sistema DEBE dar el error del catálogo, sin presentar el consolidado como vacío.
- **FR-027**: El contenido del archivo NO DEBE ir a los registros.

### Key Entities

- **Consolidado**: la versión del distributivo después de consolidar: quién y cuándo, y lo que se calcula al abrirlo.
- **Informe sustentado**: el PDF del Responsable de consolidación, asociado a una versión: nombre, tamaño, huella, quién y cuándo. Se conservan todos, y vale el último.
- **Parcial de carrera**: ya existe. Aporta su estado y su avance.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con los datos de prueba completos, el consolidado coincide con una suma hecha aparte, carrera por carrera.
- **SC-002**: Con los datos de prueba completos, el consolidado se abre en menos de 5 segundos.
- **SC-003**: Adjuntar un PDF de 10 MB tarda menos de 10 segundos, y descargarlo devuelve el mismo archivo.
- **SC-004**: Un Responsable de consolidación consolida y adjunta el informe en menos de 2 minutos, sin ayuda.
- **SC-005**: Ningún archivo que no sea PDF queda guardado: se probaron al menos 3 casos, con nombre `.pdf`.
- **SC-006**: Con otro proceso detenido, responde el error del catálogo en menos de 10 segundos, y nunca como vacío.

## Assumptions

- **Unidades:** no tienen parcial todavía. Investigación, vinculación y gestión se registran como actividades dentro del parcial de cada carrera.
- **Parciales:** hoy todos están en elaboración. El consolidado muestra el estado real, sin esperar a F12-H02.
- **Desactualizado:** se decide comparando la última vez que se consolidó con el último cambio de propuestas y actividades. Un cambio en la oferta, en los docentes o en la normativa no lo marca, porque son de otros procesos: el consolidado se calcula al abrirlo, así que igual se ve actualizado.
- **Versión:** todo se asocia a la versión vigente. Crear versiones nuevas es de la Ola 3.
- **Copias de seguridad:** el almacén de archivos entra en las copias del equipo, si las hay. Esta historia no agrega un mecanismo de copias.
- **Sin antivirus:** se valida que sea un PDF, pero no se analiza su contenido. Lo adjunta un rol de confianza.

## Out of Scope

- Enviar a revisión (F10-H03).
- Mover el estado de los parciales (F12-H02).
- Parciales de unidad.
- Congelar y firmar el informe (Ola 3).
- Generar el informe desde el sistema.
- Leer el contenido del PDF.
- Borrar un informe.
