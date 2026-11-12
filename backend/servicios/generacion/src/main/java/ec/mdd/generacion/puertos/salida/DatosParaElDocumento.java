package ec.mdd.generacion.puertos.salida;

import java.util.Map;

/** Lo que el contenido del documento firmable necesita de Datos (F13-H02, ADR-023). */
public interface DatosParaElDocumento {

    /**
     * @return el nombre de cada carrera, por su código
     */
    Map<String, String> nombresDeCarreras();
}
