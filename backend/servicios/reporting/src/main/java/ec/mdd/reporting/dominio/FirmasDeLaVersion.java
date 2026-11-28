package ec.mdd.reporting.dominio;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lo que Firma informa de la cadena del documento de una versión (F19-H02, D-120). Firma solo da
 * los hechos; qué es una versión «firmada» lo decide {@link CondicionesDeExportacion}.
 *
 * @param hashContenido el hash de la versión congelada de la que se generó el documento
 * @param pasos los pasos de la cadena, por orden
 * @param anulada si sus firmas se anularon
 * @param rechazada si se rechazó en firma
 */
public record FirmasDeLaVersion(
        String hashContenido, List<PasoFirmado> pasos, boolean anulada, boolean rechazada) {

    private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");

    /** Exige un hash SHA-256 en hexadecimal y pasos sin orden repetido; copia la lista. */
    public FirmasDeLaVersion {
        Objects.requireNonNull(hashContenido, "hashContenido");
        if (!HASH.matcher(hashContenido).matches()) {
            throw new IllegalArgumentException("hash que no es SHA-256 en hexadecimal");
        }
        pasos = List.copyOf(pasos);
        Set<Integer> ordenes = new HashSet<>();
        for (PasoFirmado paso : pasos) {
            if (!ordenes.add(paso.orden())) {
                throw new IllegalArgumentException("paso repetido: " + paso.orden());
            }
        }
    }

    /** Cuántos pasos tienen su firma. */
    public int firmados() {
        return (int) pasos.stream().filter(PasoFirmado::firmado).count();
    }

    /** Cuántos pasos tiene la cadena. */
    public int total() {
        return pasos.size();
    }

    /**
     * Un paso de la cadena.
     *
     * @param orden su lugar, desde 1
     * @param firmado si tiene su firma
     */
    public record PasoFirmado(int orden, boolean firmado) {

        /** Exige un orden desde 1. */
        public PasoFirmado {
            if (orden < 1) {
                throw new IllegalArgumentException("El orden empieza en 1");
            }
        }
    }
}
