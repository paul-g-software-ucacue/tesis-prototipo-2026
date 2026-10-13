package ec.mdd.plataforma.errores.nucleo;

import java.util.UUID;

/**
 * Identificadores de incidente: une la respuesta de un error técnico con su causa en el registro.
 *
 * <p>Es un UUID aleatorio (versión 4): no se puede adivinar ni sirve para contar incidentes.
 */
public final class Incidentes {

    private Incidentes() {}

    /** Un identificador nuevo. */
    public static String nuevoId() {
        return UUID.randomUUID().toString();
    }
}
