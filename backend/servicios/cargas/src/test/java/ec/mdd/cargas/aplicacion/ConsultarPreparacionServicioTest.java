package ec.mdd.cargas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.dominio.CargaDelPeriodo;
import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.EstadoDeLaCarga;
import ec.mdd.cargas.dominio.MotivoDeRechazo;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.dominio.TipoDeLoQueFalta;
import ec.mdd.cargas.puertos.entrada.PeriodoParaElegir;
import ec.mdd.cargas.puertos.entrada.PreparacionConsultada;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.PeriodoAcademico;
import ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado;
import ec.mdd.cargas.puertos.salida.ProyectosDelPeriodo;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** El tablero de preparación de un periodo (F02-H02), con Mockito en los puertos. */
class ConsultarPreparacionServicioTest {

    private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00Z");
    private static final PeriodoAcademico PRIMERO =
            new PeriodoAcademico("2026-I", "Periodo 2026-I", LocalDate.of(2026, 4, 6), true, false);
    private static final PeriodoAcademico SEGUNDO =
            new PeriodoAcademico(
                    "2026-II", "Periodo 2026-II", LocalDate.of(2026, 10, 5), false, false);

    private final FuenteDelDiccionario fuente = mock(FuenteDelDiccionario.class);
    private final AplicadorDeCargas dueno = mock(AplicadorDeCargas.class);
    private final HistorialDeCargas historial = mock(HistorialDeCargas.class);
    private final ProyectosDelPeriodo proyectos = mock(ProyectosDelPeriodo.class);
    private final ReferenciasDelSistema referencias = mock(ReferenciasDelSistema.class);
    private final ConsultarPreparacionServicio servicio;

    ConsultarPreparacionServicioTest() {
        when(fuente.leer()).thenReturn(new DiccionarioYaml().leer());
        when(dueno.cargas()).thenReturn(ValidarArchivoServicioTest.CARGAS);
        when(dueno.tieneDatosEn(anyString(), anyString())).thenReturn(true);
        when(proyectos.hayAsignables(anyString())).thenReturn(true);
        when(referencias.periodos()).thenReturn(List.of(SEGUNDO, PRIMERO));
        servicio =
                new ConsultarPreparacionServicio(
                        new ConsultarPlantillasServicio(fuente),
                        new DuenosDeLasCargas(List.of(dueno), ValidarArchivoServicioTest.CARGAS),
                        historial,
                        proyectos,
                        referencias);
    }

    private static CargaDelPeriodo carga(PreparacionConsultada consultada, String codigo) {
        return consultada.tablero().cargas().stream()
                .filter(una -> una.codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void sinPeriodoPedido_esElQueEmpiezaMasTarde_yTraeLaListaParaElegir() {
        PreparacionConsultada consultada = servicio.preparacion(Optional.empty());

        assertThat(consultada.tablero().periodo()).contains("2026-II");
        assertThat(consultada.periodos())
                .containsExactly(
                        new PeriodoParaElegir("2026-II", "Periodo 2026-II"),
                        new PeriodoParaElegir("2026-I", "Periodo 2026-I"));
        assertThat(consultada.tablero().faltan())
                .extracting(falta -> falta.tipo())
                .as("2026-II no está abierto")
                .containsExactly(TipoDeLoQueFalta.PERIODO_SIN_ABRIR);
    }

    @Test
    void conTodoYElPeriodoAbierto_estaListo_yPreguntaLosDatosDeEsePeriodo() {
        PreparacionConsultada consultada = servicio.preparacion(Optional.of("2026-I"));

        assertThat(consultada.tablero().listo()).isTrue();
        org.mockito.Mockito.verify(dueno).tieneDatosEn("oferta_periodo", "2026-I");
        org.mockito.Mockito.verify(proyectos).hayAsignables("2026-I");
    }

    @Test
    void unPeriodoQueNoExiste_esPeriodoNoEncontrado() {
        assertThatThrownBy(() -> servicio.preparacion(Optional.of("2027-I")))
                .isInstanceOfSatisfying(
                        PeriodoNoEncontrado.class,
                        error -> assertThat(error.periodo()).isEqualTo("2027-I"));
    }

    @Test
    void sinPeriodos_elTableroDiceQueFaltanLosPeriodos() {
        when(referencias.periodos()).thenReturn(List.of());

        PreparacionConsultada consultada = servicio.preparacion(Optional.empty());

        assertThat(consultada.periodos()).isEmpty();
        assertThat(consultada.tablero().faltan())
                .extracting(falta -> falta.tipo())
                .containsExactly(TipoDeLoQueFalta.SIN_PERIODOS);
    }

    @Test
    void conErrores_sale_delUltimoIntentoDelHistorial_sinContarSinProyectos() {
        when(dueno.tieneDatosEn("docentes", "2026-I")).thenReturn(false);
        when(historial.deLaCarga("docentes"))
                .thenReturn(
                        List.of(
                                new EntradaDeHistorial(
                                        "docentes",
                                        "datos.demo",
                                        AHORA,
                                        "06_docentes.csv",
                                        "a".repeat(64),
                                        ResultadoDeHistorial.RECHAZADA,
                                        0,
                                        0,
                                        0,
                                        Optional.of(MotivoDeRechazo.CON_ERRORES))));

        CargaDelPeriodo docentes = carga(servicio.preparacion(Optional.of("2026-I")), "docentes");

        assertThat(docentes.estado()).isEqualTo(EstadoDeLaCarga.CON_ERRORES);
        assertThat(docentes.motivo()).contains(MotivoDeRechazo.CON_ERRORES);
    }

    @Test
    void losProyectos_cuentanSiAlgunoEsAsignable_oSiElPeriodoEstaSinProyectos() {
        when(proyectos.hayAsignables("2026-I")).thenReturn(false);
        assertThat(
                        carga(servicio.preparacion(Optional.of("2026-I")), "proyectos_practicas")
                                .estado())
                .isEqualTo(EstadoDeLaCarga.PENDIENTE);

        when(historial.sinProyectos("proyectos_practicas"))
                .thenReturn(
                        List.of(
                                EntradaDeHistorial.sinProyectos(
                                        "proyectos_practicas", "2026-II", "x", AHORA),
                                EntradaDeHistorial.sinProyectos(
                                        "proyectos_practicas", "2026-I", "x", AHORA)));
        assertThat(
                        carga(servicio.preparacion(Optional.of("2026-I")), "proyectos_practicas")
                                .estado())
                .isEqualTo(EstadoDeLaCarga.SIN_PROYECTOS);
        org.mockito.Mockito.verify(dueno, org.mockito.Mockito.never())
                .tieneDatosEn(org.mockito.ArgumentMatchers.eq("proyectos_practicas"), any());
    }
}
