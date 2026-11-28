# Feature Specification: Validar antes de exportar y registrar la exportación

**Feature Branch**: `061-exportacion-siga`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "F19-H02"

**Origen**: issue #267 · historia (`tipo:historia`, `prioridad:must`, `RF-53`, `RF-44`, `modulo:08-reporting`) · feature F19 (Obtener el CSV para SIGA, #258) · milestone «Ola 4 · Publicado» · Ola del secuenciador: Incremento 1 · servicio 08-reporting · tamaño S (13 h, D-119) · persona: Gabriela Cevallos. Depende de F19-H01 (#266, el CSV para SIGA, spec 060) y de F17-H01 (#69, la bitácora), las dos terminadas. Es la última historia de F19.

**Historia (tal como está en el issue)**: Como Secretaria general académica, quiero que el CSV solo se exporte de una versión firmada y sin inconsistencias, con su suma de verificación registrada, para que lo que llega a SIGA sea la versión oficial y se pueda verificar.

**Requerimientos que cubre**:
- **RF-53:** validar antes de exportar (distributivo firmado, sin inconsistencias), calcular una suma de verificación del archivo y registrar la exportación (usuario, fecha y versión). Su criterio en la ERS: «No se puede exportar una versión que no esté firmada.»
- **RF-44:** la bitácora de auditoría (quién, qué, cuándo, valor anterior y valor nuevo), que no se puede borrar.
- **RNF-INT-01:** el registro de la exportación es transaccional; no quedan registros a medias.
- **Glosario:** «Exportación a SIGA: el CSV por docente de la versión publicada, con su suma de verificación y su registro (usuario, fecha y versión)». «CSV para SIGA: el archivo de la versión publicada con el layout de exportación vigente […]. Con su suma de verificación y su registro es la Exportación a SIGA.» «Hash: SHA-256 del documento.» «Inconsistencia: violación de una regla. Es dura o blanda, de una asignación o global, y está abierta o justificada.» «Paso de la cadena: orden, rol, etiqueta, usuario asignado y estado (PENDIENTE, FIRMADO o ANULADO).» «Bitácora».

**Decisiones que aplica**:
- MD-11: el CSV para SIGA se genera a pedido de la Secretaría sobre la versión publicada, y cada exportación queda registrada; su suma de verificación se registra (paso 9 de la ERS 4).
- MD-07: una inconsistencia blanda justificada deja de estar abierta; «sin inconsistencias» es 0 duras y 0 blandas sin justificar.
- D-113: durante una reforma, la versión publicada anterior sigue vigente, con sus firmas; no se anula.
- D-119 (F19-H01): el CSV para SIGA, su layout de exportación, su comprobación contra el reporte general y la entrada `CSV_SIGA_GENERADO` de la bitácora antes de entregar.
- D-16: la Secretaría general académica tiene «Publicación: aprobación».

**Principios de la constitución**: I, III, IV, V, VII, VIII, IX, X, XIII y XIV.

**Punto de partida.**
- La Secretaría genera el CSV para SIGA de la versión publicada vigente del periodo, con el layout de exportación vigente (F19-H01). Un periodo sin versión publicada responde 409 `REPORTING_SIN_PUBLICACION`.
- Antes de entregarlo, el sistema comprueba que el contenido congelado coincide con el hash de la versión publicada y que los totales son los del reporte general; si no, no lo entrega.
- Cada CSV generado deja una entrada `CSV_SIGA_GENERADO` en la bitácora, con quién, cuándo, el periodo, la versión y la versión del layout, antes de entregarse.
- El archivo no tiene suma de verificación, no queda un registro de la exportación que se pueda consultar, y no se comprueba de forma explícita que la versión tenga sus firmas completas ni que no tenga inconsistencias abiertas: se da por hecho porque llegó a PUBLICADO.

