package ec.mdd.docentes.dominio;

/**
 * Un cargo solo se da a un docente activo a tiempo completo (POL 5.4, diccionario de la carga 8).
 * El mensaje no repite la identificación (principio IX).
 */
public class DocenteNoHabilitado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sin datos del docente. */
    public DocenteNoHabilitado() {
        super("El docente no está activo a tiempo completo");
    }
}
