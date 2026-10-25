package ec.mdd.docentes.dominio;

/**
 * Una maestría, especialización o doctorado de la ficha académica (RF-17, carga 6). Lo valida
 * {@link FichaAcademica}.
 *
 * @param titulo hasta 150 caracteres
 * @param tipo maestría, especialización o doctorado
 * @param campoEspecifico tres dígitos; los dos primeros son su campo amplio
 * @param anio de 1950 al año actual
 */
public record TituloCuartoNivel(
        String titulo, TipoDeTituloDeCuartoNivel tipo, String campoEspecifico, int anio) {

    /** Su campo amplio: los dos primeros dígitos del específico. */
    public String campoAmplio() {
        return campoEspecifico.substring(0, 2);
    }

    /** Como en la carga 6: «título|tipo|campo_especifico|año». */
    @Override
    public String toString() {
        return titulo + "|" + tipo + "|" + campoEspecifico + "|" + anio;
    }
}
