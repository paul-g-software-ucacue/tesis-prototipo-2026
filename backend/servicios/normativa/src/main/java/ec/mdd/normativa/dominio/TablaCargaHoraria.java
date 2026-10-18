package ec.mdd.normativa.dominio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Las horas por componente de cada perfil según su tipo de IST (glosario §7.3, carga 7). En la
 * política 2024 son 22 filas.
 *
 * <p>Invariantes: un perfil y un tipo de IST no se repiten, y un perfil aplica a todos los tipos o
 * a tipos concretos, no a las dos cosas. Así, cada perfil tiene a lo sumo una fila para cada tipo.
 *
 * @param filas las filas, en el orden de la política
 */
public record TablaCargaHoraria(List<FilaCargaHoraria> filas) {

    /** Comprueba las invariantes y copia las filas. */
    public TablaCargaHoraria {
        if (filas == null || filas.isEmpty()) {
            throw new IllegalArgumentException(
                    "La tabla de carga horaria necesita al menos una fila");
        }
        filas = List.copyOf(filas);
        Set<String> vistas = new HashSet<>();
        Map<String, Boolean> aplicaATodos = new HashMap<>();
        for (FilaCargaHoraria fila : filas) {
            if (!vistas.add(fila.perfil() + "@" + fila.aplicaA())) {
                throw new IllegalArgumentException(
                        "La fila de " + fila.perfil() + " para " + nombre(fila) + " está repetida");
            }
            Boolean anterior = aplicaATodos.putIfAbsent(fila.perfil(), fila.aplicaATodos());
            if (anterior != null && anterior != fila.aplicaATodos()) {
                throw new IllegalArgumentException(
                        "El perfil "
                                + fila.perfil()
                                + " aplica a la vez a todos los tipos de IST y a uno concreto");
            }
        }
    }

    /** La fila del perfil para ese tipo de IST: la del tipo o, si no hay, la que aplica a todos. */
    public Optional<FilaCargaHoraria> filaPara(String perfil, TipoIst tipoIst) {
        return filas.stream()
                .filter(fila -> fila.perfil().equals(perfil))
                .filter(fila -> fila.aplicaATodos() || fila.aplicaA() == tipoIst)
                .findFirst();
    }

    private static String nombre(FilaCargaHoraria fila) {
        return fila.aplicaATodos() ? "todos los tipos" : fila.aplicaA().name();
    }
}
