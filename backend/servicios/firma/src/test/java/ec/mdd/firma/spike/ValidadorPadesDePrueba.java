package ec.mdd.firma.spike;

import ec.mdd.firma.spike.ResultadoFirma.Comprobacion;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Selector;

/**
 * Validador de firmas PAdES del spike B00-S01, con PDFBox y Bouncy Castle (FR-006,
 * contracts/validacion.md, research §6).
 *
 * <p>Comprueba, por cada firma del documento: el ByteRange, la integridad, la cadena hasta la raíz
 * de confianza (PKIX, sin revocación, en el momento de la firma) y la vigencia del certificado en
 * ese momento. <b>No comprueba</b> el orden ni el rol de los firmantes, la identidad contra el
 * árbol de firmas, el sello de tiempo, LTV ni DocMDP: el orden y el rol son de F14, y lo demás está
 * fuera de alcance (constitución, sección 3). El prefijo de bytes entre revisiones se comprueba
 * fuera, comparando los archivos.
 *
 * <p>Es código de prueba: el código de producción del servicio firma no cambia (D-44).
 */
public final class ValidadorPadesDePrueba {

    /**
     * Valida las firmas del PDF contra una raíz de confianza, con los intermedios que trae cada
     * CMS.
     *
     * @param pdf los bytes del documento firmado
     * @param raiz la raíz de confianza
     * @return un resultado por firma, en el orden del documento
     * @throws IOException si PDFBox no puede leer el documento
     */
    public List<ResultadoFirma> validar(byte[] pdf, X509Certificate raiz) throws IOException {
        return validar(pdf, raiz, List.of());
    }

