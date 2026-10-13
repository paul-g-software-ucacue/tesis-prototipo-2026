package ec.mdd.usuarios.infraestructura;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de Usuarios ({@code mdd.usuarios}).
 *
 * @param token vigencia de los tokens
 * @param llavePrivada ruta de un archivo PEM con la llave privada RSA (PKCS#8); sin valor, la llave
 *     es efímera (research §2)
 * @param usuarioPrueba usuario de prueba, solo donde se habilita (research §8)
 */
@ConfigurationProperties("mdd.usuarios")
public record PropiedadesUsuarios(
        @DefaultValue Token token, String llavePrivada, @DefaultValue UsuarioPrueba usuarioPrueba) {

    /**
     * @param vigencia cuánto dura un token; 30 minutos por defecto
     */
    public record Token(@DefaultValue("PT30M") Duration vigencia) {}

    /**
     * @param habilitado si se crea {@code autor.prueba} al arrancar
     * @param clave su contraseña; nunca se registra
     */
    public record UsuarioPrueba(@DefaultValue("false") boolean habilitado, String clave) {

        /** No incluye la contraseña. */
        @Override
        public String toString() {
            return "UsuarioPrueba[habilitado=" + habilitado + "]";
        }
    }
}
