package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Un título de cuarto nivel, un cargo o un curso de la ficha, con su campo específico.
 *
 * @param nombre el título, el cargo o el nombre del curso
 * @param campoEspecifico su campo específico
 */
public record ElementoDeFormacion(String nombre, String campoEspecifico) {

    /** Exige los dos datos. */
    public ElementoDeFormacion {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(campoEspecifico, "campoEspecifico");
    }
}
