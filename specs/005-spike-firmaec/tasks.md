---

description: "Tareas de B00-S01: prueba temprana de firma con FirmaEC y validación con PDFBox"
---

# Tasks: Prueba temprana de firma con FirmaEC y validación con PDFBox

**Input**: documentos de diseño en `specs/005-spike-firmaec/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: el *spike* son pruebas. La validación y sus casos negativos se escriben antes que el validador (principio VIII).

**Tiempo fijo**: 2 días de trabajo (D-45). Si se cumple con tareas pendientes, se salta a la fase 6 y lo pendiente queda en el resultado.

**Decisiones**: D-23, ADR-007, D-43, D-44 y D-45 en [`docs/decisiones.md`](../../docs/decisiones.md).

**Organization**: por escenario: US1, certificados de la demo; US2, firma incremental; US3, validación; US4, resultado.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **(autor)**: tarea manual del autor con FirmaEC; el asistente prepara los archivos y anota el resultado.
- Las rutas son relativas a la raíz. `F` = `backend/servicios/firma/src/test`. Gradle con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`.

## Convenciones

- Todo el código va en las pruebas del servicio `firma`, paquete `ec.mdd.firma.spike`. El código de producción no cambia (D-44).
- Los `.p12` y los PDF firmados solo van en `F/resources/ca-prueba/` (Gitleaks). La llave privada de la CA nunca se guarda.
- Un certificado real o un PDF firmado con él nunca entra al repositorio, ni su CN real (D-43).
- JavaDoc en español; `spotlessApply` antes de cada commit; la evidencia en `specs/005-spike-firmaec/evidencia.md`.
- Las herramientas en contenedor corren sobre copias, nunca montando el repositorio con `:z`.

---

## Phase 1: Setup

- [X] T001 En `backend/gradle.properties`, agregar `versionPdfbox=3.0.8`, `versionOpenHtmlToPdf=1.1.87` y `versionVeraPdf=1.30.2`, cada una con su motivo (research §2). Bouncy Castle ya está en `versionBouncyCastle`.
- [X] T002 En `backend/servicios/firma/build.gradle`, agregar a la suite `test` las dependencias `org.apache.pdfbox:pdfbox`, `org.bouncycastle:bcpkix-jdk18on`, `io.github.openhtmltopdf:openhtmltopdf-pdfbox` y `org.verapdf:validation-model`, con sus versiones. Comentario: «solo en pruebas: spike B00-S01 (D-44)». Comprobar con `dependencies --configuration runtimeClasspath` que no entran al de ejecución. Además, reenviar a la JVM de las pruebas las propiedades del modo local (D-43): en `tasks.named('test')`, `systemProperty` de `mdd.firma.pdf0`, `mdd.firma.pdf1`, `mdd.firma.pdf2` y `mdd.firma.raiz`, leídas con `providers.systemProperty(...)` y solo si están definidas. Sin esto, `-D` llega a Gradle pero no a la prueba (hallazgo I1).
- [X] T003 [P] Crear `specs/005-spike-firmaec/evidencia.md` con estas secciones:
  - entorno, con la versión de FirmaEC y cómo se instaló;
  - escenario 1;
  - escenario 2;
  - escenario 3;
  - escenario 4;
  - tiempo usado;
  - notas.

---

## Phase 2: Foundational

- [X] T004 [P] Agregar la fuente `F/resources/fuentes/DejaVuSans.ttf` con su licencia en `F/resources/fuentes/LICENSE-DejaVu.txt`. No está instalada en Fedora: descargar `dejavu-fonts-ttf-2.37.tar.bz2` de la versión publicada oficial (`github.com/dejavu-fonts/dejavu-fonts/releases`) en una carpeta nueva del scratchpad, verificar su SHA-256 y copiar solo `DejaVuSans.ttf` y `LICENSE`. Indicar el origen y el SHA-256 en la licencia. Si la descarga no es posible, usar Liberation Sans (instalada, OFL) y anotarlo.
- [X] T005 [P] Crear `F/resources/spike/plantilla-pdfa.html`: XHTML con título, metadatos (`<title>`, idioma `es`) y una tabla ficticia «Distributivo de prueba, periodo 2026-I», con la fuente DejaVu Sans. Sin datos personales.

