package ec.mdd.normativa.aplicacion.cargas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.TablaCargaHoraria;
import ec.mdd.normativa.dominio.TipoIst;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import ec.mdd.plataforma.cargas.CambioDeCampo;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** El traductor de la carga 07, la política (F01-H04, research §7), con Mockito. */
class TraductorPoliticaTest {

    private static final String HASH = "a".repeat(64);
    private static final Regla RN_02 =
            new Regla(
                    "RN-02", Severidad.DURA, Map.of("horas_maximas_docente", new BigDecimal("40")));

    private final RepositorioVersiones repositorio = mock(RepositorioVersiones.class);
    private final CrearVersion crear = mock(CrearVersion.class);
    private final EditarVersion editar = mock(EditarVersion.class);
    private final TraductorPolitica politica = new TraductorPolitica(repositorio, crear, editar);

    private static VersionNormativa version(int numero, String codigo) {
        return new VersionNormativa(
                numero,
                codigo,
                "Política SENESCYT 2024",
                LocalDate.of(2024, 3, 11),
                TipoIst.T2,
                new TablaCargaHoraria(
                        List.of(
                                new FilaCargaHoraria(
                                        null,
                                        "DOCENTE_TC",
                                        40,
                                        22,
                                        18,
                                        24,
                                        18,
                                        0,
                                        null,
                                        Severidad.DURA,
                                        "POL 6",
                                        null),
                                new FilaCargaHoraria(
                                        null,
                                        "DOCENTE_MT",
                                        20,
                                        12,
                                        8,
                                        12,
                                        8,
                                        0,
                                        null,
                                        Severidad.DURA,
                                        "POL 6",
                                        null))),
                List.of(RN_02));
    }

    private static FilaDelLote fila(int numero, String codigo, String perfil, String clase) {
        Map<String, String> valores = new HashMap<>();
        valores.put("codigo_version_politica", codigo);
        valores.put("fecha_vigencia_desde", "2024-03-11");
        valores.put("tipo_ist", "TODOS");
        valores.put("perfil", perfil);
        boolean tc = perfil.equals("DOCENTE_TC");
        valores.put("dedicacion_total", tc ? "40" : "20");
        valores.put("horas_clase", clase);
        valores.put("horas_clase_min", tc ? "18" : "8");
        valores.put("horas_clase_max", tc ? "24" : "12");
        valores.put("horas_otras_docencia", tc ? "18" : "8");
        valores.put("horas_gestion", "0");
        valores.put("horas_investigacion_vinculacion_max", "");
        valores.put("referencia_normativa", "POL 6");
        return new FilaDelLote(numero, valores);
    }

    private static LoteDeCarga lote(FilaDelLote... filas) {
        return new LoteDeCarga("politica_carga_horaria", HASH, List.of(filas));
    }

    @Test
    void laMismaTablaEsIgual_y22_0EsIgualA22() {
        when(repositorio.todas()).thenReturn(List.of(version(1, "SENESCYT-2024-03")));

        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        politica,
                        lote(
                                fila(2, "SENESCYT-2024-03", "DOCENTE_TC", "22.0"),
                                fila(3, "SENESCYT-2024-03", "DOCENTE_MT", "12")));

        assertThat(vista.cuantos(TipoDeRegistro.IGUAL)).isEqualTo(2);
        assertThat(vista.registros().getFirst().clave())
                .isEqualTo("SENESCYT-2024-03 · TODOS · DOCENTE_MT");
    }

    @Test
    void unaVersionQueExiste_seEditaEnteraConSusParametros() {
        VersionNormativa uno = version(1, "SENESCYT-2024-03");
        when(repositorio.todas()).thenReturn(List.of(uno));
        LoteDeCarga cambiada =
                lote(
                        fila(2, "SENESCYT-2024-03", "DOCENTE_TC", "20"),
                        fila(3, "SENESCYT-2024-03", "DOCENTE_MT", "12"));
        VistaPreviaDelLote vista = MotorDelLote.previsualizar(politica, cambiada);
        assertThat(vista.registros())
                .filteredOn(r -> r.tipo() == TipoDeRegistro.CAMBIA)
                .singleElement()
                .satisfies(
                        r ->
                                assertThat(r.cambios())
                                        .containsExactly(
                                                new CambioDeCampo("horas_clase", "22", "20")));

        ResultadoDelLote resultado = politica.aplicar(cambiada, vista, "vicerrectorado.demo");

        assertThat(resultado).isEqualTo(new ResultadoDelLote(0, 1, 0));
        ArgumentCaptor<CambiosDeVersion> cambios = ArgumentCaptor.forClass(CambiosDeVersion.class);
        verify(editar).editar(eq(1), cambios.capture(), eq("vicerrectorado.demo"));
        assertThat(cambios.getValue().filas()).hasSize(2);
        assertThat(cambios.getValue().parametros()).containsKey("RN-02");
        verify(crear, never()).crear(anyInt(), any(), any());
    }

    @Test
    void unaVersionNueva_seCreaCopiandoLaUltimaYSeEdita() {
        VersionNormativa uno = version(1, "SENESCYT-2024-03");
        VersionNormativa dos = version(2, "SENESCYT-2026-01");
        when(repositorio.todas()).thenReturn(List.of(uno));
        when(crear.crear(eq(1), any(), eq("vicerrectorado.demo"))).thenReturn(dos);
        LoteDeCarga nueva =
                lote(
                        fila(2, "SENESCYT-2026-01", "DOCENTE_TC", "22"),
                        fila(3, "SENESCYT-2026-01", "DOCENTE_MT", "12"));

        ResultadoDelLote resultado =
                politica.aplicar(
                        nueva, MotorDelLote.previsualizar(politica, nueva), "vicerrectorado.demo");

        assertThat(resultado).isEqualTo(new ResultadoDelLote(2, 0, 0));
        verify(crear)
                .crear(
                        1,
                        new DatosDeVersion("SENESCYT-2026-01", "POL 6", LocalDate.of(2024, 3, 11)),
                        "vicerrectorado.demo");
        verify(editar).editar(eq(2), any(), eq("vicerrectorado.demo"));
    }

    @Test
    void unaVersionSinCambios_noSeEdita_yHorasConDecimalesSeRechazan() {
        when(repositorio.todas()).thenReturn(List.of(version(1, "SENESCYT-2024-03")));
        LoteDeCarga igual =
                lote(
                        fila(2, "SENESCYT-2024-03", "DOCENTE_TC", "22"),
                        fila(3, "SENESCYT-2024-03", "DOCENTE_MT", "12"));
        politica.aplicar(igual, MotorDelLote.previsualizar(politica, igual), "x");
        verify(editar, never()).editar(anyInt(), any(), any());

        LoteDeCarga conDecimales =
                lote(
                        fila(2, "SENESCYT-2024-03", "DOCENTE_TC", "22.5"),
                        fila(3, "SENESCYT-2024-03", "DOCENTE_MT", "12"));
        assertThatThrownBy(
                        () ->
                                politica.aplicar(
                                        conDecimales,
                                        MotorDelLote.previsualizar(politica, conDecimales),
                                        "x"))
                .isInstanceOfSatisfying(
                        RechazoDelLote.class,
                        rechazo -> {
                            assertThat(rechazo.fila()).isEqualTo(2);
                            assertThat(rechazo.campo()).contains("horas");
                        });
        assertThatThrownBy(() -> politica.registrar(igual.filas().getFirst(), "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
