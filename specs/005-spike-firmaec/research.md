# Investigación (fase 0): B00-S01 · Prueba temprana de firma con FirmaEC

**Fecha:** 2026-10-13 · **Especificación:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Fuentes leídas:
- constitución 2.1.0 (principios VIII y IX; secciones 3 y 8) y `docs/decisiones.md` (D-19, D-23, ADR-004, ADR-007, D-43 a D-45);
- `.gitleaks.toml`, que permite certificados en `backend/.+/src/test/resources/ca-prueba/`;
- Maven Central (PDFBox, Bouncy Castle, OpenHTMLtoPDF y veraPDF) y el manual de usuario de FirmaEC.

---

## 1. Qué responde el *spike*

La sección 8 de la constitución deja una comprobación pendiente: «PDFBox y Bouncy Castle validan un PDF/A firmado con FirmaEC». D-23 exige, al subir un PDF firmado:
1. que empiece con los bytes exactos del anterior (prefijo);
2. que haya una sola firma nueva;
3. la integridad;
4. la vigencia y el orden;
5. la confianza contra raíces en un archivo;
6. la identidad por el CN.

El *spike* prueba, con firmas reales de FirmaEC, las piezas técnicas que pide la historia:
- el prefijo de bytes;
- la integridad sobre el ByteRange;
- la cadena hasta una raíz en un archivo;
- la vigencia.

El orden y el rol se construyen en F14 (spec, Assumptions).

## 2. Versiones (todas solo en las pruebas del servicio `firma`)

