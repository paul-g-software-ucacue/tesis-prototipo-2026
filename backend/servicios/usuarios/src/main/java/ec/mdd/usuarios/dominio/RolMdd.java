package ec.mdd.usuarios.dominio;

/**
 * Los 10 roles del MDD (matriz de roles D-16 y glosario de {@code sdd/modelo_dominio.md}).
 *
 * <p>El identificador es el texto que viaja en el token y en {@code SesionActual} (ADR-014). Qué
 * permite cada rol es de F05-H02; un usuario puede tener varios.
 */
public enum RolMdd {

    /** Administrador del sistema. */
    ADMINISTRADOR,

    /** Talento Humano, que opera los datos de docentes y estructura. */
    OPERADOR_DATOS,

    /** Coordinador de carrera, que arma el distributivo de su carrera. */
    COORDINADOR_CARRERA,

    /** Gestor de investigación o de vinculación de una unidad. */
    GESTOR_UNIDAD,

    /** Responsable de consolidación del periodo (D-13; su designación por periodo es F05-H03). */
    RESPONSABLE_CONSOLIDACION,

    /** Vicerrector académico. */
    VICERRECTOR,

    /** Rector. */
    RECTOR,

    /** Miembro del Órgano Colegiado Superior. */
    MIEMBRO_OCS,

    /** Secretaría general académica. */
    SECRETARIA_GENERAL,

    /** Docente. */
    DOCENTE;

    /**
     * El rol con ese identificador.
     *
     * @throws IllegalArgumentException si el texto no es uno de los 10 identificadores
     */
    public static RolMdd desde(String identificador) {
        for (RolMdd rol : values()) {
            if (rol.name().equals(identificador)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol desconocido: " + identificador);
    }
}
