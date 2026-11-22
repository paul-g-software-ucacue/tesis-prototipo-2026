package ec.mdd.reporting.dominio;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * De dónde sale el valor de una columna del CSV para SIGA: la lista cerrada de campos del archivo
 * de mapeo (research §6, D-119).
 *
 * <p>Solo los campos con un catálogo cerrado, cuyos códigos fija el sistema, admiten traducción:
 * así, que una traducción «cubra todos los códigos» se comprueba al cargar, sin preguntar a otro
 * servicio. Los códigos de actividad y de carrera salen con el código interno.
 */
public enum CampoDelLayout {
    PERIODO("periodo"),
    VERSION("version"),
    FECHA_PUBLICACION("fecha_publicacion"),
    HASH_VERSION("hash_version"),
    TIPO_IDENTIFICACION(
            "tipo_identificacion",
            Arrays.stream(TipoDeIdentificacion.values()).map(Enum::name).toList()),
    IDENTIFICACION("identificacion", true),
    DOCENTE("docente"),
    DEDICACION("dedicacion", Arrays.stream(Dedicacion.values()).map(Enum::name).toList()),
    COMPONENTE("componente", Arrays.stream(Componente.values()).map(Enum::name).toList(), true),
    ACTIVIDAD("actividad"),
    CARRERA("carrera"),
    ASIGNATURA("asignatura"),
    PARALELO("paralelo"),
    PROYECTO("proyecto"),
    HORAS("horas", true);

    private final String clave;
    private final List<String> codigos;
    private final boolean obligatorio;

    CampoDelLayout(String clave) {
        this(clave, List.of(), false);
    }

    CampoDelLayout(String clave, boolean obligatorio) {
        this(clave, List.of(), obligatorio);
    }

    CampoDelLayout(String clave, List<String> codigos) {
        this(clave, codigos, false);
    }

    CampoDelLayout(String clave, List<String> codigos, boolean obligatorio) {
        this.clave = clave;
        this.codigos = List.copyOf(codigos);
        this.obligatorio = obligatorio;
    }

    /** El nombre del campo en el archivo de mapeo. */
    public String clave() {
        return clave;
    }

    /** Si admite traducción: solo los catálogos cerrados. */
    public boolean traducible() {
        return !codigos.isEmpty();
    }

    /** Los códigos de su catálogo, en su orden; vacío si no es traducible. */
    public List<String> codigos() {
        return codigos;
    }

    /** Si el layout tiene que tener al menos una columna con él. */
    public boolean obligatorio() {
        return obligatorio;
    }

    /**
     * @param clave el nombre en el archivo, exacto
     * @return el campo; vacío si no existe
     */
    public static Optional<CampoDelLayout> deClave(String clave) {
        return Arrays.stream(values()).filter(uno -> uno.clave.equals(clave)).findFirst();
    }
}
