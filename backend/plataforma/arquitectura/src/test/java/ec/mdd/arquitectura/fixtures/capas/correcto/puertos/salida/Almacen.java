package ec.mdd.arquitectura.fixtures.capas.correcto.puertos.salida;

import ec.mdd.arquitectura.fixtures.capas.correcto.dominio.Valor;

/** Puerto de salida de la fixture correcta: depende solo del dominio. */
public interface Almacen {
    void guardar(Valor valor);
}
