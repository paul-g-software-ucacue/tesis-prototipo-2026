package ec.mdd.plataforma.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Las autoridades de un token, con su alcance y su carrera (ADR-021, D-64). */
class AutoridadesDelTokenTest {

    private final AutoridadesDelToken autoridades = new AutoridadesDelToken();

    @Test
    void dadoUnPermisoPropioConCarrera_cuandoSeConvierte_entoncesLasMarca() {
        assertThat(
                        convertir(
                                Map.of(
                                        "permisos",
                                        List.of("OFERTA:LECTURA:PROPIO", "DOCENTES:LECTURA"),
                                        "carrera",
                                        "DS-01")))
                .containsExactlyInAnyOrder(
                        "PERMISO_OFERTA:LECTURA",
                        "ALCANCE_PROPIO_OFERTA:LECTURA",
                        "PERMISO_DOCENTES:LECTURA",
                        "CARRERA_DS-01");
    }

    @Test
    void dadoUnPermisoSinAlcancePropioNiCarrera_cuandoSeConvierte_entoncesNoAgregaNada() {
        assertThat(convertir(Map.of("permisos", List.of("OFERTA:LECTURA"))))
                .containsExactly("PERMISO_OFERTA:LECTURA");
    }

    @Test
    void dadoUnTokenDeProceso_cuandoSeConvierte_entoncesSoloDaSuRol() {
        assertThat(convertir(Map.of("roles", List.of("PROCESO")))).containsExactly("ROLE_PROCESO");
    }

    private Collection<String> convertir(Map<String, Object> reclamos) {
        Jwt token =
                new Jwt(
                        "valor",
                        Instant.EPOCH,
                        Instant.EPOCH.plusSeconds(60),
                        Map.of("alg", "RS256"),
                        reclamos);
        return autoridades.convert(token).stream().map(GrantedAuthority::getAuthority).toList();
    }
}
