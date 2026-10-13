package ec.mdd.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LlavesDeFirmaTest {

    // Armadura PEM partida, para que ni Gitleaks ni una búsqueda de «BEGIN PRIVATE KEY» la tomen
    // por una llave real en el repositorio. El contenido se genera en cada prueba.
    private static final String INICIO = "-----BEGIN " + "PRIVATE KEY-----";
    private static final String FIN = "-----END " + "PRIVATE KEY-----";

    @Test
    void dadoQueNoHayArchivo_cuandoSeCargan_entoncesGeneraUnaLlaveEfimeraDe2048Bits()
            throws Exception {
        LlavesDeFirma llaves = LlavesDeFirma.cargar("");

        RSAKey llave = llaves.llave();
        assertThat(llave.size()).isEqualTo(2048);
        assertThat(llave.isPrivate()).isTrue();
        assertThat(llave.getKeyID()).isEqualTo(llave.computeThumbprint().toString());
        assertThat(llave.getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
    }

    @Test
    void dadoUnArchivoPem_cuandoSeCargan_entoncesUsaEsaLlave(@TempDir Path directorio)
            throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        KeyPair par = generador.generateKeyPair();
        Path archivo = directorio.resolve("llave.pem");
        Files.writeString(
                archivo,
                INICIO
                        + "\n"
                        + Base64.getMimeEncoder().encodeToString(par.getPrivate().getEncoded())
                        + "\n"
                        + FIN
                        + "\n");

        LlavesDeFirma llaves = LlavesDeFirma.cargar(archivo.toString());

        assertThat(llaves.llave().toRSAPublicKey().getModulus())
                .isEqualTo(((RSAPublicKey) par.getPublic()).getModulus());
    }

    @Test
    void dadoLasLlaves_cuandoSePideElJwksPublico_entoncesNoTienePartesPrivadas() {
        LlavesDeFirma llaves = LlavesDeFirma.cargar(null);

        assertThat(llaves.jwksPublico().getKeys()).hasSize(1).allMatch(llave -> !llave.isPrivate());
        assertThat(llaves.jwksPublico().toString(false)).doesNotContain("\"d\"");
    }
}
