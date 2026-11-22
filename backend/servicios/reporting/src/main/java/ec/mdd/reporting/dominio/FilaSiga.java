package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Una fila del CSV para SIGA antes del layout: un docente y una de sus asignaturas-paralelo o de
 * sus actividades (ADR-002, regla 1; research §5, D-119).
 *
 * @param publicacion la versión publicada de la que sale
 * @param docente sus datos al congelar
 * @param componente {@link Componente#CLASE} en una asignatura; el de la actividad en otro caso
 * @param codigoDeActividad el código del catálogo; {@code null} en una asignatura
 * @param carrera el código de la carrera de la asignatura, o la del parcial de la actividad
 * @param codigoDeAsignatura su código; {@code null} en una actividad
 * @param delParalelo el paralelo; {@code null} en una actividad
 * @param codigoDeProyecto el proyecto de la actividad; {@code null} si no tiene
 * @param horas sus horas semanales
 */
public record FilaSiga(
        Publicacion publicacion,
        DatosDelDocente docente,
        Componente componente,
        String codigoDeActividad,
        String carrera,
        String codigoDeAsignatura,
        String delParalelo,
        String codigoDeProyecto,
        BigDecimal horas) {

    /** Exige los datos, horas válidas, y una asignatura o una actividad, no las dos. */
    public FilaSiga {
        Objects.requireNonNull(publicacion, "publicacion");
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(componente, "componente");
        Objects.requireNonNull(carrera, "carrera");
        horas = Horas.validas(horas);
        boolean esClase = componente == Componente.CLASE;
        if (esClase != (codigoDeAsignatura != null && delParalelo != null)
                || esClase == (codigoDeActividad != null)) {
            throw new IllegalArgumentException(
                    "una fila es de una asignatura-paralelo (clase) o de una actividad");
        }
    }

    /**
     * @param publicacion la versión publicada
     * @param docente el docente
     * @param asignatura una de sus asignaturas-paralelo, con el código de su carrera
     * @return su fila de clase
     */
    public static FilaSiga de(
            Publicacion publicacion, DatosDelDocente docente, FilaDeAsignatura asignatura) {
        return new FilaSiga(
                publicacion,
                docente,
                Componente.CLASE,
                null,
                asignatura
                        .codigoCarrera()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "asignatura sin el código de su carrera")),
                asignatura.codigo(),
                asignatura.paralelo(),
                null,
                asignatura.horas());
    }

    /**
     * @param publicacion la versión publicada
     * @param docente el docente
     * @param actividad una de sus actividades, con el código de su carrera
     * @return su fila
     */
    public static FilaSiga de(
            Publicacion publicacion, DatosDelDocente docente, FilaDeActividad actividad) {
        return new FilaSiga(
                publicacion,
                docente,
                actividad.componente(),
                actividad.codigo(),
                actividad
                        .codigoCarrera()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "actividad sin el código de su carrera")),
                null,
                null,
                actividad.codigoProyecto().orElse(null),
                actividad.horas());
    }

    /** El código de la actividad; vacío en una asignatura. */
    public Optional<String> actividad() {
        return Optional.ofNullable(codigoDeActividad);
    }

    /** El código de la asignatura; vacío en una actividad. */
    public Optional<String> asignatura() {
        return Optional.ofNullable(codigoDeAsignatura);
    }

    /** El paralelo; vacío en una actividad. */
    public Optional<String> paralelo() {
        return Optional.ofNullable(delParalelo);
    }

    /** El código del proyecto; vacío si no tiene. */
    public Optional<String> proyecto() {
        return Optional.ofNullable(codigoDeProyecto);
    }

    /** El código que la identifica dentro del docente: el de la asignatura o el de la actividad. */
    String codigo() {
        return codigoDeAsignatura != null ? codigoDeAsignatura : codigoDeActividad;
    }
}
