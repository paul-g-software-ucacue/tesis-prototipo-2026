package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import ec.mdd.reporting.dominio.CondicionesDeExportacion;
import ec.mdd.reporting.dominio.DescargaDeReporte;
import ec.mdd.reporting.dominio.ExportacionSiga;
import ec.mdd.reporting.dominio.LayoutCargado;
import ec.mdd.reporting.dominio.RechazoDeExportacion;
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
    private static final String EXPORTACION_SIGA_RECHAZADA = "EXPORTACION_SIGA_RECHAZADA";

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
    public void registrar(ExportacionSiga exportacion) {
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada(exportacion), reloj));
    }

    /**
     * Un CSV para SIGA entregado (F19-H01, D-119; F19-H02, D-120): el objeto es el periodo y la
     * versión publicada, y lo nuevo, la versión del layout de exportación, el registro de la
     * exportación, sus filas y su suma de verificación. Sin hashes de la versión, nombres,
     * identificaciones ni horas.
     *
     * <p>Se conserva la acción {@code CSV_SIGA_GENERADO} de F19-H01: el CSV con su suma y su
     * registro es la Exportación a SIGA del glosario, y otra acción dejaría dos nombres para lo
     * mismo en la historia de la bitácora (research §5).
     */
    static Map<String, Object> entrada(ExportacionSiga exportacion) {
        Map<String, Object> objeto = new LinkedHashMap<>();
        objeto.put("periodo", exportacion.periodo());
        objeto.put("version", exportacion.version());
        Map<String, Object> nuevo = new LinkedHashMap<>();
        nuevo.put("layout", exportacion.layout());
        nuevo.put("exportacion", exportacion.id().toString());
        nuevo.put("filas", exportacion.filas());
        nuevo.put("sha256", exportacion.suma().valor());
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", exportacion.usuario());
        entrada.put("accion", CSV_SIGA_GENERADO);
        entrada.put("momento", exportacion.momento().toString());
        entrada.put("periodo", exportacion.periodo());
        entrada.put("objeto", objeto);
        entrada.put("nuevo", nuevo);
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

    @Override
    public void registrar(RechazoDeExportacion rechazo) {
        eventos.publishEvent(
                EventoEntreProcesos.nuevo(TIPO, ORIGEN, DESTINO, entrada(rechazo), reloj));
    }

    /**
     * Un CSV para SIGA rechazado (F19-H02, D-120): el objeto es el periodo y la versión, y lo nuevo
     * son los motivos y, cuando se conocen, los números: los pasos firmados de cuántos, y las duras
     * y las blandas abiertas. Sin hashes ni datos personales.
     */
    static Map<String, Object> entrada(RechazoDeExportacion rechazo) {
        Map<String, Object> objeto = new LinkedHashMap<>();
        objeto.put("periodo", rechazo.periodo());
        objeto.put("version", rechazo.version());
        CondicionesDeExportacion condiciones = rechazo.condiciones();
        Map<String, Object> nuevo = new LinkedHashMap<>();
        nuevo.put("motivos", condiciones.motivos().stream().map(Enum::name).toList());
        condiciones
                .firmas()
                .ifPresent(
                        firmas -> {
                            nuevo.put("firmados", firmas.firmados());
                            nuevo.put("pasos", firmas.total());
                        });
        condiciones
                .inconsistencias()
                .ifPresent(
                        abiertas -> {
                            nuevo.put("duras", abiertas.duras());
                            nuevo.put("blandas", abiertas.blandas());
                        });
        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("usuario", rechazo.usuario());
        entrada.put("accion", EXPORTACION_SIGA_RECHAZADA);
        entrada.put("momento", rechazo.momento().toString());
        entrada.put("periodo", rechazo.periodo());
        entrada.put("objeto", objeto);
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
