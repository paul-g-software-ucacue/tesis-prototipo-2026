package ec.mdd.cargas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.dominio.ArchivoSinRegistros;
import ec.mdd.cargas.dominio.EncabezadosDistintos;
import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.puertos.entrada.ArchivoDemasiadoGrande;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.Formato;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.LectorDeArchivo;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Validar el archivo de una carga (F01-H02), con Mockito sobre el lector y el diccionario real. */
class ValidarArchivoServicioTest {

    private static final List<String> CICLOS =
            List.of(
                    "codigo_periodo",
                    "nombre",
                    "fecha_inicio",
                    "fecha_fin",
                    "semanas",
                    "fecha_limite_envio_senescyt",
                    "resolucion_aprobacion",
                    "estado");

    private final LectorDeArchivo lector = mock(LectorDeArchivo.class);
    private final FuenteDelDiccionario fuente = mock(FuenteDelDiccionario.class);
    private final ValidarArchivoServicio servicio;

    ValidarArchivoServicioTest() {
        when(fuente.leer()).thenReturn(new DiccionarioYaml().leer());
        servicio = new ValidarArchivoServicio(new ConsultarPlantillasServicio(fuente), lector);
    }

    @Test
    void dadoUnArchivoValido_cuandoSeValida_entoncesSuCargaSuNombreSuFormatoYSusRegistros() {
        when(lector.leer(eq("ciclos.csv"), any()))
                .thenReturn(
                        new ArchivoLeido(
                                Formato.CSV,
                                new TablaDeDatos(
                                        CICLOS,
                                        List.of(
                                                List.of(
                                                        "2026-I", "a", "b", "c", "d", "e", "f",
                                                        "g"),
                                                List.of("", "", "", "", "", "", "", "")))));

        ArchivoValidado validado =
                servicio.validar(
                        "ciclos_academicos",
                        new ArchivoSubido("C:\\Datos\\ciclos.csv", new byte[10]));

        assertThat(validado)
                .isEqualTo(new ArchivoValidado("ciclos_academicos", "ciclos.csv", Formato.CSV, 1));
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
    }

    private ArchivoValidado validar(String nombre) {
        return servicio.validar("ciclos_academicos", new ArchivoSubido(nombre, new byte[1]));
    }
}
