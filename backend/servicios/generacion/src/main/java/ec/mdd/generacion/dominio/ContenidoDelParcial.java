package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * El contenido de un parcial que se aprueba al enviarlo, en una forma fija, y su SHA-256 (F12-H02,
 * research §4). La misma entrada da siempre el mismo hash: un JSON con las claves ordenadas, sin
 * espacios, con lo que no es ASCII escrito como {@code \\uXXXX} y las horas con dos decimales.
 *
 * <p>Entran el periodo, la carrera, la versión, las propuestas de la carrera (asignatura, paralelo,
 * docente, tipo y texto de la justificación) ordenadas por asignatura y paralelo, y sus actividades
 * (docente, actividad, proyecto y horas) ordenadas por docente, actividad y proyecto. No entran
 * quién ni cuándo, ni los nombres, que se derivan de los códigos: no son contenido (como en D-94).
 */
public final class ContenidoDelParcial {

    private static final Comparator<String> NULOS_PRIMERO =
            Comparator.nullsFirst(Comparator.naturalOrder());

    private final String canonico;

    private ContenidoDelParcial(String canonico) {
        this.canonico = canonico;
    }

    /**
     * El contenido del parcial de una carrera en una versión.
     *
     * @param periodo el periodo del distributivo
     * @param carrera la carrera del parcial
     * @param version la versión con sus propuestas y actividades
     * @return el contenido, con su forma canónica y su hash
     */
    public static ContenidoDelParcial de(
            String periodo, String carrera, VersionDistributivo version) {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(carrera, "carrera");
        List<Object> propuestas =
                version.propuestas().stream()
                        .filter(una -> una.carrera().equals(carrera))
                        .sorted(
                                Comparator.comparing(Propuesta::asignatura)
                                        .thenComparing(Propuesta::paralelo))
                        .<Object>map(ContenidoDelParcial::propuesta)
                        .toList();
        List<Object> actividades =
                version.actividades().stream()
                        .filter(una -> una.carrera().equals(carrera))
                        .sorted(
                                Comparator.comparing(ActividadNoLectiva::docente)
                                        .thenComparing(ActividadNoLectiva::actividad)
                                        .thenComparing(ActividadNoLectiva::proyecto, NULOS_PRIMERO))
                        .<Object>map(ContenidoDelParcial::actividad)
                        .toList();
        Map<String, Object> contenido = new TreeMap<>();
        contenido.put("periodo", periodo);
        contenido.put("carrera", carrera);
        contenido.put("version", version.numero());
        contenido.put("propuestas", propuestas);
        contenido.put("actividades", actividades);
        return new ContenidoDelParcial(json(contenido));
    }

    /**
     * El hash del distributivo al pasar de etapa: el de los hashes de sus parciales, ordenados por
     * carrera.
     *
     * @param periodo el periodo del distributivo
     * @param version el número de la versión
     * @param hashes el hash de cada parcial, por carrera
     * @return el SHA-256 en hexadecimal
     */
    public static String hashDelDistributivo(
            String periodo, int version, Map<String, String> hashes) {
        List<Object> parciales = new ArrayList<>();
        new TreeMap<>(hashes)
                .forEach(
                        (carrera, hash) -> {
                            Map<String, Object> uno = new TreeMap<>();
                            uno.put("carrera", carrera);
                            uno.put("hash", hash);
                            parciales.add(uno);
                        });
        Map<String, Object> contenido = new TreeMap<>();
        contenido.put("periodo", periodo);
        contenido.put("version", version);
        contenido.put("parciales", parciales);
        return sha256(json(contenido));
    }

    /** El JSON canónico. */
    public String canonico() {
        return canonico;
    }

    /** El SHA-256 del JSON canónico, en hexadecimal (64 caracteres). */
    public String hash() {
        return sha256(canonico);
    }

    private static Map<String, Object> propuesta(Propuesta propuesta) {
        Map<String, Object> justificacion = new TreeMap<>();
        justificacion.put("tipo", propuesta.justificacion().tipo().name());
        justificacion.put("texto", propuesta.justificacion().texto());
        Map<String, Object> una = new TreeMap<>();
        una.put("asignatura", propuesta.asignatura());
        una.put("paralelo", propuesta.paralelo());
        una.put("docente", propuesta.docente());
        una.put("justificacion", justificacion);
        return una;
    }

    private static Map<String, Object> actividad(ActividadNoLectiva actividad) {
        Map<String, Object> una = new TreeMap<>();
        una.put("docente", actividad.docente());
        una.put("actividad", actividad.actividad());
        una.put("proyecto", actividad.proyecto());
        una.put("horas", actividad.horas().setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        return una;
    }

    // Solo los tipos que arma esta clase: mapas ordenados, listas, textos, enteros y nulos.
    private static String json(Object valor) {
        StringBuilder texto = new StringBuilder();
        escribir(texto, valor);
        return texto.toString();
    }

    private static void escribir(StringBuilder texto, Object valor) {
        switch (valor) {
            case null -> texto.append("null");
            case Map<?, ?> mapa -> {
                texto.append('{');
                String separador = "";
                for (Map.Entry<?, ?> entrada : new LinkedHashMap<>(mapa).entrySet()) {
                    texto.append(separador);
                    cadena(texto, String.valueOf(entrada.getKey()));
                    texto.append(':');
                    escribir(texto, entrada.getValue());
                    separador = ",";
                }
                texto.append('}');
            }
            case List<?> lista -> {
                texto.append('[');
                String separador = "";
                for (Object elemento : lista) {
                    texto.append(separador);
                    escribir(texto, elemento);
                    separador = ",";
                }
                texto.append(']');
            }
            case Integer numero -> texto.append(numero);
            case BigDecimal numero -> texto.append(numero.toPlainString());
            case String cadena -> cadena(texto, cadena);
            default -> throw new IllegalArgumentException("Tipo no admitido: " + valor.getClass());
        }
    }

    private static void cadena(StringBuilder texto, String valor) {
        texto.append('"');
        for (char caracter : valor.toCharArray()) {
            switch (caracter) {
                case '"' -> texto.append("\\\"");
                case '\\' -> texto.append("\\\\");
                default -> {
                    if (caracter < 0x20 || caracter > 0x7e) {
                        texto.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        texto.append(caracter);
                    }
                }
            }
        }
        texto.append('"');
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
