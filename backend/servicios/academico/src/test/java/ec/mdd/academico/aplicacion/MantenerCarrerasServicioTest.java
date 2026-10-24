package ec.mdd.academico.aplicacion;

import static ec.mdd.academico.aplicacion.DatosDePrueba.RELOJ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.CampoAmplio;
import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.EstadoDeMalla;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Malla;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadDeOrganizacionCurricular;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.AsignaturaNoEncontrada;
import ec.mdd.academico.puertos.entrada.AsignaturaRepetida;
import ec.mdd.academico.puertos.entrada.CarreraNoEncontrada;
import ec.mdd.academico.puertos.entrada.CarreraRepetida;
import ec.mdd.academico.puertos.entrada.DatosDeAsignatura;
import ec.mdd.academico.puertos.entrada.DatosDeCarrera;
import ec.mdd.academico.puertos.entrada.MallaNoEncontrada;
import ec.mdd.academico.puertos.entrada.MallaRepetida;
import ec.mdd.academico.puertos.entrada.SiglasRepetidas;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Mantener carreras, mallas y asignaturas (FR-003, FR-005, FR-009): cada cambio se valida con los
 * datos bloqueados, se guarda y se registra en la misma transacción. Con Mockito.
 */
class MantenerCarrerasServicioTest {

    private static final String QUIEN = "vicerrectorado.demo";
    private static final CampoConocimiento TIC = new CampoConocimiento("06", "061");
    private static final CampoConocimiento MATE = new CampoConocimiento("05", "054");

    private final RepositorioCarreras repositorio = mock(RepositorioCarreras.class);
    private final RepositorioEstructura estructura = mock(RepositorioEstructura.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final DatosDePrueba.TransaccionDePrueba transaccion =
            new DatosDePrueba.TransaccionDePrueba();
    private final MantenerCarrerasServicio servicio =
            new MantenerCarrerasServicio(repositorio, estructura, registro, transaccion, RELOJ);

    private final List<Carrera> carreras = new ArrayList<>();
    private final List<Malla> mallas = new ArrayList<>();
    private final List<Asignatura> asignaturas = new ArrayList<>();

    @BeforeEach
    void datos() {
        when(estructura.todas())
                .thenReturn(
                        List.of(
                                UnidadOrganica.nueva(
                                        "MATRIZ",
                                        "Sede matriz",
                                        TipoDeUnidad.SEDE_MATRIZ,
                                        null,
                                        null,
                                        null)));
        when(repositorio.camposAmplios())
                .thenReturn(
                        List.of(new CampoAmplio("05", "Ciencias"), new CampoAmplio("06", "TIC")));
        when(repositorio.carreras()).thenAnswer(invocacion -> List.copyOf(carreras));
        when(repositorio.mallas()).thenAnswer(invocacion -> List.copyOf(mallas));
        when(repositorio.asignaturas()).thenAnswer(invocacion -> List.copyOf(asignaturas));
    }

    @Test
    void dadaUnaCarreraValida_cuandoSeRegistra_entoncesBloqueaValidaGuardaYRegistra() {
        Carrera nueva = carrera("C1", "DS");

        assertThat(servicio.registrarCarrera(nueva, QUIEN)).isEqualTo(nueva);

        InOrder orden = inOrder(repositorio, registro);
        orden.verify(repositorio).bloquear();
        orden.verify(repositorio).registrar(nueva, QUIEN, DatosDePrueba.AHORA);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.CARRERA_REGISTRADA);
        assertThat(cambio.detalle()).containsEntry("codigo", "C1").containsEntry("siglas", "DS");
        assertThat(transaccion.dentro()).isFalse();
    }

