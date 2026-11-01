package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Lo que Generación sabe de una asignatura-paralelo para sugerir docentes. Es un tipo propio de
 * este contexto, no el de Académico (principio V).
 *
 * @param asignatura su asignatura, con sus campos
 * @param paralelo su paralelo
 * @param carrera el código de su carrera: define qué son «otras carreras»
 * @param horasSemanales sus horas efectivas; admiten decimales
 * @param troncoComun si su asignatura es de tronco común: queda exenta del mínimo
 * @param retirada si salió de la oferta
 * @param periodoCerrado si el periodo está cerrado
 */
public record AsignaturaParalelo(
        AsignaturaOfertada asignatura,
        String paralelo,
        String carrera,
        BigDecimal horasSemanales,
        boolean troncoComun,
        boolean retirada,
        boolean periodoCerrado) {

    /** Exige los datos. */
    public AsignaturaParalelo {
        Objects.requireNonNull(asignatura, "asignatura");
        Objects.requireNonNull(paralelo, "paralelo");
        Objects.requireNonNull(carrera, "carrera");
        Objects.requireNonNull(horasSemanales, "horasSemanales");
    }

    /** Una asignatura-paralelo de un periodo que no está cerrado. */
    public AsignaturaParalelo(
            AsignaturaOfertada asignatura,
            String paralelo,
            String carrera,
            BigDecimal horasSemanales,
            boolean troncoComun,
            boolean retirada) {
        this(asignatura, paralelo, carrera, horasSemanales, troncoComun, retirada, false);
    }
}
