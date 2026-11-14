package ec.mdd.firma.adaptadores.salida;

import static ec.mdd.firma.dominio.Comprobacion.CONFIANZA;
import static ec.mdd.firma.dominio.Comprobacion.INTEGRIDAD;
import static ec.mdd.firma.dominio.Comprobacion.UNA_FIRMA_NUEVA;

import ec.mdd.firma.dominio.CertificadoDeLaFirma;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La validación básica de la firma nueva con PDFBox y Bouncy Castle (D-23, D-103), a partir del
 * validador del spike B00-S01, probado con las firmas reales de FirmaEC (D-46).
 *
 * <ul>
 *   <li><b>Una sola firma nueva:</b> el archivo subido tiene un diccionario de firma más que el
 *       vigente. Se cuentan firmas, no campos: los campos vacíos del PDF/A de F13-H02 no tienen
 *       diccionario, así que vale si el firmante llena el campo de su paso o agrega uno.
 *   <li><b>Integridad:</b> la firma nueva es la que termina en el final del archivo: su ByteRange
 *       es {@code [0, a, b, c]}, cubre todo el vigente, su hueco es exactamente {@code /Contents} y
 *       {@code b + c} es el largo del archivo. Su CMS se lee como un solo objeto ASN.1, sin el
 *       relleno de ceros (D-46, hallazgo 3), y se verifica con la llave pública del certificado,
 *       sin mezclar la vigencia (hallazgo 4).
 *   <li><b>Confianza:</b> PKIX desde el certificado, con los intermedios del CMS (hallazgo 2),
 *       hasta una raíz de la lista, sin revocación (ADR-007), a la fecha del {@code signingTime}
 *       firmado. La vigencia al subir es otra comprobación, de F14-H03: así el resultado dice cuál
 *       falla.
 * </ul>
 *
 * <p>Un archivo malo nunca lanza: es un rechazo con un motivo fijo. El mensaje de la biblioteca
 * solo va al registro, nunca al resultado (RNF-SEG-04).
 */
public final class ValidadorPades implements ValidadorDeFirmas {

    private static final Logger LOG = LoggerFactory.getLogger(ValidadorPades.class);

    private final ListaConfianza lista;

    /**
     * Recibe la lista de confianza del entorno.
     *
     * @param lista las raíces, leídas al arrancar
     */
    public ValidadorPades(ListaConfianza lista) {
        this.lista = Objects.requireNonNull(lista, "lista");
    }

    @Override
    public HallazgosDeLaFirma validar(byte[] vigente, byte[] subido) {
        int firmasDelVigente = cantidadDeFirmas(vigente);
        try (PDDocument documento = Loader.loadPDF(subido)) {
            List<PDSignature> firmas = documento.getSignatureDictionaries();
            int nuevas = firmas.size() - firmasDelVigente;
            if (nuevas != 1) {
                return rechazo(
                        UNA_FIRMA_NUEVA,
                        nuevas < 1
                                ? "El archivo no tiene una firma nueva."
                                : "El archivo tiene " + nuevas + " firmas nuevas; debe tener una.");
            }
            return revisarFirmaNueva(firmas, vigente.length, subido);
        } catch (IOException e) {
            LOG.info("El PDF subido no se puede leer: {}", e.getMessage());
            return rechazo(
                    UNA_FIRMA_NUEVA,
                    "El archivo no se puede leer como PDF después de la versión vigente.");
        }
    }

    private static int cantidadDeFirmas(byte[] vigente) {
        try (PDDocument documento = Loader.loadPDF(vigente)) {
            return documento.getSignatureDictionaries().size();
        } catch (IOException e) {
            // El vigente es un archivo propio, verificado por su SHA-256: si no se lee, es un fallo
            // del sistema, no del firmante.
            throw new IllegalStateException("El archivo vigente no se puede leer", e);
        }
    }

