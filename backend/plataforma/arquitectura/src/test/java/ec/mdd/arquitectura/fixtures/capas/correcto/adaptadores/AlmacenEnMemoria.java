package ec.mdd.arquitectura.fixtures.capas.correcto.adaptadores;

import java.util.ArrayList;
import java.util.List;
import ec.mdd.arquitectura.fixtures.capas.correcto.dominio.Valor;
import ec.mdd.arquitectura.fixtures.capas.correcto.puertos.salida.Almacen;

/** Adaptador de la fixture correcta: implementa un puerto de salida. */
public class AlmacenEnMemoria implements Almacen {
    private final List<Valor> valores = new ArrayList<>();

    @Override
    public void guardar(Valor valor) {
        valores.add(valor);
    }
}