Esta historia agrega:
- **La validación antes de exportar:** la versión publicada vigente tiene su cadena de firmas completa sobre el documento de esa misma versión y no tiene inconsistencias abiertas. Si no, el CSV no se exporta.
- **La suma de verificación:** el SHA-256 del archivo entregado, que la Secretaría ve y que cualquiera puede recalcular sobre el archivo.
- **El registro de la exportación** (la Exportación a SIGA del glosario): quién, cuándo, el periodo, la versión, la versión del layout y la suma de verificación, que no se edita ni se borra, consultable en la pantalla y anotado en la bitácora.

## Clarifications

### Session 2026-11-22

- Q: Para verificar después que un archivo es el que se exportó, ¿basta con recalcular su SHA-256 fuera del sistema, o hace falta una pantalla para subir el archivo? → A: Se recalcula fuera del sistema, sin pantalla para subirlo; la pantalla explica en una línea cómo recalcular el SHA-256 en Windows y en Linux.
- Q: ¿La pantalla «Exportación a SIGA» debe avisar que una hoja de cálculo lee la cédula como número y pierde el cero inicial? → A: Sí, junto a «Generar CSV para SIGA»: el archivo va a SIGA tal como sale; si se abre en una hoja de cálculo, hay que importar la cédula como texto y no volver a guardarlo, porque deja de coincidir con su suma de verificación.
- Q: Si se intenta exportar una versión publicada sin todas sus firmas o con inconsistencias abiertas, ¿ese intento rechazado debe quedar en la bitácora? → A: Sí, solo ese rechazo, con una entrada propia (quién, cuándo, el periodo, la versión y qué condición falló), sin registro de exportación; el 409 de «sin publicación» sigue sin entrada.
- Q: Antes de pulsar «Generar CSV para SIGA», ¿la pantalla debe mostrar si la versión está lista para exportarse, o basta con validar al exportar? → A: Basta con validar al exportar; si falla, la pantalla muestra el rechazo con la condición que falla y qué hacer. No hay un indicador previo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Solo se exporta una versión firmada y sin inconsistencias (Priority: P1)

Como Secretaria general académica, quiero que el sistema compruebe, antes de exportar, que la versión está publicada, firmada por toda la cadena y sin inconsistencias abiertas, para que a SIGA llegue solo la versión oficial.

**Why this priority**: es el primer escenario del issue y el criterio de RF-53 («No se puede exportar una versión que no esté firmada»). Sin la validación, registrar la exportación no garantiza que sea la oficial.

**Independent Test**:
1. Con 2026-I en EN_OCS (firmada, pero no publicada), `secretaria.demo` pide el CSV para SIGA por la API: se rechaza y no queda registro ni entrada en la bitácora.
2. Con 2026-I publicada, en un entorno de prueba se deja un paso de la cadena de su documento sin firmar (o una inconsistencia abierta en la versión) y se pide el CSV: se rechaza con un mensaje que dice qué condición falla, sin registro de exportación, y la bitácora tiene una entrada del rechazo con esa condición.
3. Con 2026-I publicada, firmada y sin inconsistencias, se pide el CSV: se exporta.

**Acceptance Scenarios**:

