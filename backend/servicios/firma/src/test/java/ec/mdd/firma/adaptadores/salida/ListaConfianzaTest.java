package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.firma.pruebas.FirmasDePrueba;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * La lista de confianza de Firma (F14-H02, D-103): se lee al arrancar, sin raíces no hay forma de
 * aceptar firmas, y la carpeta de las raíces solo tiene certificados públicos (principio IX).
 */
class ListaConfianzaTest {

    private static Resource texto(String contenido) {
        return new ByteArrayResource(contenido.getBytes(StandardCharsets.US_ASCII), "prueba");
    }

    @Test
    void dadasLasRaicesDelRepositorio_cuandoSeLeen_entoncesEstanLaCaDePruebaYLaDelBce() {
        ListaConfianza lista =
                ListaConfianza.desde(
                        List.of(
                                new ClassPathResource("firma/raices/ca-prueba.crt"),
                                new ClassPathResource("firma/raices/bce-raiz.crt")));

        assertThat(lista.raices()).hasSize(2);
        assertThat(lista.raices().getFirst().getSubjectX500Principal().getName())
                .contains("CA de Prueba MDD Demo");
    }

    @Test
    void dadoUnPemConDosRaices_cuandoSeLee_entoncesLasCargaAmbas() throws Exception {
        String dos =
                FirmasDePrueba.pem(FirmasDePrueba.ca("Una").certificado())
                        + FirmasDePrueba.pem(FirmasDePrueba.ca("Otra").certificado());

        assertThat(ListaConfianza.desde(List.of(texto(dos))).raices()).hasSize(2);
    }

    @Test
    void dadaUnaListaVacia_cuandoSeLee_entoncesFallaYNoSeArranca() {
        assertThatThrownBy(() -> ListaConfianza.desde(List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ListaConfianza.desde(List.of(texto(""))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadoUnArchivoIlegibleOQueNoExiste_cuandoSeLee_entoncesFalla() {
        assertThatThrownBy(() -> ListaConfianza.desde(List.of(texto("no es un certificado"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                ListaConfianza.desde(
                                        List.of(
                                                new ClassPathResource(
                                                        "firma/raices/no-existe.crt"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadoUnArchivoConUnaLlavePrivada_cuandoSeLee_entoncesFalla() throws Exception {
        String conLlave =
                FirmasDePrueba.pem(FirmasDePrueba.ca("Con llave").certificado())
                        + "-----BEGIN PRIVATE KEY-----\nAAAA\n-----END PRIVATE KEY-----\n";

        assertThatThrownBy(() -> ListaConfianza.desde(List.of(texto(conLlave))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dadaLaCarpetaDeRaices_cuandoSeRevisaCadaArchivo_entoncesSoloHayCertificadosSinLlaves()
            throws IOException {
        Resource[] archivos =
                new PathMatchingResourcePatternResolver().getResources("classpath*:firma/raices/*");

        assertThat(archivos).isNotEmpty();
        for (Resource archivo : archivos) {
            assertThat(archivo.getFilename()).endsWith(".crt");
            String contenido = archivo.getContentAsString(StandardCharsets.US_ASCII);
            assertThat(contenido).contains("-----BEGIN CERTIFICATE-----").doesNotContain("PRIVATE");
            assertThat(ListaConfianza.desde(List.of(archivo)).raices()).isNotEmpty();
        }
    }

    @Test
    void dadaSoloLaCaDePrueba_cuandoSeValidaUnaFirmaDeOtraRaiz_entoncesNoEsDeConfianza()
            throws Exception {
        // La propiedad mdd.firma.raices reemplaza la lista (principio IX, S-10): con otra lista, la
        // misma firma deja de ser de confianza.
        var otra = FirmasDePrueba.ca("Otra raíz");
        byte[] original = new GeneradorPdfa().generar(ContenidosDePrueba.pequeno());
        byte[] firmado =
                FirmasDePrueba.firmar(original, FirmasDePrueba.firmante(otra, "Alguien"), null);
        ListaConfianza soloPrueba =
                ListaConfianza.desde(List.of(new ClassPathResource("firma/raices/ca-prueba.crt")));

        assertThat(new ValidadorPades(soloPrueba).validar(original, firmado).evaluadas().getLast())
                .satisfies(
                        una ->
                                assertThat(una.resultado())
                                        .isEqualTo(ec.mdd.firma.dominio.Resultado.RECHAZADA));
    }

    @Test
    void dadasLasRaicesDelRepositorio_cuandoSeCalculaSuHuella_entoncesSonLasDeD103()
            throws Exception {
        // Si alguien cambia un archivo de la lista, esta prueba lo detecta (D-103).
        assertThat(huella("firma/raices/bce-raiz.crt"))
                .isEqualTo("11c7c59be9d21d216f0e8151378d53d03b314060559adc49da161ec4f7829bec");
        assertThat(huella("firma/raices/ca-prueba.crt"))
                .isEqualTo("60ca4684e97b094971d432f6fe59b244c42f83af1dfcc8df7d40979d6349e406");
    }

    private static String huella(String recurso) throws Exception {
        byte[] der =
                ListaConfianza.desde(List.of(new ClassPathResource(recurso)))
                        .raices()
                        .getFirst()
                        .getEncoded();
        return java.util.HexFormat.of()
                .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(der));
    }
}
