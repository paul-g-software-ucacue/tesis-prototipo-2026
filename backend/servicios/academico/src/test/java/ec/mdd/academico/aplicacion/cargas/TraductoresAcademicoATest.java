package ec.mdd.academico.aplicacion.cargas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.academico.dominio.CampoConocimiento;
import ec.mdd.academico.dominio.Carrera;
import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.academico.dominio.EstadoDeCarrera;
import ec.mdd.academico.dominio.Jornada;
import ec.mdd.academico.dominio.Modalidad;
import ec.mdd.academico.dominio.NivelDeFormacion;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.plataforma.cargas.CambioDeCampo;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.TipoDeRegistro;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Los traductores de las cargas 01 y 02 (F01-H04, FR-012), con Mockito para los casos de uso. */
class TraductoresAcademicoATest {

    private static final String HASH = "a".repeat(64);

    private final RepositorioEstructura estructura = mock(RepositorioEstructura.class);
    private final MantenerEstructura mantenerEstructura = mock(MantenerEstructura.class);
    private final RepositorioCarreras carreras = mock(RepositorioCarreras.class);
    private final MantenerCarreras mantenerCarreras = mock(MantenerCarreras.class);
    private final TraductorEstructura unidades =
            new TraductorEstructura(estructura, mantenerEstructura);
    private final TraductorCarreras traductorCarreras =
            new TraductorCarreras(carreras, mantenerCarreras);

    private static FilaDelLote unidad(
            int fila, String codigo, String tipo, String padre, String estado) {
        Map<String, String> valores = new HashMap<>();
        valores.put("codigo_unidad", codigo);
        valores.put("nombre", "Unidad " + codigo);
        valores.put("tipo_unidad", tipo);
        valores.put("codigo_unidad_padre", padre);
        valores.put("codigo_gestion_autorizada", "");
        valores.put("es_comision_permanente", "");
        valores.put("estado", estado);
        return new FilaDelLote(fila, valores);
    }

    private static FilaDelLote carrera(int fila, String nombre, String jornadas, String ciclos) {
        Map<String, String> valores = new HashMap<>();
        valores.put("codigo_carrera", "550613A01-P-0301");
        valores.put("nombre", nombre);
        valores.put("siglas", "DS");
        valores.put("codigo_unidad_sede", "MATRIZ");
        valores.put("nivel_formacion", "TECNOLOGICO_SUPERIOR");
        valores.put("modalidad", "PRESENCIAL");
        valores.put("jornadas", jornadas);
        valores.put("campo_amplio", "06");
        valores.put("campo_especifico", "061");
        valores.put("campo_detallado", "0613");
        valores.put("numero_ciclos", ciclos);
        valores.put("estado", "VIGENTE");
        return new FilaDelLote(fila, valores);
    }

    private static Carrera desarrollo() {
        return Carrera.nueva(
                "550613A01-P-0301",
                "DESARROLLO DE SOFTWARE",
                "DS",
                "MATRIZ",
                NivelDeFormacion.TECNOLOGICO_SUPERIOR,
                Modalidad.PRESENCIAL,
                List.of(Jornada.MATUTINA, Jornada.NOCTURNA),
                new CampoConocimiento("06", "061"),
                "0613",
                5);
    }

    @Test
    void lasUnidadesNuevasSeAplicanConLosPadresAntesQueLosHijos() {
        when(estructura.todas()).thenReturn(List.of());
        List<FilaDelLote> filas =
                List.of(
                        unidad(2, "HIJA", "GESTION", "PADRE", "ACTIVO"),
                        unidad(3, "PADRE", "GOBIERNO", "MATRIZ", "ACTIVO"),
                        unidad(4, "MATRIZ", "SEDE_MATRIZ", "", "INACTIVO"));

        assertThat(unidades.enOrden(filas))
                .extracting(fila -> fila.valor("codigo_unidad"))
                .containsExactly("MATRIZ", "PADRE", "HIJA");
        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        unidades, new LoteDeCarga("estructura_organica", HASH, filas));
        assertThat(vista.cuantos(TipoDeRegistro.NUEVO)).isEqualTo(3);

