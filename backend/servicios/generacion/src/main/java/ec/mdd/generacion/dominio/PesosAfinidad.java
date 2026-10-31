package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Los puntos que suma cada título, experiencia o curso relacionado con una asignatura (RN-24). Son
 * los de la versión de normativa del periodo: Generación los recibe, nunca los escribe en el código
 * (principio VI).
 *
 * @param cuartoNivelCampoEspecifico un título de cuarto nivel en el campo específico
 * @param cuartoNivelCampoAmplio un título de cuarto nivel que solo coincide en el campo amplio
 * @param tercerNivelCampoEspecifico un título de tercer nivel en el campo específico
 * @param tercerNivelCampoAmplio un título de tercer nivel que solo coincide en el campo amplio
 * @param experienciaRelacionada cada experiencia relacionada
 * @param cursoRelacionado cada curso relacionado
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

    /** Exige los siete valores. */
    public PesosAfinidad {
        Objects.requireNonNull(cuartoNivelCampoEspecifico, "cuartoNivelCampoEspecifico");
        Objects.requireNonNull(cuartoNivelCampoAmplio, "cuartoNivelCampoAmplio");
        Objects.requireNonNull(tercerNivelCampoEspecifico, "tercerNivelCampoEspecifico");
        Objects.requireNonNull(tercerNivelCampoAmplio, "tercerNivelCampoAmplio");
        Objects.requireNonNull(experienciaRelacionada, "experienciaRelacionada");
        Objects.requireNonNull(cursoRelacionado, "cursoRelacionado");
        Objects.requireNonNull(topeCursos, "topeCursos");
    }
}
