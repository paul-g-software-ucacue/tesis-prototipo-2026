package ec.mdd.academico.dominio;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Las carreras, sus mallas y sus asignaturas, con lo que hace falta para validarlas: la estructura
 * orgánica y los campos amplios de la CINE (F03-H02, research §4). Tiene las reglas que miran a más
 * de una entidad: la sede activa, la carrera y la malla vigentes, la malla de la misma carrera y el
 * ciclo frente al número de ciclos. Los códigos y las siglas repetidos los decide la aplicación,
 * con el repositorio.
 */
public final class DatosMaestros {

    private static final Set<TipoDeUnidad> SEDES =
            EnumSet.of(
                    TipoDeUnidad.SEDE_MATRIZ,
                    TipoDeUnidad.SEDE,
                    TipoDeUnidad.EXTENSION,
                    TipoDeUnidad.CAMPUS);

    private final Map<String, Carrera> carreras = new LinkedHashMap<>();
    private final Map<String, Malla> mallas = new LinkedHashMap<>();
    private final Map<String, Asignatura> asignaturas = new LinkedHashMap<>();
    private final Map<String, UnidadOrganica> unidades = new LinkedHashMap<>();
    private final Set<String> camposAmplios;

    /**
     * @param carreras las carreras registradas
     * @param mallas las mallas registradas
     * @param asignaturas las asignaturas registradas
     * @param unidades las unidades de la estructura orgánica, para validar la sede
     * @param camposAmplios los códigos de los 11 campos amplios de la CINE
     */
    public DatosMaestros(
            Collection<Carrera> carreras,
            Collection<Malla> mallas,
            Collection<Asignatura> asignaturas,
            Collection<UnidadOrganica> unidades,
            Collection<String> camposAmplios) {
        carreras.forEach(carrera -> this.carreras.put(carrera.codigo(), carrera));
        mallas.forEach(malla -> this.mallas.put(malla.codigo(), malla));
        asignaturas.forEach(asignatura -> this.asignaturas.put(asignatura.codigo(), asignatura));
        unidades.forEach(unidad -> this.unidades.put(unidad.codigo(), unidad));
        this.camposAmplios = Set.copyOf(camposAmplios);
    }

    /** Las carreras, en el orden en que se dieron. */
    public List<Carrera> carreras() {
        return List.copyOf(carreras.values());
    }

    /** Las mallas, en el orden en que se dieron. */
    public List<Malla> mallas() {
        return List.copyOf(mallas.values());
    }

    /** Las asignaturas, en el orden en que se dieron. */
    public List<Asignatura> asignaturas() {
        return List.copyOf(asignaturas.values());
    }

    /** La carrera con ese código, si existe. */
    public Optional<Carrera> carrera(String codigo) {
        return Optional.ofNullable(carreras.get(codigo));
    }

    /** La malla con ese código de versión, si existe. */
    public Optional<Malla> malla(String codigo) {
        return Optional.ofNullable(mallas.get(codigo));
    }

    /** La asignatura con ese código, si existe. */
    public Optional<Asignatura> asignatura(String codigo) {
        return Optional.ofNullable(asignaturas.get(codigo));
    }

