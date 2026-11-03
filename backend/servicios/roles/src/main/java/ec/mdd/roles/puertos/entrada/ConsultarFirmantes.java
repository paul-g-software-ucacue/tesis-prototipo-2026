package ec.mdd.roles.puertos.entrada;

/** Caso de uso: quién puede firmar en un periodo (RF-58). */
public interface ConsultarFirmantes {

    /**
     * @param periodo el código del periodo
     * @return los roles con usuario y si tiene Responsable designado
     */
    Firmantes firmantes(String periodo);
}
