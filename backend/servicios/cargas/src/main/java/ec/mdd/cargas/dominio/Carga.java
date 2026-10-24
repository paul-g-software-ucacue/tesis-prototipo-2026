package ec.mdd.cargas.dominio;

import java.util.HashSet;
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
     * El nombre del archivo de su diccionario, como {@code 01_estructura_organica_diccionario.csv}.
     */
    public String archivoDelDiccionario() {
        return archivo.substring(0, archivo.length() - EXTENSION.length())
                + "_diccionario"
                + EXTENSION;
    }
}
