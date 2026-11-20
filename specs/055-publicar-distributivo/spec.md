# Feature Specification: Publicar el distributivo aprobado

**Feature Branch**: `055-publicar-distributivo`

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "F18-H01"

**Origen**: issue #71 · historia (`tipo:historia`, `prioridad:must`, `RF-40`, `RF-41`) · feature F18 (#17) · milestone «Ola 3 · Aprobado y firmado» · servicio 06-validacion-ocs · tamaño M. Depende de F15-H01 (#66) y F16-H01 (#67), las dos terminadas. F18-H02 (#72, la reforma) depende de esta historia. Es el criterio de cierre de la ola 3 y del MVP: tres firmas válidas sobre la versión congelada y el distributivo publicado.

**Historia (tal como está en el issue)**: Como Secretaría general académica, quiero publicar el distributivo aprobado, para que la versión quede bloqueada y los docentes sepan que está publicada.

**Requerimientos que cubre**:
- **RF-40:** bloquear la versión aprobada y firmada; cualquier cambio posterior es una reforma con un nuevo ciclo de aprobación. Su criterio en la ERS: «No hay acciones de edición disponibles sobre una versión firmada.»
- **RF-41:** notificaciones internas a los involucrados en cada cambio de estado, incluido «publicado».
- **MD-11:** publicar bloquea la versión y notifica. El CSV para SIGA se genera a pedido de la Secretaría sobre la versión publicada (F19). Publicación y consulta solo lee la versión publicada; no la publica.
- **Glosario:** «Publicar: pasar a PUBLICADO: la versión queda bloqueada y se notifica a los docentes, a Talento Humano y a las coordinaciones» (`publicar()`, Aprobación); el evento `DistributivoPublicado`.

**Decisiones que aplica**:
- D-95: las transiciones las registra Aprobación.
- D-100 y D-101: la versión se congela al aprobar el contenido, y sobre ella se firma.
- D-104: la cadena de firmas completa lleva a EN_OCS.
- D-107: registrar la resolución del OCS pasa a APROBADO; después no se corrige.
- D-108 y D-110: las notificaciones de cada transición, por rol o por persona.
- D-16 y D-63: los permisos de la matriz y los diez roles. La Secretaría general académica tiene «Publicación: aprobación».

**Principios de la constitución**: II, III, IV, V, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- El distributivo llega a APROBADO cuando la Secretaría general académica registra la resolución del OCS (F15-H01). Antes pasó por la cadena de firmas completa: F1, F2 y F3 válidas sobre la versión congelada.
- La versión congelada ya no admite cambios desde que el Vicerrector aprobó el contenido (D-100).
- En APROBADO no hay ninguna acción: el flujo se detiene ahí. PUBLICADO es la última etapa del esqueleto fijo (ADR-007), y nadie llega a ella.
- La notificación de PUBLICADO de F16-H01 iría al rol de la etapa, que es la Secretaría, la misma que publicaría. Su enlace es provisorio hasta esta historia.
- Talento Humano es el rol OPERADOR_DATOS («Talento Humano (operador de datos)»); las coordinaciones, COORDINADOR_CARRERA.

Esta historia agrega:
- **Publicar:** la Secretaría general académica pasa el distributivo de APROBADO a PUBLICADO, y la versión queda bloqueada como la publicada.
- **La notificación de la publicación:** a los docentes, a Talento Humano y a las coordinaciones.
- **La versión publicada, sin acciones de edición:** quien la abre ve que está publicada y no puede cambiarla.

## Clarifications

### Session 2026-11-18

- Q: Al publicar, «los docentes» reciben una notificación. ¿A qué docentes se refiere? → A: A todas las cuentas activas con el rol DOCENTE, como en F16-H01, junto con las de OPERADOR_DATOS (Talento Humano) y COORDINADOR_CARRERA. Notifica aunque un docente no tenga carga en este periodo.
- Q: ¿Dónde publica la Secretaría general académica? → A: En «Resolución del OCS», su pantalla, donde registró la resolución. En APROBADO aparece «Publicar», con una confirmación; después, la pantalla muestra la constancia de la publicación: quién y cuándo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - La Secretaría publica el distributivo aprobado (Priority: P1)

