# Feature Specification: Consultar la bitácora

**Feature Branch**: `040-consultar-bitacora`

**Created**: 2026-11-07

**Status**: Draft

**Input**: User description: "F17-H02 · Consultar la bitácora (issue #70). Usar el texto y los criterios del issue. Tener en cuenta la nota de specs/039-bitacora-cambios/evidencia.md («Para revisar en F17-H02»): guardar una actividad con las mismas horas deja una entrada de 4 a 4."

**Origen**: issue #70 · F17-H02 · sub-issue de F17 (Bitácora de auditoría que no se puede borrar, #16) · milestone «Ola 3 · Aprobado y firmado» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:01-usuarios`, `RF-44` · tamaño propuesto M · depende de #69 (F17-H01), terminada en el #193.

**Requerimientos que cubre**:
- **RF-44:** una bitácora de auditoría (quién, qué, cuándo, valor anterior y valor nuevo) «consultable por los usuarios autorizados». F17-H01 la llenó; esta historia la hace consultable.
- **RNF-INT-02:** su criterio en la ERS es «consulta de la bitácora».
- **D-16:** solo el Administrador del sistema («todo, incluida la bitácora») y el Vicerrector académico («todo el distributivo, reportes y bitácora») la leen. En la matriz de D-65 son los dos únicos roles con lectura de `BITACORA`.

**Decisiones que aplica** (`docs/decisiones.md`): D-65 (la matriz D-16 en permisos), ADR-021 (permisos validados en el backend), D-90 y D-91 (qué tiene la bitácora y qué datos personales no guarda), D-72 y el principio XIV (validación de los filtros), D-74 y ADR-016 (catálogo de errores). **Principios de la constitución**: III (lenguaje ubicuo), IV (API primero), VII (trazabilidad), IX (mínimo privilegio), X (alcance), XIII (errores) y XIV (validación de las entradas).

**Historia (tal como está en el issue)**: Como Vicerrector académico, quiero consultar la bitácora filtrando por fecha, usuario y objeto, para responder a una auditoría.

**Punto de partida.** F17-H01 dejó la bitácora en el servicio de Usuarios: cada cambio de los seis servicios llega como una entrada con quién, qué acción, cuándo, el servicio de origen, el periodo, el objeto, el valor anterior y el valor nuevo. No se puede borrar ni modificar, y ya tiene más de 600 entradas en el equipo de demostración. Hoy solo se revisa con consultas de solo lectura sobre la base: no hay pantalla ni operación para los usuarios.

La evidencia de F17-H01 dejó una nota para esta historia: guardar una actividad con las mismas horas deja una entrada de 4 a 4. No viola ningún requisito, pero es ruido en la pantalla de la bitácora.

## Clarifications

### Session 2026-11-07

- Q: ¿Dónde se quita el ruido de las entradas en las que nada cambió? → A: En el origen y en todos los servicios: un guardado sin ningún cambio no deja entrada, ni en el registro local ni en la bitácora. Es la regla que F17-H01 ya aplica a un intento rechazado («nada cambió»), y reemplaza su supuesto «Mismo valor». Se registró como D-94. Al planificar se vio que comparar solo el valor anterior y el nuevo de la entrada perdería cambios reales (el texto de una justificación no va en la entrada), así que se compara el elemento entero en su servicio; la mayoría de los servicios ya lo hacía y faltaban seis acciones.
- Q: ¿Qué es «objeto» en el filtro? → A: Las dos cosas: el tipo de cambio (servicio y acción) y un identificador del elemento, que se busca entre los del objeto de la entrada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar la bitácora con filtros (Priority: P1)

Como Vicerrector académico o Administrador del sistema, quiero ver las entradas de la bitácora y filtrarlas por fecha, usuario y objeto, para responder a una auditoría con las entradas que me piden y solo esas.

**Why this priority**: es la historia, y lo que hace a la bitácora «consultable por los usuarios autorizados» (RF-44).

**Independent Test**: con el Vicerrector, abrir la bitácora, filtrar por `coordinacion.demo` y por el día de hoy, y ver solo las entradas de ese usuario en ese día, con su acción, su objeto y sus valores anterior y nuevo.

**Acceptance Scenarios**:

