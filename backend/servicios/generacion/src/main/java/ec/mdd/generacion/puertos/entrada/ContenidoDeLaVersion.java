package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.CargaHoraria;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * El contenido de la versión congelada, para el documento firmable (F13-H02, D-101): lo mismo que
 * muestra el consolidado y la carga horaria de cada docente, sin datos de contacto ni de contrato
 * (principio IX).
 *
 * @param periodo el código del periodo
 * @param version el número de la versión congelada
 * @param hash su hash de contenido, el de {@link ParcialesDelDistributivo#hashDelDistributivo}
 * @param carreras el resumen por carrera, por código
 * @param totales los totales institucionales por componente
 * @param docentes cada docente con asignaciones, por nombre
 */
public record ContenidoDeLaVersion(
        String periodo,
        int version,
        String hash,
        List<Carrera> carreras,
        CargaHoraria totales,
        List<Docente> docentes) {

    /** Exige los datos y copia las listas. */
    public ContenidoDeLaVersion {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(hash, "hash");
        Objects.requireNonNull(totales, "totales");
        carreras = List.copyOf(carreras);
        docentes = List.copyOf(docentes);
    }

    /**
     * Una carrera del consolidado.
     *
     * @param codigo su código
     * @param nombre su nombre; el código si Académico no lo tiene
     * @param estado el estado de su parcial
     * @param paralelos las asignaturas-paralelo ofertadas
     * @param propuestos las que tienen propuesta
     * @param horas sus horas por componente
     */
    public record Carrera(
            String codigo,
            String nombre,
            String estado,
            int paralelos,
            int propuestos,
            CargaHoraria horas) {}

    /**
     * Un docente con sus asignaciones de la versión.
     *
     * @param identificacion su número de identificación
     * @param nombre sus apellidos y nombres, como se guardaron con la asignación
     * @param asignaturas sus asignaturas-paralelo, por código y paralelo
     * @param actividades sus actividades no lectivas, por nombre
     * @param horas sus horas por componente
     */
    public record Docente(
            String identificacion,
            String nombre,
            List<Asignatura> asignaturas,
            List<Actividad> actividades,
            CargaHoraria horas) {

        /** Copia las listas. */
        public Docente {
            asignaturas = List.copyOf(asignaturas);
            actividades = List.copyOf(actividades);
        }
    }

    /**
     * Una asignatura-paralelo de un docente.
     *
     * @param codigo el código de la asignatura
     * @param nombre su nombre
     * @param paralelo el paralelo
     * @param carrera el código de su carrera
     * @param horas sus horas semanales de clase
     */
    public record Asignatura(
            String codigo, String nombre, String paralelo, String carrera, BigDecimal horas) {}

    /**
     * Una actividad no lectiva de un docente.
     *
     * @param nombre el nombre de la actividad
     * @param componente su componente
     * @param horas sus horas semanales
     */
    public record Actividad(String nombre, String componente, BigDecimal horas) {}
}
