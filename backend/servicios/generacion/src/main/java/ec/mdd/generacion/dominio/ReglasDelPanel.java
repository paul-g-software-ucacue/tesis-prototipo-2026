package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Las severidades y las cifras del panel, en la versión de normativa de un periodo (principio VI).
 * Sin alguna no se construye: no se inventan.
 *
 * @param severidades la severidad de cada regla, por su identificador
 * @param horasMaximasDocente el tope de RN-02
 * @param metaClase el porcentaje de clase de RN-05
 * @param metaOtrasDocencia el porcentaje de otras actividades de docencia de RN-05
 * @param margenPuntos los puntos que un docente puede apartarse de la meta (D-86)
 * @param gestoresMinimo el mínimo de RN-10
 * @param gestoresMaximo el máximo de RN-10
 * @param actividadesObligatorias los códigos de las que tienen que constar (RN-05)
 */
public record ReglasDelPanel(
        Map<String, Severidad> severidades,
        BigDecimal horasMaximasDocente,
        BigDecimal metaClase,
        BigDecimal metaOtrasDocencia,
        BigDecimal margenPuntos,
        int gestoresMinimo,
        int gestoresMaximo,
        List<String> actividadesObligatorias) {

    /** Exige las cifras y copia las colecciones. */
    public ReglasDelPanel {
        severidades = Map.copyOf(severidades);
        Objects.requireNonNull(horasMaximasDocente, "horasMaximasDocente");
        Objects.requireNonNull(metaClase, "metaClase");
        Objects.requireNonNull(metaOtrasDocencia, "metaOtrasDocencia");
        Objects.requireNonNull(margenPuntos, "margenPuntos");
        actividadesObligatorias = List.copyOf(actividadesObligatorias);
        for (TipoDeInconsistencia tipo : TipoDeInconsistencia.values()) {
            if (tipo.evalua() && !severidades.containsKey(tipo.regla())) {
                throw new IllegalArgumentException("Falta la severidad de " + tipo.regla());
            }
        }
    }

    /** La severidad de un tipo de inconsistencia en la versión. */
    public Severidad severidad(TipoDeInconsistencia tipo) {
        return severidades.get(tipo.regla());
    }

    /** El límite de un docente: sus horas contratadas, sin pasar el tope de RN-02. */
    public BigDecimal limite(int horasContratadas) {
        return BigDecimal.valueOf(horasContratadas).min(horasMaximasDocente);
    }
}
