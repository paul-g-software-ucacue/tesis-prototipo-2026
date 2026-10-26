package ec.mdd.cargas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.dominio.EncabezadosDistintos;
import ec.mdd.cargas.dominio.InformeValidacionCarga;
import ec.mdd.cargas.dominio.ProblemaDeFila;
import ec.mdd.cargas.dominio.ReferenciasExistentes;
import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.dominio.VistaPrevia;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.Formato;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import ec.mdd.plataforma.cargas.VistaPreviaDelLote;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Validar el archivo de una carga (F01-H02), con Mockito sobre el lector y el diccionario real. */
class ValidarArchivoServicioTest {

    static final List<String> CICLOS =
            List.of(
                    "codigo_periodo",
                    "nombre",
                    "fecha_inicio",
                    "fecha_fin",
                    "semanas",
                    "fecha_limite_envio_senescyt",
                    "resolucion_aprobacion",
                    "estado");

    /** Una fila válida de la carga 03. */
    static final List<String> PERIODO =
            List.of(
                    "2026-I",
                    "PRIMER PERIODO",
                    "2026-04-01",
                    "2026-08-31",
                    "20",
                    "",
                    "",
                    "APROBADO");

    private final LectorDeArchivo lector = mock(LectorDeArchivo.class);
    private final FuenteDelDiccionario fuente = mock(FuenteDelDiccionario.class);
    private final ReferenciasDelSistema referencias = mock(ReferenciasDelSistema.class);
    private final ValidarArchivoServicio servicio;

    ValidarArchivoServicioTest() {
        when(fuente.leer()).thenReturn(new DiccionarioYaml().leer());
        when(referencias.existentes())
                .thenReturn(
                        new ReferenciasExistentes(
                                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()));
        when(dueno.cargas()).thenReturn(CARGAS);
        when(dueno.previsualizar(any()))
                .thenReturn(new VistaPreviaDelLote(List.of(), "0".repeat(64)));
        ConsultarPlantillasServicio plantillas = new ConsultarPlantillasServicio(fuente);
        servicio =
                new ValidarArchivoServicio(
                        plantillas,
                        lector,
                        referencias,
                        new DuenosDeLasCargas(List.of(dueno), CARGAS));
    }

    /** Los códigos de las 10 cargas del diccionario. */
    static final Set<String> CARGAS =
            new DiccionarioYaml()
                    .leer().cargas().stream()
                            .map(ec.mdd.cargas.dominio.Carga::codigo)
                            .collect(java.util.stream.Collectors.toSet());

    private final AplicadorDeCargas dueno = mock(AplicadorDeCargas.class);

    @Test
    void dadoUnArchivoValido_cuandoSeValida_entoncesSuCargaSuNombreSuFormatoYSuInforme() {
        when(lector.leer(eq("ciclos.csv"), any()))
                .thenReturn(
                        new ArchivoLeido(
                                Formato.CSV,
                                new TablaDeDatos(
                                        CICLOS,
                                        List.of(
                                                PERIODO,
                                                List.of("", "", "", "", "", "", "", "")))));

        ArchivoValidado validado =
                servicio.validar(
                        "ciclos_academicos",
                        new ArchivoSubido("C:\\Datos\\ciclos.csv", new byte[10]));

        assertThat(validado.informe()).isEqualTo(new InformeValidacionCarga(1, List.of()));
        assertThat(validado.carga()).isEqualTo("ciclos_academicos");
        assertThat(validado.archivo()).isEqualTo("ciclos.csv");
        assertThat(validado.formato()).isEqualTo(Formato.CSV);
        assertThat(validado.registros()).isOne();
        verify(referencias, times(1)).existentes();
    }

    @Test
    void dadoUnArchivoSinErrores_cuandoSeValida_entoncesLaVistaPreviaEsLaDelDueno() {
        when(lector.leer(eq("ciclos.csv"), any()))
                .thenReturn(
                        new ArchivoLeido(Formato.CSV, new TablaDeDatos(CICLOS, List.of(PERIODO))));
        VistaPreviaDelLote delDueno = new VistaPreviaDelLote(List.of(), "c".repeat(64));
        when(dueno.previsualizar(any())).thenReturn(delDueno);
        byte[] contenido = "contenido".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        ArchivoValidado validado =
                servicio.validar("ciclos_academicos", new ArchivoSubido("ciclos.csv", contenido));

        assertThat(validado.vistaPrevia())
                .isEqualTo(new VistaPrevia(0, java.util.Optional.of(delDueno)));
        ArgumentCaptor<LoteDeCarga> lote = ArgumentCaptor.forClass(LoteDeCarga.class);
        verify(dueno).previsualizar(lote.capture());
        assertThat(lote.getValue().carga()).isEqualTo("ciclos_academicos");
        assertThat(lote.getValue().hash()).isEqualTo(MotorDelLote.sha256(contenido));
        assertThat(lote.getValue().filas())
                .singleElement()
                .satisfies(
                        fila -> {
                            assertThat(fila.fila()).isEqualTo(2);
                            assertThat(fila.valor("codigo_periodo")).isEqualTo("2026-I");
                        });
    }

