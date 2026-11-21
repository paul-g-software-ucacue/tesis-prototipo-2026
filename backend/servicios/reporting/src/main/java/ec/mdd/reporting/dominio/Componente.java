package ec.mdd.reporting.dominio;

/** Dónde cuentan las horas de un docente (RF-27), en el orden del glosario y del reporte. */
public enum Componente {
    /** Horas de clase: las asignaturas-paralelo. */
    CLASE("Clase"),
    /** Otras actividades de docencia (art. 140 del RCE). */
    OTRAS_DOCENCIA("Otras actividades de docencia"),
    /** Investigación (art. 141 del RCE). */
    INVESTIGACION("Investigación"),
    /** Vinculación (art. 142 del RCE). */
    VINCULACION("Vinculación"),
    /** Gestión (art. 143 del RCE). */
    GESTION("Gestión");

    private final String etiqueta;

    Componente(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** El nombre que ve el usuario en el PDF, en el CSV y en la pantalla. */
    public String etiqueta() {
        return etiqueta;
    }
}
