package ec.mdd.cargas.dominio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Una de las 10 cargas de la ERS §8.3: el archivo de una entidad que entra al MDD con validación y
 * vista previa (glosario «Carga»). No es la carga horaria.
 *
 * @param numero su orden, de 1 a 10
 * @param codigo el nombre de su archivo sin el número, como {@code estructura_organica}
 * @param nombre su nombre, como «Estructura orgánica»
 * @param archivo el nombre de su plantilla, como {@code 01_estructura_organica.csv}
 * @param dependeDe los números de las cargas que tienen que estar antes
 * @param obligatoriedad si es necesaria para crear el distributivo
 * @param columnas sus columnas, en el orden de la plantilla, con campos sin repetir
 */
public record Carga(
        int numero,
        String codigo,
        String nombre,
        String archivo,
        List<Integer> dependeDe,
        Obligatoriedad obligatoriedad,
        List<ColumnaDeCarga> columnas) {

    private static final Pattern CODIGO = Pattern.compile("^[a-z_]{1,40}$");

    private static final String EXTENSION = ".csv";

    /** Valida la carga y copia sus listas. */
    public Carga {
        if (numero < 1 || numero > 10) {
            throw new IllegalArgumentException("Las cargas se numeran de 1 a 10, no " + numero);
        }
        String donde = "La carga " + numero + ": ";
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new IllegalArgumentException(
                    donde + "el código «" + codigo + "» solo admite minúsculas y guion bajo");
        }
        nombre = Textos.obligatorio(nombre, donde + "el nombre");
        archivo = Textos.obligatorio(archivo, donde + "el archivo");
        if (!archivo.endsWith(EXTENSION)) {
            throw new IllegalArgumentException(donde + "el archivo tiene que ser " + EXTENSION);
        }
        if (obligatoriedad == null) {
            throw new IllegalArgumentException(donde + "falta su obligatoriedad");
        }
        if (columnas == null || columnas.isEmpty()) {
            throw new IllegalArgumentException(donde + "no tiene ninguna columna");
        }
        Set<String> campos = new HashSet<>();
        for (ColumnaDeCarga columna : columnas) {
            if (!campos.add(columna.campo())) {
                throw new IllegalArgumentException(
                        donde + "la columna " + columna.campo() + " se repite");
            }
        }
        dependeDe = List.copyOf(dependeDe == null ? List.of() : dependeDe);
        columnas = List.copyOf(columnas);
    }

    /** Su plantilla: el archivo y los encabezados, en orden. */
    public PlantillaCarga plantilla() {
        return new PlantillaCarga(archivo, columnas.stream().map(ColumnaDeCarga::campo).toList());
    }

    /**
     * Compara los encabezados de un archivo con los de su plantilla (RF-07). Los espacios de los
     * extremos se ignoran. Primero las columnas repetidas, luego las que faltan (en el orden de la
     * plantilla) y las que sobran (en el orden del archivo); si son las mismas columnas, la primera
     * fuera de lugar.
     *
     * @param encabezados los del archivo, en su orden
     * @return las diferencias; vacía si son iguales
     */
    public List<DiferenciaDeEncabezado> compararEncabezados(List<String> encabezados) {
        List<String> delArchivo = encabezados.stream().map(String::strip).toList();
        List<String> esperados = plantilla().encabezados();
        List<DiferenciaDeEncabezado> diferencias = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        Set<String> repetidas = new LinkedHashSet<>();
        for (String columna : delArchivo) {
            if (!vistos.add(columna)) {
                repetidas.add(columna);
            }
        }
        for (String columna : repetidas) {
            diferencias.add(new DiferenciaDeEncabezado(columna, MotivoDeDiferencia.REPETIDA, null));
        }
        for (String columna : esperados) {
            if (!vistos.contains(columna)) {
                diferencias.add(
                        new DiferenciaDeEncabezado(columna, MotivoDeDiferencia.FALTA, null));
            }
        }
        for (String columna : new LinkedHashSet<>(delArchivo)) {
            if (!esperados.contains(columna)) {
                diferencias.add(
                        new DiferenciaDeEncabezado(columna, MotivoDeDiferencia.SOBRA, null));
            }
        }
        if (diferencias.isEmpty()) {
            for (int i = 0; i < esperados.size(); i++) {
                String columna = delArchivo.get(i);
                if (!esperados.get(i).equals(columna)) {
                    diferencias.add(
                            new DiferenciaDeEncabezado(
                                    columna,
                                    MotivoDeDiferencia.FUERA_DE_LUGAR,
                                    esperados.indexOf(columna) + 1));
                    break;
                }
            }
        }
        return diferencias;
    }

    /**
     * Valida la estructura de una tabla de esta carga: los encabezados de su plantilla y al menos
     * un registro (RF-06, RF-07). El contenido de cada fila lo revisa F01-H03.
     *
     * @param tabla lo que entregó la fuente de datos
     * @return el número de registros
     * @throws EncabezadosDistintos si los encabezados no son los de la plantilla
     * @throws ArchivoSinRegistros si no tiene registros, o si el archivo está vacío
     */
    public int leer(TablaDeDatos tabla) {
        if (tabla.filas().isEmpty() && tabla.encabezados().stream().allMatch(String::isBlank)) {
            throw new ArchivoSinRegistros();
        }
        List<DiferenciaDeEncabezado> diferencias = compararEncabezados(tabla.encabezados());
        if (!diferencias.isEmpty()) {
            throw new EncabezadosDistintos(diferencias);
        }
        int registros = tabla.registros();
        if (registros == 0) {
            throw new ArchivoSinRegistros();
        }
        return registros;
    }

    /**
     * El nombre del archivo de su diccionario, como {@code 01_estructura_organica_diccionario.csv}.
     */
    public String archivoDelDiccionario() {
        return archivo.substring(0, archivo.length() - EXTENSION.length())
                + "_diccionario"
                + EXTENSION;
    }
}