    @Test
    void dadaUnaFilaConErrores_cuandoSeValida_entoncesDevuelveElInformeSinLanzar() {
        when(lector.leer(eq("ciclos.csv"), any()))
                .thenReturn(
                        new ArchivoLeido(
                                Formato.CSV,
                                new TablaDeDatos(
                                        CICLOS,
                                        List.of(
                                                List.of(
                                                        "2026-III",
                                                        "PRIMER PERIODO",
                                                        "2026-02-30",
                                                        "2026-08-31",
                                                        "20",
                                                        "",
                                                        "",
                                                        "APROBADO")))));

        ArchivoValidado validado = validar("ciclos.csv");
        InformeValidacionCarga informe = validado.informe();

        assertThat(validado.vistaPrevia())
                .isEqualTo(new VistaPrevia(1, java.util.Optional.empty()));
        verify(dueno, never()).previsualizar(any());
        assertThat(informe.valido()).isFalse();
        assertThat(informe.problemas())
                .extracting(ProblemaDeFila::campo)
                .containsExactly("codigo_periodo", "fecha_inicio");
    }

    @Test
    void dadoUnNombreConRutaOLargo_cuandoSeSanea_entoncesSinRutaYHasta255() {
        assertThat(ArchivoSubido.sinRuta("/tmp/x/../carreras.csv")).isEqualTo("carreras.csv");
        assertThat(ArchivoSubido.sinRuta("C:\\a\\b.xlsx")).isEqualTo("b.xlsx");
        assertThat(ArchivoSubido.sinRuta("a".repeat(300) + ".csv")).hasSize(255).endsWith(".csv");
        assertThat(new ArchivoSubido("/x/y.csv", new byte[0]).nombre()).isEqualTo("y.csv");
    }

    @Test
    void dadaUnaCargaQueNoExiste_cuandoSeValida_entoncesCargaNoEncontradaSinLeer() {
        assertThatThrownBy(
                        () ->
                                servicio.validar(
                                        "comisiones", new ArchivoSubido("a.csv", new byte[1])))
                .isInstanceOf(CargaNoEncontrada.class);
        verify(lector, never()).leer(anyString(), any());
    }

    @Test
    void dadoUnArchivoDeMasDeCincoMegas_cuandoSeValida_entoncesDemasiadoGrandeSinLeer() {
        byte[] grande = new byte[ValidarArchivoServicio.MAXIMO + 1];

        assertThatThrownBy(() -> servicio.validar("docentes", new ArchivoSubido("d.csv", grande)))
                .isInstanceOf(ArchivoDemasiadoGrande.class);
        verify(lector, never()).leer(anyString(), any());
        assertThat(ValidarArchivoServicio.MAXIMO).isEqualTo(5 * 1024 * 1024);
    }

    @Test
    void dadosErroresDelLectorODelDominio_cuandoSeValida_entoncesPasanTalCual() {
        when(lector.leer(eq("ilegible.csv"), any())).thenThrow(new ArchivoIlegible("La fila 2."));
        when(lector.leer(eq("otro.csv"), any()))
                .thenReturn(
                        new ArchivoLeido(
                                Formato.CSV,
                                new TablaDeDatos(List.of("x"), List.of(List.of("1")))));
        when(lector.leer(eq("vacio.csv"), any()))
                .thenReturn(new ArchivoLeido(Formato.CSV, new TablaDeDatos(CICLOS, List.of())));

        assertThatThrownBy(() -> validar("ilegible.csv")).isInstanceOf(ArchivoIlegible.class);
        assertThatThrownBy(() -> validar("otro.csv")).isInstanceOf(EncabezadosDistintos.class);
        assertThatThrownBy(() -> validar("vacio.csv")).isInstanceOf(ArchivoSinRegistros.class);
        verify(referencias, never()).existentes();
    }

    private ArchivoValidado validar(String nombre) {
        return servicio.validar("ciclos_academicos", new ArchivoSubido(nombre, new byte[1]));
    }
}
