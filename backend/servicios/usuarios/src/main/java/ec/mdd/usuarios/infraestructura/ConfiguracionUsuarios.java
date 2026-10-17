package ec.mdd.usuarios.infraestructura;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import ec.mdd.usuarios.adaptadores.salida.EmisorJwt;
import ec.mdd.usuarios.adaptadores.salida.RepositorioEventosRecibidosJdbc;
import ec.mdd.usuarios.adaptadores.salida.RepositorioIntentosJdbc;
import ec.mdd.usuarios.adaptadores.salida.RepositorioUsuariosJpa;
import ec.mdd.usuarios.adaptadores.salida.UsuarioJpaRepositorio;
import ec.mdd.usuarios.adaptadores.salida.VerificadorArgon2;
import ec.mdd.usuarios.aplicacion.EmitirTokenDeProcesoServicio;
import ec.mdd.usuarios.aplicacion.IniciarSesionServicio;
import ec.mdd.usuarios.aplicacion.RegistrarEventoRecibidoServicio;
import ec.mdd.usuarios.aplicacion.RenovarSesionServicio;
import ec.mdd.usuarios.dominio.PoliticaDeIntentos;
import ec.mdd.usuarios.puertos.entrada.EmitirTokenDeProceso;
import ec.mdd.usuarios.puertos.entrada.IniciarSesion;
import ec.mdd.usuarios.puertos.entrada.RegistrarEventoRecibido;
import ec.mdd.usuarios.puertos.entrada.RenovarSesion;
import ec.mdd.usuarios.puertos.salida.EmisorDeTokens;
import ec.mdd.usuarios.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.usuarios.puertos.salida.RepositorioIntentos;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Ensambla Usuarios: llaves, adaptadores de salida, los casos de uso, el usuario de prueba y los
 * usuarios de la demo.
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
                propiedades.tokenProceso().vigencia(),
                emisor,
                llaves.llave().getKeyID());
    }

    @Bean
    public RepositorioIntentos repositorioIntentos(JdbcClient jdbc) {
        return new RepositorioIntentosJdbc(jdbc);
    }

    /** Inicio de sesión con el bloqueo de {@code mdd.usuarios.intentos} (D-60). */
    @Bean
    public IniciarSesion iniciarSesion(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            EmisorDeTokens emisor,
            RepositorioIntentos intentos,
            PropiedadesUsuarios propiedades) {
        return new IniciarSesionServicio(
                repositorio,
                verificador,
                emisor,
                intentos,
                new PoliticaDeIntentos(
                        propiedades.intentos().maximo(), propiedades.intentos().bloqueo()),
                Clock.systemUTC());
    }

    /** Renovación hasta {@code mdd.usuarios.token.maximo} desde el inicio (ADR-020). */
    @Bean
    public RenovarSesion renovarSesion(
            RepositorioUsuarios repositorio,
            EmisorDeTokens emisor,
            PropiedadesUsuarios propiedades) {
        return new RenovarSesionServicio(
                repositorio, emisor, propiedades.token().maximo(), Clock.systemUTC());
    }

    /** Token de proceso con las claves de {@code mdd.usuarios.procesos} (D-49, ADR-018). */
    @Bean
    public EmitirTokenDeProceso emitirTokenDeProceso(
            PropiedadesUsuarios propiedades, EmisorDeTokens emisor) {
        Map<String, String> claves = new HashMap<>();
        propiedades.procesos().forEach((proceso, datos) -> claves.put(proceso, datos.clave()));
        return new EmitirTokenDeProcesoServicio(claves, emisor);
    }

    @Bean
    public RepositorioEventosRecibidos repositorioEventosRecibidos(JdbcClient jdbc) {
        return new RepositorioEventosRecibidosJdbc(jdbc);
    }

    /** Receptor de eventos de prueba: solo {@code generacion.EventoDePrueba} (D-47). */
    @Bean
    public RegistrarEventoRecibido registrarEventoRecibido(
            RepositorioEventosRecibidos repositorio) {
        return new RegistrarEventoRecibidoServicio(
                repositorio, RegistrarEventoRecibidoServicio.manejadoresDePrueba());
    }

    @Bean
    public CargaUsuarioDePrueba cargaUsuarioDePrueba(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            PropiedadesUsuarios propiedades) {
        return new CargaUsuarioDePrueba(repositorio, verificador, propiedades.usuarioPrueba());
    }

    /** Los usuarios de {@code usuarios-demo.yaml}, con las contraseñas del entorno (D-59). */
    @Bean
    public CargaUsuariosDemo cargaUsuariosDemo(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            PropiedadesUsuarios propiedades) {
        return new CargaUsuariosDemo(
                repositorio,
                verificador,
                CargaUsuariosDemo.delArchivoVersionado(),
                propiedades.demo());
    }
}
