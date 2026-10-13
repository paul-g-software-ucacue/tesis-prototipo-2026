# Feature Specification: Prueba temprana de firma con FirmaEC y validación con PDFBox

**Feature Branch**: `005-spike-firmaec`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "Especifica la historia B00-S01. Léela de su issue con gh issue list --search "B00-S01 in:title" y gh issue view <número>. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md y docs/decisiones.md"

**Origen**: issue #26 · B00-S01 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:spike`, `prioridad:must`, `modulo:10-firma` · tamaño propuesto M · sin dependencias.

**Requerimientos que cubre**: D-23 · ADR-007 · ADR-008 · S-10 · H-11 · constitución, sección 8 («PDFBox y Bouncy Castle validan un PDF/A firmado con FirmaEC», antes pendiente 5). **Decisiones tomadas en esta historia:** D-43, D-44 y D-45.

**Historia (tal como está en el issue)**: Como autor, quiero firmar con FirmaEC un PDF/A de prueba con certificados de la CA de prueba y validarlo con PDFBox y Bouncy Castle, para saber en la ola 0, y no en la 3, si la validación básica de D-23 funciona.

## Clarifications

### Session 2026-10-13

- Q: Si FirmaEC no firma con certificados de la CA de prueba, ¿cómo siguen los escenarios 2 y 3? → A: Se registra el hallazgo y los escenarios 2 y 3 se hacen con el certificado real del autor, solo en su equipo. Al repositorio no entran ni el certificado ni los PDF firmados con él: solo el resultado y los datos públicos de la comprobación (D-43).
- Q: ¿Dónde queda el código de la validación? → A: Como prueba automática dentro del servicio `firma` (suite `test`), con la CA de prueba, sus certificados y los PDF firmados en `src/test/resources/ca-prueba/`; el código de producción no cambia (D-44).
- Q: ¿Cuánto dura el tiempo fijo del *spike*? → A: 2 días de trabajo (D-45).

> **Nota sobre esta especificación.** Es un *spike*: su resultado es conocimiento, no una funcionalidad para los usuarios del MDD. Su usuario es el autor y su objeto es comprobar, con las herramientas que ya fija la constitución (FirmaEC, OpenHTMLtoPDF, PDFBox y Bouncy Castle; sección 3), que la validación básica de D-23 es factible. Los nombres de esas herramientas aparecen porque los trae la historia.

## User Scenarios & Testing *(mandatory)*

El *spike* es uno solo. Sus cuatro escenarios son pasos de una misma prueba; la prioridad indica el orden en que se hacen.

### User Story 1 - Certificados de la demo que FirmaEC acepta (Priority: P1)

Como autor, quiero una CA de prueba con tres certificados de firmantes ficticios, y saber si FirmaEC firma con ellos, para que la demo use certificados de prueba y nunca el certificado real del autor (principio IX, S-10).

**Why this priority**: Sin certificados con los que FirmaEC firme, los demás pasos no se pueden hacer como están escritos.

**Independent Test**: Crear la CA y los tres certificados, intentar firmar un PDF con cada uno en FirmaEC y registrar el resultado.

**Acceptance Scenarios**:

```gherkin
Escenario: Certificados de la demo
  Dado una CA de prueba creada para la demo
  Cuando se emiten tres certificados .p12 con nombres ficticios
  Entonces se registra si FirmaEC firma con ellos
```

---

### User Story 2 - Firma incremental de un PDF/A (Priority: P1)

Como autor, quiero firmar dos veces, con dos certificados distintos, un PDF/A-2b de prueba generado con la herramienta del proyecto, y comprobar que la segunda firma no altera la primera, para saber si el modo «descargar, firmar y subir» conserva los bytes que exige D-23.

**Why this priority**: El prefijo de bytes es la primera comprobación de D-23; si FirmaEC no firma de forma incremental, la validación de D-23 no funciona como está.

