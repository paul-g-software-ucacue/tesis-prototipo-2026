package ec.mdd.firma.pruebas;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
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
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
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

/**
 * Firma PDF en las pruebas de F14-H02 como lo hace FirmaEC (D-46): de forma incremental, con {@code
 * adbe.pkcs7.detached}, SHA-256 con RSA, la cadena completa dentro del CMS y un {@code /Contents}
 * reservado más grande que el CMS, relleno de ceros.
 *
 * <p>Firma en el campo vacío que se le pida (los {@code firma-<orden>-<rol>} del PDF/A de F13-H02)
 * o en uno nuevo. Emite en memoria una CA y firmantes ficticios, y lee los de la CA de prueba
 * ({@code ca-prueba/}, con su contraseña ficticia). Es código de prueba: nunca entra a la imagen.
 */
public final class FirmasDePrueba {

    /** La contraseña ficticia de los PKCS #12 de la CA de prueba ({@code ca-prueba/generar.sh}). */
    public static final String CLAVE_CA_PRUEBA = "cambiar-en-local-firma";

    // El tamaño que reserva FirmaEC para /Contents (D-46, hallazgo 2).
    private static final int CONTENIDO_RESERVADO = 16_384;

    private static final AtomicLong SERIE = new AtomicLong(System.currentTimeMillis());

    /**
     * Una llave con su certificado y la cadena que va en el CMS.
     *
     * @param certificado el del firmante
     * @param llave su llave privada
     * @param cadena el certificado y sus emisores, sin la raíz si se quiere
     */
    public record Identidad(
            X509Certificate certificado, PrivateKey llave, List<X509Certificate> cadena) {

        /** Copia la cadena. */
        public Identidad {
            Objects.requireNonNull(certificado, "certificado");
            Objects.requireNonNull(llave, "llave");
            cadena = List.copyOf(cadena);
        }
    }

    private FirmasDePrueba() {}

    /**
     * Agrega una firma como actualización incremental, ahora.
     *
     * @param pdf el documento, firmado o no
     * @param firmante la identidad que firma
     * @param campo el nombre del campo vacío donde firmar, o {@code null} para uno nuevo
     * @return el documento con una firma más; empieza con los bytes exactos de {@code pdf}
     * @throws IOException si PDFBox no puede firmar o el campo no existe
     */
    public static byte[] firmar(byte[] pdf, Identidad firmante, String campo) throws IOException {
        return firmar(pdf, firmante, campo, Instant.now());
    }

