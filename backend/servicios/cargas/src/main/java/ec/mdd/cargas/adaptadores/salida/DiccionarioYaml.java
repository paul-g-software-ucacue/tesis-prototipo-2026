package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.dominio.Carga;
import ec.mdd.cargas.dominio.Catalogo;
import ec.mdd.cargas.dominio.ColumnaDeCarga;
import ec.mdd.cargas.dominio.Convencion;
import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.dominio.Obligatoriedad;
import ec.mdd.cargas.dominio.ValorDeCatalogo;
import ec.mdd.cargas.puertos.salida.FuenteDelDiccionario;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * El diccionario de datos de {@value #ARCHIVO}, en los recursos del servicio (research §2): las 10
 * cargas, sus columnas, las convenciones y los catálogos, transcritos de la ERS v2.1. Se lee con
 * SnakeYAML en modo seguro, como {@code usuarios-demo.yaml}.
 */
public final class DiccionarioYaml implements FuenteDelDiccionario {

    /** Archivo con la definición, en los recursos del servicio. */
    public static final String ARCHIVO = "cargas/diccionario-de-datos.yaml";

    @Override
    public DiccionarioDeDatos leer() {
        try (InputStream archivo = DiccionarioYaml.class.getResourceAsStream("/" + ARCHIVO)) {
            if (archivo == null) {
                throw new IllegalStateException("Falta " + ARCHIVO + " en los recursos");
            }
            return leer(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Lee un diccionario de datos en YAML.
     *
     * @param yaml el YAML, con {@code convenciones}, {@code catalogos} y {@code cargas}
     * @return el diccionario validado
     * @throws IllegalArgumentException si no está completo, con la carga y la columna
     */
    public static DiccionarioDeDatos leer(InputStream yaml) {
        Map<?, ?> raiz = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        List<Convencion> convenciones = new ArrayList<>();
        for (Map<?, ?> fila : lista(raiz, "convenciones")) {
            convenciones.add(new Convencion(texto(fila, "tema"), texto(fila, "regla")));
        }
        List<Catalogo> catalogos = new ArrayList<>();
        for (Map<?, ?> fila : lista(raiz, "catalogos")) {
            List<ValorDeCatalogo> valores = new ArrayList<>();
            for (Map<?, ?> valor : lista(fila, "valores")) {
                valores.add(
                        new ValorDeCatalogo(texto(valor, "codigo"), texto(valor, "descripcion")));
            }
            catalogos.add(new Catalogo(texto(fila, "codigo"), texto(fila, "nombre"), valores));
        }
        List<Carga> cargas = new ArrayList<>();
        for (Map<?, ?> fila : lista(raiz, "cargas")) {
            cargas.add(carga(fila));
        }
        return new DiccionarioDeDatos(cargas, convenciones, catalogos);
    }

    private static Carga carga(Map<?, ?> fila) {
        Object numero = fila.get("numero");
        try {
            List<ColumnaDeCarga> columnas = new ArrayList<>();
            for (Map<?, ?> columna : lista(fila, "columnas")) {
                columnas.add(
                        new ColumnaDeCarga(
                                texto(columna, "campo"),
                                texto(columna, "descripcion"),
                                texto(columna, "tipo"),
                                (Integer) columna.get("longitud"),
                                texto(columna, "obligatorio"),
                                texto(columna, "formato"),
                                texto(columna, "clave"),
                                texto(columna, "validacion"),
                                texto(columna, "ejemplo"),
                                texto(columna, "catalogo")));
            }
            List<Integer> dependeDe = new ArrayList<>();
            Object anteriores = fila.get("dependeDe");
            if (anteriores instanceof List<?> numeros) {
                for (Object anterior : numeros) {
                    dependeDe.add((Integer) anterior);
                }
            }
            return new Carga(
                    (Integer) numero,
                    texto(fila, "codigo"),
                    texto(fila, "nombre"),
                    texto(fila, "archivo"),
                    dependeDe,
                    Obligatoriedad.valueOf(texto(fila, "obligatoriedad")),
                    columnas);
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            String motivo = String.valueOf(e.getMessage());
            throw new IllegalArgumentException(
                    motivo.startsWith("La carga ") ? motivo : "La carga " + numero + ": " + motivo,
                    e);
        }
    }

    private static List<Map<?, ?>> lista(Map<?, ?> padre, String clave) {
        List<Map<?, ?>> filas = new ArrayList<>();
        if (padre.get(clave) instanceof List<?> elementos) {
            for (Object elemento : elementos) {
                filas.add((Map<?, ?>) elemento);
            }
        }
        return filas;
    }

    private static String texto(Map<?, ?> fila, String clave) {
        Object valor = fila.get(clave);
        return valor == null ? null : String.valueOf(valor);
    }
}
