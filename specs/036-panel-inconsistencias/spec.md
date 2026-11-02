# Feature Specification: Ver el panel de inconsistencias globales

**Feature Branch**: `036-panel-inconsistencias`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Especifica la historia F10-H01 · Ver el panel de inconsistencias globales (issue #53), a partir de RF-29, RN-05, RN-10, RN-12, RN-17, RN-18, RF-24 y MD-07, con las aclaraciones del autor.

**Origen**: issue #53 · F10-H01 · sub-issue de F10 (Consolidar, con el panel de inconsistencias y el informe sustentado, #10) · milestone «Ola 2 · Distributivo válido» · Ola del secuenciador MVP · tamaño L · depende de #52 (F07-H03), cerrada.

**Requerimientos que cubre**:
- **RF-29:** detectar y listar las inconsistencias globales: docentes sin horas de clase, asignaturas sin docente, sobrecarga, coordinadores duplicados y gestores fuera de rango.
- **RN-05:** proporción 60/40, con la planificación y la evaluación entre las otras actividades de docencia.
- **RN-10:** número de docentes con cargo de gestión según el tipo de IST.
- **RN-12:** un coordinador de carrera por cada carrera ofertada.
- **RN-17:** ningún docente queda sin horas de clase.
- **RN-18:** las horas de clase de una carrera no exceden las de su malla.
- **RF-24:** cada asignatura de la oferta tiene un docente propuesto.
- **MD-07:** una inconsistencia blanda justificada deja de estar abierta.

**Decisiones que aplica** (`docs/decisiones.md` y anexos): D-13, D-72, D-74, D-85 y ADR-010. **Principios de la constitución 2.2.0**: II (DDD táctico en el núcleo), III (lenguaje ubicuo), IV (API primero), VI (las reglas son datos), VII (trazabilidad), VIII (pruebas primero en el dominio), XIII (catálogo de errores) y XIV (validación de entradas).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero ver las inconsistencias globales del distributivo con la acción que corrige cada una, para resolverlas antes de enviar a revisión.

**Punto de partida.** Cada asignación ya se valida al guardarla (F07-H02), y una propuesta bajo el mínimo de afinidad queda como excepción justificada (F07-H03). Nadie mira todavía el distributivo completo: un docente puede quedar sin clase, un paralelo sin propuesta o una carrera sin coordinador, y nada lo avisa.

Esta historia agrega:
- **El panel de inconsistencias:** todo el periodo, con la regla, lo afectado y la acción que corrige cada una.
- **Siete comprobaciones globales,** con la severidad de la normativa.
- **La justificación de las blandas,** a cargo del Responsable de consolidación, con la proporción 60/40 que D-85 dejó aquí.

## Clarifications

### Session 2026-11-02

- Q: ¿Qué evalúa el panel? → A: Siete comprobaciones: docente sin horas de clase (RN-17), asignatura-paralelo sin propuesta (RF-24), un coordinador por carrera (RN-12), número de gestores (RN-10), horas de clase frente a la malla (RN-18), sobrecarga (RF-29, RN-02) y proporción 60/40 (RN-05, D-85). Las horas por dedicación y cargo, RN-09 y RN-16 quedan para otra historia.
- Q: ¿Quién justifica las blandas? → A: El Responsable de consolidación, en el panel, con un texto de 10 a 500 caracteres. Deja de estar abierta, sigue visible como justificada y queda registro de quién y cuándo.
- Q: ¿Qué ofrece cada inconsistencia? → A: Una indicación de qué hacer y quién debe hacerlo, con un enlace a la pantalla donde se corrige. El panel no cambia datos, salvo las justificaciones.
- Q: ¿Quién abre el panel? → A: El Responsable de consolidación y el Vicerrectorado, que ven todo el periodo. El Vicerrectorado solo lee. Los coordinadores siguen con sus avisos al guardar.
- Q: ¿Cómo se mide RN-18? → A: Por paralelo, como en F03-H03: un paralelo cuyas horas pasan las de su asignatura en la malla. Sale una inconsistencia por carrera, que lista esos paralelos.
- Q: ¿Cuándo un docente incumple el 60/40? → A: Cuando su porcentaje de clase se aparta de la meta más que un margen. El margen es un dato nuevo de la versión de normativa, con 5 puntos por omisión.
- Q: ¿El panel revisa la planificación y la evaluación? → A: Sí, dentro de RN-05: a un docente con horas de clase le tienen que constar las dos.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver lo que falta por resolver (Priority: P1)

Como Responsable de consolidación, abro el panel de un periodo y veo cada inconsistencia con su regla, su severidad y lo que afecta, para saber qué falta antes de enviar a revisión.

**Why this priority**: es la historia. Sin el panel, nadie ve el distributivo completo.

**Independent Test**: con una versión que tiene un docente sin horas de clase, un paralelo sin propuesta y dos coordinadores en la misma carrera, abrir el panel y ver las tres.

**Acceptance Scenarios**:

1. **Dada** una versión con un docente sin horas de clase, una asignatura-paralelo sin propuesta y dos coordinadores en la misma carrera, **cuando** abro el panel, **entonces** veo las tres, cada una con su regla y la acción que la corrige.
2. **Dada** una carrera con paralelos cuyas horas pasan las de su asignatura en la malla, **cuando** abro el panel, **entonces** aparece una inconsistencia de RN-18 para esa carrera, con esos paralelos.
3. **Dado** un número de docentes con cargo de gestión fuera del rango del tipo de IST, **cuando** abro el panel, **entonces** aparece una inconsistencia de RN-10, con el número y el rango.
4. **Dado** un docente que pasa sus horas, **cuando** abro el panel, **entonces** aparece como sobrecarga, con sus horas y su límite.
5. **Dada** una carrera ofertada sin coordinador, **cuando** abro el panel, **entonces** aparece una inconsistencia de RN-12 para esa carrera.
6. **Dada** una versión sin inconsistencias, **cuando** abro el panel, **entonces** veo que no hay ninguna abierta, y recuerdo qué reglas no se evalúan todavía.

---

### User Story 2 - Saber cómo corregir cada una (Priority: P1)

Como Responsable de consolidación, veo qué hay que hacer y quién debe hacerlo, con un enlace a la pantalla donde se corrige, porque yo no cambio la oferta ni las propuestas.

**Why this priority**: el issue lo pide, y un listado sin acción no resuelve nada.

**Independent Test**: en un paralelo sin propuesta, ver quién debe proponer y llegar a la oferta del periodo con el enlace.

**Acceptance Scenarios**:

1. **Dada** una inconsistencia, **cuando** la veo, **entonces** dice qué hacer, quién debe hacerlo y lleva a la pantalla donde se corrige.
2. **Dada** una inconsistencia corregida en su pantalla, **cuando** vuelvo a abrir el panel, **entonces** ya no aparece.
3. **Dado** que no puedo abrir la pantalla de destino, **cuando** veo la inconsistencia, **entonces** veo la indicación sin el enlace.

---

### User Story 3 - Justificar una blanda (Priority: P1)

Como Responsable de consolidación, escribo la justificación de una inconsistencia blanda, para que deje de estar abierta sin cambiar el distributivo.

**Why this priority**: es lo que D-85 dejó aquí, y sin esto un 60/40 razonable bloquearía el envío a revisión.

**Independent Test**: con un docente fuera del margen de 60/40, justificarlo, ver que deja de contar como abierta y que sigue visible como justificada.

**Acceptance Scenarios**:

1. **Dada** una inconsistencia blanda, **cuando** escribo la justificación, **entonces** deja de estar abierta y sigue visible como justificada, con el texto, quién y cuándo.
2. **Dada** una inconsistencia blanda con justificación, **cuando** abro el panel, **entonces** no aparece como abierta.
3. **Dada** una propuesta guardada como excepción (RN-19), **cuando** abro el panel, **entonces** aparece como justificada, no como abierta.
4. **Dado** un texto de menos de 10 o de más de 500 caracteres, **cuando** guardo, **entonces** el campo dice qué corregir y lo escrito sigue.
5. **Dada** una inconsistencia dura, **cuando** la veo, **entonces** no ofrece justificarla.
6. **Dada** una justificación ya guardada, **cuando** la cambio, **entonces** queda el texto nuevo y el registro conserva el cambio.

---

### User Story 4 - El 60/40 de cada docente (Priority: P2)

Como Responsable de consolidación, veo a los docentes que se apartan de la proporción más de lo que admite la normativa, o a los que les falta la planificación o la evaluación, para no revisar a uno por uno.

**Why this priority**: D-85 la trae aquí, y depende de que exista el panel.

**Independent Test**: con la meta 60/40 y margen 5, un docente al 65 % no aparece y uno al 66 % sí.

**Acceptance Scenarios**:

1. **Dado** un docente dentro del margen, **cuando** abro el panel, **entonces** no aparece por los porcentajes.
2. **Dado** un docente que se aparta más que el margen, **cuando** abro el panel, **entonces** aparece con sus porcentajes, la meta y el margen.
3. **Dado** un docente con horas de clase al que le falta la planificación o la evaluación, **cuando** abro el panel, **entonces** aparece por RN-05, diciendo cuál falta.
4. **Dada** una versión de normativa con otra meta o con otro margen, **cuando** abro el panel de un periodo que la usa, **entonces** se aplican esos valores.
5. **Dado** un docente sin horas de clase ni de otras actividades de docencia, **cuando** abro el panel, **entonces** no aparece por RN-05.

---

### User Story 5 - El Vicerrectorado sigue el estado (Priority: P2)

Como Vicerrector, abro el panel para ver cómo va el periodo antes de recibir el consolidado, sin poder cambiar nada.

**Why this priority**: es útil, pero no impide consolidar.

**Independent Test**: entrar como Vicerrector, ver el mismo panel, sin la opción de justificar.

**Acceptance Scenarios**:

1. **Dado** el Vicerrector, **cuando** abre el panel, **entonces** ve lo mismo que el Responsable de consolidación, sin la opción de justificar.
2. **Dado** un Coordinador de carrera, **cuando** intenta abrir el panel, **entonces** no tiene acceso.
3. **Dado** el Vicerrector, **cuando** intenta guardar una justificación, **entonces** se rechaza y no se guarda nada.

---

### Edge Cases

- **Periodo sin distributivo:** un aviso, no un error, porque todavía no hay nada que evaluar.
- **Periodo cerrado:** el panel se puede leer, pero no justificar.
- **Paralelo retirado:** no cuenta como sin propuesta, y su propuesta no cuenta para las horas.
- **Docente en licencia o inactivo:** no cuenta para RN-17, RN-05 ni la sobrecarga. Si tiene una propuesta, el paralelo no sale como sin propuesta.
- **Regla sin severidad o sin parámetro en la versión:** el panel falla con el error del catálogo, sin inventar el dato (principio VI).
- **Una inconsistencia justificada que desaparece:** deja de mostrarse. Si vuelve a aparecer en la misma versión, conserva su justificación.
- **Severidad que cambia:** cada periodo usa su versión, que no cambia después de abrirlo.
- **Regla dura en la versión:** si la versión del periodo marca RN-05 o RN-10 como dura, no se puede justificar. Una justificación que exista para una dura no la cierra.
- **Justificar algo que ya no existe:** responde con un error claro, por ejemplo si otro lo corrigió con el panel abierto.
- **Un proceso no responde:** el error del catálogo, sin mostrar un panel incompleto como si estuviera vacío.
- **Dos personas justifican a la vez:** queda la última, y el registro conserva las dos.
- **El texto:** se guarda sin los espacios de los extremos, y no va a los registros técnicos.

## Requirements *(mandatory)*

### Functional Requirements

**El panel**

- **FR-001**: El sistema DEBE ofrecer un panel de inconsistencias por periodo, evaluado sobre la versión vigente del distributivo al abrirlo, sin guardar una foto.
- **FR-002**: Cada inconsistencia DEBE decir su regla, su severidad, lo que afecta, el detalle del incumplimiento y si está abierta o justificada.
- **FR-003**: El panel DEBE mostrar cuántas hay abiertas, por severidad, y primero las duras.
- **FR-004**: El panel DEBE mostrar las justificadas aparte de las abiertas.
- **FR-005**: El panel DEBE decir qué reglas no evalúa todavía.
- **FR-006**: La severidad y los parámetros de cada regla DEBEN salir de la versión de normativa del periodo. También la de RF-24, la asignatura-paralelo sin propuesta, que la política marca como dura.

**Las comprobaciones**

- **FR-007 (RN-17)**: DEBE haber una inconsistencia por cada docente activo sin horas de clase en todas las carreras.
- **FR-008 (RF-24)**: DEBE haber una inconsistencia por cada asignatura-paralelo no retirada sin propuesta.
- **FR-009 (RN-12)**: DEBE haber una inconsistencia por cada carrera ofertada que no tenga exactamente un coordinador con cargo vigente.
- **FR-010 (RN-10)**: DEBE haber una inconsistencia si el número de docentes con cargo de gestión vigente está fuera del rango del tipo de IST.
- **FR-011 (RN-18)**: DEBE haber una inconsistencia por carrera si alguno de sus paralelos no retirados tiene horas por encima de las de su asignatura en la malla, y DEBE listarlos.
- **FR-012 (sobrecarga)**: DEBE haber una inconsistencia por cada docente que pasa su límite de horas, sumando todas las carreras.
- **FR-013 (RN-05)**: DEBE haber una inconsistencia por cada docente cuyo porcentaje de clase se aparta de la meta más que el margen.
- **FR-014 (RN-05)**: DEBE haber una inconsistencia por cada docente con horas de clase al que le falte la planificación o la evaluación.
- **FR-015**: El margen de RN-05 DEBE ser un dato de la versión de normativa, con 5 puntos por omisión, y las versiones existentes DEBEN tener ese valor.
- **FR-016**: Las propuestas con excepción (RN-19) DEBEN aparecer como justificadas.

**La acción**

- **FR-017**: Cada inconsistencia DEBE decir qué hacer y quién debe hacerlo, en «tú» donde corresponda y sin lenguaje técnico.
- **FR-018**: Cada inconsistencia DEBE llevar a la pantalla donde se corrige, si quien consulta puede abrirla.
- **FR-019**: El panel NO DEBE cambiar datos, salvo las justificaciones.

**La justificación**

- **FR-020**: El Responsable de consolidación DEBE poder justificar una inconsistencia blanda abierta, con un texto de 10 a 500 caracteres.
- **FR-021**: Una inconsistencia dura NO DEBE poder justificarse.
- **FR-022**: Una justificación DEBE valer para esa regla y ese objeto en esa versión.
- **FR-023**: Una justificación DEBE poder cambiarse, y cada cambio DEBE quedar en el registro con quién y cuándo.
- **FR-024**: Un periodo cerrado NO DEBE admitir justificaciones.
- **FR-025**: La justificación de RN-19 DEBE seguir en su propuesta: el panel la muestra y no la edita.

**Acceso y errores**

- **FR-026**: El Responsable de consolidación y el Vicerrectorado DEBEN poder abrir el panel, con todo el periodo. Un coordinador NO DEBE poder abrirlo. El Administrador del sistema lo lee, porque la matriz D-16 le da la lectura de la consolidación.
- **FR-027**: Solo el Responsable de consolidación DEBE poder justificar.
- **FR-028**: Un periodo sin distributivo DEBE responder con un aviso, no con un error.
- **FR-029**: Cada error nuevo DEBE estar en el catálogo, con un mensaje claro y accionable (principio XIII).
- **FR-030**: Si otro proceso no responde, el sistema DEBE dar el error del catálogo, sin presentar el panel como vacío.

### Key Entities

- **Inconsistencia**: regla, severidad, nivel, objeto afectado, detalle, indicación y estado (abierta o justificada).
- **Objeto afectado**: lo que incumple la regla: un docente, una asignatura-paralelo, una carrera o el periodo.
- **Justificación de una inconsistencia**: versión, regla, objeto afectado, texto, quién y cuándo. Es distinta de la `Justificacion` de una propuesta, que dice por qué se propuso a un docente.
- **Panel de inconsistencias**: las inconsistencias de la versión vigente de un periodo, abiertas y justificadas, con sus totales.
- **Margen de la proporción**: los puntos que un docente puede apartarse de la meta de RN-05 sin incumplirla.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con los datos de prueba completos, el panel coincide con un conteo hecho aparte, regla por regla.
- **SC-002**: Con los datos de prueba completos, el panel se abre en menos de 5 segundos.
- **SC-003**: Las siete comprobaciones responden a sus casos: cada una aparece cuando debe y desaparece al corregirla.
- **SC-004**: Un Responsable de consolidación entiende qué hacer con una inconsistencia y la justifica en menos de 1 minuto, sin ayuda.
- **SC-005**: Guardar una justificación tarda menos de 3 segundos.
- **SC-006**: Con otro proceso detenido, el panel responde con el error del catálogo en menos de 10 segundos, y nunca como vacío.
- **SC-007**: Las reglas del panel en el motor tienen al menos 80 % de cobertura.

## Assumptions

- **Tipo de IST:** Tipo 2 fijo en el prototipo. El rango de RN-10 sale de la versión.
- **Gestores:** RN-10 cuenta los cargos vigentes G06 a G21 (D-73). El rector, el vicerrector, los coordinadores y los miembros de comisión no cuentan.
- **Coordinador:** RN-12 mira el cargo de Coordinador de carrera vigente, no la carrera indicada al usuario.
- **Docente activo:** cuenta para RN-17, sin excepciones por cargo. Si la normativa exime a un cargo, será otra historia.
- **Porcentajes:** son los de F07-H01. El límite es inclusivo: con meta 60 y margen 5, 55 y 65 cumplen.
- **La versión vigente:** una justificación no pasa a otra versión del distributivo.
- **Cambios después de justificar:** si la inconsistencia sigue, la justificación sigue, aunque cambien las cifras.
- **Sin paginación:** el panel de un periodo es corto, porque el instituto tiene decenas de docentes.
- **Enviar a revisión:** todavía no existe. Esta historia deja el total de abiertas para esa etapa.

## Out of Scope

- Enviar a revisión con 0 inconsistencias, que es de la cadena de aprobación.
- Consolidar y el informe sustentado (F10-H02).
- Las horas por dedicación y cargo (RN-03, RN-04, RN-07, RN-08, RN-11, RN-14, RN-15), RN-09 y RN-16. **Limitación:** el panel puede salir vacío aunque alguna se incumpla, y lo dice. Una propuesta a un docente que dejó de estar activo tampoco se detecta.
- Los choques de horario y la capacidad por jornada (RF-60, RN-26).
- Corregir datos desde el panel.
- El panel para coordinadores.
- Volver a evaluar al guardar cada asignación las reglas globales.
