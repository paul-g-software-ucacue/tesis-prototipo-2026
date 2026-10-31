package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Lo que Generación sabe de una asignatura de la oferta para calcular la afinidad. Es un tipo
 * propio de este contexto, no el de Académico (principio V).
 *
 * @param codigo su código
 * @param nombre su nombre
 * @param campoAmplio su campo amplio
 * @param campoEspecifico su campo específico
 */
public record AsignaturaOfertada(
        String codigo, String nombre, String campoAmplio, String campoEspecifico) {

    /** Exige los cuatro datos. */
    public AsignaturaOfertada {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(campoAmplio, "campoAmplio");
        Objects.requireNonNull(campoEspecifico, "campoEspecifico");
    }
}
