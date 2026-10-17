package ec.mdd.usuarios.infraestructura;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de Usuarios ({@code mdd.usuarios}).
 *
 * @param token vigencia de los tokens y duración máxima de una sesión
 * @param llavePrivada ruta de un archivo PEM con la llave privada RSA (PKCS#8); sin valor, la llave
 *     es efímera (research §2)
 * @param usuarioPrueba usuario de prueba, solo donde se habilita (research §8)
 * @param procesos claves de los procesos que pueden pedir un token de proceso, por nombre (D-49)
 * @param tokenProceso vigencia de los tokens de proceso
 * @param intentos bloqueo por intentos fallidos (D-60)
 * @param demo contraseñas de los usuarios de la demo (D-59)
 */
@ConfigurationProperties("mdd.usuarios")
public record PropiedadesUsuarios(
        @DefaultValue Token token,
        String llavePrivada,
        @DefaultValue UsuarioPrueba usuarioPrueba,
        Map<String, Proceso> procesos,
        @DefaultValue TokenProceso tokenProceso,
        @DefaultValue Intentos intentos,
        @DefaultValue Demo demo) {

    /** Copia defensiva; sin procesos, el mapa queda vacío. */
    public PropiedadesUsuarios {
        procesos = procesos == null ? Map.of() : Map.copyOf(procesos);
    }

    /**
     * @param vigencia cuánto dura un token; 30 minutos por defecto
     * @param maximo cuánto puede durar una sesión renovándose; 8 horas por defecto (ADR-020)
     */
    public record Token(
            @DefaultValue("PT30M") Duration vigencia, @DefaultValue("PT8H") Duration maximo) {}

    /**
     * @param maximo intentos fallidos seguidos que bloquean un nombre; 5 por defecto
     * @param bloqueo cuánto dura el bloqueo; 15 minutos por defecto
     */
    public record Intentos(
            @DefaultValue("5") int maximo, @DefaultValue("PT15M") Duration bloqueo) {}

    /**
     * @param claves ruta del archivo {@code usuario=clave} del equipo de demostración
     * @param clave clave común de local y CI, si no hay archivo; nunca se registra
     */
    public record Demo(String claves, String clave) {

        /** No incluye la clave común. */
        @Override
        public String toString() {
            return "Demo[claves="
                    + claves
                    + ", clave común configurada="
                    + (clave != null && !clave.isBlank())
                    + "]";
        }
    }

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
