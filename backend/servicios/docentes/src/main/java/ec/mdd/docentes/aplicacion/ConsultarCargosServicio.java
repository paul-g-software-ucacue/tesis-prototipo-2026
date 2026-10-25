package ec.mdd.docentes.aplicacion;

import ec.mdd.docentes.dominio.Cargo;
import ec.mdd.docentes.dominio.Docente;
import ec.mdd.docentes.dominio.EstadoDeCargo;
import ec.mdd.docentes.puertos.entrada.CargoConsultado;
import ec.mdd.docentes.puertos.entrada.ConsultarCargos;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consultar los cargos (F03-H05). Los filtros van en memoria: son cientos, como los docentes. Las
 * advertencias se calculan con el docente y la fecha de hoy, así que cambian solas cuando cambia su
 * ficha, su estado o el día (research §5).
 */
public final class ConsultarCargosServicio implements ConsultarCargos {

    private final RepositorioCargos repositorio;
    private final RepositorioDocentes docentes;
    private final Clock reloj;

    /** Recibe los repositorios y el reloj. */
    public ConsultarCargosServicio(
            RepositorioCargos repositorio, RepositorioDocentes docentes, Clock reloj) {
        this.repositorio = repositorio;
        this.docentes = docentes;
        this.reloj = reloj;
    }

    @Override
    public List<CargoConsultado> listar(EstadoDeCargo estado, String docente) {
        Map<String, Docente> porIdentificacion =
                docentes.todos().stream()
                        .collect(
                                Collectors.toMap(
                                        uno -> uno.identificacion().numero(), Function.identity()));
        LocalDate hoy = LocalDate.now(reloj);
        return repositorio.todos().stream()
                .filter(cargo -> estado == null || cargo.estado() == estado)
                .filter(cargo -> docente == null || cargo.docente().equals(docente))
                .map(cargo -> consultado(cargo, porIdentificacion.get(cargo.docente()), hoy))
                .toList();
    }

    private static CargoConsultado consultado(Cargo cargo, Docente suDocente, LocalDate hoy) {
        return new CargoConsultado(
                cargo, suDocente.datos().nombreCompleto(), cargo.advertencias(suDocente, hoy));
    }
}
