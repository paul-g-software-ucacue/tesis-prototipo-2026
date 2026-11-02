package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Un cargo vigente de un docente, según Docentes. Es un tipo propio de este contexto (principio V).
 *
 * @param docente su número de identificación
 * @param tipo el tipo de cargo
 * @param carrera la carrera, en los cargos que la tienen; o {@code null}
 */
public record CargoVigente(String docente, String tipo, String carrera) {

    private static final String COORDINADOR = "COORDINADOR_CARRERA";
    private static final String GESTOR = "G\\d{2}_.+";

    /** Exige los datos. */
    public CargoVigente {
        Objects.requireNonNull(docente, "docente");
        Objects.requireNonNull(tipo, "tipo");
    }

    /** Si coordina esa carrera (RN-12). */
    public boolean coordina(String codigo) {
        return COORDINADOR.equals(tipo) && codigo.equals(carrera);
    }

    /** Si es uno de los cargos de gestión G06 a G21 (RN-10, D-73). */
    public boolean esDeGestion() {
        return tipo.matches(GESTOR);
    }
}
