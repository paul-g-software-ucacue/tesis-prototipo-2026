package ec.mdd.demoalfa.aplicacion;

import ec.mdd.demoalfa.puertos.entrada.ConsultaDemo;

/** Caso de uso de demostración: implementación interna del puerto de entrada. */
public class ServicioDemo implements ConsultaDemo {
    @Override
    public String saludo() {
        return "hola";
    }
}