    @Test
    void dadoUnCodigoOUnasSiglasRepetidos_cuandoSeRegistra_entoncesSuErrorSinRegistro() {
        carreras.add(carrera("C1", "DS"));

        assertThatThrownBy(() -> servicio.registrarCarrera(carrera("C1", "OT"), QUIEN))
                .isInstanceOf(CarreraRepetida.class);
        assertThatThrownBy(() -> servicio.registrarCarrera(carrera("C2", "DS"), QUIEN))
                .isInstanceOf(SiglasRepetidas.class);
        verify(repositorio, never()).registrar(any(Carrera.class), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaCarrera_cuandoSeCorrige_entoncesRegistraCadaCambioYSinCambiosNoRegistra() {
        carreras.add(carrera("C1", "DS"));
        carreras.add(carrera("C2", "CO"));

        Carrera corregida = servicio.corregirCarrera("C1", datos("SOFTWARE", "DS", 5), QUIEN);

        assertThat(corregida.nombre()).isEqualTo("SOFTWARE");
        verify(repositorio).guardar(corregida);
        assertThat(registrado().accion()).isEqualTo(Accion.CARRERA_CORREGIDA);
        assertThatThrownBy(() -> servicio.corregirCarrera("C1", datos("X", "CO", 5), QUIEN))
                .isInstanceOf(SiglasRepetidas.class);
        assertThatThrownBy(() -> servicio.corregirCarrera("C9", datos("X", "X", 5), QUIEN))
                .isInstanceOf(CarreraNoEncontrada.class);
    }

    @Test
    void dadaUnaCarrera_cuandoCambiaSuEstado_entoncesRegistraYElMismoEstadoNo() {
        carreras.add(carrera("C1", "DS"));

        Carrera enCierre = servicio.cambiarEstadoCarrera("C1", EstadoDeCarrera.EN_CIERRE, QUIEN);
        servicio.cambiarEstadoCarrera("C1", EstadoDeCarrera.VIGENTE, QUIEN);

        assertThat(enCierre.estado()).isEqualTo(EstadoDeCarrera.EN_CIERRE);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.CARRERA_ESTADO_CAMBIADO);
        assertThat(cambio.detalle())
                .containsEntry("anterior", "VIGENTE")
                .containsEntry("nuevo", "EN_CIERRE");
    }

    @Test
    void dadaUnaCarreraVigente_cuandoSeRegistraSuMalla_entoncesRegistraYUnaRepetidaNo() {
        carreras.add(carrera("C1", "DS"));

        Malla malla = servicio.registrarMalla("C1", "DS-2019", QUIEN);

        assertThat(malla.estado()).isEqualTo(EstadoDeMalla.VIGENTE);
        verify(repositorio).registrar(malla, QUIEN, DatosDePrueba.AHORA);
        assertThat(registrado().accion()).isEqualTo(Accion.MALLA_REGISTRADA);
        mallas.add(malla);
        assertThatThrownBy(() -> servicio.registrarMalla("C1", "DS-2019", QUIEN))
                .isInstanceOf(MallaRepetida.class);
        assertThatThrownBy(() -> servicio.registrarMalla("C9", "OT-2019", QUIEN))
                .isInstanceOf(CarreraNoEncontrada.class);
    }

    @Test
    void dadaUnaMalla_cuandoSeDeroga_entoncesRegistraSuEstado() {
        carreras.add(carrera("C1", "DS"));
        mallas.add(Malla.nueva("DS-2019", "C1"));

        assertThat(servicio.cambiarEstadoMalla("DS-2019", EstadoDeMalla.DEROGADA, QUIEN).estado())
                .isEqualTo(EstadoDeMalla.DEROGADA);
        assertThat(registrado().accion()).isEqualTo(Accion.MALLA_ESTADO_CAMBIADO);
        assertThatThrownBy(() -> servicio.cambiarEstadoMalla("XX", EstadoDeMalla.DEROGADA, QUIEN))
                .isInstanceOf(MallaNoEncontrada.class);
    }

    @Test
    void dadaUnaAsignaturaValida_cuandoSeRegistra_entoncesRegistraYUnaRepetidaNo() {
        carreras.add(carrera("C1", "DS"));
        mallas.add(Malla.nueva("DS-2019", "C1"));

        Asignatura nueva = servicio.registrarAsignatura(asignatura("A1", 1), QUIEN);

        verify(repositorio).registrar(nueva, QUIEN, DatosDePrueba.AHORA);
        assertThat(registrado().accion()).isEqualTo(Accion.ASIGNATURA_REGISTRADA);
        asignaturas.add(nueva);
        assertThatThrownBy(() -> servicio.registrarAsignatura(asignatura("A1", 2), QUIEN))
                .isInstanceOf(AsignaturaRepetida.class);
        assertThatThrownBy(() -> servicio.registrarAsignatura(asignatura("A2", 6), QUIEN))
                .isInstanceOfSatisfying(
                        DatoInvalido.class, e -> assertThat(e.campo()).isEqualTo("ciclo"));
    }

    @Test
    void dadaUnaAsignatura_cuandoSeCorrigeYSeInactivaYReactiva_entoncesRegistraCadaAccion() {
        carreras.add(carrera("C1", "DS"));
        mallas.add(Malla.nueva("DS-2019", "C1"));
        asignaturas.add(asignatura("A1", 1));

        Asignatura corregida =
                servicio.corregirAsignatura(
                        "A1",
                        new DatosDeAsignatura(
                                "MATEMÁTICA",
                                2,
                                UnidadDeOrganizacionCurricular.BASICA,
                                new BigDecimal("3"),
                                null,
                                null,
                                true,
                                MATE,
                                List.of("a", "b")),
                        QUIEN);
        assertThat(corregida.ciclo()).isEqualTo(2);
        asignaturas.set(0, corregida);
        servicio.cambiarEstadoAsignatura("A1", false, QUIEN);
        asignaturas.set(0, corregida.conEstado(false));
        servicio.cambiarEstadoAsignatura("A1", true, QUIEN);

        ArgumentCaptor<CambioRegistrado> cambios = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro, org.mockito.Mockito.times(3)).registrar(cambios.capture());
        assertThat(cambios.getAllValues())
                .extracting(CambioRegistrado::accion)
                .containsExactly(
                        Accion.ASIGNATURA_CORREGIDA,
                        Accion.ASIGNATURA_INACTIVADA,
                        Accion.ASIGNATURA_REACTIVADA);
        assertThatThrownBy(() -> servicio.cambiarEstadoAsignatura("XX", true, QUIEN))
                .isInstanceOf(AsignaturaNoEncontrada.class);
    }

