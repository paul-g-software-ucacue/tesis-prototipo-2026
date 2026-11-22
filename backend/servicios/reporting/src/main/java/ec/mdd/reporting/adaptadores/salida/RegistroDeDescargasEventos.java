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
 * F20-H02, D-115, D-117), con la acción de su tipo de reporte. El registro de publicaciones del
 * proceso salida lo guarda en la transacción en curso y lo entrega después de confirmarla
 * (ADR-018); si identidad no responde, lo reenvía.
 *
 * <p>Reporting no guarda una copia local: su esquema no tiene otra cosa (principio X).
 */
public final class RegistroDeDescargasEventos implements RegistroDeDescargas {

    /**
     * El tipo del evento, con el contenido {@code EntradaBitacora} de {@code contratos/usuarios}.
     */
    public static final String TIPO = "reporting.EntradaBitacora";

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

    /**
     * El contenido {@code EntradaBitacora}, sin nombres ni horas. La acción depende del tipo de
     * reporte (D-117); el objeto del general es el periodo y la versión, sin docente.
     */
    static Map<String, Object> entrada(DescargaDeReporte descarga) {
        Map<String, Object> objeto = new LinkedHashMap<>();
        objeto.put("periodo", descarga.periodo());
        objeto.put("version", descarga.version());
        if (descarga.identificacion() != null) {
            objeto.put("identificacion", descarga.identificacion());
        }
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", descarga.usuario());
        entrada.put("accion", descarga.tipo().accion());
        entrada.put("momento", descarga.momento().toString());
        entrada.put("periodo", descarga.periodo());
        entrada.put("objeto", objeto);
        entrada.put("nuevo", Map.of("formato", descarga.formato().name()));
        return entrada;
    }
}
