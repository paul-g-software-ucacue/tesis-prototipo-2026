package ec.mdd.roles.dominio;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Conjunto de permisos con un nombre (glosario, RF-02). Puede ser uno de los 10 de la matriz D-16,
 * que son fijos (D-63), o uno creado por el administrador.
 *
 * <p>El identificador viaja en el token: se deriva del nombre al crear el rol y no cambia aunque el
 * nombre cambie (research §7).
 *
 * @param identificador en mayúsculas con guion bajo, hasta 64 caracteres
 * @param nombre de 1 a 80 caracteres, con al menos una letra o un dígito
 * @param descripcion de 1 a 300 caracteres
 * @param predefinido si es uno de los 10 de D-16
 * @param permisos al menos uno, ordenados por funcionalidad y tipo
 */
public record Rol(
        String identificador,
        String nombre,
        String descripcion,
        boolean predefinido,
        List<Permiso> permisos) {

    /** Largo máximo del identificador, que es el de su columna. */
    public static final int LARGO_IDENTIFICADOR = 64;

    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Z0-9_]{1,64}");
    private static final Pattern LETRA_O_DIGITO = Pattern.compile("[\\p{L}\\p{N}]");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARADORES = Pattern.compile("[^A-Z0-9]+");

    /** Valida los datos, recorta los espacios del nombre y la descripción y ordena los permisos. */
    public Rol {
        if (identificador == null || !IDENTIFICADOR.matcher(identificador).matches()) {
            throw new IllegalArgumentException(
                    "Identificador de rol mal formado: " + identificador);
        }
        nombre = Objects.requireNonNull(nombre, "nombre").strip();
        if (nombre.length() > 80 || !LETRA_O_DIGITO.matcher(nombre).find()) {
            throw new IllegalArgumentException("El nombre del rol no es válido");
        }
        descripcion = Objects.requireNonNull(descripcion, "descripcion").strip();
        if (descripcion.isEmpty() || descripcion.length() > 300) {
            throw new IllegalArgumentException("La descripción del rol no es válida");
        }
        if (permisos == null || permisos.isEmpty()) {
            throw new PermisosInvalidos("El rol necesita al menos un permiso.");
        }
        permisos = permisos.stream().sorted(Permiso.ORDEN).toList();
    }

    /** Un rol nuevo del administrador, con el identificador derivado del nombre. */
    public static Rol nuevo(String nombre, String descripcion, Collection<Permiso> permisos) {
        return new Rol(identificadorDe(nombre), nombre, descripcion, false, List.copyOf(permisos));
    }

    /**
     * El mismo rol con otro nombre, otra descripción y otros permisos; el identificador no cambia.
     */
    public Rol editado(
            String otroNombre, String otraDescripcion, Collection<Permiso> otrosPermisos) {
        return new Rol(
                identificador,
                otroNombre,
                otraDescripcion,
                predefinido,
                List.copyOf(otrosPermisos));
    }

    /**
     * El identificador que da un nombre: sin tildes, en mayúsculas, cada tramo de caracteres que no
     * son letras ni dígitos pasa a un guion bajo, sin guiones bajos en los extremos y recortado a
     * 64. Dos nombres con el mismo identificador son el mismo nombre (research §7).
     */
    public static String identificadorDe(String nombre) {
        String sinTildes =
                MARCAS.matcher(Normalizer.normalize(nombre.strip(), Normalizer.Form.NFD))
                        .replaceAll("");
        String identificador =
                recortarGuiones(
                        SEPARADORES.matcher(sinTildes.toUpperCase(Locale.ROOT)).replaceAll("_"));
        if (identificador.length() > LARGO_IDENTIFICADOR) {
            identificador = recortarGuiones(identificador.substring(0, LARGO_IDENTIFICADOR));
        }
        return identificador;
    }

    /** Si da ese permiso, con cualquier alcance. */
    public boolean da(String funcionalidad, TipoDePermiso tipo) {
        return permisos.stream().anyMatch(permiso -> permiso.mismoQue(funcionalidad, tipo));
    }

    private static String recortarGuiones(String texto) {
        int inicio = 0;
        int fin = texto.length();
        while (inicio < fin && texto.charAt(inicio) == '_') {
            inicio++;
        }
        while (fin > inicio && texto.charAt(fin - 1) == '_') {
            fin--;
        }
        return texto.substring(inicio, fin);
    }
}