1. **Dado** un distributivo cuya versión no está firmada ni publicada, **cuando** intento exportar el CSV, **entonces** el sistema lo rechaza. *(issue)*
2. **Dado** un distributivo firmado por toda la cadena que todavía no se publicó (en EN_OCS o APROBADO), **cuando** intento exportar el CSV, **entonces** se rechaza con el 409 `REPORTING_SIN_PUBLICACION`, como en F19-H01: solo se exporta la versión publicada vigente.
3. **Dada** la versión publicada vigente con un paso de su cadena de firmas sin firmar (PENDIENTE o RECHAZADO), o con sus firmas anuladas, **cuando** intento exportar el CSV, **entonces** se rechaza con un error del catálogo que dice que la versión no tiene todas sus firmas; no se genera ni se entrega nada, no queda registro de exportación, y queda una entrada del rechazo en la bitácora (FR-016).
4. **Dada** la versión publicada vigente cuyo documento firmado se generó de otra versión congelada (el hash de la versión del documento no es el de la versión publicada), **cuando** intento exportar el CSV, **entonces** se rechaza igual que en el escenario 3.
5. **Dada** la versión publicada vigente con una inconsistencia abierta (una dura, o una blanda sin justificar), **cuando** intento exportar el CSV, **entonces** se rechaza con un error del catálogo que dice que la versión tiene inconsistencias abiertas y cuántas; no se entrega nada, no queda registro de exportación, y queda una entrada del rechazo en la bitácora (FR-016).
6. **Dada** la versión publicada vigente con todas sus firmas y con inconsistencias blandas justificadas, **cuando** exporto el CSV, **entonces** se exporta: una blanda justificada no está abierta (MD-07).
7. **Dado** el distributivo en reforma, **cuando** exporto el CSV, **entonces** se valida y se exporta la versión publicada vigente, la anterior a la reforma, con sus firmas; la versión en curso de la reforma no cuenta.
8. **Dada** una exportación rechazada, **cuando** la Secretaría la ve en la pantalla, **entonces** el mensaje dice qué condición falla y qué hacer, sin datos técnicos.

---

### User Story 2 - Cada exportación queda registrada con su suma de verificación (Priority: P1)

Como Secretaria general académica, quiero que cada exportación quede registrada con quién la hizo, cuándo, la versión y la suma de verificación del archivo, y que quede en la bitácora, para probar qué archivo se entregó a SIGA y verificarlo después.

**Why this priority**: es el segundo escenario del issue y la otra mitad de RF-53. Sin la suma de verificación registrada no se puede probar que el archivo cargado en SIGA es el que generó el sistema.

**Independent Test**:
1. Con 2026-I publicada, `secretaria.demo` exporta el CSV para SIGA desde la pantalla.
2. La pantalla muestra la suma de verificación del archivo; se recalcula sobre el archivo descargado con una herramienta estándar de SHA-256 y es la misma.
3. En «Exportaciones del periodo» aparece la exportación con el usuario, la fecha y la hora, la versión, la versión del layout y la suma de verificación; en la bitácora hay una entrada de esa exportación con los mismos datos.

**Acceptance Scenarios**:

1. **Dado** el distributivo publicado, **cuando** exporto el CSV, **entonces** queda registrada la exportación con el usuario, la fecha, la versión y la suma de verificación del archivo, y la exportación queda en la bitácora. *(issue)*
2. **Dado** el CSV exportado, **cuando** calculo el SHA-256 del archivo descargado, **entonces** es igual a la suma de verificación que mostró la pantalla y a la del registro de la exportación.
3. **Dada** la misma versión publicada y el mismo layout de exportación, **cuando** exporto dos veces, **entonces** quedan dos registros, cada uno con su usuario y su momento, y con la misma suma de verificación, porque el archivo es idéntico byte a byte (F19-H01).
4. **Dado** un layout de exportación nuevo cargado entre dos exportaciones, **cuando** comparo sus registros, **entonces** cada uno dice con qué versión del layout salió, y sus sumas de verificación son distintas.
5. **Dado** que el registro de la exportación o su entrada en la bitácora no se pueden guardar, **cuando** exporto, **entonces** el archivo no se entrega y no queda ni el registro ni la entrada: se guardan los dos juntos o ninguno.
6. **Dado** un registro de exportación, **cuando** cualquier usuario intenta modificarlo o borrarlo, **entonces** no existe ninguna operación que lo permita.
7. **Dadas** las exportaciones de un periodo, **cuando** la Secretaría abre «Exportación a SIGA», **entonces** ve la lista de exportaciones del periodo elegido, de la más reciente a la más antigua, con quién, cuándo, la versión, la versión del layout, el nombre del archivo y la suma de verificación.
8. **Dada** una exportación, **cuando** se consulta la bitácora, **entonces** hay una sola entrada de esa exportación, con quién, cuándo, el periodo, la versión, la versión del layout y la suma de verificación: no una entrada del CSV generado y otra de la exportación.
9. **Dado** un usuario sin «Publicación: aprobación», **cuando** pide la lista de exportaciones, **entonces** recibe el 403 común, y la pantalla no le ofrece la opción.

