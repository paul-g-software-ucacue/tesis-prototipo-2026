package ec.mdd.cargas.aplicacion;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.mdd.cargas.adaptadores.entrada.AplicadorDePrueba;
import ec.mdd.cargas.adaptadores.salida.DiccionarioYaml;
import ec.mdd.cargas.adaptadores.salida.LectorPorFormato;
import ec.mdd.cargas.dominio.ReferenciasExistentes;
import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.ArchivoValidado;
import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.Formato;
import ec.mdd.cargas.puertos.salida.FuenteDeDatos;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * La fuente de datos es intercambiable (RF-57, F01-H05): los mismos registros de los ciclos
 * académicos, entregados por una fuente que no lee ningún archivo, dan el mismo informe y la misma
 * vista previa que el CSV leído por el adaptador de archivos.
 */
class FuenteDeDatosIntercambiableTest {

    private static final List<List<String>> PERIODOS =
            List.of(
                    List.of(
                            "2026-I",
                            "Periodo académico 2026-I",
                            "2026-04-06",
                            "2026-08-07",
                            "18",
                            "",
                            "RES-OCS-PRUEBA-2026-001",
                            "APROBADO"),
                    List.of(
                            "2026-II",
                            "Periodo académico 2026-II",
                            "2026-10-05",
                            "2027-02-05",
                            "18",
                            "",
                            "",
                            "PLANIFICADO"));

    /** Una fuente que no es un archivo, como lo sería Moodle o SIGA: ignora los bytes. */
    private static final FuenteDeDatos OTRA_FUENTE =
            (nombre, contenido) ->
                    new ArchivoLeido(
                            Formato.CSV,
                            new TablaDeDatos(ValidarArchivoServicioTest.CICLOS, PERIODOS));

    private static ValidarArchivoServicio servicio(FuenteDeDatos fuente) {
        FuenteDelDiccionario diccionario = mock(FuenteDelDiccionario.class);
        when(diccionario.leer()).thenReturn(new DiccionarioYaml().leer());
        ReferenciasDelSistema referencias = mock(ReferenciasDelSistema.class);
        when(referencias.existentes())
                .thenReturn(
                        new ReferenciasExistentes(
                                Set.of(), Set.of(), Set.of(), Set.of(), Set.of()));
        return new ValidarArchivoServicio(
                new ConsultarPlantillasServicio(diccionario),
                fuente,
                referencias,
                new DuenosDeLasCargas(
                        List.of(new AplicadorDePrueba()), ValidarArchivoServicioTest.CARGAS));
    }

    private static byte[] csv() {
        return Stream.concat(Stream.of(ValidarArchivoServicioTest.CICLOS), PERIODOS.stream())
                .map(fila -> String.join(",", fila))
                .collect(Collectors.joining("\r\n", "", "\r\n"))
                .getBytes(UTF_8);
    }

    @Test
    void losMismosRegistrosDeOtraFuente_danElMismoInformeYLaMismaVistaPrevia() {
        ArchivoSubido archivo = new ArchivoSubido("03_ciclos_academicos.csv", csv());

        ArchivoValidado delArchivo =
                servicio(new LectorPorFormato()).validar("ciclos_academicos", archivo);
        ArchivoValidado deOtraFuente = servicio(OTRA_FUENTE).validar("ciclos_academicos", archivo);

        assertThat(delArchivo.informe().valido()).isTrue();
        assertThat(delArchivo.registros()).isEqualTo(2);
        assertThat(deOtraFuente.informe()).isEqualTo(delArchivo.informe());
        assertThat(deOtraFuente.vistaPrevia()).isEqualTo(delArchivo.vistaPrevia());
    }
}
