package ec.mdd.docentes.dominio;

import java.util.List;
import java.util.Objects;

/** Arma la lista de lo que cambió al corregir un docente, para el registro (principio VII). */
final class Cambios {

    private Cambios() {}

    /** Agrega el cambio si el valor nuevo es otro; un valor nulo es «sin valor». */
    static void agregar(List<CambioDeValor> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(new CambioDeValor(campo, texto(anterior), texto(nuevo)));
        }
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof java.math.BigDecimal numero) {
            return numero.stripTrailingZeros().toPlainString();
        }
        if (valor instanceof List<?> lista) {
            return String.join("; ", lista.stream().map(String::valueOf).toList());
        }
        return valor.toString();
    }
}
