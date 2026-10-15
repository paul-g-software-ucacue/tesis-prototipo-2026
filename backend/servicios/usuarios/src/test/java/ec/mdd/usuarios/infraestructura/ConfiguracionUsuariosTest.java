package ec.mdd.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.entrada.TokenEmitido;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Los métodos @Bean de la configuración, llamados directamente. */
class ConfiguracionUsuariosTest {

    private final ConfiguracionUsuarios configuracion = new ConfiguracionUsuarios();
    private final PropiedadesUsuarios propiedades =
            new PropiedadesUsuarios(
                    new PropiedadesUsuarios.Token(Duration.ofMinutes(5)),
                    null,
                    new PropiedadesUsuarios.UsuarioPrueba(false, null),
                    Map.of(
                            "distributivo",
                            new PropiedadesUsuarios.Proceso("clave-de-prueba"),
                            "salida",
                            new PropiedadesUsuarios.Proceso("")),
                    new PropiedadesUsuarios.TokenProceso(Duration.ofMinutes(5)));

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
        assertThat(configuracion.repositorioEventosRecibidos(null)).isNotNull();
        assertThat(configuracion.registrarEventoRecibido(null)).isNotNull();
        assertThat(propiedades.usuarioPrueba().toString()).doesNotContain("clave=");
    }

    @Test
    void dadoUnProcesoConLaClaveVacia_cuandoSeArmaElTokenDeProceso_entoncesNoQuedaHabilitado() {
        LlavesDeFirma llaves = configuracion.llavesDeFirma(propiedades);
        EmisorDeTokens emisor = configuracion.emisorDeTokens(llaves, propiedades, "mdd-usuarios");

        var servicio =
                (ec.mdd.usuarios.aplicacion.EmitirTokenDeProcesoServicio)
                        configuracion.emitirTokenDeProceso(propiedades, emisor);

        assertThat(servicio.procesosHabilitados()).containsExactly("distributivo");
        assertThat(servicio.emitir("distributivo", "clave-de-prueba").valor()).isNotBlank();
        assertThat(propiedades.procesos().get("distributivo").toString())
                .doesNotContain("clave-de-prueba");
    }
}
