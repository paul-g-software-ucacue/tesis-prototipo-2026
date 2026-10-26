package ec.mdd.cargas.dominio;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Si una celda puede quedar vacía: «Sí», «No» o una de las 5 condiciones del diccionario de datos
 * (research §4). Son la única prosa del diccionario que esta historia interpreta, porque deciden si
 * una celda puede faltar; un texto que no es ninguno de estos detiene la carga del diccionario.
 *
 * @param modo siempre, nunca, si otra columna tiene un valor o salvo que lo tenga
 * @param campo la otra columna, o vacío
 * @param valor su valor, o vacío
 */
public record CondicionDeObligatoriedad(Modo modo, String campo, String valor) {

    /** Cuándo es obligatoria. */
    public enum Modo {
        /** «Sí». */
        SIEMPRE,
        /** «No». */
        NUNCA,
        /** «Sí si …»: cuando la otra columna tiene ese valor. */
        SI,
        /** «Sí salvo …»: salvo cuando la otra columna tiene ese valor. */
        SALVO
    }

    private static final Pattern SI_CAMPO = Pattern.compile("Sí si ([a-z_]+) = ([A-Z0-9_]+)");
    private static final Pattern SI_VALOR = Pattern.compile("Sí si ([A-Z0-9_]+)");
    private static final Pattern SALVO_TEXTO = Pattern.compile("Sí salvo (.+)");

    /** Los valores que el diccionario nombra en prosa en «Sí salvo …». */
    private static final Map<String, String> EN_PROSA =
            Map.of("coordinador de carrera", "COORDINADOR_CARRERA");

    /**
     * Lee la obligatoriedad de una columna. «Sí si TITULAR» busca la columna de la carga que admite
     * ese valor.
     *
     * @param columna la columna
     * @param carga su carga
     * @param valoresDe los valores permitidos de cada campo de la carga
     * @throws IllegalArgumentException si el texto no es uno de los conocidos
     */
    static CondicionDeObligatoriedad de(
            ColumnaDeCarga columna, Carga carga, Function<String, List<String>> valoresDe) {
        String texto = columna.obligatorio();
        if (texto.equals("Sí")) {
            return new CondicionDeObligatoriedad(Modo.SIEMPRE, "", "");
        }
        if (texto.equals("No")) {
            return new CondicionDeObligatoriedad(Modo.NUNCA, "", "");
        }
        Matcher siCampo = SI_CAMPO.matcher(texto);
        if (siCampo.matches()) {
            return new CondicionDeObligatoriedad(Modo.SI, siCampo.group(1), siCampo.group(2));
        }
        Matcher siValor = SI_VALOR.matcher(texto);
        if (siValor.matches()) {
            String valor = siValor.group(1);
            return new CondicionDeObligatoriedad(
                    Modo.SI, campoQueAdmite(valor, columna, carga, valoresDe), valor);
        }
        Matcher salvo = SALVO_TEXTO.matcher(texto);
        if (salvo.matches() && EN_PROSA.containsKey(salvo.group(1))) {
            String valor = EN_PROSA.get(salvo.group(1));
            return new CondicionDeObligatoriedad(
                    Modo.SALVO, campoQueAdmite(valor, columna, carga, valoresDe), valor);
        }
        throw new IllegalArgumentException(
                "La columna "
                        + columna.campo()
                        + ": la obligatoriedad «"
                        + texto
                        + "» no es «Sí», «No» ni una de las condiciones conocidas");
    }

    /** Si la celda es obligatoria en esa fila, con sus valores por campo. */
    public boolean exige(Map<String, String> fila) {
        return switch (modo) {
            case SIEMPRE -> true;
            case NUNCA -> false;
            case SI -> valor.equals(fila.get(campo));
            case SALVO -> !valor.equals(fila.get(campo));
        };
    }

    /** El motivo cuando falta: qué hacer y, si es condicional, cuándo. */
    public String motivo() {
        return switch (modo) {
            case SI -> "Es obligatorio cuando " + campo + " es " + valor + ": complétalo.";
            case SALVO -> "Es obligatorio salvo cuando " + campo + " es " + valor + ": complétalo.";
            default -> "Es obligatorio: complétalo.";
        };
    }

    private static String campoQueAdmite(
            String valor,
            ColumnaDeCarga columna,
            Carga carga,
            Function<String, List<String>> valoresDe) {
        return carga.columnas().stream()
                .map(ColumnaDeCarga::campo)
                .filter(campo -> valoresDe.apply(campo).contains(valor))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "La columna "
                                                + columna.campo()
                                                + ": ninguna columna de su carga admite "
                                                + valor));
    }
}
