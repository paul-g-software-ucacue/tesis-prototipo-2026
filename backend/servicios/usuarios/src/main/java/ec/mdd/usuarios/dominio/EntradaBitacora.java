package ec.mdd.usuarios.dominio;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Una entrada de la bitácora (glosario §7.6, lenguaje publicado {@code EntradaBitacora} §4): quién,
 * qué, cuándo, objeto, valor anterior y valor nuevo de un cambio ya guardado en otro servicio
 * (RF-44). Se crea una vez y no cambia nunca.
 *
 * <p>Invariantes, que {@link #de} valida y que fallan con {@link EntradaBitacoraInvalida}:
 *
 * <ul>
 *   <li>el servicio es uno de los que registran cambios (D-90);
 *   <li>{@code usuario}, {@code accion}, {@code momento} y {@code objeto} existen, con su forma;
 *       {@code periodo} es opcional; no hay otros campos;
 *   <li>{@code anterior} y {@code nuevo} no faltan los dos;
 *   <li>ninguna clave de {@code objeto}, {@code anterior} ni {@code nuevo}, a cualquier
 *       profundidad, parece un secreto (FR-011), salvo las de {@link #NO_SON_SECRETOS} (D-92);
 *   <li>una entrada de {@code docentes} no lleva en {@code anterior} ni en {@code nuevo} el valor
 *       de un dato personal (FR-014, D-91);
 *   <li>el contenido no pasa de {@link #TAMANO_MAXIMO} bytes, medido con {@link #tamanoDe}.
 * </ul>
 */
public final class EntradaBitacora {

    /** 64 KB: una entrada describe un cambio, no un archivo. */
    public static final int TAMANO_MAXIMO = 64 * 1024;

    private static final Set<String> SERVICIOS =
            Set.of("generacion", "academico", "docentes", "normativa", "roles", "validacionocs");
    private static final Set<String> CAMPOS =
            Set.of("usuario", "accion", "momento", "periodo", "objeto", "anterior", "nuevo");
    private static final Pattern ACCION = Pattern.compile("^[A-Z][A-Z_]{2,39}$");
    private static final Pattern PERIODO = Pattern.compile("^[A-Za-z0-9._-]{1,20}$");
    private static final int USUARIO_MAXIMO = 64;
    private static final List<String> PARECEN_SECRETOS =
            List.of("clave", "contrasena", "contraseña", "password", "token", "secreto", "jwt");

    /**
     * Las claves que parecen un secreto y no lo son, escritas tal cual (D-92): las palabras clave
     * de una asignatura de Académico.
     */
    public static final Set<String> NO_SON_SECRETOS = Set.of("palabrasClave");

    /** Los datos personales de un docente que no llegan con su valor (D-91). */
    public static final Set<String> DATOS_PERSONALES_DE_DOCENTE =
            Set.of(
                    "primerNombre",
                    "segundoNombre",
                    "primerApellido",
                    "segundoApellido",
                    "correoInstitucional",
                    "telefono",
                    "condicionEspecial");

    private final UUID idEvento;
    private final String servicio;
    private final String usuario;
    private final String accion;
    private final Instant momento;
    private final String periodo;
    private final Map<String, Object> objeto;
    private final Map<String, Object> anterior;
    private final Map<String, Object> nuevo;

    private EntradaBitacora(
            UUID idEvento,
            String servicio,
            String usuario,
            String accion,
            Instant momento,
            String periodo,
            Map<String, Object> objeto,
            Map<String, Object> anterior,
            Map<String, Object> nuevo) {
        this.idEvento = idEvento;
        this.servicio = servicio;
        this.usuario = usuario;
        this.accion = accion;
        this.momento = momento;
        this.periodo = periodo;
        this.objeto = objeto;
        this.anterior = anterior;
        this.nuevo = nuevo;
    }

    /**
     * Construye la entrada desde el contenido de un evento {@code <servicio>.EntradaBitacora}.
     *
     * @param idEvento el identificador del evento, que es también el de la entrada
     * @param servicio el servicio de origen, ya comprobado contra el tipo del evento
     * @param contenido el contenido del evento, tal como llegó
     * @return la entrada válida
     * @throws EntradaBitacoraInvalida si no cumple alguna invariante
     */
    public static EntradaBitacora de(
            UUID idEvento, String servicio, Map<String, Object> contenido) {
        if (idEvento == null) {
            throw new EntradaBitacoraInvalida("Falta el idEvento.");
        }
        if (servicio == null || !SERVICIOS.contains(servicio)) {
            throw new EntradaBitacoraInvalida("El servicio de origen no registra en la bitácora.");
        }
        if (contenido == null) {
            throw new EntradaBitacoraInvalida("Falta el contenido.");
        }
        for (String campo : contenido.keySet()) {
            if (!CAMPOS.contains(campo)) {
                throw new EntradaBitacoraInvalida("El campo " + campo + " no está en el contrato.");
            }
        }
        String usuario = usuario(contenido.get("usuario"));
        String accion = texto(contenido, "accion");
        if (!ACCION.matcher(accion).matches()) {
            throw new EntradaBitacoraInvalida("El campo accion no tiene la forma " + ACCION);
        }
        Instant momento = momento(contenido.get("momento"));
        String periodo = periodo(contenido.get("periodo"));
        Map<String, Object> objeto = mapa(contenido, "objeto", true);
        Map<String, Object> anterior = mapa(contenido, "anterior", false);
        Map<String, Object> nuevo = mapa(contenido, "nuevo", false);
        if (anterior == null && nuevo == null) {
            throw new EntradaBitacoraInvalida("Faltan anterior y nuevo: debe haber al menos uno.");
        }
        sinSecretos(objeto);
        sinSecretos(anterior);
        sinSecretos(nuevo);
        if ("docentes".equals(servicio)) {
            sinDatosPersonales(anterior);
            sinDatosPersonales(nuevo);
        }
        if (tamanoDe(contenido) > TAMANO_MAXIMO) {
            throw new EntradaBitacoraInvalida("El contenido pasa de 64 KB.");
        }
        return new EntradaBitacora(
                idEvento, servicio, usuario, accion, momento, periodo, objeto, anterior, nuevo);
    }

    /**
     * El tamaño del contenido: la suma, en UTF-8, de cada clave y de cada valor convertido en
     * texto, a cualquier profundidad.
     *
     * @param contenido el contenido de un evento
     * @return los bytes contados
     */
    public static int tamanoDe(Map<String, Object> contenido) {
        return tamano(contenido);
    }

    public UUID idEvento() {
        return idEvento;
    }

    public String servicio() {
        return servicio;
    }

    public String usuario() {
        return usuario;
    }

    public String accion() {
        return accion;
    }

    public Instant momento() {
        return momento;
    }

    /** El periodo del cambio; vacío si no pertenece a uno. */
    public Optional<String> periodo() {
        return Optional.ofNullable(periodo);
    }

    public Map<String, Object> objeto() {
        return objeto;
    }

    /** Los valores antes del cambio; vacío si el cambio creó algo. */
    public Optional<Map<String, Object>> anterior() {
        return Optional.ofNullable(anterior);
    }

    /** Los valores después del cambio; vacío si el cambio quitó algo. */
    public Optional<Map<String, Object>> nuevo() {
        return Optional.ofNullable(nuevo);
    }

    /** Sin el contenido, para que no llegue al registro del proceso. */
    @Override
    public String toString() {
        return "EntradaBitacora[idEvento="
                + idEvento
                + ", servicio="
                + servicio
                + ", accion="
                + accion
                + "]";
    }

    private static String usuario(Object valor) {
        if (!(valor instanceof String texto)) {
            throw new EntradaBitacoraInvalida("Falta el campo usuario, o no es un texto.");
        }
        if (texto.isEmpty() || texto.length() > USUARIO_MAXIMO) {
            throw new EntradaBitacoraInvalida("El campo usuario debe tener de 1 a 64 caracteres.");
        }
        return texto;
    }

    private static String texto(Map<String, Object> contenido, String campo) {
        if (!(contenido.get(campo) instanceof String texto)) {
            throw new EntradaBitacoraInvalida("Falta el campo " + campo + ", o no es un texto.");
        }
        return texto;
    }

    private static Instant momento(Object valor) {
        if (!(valor instanceof String texto)) {
            throw new EntradaBitacoraInvalida("Falta el campo momento, o no es un texto.");
        }
        try {
            return OffsetDateTime.parse(texto).toInstant();
        } catch (DateTimeParseException e) {
            throw new EntradaBitacoraInvalida("El campo momento no es una fecha y hora ISO 8601.");
        }
    }

    private static String periodo(Object valor) {
        if (valor == null) {
            return null;
        }
        if (!(valor instanceof String texto) || !PERIODO.matcher(texto).matches()) {
            throw new EntradaBitacoraInvalida("El campo periodo no tiene la forma " + PERIODO);
        }
        return texto;
    }

    private static Map<String, Object> mapa(
            Map<String, Object> contenido, String campo, boolean obligatorio) {
        Object valor = contenido.get(campo);
        if (valor == null) {
            if (obligatorio) {
                throw new EntradaBitacoraInvalida("Falta el campo " + campo + ".");
            }
            return null;
        }
        if (!(valor instanceof Map<?, ?> mapa)) {
            throw new EntradaBitacoraInvalida("El campo " + campo + " no es un objeto.");
        }
        return copia(mapa);
    }

    private static Map<String, Object> copia(Map<?, ?> mapa) {
        Map<String, Object> copia = new LinkedHashMap<>();
        mapa.forEach((clave, valor) -> copia.put(String.valueOf(clave), valor));
        return Collections.unmodifiableMap(copia);
    }

    // Recorre mapas y listas; solo mira las claves, nunca los valores.
    private static void sinSecretos(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                String original = String.valueOf(entrada.getKey());
                String clave = original.toLowerCase(Locale.ROOT);
                for (String secreto : PARECEN_SECRETOS) {
                    if (clave.contains(secreto) && !NO_SON_SECRETOS.contains(original)) {
                        throw new EntradaBitacoraInvalida(
                                "Una clave del contenido parece un secreto: " + secreto);
                    }
                }
                sinSecretos(entrada.getValue());
            }
        } else if (valor instanceof Collection<?> lista) {
            lista.forEach(EntradaBitacora::sinSecretos);
        }
    }

    private static void sinDatosPersonales(Map<String, Object> valores) {
        if (valores == null) {
            return;
        }
        for (String campo : valores.keySet()) {
            if (DATOS_PERSONALES_DE_DOCENTE.contains(campo)) {
                throw new EntradaBitacoraInvalida(
                        "El valor de " + campo + " es un dato personal de un docente (D-91).");
            }
        }
    }

    private static int tamano(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            int total = 0;
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                total += bytes(String.valueOf(entrada.getKey())) + tamano(entrada.getValue());
            }
            return total;
        }
        if (valor instanceof Collection<?> lista) {
            int total = 0;
            for (Object elemento : lista) {
                total += tamano(elemento);
            }
            return total;
        }
        return valor == null ? 0 : bytes(String.valueOf(valor));
    }

    private static int bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8).length;
    }
}
