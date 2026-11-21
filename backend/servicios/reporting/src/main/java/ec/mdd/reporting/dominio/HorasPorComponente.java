package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Las horas del docente en cada uno de los cinco componentes, también los que suman 0, y su total
 * (RF-45).
 *
 * @param clase de clase
 * @param otrasDocencia de otras actividades de docencia
 * @param investigacion de investigación
 * @param vinculacion de vinculación
 * @param gestion de gestión
 */
public record HorasPorComponente(
        BigDecimal clase,
        BigDecimal otrasDocencia,
        BigDecimal investigacion,
        BigDecimal vinculacion,
        BigDecimal gestion) {

    /** Un docente sin horas. */
    public static final HorasPorComponente NINGUNA =
            new HorasPorComponente(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);

    /** Exige horas válidas en los cinco componentes. */
    public HorasPorComponente {
        clase = Horas.validas(clase);
        otrasDocencia = Horas.validas(otrasDocencia);
        investigacion = Horas.validas(investigacion);
        vinculacion = Horas.validas(vinculacion);
        gestion = Horas.validas(gestion);
    }

    /** La suma de los cinco componentes. */
    public BigDecimal total() {
        return clase.add(otrasDocencia).add(investigacion).add(vinculacion).add(gestion);
    }

    /**
     * @param componente el componente
     * @return sus horas
     */
    public BigDecimal de(Componente componente) {
        return switch (componente) {
            case CLASE -> clase;
            case OTRAS_DOCENCIA -> otrasDocencia;
            case INVESTIGACION -> investigacion;
            case VINCULACION -> vinculacion;
            case GESTION -> gestion;
        };
    }

    /** Los cinco componentes con sus horas, en el orden del glosario. */
    public Map<Componente, BigDecimal> porComponente() {
        Map<Componente, BigDecimal> horas = new EnumMap<>(Componente.class);
        for (Componente componente : Componente.values()) {
            horas.put(componente, de(componente));
        }
        return horas;
    }
}
