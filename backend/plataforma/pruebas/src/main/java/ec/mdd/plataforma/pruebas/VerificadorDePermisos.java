package ec.mdd.plataforma.pruebas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * La prueba de autorización por operación de RNF-SEG-03 (ADR-021): une el permiso que el contrato
 * declara con {@code x-mdd-permiso} y el {@code @PreAuthorize} del controlador.
 *
 * <p>Por cada operación con {@code x-mdd-permiso}, llama con MockMvc:
 *
 * <ul>
 *   <li>con un token que tiene todos los demás permisos del contrato, pero ninguno de los suyos:
 *       espera 403 con {@code SEGURIDAD_ACCESO_NO_AUTORIZADO};
 *   <li>con un token que solo tiene uno de sus permisos, una vez por cada uno: espera algo distinto
 *       de 403.
 * </ul>
 *
 * <p>{@code x-mdd-permiso} es un permiso o una lista de permisos alternativos, de los que basta con
 * uno (D-70).
 *
 * <p>Las operaciones con cuerpo se llaman con {@code {}}: con el permiso, un 400 basta para saber
 * que la autorización pasó. Los parámetros de la ruta toman su {@code example} del contrato.
 */
public final class VerificadorDePermisos {

    private static final String EXTENSION = "x-mdd-permiso";
    private static final String CODIGO_403 = "SEGURIDAD_ACCESO_NO_AUTORIZADO";
    private static final Pattern PARAMETRO = Pattern.compile("\\{([^}]+)}");

    private final List<OperacionProtegida> operaciones;

    private VerificadorDePermisos(Path contrato) {
        ParseOptions opciones = new ParseOptions();
        opciones.setResolve(true);
        opciones.setResolveFully(true);
        OpenAPI api = new OpenAPIV3Parser().read(contrato.toString(), null, opciones);
        if (api == null) {
            throw new IllegalStateException("No se pudo leer el contrato " + contrato);
        }
        this.operaciones = leer(api);
    }

    /**
     * Verificador para el contrato de un servicio.
     *
     * @param servicio nombre del servicio, por ejemplo {@code roles}
     * @return el verificador de {@code contratos/<servicio>/openapi.yaml}
     */
    public static VerificadorDePermisos para(String servicio) {
        return desdeArchivo(CarpetaDeContratos.buscar().resolve(servicio).resolve("openapi.yaml"));
    }

    /** Verificador para un archivo de contrato cualquiera. */
    public static VerificadorDePermisos desdeArchivo(Path contrato) {
        return new VerificadorDePermisos(contrato);
    }

    /** Las operaciones del contrato que declaran {@code x-mdd-permiso}. */
    public List<OperacionProtegida> operaciones() {
        return operaciones;
    }

    /**
     * Llama cada operación protegida sin su permiso y con él.
     *
     * @param mvc MockMvc con la seguridad de los procesos y los controladores del servicio
     * @param conversor el conversor de tokens del proceso, que da las autoridades {@code PERMISO_}
     * @throws AssertionError con todas las operaciones que no cumplen, o si el contrato no tiene
     *     ninguna operación protegida
     */
    public void verificar(
            MockMvc mvc, Converter<Jwt, ? extends AbstractAuthenticationToken> conversor) {
        if (operaciones.isEmpty()) {
            throw new AssertionError("El contrato no tiene operaciones con " + EXTENSION);
        }
        TreeSet<String> todos = new TreeSet<>();
        operaciones.forEach(operacion -> todos.addAll(operacion.permisos()));
        List<String> fallas = new ArrayList<>();
        for (OperacionProtegida operacion : operaciones) {
            List<String> demas = new ArrayList<>(todos);
            demas.removeAll(operacion.permisos());
            MockHttpServletResponse sin = llamar(mvc, conversor, operacion, demas);
            if (sin.getStatus() != 403 || !cuerpo(sin).contains(CODIGO_403)) {
                fallas.add(
                        operacion + ": sin el permiso respondió " + sin.getStatus() + ", no 403");
            }
            for (String permiso : operacion.permisos()) {
                MockHttpServletResponse con = llamar(mvc, conversor, operacion, List.of(permiso));
                if (con.getStatus() == 403) {
                    fallas.add(operacion + ": con " + permiso + " respondió 403");
                }
            }
        }
        if (!fallas.isEmpty()) {
            throw new AssertionError(
                    "Operaciones cuyo código no exige el permiso del contrato:\n  "
                            + String.join("\n  ", fallas));
        }
    }

    private static MockHttpServletResponse llamar(
            MockMvc mvc,
            Converter<Jwt, ? extends AbstractAuthenticationToken> conversor,
            OperacionProtegida operacion,
            List<String> permisos) {
        RequestPostProcessor token =
                jwt().jwt(
                                jwt ->
                                        jwt.subject("verificador.permisos")
                                                .claim("roles", List.of())
                                                .claim("permisos", permisos))
                        .authorities((Jwt jwt) -> autoridades(conversor, jwt));
        HttpMethod metodo = HttpMethod.valueOf(operacion.metodo());
        RequestBuilder peticion;
        if (!operacion.partes().isEmpty()) {
            // Un archivo de prueba en cada parte, para que el 403 no lo tape un 400 o un 415.
            MockMultipartHttpServletRequestBuilder multipart =
                    MockMvcRequestBuilders.multipart(metodo, operacion.ruta());
            for (String parte : operacion.partes()) {
                multipart.file(
                        new MockMultipartFile(parte, "verificador.csv", null, new byte[] {'x'}));
            }
            peticion = multipart.with(token);
        } else if (operacion.conCuerpo()) {
            peticion =
                    MockMvcRequestBuilders.request(metodo, operacion.ruta())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")
                            .with(token);
        } else {
            peticion = MockMvcRequestBuilders.request(metodo, operacion.ruta()).with(token);
        }
        try {
            return mvc.perform(peticion).andReturn().getResponse();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo llamar " + operacion, e);
        }
    }

