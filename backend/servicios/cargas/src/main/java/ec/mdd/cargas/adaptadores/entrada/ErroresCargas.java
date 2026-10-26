package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.dominio.DiferenciaDeEncabezado;
import ec.mdd.cargas.dominio.EncabezadosDistintos;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.entrada.CargaConErrores;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.CodificacionNoAdmitida;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.VariasHojas;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.VistaPreviaDesactualizada;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.plataforma.errores.nucleo.RegistroDeErrores;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Los errores de Cargas en el catálogo (ADR-016): los rechazos de un archivo de carga (F01-H02).
 * Sus textos son los de {@code contratos/comun/errores.md}; una prueba lo comprueba. Los detalles
 * dicen la columna o la fila, nunca un valor del archivo (principio IX).
 */
@Component
class ErroresCargas implements CatalogoDeErrores {

    /** El rechazo de una fila cuya excepción no está en el catálogo del proceso. */
    static final String RECHAZO_SIN_CODIGO =
            "Esta fila no cumple una regla de su servicio dueño. Revísala y vuelve a subir el"
                    + " archivo.";

    private final ObjectProvider<RegistroDeErrores> registro;

    /**
     * Recibe el registro de errores del proceso, para traducir el rechazo de un dueño con su propio
     * código (F01-H04). Se pide al traducir, porque el registro se arma con este catálogo.
     */
    @Autowired
    ErroresCargas(ObjectProvider<RegistroDeErrores> registro) {
        this.registro = registro;
    }

    /** Sin el registro del proceso, como en las pruebas del catálogo. */
    ErroresCargas() {
        this(null);
    }

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        FormatoNoAdmitido.class,
                        "CARGAS_FORMATO_NO_ADMITIDO",
                        400,
                        "Formato no admitido",
                        "Solo se admiten archivos CSV y XLSX. Descarga la plantilla de la carga y"
                                + " guárdala en uno de esos formatos."),
                EntradaCatalogo.de(
                        CodificacionNoAdmitida.class,
                        "CARGAS_CODIFICACION_NO_ADMITIDA",
                        400,
                        "Codificación no admitida",
                        "El CSV tiene que estar en UTF-8. Guárdalo como «CSV UTF-8» y vuelve a"
                                + " subirlo."),
                EntradaCatalogo.de(
                        VariasHojas.class,
                        "CARGAS_VARIAS_HOJAS",
                        400,
                        "Varias hojas",
                        "El archivo XLSX tiene que tener una sola hoja, con los encabezados en la"
                                + " primera fila."),
                EntradaCatalogo.conDetalles(
                        ArchivoIlegible.class,
                        new DefinicionDeCodigo(
                                "CARGAS_ARCHIVO_ILEGIBLE",
                                400,
                                "Archivo ilegible",
                                "El archivo no se puede leer. Revisa el detalle, corrígelo y vuelve"
                                        + " a subirlo."),
                        error -> List.of(new DetalleRespuesta("archivo", error.motivo()))),
                EntradaCatalogo.conDetalles(
                        EncabezadosDistintos.class,
                        new DefinicionDeCodigo(
                                "CARGAS_ENCABEZADOS_DISTINTOS",
                                400,
                                "Encabezados distintos de la plantilla",
                                "Las columnas del archivo no son las de la plantilla de la carga,"
                                        + " en su orden. Revisa las columnas marcadas."),
                        error -> error.diferencias().stream().map(ErroresCargas::detalle).toList()),
                EntradaCatalogo.de(
                        ArchivoSinRegistros.class,
                        "CARGAS_ARCHIVO_SIN_REGISTROS",
                        400,
                        "Archivo sin registros",
                        "El archivo no tiene registros debajo de los encabezados."),
                EntradaCatalogo.de(
                        ArchivoDemasiadoGrande.class,
                        "CARGAS_ARCHIVO_DEMASIADO_GRANDE",
                        413,
                        "Archivo demasiado grande",
                        "El archivo pasa de 5 MB, el tamaño máximo de una carga."),
                EntradaCatalogo.conDetalles(
                        CargaConErrores.class,
                        new DefinicionDeCodigo(
                                "CARGAS_CARGA_CON_ERRORES",
                                409,
                                "Carga con errores",
                                "El archivo tiene filas con errores y no se puede confirmar."
                                        + " Descarga el informe, corrige esas filas y vuelve a"
                                        + " subirlo."),
                        error ->
                                List.of(
                                        new DetalleRespuesta(
                                                "archivo",
                                                error.filas() == 1
                                                        ? "1 fila tiene errores."
                                                        : error.filas()
                                                                + " filas tienen errores."))),
                EntradaCatalogo.de(
                        VistaPreviaDesactualizada.class,
                        "CARGAS_VISTA_PREVIA_DESACTUALIZADA",
                        409,
                        "Vista previa desactualizada",
                        "Los datos cambiaron desde que revisaste la vista previa. Vuelve a subir el"
                                + " archivo y revisa la vista previa antes de confirmar."),
                EntradaCatalogo.conDetalles(
                        RechazoDelLote.class,
                        new DefinicionDeCodigo(
                                "CARGAS_CARGA_RECHAZADA",
                                409,
                                "Carga rechazada",
                                "El servicio dueño rechazó la carga y no se aplicó ninguna fila."
                                        + " Revisa el detalle, corrige el archivo y vuelve a"
                                        + " subirlo."),
                        this::detalles));
    }

    /**
     * Los detalles de un rechazo: la fila y, con el catálogo del dueño, el campo y su descripción,
     * o su título y su descripción. Nunca la traza ni el mensaje técnico de la excepción.
     */
    List<DetalleRespuesta> detalles(RechazoDelLote rechazo) {
        String fila = "fila " + rechazo.fila();
        if (rechazo.campo().isPresent()) {
            return List.of(
                    new DetalleRespuesta(
                            fila + " · " + rechazo.campo().get(), rechazo.getMessage()));
        }
        Throwable causa = rechazo.getCause();
        if (causa == null) {
            return List.of(new DetalleRespuesta(fila, rechazo.getMessage()));
        }
        Optional<EntradaCatalogo> entrada =
                Optional.ofNullable(registro)
                        .map(ObjectProvider::getIfAvailable)
                        .flatMap(delProceso -> delProceso.buscarEntrada(causa.getClass()));
        if (entrada.isEmpty()) {
            return List.of(new DetalleRespuesta(fila, RECHAZO_SIN_CODIGO));
        }
        List<DetalleRespuesta> delDueno = entrada.get().detalles(causa);
        if (delDueno.isEmpty()) {
            DefinicionDeCodigo definicion = entrada.get().definicion();
            return List.of(
                    new DetalleRespuesta(
                            fila, definicion.titulo() + ": " + definicion.descripcion()));
        }
        return delDueno.stream()
                .map(
                        detalle ->
                                new DetalleRespuesta(
                                        fila + " · " + detalle.campo(), detalle.descripcion()))
                .toList();
    }

    private static DetalleRespuesta detalle(DiferenciaDeEncabezado diferencia) {
        String descripcion =
                switch (diferencia.motivo()) {
                    case FALTA -> "Falta en el archivo.";
                    case SOBRA -> "No está en la plantilla.";
                    case REPETIDA -> "Está repetida.";
                    case FUERA_DE_LUGAR ->
                            "Está fuera de lugar: va en la posición " + diferencia.posicion() + ".";
                };
        return new DetalleRespuesta(diferencia.columna(), descripcion);
    }
}
