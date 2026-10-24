package ec.mdd.docentes.dominio;

import java.math.BigDecimal;

/**
 * Una experiencia laboral relacionada de la ficha académica (RF-17, carga 6). La valida {@link
 * FichaAcademica}.
 *
 * @param cargo hasta 150 caracteres
 * @param institucion hasta 150 caracteres
 * @param campoEspecifico tres dígitos; los dos primeros son su campo amplio
 * @param anios mayores que 0 y hasta 60, con decimales
 */
public record ExperienciaLaboral(
        String cargo, String institucion, String campoEspecifico, BigDecimal anios) {

    /** Su campo amplio: los dos primeros dígitos del específico. */
    public String campoAmplio() {
        return campoEspecifico.substring(0, 2);
    }

    /** Como en la carga 6: «cargo|institución|campo_especifico|años». */
    @Override
    public String toString() {
        return cargo
                + "|"
                + institucion
                + "|"
                + campoEspecifico
                + "|"
                + anios.stripTrailingZeros().toPlainString();
    }
}
