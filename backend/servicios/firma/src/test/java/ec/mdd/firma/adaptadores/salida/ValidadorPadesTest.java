package ec.mdd.firma.adaptadores.salida;

import static ec.mdd.firma.dominio.Comprobacion.CONFIANZA;
import static ec.mdd.firma.dominio.Comprobacion.INTEGRIDAD;
import static ec.mdd.firma.dominio.Comprobacion.UNA_FIRMA_NUEVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ec.mdd.firma.dominio.Resultado;
import ec.mdd.firma.dominio.ResultadoDeComprobacion;
import ec.mdd.firma.pruebas.FirmasDePrueba;
import ec.mdd.firma.pruebas.FirmasDePrueba.Identidad;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas.HallazgosDeLaFirma;
import ec.mdd.firma.spike.ValidadorPadesDePrueba;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

/**
 * La validación básica de F14-H02 sobre el PDF/A real de F13-H02, firmado como FirmaEC por {@link
 * FirmasDePrueba} (D-46, D-103): una sola firma nueva, la integridad y la confianza. La primera
 * comprobación, el mismo documento, es del dominio.
 */
class ValidadorPadesTest {

    private static final String F1 = "firma-1-RESPONSABLE_CONSOLIDACION";
    private static final String F2 = "firma-2-VICERRECTOR";
    private static final String F3 = "firma-3-RECTOR";

    private static byte[] original;
    private static Identidad ca;
    private static Identidad firmante;
    private static Identidad caAjena;
    private static ValidadorPades validador;

    @BeforeAll
    static void preparar() throws Exception {
        original = new GeneradorPdfa().generar(ContenidosDePrueba.pequeno());
        ca = FirmasDePrueba.ca("CA de Prueba MDD Validador");
        firmante = FirmasDePrueba.firmante(ca, "Firmante Uno Prueba (F1)");
        caAjena = FirmasDePrueba.ca("CA Ajena");
        validador = new ValidadorPades(lista(ca.certificado()));
    }

    private static ListaConfianza lista(X509Certificate... raices) throws Exception {
        StringBuilder pem = new StringBuilder();
        for (X509Certificate raiz : raices) {
            pem.append(FirmasDePrueba.pem(raiz));
        }
        return ListaConfianza.desde(
                List.of(
                        new ByteArrayResource(
                                pem.toString().getBytes(StandardCharsets.US_ASCII), "raices")));
    }

    private static List<org.assertj.core.groups.Tuple> resultados(HallazgosDeLaFirma hallazgos) {
        return hallazgos.evaluadas().stream()
                .map(una -> tuple(una.comprobacion(), una.resultado()))
                .toList();
    }

    @Test
    void dadoElVigenteFirmadoEnElCampoDelPaso_cuandoSeValida_entoncesLasTresPasanConElCertificado()
            throws Exception {
        byte[] firmado = FirmasDePrueba.firmar(original, firmante, F1);

        HallazgosDeLaFirma hallazgos = validador.validar(original, firmado);

        assertThat(resultados(hallazgos))
                .containsExactly(
                        tuple(UNA_FIRMA_NUEVA, Resultado.APROBADA),
                        tuple(INTEGRIDAD, Resultado.APROBADA),
                        tuple(CONFIANZA, Resultado.APROBADA));
        assertThat(hallazgos.certificado())
                .get()
                .satisfies(
                        certificado -> {
                            assertThat(certificado.nombre()).isEqualTo("Firmante Uno Prueba (F1)");
                            assertThat(certificado.emisor()).contains("CA de Prueba MDD Validador");
                            assertThat(certificado.serie())
                                    .isEqualTo(
                                            firmante.certificado().getSerialNumber().toString(16));
                            assertThat(certificado.validoHasta())
                                    .isEqualTo(firmante.certificado().getNotAfter().toInstant());
                        });
    }

    @Test
    void dadoElVigenteFirmadoEnUnCampoNuevo_cuandoSeValida_entoncesTambienPasa() throws Exception {
        byte[] firmado = FirmasDePrueba.firmar(original, firmante, null);

        assertThat(validador.validar(original, firmado).evaluadas())
                .allMatch(una -> una.resultado() == Resultado.APROBADA)
                .hasSize(3);
    }

    @Test
    void dadoElVigenteSinFirmaNueva_cuandoSeValida_entoncesSeRechazaYLasDemasNoSeEvaluan() {
        HallazgosDeLaFirma hallazgos = validador.validar(original, original.clone());

        assertThat(resultados(hallazgos))
                .containsExactly(tuple(UNA_FIRMA_NUEVA, Resultado.RECHAZADA));
        assertThat(hallazgos.evaluadas().getFirst().motivo()).isPresent();
        assertThat(hallazgos.certificado()).isEmpty();
    }

    @Test
    void dadoElVigenteConDosFirmasNuevas_cuandoSeValida_entoncesSeRechaza() throws Exception {
        byte[] dos =
                FirmasDePrueba.firmar(FirmasDePrueba.firmar(original, firmante, F1), firmante, F2);

        assertThat(resultados(validador.validar(original, dos)))
                .containsExactly(tuple(UNA_FIRMA_NUEVA, Resultado.RECHAZADA));
    }

    @Test
    void dadoUnPdfFirmadoYAlteradoDespues_cuandoSeValida_entoncesNoEsIntegroYLaConfianzaNoSeEvalua()
            throws Exception {
        byte[] alterado =
                FirmasDePrueba.alterar(
                        FirmasDePrueba.firmar(original, firmante, F1), original.length);

        HallazgosDeLaFirma hallazgos = validador.validar(original, alterado);

        assertThat(resultados(hallazgos))
                .containsExactly(
                        tuple(UNA_FIRMA_NUEVA, Resultado.APROBADA),
                        tuple(INTEGRIDAD, Resultado.RECHAZADA));
    }

