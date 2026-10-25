package ec.mdd.academico.puertos.entrada;

import java.util.List;

/**
 * Las referencias de Académico para Docentes (F03-H04 y F03-H05), por código.
 *
 * @param carreras todas las carreras, en cualquier estado: un docente puede pertenecer a una
 *     carrera en cierre
 * @param asignaturas todas las asignaturas, activas e inactivas
 * @param camposAmplios los 11 campos amplios de la CINE
 * @param unidades todas las unidades de la estructura orgánica, activas e inactivas (F03-H05)
 * @param periodos todos los periodos académicos, en cualquier estado (F03-H05)
 */
public record ReferenciasDeAcademico(
        List<CarreraDeReferencia> carreras,
        List<AsignaturaDeReferencia> asignaturas,
        List<CampoAmplioDeReferencia> camposAmplios,
        List<UnidadDeReferencia> unidades,
        List<PeriodoDeReferencia> periodos) {

    /** Copia las listas. */
    public ReferenciasDeAcademico {
        carreras = List.copyOf(carreras);
        asignaturas = List.copyOf(asignaturas);
        camposAmplios = List.copyOf(camposAmplios);
        unidades = List.copyOf(unidades);
        periodos = List.copyOf(periodos);
    }
}
