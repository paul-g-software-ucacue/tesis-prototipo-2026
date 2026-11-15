# Feature Specification: Registrar la resolución del OCS

**Feature Branch**: `052-resolucion-ocs`

**Created**: 2026-11-15

**Status**: Draft

**Input**: User description: "F15-H01 · Registrar la resolución del OCS (#66). Usar el texto y los criterios del issue. Respetar .specify/memory/constitution.md, docs/decisiones.md (D-16, D-19, D-95, D-98, D-100, D-104, D-106, ADR-007, ADR-018, MD-04, MD-05) y el diseño de firmas (Diseno_firmas_cadena_aprobacion.md §2, fila EN_OCS: «Votación electrónica opcional (C-15) y registro obligatorio de la resolución; número, fecha y PDF de la resolución adjunto; en el prototipo el MDD no valida sus firmas»; y §6/§7 OCS). ERS RF-37: «Sin la resolución registrada (número, fecha y PDF de la resolución adjunto; en el prototipo no se validan sus firmas, D-19) el distributivo no pasa a Aprobado». Glosario (sdd/modelo_dominio.md): ResolucionOcs, entidad de ProcesoAprobacion, «Número, fecha y PDF. El MDD no valida sus firmas»; operación registrarResolucion() de ProcesoAprobacion; evento ResolucionRegistrada («Secretaría registra la resolución → pasa a APROBADO»); rol SECRETARIA_GENERAL (Secretaría general académica, secretaria.demo), con el permiso RESOLUCION_OCS:ESCRITURA de la matriz D-16; la etapa EN_OCS de la configuración por defecto tiene rol SECRETARIA_GENERAL y el nombre «Resolución del OCS». Parte de F14-H03: con la cadena de firmas completa el distributivo llega a EN_OCS por el sistema. Decisiones del autor: (1) el PDF lo guarda Aprobación (Validación OCS) en el almacén de archivos del proceso, con su huella SHA-256, junto con el número y la fecha; se valida que sea un PDF y su tamaño (como el informe sustentado), pero no sus firmas (D-19); (2) la fecha de la resolución no puede ser futura ni anterior al día en que el distributivo entró en EN_OCS; (3) registrar es aprobar: en una sola operación de quien tiene el rol de la etapa EN_OCS, la resolución queda guardada y el distributivo pasa de EN_OCS a APROBADO, con su transición y su entrada en la bitácora; después no se corrige (solo con una reforma, F18-H02). Pasar a APROBADO sin la resolución se rechaza con un 409 del catálogo. El número es obligatorio. La resolución se puede consultar (número, fecha, quién la registró, cuándo, la huella) y su PDF descargar desde el estado del flujo por quien lee el distributivo. La SPA: en «Consolidado del periodo», con el distributivo en EN_OCS, el rol de la etapa ve un formulario con número, fecha y el PDF; después, la constancia de la resolución con su descarga. Fuera de alcance: la votación electrónica del OCS (C-15, RF-37 «Could»), la devolución desde EN_OCS, publicar (F18-H01), la reforma (F18-H02), validar las firmas del PDF y las notificaciones (F16)."

**Origen**: issue #66 · historia (`tipo:historia`, `prioridad:must`, `RF-37`) · feature F15 (#14) · milestone «Ola 3 · Aprobado y firmado» · servicio 06-validacion-ocs · tamaño M. Depende de F14-H03 (#64), terminada. Es la única historia de F15.

**Historia (tal como está en el issue)**: Como Secretaría general académica, quiero registrar el número, la fecha y el PDF de la resolución del OCS, para que el distributivo quede aprobado con su respaldo.

**Requerimientos que cubre**:
- **RF-37:** la parte obligatoria: el número, la fecha y el PDF de la resolución. Su criterio en la ERS: «Sin la resolución registrada (número, fecha y PDF de la resolución adjunto; en el prototipo no se validan sus firmas, D-19) el distributivo no pasa a "Aprobado".» La votación electrónica de cada miembro (C-15, «Could») queda fuera.

**Decisiones que aplica**:
- D-19 y ADR-007: el MDD no valida las firmas del PDF de la resolución.
- D-16: el permiso de la Secretaría general académica sobre la resolución del OCS.
- D-104: el distributivo llega a EN_OCS por el sistema cuando se completa la cadena de firmas.
- D-95: la transición la registra Aprobación, con su entrada en la bitácora.

