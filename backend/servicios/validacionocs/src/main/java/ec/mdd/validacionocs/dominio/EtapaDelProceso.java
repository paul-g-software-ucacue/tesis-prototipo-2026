package ec.mdd.validacionocs.dominio;

import java.util.Objects;

/**
 * Una etapa en la copia de la configuración con la que empezó el distributivo (F12-H02, FR-003).
 *
 * @param tipo cuál es
 * @param activa si el distributivo pasa por ella
 * @param nombre el nombre que ve el usuario
 * @param rol el rol que la mueve
 * @param admiteDevolucion si desde ella se puede devolver (F12-H05)
 */
public record EtapaDelProceso(
        TipoDeEtapa tipo, boolean activa, String nombre, String rol, boolean admiteDevolucion) {

    /** Exige los datos. */
    public EtapaDelProceso {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(rol, "rol");
    }

    /** La etapa tal como está en la configuración del periodo. */
    public static EtapaDelProceso de(Etapa etapa) {
        return new EtapaDelProceso(
                etapa.tipo(),
                etapa.activa(),
                etapa.nombre(),
                etapa.rol(),
                etapa.admiteDevolucion());
    }
}
