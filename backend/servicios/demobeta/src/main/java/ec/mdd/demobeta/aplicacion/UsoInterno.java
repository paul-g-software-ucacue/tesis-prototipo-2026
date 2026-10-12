package ec.mdd.demobeta.aplicacion;

import ec.mdd.demoalfa.aplicacion.ServicioDemo;

/**
 * Violación de demostración: usa una clase interna de demoalfa, no su puertos.entrada (escenario
 * 2).
 */
public class UsoInterno {
    public String saludo() {
        return new ServicioDemo().saludo();
    }
}
