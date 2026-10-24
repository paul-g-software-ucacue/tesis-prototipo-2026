package ec.mdd.cargas.puertos.salida;

import ec.mdd.cargas.dominio.DiccionarioDeDatos;

/** De dónde sale el diccionario de datos de las 10 cargas (research §2). */
public interface FuenteDelDiccionario {

    /**
     * Lee y valida el diccionario de datos.
     *
     * @return el diccionario de las 10 cargas
     * @throws IllegalArgumentException si la definición no está completa, con la carga y la columna
     */
    DiccionarioDeDatos leer();
}
