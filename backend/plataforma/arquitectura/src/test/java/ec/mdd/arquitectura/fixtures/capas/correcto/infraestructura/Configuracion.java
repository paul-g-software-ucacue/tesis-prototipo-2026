package ec.mdd.arquitectura.fixtures.capas.correcto.infraestructura;

import ec.mdd.arquitectura.fixtures.capas.correcto.adaptadores.AlmacenEnMemoria;
import ec.mdd.arquitectura.fixtures.capas.correcto.aplicacion.RegistrarServicio;
import ec.mdd.arquitectura.fixtures.capas.correcto.puertos.entrada.Registrar;

/** Infraestructura de la fixture correcta: ensambla el adaptador y el caso de uso. */
public class Configuracion {
    public Registrar registrar() {
        return new RegistrarServicio(new AlmacenEnMemoria());
    }
}
