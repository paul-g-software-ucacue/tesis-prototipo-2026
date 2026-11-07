package ec.mdd.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Las reglas de una entrada de la bitácora (data-model.md, RF-44, FR-011, FR-014, D-91). */
class EntradaBitacoraTest {

    private static final UUID ID = UUID.fromString("6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f");

    @Test
    void
            dadoElEscenarioDelIssue_cuandoSeConstruye_entoncesGuardaQuienQueCuandoObjetoAnteriorYNuevo() {
        EntradaBitacora entrada = EntradaBitacora.de(ID, "generacion", valida());

        assertThat(entrada.idEvento()).isEqualTo(ID);
        assertThat(entrada.servicio()).isEqualTo("generacion");
        assertThat(entrada.usuario()).isEqualTo("coordinacion.demo");
        assertThat(entrada.accion()).isEqualTo("ACTIVIDAD_GUARDADA");
        assertThat(entrada.momento()).isEqualTo(Instant.parse("2026-11-06T15:00:00Z"));
        assertThat(entrada.periodo()).contains("2026-I");
        assertThat(entrada.objeto()).containsEntry("actividad", "TUTORIAS");
        assertThat(entrada.anterior()).contains(Map.of("horas", "4"));
        assertThat(entrada.nuevo()).contains(Map.of("horas", "6"));
    }

    @Test
    void dadoUnCambioQueCrea_cuandoSeConstruye_entoncesNoTieneAnterior() {
        Map<String, Object> contenido = valida();
        contenido.remove("anterior");

        EntradaBitacora entrada = EntradaBitacora.de(ID, "generacion", contenido);

        assertThat(entrada.anterior()).isEmpty();
        assertThat(entrada.nuevo()).isPresent();
    }

    @Test
    void dadoUnCambioQueQuita_cuandoSeConstruye_entoncesNoTieneNuevo() {
        Map<String, Object> contenido = valida();
        contenido.remove("nuevo");

        EntradaBitacora entrada = EntradaBitacora.de(ID, "generacion", contenido);

        assertThat(entrada.anterior()).isPresent();
        assertThat(entrada.nuevo()).isEmpty();
    }