    @Test
    void dadoUnPdfFirmadoConUnaCaAjena_cuandoSeValida_entoncesNoEsDeConfianza() throws Exception {
        Identidad ajeno = FirmasDePrueba.firmante(caAjena, "Firmante Ajeno");
        byte[] firmado = FirmasDePrueba.firmar(original, ajeno, F1);

        HallazgosDeLaFirma hallazgos = validador.validar(original, firmado);

        assertThat(resultados(hallazgos))
                .containsExactly(
                        tuple(UNA_FIRMA_NUEVA, Resultado.APROBADA),
                        tuple(INTEGRIDAD, Resultado.APROBADA),
                        tuple(CONFIANZA, Resultado.RECHAZADA));
        assertThat(hallazgos.certificado())
                .get()
                .extracting(c -> c.nombre())
                .isEqualTo("Firmante Ajeno");
    }

    @Test
    void dadoUnPdfDanadoDespuesDelVigente_cuandoSeValida_entoncesSeRechazaConUnMotivoDelSistema() {
        byte[] danado = Arrays.copyOf(original, original.length + 64);
        byte[] basura =
                "\n%%EOF 1 0 obj << /Type /Sig /ByteRange [".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(basura, 0, danado, original.length, basura.length);

        HallazgosDeLaFirma hallazgos = validador.validar(original, danado);

        assertThat(resultados(hallazgos))
                .containsExactly(tuple(UNA_FIRMA_NUEVA, Resultado.RECHAZADA));
        assertThat(hallazgos.evaluadas().stream().map(ResultadoDeComprobacion::motivo))
                .allSatisfy(
                        motivo ->
                                assertThat(motivo.orElse(""))
                                        .doesNotContain("Exception")
                                        .doesNotContain("org."));
    }

    @Test
    void
            dadasLasTresFirmasSucesivas_cuandoSeValidaCadaUna_entoncesPasanYElValidadorDelSpikeLasAcepta()
                    throws Exception {
        Identidad segundo = FirmasDePrueba.firmante(ca, "Firmante Dos Prueba (F2)");
        Identidad tercero = FirmasDePrueba.firmante(ca, "Firmante Tres Prueba (F3)");
        byte[] uno = FirmasDePrueba.firmar(original, firmante, F1);
        byte[] dos = FirmasDePrueba.firmar(uno, segundo, F2);
        byte[] tres = FirmasDePrueba.firmar(dos, tercero, F3);

        assertThat(validador.validar(original, uno).evaluadas())
                .allMatch(una -> una.resultado() == Resultado.APROBADA);
        assertThat(validador.validar(uno, dos).evaluadas())
                .allMatch(una -> una.resultado() == Resultado.APROBADA);
        assertThat(validador.validar(dos, tres).evaluadas())
                .allMatch(una -> una.resultado() == Resultado.APROBADA);
        assertThat(new ValidadorPadesDePrueba().validar(tres, ca.certificado()))
                .hasSize(3)
                .allMatch(firma -> firma.esValida());
    }

    @Test
    void dadoUnFirmanteDeLaCaDePrueba_cuandoSeValidaContraSuRaizDelRepositorio_entoncesPasa()
            throws Exception {
        Identidad deP12;
        try (InputStream p12 = getClass().getResourceAsStream("/ca-prueba/firmante-1.p12")) {
            deP12 = FirmasDePrueba.deP12(p12);
        }
        X509Certificate raiz;
        try (InputStream crt = getClass().getResourceAsStream("/firma/raices/ca-prueba.crt")) {
            raiz =
                    (X509Certificate)
                            CertificateFactory.getInstance("X.509").generateCertificate(crt);
        }
        byte[] firmado = FirmasDePrueba.firmar(original, deP12, F1);

        assertThat(new ValidadorPades(lista(raiz)).validar(original, firmado).evaluadas())
                .allMatch(una -> una.resultado() == Resultado.APROBADA);
    }

    @Test
    void
            dadoUnSujetoConElCnYElNumeroDeSerieEnElMismoComponente_cuandoSeValida_entoncesElNombreEsElCn()
                    throws Exception {
        // Como los certificados de la ECIBCE: el CN y el SERIALNUMBER en un RDN de varios valores.
        // El nombre del resultado es el CN, no el primer valor del componente.
        org.bouncycastle.asn1.x500.X500NameBuilder sujeto =
                new org.bouncycastle.asn1.x500.X500NameBuilder(
                                org.bouncycastle.asn1.x500.style.BCStyle.INSTANCE)
                        .addRDN(org.bouncycastle.asn1.x500.style.BCStyle.C, "EC")
                        .addMultiValuedRDN(
                                new org.bouncycastle.asn1.ASN1ObjectIdentifier[] {
                                    org.bouncycastle.asn1.x500.style.BCStyle.SERIALNUMBER,
                                    org.bouncycastle.asn1.x500.style.BCStyle.CN
                                },
                                new String[] {"0123456789", "NOMBRE FICTICIO DE PRUEBA"});
        Identidad conSerie = FirmasDePrueba.firmante(ca, sujeto.build());
        byte[] firmado = FirmasDePrueba.firmar(original, conSerie, F1);

        assertThat(validador.validar(original, firmado).certificado())
                .get()
                .extracting(certificado -> certificado.nombre())
                .isEqualTo("NOMBRE FICTICIO DE PRUEBA");
    }
}
