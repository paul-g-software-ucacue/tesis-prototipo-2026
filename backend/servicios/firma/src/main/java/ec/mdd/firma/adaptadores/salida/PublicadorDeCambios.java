package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica cada cambio de Firma como el evento {@value #TIPO} hacia Usuarios, para la bitácora
 * (F17-H01, F13-H02). El registro persistente de publicaciones lo guarda en la transacción en curso
 * y se entrega después de confirmarla (ADR-018).
 */
public final class PublicadorDeCambios implements RegistroDeCambios {

    /**
     * El tipo del evento, con el contenido {@code EntradaBitacora} de {@code contratos/usuarios}.
     */
    public static final String TIPO = "firma.EntradaBitacora";

    private static final String ORIGEN = "firma";
    private static final String DESTINO = "usuarios";

    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * @param eventos el publicador de Spring, que guarda el evento en la transacción en curso
     * @param reloj el momento de la publicación
     */
    public PublicadorDeCambios(ApplicationEventPublisher eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public void registrar(CambioDeFirma cambio) {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", cambio.usuario());
        entrada.put("accion", cambio.accion());
        entrada.put("momento", cambio.momento().toString());
        if (cambio.detalle().get("periodo") instanceof String periodo) {
            entrada.put("periodo", periodo);
        }
        entrada.put("objeto", cambio.detalle());
        entrada.put("nuevo", cambio.nuevo());
        eventos.publishEvent(EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada, reloj));
    }
}
