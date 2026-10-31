package ec.mdd.generacion.dominio;

import java.util.Objects;

/**
 * Lo que Generación sabe de un docente activo para sugerirlo. Es un tipo propio de este contexto,
 * no el de Docentes (principio V).
 *
 * @param identificacion su número de identificación
 * @param nombre sus apellidos y nombres
 * @param dedicacion su dedicación
 * @param horasContratadas sus horas semanales contratadas
 * @param formacion lo que suma a la afinidad
 */
public record DocenteActivo(
        String identificacion,
        String nombre,
        Dedicacion dedicacion,
        int horasContratadas,
        FormacionDelDocente formacion) {

    /** Exige los datos. */
    public DocenteActivo {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(dedicacion, "dedicacion");
        Objects.requireNonNull(formacion, "formacion");
    }
}