1. **Dado** un usuario Vicerrector académico o Administrador, **cuando** filtro la bitácora por usuario y fecha, **entonces** veo solo las entradas que cumplen el filtro. *(issue)*
2. **Dado** un usuario autorizado, **cuando** abre la bitácora sin filtros, **entonces** ve las entradas más recientes primero, por páginas.
3. **Dado** un usuario autorizado, **cuando** filtra por un objeto, **entonces** ve solo las entradas de ese objeto.
4. **Dado** un usuario autorizado, **cuando** combina varios filtros, **entonces** ve solo las entradas que cumplen todos a la vez.
5. **Dado** un filtro que ninguna entrada cumple, **cuando** se aplica, **entonces** el usuario ve que no hay entradas, sin un error.
6. **Dado** una entrada, **cuando** el usuario la mira, **entonces** ve quién, qué acción, cuándo, el servicio, el periodo, el objeto, el valor anterior y el valor nuevo; si falta el anterior (al crear) o el nuevo (al quitar), lo ve indicado.

---

### User Story 2 - Solo los autorizados la consultan (Priority: P1)

Como institución, quiero que solo el Vicerrector académico y el Administrador del sistema puedan consultar la bitácora, para que la auditoría no quede expuesta a cualquier usuario (D-16, principio IX).

**Why this priority**: el issue lo pide, y la bitácora dice quién hizo cada cambio.

**Independent Test**: con un Coordinador de carrera, pedir la bitácora por la API y por la pantalla, y recibir el error 403 del catálogo; y que la opción no aparezca en su menú.

**Acceptance Scenarios**:

1. **Dado** un Coordinador de carrera, **cuando** intenta consultar la bitácora, **entonces** recibe un error 403. *(issue)*
2. **Dado** un usuario con cualquier rol sin lectura de la bitácora, **cuando** intenta consultarla, **entonces** recibe el mismo error 403, sin que el error diga qué permiso le falta.
3. **Dado** un usuario sin lectura de la bitácora, **cuando** navega, **entonces** no ve la opción de la bitácora.
4. **Dado** una petición sin sesión, **cuando** intenta consultar la bitácora, **entonces** recibe un error 401.

---

### User Story 3 - Sin entradas que no cambian nada (Priority: P2)

Como Vicerrector académico, quiero que la bitácora no tenga entradas en las que el valor anterior y el nuevo son iguales, para que cada entrada que reviso en una auditoría sea un cambio real.

**Why this priority**: viene de la nota de F17-H01. No es un criterio del issue, pero sin esto la pantalla muestra ruido, como una actividad de 4 a 4 horas.

**Independent Test**: guardar una actividad de 4 horas con las mismas 4 horas, y ver que ni el registro de cambios de Generación ni la bitácora tienen una entrada nueva; después cambiarla a 6 y ver la entrada de 4 a 6. Repetir el guardado sin cambios con un caso de cada servicio.

**Acceptance Scenarios**:

1. **Dado** una actividad de 4 horas, **cuando** un coordinador la guarda con las mismas 4 horas, **entonces** el guardado responde con éxito, y no queda ninguna entrada, ni en el registro de cambios de Generación ni en la bitácora, porque nada cambió.
2. **Dado** una actividad de 4 horas, **cuando** un coordinador la cambia a 6, **entonces** la bitácora la registra como hoy, con 4 y 6.
3. **Dado** un elemento de cualquier servicio (un periodo, un docente, una versión de la normativa, un rol), **cuando** se corrige con los mismos valores que tenía, **entonces** no queda ninguna entrada.
4. **Dado** un cambio que crea, que quita o que es un acto (consolidar, adjuntar el informe sustentado, cargar la configuración del flujo), **cuando** se guarda, **entonces** deja su entrada como hoy: la regla solo aplica a las acciones que cambian algo que ya existía, y compara el elemento entero, no el valor anterior y el nuevo de la entrada (D-94).

---

### Edge Cases

