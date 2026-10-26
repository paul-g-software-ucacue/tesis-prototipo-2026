package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.dominio.EncabezadosDistintos;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.CodificacionNoAdmitida;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.VariasHojas;

/**
 * Caso de uso: subir el archivo de una carga, validar su estructura (RF-06, RF-07, F01-H02) y
 * revisar sus filas (RF-08, RF-09, F01-H03). Refleja las operaciones {@code validarArchivo} y
 * {@code descargarInforme} del contrato de Cargas. El archivo no se guarda.
 */
public interface ValidarArchivo {

    /**
     * Lee el archivo, valida su estructura y revisa sus filas. Un archivo con errores en sus filas
     * no lanza: su informe los trae (principio XIII).
     *
     * @param codigo el código de la carga
     * @param archivo el archivo subido
     * @return la carga, el nombre, el formato y el informe de sus filas
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