---

## Phase 3: User Story 1 - Certificados de la demo (Priority: P1)

**Goal**: CA de prueba y tres certificados de firmantes ficticios, y saber si FirmaEC firma con ellos (escenario 1; FR-001, FR-002).

**Independent Test**: los tres `.p12` se abren con OpenSSL con la cadena correcta, y el resultado de FirmaEC queda anotado.

- [X] T006 [US1] Crear `F/resources/ca-prueba/generar.sh`, ejecutable, con `set -euo pipefail` y OpenSSL 3, según `data-model.md`:
  - la CA con RSA de 3072 bits, 10 años y sus extensiones;
  - tres firmantes con RSA de 2048 bits, 3 años, `digitalSignature` y `nonRepudiation`, y los CN de F1, F2 y F3;
  - `firmante-N.p12` con la contraseña `cambiar-en-local-firma` y la cadena incluida;
  - `ca-prueba.crt`;
  - una variante `firmante-N-legacy.p12` con el cifrado antiguo de PKCS #12 (`-legacy`: 3DES y MAC SHA-1), por si FirmaEC no lee el formato moderno de OpenSSL 3 (AES-256 y PBKDF2). Así un rechazo por formato no se confunde con un rechazo de la CA (hallazgo U1).

  Trabaja en un directorio temporal y borra la llave de la CA al terminar. Validar con shellcheck, sobre una copia.
- [X] T007 [US1] Ejecutar `generar.sh` y verificar con `openssl pkcs12` y `openssl verify -CAfile ca-prueba.crt` que los tres certificados se encadenan a la CA y tienen las extensiones esperadas. Correr Gitleaks local sobre un clon con los archivos, para confirmar que la carpeta `ca-prueba/` se acepta. Anotar en `evidencia.md`.
- [X] T008 [US1] (autor) **Después de T010**, firmar con FirmaEC una copia desechable de `pdfa-prueba.pdf` con cada uno de los tres certificados, según [contracts/procedimiento-firma.md](contracts/procedimiento-firma.md). Primero con la variante moderna y, si falla, con la `-legacy`. Las copias no se guardan en el repositorio. Anotar en la sección «escenario 1» de `evidencia.md`:
  - la versión de FirmaEC y cómo se instaló en Fedora;
  - si firma con cada certificado, con qué variante, y el mensaje exacto si no.

  Si no los acepta, seguir D-43 en US2 y US3.

---

## Phase 4: User Story 2 - Firma incremental de un PDF/A (Priority: P1)

**Goal**: el PDF/A-2b firmado dos veces, con la segunda firma agregada sin alterar la primera (escenario 2; FR-003 a FR-005).

**Independent Test**: `pdfa-firmado-2.pdf` empieza con los bytes exactos de `pdfa-firmado-1.pdf` y tiene una firma más.

- [X] T009 [US2] Crear `F/java/ec/mdd/firma/spike/PdfaDePruebaTest.java`:
  - genera el PDF desde `plantilla-pdfa.html` con OpenHTMLtoPDF, con `usePdfAConformance(PDFA_2_B)`, la fuente incrustada y el perfil sRGB de `ICC_Profile.getInstance(ColorSpace.CS_sRGB)`;
  - lo valida con veraPDF (`PDFAFlavour.PDFA_2_B`) y falla si no es conforme, con las reglas que no cumple;
  - escribe una copia en `backend/servicios/firma/build/spike-firma/pdfa-prueba.pdf` (ruta desde la raíz; la misma en research, el procedimiento y el quickstart).
- [X] T010 [US2] Ejecutar `PdfaDePruebaTest`, copiar el PDF a `F/resources/ca-prueba/pdfa-prueba.pdf` y anotar en `evidencia.md` el resultado de veraPDF, el tamaño y el SHA-256.
- [X] T011 [US2] (autor) Firmar con FirmaEC según el procedimiento:
  - `pdfa-prueba.pdf` con `firmante-1.p12` → `pdfa-firmado-1.pdf`;
  - ese resultado con `firmante-2.p12` → `pdfa-firmado-2.pdf`.

  Copiar los dos a `F/resources/ca-prueba/` y anotar las opciones y los avisos de FirmaEC. **Con certificado real (D-43):** firmar en una carpeta fuera del repositorio y no copiar nada a `ca-prueba/`.
