package ec.mdd.generacion.dominio;

/**
 * Cuánto le falta a un parcial (RF-24).
 *
 * @param ofertadas las asignaturas-paralelo ofertadas de la carrera
 * @param conPropuesta las ofertadas con propuesta; una retirada no cuenta
 */
public record AvanceDelParcial(int ofertadas, int conPropuesta) {

    /** Si todas las ofertadas tienen propuesta. */
    public boolean completo() {
        return ofertadas > 0 && conPropuesta == ofertadas;
    }
}
