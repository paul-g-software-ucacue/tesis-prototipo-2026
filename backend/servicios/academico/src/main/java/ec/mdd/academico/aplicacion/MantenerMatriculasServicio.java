package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.Matricula;
import ec.mdd.academico.puertos.entrada.MantenerMatriculas;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioMatriculas;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registrar y corregir los matriculados (F01-H04): en una transacción, con su registro de cambios
 * (principio VII). Las referencias al periodo y a la carrera las asegura la base.
 */
public final class MantenerMatriculasServicio implements MantenerMatriculas {

    private final RepositorioMatriculas repositorio;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe el repositorio, el registro de cambios, la transacción y el reloj. */
    public MantenerMatriculasServicio(
            RepositorioMatriculas repositorio,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void registrar(Matricula matricula, String quien) {
        transaccion.ejecutar(
                () -> {
                    Instant ahora = reloj.instant();
                    repositorio.registrar(matricula, quien, ahora);
                    registro.registrar(
                            new CambioRegistrado(
                                    ahora, quien, Accion.MATRICULA_REGISTRADA, detalle(matricula)));
                    return null;
                });
    }

    @Override
    public void corregir(Matricula matricula, String quien) {
        transaccion.ejecutar(
                () -> {
                    repositorio.guardar(matricula);
                    registro.registrar(
                            new CambioRegistrado(
                                    reloj.instant(),
                                    quien,
                                    Accion.MATRICULA_CORREGIDA,
                                    detalle(matricula)));
                    return null;
                });
    }

    private static Map<String, Object> detalle(Matricula matricula) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("periodo", matricula.periodo());
        detalle.put("carrera", matricula.carrera());
        detalle.put("total", matricula.total());
        detalle.put("fechaCorte", matricula.fechaCorte().toString());
        return detalle;
    }
}