    /**
     * Agrega una firma como actualización incremental, con el {@code signingTime} que se pida.
     *
     * @param pdf el documento, firmado o no
     * @param firmante la identidad que firma
     * @param campo el nombre del campo vacío donde firmar, o {@code null} para uno nuevo
     * @param momento el {@code signingTime} y la fecha {@code /M}
     * @return el documento con una firma más; empieza con los bytes exactos de {@code pdf}
     * @throws IOException si PDFBox no puede firmar o el campo no existe
     */
    public static byte[] firmar(byte[] pdf, Identidad firmante, String campo, Instant momento)
            throws IOException {
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
                PDSignatureField existente = campoVacio(documento, campo);
                existente.getCOSObject().setItem(COSName.V, firma);
            }
            documento.addSignature(
                    firma, contenido -> cms(contenido.readAllBytes(), firmante, momento), opciones);
            documento.saveIncremental(salida);
        }
        return salida.toByteArray();
    }

    /**
     * Cambia un byte dentro de lo que cubre la última firma, antes de su {@code /Contents}: la
     * firma deja de ser íntegra, pero el archivo empieza igual con los bytes del vigente si el byte
     * está después de él.
     *
     * @param firmado el documento firmado
     * @param desde el primer byte que se puede cambiar (el largo del vigente)
     * @return una copia con un byte cambiado
     */
    public static byte[] alterar(byte[] firmado, int desde) {
        byte[] copia = firmado.clone();
        String texto = new String(copia, java.nio.charset.StandardCharsets.ISO_8859_1);
        int posicion = texto.indexOf("/M (D:", desde);
        if (posicion < 0) {
            throw new IllegalArgumentException(
                    "la firma nueva no tiene fecha /M después del vigente");
        }
        int digito = posicion + "/M (D:".length();
        copia[digito] = (byte) (copia[digito] == '1' ? '2' : '1');
        return copia;
    }

    /**
     * Emite una CA autofirmada en memoria.
     *
     * @param nombre el CN de la CA
     * @return la identidad de la CA; su cadena es solo su certificado
     * @throws GeneralSecurityException si no se puede emitir
     */
    public static Identidad ca(String nombre) throws GeneralSecurityException {
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
     * Emite un firmante ficticio de la CA, vigente desde ayer por un año.
     *
     * @param ca la CA emisora
     * @param nombre el CN del firmante
     * @return su identidad, con su certificado y el de la CA en la cadena
     * @throws GeneralSecurityException si no se puede emitir
     */
    public static Identidad firmante(Identidad ca, String nombre) throws GeneralSecurityException {
        return firmante(ca, new X500Name("CN=" + nombre + ",O=MDD Demo,C=EC"));
    }

    /**
     * Emite un firmante ficticio con el sujeto que se pida, por ejemplo con el CN y el número de
     * serie en el mismo componente, como los certificados de la ECIBCE.
     *
     * @param ca la CA emisora
     * @param sujeto el nombre completo del sujeto
     * @return su identidad, con su certificado y el de la CA en la cadena
     * @throws GeneralSecurityException si no se puede emitir
     */
    public static Identidad firmante(Identidad ca, X500Name sujeto)
            throws GeneralSecurityException {
        Instant ahora = Instant.now();
        KeyPair par = llaves();
        X500Name emisor =
                X500Name.getInstance(ca.certificado().getSubjectX500Principal().getEncoded());
        X509Certificate certificado =
                emitir(
                        emisor,
                        sujeto,
                        par,
                        ca.llave(),
                        ahora.minus(Duration.ofDays(1)),
                        ahora.plus(Duration.ofDays(365)),
                        false);
        return new Identidad(certificado, par.getPrivate(), List.of(certificado, ca.certificado()));
    }

    /**
     * Lee un firmante de un PKCS #12 de la CA de prueba.
     *
     * @param p12 el archivo, por ejemplo el recurso {@code /ca-prueba/firmante-1.p12}
     * @return su identidad, con la cadena que trae el archivo
     * @throws GeneralSecurityException si la contraseña o el archivo no sirven
     * @throws IOException si no se puede leer
     */
    public static Identidad deP12(InputStream p12) throws GeneralSecurityException, IOException {
        KeyStore almacen = KeyStore.getInstance("PKCS12");
        almacen.load(p12, CLAVE_CA_PRUEBA.toCharArray());
        String alias = Collections.list(almacen.aliases()).getFirst();
        List<X509Certificate> cadena = new ArrayList<>();
        for (Certificate uno : almacen.getCertificateChain(alias)) {
            cadena.add((X509Certificate) uno);
        }
        PrivateKey llave = (PrivateKey) almacen.getKey(alias, CLAVE_CA_PRUEBA.toCharArray());
        return new Identidad(cadena.getFirst(), llave, cadena);
    }

    /**
     * Un certificado en PEM, para escribir una lista de confianza de prueba.
     *
     * @param certificado el certificado
     * @return su texto PEM
     * @throws GeneralSecurityException si no se puede codificar
     */
    public static String pem(X509Certificate certificado) throws GeneralSecurityException {
        return "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(
                                64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                        .encodeToString(certificado.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
    }

    private static PDSignatureField campoVacio(PDDocument documento, String nombre)
            throws IOException {
        PDAcroForm formulario = documento.getDocumentCatalog().getAcroForm(null);
        PDField campo = formulario == null ? null : formulario.getField(nombre);
        if (!(campo instanceof PDSignatureField firma) || firma.getSignature() != null) {
            throw new IOException("no hay un campo de firma vacío llamado " + nombre);
        }
        return firma;
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
        } catch (OperatorCreationException | IOException e) {
            throw new GeneralSecurityException("no se pudo emitir el certificado", e);
        }
    }
}
