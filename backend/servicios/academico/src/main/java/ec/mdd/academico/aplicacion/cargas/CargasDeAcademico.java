package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.puertos.entrada.CorregirPeriodo;
import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.entrada.MantenerMatriculas;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.academico.puertos.entrada.RegistrarPeriodo;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioMatriculas;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.AplicadorPorTraductores;
import java.util.List;

/**
 * El aplicador de las cargas de Académico (MD-13, ADR-022): las cargas 01 a 05 y 10. Cada lote se
 * aplica completo o nada en una transacción de Académico, con sus casos de uso.
 */
public final class CargasDeAcademico {

    private CargasDeAcademico() {}

    /**
     * Lo que necesitan los traductores de Académico.
     *
     * @param transaccion la transacción de Académico
     * @param estructura las unidades
     * @param mantenerEstructura su caso de uso
     * @param carreras las carreras, las mallas y las asignaturas
     * @param mantenerCarreras su caso de uso
     * @param periodos los periodos
     * @param registrarPeriodo su registro
     * @param corregirPeriodo su corrección
     * @param oferta las asignaturas-paralelo
     * @param mantenerOferta su caso de uso
     * @param matriculas los matriculados
     * @param mantenerMatriculas su caso de uso
     */
    public record Dependencias(
            Transaccion transaccion,
            RepositorioEstructura estructura,
            MantenerEstructura mantenerEstructura,
            RepositorioCarreras carreras,
            MantenerCarreras mantenerCarreras,
            RepositorioPeriodos periodos,
            RegistrarPeriodo registrarPeriodo,
            CorregirPeriodo corregirPeriodo,
            RepositorioOferta oferta,
            MantenerOferta mantenerOferta,
            RepositorioMatriculas matriculas,
            MantenerMatriculas mantenerMatriculas) {}

    /** El aplicador con los 6 traductores de Académico. */
    public static AplicadorDeCargas aplicador(Dependencias d) {
        return new AplicadorPorTraductores(
                List.of(
                        new TraductorEstructura(d.estructura(), d.mantenerEstructura()),
                        new TraductorCarreras(d.carreras(), d.mantenerCarreras()),
                        new TraductorPeriodos(
                                d.periodos(), d.registrarPeriodo(), d.corregirPeriodo()),
                        new TraductorAsignaturas(d.carreras(), d.mantenerCarreras()),
                        new TraductorOferta(d.oferta(), d.mantenerOferta()),
                        new TraductorMatriculas(d.matriculas(), d.mantenerMatriculas())),
                d.transaccion()::ejecutar);
    }
}