---

### Edge Cases

- **Una versión publicada antes de esta historia** (por ejemplo, con el contenido congelado sin el tipo de identificación): se valida y se exporta igual; la suma de verificación es la del archivo tal como sale, con sus «NA».
- **La conexión se corta después de registrar y antes de que el archivo llegue:** el registro queda, porque el archivo se generó y su suma de verificación es conocida. Exportar otra vez da el mismo archivo y la misma suma de verificación (FR-007 de F19-H01), con un registro más.
- **El proceso que da las firmas o las inconsistencias no responde:** no se exporta, la pantalla lo dice con el error del catálogo que ya usan los reportes para un proceso que no responde, y no queda registro ni entrada.
- **La versión tiene firmas completas, pero su contenido no coincide con su hash, o los totales no cuadran con el reporte general:** se rechaza como en F19-H01 (`REPORTING_CONTENIDO_INCONSISTENTE`), sin registro ni entrada.
- **Se cumplen varias condiciones de rechazo a la vez** (por ejemplo, faltan firmas y hay inconsistencias abiertas): el mensaje dice todas las que fallan, no solo la primera.
- **Una exportación rechazada:** no es una exportación y no deja registro de exportación. Solo el rechazo por firmas incompletas o inconsistencias abiertas deja una entrada propia en la bitácora (FR-016); el 409 de un periodo sin publicación, el proceso que no responde y el contenido inconsistente de F19-H01 no la dejan.
- **No se puede guardar la entrada del rechazo:** la exportación se rechaza igual, con el mismo error; el rechazo nunca se convierte en una exportación.
- **La cadena de firmas de la versión tiene un solo paso**, porque así lo dice la configuración del flujo del periodo: basta con que ese paso esté FIRMADO.
- **Una inconsistencia que apareció después de publicar** (por ejemplo, una regla que cambió): no cuenta; la versión publicada está congelada y se valida con las inconsistencias de esa versión, no con una evaluación nueva.
- **Un archivo abierto y vuelto a guardar en una hoja de cálculo:** sus bytes cambian (y la cédula puede perder el cero inicial), así que su SHA-256 ya no es el registrado; la verificación lo detecta, y la pantalla lo advierte antes de exportar (FR-019a).
- **Un periodo sin exportaciones:** la lista dice que todavía no se exportó el CSV para SIGA de ese periodo.
- **Una versión publicada sin el registro de sus inconsistencias** (congelada antes de esta historia): no se exporta; el mensaje dice que no se registraron sus inconsistencias al congelarla, y queda la entrada del rechazo (FR-003, FR-016).
- **Dos exportaciones del mismo periodo casi al mismo tiempo:** quedan las dos registradas, cada una con su momento, sin perder ninguna.

## Requirements *(mandatory)*

### Functional Requirements

**La validación antes de exportar**

