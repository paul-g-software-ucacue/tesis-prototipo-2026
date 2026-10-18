package ec.mdd.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

/**
 * Seguridad del API Gateway: valida el JWT RS256 de toda petición a {@code /api/**} antes de
 * reenviarla al proceso (ADR-014).
 *
 * <p>Es la primera de dos validaciones: el proceso que recibe la petición vuelve a validar el token
 * por su cuenta, así que una petición que llegue directo a un proceso tampoco entra. Sin token solo
 * pasan el inicio de sesión ({@code POST /api/usuarios/sesion}, D-37) y la salud de Actuator;
 * cualquier otra ruta fuera de {@code /api/**} se rechaza: sin token con el 401 y con token con el
 * 403 de {@link RespuestaAccesoNoAutorizadoReactiva}, el mismo cuerpo que los procesos (ADR-021).
 * Los permisos de cada operación los valida el proceso, no el API Gateway.
 */
@Configuration(proxyBeanMethods = false)
class SeguridadGatewayConfig {

    /** Decodificador RS256 con el JWKS de Usuarios y la validación del emisor y del vencimiento. */
    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks,
            @Value("${mdd.seguridad.emisor:mdd-usuarios}") String emisor) {
        NimbusReactiveJwtDecoder decodificador =
                NimbusReactiveJwtDecoder.withJwkSetUri(jwks)
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisor));
        return decodificador;
    }

    @Bean
    SecurityWebFilterChain cadenaDeSeguridad(ServerHttpSecurity http) {
        RespuestaNoAutenticadoReactiva respuesta401 = new RespuestaNoAutenticadoReactiva();
        RespuestaAccesoNoAutorizadoReactiva respuesta403 =
                new RespuestaAccesoNoAutorizadoReactiva();
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(
                        reglas ->
                                reglas.pathMatchers(HttpMethod.POST, "/api/usuarios/sesion")
                                        .permitAll()
                                        .pathMatchers("/actuator/health/**")
                                        .permitAll()
                                        .pathMatchers("/api/**")
                                        .authenticated()
                                        .anyExchange()
                                        .denyAll())
                .oauth2ResourceServer(
                        servidor -> servidor.jwt(jwt -> {}).authenticationEntryPoint(respuesta401))
                .exceptionHandling(
                        errores ->
                                errores.authenticationEntryPoint(respuesta401)
                                        .accessDeniedHandler(respuesta403))
                .build();
    }
}
