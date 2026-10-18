package ec.mdd.roles.dominio;

import java.util.Comparator;
import java.util.Objects;

/**
 * Una funcionalidad con un tipo de permiso y un alcance (RF-02, D-64).
 *
 * @param funcionalidad código de la funcionalidad
 * @param tipo lo que deja hacer
 * @param alcance sobre qué datos vale
 */
public record Permiso(String funcionalidad, TipoDePermiso tipo, Alcance alcance) {

    /** Por funcionalidad y, dentro de ella, en el orden lectura, escritura, aprobación y firma. */
    static final Comparator<Permiso> ORDEN =
            Comparator.comparing(Permiso::funcionalidad).thenComparing(Permiso::tipo);

    /** Exige los tres datos. */
    public Permiso {
        Objects.requireNonNull(funcionalidad, "funcionalidad");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(alcance, "alcance");
    }

    /**
     * La forma del permiso en el token: {@code FUNCIONALIDAD:TIPO}, o {@code
     * FUNCIONALIDAD:TIPO:PROPIO} si solo vale para lo propio (ADR-021).
     */
    public String texto() {
        String texto = funcionalidad + ":" + tipo;
        return alcance == Alcance.PROPIO ? texto + ":" + Alcance.PROPIO : texto;
    }

    /** Si es el mismo permiso, sin mirar el alcance. */
    boolean mismoQue(String otraFuncionalidad, TipoDePermiso otroTipo) {
        return funcionalidad.equals(otraFuncionalidad) && tipo == otroTipo;
    }
}
