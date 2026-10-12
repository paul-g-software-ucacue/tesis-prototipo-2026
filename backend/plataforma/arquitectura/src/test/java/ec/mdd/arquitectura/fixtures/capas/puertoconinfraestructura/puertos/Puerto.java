package ec.mdd.arquitectura.fixtures.capas.puertoconinfraestructura.puertos;

import ec.mdd.arquitectura.fixtures.capas.puertoconinfraestructura.infraestructura.Config;

/** Violación: un puerto expone una clase de infraestructura. */
public interface Puerto {
    Config configuracion();
}
