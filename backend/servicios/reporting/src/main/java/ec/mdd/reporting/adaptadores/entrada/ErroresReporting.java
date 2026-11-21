package ec.mdd.reporting.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
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
                EntradaCatalogo.de(
                        ContenidoInconsistente.class,
                        "REPORTING_CONTENIDO_INCONSISTENTE",
                        500,
                        "Reporte no disponible",
                        "No se pudo generar el reporte. Avisa al administrador con el"
                                + " identificador del incidente."));
    }
}
