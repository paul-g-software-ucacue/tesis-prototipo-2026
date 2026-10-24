package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.AsignaturaParalelo;
import ec.mdd.academico.dominio.EstadoDeAsignaturaParalelo;
import ec.mdd.academico.dominio.OfertaCerrada;
import ec.mdd.academico.dominio.OfertaDelPeriodo;
import ec.mdd.academico.dominio.PeriodoAcademico;
import ec.mdd.academico.puertos.entrada.AsignaturaParaleloNoEncontrada;
import ec.mdd.academico.puertos.entrada.DatosDeAsignaturaParalelo;
import ec.mdd.academico.puertos.entrada.MantenerOferta;
import ec.mdd.academico.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioCarreras;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.RepositorioOferta;
import ec.mdd.academico.puertos.salida.RepositorioPeriodos;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mantener la oferta de un periodo: en una transacción se bloquean el periodo, las carreras y la
 * oferta, el dominio valida el cambio y, si algo cambió, se guarda y se registra. Las repetidas se
 * deciden con lo que se leyó bajo el candado (research §4). Sin cambios, no se registra.
 */
public final class MantenerOfertaServicio implements MantenerOferta {

    private final RepositorioPeriodos periodos;
    private final RepositorioOferta repositorio;
    private final RepositorioCarreras carreras;
    private final RepositorioEstructura estructura;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public MantenerOfertaServicio(
            RepositorioPeriodos periodos,
            RepositorioOferta repositorio,
            RepositorioCarreras carreras,
            RepositorioEstructura estructura,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.periodos = periodos;
        this.repositorio = repositorio;
        this.carreras = carreras;
        this.estructura = estructura;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public AsignaturaParalelo ofertar(AsignaturaParalelo nueva, String quien) {
        return transaccion.ejecutar(
                () -> {
                    bloqueada(nueva.periodo()).ofertar(nueva);
                    Instant ahora = reloj.instant();
                    repositorio.registrar(nueva, quien, ahora);
                    Map<String, Object> detalle = clave(nueva);
                    detalle.put("jornada", nueva.jornada().name());
                    detalle.put("modalidad", nueva.modalidad().name());
                    detalle.put("estudiantes", nueva.estudiantes());
                    if (nueva.sede() != null) {
                        detalle.put("sede", nueva.sede());
                    }
                    if (nueva.horasIndicadas() != null) {
                        detalle.put(
                                "horasSemanales",
                                nueva.horasIndicadas().stripTrailingZeros().toPlainString());
                    }
                    registrar(ahora, quien, Accion.ASIGNATURA_PARALELO_REGISTRADA, detalle);
                    return nueva;
                });
    }

    @Override
    public AsignaturaParalelo corregir(
            String periodo,
            String asignatura,
            String paralelo,
            DatosDeAsignaturaParalelo datos,
            String quien) {
        return transaccion.ejecutar(
                () -> {
                    OfertaDelPeriodo oferta = bloqueada(periodo);
                    AsignaturaParalelo actual = existente(oferta, asignatura, paralelo);
                    AsignaturaParalelo.Correccion correccion =
                            oferta.corregir(
                                    actual,
                                    datos.jornada(),
                                    datos.modalidad(),
                                    datos.sede(),
                                    datos.estudiantes(),
                                    datos.horasIndicadas());
                    if (!correccion.cambios().isEmpty()) {
                        repositorio.guardar(correccion.asignaturaParalelo());
                        Map<String, Object> detalle = clave(actual);
                        detalle.put(
                                "cambios",
                                correccion.cambios().stream().map(Detalles::de).toList());
                        registrar(
                                reloj.instant(),
                                quien,
                                Accion.ASIGNATURA_PARALELO_CORREGIDA,
                                detalle);
                    }
                    return correccion.asignaturaParalelo();
                });
    }

    @Override
    public AsignaturaParalelo cambiarEstado(
            String periodo,
            String asignatura,
            String paralelo,
            EstadoDeAsignaturaParalelo estado,
            String quien) {
        return transaccion.ejecutar(
                () -> {
                    OfertaDelPeriodo oferta = bloqueada(periodo);
                    AsignaturaParalelo actual = existente(oferta, asignatura, paralelo);
                    if (!oferta.modificable()) {
                        throw new OfertaCerrada(periodo);
                    }
                    if (actual.estado() == estado) {
                        return actual;
                    }
                    AsignaturaParalelo cambiada = oferta.cambiarEstado(actual, estado);
                    repositorio.guardar(cambiada);
                    registrar(
                            reloj.instant(),
                            quien,
                            estado == EstadoDeAsignaturaParalelo.RETIRADA
                                    ? Accion.ASIGNATURA_PARALELO_RETIRADA
                                    : Accion.ASIGNATURA_PARALELO_REOFERTADA,
                            clave(actual));
                    return cambiada;
                });
    }

    // El periodo, las carreras y la oferta, en ese orden, para que dos cambios no se crucen.
    private OfertaDelPeriodo bloqueada(String codigo) {
        PeriodoAcademico periodo =
                periodos.bloquear(codigo).orElseThrow(() -> new PeriodoNoEncontrado(codigo));
        carreras.bloquear();
        repositorio.bloquear();
        return new OfertaDelPeriodo(
                periodo,
                repositorio.delPeriodo(codigo),
                ConsultarOfertaServicio.datosMaestros(carreras, estructura));
    }

    private static AsignaturaParalelo existente(
            OfertaDelPeriodo oferta, String asignatura, String paralelo) {
        return oferta.asignaturaParalelo(asignatura, paralelo)
                .orElseThrow(
                        () ->
                                new AsignaturaParaleloNoEncontrada(
                                        oferta.periodo().codigo(), asignatura, paralelo));
    }

    private static Map<String, Object> clave(AsignaturaParalelo asignaturaParalelo) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("periodo", asignaturaParalelo.periodo());
        detalle.put("asignatura", asignaturaParalelo.asignatura());
        detalle.put("paralelo", asignaturaParalelo.paralelo());
        return detalle;
    }

    private void registrar(
            Instant momento, String quien, Accion accion, Map<String, Object> detalle) {
        registro.registrar(new CambioRegistrado(momento, quien, accion, detalle));
    }
}
