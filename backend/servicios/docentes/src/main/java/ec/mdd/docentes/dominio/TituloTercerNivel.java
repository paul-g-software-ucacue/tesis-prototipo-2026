package ec.mdd.docentes.dominio;

/**
 * El título de tercer nivel de la ficha académica, como consta en la SENESCYT (RF-17, carga 6). Lo
 * valida {@link FichaAcademica}.
 *
 * @param titulo hasta 150 caracteres
 * @param registroSenescyt el número de registro, hasta 30 caracteres, o {@code null}
 * @param campo su campo amplio y específico
 */
public record TituloTercerNivel(String titulo, String registroSenescyt, CampoConocimiento campo) {}
