package ec.mdd.plataforma.seguridad;

import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Aplicación mínima para probar la seguridad de los procesos con un servidor real. */
@SpringBootApplication
class AplicacionDePrueba {

    @RestController
    static class Sesion {

        @GetMapping("/api/prueba/sesion")
        String consultar(Authentication autenticacion) {
            return autenticacion.getName()
                    + "|"
                    + autenticacion.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            // Spring Security 7 agrega FACTOR_BEARER; aquí solo interesan los
                            // roles.
                            .filter(autoridad -> autoridad.startsWith("ROLE_"))
                            .sorted()
                            .collect(Collectors.joining(","));
        }

        @PostMapping("/api/prueba/sesion")
        String iniciar() {
            return "publica";
        }

        @PostMapping("/interno/prueba/eventos")
        String recibirEvento(Authentication autenticacion) {
            return "interno|" + autenticacion.getName();
        }

        @GetMapping("/api/prueba/recurso")
        String recurso() {
            return "recurso";
        }
    }
}