- [X] T012 [US2] En `F/java/ec/mdd/firma/spike/FirmasFirmaecTest.java`, la parte del escenario 2:
  - `pdfa-firmado-2.pdf` empieza con todos los bytes de `pdfa-firmado-1.pdf`, y este con los de `pdfa-prueba.pdf`;
  - cada uno tiene una firma más que el anterior (`PDDocument.getSignatureDictionaries`).

  Si los PDF firmados no están en `ca-prueba/`, la prueba se salta con un mensaje (`Assumptions`). En modo local, usa las rutas de `-Dmdd.firma.pdf1`, `-Dmdd.firma.pdf2` y, opcionalmente, `-Dmdd.firma.pdf0`. Comprobar que el modo local de verdad lee las rutas: con `-Dmdd.firma.pdf1` apuntando a un archivo que no existe, la prueba falla (no se salta).

---

## Phase 5: User Story 3 - Validación con las librerías del proyecto (Priority: P1)

**Goal**: por cada firma, integridad sobre su ByteRange, cadena hasta la CA de prueba y vigencia; y los 3 casos negativos (escenario 3; FR-006 a FR-008).

**Independent Test**: `./gradlew :servicios:firma:test --tests '*spike*'` en verde.

### Tests for User Story 3 ⚠️

- [X] T013 [US3] Escribir primero `F/java/ec/mdd/firma/spike/CasosNegativosTest.java` y un apoyo `FirmadorDePrueba.java`, que firma un PDF con PDFBox y Bouncy Castle (CMS separado, SHA-256) con un certificado y una fecha dados. Los casos de [contracts/validacion.md](contracts/validacion.md):
  - un byte cambiado dentro del ByteRange da integridad no válida;
  - la validación contra otra raíz, una CA generada en la prueba, da cadena no válida;
  - un certificado vencido antes del momento de la firma da vigencia no válida.

  Además, un caso positivo: un PDF firmado por la prueba con un certificado de la CA de prueba en memoria es válido en las cuatro comprobaciones. Al principio no compila: falta T014.

### Implementation for User Story 3

- [X] T014 [US3] Implementar `F/java/ec/mdd/firma/spike/ValidadorPadesDePrueba.java` y su registro `ResultadoFirma`, según research §6 y `contracts/validacion.md`:
  - ByteRange;
  - integridad con `CMSSignedData` y `JcaSimpleSignerInfoVerifierBuilder`;
  - cadena con `CertPathBuilder` PKIX, la raíz como único `TrustAnchor`, sin revocación y con la fecha de la firma;
  - vigencia en el momento de la firma, tomado de `signingTime` o de `/M`;
  - revisión cerrada: en `FirmasFirmaecTest`, el ByteRange `[0, a, b, c]` de la primera firma cumple `b + c` = longitud de `pdfa-firmado-1.pdf` (hallazgo U3).

  JavaDoc del contrato y de lo que no comprueba (orden, rol, sello de tiempo, LTV y DocMDP). Así pasa T013.
- [X] T015 [US3] En `FirmasFirmaecTest`, la parte del escenario 3: las 2 firmas de `pdfa-firmado-2.pdf` son íntegras, con cadena válida hasta `ca-prueba.crt` y vigentes; y los CN son los de F1 y F2. En modo local, la raíz sale de `-Dmdd.firma.raiz`, y la salida imprime cada comprobación sin el CN real.
- [X] T016 [US3] Ejecutar `./gradlew :servicios:firma:spotlessApply :servicios:firma:test`, o el modo local según D-43, y anotar en `evidencia.md`, secciones «escenario 2» y «escenario 3»:
  - la comparación de bytes;
  - la cantidad de firmas;
  - el resultado por firma;
  - los 3 casos negativos.

