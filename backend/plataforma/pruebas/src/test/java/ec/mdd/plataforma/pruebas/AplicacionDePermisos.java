package ec.mdd.plataforma.pruebas;

import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Aplicación mínima para probar {@link VerificadorDePermisos} con la seguridad de los procesos. */
@SpringBootApplication
class AplicacionDePermisos {

    @RestController
    static class Registros {

        @PreAuthorize("hasAuthority('PERMISO_PRUEBA:LECTURA')")
        @GetMapping("/api/prueba/registros")
        String listar() {
            return "[]";
        }

        @PreAuthorize("hasAuthority('PERMISO_PRUEBA:ESCRITURA')")
        @PostMapping("/api/prueba/registros")
        @ResponseStatus(HttpStatus.CREATED)
        String crear(@RequestBody Map<String, String> registro) {
            return registro.getOrDefault("nombre", "");
        }

        @PreAuthorize("hasAuthority('PERMISO_PRUEBA:ESCRITURA')")
        @DeleteMapping("/api/prueba/registros/{codigo}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void borrar(@PathVariable String codigo) {}

        @GetMapping("/api/prueba/libre")
        String libre() {
            return "libre";
        }

        @GetMapping("/api/prueba/olvidada")
        String olvidada() {
            return "sin anotación";
        }

        @PreAuthorize("hasAuthority('PERMISO_PRUEBA:ESCRITURA')")
        @GetMapping("/api/prueba/equivocada")
        String equivocada() {
            return "otro permiso";
        }
    }
}
