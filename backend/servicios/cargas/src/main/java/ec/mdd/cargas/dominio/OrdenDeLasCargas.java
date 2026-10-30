package ec.mdd.cargas.dominio;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * El orden de las cargas (glosario «Orden de las cargas», RF-56, ERS §8.3): una carga no se puede
 * subir mientras alguna de las que depende esté pendiente. Las dependencias salen solo del {@code
 * dependeDe} del diccionario de datos. No conoce a los servicios dueños: recibe quién dice si una
 * carga tiene datos y le pregunta una sola vez por cada carga, así que se arma para cada consulta.
 */
public final class OrdenDeLasCargas {

    private final DiccionarioDeDatos diccionario;
    private final Predicate<String> tieneDatos;
    private final Map<String, Boolean> respuestas = new HashMap<>();

    /**
     * @param diccionario el diccionario de datos, con las 10 cargas y sus dependencias
     * @param tieneDatos si el servicio dueño de la carga con ese código tiene datos vigentes de
     *     ella
     */
    public OrdenDeLasCargas(DiccionarioDeDatos diccionario, Predicate<String> tieneDatos) {
        this.diccionario = diccionario;
        this.tieneDatos = tieneDatos;
    }

    /**
     * Las cargas de las que depende la del código que no tienen datos, en el orden de la ERS.
     *
     * @throws IllegalArgumentException si no hay una carga con ese código
     */
    public List<CargaQueFalta> faltantes(String codigo) {
        Carga carga =
                diccionario
                        .carga(codigo)
                        .orElseThrow(
                                () -> new IllegalArgumentException("No hay una carga " + codigo));
        return diccionario.cargas().stream()
                .filter(anterior -> carga.dependeDe().contains(anterior.numero()))
                .filter(anterior -> !cargada(anterior))
                .map(CargaQueFalta::de)
                .toList();
    }

    /**
     * El estado de las 10 cargas, en su orden.
     *
     * @param periodosSinProyectos los periodos confirmados «sin proyectos» que siguen valiendo;
     *     solo cuentan para la carga que admite esa confirmación
     */
    public List<EstadoDeCarga> estados(Collection<String> periodosSinProyectos) {
        List<String> periodos = periodosSinProyectos.stream().sorted().toList();
        return diccionario.cargas().stream()
                .map(
                        carga -> {
                            boolean admite =
                                    carga.obligatoriedad()
                                            == Obligatoriedad.OBLIGATORIA_SALVO_SIN_PROYECTOS;
                            List<String> suyos = admite ? periodos : List.of();
                            return new EstadoDeCarga(
                                    carga.numero(),
                                    carga.codigo(),
                                    estado(carga, suyos),
                                    faltantes(carga.codigo()),
                                    suyos);
                        })
                .toList();
    }

    private EstadoDeLaCarga estado(Carga carga, List<String> periodosSinProyectos) {
        if (cargada(carga)) {
            return EstadoDeLaCarga.CARGADA;
        }
        return periodosSinProyectos.isEmpty()
                ? EstadoDeLaCarga.PENDIENTE
                : EstadoDeLaCarga.SIN_PROYECTOS;
    }

    private boolean cargada(Carga carga) {
        return respuestas.computeIfAbsent(carga.codigo(), tieneDatos::test);
    }
}
