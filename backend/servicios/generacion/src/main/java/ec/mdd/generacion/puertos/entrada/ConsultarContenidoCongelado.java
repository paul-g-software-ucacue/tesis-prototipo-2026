package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.CargaCongelada;
import ec.mdd.generacion.dominio.DocenteDeLaVersion;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El contenido congelado de una versión, para Reporting (F20-H01, D-115). Si una versión congelada
 * no lo tiene (por ejemplo, porque se congeló antes de D-115), lo calcula en ese momento y lo
 * guarda como tardío. Puede llamar a Datos: no se usa dentro de una transacción.
 */
public interface ConsultarContenidoCongelado {

    /**
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @return el hash y los docentes; vacío si la versión no existe o no está congelada
     */
    Optional<CargasCongeladas> cargas(String periodo, int version);

    /**
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @param docente su número de identificación
     * @return el hash y la carga del docente; vacío si la versión no existe o no está congelada, o
     *     si el docente no está en ella
     */
    Optional<CargaDeLaVersion> carga(String periodo, int version, String docente);

    /**
     * El contenido completo de todos los docentes de una versión, para el CSV para SIGA (F19-H01,
     * D-119): una sola lectura en lugar de una por docente.
     *
     * @param periodo el código del periodo
     * @param version el número de la versión
     * @return el hash y las cargas, por nombre; vacío si la versión no existe o no está congelada
     */
    Optional<ContenidoCompleto> contenido(String periodo, int version);

    /**
     * El hash de una versión congelada y sus docentes.
     *
     * @param hash el hash de la versión
     * @param docentes por nombre y, a igual nombre, por identificación
     */
    record CargasCongeladas(String hash, List<DocenteDeLaVersion> docentes) {

        /** Exige el hash y copia la lista. */
        public CargasCongeladas {
            Objects.requireNonNull(hash, "hash");
            docentes = List.copyOf(docentes);
        }
    }

    /**
     * El hash de una versión congelada y la carga de cada docente.
     *
     * @param hash el hash de la versión
     * @param cargas por nombre y, a igual nombre, por identificación
     */
    record ContenidoCompleto(String hash, List<CargaCongelada> cargas) {

        /** Exige el hash y copia la lista. */
        public ContenidoCompleto {
            Objects.requireNonNull(hash, "hash");
            cargas = List.copyOf(cargas);
        }
    }

    /**
     * La carga congelada de un docente, con el hash de su versión.
     *
     * @param hash el hash de la versión
     * @param carga la carga del docente
     */
    record CargaDeLaVersion(String hash, CargaCongelada carga) {

        /** Exige los datos. */
        public CargaDeLaVersion {
            Objects.requireNonNull(hash, "hash");
            Objects.requireNonNull(carga, "carga");
        }
    }
}
