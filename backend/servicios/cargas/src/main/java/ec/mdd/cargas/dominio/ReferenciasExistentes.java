package ec.mdd.cargas.dominio;

import java.util.Set;

/**
 * Lo que existe en el sistema al validar un archivo: los códigos de las unidades, las carreras, los
 * periodos y las asignaturas, y las identificaciones de los docentes (aclaración 2). Se piden una
 * vez por archivo.
 */
public record ReferenciasExistentes(
        Set<String> unidades,
        Set<String> carreras,
        Set<String> periodos,
        Set<String> asignaturas,
        Set<String> docentes) {

    /** Copia los conjuntos. */
    public ReferenciasExistentes {
        unidades = Set.copyOf(unidades);
        carreras = Set.copyOf(carreras);
        periodos = Set.copyOf(periodos);
        asignaturas = Set.copyOf(asignaturas);
        docentes = Set.copyOf(docentes);
    }

    /** Si existe ese código de ese tipo. */
    public boolean existe(TipoDeReferencia tipo, String codigo) {
        return switch (tipo) {
            case UNIDAD -> unidades.contains(codigo);
            case CARRERA -> carreras.contains(codigo);
            case PERIODO -> periodos.contains(codigo);
            case ASIGNATURA -> asignaturas.contains(codigo);
            case DOCENTE -> docentes.contains(codigo);
        };
    }
}
