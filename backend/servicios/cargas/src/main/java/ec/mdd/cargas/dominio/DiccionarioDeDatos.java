package ec.mdd.cargas.dominio;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * El diccionario de datos de las 10 cargas: sus columnas, las convenciones comunes y los catálogos
 * (RF-05, ERS §8.3, glosario «Diccionario de datos»). Es la definición de la que salen la plantilla
 * y el diccionario de cada carga, y con la que se validan las cargas y sus filas (F01-H02,
 * F01-H03). Una definición incompleta lanza {@link IllegalArgumentException} con la carga y la
 * columna.
 */
public final class DiccionarioDeDatos {

    /** Cuántas cargas tiene la ERS §8.3. */
    public static final int CARGAS = 10;

    private final List<Carga> cargas;
    private final List<Convencion> convenciones;
    private final Map<String, Catalogo> catalogos = new LinkedHashMap<>();
    private final Map<String, ValidadorDeFilas> validadores = new LinkedHashMap<>();

    /**
     * Valida la definición completa.
     *
     * @param cargas las 10 cargas, en el orden de la ERS §8.3
     * @param convenciones las reglas comunes a todas las cargas
     * @param catalogos los catálogos que nombran las columnas, sin códigos repetidos
     */
    public DiccionarioDeDatos(
            List<Carga> cargas, List<Convencion> convenciones, List<Catalogo> catalogos) {
        for (Catalogo catalogo : catalogos) {
            if (this.catalogos.putIfAbsent(catalogo.codigo(), catalogo) != null) {
                throw new IllegalArgumentException(
                        "El catálogo " + catalogo.codigo() + " se repite");
            }
        }
        if (cargas.size() != CARGAS) {
            throw new IllegalArgumentException(
                    "El diccionario tiene que tener las "
                            + CARGAS
                            + " cargas, no "
                            + cargas.size());
        }
        Set<String> codigos = new HashSet<>();
        Set<String> archivos = new HashSet<>();
        for (int posicion = 1; posicion <= CARGAS; posicion++) {
            Carga carga = cargas.get(posicion - 1);
            if (carga.numero() != posicion) {
                throw new IllegalArgumentException(
                        "En la posición "
                                + posicion
                                + " va la carga "
                                + posicion
                                + ", no la "
                                + carga.numero());
            }
            if (!codigos.add(carga.codigo())) {
                throw new IllegalArgumentException(
                        "La carga " + posicion + " repite el código " + carga.codigo());
            }
            if (!archivos.add(carga.archivo())) {
                throw new IllegalArgumentException(
                        "La carga " + posicion + " repite el archivo " + carga.archivo());
            }
            for (int anterior : carga.dependeDe()) {
                if (anterior < 1 || anterior >= posicion) {
                    throw new IllegalArgumentException(
                            "La carga "
                                    + posicion
                                    + " solo puede depender de una carga anterior,"
                                    + " no de la "
                                    + anterior);
                }
            }
            for (ColumnaDeCarga columna : carga.columnas()) {
                Optional<String> catalogo = columna.catalogoDeValores();
                if (catalogo.isPresent() && !this.catalogos.containsKey(catalogo.get())) {
                    throw new IllegalArgumentException(
                            "La carga "
                                    + posicion
                                    + ", columna "
                                    + columna.campo()
                                    + ": no existe el catálogo "
                                    + catalogo.get());
                }
            }
        }
        this.cargas = List.copyOf(cargas);
        this.convenciones = List.copyOf(convenciones);
        for (Carga carga : this.cargas) {
            validadores.put(
                    carga.codigo(),
                    new ValidadorDeFilas(
                            carga,
                            columna ->
                                    valoresDe(columna).stream()
                                            .map(ValorDeCatalogo::codigo)
                                            .toList(),
                            this::valoresDeOtraCarga));
        }
    }

    /** Las 10 cargas, en el orden de la ERS §8.3. */
    public List<Carga> cargas() {
        return cargas;
    }

    /** Las convenciones comunes a todas las cargas. */
    public List<Convencion> convenciones() {
        return convenciones;
    }

    /** Los catálogos, en el orden en que se definieron. */
    public List<Catalogo> catalogos() {
        return List.copyOf(catalogos.values());
    }

    /** La carga con ese código, si existe. */
    public Optional<Carga> carga(String codigo) {
        return cargas.stream().filter(carga -> carga.codigo().equals(codigo)).findFirst();
    }

    /** Los valores permitidos de una columna: los de su catálogo, o ninguno. */
    public List<ValorDeCatalogo> valoresDe(ColumnaDeCarga columna) {
        return columna.catalogoDeValores()
                .map(catalogos::get)
                .map(Catalogo::valores)
                .orElse(List.of());
    }

    /**
     * El validador de las filas de una carga, con sus reglas leídas al construir el diccionario
     * (FR-012).
     */
    public ValidadorDeFilas validador(Carga carga) {
        return validadores.get(carga.codigo());
    }

    /** Los valores permitidos de una columna de una carga anterior, como «02.modalidad». */
    private List<String> valoresDeOtraCarga(int numero, String campo) {
        ValidadorDeFilas anterior = validadores.get(cargas.get(numero - 1).codigo());
        if (anterior == null) {
            throw new IllegalArgumentException(
                    "Una columna solo puede tomar los valores de una carga anterior, no de la "
                            + numero);
        }
        return anterior.reglas(campo).valoresPermitidos();
    }

    /** Cuántas columnas tienen las 10 cargas juntas. */
    public int columnas() {
        return cargas.stream().mapToInt(carga -> carga.columnas().size()).sum();
    }
}
