package ec.mdd.plataforma.errores.fixtures.dominio;

import java.util.List;

/** Error de prueba con detalles: las columnas que faltan en un archivo. */
public class ColumnasDistintas extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> faltan;

    public ColumnasDistintas(List<String> faltan) {
        super("Faltan columnas");
        this.faltan = List.copyOf(faltan);
    }

    public List<String> faltan() {
        return faltan;
    }
}
