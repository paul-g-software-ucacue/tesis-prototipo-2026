// Firma de prueba para los scripts de punta a punta (F14-H02, research §6, D-103). Firma un PDF como
// FirmaEC (D-46): de forma incremental, con adbe.pkcs7.detached, SHA-256 con RSA, la cadena en el CMS
// y un /Contents de 16 384 bytes. Usa los certificados ficticios de la CA de prueba
// (backend/servicios/firma/src/test/resources/ca-prueba/) o, con --ca-ajena, una CA creada en
// memoria que no está en la lista de confianza. Con --alterar, cambia un byte firmado después de
// firmar, para que la firma no sea íntegra.
//
// Corre como programa de un solo archivo en eclipse-temurin:25-jdk, con PDFBox y Bouncy Castle
// copiados del contenedor distributivo (las mismas versiones del proceso):
//
//   java -cp 'lib/*' FirmarDePrueba.java firmar <entrada.pdf> <salida.pdf> <firmante.p12> <campo>
//       [--ca-ajena] [--alterar]
//
// <campo> es el campo vacío donde firmar (firma-<orden>-<rol>), o «-» para uno nuevo. La contraseña
// ficticia del PKCS #12 llega por la variable MDD_CLAVE_P12 y nunca se imprime. Es código de
// prueba: no entra a ninguna imagen ni toca la demo.

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

/** La herramienta de firma de los scripts; ver el comentario del archivo. */
public final class FirmarDePrueba {

    private static final int CONTENIDO_RESERVADO = 16_384;

    private record Identidad(
            X509Certificate certificado, PrivateKey llave, List<X509Certificate> cadena) {}

    private FirmarDePrueba() {}

    /** Firma según los argumentos; termina con 2 si están mal y con 1 si no puede firmar. */
    public static void main(String[] argumentos) throws Exception {
        List<String> posicionales = new ArrayList<>();
        Set<String> opciones = new java.util.HashSet<>();
        for (String argumento : argumentos) {
            if (argumento.startsWith("--")) {
                opciones.add(argumento);
            } else {
                posicionales.add(argumento);
            }
        }
        if (posicionales.size() != 5
                || !posicionales.getFirst().equals("firmar")
                || !Set.of("--ca-ajena", "--alterar").containsAll(opciones)) {
            System.err.println(
                    "Uso: FirmarDePrueba firmar <entrada.pdf> <salida.pdf> <firmante.p12> <campo|->"
                            + " [--ca-ajena] [--alterar]");
            System.exit(2);
        }
        byte[] entrada = Files.readAllBytes(Path.of(posicionales.get(1)));
        Identidad firmante =
                opciones.contains("--ca-ajena")
                        ? deCaAjena()
                        : deP12(Path.of(posicionales.get(3)), clave());
        String campo = posicionales.get(4).equals("-") ? null : posicionales.get(4);
        byte[] firmado = firmar(entrada, firmante, campo);
        if (opciones.contains("--alterar")) {
            firmado = alterar(firmado, entrada.length);
        }
        Files.write(Path.of(posicionales.get(2)), firmado);
    }

    private static char[] clave() {
        String clave = System.getenv("MDD_CLAVE_P12");
        if (clave == null || clave.isEmpty()) {
            System.err.println("Falta MDD_CLAVE_P12, la contraseña ficticia de la CA de prueba");
            System.exit(2);
        }
        return clave.toCharArray();
    }

