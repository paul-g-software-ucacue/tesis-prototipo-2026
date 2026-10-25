package ec.mdd.academico.adaptadores.entrada;

import ec.mdd.academico.aplicacion.ConsultarCarrerasServicio;
import ec.mdd.academico.aplicacion.ConsultarReferenciasServicio;
import ec.mdd.academico.aplicacion.MantenerCarrerasServicio;
import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.ConsultarCarreras;
import ec.mdd.academico.puertos.entrada.ConsultarReferencias;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Doble de los repositorios escrito a mano (principio VIII): las carreras, mallas y asignaturas en
 * memoria, con los servicios y las reglas reales del dominio. La estructura tiene la sede matriz
 * {@code MATRIZ} y el Rectorado {@code RECT}; los campos amplios son los 11 de la CINE.
 */
final class CarrerasEnMemoria implements RepositorioCarreras {

    static final List<CampoAmplio> CAMPOS =
            List.of(
                    new CampoAmplio("00", "Programas genéricos"),
                    new CampoAmplio("01", "Educación"),
                    new CampoAmplio("02", "Artes y humanidades"),
                    new CampoAmplio("03", "Ciencias sociales, periodismo e información"),
                    new CampoAmplio("04", "Administración de empresas y derecho"),
                    new CampoAmplio("05", "Ciencias naturales, matemáticas y estadística"),
                    new CampoAmplio("06", "Tecnologías de la información y la comunicación"),
                    new CampoAmplio("07", "Ingeniería, industria y construcción"),
                    new CampoAmplio("08", "Agricultura, silvicultura, pesca y veterinaria"),
                    new CampoAmplio("09", "Salud y bienestar"),
                    new CampoAmplio("10", "Servicios"));

    private final List<Carrera> carreras = new ArrayList<>();
    private final List<Malla> mallas = new ArrayList<>();
    private final List<Asignatura> asignaturas = new ArrayList<>();

    /** Empieza vacío, para que cada prueba empiece igual. */
    void reiniciar() {
        carreras.clear();
        mallas.clear();
        asignaturas.clear();
    }

    /** La consulta real sobre este doble. */
    ConsultarCarreras consultar() {
        return new ConsultarCarrerasServicio(this);
    }

    /** Las referencias reales sobre este doble, la estructura de prueba y el periodo 2026-I. */
    ConsultarReferencias referencias() {
        return new ConsultarReferenciasServicio(this, estructura(), periodos());
    }

    /** Un solo periodo de prueba, 2026-I, para las referencias de Docentes (F03-H05). */
    static RepositorioPeriodos periodos() {
        PeriodoAcademico periodo =
                PeriodoAcademico.nuevo(
                        "2026-I",
                        "Periodo académico 2026-I",
                        LocalDate.of(2026, 4, 6),
                        LocalDate.of(2026, 8, 7));
        return new RepositorioPeriodos() {
            @Override
            public List<PeriodoAcademico> todos() {
                return List.of(periodo);
            }

            @Override
            public Optional<PeriodoAcademico> porCodigo(String codigo) {
                return Optional.of(periodo).filter(uno -> uno.codigo().equals(codigo));
            }

            @Override
            public Optional<PeriodoAcademico> bloquear(String codigo) {
                return porCodigo(codigo);
            }

            @Override
            public boolean registrar(PeriodoAcademico nuevo, String quien, Instant momento) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void guardar(PeriodoAcademico otro) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** El mantenimiento real sobre este doble, sin registro y sin transacción. */
    MantenerCarreras mantener() {
        Transaccion directa =
                new Transaccion() {
                    @Override
                    public <T> T ejecutar(Supplier<T> trabajo) {
                        return trabajo.get();
                    }
                };
        return new MantenerCarrerasServicio(
                this, estructura(), cambio -> {}, directa, Clock.systemUTC());
    }

    /** La estructura de prueba: la sede matriz y el Rectorado. */
    static RepositorioEstructura estructura() {
        List<UnidadOrganica> unidades =
                List.of(
                        UnidadOrganica.nueva(
                                "MATRIZ",
                                "Sede matriz",
                                TipoDeUnidad.SEDE_MATRIZ,
                                null,
                                null,
                                null),
                        UnidadOrganica.nueva(
                                "RECT", "Rectorado", TipoDeUnidad.GOBIERNO, "MATRIZ", "G01", null));
        return new RepositorioEstructura() {
            @Override
            public List<UnidadOrganica> todas() {
                return unidades;
            }

            @Override
            public List<GestionAutorizada> catalogo() {
                return List.of();
            }

            @Override
            public void bloquear() {}

            @Override
            public boolean registrar(UnidadOrganica unidad, String quien, Instant momento) {
                return false;
            }

            @Override
            public void guardar(UnidadOrganica unidad) {}
        };
    }

    @Override
    public List<CampoAmplio> camposAmplios() {
        return CAMPOS;
    }

    @Override
    public List<Carrera> carreras() {
        return ordenadas(carreras, Carrera::codigo);
    }

    @Override
    public List<Malla> mallas() {
        return ordenadas(mallas, Malla::codigo);
    }

    @Override
    public List<Asignatura> asignaturas() {
        return ordenadas(asignaturas, Asignatura::codigo);
    }

    @Override
    public void bloquear() {}

    @Override
    public void registrar(Carrera carrera, String quien, Instant momento) {
        carreras.add(carrera);
    }

    @Override
    public void registrar(Malla malla, String quien, Instant momento) {
        mallas.add(malla);
    }

    @Override
    public void registrar(Asignatura asignatura, String quien, Instant momento) {
        asignaturas.add(asignatura);
    }

    @Override
    public void guardar(Carrera carrera) {
        carreras.replaceAll(una -> una.codigo().equals(carrera.codigo()) ? carrera : una);
    }

    @Override
    public void guardar(Malla malla) {
        mallas.replaceAll(una -> una.codigo().equals(malla.codigo()) ? malla : una);
    }

    @Override
    public void guardar(Asignatura asignatura) {
        asignaturas.replaceAll(una -> una.codigo().equals(asignatura.codigo()) ? asignatura : una);
    }

    private static <T> List<T> ordenadas(List<T> lista, Function<T, String> codigo) {
        return lista.stream().sorted(Comparator.comparing(codigo)).toList();
    }
}
