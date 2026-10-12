package ec.mdd.arquitectura.fixtures.modulos.usointerno.alfa.aplicacion;

import ec.mdd.arquitectura.fixtures.modulos.usointerno.alfa.puertos.entrada.ConsultaAlfa;

/** Implementación interna del módulo alfa. */
public class ServicioAlfa implements ConsultaAlfa {
    @Override
    public String consultar() {
        return "alfa";
    }
}
