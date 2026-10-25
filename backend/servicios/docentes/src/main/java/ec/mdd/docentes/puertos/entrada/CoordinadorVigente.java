package ec.mdd.docentes.puertos.entrada;

/** La carrera ya tiene un coordinador vigente: uno por carrera (RN-12, F03-H05). */
public class CoordinadorVigente extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos de la carrera. */
    public CoordinadorVigente() {
        super("La carrera ya tiene un coordinador vigente");
    }
}
