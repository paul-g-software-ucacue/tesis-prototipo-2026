package ec.mdd.normativa.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Los puntos que suma cada título, experiencia o curso relacionado con una asignatura (RN-24,
 * glosario §7.3). Son los parámetros de la regla RN-24 de la versión; Distributivo los usa para
 * calcular el puntaje de afinidad (MD-02, ADR-010).
 *
 * <p>Invariante: un título suma más si coincide el campo específico de la asignatura que si solo
 * coincide el campo amplio (RN-24), así que, en cada nivel, el peso del campo amplio no supera al
 * del específico. Iguales es válido.
 *
 * @param cuartoNivelCampoEspecifico un título de cuarto nivel en el campo específico
 * @param cuartoNivelCampoAmplio un título de cuarto nivel que solo coincide en el campo amplio
 * @param tercerNivelCampoEspecifico un título de tercer nivel en el campo específico
 * @param tercerNivelCampoAmplio un título de tercer nivel que solo coincide en el campo amplio
 * @param experienciaRelacionada la experiencia laboral relacionada
 * @param cursoRelacionado cada curso o capacitación relacionada
 * @param topeCursos lo máximo que suman los cursos
 */
public record PesosAfinidad(
        BigDecimal cuartoNivelCampoEspecifico,
        BigDecimal cuartoNivelCampoAmplio,
        BigDecimal tercerNivelCampoEspecifico,
        BigDecimal tercerNivelCampoAmplio,
        BigDecimal experienciaRelacionada,
        BigDecimal cursoRelacionado,
        BigDecimal topeCursos) {

    /** La regla que los da. */
    public static final String REGLA = "RN-24";

    /**
     * Comprueba que el campo amplio no pese más que el específico en ningún nivel.
     *
     * @throws CambioNoPermitido si lo supera, con el parámetro del campo amplio
     */
    public PesosAfinidad {
        Objects.requireNonNull(experienciaRelacionada, "experienciaRelacionada");
        Objects.requireNonNull(cursoRelacionado, "cursoRelacionado");
        Objects.requireNonNull(topeCursos, "topeCursos");
        noSupera("cuarto_nivel_campo_amplio", cuartoNivelCampoAmplio, cuartoNivelCampoEspecifico);
        noSupera("tercer_nivel_campo_amplio", tercerNivelCampoAmplio, tercerNivelCampoEspecifico);
    }

    /**
     * Los pesos de la regla RN-24.
     *
     * @throws IllegalArgumentException si la regla no es RN-24 o le falta un parámetro
     */
    public static PesosAfinidad de(Regla regla) {
        if (!REGLA.equals(regla.identificador())) {
            throw new IllegalArgumentException(
                    "Los pesos de afinidad son de " + REGLA + ", no de " + regla.identificador());
        }
        return new PesosAfinidad(
                parametro(regla, "cuarto_nivel_campo_especifico"),
                parametro(regla, "cuarto_nivel_campo_amplio"),
                parametro(regla, "tercer_nivel_campo_especifico"),
                parametro(regla, "tercer_nivel_campo_amplio"),
                parametro(regla, "experiencia_relacionada"),
                parametro(regla, "curso_relacionado"),
                parametro(regla, "tope_cursos"));
    }

    private static BigDecimal parametro(Regla regla, String nombre) {
        return regla.parametro(nombre)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "A " + REGLA + " le falta el parámetro " + nombre));
    }

    private static void noSupera(String nombre, BigDecimal amplio, BigDecimal especifico) {
        Objects.requireNonNull(amplio, nombre);
        Objects.requireNonNull(especifico, nombre);
        if (amplio.compareTo(especifico) > 0) {
            throw new CambioNoPermitido(
                    "reglas[" + REGLA + "]." + nombre,
                    "El peso del campo amplio ("
                            + amplio.stripTrailingZeros().toPlainString()
                            + ") no puede superar al del campo específico ("
                            + especifico.stripTrailingZeros().toPlainString()
                            + ")");
        }
    }
}
