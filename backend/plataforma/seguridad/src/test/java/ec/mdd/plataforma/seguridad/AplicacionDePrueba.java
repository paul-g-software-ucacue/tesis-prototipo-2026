package ec.mdd.plataforma.seguridad;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Aplicación mínima para probar la seguridad de los procesos con un servidor real. */
@SpringBootApplication
class AplicacionDePrueba {

    /** Como las interfaces que openapi-generator genera desde el contrato. */
    interface RecursoApi {

        @GetMapping("/api/prueba/por-interfaz")
        String porInterfaz();
    }

    /** Un controlador que implementa la interfaz generada y exige un permiso. */
    @RestController
    static class Recurso implements RecursoApi {

        @Override
        @PreAuthorize("hasAuthority('PERMISO_USUARIOS_ROLES:LECTURA')")
        public String porInterfaz() {
            return "por interfaz";
        }
    }

    @RestController
    static class Sesion {

        @GetMapping("/api/prueba/sesion")
        String consultar(Authentication autenticacion) {
            return autenticacion.getName()
                    + "|"
                    + autenticacion.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            // Spring Security 7 agrega FACTOR_BEARER; aquí solo interesan los
                            // roles y los permisos.
                            .filter(
                                    autoridad ->
                                            autoridad.startsWith("ROLE_")
                                                    || autoridad.startsWith("PERMISO_"))
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

        @PreAuthorize("hasAuthority('PERMISO_USUARIOS_ROLES:LECTURA')")
        @GetMapping("/api/prueba/protegido")
        String protegido() {
            return "protegido";
        }

        // Con cuerpo: sin el permiso, el 403 llega antes de leerlo (AutorizacionPrevia).
        @PreAuthorize("hasAuthority('PERMISO_USUARIOS_ROLES:ESCRITURA')")
        @PostMapping("/api/prueba/protegido")
        String escribir(@RequestBody Map<String, String> cuerpo) {
            return "escrito|" + cuerpo.size();
        }
    }
}
