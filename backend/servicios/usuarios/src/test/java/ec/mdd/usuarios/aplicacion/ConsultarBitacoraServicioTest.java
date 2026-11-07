package ec.mdd.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.mdd.usuarios.dominio.FiltroDeBitacora;
import ec.mdd.usuarios.dominio.FiltroDeBitacoraInvalido;
import ec.mdd.usuarios.dominio.PaginaPedida;
import ec.mdd.usuarios.puertos.entrada.AccionConocida;
import ec.mdd.usuarios.puertos.entrada.ConsultarBitacora.Filtro;
import ec.mdd.usuarios.puertos.entrada.EntradaConsultada;
import ec.mdd.usuarios.puertos.entrada.PaginaDeEntradas;
import ec.mdd.usuarios.puertos.salida.RepositorioBitacora;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** La consulta de la bitácora (F17-H02, FR-001 a FR-006), con Mockito para el repositorio. */
class ConsultarBitacoraServicioTest {

    private static final Instant AHORA = Instant.parse("2026-11-07T20:00:00Z");

    private final RepositorioBitacora repositorio = mock(RepositorioBitacora.class);
    private final ConsultarBitacoraServicio servicio =
            new ConsultarBitacoraServicio(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void dadoUnFiltroYSinPagina_cuandoSeConsulta_entoncesPideLaPrimeraDe50ConCorteAhora() {
        PaginaDeEntradas pagina = new PaginaDeEntradas(List.of(entrada()), 1, 1, 50, AHORA);
        when(repositorio.consultar(any(), any())).thenReturn(pagina);

        PaginaDeEntradas respuesta =
                servicio.consultar(
                        new Filtro(
                                LocalDate.parse("2026-11-07"),
                                LocalDate.parse("2026-11-07"),
                                "coordinacion.demo",
                                null,
                                null,
                                null,
                                null),
                        null,
                        null,
                        null);

        ArgumentCaptor<FiltroDeBitacora> filtro = ArgumentCaptor.forClass(FiltroDeBitacora.class);
        ArgumentCaptor<PaginaPedida> pedida = ArgumentCaptor.forClass(PaginaPedida.class);
        verify(repositorio).consultar(filtro.capture(), pedida.capture());
        assertThat(filtro.getValue().usuario()).contains("coordinacion.demo");
        assertThat(filtro.getValue().desde()).contains(Instant.parse("2026-11-07T05:00:00Z"));
        assertThat(pedida.getValue()).isEqualTo(new PaginaPedida(1, 50, AHORA));
        assertThat(respuesta).isSameAs(pagina);
    }

    @Test
    void dadoElCorteDeLaPrimeraRespuesta_cuandoSePideLaSegunda_entoncesLoConserva() {
        Instant corte = AHORA.minusSeconds(120);
        when(repositorio.consultar(any(), any()))
                .thenReturn(new PaginaDeEntradas(List.of(), 0, 2, 25, corte));

        servicio.consultar(sinFiltros(), 2, 25, corte);

        ArgumentCaptor<PaginaPedida> pedida = ArgumentCaptor.forClass(PaginaPedida.class);
        verify(repositorio).consultar(any(), pedida.capture());
        assertThat(pedida.getValue()).isEqualTo(new PaginaPedida(2, 25, corte));
    }

    @Test
    void dadoUnaEntrada_cuandoSeConsulta_entoncesLaDevuelveTalComoLaGuardoElRepositorio() {
        EntradaConsultada entrada = entrada();
        when(repositorio.consultar(any(), any()))
                .thenReturn(new PaginaDeEntradas(List.of(entrada), 1, 1, 50, AHORA));

        assertThat(servicio.consultar(sinFiltros(), null, null, null).entradas())
                .containsExactly(entrada);
    }

    @Test
    void dadoUnFiltroInvalido_cuandoSeConsulta_entoncesFallaSinLlamarAlRepositorio() {
        Filtro alReves =
                new Filtro(
                        LocalDate.parse("2026-11-07"),
                        LocalDate.parse("2026-11-06"),
                        null,
                        null,
                        null,
                        null,
                        null);

        assertThatThrownBy(() -> servicio.consultar(alReves, null, null, null))
                .isInstanceOf(FiltroDeBitacoraInvalido.class);
        verifyNoInteractions(repositorio);
    }

    @Test
    void dadoUnaPaginaInvalida_cuandoSeConsulta_entoncesFallaSinLlamarAlRepositorio() {
        assertThatThrownBy(() -> servicio.consultar(sinFiltros(), 1, 101, null))
                .isInstanceOf(FiltroDeBitacoraInvalido.class);
        verifyNoInteractions(repositorio);
    }

    @Test
    void dadoLaBitacora_cuandoSePidenLasAcciones_entoncesDevuelveLasDelRepositorio() {
        List<AccionConocida> acciones =
                List.of(new AccionConocida("generacion", "ACTIVIDAD_GUARDADA"));
        when(repositorio.acciones()).thenReturn(acciones);

        assertThat(servicio.acciones()).isEqualTo(acciones);
    }

    private static Filtro sinFiltros() {
        return new Filtro(null, null, null, null, null, null, null);
    }

    private static EntradaConsultada entrada() {
        return new EntradaConsultada(
                UUID.fromString("6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f"),
                "docentes",
                "datos.demo",
                "DOCENTE_CORREGIDO",
                Instant.parse("2026-11-07T15:00:00Z"),
                Optional.empty(),
                Map.of("identificacion", "0102030405", "camposPersonales", List.of("telefono")),
                Optional.of(Map.of()),
                Optional.of(Map.of()));
    }
}
