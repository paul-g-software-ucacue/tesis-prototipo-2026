package ec.mdd.cargas.adaptadores.entrada;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.cargas.dominio.InformeValidacionCarga;
import ec.mdd.cargas.dominio.ProblemaDeFila;
import ec.mdd.cargas.dominio.TipoDeProblema;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** El informe en CSV, con las celdas neutralizadas (RF-09, RNF-SEG-04, SC-003). */
class EscritorDeInformeTest {

    @Test
    void dadoUnInformeConLaCedulaSinCero_entoncesLasColumnasYLaSugerencia() {
        String csv =
                EscritorDeInforme.escribir(
                        informe(
                                new ProblemaDeFila(
                                        2,
                                        "numero_identificacion",
                                        "109990010",
                                        "Cédula inválida o sin cero inicial: le falta el 0 inicial.",
                                        TipoDeProblema.ERROR,
                                        Optional.of("0109990010"))));

        assertThat(csv)
                .isEqualTo(
                        "﻿fila,campo,valor,motivo,tipo,sugerencia\r\n"
                                + "2,numero_identificacion,109990010,Cédula inválida o sin cero"
                                + " inicial: le falta el 0 inicial.,ERROR,0109990010\r\n");
        assertThat(csv.getBytes(StandardCharsets.UTF_8))
                .startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
    }

    @ParameterizedTest(name = "«{0}»")
    @ValueSource(strings = {"=1+1", "+57", "-1", "@SUMA(A1)", "\tx", "\rx"})
    void dadoUnValorQueEmpiezaComoFormula_entoncesLlevaUnApostrofo(String valor) {
        String csv = EscritorDeInforme.escribir(informe(error(valor, Optional.empty())));

        assertThat(csv).contains("'" + valor).doesNotContain("," + valor);
    }

    @Test
    void dadoUnHyperlinkConComillasYUnaSugerenciaNegativa_entoncesNeutralizadosYEntreComillas() {
        String csv =
                EscritorDeInforme.escribir(informe(error("=HYPERLINK(\"x\")", Optional.of("-2"))));

        assertThat(csv.split("\r\n")[1])
                .isEqualTo("2,c,\"'=HYPERLINK(\"\"x\"\")\",Revise el valor.,ERROR,'-2");
    }

    @Test
    void dadoUnValorConComaComillasYSaltoDeLinea_entoncesRfc4180() {
        String csv = EscritorDeInforme.escribir(informe(error("a, \"b\"\nc", Optional.empty())));

        assertThat(csv).endsWith("2,c,\"a, \"\"b\"\"\nc\",Revise el valor.,ERROR,\r\n");
    }

    @Test
    void sinProblemas_entoncesSoloLosEncabezados() {
        assertThat(EscritorDeInforme.escribir(new InformeValidacionCarga(5, List.of())))
                .isEqualTo("﻿fila,campo,valor,motivo,tipo,sugerencia\r\n");
    }

    private static ProblemaDeFila error(String valor, Optional<String> sugerencia) {
        return new ProblemaDeFila(
                2, "c", valor, "Revise el valor.", TipoDeProblema.ERROR, sugerencia);
    }

    private static InformeValidacionCarga informe(ProblemaDeFila problema) {
        return new InformeValidacionCarga(1, List.of(problema));
    }
}
