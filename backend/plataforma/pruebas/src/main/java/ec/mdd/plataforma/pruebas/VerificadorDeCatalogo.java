package ec.mdd.plataforma.pruebas;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprueba que los códigos del código coinciden con el catálogo documentado, {@code
 * contratos/comun/errores.md} (ADR-016).
 *
 * <p>Por cada código, falla si no tiene fila en la tabla o si su estado, su título o su descripción
 * no son iguales a los de su fila. Así el catálogo que lee la SPA y el que usa el código no se
 * desalinean.
 */
public final class VerificadorDeCatalogo {

    private VerificadorDeCatalogo() {}

    /**
     * Verifica las entradas de un catálogo de servicio contra {@code contratos/comun/errores.md}.
     */
    public static void verificar(CatalogoDeErrores catalogo) {
        verificar(catalogo.entradas().stream().map(EntradaCatalogo::definicion).toList());
    }

    /** Verifica definiciones (por ejemplo, los códigos comunes) contra {@code errores.md}. */
    public static void verificar(List<DefinicionDeCodigo> definiciones) {
        verificar(definiciones, CarpetaDeContratos.buscar().resolve("comun").resolve("errores.md"));
    }

    /**
     * Verifica definiciones contra un archivo de catálogo cualquiera.
     *
     * @throws AssertionError con todas las diferencias encontradas
     */
    public static void verificar(List<DefinicionDeCodigo> definiciones, Path catalogo) {
        Map<String, String[]> filas = leerFilas(catalogo);
        List<String> diferencias = new ArrayList<>();
        for (DefinicionDeCodigo definicion : definiciones) {
            String[] fila = filas.get(definicion.codigo());
            if (fila == null) {
                diferencias.add(definicion.codigo() + ": no está en " + catalogo.getFileName());
                continue;
            }
            comparar(
                    diferencias,
                    definicion.codigo(),
                    "estado",
                    String.valueOf(definicion.estado()),
                    fila[1]);
            comparar(diferencias, definicion.codigo(), "título", definicion.titulo(), fila[2]);
            comparar(
                    diferencias,
                    definicion.codigo(),
                    "descripción",
                    definicion.descripcion(),
                    fila[3]);
        }
        if (!diferencias.isEmpty()) {
            throw new AssertionError(
                    "El catálogo del código no coincide con "
                            + catalogo.getFileName()
                            + ":\n"
                            + String.join("\n", diferencias));
        }
    }

    private static void comparar(
            List<String> diferencias,
            String codigo,
            String campo,
            String enCodigo,
            String enTabla) {
        if (!enCodigo.equals(enTabla)) {
            diferencias.add(
                    codigo
                            + ": el "
                            + campo
                            + " es «"
                            + enCodigo
                            + "» en el código y «"
                            + enTabla
                            + "» en la tabla");
        }
    }

    // Filas de la tabla «| `CÓDIGO` | estado | título | descripción | cuándo |», por código.
    private static Map<String, String[]> leerFilas(Path catalogo) {
        List<String> lineas;
        try {
            lineas = Files.readAllLines(catalogo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + catalogo, e);
        }
        Map<String, String[]> filas = new HashMap<>();
        for (String linea : lineas) {
            if (!linea.startsWith("| `")) {
                continue;
            }
            String[] celdas = linea.substring(1, linea.lastIndexOf('|')).split("\\|");
            String[] fila = new String[celdas.length];
            for (int i = 0; i < celdas.length; i++) {
                fila[i] = celdas[i].trim();
            }
            filas.put(fila[0].replace("`", ""), fila);
        }
        return filas;
    }
}
