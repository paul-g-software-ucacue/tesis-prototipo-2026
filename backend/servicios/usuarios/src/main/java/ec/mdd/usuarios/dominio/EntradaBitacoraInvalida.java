package ec.mdd.usuarios.dominio;

/**
 * Una entrada de la bitácora que no cumple el lenguaje publicado {@code EntradaBitacora} o sus
 * invariantes. Se traduce a {@code USUARIOS_ENTRADA_BITACORA_INVALIDA} (400): no se registra nada y
 * el emisor deja la publicación pendiente.
 *
 * <p>El mensaje dice qué regla falló y en qué campo; nunca un valor del contenido.
 */
public final class EntradaBitacoraInvalida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Error con un motivo que no cita valores del contenido. */
    public EntradaBitacoraInvalida(String motivo) {
        super(motivo);
    }
}
