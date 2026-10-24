package ec.mdd.docentes.dominio;

/**
 * El campo de conocimiento CINE del título de tercer nivel: el amplio y el específico (glosario
 * «Campo de conocimiento», RF-17, RN-24). Lo valida {@link FichaAcademica}, que sabe en qué campo
 * del contrato está; que el amplio sea uno de los 11 de la CINE lo comprueba {@link Referencias}.
 *
 * @param amplio dos dígitos, como {@code 06}
 * @param especifico tres dígitos que empiezan por el amplio, como {@code 061}
 */
public record CampoConocimiento(String amplio, String especifico) {}
