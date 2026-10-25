package ec.mdd.docentes.dominio;

/**
 * Un curso o capacitación de la ficha académica (RF-17, carga 6). Lo valida {@link FichaAcademica}.
 *
 * @param nombre hasta 150 caracteres
 * @param campoEspecifico tres dígitos; los dos primeros son su campo amplio
 * @param horas enteras, de 1 a 2000
 * @param anio de 1950 al año actual
 */
public record Curso(String nombre, String campoEspecifico, int horas, int anio) {

    /** Su campo amplio: los dos primeros dígitos del específico. */
    public String campoAmplio() {
        return campoEspecifico.substring(0, 2);
    }

    /** Como en la carga 6: «nombre|campo_especifico|horas|año». */
    @Override
    public String toString() {
        return nombre + "|" + campoEspecifico + "|" + horas + "|" + anio;
    }
}
