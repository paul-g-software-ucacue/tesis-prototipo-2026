package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo.FirmanteDelPeriodo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * El nombre local del autor (F14-H03, D-104, ADR-017): con valor, es el nombre impreso y esperado
 * de todos los pasos, sin cambiar quién está en turno; vacío, no cambia nada. El repositorio no le
 * da valor.
 */
class FirmantesConNombreLocalTest {

    private static final FirmantesDelPeriodo DE_LOS_PROCESOS =
            (periodo, roles) ->
                    Map.of(
                            "RESPONSABLE_CONSOLIDACION",
                            FirmanteDelPeriodo.porUsuario("Docente (demo)", "docente.demo"),
                            "VICERRECTOR",
                            FirmanteDelPeriodo.porIdentificacion(
                                    "CABRERA VAZQUEZ ANA GABRIELA", "0109990028"));

    private static final Set<String> ROLES = Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR");

    @Test
    void dadoUnNombreLocal_cuandoSePidenLosFirmantes_entoncesEsElDeTodosYConservaLaPersona() {
        Map<String, FirmanteDelPeriodo> firmantes =
                new FirmantesConNombreLocal(DE_LOS_PROCESOS, "Autor De Prueba")
                        .firmantes("2026-I", ROLES);

        assertThat(firmantes.get("RESPONSABLE_CONSOLIDACION"))
                .isEqualTo(FirmanteDelPeriodo.porUsuario("Autor De Prueba", "docente.demo"));
        assertThat(firmantes.get("VICERRECTOR"))
                .isEqualTo(FirmanteDelPeriodo.porIdentificacion("Autor De Prueba", "0109990028"));
    }

    @Test
    void dadoElNombreLocalVacio_cuandoSePidenLosFirmantes_entoncesSonLosDeSiempre() {
        assertThat(new FirmantesConNombreLocal(DE_LOS_PROCESOS, "  ").firmantes("2026-I", ROLES))
                .isEqualTo(DE_LOS_PROCESOS.firmantes("2026-I", ROLES));
    }

    @Test
    void dadoElEjemploVersionado_cuandoSeLee_entoncesNoLeDaValorAlNombreLocal() throws Exception {
        Path ejemplo = Path.of("../../../.env.ejemplo");

        assertThat(Files.readAllLines(ejemplo))
                .noneMatch(linea -> linea.matches("^MDD_FIRMA_NOMBRE_LOCAL=.+"));
    }
}