    @Test
    void dadoSinAnteriorNiNuevo_cuandoSeConstruye_entoncesSeRechaza() {
        Map<String, Object> contenido = valida();
        contenido.remove("anterior");
        contenido.remove("nuevo");

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void dadoSinPeriodo_cuandoSeConstruye_entoncesElPeriodoFalta() {
        Map<String, Object> contenido = valida();
        contenido.remove("periodo");

        assertThat(EntradaBitacora.de(ID, "docentes", contenido).periodo()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "generacion",
                "academico",
                "docentes",
                "normativa",
                "roles",
                "validacionocs"
            })
    void dadoCadaServicioAdmitido_cuandoSeConstruye_entoncesSeAcepta(String servicio) {
        assertThat(EntradaBitacora.de(ID, servicio, valida()).servicio()).isEqualTo(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuarios", "cargas", "reporting", "", "Generacion"})
    void dadoUnServicioNoAdmitido_cuandoSeConstruye_entoncesSeRechaza(String servicio) {
        assertThatThrownBy(() -> EntradaBitacora.de(ID, servicio, valida()))
                .isInstanceOf(EntradaBitacoraInvalida.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuario", "accion", "momento", "objeto"})
    void dadoSinUnCampoObligatorio_cuandoSeConstruye_entoncesSeRechazaDiciendoCual(String campo) {
        Map<String, Object> contenido = valida();
        contenido.remove(campo);

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining(campo);
    }

    @Test
    void dadoUnCampoQueNoEstaEnElContrato_cuandoSeConstruye_entoncesSeRechaza() {
        Map<String, Object> contenido = valida();
        contenido.put("extra", "x");

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("extra");
    }

    @Test
    void dadoElUsuarioEnSusLimites_cuandoSeConstruye_entoncesSeAcepta() {
        assertThat(EntradaBitacora.de(ID, "roles", con("usuario", "a")).usuario()).isEqualTo("a");
        assertThat(EntradaBitacora.de(ID, "roles", con("usuario", "u".repeat(64))).usuario())
                .hasSize(64);
        assertThat(EntradaBitacora.de(ID, "roles", con("usuario", "sistema")).usuario())
                .isEqualTo("sistema");
    }

    @Test
    void dadoElUsuarioFueraDeSusLimites_cuandoSeConstruye_entoncesSeRechaza() {
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "roles", con("usuario", "")))
                .isInstanceOf(EntradaBitacoraInvalida.class);
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "roles", con("usuario", "u".repeat(65))))
                .isInstanceOf(EntradaBitacoraInvalida.class);
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "roles", con("usuario", 7)))
                .isInstanceOf(EntradaBitacoraInvalida.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "ACTIVIDAD_GUARDADA", "CONFIGURACION_FLUJO_CARGADA"})
    void dadoUnaAccionValida_cuandoSeConstruye_entoncesSeAcepta(String accion) {
        assertThat(EntradaBitacora.de(ID, "generacion", con("accion", accion)).accion())
                .isEqualTo(accion);
    }

    @Test
    void dadoLaAccionEnSuLargoMaximo_cuandoSeConstruye_entoncesSeAcepta() {
        String cuarenta = "A" + "_".repeat(39);

        assertThat(EntradaBitacora.de(ID, "generacion", con("accion", cuarenta)).accion())
                .hasSize(40);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "actividad_guardada", "_ACCION", "ACCION-1", "ACCION 1"})
    void dadoUnaAccionInvalida_cuandoSeConstruye_entoncesSeRechaza(String accion) {
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", con("accion", accion)))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("accion");
    }

    @Test
    void dadoUnaAccionDe41Caracteres_cuandoSeConstruye_entoncesSeRechaza() {
        String cuarentaYUno = "A" + "_".repeat(40);

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", con("accion", cuarentaYUno)))
                .isInstanceOf(EntradaBitacoraInvalida.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ayer", "2026-11-06", "2026-13-01T00:00:00Z"})
    void dadoUnMomentoQueNoEsUnInstante_cuandoSeConstruye_entoncesSeRechaza(String momento) {
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", con("momento", momento)))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("momento");
    }

    @Test
    void dadoUnMomentoConZonaHoraria_cuandoSeConstruye_entoncesLoGuardaComoInstante() {
        EntradaBitacora entrada =
                EntradaBitacora.de(ID, "generacion", con("momento", "2026-11-06T10:00:00-05:00"));

        assertThat(entrada.momento()).isEqualTo(Instant.parse("2026-11-06T15:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-I", "003", "a", "A.b_c-1234567890123"})
    void dadoUnPeriodoValido_cuandoSeConstruye_entoncesSeAcepta(String periodo) {
        assertThat(EntradaBitacora.de(ID, "generacion", con("periodo", periodo)).periodo())
                .contains(periodo);
    }

    @Test
    void dadoUnPeriodoInvalido_cuandoSeConstruye_entoncesSeRechaza() {
        for (String periodo : List.of("", "2026 I", "x".repeat(21), "2026/I")) {
            assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", con("periodo", periodo)))
                    .as(periodo)
                    .isInstanceOf(EntradaBitacoraInvalida.class)
                    .hasMessageContaining("periodo");
        }
    }

    @Test
    void dadoUnObjetoQueNoEsUnMapa_cuandoSeConstruye_entoncesSeRechaza() {
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", con("objeto", "texto")))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("objeto");
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", con("nuevo", List.of(1))))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("nuevo");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "clave",
                "claveDelUsuario",
                "Contrasena",
                "password",
                "accessToken",
                "TOKEN",
                "secreto",
                "jwt"
            })
    void dadoUnaClaveQueParecesUnSecreto_cuandoSeConstruye_entoncesSeRechaza(String clave) {
        Map<String, Object> contenido = valida();
        contenido.put("nuevo", Map.of("horas", "6", clave, "x"));

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("secreto")
                .message()
                .doesNotContain("\"x\"");
    }

    @Test
    void dadoUnSecretoAnidadoEnUnaListaDelObjeto_cuandoSeConstruye_entoncesSeRechaza() {
        Map<String, Object> contenido = valida();
        contenido.put(
                "objeto",
                Map.of("cambios", List.of(Map.of("campo", "x", "detalle", Map.of("jwt", "y")))));

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class);
    }

    /** D-92: las palabras clave de una asignatura no son un secreto. */
    @Test
    void dadasLasPalabrasClaveDeUnaAsignatura_cuandoSeConstruye_entoncesSeAcepta() {
        Map<String, Object> contenido = valida();
        contenido.put("anterior", Map.of("palabrasClave", "redes"));
        contenido.put("nuevo", Map.of("palabrasClave", "redes; seguridad"));

        assertThat(EntradaBitacora.de(ID, "academico", contenido).nuevo())
                .hasValue(Map.of("palabrasClave", "redes; seguridad"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"palabrasClaveDelUsuario", "PALABRASCLAVE", "palabras_clave"})
    void dadaOtraClaveParecidaALasPalabrasClave_cuandoSeConstruye_entoncesSeRechaza(String clave) {
        Map<String, Object> contenido = valida();
        contenido.put("nuevo", Map.of(clave, "x"));

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "academico", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class);
    }

    @Test
    void dadoUnSecretoComoValorYNoComoClave_cuandoSeConstruye_entoncesSeAcepta() {
        Map<String, Object> contenido = valida();
        contenido.put("objeto", Map.of("campo", "clave"));

        assertThat(EntradaBitacora.de(ID, "generacion", contenido).objeto())
                .containsEntry("campo", "clave");
    }

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
    void dadoElValorDeUnDatoPersonalDeUnDocente_cuandoSeConstruye_entoncesSeRechaza(String campo) {
        Map<String, Object> contenido = valida();
        contenido.put("anterior", Map.of(campo, "valor"));

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "docentes", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining(campo);
        Map<String, Object> enNuevo = valida();
        enNuevo.put("nuevo", Map.of(campo, "valor"));
        assertThatThrownBy(() -> EntradaBitacora.de(ID, "docentes", enNuevo))
                .isInstanceOf(EntradaBitacoraInvalida.class);
    }

    @Test
    void dadoLosCamposPersonalesNombradosSinValores_cuandoSeConstruye_entoncesSeAcepta() {
        Map<String, Object> contenido = valida();
        contenido.put(
                "objeto",
                Map.of(
                        "identificacion",
                        "0100000001",
                        "camposPersonales",
                        List.of("telefono", "condicionEspecial")));
        contenido.put("anterior", Map.of("dedicacion", "MEDIO_TIEMPO"));
        contenido.put("nuevo", Map.of("dedicacion", "TIEMPO_COMPLETO"));

        assertThat(EntradaBitacora.de(ID, "docentes", contenido).objeto())
                .containsKey("camposPersonales");
    }

    @Test
    void dadoUnCampoLlamadoTelefonoEnOtroServicio_cuandoSeConstruye_entoncesSeAcepta() {
        Map<String, Object> contenido = valida();
        contenido.put("nuevo", Map.of("telefono", "no es de un docente"));

        assertThat(EntradaBitacora.de(ID, "academico", contenido).nuevo()).isPresent();
    }

    @Test
    void dadoUnContenidoDe64Kb_cuandoSeConstruye_entoncesSeAcepta() {
        Map<String, Object> contenido = valida();
        contenido.put("objeto", Map.of("texto", relleno(contenido, EntradaBitacora.TAMANO_MAXIMO)));

        assertThat(EntradaBitacora.de(ID, "generacion", contenido)).isNotNull();
    }

    @Test
    void dadoUnContenidoDeMasDe64Kb_cuandoSeConstruye_entoncesSeRechaza() {
        Map<String, Object> contenido = valida();
        contenido.put(
                "objeto", Map.of("texto", relleno(contenido, EntradaBitacora.TAMANO_MAXIMO + 1)));

        assertThatThrownBy(() -> EntradaBitacora.de(ID, "generacion", contenido))
                .isInstanceOf(EntradaBitacoraInvalida.class)
                .hasMessageContaining("64 KB");
    }

    @Test
    void dadoUnaEntrada_cuandoSeConvierteEnTexto_entoncesNoMuestraElContenido() {
        EntradaBitacora entrada = EntradaBitacora.de(ID, "generacion", valida());

        assertThat(entrada.toString()).contains("ACTIVIDAD_GUARDADA").doesNotContain("TUTORIAS");
    }

    @Test
    void dadoUnaEntrada_cuandoSeIntentaCambiarSusMapas_entoncesNoSePuede() {
        EntradaBitacora entrada = EntradaBitacora.de(ID, "generacion", valida());

        assertThatThrownBy(() -> entrada.objeto().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Map<String, Object> valida() {
        Map<String, Object> contenido = new HashMap<>();
        contenido.put("usuario", "coordinacion.demo");
        contenido.put("accion", "ACTIVIDAD_GUARDADA");
        contenido.put("momento", "2026-11-06T15:00:00Z");
        contenido.put("periodo", "2026-I");
        contenido.put(
                "objeto",
                Map.of(
                        "periodo",
                        "2026-I",
                        "carrera",
                        "TDS",
                        "docente",
                        "0100000001",
                        "actividad",
                        "TUTORIAS"));
        contenido.put("anterior", Map.of("horas", "4"));
        contenido.put("nuevo", Map.of("horas", "6"));
        return contenido;
    }

    private static Map<String, Object> con(String campo, Object valor) {
        Map<String, Object> contenido = valida();
        contenido.put(campo, valor);
        return contenido;
    }

    // Un texto que deja el contenido con exactamente el tamaño pedido.
    private static String relleno(Map<String, Object> base, int tamano) {
        Map<String, Object> sinObjeto = new HashMap<>(base);
        sinObjeto.put("objeto", Map.of("texto", ""));
        int actual = EntradaBitacora.tamanoDe(sinObjeto);
        return "x".repeat(tamano - actual);
    }
}
