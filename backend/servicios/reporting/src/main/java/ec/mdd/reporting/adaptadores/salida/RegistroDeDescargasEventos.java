package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.puertos.salida.RegistroDeDescargas;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Cada descarga como el evento {@value #TIPO} hacia Usuarios, para la bitácora (F17-H01, F20-H01,
 * D-115). El registro de publicaciones del proceso salida lo guarda en la transacción en curso y lo
 * entrega después de confirmarla (ADR-018); si identidad no responde, lo reenvía.
 *
 * <p>Reporting no guarda una copia local: su esquema no tiene otra cosa (principio X).
 */
public final class RegistroDeDescargasEventos implements RegistroDeDescargas {

    /**
     * El tipo del evento, con el contenido {@code EntradaBitacora} de {@code contratos/usuarios}.
     */
    public static final String TIPO = "reporting.EntradaBitacora";

    /** La acción de la bitácora. */
    public static final String ACCION = "REPORTE_INDIVIDUAL_DESCARGADO";

    private static final String ORIGEN = "reporting";
    private static final String DESTINO = "usuarios";

    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * @param eventos el publicador de Spring, que guarda el evento en la transacción en curso
     * @param reloj el momento de la publicación
     */
    public RegistroDeDescargasEventos(ApplicationEventPublisher eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public void registrar(DescargaDeReporte descarga) {
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada(descarga), reloj));
    }

    /** El contenido {@code EntradaBitacora}, sin el nombre del docente ni sus horas. */
    static Map<String, Object> entrada(DescargaDeReporte descarga) {
        Map<String, Object> objeto = new LinkedHashMap<>();
        objeto.put("periodo", descarga.periodo());
        objeto.put("version", descarga.version());
        objeto.put("identificacion", descarga.identificacion());
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", descarga.usuario());
        entrada.put("accion", ACCION);
        entrada.put("momento", descarga.momento().toString());
        entrada.put("periodo", descarga.periodo());
        entrada.put("objeto", objeto);
        entrada.put("nuevo", Map.of("formato", descarga.formato().name()));
        return entrada;
    }
}