Como Secretaría general académica, quiero publicar el distributivo aprobado, para que la versión quede bloqueada y sea la oficial del periodo.

**Why this priority**: es el primer escenario del issue y cierra el flujo de aprobación: sin publicar, el distributivo se queda en APROBADO.

**Independent Test**:
1. Con el distributivo en APROBADO y la resolución registrada, `secretaria.demo` abre «Resolución del OCS».
2. Pulsa «Publicar» y confirma.
3. El distributivo queda en PUBLICADO, con quién publicó y cuándo.

**Acceptance Scenarios**:

1. **Dado** el distributivo en APROBADO, con F1, F2 y F3 válidas y la resolución registrada, **cuando** lo publico, **entonces** pasa a PUBLICADO y la versión queda bloqueada. *(issue)*
2. **Dado** el distributivo en EN_OCS, **cuando** intento publicarlo, **entonces** se rechaza con un error 409. *(issue)*
3. **Dado** el distributivo en cualquier etapa que no es APROBADO, incluida PUBLICADO, **cuando** intento publicarlo, **entonces** se rechaza con un 409 del catálogo que dice por qué, y nada cambia.
4. **Dado** un usuario sin el rol de la etapa APROBADO, por ejemplo el Vicerrector, **cuando** intenta publicar, **entonces** recibe el 403 común y nada cambia.
5. **Dado** el distributivo en APROBADO, **cuando** la Secretaría abre «Resolución del OCS», **entonces** ve la acción «Publicar»; al pulsarla, una confirmación dice que la versión quedará bloqueada y que se notificará a los docentes, a Talento Humano y a las coordinaciones.
6. **Dado** el distributivo publicado, **cuando** cualquiera que lee el estado del flujo lo abre, **entonces** ve la constancia de la publicación: la versión, quién y cuándo.
7. **Dado** una publicación, **cuando** se registra, **entonces** queda una entrada en la bitácora con quién publicó, cuándo, el periodo y la versión.
8. **Dado** dos publicaciones a la vez del mismo distributivo, **cuando** se registran, **entonces** solo una pasa a PUBLICADO; la otra recibe el 409 de la etapa.

---

### User Story 2 - Los docentes, Talento Humano y las coordinaciones reciben la notificación (Priority: P1)

Como docente, quiero recibir en mi bandeja una notificación cuando el distributivo se publica, para saber que ya es el oficial.

**Why this priority**: es la segunda parte del primer escenario del issue y la razón de la historia para los docentes (RF-41).

**Independent Test**:
1. `secretaria.demo` publica el distributivo.
2. `docente.demo`, el operador de datos y `coordinacion.demo` ven en su campana «El distributivo de 2026-I fue publicado», con la versión y quién lo publicó.
3. La Secretaría que publicó no la recibe.

**Acceptance Scenarios**:

1. **Dado** la publicación, **cuando** se completa, **entonces** los docentes, Talento Humano y las coordinaciones reciben una notificación interna. *(issue)*
2. **Dado** la notificación de la publicación, **cuando** el destinatario la lee, **entonces** dice que el distributivo se publicó, el periodo, la versión, quién lo publicó y cuándo.
3. **Dado** un usuario con más de uno de esos roles, **cuando** se publica, **entonces** recibe una sola notificación.
4. **Dado** la Secretaría que publicó, **cuando** se publica, **entonces** no recibe su propia notificación, aunque tenga además uno de esos roles.
5. **Dado** un usuario desactivado con uno de esos roles, **cuando** se publica, **entonces** no la recibe.
6. **Dado** el proceso de identidad detenido, **cuando** se publica, **entonces** la publicación se completa sin error para la Secretaría, y la notificación llega cuando el proceso vuelve, una sola vez.

---

### User Story 3 - La versión publicada no se puede editar (Priority: P1)

Como cualquier usuario, quiero que la versión publicada no ofrezca acciones de edición, para que nadie la cambie fuera de una reforma.

