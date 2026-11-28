package ec.mdd.reporting.dominio;

import java.util.Objects;

/**
 * La versión publicada no se exporta: le faltan firmas o tiene inconsistencias abiertas (RF-53,
 * F19-H02, D-120). Lleva las condiciones completas, para decir todo lo que falla de una vez.
 */
public class ExportacionRechazada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String periodo;
    private final int version;
    private final transient CondicionesDeExportacion condiciones;

    /**
     * @param periodo el código del periodo
     * @param version la versión publicada
     * @param condiciones las condiciones, con al menos un motivo
     */
    public ExportacionRechazada(String periodo, int version, CondicionesDeExportacion condiciones) {
        super(
                "La versión "
                        + version
                        + " del periodo "
                        + periodo
                        + " no se exporta: "
                        + condiciones.motivos()
                        + ".");
        this.periodo = periodo;
        this.version = version;
        this.condiciones = Objects.requireNonNull(condiciones, "condiciones");
    }

    /** El código del periodo. */
    public String periodo() {
        return periodo;
    }

    /** La versión publicada. */
    public int version() {
        return version;
    }

    /** Las condiciones que no se cumplieron. */
    public CondicionesDeExportacion condiciones() {
        return condiciones;
    }
}
