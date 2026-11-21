package ec.mdd.reporting.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Los docentes de una versión congelada, con el hash de esa versión (D-115).
 *
 * @param hash el hash de la versión
 * @param docentes por nombre y, a igual nombre, por identificación
 */
public record DocentesDeLaVersion(String hash, List<Docente> docentes) {

    /** Exige el hash y copia la lista. */
    public DocentesDeLaVersion {
        Objects.requireNonNull(hash, "hash");
        docentes = List.copyOf(docentes);
    }

    /**
     * Un docente de la versión.
     *
     * @param identificacion su número de identificación
     * @param nombre sus apellidos y nombres
     */
    public record Docente(String identificacion, String nombre) {

        /** Exige los datos. */
        public Docente {
            Objects.requireNonNull(identificacion, "identificacion");
            Objects.requireNonNull(nombre, "nombre");
        }
    }
}
