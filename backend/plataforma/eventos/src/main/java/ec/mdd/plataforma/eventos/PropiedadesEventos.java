package ec.mdd.plataforma.eventos;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la entrega de eventos de un proceso ({@code mdd.eventos}, ADR-018).
 *
 * @param plazo plazo para conectar y para esperar la respuesta; 5 s por defecto
 * @param reenvio cada cuánto se reenvían las publicaciones fallidas
 * @param destinos URL del receptor de cada servicio destino, por ejemplo {@code usuarios}
 * @param tokenProceso URL del token de proceso de Usuarios
 * @param proceso nombre de este proceso, para pedir su token
 * @param clave clave del proceso ({@code MDD_CLAVE_PROCESO_<PROCESO>}); vacía, el proceso arranca
 *     igual y cada entrega falla hasta que se configure; nunca se registra
 */
@ConfigurationProperties("mdd.eventos")
public record PropiedadesEventos(
        @DefaultValue("PT5S") Duration plazo,
        @DefaultValue Reenvio reenvio,
        Map<String, URI> destinos,
        URI tokenProceso,
        String proceso,
        String clave) {

    /** Copia defensiva; sin destinos, el mapa queda vacío. */
    public PropiedadesEventos {
        destinos = destinos == null ? Map.of() : Map.copyOf(destinos);
    }

    /**
     * @param intervalo tiempo entre dos ciclos de la tarea programada; 30 s por defecto
     */
    public record Reenvio(@DefaultValue("PT30S") Duration intervalo) {}

    /** Sin la clave. */
    @Override
    public String toString() {
        return "PropiedadesEventos[plazo="
                + plazo
                + ", reenvio="
                + reenvio
                + ", destinos="
                + destinos
                + ", tokenProceso="
                + tokenProceso
                + ", proceso="
                + proceso
                + "]";
    }
}
