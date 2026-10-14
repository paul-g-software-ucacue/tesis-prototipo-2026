package ec.mdd.firma.spike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Escenarios 2 y 3 del spike B00-S01 sobre los PDF firmados con FirmaEC (FR-005, FR-006, FR-008,
 * contracts/validacion.md).
 *
 * <p><b>En la CI</b> usa los PDF de {@code ca-prueba/} firmados con los certificados de la CA de
 * prueba; si no están (FirmaEC rechazó la CA de prueba, D-43), las pruebas se saltan y lo dicen.
 *
 * <p><b>Modo local (D-43)</b>: con {@code -Dmdd.firma.pdf1}, {@code -Dmdd.firma.pdf2}, {@code
 * -Dmdd.firma.raiz} y, opcionalmente, {@code -Dmdd.firma.pdf0}, valida PDF que no están en el
 * repositorio contra la raíz indicada; el archivo de la raíz puede traer también intermedios. En
 * ese modo no se imprime ni se compara el CN real.
 */
class FirmasFirmaecTest {

    private static final String PDF0 = "mdd.firma.pdf0";
    private static final String PDF1 = "mdd.firma.pdf1";
    private static final String PDF2 = "mdd.firma.pdf2";
    private static final String RAIZ = "mdd.firma.raiz";

    private final ValidadorPadesDePrueba validador = new ValidadorPadesDePrueba();

    private boolean modoLocal;
    private byte[] original;
    private byte[] unaFirma;
    private byte[] dosFirmas;
    private X509Certificate raiz;
    private List<X509Certificate> intermedios;

    @BeforeEach
    void cargarDocumentos() throws Exception {
        modoLocal = System.getProperty(PDF1) != null;
        if (modoLocal) {
            // Las rutas tienen que existir: si no, la prueba falla en lugar de saltarse.
            original = System.getProperty(PDF0) == null ? null : leer(System.getProperty(PDF0));
            unaFirma = leer(System.getProperty(PDF1));
            dosFirmas = leer(requerida(PDF2));
            List<X509Certificate> confianza = certificados(leer(requerida(RAIZ)));
            raiz = confianza.getFirst();
            intermedios = confianza.subList(1, confianza.size());
        } else {
            unaFirma = recurso("/ca-prueba/pdfa-firmado-1.pdf");
            dosFirmas = recurso("/ca-prueba/pdfa-firmado-2.pdf");
            assumeTrue(
                    unaFirma != null && dosFirmas != null,
                    "Sin pdfa-firmado-1.pdf y pdfa-firmado-2.pdf en ca-prueba/: FirmaEC no firmó"
                            + " con la CA de prueba o todavía no se firmó (D-43, modo local).");
            original = recurso("/ca-prueba/pdfa-prueba.pdf");
            raiz = certificados(recurso("/ca-prueba/ca-prueba.crt")).getFirst();
            intermedios = List.of();
        }
    }

    @Test
    void escenario2ElSegundoArchivoEmpiezaConLosBytesDelPrimero() {
        assertThat(empiezaCon(dosFirmas, unaFirma)).as("prefijo: firmado-2 ⊇ firmado-1").isTrue();
        if (original != null) {
            assertThat(empiezaCon(unaFirma, original)).as("prefijo: firmado-1 ⊇ original").isTrue();
        }
    }

    @Test
    void escenario2CadaArchivoTieneUnaFirmaMas() throws IOException {
        int antes = original == null ? 0 : ValidadorPadesDePrueba.cantidadDeFirmas(original);

        assertThat(ValidadorPadesDePrueba.cantidadDeFirmas(unaFirma)).isEqualTo(antes + 1);
        assertThat(ValidadorPadesDePrueba.cantidadDeFirmas(dosFirmas)).isEqualTo(antes + 2);
    }

    @Test
    void escenario2LaFirmaAnteriorEsUnaRevisionCerrada() throws IOException {
        // La penúltima firma es la que trae el primer archivo: su ByteRange termina donde termina.
        List<int[]> rangos = ValidadorPadesDePrueba.byteRanges(dosFirmas);
        int[] anterior = rangos.get(rangos.size() - 2);
        int[] ultima = rangos.getLast();

        assertThat((long) anterior[2] + anterior[3]).isEqualTo(unaFirma.length);
        assertThat((long) ultima[2] + ultima[3]).isEqualTo(dosFirmas.length);
    }

    @Test
    void escenario3CadaFirmaEsIntegraConCadenaYVigente() throws IOException {
        List<ResultadoFirma> resultados = validador.validar(dosFirmas, raiz, intermedios);

        resultados.forEach(r -> System.out.println(modoLocal ? r.resumenSinFirmante() : r));
        // En la CI son 2 (F1 y F2, ver abajo); en modo local, todas las del documento.
        assertThat(resultados)
                .hasSize(ValidadorPadesDePrueba.cantidadDeFirmas(dosFirmas))
                .allSatisfy(
                        r ->
                                assertThat(r.esValida())
                                        .as(modoLocal ? r.resumenSinFirmante() : r.toString())
                                        .isTrue());
        if (!modoLocal) {
            assertThat(resultados)
                    .extracting(ResultadoFirma::firmante)
                    .containsExactly("Firmante Uno Prueba (F1)", "Firmante Dos Prueba (F2)");
        }
    }

    private static boolean empiezaCon(byte[] archivo, byte[] prefijo) {
        return archivo.length > prefijo.length
                && Arrays.equals(archivo, 0, prefijo.length, prefijo, 0, prefijo.length);
    }

    private static String requerida(String propiedad) {
        String valor = System.getProperty(propiedad);
        if (valor == null) {
            throw new IllegalStateException("el modo local necesita -D" + propiedad);
        }
        return valor;
    }

    private static byte[] leer(String ruta) throws IOException {
        return Files.readAllBytes(Path.of(ruta));
    }

    private static byte[] recurso(String ruta) throws IOException {
        try (InputStream entrada = FirmasFirmaecTest.class.getResourceAsStream(ruta)) {
            return entrada == null ? null : entrada.readAllBytes();
        }
    }

    private static List<X509Certificate> certificados(byte[] pem) throws CertificateException {
        List<X509Certificate> lista = new ArrayList<>();
        for (var certificado :
                CertificateFactory.getInstance("X.509")
                        .generateCertificates(new ByteArrayInputStream(pem))) {
            lista.add((X509Certificate) certificado);
        }
        return lista;
    }
}
