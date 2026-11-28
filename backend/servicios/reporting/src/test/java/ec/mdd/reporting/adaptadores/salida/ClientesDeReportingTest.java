package ec.mdd.reporting.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.plataforma.eventos.ProveedorDeTokenDeProceso;
import ec.mdd.plataforma.pruebas.ReceptorSimulado;
import ec.mdd.reporting.dominio.CargasDeLaVersion;
import ec.mdd.reporting.dominio.Componente;
import ec.mdd.reporting.dominio.ContenidoDelDocente;
import ec.mdd.reporting.dominio.Dedicacion;
import ec.mdd.reporting.dominio.DocentesDeLaVersion;
import ec.mdd.reporting.dominio.FirmasDeLaVersion;
import ec.mdd.reporting.dominio.InconsistenciasAbiertas;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.dominio.TipoDeIdentificacion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Los clientes de Reporting por las rutas internas del proceso distributivo (ADR-023, F20-H01,
 * F20-H02).
 */
class ClientesDeReportingTest {

    private static final String HASH = "a".repeat(64);

    private final ReceptorSimulado receptor = new ReceptorSimulado();
    private final ConsultasEntreProcesos consultas =
            new ConsultasEntreProcesos(
                    RestClient.create(),
                    new ProveedorDeTokenDeProceso(
                            RestClient.create(),
                            receptor.urlToken(),
                            "salida",
                            "clave",
                            Clock.systemUTC()));
    private final PublicacionesDesdeAprobacion publicaciones =
            new PublicacionesDesdeAprobacion(consultas, receptor.base());
    private final ContenidoDesdeGeneracion contenido =
            new ContenidoDesdeGeneracion(consultas, receptor.base());
    private final FirmasDesdeFirma firmas = new FirmasDesdeFirma(consultas, receptor.base());

    @AfterEach
    void cerrar() {
        receptor.close();
    }

    @Test
    void dadasLasPublicacionesDeAprobacion_cuandoSePiden_entoncesLasTraduce() {
        receptor.responderConsulta(
                "/interno/validacionocs/publicaciones",
                200,
                "[{\"periodo\":\"2026-I\",\"version\":1,\"hash\":\""
                        + HASH
                        + "\",\"publicadaPor\":\"secretaria.demo\","
                        + "\"publicadaEn\":\"2026-11-18T15:00:00Z\",\"enReforma\":true}]");

        assertThat(publicaciones.vigentes())
                .containsExactly(
                        new Publicacion(
                                "2026-I",
                                1,
                                HASH,
                                Instant.parse("2026-11-18T15:00:00Z"),
                                "secretaria.demo",
                                true));
    }

    @Test
    void dadoUnErrorDeAprobacion_cuandoSePiden_entoncesSePropaga() {
        receptor.responderConsulta("/interno/validacionocs/publicaciones", 500, "{}");

        assertThatThrownBy(publicaciones::vigentes).isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadosLosDocentesDeUnaVersion_cuandoSePiden_entoncesLosTraduceConSuDedicacionYSusHoras() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/1/cargas",
                200,
                "{\"hash\":\""
                        + HASH
                        + "\",\"docentes\":[{\"identificacion\":\"0109990010\","
                        + "\"nombre\":\"PERALTA MALDONADO JUAN ANDRES\",\"dedicacion\":\"TC\","
                        + "\"horas\":{\"clase\":8.00,\"otrasDocencia\":2.5,\"investigacion\":0,"
                        + "\"vinculacion\":0,\"gestion\":1}},{\"identificacion\":\"0109990020\","
                        + "\"nombre\":\"ZAMBRANO PAREDES ROSA ELENA\",\"horas\":{\"clase\":0,"
                        + "\"otrasDocencia\":0,\"investigacion\":0,\"vinculacion\":0,"
                        + "\"gestion\":3}}]}");

        DocentesDeLaVersion docentes = contenido.docentes("2026-I", 1).orElseThrow();

