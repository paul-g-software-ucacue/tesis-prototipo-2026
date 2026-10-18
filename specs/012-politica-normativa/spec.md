# Feature Specification: La política 2024 como primera versión de normativa

**Feature Branch**: `012-politica-normativa`

**Created**: 2026-10-18

**Status**: Draft

**Input**: User description: "Especifica la historia F04-H01 · Tener la política 2024 como primera versión de normativa (issue #40), a partir del ERS (RF-19, RN-01 y RN-03 a RN-15), la carga 7 y el modelo de dominio de Normativa."

**Origen**: issue #40 · F04-H01 · sub-issue de F04 (Parametrizar y versionar la política) · milestone «Ola 1 · Datos listos» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:12-normativa`, `RF-19`, `RN-01`, `RN-03` a `RN-11`, `RN-13` a `RN-15` · tamaño propuesto M · depende de #18 (B00-H01), cerrada. Es la raíz de la cadena F04-H02 (#41) → F02-H01 (#32) → F05-H03 (#45).

**Requerimientos que cubre**: RF-19 (en la parte de la primera versión) · RN-01 y RN-03 a RN-15 (ERS, `requerimientos_mdd/ERS_MDD_Requerimientos.md`), con las 22 filas de la carga 7 (ERS 8.3). **Decisiones tomadas en esta historia:** D-66 (el Vicerrector académico lee «Parámetros y normativa»). **Decisiones que aplica** (`docs/decisiones.md`): D-11 (docente de medio tiempo: 12 + 8), D-13 y ADR-003 (el coordinador académico institucional no existe en la institución de referencia), D-65 y ADR-021 (permisos), ADR-016 (catálogo de errores) y ADR-019 (base de la SPA). **Principios de la constitución** que la rigen: VI (las cifras de la política son datos versionados, nunca fijas en el código) y VII (inmutabilidad y trazabilidad).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero que la Política SENESCYT 2024 venga precargada como la versión 1 de la normativa, para no tener que digitar la tabla de carga horaria.

**Punto de partida.** El servicio Normativa existe solo como esqueleto: tiene su esquema, pero ninguna versión, regla ni parámetro. Las cifras de la política (22 h de clase para un docente de tiempo completo, 12 + 8 para uno de medio tiempo, las horas por tipo de IST de coordinadores y gestores…) no están en ninguna parte del MDD. Esta historia hace que el MDD **nazca con la versión 1 de la normativa**: la Política SENESCYT 2024 (código `SENESCYT-2024-03`, vigente desde el 11 de marzo de 2024) con sus 22 filas de carga horaria, el tipo de IST de la institución y la severidad de cada fila. Es la base de todo lo que viene: versionar la política (F04-H02), abrir un periodo con su versión (F02-H01) y validar el distributivo.

## Clarifications

### Session 2026-10-18

- Q: ¿Quién puede consultar las versiones de normativa, si la historia nombra al Vicerrector académico y D-65 solo da «Parámetros y normativa» al Administrador? → A: El Administrador del sistema y el Vicerrector académico las leen; solo el Administrador las escribe. Ajusta D-65; es D-66.
- Q: ¿Qué parámetros entran en la versión 1 además de las 22 filas? → A: Los de RN-01 a RN-15 que las filas no expresan: las dedicaciones, el máximo de horas, la proporción 60/40, los rangos de matriculados por tipo de IST, los umbrales del coordinador académico institucional, el número de gestores por tipo y la proporción de la nómina para investigación y vinculación.
- Q: ¿Cómo se consulta la versión 1 en esta historia? → A: En una pantalla «Normativa» de la SPA, de solo lectura; F04-H02 le agrega la edición.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - El MDD trae la política 2024 como versión 1 (Priority: P1)

Como Vicerrector académico, quiero que, desde que el MDD se instala, exista la versión 1 de la normativa con la política 2024 completa, para no tener que digitar la tabla de carga horaria antes de abrir el primer periodo.