- **FR-001**: Antes de generar el CSV para SIGA, el sistema DEBE validar que la versión a exportar es la versión publicada vigente del periodo (como en F19-H01), que está firmada y que no tiene inconsistencias abiertas. Si alguna condición no se cumple, NO DEBE generar ni entregar el archivo, ni registrar una exportación.
- **FR-002**: «Firmada» DEBE significar que el documento firmable de la versión publicada vigente se generó de esa versión (el hash de la versión congelada del documento es el de la versión publicada), que todos los pasos de su cadena de firmas están FIRMADOS (ninguno PENDIENTE ni RECHAZADO) y que sus firmas no están anuladas.
- **FR-003**: «Sin inconsistencias» DEBE significar que la versión publicada vigente tiene 0 inconsistencias duras y 0 blandas sin justificar (MD-07), según las inconsistencias de esa versión que se registran al congelarla, sin volver a evaluarla con las reglas de hoy. Si la versión no tiene ese registro (por ejemplo, se congeló antes de esta historia), la condición NO se cumple: solo se exporta lo que se pudo comprobar (D-120).
- **FR-004**: Un periodo sin versión publicada DEBE seguir respondiendo el 409 `REPORTING_SIN_PUBLICACION`, aunque su versión esté firmada (en EN_OCS o APROBADO).
- **FR-005**: Una versión publicada sin todas sus firmas, o con inconsistencias abiertas, DEBE rechazarse con un error del catálogo que diga qué condiciones fallan (las firmas, las inconsistencias o las dos) y, para las inconsistencias, cuántas hay abiertas o que no se registraron. El mensaje NO DEBE mostrar datos técnicos.
- **FR-006**: Las comprobaciones de F19-H01 (el contenido coincide con el hash de la versión y los totales son los del reporte general) DEBEN seguir aplicándose, después de la validación.
- **FR-007**: Si el proceso del que se leen las firmas o las inconsistencias no responde, el CSV NO DEBE exportarse y DEBE responder con el error del catálogo para un proceso que no responde, sin registro ni entrada en la bitácora.
- **FR-008**: Durante una reforma, la validación DEBE aplicarse a la versión publicada vigente, la anterior a la reforma (D-113).

**La suma de verificación**

- **FR-009**: El sistema DEBE calcular la suma de verificación del archivo como el SHA-256 de los bytes exactos que entrega, en hexadecimal en minúsculas (64 caracteres), igual que el hash del glosario.
- **FR-010**: La suma de verificación DEBE entregarse junto con el archivo, para que quien lo descarga por la API la pueda comparar.
- **FR-011**: Cualquiera DEBE poder verificar el archivo recalculando su SHA-256 con una herramienta estándar y comparándolo con el registro, sin el sistema. La pantalla DEBE explicar, en una línea junto a la suma de verificación, cómo recalcularla en Windows y en Linux con las herramientas que traen (aclaración del 2026-11-22). El sistema no ofrece una pantalla para subir un archivo y compararlo.

**El registro de la exportación**

- **FR-012**: Cada exportación DEBE quedar registrada como una Exportación a SIGA con: quién la hizo, cuándo, el periodo, la versión publicada, el hash de la versión, la versión del layout de exportación, el nombre del archivo, el número de filas y la suma de verificación.
- **FR-013**: El registro y su entrada en la bitácora DEBEN guardarse juntos, en una sola transacción, antes de entregar el archivo. Si no se pueden guardar, el archivo NO DEBE entregarse y no DEBE quedar ninguno de los dos.
- **FR-014**: Cada exportación DEBE dejar una sola entrada en la bitácora, con quién, cuándo, el periodo, la versión, la versión del layout y la suma de verificación. Es la entrada de cada CSV generado de F19-H01, que desde esta historia lleva también la suma de verificación; no se agrega una segunda entrada por la misma exportación.
- **FR-015**: Los registros de exportación NO DEBEN poder modificarse ni borrarse, por ninguna operación.
- **FR-016**: Una exportación rechazada NO DEBE dejar registro de exportación. El rechazo por firmas incompletas o por inconsistencias abiertas (FR-005) DEBE dejar una entrada propia en la bitácora, distinta de la de una exportación, con quién, cuándo, el periodo, la versión y qué condición falló (y cuántas inconsistencias abiertas hay). Los demás rechazos (el 409 de un periodo sin publicación, un proceso que no responde, el contenido inconsistente) NO DEBEN dejar entrada (aclaración del 2026-11-22). Si la entrada del rechazo no se puede guardar, la exportación se rechaza igual.

**Acceso y pantalla**