---

## Phase 6: User Story 4 - Resultado registrado (Priority: P2)

**Goal**: el resultado de cada escenario en el issue y en la evidencia, y la sección 8 de la constitución resuelta (escenario 4; FR-009, FR-010).

**Independent Test**: el issue #26 tiene el comentario con el resultado; la sección 8 dice «confirmada», o hay un ADR.

- [ ] T017 [US4] Completar `specs/005-spike-firmaec/evidencia.md`: el estado de cada escenario (funcionó, no funcionó o pendiente), las tareas que quedaron pendientes si se cumplió el tiempo, y el tiempo usado.
- [ ] T018 [US4] Según el resultado:
  - **si los escenarios 2 y 3 funcionaron:** registrar D-46, «Comprobación de la sección 8: PDFBox y Bouncy Castle validan las firmas de FirmaEC», con los datos de la evidencia, y agregar a esa fila de la sección 8 de la constitución «Confirmada (D-46): …», como en la fila de B00-H01. Es una enmienda de parche (§7): versión 2.1.0 → 2.1.1, con su entrada en el historial de versiones al inicio, y la fecha actualizada. Si FirmaEC rechazó la CA de prueba, D-46 también dice cómo se resuelven los certificados de la demo, o abre un ADR para eso;
  - **si no funcionaron:** proponer al autor un ADR que cambie D-23 y registrarlo antes de cerrar (FR-010).
- [ ] T019 [US4] **Con la aprobación del autor**, publicar en el issue #26 un comentario con el resumen del resultado por escenario y el enlace a `evidencia.md` (FR-009).

---

## Phase 7: Polish & cierre

- [ ] T020 Revisar el JavaDoc de las clases de `ec.mdd.firma.spike` y ejecutar `spotlessApply`. Comprobar con `git status` que no hay archivos temporales ni un PDF o certificado fuera de `ca-prueba/`. Correr Gitleaks local sobre un clon: `no leaks found`.
- [ ] T021 Hacer commit y push, y abrir el pull request de `005-spike-firmaec` **en borrador**, con `Closes #26`. Esperar a que terminen los runs del borrador y recién entonces marcarlo como listo. Comprobar que `3 · secretos` acepta los `.p12` de `ca-prueba/`, que la suite `test` del servicio `firma` pasa en la CI, y que `4 · sca` sigue en verde, porque las dependencias son solo de prueba.
- [ ] T022 Marcar la «Definición de terminado» del pull request y fusionarlo con *squash* **con la aprobación del autor**. Borrar la rama.

---

## Dependencies & Execution Order

- **Setup (fase 1)** y **Foundational (fase 2)**: sin dependencias.
- **US1 (fase 3)**: depende de la fase 1. T008 (autor) depende de T007 y de T010, porque firma una copia del PDF/A.
- **US2 (fase 4)**: T009 depende de T004 y T005; T011 (autor) depende de T008 y T010.
- **US3 (fase 5)**: T013 y T014 no dependen de FirmaEC y se pueden hacer mientras el autor firma; T015 y T016 dependen de T011.
- **US4 (fase 6)**: depende del resultado de US1 a US3, o de que se cumpla el tiempo.
- **Cierre (fase 7)**: depende de US4.

### Parallel Opportunities

- T003, T004 y T005.
- T013 y T014 (validador con firmas generadas en la prueba) mientras el autor hace T008 y T011.

---

## Implementation Strategy

1. **Día 1:** fases 1 y 2, T006, T007, T009 y T010. El autor prueba los certificados (T008) y firma (T011). En paralelo, el validador y los casos negativos (T013 y T014).
2. **Día 2:** validación de las firmas de FirmaEC (T015 y T016), resultado (fase 6) y cierre (fase 7).

---

## Notes

- Si FirmaEC no se puede instalar en Fedora dentro del tiempo, eso mismo es un resultado: se registra y se decide con el autor cómo seguir. Por ejemplo, otra máquina o FirmaEC en Java.
- Hacer commit por tarea o por grupo lógico en `005-spike-firmaec`, con `Refs #26`.
