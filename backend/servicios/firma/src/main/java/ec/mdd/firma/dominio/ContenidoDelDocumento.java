package ec.mdd.firma.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Lo que se imprime en el documento firmable, inmutable (F13-H02, FR-003, FR-004, FR-006, FR-007):
 * el encabezado con el periodo, el resumen por carrera, los totales, el detalle por docente, el pie
 * con la versión y el hash, y el bloque de firmas.
 *
 * @param periodo el código del periodo
 * @param version la versión congelada
 * @param hashContenido su hash de contenido, el del pie (D-100)
 * @param carreras el resumen por carrera
 * @param totales los totales institucionales
 * @param docentes cada docente con sus asignaciones
 * @param firmantes el bloque de firmas, en orden
 */
public record ContenidoDelDocumento(
        String periodo,
        int version,
        String hashContenido,
        List<Carrera> carreras,
        Horas totales,
        List<Docente> docentes,
        List<Firmante> firmantes) {

    /** Exige los datos y copia las listas. */
    public ContenidoDelDocumento {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(hashContenido, "hashContenido");
        Objects.requireNonNull(totales, "totales");
        carreras = List.copyOf(carreras);
        docentes = List.copyOf(docentes);
        firmantes = List.copyOf(firmantes);
    }

    /**
     * Las horas semanales por componente (D-82).
     *
     * @param clase de clase
     * @param otrasDocencia de otras actividades de docencia
     * @param investigacion de investigación
     * @param vinculacion de vinculación
     * @param gestion de gestión
     */
    public record Horas(
            BigDecimal clase,
            BigDecimal otrasDocencia,
            BigDecimal investigacion,
            BigDecimal vinculacion,
            BigDecimal gestion) {

        /** La suma de los cinco componentes. */
        public BigDecimal total() {
            return clase.add(otrasDocencia).add(investigacion).add(vinculacion).add(gestion);
        }
    }

    /**
     * Una carrera del resumen.
     *
     * @param nombre su nombre
     * @param estado el estado de su parcial
     * @param paralelos las asignaturas-paralelo ofertadas
     * @param propuestos las que tienen propuesta
     * @param horas sus horas por componente
     */
    public record Carrera(
            String nombre, String estado, int paralelos, int propuestos, Horas horas) {}

    /**
     * Un docente con sus asignaciones.
     *
     * @param identificacion su número de identificación
     * @param nombre sus apellidos y nombres
     * @param asignaturas sus asignaturas-paralelo
     * @param actividades sus actividades no lectivas
     * @param horas sus horas por componente
     */
    public record Docente(
            String identificacion,
            String nombre,
            List<Asignatura> asignaturas,
            List<Actividad> actividades,
            Horas horas) {

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
     * @param carrera la carrera
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