    private static byte[] firmar(byte[] pdf, Identidad firmante, String campo) throws IOException {
        Instant momento = Instant.now();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PDDocument documento = Loader.loadPDF(pdf);
                SignatureOptions opciones = new SignatureOptions()) {
            opciones.setPreferredSignatureSize(CONTENIDO_RESERVADO);
            PDSignature firma = new PDSignature();
            firma.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            firma.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            firma.setName(firmante.certificado().getSubjectX500Principal().getName());
            Calendar fecha = Calendar.getInstance();
            fecha.setTimeInMillis(momento.toEpochMilli());
            firma.setSignDate(fecha);
            if (campo != null) {
                PDAcroForm formulario = documento.getDocumentCatalog().getAcroForm(null);
                PDField existente = formulario == null ? null : formulario.getField(campo);
                if (!(existente instanceof PDSignatureField vacio) || vacio.getSignature() != null) {
                    throw new IOException("No hay un campo de firma vacío llamado " + campo);
                }
                vacio.getCOSObject().setItem(COSName.V, firma);
            }
            documento.addSignature(
                    firma, contenido -> cms(contenido.readAllBytes(), firmante, momento), opciones);
            documento.saveIncremental(salida);
        }
        return salida.toByteArray();
    }

    // Cambia un dígito de la fecha /M de la firma nueva: está dentro de lo firmado y después del
    // vigente, así que el archivo sigue empezando con el vigente pero la firma deja de ser íntegra.
    private static byte[] alterar(byte[] firmado, int desde) {
        byte[] copia = firmado.clone();
        String texto = new String(copia, StandardCharsets.ISO_8859_1);
        int posicion = texto.indexOf("/M (D:", desde);
        if (posicion < 0) {
            throw new IllegalStateException("La firma nueva no tiene fecha /M");
        }
        int digito = posicion + "/M (D:".length();
        copia[digito] = (byte) (copia[digito] == '1' ? '2' : '1');
        return copia;
    }

    private static Identidad deP12(Path p12, char[] clave)
            throws GeneralSecurityException, IOException {
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        try (InputStream entrada = Files.newInputStream(p12)) {
            almacen.load(entrada, clave);
        }
        String alias = Collections.list(almacen.aliases()).getFirst();
        List<X509Certificate> cadena = new ArrayList<>();
        for (Certificate uno : almacen.getCertificateChain(alias)) {
            cadena.add((X509Certificate) uno);
        }
        PrivateKey llave = (PrivateKey) almacen.getKey(alias, clave);
        return new Identidad(cadena.getFirst(), llave, cadena);
    }

    private static Identidad deCaAjena() throws GeneralSecurityException {
        Instant ahora = Instant.now();
        KeyPair deLaCa = llaves();
        X500Name ca = new X500Name("CN=CA Ajena de Prueba,O=MDD Demo,C=EC");
        X509Certificate certificadoDeLaCa =
                emitir(ca, ca, deLaCa, deLaCa.getPrivate(), ahora, Duration.ofDays(3650), true);
        KeyPair delFirmante = llaves();
        X509Certificate certificado =
                emitir(
                        ca,
                        new X500Name("CN=Firmante Ajeno de Prueba,O=MDD Demo,C=EC"),
                        delFirmante,
                        deLaCa.getPrivate(),
                        ahora,
                        Duration.ofDays(365),
                        false);
        return new Identidad(
                certificado, delFirmante.getPrivate(), List.of(certificado, certificadoDeLaCa));
    }

    private static byte[] cms(byte[] contenido, Identidad firmante, Instant momento)
            throws IOException {
        try {
            ASN1EncodableVector atributos = new ASN1EncodableVector();
            atributos.add(
                    new Attribute(
                            CMSAttributes.signingTime, new DERSet(new Time(Date.from(momento)))));
            CMSSignedDataGenerator generador = new CMSSignedDataGenerator();
            generador.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                                    new JcaDigestCalculatorProviderBuilder().build())
                            .setSignedAttributeGenerator(
                                    new DefaultSignedAttributeTableGenerator(
                                            new AttributeTable(atributos)))
                            .build(
                                    new JcaContentSignerBuilder("SHA256withRSA")
                                            .build(firmante.llave()),
                                    firmante.certificado()));
            generador.addCertificates(new JcaCertStore(firmante.cadena()));
            return generador
                    .generate(new CMSProcessableByteArray(contenido), false)
                    .getEncoded(ASN1Encoding.DER);
        } catch (GeneralSecurityException | OperatorCreationException | CMSException e) {
            throw new IOException("No se pudo crear el CMS", e);
        }
    }

    private static KeyPair llaves() throws GeneralSecurityException {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return generador.generateKeyPair();
    }

    private static X509Certificate emitir(
            X500Name emisor,
            X500Name sujeto,
            KeyPair par,
            PrivateKey llaveEmisor,
            Instant ahora,
            Duration vigencia,
            boolean esCa)
            throws GeneralSecurityException {
        try {
            JcaX509v3CertificateBuilder constructor =
                    new JcaX509v3CertificateBuilder(
                            emisor,
                            BigInteger.valueOf(System.nanoTime()),
                            Date.from(ahora.minus(Duration.ofDays(1))),
                            Date.from(ahora.plus(vigencia)),
                            sujeto,
                            par.getPublic());
            constructor.addExtension(Extension.basicConstraints, true, new BasicConstraints(esCa));
            constructor.addExtension(
                    Extension.keyUsage,
                    true,
                    new KeyUsage(
                            esCa
                                    ? KeyUsage.keyCertSign | KeyUsage.cRLSign
                                    : KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
            return new JcaX509CertificateConverter()
                    .getCertificate(
                            constructor.build(
                                    new JcaContentSignerBuilder("SHA256withRSA")
                                            .build(llaveEmisor)));
        } catch (OperatorCreationException | IOException e) {
            throw new GeneralSecurityException("No se pudo emitir el certificado", e);
        }
    }
}
