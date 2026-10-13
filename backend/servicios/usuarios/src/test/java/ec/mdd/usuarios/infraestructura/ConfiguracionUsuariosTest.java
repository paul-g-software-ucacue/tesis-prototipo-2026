package ec.mdd.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Los métodos @Bean de la configuración, llamados directamente. */
class ConfiguracionUsuariosTest {

    private final ConfiguracionUsuarios configuracion = new ConfiguracionUsuarios();
    private final PropiedadesUsuarios propiedades =
            new PropiedadesUsuarios(
                    new PropiedadesUsuarios.Token(Duration.ofMinutes(5)),
                    null,
                    new PropiedadesUsuarios.UsuarioPrueba(false, null));

    @Test
    void dadoLaConfiguracion_cuandoSeArmanLasPiezas_entoncesFirmanYVerifican() {
        LlavesDeFirma llaves = configuracion.llavesDeFirma(propiedades);
        VerificadorDeClaves verificador = configuracion.verificadorDeClaves();
        EmisorDeTokens emisor = configuracion.emisorDeTokens(llaves, propiedades, "mdd-usuarios");

        TokenEmitido token = emisor.emitir(new Usuario("autor.prueba", "hash", true, Set.of()));

        assertThat(token.valor()).isNotBlank();
        assertThat(configuracion.jwksUsuarios(llaves).getKeys()).allMatch(l -> !l.isPrivate());
        assertThat(verificador.coincide("clave", verificador.calcularHash("clave"))).isTrue();
        assertThat(configuracion.repositorioUsuarios(null)).isNotNull();
        assertThat(configuracion.iniciarSesion(null, verificador, emisor)).isNotNull();
        assertThat(configuracion.cargaUsuarioDePrueba(null, verificador, propiedades)).isNotNull();
        assertThat(propiedades.usuarioPrueba().toString()).doesNotContain("clave=");
    }
}
