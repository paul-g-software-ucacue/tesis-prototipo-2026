package ec.mdd.arquitectura.fixtures.capas.aplicacionconadaptador.aplicacion;

import ec.mdd.arquitectura.fixtures.capas.aplicacionconadaptador.adaptadores.Adaptador;

/** Violación: la aplicación depende de un adaptador concreto. */
public class Servicio {
    private final Adaptador adaptador = new Adaptador();

    public void ejecutar() {
        adaptador.ejecutar();
    }
}