    /** La suma de las horas semanales de clase de las asignaturas activas de la malla (RN-18). */
    public BigDecimal horasDe(String malla) {
        return asignaturas.values().stream()
                .filter(asignatura -> asignatura.activa() && asignatura.malla().equals(malla))
                .map(Asignatura::horasSemanalesClase)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Los mismos datos con esta carrera, malla o asignatura agregada o reemplazada. */
    public DatosMaestros con(Carrera carrera) {
        DatosMaestros otros = copia();
        otros.carreras.put(carrera.codigo(), carrera);
        return otros;
    }

    /** Los mismos datos con esta malla agregada o reemplazada. */
    public DatosMaestros con(Malla malla) {
        DatosMaestros otros = copia();
        otros.mallas.put(malla.codigo(), malla);
        return otros;
    }

    /** Los mismos datos con esta asignatura agregada o reemplazada. */
    public DatosMaestros con(Asignatura asignatura) {
        DatosMaestros otros = copia();
        otros.asignaturas.put(asignatura.codigo(), asignatura);
        return otros;
    }

    /**
     * Valida una carrera nueva: su sede es una sede, extensión o campus activo, y su campo amplio
     * es de la CINE.
     *
     * @return la misma carrera
     * @throws DatoInvalido si no cumple una regla
     */
    public Carrera registrarCarrera(Carrera nueva) {
        validarSede(nueva.sede());
        validarCampo(nueva.campo());
        return nueva;
    }

    /**
     * Corrige una carrera: la sede y el campo como al registrarla, y el número de ciclos no baja
     * del ciclo de una de sus asignaturas activas.
     *
     * @throws DatoInvalido si no cumple una regla
     */
    public Carrera.Correccion corregirCarrera(
            Carrera actual,
            String nombre,
            String siglas,
            String sede,
            NivelDeFormacion nivelFormacion,
            Modalidad modalidad,
            List<Jornada> jornadas,
            CampoConocimiento campo,
            String campoDetallado,
            int numeroCiclos) {
        Carrera.Correccion correccion =
                actual.corregida(
                        nombre,
                        siglas,
                        sede,
                        nivelFormacion,
                        modalidad,
                        jornadas,
                        campo,
                        campoDetallado,
                        numeroCiclos);
        if (!sede.equals(actual.sede())) {
            validarSede(sede);
        }
        validarCampo(campo);
        int mayorCiclo =
                asignaturas.values().stream()
                        .filter(a -> a.activa() && a.carrera().equals(actual.codigo()))
                        .mapToInt(Asignatura::ciclo)
                        .max()
                        .orElse(0);
        if (numeroCiclos < mayorCiclo) {
            throw new DatoInvalido(
                    "numeroCiclos",
                    "La carrera tiene asignaturas activas en el ciclo " + mayorCiclo);
        }
        return correccion;
    }

    /**
     * Valida una malla nueva: su carrera existe y está vigente.
     *
     * @return la misma malla
     * @throws DatoInvalido si no cumple una regla
     */
    public Malla registrarMalla(Malla nueva) {
        Carrera carrera = carreraVigente(nueva.carrera());
        return new Malla(nueva.codigo(), carrera.codigo(), nueva.estado());
    }

    /**
     * Valida una asignatura nueva: su carrera existe y está vigente, su malla es una vigente de esa
     * carrera, su ciclo no pasa el número de ciclos de la carrera y su campo amplio es de la CINE.
     *
     * @return la misma asignatura
     * @throws DatoInvalido si no cumple una regla
     */
    public Asignatura registrarAsignatura(Asignatura nueva) {
        Carrera carrera = carreraVigente(nueva.carrera());
        Malla malla = mallas.get(nueva.malla());
        if (malla == null || !malla.carrera().equals(carrera.codigo())) {
            throw new DatoInvalido("malla", "La malla no es de esa carrera");
        }
        validarMallaVigente(malla);
        validarCiclo(nueva.ciclo(), carrera);
        validarCampo(nueva.campo());
        return nueva;
    }

    /**
     * Corrige una asignatura: el ciclo frente a su carrera y el campo, como al registrarla.
     *
     * @throws DatoInvalido si no cumple una regla
     */
    public Asignatura.Correccion corregirAsignatura(
            Asignatura actual,
            String nombre,
            int ciclo,
            UnidadDeOrganizacionCurricular unidadOrganizacionCurricular,
            BigDecimal horasSemanalesClase,
            BigDecimal horasPracticasSemanales,
            BigDecimal creditos,
            boolean troncoComun,
            CampoConocimiento campo,
            List<String> palabrasClave) {
        Asignatura.Correccion correccion =
                actual.corregida(
                        nombre,
                        ciclo,
                        unidadOrganizacionCurricular,
                        horasSemanalesClase,
                        horasPracticasSemanales,
                        creditos,
                        troncoComun,
                        campo,
                        palabrasClave);
        validarCiclo(ciclo, carreras.get(actual.carrera()));
        validarCampo(campo);
        return correccion;
    }

    /**
     * Inactiva o reactiva una asignatura; reactivarla exige su malla vigente.
     *
     * @throws DatoInvalido si la malla está derogada
     */
    public Asignatura cambiarEstadoAsignatura(Asignatura actual, boolean activa) {
        if (activa && !actual.activa()) {
            validarMallaVigente(mallas.get(actual.malla()));
        }
        return actual.conEstado(activa);
    }

    private Carrera carreraVigente(String codigo) {
        Carrera carrera = carreras.get(codigo);
        if (carrera == null) {
            throw new DatoInvalido("carrera", "No hay una carrera con ese código");
        }
        if (carrera.estado() != EstadoDeCarrera.VIGENTE) {
            throw new DatoInvalido("carrera", "La carrera no está vigente");
        }
        return carrera;
    }

    private static void validarMallaVigente(Malla malla) {
        if (malla.estado() != EstadoDeMalla.VIGENTE) {
            throw new DatoInvalido(
                    "malla", "La malla está derogada: no admite asignaturas nuevas ni reactivadas");
        }
    }

    private static void validarCiclo(int ciclo, Carrera carrera) {
        if (ciclo > carrera.numeroCiclos()) {
            throw new DatoInvalido(
                    "ciclo", "La carrera tiene " + carrera.numeroCiclos() + " ciclos");
        }
    }

    private void validarSede(String codigo) {
        UnidadOrganica sede = unidades.get(codigo);
        if (sede == null || !SEDES.contains(sede.tipo())) {
            throw new DatoInvalido(
                    "sede", "Elige una sede, extensión o campus de la estructura orgánica");
        }
        if (!sede.activa()) {
            throw new DatoInvalido("sede", "La sede está inactiva");
        }
    }

    private void validarCampo(CampoConocimiento campo) {
        if (!camposAmplios.contains(campo.amplio())) {
            throw new DatoInvalido("campoAmplio", "No es un campo amplio de la CINE");
        }
    }

    private DatosMaestros copia() {
        return new DatosMaestros(
                carreras.values(),
                mallas.values(),
                asignaturas.values(),
                unidades.values(),
                camposAmplios);
    }
}
