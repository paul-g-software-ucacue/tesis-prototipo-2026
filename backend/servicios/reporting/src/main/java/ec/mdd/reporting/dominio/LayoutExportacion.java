package ec.mdd.reporting.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * El layout de exportación: el mapeo de columnas y catálogos del CSV para SIGA (RF-52, ADR-002,
 * D-119). Adaptar el CSV al formato de SIGA es cargar otro archivo, no cambiar el código (principio
 * VI).
 *
 * <p>Es inmutable: cada carga es una versión nueva del layout de exportación, que no se edita ni se
 * borra (principio VII). La versión 0 es el layout por defecto, que sale de la plantilla.
 *
 * @param numero la versión del layout de exportación; 0 para el layout por defecto
 * @param columnas de 1 a 30, en el orden del archivo, que es su posición en el CSV
 * @param texto el archivo tal cual, sin la marca de orden de bytes
 * @param sha256 el SHA-256 del texto en UTF-8, en hexadecimal
 * @param quienLoCargo quién lo cargó; {@code null} en el layout por defecto
 * @param cuandoSeCargo cuándo; {@code null} en el layout por defecto
 */
public record LayoutExportacion(
        int numero,
        List<ColumnaDelLayout> columnas,
        String texto,
        String sha256,
        String quienLoCargo,
        Instant cuandoSeCargo) {

    /** Cuántas columnas puede tener como mucho. */
    public static final int MAXIMO_DE_COLUMNAS = 30;

    private static final String ARCHIVO = "archivo";

    /** Exige columnas válidas y que solo el layout por defecto no tenga quién ni cuándo. */
    public LayoutExportacion {
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(sha256, "sha256");
        columnas = List.copyOf(columnas);
        if (numero < 0) {
            throw new IllegalArgumentException("versión del layout negativa");
        }
        if ((numero == 0) != (quienLoCargo == null) || (numero == 0) != (cuandoSeCargo == null)) {
            throw new IllegalArgumentException(
                    "solo el layout por defecto (versión 0) no tiene quién ni cuándo");
        }
        if (columnas.isEmpty() || columnas.size() > MAXIMO_DE_COLUMNAS) {
            throw new IllegalArgumentException("de 1 a 30 columnas");
        }
    }

    /**
     * @param texto el texto de la plantilla
     * @param columnas sus columnas, ya validadas
     * @return el layout por defecto, versión 0
     */
    public static LayoutExportacion porDefecto(String texto, List<ColumnaDelLayout> columnas) {
        return new LayoutExportacion(0, columnas, texto, huella(texto), null, null);
    }

    /**
     * @param numero la versión del layout de exportación, desde 1
     * @param texto el archivo cargado
     * @param columnas sus columnas, ya validadas
     * @param quien quién lo cargó
     * @param momento cuándo
     * @return la versión cargada
     */
    public static LayoutExportacion cargado(
            int numero,
            String texto,
            List<ColumnaDelLayout> columnas,
            String quien,
            Instant momento) {
        if (numero < 1) {
            throw new IllegalArgumentException("una versión cargada empieza en 1");
        }
        Objects.requireNonNull(quien, "quien");
        Objects.requireNonNull(momento, "momento");
        return new LayoutExportacion(numero, columnas, texto, huella(texto), quien, momento);
    }

    /** Si es el layout por defecto. */
    public boolean esPorDefecto() {
        return numero == 0;
    }

    /** Quién lo cargó; vacío en el layout por defecto. */
    public Optional<String> cargadoPor() {
        return Optional.ofNullable(quienLoCargo);
    }

    /** Cuándo se cargó; vacío en el layout por defecto. */
    public Optional<Instant> cargadoEn() {
        return Optional.ofNullable(cuandoSeCargo);
    }

    /** Los nombres de las columnas, en orden: la primera fila del CSV. */
    public List<String> encabezado() {
        return columnas.stream().map(ColumnaDelLayout::nombre).toList();
    }

    /**
     * Valida un archivo de mapeo con todas las reglas de FR-015 y devuelve todos sus problemas, no
     * solo el primero, para que se corrija de una vez.
     *
     * @param borrador lo que entendió el lector
     * @return las columnas, o todos los problemas
     */
    public static ValidacionDelLayout validar(BorradorDeLayout borrador) {
        List<ProblemaDelLayout> problemas = new ArrayList<>(borrador.problemas());
        List<BorradorDeColumna> escritas = borrador.columnas();
        if (escritas.isEmpty()) {
            problemas.add(
                    new ProblemaDelLayout(
                            ARCHIVO,
                            "El archivo no tiene columnas. Agrega al menos una en «columnas»."));
        } else if (escritas.size() > MAXIMO_DE_COLUMNAS) {
            problemas.add(
                    new ProblemaDelLayout(
                            ARCHIVO,
                            "Tiene "
                                    + escritas.size()
                                    + " columnas; el máximo es "
                                    + MAXIMO_DE_COLUMNAS
                                    + "."));
        }
        List<ColumnaDelLayout> columnas = new ArrayList<>();
        Map<String, Integer> nombres = new HashMap<>();
        Set<CampoDelLayout> campos = EnumSet.noneOf(CampoDelLayout.class);
        for (int i = 0; i < escritas.size(); i++) {
            BorradorDeColumna escrita = escritas.get(i);
            String donde =
                    "columnas["
                            + (i + 1)
                            + "]"
                            + (escrita.nombre() == null || escrita.nombre().isBlank()
                                    ? ""
                                    : " (" + escrita.nombre() + ")");
            int antes = problemas.size();
            revisarNombre(escrita.nombre(), donde, nombres, i + 1, problemas);
            Optional<CampoDelLayout> campo = revisarCampo(escrita.campo(), donde, problemas);
            campo.ifPresent(campos::add);
            if (campo.isPresent() && escrita.traduccion() != null) {
                revisarTraduccion(campo.get(), escrita.traduccion(), donde, problemas);
            }
            if (problemas.size() == antes && campo.isPresent()) {
                columnas.add(
                        new ColumnaDelLayout(escrita.nombre(), campo.get(), escrita.traduccion()));
            }
        }
        for (CampoDelLayout obligatorio : CampoDelLayout.values()) {
            if (obligatorio.obligatorio() && !campos.contains(obligatorio)) {
                problemas.add(
                        new ProblemaDelLayout(
                                ARCHIVO,
                                "Falta una columna con el campo «" + obligatorio.clave() + "»."));
            }
        }
        return problemas.isEmpty()
                ? new ValidacionDelLayout(columnas, List.of())
                : new ValidacionDelLayout(List.of(), problemas);
    }

    private static void revisarNombre(
            String nombre,
            String donde,
            Map<String, Integer> nombres,
            int posicion,
            List<ProblemaDelLayout> problemas) {
        if (nombre == null || nombre.isBlank()) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde, "Falta «nombre». Escribe el encabezado de la columna."));
            return;
        }
        if (!ColumnaDelLayout.NOMBRE.matcher(nombre).matches()) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde,
                            "El nombre «"
                                    + nombre
                                    + "» no es válido: empieza con una letra y usa solo letras"
                                    + " sin tildes, dígitos o «_», hasta 64 caracteres."));
            return;
        }
        Integer primera = nombres.putIfAbsent(nombre.toUpperCase(Locale.ROOT), posicion);
        if (primera != null) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde,
                            "El nombre «"
                                    + nombre
                                    + "» ya está en columnas["
                                    + primera
                                    + "]. Cada columna necesita un nombre distinto."));
        }
    }

    private static Optional<CampoDelLayout> revisarCampo(
            String clave, String donde, List<ProblemaDelLayout> problemas) {
        if (clave == null || clave.isBlank()) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde, "Falta «campo». Usa uno de la lista de la plantilla."));
            return Optional.empty();
        }
        Optional<CampoDelLayout> campo = CampoDelLayout.deClave(clave);
        if (campo.isEmpty()) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde,
                            "El campo «"
                                    + clave
                                    + "» no existe. Usa uno de la lista de la plantilla."));
        }
        return campo;
    }

    private static void revisarTraduccion(
            CampoDelLayout campo,
            Map<String, String> traduccion,
            String donde,
            List<ProblemaDelLayout> problemas) {
        if (!campo.traducible()) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde,
                            "El campo «"
                                    + campo.clave()
                                    + "» no admite traducción. Quita «traduccion»."));
            return;
        }
        List<String> sobran =
                traduccion.keySet().stream()
                        .filter(codigo -> !campo.codigos().contains(codigo))
                        .toList();
        for (String codigo : sobran) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde,
                            "«"
                                    + codigo
                                    + "» no es un código de «"
                                    + campo.clave()
                                    + "»: usa solo "
                                    + String.join(", ", campo.codigos())
                                    + "."));
        }
        List<String> faltan =
                campo.codigos().stream().filter(codigo -> !traduccion.containsKey(codigo)).toList();
        if (!faltan.isEmpty()) {
            problemas.add(
                    new ProblemaDelLayout(
                            donde, "Falta traducir: " + String.join(", ", faltan) + "."));
        }
        Map<String, String> primeroConValor = new HashMap<>();
        for (Map.Entry<String, String> par : traduccion.entrySet()) {
            String codigo = par.getKey();
            String valor = par.getValue() == null ? "" : par.getValue();
            if ("NA".equals(valor.toUpperCase(Locale.ROOT))) {
                problemas.add(
                        new ProblemaDelLayout(
                                donde,
                                "«"
                                        + codigo
                                        + "» no puede traducirse a «"
                                        + valor
                                        + "», que significa «no aplica»."));
            } else if (!ColumnaDelLayout.VALOR.matcher(valor).matches()) {
                problemas.add(
                        new ProblemaDelLayout(
                                donde,
                                "El valor «"
                                        + valor
                                        + "» de «"
                                        + codigo
                                        + "» no es válido: de 1 a 30 letras sin tildes, dígitos,"
                                        + " espacio, «.», «_» o «-», empezando con letra o"
                                        + " dígito."));
            } else {
                String otro = primeroConValor.putIfAbsent(valor.toUpperCase(Locale.ROOT), codigo);
                if (otro != null) {
                    problemas.add(
                            new ProblemaDelLayout(
                                    donde,
                                    "El valor «"
                                            + valor
                                            + "» se repite en «"
                                            + otro
                                            + "» y «"
                                            + codigo
                                            + "»: cada código necesita un valor distinto."));
                }
            }
        }
    }

    /**
     * @param texto el texto de un archivo de mapeo
     * @return su SHA-256 en UTF-8, en hexadecimal: el que se guarda y va a la bitácora
     */
    public static String huella(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("la JVM no tiene SHA-256", imposible);
        }
    }
}
