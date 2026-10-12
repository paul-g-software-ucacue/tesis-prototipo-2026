package ec.mdd.arquitectura.fixtures.modulos.correcto.alfa.aplicacion;

import ec.mdd.arquitectura.fixtures.modulos.correcto.alfa.puertos.entrada.ConsultaAlfa;

/** Implementación interna del módulo alfa. */
public class ServicioAlfa implements ConsultaAlfa {
    @Override
    public String consultar() {
        return "alfa";
    }
}
