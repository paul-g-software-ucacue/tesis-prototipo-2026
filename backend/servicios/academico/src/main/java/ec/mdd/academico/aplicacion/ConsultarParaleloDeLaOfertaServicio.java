package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.Asignatura;
import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoPeriodo;
import ec.mdd.academico.dominio.OfertaDelPeriodo;
import ec.mdd.academico.puertos.entrada.ConsultarOferta;
import ec.mdd.academico.puertos.entrada.ConsultarParaleloDeLaOferta;
import ec.mdd.academico.puertos.entrada.ParaleloConCampos;
import java.util.List;
import java.util.Optional;

/**
 * Una asignatura-paralelo de un periodo, en cualquier estado del periodo. La retirada también se
 * devuelve, con su estado: quien la pide decide qué hacer con ella.
 */
public final class ConsultarParaleloDeLaOfertaServicio implements ConsultarParaleloDeLaOferta {

    private final ConsultarOferta consultar;

    /** Recibe la consulta de la oferta del periodo. */
    public ConsultarParaleloDeLaOfertaServicio(ConsultarOferta consultar) {
        this.consultar = consultar;
    }

    @Override
    public Optional<ParaleloConCampos> paralelo(
            String periodo, String asignatura, String paralelo) {
        OfertaDelPeriodo oferta = consultar.oferta(periodo);
        return oferta.asignaturaParalelo(asignatura, paralelo)
                .flatMap(
                        una ->
                                oferta.datos()
                                        .asignatura(asignatura)
                                        .map(datos -> respuesta(oferta, una, datos)));
    }

    @Override
    public List<ParaleloConCampos> delPeriodo(String periodo) {
        OfertaDelPeriodo oferta = consultar.oferta(periodo);
        return oferta.asignaturasParalelo().stream()
                .flatMap(
                        una ->
                                oferta.datos().asignatura(una.asignatura()).stream()
                                        .map(datos -> respuesta(oferta, una, datos)))
                .toList();
    }

    private static ParaleloConCampos respuesta(
            OfertaDelPeriodo oferta, AsignaturaParalelo una, Asignatura asignatura) {
        return new ParaleloConCampos(
                asignatura.codigo(),
                asignatura.nombre(),
                asignatura.carrera(),
                una.paralelo(),
                asignatura.campo().amplio(),
                asignatura.campo().especifico(),
                asignatura.troncoComun(),
                oferta.horasDe(una),
                una.estado(),
                oferta.periodo().estado() == EstadoPeriodo.CERRADO);
    }
}
