package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.InformeValidacionCarga;
import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.dominio.VistaPrevia;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.ValidarArchivo;
import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validar el archivo de una carga: busca la carga, comprueba el tamaño, lo lee con la fuente de
 * datos y deja que la carga valide su estructura (F01-H02). Después pide una vez lo que existe en
 * el sistema y revisa cada fila con el validador de la carga (F01-H03). Si las filas no tienen
 * errores, pide a su servicio dueño la vista previa (F01-H04, MD-13). No guarda nada ni registra el
 * contenido (principio IX, FR-008): cada envío se valida desde cero.
 */
public final class ValidarArchivoServicio implements ValidarArchivo {

    /** El tamaño máximo de un archivo de carga: 5 MB (research §3 de F01-H02). */
    public static final int MAXIMO = 5 * 1024 * 1024;

    private final ConsultarPlantillas plantillas;
    private final LectorDeArchivo lector;
    private final ReferenciasDelSistema referencias;
    private final DuenosDeLasCargas duenos;

    /**
     * Recibe las cargas del diccionario de datos, la fuente de datos de los archivos, lo que existe
     * en el sistema y el dueño de cada carga.
     */
    public ValidarArchivoServicio(
            ConsultarPlantillas plantillas,
            LectorDeArchivo lector,
            ReferenciasDelSistema referencias,
            DuenosDeLasCargas duenos) {
        this.plantillas = plantillas;
        this.lector = lector;
        this.referencias = referencias;
        this.duenos = duenos;
    }

    @Override
    public ArchivoValidado validar(String codigo, ArchivoSubido archivo) {
        Revision revision = revisar(codigo, archivo);
        InformeValidacionCarga informe = revision.informe();
        VistaPrevia vista =
                informe.valido()
                        ? new VistaPrevia(
                                0,
                                Optional.of(
                                        duenos.de(revision.carga().codigo())
                                                .previsualizar(revision.lote())))
                        : new VistaPrevia(informe.filasConErrores(), Optional.empty());
        return new ArchivoValidado(
                revision.carga().codigo(),
                archivo.nombre(),
                revision.leido().formato(),
                informe,
                vista);
    }

    /**
     * Lee el archivo, valida su estructura y revisa sus filas, sin pedir la vista previa.
     *
     * @throws ec.mdd.cargas.puertos.entrada.CargaNoEncontrada si no es una de las 10
     */
    Revision revisar(String codigo, ArchivoSubido archivo) {
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
        String hash = MotorDelLote.sha256(archivo.contenido());
        return new Revision(
                carga, leido, informe, new LoteDeCarga(carga.codigo(), hash, filas(leido.tabla())));
    }

    /** Las filas con datos, con sus valores por campo y sin espacios: el lote para el dueño. */
    private static List<FilaDelLote> filas(TablaDeDatos tabla) {
        List<String> campos = tabla.encabezados().stream().map(String::strip).toList();
        List<FilaDelLote> filas = new ArrayList<>();
        for (int i = 0; i < tabla.filas().size(); i++) {
            List<String> celdas = tabla.filas().get(i);
            if (celdas.stream().allMatch(String::isBlank)) {
                continue;
            }
            Map<String, String> valores = new HashMap<>();
            for (int c = 0; c < campos.size(); c++) {
                valores.put(campos.get(c), celdas.get(c).strip());
            }
            filas.add(new FilaDelLote(i + 2, valores));
        }
        return filas;
    }

    /**
     * Un archivo leído y revisado.
     *
     * @param carga su carga
     * @param leido su formato y su tabla
     * @param informe los problemas de sus filas
     * @param lote sus filas con datos y el hash del archivo
     */
    record Revision(
            Carga carga, ArchivoLeido leido, InformeValidacionCarga informe, LoteDeCarga lote) {}
}
