package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.VencimientoDePlazo;

/**
 * Los vencimientos de plazo registrados (F12-H03, D-114), uno por llegada a la etapa. Solo se
 * insertan (principio VII).
 */
@FunctionalInterface
public interface RepositorioPlazos {

    /**
     * Registra un vencimiento en la transacción en curso, si su llegada no estaba ya registrada.
     *
     * @param vencimiento el vencimiento detectado
     * @return {@code true} si lo registró; {@code false} si esa llegada ya tenía su vencimiento, y
     *     entonces no hay que avisar otra vez
     */
    boolean registrar(VencimientoDePlazo vencimiento);
}
