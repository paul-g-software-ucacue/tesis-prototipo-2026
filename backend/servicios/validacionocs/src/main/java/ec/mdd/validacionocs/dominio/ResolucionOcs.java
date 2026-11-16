package ec.mdd.validacionocs.dominio;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * La resolución del OCS que aprueba el distributivo (F15-H01, RF-37, D-107): entidad de {@link
 * ProcesoAprobacion} en el glosario, con su número, su fecha y su PDF. El MDD no valida las firmas
 * del PDF (D-19); guarda su huella para entregarlo idéntico.
 *
 * <p>El número no tiene formato fijo, porque cada institución numera a su manera; se guarda sin los
 * espacios de los extremos, de 1 a 60 caracteres. Que la fecha esté en su rango lo decide el
 * agregado, que sabe cuándo llegó el distributivo a EN_OCS.
 *
 * @param numero el número de la resolución
 * @param fecha la fecha de la resolución, sin hora
 * @param sha256 la huella del PDF
 * @param nombre el nombre del archivo recibido, sin ruta; solo se muestra
 * @param tamano los bytes del PDF
 */
public record ResolucionOcs(
        String numero, LocalDate fecha, String sha256, String nombre, int tamano) {

    /** El largo máximo del número, recortado. */
    public static final int LARGO_DEL_NUMERO = 60;

    private static final Pattern HUELLA = Pattern.compile("^[0-9a-f]{64}$");

    /** Recorta el número y exige los datos. */
    public ResolucionOcs {
        numero = numero == null ? "" : numero.strip();
        if (numero.isEmpty() || numero.length() > LARGO_DEL_NUMERO) {
            throw new NumeroDeResolucionInvalido();
        }
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(nombre, "nombre");
        if (sha256 == null || !HUELLA.matcher(sha256).matches()) {
            throw new IllegalArgumentException("La huella no es un SHA-256 en hexadecimal");
        }
        if (tamano < 1) {
            throw new IllegalArgumentException("El PDF tiene al menos un byte");
        }
    }
}