    private static Collection<GrantedAuthority> autoridades(
            Converter<Jwt, ? extends AbstractAuthenticationToken> conversor, Jwt token) {
        return Objects.requireNonNull(conversor.convert(token)).getAuthorities();
    }

    private static String cuerpo(MockHttpServletResponse respuesta) {
        try {
            return respuesta.getContentAsString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo leer la respuesta", e);
        }
    }

    private static List<OperacionProtegida> leer(OpenAPI api) {
        List<OperacionProtegida> protegidas = new ArrayList<>();
        if (api.getPaths() == null) {
            return protegidas;
        }
        api.getPaths()
                .forEach(
                        (ruta, item) ->
                                item.readOperationsMap()
                                        .forEach(
                                                (metodo, operacion) ->
                                                        agregar(
                                                                protegidas,
                                                                ruta,
                                                                item,
                                                                metodo,
                                                                operacion)));
        return List.copyOf(protegidas);
    }

    private static void agregar(
            List<OperacionProtegida> protegidas,
            String ruta,
            PathItem item,
            PathItem.HttpMethod metodo,
            Operation operacion) {
        Map<String, Object> extensiones = operacion.getExtensions();
        Object permiso = extensiones == null ? null : extensiones.get(EXTENSION);
        if (permiso == null) {
            return;
        }
        List<Parameter> parametros = new ArrayList<>();
        if (item.getParameters() != null) {
            parametros.addAll(item.getParameters());
        }
        if (operacion.getParameters() != null) {
            parametros.addAll(operacion.getParameters());
        }
        protegidas.add(
                new OperacionProtegida(
                        metodo.name().toUpperCase(Locale.ROOT),
                        conEjemplos(ruta, parametros),
                        permiso instanceof List<?> lista
                                ? lista.stream().map(Object::toString).toList()
                                : List.of(permiso.toString()),
                        operacion.getRequestBody() != null,
                        partes(operacion)));
    }

    // Las partes de un cuerpo multipart/form-data, para mandar un archivo en cada una (F01-H02).
    private static List<String> partes(Operation operacion) {
        if (operacion.getRequestBody() == null || operacion.getRequestBody().getContent() == null) {
            return List.of();
        }
        var multipart = operacion.getRequestBody().getContent().get("multipart/form-data");
        if (multipart == null
                || multipart.getSchema() == null
                || multipart.getSchema().getProperties() == null) {
            return List.of();
        }
        return List.copyOf(multipart.getSchema().getProperties().keySet());
    }

    // Cambia cada {parametro} de la ruta por su ejemplo del contrato, o por «x» si no tiene.
    private static String conEjemplos(String ruta, List<Parameter> parametros) {
        Matcher coincidencia = PARAMETRO.matcher(ruta);
        StringBuilder resultado = new StringBuilder();
        while (coincidencia.find()) {
            String nombre = coincidencia.group(1);
            String ejemplo =
                    parametros.stream()
                            .filter(parametro -> nombre.equals(parametro.getName()))
                            .map(VerificadorDePermisos::ejemplo)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("x");
            coincidencia.appendReplacement(resultado, Matcher.quoteReplacement(ejemplo));
        }
        coincidencia.appendTail(resultado);
        return resultado.toString();
    }

    private static String ejemplo(Parameter parametro) {
        if (parametro.getExample() != null) {
            return parametro.getExample().toString();
        }
        if (parametro.getSchema() != null && parametro.getSchema().getExample() != null) {
            return parametro.getSchema().getExample().toString();
        }
        return null;
    }

    /**
     * Una operación del contrato con sus permisos.
     *
     * @param metodo método HTTP, en mayúsculas
     * @param ruta ruta con los parámetros ya reemplazados por sus ejemplos
     * @param permisos los de {@code x-mdd-permiso}, por ejemplo {@code USUARIOS_ROLES:LECTURA}; si
     *     son varios, alternativos: basta con uno (D-70)
     * @param conCuerpo si la operación recibe un cuerpo
     * @param partes las partes de un cuerpo {@code multipart/form-data}, donde va un archivo de
     *     prueba; vacía si el cuerpo es JSON o no hay cuerpo
     */
    public record OperacionProtegida(
            String metodo,
            String ruta,
            List<String> permisos,
            boolean conCuerpo,
            List<String> partes) {

        /** Copia las listas. */
        public OperacionProtegida {
            permisos = List.copyOf(permisos);
            partes = List.copyOf(partes);
        }

        @Override
        public String toString() {
            String declarados = permisos.size() == 1 ? permisos.getFirst() : permisos.toString();
            return metodo + " " + ruta + " (" + declarados + ")";
        }
    }
}
