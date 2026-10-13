package ec.mdd.plataforma.errores.fixtures.dominio;

/** Error de dominio de prueba (D-41): Java puro, sin HTTP. */
public class HorasExcedidas extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HorasExcedidas() {
        super("El docente supera sus horas");
    }
}
