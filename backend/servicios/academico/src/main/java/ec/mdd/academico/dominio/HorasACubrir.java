package ec.mdd.academico.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las horas a cubrir de la oferta de un periodo: del periodo, por carrera y por asignatura
 * (glosario «Horas a cubrir», RF-15). Solo suman las asignaturas-paralelo ofertadas.
 *
 * @param total las del periodo
 * @param carreras las de cada carrera con oferta, por código
 */
public record HorasACubrir(BigDecimal total, List<DeCarrera> carreras) {

    /** Copia la lista. */
    public HorasACubrir {
        carreras = List.copyOf(carreras);
    }

    /** Las de cada asignatura de la oferta, carrera por carrera. */
    public List<DeAsignatura> asignaturas() {
        return carreras.stream().flatMap(carrera -> carrera.asignaturas().stream()).toList();
    }

    /**
     * Las horas a cubrir de una carrera.
     *
     * @param carrera el código de la carrera
     * @param total la suma de sus asignaturas
     * @param asignaturas las de cada una de sus asignaturas en la oferta, por ciclo y código
     */
    public record DeCarrera(String carrera, BigDecimal total, List<DeAsignatura> asignaturas) {

        /** Copia la lista. */
        public DeCarrera {
            asignaturas = List.copyOf(asignaturas);
        }
    }

    /**
     * Las horas a cubrir de una asignatura: la suma de sus paralelos ofertados.
     *
     * @param asignatura el código de la asignatura
     * @param ciclo su ciclo en la malla
     * @param paralelos los paralelos ofertados; los retirados no cuentan
     * @param total la suma de sus horas
     */
    public record DeAsignatura(String asignatura, int ciclo, int paralelos, BigDecimal total) {}
}
