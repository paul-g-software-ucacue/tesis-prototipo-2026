package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.InformeValidacionCarga;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;

/**
 * Validar el archivo de una carga: busca la carga, comprueba el tamaño, lo lee con la fuente de
 * datos y deja que la carga valide su estructura (F01-H02). Después pide una vez lo que existe en
 * el sistema y revisa cada fila con el validador de la carga (F01-H03). No guarda nada ni registra
 * el contenido (principio IX, FR-008): cada envío se valida desde cero.
 */
public final class ValidarArchivoServicio implements ValidarArchivo {

    /** El tamaño máximo de un archivo de carga: 5 MB (research §3). */
    public static final int MAXIMO = 5 * 1024 * 1024;

    private final ConsultarPlantillas plantillas;
    private final LectorDeArchivo lector;
    private final ReferenciasDelSistema referencias;

    /**
     * Recibe las cargas del diccionario de datos, la fuente de datos de los archivos y lo que
     * existe en el sistema.
     */
    public ValidarArchivoServicio(
            ConsultarPlantillas plantillas,
            LectorDeArchivo lector,
            ReferenciasDelSistema referencias) {
        this.plantillas = plantillas;
        this.lector = lector;
        this.referencias = referencias;
    }

    @Override
    public ArchivoValidado validar(String codigo, ArchivoSubido archivo) {
        Carga carga = plantillas.carga(codigo);
        if (archivo.contenido().length > MAXIMO) {
            throw new ArchivoDemasiadoGrande(archivo.contenido().length);
        }
        ArchivoLeido leido = lector.leer(archivo.nombre(), archivo.contenido());
        carga.leer(leido.tabla());
        InformeValidacionCarga informe =
                plantillas
                        .diccionario()
                        .validador(carga)
                        .validar(leido.tabla(), referencias.existentes());
        return new ArchivoValidado(carga.codigo(), archivo.nombre(), leido.formato(), informe);
    }
}
