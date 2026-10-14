# Evidencia: B00-S01 · Prueba temprana de firma con FirmaEC

**Issue:** #26 · **Rama:** `005-spike-firmaec` · **Tiempo fijo:** 2 días de trabajo (D-45)

Resultado de cada escenario: **funcionó**, **no funcionó** o **pendiente**. Aquí no hay datos personales. Los escenarios 2 y 3 siguieron el camino de D-43 con el certificado real del autor: se registran solo los tamaños, la comparación de bytes, el resultado de la validación y los datos públicos de la entidad emisora, nunca el CN del titular. Los PDF firmados y el certificado quedan fuera del repositorio.

## Entorno

| Pieza | Versión |
|---|---|
| Sistema | Fedora 44 |
| OpenSSL | 3.5.9 (29/09/2026) |
| JDK | 25 |
| PDFBox / Bouncy Castle / OpenHTMLtoPDF / veraPDF | 3.0.8 / 1.86 / 1.1.87 / 1.30.2 |
| FirmaEC | 5.3.0-1, paquete RPM oficial (`firmaec`, de firmadigital.gob.ec), instalado el 16/09/2026 en `/usr/share/firmaec`, con su propia JRE (Temurin 17.0.13). Lanzadores: «FirmaEC» (`firmador`) y «FirmaECTransversal» (`firmaec`). |

## Escenario 1 · Certificados de la demo

**Estado:** **no funcionó**: FirmaEC rechaza la CA de prueba con «Entidad Certificadora no reconocida».

**Preparación (T006 y T007).** `generar.sh` creó la CA «CA de Prueba MDD Demo» (RSA 3072, vigente hasta el 10/10/2036, `CA:TRUE`, `keyCertSign, cRLSign`) y tres firmantes ficticios (RSA 2048, vigentes hasta el 12/10/2029, `digitalSignature, nonRepudiation`). La llave de la CA se borró al terminar.

| Archivo | Cifrado del PKCS #12 | `openssl verify -CAfile ca-prueba.crt` | Java (`keytool`/`KeyStore`) |
|---|---|---|---|
| `firmante-{1,2,3}.p12` | PBES2, PBKDF2, AES-256-CBC, MAC SHA-256 | OK los tres | se lee |
| `firmante-{1,2,3}-legacy.p12` | PBE-SHA1-3DES, MAC SHA-1 | OK los tres | se lee |

- Shellcheck sobre una copia de `generar.sh`: sin observaciones.
- Gitleaks 8.30.1 con `.gitleaks.toml` sobre un clon: `no leaks found`. Control: sin la lista permitida, los mismos archivos dan 6 hallazgos, así que lo que los admite es la carpeta `ca-prueba/`.

**Qué cabe esperar, por la inspección del paquete** (sin ejecutarlo):
- FirmaEC reconoce por el nombre del emisor a las entidades acreditadas (BCE, Security Data, ANF AC, Uanataca, Datil, Lazzate y otras) y tiene un caso «desconocido» para las demás.
- Consulta en línea si el certificado está revocado (`/certificado/fechaRevocado`).
- Con la CA de prueba, puede firmar tratándola como emisor desconocido, o rechazarla con `CertificadoInvalidoException`. Eso es lo que responde este escenario.

**Prueba en FirmaEC 5.3.0 (T008, 13/10/2026):** «Firmar Documento» → «Certificado en: Archivo» → `.p12` de la CA de prueba con su contraseña → diálogo de error **«Entidad Certificadora no reconocida»** (captura del autor).

- FirmaEC pudo abrir el archivo, porque leyó el emisor, así que no es un problema de formato y la variante `-legacy` no cambia nada.
- Los tres certificados tienen el mismo emisor, por lo que el resultado es el mismo para F1, F2 y F3.
- Se confirma lo que mostraba la inspección: FirmaEC solo acepta las entidades acreditadas que conoce.
- Descartado: poner a la CA de prueba el nombre de una entidad acreditada. Sería suplantar a una organización real, y además FirmaEC consulta la revocación en línea.

**Consecuencia:** los firmantes ficticios de la demo no pueden firmar con FirmaEC. Se decide en el escenario 4 (ADR).

## Escenario 2 · Firma incremental