**Principios de la constitución**: II, IV, V, VII, VIII, IX, XIII y XIV.

## Clarifications

### Session 2026-11-15

- Q: ¿Dónde se guarda el PDF de la resolución? → A: En Aprobación, en el almacén de archivos del proceso, con su huella, junto con el número y la fecha. La resolución es parte del proceso de aprobación (glosario). Se comprueba que sea un PDF y su tamaño, como el informe sustentado, pero no sus firmas.
- Q: ¿Qué reglas lleva la fecha? → A: No puede ser futura ni anterior al día en que el distributivo entró en EN_OCS: la resolución aprueba la versión ya firmada.
- Q: ¿Quién puede leer la resolución y descargar su PDF? → A: Quien tiene `RESOLUCION_OCS:LECTURA` (D-16). Se suma a VICERRECTOR, RECTOR y MIEMBRO_OCS, además de SECRETARIA_GENERAL y ADMINISTRADOR, que ya lo tienen. No basta con leer el flujo (análisis S1).
- Q: ¿Registrar y aprobar son pasos distintos? → A: No. Registrar la resolución pasa el distributivo a APROBADO en la misma operación. Después no se corrige; solo una reforma (F18-H02) cambia una versión aprobada.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - La Secretaría registra la resolución y el distributivo queda aprobado (Priority: P1)

Como Secretaría general académica, quiero registrar el número, la fecha y el PDF de la resolución del OCS, para que el distributivo quede aprobado con su respaldo.

**Why this priority**: son los dos escenarios del issue. Sin la resolución, el distributivo se queda en EN_OCS después de las firmas.

**Independent Test**: con el distributivo en EN_OCS, `secretaria.demo` intenta aprobar sin resolución (se rechaza) y después registra número, fecha y PDF; el distributivo pasa a APROBADO, con la resolución guardada y su transición en la bitácora.

**Acceptance Scenarios**:

1. **Dado** el distributivo en EN_OCS, **cuando** intento pasarlo a APROBADO sin la resolución, **entonces** se rechaza con un error 409 del catálogo, y nada cambia. *(issue)*
2. **Dado** el distributivo en EN_OCS, **cuando** registro el número, la fecha y el PDF de la resolución, **entonces** el distributivo pasa a APROBADO, **y** el MDD no valida las firmas del PDF de la resolución. *(issue)*
3. **Dado** una solicitud sin número, sin fecha o sin PDF, **cuando** registro, **entonces** se rechaza con el error común de solicitud inválida, con la parte que falta en sus detalles, y el distributivo sigue en EN_OCS.
4. **Dado** una fecha futura o anterior al día en que el distributivo entró en EN_OCS, **cuando** registro, **entonces** se rechaza con un error del catálogo que dice el rango admitido.
5. **Dado** un archivo que no es un PDF, o uno de más de 10 MB, **cuando** registro, **entonces** se rechaza con su error del catálogo, sin guardar nada.
6. **Dado** un PDF firmado con un certificado cualquiera, o sin firmar, **cuando** registro, **entonces** se acepta: las firmas del PDF no se comprueban (D-19).
7. **Dado** que no tengo el rol de la etapa EN_OCS, **cuando** intento registrar, **entonces** recibo el error de acceso común, y nada cambia.
8. **Dado** el distributivo en otra etapa que EN_OCS, **cuando** intento registrar, **entonces** se rechaza con un error del catálogo, y nada cambia.
9. **Dado** la resolución registrada, **cuando** se consulta la bitácora, **entonces** tiene el paso a APROBADO, con quién, cuándo, el número, la fecha y la huella del PDF.

---

### User Story 2 - Quien puede leer la resolución la ve y descarga su PDF (Priority: P2)

Como Vicerrector, Rector, miembro del OCS o Secretaría, quiero ver la resolución que aprobó el distributivo y descargar su PDF, para tener a mano el respaldo de la aprobación.

**Why this priority**: la resolución es el respaldo de la aprobación; sin poder consultarla, solo queda en la base.

**Independent Test**: con el distributivo en APROBADO, quien tiene el permiso de leer la resolución ve en el estado del flujo el número, la fecha, quién la registró, cuándo y la huella, y descarga el PDF igual al que se subió.

