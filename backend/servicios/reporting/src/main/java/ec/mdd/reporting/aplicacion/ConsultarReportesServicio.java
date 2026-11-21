package ec.mdd.reporting.aplicacion;

import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.ContenidoInconsistente;
import ec.mdd.reporting.dominio.DocenteSinDistributivo;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.ReporteIndividual;
import ec.mdd.reporting.dominio.SinPublicacion;
import ec.mdd.reporting.puertos.entrada.ConsultarReportes;
import ec.mdd.reporting.puertos.salida.ContenidoDeLaVersion;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;

/**
 * Los reportes de la versión publicada vigente (F20-H01, D-115). Aprobación dice cuál versión es la
 * publicada y Generación da su contenido congelado; Reporting solo los junta y comprueba que sean
 * de la misma versión, por su hash.
 */
public final class ConsultarReportesServicio implements ConsultarReportes {

    private final PublicacionesDelDistributivo publicaciones;
    private final ContenidoDeLaVersion contenido;
    private final Clock reloj;

    /**
     * @param publicaciones las publicaciones vigentes, de Aprobación
     * @param contenido el contenido congelado, de Generación
     * @param reloj cuándo se genera cada reporte
     */
    public ConsultarReportesServicio(
            PublicacionesDelDistributivo publicaciones,
            ContenidoDeLaVersion contenido,
            Clock reloj) {
        this.publicaciones = publicaciones;
        this.contenido = contenido;
        this.reloj = reloj;
    }

    @Override
    public List<Publicacion> publicaciones() {
        return publicaciones.vigentes().stream()
                .sorted(
                        Comparator.comparing(Publicacion::publicadaEn)
                                .reversed()
                                .thenComparing(Publicacion::periodo))
                .toList();
    }

    @Override
    public DocentesDelPeriodo docentes(String periodo) {
        Publicacion publicacion = publicacion(periodo);
        DocentesDeLaVersion docentes = docentesDe(publicacion);
        return new DocentesDelPeriodo(publicacion, docentes.docentes());
    }

    @Override
    public ReporteIndividual reporte(String periodo, String identificacion, String quien) {
        Publicacion publicacion = publicacion(periodo);
        ContenidoDelDocente delDocente =
                contenido
                        .contenido(periodo, publicacion.version(), identificacion)
                        .orElseThrow(
                                () -> {
                                    // Solo en el camino del error: distingue un docente que no
                                    // está de una versión sin contenido.
                                    docentesDe(publicacion);
                                    return new DocenteSinDistributivo(
                                            periodo, publicacion.version());
                                });
        return ReporteIndividual.de(publicacion, delDocente, quien, reloj.instant());
    }

    /**
     * La publicación vigente del periodo.
     *
     * @throws SinPublicacion si no tiene
     */
    Publicacion publicacion(String periodo) {
        return publicaciones.vigentes().stream()
                .filter(una -> una.periodo().equals(periodo))
                .findFirst()
                .orElseThrow(() -> new SinPublicacion(periodo));
    }

    private DocentesDeLaVersion docentesDe(Publicacion publicacion) {
        DocentesDeLaVersion docentes =
                contenido
                        .docentes(publicacion.periodo(), publicacion.version())
                        .orElseThrow(
                                () ->
                                        new ContenidoInconsistente(
                                                "Generación no tiene la versión publicada "
                                                        + publicacion.version()
                                                        + " del periodo "
                                                        + publicacion.periodo()
                                                        + " congelada."));
        if (!docentes.hash().equals(publicacion.hash())) {
            throw new ContenidoInconsistente(
                    "El hash de la publicación "
                            + publicacion.hash()
                            + " no es el del contenido "
                            + docentes.hash()
                            + ".");
        }
        return docentes;
    }
}