**Estado:** **funcionó**, por el camino de D-43: con el certificado real del autor, solo en local y fuera del repositorio.

**Firma con FirmaEC (T011, 13/10/2026).** El autor firmó tres veces seguidas con su certificado, cada vez sobre el resultado anterior. Desviación: las tres firmas son del mismo certificado, porque el autor tiene uno solo.
- **Opciones:** firma visible con código QR y el texto «Firmado electrónicamente por…», en tres posiciones de la página (abajo a la izquierda, abajo a la derecha y abajo al centro).
- **Firma:** `SubFilter /adbe.pkcs7.detached`; `/Contents` reservado de 16 384 bytes, con un CMS de 11 039 bytes que incluye la cadena completa (raíz y AC del BCE).
- **Emisor:** AC BANCO CENTRAL DEL ECUADOR.

| Archivo (fuera del repositorio) | Tamaño | Firmas |
|---|---|---|
| `pdfa-prueba.pdf` (original) | 18 742 bytes | 0 |
| `firmado-1.pdf` | 73 667 bytes | 1 |
| `firmado-2.pdf` | 128 448 bytes | 2 |
| `firmado-3.pdf` | 183 236 bytes | 3 |

**Resultado de `FirmasFirmaecTest` en modo local (T016)**, en dos tramos: original → 1 → 2 y 1 → 2 → 3. En cada tramo, 4 de 4 pruebas en verde:
- cada archivo empieza con los bytes exactos del anterior;
- cada archivo tiene una firma más;
- el ByteRange de la firma anterior termina justo donde termina el archivo anterior (revisión cerrada);
- la última firma cubre hasta el final del archivo.

**Los PDF firmados se guardaron primero por error dentro del repositorio** (`ca-prueba/` y `resources/spike/`). Nunca se agregaron a Git y se movieron a `~/spike/` antes de cualquier commit.

**PDF/A de prueba (T009 y T010).** `PdfaDePruebaTest` lo genera con OpenHTMLtoPDF 1.1.87 (PDF 1.6, DejaVu Sans incrustada, perfil sRGB, XMP con `pdfaid:part=2` y `pdfaid:conformance=B`), y veraPDF 1.30.2 lo da **conforme a PDF/A-2b**. Control: un PDF común de PDFBox no pasa.

| Archivo | Tamaño | SHA-256 |
|---|---|---|
| `ca-prueba/pdfa-prueba.pdf` | 18 742 bytes | `774a78f5ed5752fca545c4e97142b5ffc48cbb360252180b9ac97274c298ce56` |

**Ensayo sin FirmaEC.** `pdfa-prueba.pdf` se firmó con PDFBox: primero con `firmante-1.p12` y después con `firmante-2-legacy.p12`. Los dos PDF quedaron en `build/` y no se versionan. `FirmasFirmaecTest`, en modo local, pasó las 4 pruebas:
- prefijo de bytes;
- una firma más en cada paso;
- la revisión de la primera firma está cerrada;
- las 2 firmas son íntegras, tienen cadena hasta `ca-prueba.crt` y están vigentes.

Esto confirma que la CA, los `.p12` (moderno y legacy), el PDF/A y el validador funcionan juntos. Falta lo que solo FirmaEC puede responder.

## Escenario 3 · Validación con las librerías del stack

**Estado:** **funcionó**.

**Firmas reales de FirmaEC (T016, modo local, D-43).**
- **Ancla de confianza:** la raíz del BCE que trae FirmaEC (`CaCertBce`), no la que viene en el PDF; la AC del BCE va como intermedio. Huellas SHA-256, que coinciden con las del CMS:
  - raíz «AUTORIDAD DE CERTIFICACION RAIZ DEL BANCO CENTRAL DEL ECUADOR»: `11:C7:C5:9B:E9:D2:1D:21:6F:0E:81:51:37:8D:53:D0:3B:31:40:60:55:9A:DC:49:DA:16:1E:C4:F7:82:9B:EC`;
  - «AC BANCO CENTRAL DEL ECUADOR» (2019-2029): `FA:E2:44:1B:47:0F:ED:69:CC:E8:62:98:48:94:1C:D7:93:0A:86:F3:5F:6A:D9:A6:8D:7F:A7:17:A2:6E:E2:D1`.