**Acceptance Scenarios**:

1. **Dado** el distributivo en APROBADO, **cuando** quien puede leer la resolución consulta el estado del flujo, **entonces** trae la resolución con el número, la fecha, quién la registró, cuándo y la huella del PDF; a quien lee el flujo sin ese permiso, sin la resolución.
2. **Dado** la resolución registrada, **cuando** quien puede leer la resolución descarga el PDF, **entonces** recibe el mismo archivo que se subió, byte a byte.
3. **Dado** que no tengo el permiso de leer la resolución (por ejemplo, un coordinador de carrera), **cuando** pido su PDF, **entonces** recibo el error de acceso común.
4. **Dado** un distributivo sin resolución, **cuando** se pide su PDF, **entonces** se responde que no existe, con un error del catálogo.

---

### User Story 3 - Registrar la resolución desde «Consolidado del periodo» (Priority: P2)

Como Secretaría general académica, quiero un formulario en «Consolidado del periodo» para registrar la resolución, y ver después su constancia con la descarga.

**Why this priority**: la operación ya se puede hacer por la API; la pantalla la pone al alcance de la Secretaría.

**Independent Test**: con el distributivo en EN_OCS, `secretaria.demo` abre «Consolidado del periodo», completa el número, la fecha y el PDF, confirma, y ve el distributivo en APROBADO con la constancia de la resolución y «Descargar la resolución».

**Acceptance Scenarios**:

1. **Dado** el distributivo en EN_OCS y el rol de la etapa, **cuando** abro «Consolidado del periodo», **entonces** veo el formulario «Registrar la resolución del OCS» con el número, la fecha y el PDF, y una confirmación que dice que el distributivo quedará aprobado.
2. **Dado** el formulario incompleto o con la fecha fuera de rango, **cuando** intento confirmar, **entonces** la pantalla dice qué falta; el servidor lo comprueba igual.
3. **Dado** la resolución registrada, **cuando** vuelve la pantalla, **entonces** veo APROBADO y la constancia con el número, la fecha, quién, cuándo, la huella y «Descargar la resolución».
4. **Dado** otro rol, **cuando** abro la pantalla con el distributivo en EN_OCS, **entonces** no veo el formulario.
5. **Dado** la pantalla, **cuando** se revisa con un lector de pantalla y el teclado, **entonces** el registro se hace completo y cumple WCAG 2.1 AA.

---

### Edge Cases

- **Dos registros a la vez**: uno gana y pasa a APROBADO; el otro se rechaza porque el distributivo ya no está en EN_OCS. Nunca quedan dos resoluciones.
- **Un archivo guardado y la transacción fallida**: si el registro falla después de guardar el PDF, no queda resolución; el archivo huérfano no se ofrece en ninguna parte.
- **El número con espacios en los extremos**: se recortan antes de guardarlo y de medirlo.
- **La fecha del mismo día en que entró en EN_OCS**: se admite; las dos son fechas sin hora, en la zona de la institución.
- **La configuración del distributivo con EN_OCS**: está en el esqueleto fijo; siempre existe y su rol es el de la copia de la configuración.
- **Después de APROBADO**: la resolución no se corrige ni se reemplaza; un segundo registro se rechaza porque ya no está en EN_OCS.
- **El PDF de la resolución vacío o dañado**: si no empieza como un PDF, se rechaza; no se analiza su contenido más allá de eso.

## Requirements *(mandatory)*

### Functional Requirements

**Registrar y aprobar**