**Why this priority**: Sin una versión de normativa no se puede abrir un periodo (F02-H01) ni validar un distributivo; es la raíz de la Ola 1.

**Independent Test**: En un MDD recién instalado, entrar con `vicerrectorado.demo` y abrir «Normativa»: hay exactamente una versión, la 1, con las 22 filas de la carga 7 tal como están en la política, el tipo de IST Tipo 2, la severidad de cada fila y los parámetros de RN-01 a RN-15.

**Acceptance Scenarios**:

```gherkin
Escenario: Versión inicial
  Dado el MDD recién instalado
  Cuando consulto las versiones de normativa
  Entonces existe la versión 1 con las 22 filas de la política 2024 de la carga 7
  Y el tipo de IST es Tipo 2
```

1. **Given** el MDD recién instalado, **When** consulto las versiones de normativa, **Then** hay una sola, la versión 1, con su origen («Política SENESCYT 2024», código `SENESCYT-2024-03`) y su fecha de vigencia (11 de marzo de 2024).
2. **Given** la versión 1, **When** consulto su tabla de carga horaria, **Then** tiene las 22 filas de la carga 7, cada una con el tipo de IST al que aplica, el perfil, la dedicación total, las horas de clase con su mínimo y su máximo, las horas de otras actividades de docencia, de gestión y el máximo de investigación y vinculación, la severidad (dura o blanda), la referencia normativa y la observación.
3. **Given** la versión 1, **When** consulto la fila del docente de tiempo completo, **Then** dice 22 horas de clase (entre 18 y 24) y 18 de otras actividades de docencia (RN-03); la del docente de medio tiempo, 12 y 8 (RN-04, D-11).
4. **Given** la versión 1, **When** consulto su tipo de IST, **Then** es Tipo 2.
5. **Given** la versión 1, **When** consulto sus parámetros, **Then** están las dedicaciones (40, 20 y hasta 19 h), el máximo de 40 h, la proporción 60/40, los rangos de matriculados por tipo de IST, los umbrales del coordinador académico institucional, el número de gestores por tipo y el 10 % de la nómina, cada uno con su regla RN-xx.
6. **Given** el Vicerrector académico o el Administrador con sesión, **When** abre «Normativa», **Then** ve la versión 1; **Given** un docente, **When** abre la misma dirección, **Then** ve «Acceso no autorizado» y la pantalla no está en su menú.

---

### User Story 2 - La versión 1 rige solo con datos, nunca con cifras en el código (Priority: P1)

Como institución, quiero que las cifras de la política solo existan en la versión de normativa, para que un cambio de la norma se haga creando otra versión y no cambiando el sistema (principio VI).

**Why this priority**: Es la condición de RF-19 («al cambiar un parámetro en una versión nueva, los periodos anteriores no se ven afectados») y del principio VI; si una cifra queda fija en el código, F04-H02 no puede versionarla.

**Independent Test**: Comprobar que cada cifra de RN-01 y RN-03 a RN-15 que esta historia incluye se lee de la versión 1, y que reinstalar o reiniciar el MDD no crea otra versión ni cambia la versión 1.

**Acceptance Scenarios**:

1. **Given** el MDD con la versión 1, **When** se reinicia o se despliega una versión nueva del sistema, **Then** sigue habiendo una sola versión 1, con los mismos datos.
2. **Given** la versión 1, **When** se compara cada una de sus 22 filas con la carga 7 de la política, **Then** coinciden todas, fila por fila y valor por valor.
3. **Given** la versión 1, **When** alguien intenta cambiarla, **Then** no hay forma de hacerlo en esta historia: editar una versión o crear otra es de F04-H02.

---

### Edge Cases

