package ec.mdd.firma.dominio;

import java.util.List;
import java.util.Optional;

/** Esqueleto (F14-H02, T006). */
public record ResultadoDeComprobacion(
        Comprobacion comprobacion, Resultado resultado, Optional<String> motivo) {

    public static ResultadoDeComprobacion aprobada(Comprobacion comprobacion) {
        throw new UnsupportedOperationException();
    }

    public static ResultadoDeComprobacion rechazada(Comprobacion comprobacion, String motivo) {
        throw new UnsupportedOperationException();
    }

    public static ResultadoDeComprobacion noEvaluada(Comprobacion comprobacion, String motivo) {
        throw new UnsupportedOperationException();
    }

    public static List<ResultadoDeComprobacion> desde(ResultadoDeComprobacion... evaluadas) {
        throw new UnsupportedOperationException();
    }
}
