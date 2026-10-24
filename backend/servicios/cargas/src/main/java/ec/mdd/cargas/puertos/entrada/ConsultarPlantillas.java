package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.DiccionarioDeDatos;

/**
 * Caso de uso: consultar el diccionario de datos y la carga de cada plantilla (RF-05, F01-H01).
 * Refleja las operaciones {@code listarCargas}, {@code descargarPlantilla} y {@code
 * descargarDiccionario} del contrato de Cargas.
 */
public interface ConsultarPlantillas {

    /** El diccionario de datos de las 10 cargas. */
    DiccionarioDeDatos diccionario();

    /**
     * La carga con ese código, de la que salen su plantilla y su diccionario.
     *
     * @throws CargaNoEncontrada si no es una de las 10
     */
    Carga carga(String codigo);
}
