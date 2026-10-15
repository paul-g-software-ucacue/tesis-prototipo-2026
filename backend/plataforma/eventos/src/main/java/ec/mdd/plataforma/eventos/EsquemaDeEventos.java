package ec.mdd.plataforma.eventos;

import java.util.regex.Pattern;

/**
 * El esquema del registro de publicaciones de un proceso: {@code eventos_<proceso>}, creado por
 * {@code deploy/postgres/01-esquemas.sh} con el rol del proceso como dueño (ADR-018).
 */
final class EsquemaDeEventos {

    static final String PROPIEDAD = "spring.modulith.events.jdbc.schema";

    private static final Pattern FORMA = Pattern.compile("^eventos_[a-z]+$");

    private EsquemaDeEventos() {}

    /**
     * Comprueba que el esquema configurado sea el de eventos de un proceso.
     *
     * @param esquema el valor de {@value #PROPIEDAD}
     * @return el mismo esquema
     * @throws IllegalStateException si falta o no tiene la forma {@code eventos_<proceso>}
     */
    static String validar(String esquema) {
        if (esquema == null || !FORMA.matcher(esquema).matches()) {
            throw new IllegalStateException(
                    PROPIEDAD
                            + " debe ser el esquema de eventos del proceso, con la forma"
                            + " eventos_<proceso> (ADR-018); se recibió: "
                            + esquema);
        }
        return esquema;
    }
}
