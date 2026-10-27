package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** El historial de cargas en memoria, escrito a mano para las pruebas de los controladores. */
public final class HistorialEnMemoria implements HistorialDeCargas {

    private final List<EntradaDeHistorial> entradas = new ArrayList<>();

    @Override
    public synchronized void registrar(EntradaDeHistorial entrada) {
        entradas.add(entrada);
    }

    @Override
    public synchronized List<EntradaDeHistorial> deLaCarga(String carga) {
        List<EntradaDeHistorial> deLaCarga =
                new ArrayList<>(entradas.stream().filter(e -> e.carga().equals(carga)).toList());
        deLaCarga.sort(Comparator.comparing(EntradaDeHistorial::momento).reversed());
        return deLaCarga;
    }

    @Override
    public synchronized Optional<EntradaDeHistorial> aplicada(String carga, String hash) {
        return entradas.stream()
                .filter(e -> e.carga().equals(carga) && e.hash().equals(hash))
                .filter(e -> e.resultado() == ResultadoDeHistorial.APLICADA)
                .findFirst();
    }

    /** Vacía el historial. */
    public synchronized void vaciar() {
        entradas.clear();
    }
}
