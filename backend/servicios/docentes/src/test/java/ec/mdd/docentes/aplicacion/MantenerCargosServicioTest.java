package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.AdvertenciaDeCargo;
import ec.mdd.docentes.dominio.CargasDePrueba;
import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.Dedicacion;
import ec.mdd.docentes.dominio.DocenteNoHabilitado;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.dominio.TipoDeCargo;
import ec.mdd.docentes.puertos.entrada.CargoConsultado;
import ec.mdd.docentes.puertos.entrada.CargoNoEncontrado;
import ec.mdd.docentes.puertos.entrada.CargoRepetido;
import ec.mdd.docentes.puertos.entrada.CoordinadorVigente;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Mantener los cargos (F03-H05, FR-005, FR-006, FR-012), con Mockito en los puertos de salida. */
class MantenerCargosServicioTest {

    private static final String QUIEN = "datos.demo";
    private static final Instant AHORA = Instant.parse("2026-10-25T15:00:00Z");
    private static final String CARRERA = "550613A01-P-0301";

    private final RepositorioCargos repositorio = mock(RepositorioCargos.class);
    private final RepositorioDocentes docentes = mock(RepositorioDocentes.class);
    private final ReferenciasAcademicas referencias = mock(ReferenciasAcademicas.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final TransaccionDePrueba transaccion = new TransaccionDePrueba();
    private final MantenerCargosServicio servicio =
            new MantenerCargosServicio(
                    repositorio,
                    docentes,
                    referencias,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void datos() {
        when(referencias.referencias()).thenReturn(CargasDePrueba.referencias());
        when(docentes.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(CargasDePrueba.docente(true)));
        when(repositorio.buscar(anyString(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void dadoUnCoordinadorValido_cuandoSeRegistra_entoncesBloqueaComprueba_GuardaYRegistra() {
        Cargo coordinador = coordinador(EstadoDeCargo.VIGENTE);

        CargoConsultado registrado = servicio.registrar(coordinador, QUIEN);

        assertThat(registrado.cargo()).isEqualTo(coordinador);
        assertThat(registrado.nombreDelDocente()).isEqualTo("PERALTA MALDONADO JUAN ANDRES");
        assertThat(registrado.advertencias()).isEmpty();
        InOrder orden = inOrder(referencias, repositorio, registro);
        orden.verify(referencias).referencias();
        orden.verify(repositorio).bloquear();
        orden.verify(repositorio).hayCoordinadorVigente(CARRERA, null);
        orden.verify(repositorio).registrar(coordinador, QUIEN, AHORA);
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        orden.verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion()).isEqualTo(CambioRegistrado.Accion.CARGO_REGISTRADO);
        assertThat(cambio.getValue().detalle())
                .containsEntry("docente", DocentesDePrueba.CEDULA)
                .containsEntry("tipo", "COORDINADOR_CARRERA")
                .containsEntry("carrera", CARRERA)
                .doesNotContainKey("unidad");
    }

    @Test
    void
            dadoUnDocenteSinCuartoNivel_cuandoSeRegistraComoCoordinador_entoncesSeGuardaConSuAdvertencia() {
        when(docentes.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(CargasDePrueba.docente(false)));

        CargoConsultado registrado = servicio.registrar(coordinador(EstadoDeCargo.VIGENTE), QUIEN);

        assertThat(registrado.advertencias()).containsExactly(AdvertenciaDeCargo.CUARTO_NIVEL);
        verify(repositorio).registrar(any(), any(), any());
    }

    @Test
    void dadaUnaClaveRepetida_cuandoSeRegistra_entoncesCargoRepetidoSinGuardarNiRegistrar() {
        when(repositorio.buscar(anyString(), any(), any()))
                .thenReturn(Optional.of(coordinador(EstadoDeCargo.VIGENTE)));

        assertThatThrownBy(() -> servicio.registrar(coordinador(EstadoDeCargo.VIGENTE), QUIEN))
                .isInstanceOf(CargoRepetido.class);
        verify(repositorio, never()).registrar(any(), any(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaCarreraConCoordinadorVigente_cuandoSeRegistraOtro_entoncesCoordinadorVigente() {
        when(repositorio.hayCoordinadorVigente(CARRERA, null)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(coordinador(EstadoDeCargo.VIGENTE), QUIEN))
                .isInstanceOf(CoordinadorVigente.class);
        verify(repositorio, never()).registrar(any(), any(), any());
    }

    @Test
    void dadoUnDocenteEnLicenciaODeMedioTiempo_cuandoRecibeUnCargo_entoncesNoEstaHabilitado() {
        when(docentes.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(
                        Optional.of(
                                CargasDePrueba.docente(true).conEstado(EstadoLaboral.LICENCIA)));
        assertThatThrownBy(() -> servicio.registrar(coordinador(EstadoDeCargo.VIGENTE), QUIEN))
                .isInstanceOf(DocenteNoHabilitado.class);

        when(docentes.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(CargasDePrueba.docente(true, Dedicacion.MT)));
        assertThatThrownBy(() -> servicio.registrar(coordinador(EstadoDeCargo.FINALIZADO), QUIEN))
                .isInstanceOf(DocenteNoHabilitado.class);
        verify(repositorio, never()).registrar(any(), any(), any());
    }

    @Test
    void dadoUnDocenteQueNoEstaRegistrado_cuandoRecibeUnCargo_entoncesDatoInvalidoEnDocente() {
        when(docentes.buscar(DocentesDePrueba.CEDULA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrar(coordinador(EstadoDeCargo.VIGENTE), QUIEN))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("docente");
    }

    @Test
    void dadoUnCargoVigente_cuandoSeFinaliza_entoncesSeGuardaAunqueElDocenteYaNoEsteHabilitado() {
        when(repositorio.buscar(anyString(), any(), any()))
                .thenReturn(Optional.of(coordinador(EstadoDeCargo.VIGENTE)));
        when(docentes.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(
                        Optional.of(
                                CargasDePrueba.docente(true).conEstado(EstadoLaboral.INACTIVO)));
        Cargo finalizado =
                new Cargo(
                        DocentesDePrueba.CEDULA,
                        TipoDeCargo.COORDINADOR_CARRERA,
                        LocalDate.of(2025, 4, 1),
                        null,
                        CARRERA,
                        LocalDate.of(2026, 9, 30),
                        "RES-PRUEBA-2025-004",
                        EstadoDeCargo.FINALIZADO);

        CargoConsultado corregido = servicio.corregir(finalizado, QUIEN);

        assertThat(corregido.cargo()).isEqualTo(finalizado);
        assertThat(corregido.advertencias()).isEmpty();
        verify(repositorio).guardar(finalizado);
        verify(repositorio, never()).hayCoordinadorVigente(anyString(), any());
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro).registrar(cambio.capture());
        assertThat(cambio.getValue().accion()).isEqualTo(CambioRegistrado.Accion.CARGO_CORREGIDO);
        assertThat(cambio.getValue().detalle()).containsKey("cambios");
    }

    @Test
    void
            dadoUnCoordinadorFinalizado_cuandoVuelveAVigenteYLaCarreraTieneOtro_entoncesCoordinadorVigente() {
        Cargo finalizado = coordinador(EstadoDeCargo.FINALIZADO);
        when(repositorio.buscar(anyString(), any(), any())).thenReturn(Optional.of(finalizado));
        when(repositorio.hayCoordinadorVigente(CARRERA, finalizado)).thenReturn(true);

        assertThatThrownBy(() -> servicio.corregir(coordinador(EstadoDeCargo.VIGENTE), QUIEN))
                .isInstanceOf(CoordinadorVigente.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void dadoUnCargoSinCambios_cuandoSeCorrige_entoncesNoGuardaNiRegistra() {
        when(repositorio.buscar(anyString(), any(), any()))
                .thenReturn(Optional.of(coordinador(EstadoDeCargo.VIGENTE)));

        servicio.corregir(coordinador(EstadoDeCargo.VIGENTE), QUIEN);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoUnCargoQueNoExiste_cuandoSeCorrige_entoncesCargoNoEncontrado() {
        assertThatThrownBy(() -> servicio.corregir(coordinador(EstadoDeCargo.VIGENTE), QUIEN))
                .isInstanceOf(CargoNoEncontrado.class);
    }

    private static Cargo coordinador(EstadoDeCargo estado) {
        return new Cargo(
                DocentesDePrueba.CEDULA,
                TipoDeCargo.COORDINADOR_CARRERA,
                LocalDate.of(2025, 4, 1),
                null,
                CARRERA,
                null,
                "RES-PRUEBA-2025-004",
                estado);
    }
}
