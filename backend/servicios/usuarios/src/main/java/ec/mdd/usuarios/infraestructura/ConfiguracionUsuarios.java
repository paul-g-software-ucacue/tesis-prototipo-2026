package ec.mdd.usuarios.infraestructura;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import ec.mdd.usuarios.adaptadores.salida.EmisorJwt;
import ec.mdd.usuarios.adaptadores.salida.RepositorioUsuariosJpa;
import ec.mdd.usuarios.adaptadores.salida.UsuarioJpaRepositorio;
import ec.mdd.usuarios.adaptadores.salida.VerificadorArgon2;
import ec.mdd.usuarios.aplicacion.IniciarSesionServicio;
import ec.mdd.usuarios.puertos.entrada.IniciarSesion;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Ensambla Usuarios: llaves, adaptadores de salida, el caso de uso y el usuario de prueba.
 *
 * <p>Los adaptadores reciben solo tipos que no son de la infraestructura (codificadores, reloj,
 * duraciones y textos), porque ninguna capa usa la infraestructura (principio I).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesUsuarios.class)
public class ConfiguracionUsuarios {

    /** La llave de firma, del archivo configurado o efímera. */
    @Bean
    public LlavesDeFirma llavesDeFirma(PropiedadesUsuarios propiedades) {
        return LlavesDeFirma.cargar(propiedades.llavePrivada());
    }

    /** El JWKS público que publica {@code GET /api/usuarios/jwks}. */
    @Bean
    public JWKSet jwksUsuarios(LlavesDeFirma llaves) {
        return llaves.jwksPublico();
    }

    @Bean
    public RepositorioUsuarios repositorioUsuarios(UsuarioJpaRepositorio filas) {
        return new RepositorioUsuariosJpa(filas);
    }

    /** Argon2id con los valores recomendados por Spring Security. */
    @Bean
    public VerificadorDeClaves verificadorDeClaves() {
        return new VerificadorArgon2(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
    }

    @Bean
    public EmisorDeTokens emisorDeTokens(
            LlavesDeFirma llaves,
            PropiedadesUsuarios propiedades,
            @Value("${mdd.seguridad.emisor:mdd-usuarios}") String emisor) {
        return new EmisorJwt(
                new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(llaves.llave()))),
                Clock.systemUTC(),
                propiedades.token().vigencia(),
                emisor,
                llaves.llave().getKeyID());
    }

    @Bean
    public IniciarSesion iniciarSesion(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            EmisorDeTokens emisor) {
        return new IniciarSesionServicio(repositorio, verificador, emisor);
    }

    @Bean
    public CargaUsuarioDePrueba cargaUsuarioDePrueba(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            PropiedadesUsuarios propiedades) {
        return new CargaUsuarioDePrueba(repositorio, verificador, propiedades.usuarioPrueba());
    }
}
