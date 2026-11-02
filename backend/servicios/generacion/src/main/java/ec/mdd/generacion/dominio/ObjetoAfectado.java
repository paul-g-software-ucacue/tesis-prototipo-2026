package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Lo que incumple una regla global.
 *
 * @param clase qué es
 * @param clave lo identifica dentro de su clase
 * @param nombre lo que se muestra
 */
public record ObjetoAfectado(Clase clase, String clave, String nombre) {

    /** Las clases de objeto. */
    public enum Clase {
        /** Un docente. */
        DOCENTE,
        /** Una asignatura-paralelo. */
        PARALELO,
        /** Una carrera. */
        CARRERA,
        /** El periodo completo. */
        PERIODO
    }

    /** Exige los datos. */
    public ObjetoAfectado {
        Objects.requireNonNull(clase, "clase");
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(nombre, "nombre");
    }

    /** Un docente. */
    public static ObjetoAfectado de(DocenteActivo docente) {
        return new ObjetoAfectado(Clase.DOCENTE, docente.identificacion(), docente.nombre());
    }

    /** Una asignatura-paralelo. */
    public static ObjetoAfectado de(AsignaturaParalelo una) {
        return new ObjetoAfectado(
                Clase.PARALELO,
                una.asignatura().codigo() + "/" + una.paralelo(),
                una.asignatura().nombre() + " · " + una.paralelo());
    }

    /** Una carrera, por su código. */
    public static ObjetoAfectado carrera(String codigo) {
        return new ObjetoAfectado(Clase.CARRERA, codigo, codigo);
    }

    /** El periodo. */
    public static ObjetoAfectado periodo(String codigo) {
        return new ObjetoAfectado(Clase.PERIODO, codigo, codigo);
    }
}