    /**
     * Valida las firmas del PDF contra una raíz de confianza y unos intermedios conocidos.
     *
     * @param pdf los bytes del documento firmado
     * @param raiz la raíz de confianza
     * @param intermedios certificados intermedios que el CMS podría no traer
     * @return un resultado por firma, en el orden del documento
     * @throws IOException si PDFBox no puede leer el documento
     */
    public List<ResultadoFirma> validar(
            byte[] pdf, X509Certificate raiz, Collection<X509Certificate> intermedios)
            throws IOException {
        List<ResultadoFirma> resultados = new ArrayList<>();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            List<PDSignature> firmas = documento.getSignatureDictionaries();
            for (int i = 0; i < firmas.size(); i++) {
                resultados.add(validarFirma(i + 1, firmas.get(i), pdf, raiz, intermedios));
            }
        }
        return resultados;
    }

    /**
     * Cantidad de firmas del documento.
     *
     * @param pdf los bytes del documento
     * @return la cantidad de diccionarios de firma
     * @throws IOException si PDFBox no puede leer el documento
     */
    public static int cantidadDeFirmas(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return documento.getSignatureDictionaries().size();
        }
    }

    /**
     * ByteRange de cada firma, en el orden del documento.
     *
     * @param pdf los bytes del documento
     * @return los ByteRange {@code [0, a, b, c]}
     * @throws IOException si PDFBox no puede leer el documento
     */
    public static List<int[]> byteRanges(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return documento.getSignatureDictionaries().stream()
                    .map(PDSignature::getByteRange)
                    .toList();
        }
    }

    private ResultadoFirma validarFirma(
            int indice,
            PDSignature firma,
            byte[] pdf,
            X509Certificate raiz,
            Collection<X509Certificate> intermedios)
            throws IOException {
        Comprobacion byteRange = comprobarByteRange(firma.getByteRange(), pdf);
        CMSSignedData cms;
        byte[] firmado;
        try {
            firmado = firma.getSignedContent(pdf);
            cms =
                    new CMSSignedData(
                            new CMSProcessableByteArray(firmado), sinRelleno(firma.getContents()));
        } catch (CMSException | IOException | RuntimeException e) {
            Comprobacion sinCms = Comprobacion.falla("no se pudo leer el CMS: " + e.getMessage());
            return new ResultadoFirma(
                    indice, null, momento(null, firma), byteRange, sinCms, sinCms, sinCms);
        }
        SignerInformation firmante = cms.getSignerInfos().getSigners().iterator().next();
        List<X509Certificate> certificados = certificados(cms);
        X509Certificate certificado = certificadoDelFirmante(cms, firmante);
        Instant momento = momento(firmante, firma);
        if (certificado == null) {
            Comprobacion sinCertificado =
                    Comprobacion.falla("el CMS no trae el certificado del firmante");
            return new ResultadoFirma(
                    indice,
                    null,
                    momento,
                    byteRange,
                    sinCertificado,
                    sinCertificado,
                    sinCertificado);
        }
        return new ResultadoFirma(
                indice,
                nombreComun(certificado),
                momento,
                byteRange,
                comprobarIntegridad(cms, firmante, certificado, firmado, firma.getSubFilter()),
                comprobarCadena(certificado, raiz, certificados, intermedios, momento),
                comprobarVigencia(certificado, momento));
    }

    /**
     * Lee el primer objeto ASN.1 de /Contents y descarta los ceros de relleno que lo siguen.
     *
     * <p>/Contents se reserva más grande que el CMS y se rellena con ceros; Bouncy Castle 1.86
     * rechaza esos bytes extra («Extra data detected in stream»), así que se lee un solo objeto.
     */
    private static ContentInfo sinRelleno(byte[] contenido) throws IOException {
        try (ASN1InputStream entrada = new ASN1InputStream(contenido)) {
            return ContentInfo.getInstance(entrada.readObject());
        }
    }

    private static Comprobacion comprobarByteRange(int[] rango, byte[] pdf) {
        if (rango == null || rango.length != 4) {
            return Comprobacion.falla("no tiene 4 números: " + Arrays.toString(rango));
        }
        int inicioHueco = rango[1];
        int finHueco = rango[2];
        long fin = (long) rango[2] + rango[3];
        if (rango[0] != 0 || inicioHueco <= 0 || finHueco <= inicioHueco || fin > pdf.length) {
            return Comprobacion.falla("rango fuera del archivo: " + Arrays.toString(rango));
        }
        if (pdf[inicioHueco] != '<' || pdf[finHueco - 1] != '>') {
            return Comprobacion.falla("el hueco no es exactamente /Contents");
        }
        return Comprobacion.ok();
    }

    private static Comprobacion comprobarIntegridad(
            CMSSignedData cms,
            SignerInformation firmante,
            X509Certificate certificado,
            byte[] firmado,
            String subFiltro) {
        try {
            if ("adbe.pkcs7.sha1".equals(subFiltro)) {
                // El CMS encapsula el SHA-1 del ByteRange en lugar de firmarlo separado.
                Object encapsulado = cms.getSignedContent().getContent();
                byte[] esperado = ASN1OctetString.getInstance(encapsulado).getOctets();
                byte[] calculado = MessageDigest.getInstance("SHA-1").digest(firmado);
                if (!MessageDigest.isEqual(esperado, calculado)) {
                    return Comprobacion.falla("el SHA-1 encapsulado no coincide con el ByteRange");
                }
            }
            // Con la llave pública: si se pasa el certificado, Bouncy Castle también exige que esté
            // vigente en el signingTime y mezcla la vigencia con la integridad.
            boolean verificada =
                    firmante.verify(
                            new JcaSimpleSignerInfoVerifierBuilder()
                                    .build(certificado.getPublicKey()));
            return verificada
                    ? Comprobacion.ok()
                    : Comprobacion.falla("la firma no se verifica con el certificado");
        } catch (CMSException e) {
            return Comprobacion.falla("el contenido no coincide con lo firmado: " + e.getMessage());
        } catch (OperatorCreationException | NoSuchAlgorithmException | RuntimeException e) {
            return Comprobacion.falla("no se pudo verificar: " + e.getMessage());
        }
    }

    private static Comprobacion comprobarCadena(
            X509Certificate certificado,
            X509Certificate raiz,
            List<X509Certificate> delCms,
            Collection<X509Certificate> intermedios,
            Instant momento) {
        try {
            X509CertSelector objetivo = new X509CertSelector();
            objetivo.setCertificate(certificado);
            PKIXBuilderParameters parametros =
                    new PKIXBuilderParameters(Set.of(new TrustAnchor(raiz, null)), objetivo);
            parametros.setRevocationEnabled(false); // D-23 no pide revocación
            parametros.setDate(Date.from(momento));
            List<X509Certificate> conocidos = new ArrayList<>(delCms);
            conocidos.addAll(intermedios);
            parametros.addCertStore(
                    CertStore.getInstance(
                            "Collection", new CollectionCertStoreParameters(conocidos)));
            CertPathBuilder.getInstance("PKIX").build(parametros);
            return Comprobacion.ok();
        } catch (CertPathBuilderException e) {
            return Comprobacion.falla("no hay camino hasta la raíz: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            return Comprobacion.falla("no se pudo construir la cadena: " + e.getMessage());
        }
    }

    private static Comprobacion comprobarVigencia(X509Certificate certificado, Instant momento) {
        try {
            certificado.checkValidity(Date.from(momento));
            return Comprobacion.ok();
        } catch (CertificateExpiredException e) {
            return Comprobacion.falla("vencido el " + certificado.getNotAfter().toInstant());
        } catch (CertificateNotYetValidException e) {
            return Comprobacion.falla(
                    "todavía no vigente; desde " + certificado.getNotBefore().toInstant());
        }
    }

    private static Instant momento(SignerInformation firmante, PDSignature firma) {
        if (firmante != null && firmante.getSignedAttributes() != null) {
            Attribute hora = firmante.getSignedAttributes().get(CMSAttributes.signingTime);
            if (hora != null) {
                ASN1Encodable valor = hora.getAttrValues().getObjectAt(0);
                return Time.getInstance(valor).getDate().toInstant();
            }
        }
        Calendar fecha = firma.getSignDate();
        return fecha == null ? Instant.EPOCH : fecha.toInstant();
    }

    @SuppressWarnings("unchecked")
    private static X509Certificate certificadoDelFirmante(
            CMSSignedData cms, SignerInformation firmante) {
        Collection<X509CertificateHolder> coincidencias =
                cms.getCertificates()
                        .getMatches((Selector<X509CertificateHolder>) firmante.getSID());
        return coincidencias.stream()
                .findFirst()
                .map(ValidadorPadesDePrueba::convertir)
                .orElse(null);
    }

    private static List<X509Certificate> certificados(CMSSignedData cms) {
        return cms.getCertificates().getMatches(null).stream()
                .map(ValidadorPadesDePrueba::convertir)
                .toList();
    }

    private static X509Certificate convertir(X509CertificateHolder titular) {
        try {
            return new JcaX509CertificateConverter().getCertificate(titular);
        } catch (CertificateException e) {
            throw new IllegalStateException("certificado ilegible en el CMS", e);
        }
    }

    /**
     * CN del sujeto del certificado.
     *
     * @param certificado el certificado
     * @return el CN, o el nombre completo si no tiene CN
     */
    static String nombreComun(X509Certificate certificado) {
        X500Name nombre = X500Name.getInstance(certificado.getSubjectX500Principal().getEncoded());
        var cn = nombre.getRDNs(BCStyle.CN);
        return cn.length == 0 ? nombre.toString() : cn[0].getFirst().getValue().toString();
    }
}