- **FR-017**: Solo quien tiene «Publicación: aprobación» (D-16), es decir, la Secretaría general académica, DEBE poder exportar el CSV para SIGA y consultar las exportaciones. Cualquier otro usuario DEBE recibir el 403 común. La bitácora sigue con sus permisos.
- **FR-018**: La pantalla «Exportación a SIGA» DEBE mostrar, para el periodo elegido, la lista de sus exportaciones, de la más reciente a la más antigua, con quién, cuándo, la versión, la versión del layout, el nombre del archivo y la suma de verificación.
- **FR-019**: Después de exportar, la pantalla DEBE decir que se generó el archivo, con su nombre y su suma de verificación, que se pueda copiar, y la lista DEBE incluir la exportación nueva.
- **FR-019a**: Junto a «Generar CSV para SIGA», la pantalla DEBE avisar que el archivo va a SIGA tal como sale y que, si se abre en una hoja de cálculo, hay que importar la columna de la identificación como texto (para no perder el cero inicial de la cédula) y no volver a guardarlo, porque un archivo guardado de nuevo deja de coincidir con su suma de verificación (aclaración del 2026-11-22; hallazgo del recorrido T038 de F19-H01).
- **FR-020**: La validación DEBE correr al exportar, no al abrir la pantalla ni al elegir el periodo: la pantalla no muestra un estado previo de las firmas ni de las inconsistencias (aclaración del 2026-11-22). Si la exportación se rechaza, la pantalla DEBE mostrar el mensaje del error del catálogo, con qué condición falla y qué hacer.
- **FR-021**: Las pantallas DEBEN cumplir WCAG 2.1 AA.
- **FR-022**: Cada error nuevo DEBE estar en el catálogo, con su clasificación, causa, acción y descripción técnica (principio XIII).
- **FR-023**: Reporting NO DEBE cambiar nada del distributivo, de la versión, de sus firmas ni de sus inconsistencias: solo lee.

### Key Entities

- **Versión publicada** (de Aprobación, existente): la versión congelada vigente del periodo, con su hash. Reporting la lee; no la cambia.
- **Documento firmable y su cadena de firmas** (de Firma, existentes): el PDF/A de la versión congelada, con el hash de esa versión y su hash original, y sus pasos, cada uno PENDIENTE, FIRMADO o RECHAZADO; sus firmas pueden estar anuladas. La validación los lee; no los cambia.
- **Inconsistencias de la versión** (de Distributivo, existentes): duras o blandas, abiertas o justificadas. La validación cuenta las abiertas; no las cambia.
- **Exportación a SIGA** (nueva en Publicación y consulta): el registro de cada CSV para SIGA entregado, con quién, cuándo, el periodo, la versión publicada y su hash, la versión del layout de exportación, el nombre del archivo, el número de filas y la suma de verificación. No se edita ni se borra.
- **Suma de verificación** (nueva, parte de la exportación): el SHA-256 de los bytes del archivo entregado.
- **CSV para SIGA** (existente desde F19-H01): el archivo; no se guarda, porque con la versión y la versión del layout se vuelve a generar idéntico.
- **Entrada de bitácora** (existente): una por exportación, con la suma de verificación, y una por cada rechazo por firmas incompletas o inconsistencias abiertas, con su propia acción.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los intentos de exportar una versión no publicada, sin todas sus firmas o con inconsistencias abiertas se rechazan, sin archivo y sin registro de exportación, probado con un caso por cada condición y uno con varias a la vez; los rechazos por firmas o inconsistencias dejan su entrada de rechazo en la bitácora con la condición que falló, y el de una versión no publicada no deja ninguna.
- **SC-002**: Para el 100 % de las exportaciones de las pruebas, el SHA-256 calculado con una herramienta estándar sobre el archivo descargado es igual al que mostró la pantalla y al del registro.
- **SC-003**: El 100 % de las exportaciones de las pruebas tienen su registro y una sola entrada en la bitácora, con el usuario, la fecha, la versión, la versión del layout y la suma de verificación.
- **SC-004**: Una falla simulada al guardar el registro o su entrada en la bitácora no entrega el archivo ni deja ninguno de los dos, en el 100 % de los casos probados.
- **SC-005**: Dos exportaciones de la misma versión y el mismo layout tienen la misma suma de verificación.
- **SC-006**: No existe ninguna operación, por la pantalla ni por la API, que modifique o borre un registro de exportación.
- **SC-007**: La Secretaría exporta el CSV y tiene su suma de verificación en menos de 1 minuto desde que abre la pantalla; la validación, la suma de verificación y el registro no llevan la exportación de un distributivo de 300 docentes por encima de 10 segundos.
- **SC-008**: Ningún rol sin «Publicación: aprobación» exporta el CSV ni ve las exportaciones, por la pantalla ni por la API.
- **SC-009**: Las pantallas no tienen fallas de accesibilidad WCAG 2.1 AA en la revisión automática.

