package ec.mdd.cargas.dominio;

import java.util.ArrayList;
import java.util.List;

/** Definiciones pequeñas para probar las reglas del diccionario, sin el YAML real. */
final class DiccionarioDePrueba {

    private DiccionarioDePrueba() {}

    /** Una columna válida, con su campo. */
    static ColumnaDeCarga columna(String campo) {
        return new ColumnaDeCarga(
                campo, "Descripción de " + campo, "Texto", 20, "Sí", "", "", "", "ABC", null);
    }

    /** Una columna válida que toma sus valores de un catálogo. */
    static ColumnaDeCarga columnaConCatalogo(String campo, String catalogo) {
        return new ColumnaDeCarga(
                campo, "Descripción", "Catálogo", 3, "Sí", "", "", "", "G01", catalogo);
    }

    /** Una carga válida, con su número, que depende de las que se indiquen. */
    static Carga carga(int numero, List<Integer> dependeDe, ColumnaDeCarga... columnas) {
        return new Carga(
                numero,
                "carga_" + (char) ('a' + numero - 1),
                "Carga " + numero,
                String.format("%02d_carga.csv", numero),
                dependeDe,
                numero == 10 ? Obligatoriedad.RECOMENDADA : Obligatoriedad.OBLIGATORIA,
                columnas.length == 0 ? List.of(columna("codigo")) : List.of(columnas));
    }

    /** Las 10 cargas válidas, sin dependencias. */
    static List<Carga> diezCargas() {
        List<Carga> cargas = new ArrayList<>();
        for (int numero = 1; numero <= 10; numero++) {
            cargas.add(carga(numero, List.of()));
        }
        return cargas;
    }

    /** El catálogo de prueba {@code GESTIONES}. */
    static Catalogo gestiones() {
        return new Catalogo(
                "GESTIONES",
                "Gestiones autorizadas",
                List.of(
                        new ValorDeCatalogo("G01", "Rector"),
                        new ValorDeCatalogo("G02", "Vicerrector")));
    }

    /** Las convenciones de prueba. */
    static List<Convencion> convenciones() {
        return List.of(new Convencion("Fechas", "aaaa-mm-dd (ISO 8601)."));
    }

    /** Un diccionario con esas cargas, las convenciones y el catálogo de prueba. */
    static DiccionarioDeDatos diccionario(List<Carga> cargas) {
        return new DiccionarioDeDatos(cargas, convenciones(), List.of(gestiones()));
    }
}
