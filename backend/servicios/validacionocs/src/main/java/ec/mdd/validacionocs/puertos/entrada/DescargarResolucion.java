package ec.mdd.validacionocs.puertos.entrada;

/**
 * Descargar el PDF de la resolución del OCS, idéntico al registrado (F15-H01, D-107). Lo pide quien
 * tiene el permiso de leer la resolución (D-16); el permiso lo comprueba el controlador.
 */
@FunctionalInterface
public interface DescargarResolucion {

    /**
     * @param periodo el código del periodo
     * @return el nombre guardado y los bytes
     * @throws ResolucionNoEncontrada si el periodo no tiene resolución
     * @throws ResolucionIlegible si el archivo falta o no coincide con su huella
     */
    ArchivoDeLaResolucion descargar(String periodo);

    /**
     * El PDF de la resolución.
     *
     * @param nombre el nombre con que se recibió, sin ruta
     * @param contenido sus bytes
     */
    record ArchivoDeLaResolucion(String nombre, byte[] contenido) {

        /** Copia los bytes: nadie de fuera los cambia. */
        public ArchivoDeLaResolucion {
            contenido = contenido.clone();
        }

        /** Copia los bytes al entregarlos. */
        @Override
        public byte[] contenido() {
            return contenido.clone();
        }
    }
}