| Firma | Momento (UTC) | ByteRange | Integridad | Cadena hasta la raíz del BCE | Vigencia |
|---|---|---|---|---|---|
| 1 | 2026-10-14 03:22:51 | válida | válida | válida | válida |
| 2 | 2026-10-14 03:24:01 | válida | válida | válida | válida |
| 3 | 2026-10-14 03:25:44 | válida | válida | válida | válida |

**Control:** con `ca-prueba.crt` como raíz, las mismas firmas dan «cadena no válida» y el resto sigue válido. El CN del titular no se imprime ni se registra.

**Casos negativos (T013 y T014), con firmas generadas en la prueba:**

| Caso | Resultado |
|---|---|
| Dos firmas válidas e incrementales (positivo) | las 2 válidas en las cuatro comprobaciones; prefijo de bytes y revisión cerrada |
| Un byte alterado dentro del ByteRange | integridad **no válida** |
| Otra raíz de confianza | cadena **no válida** (la integridad sigue válida) |
| Certificado vencido antes del momento de la firma | vigencia **no válida** (la integridad sigue válida) |

**Hallazgos técnicos para F14:**
1. **Relleno de `/Contents`.** El CMS ocupa menos que el espacio reservado, y el resto son ceros. Bouncy Castle 1.86 rechaza esos bytes («Extra data detected in stream»). Hay que leer un solo objeto ASN.1 (`ASN1InputStream.readObject`) y descartar el resto; FirmaEC también rellena.
2. **Integridad y vigencia separadas.** `SignerInformation.verify` con un verificador construido desde el *certificado* también exige que esté vigente en el `signingTime`, y un certificado vencido aparece como «firma no íntegra». Con la llave pública, cada comprobación informa lo suyo, como pide D-23.
3. **veraPDF y JAXB.** veraPDF usa JAXB 2.3 (`javax.xml.bind`), pero el BOM de Spring Boot 4.1 sube `jaxb-impl` a la 4.x (`jakarta`), y veraPDF ya no carga sus perfiles («PDFAFlavour 2b is not supported»). Se fija JAXB 2.3.9 solo en el classpath de las pruebas de `firma`. F13 lo va a necesitar si verifica el PDF/A en el servicio.

## Escenario 4 · Resultado del spike

**Estado:** **funcionó**.

| Escenario | Resultado |
|---|---|
| 1 · Certificados de la demo | **No funcionó:** FirmaEC rechaza la CA de prueba («Entidad Certificadora no reconocida») |
| 2 · Firma incremental | **Funcionó** con el certificado real (D-43): cada archivo empieza con los bytes exactos del anterior |
| 3 · Validación con PDFBox y Bouncy Castle | **Funcionó:** integridad, cadena hasta la raíz del BCE y vigencia de las 3 firmas; los casos negativos se detectan |

- **D-23 no cambia.** La comprobación de la sección 8 de la constitución queda confirmada (D-46, enmienda 2.1.1).
- **ADR-017 resuelve el escenario 1:**
  - la validación es la misma en todos los entornos;
  - en la instalación de pruebas del autor, una misma persona ocupa F1, F2 y F3 con su certificado real, y esa asignación se carga desde un archivo local no versionado;
  - la CI y las pruebas de punta a punta firman con `FirmadorDePrueba` y la CA de prueba.
- **Hallazgos para F14:** en D-46 (relleno de `/Contents`, verificación con la llave pública, JAXB para veraPDF, formato de las firmas de FirmaEC).

## Tiempo usado

| Día | Trabajo |
|---|---|
| 13/10/2026 | Especificación, plan, tareas y análisis; CA de prueba, PDF/A y validador; prueba en FirmaEC; firmas reales y validación; cierre |

Dentro del tiempo fijo de 2 días (D-45, SC-005): se cerró en 1 día calendario. No quedaron tareas pendientes del spike.

## Notas

- Las tres firmas reales son del mismo certificado, porque el autor tiene uno solo. Es la desviación prevista en D-43, y ADR-017 la adopta para las pruebas del autor.
- No se comprobó si el PDF sigue cumpliendo PDF/A-2b después de firmarlo con FirmaEC. No estaba en el alcance del spike y queda como pregunta para F13 y F14.
