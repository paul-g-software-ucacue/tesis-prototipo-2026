package ec.mdd.docentes.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.mdd.docentes.puertos.salida.CambioRegistrado;
import ec.mdd.docentes.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.plataforma.eventos.EventoEntreProcesos;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Cada cambio se publica como {@code docentes.EntradaBitacora}, sin el valor de los datos
 * personales del docente (F17-H01, FR-014, D-91). El registro local no cambia.
 */
class RegistroDeCambiosJdbcTest {

    private static final Instant MOMENTO = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-11-06T15:00:01Z");
    private static final String QUIEN = "talento.demo";
    private static final String CEDULA = "0100000001";

    private final JdbcClient jdbc = mock(JdbcClient.class);
    private final JdbcClient.StatementSpec sentencia =
            mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
    private final List<Object> publicados = new ArrayList<>();
    private final RegistroDeCambiosJdbc registro =
            new RegistroDeCambiosJdbc(
                    jdbc, publicados::add, Clock.fixed(PUBLICADO, ZoneOffset.UTC));

    {
        when(jdbc.sql(anyString())).thenReturn(sentencia);
    }

    private EventoEntreProcesos publicado() {
        assertThat(publicados).hasSize(1);
        return (EventoEntreProcesos) publicados.getFirst();
    }

    private static Map<String, Object> cambio(String campo, String anterior, String nuevo) {
        Map<String, Object> cambio = new LinkedHashMap<>();
        cambio.put("campo", campo);
        cambio.put("anterior", anterior);
        cambio.put("nuevo", nuevo);
        return cambio;
    }

    /** Una corrección con un teléfono, una condición especial y la dedicación. */
    private CambioRegistrado correccion() {
        Map<String, Object> anterior = new LinkedHashMap<>();
        anterior.put("telefono", "0990000001");
        anterior.put("condicionEspecial", null);
        anterior.put("dedicacion", "MT");
        Map<String, Object> nuevo = new LinkedHashMap<>();
        nuevo.put("telefono", "0990000002");
        nuevo.put("condicionEspecial", "LACTANCIA");
        nuevo.put("dedicacion", "TC");
        return new CambioRegistrado(
                MOMENTO,
                QUIEN,
                Accion.DOCENTE_CORREGIDO,
                Map.of(
                        "identificacion",
                        CEDULA,
                        "cambios",
                        List.of(
                                cambio("telefono", "0990000001", "0990000002"),
                                cambio("condicionEspecial", null, "LACTANCIA"),
                                cambio("dedicacion", "MT", "TC"))),
                anterior,
                nuevo);
    }

    @Test
    void unaCorreccion_cuandoSeRegistra_entoncesPublicaSuEntradaSinLosDatosPersonales() {
        registro.registrar(correccion());

        EventoEntreProcesos evento = publicado();
        assertThat(evento.tipo()).isEqualTo("docentes.EntradaBitacora");
        assertThat(evento.origen()).isEqualTo("docentes");
        assertThat(evento.destino()).isEqualTo("usuarios");
        assertThat(evento.contenido())
                .containsEntry("usuario", QUIEN)
                .containsEntry("accion", "DOCENTE_CORREGIDO")
                .containsEntry("momento", "2026-11-06T15:00:00Z")
                .doesNotContainKey("periodo")
                .containsEntry("anterior", Map.of("dedicacion", "MT"))
                .containsEntry("nuevo", Map.of("dedicacion", "TC"))
                .containsEntry(
                        "objeto",
                        Map.of(
                                "identificacion",
                                CEDULA,
                                "cambios",
                                List.of(cambio("dedicacion", "MT", "TC")),
                                "camposPersonales",
                                List.of("condicionEspecial", "telefono")));
        assertThat(evento.contenido().toString())
                .doesNotContain("0990000001")
                .doesNotContain("0990000002")
                .doesNotContain("LACTANCIA");
    }

    @Test
    void unaCorreccion_cuandoSeRegistra_entoncesLaFilaLocalGuardaElDetalleCompleto() {
        registro.registrar(correccion());

        ArgumentCaptor<Object> detalle = ArgumentCaptor.forClass(Object.class);
        verify(sentencia).param(eq("detalle"), detalle.capture());
        assertThat((String) detalle.getValue())
                .contains("\"identificacion\":\"0100000001\"")
                .contains("0990000001", "0990000002", "LACTANCIA")
                .doesNotContain("camposPersonales");
        verify(sentencia).update();
    }

    @Test
    void unaCorreccionSoloDeDatosPersonales_cuandoSeRegistra_entoncesLlevaAnteriorYNuevoVacios() {
        registro.registrar(
                new CambioRegistrado(
                        MOMENTO,
                        QUIEN,
                        Accion.DOCENTE_CORREGIDO,
                        Map.of(
                                "identificacion",
                                CEDULA,
                                "cambios",
                                List.of(cambio("correoInstitucional", "a@x.ec", "b@x.ec"))),
                        Map.of("correoInstitucional", "a@x.ec"),
                        Map.of("correoInstitucional", "b@x.ec")));

        assertThat(publicado().contenido())
                .containsEntry("anterior", Map.of())
                .containsEntry("nuevo", Map.of())
                .containsEntry(
                        "objeto",
                        Map.of(
                                "identificacion",
                                CEDULA,
                                "cambios",
                                List.of(),
                                "camposPersonales",
                                List.of("correoInstitucional")));
    }

    @Test
    void unRegistroSinDatosPersonales_cuandoSeRegistra_entoncesNoNombraCamposPersonales() {
        Map<String, Object> datos =
                Map.of("identificacion", CEDULA, "dedicacion", "TC", "carreraBase", "C1");

        registro.registrar(
                new CambioRegistrado(
                        MOMENTO, QUIEN, Accion.DOCENTE_REGISTRADO, datos, Map.of(), datos));

        assertThat(publicado().contenido())
                .containsEntry("objeto", datos)
                .containsEntry("nuevo", datos)
                .doesNotContainKey("anterior");
    }

    /** FR-014 y T025: ningún dato personal de D-91 llega con su valor, en ninguna acción. */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "primerNombre",
                "segundoNombre",
                "primerApellido",
                "segundoApellido",
                "correoInstitucional",
                "telefono",
                "condicionEspecial"
            })
    void unDatoPersonal_cuandoCambia_entoncesLaEntradaLoNombraSinSuValor(String campo) {
        registro.registrar(
                new CambioRegistrado(
                        MOMENTO,
                        QUIEN,
                        Accion.DOCENTE_CORREGIDO,
                        Map.of(
                                "identificacion",
                                CEDULA,
                                "cambios",
                                List.of(cambio(campo, "valor-anterior", "valor-nuevo"))),
                        Map.of(campo, "valor-anterior"),
                        Map.of(campo, "valor-nuevo")));

        Map<String, Object> contenido = publicado().contenido();
        assertThat(contenido.toString()).doesNotContain("valor-anterior", "valor-nuevo");
        assertThat(contenido.get("objeto"))
                .isEqualTo(
                        Map.of(
                                "identificacion",
                                CEDULA,
                                "cambios",
                                List.of(),
                                "camposPersonales",
                                List.of(campo)));
    }
}
