package ec.mdd.plataforma.cargas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * El motor del lote con un traductor en memoria, sin dobles (ADR-022): la comparación, la huella y
 * el todo o nada.
 */
class MotorDelLoteTest {

    private static final String HASH = "a".repeat(64);

    /** Un dueño en memoria: unidades con código, nombre y periodo; aplica sobre una copia. */
    static final class Unidades implements TraductorDeCarga<Map<String, String>> {

        final Map<String, Map<String, String>> datos = new LinkedHashMap<>();
        final List<String> aplicadas = new ArrayList<>();
        String rechazaLa = "";
        int bloqueos;

        @Override
        public String carga() {
            return "unidades";
        }

        @Override
        public void bloquear() {
            bloqueos++;
        }

        @Override
        public Map<String, Map<String, String>> existentes(LoteDeCarga lote) {
            List<String> periodos =
                    lote.filas().stream().map(fila -> fila.valor("periodo")).toList();
            Map<String, Map<String, String>> enAlcance = new LinkedHashMap<>();
            datos.forEach(
                    (clave, unidad) -> {
                        if (periodos.contains(unidad.get("periodo"))) {
                            enAlcance.put(clave, unidad);
                        }
                    });
            return enAlcance;
        }

        @Override
        public String clave(FilaDelLote fila) {
            return fila.valor("codigo");
        }

        @Override
        public Map<String, String> campos(Map<String, String> existente) {
            Map<String, String> campos = new LinkedHashMap<>();
            campos.put("nombre", existente.get("nombre"));
            campos.put("horas", existente.get("horas"));
            return campos;
        }

        @Override
        public Map<String, String> campos(FilaDelLote fila) {
            Map<String, String> campos = new LinkedHashMap<>();
            campos.put("nombre", fila.valor("nombre"));
            campos.put("horas", fila.valor("horas"));
            return campos;
        }

        @Override
        public void registrar(FilaDelLote fila, String quien) {
            aplicar(fila, "registrar");
        }

        @Override
        public void corregir(FilaDelLote fila, Map<String, String> actual, String quien) {
            aplicar(fila, "corregir");
        }

        private void aplicar(FilaDelLote fila, String como) {
            if (fila.valor("codigo").equals(rechazaLa)) {
                throw new IllegalStateException("Regla del dueño");
            }
            aplicadas.add(como + " " + fila.valor("codigo"));
        }

        void existe(String codigo, String nombre, String horas, String periodo) {
            datos.put(
                    codigo,
                    Map.of("codigo", codigo, "nombre", nombre, "horas", horas, "periodo", periodo));
        }
    }

    /** Una transacción en memoria: si el trabajo falla, deshace lo aplicado. */
    private static Function<Supplier<ResultadoDelLote>, ResultadoDelLote> transaccion(
            Unidades unidades) {
        return trabajo -> {
            List<String> antes = new ArrayList<>(unidades.aplicadas);
            try {
                return trabajo.get();
            } catch (RuntimeException error) {
                unidades.aplicadas.clear();
                unidades.aplicadas.addAll(antes);
                throw error;
            }
        };
    }

    private static FilaDelLote fila(int numero, String codigo, String nombre, String horas) {
        return new FilaDelLote(
                numero,
                Map.of("codigo", codigo, "nombre", nombre, "horas", horas, "periodo", "P1"));
    }

    private static LoteDeCarga lote(FilaDelLote... filas) {
        return new LoteDeCarga("unidades", HASH, List.of(filas));
    }

    private static Unidades conTresUnidades() {
        Unidades unidades = new Unidades();
        unidades.existe("B", "BIBLIOTECA", "4", "P1");
        unidades.existe("C", "CALIDAD", "2", "P1");
        unidades.existe("D", "DOCENCIA", "6", "P1");
        unidades.existe("Z", "OTRO PERIODO", "1", "P2");
        return unidades;
    }

