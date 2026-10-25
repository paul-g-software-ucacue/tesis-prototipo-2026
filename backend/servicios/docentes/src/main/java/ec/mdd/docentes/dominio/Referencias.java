package ec.mdd.docentes.dominio;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Lo que Docentes necesita de Académico: las carreras, las asignaturas y los campos amplios que
 * existen, cada uno con su nombre (FR-005 de F03-H04), y las unidades y los periodos (FR-008 de
 * F03-H05). Tiene las reglas de un docente, de un cargo y de un proyecto que miran a otro servicio.
 *
 * @param carreras cada carrera por código, en cualquier estado
 * @param asignaturas cada asignatura por código, activas e inactivas
 * @param camposAmplios el nombre de cada uno de los 11 campos amplios de la CINE, por código
 * @param unidades cada unidad de la estructura orgánica por código, activas e inactivas
 * @param periodos cada periodo académico por código, con sus fechas
 */
public record Referencias(
        Map<String, Carrera> carreras,
        Map<String, Asignatura> asignaturas,
        Map<String, String> camposAmplios,
        Map<String, Unidad> unidades,
        Map<String, Periodo> periodos) {

    /** Copia los mapas. */
    public Referencias {
        carreras = Map.copyOf(carreras);
        asignaturas = Map.copyOf(asignaturas);
        camposAmplios = Map.copyOf(camposAmplios);
        unidades = Map.copyOf(unidades);
        periodos = Map.copyOf(periodos);
    }

    /** Las referencias de un docente, sin unidades ni periodos (F03-H04). */
    public Referencias(
            Map<String, Carrera> carreras,
            Map<String, Asignatura> asignaturas,
            Map<String, String> camposAmplios) {
        this(carreras, asignaturas, camposAmplios, Map.of(), Map.of());
    }

    /**
     * Comprueba que la unidad y la carrera del cargo existan y correspondan a él (research §4): la
     * unidad, activa; la de un gestor, la de su gestión autorizada; la de un miembro de comisión,
     * una comisión. Una carrera en cierre sirve.
     *
     * @throws DatoInvalido en {@code unidad} o {@code carrera}
     */
    public void verificar(Cargo cargo) {
        if (cargo.unidad() != null) {
            Unidad unidad = unidades.get(cargo.unidad());
            if (unidad == null || !unidad.activa()) {
                throw new DatoInvalido(
                        "unidad", "La unidad no existe o está inactiva en Académico");
            }
            if (cargo.tipo().esGestor() && !cargo.tipo().gestion().equals(unidad.gestion())) {
                throw new DatoInvalido(
                        "unidad",
                        "Un gestor dirige la unidad de su gestión autorizada, "
                                + cargo.tipo().gestion());
            }
            if (cargo.tipo().esDeComision() && !"COMISION".equals(unidad.tipo())) {
                throw new DatoInvalido(
                        "unidad", "Un miembro de comisión pertenece a una unidad de tipo comisión");
            }
        }
        if (cargo.carrera() != null && !carreras.containsKey(cargo.carrera())) {
            throw new DatoInvalido("carrera", "La carrera no existe en Académico");
        }
    }

    /**
     * Comprueba que cada carrera del proyecto exista. Una carrera en cierre sirve.
     *
     * @throws DatoInvalido en {@code carreras[i]}
     */
    public void verificar(Proyecto proyecto) {
        for (int i = 0; i < proyecto.carreras().size(); i++) {
            if (!carreras.containsKey(proyecto.carreras().get(i))) {
                throw new DatoInvalido("carreras[" + i + "]", "La carrera no existe en Académico");
            }
        }
    }

    /**
     * El periodo con sus fechas.
     *
     * @throws DatoInvalido en {@code periodo} si no existe
     */
    public Periodo periodo(String codigo) {
        Periodo periodo = periodos.get(codigo);
        if (periodo == null) {
            throw new DatoInvalido("periodo", "El periodo no existe en Académico");
        }
        return periodo;
    }

    /** El nombre de la unidad, si existe. */
    public Optional<String> unidad(String codigo) {
        return Optional.ofNullable(codigo).map(unidades::get).map(Unidad::nombre);
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

    /**
     * Una unidad de la estructura orgánica de Académico.
     *
     * @param nombre su nombre
     * @param tipo el nombre de su tipo, como {@code GESTION} o {@code COMISION}
     * @param gestion la gestión autorizada de una unidad de gestión, como {@code G07}; nula en las
     *     demás
     * @param activa si no se inactivó
     */
    public record Unidad(String nombre, String tipo, String gestion, boolean activa) {}

    /**
     * Un periodo académico de Académico.
     *
     * @param nombre su nombre
     * @param inicio el primer día
     * @param fin el último día
     */
    public record Periodo(String nombre, LocalDate inicio, LocalDate fin) {}
}
