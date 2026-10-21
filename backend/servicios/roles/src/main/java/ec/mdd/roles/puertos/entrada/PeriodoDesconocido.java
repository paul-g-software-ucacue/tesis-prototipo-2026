package ec.mdd.roles.puertos.entrada;

/**
 * Roles todavía no conoce el periodo: no existe, o su aviso de Académico aún no llegó (F05-H03,
 * research §1). Está junto a {@link DesignarResponsable}, que la lanza.
 */
public class PeriodoDesconocido extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param periodo el código del periodo
     */
    public PeriodoDesconocido(String periodo) {
        super("Roles no conoce el periodo " + periodo);
    }
}