- **Coordinador académico institucional (RN-09)**: la fila existe en la política y entra en la versión 1 tal cual, con su observación («Solo T4–T5, o T1–T3 con más de 500 estudiantes o 3 o más campus»). En la institución de referencia, Tipo 2 con una sede, ese cargo no existe y la consolidación la hace el Responsable de consolidación (D-13, ADR-003); el aviso de RN-09 lo muestra la validación del distributivo, no esta historia.
- **Filas sin un valor**: la fila de docentes de investigación y vinculación no tiene horas de otras actividades de docencia ni de gestión, pero sí un máximo de 24 h de investigación y vinculación. Un valor vacío se guarda como «sin valor», no como cero.
- **Filas para todos los tipos de IST**: siete filas aplican a todos los tipos (`TODOS`) y quince a un tipo concreto (T1 a T5: coordinadores, gestores, y rector y vicerrector). La versión las guarda así, sin repetir las de `TODOS` por cada tipo.
- **Observaciones de interpretación**: varias filas dicen «Interpretación: …» o «verificar (C-03)». Se guardan como están; las cerradas por decisiones (D-11 para el medio tiempo) ya tienen su valor definitivo en la fila.
- **Usuario sin permiso**: quien no tenga el permiso de lectura de «Parámetros y normativa» recibe «Acceso no autorizado» (ADR-021), igual que en el resto del MDD.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Desde la instalación, el MDD DEBE tener la versión 1 de la normativa, con su número, su origen («Política SENESCYT 2024», código `SENESCYT-2024-03») y su fecha de vigencia (11 de marzo de 2024), sin que nadie la cargue.
- **FR-002**: La versión 1 DEBE tener la tabla de carga horaria con las 22 filas de la carga 7 (ERS 8.3), cada una con el tipo de IST al que aplica (T1 a T5 o todos), el perfil, la dedicación total, las horas de clase con su mínimo y su máximo, las horas de otras actividades de docencia, de gestión y el máximo de investigación y vinculación, la severidad (dura o blanda), la referencia normativa y la observación.
- **FR-003**: La versión 1 DEBE tener el tipo de IST de la institución: Tipo 2 (RN-06; en el prototipo es un parámetro fijo, el cálculo por matriculados es F23).
- **FR-004**: La versión 1 DEBE incluir, con su regla RN-xx y su severidad, los parámetros de RN-01 a RN-15 que las 22 filas no expresan:
  - RN-01: las dedicaciones de tiempo completo (40 h), medio tiempo (20 h) y tiempo parcial (hasta 19 h);
  - RN-02: el máximo de 40 h asignadas a un docente;
  - RN-05: la proporción de 60 % de clase y 40 % de otras actividades de docencia;
  - RN-06: los rangos de matriculados de cada tipo de IST (T1 1–300, T2 301–600, T3 601–1200, T4 1201–1800, T5 1801–3500);
  - RN-09: los umbrales que admiten un coordinador académico institucional en un IST Tipo 1 a 3 (más de 500 estudiantes o 3 o más campus);
  - RN-10: el número de docentes con cargo de gestión por tipo de IST (T1 5–7, T2 8–11, T3 12–15, T4 15–18, T5 21–25);
  - RN-13: el 10 % de la nómina para investigación y vinculación.
- **FR-005**: Ninguna cifra de la política que esté en la versión 1 DEBE quedar fija en el código: quien la necesite la lee de la versión de normativa (principio VI).
- **FR-006**: Reiniciar el MDD o desplegar una versión nueva del sistema NO DEBE crear otra versión de normativa ni cambiar los datos de la versión 1.
- **FR-007**: La versión 1 DEBE poder consultarse, con todos sus datos, con el permiso de lectura de «Parámetros y normativa», que tienen el Administrador del sistema y el Vicerrector académico. La escritura sigue solo en el Administrador (D-65, ajustada por D-66).
- **FR-008**: La SPA DEBE tener una pantalla «Normativa», de solo lectura y en el menú solo para quien tenga el permiso, con la lista de versiones y, de la versión elegida, su origen, su código, su vigencia, el tipo de IST, los parámetros de FR-004 y la tabla de carga horaria con sus 22 filas, en español.
- **FR-009**: Sin el permiso de lectura de «Parámetros y normativa», la consulta DEBE responder «Acceso no autorizado» (403), validado en el servidor (RNF-SEG-03, ADR-021).
- **FR-010**: La creación de la versión 1 DEBE quedar registrada como el primer evento de la normativa (`VersionNormativaCreada`), para la bitácora y para los servicios que dependan de la normativa.

### Key Entities

- **Política**: la Política SENESCYT 2024 de carga horaria (POL) con el RCE. Es el origen de la versión 1 (glosario §7.3).
- **Versión de normativa**: conjunto numerado de parámetros de RN-01 a RN-26 que rige un periodo; no cambia una vez asociada a un periodo (glosario §7.3, `VersionNormativa`). Tiene número, origen, código, fecha de vigencia, tipo de IST, tabla de carga horaria y reglas.
- **Tabla de carga horaria**: las horas por componente según el perfil, la dedicación y el tipo de IST (carga 7, 22 filas; `TablaCargaHoraria`).
- **Regla**: comprobación de la política con su identificador RN-xx, sus parámetros y su severidad (glosario §7.3); en la tabla, la severidad de cada fila.
- **Severidad**: dura (impide guardar o avanzar) o blanda (exige justificación).
- **Tipo de IST**: T1 a T5; en el prototipo, Tipo 2 fijo.
- **Componente**: clase, otras actividades de docencia, investigación, vinculación o gestión.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En un MDD recién instalado, el 100 % de las 22 filas de la versión 1 coincide, valor por valor, con la carga 7 de la política 2024; ninguna falta ni sobra.
- **SC-002**: El Vicerrector académico no digita ninguna cifra de la tabla de carga horaria antes de abrir el primer periodo: 0 filas cargadas a mano, y encuentra la versión 1 en «Normativa» en menos de 1 minuto desde que inicia sesión.
- **SC-003**: Después de 3 reinicios o despliegues seguidos, sigue habiendo exactamente una versión 1, con los mismos datos.
- **SC-004**: Ninguna de las cifras de la política que la versión 1 contiene aparece fija en el código del MDD (principio VI); lo comprueba una revisión del código y una prueba que lee los valores de la versión.
- **SC-005**: Un usuario sin permiso que intenta consultar la normativa recibe «Acceso no autorizado» en el 100 % de los casos, también llamando directamente al servidor.

## Assumptions

- Los datos de la versión 1 son los de la carga 7 de los artefactos de requerimientos (`cargas/datos_prueba/07_politica_carga_horaria.csv`), que reproducen la tabla pública de la Política SENESCYT de marzo de 2024; no son datos personales.
- La fila del docente de medio tiempo queda con 12 h de clase y 8 de otras actividades de docencia, como cerró D-11, aunque su observación diga «verificar (C-03)».
- Los parámetros de RN-16 a RN-26 (entre ellos los pesos y el mínimo de afinidad de RN-19 y RN-24 y las ventanas de jornada), la edición de las severidades (MD-08) y el catálogo de actividades (RF-21) son de F04-H03 y de historias posteriores; la versión 1 no los incluye todavía.
- Las severidades de los parámetros de FR-004 son las del ERS cuando las dice; las que no, se proponen en el plan y se confirman al especificar la validación del distributivo (MD-08).
- Crear versiones nuevas o editarlas es de F04-H02; asociar una versión a un periodo, de F02-H01; cargar la política desde un archivo (carga 7), de F01.
- El tipo de IST Tipo 2 corresponde a la institución de referencia (497 estudiantes, una sede, C-22).
- Los datos son ficticios o públicos.

## Dependencies

- B00-H01 (#18), cerrada: el esqueleto del servicio Normativa con su esquema.
- F05-H02 (#44), cerrada: los permisos por funcionalidad (`PARAMETROS`, D-65, ADR-021).
- Modelo de errores común (ADR-016) y base de la SPA (ADR-019).
