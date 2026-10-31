package ec.mdd.generacion.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Lo que Generación sabe de la ficha de un docente para calcular la afinidad. Es un tipo propio de
 * este contexto, no el de Docentes (principio V), y no trae asignaturas afines: no suman hasta que
 * el coordinador las valide.
 *
 * @param tercerNivel su título de tercer nivel
 * @param cuartoNivel sus títulos de cuarto nivel
 * @param experiencia sus cargos
 * @param cursos sus cursos
 */
public record FormacionDelDocente(
        TituloDeTercerNivel tercerNivel,
        List<ElementoDeFormacion> cuartoNivel,
        List<ElementoDeFormacion> experiencia,
        List<ElementoDeFormacion> cursos) {

    /** Exige el tercer nivel y copia las listas. */
    public FormacionDelDocente {
        Objects.requireNonNull(tercerNivel, "tercerNivel");
        cuartoNivel = List.copyOf(cuartoNivel);
        experiencia = List.copyOf(experiencia);
        cursos = List.copyOf(cursos);
    }
}
