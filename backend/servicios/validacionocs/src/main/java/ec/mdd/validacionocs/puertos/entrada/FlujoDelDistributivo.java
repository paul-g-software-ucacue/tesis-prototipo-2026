package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.AccionDelProceso;
import ec.mdd.validacionocs.dominio.TipoDeEtapa;
import ec.mdd.validacionocs.dominio.Transicion;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El estado del flujo del distributivo de un periodo, para quien lo consulta (esquema {@code
 * EstadoDelFlujo}, F12-H02, FR-014).
 *
 * @param periodo el código del periodo
 * @param etapa la etapa actual
 * @param nombreDeEtapa su nombre en la copia de la configuración
 * @param version la versión vigente
 * @param parciales el envío de cada parcial, por carrera
 * @param acciones lo que puede hacer ahora quien consulta
 * @param transiciones las transiciones, en orden
 * @param congelacion la congelación de la versión, desde que el distributivo llegó a
 *     CONGELADO_EN_FIRMA (F13-H01)
 * @param rechazoEnFirma el rechazo en firma por el que volvió, mientras sea la última transición
 *     (F14-H04)
 */
public record FlujoDelDistributivo(
        String periodo,
        TipoDeEtapa etapa,
        String nombreDeEtapa,
        int version,
        List<Parcial> parciales,
        List<AccionDelProceso> acciones,
        List<Transicion> transiciones,
        Optional<Congelacion> congelacion,
        Optional<RechazoEnFirma> rechazoEnFirma) {

    /** Exige los datos y copia las listas. */
    public FlujoDelDistributivo {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(etapa, "etapa");
        Objects.requireNonNull(congelacion, "congelacion");
        Objects.requireNonNull(rechazoEnFirma, "rechazoEnFirma");
        parciales = List.copyOf(parciales);
        acciones = List.copyOf(acciones);
        transiciones = List.copyOf(transiciones);
    }

    /** Un flujo antes de la congelación. */
    public FlujoDelDistributivo(
            String periodo,
            TipoDeEtapa etapa,
            String nombreDeEtapa,
            int version,
            List<Parcial> parciales,
            List<AccionDelProceso> acciones,
            List<Transicion> transiciones) {
        this(
                periodo,
                etapa,
                nombreDeEtapa,
                version,
                parciales,
                acciones,
                transiciones,
                Optional.empty(),
                Optional.empty());
    }

    /**
     * La congelación de la versión: el paso hacia CONGELADO_EN_FIRMA (F13-H01, esquema {@code
     * CongelacionDeLaVersion}).
     *
     * @param version la versión congelada
     * @param hash su hash, la huella que ve la pantalla
     * @param quien quién la congeló
     * @param momento cuándo
     */
    public record Congelacion(int version, String hash, String quien, Instant momento) {}

    /**
     * El rechazo en firma por el que volvió el distributivo (F14-H04, esquema {@code
     * RechazoEnFirmaDelFlujo}).
     *
     * @param orden el paso que se rechazó
     * @param rol su rol
     * @param por quién rechazó
     * @param en cuándo
     * @param observacion qué hay que corregir
     * @param versionAnulada la versión rechazada, que quedó anulada (F12-H04)
     * @param versionNueva la versión creada por el rechazo, si ya existe
     */
    public record RechazoEnFirma(
            int orden,
            String rol,
            String por,
            Instant en,
            String observacion,
            int versionAnulada,
            java.util.OptionalInt versionNueva) {}

    /**
     * El envío de un parcial.
     *
     * @param carrera su carrera
     * @param enviado si su transición de envío está registrada
     * @param pendienteDeConfirmar si Generación lo marcó y la transición no se registró (research
     *     §2)
     * @param enviadoPor quién lo envió
     * @param enviadoEn cuándo
     * @param devolucion la última devolución, mientras el parcial está devuelto (F12-H05)
     */
    public record Parcial(
            String carrera,
            boolean enviado,
            boolean pendienteDeConfirmar,
            Optional<String> enviadoPor,
            Optional<Instant> enviadoEn,
            Optional<Devolucion> devolucion) {}

    /**
     * La última devolución de un parcial (F12-H05, FR-015).
     *
     * @param por quién lo devolvió
     * @param en cuándo
     * @param observacion qué tiene que corregir la carrera
     */
    public record Devolucion(String por, Instant en, String observacion) {}
}
