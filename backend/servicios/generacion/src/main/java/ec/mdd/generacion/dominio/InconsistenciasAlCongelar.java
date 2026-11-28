package ec.mdd.generacion.dominio;

/**
 * Las inconsistencias abiertas de una versión cuando se guardó su contenido congelado (F19-H02,
 * D-120): las duras y las blandas sin justificar del panel (MD-07). Se fijan ahí porque el panel se
 * calcula al abrirlo, con la oferta y las reglas del día; después de congelar ya no se puede
 * reconstruir lo que valía para la versión firmada.
 *
 * @param duras las duras, desde 0
 * @param blandas las blandas sin justificar, desde 0
 */
public record InconsistenciasAlCongelar(int duras, int blandas) {

    /** Exige números desde 0. */
    public InconsistenciasAlCongelar {
        if (duras < 0 || blandas < 0) {
            throw new IllegalArgumentException("Las inconsistencias abiertas van desde 0");
        }
    }
}
