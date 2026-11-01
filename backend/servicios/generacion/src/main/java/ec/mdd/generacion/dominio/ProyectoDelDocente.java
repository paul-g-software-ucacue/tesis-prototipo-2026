package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Un proyecto asignable en el periodo en el que participa un docente. Es un tipo propio de este
 * contexto, no el de Docentes (principio V).
 *
 * @param codigo su código
 * @param nombre su nombre
 * @param tipo su tipo, como texto: Generación solo lo compara con el catálogo
 */
public record ProyectoDelDocente(String codigo, String nombre, String tipo) {

    /** Exige los datos. */
    public ProyectoDelDocente {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(tipo, "tipo");
    }
}
