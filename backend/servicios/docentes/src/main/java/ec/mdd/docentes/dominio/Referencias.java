package ec.mdd.docentes.dominio;

import java.util.Map;
import java.util.Optional;

/**
 * Lo que Docentes necesita de Académico: las carreras, las asignaturas y los campos amplios que
 * existen, cada uno con su nombre (FR-005, research §4). Tiene las reglas de un docente que miran a
 * otro servicio.
 *
 * @param carreras cada carrera por código, en cualquier estado
 * @param asignaturas cada asignatura por código, activas e inactivas
 * @param camposAmplios el nombre de cada uno de los 11 campos amplios de la CINE, por código
 */
public record Referencias(
        Map<String, Carrera> carreras,
        Map<String, Asignatura> asignaturas,
        Map<String, String> camposAmplios) {

    /** Copia los mapas. */
    public Referencias {
        carreras = Map.copyOf(carreras);
        asignaturas = Map.copyOf(asignaturas);
        camposAmplios = Map.copyOf(camposAmplios);
    }

    /**
     * Comprueba que la carrera base, cada asignatura afín y el campo amplio del título de tercer
     * nivel existan. Una carrera en cierre o una asignatura inactiva sirven: un docente puede
     * pertenecer a una carrera que cierra.
     *
     * @throws DatoInvalido en {@code carreraBase}, {@code ficha.asignaturasAfines[i]} o {@code
     *     ficha.tercerNivel.campoAmplio}
     */
    public void verificar(DatosDelDocente datos) {
        if (!carreras.containsKey(datos.carreraBase())) {
            throw new DatoInvalido("carreraBase", "La carrera no existe en Académico");
        }
        if (!camposAmplios.containsKey(datos.ficha().tercerNivel().campo().amplio())) {
            throw new DatoInvalido(
                    "ficha.tercerNivel.campoAmplio",
                    "El campo amplio tiene que ser uno de los 11 de la CINE");
        }
        var afines = datos.ficha().asignaturasAfines();
        for (int i = 0; i < afines.size(); i++) {
            if (!asignaturas.containsKey(afines.get(i))) {
                throw new DatoInvalido(
                        "ficha.asignaturasAfines[" + i + "]",
                        "La asignatura no existe en Académico");
            }
        }
    }

    /** El nombre de la carrera, si existe. */
    public Optional<String> carrera(String codigo) {
        return Optional.ofNullable(carreras.get(codigo)).map(Carrera::nombre);
    }

    /** El nombre de la asignatura, si existe. */
    public Optional<String> asignatura(String codigo) {
        return Optional.ofNullable(asignaturas.get(codigo)).map(Asignatura::nombre);
    }

    /** El nombre del campo amplio, si existe. */
    public Optional<String> campoAmplio(String codigo) {
        return Optional.ofNullable(camposAmplios.get(codigo));
    }

    /**
     * Una carrera de Académico.
     *
     * @param nombre su nombre
     * @param siglas sus siglas
     */
    public record Carrera(String nombre, String siglas) {}

    /**
     * Una asignatura de Académico.
     *
     * @param nombre su nombre
     * @param carrera el código de su carrera
     */
    public record Asignatura(String nombre, String carrera) {}
}
