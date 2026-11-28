package ec.mdd.reporting.dominio;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Una columna del layout de exportación: su encabezado, de dónde sale y, si su campo tiene un
 * catálogo cerrado, la traducción de sus códigos a los de SIGA (D-119). Su posición es la de la
 * lista del layout.
 *
 * @param nombre el encabezado en el CSV
 * @param campo de dónde sale el valor
 * @param traduccion código → valor en SIGA, en el orden del catálogo; vacía si no se traduce
 */
public record ColumnaDelLayout(
        String nombre, CampoDelLayout campo, Map<String, String> traduccion) {

    /** Letra al inicio, letras ASCII, dígitos o «_», de 1 a 64. Nunca empieza como fórmula. */
    static final Pattern NOMBRE = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    /** De 1 a 30, empezando con letra o dígito. */
    static final Pattern VALOR = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 ._-]{0,29}$");

    /** Exige un nombre válido y, si hay traducción, que cubra exactamente el catálogo. */
    public ColumnaDelLayout {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(campo, "campo");
        if (!NOMBRE.matcher(nombre).matches()) {
            throw new IllegalArgumentException("nombre de columna inválido: " + nombre);
        }
        traduccion = traduccion == null ? Map.of() : traduccion;
        if (!traduccion.isEmpty()) {
            if (!campo.traducible()
                    || !traduccion.keySet().equals(new java.util.HashSet<>(campo.codigos()))) {
                throw new IllegalArgumentException(
                        "traducción que no cubre el catálogo de " + campo);
            }
            Map<String, String> ordenada = new LinkedHashMap<>();
            for (String codigo : campo.codigos()) {
                ordenada.put(codigo, traduccion.get(codigo));
            }
            traduccion = Collections.unmodifiableMap(ordenada);
        }
    }

    /**
     * El valor de esta columna en una fila: el de su campo, traducido si la columna traduce, o
     * {@link ValorDeColumna#NO_APLICA} si no aplica a la fila o el dato falta.
     *
     * @param fila la fila
     * @return su valor tipado, sin formato de texto
     */
    public ValorDeColumna valor(FilaSiga fila) {
        Publicacion publicacion = fila.publicacion();
        DatosDelDocente docente = fila.docente();
        return switch (campo) {
            case PERIODO -> new ValorDeColumna.Texto(publicacion.periodo());
            case VERSION -> new ValorDeColumna.Entero(publicacion.version());
            case FECHA_PUBLICACION ->
                    new ValorDeColumna.Fecha(
                            LocalDate.ofInstant(publicacion.publicadaEn(), ECUADOR));
            case HASH_VERSION -> new ValorDeColumna.Texto(publicacion.hash());
            case TIPO_IDENTIFICACION -> codigo(docente.tipoIdentificacion().map(Enum::name));
            case IDENTIFICACION -> new ValorDeColumna.Texto(docente.identificacion());
            case DOCENTE -> new ValorDeColumna.Texto(docente.nombre());
            case DEDICACION -> codigo(docente.dedicacion().map(Enum::name));
            case COMPONENTE -> codigo(Optional.of(fila.componente().name()));
            case ACTIVIDAD -> texto(fila.actividad());
            case CARRERA -> new ValorDeColumna.Texto(fila.carrera());
            case ASIGNATURA -> texto(fila.asignatura());
            case PARALELO -> texto(fila.paralelo());
            case PROYECTO -> texto(fila.proyecto());
            case HORAS -> new ValorDeColumna.Horas(fila.horas());
        };
    }

    private ValorDeColumna codigo(Optional<String> codigo) {
        return codigo.<ValorDeColumna>map(uno -> new ValorDeColumna.Texto(traducida(uno)))
                .orElse(ValorDeColumna.NO_APLICA);
    }

    private static ValorDeColumna texto(Optional<String> texto) {
        return texto.filter(uno -> !uno.isBlank())
                .<ValorDeColumna>map(ValorDeColumna.Texto::new)
                .orElse(ValorDeColumna.NO_APLICA);
    }

    /**
     * @param codigo un código del catálogo del campo
     * @return su valor en SIGA, o el mismo código si la columna no traduce
     */
    public String traducida(String codigo) {
        return traduccion.getOrDefault(codigo, codigo);
    }
}
