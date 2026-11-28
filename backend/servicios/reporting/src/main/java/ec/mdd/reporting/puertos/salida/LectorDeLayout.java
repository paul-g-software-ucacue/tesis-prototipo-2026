package ec.mdd.reporting.puertos.salida;

import ec.mdd.reporting.dominio.BorradorDeLayout;
import java.util.Objects;

/**
 * Lee un archivo de mapeo del CSV para SIGA sin interpretarlo: cada valor queda como el texto
 * escrito, para que la validación del dominio diga qué está mal (F19-H01, research §7, D-119).
 */
public interface LectorDeLayout {

    /**
     * @param archivo los bytes subidos, hasta 256 KB
     * @return el texto sin la marca de orden de bytes y lo que se entendió de él
     * @throws ec.mdd.reporting.dominio.ArchivoIlegible si no es UTF-8 ni YAML, o tiene anclas,
     *     alias, etiquetas o más de 4 niveles
     */
    LecturaDelLayout leer(byte[] archivo);

    /**
     * Un archivo de mapeo leído.
     *
     * @param texto el texto, sin la marca de orden de bytes: el que se guarda y se resume con
     *     SHA-256
     * @param borrador lo que se entendió, sin validar
     */
    record LecturaDelLayout(String texto, BorradorDeLayout borrador) {

        /** Exige los dos. */
        public LecturaDelLayout {
            Objects.requireNonNull(texto, "texto");
            Objects.requireNonNull(borrador, "borrador");
        }
    }
}
