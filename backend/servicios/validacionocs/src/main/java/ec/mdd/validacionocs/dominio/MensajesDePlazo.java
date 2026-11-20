package ec.mdd.validacionocs.dominio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Los mensajes del vencimiento de un plazo para Notificaciones (F12-H03, D-114), como {@link
 * MensajesDeTransicion}: Aprobación arma el título, el detalle y el enlace, y Notificaciones los
 * guarda tal como llegan. Uno por destino de {@link ProcesoAprobacion#destinosDelVencimiento}. No
 * llevan datos personales: el periodo, la etapa, las fechas y los días.
 *
 * <p>El rol de la etapa va a la pantalla de su etapa; el Vicerrector, a «Consolidado del periodo»,
 * donde ve la tarjeta del flujo con el plazo. En BORRADOR_CARRERA cada coordinación lee cuál de sus
 * carreras falta, por su nombre en Académico o, si no se tiene, por su código.
 */
public final class MensajesDePlazo {

    /** El largo máximo de un título en el contrato de Notificaciones. */
    private static final int TITULO_MAXIMO = 200;

    /** El largo máximo de un detalle en el contrato de Notificaciones. */
    private static final int DETALLE_MAXIMO = 1000;

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private static final String ENLACE_DEL_VICERRECTOR = "/consolidado";

    private MensajesDePlazo() {}

    /**
     * @param proceso el proceso, con la copia de la configuración
     * @param vencimiento el vencimiento de su etapa actual
     * @param nombresDeCarreras el nombre de cada carrera, por su código; puede no tener alguna
     * @return un mensaje por destino
     */
    public static List<MensajeDeTransicion> de(
            ProcesoAprobacion proceso,
            VencimientoDePlazo vencimiento,
            Map<String, String> nombresDeCarreras) {
        Objects.requireNonNull(nombresDeCarreras, "nombresDeCarreras");
        Plazo plazo = vencimiento.plazo();
        EtapaDelProceso etapa = proceso.etapaDe(plazo.etapa());
        String nombre = "«" + etapa.nombre() + "»";
        String titulo = acortar("Venció el plazo de " + nombre, TITULO_MAXIMO);
        String detalle =
                "El distributivo de "
                        + proceso.periodo()
                        + " está en "
                        + nombre
                        + " desde el "
                        + FECHA.format(LocalDate.ofInstant(plazo.llegada(), ProcesoAprobacion.ZONA))
                        + ". Su plazo de "
                        + dias(plazo.dias())
                        + " venció el "
                        + FECHA.format(plazo.fechaLimite())
                        + "; lleva "
                        + dias(vencimiento.diasDeAtraso())
                        + " de atraso.";
        return proceso.destinosDelVencimiento().stream()
                .map(
                        destino -> {
                            boolean delaEtapa = destino.rol().equals(etapa.rol());
                            String texto =
                                    destino.carrera()
                                            .map(
                                                    carrera ->
                                                            detalle
                                                                    + " La carrera "
                                                                    + nombresDeCarreras
                                                                            .getOrDefault(
                                                                                    carrera,
                                                                                    carrera)
                                                                    + " todavía no envió su"
                                                                    + " parcial.")
                                            .orElse(detalle);
                            return new MensajeDeTransicion(
                                    destino,
                                    titulo,
                                    acortar(texto, DETALLE_MAXIMO),
                                    delaEtapa
                                            ? MensajesDeTransicion.enlaceDe(plazo.etapa())
                                            : ENLACE_DEL_VICERRECTOR);
                        })
                .toList();
    }

    private static String dias(int cantidad) {
        return cantidad == 1 ? "1 día" : cantidad + " días";
    }

    // Un nombre de etapa largo (hasta 80 caracteres en la configuración) no debería pasar del
    // contrato, pero se acorta con «…» para no perder el aviso.
    private static String acortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1).strip() + "…";
    }
}
