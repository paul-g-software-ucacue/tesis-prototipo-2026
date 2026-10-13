package ec.mdd.plataforma.errores.nucleo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SaneadorDeTrazasTest {

    private static final String JWT = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhdXRvciJ9.ZmlybWE";

    @Test
    void dadoUnaTrazaConSecretos_cuandoSeSanea_entoncesLosOcultaYConservaLaTraza() {
        Exception causa = new IllegalArgumentException("password=x y Bearer abc.def");
        Exception error = new IllegalStateException("fallo con " + JWT + " y clave=secreta", causa);

        String traza = SaneadorDeTrazas.trazaSaneada(error);

        assertThat(traza)
                .doesNotContain(JWT)
                .doesNotContain("secreta")
                .doesNotContain("password=x")
                .doesNotContain("abc.def")
                .contains("java.lang.IllegalStateException")
                .contains("Caused by: java.lang.IllegalArgumentException")
                .contains("at ec.mdd.plataforma.errores.nucleo.SaneadorDeTrazasTest");
        assertThat(traza.split("\\[oculto]", -1)).hasSize(5);
    }

    @Test
    void dadoUnTextoSinSecretos_cuandoSeSanea_entoncesNoCambia() {
        assertThat(SaneadorDeTrazas.sanear("Connection refused: postgres/5432"))
                .isEqualTo("Connection refused: postgres/5432");
    }

    @Test
    void dadoUnaContrasenaEnEspanol_cuandoSeSanea_entoncesLaOculta() {
        assertThat(SaneadorDeTrazas.sanear("contraseña: abc123; usuario=x"))
                .isEqualTo("contraseña=[oculto]; usuario=x");
    }
}
