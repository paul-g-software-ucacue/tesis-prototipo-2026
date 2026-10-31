package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.dominio.AfinidadDelDocente;
import ec.mdd.generacion.dominio.AsignaturaOfertada;
import ec.mdd.generacion.dominio.CalculadoraAfinidad;
import ec.mdd.generacion.dominio.FormacionDelDocente;
import ec.mdd.generacion.dominio.PesosDelPeriodo;
import ec.mdd.generacion.puertos.entrada.ConsultarAfinidad;
import ec.mdd.generacion.puertos.entrada.DocenteNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.generacion.puertos.entrada.PeriodoSinAbrir;
import ec.mdd.generacion.puertos.salida.DatosParaAfinidad;
import java.util.List;

/**
 * Calcula la afinidad en cada consulta, así que siempre refleja la ficha de ese momento; nada se
 * guarda (FR-017). Pide los datos uno a uno y el primero que falla corta: nunca hay puntajes
 * parciales.
 */
public final class ConsultarAfinidadServicio implements ConsultarAfinidad {

    private final DatosParaAfinidad datos;
    private final CalculadoraAfinidad calculadora = new CalculadoraAfinidad();

    /** Recibe los datos de los otros servicios. */
    public ConsultarAfinidadServicio(DatosParaAfinidad datos) {
        this.datos = datos;
    }

    @Override
    public AfinidadDelDocente afinidad(String identificacion, String periodo) {
        List<AsignaturaOfertada> asignaturas =
                datos.asignaturasOfertadas(periodo).orElseThrow(PeriodoNoEncontrado::new);
        PesosDelPeriodo pesos = datos.pesos(periodo).orElseThrow(PeriodoSinAbrir::new);
        FormacionDelDocente formacion =
                datos.formacion(identificacion).orElseThrow(DocenteNoEncontrado::new);
        return new AfinidadDelDocente(
                periodo,
                pesos.version(),
                calculadora.calcular(formacion, asignaturas, pesos.pesos()));
    }
}
