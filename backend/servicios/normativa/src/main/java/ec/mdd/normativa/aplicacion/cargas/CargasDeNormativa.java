package ec.mdd.normativa.aplicacion.cargas;

import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import ec.mdd.normativa.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.AplicadorPorTraductores;
import java.util.List;

/**
 * El aplicador de la carga de Normativa (MD-13, ADR-022): la 07, la política de carga horaria, que
 * entra como una versión de normativa nueva o editada, completa o nada.
 */
public final class CargasDeNormativa {

    private CargasDeNormativa() {}

    /** El aplicador con el traductor de la política. */
    public static AplicadorDeCargas aplicador(
            Transaccion transaccion,
            RepositorioVersiones versiones,
            CrearVersion crear,
            EditarVersion editar) {
        return new AplicadorPorTraductores(
                List.of(new TraductorPolitica(versiones, crear, editar)), transaccion::ejecutar);
    }
}
