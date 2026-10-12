package ec.mdd.arquitectura.fixtures.capas.correcto.adaptadores;

import ec.mdd.arquitectura.fixtures.capas.correcto.dominio.Valor;
import ec.mdd.arquitectura.fixtures.capas.correcto.puertos.salida.Almacen;
import java.util.ArrayList;
import java.util.List;

/** Adaptador de la fixture correcta: implementa un puerto de salida. */
public class AlmacenEnMemoria implements Almacen {
    private final List<Valor> valores = new ArrayList<>();

    @Override
    public void guardar(Valor valor) {
        valores.add(valor);
    }
}