    private HallazgosDeLaFirma revisarFirmaNueva(
            List<PDSignature> firmas, int largoDelVigente, byte[] subido) {
        ResultadoDeComprobacion unaFirmaNueva = ResultadoDeComprobacion.aprobada(UNA_FIRMA_NUEVA);
        Optional<PDSignature> encontrada =
                firmas.stream().filter(una -> terminaAlFinal(una, subido.length)).findFirst();
        if (encontrada.isEmpty()
                || !rangoValido(encontrada.get().getByteRange(), largoDelVigente, subido)) {
            return new HallazgosDeLaFirma(
                    List.of(
                            unaFirmaNueva,
                            ResultadoDeComprobacion.rechazada(
                                    INTEGRIDAD,
                                    "La firma nueva no cubre el archivo completo: hay contenido"
                                            + " agregado fuera de la firma.")),
                    Optional.empty());
        }
        PDSignature firma = encontrada.get();
        CMSSignedData cms;
        byte[] firmado;
        try {
            firmado = firma.getSignedContent(subido);
            cms =
                    new CMSSignedData(
                            new CMSProcessableByteArray(firmado), sinRelleno(firma.getContents()));
        } catch (CMSException | IOException | RuntimeException e) {
            LOG.info("El CMS de la firma nueva no se puede leer: {}", e.getMessage());
            return new HallazgosDeLaFirma(
                    List.of(
                            unaFirmaNueva,
                            ResultadoDeComprobacion.rechazada(
                                    INTEGRIDAD, "La firma nueva no se puede leer.")),
                    Optional.empty());
        }
        SignerInformation firmante = cms.getSignerInfos().getSigners().iterator().next();
        Optional<X509Certificate> certificado = certificadoDelFirmante(cms, firmante);
        if (certificado.isEmpty()) {
            return new HallazgosDeLaFirma(
                    List.of(
                            unaFirmaNueva,
                            ResultadoDeComprobacion.rechazada(
                                    INTEGRIDAD,
                                    "La firma nueva no trae el certificado de quien firma.")),
                    Optional.empty());
        }
        Optional<CertificadoDeLaFirma> datos = Optional.of(datos(certificado.get()));
        if (!integra(cms, firmante, certificado.get(), firmado, firma.getSubFilter())) {
            return new HallazgosDeLaFirma(
                    List.of(
                            unaFirmaNueva,
                            ResultadoDeComprobacion.rechazada(
                                    INTEGRIDAD,
                                    "La firma nueva no corresponde al contenido del archivo: cambió"
                                            + " después de firmarlo.")),
                    datos);
        }
        ResultadoDeComprobacion confianza =
                deConfianza(certificado.get(), certificados(cms), momento(firmante, firma))
                        ? ResultadoDeComprobacion.aprobada(CONFIANZA)
                        : ResultadoDeComprobacion.rechazada(
                                CONFIANZA,
                                "El certificado no llega a una raíz de la lista de confianza.");
        return new HallazgosDeLaFirma(
                List.of(unaFirmaNueva, ResultadoDeComprobacion.aprobada(INTEGRIDAD), confianza),
                datos);
    }

    private static HallazgosDeLaFirma rechazo(
            ec.mdd.firma.dominio.Comprobacion comprobacion, String motivo) {
        return new HallazgosDeLaFirma(
                List.of(ResultadoDeComprobacion.rechazada(comprobacion, motivo)), Optional.empty());
    }

    private static boolean terminaAlFinal(PDSignature firma, int largo) {
        int[] rango = firma.getByteRange();
        return rango != null && rango.length == 4 && (long) rango[2] + rango[3] == largo;
    }

    /**
     * El rango de la firma nueva empieza en 0, cubre todo el vigente antes de su hueco, y el hueco
     * es exactamente {@code /Contents}, entre {@code <} y {@code >}.
     */
    private static boolean rangoValido(int[] rango, int largoDelVigente, byte[] subido) {
        int inicioHueco = rango[1];
        int finHueco = rango[2];
        return rango[0] == 0
                && inicioHueco >= largoDelVigente
                && finHueco > inicioHueco
                && finHueco <= subido.length
                && subido[inicioHueco] == '<'
                && subido[finHueco - 1] == '>';
    }