- **Rango de fechas al revés** (la fecha desde es posterior a la fecha hasta): se rechaza con un error del catálogo que dice qué corregir, y la pantalla lo marca antes de enviar.
- **Un filtro con caracteres no permitidos o más largo de lo permitido:** se rechaza con un error del catálogo, en la pantalla y en la API (principio XIV).
- **Una página más allá de la última:** responde sin entradas, sin error.
- **Una entrada que llega mientras se consulta:** aparece en la próxima consulta; el orden por momento no cambia las páginas ya vistas de forma que se repita o se salte una entrada.
- **Una entrada de Docentes con datos personales:** se ve qué campos personales cambiaron, sin valores, como la guardó F17-H01 (D-91). La consulta no agrega ni reconstruye esos valores.
- **Las entradas del usuario `sistema`** (por ejemplo, los datos iniciales): se filtran por usuario como cualquier otro.
- **Fechas y zona horaria:** un filtro «del 7 de noviembre» incluye todo ese día en la hora de Ecuador continental, no en UTC.
- **Una corrección que solo cambia campos personales de un docente:** sí es un cambio, aunque la entrada no lleve sus valores (D-91), y deja su entrada con los campos que cambiaron.
- **Un guardado que cambia un campo y repite los demás:** es un cambio y deja su entrada.
- **Un cambio solo en el texto de una justificación** (de una propuesta o de una inconsistencia): es un cambio y deja su entrada; como la entrada no lleva el texto, su objeto dice que el texto cambió (D-94).
- **Un identificador que aparece en el objeto de varias entradas de servicios distintos** (por ejemplo, el código de un periodo): el filtro por identificador las muestra todas; se acota con el filtro por servicio.
- **Un usuario que perdió el rol durante la sesión:** pierde el acceso cuando su permiso deja el token, en la próxima renovación (D-62).

## Requirements *(mandatory)*

### Functional Requirements

**Consultar**

- **FR-001**: Un usuario con lectura de la bitácora DEBE poder consultar sus entradas, de las más recientes a las más antiguas por el momento del cambio.
- **FR-002**: La consulta DEBE poder filtrarse por:
  - **Fecha:** desde y hasta, ambas opcionales e inclusivas, por días completos.
  - **Usuario:** el nombre de usuario que hizo el cambio, exacto.
  - **Objeto, por tipo de cambio:** el servicio de origen y, dentro de él, la acción (por ejemplo, Generación y `ACTIVIDAD_GUARDADA`), elegidos de listas. La acción solo se elige con su servicio.
  - **Objeto, por identificador:** el identificador de un elemento (la cédula de un docente, el código de una asignatura, de una actividad o de un rol). Una entrada lo cumple si alguno de los identificadores de su objeto es igual al buscado, completo y exacto.
  - **Periodo:** el código del periodo, opcional.
- **FR-003**: Varios filtros DEBEN combinarse: una entrada aparece solo si cumple todos.
- **FR-004**: La consulta DEBE responder por páginas, con el total de entradas que cumplen el filtro, para que nunca devuelva la bitácora entera de una vez.
- **FR-005**: Cada entrada DEBE mostrarse con quién, la acción, cuándo, el servicio de origen, el periodo, el objeto, el valor anterior y el valor nuevo, tal como se registraron.
- **FR-006**: La consulta NO DEBE modificar la bitácora: es solo de lectura.

**Quién puede**

