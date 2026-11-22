package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.reporting.dominio.CsvGenerado;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.LayoutCargado;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Cada descarga (F20-H01, F20-H02, D-115, D-117) y cada CSV para SIGA o carga del layout de
 * exportación (F19-H01, D-119) como el evento {@value #TIPO} hacia Usuarios, para la bitácora
 * (F17-H01), con su acción. El registro de publicaciones del proceso salida lo guarda en la
 * transacción en curso y lo entrega después de confirmarla (ADR-018); si identidad no responde, lo
 * reenvía.
 *
 * <p>Reporting no guarda una copia local: su esquema no tiene otra cosa (principio X).
 */
public final class RegistroEnBitacoraEventos implements RegistroEnBitacora {

    /**
     * El tipo del evento, con el contenido {@code EntradaBitacora} de {@code contratos/usuarios}.
     */
    public static final String TIPO = "reporting.EntradaBitacora";

    private static final String ORIGEN = "reporting";
    private static final String DESTINO = "usuarios";
    private static final String CSV_SIGA_GENERADO = "CSV_SIGA_GENERADO";
    private static final String LAYOUT_SIGA_CARGADO = "LAYOUT_SIGA_CARGADO";

    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    /**
     * @param eventos el publicador de Spring, que guarda el evento en la transacción en curso
     * @param reloj el momento de la publicación
     */
    public RegistroEnBitacoraEventos(ApplicationEventPublisher eventos, Clock reloj) {
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    public void registrar(DescargaDeReporte descarga) {
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada(descarga), reloj));
    }

    @Override
    public void registrar(CsvGenerado generado) {
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada(generado), reloj));
    }

    /**
     * Un CSV para SIGA (F19-H01, D-119): el objeto es el periodo y la versión publicada, y lo nuevo
     * es la versión del layout de exportación con que salió. Sin nombres, identificaciones ni
     * horas.
     */
    static Map<String, Object> entrada(CsvGenerado generado) {
        Map<String, Object> objeto = new LinkedHashMap<>();
        objeto.put("periodo", generado.periodo());
        objeto.put("version", generado.version());
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", generado.usuario());
        entrada.put("accion", CSV_SIGA_GENERADO);
        entrada.put("momento", generado.momento().toString());
        entrada.put("periodo", generado.periodo());
        entrada.put("objeto", objeto);
        entrada.put("nuevo", Map.of("layout", generado.layout()));
        return entrada;
    }

    @Override
    public void registrar(LayoutCargado cargado) {
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada(cargado), reloj));
    }

    /**
     * Una versión nueva del layout de exportación (F19-H01, D-119): no pertenece a un periodo; el
     * objeto es su número y lo nuevo, cuántas columnas tiene y su SHA-256.
     */
    static Map<String, Object> entrada(LayoutCargado cargado) {
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", cargado.usuario());
        entrada.put("accion", LAYOUT_SIGA_CARGADO);
        entrada.put("momento", cargado.momento().toString());
        entrada.put("objeto", Map.of("layout", cargado.layout()));
        Map<String, Object> nuevo = new LinkedHashMap<>();
        nuevo.put("columnas", cargado.columnas());
        nuevo.put("sha256", cargado.sha256());
        entrada.put("nuevo", nuevo);
        return entrada;
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
