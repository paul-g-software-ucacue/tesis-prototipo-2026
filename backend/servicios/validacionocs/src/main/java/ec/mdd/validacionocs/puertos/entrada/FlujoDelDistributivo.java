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
 */
public record FlujoDelDistributivo(
        String periodo,
        TipoDeEtapa etapa,
        String nombreDeEtapa,
        int version,
        List<Parcial> parciales,
        List<AccionDelProceso> acciones,
        List<Transicion> transiciones) {

    /** Exige los datos y copia las listas. */
    public FlujoDelDistributivo {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(etapa, "etapa");
        parciales = List.copyOf(parciales);
        acciones = List.copyOf(acciones);
        transiciones = List.copyOf(transiciones);
    }

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
