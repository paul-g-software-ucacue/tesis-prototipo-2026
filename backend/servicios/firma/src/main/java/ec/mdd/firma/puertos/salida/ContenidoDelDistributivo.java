package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.dominio.ContenidoDelDocumento;

/**
 * El contenido de la versión congelada, que es de Generación (F13-H02). Dentro del proceso lo da su
 * interfaz publicada; Firma no lee sus tablas (principio V). Puede llamar a Datos (D-83): no se usa
 * dentro de una transacción.
 */
@FunctionalInterface
public interface ContenidoDelDistributivo {

    /**
     * @param periodo el código del periodo
     * @return el contenido sin los firmantes, que agrega Firma
     */
    ContenidoDelDocumento contenido(String periodo);
}