**Independent Test**: Generar el PDF/A, firmarlo en FirmaEC, firmar ese resultado con otro certificado y comparar los bytes iniciales del segundo archivo con el primero.

**Acceptance Scenarios**:

```gherkin
Escenario: Firma incremental
  Dado un PDF/A-2b de prueba generado con OpenHTMLtoPDF
  Cuando se firma dos veces con FirmaEC, con dos certificados distintos
  Entonces el segundo archivo empieza con los bytes exactos del primero
```

---

### User Story 3 - Validación con las librerías del proyecto (Priority: P1)

Como autor, quiero validar el PDF con dos firmas con las librerías que fija la constitución, comprobando la integridad de cada firma, la cadena hasta la CA de prueba y la vigencia del certificado, para saber si la validación básica de D-23 se puede construir en la ola 3 sin cambiar de herramientas.

**Why this priority**: Es la pregunta central del *spike* (constitución, sección 8).

**Independent Test**: Ejecutar la validación sobre el PDF con dos firmas y comprobar que informa, por cada firma, integridad, cadena y vigencia; y que detecta un PDF alterado.

**Acceptance Scenarios**:

```gherkin
Escenario: Validación con las librerías del stack
  Dado el PDF con dos firmas
  Cuando se valida con PDFBox y Bouncy Castle
  Entonces se comprueban la integridad de cada firma sobre su ByteRange, la cadena hasta la CA de prueba y la vigencia del certificado
```

---

### User Story 4 - Resultado registrado (Priority: P2)

Como autor, quiero dejar registrado qué funcionó y qué no al cerrar el tiempo del *spike*, y registrar un ADR si hay que cambiar D-23, para que la ola 3 parta de un resultado conocido.

**Why this priority**: Es la salida del *spike*; depende de los pasos anteriores.

**Independent Test**: Revisar el issue al cerrar el *spike*: tiene el resultado de cada escenario y, si corresponde, el enlace al ADR.

**Acceptance Scenarios**:

```gherkin
Escenario: Resultado del spike
  Dado el tiempo fijo cumplido
  Cuando se cierra el spike
  Entonces queda en el issue lo que funcionó y lo que no
  Y si hay que cambiar D-23, se registra un ADR
```

---

### Edge Cases

- **FirmaEC no acepta la CA de prueba** (por ejemplo, porque exige una cadena de una entidad acreditada): se registra en el resultado como hallazgo del escenario 1. Los escenarios 2 y 3 se hacen igual con el certificado real del autor, solo en su equipo; al repositorio no entran ni el certificado ni los PDF firmados con él, solo el resultado y los datos públicos de la comprobación (D-43).
- **FirmaEC firma, pero no de forma incremental** (reescribe el archivo): el escenario 2 falla y se registra; es un motivo para un ADR sobre D-23.
- **PDF alterado después de firmar**: la validación lo detecta como firma no íntegra.
- **Certificado vencido o todavía no vigente**: la validación lo informa como certificado fuera de vigencia.
- **Certificado de otra CA**: la validación informa que la cadena no llega a la CA de prueba.
- **El PDF/A generado no es PDF/A-2b válido**: se registra; el escenario 2 parte de un PDF/A-2b, así que se comprueba antes de firmar.
- **Se acaba el tiempo con escenarios a medias**: el *spike* se cierra igual con lo que se sabe; lo pendiente queda en el resultado.

## Requirements *(mandatory)*

### Functional Requirements

**Certificados**

- **FR-001**: DEBE existir una CA de prueba creada solo para la demo, con tres certificados de firmante con nombres ficticios, en formato `.p12`, guardados únicamente en la carpeta de la CA de prueba que permite Gitleaks (principio IX, S-10).
- **FR-002**: El resultado DEBE registrar si FirmaEC firma con cada uno de los tres certificados.

**Documento y firmas**

