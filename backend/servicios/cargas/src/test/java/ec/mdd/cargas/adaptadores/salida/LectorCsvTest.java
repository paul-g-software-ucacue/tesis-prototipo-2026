package ec.mdd.cargas.adaptadores.salida;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.CodificacionNoAdmitida;
import ec.mdd.cargas.puertos.salida.FormatoNoAdmitido;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * El lector de CSV (research §4): UTF-8 con o sin BOM, coma o punto y coma, RFC 4180, con los 10
 * archivos de prueba de la ERS.
 */
class LectorCsvTest {

    private static final DiccionarioDeDatos DICCIONARIO = new DiccionarioYaml().leer();

    private final LectorCsv lector = new LectorCsv();

    @Test
    void dadosLosDiezArchivosDePrueba_cuandoSeLeen_entoncesSusEncabezadosYSusRegistros() {
        for (Map.Entry<String, Integer> archivo : DatosDePrueba.REGISTROS.entrySet()) {
            TablaDeDatos tabla = lector.leer(DatosDePrueba.contenido(archivo.getKey()));
            Carga carga = cargaDe(archivo.getKey());

            assertThat(carga.compararEncabezados(tabla.encabezados()))
                    .as(archivo.getKey())
                    .isEmpty();
            assertThat(tabla.registros()).as(archivo.getKey()).isEqualTo(archivo.getValue());
            assertThat(carga.leer(tabla)).isEqualTo(archivo.getValue());
        }
    }

    @Test
    void dadoElMismoArchivoConBomYCrlf_cuandoSeLee_entoncesDaLoMismo() {
        // Los archivos de prueba de la ERS tienen CRLF.
        String texto = new String(DatosDePrueba.contenido("06_docentes.csv"), UTF_8);
        byte[] conLf = texto.replace("\r\n", "\n").getBytes(UTF_8);
        byte[] conBomYCrlf = ("\uFEFF" + texto).getBytes(UTF_8);

        TablaDeDatos uno = lector.leer(conLf);
        TablaDeDatos otro = lector.leer(conBomYCrlf);

        assertThat(otro).isEqualTo(uno);
        assertThat(uno.registros()).isEqualTo(47);
        assertThat(otro.encabezados().getFirst()).isEqualTo("tipo_identificacion");
    }

    @Test
    void dadoUnArchivoConPuntoYComa_cuandoSeLee_entoncesLoDetecta() {
        TablaDeDatos coma = lector.leer(bytes("codigo,nombre\nC1,\"Uno; dos\"\nC2,Tres\n"));
        TablaDeDatos puntoYComa = lector.leer(bytes("codigo;nombre\nC1;\"Uno; dos\"\nC2;Tres\n"));

        assertThat(puntoYComa).isEqualTo(coma);
        assertThat(coma.filas().getFirst()).containsExactly("C1", "Uno; dos");
    }

    @Test
    void dadoUnValorEntreComillas_cuandoSeLee_entoncesQuedaEnUnaCelda() {
        TablaDeDatos tabla =
                lector.leer(bytes("codigo,nombre\nC1,\"Peñafiel, \"\"el\"\"\nMuñoz\"\r\n"));

        assertThat(tabla.filas()).containsExactly(List.of("C1", "Peñafiel, \"el\"\nMuñoz"));
    }

    @Test
    void dadasLineasVaciasOSoloConSeparadores_cuandoSeLeen_entoncesSonFilasVacias() {
        TablaDeDatos tabla = lector.leer(bytes("a;b;c\n1;2;3\n\n;;\n;\n4;5;6\n\n;;;\n"));

        assertThat(tabla.filas())
                .containsExactly(
                        List.of("1", "2", "3"),
                        List.of("", "", ""),
                        List.of("", "", ""),
                        List.of("", "", ""),
                        List.of("4", "5", "6"),
                        List.of("", "", ""),
                        List.of("", "", ""));
        assertThat(tabla.registros()).isEqualTo(2);
    }

    @Test
    void dadoUnArchivoVacioOSoloConElBom_cuandoSeLee_entoncesUnaTablaVacia() {
        assertThat(lector.leer(new byte[0])).isEqualTo(new TablaDeDatos(List.of(), List.of()));
        assertThat(lector.leer(bytes("\uFEFF"))).isEqualTo(new TablaDeDatos(List.of(), List.of()));
    }

    @Test
    void dadoUnCsvEnWindows1252ConTildes_cuandoSeLee_entoncesCodificacionNoAdmitida() {
        byte[] latino = "codigo,apellido\nC1,Peñafiel\n".getBytes(Charset.forName("windows-1252"));

        assertThatThrownBy(() -> lector.leer(latino)).isInstanceOf(CodificacionNoAdmitida.class);
    }

    @Test
    void dadoUnCsvQueEsOtroFormatoRenombrado_cuandoSeLee_entoncesFormatoNoAdmitido() {
        byte[] pdf = "%PDF-1.7\n1 0 obj".getBytes(UTF_8);
        byte[] zip = {'P', 'K', 3, 4, 20, 0, 0, 0};
        byte[] ole2 = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
        };
        byte[] conNulo = {'a', ',', 'b', '\n', '1', 0, ',', '2', '\n'};

        for (byte[] contenido : List.of(pdf, zip, ole2, conNulo)) {
            assertThatThrownBy(() -> lector.leer(contenido)).isInstanceOf(FormatoNoAdmitido.class);
        }
    }

    @Test
    void dadasComillasSinCerrar_cuandoSeLee_entoncesIlegibleConSuFila() {
        assertThatThrownBy(() -> lector.leer(bytes("a,b\n1,2\n3,\"cuatro\n5,6\n")))
                .isInstanceOf(ArchivoIlegible.class)
                .hasMessage("La fila 3 tiene comillas sin cerrar.");
    }

    @Test
    void dadaUnaFilaConOtroNumeroDeValores_cuandoSeLee_entoncesIlegibleConSuFilaPeroNoUnaVacia() {
        assertThatThrownBy(() -> lector.leer(bytes("a,b,c\n1,2,3\n4,5\n")))
                .isInstanceOf(ArchivoIlegible.class)
                .hasMessage("La fila 3 tiene 2 valores y los encabezados, 3.");
        assertThat(lector.leer(bytes("a,b,c\n1,2,3\n\n4,5,6\n")).registros()).isEqualTo(2);
    }

    @Test
    void dadoTextoDespuesDeCerrarLasComillas_cuandoSeLee_entoncesIlegible() {
        assertThatThrownBy(() -> lector.leer(bytes("a,b\n\"1\"x,2\n")))
                .isInstanceOf(ArchivoIlegible.class)
                .hasMessageContaining("fila 2");
    }

    private static Carga cargaDe(String archivo) {
        return DICCIONARIO.cargas().stream()
                .filter(carga -> carga.archivo().equals(archivo))
                .findFirst()
                .orElseThrow();
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(UTF_8);
    }
}
