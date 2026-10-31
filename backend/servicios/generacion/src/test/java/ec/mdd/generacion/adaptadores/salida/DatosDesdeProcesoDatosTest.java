package ec.mdd.generacion.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.ElementoDeFormacion;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.dominio.TituloDeTercerNivel;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.plataforma.eventos.ProveedorDeTokenDeProceso;
import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import java.time.Clock;
import java.util.List;
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

        assertThat(datos.asignaturasOfertadas(PERIODO))
                .contains(List.of(new AsignaturaOfertada("POO-204", "Programación", "06", "061")));
    }

    @Test
    void dadoUnPeriodoQueAcademicoNoTiene_cuandoSePide_entoncesVacio() {
        assertThat(datos.asignaturasOfertadas(PERIODO)).isEmpty();
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
                        + "\"topeCursos\":4}}");

        PesosDelPeriodo pesos = datos.pesos(PERIODO).orElseThrow();

        assertThat(pesos.version()).isEqualTo(2);
        assertThat(pesos.pesos().cuartoNivelCampoEspecifico()).isEqualByComparingTo("7");
        assertThat(pesos.pesos().cuartoNivelCampoAmplio()).isEqualByComparingTo("4");
        assertThat(pesos.pesos().tercerNivelCampoEspecifico()).isEqualByComparingTo("5");
        assertThat(pesos.pesos().tercerNivelCampoAmplio()).isEqualByComparingTo("3");
        assertThat(pesos.pesos().experienciaRelacionada()).isEqualByComparingTo("2.5");
        assertThat(pesos.pesos().cursoRelacionado()).isEqualByComparingTo("1.5");
        assertThat(pesos.pesos().topeCursos()).isEqualByComparingTo("4");
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
}
