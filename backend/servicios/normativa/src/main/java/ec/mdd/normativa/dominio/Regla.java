package ec.mdd.normativa.dominio;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Una regla de la política con su identificador RN-xx, sus parámetros y su severidad (glosario
 * §7.3). La comprobación está en el código de quien la aplica; las cifras y la severidad son datos
 * de la versión (principio VI, MD-08).
 *
 * @param identificador {@code RN-01} a {@code RN-26}
 * @param severidad dura o blanda; {@code null} si la regla solo da parámetros (RN-01, RN-06)
 * @param parametros nombre y valor de cada parámetro, en su orden; al menos uno
 */
public record Regla(String identificador, Severidad severidad, Map<String, BigDecimal> parametros) {

    private static final Pattern IDENTIFICADOR = Pattern.compile("RN-[0-9]{2}");

    /** Comprueba el identificador y que haya parámetros, y los copia en su orden. */
    public Regla {
        if (identificador == null || !IDENTIFICADOR.matcher(identificador).matches()) {
            throw new IllegalArgumentException(
                    "Identificador de regla mal formado: " + identificador);
        }
        if (parametros == null || parametros.isEmpty()) {
            throw new IllegalArgumentException(
                    "La regla " + identificador + " no tiene parámetros");
        }
        parametros = Collections.unmodifiableMap(new LinkedHashMap<>(parametros));
    }

    /** El valor de un parámetro, si la regla lo tiene. */
    public Optional<BigDecimal> parametro(String nombre) {
        return Optional.ofNullable(parametros.get(nombre));
    }
}
