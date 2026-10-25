package ec.mdd.docentes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.docentes.dominio.DatosDelDocente;
import ec.mdd.docentes.dominio.Dedicacion;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.DocentesDePrueba;
import ec.mdd.docentes.dominio.EstadoLaboral;
import ec.mdd.docentes.dominio.Identificacion;
import ec.mdd.docentes.dominio.TipoDeIdentificacion;
import ec.mdd.docentes.puertos.entrada.CorreoRepetido;
import ec.mdd.docentes.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.docentes.puertos.entrada.DocenteRepetido;
import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.docentes.puertos.salida.ReferenciasAcademicas;
import ec.mdd.docentes.puertos.salida.RegistroDeCambios;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Mantener los docentes (FR-003 a FR-006, FR-010): las referencias se verifican antes de la
 * transacción; dentro, los docentes se bloquean, se deciden los repetidos, se guarda y se registra.
 * Con Mockito.
 */
class MantenerDocentesServicioTest {

    private static final String QUIEN = "datos.demo";
    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final Identificacion CEDULA =
            new Identificacion(TipoDeIdentificacion.CEDULA, DocentesDePrueba.CEDULA);

    private final RepositorioDocentes repositorio = mock(RepositorioDocentes.class);
    private final ReferenciasAcademicas referencias = mock(ReferenciasAcademicas.class);
    private final RegistroDeCambios registro = mock(RegistroDeCambios.class);
    private final TransaccionDePrueba transaccion = new TransaccionDePrueba();
    private final MantenerDocentesServicio servicio =
            new MantenerDocentesServicio(
                    repositorio,
                    referencias,
                    registro,
                    transaccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC));

    @BeforeEach
    void datos() {
        when(referencias.referencias()).thenReturn(DocentesDePrueba.referencias());
        when(repositorio.buscar(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void dadoUnDocenteNuevo_cuandoSeRegistra_entoncesBloqueaGuardaYRegistraConSuIdentificacion() {
        Docente registrado = servicio.registrar(CEDULA, DocentesDePrueba.datos(), QUIEN);

        assertThat(registrado.estado()).isEqualTo(EstadoLaboral.ACTIVO);
        InOrder orden = inOrder(repositorio, registro);
        orden.verify(repositorio).bloquear();
        orden.verify(repositorio).registrar(registrado, QUIEN, AHORA);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.DOCENTE_REGISTRADO);
        assertThat(cambio.usuario()).isEqualTo(QUIEN);
        assertThat(cambio.detalle())
                .containsEntry("identificacion", DocentesDePrueba.CEDULA)
                .containsEntry("carreraBase", "C1")
                .containsEntry("dedicacion", "TC");
    }

    @Test
    void dadaUnaIdentificacionOUnCorreoRepetidos_cuandoSeRegistra_entoncesSeRechazaSinRegistro() {
        when(repositorio.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(DocentesDePrueba.docente()));
        assertThatThrownBy(() -> servicio.registrar(CEDULA, DocentesDePrueba.datos(), QUIEN))
                .isInstanceOf(DocenteRepetido.class)
                .hasMessageNotContaining(DocentesDePrueba.CEDULA);

        when(repositorio.buscar(DocentesDePrueba.CEDULA)).thenReturn(Optional.empty());
        when(repositorio.existeCorreo(any(), isNull())).thenReturn(true);
        assertThatThrownBy(() -> servicio.registrar(CEDULA, DocentesDePrueba.datos(), QUIEN))
                .isInstanceOf(CorreoRepetido.class)
                .hasMessageNotContaining("juan.peralta");

        verify(repositorio, never()).registrar(any(), anyString(), any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadaUnaCarreraBaseInexistente_cuandoSeRegistra_entoncesDatoInvalidoAntesDeLaTransaccion() {
        DatosDelDocente sinCarrera =
                DocentesDePrueba.con(
                        v -> {
                            v.carrera = "NO-EXISTE";
                            return v;
                        });

        assertThatThrownBy(() -> servicio.registrar(CEDULA, sinCarrera, QUIEN))
                .isInstanceOf(DatoInvalido.class)
                .extracting("campo")
                .isEqualTo("carreraBase");
        verify(repositorio, never()).bloquear();
        verify(repositorio, never()).registrar(any(), anyString(), any());
    }

    @Test
    void dadoUnDocente_cuandoSeCorrige_entoncesGuardaYRegistraCadaCambio() {
        when(repositorio.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(DocentesDePrueba.docente()));
        DatosDelDocente medioTiempo =
                DocentesDePrueba.con(
                        v -> {
                            v.dedicacion = Dedicacion.MT;
                            v.horas = 20;
                            return v;
                        });

        Docente corregido = servicio.corregir(DocentesDePrueba.CEDULA, medioTiempo, QUIEN);

        assertThat(corregido.datos().dedicacion()).isEqualTo(Dedicacion.MT);
        verify(repositorio).guardar(corregido);
        verify(repositorio).existeCorreo(medioTiempo.correo(), DocentesDePrueba.CEDULA);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.DOCENTE_CORREGIDO);
        assertThat(cambio.detalle().get("cambios"))
                .isEqualTo(
                        List.of(
                                Map.of("campo", "dedicacion", "anterior", "TC", "nuevo", "MT"),
                                Map.of(
                                        "campo",
                                        "horasContratadas",
                                        "anterior",
                                        "40",
                                        "nuevo",
                                        "20")));
    }

    @Test
    void dadoUnDocenteSinCambios_cuandoSeCorrige_entoncesNoGuardaNiRegistra() {
        when(repositorio.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(DocentesDePrueba.docente()));

        servicio.corregir(DocentesDePrueba.CEDULA, DocentesDePrueba.datos(), QUIEN);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    @Test
    void dadoElCorreoDeOtroDocente_cuandoSeCorrige_entoncesCorreoRepetido() {
        when(repositorio.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(DocentesDePrueba.docente()));
        when(repositorio.existeCorreo(eq("otro@prueba.edu.ec"), eq(DocentesDePrueba.CEDULA)))
                .thenReturn(true);
        DatosDelDocente conOtroCorreo =
                DocentesDePrueba.con(
                        v -> {
                            v.correo = "otro@prueba.edu.ec";
                            return v;
                        });

        assertThatThrownBy(() -> servicio.corregir(DocentesDePrueba.CEDULA, conOtroCorreo, QUIEN))
                .isInstanceOf(CorreoRepetido.class);
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoSeCorrigeOCambiaDeEstado_entoncesDocenteNoEncontrado() {
        assertThatThrownBy(() -> servicio.corregir("0109990028", DocentesDePrueba.datos(), QUIEN))
                .isInstanceOf(DocenteNoEncontrado.class);
        assertThatThrownBy(
                        () -> servicio.cambiarEstado("0109990028", EstadoLaboral.INACTIVO, QUIEN))
                .isInstanceOf(DocenteNoEncontrado.class);
    }

    @Test
    void dadoUnDocenteActivo_cuandoCambiaDeEstado_entoncesGuardaYRegistraElAnteriorYElNuevo() {
        when(repositorio.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(DocentesDePrueba.docente()));

        Docente enLicencia =
                servicio.cambiarEstado(DocentesDePrueba.CEDULA, EstadoLaboral.LICENCIA, QUIEN);

        assertThat(enLicencia.estado()).isEqualTo(EstadoLaboral.LICENCIA);
        verify(repositorio).guardar(enLicencia);
        CambioRegistrado cambio = registrado();
        assertThat(cambio.accion()).isEqualTo(Accion.DOCENTE_ESTADO_CAMBIADO);
        assertThat(cambio.detalle())
                .containsEntry("anterior", "ACTIVO")
                .containsEntry("nuevo", "LICENCIA");
    }

    @Test
    void dadoElMismoEstado_cuandoSeCambia_entoncesNoGuardaNiRegistra() {
        when(repositorio.buscar(DocentesDePrueba.CEDULA))
                .thenReturn(Optional.of(DocentesDePrueba.docente()));

        servicio.cambiarEstado(DocentesDePrueba.CEDULA, EstadoLaboral.ACTIVO, QUIEN);

        verify(repositorio, never()).guardar(any());
        verify(registro, never()).registrar(any());
    }

    private CambioRegistrado registrado() {
        ArgumentCaptor<CambioRegistrado> cambio = ArgumentCaptor.forClass(CambioRegistrado.class);
        verify(registro).registrar(cambio.capture());
        return cambio.getValue();
    }
}
