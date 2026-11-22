package ec.mdd.reporting.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.reporting.dominio.ArchivoIlegible;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
import ec.mdd.reporting.dominio.LayoutInvalido;
import ec.mdd.reporting.dominio.SinPublicacion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Reporting, con los códigos de {@code contratos/comun/errores.md} (principio XIII,
 * F20-H01). Los fallos entre procesos los traduce {@code ErroresEntreProcesos}. El 500 de un
 * contenido inconsistente no dice qué hash no coincide: eso queda en el log con el identificador
 * del incidente.
 */
@Component
class ErroresReporting implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        SinPublicacion.class,
                        "REPORTING_SIN_PUBLICACION",
                        409,
                        "Sin distributivo publicado",
                        "El periodo todavía no tiene un distributivo publicado. Los reportes salen"
                                + " de la versión publicada."),
                EntradaCatalogo.de(
                        DocenteSinDistributivo.class,
                        "REPORTING_DOCENTE_SIN_DISTRIBUTIVO",
                        404,
                        "Docente sin distributivo",
                        "El docente no tiene asignaciones en la versión publicada de este periodo."),
                // F19-H01 (D-119): el archivo de mapeo del CSV para SIGA.
                EntradaCatalogo.conDetalles(
                        LayoutInvalido.class,
                        new DefinicionDeCodigo(
                                "REPORTING_LAYOUT_INVALIDO",
                                400,
                                "Archivo de mapeo con problemas",
                                "Corrige lo que se indica en cada columna y vuelve a cargar el"
                                        + " archivo. El layout vigente no cambió."),
                        error ->
                                error.problemas().stream()
                                        .map(
                                                uno ->
                                                        new DetalleRespuesta(
                                                                uno.columna(), uno.regla()))
                                        .toList()),
                EntradaCatalogo.conDetalles(
                        ArchivoIlegible.class,
                        new DefinicionDeCodigo(
                                "REPORTING_ARCHIVO_ILEGIBLE",
                                400,
                                "Archivo no válido",
                                "No se pudo leer el archivo de mapeo. Parte de la plantilla y"
                                        + " vuelve a cargarlo."),
                        error ->
                                error.linea().stream()
                                        .mapToObj(
                                                linea ->
                                                        new DetalleRespuesta(
                                                                "archivo",
                                                                "Revisa la línea " + linea + "."))
                                        .toList()),
                EntradaCatalogo.de(
                        ArchivoDemasiadoGrande.class,
                        "REPORTING_ARCHIVO_DEMASIADO_GRANDE",
                        413,
                        "Archivo demasiado grande",
                        "El archivo de mapeo pasa de 256 KB. Parte de la plantilla y vuelve a"
                                + " cargarlo."),
                EntradaCatalogo.de(
                        ContenidoInconsistente.class,
                        "REPORTING_CONTENIDO_INCONSISTENTE",
                        500,
                        "Reporte no disponible",
                        "No se pudo generar el reporte. Avisa al administrador con el"
                                + " identificador del incidente."));
    }
}
