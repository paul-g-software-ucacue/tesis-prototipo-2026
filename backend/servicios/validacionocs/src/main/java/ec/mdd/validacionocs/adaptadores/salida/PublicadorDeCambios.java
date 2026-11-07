package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.validacionocs.puertos.salida.CambioDeFlujo;
import ec.mdd.validacionocs.puertos.salida.RegistroDeCambios;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica cada cambio como el evento {@value #TIPO} hacia Usuarios, para la bitácora (F17-H01). No
 * guarda nada más: la fila de la carga ya es el registro local. El evento lo guarda el registro
 * persistente de publicaciones en la transacción en curso, y se entrega después de confirmarla
 * (ADR-018).
 */
public final class PublicadorDeCambios implements RegistroDeCambios {

    /**
     * El tipo del evento, con el contenido {@code EntradaBitacora} de {@code contratos/usuarios}.
     */
    public static final String TIPO = "validacionocs.EntradaBitacora";

    private static final String ORIGEN = "validacionocs";
    private static final String DESTINO = "usuarios";

    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * Publicador sobre el {@link ApplicationEventPublisher} del contexto.
     *
     * @param eventos el publicador de Spring, que guarda el evento en la transacción en curso
     * @param reloj el momento de la publicación
     */
    public PublicadorDeCambios(ApplicationEventPublisher eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public void registrar(CambioDeFlujo cambio) {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", cambio.usuario());
        entrada.put("accion", cambio.accion().name());
        entrada.put("momento", cambio.momento().toString());
        if (cambio.detalle().get("periodo") instanceof String periodo) {
            entrada.put("periodo", periodo);
        }
        entrada.put("objeto", cambio.detalle());
        if (!cambio.anterior().isEmpty()) {
            entrada.put("anterior", cambio.anterior());
        }
        entrada.put("nuevo", cambio.nuevo());
        eventos.publishEvent(EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada, reloj));
    }
}
