package ec.mdd.normativa.dominio;

import java.util.Objects;

/**
 * Una fila de la tabla de carga horaria: las horas por componente de un perfil en un tipo de IST, o
 * en todos (carga 7, RN-03 a RN-15).
 *
 * <p>Invariantes: las horas son enteras y no negativas; el mínimo de clase no supera las horas de
 * clase y estas no superan el máximo; la suma de clase, otras actividades de docencia, gestión y el
 * máximo de investigación y vinculación no supera la dedicación total. Un valor que la política no
 * da es {@code null}, no cero. Una fila que no las cumple lanza {@link FilaInvalida}.
 *
 * @param aplicaA el tipo de IST al que aplica, o {@code null} si aplica a todos
 * @param perfil el perfil, por ejemplo {@code DOCENTE_TC}
 * @param dedicacionTotal horas semanales de la dedicación
 * @param horasClase horas de clase
 * @param horasClaseMinimo mínimo de horas de clase
 * @param horasClaseMaximo máximo de horas de clase
 * @param horasOtrasDocencia horas de otras actividades de docencia; {@code null} si no se dan
 * @param horasGestion horas de gestión; {@code null} si no se dan
 * @param horasInvestigacionVinculacionMaximo máximo de investigación y vinculación; {@code null} si
 *     no se da
 * @param severidad dura o blanda
 * @param referencia artículo de la política o del RCE
 * @param observacion nota de la política; {@code null} si no tiene
 */
public record FilaCargaHoraria(
        TipoIst aplicaA,
        String perfil,
        int dedicacionTotal,
        int horasClase,
        int horasClaseMinimo,
        int horasClaseMaximo,
        Integer horasOtrasDocencia,
        Integer horasGestion,
        Integer horasInvestigacionVinculacionMaximo,
        Severidad severidad,
        String referencia,
        String observacion) {

    /** Comprueba las invariantes. */
    public FilaCargaHoraria {
        if (perfil == null || perfil.isBlank()) {
            throw new IllegalArgumentException("La fila necesita un perfil");
        }
        Objects.requireNonNull(severidad, "severidad");
        Objects.requireNonNull(referencia, "referencia");
        noNegativas(
                perfil,
                aplicaA,
                dedicacionTotal,
                horasClase,
                horasClaseMinimo,
                horasClaseMaximo,
                valor(horasOtrasDocencia),
                valor(horasGestion),
                valor(horasInvestigacionVinculacionMaximo));
        if (horasClaseMinimo > horasClase || horasClase > horasClaseMaximo) {
            throw new FilaInvalida(
                    perfil,
                    aplicaA,
                    "Las horas de clase ("
                            + horasClase
                            + ") no están entre el mínimo y el máximo ("
                            + horasClaseMinimo
                            + " a "
                            + horasClaseMaximo
                            + ")");
        }
        int suma =
                horasClase
                        + valor(horasOtrasDocencia)
                        + valor(horasGestion)
                        + valor(horasInvestigacionVinculacionMaximo);
        if (suma > dedicacionTotal) {
            throw new FilaInvalida(
                    perfil,
                    aplicaA,
                    "Las horas (" + suma + ") superan la dedicación de " + dedicacionTotal);
        }
    }

    /** Si aplica a todos los tipos de IST. */
    public boolean aplicaATodos() {
        return aplicaA == null;
    }

    private static int valor(Integer horas) {
        return horas == null ? 0 : horas;
    }

    private static void noNegativas(String perfil, TipoIst aplicaA, int... horas) {
        for (int hora : horas) {
            if (hora < 0) {
                throw new FilaInvalida(perfil, aplicaA, "Las horas no pueden ser negativas");
            }
        }
    }
}
