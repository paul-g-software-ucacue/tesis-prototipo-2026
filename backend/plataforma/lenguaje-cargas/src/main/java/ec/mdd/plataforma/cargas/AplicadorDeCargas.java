package ec.mdd.plataforma.cargas;

import java.util.Set;

/**
 * Lo que Cargas ve de un servicio dueño (ADR-022, MD-13): las cargas que atiende, la vista previa
 * de un lote y su aplicación completa o nada en la transacción del dueño.
 */
public interface AplicadorDeCargas {

    /** Los códigos de las cargas que atiende. */
    Set<String> cargas();

    /**
     * Si el dueño tiene al menos un registro vigente de la carga (F01-H05).
     *
     * @throws IllegalArgumentException si no atiende esa carga
     */
    boolean tieneDatos(String carga);

    /**
     * Si el dueño tiene al menos un registro vigente de la carga en ese periodo (F02-H02).
     *
     * @throws IllegalArgumentException si no atiende esa carga
     */
    boolean tieneDatosEn(String carga, String periodo);

    /** La vista previa del lote frente a lo que tiene el dueño. */
    VistaPreviaDelLote previsualizar(LoteDeCarga lote);

    /**
     * Aplica el lote en una sola transacción, si su vista previa es la revisada.
     *
     * @throws VistaPreviaDesactualizada si la huella cambió
     * @throws RechazoDelLote si una regla del dueño falla en una fila
     */
    ResultadoDelLote aplicar(LoteDeCarga lote, String huella, String quien);
}
