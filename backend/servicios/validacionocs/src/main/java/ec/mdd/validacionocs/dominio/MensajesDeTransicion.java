package ec.mdd.validacionocs.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Los mensajes de una transición para Notificaciones (F16-H01, D-108, contracts/eventos.md):
 * Aprobación conoce el flujo y arma el título, el detalle y el enlace; Notificaciones los guarda
 * tal como llegan. Los nombres de las etapas son los de la copia de la configuración.
 *
 * <p>El detalle de un rechazo en firma nombra el paso y el rol, no a la persona: Aprobación no
 * conoce su nombre para mostrar, y el autor de esa transición es el sistema. Una carrera se nombra
 * por su nombre en Académico; si no se tiene, por su código.
 */
public final class MensajesDeTransicion {

    /**
     * La ruta de la SPA donde se ve el distributivo en cada etapa de llegada, que el rol de la
     * etapa por defecto puede abrir. El Gestor de unidad no tiene una pantalla del distributivo: va
     * al inicio. PUBLICADO es provisorio hasta F18-H01.
     */
    private static final Map<TipoDeEtapa, String> ENLACES =
            Map.of(
                    TipoDeEtapa.BORRADOR_CARRERA, "/oferta",
                    TipoDeEtapa.BORRADOR_UNIDADES, "/",
                    TipoDeEtapa.EN_CONSOLIDACION, "/consolidado",
                    TipoDeEtapa.EN_REVISION_VRA, "/consolidado",
                    TipoDeEtapa.REMITIDO_ZONAL, "/consolidado",
                    TipoDeEtapa.CONGELADO_EN_FIRMA, "/firmas-pendientes",
                    TipoDeEtapa.EN_OCS, "/resolucion-ocs",
                    TipoDeEtapa.APROBADO, "/resolucion-ocs",
                    TipoDeEtapa.PUBLICADO, "/resolucion-ocs");

    /** El largo máximo de un título en el contrato de Notificaciones. */
    private static final int TITULO_MAXIMO = 200;

    private MensajesDeTransicion() {}

    /**
     * El enlace de una etapa de llegada.
     *
     * @param etapa la etapa
     * @return la ruta de la SPA
     */
    public static String enlaceDe(TipoDeEtapa etapa) {
        return ENLACES.get(etapa);
    }

    /**
     * Los mensajes de una transición, uno por destino ({@link ProcesoAprobacion#destinoDe}).
     *
     * @param proceso el proceso, con la copia de la configuración
     * @param transicion una transición de ese proceso
     * @param nombresDeCarreras el nombre de cada carrera, por su código; puede no tener alguna
     * @return los mensajes
     */
    public static List<MensajeDeTransicion> de(
            ProcesoAprobacion proceso,
            Transicion transicion,
            Map<String, String> nombresDeCarreras) {
        List<MensajeDeTransicion> mensajes = new ArrayList<>();
        String datos = "Periodo " + proceso.periodo() + ", versión " + transicion.version() + ".";
        String llegada = "«" + proceso.etapaDe(transicion.hacia()).nombre() + "»";
        for (DestinoDeNotificacion destino : proceso.destinoDe(transicion)) {
            mensajes.add(
                    switch (transicion.tipo()) {
                        case ENVIO_PARCIAL ->
                                new MensajeDeTransicion(
                                        destino,
                                        "La carrera "
                                                + nombre(
                                                        transicion.carrera().orElse(""),
                                                        nombresDeCarreras)
                                                + " envió su parcial",
                                        datos,
                                        enlaceDe(
                                                proceso.siguienteActiva(
                                                        TipoDeEtapa.BORRADOR_CARRERA)));
                        case PASO_DE_ETAPA ->
                                new MensajeDeTransicion(
                                        destino,
                                        "El distributivo llegó a " + llegada,
                                        datos,
                                        enlaceDe(transicion.hacia()));
                        case DEVOLUCION -> {
                            String carrera = destino.carrera().orElseThrow();
                            yield new MensajeDeTransicion(
                                    destino,
                                    titulo(
                                            "El parcial de ",
                                            nombre(carrera, nombresDeCarreras),
                                            " fue devuelto a " + llegada),
                                    "Observación: «"
                                            + transicion.observaciones().get(carrera).texto()
                                            + "»",
                                    enlaceDe(transicion.hacia()));
                        }
                        case RECHAZO_EN_FIRMA -> {
                            RechazoDelFirmante rechazo = transicion.rechazo().orElseThrow();
                            yield new MensajeDeTransicion(
                                    destino,
                                    "La firma F"
                                            + rechazo.orden()
                                            + " fue rechazada: el distributivo volvió a "
                                            + llegada,
                                    "Rechazó el paso F"
                                            + rechazo.orden()
                                            + " ("
                                            + rechazo.rol()
                                            + "): «"
                                            + rechazo.observacion().texto()
                                            + "»",
                                    enlaceDe(transicion.hacia()));
                        }
                        case VERSION_NUEVA ->
                                new MensajeDeTransicion(
                                        destino,
                                        "Se creó la versión "
                                                + transicion.version()
                                                + " del distributivo",
                                        "La versión "
                                                + (transicion.version() - 1)
                                                + " quedó anulada por el rechazo en firma.",
                                        enlaceDe(transicion.hacia()));
                    });
        }
        return List.copyOf(mensajes);
    }

    private static String nombre(String carrera, Map<String, String> nombres) {
        return nombres.getOrDefault(carrera, carrera);
    }

    // Un título con el nombre de una carrera (hasta 150 caracteres) y el de una etapa (hasta 80)
    // puede pasar del máximo del contrato: se acorta el nombre de la carrera con «…».
    private static String titulo(String antes, String carrera, String despues) {
        int cabe = TITULO_MAXIMO - antes.length() - despues.length();
        String nombre =
                carrera.length() <= cabe ? carrera : carrera.substring(0, cabe - 1).strip() + "…";
        return antes + nombre + despues;
    }
}
