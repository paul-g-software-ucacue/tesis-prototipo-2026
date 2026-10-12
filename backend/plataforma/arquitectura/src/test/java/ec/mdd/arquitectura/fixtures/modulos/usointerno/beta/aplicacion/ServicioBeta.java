package ec.mdd.arquitectura.fixtures.modulos.usointerno.beta.aplicacion;

import ec.mdd.arquitectura.fixtures.modulos.usointerno.alfa.aplicacion.ServicioAlfa;

/** Violación: el módulo beta usa una clase interna del módulo alfa. */
public class ServicioBeta {
    private final ServicioAlfa alfa = new ServicioAlfa();

    public String consultar() {
        return alfa.consultar();
    }
}
