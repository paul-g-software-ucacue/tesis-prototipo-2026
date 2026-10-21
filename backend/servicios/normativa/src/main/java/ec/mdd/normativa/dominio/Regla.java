package ec.mdd.normativa.dominio;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Una regla de la política con su identificador RN-xx (o RF-xx, como RF-24), sus parámetros y su
 * severidad (glosario §7.3). La comprobación está en el código de quien la aplica; las cifras y la
 * severidad son datos de la versión (principio VI, MD-08).
 *
 * <p>Las reglas del motor (tabla 5.2 del modelo de dominio) tienen severidad, y algunas no tienen
 * cifras (RN-17, RF-24). Las que solo dan parámetros (RN-01, RN-06 y RN-24) no tienen severidad.
 * Una regla sin parámetros ni severidad no dice nada, y no se admite.
 *
 * @param identificador {@code RN-01} a {@code RN-26}, o {@code RF-24}
 * @param severidad dura o blanda; {@code null} si la regla solo da parámetros
 * @param parametros nombre y valor de cada parámetro, en su orden; vacío si la regla solo tiene
 *     severidad
 */
public record Regla(String identificador, Severidad severidad, Map<String, BigDecimal> parametros) {

    private static final Pattern IDENTIFICADOR = Pattern.compile("R[NF]-[0-9]{2}");

    /** Comprueba el identificador y que haya parámetros o severidad, y copia los parámetros. */
    public Regla {
        if (identificador == null || !IDENTIFICADOR.matcher(identificador).matches()) {
            throw new IllegalArgumentException(
                    "Identificador de regla mal formado: " + identificador);
        }
        if ((parametros == null || parametros.isEmpty()) && severidad == null) {
            throw new IllegalArgumentException(
                    "La regla " + identificador + " no tiene parámetros ni severidad");
        }
        parametros =
                Collections.unmodifiableMap(
                        new LinkedHashMap<>(parametros == null ? Map.of() : parametros));
    }

    /** Si el motor la comprueba: tiene severidad (MD-08). */
    public boolean delMotor() {
        return severidad != null;
    }

    /** El valor de un parámetro, si la regla lo tiene. */
    public Optional<BigDecimal> parametro(String nombre) {
        return Optional.ofNullable(parametros.get(nombre));
    }
}