**Why this priority**: es el tercer escenario del issue y el criterio de RF-40.

**Independent Test**:
1. Con el distributivo publicado, cada rol de la demo abre las pantallas del distributivo.
2. Ninguna ofrece una acción que cambie la versión publicada, y las operaciones de cambio responden con un error.

**Acceptance Scenarios**:

1. **Dado** la versión publicada, **cuando** cualquier usuario la abre, **entonces** no hay acciones de edición disponibles. *(issue)*
2. **Dado** la versión publicada, **cuando** se abre «Consolidado del periodo», **entonces** dice que la versión está publicada y no se puede cambiar; no ofrece consolidar, adjuntar el informe, justificar ni aprobar.
3. **Dado** el distributivo en PUBLICADO, **cuando** cualquiera consulta el estado del flujo, **entonces** no hay ninguna acción disponible, para ningún rol.
4. **Dado** la versión publicada, **cuando** alguien intenta cambiarla por la API (proponer un docente, una actividad, consolidar, justificar, enviar, devolver o aprobar), **entonces** recibe el error del catálogo que ya existe para una versión congelada o una etapa que no lo admite, y nada cambia.

---

### Edge Cases

- **Publicar sin resolución:** no puede pasar, porque a APROBADO solo se llega registrando la resolución (D-107). Si el distributivo no está en APROBADO, es el 409 de la etapa.
- **Un rol sin usuarios activos** (por ejemplo, ningún operador de datos): la publicación se completa y ese rol no recibe nada. No es un error.
- **Docentes sin cuenta:** solo reciben la notificación las cuentas activas con el rol DOCENTE; un docente sin cuenta no tiene bandeja.
- **Un docente sin carga en el periodo:** recibe la notificación igual, porque el destino es el rol (aclaración del 2026-11-18).
- **La Secretaría con otro rol** que también se notifica: no recibe su propia notificación (FR-002 d de F16-H01).
- **Dos secretarías:** la que no publicó tampoco recibe una notificación, porque el destino son los tres roles de la historia, no el rol de la etapa.
- **La versión de un rechazo en firma (F12-H04):** solo se publica la versión vigente, la que llegó a APROBADO; las anuladas no cambian.
- **Volver a publicar:** el distributivo ya está en PUBLICADO, así que es el 409 de la etapa.
- **Cambiar después de publicar:** queda para la reforma (F18-H02). Esta historia no la ofrece.

## Requirements *(mandatory)*

### Functional Requirements

**Publicar**

- **FR-001**: El sistema DEBE permitir publicar el distributivo de un periodo solo cuando está en APROBADO, y solo a quien tiene el permiso «Publicación: aprobación» de la matriz D-16 y el rol de la etapa APROBADO en la configuración del periodo, que por defecto es la Secretaría general académica.
- **FR-002**: Publicar DEBE pasar el distributivo de APROBADO a PUBLICADO, con quién publicó, cuándo y la versión publicada, que es la vigente.
- **FR-003**: Publicar en otra etapa DEBE rechazarse con un 409 de un código del catálogo que diga por qué, y no cambiar nada. Un usuario sin el rol de la etapa DEBE recibir el 403 común.
- **FR-004**: Dos publicaciones a la vez del mismo distributivo DEBEN dejar una sola transición a PUBLICADO.
- **FR-005**: Cada publicación DEBE dejar una entrada en la bitácora con quién, cuándo, el periodo y la versión.
- **FR-006**: El estado del flujo DEBE mostrar la constancia de la publicación (la versión, quién y cuándo) a quien lo lee, y la acción «Publicar» solo a quien puede publicar.

**Notificar**

- **FR-007**: La publicación DEBE notificar a las cuentas activas con los roles DOCENTE, OPERADOR_DATOS (Talento Humano) y COORDINADOR_CARRERA, con una sola notificación por usuario aunque tenga varios de esos roles, y nunca a quien publicó.
- **FR-008**: La notificación DEBE decir que el distributivo se publicó, el periodo, la versión, quién lo publicó y cuándo.
- **FR-009**: La notificación DEBE generarse si la publicación se guardó, y solo en ese caso. Que Notificaciones no responda NO DEBE impedir ni demorar la publicación; la notificación llega cuando vuelve, una sola vez.

