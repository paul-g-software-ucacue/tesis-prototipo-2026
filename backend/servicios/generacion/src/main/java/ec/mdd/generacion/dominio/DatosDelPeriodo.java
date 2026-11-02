package ec.mdd.generacion.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Lo que el motor evalúa al mirar la versión completa (RF-29).
 *
 * @param periodo el código del periodo
 * @param version la versión vigente del distributivo
 * @param oferta las asignaturas-paralelo del periodo, también las retiradas
 * @param docentes los docentes activos
 * @param cargos los cargos vigentes
 */
public record DatosDelPeriodo(
        String periodo,
        VersionDistributivo version,
        List<AsignaturaParalelo> oferta,
        List<DocenteActivo> docentes,
        List<CargoVigente> cargos) {

    /** Exige los datos y copia las listas. */
    public DatosDelPeriodo {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(version, "version");
        oferta = List.copyOf(oferta);
        docentes = List.copyOf(docentes);
        cargos = List.copyOf(cargos);
    }
}
