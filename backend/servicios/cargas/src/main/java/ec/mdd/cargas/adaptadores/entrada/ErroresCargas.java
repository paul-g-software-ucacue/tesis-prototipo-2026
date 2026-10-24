package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.dominio.DiferenciaDeEncabezado;
import ec.mdd.cargas.dominio.EncabezadosDistintos;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.CodificacionNoAdmitida;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import ec.mdd.cargas.puertos.salida.VariasHojas;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Cargas en el catálogo (ADR-016): los rechazos de un archivo de carga (F01-H02).
 * Sus textos son los de {@code contratos/comun/errores.md}; una prueba lo comprueba. Los detalles
 * dicen la columna o la fila, nunca un valor del archivo (principio IX).
 */
@Component
class ErroresCargas implements CatalogoDeErrores {

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
                        "El archivo pasa de 5 MB, el tamaño máximo de una carga."));
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
