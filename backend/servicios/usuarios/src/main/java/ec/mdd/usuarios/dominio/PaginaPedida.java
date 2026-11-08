package ec.mdd.usuarios.dominio;

import java.time.Instant;

/**
 * La página pedida de una consulta de la bitácora (FR-004, research §2): su número, desde 1; su
 * tamaño, de 1 a 100; y el corte, el instante hasta el que cuentan las entradas recibidas, para que
 * las páginas siguientes no cambien si llega una entrada tarde. No se guarda.
 *
 * @param pagina desde 1
 * @param tamano de 1 a 100
 * @param corte no posterior al instante de la consulta
 */
public record PaginaPedida(int pagina, int tamano, Instant corte) {

    /** La primera página. */
    public static final int PAGINA_POR_DEFECTO = 1;

    /** Entradas por página si no se pide otra cantidad. */
    public static final int TAMANO_POR_DEFECTO = 50;

    /** Nunca se devuelven más entradas de una vez. */
    public static final int TAMANO_MAXIMO = 100;

    /**
     * Valida la página y completa los valores por defecto.
     *
     * @param pagina el número pedido; {@code null} es la primera
     * @param tamano las entradas por página; {@code null} es {@value #TAMANO_POR_DEFECTO}
     * @param corte el corte de la primera respuesta; {@code null} es {@code ahora}
     * @param ahora el instante de la consulta
     * @return la página válida
     * @throws FiltroDeBitacoraInvalido si un campo no cumple su regla
     */
    public static PaginaPedida de(Integer pagina, Integer tamano, Instant corte, Instant ahora) {
        int numero = pagina == null ? PAGINA_POR_DEFECTO : pagina;
        int cuantas = tamano == null ? TAMANO_POR_DEFECTO : tamano;
        if (numero < 1) {
            throw new FiltroDeBitacoraInvalido("pagina", "La página empieza en 1.");
        }
        if (cuantas < 1 || cuantas > TAMANO_MAXIMO) {
            throw new FiltroDeBitacoraInvalido(
                    "tamano", "El tamaño de la página va de 1 a 100 entradas.");
        }
        if (corte != null && corte.isAfter(ahora)) {
            throw new FiltroDeBitacoraInvalido(
                    "corte", "El corte no puede ser posterior al momento de la consulta.");
        }
        return new PaginaPedida(numero, cuantas, corte == null ? ahora : corte);
    }

    /** Cuántas entradas se saltan antes de esta página. */
    public long desplazamiento() {
        return (long) (pagina - 1) * tamano;
    }
}