- **FR-001**: Quien tiene el rol de la etapa EN_OCS DEBE poder registrar la resolución del OCS con el número, la fecha y el PDF, con el distributivo en EN_OCS.
- **FR-002**: Registrar la resolución DEBE pasar el distributivo de EN_OCS a APROBADO en la misma operación, con su transición, quién y cuándo. Si algo falla, no queda ni la resolución ni la transición.
- **FR-003**: Pasar el distributivo a APROBADO sin la resolución DEBE rechazarse con un error 409 del catálogo (RF-37).
- **FR-004**: El número DEBE ser obligatorio, de 1 a 60 caracteres después de recortar los espacios de los extremos.
- **FR-005**: La fecha DEBE ser obligatoria, no futura, y no anterior al día en que el distributivo entró en EN_OCS.
- **FR-006**: El PDF DEBE ser obligatorio, comprobado como PDF por su contenido y de hasta 10 MB, como el informe sustentado. Sus firmas NO DEBEN comprobarse (D-19).
- **FR-007**: El PDF DEBE guardarse con su huella SHA-256, y la resolución, con el número, la fecha, la versión, quién y cuándo. No se modifica ni se borra después (principio VII).
- **FR-008**: Quien no tiene el rol de la etapa, o el permiso para escribir la resolución, DEBE recibir el error de acceso común, sin que nada cambie.
- **FR-009**: Con el distributivo en otra etapa, DEBE rechazarse con el error del catálogo de la etapa, sin que nada cambie.
- **FR-010**: Dos registros a la vez NO DEBEN dejar dos resoluciones: gana el primero, y el otro se rechaza.
- **FR-011**: La bitácora DEBE tener el paso a APROBADO con el número, la fecha y la huella del PDF.

**Consultar**

- **FR-012**: El estado del flujo DEBE mostrar la resolución registrada, con el número, la fecha, quién, cuándo y la huella del PDF, solo a quien tiene el permiso de leer la resolución (D-16).
- **FR-013**: Quien tiene el permiso de leer la resolución DEBE poder descargar su PDF, igual al que se subió; los demás reciben el error de acceso común.

**Pantalla, errores y seguridad**

- **FR-014**: «Consolidado del periodo» DEBE ofrecer al rol de la etapa, con el distributivo en EN_OCS, el formulario de la resolución con su confirmación, y después la constancia con la descarga. Cumple WCAG 2.1 AA.
- **FR-015**: Los errores nuevos DEBEN estar en el catálogo con sus 8 columnas (principio XIII).
- **FR-016**: El rol, la etapa, el número, la fecha y el PDF DEBEN comprobarse en el servidor; la pantalla solo ayuda.

### Key Entities

- **Resolución del OCS** (nueva, de Aprobación): el número, la fecha, el PDF con su huella, la versión aprobada, quién la registró y cuándo. Una por proceso de aprobación; no cambia.
- **Transición a APROBADO** (de Aprobación): la de quien registra la resolución, desde EN_OCS.
- **El archivo de la resolución**: el PDF, en el almacén de archivos del proceso.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En las pruebas, el 100 % de los intentos de pasar a APROBADO sin resolución, con un dato que falta o no cumple, de otro rol o en otra etapa se rechazan sin cambiar nada.
- **SC-002**: El 100 % de los registros válidos dejan el distributivo en APROBADO con su resolución, y su PDF se descarga idéntico.
- **SC-003**: La Secretaría registra la resolución desde «Consolidado del periodo» en menos de 2 minutos, con el PDF a mano.
- **SC-004**: Los escenarios de F12-H04, F14-H04, F14-H03, F14-H02, F14-H01, F13-H02 y F13-H01 siguen pasando.

## Assumptions

- **El rol** es el de la etapa EN_OCS en la copia de la configuración del distributivo: SECRETARIA_GENERAL por defecto. El permiso es RESOLUCION_OCS:ESCRITURA (D-16).
- **El número** no tiene un formato fijo: cada institución numera sus resoluciones a su manera. No se exige que sea único entre periodos.
- **«El día en que entró en EN_OCS»** es la fecha de la transición a EN_OCS en la zona horaria de la institución (America/Guayaquil).
- **El almacén de archivos** es el del proceso, el mismo volumen que usan el informe sustentado y los documentos firmables.
- **Leer la resolución** exige el permiso `RESOLUCION_OCS:LECTURA` de la matriz D-16. Hoy lo tienen la Secretaría general y el Administrador; esta historia lo suma al Vicerrector, al Rector y a los miembros del OCS, que necesitan ver el respaldo de la aprobación (decisión del autor, análisis S1). Los coordinadores y el Responsable de consolidación no lo tienen.
- **APROBADO** no dispara nada todavía: publicar es de F18-H01.
- **En la demo**, 2026-I está en consolidación y no llega a EN_OCS.

## Out of Scope

- La votación electrónica de los miembros del OCS (C-15, RF-37 «Could»).
- La devolución desde EN_OCS con una versión nueva.
- Publicar el distributivo aprobado (F18-H01) y la reforma (F18-H02).
- Validar las firmas del PDF de la resolución (D-19).
- Las notificaciones (F16).