    @Test
    void dadoUnLote_entoncesNuevosCambianBajasEIgualesEnOrdenDeClave() {
        Unidades unidades = conTresUnidades();

        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(
                        unidades,
                        lote(
                                fila(2, "A", "ADMISIONES", "3"),
                                fila(3, "B", "BIBLIOTECA", "4"),
                                fila(4, "C", "CALIDAD Y MEJORA", "2")));

        assertThat(vista.registros())
                .extracting(
                        RegistroDeLaVista::clave, RegistroDeLaVista::tipo, RegistroDeLaVista::fila)
                .containsExactly(
                        tuple("A", TipoDeRegistro.NUEVO, 2),
                        tuple("B", TipoDeRegistro.IGUAL, 3),
                        tuple("C", TipoDeRegistro.CAMBIA, 4),
                        tuple("D", TipoDeRegistro.BAJA, null));
        assertThat(vista.registros().get(2).cambios())
                .containsExactly(new CambioDeCampo("nombre", "CALIDAD", "CALIDAD Y MEJORA"));
        assertThat(vista.cuantos(TipoDeRegistro.BAJA))
                .as("la unidad de otro periodo está fuera del alcance")
                .isOne();
        assertThat(vista.huella()).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void laHuellaNoCambiaAlReordenarElArchivoYCambiaConUnValor() {
        Unidades unidades = conTresUnidades();
        FilaDelLote a = fila(2, "A", "ADMISIONES", "3");
        FilaDelLote c = fila(3, "C", "CALIDAD Y MEJORA", "2");

        String huella = MotorDelLote.previsualizar(unidades, lote(a, c)).huella();

        assertThat(
                        MotorDelLote.previsualizar(
                                        unidades,
                                        lote(
                                                fila(2, "C", "CALIDAD Y MEJORA", "2"),
                                                fila(3, "A", "ADMISIONES", "3")))
                                .huella())
                .isEqualTo(huella);
        assertThat(
                        MotorDelLote.previsualizar(
                                        unidades, lote(a, fila(3, "C", "CALIDAD Y MEJORA", "5")))
                                .huella())
                .isNotEqualTo(huella);
    }

    @Test
    void conLaMismaHuella_aplicaNuevosYCambiosSinTocarBajasNiIguales() {
        Unidades unidades = conTresUnidades();
        LoteDeCarga lote =
                lote(
                        fila(2, "A", "ADMISIONES", "3"),
                        fila(3, "B", "BIBLIOTECA", "4"),
                        fila(4, "C", "CALIDAD Y MEJORA", "2"));
        String huella = MotorDelLote.previsualizar(unidades, lote).huella();

        ResultadoDelLote resultado =
                new AplicadorPorTraductores(List.of(unidades), transaccion(unidades))
                        .aplicar(lote, huella, "datos.demo");

        assertThat(resultado).isEqualTo(new ResultadoDelLote(1, 1, 1));
        assertThat(unidades.aplicadas).containsExactly("registrar A", "corregir C");
        assertThat(unidades.bloqueos).isOne();
    }

    @Test
    void conOtraHuella_noAplicaNada() {
        Unidades unidades = conTresUnidades();
        LoteDeCarga lote = lote(fila(2, "A", "ADMISIONES", "3"));

        assertThatThrownBy(
                        () ->
                                new AplicadorPorTraductores(
                                                List.of(unidades), transaccion(unidades))
                                        .aplicar(lote, "b".repeat(64), "datos.demo"))
                .isInstanceOf(VistaPreviaDesactualizada.class);
        assertThat(unidades.aplicadas).isEmpty();
    }

    @Test
    void siUnaReglaDelDuenoFalla_rechazaEsaFilaYDeshaceTodo() {
        Unidades unidades = conTresUnidades();
        unidades.rechazaLa = "C";
        LoteDeCarga lote =
                lote(fila(2, "A", "ADMISIONES", "3"), fila(3, "C", "CALIDAD Y MEJORA", "2"));
        String huella = MotorDelLote.previsualizar(unidades, lote).huella();

        assertThatThrownBy(
                        () ->
                                new AplicadorPorTraductores(
                                                List.of(unidades), transaccion(unidades))
                                        .aplicar(lote, huella, "datos.demo"))
                .isInstanceOfSatisfying(
                        RechazoDelLote.class,
                        rechazo -> {
                            assertThat(rechazo.fila()).isEqualTo(3);
                            assertThat(rechazo.getCause())
                                    .isInstanceOf(IllegalStateException.class);
                        });
        assertThat(unidades.aplicadas).as("la fila 2 también se deshace").isEmpty();
    }

    @Test
    void dosFilasConLaMismaClave_seRechazanConLaFilaDeLaPrimera() {
        assertThatThrownBy(
                        () ->
                                MotorDelLote.previsualizar(
                                        new Unidades(),
                                        lote(fila(2, "A", "UNO", "1"), fila(5, "A", "DOS", "1"))))
                .isInstanceOfSatisfying(
                        RechazoDelLote.class,
                        rechazo -> {
                            assertThat(rechazo.fila()).isEqualTo(5);
                            assertThat(rechazo)
                                    .hasMessage("Esta clave ya está en la fila 2: no la repitas.");
                        });
    }

    @Test
    void losPrimerosDeCadaTipo_sinLosIguales() {
        Unidades unidades = new Unidades();
        List<FilaDelLote> filas = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            filas.add(fila(i + 2, String.format("N%03d", i), "NUEVA", "1"));
        }
        for (int i = 0; i < 3; i++) {
            unidades.existe(String.format("X%03d", i), "VIEJA", "1", "P1");
        }

        VistaPreviaDelLote vista =
                MotorDelLote.previsualizar(unidades, new LoteDeCarga("unidades", HASH, filas));

        assertThat(vista.cuantos(TipoDeRegistro.NUEVO)).isEqualTo(150);
        assertThat(vista.primeros(100)).hasSize(103);
    }

    @Test
    void elAplicadorRepartePorCargaYNoAdmiteDosTraductoresDeLaMisma() {
        Unidades unidades = new Unidades();

        assertThat(new AplicadorPorTraductores(List.of(unidades), transaccion(unidades)).cargas())
                .containsExactly("unidades");
        assertThatThrownBy(
                        () ->
                                new AplicadorPorTraductores(
                                        List.of(unidades, new Unidades()), transaccion(unidades)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unidades");
        assertThatThrownBy(
                        () ->
                                new AplicadorPorTraductores(
                                                List.of(unidades), transaccion(unidades))
                                        .previsualizar(new LoteDeCarga("otra", HASH, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elHashDeUnArchivoEsSuSha256() {
        assertThat(MotorDelLote.sha256(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