- **FR-003**: DEBE generarse un PDF/A-2b de prueba con la herramienta de generación del proyecto (OpenHTMLtoPDF), con contenido ficticio, y comprobarse que es PDF/A-2b antes de firmarlo.
- **FR-004**: El PDF DEBE firmarse dos veces con FirmaEC, cada vez con un certificado distinto, sobre el resultado de la firma anterior, en el modo «descargar, firmar y subir» (ADR-007).
- **FR-005**: DEBE comprobarse que el archivo con dos firmas empieza con los bytes exactos del archivo con una firma.

**Validación**

- **FR-006**: Con las librerías de la constitución (PDFBox y Bouncy Castle), DEBE comprobarse para cada firma: la integridad sobre su ByteRange, la cadena hasta la CA de prueba y la vigencia del certificado en el momento de la firma.
- **FR-007**: La validación DEBE detectar un PDF con un byte alterado dentro de una zona firmada, un certificado de otra CA y un certificado fuera de vigencia.
- **FR-008**: La validación DEBE poder repetirse de forma automática sobre los PDF firmados del *spike*, sin intervención manual. La validación es una prueba automática del servicio `firma` (servicio 10, suite `test`), con la CA de prueba, sus certificados y los PDF firmados con ellos en `src/test/resources/ca-prueba/`; el código de producción del servicio no cambia (D-44).

**Resultado**

- **FR-009**: Al cumplirse el tiempo fijo, el issue #26 DEBE tener un comentario con el resultado de cada escenario (funcionó, no funcionó o quedó pendiente) y la evidencia. El tiempo fijo es de 2 días de trabajo (D-45).
- **FR-010**: Si el resultado obliga a cambiar D-23, DEBE registrarse un ADR en `docs/decisiones.md` antes de cerrar el *spike*; si no, la comprobación de la sección 8 de la constitución DEBE quedar marcada como confirmada.

### Key Entities

- **CA de prueba**: autoridad de certificación creada solo para la demo; emite los certificados de los firmantes ficticios y es la raíz de confianza de la validación.
- **Certificado de firmante**: certificado `.p12` con nombre ficticio, emitido por la CA de prueba.
- **PDF/A de prueba**: documento PDF/A-2b con contenido ficticio, generado con la herramienta del proyecto.
- **Firma**: firma PAdES que agrega FirmaEC, con su ByteRange y su certificado.
- **Resultado del spike**: registro de qué funcionó y qué no en cada escenario, con su evidencia.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El resultado responde, para cada uno de los cuatro escenarios, si funcionó, no funcionó o quedó pendiente, con evidencia reproducible.
- **SC-002**: La validación informa correctamente las 2 firmas del PDF del *spike* como íntegras, con cadena válida y vigentes, y rechaza los 3 casos negativos de FR-007.
- **SC-003**: La comprobación de la sección 8 de la constitución queda resuelta: confirmada, o reemplazada por un ADR.
- **SC-004**: Ningún certificado real ni dato personal entra al repositorio: la etapa de secretos de la CI pasa y los únicos `.p12` están en la carpeta de la CA de prueba. Si se usó el certificado real del autor (D-43), ni él ni los PDF firmados con él están en el repositorio.
- **SC-005**: El *spike* se cierra en 2 días de trabajo o menos (D-45).

## Assumptions

- FirmaEC lo opera el autor en su equipo; la firma con FirmaEC es manual por naturaleza (modo «descargar, firmar y subir», ADR-007).
- Los nombres de los firmantes son ficticios y corresponden a los roles F1, F2 y F3 del árbol de firmas (ADR-004, D-19).
- El sello de tiempo, LTV y DocMDP no se validan (constitución, sección 3); el *spike* tampoco los prueba.
- La validación del orden y del rol de cada firmante (D-23) se construye en la ola 3 (F14); el *spike* comprueba lo que la historia pide: integridad, cadena y vigencia, y el prefijo de bytes.
- La carpeta de la CA de prueba ya está permitida por `.gitleaks.toml` (`backend/.+/src/test/resources/ca-prueba/` y `pruebas/ca-prueba/`).
