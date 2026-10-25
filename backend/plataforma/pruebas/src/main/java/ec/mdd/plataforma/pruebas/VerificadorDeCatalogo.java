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
import java.util.Set;

/**
 * Comprueba que los códigos del código coinciden con el catálogo documentado, {@code
 * contratos/comun/errores.md} (ADR-016).
 *
 * <p>Por cada código, falla si no tiene fila en la tabla o si su estado, su título o su descripción
 * no son iguales a los de su fila. Así el catálogo que lee la SPA y el que usa el código no se
 * desalinean. También falla si su fila no está completa (D-72): las 8 columnas llenas y una
 * clasificación que cuadra con el estado.
 */
public final class VerificadorDeCatalogo {

    /** Código, estado, título, descripción, causa, clasificación, acción y descripción técnica. */
    private static final int COLUMNAS = 8;

    private static final Set<String> CLASIFICACIONES =
            Set.of("Validación", "Negocio", "Autenticación", "Autorización", "Sistema");

    private VerificadorDeCatalogo() {}

    /** Verifica que todas las filas de {@code contratos/comun/errores.md} estén completas. */
    public static void verificarFormato() {
        verificarFormato(CarpetaDeContratos.buscar().resolve("comun").resolve("errores.md"));
    }

    /**
     * Verifica que todas las filas de un catálogo estén completas, tengan o no su código en el
     * código.
     *
     * @throws AssertionError con todas las filas incompletas
     */
    public static void verificarFormato(Path catalogo) {
        List<String> diferencias = new ArrayList<>();
        leerFilas(catalogo).forEach((codigo, fila) -> completa(diferencias, codigo, fila));
        fallarSiHay(diferencias, catalogo);
    }

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
            if (!completa(diferencias, definicion.codigo(), fila)) {
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
        fallarSiHay(diferencias, catalogo);
    }

    private static void fallarSiHay(List<String> diferencias, Path catalogo) {
        if (!diferencias.isEmpty()) {
            throw new AssertionError(
                    "El catálogo del código no coincide con "
                            + catalogo.getFileName()
                            + ":\n"
                            + String.join("\n", diferencias));
        }
    }

    // Las 8 columnas llenas y una clasificación de las cinco que cuadra con el estado (D-72).
    private static boolean completa(List<String> diferencias, String codigo, String[] fila) {
        if (fila.length != COLUMNAS) {
            diferencias.add(
                    codigo + ": tiene " + fila.length + " columnas y el catálogo pide " + COLUMNAS);
            return false;
        }
        for (int i = 1; i < COLUMNAS; i++) {
            if (fila[i].isEmpty()) {
                diferencias.add(codigo + ": la columna " + (i + 1) + " está vacía");
                return false;
            }
        }
        String clasificacion = fila[5];
        if (!CLASIFICACIONES.contains(clasificacion)) {
            diferencias.add(
                    codigo
                            + ": la clasificación «"
                            + clasificacion
                            + "» no es una de "
                            + CLASIFICACIONES);
            return false;
        }
        String esperada = clasificacionDelEstado(fila[1]);
        boolean cuadra =
                esperada == null
                        ? !Set.of("Autenticación", "Autorización", "Sistema")
                                .contains(clasificacion)
                        : esperada.equals(clasificacion);
        if (!cuadra && !("429".equals(fila[1]) && "Autenticación".equals(clasificacion))) {
            diferencias.add(
                    codigo
                            + ": la clasificación «"
                            + clasificacion
                            + "» no cuadra con el estado "
                            + fila[1]);
            return false;
        }
        return true;
    }

    // 401, 403 y 5xx tienen una sola clasificación; los demás 4xx son de validación o de negocio,
    // salvo el 429 de un inicio de sesión bloqueado, que es de autenticación.
    private static String clasificacionDelEstado(String estado) {
        if ("401".equals(estado)) {
            return "Autenticación";
        }
        if ("403".equals(estado)) {
            return "Autorización";
        }
        return estado.startsWith("5") ? "Sistema" : null;
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

    // Filas de la tabla «| `CÓDIGO` | estado | título | descripción | causa | clasificación |
    // acción
    // | descripción técnica |», por código.
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
