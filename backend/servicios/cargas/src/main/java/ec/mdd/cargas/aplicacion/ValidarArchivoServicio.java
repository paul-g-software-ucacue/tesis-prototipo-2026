package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;

/**
 * Validar el archivo de una carga: busca la carga, comprueba el tamaño, lo lee con la fuente de
 * datos y deja que la carga valide los encabezados y cuente los registros. No guarda nada ni
 * registra el contenido (principio IX).
 */
public final class ValidarArchivoServicio implements ValidarArchivo {

    /** El tamaño máximo de un archivo de carga: 5 MB (research §3). */
    public static final int MAXIMO = 5 * 1024 * 1024;

    private final ConsultarPlantillas plantillas;
    private final LectorDeArchivo lector;

    /** Recibe las cargas del diccionario de datos y la fuente de datos de los archivos. */
    public ValidarArchivoServicio(ConsultarPlantillas plantillas, LectorDeArchivo lector) {
        this.plantillas = plantillas;
        this.lector = lector;
    }

    @Override
    public ArchivoValidado validar(String codigo, ArchivoSubido archivo) {
        Carga carga = plantillas.carga(codigo);
        if (archivo.contenido().length > MAXIMO) {
            throw new ArchivoDemasiadoGrande(archivo.contenido().length);
        }
        ArchivoLeido leido = lector.leer(archivo.nombre(), archivo.contenido());
        int registros = carga.leer(leido.tabla());
        return new ArchivoValidado(carga.codigo(), archivo.nombre(), leido.formato(), registros);
    }
}
