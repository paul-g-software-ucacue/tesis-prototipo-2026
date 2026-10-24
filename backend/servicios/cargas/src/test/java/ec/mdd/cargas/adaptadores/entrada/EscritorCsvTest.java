package ec.mdd.cargas.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El CSV de las plantillas y los diccionarios (research §6): RFC 4180, UTF-8 con BOM, sin fórmulas.
 */
class EscritorCsvTest {

    @Test
    void dadasDosFilas_cuandoSeEscriben_entoncesEmpiezaConElBomYSeparaConComaYCrlf() {
        String csv = EscritorCsv.escribir(List.of(List.of("a", "b"), List.of("c", "")));

        assertThat(csv).isEqualTo("\uFEFFa,b\r\nc,\r\n");
        assertThat(csv.getBytes(StandardCharsets.UTF_8))
                .startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
    }

    @Test
    void dadoUnValorConComaComillasOSaltoDeLinea_cuandoSeEscribe_entoncesVaEntreComillas() {
        String csv =
                EscritorCsv.escribir(
                        List.of(List.of("1, 2", "dice \"sí\"", "dos\nlíneas", "simple")));

        assertThat(csv).isEqualTo("\uFEFF\"1, 2\",\"dice \"\"sí\"\"\",\"dos\nlíneas\",simple\r\n");
    }

    @Test
    void dadoUnValorQueEmpiezaComoFormula_cuandoSeEscribe_entoncesLlevaUnApostrofo() {
        String csv = EscritorCsv.escribir(List.of(List.of("=1+1", "+57", "-3", "@SUMA", "a=b")));

        assertThat(csv).isEqualTo("\uFEFF'=1+1,'+57,'-3,'@SUMA,a=b\r\n");
    }

    @Test
    void dadosTextosConTildesYEnie_cuandoSeEscriben_entoncesSalenEnUtf8() {
        byte[] bytes =
                EscritorCsv.escribir(List.of(List.of("Peñafiel Muñoz", "Gestión")))
                        .getBytes(StandardCharsets.UTF_8);

        assertThat(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8))
                .isEqualTo("Peñafiel Muñoz,Gestión\r\n");
    }
}
