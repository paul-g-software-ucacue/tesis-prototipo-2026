package ec.mdd.generacion.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.AsignaturaParalelo;
import ec.mdd.generacion.dominio.Dedicacion;
import ec.mdd.generacion.dominio.DocenteActivo;
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.plataforma.eventos.ProveedorDeTokenDeProceso;
import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Generación pide cada dato a su dueño por la ruta interna y lo pasa a sus propios tipos (principio
 * V, ADR-023), contra el proceso simulado.
 */
class DatosDesdeProcesoDatosTest {

    private static final String PERIODO = "2026-I";
    private static final String DOCENTE = "0109990010";
    private static final String RUTA_OFERTA =
            "/interno/academico/periodos/2026-I/asignaturas-ofertadas";
    private static final String RUTA_PESOS = "/interno/normativa/periodos/2026-I/pesos-de-afinidad";
    private static final String RUTA_FORMACION = "/interno/docentes/0109990010/formacion";
    private static final String RUTA_PARALELO =
            "/interno/academico/periodos/2026-I/oferta/POO-204/A_NOCTURNA";
    private static final String RUTA_ACTIVOS = "/interno/docentes/activos";

    private final ReceptorSimulado receptor = new ReceptorSimulado();
    private final DatosDesdeProcesoDatos datos =
            new DatosDesdeProcesoDatos(
                    new ConsultasEntreProcesos(
                            RestClient.create(),
                            new ProveedorDeTokenDeProceso(
                                    RestClient.create(),
                                    receptor.urlToken(),
                                    "distributivo",
                                    "clave",
                                    Clock.systemUTC())),
                    receptor.base());

    @AfterEach
    void cerrar() {
        receptor.close();
    }

    @Test
    void dadaLaOferta_cuandoSePide_entoncesLaPasaASusTipos() {
        receptor.responderConsulta(
                RUTA_OFERTA,
                200,
                "[{\"codigo\":\"POO-204\",\"nombre\":\"Programación\","
                        + "\"campoAmplio\":\"06\",\"campoEspecifico\":\"061\"}]");

        assertThat(datos.asignaturasOfertadas(PERIODO, Optional.empty()))
                .contains(List.of(new AsignaturaOfertada("POO-204", "Programación", "06", "061")));
    }

    @Test
    void dadaUnaCarrera_cuandoSePideLaOferta_entoncesLaConsultaLaLleva() {
        receptor.responderConsulta(RUTA_OFERTA, 200, "[]");

        datos.asignaturasOfertadas(PERIODO, Optional.of("DS-01"));

        assertThat(receptor.direccionesConsultadas().getLast()).endsWith("?carrera=DS-01");
    }

    @Test
    void dadoQueNoHayCarrera_cuandoSePideLaOferta_entoncesLaConsultaNoLaLleva() {
        receptor.responderConsulta(RUTA_OFERTA, 200, "[]");

        datos.asignaturasOfertadas(PERIODO, Optional.empty());

        assertThat(receptor.direccionesConsultadas().getLast()).doesNotContain("carrera");
    }

    @Test
    void dadoUnPeriodoQueAcademicoNoTiene_cuandoSePide_entoncesVacio() {
        assertThat(datos.asignaturasOfertadas(PERIODO, Optional.empty())).isEmpty();
    }

    @Test
    void dadosLosPesos_cuandoSePiden_entoncesTraenLaVersionYLosSiete() {
        receptor.responderConsulta(
                RUTA_PESOS,
                200,
                "{\"periodo\":\"2026-I\",\"version\":2,\"pesos\":{"
                        + "\"cuartoNivelCampoEspecifico\":7,\"cuartoNivelCampoAmplio\":4,"
                        + "\"tercerNivelCampoEspecifico\":5,\"tercerNivelCampoAmplio\":3,"
                        + "\"experienciaRelacionada\":2.5,\"cursoRelacionado\":1.5,"
                        + "\"topeCursos\":4},\"minimoAfinidad\":3.5}");

        PesosDelPeriodo pesos = datos.pesos(PERIODO).orElseThrow();

        assertThat(pesos.version()).isEqualTo(2);
        assertThat(pesos.pesos().cuartoNivelCampoEspecifico()).isEqualByComparingTo("7");
        assertThat(pesos.pesos().cuartoNivelCampoAmplio()).isEqualByComparingTo("4");
        assertThat(pesos.pesos().tercerNivelCampoEspecifico()).isEqualByComparingTo("5");
        assertThat(pesos.pesos().tercerNivelCampoAmplio()).isEqualByComparingTo("3");
        assertThat(pesos.pesos().experienciaRelacionada()).isEqualByComparingTo("2.5");
        assertThat(pesos.pesos().cursoRelacionado()).isEqualByComparingTo("1.5");
        assertThat(pesos.pesos().topeCursos()).isEqualByComparingTo("4");
        assertThat(pesos.minimoAfinidad()).isEqualByComparingTo("3.5");
    }

    @Test
    void dadoUnPeriodoSinVersion_cuandoSePiden_entoncesVacio() {
        assertThat(datos.pesos(PERIODO)).isEmpty();
    }

