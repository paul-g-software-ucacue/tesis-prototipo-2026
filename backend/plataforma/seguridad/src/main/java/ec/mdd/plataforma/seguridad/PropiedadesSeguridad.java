package ec.mdd.plataforma.seguridad;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la seguridad de un proceso ({@code mdd.seguridad}).
 *
 * @param emisor emisor que deben traer los tokens ({@code iss}); por defecto {@code mdd-usuarios}
 * @param rutasPublicas rutas que se atienden sin token, en el formato {@code MÉTODO /ruta}, con
 *     {@code *} para cualquier método (por ejemplo, {@code POST /api/usuarios/sesion}). La salud de
 *     Actuator siempre es pública y no hace falta listarla.
 */
@ConfigurationProperties("mdd.seguridad")
public record PropiedadesSeguridad(
        @DefaultValue("mdd-usuarios") String emisor, List<String> rutasPublicas) {

    /** Copia defensiva; sin rutas públicas, la lista queda vacía. */
    public PropiedadesSeguridad {
        rutasPublicas = rutasPublicas == null ? List.of() : List.copyOf(rutasPublicas);
    }
}