        assertThat(docentes.hash()).isEqualTo(HASH);
        assertThat(docentes.docentes())
                .extracting(DocentesDeLaVersion.Docente::identificacion)
                .containsExactly("0109990010", "0109990020");
        DocentesDeLaVersion.Docente juan = docentes.docentes().get(0);
        assertThat(juan.nombre()).isEqualTo("PERALTA MALDONADO JUAN ANDRES");
        assertThat(juan.dedicacion()).contains(Dedicacion.TC);
        assertThat(juan.horas().clase()).isEqualByComparingTo("8");
        assertThat(juan.horas().otrasDocencia()).isEqualByComparingTo("2.5");
        assertThat(juan.horas().total()).isEqualByComparingTo("11.5");
        assertThat(docentes.docentes().get(1).dedicacion()).isEmpty();
        assertThat(docentes.docentes().get(1).horas().gestion()).isEqualByComparingTo("3");
    }

    @Test
    void dadoUnDocenteSinHoras_cuandoSePidenLosDocentes_entoncesEsUnErrorDelProceso() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/1/cargas",
                200,
                "{\"hash\":\""
                        + HASH
                        + "\",\"docentes\":[{\"identificacion\":\"0109990010\","
                        + "\"nombre\":\"PERALTA MALDONADO JUAN ANDRES\"}]}");

        assertThatThrownBy(() -> contenido.docentes("2026-I", 1))
                .isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadaLaCargaDeUnDocente_cuandoSePide_entoncesLaTraduceConSuUnidad() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/1/cargas/0109990010",
                200,
                "{\"hash\":\""
                        + HASH
                        + "\",\"docente\":{\"identificacion\":\"0109990010\","
                        + "\"nombre\":\"PEREZ ANA\",\"dedicacion\":\"TC\",\"horasContratadas\":40},"
                        + "\"asignaturas\":[{\"codigo\":\"BD-301\",\"nombre\":\"Bases de datos\","
                        + "\"paralelo\":\"A\",\"carrera\":\"TDS\",\"nombreCarrera\":\"Software\","
                        + "\"horas\":8.00}],\"actividades\":[{\"codigo\":\"141-a\","
                        + "\"nombre\":\"Proyecto\",\"componente\":\"INVESTIGACION\","
                        + "\"carrera\":\"TDS\",\"horas\":2.50,\"nombreProyecto\":\"Suelos\","
                        + "\"proyecto\":\"INV-01\"},{\"codigo\":\"143-a\",\"nombre\":\"Gestión\","
                        + "\"componente\":\"GESTION\",\"carrera\":\"TDS\",\"horas\":1}],"
                        + "\"horas\":{\"clase\":8.00,\"otrasDocencia\":0,\"investigacion\":2.50,"
                        + "\"vinculacion\":0,\"gestion\":1}}");

        ContenidoDelDocente leido = contenido.contenido("2026-I", 1, "0109990010").orElseThrow();

        assertThat(leido.hash()).isEqualTo(HASH);
        assertThat(leido.docente().dedicacion()).contains(Dedicacion.TC);
        assertThat(leido.asignaturas().get(0).carrera()).isEqualTo("Software");
        assertThat(leido.actividades())
                .extracting(una -> una.componente(), una -> una.unidad().orElseThrow())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(Componente.INVESTIGACION, "Suelos"),
                        org.assertj.core.groups.Tuple.tuple(Componente.GESTION, "TDS"));
        assertThat(leido.horas().total()).isEqualByComparingTo("11.5");
    }

    @Test
    void dadoElContenidoCompleto_cuandoSePide_entoncesTraduceCadaCargaConSusCodigosYSuTipo() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/1/contenido",
                200,
                "{\"hash\":\""
                        + HASH
                        + "\",\"cargas\":[{\"hash\":\""
                        + HASH
                        + "\",\"docente\":{\"identificacion\":\"0109990010\","
                        + "\"nombre\":\"PEREZ ANA\",\"tipoIdentificacion\":\"PASAPORTE\","
                        + "\"dedicacion\":\"TC\",\"horasContratadas\":40},"
                        + "\"asignaturas\":[{\"codigo\":\"BD-301\",\"nombre\":\"Bases de datos\","
                        + "\"paralelo\":\"A\",\"carrera\":\"TDS\",\"nombreCarrera\":\"Software\","
                        + "\"horas\":8.00}],\"actividades\":[{\"codigo\":\"141-a\","
                        + "\"nombre\":\"Proyecto\",\"componente\":\"INVESTIGACION\","
                        + "\"carrera\":\"TDS\",\"horas\":2.50,\"nombreProyecto\":\"Suelos\","
                        + "\"proyecto\":\"INV-01\"}],"
                        + "\"horas\":{\"clase\":8.00,\"otrasDocencia\":0,\"investigacion\":2.50,"
                        + "\"vinculacion\":0,\"gestion\":0}},{\"hash\":\""
                        + HASH
                        + "\",\"docente\":{\"identificacion\":\"0109990020\","
                        + "\"nombre\":\"VEGA LUIS\"},\"asignaturas\":[],\"actividades\":[],"
                        + "\"horas\":{\"clase\":0,\"otrasDocencia\":0,\"investigacion\":0,"
                        + "\"vinculacion\":0,\"gestion\":0}}]}");

        CargasDeLaVersion leidas = contenido.cargas("2026-I", 1).orElseThrow();

        assertThat(leidas.hash()).isEqualTo(HASH);
        assertThat(leidas.cargas()).hasSize(2);
        ContenidoDelDocente ana = leidas.cargas().get(0);
        assertThat(ana.docente().tipoIdentificacion()).contains(TipoDeIdentificacion.PASAPORTE);
        assertThat(ana.asignaturas().get(0).codigoCarrera()).contains("TDS");
        assertThat(ana.actividades().get(0).codigoCarrera()).contains("TDS");
        assertThat(ana.actividades().get(0).codigoProyecto()).contains("INV-01");
        assertThat(leidas.cargas().get(1).docente().tipoIdentificacion()).isEmpty();
        assertThat(leidas.cargas().get(1).docente().dedicacion()).isEmpty();
        // F19-H02 (D-120): un contenido sin inconsistencias registradas.
        assertThat(leidas.inconsistencias()).isEmpty();
    }

    // F19-H02 (D-120): las inconsistencias abiertas registradas al congelar.
    @Test
    void dadoUnContenidoConSusInconsistencias_cuandoSePide_entoncesLasTraduce() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/1/contenido",
                200,
                "{\"hash\":\""
                        + HASH
                        + "\",\"cargas\":[],\"inconsistenciasAbiertas\":{\"duras\":1,\"blandas\":2}}");

        assertThat(contenido.cargas("2026-I", 1).orElseThrow().inconsistencias())
                .contains(new InconsistenciasAbiertas(1, 2));
    }

    @Test
    void dadasLasFirmasDeUnaVersion_cuandoSePiden_entoncesLasTraduce() {
        receptor.responderConsulta(
                "/interno/firma/periodos/2026-I/versiones/1/firmas",
                200,
                "{\"periodo\":\"2026-I\",\"version\":1,\"hashContenido\":\""
                        + HASH
                        + "\",\"pasos\":[{\"orden\":1,\"estado\":\"FIRMADO\"},"
                        + "{\"orden\":2,\"estado\":\"PENDIENTE\"},"
                        + "{\"orden\":3,\"estado\":\"RECHAZADO\"}],"
                        + "\"anulada\":false,\"rechazada\":true}");

        assertThat(firmas.firmas("2026-I", 1))
                .contains(
                        new FirmasDeLaVersion(
                                HASH,
                                List.of(
                                        new FirmasDeLaVersion.PasoFirmado(1, true),
                                        new FirmasDeLaVersion.PasoFirmado(2, false),
                                        new FirmasDeLaVersion.PasoFirmado(3, false)),
                                false,
                                true));
    }

    @Test
    void dadaUnaVersionSinDocumento_cuandoSePidenSusFirmas_entoncesVacio() {
        receptor.responderConsulta("/interno/firma/periodos/2026-I/versiones/7/firmas", 404, "{}");

        assertThat(firmas.firmas("2026-I", 7)).isEmpty();
    }

    @Test
    void dadoUnErrorDeFirmaOUnaRespuestaSinHash_cuandoSePidenLasFirmas_entoncesErrorDelProceso() {
        receptor.responderConsulta("/interno/firma/periodos/2026-I/versiones/1/firmas", 500, "{}");
        receptor.responderConsulta(
                "/interno/firma/periodos/2026-I/versiones/2/firmas",
                200,
                "{\"periodo\":\"2026-I\",\"version\":2,\"pasos\":[],"
                        + "\"anulada\":false,\"rechazada\":false}");

        assertThatThrownBy(() -> firmas.firmas("2026-I", 1)).isInstanceOf(ProcesoConError.class);
        assertThatThrownBy(() -> firmas.firmas("2026-I", 2)).isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadoUnContenidoSinHash_cuandoSePide_entoncesEsUnErrorDelProceso() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/1/contenido",
                200,
                "{\"cargas\":[]}");

        assertThatThrownBy(() -> contenido.cargas("2026-I", 1)).isInstanceOf(ProcesoConError.class);
    }

    @Test
    void dadaUnaVersionSinContenido_cuandoSePideElCompleto_entoncesVacio() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/7/contenido", 404, "{}");

        assertThat(contenido.cargas("2026-I", 7)).isEmpty();
    }

    @Test
    void dadoUnDocenteQueNoEsta_cuandoSePideSuCarga_entoncesVacio() {
        receptor.responderConsulta(
                "/interno/generacion/periodos/2026-I/versiones/1/cargas/0109990099", 404, "{}");

        assertThat(contenido.contenido("2026-I", 1, "0109990099")).isEmpty();
        assertThat(contenido.docentes("2026-I", 7)).isEmpty();
    }
}
