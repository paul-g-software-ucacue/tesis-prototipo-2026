package ec.mdd.firma.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.plataforma.eventos.ProveedorDeTokenDeProceso;
import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.time.Clock;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Los nombres de los firmantes del periodo, por las rutas internas de Docentes y Roles (F13-H02,
 * research §6), contra los procesos simulados.
 */
class FirmantesDesdeProcesosTest {

    private static final Set<String> CADENA =
            Set.of("RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR");

    private final ReceptorSimulado receptor = new ReceptorSimulado();
    private final FirmantesDesdeProcesos firmantes =
            new FirmantesDesdeProcesos(
                    new ConsultasEntreProcesos(
                            RestClient.create(),
                            new ProveedorDeTokenDeProceso(
                                    RestClient.create(),
                                    receptor.urlToken(),
                                    "distributivo",
                                    "clave",
                                    Clock.systemUTC())),
                    receptor.base(),
                    receptor.base());

    @AfterEach
    void cerrar() {
        receptor.close();
    }

    @Test
    void dadosLosCargosYLaDesignacion_cuandoSePidenLosNombres_entoncesUnoPorRol() {
        conCargos();
        receptor.responderConsulta(
                "/interno/roles/periodos/2026-I/responsable",
                200,
                "{\"usuario\":\"consolidacion.demo\",\"nombre\":\"PÉREZ ANA\"}");

        assertThat(firmantes.nombres("2026-I", CADENA))
                .containsOnly(
                        entry("RESPONSABLE_CONSOLIDACION", "PÉREZ ANA"),
                        entry("VICERRECTOR", "LÓPEZ LUIS"),
                        entry("RECTOR", "MORA EVA"));
    }

    @Test
    void dadoUnPeriodoSinResponsable_cuandoSePidenLosNombres_entoncesNoEsta() {
        conCargos();
        receptor.responderConsulta(
                "/interno/roles/periodos/2026-I/responsable",
                404,
                "{\"codigo\":\"ROLES_SIN_RESPONSABLE\"}");

        assertThat(firmantes.nombres("2026-I", CADENA))
                .doesNotContainKey("RESPONSABLE_CONSOLIDACION")
                .containsKeys("VICERRECTOR", "RECTOR");
    }

    @Test
    void dadoUnCargoSinDocenteActivo_cuandoSePidenLosNombres_entoncesEseRolNoEsta() {
        receptor.responderConsulta(
                "/interno/docentes/cargos-vigentes",
                200,
                "[{\"docente\":\"0109990010\",\"tipo\":\"RECTOR\"}]");
        receptor.responderConsulta("/interno/docentes/activos", 200, "[]");

        assertThat(firmantes.nombres("2026-I", Set.of("RECTOR"))).isEmpty();
    }

    @Test
    void dadaUnaCadenaSinResponsable_cuandoSePidenLosNombres_entoncesNoConsultaARoles() {
        conCargos();

        firmantes.nombres("2026-I", Set.of("VICERRECTOR", "RECTOR"));

        assertThat(receptor.direccionesConsultadas())
                .noneMatch(direccion -> direccion.contains("/interno/roles/"));
    }

    private void conCargos() {
        receptor.responderConsulta(
                "/interno/docentes/cargos-vigentes",
                200,
                "[{\"docente\":\"0109990010\",\"tipo\":\"RECTOR\"},"
                        + "{\"docente\":\"0109990028\",\"tipo\":\"VICERRECTOR\"},"
                        + "{\"docente\":\"0109990044\",\"tipo\":\"COORDINADOR_CARRERA\","
                        + "\"carrera\":\"TDS\"}]");
        receptor.responderConsulta(
                "/interno/docentes/activos",
                200,
                // Como responde Docentes: con la dedicación, las horas y la formación, que no se
                // usan.
                "[{\"identificacion\":\"0109990010\",\"nombre\":\"MORA EVA\","
                        + "\"dedicacion\":\"TC\",\"horasContratadas\":40,\"formacion\":{}},"
                        + "{\"identificacion\":\"0109990028\",\"nombre\":\"LÓPEZ LUIS\","
                        + "\"dedicacion\":\"TC\",\"horasContratadas\":40,\"formacion\":{}}]");
    }
}
