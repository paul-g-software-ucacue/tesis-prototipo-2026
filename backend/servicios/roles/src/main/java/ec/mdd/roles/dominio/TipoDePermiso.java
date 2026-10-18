package ec.mdd.roles.dominio;

/** Lo que un permiso deja hacer sobre una funcionalidad (RF-02, D-16). */
public enum TipoDePermiso {
    /** Ver. */
    LECTURA("lectura"),
    /** Crear o cambiar. */
    ESCRITURA("escritura"),
    /** Aprobar o devolver con observaciones (D-16, columna «Aprobación o devolución»). */
    APROBACION("aprobación"),
    /** Firmar con certificado (F1, F2 y F3, D-19); se guarda y se muestra desde F05-H02. */
    FIRMA("firma");

    private final String nombre;

    TipoDePermiso(String nombre) {
        this.nombre = nombre;
    }

    /** El nombre en español, en minúsculas, para los mensajes. */
    public String nombre() {
        return nombre;
    }
}
