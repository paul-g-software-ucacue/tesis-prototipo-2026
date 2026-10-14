package ec.mdd.firma.spike;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
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

/**
 * Firma PDF en las pruebas del spike con PDFBox y Bouncy Castle, para los casos que no dependen de
 * FirmaEC: el caso positivo y los negativos de FR-007 (contracts/validacion.md).
 *
 * <p>Firma de forma incremental, con un CMS separado (SHA-256 con RSA, {@code ETSI.CAdES.detached})
 * y con el {@code signingTime} que se le pida, para poder firmar «en el pasado» con un certificado
 * ya vencido. También emite en memoria una CA y certificados de firmantes ficticios.
 */
final class FirmadorDePrueba {

    private static final AtomicLong SERIE = new AtomicLong(System.currentTimeMillis());

    /** Una llave con su certificado y la cadena que lo acompaña en el CMS. */
    record Identidad(X509Certificate certificado, PrivateKey llave, List<X509Certificate> cadena) {}

    private FirmadorDePrueba() {}

    /**
     * Un PDF mínimo de una página, sin firmas.
     *
     * @return los bytes del PDF
     * @throws IOException si PDFBox no puede guardarlo
     */
    static byte[] pdfSinFirmas() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            documento.save(salida);
        }
        return salida.toByteArray();
    }

    /**
     * Agrega una firma como actualización incremental.
     *
     * @param pdf el documento, firmado o no
     * @param firmante la identidad que firma
     * @param momento el {@code signingTime} y la fecha {@code /M} de la firma
     * @return el documento con una firma más; empieza con los bytes exactos de {@code pdf}
     * @throws IOException si PDFBox no puede firmar
     */
    static byte[] firmar(byte[] pdf, Identidad firmante, Instant momento) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDSignature firma = new PDSignature();
            firma.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            firma.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            firma.setName(ValidadorPadesDePrueba.nombreComun(firmante.certificado()));
            Calendar fecha = Calendar.getInstance();
            fecha.setTimeInMillis(momento.toEpochMilli());
            firma.setSignDate(fecha);
            documento.addSignature(
                    firma, contenido -> cms(contenido.readAllBytes(), firmante, momento));
            documento.saveIncremental(salida);
        }
        return salida.toByteArray();
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
            throw new IOException("no se pudo crear el CMS", e);
        }
    }

    /**
     * Emite una CA autofirmada en memoria.
     *
     * @param nombre el CN de la CA
     * @return la identidad de la CA; su cadena es solo su certificado
     * @throws GeneralSecurityException si no se puede generar la llave o el certificado
     */
    static Identidad ca(String nombre) throws GeneralSecurityException {
        KeyPair par = llaves();
        Instant ahora = Instant.now();
        X500Name sujeto = new X500Name("CN=" + nombre + ",O=MDD Demo,C=EC");
        X509Certificate certificado =
                emitir(
                        sujeto,
                        sujeto,
                        par,
                        par.getPrivate(),
                        ahora.minus(Duration.ofDays(1)),
                        ahora.plus(Duration.ofDays(3650)),
                        true);
        return new Identidad(certificado, par.getPrivate(), List.of(certificado));
    }

    /**
     * Emite un certificado de firmante ficticio firmado por la CA.
     *
     * @param ca la CA emisora
     * @param nombre el CN del firmante
     * @param desde inicio de la vigencia
     * @param hasta fin de la vigencia
     * @return la identidad del firmante, con su certificado y el de la CA en la cadena
     * @throws GeneralSecurityException si no se puede generar la llave o el certificado
     */
    static Identidad firmante(Identidad ca, String nombre, Instant desde, Instant hasta)
            throws GeneralSecurityException {
        KeyPair par = llaves();
        X500Name emisor =
                X500Name.getInstance(ca.certificado().getSubjectX500Principal().getEncoded());
        X509Certificate certificado =
                emitir(
                        emisor,
                        new X500Name("CN=" + nombre + ",O=MDD Demo,C=EC"),
                        par,
                        ca.llave(),
                        desde,
                        hasta,
                        false);
        return new Identidad(certificado, par.getPrivate(), List.of(certificado, ca.certificado()));
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
            Instant desde,
            Instant hasta,
            boolean esCa)
            throws GeneralSecurityException {
        try {
            JcaX509v3CertificateBuilder constructor =
                    new JcaX509v3CertificateBuilder(
                            emisor,
                            BigInteger.valueOf(SERIE.incrementAndGet()),
                            Date.from(desde),
                            Date.from(hasta),
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
        } catch (OperatorCreationException | java.io.IOException e) {
            throw new GeneralSecurityException("no se pudo emitir el certificado", e);
        }
    }
}
