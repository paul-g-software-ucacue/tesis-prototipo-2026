package ec.mdd.cargas.adaptadores.salida;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.cargas.puertos.salida.ArchivoLeido;
import ec.mdd.cargas.puertos.salida.Formato;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La extensión del archivo elige su lector, y el contenido se comprueba (research §3). */
class LectorPorFormatoTest {

    private final LectorPorFormato lector = new LectorPorFormato();

    private static final byte[] CSV = "codigo,nombre\nC1,Uno\n".getBytes(UTF_8);

    @Test
    void dadoUnCsv_cuandoSeLee_entoncesUsaElLectorDeCsv() {
        for (String nombre : List.of("carreras.csv", "CARRERAS.CSV", "carreras.Csv")) {
            ArchivoLeido leido = lector.leer(nombre, CSV);

            assertThat(leido.formato()).isEqualTo(Formato.CSV);
            assertThat(leido.tabla().encabezados()).containsExactly("codigo", "nombre");
        }
    }

    @Test
    void dadoUnXlsx_cuandoSeLee_entoncesUsaElLectorDeXlsx() {
        byte[] xlsx =
                LibroDePrueba.deTexto(List.of("codigo", "nombre"), List.of(List.of("C1", "Uno")));

        ArchivoLeido leido = lector.leer("carreras.XLSX", xlsx);

        assertThat(leido.formato()).isEqualTo(Formato.XLSX);
        assertThat(leido.tabla().filas()).containsExactly(List.of("C1", "Uno"));
    }
}
