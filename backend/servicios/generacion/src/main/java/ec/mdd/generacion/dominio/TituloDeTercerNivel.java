package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * El título de tercer nivel de la ficha, el único que dice su campo amplio.
 *
 * @param nombre el título
 * @param campoAmplio su campo amplio
 * @param campoEspecifico su campo específico
 */
public record TituloDeTercerNivel(String nombre, String campoAmplio, String campoEspecifico) {

    /** Exige los tres datos. */
    public TituloDeTercerNivel {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(campoAmplio, "campoAmplio");
        Objects.requireNonNull(campoEspecifico, "campoEspecifico");
    }
}
