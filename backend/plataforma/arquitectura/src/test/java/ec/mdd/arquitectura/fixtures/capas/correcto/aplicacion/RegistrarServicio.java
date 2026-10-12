package ec.mdd.arquitectura.fixtures.capas.correcto.aplicacion;

import ec.mdd.arquitectura.fixtures.capas.correcto.dominio.Valor;
import ec.mdd.arquitectura.fixtures.capas.correcto.puertos.entrada.Registrar;
import ec.mdd.arquitectura.fixtures.capas.correcto.puertos.salida.Almacen;

/** Caso de uso de la fixture correcta: usa el dominio y los puertos. */
public class RegistrarServicio implements Registrar {
    private final Almacen almacen;

    public RegistrarServicio(Almacen almacen) {
        this.almacen = almacen;
    }

    @Override
    public void registrar(String texto) {
        almacen.guardar(new Valor(texto));
    }
}
