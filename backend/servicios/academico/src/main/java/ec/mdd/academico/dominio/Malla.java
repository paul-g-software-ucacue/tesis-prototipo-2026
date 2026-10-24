package ec.mdd.academico.dominio;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * El plan de estudios aprobado de una carrera, con su código de versión (glosario «Malla», RF-14,
 * aclaración 2 de F03-H02). Una carrera puede tener varias; una derogada conserva sus asignaturas,
 * pero no admite nuevas.
 *
 * @param codigo el código de versión, como {@code DS-2019}; no cambia
 * @param carrera el código de su carrera
 * @param estado vigente o derogada
 */
public record Malla(String codigo, String carrera, EstadoDeMalla estado) {

    // El formato de codigo_version_malla de la carga 04.
    private static final Pattern CODIGO = Pattern.compile("[0-9A-Z-]{1,20}");

    /** Valida el código y la carrera. */
    public Malla {
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new DatoInvalido(
                    "codigo",
                    "El código de versión solo admite mayúsculas, números y guion, hasta 20"
                            + " caracteres");
        }
        if (carrera == null || carrera.isBlank()) {
            throw new DatoInvalido("carrera", "Elige la carrera de la malla");
        }
        Objects.requireNonNull(estado, "estado");
    }

    /** Una malla recién registrada, vigente. */
    public static Malla nueva(String codigo, String carrera) {
        return new Malla(codigo, carrera, EstadoDeMalla.VIGENTE);
    }

    /** La malla con otro estado. */
    public Malla conEstado(EstadoDeMalla estado) {
        return new Malla(codigo, carrera, estado);
    }
}