        unidades.registrar(filas.get(2), "datos.demo");
        verify(mantenerEstructura)
                .registrar(
                        "MATRIZ",
                        "Unidad MATRIZ",
                        TipoDeUnidad.SEDE_MATRIZ,
                        null,
                        null,
                        null,
                        "datos.demo");
        verify(mantenerEstructura).cambiarEstado("MATRIZ", false, "datos.demo");
    }

    @Test
    void unaUnidadIgualNoCambia_yUnaInactivadaCambiaSoloDeEstado() {
        UnidadOrganica padre =
                UnidadOrganica.nueva(
                        "PADRE", "Unidad PADRE", TipoDeUnidad.GOBIERNO, "MATRIZ", null, null);
        when(estructura.todas()).thenReturn(List.of(padre));
        FilaDelLote igual = unidad(2, "PADRE", "GOBIERNO", "MATRIZ", "ACTIVO");
        FilaDelLote inactiva = unidad(2, "PADRE", "GOBIERNO", "MATRIZ", "INACTIVO");

        assertThat(
                        MotorDelLote.previsualizar(
                                        unidades,
                                        new LoteDeCarga(
                                                "estructura_organica", HASH, List.of(igual)))
                                .cuantos(TipoDeRegistro.IGUAL))
                .isOne();
        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        unidades, new LoteDeCarga("estructura_organica", HASH, List.of(inactiva)));
        assertThat(vista.registros().getFirst().cambios())
                .containsExactly(new CambioDeCampo("estado", "ACTIVO", "INACTIVO"));

        unidades.corregir(inactiva, padre, "datos.demo");
        verify(mantenerEstructura, never())
                .corregir(anyString(), anyString(), any(), any(), any(), anyString());
        verify(mantenerEstructura).cambiarEstado("PADRE", false, "datos.demo");
    }

    @Test
    void elTipoDeUnaUnidadNoCambia_yUnDatoInvalidoSeRechazaConSuCampo() {
        UnidadOrganica padre =
                UnidadOrganica.nueva(
                        "PADRE", "Unidad PADRE", TipoDeUnidad.GOBIERNO, "MATRIZ", null, null);
        FilaDelLote otroTipo = unidad(5, "PADRE", "GESTION", "MATRIZ", "ACTIVO");

        assertThatThrownBy(() -> unidades.corregir(otroTipo, padre, "datos.demo"))
                .isInstanceOf(DatoInvalido.class);
        RechazoDelLote rechazo =
                unidades.rechazo(otroTipo, new DatoInvalido("tipo_unidad", "No cambia."))
                        .orElseThrow();
        assertThat(rechazo.fila()).isEqualTo(5);
        assertThat(rechazo.campo()).contains("tipo_unidad");
        assertThat(unidades.rechazo(otroTipo, new IllegalStateException("otra"))).isEmpty();
    }

    @Test
    void unaCarreraConOtroNombreYOtrasJornadasCambiaEnEsosDosCampos() {
        when(carreras.carreras()).thenReturn(List.of(desarrollo()));
        FilaDelLote cambiada = carrera(2, "DESARROLLO DE SOFTWARE Y TIC", "NOCTURNA", "5");
        FilaDelLote igual = carrera(2, "DESARROLLO DE SOFTWARE", "MATUTINA;NOCTURNA", "05");

        assertThat(
                        MotorDelLote.previsualizar(
                                        traductorCarreras,
                                        new LoteDeCarga("carreras", HASH, List.of(igual)))
                                .cuantos(TipoDeRegistro.IGUAL))
                .as("«MATUTINA;NOCTURNA» y «05» son los mismos valores")
                .isOne();
        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        traductorCarreras, new LoteDeCarga("carreras", HASH, List.of(cambiada)));
        assertThat(vista.registros().getFirst().cambios())
                .containsExactly(
                        new CambioDeCampo(
                                "nombre", "DESARROLLO DE SOFTWARE", "DESARROLLO DE SOFTWARE Y TIC"),
                        new CambioDeCampo("jornadas", "MATUTINA; NOCTURNA", "NOCTURNA"));

        traductorCarreras.corregir(cambiada, desarrollo(), "datos.demo");
        verify(mantenerCarreras).corregirCarrera(eq("550613A01-P-0301"), any(), eq("datos.demo"));
        verify(mantenerCarreras, never()).cambiarEstadoCarrera(anyString(), any(), anyString());
    }

    @Test
    void unaCarreraNuevaNoVigenteSeRegistraYCambiaDeEstado() {
        Map<String, String> valores =
                new HashMap<>(carrera(2, "DESARROLLO DE SOFTWARE", "NOCTURNA", "5").valores());
        valores.put("estado", "EN_CIERRE");

        traductorCarreras.registrar(new FilaDelLote(2, valores), "datos.demo");

        verify(mantenerCarreras).registrarCarrera(any(), eq("datos.demo"));
        verify(mantenerCarreras)
                .cambiarEstadoCarrera("550613A01-P-0301", EstadoDeCarrera.EN_CIERRE, "datos.demo");
    }
}