**Bloquear**

- **FR-010**: Con el distributivo en PUBLICADO, el estado del flujo NO DEBE ofrecer ninguna acción, y ninguna pantalla DEBE ofrecer una acción que cambie la versión publicada.
- **FR-011**: Las operaciones que cambian el distributivo DEBEN seguir rechazando la versión publicada con los errores del catálogo que ya existen para la versión congelada o la etapa.
- **FR-012**: «Consolidado del periodo» DEBE decir que la versión está publicada y no se puede cambiar.

**Pantalla, seguridad y errores**

- **FR-013**: «Resolución del OCS» DEBE ofrecer «Publicar» en APROBADO a quien puede publicar, con una confirmación que diga que la versión quedará bloqueada y a quiénes se notificará, y mostrar la constancia después.
- **FR-014**: La pantalla y la confirmación DEBEN cumplir WCAG 2.1 AA.
- **FR-015**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII).

### Key Entities

- **Proceso de aprobación** (de Aprobación, existente): su etapa pasa de APROBADO a PUBLICADO.
- **Transición de publicación** (existente, `PASO_DE_ETAPA` hacia PUBLICADO): quién, cuándo y la versión publicada.
- **Versión publicada**: la versión congelada que llegó a APROBADO. Ya es inmutable; desde ahora es la oficial del periodo, la que leerán la consulta y las exportaciones (F19) y la base de una reforma (F18-H02).
- **Notificación** (existente, F16-H01): una por destinatario, con el destino por rol.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En un recorrido que lleva el distributivo hasta APROBADO, la Secretaría lo publica y queda en PUBLICADO con la constancia; publicar otra vez o desde otra etapa se rechaza en el 100 % de los casos sin cambiar nada. Se comprueba en un script de punta a punta.
- **SC-002**: Después de publicar, cada cuenta activa con DOCENTE, OPERADOR_DATOS o COORDINADOR_CARRERA tiene exactamente una notificación de la publicación, y nadie más la tiene.
- **SC-003**: Con el proceso de identidad detenido al publicar, la publicación se completa sin error, y cada destinatario tiene la notificación una sola vez antes de 2 minutos desde que el proceso vuelve.
- **SC-004**: Con el distributivo publicado, ningún rol de la demo ve una acción de edición, y cada operación de cambio probada responde con su error.
- **SC-005**: La Secretaría publica desde «Resolución del OCS» en menos de 30 segundos, confirmación incluida.
- **SC-006**: La pantalla y la confirmación no tienen fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **Quién publica:** el rol de la etapa APROBADO en la configuración del flujo del periodo, como en las demás etapas. Por defecto es la Secretaría general académica, que tiene «Publicación: aprobación» en la matriz D-16. Hacen falta las dos cosas, el permiso y el rol, como al registrar la resolución (D-107). Un rol de la etapa sin el permiso recibe el 403 común; se corrige en la matriz de roles.
- **Las condiciones:** estar en APROBADO ya implica la cadena de firmas completa (D-104) y la resolución registrada (D-107). No se comprueban otra vez.
- **El enlace de la notificación:** por rol (recorrido del autor, 2026-11-19): las coordinaciones, a «Oferta y propuesta de docentes» (`/oferta`), cuya tarjeta del flujo muestra la constancia; los docentes y Talento Humano, al inicio (`/`), porque no pueden abrir otra pantalla del distributivo hasta F19.
- **El estado del periodo:** publicar no cambia el estado del periodo académico (Planificación, Vigente, Cerrado); eso es de Académico.
- **Fuera de alcance:**
  - el CSV para SIGA, su hash y el registro de la exportación (F19, MD-11);
  - la consulta del distributivo publicado y el acuse de recibo del docente (F19, RF-51);
  - la reforma de la versión publicada (F18-H02);
  - la votación del OCS y la remisión zonal.
