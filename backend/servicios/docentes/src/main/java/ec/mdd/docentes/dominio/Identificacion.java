package ec.mdd.docentes.dominio;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * La cédula o el pasaporte de un docente: su clave, que no cambia (RF-16, carga 6). Una cédula
 * tiene 10 dígitos como texto, con el 0 inicial; sus dos primeros son la provincia (01 a 24, o 30
 * para quienes viven en el exterior), el tercero es menor que 6 (persona natural) y el último
 * cumple el módulo 10. Un pasaporte tiene de 1 a 13 letras mayúsculas o dígitos.
 *
 * @param tipo cédula o pasaporte
 * @param numero el número, sin espacios
 */
public record Identificacion(TipoDeIdentificacion tipo, String numero) {

    private static final String CAMPO = "identificacion";
    private static final Pattern CEDULA = Pattern.compile("[0-9]{10}");
    private static final Pattern PASAPORTE = Pattern.compile("[0-9A-Z]{1,13}");
    private static final int PROVINCIAS = 24;
    private static final int EXTERIOR = 30;
    private static final int PERSONA_NATURAL = 6;

    /** Valida el número según su tipo; el motivo nunca repite el número (principio IX). */
    public Identificacion {
        Objects.requireNonNull(tipo, "tipo");
        numero = numero == null ? "" : numero.strip();
        if (tipo == TipoDeIdentificacion.CEDULA && !cedulaValida(numero)) {
            throw new DatoInvalido(
                    CAMPO,
                    "La cédula tiene 10 dígitos, empieza por una provincia válida y su último"
                            + " dígito tiene que cuadrar con los demás (módulo 10)");
        }
        if (tipo == TipoDeIdentificacion.PASAPORTE && !PASAPORTE.matcher(numero).matches()) {
            throw new DatoInvalido(
                    CAMPO, "El pasaporte tiene de 1 a 13 letras mayúsculas o dígitos");
        }
    }

    private static boolean cedulaValida(String numero) {
        if (!CEDULA.matcher(numero).matches()) {
            return false;
        }
        int provincia = Integer.parseInt(numero.substring(0, 2));
        if ((provincia < 1 || provincia > PROVINCIAS) && provincia != EXTERIOR) {
            return false;
        }
        if (numero.charAt(2) - '0' >= PERSONA_NATURAL) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int digito = (numero.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            suma += digito > 9 ? digito - 9 : digito;
        }
        return (10 - suma % 10) % 10 == numero.charAt(9) - '0';
    }
}