## Assumptions

- **Qué es «firmada»:** la cadena de firmas completa del documento firmable de la versión publicada vigente (todos sus pasos FIRMADOS) sobre el documento de esa misma versión. En el flujo normal una versión no llega a PUBLICADO sin eso; la validación lo comprueba de forma explícita, como pide RF-53, en lugar de darlo por hecho. La resolución del OCS no se valida aparte: ya la exige APROBADO.
- **Qué es «sin inconsistencias»:** 0 duras y 0 blandas sin justificar en la versión (MD-07), con las inconsistencias registradas de esa versión. No se vuelve a correr el motor de reglas, porque la versión está congelada y las reglas pudieron cambiar después.
- **Qué versión:** solo la publicada vigente, como en F19-H01 y como dice el escenario del issue («Dado el distributivo publicado»). Una versión firmada que no se publicó sigue rechazándose con el 409 de F19-H01; no hay una exportación previa a la publicación.
- **La suma de verificación:** SHA-256, el mismo algoritmo del hash del glosario, sobre los bytes exactos del archivo. Como el CSV es determinista (FR-007 de F19-H01), la suma de verificación de una versión con un layout es estable.
- **Verificar:** se verifica recalculando el SHA-256 del archivo con una herramienta estándar y comparándolo con el registro; la pantalla dice cómo hacerlo en Windows y en Linux (aclaración del 2026-11-22). El sistema no ofrece una pantalla para subir un archivo y compararlo.
- **El archivo no se guarda:** el registro tiene lo necesario para volver a generarlo idéntico (la versión publicada, que no cambia, y la versión del layout, que se conserva). Guardar cada archivo duplicaría lo que ya está congelado.
- **La bitácora:** una entrada por exportación, la de F19-H01 con la suma de verificación (D-119 anticipó que esta historia «no repite esta entrada»). Las exportaciones rechazadas no son exportaciones. Solo el rechazo por firmas o inconsistencias deja una entrada propia, porque en el flujo normal no debería pasar y es una señal de integridad para la auditoría (aclaración del 2026-11-22); el 409 de «sin publicación» es un error de uso corriente y no deja entrada, como en F19-H01.
- **Quién:** la Secretaría general académica, por «Publicación: aprobación» (D-16, D-119); la matriz no cambia. La lista de exportaciones es parte de su pantalla; quien tiene acceso a la bitácora ve las entradas por esa vía.
- **El tamaño:** S, 13 h (D-119). La pantalla es la de F19-H01, con la suma de verificación y la lista de exportaciones.
- **Datos ficticios:** las identificaciones y los nombres de la demo son ficticios (principio IX).
- **Fuera de alcance:**
  - enviar el archivo a SIGA: no hay API (C-11);
  - una pantalla para verificar un archivo subiéndolo;
  - guardar los archivos exportados o volver a descargar uno anterior;
  - exportar una versión firmada que todavía no se publicó, o una histórica;
  - la matriz para la coordinación zonal o SENESCYT y la alerta de los 30 días (RF-54, W);
  - el mapeo de agregación con una fila por docente y la traducción de actividades y carreras (ADR-002, D-119).