| Pieza | Versión | Motivo |
|---|---|---|
| Apache PDFBox | 3.0.8 (julio de 2026) | Última estable de la 3.0. Lee las firmas (`getSignatureDictionaries`, `getSignedContent`) y firma en las pruebas negativas. |
| Bouncy Castle `bcpkix-jdk18on` | 1.86 | Verifica el CMS (PKCS #7) de cada firma, arma la cadena y genera el certificado vencido de la prueba negativa. Misma versión que `bcprov` de B00-H03. |
| OpenHTMLtoPDF (`io.github.openhtmltopdf:openhtmltopdf-pdfbox`) | 1.1.87 (26 de septiembre de 2026) | El *fork* mantenido, sobre PDFBox 3. Las versiones 1.1.88 a 1.1.97 salieron entre el 7 y el 9 de octubre, hace menos de dos semanas. |
| veraPDF `validation-model` | 1.30.2 (junio de 2026) | Comprueba que el PDF generado es PDF/A-2b antes de firmarlo (FR-003). La 1.30.3 tiene cinco días. |

- Las versiones van en `backend/gradle.properties`, como las demás que no trae el BOM.
- Se declaran solo en la suite `test` de `servicios/firma`: no entran al `runtimeClasspath`, ni al SBOM, ni a las imágenes.

## 3. CA de prueba y certificados

- **Decisión:** un script, `backend/servicios/firma/src/test/resources/ca-prueba/generar.sh` (OpenSSL), crea:
  - **la CA de prueba** «CA de Prueba MDD Demo», RSA de 3072 bits, válida 10 años, con `basicConstraints=CA:TRUE` y `keyUsage=keyCertSign,cRLSign`;
  - **tres certificados de firmante** (RSA de 2048 bits, válidos 3 años), con `keyUsage=digitalSignature,nonRepudiation` y CN ficticios ligados a los roles F1, F2 y F3 (ADR-004):
    - «Firmante Uno Prueba (F1)»;
    - «Firmante Dos Prueba (F2)»;
    - «Firmante Tres Prueba (F3)»;
  - para cada firmante, un `.p12` con la contraseña ficticia `cambiar-en-local-firma`, y `ca-prueba.crt` con la raíz pública;
  - una variante `firmante-N-legacy.p12` con el cifrado antiguo de PKCS #12 (`-legacy`). OpenSSL 3 usa por defecto AES-256 y PBKDF2, y una aplicación con librerías antiguas podría no leerlo; así, un rechazo por formato no se confunde con un rechazo de la CA.
- **La llave privada de la CA no se guarda:** el script la usa y la borra. Para emitir más certificados se vuelve a generar todo. Así el repositorio solo tiene la raíz pública y los `.p12` de la demo.
- **Por qué OpenSSL:** está instalado en la máquina del autor y en los runners, y el script deja documentado exactamente qué extensiones lleva cada certificado, que es justo lo que hay que registrar si FirmaEC lo rechaza.
- **Riesgo conocido:** FirmaEC podría exigir una cadena de una entidad acreditada en Ecuador. Es la pregunta del escenario 1, y D-43 dice cómo seguir si ocurre.

## 4. El PDF/A de prueba

- **Decisión:** una prueba, `PdfaDePruebaTest`, genera `pdfa-prueba.pdf` con OpenHTMLtoPDF en modo PDF/A-2b a partir de una plantilla HTML con contenido ficticio, por ejemplo «Distributivo de prueba, periodo 2026-I». Para eso:
  - usa `usePdfAConformance(PDFA_2_B)`;
  - el perfil de color sRGB sale de `ICC_Profile.getInstance(ColorSpace.CS_sRGB)`, sin archivo;
  - incrusta una fuente libre (DejaVu Sans 2.37, con su licencia), en `src/test/resources/fuentes/`. No viene instalada en Fedora: se descarga de la versión publicada oficial y se verifica su SHA-256.
- **Comprobación:** la misma prueba lo valida con veraPDF (`PDFAFlavour.PDFA_2_B`) y falla si no cumple.
- **Copia para firmar:** escribe una copia en `backend/servicios/firma/build/spike-firma/pdfa-prueba.pdf` para que el autor la firme. La versión exacta que se firmó se guarda en `ca-prueba/pdfa-prueba.pdf`, porque la generación agrega fechas y no es reproducible byte a byte.

## 5. La firma con FirmaEC (manual, el autor)

1. Instalar FirmaEC. El manual documenta paquetes para Windows, macOS y Linux basado en Debian. En Fedora puede hacer falta otro camino, como la distribución en Java: es un riesgo del *spike* y se registra.
2. Firmar `pdfa-prueba.pdf` con `firmante-1.p12` y guardarlo como `pdfa-firmado-1.pdf`.
3. Firmar `pdfa-firmado-1.pdf` con `firmante-2.p12` y guardarlo como `pdfa-firmado-2.pdf`.
4. Anotar en cada paso:
   - si FirmaEC aceptó el certificado, y el mensaje exacto si no;
   - qué opciones ofreció (firma visible o invisible, posición) y cuáles se eligieron;
   - la versión de FirmaEC.
5. Copiar los dos PDF a `ca-prueba/`.

**Si FirmaEC rechaza la CA de prueba (D-43):**
- los pasos 2 y 3 se hacen con el certificado real del autor, solo en su equipo;
- si el autor tiene un solo certificado real, las dos firmas son con el mismo, y la desviación respecto de «dos certificados distintos» se anota en el resultado;
- los PDF firmados se quedan fuera del repositorio;
- la validación corre en local (§6, modo local) contra la raíz pública de la entidad emisora;
- al repositorio solo va el resultado.

## 6. La validación (PDFBox y Bouncy Castle)

`ValidadorPadesDePrueba`, una clase de las pruebas del servicio `firma`, recibe los bytes de un PDF y la raíz de confianza. Por cada firma del documento:

| Comprobación | Cómo |
|---|---|
| **ByteRange** | Cubre todo el archivo, salvo `/Contents`, hasta el final de su revisión. La última firma cubre hasta el final del archivo; la primera, hasta el final de `pdfa-firmado-1.pdf` (`b + c` = su longitud). |
| **Integridad** | `CMSSignedData` sobre `getSignedContent(bytes)`. Verificación del `SignerInformation` con el certificado del firmante: el *digest* firmado coincide con el contenido y la firma RSA es válida. |
| **Cadena** | `CertPathBuilder` PKIX con la CA de prueba como único `TrustAnchor`, los certificados del CMS como intermedios, sin revocación (D-23 no la pide) y con fecha de validación igual al momento de la firma. |
| **Vigencia** | `checkValidity` del certificado en el momento de la firma (atributo `signingTime` del CMS, o `/M` del diccionario si falta). |

**Fuera del validador:**
- el prefijo de bytes: el archivo con dos firmas empieza con los bytes exactos del archivo con una;
- la cantidad de firmas: una más que el anterior.

**Modo local (D-43):** la misma prueba se puede ejecutar con `-Dmdd.firma.pdf1=… -Dmdd.firma.pdf2=… -Dmdd.firma.raiz=…`. El `build.gradle` del servicio reenvía esas propiedades a la JVM de las pruebas, porque `-D` solo llega a la de Gradle. Así valida PDF que no están en el repositorio; sin esas propiedades, esa prueba se salta.

**Casos negativos (FR-007):**
- un byte cambiado dentro del ByteRange de la primera firma;
- la validación con otra raíz de confianza, una CA generada en la prueba;
- un PDF firmado por la prueba (PDFBox con Bouncy Castle) con un certificado que venció antes del momento de la firma.

Los tres tienen que dar «no válido» en la comprobación que corresponde.

**Por qué en la suite `test`:** no necesita Docker. Gitleaks permite los `.p12` solo en `src/test/resources/ca-prueba/` (D-44). JaCoCo no cambia, porque el código de producción no cambia.

## 7. Resultado del *spike*

- `specs/005-spike-firmaec/evidencia.md` tiene el resultado de cada escenario: funcionó, no funcionó o quedó pendiente, con la evidencia.
- Un comentario en el issue #26 resume el resultado y enlaza la evidencia (FR-009).
- **Si todo funciona:** la fila de la sección 8 de la constitución queda como confirmada. Es una enmienda de parche, porque solo registra un resultado, y se registra como decisión nueva.
- **Si algo no funciona:** se registra un ADR que cambia D-23 antes de cerrar (FR-010).
- **El tiempo fijo:** 2 días de trabajo (D-45). Al cumplirse, se cierra con lo que se sabe.

## 8. Lo que queda fuera

- El orden y el rol de los firmantes, la identidad por CN contra el árbol de firmas, y la interfaz de «descargar, firmar y subir», que son de F14.
- El sello de tiempo, LTV y DocMDP (constitución, sección 3).
- Las plantillas Thymeleaf del PDF/A real del distributivo (F13).
