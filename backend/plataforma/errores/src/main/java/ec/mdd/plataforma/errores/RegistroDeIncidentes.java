package ec.mdd.plataforma.errores;

import ec.mdd.plataforma.errores.nucleo.Incidentes;
import ec.mdd.plataforma.errores.nucleo.SaneadorDeTrazas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registra un error técnico con su {@code idIncidente} y su traza saneada (principio XIII).
 *
 * <p>Si la excepción es de dominio o de aplicación (su paquete contiene {@code .dominio.} o {@code
 * .aplicacion.}), avisa que no tiene código en el catálogo: así un error nuevo sin registrar no
 * pasa desapercibido.
 */
final class RegistroDeIncidentes {

    private static final Logger REGISTRO = LoggerFactory.getLogger(RegistroDeIncidentes.class);

    private RegistroDeIncidentes() {}

    /**
     * Registra el incidente y devuelve su identificador.
     *
     * @param excepcion causa; puede ser nula si el error no trae excepción
     * @param metodo método HTTP de la petición
     * @param ruta ruta de la petición, sin la consulta
     */
    static String registrar(Throwable excepcion, String metodo, String ruta) {
        String id = Incidentes.nuevoId();
        String rutaSaneada = SaneadorDeTrazas.sanear(String.valueOf(ruta));
        if (excepcion == null) {
            REGISTRO.error(
                    "Incidente {}: {} {} falló sin excepción asociada", id, metodo, rutaSaneada);
            return id;
        }
        String clase = excepcion.getClass().getName();
        String aviso =
                esDeDominioOAplicacion(clase) ? " (sin código en el catálogo: " + clase + ")" : "";
        REGISTRO.error(
                "Incidente {}: {} {} falló con {}{}{}{}",
                id,
                metodo,
                rutaSaneada,
                clase,
                aviso,
                System.lineSeparator(),
                SaneadorDeTrazas.trazaSaneada(excepcion));
        return id;
    }

    private static boolean esDeDominioOAplicacion(String clase) {
        return clase.contains(".dominio.") || clase.contains(".aplicacion.");
    }
}
