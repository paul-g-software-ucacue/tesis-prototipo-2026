package ec.mdd.docentes.aplicacion.cargas;

import ec.mdd.docentes.puertos.entrada.MantenerCargos;
import ec.mdd.docentes.puertos.entrada.MantenerDocentes;
import ec.mdd.docentes.puertos.entrada.MantenerProyectos;
import ec.mdd.docentes.puertos.salida.RepositorioCargos;
import ec.mdd.docentes.puertos.salida.RepositorioDocentes;
import ec.mdd.docentes.puertos.salida.RepositorioProyectos;
import ec.mdd.docentes.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.AplicadorPorTraductores;
import java.util.List;

/**
 * El aplicador de las cargas de Docentes (MD-13, ADR-022): la 06, la 08 y la 09. Cada lote se
 * aplica completo o nada en una transacción de Docentes, con sus casos de uso.
 */
public final class CargasDeDocentes {

    private CargasDeDocentes() {}

    /** El aplicador con los 3 traductores de Docentes. */
    public static AplicadorDeCargas aplicador(
            Transaccion transaccion,
            RepositorioDocentes docentes,
            MantenerDocentes mantenerDocentes,
            RepositorioCargos cargos,
            MantenerCargos mantenerCargos,
            RepositorioProyectos proyectos,
            MantenerProyectos mantenerProyectos) {
        return new AplicadorPorTraductores(
                List.of(
                        new TraductorDocentesContratados(docentes, mantenerDocentes),
                        new TraductorCargos(cargos, mantenerCargos),
                        new TraductorProyectos(proyectos, mantenerProyectos)),
                transaccion::ejecutar);
    }
}