    /**
     * Lee el primer objeto ASN.1 de {@code /Contents} y descarta los ceros de relleno que lo
     * siguen: Bouncy Castle 1.86 rechaza esos bytes extra (D-46, hallazgo 3).
     */
    private static ContentInfo sinRelleno(byte[] contenido) throws IOException {
        try (ASN1InputStream entrada = new ASN1InputStream(contenido)) {
            return ContentInfo.getInstance(entrada.readObject());
        }
    }

    private static boolean integra(
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
                    return false;
                }
            }
            // Con la llave pública: con el certificado, Bouncy Castle también exige su vigencia en
            // el
            // signingTime y mezcla la vigencia con la integridad (D-46, hallazgo 4).
            return firmante.verify(
                    new JcaSimpleSignerInfoVerifierBuilder().build(certificado.getPublicKey()));
        } catch (CMSException
                | OperatorCreationException
                | NoSuchAlgorithmException
                | RuntimeException e) {
            LOG.info("La firma nueva no se verifica: {}", e.getMessage());
            return false;
        }
    }

    private boolean deConfianza(
            X509Certificate certificado, List<X509Certificate> delCms, Instant momento) {
        try {
            X509CertSelector objetivo = new X509CertSelector();
            objetivo.setCertificate(certificado);
            PKIXBuilderParameters parametros = new PKIXBuilderParameters(lista.anclas(), objetivo);
            parametros.setRevocationEnabled(false); // D-23 no pide revocación (ADR-007).
            parametros.setDate(Date.from(momento));
            parametros.addCertStore(
                    CertStore.getInstance("Collection", new CollectionCertStoreParameters(delCms)));
            CertPathBuilder.getInstance("PKIX").build(parametros);
            return true;
        } catch (CertPathBuilderException e) {
            LOG.info("Sin camino hasta una raíz de confianza: {}", e.getMessage());
            return false;
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se puede construir la cadena de certificados", e);
        }
    }

    /** El {@code signingTime} firmado del CMS, o la fecha {@code /M}, o ahora. */
    private static Instant momento(SignerInformation firmante, PDSignature firma) {
        if (firmante.getSignedAttributes() != null) {
            Attribute hora = firmante.getSignedAttributes().get(CMSAttributes.signingTime);
            if (hora != null) {
                ASN1Encodable valor = hora.getAttrValues().getObjectAt(0);
                return Time.getInstance(valor).getDate().toInstant();
            }
        }
        Calendar fecha = firma.getSignDate();
        return fecha == null ? Instant.now() : fecha.toInstant();
    }

    @SuppressWarnings("unchecked")
    private static Optional<X509Certificate> certificadoDelFirmante(
            CMSSignedData cms, SignerInformation firmante) {
        Collection<X509CertificateHolder> coincidencias =
                cms.getCertificates()
                        .getMatches((Selector<X509CertificateHolder>) firmante.getSID());
        return coincidencias.stream().findFirst().flatMap(ValidadorPades::convertir);
    }

    private static List<X509Certificate> certificados(CMSSignedData cms) {
        List<X509Certificate> todos = new ArrayList<>();
        cms.getCertificates().getMatches(null).stream()
                .map(ValidadorPades::convertir)
                .flatMap(Optional::stream)
                .forEach(todos::add);
        return todos;
    }

    private static Optional<X509Certificate> convertir(X509CertificateHolder titular) {
        try {
            return Optional.of(new JcaX509CertificateConverter().getCertificate(titular));
        } catch (CertificateException e) {
            LOG.info("Certificado ilegible en el CMS: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static CertificadoDeLaFirma datos(X509Certificate certificado) {
        X500Name sujeto = X500Name.getInstance(certificado.getSubjectX500Principal().getEncoded());
        var cn = sujeto.getRDNs(BCStyle.CN);
        String nombre = cn.length == 0 ? sujeto.toString() : cn[0].getFirst().getValue().toString();
        return new CertificadoDeLaFirma(
                recortar(nombre, 200),
                recortar(certificado.getIssuerX500Principal().getName(), 400),
                certificado.getSerialNumber().toString(16).toLowerCase(Locale.ROOT),
                certificado.getNotBefore().toInstant(),
                certificado.getNotAfter().toInstant());
    }

    private static String recortar(String texto, int largo) {
        return texto.length() <= largo ? texto : texto.substring(0, largo);
    }
}
