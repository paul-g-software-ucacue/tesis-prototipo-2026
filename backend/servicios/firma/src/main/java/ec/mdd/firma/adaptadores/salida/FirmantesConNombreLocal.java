package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * El nombre local del autor (F14-H03, D-104, ADR-017, punto 3): en su perfil local, el nombre que
 * el PDF imprime en todos los pasos, para que firme la cadena con su certificado real.
 *
 * <p>Sale de {@code MDD_FIRMA_NOMBRE_LOCAL} en su {@code .env}, que Git ignora: el nombre real
 * nunca entra al repositorio (principio IX). Solo cambia el nombre; quién está en turno (el usuario
 * o la identificación) sigue siendo el del periodo. Vacío, como en la demo y la CI, no cambia nada.
 * No es un interruptor de la validación: el CN se compara igual, con este nombre.
 */
public final class FirmantesConNombreLocal implements FirmantesDelPeriodo {

    private final FirmantesDelPeriodo delegado;
    private final String nombreLocal;

    /**
     * @param delegado los firmantes de los procesos datos e identidad
     * @param nombreLocal el nombre del autor, o vacío
     */
    public FirmantesConNombreLocal(FirmantesDelPeriodo delegado, String nombreLocal) {
        this.delegado = Objects.requireNonNull(delegado, "delegado");
        this.nombreLocal = nombreLocal == null ? "" : nombreLocal.strip();
    }

    @Override
    public Map<String, FirmanteDelPeriodo> firmantes(String periodo, Set<String> roles) {
        Map<String, FirmanteDelPeriodo> delPeriodo = delegado.firmantes(periodo, roles);
        if (nombreLocal.isEmpty()) {
            return delPeriodo;
        }
        Map<String, FirmanteDelPeriodo> conNombreLocal = new LinkedHashMap<>();
        delPeriodo.forEach(
                (rol, firmante) ->
                        conNombreLocal.put(
                                rol,
                                new FirmanteDelPeriodo(
                                        nombreLocal,
                                        firmante.usuario(),
                                        firmante.identificacion())));
        return Map.copyOf(conNombreLocal);
    }
}
