package ec.mdd.normativa.dominio;

/**
 * Las horas de una fila de la tabla de carga horaria tal como llegan en una edición, identificada
 * por su perfil y su tipo de IST. La severidad, la referencia y la observación no se editan
 * (FR-002).
 *
 * @param aplicaA el tipo de IST, o {@code null} si aplica a todos
 * @param perfil el perfil
 * @param dedicacionTotal horas semanales de la dedicación
 * @param horasClase horas de clase
 * @param horasClaseMinimo mínimo de horas de clase
 * @param horasClaseMaximo máximo de horas de clase
 * @param horasOtrasDocencia horas de otras actividades de docencia; {@code null} si no se dan
 * @param horasGestion horas de gestión; {@code null} si no se dan
 * @param horasInvestigacionVinculacionMaximo máximo de investigación y vinculación; {@code null} si
 *     no se da
 */
public record HorasDeFila(
        TipoIst aplicaA,
        String perfil,
        int dedicacionTotal,
        int horasClase,
        int horasClaseMinimo,
        int horasClaseMaximo,
        Integer horasOtrasDocencia,
        Integer horasGestion,
        Integer horasInvestigacionVinculacionMaximo) {

    /** Las horas de una fila existente. */
    public static HorasDeFila de(FilaCargaHoraria fila) {
        return new HorasDeFila(
                fila.aplicaA(),
                fila.perfil(),
                fila.dedicacionTotal(),
                fila.horasClase(),
                fila.horasClaseMinimo(),
                fila.horasClaseMaximo(),
                fila.horasOtrasDocencia(),
                fila.horasGestion(),
                fila.horasInvestigacionVinculacionMaximo());
    }

    /** Las mismas horas con otras horas de clase. */
    public HorasDeFila conHorasClase(int horas) {
        return new HorasDeFila(
                aplicaA,
                perfil,
                dedicacionTotal,
                horas,
                horasClaseMinimo,
                horasClaseMaximo,
                horasOtrasDocencia,
                horasGestion,
                horasInvestigacionVinculacionMaximo);
    }

    /**
     * La fila y el tipo, como los nombra la edición: {@code tablaCargaHoraria[DOCENTE_MT/TODOS]}.
     */
    public String campo() {
        return campo(perfil, aplicaA);
    }

    /** El campo de una fila, como lo nombran la edición y sus errores. */
    public static String campo(String perfil, TipoIst aplicaA) {
        return "tablaCargaHoraria["
                + perfil
                + "/"
                + (aplicaA == null ? "TODOS" : aplicaA.name())
                + "]";
    }
}