    @Test
    void dadaUnaAsignaturaEnElCiclo5_cuandoSeBajanLosCiclosDeSuCarrera_entoncesDatoInvalido() {
        carreras.add(carrera("C1", "DS"));
        mallas.add(Malla.nueva("DS-2019", "C1"));
        asignaturas.add(asignatura("A1", 5));

        assertThatThrownBy(() -> servicio.corregirCarrera("C1", datos("N", "DS", 4), QUIEN))
                .isInstanceOfSatisfying(
                        DatoInvalido.class, e -> assertThat(e.campo()).isEqualTo("numeroCiclos"));
        verify(repositorio, never()).guardar(any(Carrera.class));
    }

    private CambioRegistrado registrado() {
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro, org.mockito.Mockito.atLeastOnce()).registrar(cambio.capture());
        return cambio.getAllValues().getFirst();
    }

    private static Carrera carrera(String codigo, String siglas) {
        return Carrera.nueva(
                codigo,
                "DESARROLLO DE SOFTWARE",
                siglas,
                "MATRIZ",
                NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                Modalidad.PRESENCIAL,
                List.of(Jornada.NOCTURNA),
                TIC,
                null,
                5);
    }

    private static DatosDeCarrera datos(String nombre, String siglas, int ciclos) {
        return new DatosDeCarrera(
                nombre,
                siglas,
                "MATRIZ",
                NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                Modalidad.PRESENCIAL,
                List.of(Jornada.NOCTURNA),
                TIC,
                null,
                ciclos);
    }

    private static Asignatura asignatura(String codigo, int ciclo) {
        return Asignatura.nueva(
                codigo,
                "C1",
                "DS-2019",
                "MATEMÁTICA DISCRETA",
                ciclo,
                UnidadDeOrganizacionCurricular.BASICA,
                new BigDecimal("3"),
                null,
                null,
                true,
                MATE,
                List.of("matemática", "lógica"));
    }
}