    @Test
    void dadaLaFormacion_cuandoSePide_entoncesLaPasaASusTipos() {
        receptor.responderConsulta(
                RUTA_FORMACION,
                200,
                "{\"tercerNivel\":{\"nombre\":\"Ingeniería\",\"campoAmplio\":\"06\","
                        + "\"campoEspecifico\":\"061\"},"
                        + "\"cuartoNivel\":[{\"nombre\":\"Maestría\",\"campoEspecifico\":\"061\"}],"
                        + "\"experiencia\":[{\"nombre\":\"Analista\",\"campoEspecifico\":\"062\"}],"
                        + "\"cursos\":[{\"nombre\":\"Java\",\"campoEspecifico\":\"061\"}]}");

        assertThat(datos.formacion(DOCENTE))
                .contains(
                        new FormacionDelDocente(
                                new TituloDeTercerNivel("Ingeniería", "06", "061"),
                                List.of(new ElementoDeFormacion("Maestría", "061")),
                                List.of(new ElementoDeFormacion("Analista", "062")),
                                List.of(new ElementoDeFormacion("Java", "061"))));
    }

    @Test
    void dadoUnDocenteQueNoExiste_cuandoSePide_entoncesVacio() {
        assertThat(datos.formacion(DOCENTE)).isEmpty();
    }

    @Test
    void dadoUnParalelo_cuandoSePide_entoncesLoPasaASusTipos() {
        receptor.responderConsulta(RUTA_PARALELO, 200, paralelo("OFERTADA"));

        assertThat(datos.paralelo(PERIODO, "POO-204", "A_NOCTURNA"))
                .contains(
                        new AsignaturaParalelo(
                                new AsignaturaOfertada("POO-204", "Programación", "06", "061"),
                                "A_NOCTURNA",
                                "TDS",
                                new BigDecimal("2.5"),
                                true,
                                false));
    }

    @Test
    void dadoUnParaleloRetirado_cuandoSePide_entoncesLoDice() {
        receptor.responderConsulta(RUTA_PARALELO, 200, paralelo("RETIRADA"));

        assertThat(datos.paralelo(PERIODO, "POO-204", "A_NOCTURNA").orElseThrow().retirada())
                .isTrue();
    }

    @Test
    void dadoUnParaleloQueAcademicoNoTiene_cuandoSePide_entoncesVacio() {
        assertThat(datos.paralelo(PERIODO, "POO-204", "A_NOCTURNA")).isEmpty();
    }

    @Test
    void dadoUnCodigoConCaracteresEspeciales_cuandoSePide_entoncesLoEscapaEnLaRuta() {
        // El receptor compara la ruta ya decodificada: sin escapar, ni siquiera sale la consulta.
        receptor.responderConsulta(
                "/interno/academico/periodos/2026-I/oferta/POO-204/A B", 200, paralelo("OFERTADA"));

        assertThat(datos.paralelo(PERIODO, "POO-204", "A B")).isPresent();
    }

    @Test
    void dadosLosDocentesActivos_cuandoSePiden_entoncesLosPasaASusTipos() {
        receptor.responderConsulta(
                RUTA_ACTIVOS,
                200,
                "[{\"identificacion\":\"0109990010\",\"nombre\":\"PEREZ LUIS\","
                        + "\"dedicacion\":\"MT\",\"horasContratadas\":20,\"formacion\":{"
                        + "\"tercerNivel\":{\"nombre\":\"Ingeniería\",\"campoAmplio\":\"06\","
                        + "\"campoEspecifico\":\"061\"},"
                        + "\"cuartoNivel\":[],\"experiencia\":[],\"cursos\":[]}}]");

        assertThat(datos.docentesActivos())
                .containsExactly(
                        new DocenteActivo(
                                "0109990010",
                                "PEREZ LUIS",
                                Dedicacion.MT,
                                20,
                                new FormacionDelDocente(
                                        new TituloDeTercerNivel("Ingeniería", "06", "061"),
                                        List.of(),
                                        List.of(),
                                        List.of())));
    }

    @Test
    void dadoQueNoHayDocentesActivos_cuandoSePiden_entoncesListaVacia() {
        receptor.responderConsulta(RUTA_ACTIVOS, 200, "[]");

        assertThat(datos.docentesActivos()).isEmpty();
    }

    @Test
    void dadoQueLaRutaDeActivosNoResponde_cuandoSePiden_entoncesFallaEnVezDeDecirQueNoHay() {
        assertThatThrownBy(datos::docentesActivos).isInstanceOf(IllegalStateException.class);
    }

    private static String paralelo(String estado) {
        return "{\"asignatura\":\"POO-204\",\"nombre\":\"Programación\",\"carrera\":\"TDS\","
                + "\"paralelo\":\"A_NOCTURNA\",\"campoAmplio\":\"06\","
                + "\"campoEspecifico\":\"061\",\"troncoComun\":true,"
                + "\"horasSemanales\":2.5,\"estado\":\""
                + estado
                + "\"}";
    }
}
