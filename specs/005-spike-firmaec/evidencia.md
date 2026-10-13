# Evidencia: B00-S01 · Prueba temprana de firma con FirmaEC

**Issue:** #26 · **Rama:** `005-spike-firmaec` · **Tiempo fijo:** 2 días de trabajo (D-45)

Resultado de cada escenario: **funcionó**, **no funcionó** o **pendiente**. Nada de lo que aparece aquí proviene de un certificado real; si se usó el camino de D-43, solo se registran la comparación de bytes y el resultado de la validación, sin el CN real.

## Entorno

| Pieza | Versión |
|---|---|
| Sistema | Fedora 44 |
| OpenSSL | 3.5.9 (29/09/2026) |
| JDK | 25 |
| PDFBox / Bouncy Castle / OpenHTMLtoPDF / veraPDF | 3.0.8 / 1.86 / 1.1.87 / 1.30.2 |
| FirmaEC | 5.3.0-1, paquete RPM oficial (`firmaec`, de firmadigital.gob.ec), instalado el 16/09/2026 en `/usr/share/firmaec`, con su propia JRE (Temurin 17.0.13). Lanzadores: «FirmaEC» (`firmador`) y «FirmaECTransversal» (`firmaec`). |

## Escenario 1 · Certificados de la demo

**Estado:** pendiente (falta la prueba en FirmaEC, T008)

**Preparación (T006 y T007).** `generar.sh` creó la CA «CA de Prueba MDD Demo» (RSA 3072, vigente hasta el 10/10/2036, `CA:TRUE`, `keyCertSign, cRLSign`) y tres firmantes ficticios (RSA 2048, vigentes hasta el 12/10/2029, `digitalSignature, nonRepudiation`). La llave de la CA se borró al terminar.

| Archivo | Cifrado del PKCS #12 | `openssl verify -CAfile ca-prueba.crt` | Java (`keytool`/`KeyStore`) |
|---|---|---|---|
| `firmante-{1,2,3}.p12` | PBES2, PBKDF2, AES-256-CBC, MAC SHA-256 | OK los tres | se lee |
| `firmante-{1,2,3}-legacy.p12` | PBE-SHA1-3DES, MAC SHA-1 | OK los tres | se lee |

- Shellcheck sobre una copia de `generar.sh`: sin observaciones.
- Gitleaks 8.30.1 con `.gitleaks.toml` sobre un clon: `no leaks found`. Control: sin la lista permitida, los mismos archivos dan 6 hallazgos, así que lo que los admite es la carpeta `ca-prueba/`.

**Prueba en FirmaEC (T008):** pendiente del autor.

**Qué cabe esperar, por la inspección del paquete** (sin ejecutarlo):
- FirmaEC reconoce por el nombre del emisor a las entidades acreditadas (BCE, Security Data, ANF AC, Uanataca, Datil, Lazzate y otras) y tiene un caso «desconocido» para las demás.
- Consulta en línea si el certificado está revocado (`/certificado/fechaRevocado`).
- Con la CA de prueba, puede firmar tratándola como emisor desconocido, o rechazarla con `CertificadoInvalidoException`. Eso es lo que responde este escenario.

| Certificado | Variante | ¿FirmaEC firma? | Mensaje |
|---|---|---|---|
| F1 | | | |
| F2 | | | |
| F3 | | | |

## Escenario 2 · Firma incremental

**Estado:** pendiente (falta firmar con FirmaEC, T011)

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

**Estado:** pendiente (validador listo; falta correrlo sobre las firmas de FirmaEC, T016)

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

**Estado:** pendiente

## Tiempo usado

| Día | Trabajo |
|---|---|

## Notas
