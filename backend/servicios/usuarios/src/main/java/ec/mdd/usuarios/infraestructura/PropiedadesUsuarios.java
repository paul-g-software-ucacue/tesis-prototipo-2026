package ec.mdd.usuarios.infraestructura;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de Usuarios ({@code mdd.usuarios}).
 *
 * @param token vigencia de los tokens
 * @param llavePrivada ruta de un archivo PEM con la llave privada RSA (PKCS#8); sin valor, la llave
 *     es efímera (research §2)
 * @param usuarioPrueba usuario de prueba, solo donde se habilita (research §8)
 * @param procesos claves de los procesos que pueden pedir un token de proceso, por nombre (D-49)
 * @param tokenProceso vigencia de los tokens de proceso
 */
@ConfigurationProperties("mdd.usuarios")
public record PropiedadesUsuarios(
        @DefaultValue Token token,
        String llavePrivada,
        @DefaultValue UsuarioPrueba usuarioPrueba,
        Map<String, Proceso> procesos,
        @DefaultValue TokenProceso tokenProceso) {

    /** Copia defensiva; sin procesos, el mapa queda vacío. */
    public PropiedadesUsuarios {
        procesos = procesos == null ? Map.of() : Map.copyOf(procesos);
    }

    /**
     * @param vigencia cuánto dura un token; 30 minutos por defecto
     */
    public record Token(@DefaultValue("PT30M") Duration vigencia) {}

    /**
     * @param vigencia cuánto dura un token de proceso; 5 minutos por defecto (ADR-018)
     */
    public record TokenProceso(@DefaultValue("PT5M") Duration vigencia) {}

    /**
     * @param clave clave del proceso ({@code MDD_CLAVE_PROCESO_<PROCESO>}); vacía, el proceso no
     *     puede pedir tokens; nunca se registra
     */
    public record Proceso(String clave) {

        /** No incluye la clave. */
        @Override
        public String toString() {
            return "Proceso[clave configurada=" + (clave != null && !clave.isBlank()) + "]";
        }
    }

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
