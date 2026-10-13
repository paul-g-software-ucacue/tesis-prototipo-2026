# Quickstart: ejecutar B00-S01

Guía para hacer el *spike* de punta a punta en 2 días (D-45). Los pasos manuales están en [contracts/procedimiento-firma.md](contracts/procedimiento-firma.md) y la validación en [contracts/validacion.md](contracts/validacion.md). Los resultados van a `evidencia.md`.

## Requisitos previos

- JDK 25 y Gradle, como en las historias anteriores; OpenSSL 3.
- FirmaEC instalado en la máquina del autor.

## 1. CA de prueba (escenario 1)

```bash
cd backend/servicios/firma/src/test/resources/ca-prueba
./generar.sh            # crea ca-prueba.crt, firmante-{1,2,3}.p12 y sus variantes -legacy; borra la llave de la CA
openssl pkcs12 -in firmante-1.p12 -passin pass:cambiar-en-local-firma -nokeys | openssl x509 -noout -subject -issuer -dates
```

Después del paso 2, firmar en FirmaEC una copia desechable del PDF/A con cada `.p12` (y con su variante `-legacy` si falla) y anotar el resultado.

## 2. PDF/A de prueba

```bash
cd backend
./gradlew :servicios:firma:test --tests '*PdfaDePruebaTest'
cp servicios/firma/build/spike-firma/pdfa-prueba.pdf servicios/firma/src/test/resources/ca-prueba/
```

**Resultado esperado:** la prueba pasa (veraPDF: PDF/A-2b conforme).

## 3. Validador y casos negativos

```bash
./gradlew :servicios:firma:test --tests '*CasosNegativosTest'
```

**Resultado esperado:** los 3 casos negativos se detectan.

## 4. Firma con FirmaEC y validación (escenarios 2 y 3)

1. Firmar dos veces con FirmaEC según el procedimiento, y copiar `pdfa-firmado-1.pdf` y `pdfa-firmado-2.pdf` a `ca-prueba/`.
2. Ejecutar:

   ```bash
   ./gradlew :servicios:firma:test --tests '*FirmasFirmaecTest'
   ```

**Resultado esperado:** prefijo de bytes igual, una firma más, y las 2 firmas íntegras, con cadena válida hasta `ca-prueba.crt` y vigentes.

**Con el certificado real (D-43), en local** (el `build.gradle` reenvía las propiedades `mdd.firma.*` a la prueba):

```bash
./gradlew :servicios:firma:test --tests '*FirmasFirmaecTest' \
  -Dmdd.firma.pdf1=$HOME/spike/firmado-1.pdf -Dmdd.firma.pdf2=$HOME/spike/firmado-2.pdf -Dmdd.firma.raiz=$HOME/spike/raiz.crt
```

## 5. Cierre (escenario 4)

- `evidencia.md` con el resultado de cada escenario.
- Comentario en el issue #26 con el resumen y el enlace a la evidencia.
- La sección 8 de la constitución, confirmada; o un ADR si D-23 cambia.
- Pull request con la CI en verde (`3 · secretos` acepta los `.p12` de `ca-prueba/`).
