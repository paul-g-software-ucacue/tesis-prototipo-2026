package ec.mdd.cargas.dominio;

import java.util.List;

/** Los encabezados del archivo no son los de la plantilla de su carga, en su orden (RF-07). */
public class EncabezadosDistintos extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<DiferenciaDeEncabezado> diferencias;

    /**
     * @param diferencias cada columna que no coincide; al menos una
     */
    public EncabezadosDistintos(List<DiferenciaDeEncabezado> diferencias) {
        super("Los encabezados no son los de la plantilla: " + diferencias.size() + " diferencias");
        this.diferencias = List.copyOf(diferencias);
    }

    /** Cada columna que no coincide, con su motivo. */
    public List<DiferenciaDeEncabezado> diferencias() {
        return diferencias;
    }
}
