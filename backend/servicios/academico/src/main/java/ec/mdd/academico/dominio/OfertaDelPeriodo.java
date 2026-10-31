package ec.mdd.academico.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * La oferta de un periodo: sus asignaturas-paralelo y las reglas que miran al periodo, a la
 * asignatura, a su carrera y a la sede (glosario «Oferta», RF-15, RN-18, carga 5). Es un valor:
 * cada cambio devuelve el resultado y {@link #con} da la oferta con él, como {@link DatosMaestros}.
 *
 * <p>Solo cambia mientras el periodo está en Planificación (aclaración 2). Las horas y la sede que
 * no se indican son las de la asignatura y las de la carrera, leídas al consultar: no se copian.
 */
public final class OfertaDelPeriodo {

    private final PeriodoAcademico periodo;
    private final Map<String, AsignaturaParalelo> oferta = new LinkedHashMap<>();
    private final DatosMaestros datos;

    /**
     * @param periodo el periodo de la oferta
     * @param oferta sus asignaturas-paralelo
     * @param datos las carreras, mallas, asignaturas y sedes, para las reglas y las horas
     */
    public OfertaDelPeriodo(
            PeriodoAcademico periodo, Collection<AsignaturaParalelo> oferta, DatosMaestros datos) {
        this.periodo = Objects.requireNonNull(periodo, "periodo");
        this.datos = Objects.requireNonNull(datos, "datos");
        oferta.forEach(una -> this.oferta.put(clave(una.asignatura(), una.paralelo()), una));
    }

    /** El periodo. */
    public PeriodoAcademico periodo() {
        return periodo;
    }

    /** Los datos maestros con los que se valida y se consulta. */
    public DatosMaestros datos() {
        return datos;
    }

    /** Si la oferta se puede cambiar: solo con el periodo en Planificación. */
    public boolean modificable() {
        return periodo.estado() == EstadoPeriodo.PLANIFICACION;
    }

    /** Las asignaturas-paralelo, por carrera, ciclo, asignatura y paralelo. */
    public List<AsignaturaParalelo> asignaturasParalelo() {
        return oferta.values().stream().sorted(orden()).toList();
    }

    /** La asignatura-paralelo de esa asignatura y ese paralelo, si está en la oferta. */
    public Optional<AsignaturaParalelo> asignaturaParalelo(String asignatura, String paralelo) {
        return Optional.ofNullable(oferta.get(clave(asignatura, paralelo)));
    }

    /**
     * La misma oferta con solo las asignaturas-paralelo de esa carrera: las horas a cubrir son las
     * suyas. Es lo que ve quien solo tiene «lo propio» (D-64).
     */
    public OfertaDelPeriodo deCarrera(String carrera) {
        return new OfertaDelPeriodo(
                periodo,
                oferta.values().stream()
                        .filter(una -> asignaturaDe(una).carrera().equals(carrera))
                        .toList(),
                datos);
    }

    /** La misma oferta con esta asignatura-paralelo agregada o reemplazada. */
    public OfertaDelPeriodo con(AsignaturaParalelo asignaturaParalelo) {
        List<AsignaturaParalelo> todas = new ArrayList<>(oferta.values());
        todas.removeIf(
                una ->
                        una.asignatura().equals(asignaturaParalelo.asignatura())
                                && una.paralelo().equals(asignaturaParalelo.paralelo()));
        todas.add(asignaturaParalelo);
        return new OfertaDelPeriodo(periodo, todas, datos);
    }

    /**
     * Valida una asignatura-paralelo nueva del periodo.
     *
     * @return la misma
     * @throws OfertaCerrada si el periodo no está en Planificación
     * @throws AsignaturaParaleloRepetida si la asignatura ya está en ese paralelo
     * @throws DatoInvalido si no cumple una regla
     */
    public AsignaturaParalelo ofertar(AsignaturaParalelo nueva) {
        validarModificable();
        if (!nueva.periodo().equals(periodo.codigo())) {
            throw new IllegalArgumentException("La asignatura-paralelo es de otro periodo");
        }
        if (oferta.containsKey(clave(nueva.asignatura(), nueva.paralelo()))) {
            throw new AsignaturaParaleloRepetida(
                    periodo.codigo(), nueva.asignatura(), nueva.paralelo());
        }
        validar(nueva);
        return nueva;
    }

    /**
     * Corrige una asignatura-paralelo, con las mismas reglas que al ofertarla.
     *
     * @throws OfertaCerrada si el periodo no está en Planificación
     * @throws DatoInvalido si un dato nuevo no cumple su regla
     */
    public AsignaturaParalelo.Correccion corregir(
            AsignaturaParalelo actual,
            Jornada jornada,
            Modalidad modalidad,
            String sede,
            int estudiantes,
            BigDecimal horasIndicadas) {
        validarModificable();
        AsignaturaParalelo.Correccion correccion =
                actual.corregida(jornada, modalidad, sede, estudiantes, horasIndicadas);
        validar(correccion.asignaturaParalelo());
        return correccion;
    }

    /**
     * Retira una asignatura-paralelo o la vuelve a ofertar; volver a ofertarla comprueba las mismas
     * reglas que corregirla.
     *
     * @throws OfertaCerrada si el periodo no está en Planificación
     * @throws DatoInvalido si al volver a ofertarla no cumple una regla
     */
    public AsignaturaParalelo cambiarEstado(
            AsignaturaParalelo actual, EstadoDeAsignaturaParalelo estado) {
        validarModificable();
        AsignaturaParalelo cambiada = actual.conEstado(estado);
        if (estado == EstadoDeAsignaturaParalelo.OFERTADA) {
            validar(cambiada);
        }
        return cambiada;
    }

    /** Las horas que suma: las indicadas, o las de clase de su asignatura. */
    public BigDecimal horasDe(AsignaturaParalelo asignaturaParalelo) {
        return asignaturaParalelo.horasIndicadas() != null
                ? asignaturaParalelo.horasIndicadas()
                : horasDeLaAsignatura(asignaturaParalelo);
    }

    /** Las horas de clase de su asignatura en la malla. */
    public BigDecimal horasDeLaAsignatura(AsignaturaParalelo asignaturaParalelo) {
        return asignaturaDe(asignaturaParalelo).horasSemanalesClase();
    }

    /** La sede donde se dicta: la indicada, o la de su carrera. */
    public String sedeDe(AsignaturaParalelo asignaturaParalelo) {
        return asignaturaParalelo.sede() != null
                ? asignaturaParalelo.sede()
                : carreraDe(asignaturaDe(asignaturaParalelo)).sede();
    }

    /**
     * Si las horas indicadas pasan las de su asignatura: puede pasar si la asignatura se corrigió
     * después de ofertarla (RN-18).
     */
    public boolean porEncimaDeLaAsignatura(AsignaturaParalelo asignaturaParalelo) {
        return asignaturaParalelo.horasIndicadas() != null
                && asignaturaParalelo
                                .horasIndicadas()
                                .compareTo(horasDeLaAsignatura(asignaturaParalelo))
                        > 0;
    }

    /** Las horas a cubrir del periodo, por carrera y por asignatura (RF-15). */
    public HorasACubrir horasACubrir() {
        Map<String, List<AsignaturaParalelo>> porAsignatura = new LinkedHashMap<>();
        for (AsignaturaParalelo una : asignaturasParalelo()) {
            porAsignatura.computeIfAbsent(una.asignatura(), codigo -> new ArrayList<>()).add(una);
        }
        Map<String, List<HorasACubrir.DeAsignatura>> porCarrera = new TreeMap<>();
        porAsignatura.forEach(
                (codigo, paralelos) -> {
                    Asignatura asignatura = asignaturaDe(paralelos.getFirst());
                    List<AsignaturaParalelo> ofertados =
                            paralelos.stream()
                                    .filter(
                                            una ->
                                                    una.estado()
                                                            == EstadoDeAsignaturaParalelo.OFERTADA)
                                    .toList();
                    BigDecimal total =
                            ofertados.stream()
                                    .map(this::horasDe)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    porCarrera
                            .computeIfAbsent(asignatura.carrera(), carrera -> new ArrayList<>())
                            .add(
                                    new HorasACubrir.DeAsignatura(
                                            codigo, asignatura.ciclo(), ofertados.size(), total));
                });
        List<HorasACubrir.DeCarrera> carreras = new ArrayList<>();
        porCarrera.forEach(
                (carrera, asignaturas) ->
                        carreras.add(
                                new HorasACubrir.DeCarrera(
                                        carrera,
                                        asignaturas.stream()
                                                .map(HorasACubrir.DeAsignatura::total)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                                        asignaturas)));
        return new HorasACubrir(
                carreras.stream()
                        .map(HorasACubrir.DeCarrera::total)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                carreras);
    }

    private void validarModificable() {
        if (!modificable()) {
            throw new OfertaCerrada(periodo.codigo());
        }
    }

    // Las reglas que miran a otras entidades (research §3).
    private void validar(AsignaturaParalelo asignaturaParalelo) {
        Asignatura asignatura =
                datos.asignatura(asignaturaParalelo.asignatura())
                        .orElseThrow(
                                () -> new DatoInvalido("asignatura", "La asignatura no existe"));
        if (!asignatura.activa()) {
            throw new DatoInvalido("asignatura", "La asignatura está inactiva");
        }
        Carrera carrera = carreraDe(asignatura);
        if (carrera.estado() != EstadoDeCarrera.VIGENTE) {
            throw new DatoInvalido("asignatura", "La carrera de la asignatura no está vigente");
        }
        if (!carrera.jornadas().contains(asignaturaParalelo.jornada())) {
            throw new DatoInvalido(
                    "jornada",
                    "Elige una jornada de la carrera: "
                            + String.join(
                                    ", ",
                                    carrera.jornadas().stream()
                                            .map(jornada -> jornada.name().toLowerCase(Locale.ROOT))
                                            .toList()));
        }
        if (asignaturaParalelo.sede() != null) {
            datos.validarSede(asignaturaParalelo.sede());
        }
        if (porEncimaDeLaAsignatura(asignaturaParalelo)) {
            throw new DatoInvalido(
                    "horasSemanales",
                    "No pueden pasar las "
                            + asignatura
                                    .horasSemanalesClase()
                                    .stripTrailingZeros()
                                    .toPlainString()
                                    .replace('.', ',')
                            + " horas de la asignatura (RN-18)");
        }
    }

    private Asignatura asignaturaDe(AsignaturaParalelo asignaturaParalelo) {
        return datos.asignatura(asignaturaParalelo.asignatura())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "La asignatura "
                                                + asignaturaParalelo.asignatura()
                                                + " no existe"));
    }

    private Carrera carreraDe(Asignatura asignatura) {
        return datos.carrera(asignatura.carrera()).orElseThrow();
    }

    // Por carrera, ciclo, asignatura y paralelo; una asignatura que no existe va al final.
    private Comparator<AsignaturaParalelo> orden() {
        Comparator<AsignaturaParalelo> porCarrera =
                Comparator.comparing(
                        una ->
                                datos.asignatura(una.asignatura())
                                        .map(Asignatura::carrera)
                                        .orElse("￿"));
        return porCarrera
                .thenComparing(
                        una -> datos.asignatura(una.asignatura()).map(Asignatura::ciclo).orElse(99))
                .thenComparing(AsignaturaParalelo::asignatura)
                .thenComparing(AsignaturaParalelo::paralelo);
    }

    private static String clave(String asignatura, String paralelo) {
        return asignatura + "\u0000" + paralelo;
    }
}