- **FR-007**: Solo los roles con lectura de `BITACORA` en la matriz D-16 (hoy el Vicerrector académico y el Administrador del sistema) DEBEN poder consultarla. Cualquier otro recibe `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403), sin saber qué permiso le falta (ADR-021).
- **FR-008**: El permiso DEBE validarse en el backend; ocultar la opción en el menú no basta (principio IX).
- **FR-009**: La pantalla DEBE mostrar la opción de la bitácora solo a quien tiene el permiso.

**Validación de los filtros**

- **FR-010**: Cada filtro DEBE validarse según su definición, en la pantalla mientras se escribe y antes de enviar, y en la API (principio XIV):
  - **Usuario:** de 1 a 64 caracteres, como el nombre de usuario de Usuarios.
  - **Periodo:** `^[A-Za-z0-9._-]{1,20}$`, como el periodo de la entrada.
  - **Fechas:** fechas válidas, y la de inicio no posterior a la de fin.
  - **Servicio:** uno de `generacion`, `academico`, `docentes`, `normativa`, `roles` y `validacionocs`.
  - **Acción:** `^[A-Z][A-Z_]{2,39}$`, como la acción de la entrada, y solo junto con su servicio. La pantalla la elige de la lista de acciones que hay en la bitácora para ese servicio.
  - **Identificador:** de 1 a 64 caracteres: letras sin tilde, dígitos, punto, guion, guion bajo y barra (`^[0-9A-Za-z._/-]{1,64}$`). Cubre los identificadores de los contratos de los seis servicios; el más largo es la huella de un archivo (64) y la barra la usa el objeto de una inconsistencia (research §4).
  - **Página y tamaño:** enteros, la página desde 1 y el tamaño en un rango fijo.
- **FR-011**: Cada error nuevo DEBE estar en el catálogo con sus 8 columnas (principio XIII).

**Entradas que no cambian nada**

- **FR-012**: Un guardado que deja el elemento con los mismos datos que tenía NO DEBE guardar nada ni dejar entrada, ni en el registro de cambios del servicio ni en la bitácora (D-94). Cuentan todos los datos del negocio del elemento, también los que la entrada no lleva (el texto de una justificación, los datos personales de un docente, D-91); no cuentan quién lo guardó ni cuándo. Aplica a todos los servicios del alcance de F17-H01 (D-90). Las acciones que crean, que quitan o que son un acto (consolidar, adjuntar el informe sustentado, cargar la configuración del flujo) registran siempre.
- **FR-013**: Un guardado sin cambios DEBE responder con éxito, sin un error ni un aviso nuevo, y con el elemento tal como quedó guardado (por ejemplo, la actividad con su `asignadaEn` y su `asignadaPor` originales).
- **FR-014**: Las entradas sin cambio que ya están en la bitácora DEBEN quedar como están, porque la bitácora no se puede modificar ni borrar (F17-H01, FR-009 y FR-010).

### Key Entities

- **Entrada de la bitácora** (`EntradaBitacora`, glosario §7): la de F17-H01, sin cambios. Quién, qué, cuándo, el objeto, el valor anterior y el nuevo, el servicio de origen y el periodo.
- **Filtro de la bitácora**: los criterios de una consulta (fechas, usuario, tipo de cambio, identificador y periodo). No se guarda.
- **Página pedida**: el número de página, su tamaño y el corte que mantiene estables las páginas siguientes. No se guarda.
- **Página de entradas**: las entradas de una página, con el total que cumple el filtro y el corte.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con un filtro por usuario y fecha, el 100 % de las entradas mostradas cumplen el filtro, y ninguna entrada que lo cumple falta, comparado con una consulta de solo lectura sobre la base.
- **SC-002**: Los 8 roles sin lectura de la bitácora reciben el 403 en el 100 % de los intentos, por la API y por la pantalla.
- **SC-003**: Con al menos 10 000 entradas, una consulta filtrada muestra su primera página en menos de 2 segundos.
- **SC-004**: El Vicerrector encuentra quién cambió una actividad dada en un día dado en menos de 1 minuto, desde que abre la bitácora.
- **SC-005**: Ninguna consulta modifica, agrega ni borra una entrada: el número de entradas antes y después de las pruebas de consulta es el mismo.
- **SC-006**: En las pruebas de cada servicio, el 100 % de los guardados sin cambios dejan cero entradas, y el 100 % de los guardados con un cambio dejan exactamente una.

## Assumptions

- **Los roles:** la matriz D-16 no cambia. Si un rol nuevo recibe lectura de `BITACORA`, lo consulta sin otro cambio.
- **«Lo propio»:** la lectura de `BITACORA` de los dos roles es sobre todo (`TODO`), así que no hay filtro por carrera.
- **Hora:** las fechas del filtro y el momento que se muestra están en la hora de Ecuador continental.
- **Orden y página:** de las más recientes a las más antiguas, con un tamaño de página por defecto de 50 y un máximo de 100.
- **Usuario exacto:** el filtro por usuario compara el nombre de usuario completo, sin búsqueda parcial; quien audita conoce el usuario.
- **Lo que se muestra:** el nombre de usuario, no el nombre completo, porque es lo que guarda la entrada.
- **Sin carga histórica:** la consulta muestra lo que F17-H01 registró desde su despliegue, incluidas las pocas entradas sin cambio anteriores a esta historia (FR-014).
- **Igualdad:** «sin cambios» compara los datos del negocio del elemento en su servicio, no el valor anterior y el nuevo de la entrada: quién y cuándo no cuentan, y un dato que la entrada no lleva sí cuenta (D-94).
- **Supuesto de F17-H01 reemplazado:** su supuesto «Mismo valor» («la bitácora recibe lo que cada servicio ya considera un cambio, sin una regla nueva») deja de valer: la regla nueva es FR-012.

## Out of Scope

- Exportar la bitácora a un archivo o imprimirla (la ERS no lo pide como Must).
- Firmar o sellar la bitácora.
- Registrar en la bitácora quién la consultó.
- Registrar acciones nuevas (firmas, transiciones de etapa, congelamientos, publicación): cada historia las agrega (F12-H02, F13, F14, F15, F18).
- Búsqueda de texto libre dentro de los valores anterior y nuevo.
- Cambiar qué guarda una entrada o los datos personales que se omiten (D-91).
