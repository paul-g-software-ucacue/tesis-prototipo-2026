package ec.mdd.cargas.dominio;

/** A qué apunta una referencia ({@code FK}) del diccionario de datos (research §6). */
public enum TipoDeReferencia {
    /** Una unidad de la estructura orgánica, de Académico. */
    UNIDAD("la unidad"),
    /** Una carrera, de Académico. */
    CARRERA("la carrera"),
    /** Un periodo académico, de Académico. */
    PERIODO("el periodo"),
    /** Una asignatura, de Académico. */
    ASIGNATURA("la asignatura"),
    /** Un docente, por su identificación, de Docentes. */
    DOCENTE("el docente");

    private final String nombre;

    TipoDeReferencia(String nombre) {
        this.nombre = nombre;
    }

    /** Cómo se nombra en un motivo, con su artículo: «la carrera». */
    public String nombre() {
        return nombre;
    }
}
