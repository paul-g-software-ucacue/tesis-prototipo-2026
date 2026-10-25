package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.Participante;
import ec.mdd.docentes.dominio.Proyecto;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/** El nombre completo del director y de cada participante de un proyecto, por identificación. */
final class NombresDeDocentes {

    private NombresDeDocentes() {}

    static Map<String, String> de(Proyecto proyecto, Function<String, Optional<Docente>> buscar) {
        Map<String, String> nombres = new LinkedHashMap<>();
        Stream.concat(
                        Stream.of(proyecto.director()),
                        proyecto.participantes().stream().map(Participante::docente))
                .distinct()
                .forEach(
                        uno ->
                                buscar.apply(uno)
                                        .ifPresent(
                                                docente ->
                                                        nombres.put(
                                                                uno,
                                                                docente.datos().nombreCompleto())));
        return nombres;
    }
}
