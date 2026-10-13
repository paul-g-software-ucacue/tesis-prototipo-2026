package ec.mdd.plataforma.seguridad;

import jakarta.servlet.DispatcherType;
import java.util.Locale;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Seguridad de los 4 procesos: servidor de recursos con JWT RS256 (ADR-014).
 *
 * <p>Cada proceso valida por su cuenta el token de cada petición, aunque haya pasado por el API
 * Gateway: solo acepta RS256, la firma de una llave del JWKS de Usuarios, el emisor configurado y
 * un vencimiento que no pasó. Los roles salen del claim {@code roles} como autoridades {@code
 * ROLE_<rol>}. Sin un token válido, solo se atienden la salud de Actuator y las rutas de {@link
 * PropiedadesSeguridad#rutasPublicas()}; lo demás responde con {@link RespuestaNoAutenticado}.
 *
 * <p>Se aplica antes que la seguridad por defecto de Spring Boot, que se retira al encontrar esta
 * cadena de filtros y este decodificador.
 */
@AutoConfiguration(
        beforeName = {
            "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
            "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration",
            "org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration"
        })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(PropiedadesSeguridad.class)
// Activa la seguridad web aunque no esté la autoconfiguración de Spring Boot que lo hace, por
// ejemplo en las pruebas con @WebMvcTest.
@EnableWebSecurity
public class SeguridadProcesoAutoConfiguracion {

    private static final String JWKS = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";

    /** Decodificador RS256 con el JWKS de Usuarios y la validación del emisor y del vencimiento. */
    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(Environment entorno, PropiedadesSeguridad propiedades) {
        NimbusJwtDecoder decodificador =
                NimbusJwtDecoder.withJwkSetUri(entorno.getRequiredProperty(JWKS))
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.emisor()));
        return decodificador;
    }

    /** Convierte el claim {@code roles} en autoridades {@code ROLE_<rol>}. */
    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(roles);
        return conversor;
    }

    /** Cadena de filtros sin sesión: rutas públicas, el resto autenticado con el token. */
    @Bean
    public SecurityFilterChain cadenaDeSeguridad(
            HttpSecurity http,
            PropiedadesSeguridad propiedades,
            JwtAuthenticationConverter conversor)
            throws Exception {
        RespuestaNoAutenticado respuesta401 = new RespuestaNoAutenticado();
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        reglas -> {
                            // La página de error se puede escribir (B00-H04, ADR-016). Una petición
                            // directa a /error es de tipo REQUEST y sigue pidiendo token.
                            reglas.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                            reglas.requestMatchers(ruta("* /actuator/health/**")).permitAll();
                            propiedades
                                    .rutasPublicas()
                                    .forEach(r -> reglas.requestMatchers(ruta(r)).permitAll());
                            reglas.anyRequest().authenticated();
                        })
                .oauth2ResourceServer(
                        servidor ->
                                servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                                        .authenticationEntryPoint(respuesta401))
                .exceptionHandling(errores -> errores.authenticationEntryPoint(respuesta401));
        return http.build();
    }

    // «MÉTODO /ruta», con * para cualquier método.
    static RequestMatcher ruta(String definicion) {
        String[] partes = definicion.trim().split("\\s+", 2);
        if (partes.length != 2) {
            throw new IllegalArgumentException(
                    "Ruta pública sin el formato «MÉTODO /ruta»: " + definicion);
        }
        PathPatternRequestMatcher.Builder constructor = PathPatternRequestMatcher.withDefaults();
        return "*".equals(partes[0])
                ? constructor.matcher(partes[1])
                : constructor.matcher(
                        HttpMethod.valueOf(partes[0].toUpperCase(Locale.ROOT)), partes[1]);
    }
}
