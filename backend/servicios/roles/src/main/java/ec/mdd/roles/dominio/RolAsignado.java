package ec.mdd.roles.dominio;

/** El rol está asignado a usuarios y no se puede borrar hasta quitárselo. */
public class RolAsignado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int usuarios;

    /**
     * @param identificador el rol
     * @param usuarios a cuántos usuarios está asignado
     */
    public RolAsignado(String identificador, int usuarios) {
        super("El rol " + identificador + " está asignado a " + usuarios + " usuarios");
        this.usuarios = usuarios;
    }

    /** A cuántos usuarios está asignado. */
    public int usuarios() {
        return usuarios;
    }
}
