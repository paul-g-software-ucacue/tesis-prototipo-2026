package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.dominio.EncabezadosDistintos;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.CodificacionNoAdmitida;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.VariasHojas;

/**
 * Caso de uso: subir el archivo de una carga y validar su estructura (RF-06, RF-07, F01-H02).
 * Refleja la operación {@code validarArchivo} del contrato de Cargas. El archivo no se guarda.
 */
public interface ValidarArchivo {

    /**
     * Lee el archivo y valida su estructura.
     *
     * @param codigo el código de la carga
     * @param archivo el archivo subido
     * @return la carga, el nombre, el formato y el número de registros
     * @throws CargaNoEncontrada si no es una de las 10
     * @throws ArchivoDemasiadoGrande si pasa de 5 MB
     * @throws FormatoNoAdmitido si no es CSV ni XLSX
     * @throws CodificacionNoAdmitida si es un CSV que no está en UTF-8
     * @throws VariasHojas si es un XLSX con más de una hoja
     * @throws ArchivoIlegible si no se puede leer
     * @throws EncabezadosDistintos si sus encabezados no son los de la plantilla
     * @throws ArchivoSinRegistros si no tiene registros
     */
    ArchivoValidado validar(String codigo, ArchivoSubido archivo);
}
