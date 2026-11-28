package ec.mdd.reporting.puertos.entrada;

import ec.mdd.reporting.dominio.LayoutExportacion;
import java.util.List;
import java.util.Objects;

/**
 * El layout de exportación del CSV para SIGA: consultar el vigente y los anteriores, descargar la
 * plantilla y cargar un archivo de mapeo nuevo (FR-013 a FR-018, F19-H01, D-119).
 */
public interface GestionarLayoutSiga {

    /**
     * @return el vigente (el último cargado o el por defecto) y los anteriores
     */
    LayoutsSiga consultar();

    /**
     * @return la plantilla comentada, que es el layout por defecto
     */
    String plantilla();

    /**
     * Valida el archivo y, si vale, lo guarda como la versión siguiente y deja su entrada en la
     * bitácora, en la misma transacción. Un archivo rechazado no cambia nada.
     *
     * @param archivo los bytes subidos, hasta 256 KB
     * @param quien quién lo carga
     * @return la versión nueva, ya vigente
     * @throws ec.mdd.reporting.dominio.ArchivoIlegible si no se puede leer
     * @throws ec.mdd.reporting.dominio.LayoutInvalido con todos sus problemas
     */
    LayoutExportacion cargar(byte[] archivo, String quien);

    /**
     * El layout vigente y las versiones anteriores.
     *
     * @param vigente el que rige
     * @param anteriores los demás cargados, del más reciente al más antiguo
     */
    record LayoutsSiga(LayoutExportacion vigente, List<LayoutExportacion> anteriores) {

        /** Exige el vigente y copia la lista. */
        public LayoutsSiga {
            Objects.requireNonNull(vigente, "vigente");
            anteriores = List.copyOf(anteriores);
        }
    }
}
