package ec.mdd.cargas.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las reglas de una columna de una carga, leídas una vez del diccionario de datos (FR-012, research
 * §3): el tipo, el largo, los valores permitidos, el patrón, el rango y las partes de una lista. Es
 * el único lugar donde se traduce el {@code formato} del diccionario; lo que no reconoce no se
 * valida (principio XIV: no se asume una regla sin definición) y {@link #formatoInterpretado()} lo
 * dice, para que una prueba liste esos formatos. La obligatoriedad, la cédula, la clave y las
 * referencias, que dependen de la fila o del archivo, las aplica {@link ValidadorDeFilas}.
 */
public final class ReglasDeColumna {

    private static final int VALORES_EN_EL_MOTIVO = 10;
    private static final String ENUMERACION =
            "[A-Z0-9_]+(?:\\.\\.\\d+)?(?:, [A-Z0-9_]+(?:\\.\\.\\d+)?)+";
    private static final Pattern ENUMERADO = Pattern.compile(ENUMERACION);
    private static final Pattern ENUMERADO_EN_LISTA =
            Pattern.compile("[A-Z0-9_]+(?:; [A-Z0-9_]+)+");
    private static final Pattern RANGO_DE_EXPANSION =
            Pattern.compile("([A-Z_]+?)(\\d+)\\.\\.(\\d+)");
    private static final Pattern DIGITOS = Pattern.compile("(\\d) dígitos");
    private static final Pattern ENTRE =
            Pattern.compile("(\\d+(?:,\\d+)?)–(\\d+(?:,\\d+)?)(?: \\(punto decimal\\))?");
    private static final Pattern COTA = Pattern.compile("(≥|>|≤) (\\d+(?:,\\d+)?)");
    private static final Pattern IGUAL_QUE =
            Pattern.compile("Igual catálogo que (\\d{2})\\.([a-z_]+)");
    private static final Pattern LISTA_CON_PARTES = Pattern.compile("Lista \\(;\\) de «(.+)»");
    private static final Pattern PARTE_ENUMERADA =
            Pattern.compile("(\\p{L}+): (" + ENUMERACION + ")");
    private static final Pattern PARTE_DECIMAL = Pattern.compile("(\\p{L}+) decimal");

    private static final Pattern ENTERO = Pattern.compile("\\d+");
    private static final Pattern DECIMAL = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Pattern FECHA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern HORA = Pattern.compile("(?:[01]\\d|2[0-3]):[0-5]\\d");
    private static final Pattern CODIGO_DE_UNIDAD = Pattern.compile("[A-Z0-9_-]+");
    private static final Pattern PERIODO = Pattern.compile("\\d{4}-(?:I|II)");
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    /** El {@code formato} de la identificación del docente; la regla la aplica el validador. */
    static final String FORMATO_DE_IDENTIFICACION =
            "Cédula: 10 dígitos como TEXTO (conservar el 0 inicial)";

    /** Los formatos de texto que definen sus caracteres, con su patrón y su motivo. */
    private static final Map<String, Patron> TEXTOS =
            Map.of(
                    "Mayúsculas", Patron.MAYUSCULAS,
                    "Mayúsculas; vacío si no tiene", Patron.MAYUSCULAS,
                    "Mayúsculas, dígitos, «-» o «_»",
                            new Patron(
                                    CODIGO_DE_UNIDAD, "Usa solo mayúsculas, dígitos, «-» o «_»."),
                    "AAAA-I o AAAA-II (ej.: 2026-I)",
                            new Patron(
                                    PERIODO,
                                    "Escribe el periodo como AAAA-I o AAAA-II, por ejemplo 2026-I."),
                    "usuario@dominio",
                            new Patron(CORREO, "Escribe un correo como usuario@dominio."));

    /** Los tipos de columna del diccionario. */
    private enum Tipo {
        TEXTO,
        ENTERO,
        DECIMAL,
        FECHA,
        SN,
        CATALOGO,
        LISTA
    }

    private final ColumnaDeCarga columna;
    private final Tipo tipo;
    private final List<String> valores;
    private final Patron patron;
    private final Rango rango;
    private final List<String> elementos;
    private final List<String> partes;
    private final Map<Integer, List<String>> valoresDeParte = new HashMap<>();
    private final Set<Integer> partesHora = new HashSet<>();
    private final Set<Integer> partesDecimal = new HashSet<>();
    private final boolean identificacion;
    private boolean interpretado;

    /**
     * Lee las reglas de una columna.
     *
     * @param columna la columna del diccionario
     * @param catalogo los valores de su catálogo, si lo tiene
     * @param otraColumna los valores permitidos de la columna de otra carga, por número y campo
     * @throws IllegalArgumentException si el tipo no es uno del diccionario
     */
    ReglasDeColumna(
            ColumnaDeCarga columna,
            List<String> catalogo,
            BiFunction<Integer, String, List<String>> otraColumna) {
        this.columna = columna;
        this.tipo = tipo(columna);
        String formato = columna.formato();
        this.interpretado = formato.isEmpty();
        this.identificacion = formato.equals(FORMATO_DE_IDENTIFICACION);
        if (identificacion) {
            interpretado = true;
        }
        List<String> permitidos = List.of();
        Patron delFormato = TEXTOS.get(formato);
        Rango delRango = null;
        List<String> enLista = List.of();
        if (delFormato != null) {
            interpretado = true;
        }
        if (!catalogo.isEmpty()) {
            permitidos = catalogo;
            interpretado = true;
        } else if (ENUMERADO.matcher(formato).matches()) {
            permitidos = enumeracion(formato);
            interpretado = true;
        }
        Matcher igual = IGUAL_QUE.matcher(formato);
        if (igual.matches()) {
            permitidos = otraColumna.apply(Integer.parseInt(igual.group(1)), igual.group(2));
            interpretado = true;
        }
        Matcher digitos = DIGITOS.matcher(formato);
        if (digitos.matches()) {
            delFormato =
                    new Patron(
                            Pattern.compile("\\d{" + digitos.group(1) + "}"),
                            "Escribe exactamente " + digitos.group(1) + " dígitos.");
            interpretado = true;
        }
        if (tipo == Tipo.ENTERO || tipo == Tipo.DECIMAL) {
            delRango = Rango.de(formato);
            if (delRango != null) {
                interpretado = true;
            }
        }
        if ((tipo == Tipo.FECHA && formato.equals("aaaa-mm-dd"))
                || (tipo == Tipo.SN && formato.equals("S o N"))) {
            interpretado = true;
        }
        Matcher conPartes = LISTA_CON_PARTES.matcher(columna.tipo());
        this.partes = conPartes.matches() ? List.of(conPartes.group(1).split("\\|")) : List.of();
        if (tipo == Tipo.LISTA
                && partes.isEmpty()
                && ENUMERADO_EN_LISTA.matcher(formato).matches()) {
            enLista = List.of(formato.split("; "));
            interpretado = true;
        }
        if (!partes.isEmpty() && !formato.isEmpty()) {
            interpretado = clausulas(formato);
        }
        this.valores = permitidos;
        this.patron = delFormato;
        this.rango = delRango;
        this.elementos = enLista;
    }

    /** El campo de la columna. */
    public String campo() {
        return columna.campo();
    }

    /** Si su {@code formato} se tradujo a reglas; uno vacío no tiene nada que traducir. */
    public boolean formatoInterpretado() {
        return interpretado;
    }

    /** Los valores permitidos de un catálogo o de una enumeración; vacía si no los tiene. */
    public List<String> valoresPermitidos() {
        return valores;
    }

    /** Los nombres de las partes de cada elemento de una lista, como {@code día|inicio|fin}. */
    List<String> partes() {
        return partes;
    }

    /** Si es una lista separada por {@code ;}. */
    boolean esLista() {
        return tipo == Tipo.LISTA;
    }

    /** Si es la identificación de un docente, con la regla de la cédula o del pasaporte. */
    boolean esIdentificacion() {
        return identificacion;
    }

    /**
     * Revisa un valor que no está vacío, ya sin los espacios de los extremos.
     *
     * @param valor el valor de la celda
     * @return el motivo del primer problema, que dice qué hacer; vacío si pasa
     */
    public Optional<String> revisar(String valor) {
        Integer longitud = columna.longitud();
        int largo = valor.codePointCount(0, valor.length());
        if (longitud != null && largo > longitud) {
            return Optional.of("Tiene " + largo + " caracteres; el máximo es " + longitud + ".");
        }
        Optional<String> delTipo = revisarTipo(valor);
        if (delTipo.isPresent()) {
            return delTipo;
        }
        if (!valores.isEmpty() && !valores.contains(valor)) {
            return Optional.of("Usa uno de los valores permitidos: " + listar(valores) + ".");
        }
        if (patron != null && !patron.cumple(valor)) {
            return Optional.of(patron.motivo());
        }
        if (rango != null && !rango.contiene(new BigDecimal(valor))) {
            return Optional.of(rango.motivo());
        }
        if (tipo == Tipo.LISTA) {
            return revisarElementos(valor);
        }
        return Optional.empty();
    }

    /** Los elementos de una lista: separados por {@code ;}, sin espacios y sin los vacíos. */
    static List<String> elementos(String valor) {
        List<String> elementos = new ArrayList<>();
        for (String elemento : valor.split(";")) {
            if (!elemento.isBlank()) {
                elementos.add(elemento.strip());
            }
        }
        return elementos;
    }

    private Optional<String> revisarTipo(String valor) {
        return switch (tipo) {
            case ENTERO ->
                    ENTERO.matcher(valor).matches()
                            ? Optional.empty()
                            : Optional.of("Escribe un número entero, solo con dígitos.");
            case DECIMAL ->
                    DECIMAL.matcher(valor).matches()
                            ? Optional.empty()
                            : Optional.of(
                                    "Escribe un número con punto decimal, como 2.5, sin separador"
                                            + " de miles.");
            case FECHA ->
                    fecha(valor)
                            ? Optional.empty()
                            : Optional.of(
                                    "Escribe una fecha que exista, con el formato aaaa-mm-dd.");
            case SN ->
                    valor.equals("S") || valor.equals("N")
                            ? Optional.empty()
                            : Optional.of("Escribe S o N.");
            default -> Optional.empty();
        };
    }

    private Optional<String> revisarElementos(String valor) {
        List<String> lista = elementos(valor);
        for (int i = 0; i < lista.size(); i++) {
            String elemento = lista.get(i);
            String donde = "El elemento " + (i + 1);
            if (!elementos.isEmpty() && !elementos.contains(elemento)) {
                return Optional.of(
                        donde + " no es un valor permitido: " + String.join(", ", elementos) + ".");
            }
            if (partes.isEmpty()) {
                continue;
            }
            String[] trozos = elemento.split("\\|", -1);
            if (trozos.length != partes.size()) {
                return Optional.of(
                        donde
                                + " tiene "
                                + trozos.length
                                + " partes separadas por «|»; deben ser "
                                + partes.size()
                                + ": "
                                + String.join("|", partes)
                                + ".");
            }
            for (int parte = 0; parte < trozos.length; parte++) {
                String trozo = trozos[parte].strip();
                String nombre = "«" + partes.get(parte) + "»";
                List<String> permitidos = valoresDeParte.get(parte);
                if (permitidos != null && !permitidos.contains(trozo)) {
                    return Optional.of(
                            donde
                                    + ": "
                                    + nombre
                                    + " debe ser uno de "
                                    + String.join(", ", permitidos)
                                    + ".");
                }
                if (partesHora.contains(parte) && !HORA.matcher(trozo).matches()) {
                    return Optional.of(donde + ": " + nombre + " debe ser una hora HH:MM.");
                }
                if (partesDecimal.contains(parte) && !DECIMAL.matcher(trozo).matches()) {
                    return Optional.of(
                            donde + ": " + nombre + " debe ser un número con punto decimal.");
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Las cláusulas del formato de una lista con partes, separadas por {@code ; }: {@code parte: A,
     * B}, {@code horas HH:MM} (las partes {@code inicio} y {@code fin}) y {@code parte decimal}.
     *
     * @return si se interpretaron todas
     */
    private boolean clausulas(String formato) {
        for (String clausula : formato.split("; ")) {
            Matcher enumerada = PARTE_ENUMERADA.matcher(clausula);
            Matcher decimal = PARTE_DECIMAL.matcher(clausula);
            if (enumerada.matches()) {
                valoresDeParte.put(parte(enumerada.group(1)), enumeracion(enumerada.group(2)));
            } else if (clausula.equals("horas HH:MM")) {
                partesHora.add(parte("inicio"));
                partesHora.add(parte("fin"));
            } else if (decimal.matches()) {
                partesDecimal.add(parte(decimal.group(1)));
            } else {
                return false;
            }
        }
        return true;
    }

    private int parte(String nombre) {
        int indice = partes.indexOf(nombre);
        if (indice < 0) {
            throw new IllegalArgumentException(
                    "La columna "
                            + columna.campo()
                            + ": su formato nombra la parte «"
                            + nombre
                            + "», que no tiene");
        }
        return indice;
    }

    private static Tipo tipo(ColumnaDeCarga columna) {
        String tipo = columna.tipo();
        if (tipo.startsWith("Lista (;)")) {
            return Tipo.LISTA;
        }
        return switch (tipo) {
            case "Texto" -> Tipo.TEXTO;
            case "Entero" -> Tipo.ENTERO;
            case "Decimal" -> Tipo.DECIMAL;
            case "Fecha" -> Tipo.FECHA;
            case "S/N" -> Tipo.SN;
            case "Catálogo" -> Tipo.CATALOGO;
            default ->
                    throw new IllegalArgumentException(
                            "La columna "
                                    + columna.campo()
                                    + ": el tipo «"
                                    + tipo
                                    + "» no es uno del diccionario");
        };
    }

    /** Los códigos de una enumeración; {@code AGREGADO_1..3} da los tres. */
    private static List<String> enumeracion(String formato) {
        List<String> codigos = new ArrayList<>();
        for (String codigo : formato.split(", ")) {
            Matcher rango = RANGO_DE_EXPANSION.matcher(codigo);
            if (rango.matches()) {
                int desde = Integer.parseInt(rango.group(2));
                int hasta = Integer.parseInt(rango.group(3));
                for (int n = desde; n <= hasta; n++) {
                    codigos.add(rango.group(1) + n);
                }
            } else {
                codigos.add(codigo);
            }
        }
        return List.copyOf(codigos);
    }

    private static boolean fecha(String valor) {
        if (!FECHA.matcher(valor).matches()) {
            return false;
        }
        try {
            LocalDate.parse(valor);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String listar(List<String> valores) {
        if (valores.size() <= VALORES_EN_EL_MOTIVO) {
            return String.join(", ", valores);
        }
        return String.join(", ", valores.subList(0, VALORES_EN_EL_MOTIVO))
                + " y "
                + (valores.size() - VALORES_EN_EL_MOTIVO)
                + " más (mira el diccionario de la carga)";
    }

    /** Un patrón de caracteres del formato, con su motivo. */
    private record Patron(Pattern patron, String motivo) {

        /** «Mayúsculas»: sin letras minúsculas. */
        static final Patron MAYUSCULAS =
                new Patron(null, "Escríbelo en mayúsculas, como pide el diccionario.");

        boolean cumple(String valor) {
            if (patron == null) {
                return valor.equals(valor.toUpperCase(Locale.ROOT));
            }
            return patron.matcher(valor).matches();
        }
    }

    /** El rango de un entero o un decimal, como {@code 1–10}, {@code ≥ 0} o {@code > 0}. */
    private record Rango(BigDecimal minimo, boolean minimoIncluido, BigDecimal maximo) {

        static Rango de(String formato) {
            Matcher entre = ENTRE.matcher(formato);
            if (entre.matches()) {
                return new Rango(numero(entre.group(1)), true, numero(entre.group(2)));
            }
            Matcher cota = COTA.matcher(formato);
            if (cota.matches()) {
                BigDecimal limite = numero(cota.group(2));
                return switch (cota.group(1)) {
                    case "≥" -> new Rango(limite, true, null);
                    case ">" -> new Rango(limite, false, null);
                    default -> new Rango(null, true, limite);
                };
            }
            return null;
        }

        boolean contiene(BigDecimal valor) {
            if (minimo != null) {
                int comparado = valor.compareTo(minimo);
                if (comparado < 0 || (comparado == 0 && !minimoIncluido)) {
                    return false;
                }
            }
            return maximo == null || valor.compareTo(maximo) <= 0;
        }

        String motivo() {
            if (minimo != null && maximo != null) {
                return "Debe estar entre " + texto(minimo) + " y " + texto(maximo) + ".";
            }
            if (minimo != null) {
                return minimoIncluido
                        ? "Debe ser mayor o igual que " + texto(minimo) + "."
                        : "Debe ser mayor que " + texto(minimo) + ".";
            }
            return "Debe ser menor o igual que " + texto(maximo) + ".";
        }

        private static BigDecimal numero(String texto) {
            return new BigDecimal(texto.replace(',', '.'));
        }

        private static String texto(BigDecimal numero) {
            return numero.stripTrailingZeros().toPlainString();
        }
    }
}
