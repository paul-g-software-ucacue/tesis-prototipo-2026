package ec.mdd.arquitectura.fixtures.modulos.correcto.beta.aplicacion;

import ec.mdd.arquitectura.fixtures.modulos.correcto.alfa.puertos.entrada.ConsultaAlfa;

/** El módulo beta usa solo la API publicada del módulo alfa. */
public class ServicioBeta {
    private final ConsultaAlfa alfa;

    public ServicioBeta(ConsultaAlfa alfa) {
        this.alfa = alfa;
    }

    public String consultar() {
        return alfa.consultar();
    }
}
