package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.puertos.entrada.MantenerCarreras;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.AplicadorDeCargas;
import ec.mdd.plataforma.cargas.AplicadorPorTraductores;
import java.util.List;

/**
 * El aplicador de las cargas de Académico (MD-13, ADR-022): cada lote se aplica completo o nada en
 * una transacción de Académico, con sus casos de uso.
 */
public final class CargasDeAcademico {

    private CargasDeAcademico() {}

    /** El aplicador con los traductores de Académico. */
    public static AplicadorDeCargas aplicador(
            Transaccion transaccion,
            RepositorioEstructura estructura,
            MantenerEstructura mantenerEstructura,
            RepositorioCarreras carreras,
            MantenerCarreras mantenerCarreras) {
        return new AplicadorPorTraductores(
                List.of(
                        new TraductorEstructura(estructura, mantenerEstructura),
                        new TraductorCarreras(carreras, mantenerCarreras)),
                transaccion::ejecutar);
    }
}
