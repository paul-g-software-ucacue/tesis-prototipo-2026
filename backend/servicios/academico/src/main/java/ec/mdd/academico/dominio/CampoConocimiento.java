package ec.mdd.academico.dominio;

import java.util.regex.Pattern;

/**
 * El campo de conocimiento CINE de una carrera o una asignatura: el amplio y el específico
 * (glosario «Campo de conocimiento», RN-24). El específico empieza por el amplio. Que el amplio sea
 * uno de los 11 de la CINE lo comprueba {@link DatosMaestros}, con el catálogo.
 *
 * @param amplio dos dígitos, como {@code 06}
 * @param especifico tres dígitos que empiezan por el amplio, como {@code 061}
 */
public record CampoConocimiento(String amplio, String especifico) {

    private static final Pattern AMPLIO = Pattern.compile("[0-9]{2}");
    private static final Pattern ESPECIFICO = Pattern.compile("[0-9]{3}");

    /** Valida la forma de los dos y que el específico empiece por el amplio. */
    public CampoConocimiento {
        if (amplio == null || !AMPLIO.matcher(amplio).matches()) {
            throw new DatoInvalido("campoAmplio", "El campo amplio tiene dos dígitos");
        }
        if (especifico == null || !ESPECIFICO.matcher(especifico).matches()) {
            throw new DatoInvalido("campoEspecifico", "El campo específico tiene tres dígitos");
        }
        if (!especifico.startsWith(amplio)) {
            throw new DatoInvalido(
                    "campoEspecifico", "El campo específico tiene que empezar por el campo amplio");
        }
    }
}
