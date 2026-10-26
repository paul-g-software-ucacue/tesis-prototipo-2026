package ec.mdd.cargas.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Las columnas que forman la clave de una carga: la columna {@code PK}, o las que nombra {@code PK
 * compuesta: a + b + c} (research §7). La clave no se repite dentro del archivo.
 *
 * @param campo la columna que declara la clave, donde se informa la repetida
 * @param campos las columnas de la clave, en su orden
 */
public record ClaveDeCarga(String campo, List<String> campos) {

    private static final String SIMPLE = "PK";
    private static final String COMPUESTA = "PK compuesta: ";

    /** Los nombres que usa el diccionario en las claves y las partes, con su columna. */
    private static final Map<String, String> NOMBRES =
            Map.of(
                    "periodo", "codigo_periodo",
                    "asignatura", "codigo_asignatura",
                    "versión", "codigo_version_politica",
                    "identificación", "numero_identificacion",
                    "cargo", "codigo_cargo");

    /** Copia los campos. */
    public ClaveDeCarga {
        campos = List.copyOf(campos);
    }

    /**
     * La clave de una carga, si la tiene.
     *
     * @throws IllegalArgumentException si la clave compuesta nombra algo que no es una columna
     */
    public static Optional<ClaveDeCarga> de(Carga carga) {
        for (ColumnaDeCarga columna : carga.columnas()) {
            if (columna.clave().equals(SIMPLE)) {
                return Optional.of(new ClaveDeCarga(columna.campo(), List.of(columna.campo())));
            }
            if (columna.clave().startsWith(COMPUESTA)) {
                List<String> campos = new ArrayList<>();
                for (String nombre : columna.clave().substring(COMPUESTA.length()).split(" \\+ ")) {
                    campos.add(campoDe(nombre.strip(), carga, columna));
                }
                return Optional.of(new ClaveDeCarga(columna.campo(), campos));
            }
        }
        return Optional.empty();
    }

    /** La columna que nombra el diccionario: la del mismo nombre, o la de la tabla de nombres. */
    static String campoDe(String nombre, Carga carga, ColumnaDeCarga donde) {
        String campo = NOMBRES.getOrDefault(nombre, nombre);
        boolean existe = carga.columnas().stream().anyMatch(c -> c.campo().equals(campo));
        if (!existe) {
            throw new IllegalArgumentException(
                    "La columna " + donde.campo() + ": no sé qué columna es «" + nombre + "»");
        }
        return campo;
    }

    /** El campo de otra carga que corresponde a un nombre del diccionario, sin comprobarlo. */
    static String campoDe(String nombre) {
        return NOMBRES.getOrDefault(nombre, nombre);
    }

    /** El motivo de una clave repetida. */
    String motivo(int filaAnterior) {
        if (campos.size() == 1) {
            return "Esta clave ya está en la fila " + filaAnterior + ": no la repita.";
        }
        return "Esta combinación de "
                + String.join(", ", campos.subList(0, campos.size() - 1))
                + " y "
                + campos.getLast()
                + " ya está en la fila "
                + filaAnterior
                + ": no la repita.";
    }
}
