package ec.mdd.cargas.puertos.entrada;

import ec.mdd.cargas.dominio.CargaQueFalta;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Alguna carga de la que depende la que se quiere subir no tiene datos (RF-56, F01-H05): la carga
 * no se valida ni se confirma, y el archivo no se lee.
 */
public final class CargasAnterioresPendientes extends RuntimeException {

    private final transient List<CargaQueFalta> faltan;

    /**
     * @param carga el código de la carga que se quería subir
     * @param faltan las cargas anteriores que no tienen datos, en su orden
     */
    public CargasAnterioresPendientes(String carga, List<CargaQueFalta> faltan) {
        super(
                "La carga "
                        + carga
                        + " depende de cargas sin datos: "
                        + faltan.stream()
                                .map(CargaQueFalta::codigo)
                                .collect(Collectors.joining(", ")));
        this.faltan = List.copyOf(faltan);
    }

    /** Las cargas anteriores que no tienen datos, en su orden. */
    public List<CargaQueFalta> faltan() {
        return faltan;
    }
}
