package ec.mdd.cargas.dominio;

import static ec.mdd.cargas.dominio.DiccionarioReal.DICCIONARIO;
import static ec.mdd.cargas.dominio.DiccionarioReal.carga;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Las 5 condiciones de obligatoriedad del diccionario real (research §4). */
class CondicionDeObligatoriedadTest {

    @ParameterizedTest(name = "{0}.{1} con {2} = {3} → {4}")
    @CsvSource({
        "estructura_organica, codigo_gestion_autorizada, tipo_unidad, GESTION, true",
        "estructura_organica, codigo_gestion_autorizada, tipo_unidad, SEDE, false",
        "estructura_organica, es_comision_permanente, tipo_unidad, COMISION, true",
        "estructura_organica, es_comision_permanente, tipo_unidad, GESTION, false",
        "docentes, categoria_escalafon, tipo_personal, TITULAR, true",
        "docentes, categoria_escalafon, tipo_personal, OCASIONAL, false",
        "docentes, fin_contrato, tipo_personal, OCASIONAL, true",
        "docentes, fin_contrato, tipo_personal, TITULAR, false",
        "cargos_autoridades, codigo_carrera, codigo_cargo, COORDINADOR_CARRERA, true",
        "cargos_autoridades, codigo_carrera, codigo_cargo, RECTOR, false",
        "cargos_autoridades, codigo_unidad, codigo_cargo, RECTOR, true",
        "cargos_autoridades, codigo_unidad, codigo_cargo, COORDINADOR_CARRERA, false"
    })
    void cadaCondicionDelDiccionario(
            String carga, String campo, String otro, String valor, boolean exige) {
        CondicionDeObligatoriedad condicion = condicion(carga, campo);

        assertThat(condicion.exige(Map.of(otro, valor))).isEqualTo(exige);
    }

    @Test
    void siYNoSonSiempreYNunca() {
        assertThat(condicion("docentes", "primer_nombre").exige(Map.of())).isTrue();
        assertThat(condicion("docentes", "segundo_nombre").exige(Map.of())).isFalse();
    }

    @Test
    void elMotivoDiceLaCondicion() {
        assertThat(condicion("docentes", "primer_nombre").motivo())
                .isEqualTo("Es obligatorio: complétalo.");
        assertThat(condicion("docentes", "categoria_escalafon").motivo())
                .isEqualTo("Es obligatorio cuando tipo_personal es TITULAR: complétalo.");
        assertThat(condicion("cargos_autoridades", "codigo_unidad").motivo())
                .isEqualTo(
                        "Es obligatorio salvo cuando codigo_cargo es COORDINADOR_CARRERA:"
                                + " complétalo.");
    }

    @Test
    void dadoUnTextoDesconocido_cuandoSeCargaElDiccionario_entoncesFallaConSuColumna() {
        ColumnaDeCarga columna =
                new ColumnaDeCarga(
                        "codigo", "Código", "Texto", 20, "A veces", "", "", "", "ABC", null);
        List<Carga> cargas = DiccionarioDePrueba.diezCargas();
        cargas.set(0, DiccionarioDePrueba.carga(1, List.of(), columna));

        assertThatThrownBy(() -> DiccionarioDePrueba.diccionario(cargas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codigo")
                .hasMessageContaining("A veces");
    }

    private static CondicionDeObligatoriedad condicion(String carga, String campo) {
        return DICCIONARIO.validador(carga(carga)).condicion(campo);
    }
}
