package ec.mdd.reporting.puertos.entrada;

import ec.mdd.reporting.dominio.Formato;
import java.util.Objects;

/**
 * Descarga el reporte individual en PDF o en CSV (RF-48, F20-H01). Cada descarga queda en la
 * bitácora antes de entregarse (aclaración del 2026-11-21, D-115).
 */
public interface DescargarReporte {

    /**
     * @param periodo el código del periodo
     * @param identificacion el número de identificación del docente
     * @param formato PDF o CSV
     * @param quien quién descarga
     * @return el archivo, ya registrado
     * @throws ec.mdd.reporting.dominio.SinPublicacion si el periodo no tiene versión publicada
     * @throws ec.mdd.reporting.dominio.DocenteSinDistributivo si el docente no está en ella
     * @throws ec.mdd.reporting.dominio.ContenidoInconsistente si el contenido no es el publicado
     */
    ArchivoDelReporte descargar(
            String periodo, String identificacion, Formato formato, String quien);

    /**
     * @param periodo el código del periodo
     * @param formato PDF o CSV
     * @param quien quién descarga
     * @return el archivo del reporte general (F20-H02, D-117), ya registrado
     * @throws ec.mdd.reporting.dominio.SinPublicacion si el periodo no tiene versión publicada
     * @throws ec.mdd.reporting.dominio.ContenidoInconsistente si el contenido no es el publicado o
     *     un docente viene repetido
     */
    ArchivoDelReporte descargarGeneral(String periodo, Formato formato, String quien);

    /**
     * Un reporte listo para entregar.
     *
     * @param contenido sus bytes
     * @param formato PDF o CSV
     * @param nombre el nombre del archivo, sin espacios
     */
    record ArchivoDelReporte(byte[] contenido, Formato formato, String nombre) {

        /** Exige los datos y copia los bytes. */
        public ArchivoDelReporte {
            Objects.requireNonNull(formato, "formato");
            Objects.requireNonNull(nombre, "nombre");
            contenido = contenido.clone();
        }

        @Override
        public byte[] contenido() {
            return contenido.clone();
        }
    }
}
