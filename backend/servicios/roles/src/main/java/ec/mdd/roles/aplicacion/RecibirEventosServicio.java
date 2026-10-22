package ec.mdd.roles.aplicacion;

import ec.mdd.roles.dominio.EstadoConocido;
import ec.mdd.roles.dominio.PeriodoConocido;
import ec.mdd.roles.puertos.entrada.EventoRecibido;
import ec.mdd.roles.puertos.entrada.RecibirEventos;
import ec.mdd.roles.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.roles.puertos.salida.RepositorioPeriodosConocidos;
import ec.mdd.roles.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * El receptor de los avisos de Académico (F05-H03, research §1). Anota el evento y, solo si es
 * nuevo, actualiza la copia del periodo, las dos cosas en una sola transacción: si algo falla, el
 * emisor lo vuelve a enviar. La copia nunca retrocede, así que el orden de llegada no importa.
 */
public final class RecibirEventosServicio implements RecibirEventos {

    /** El tipo que admite, con su contenido {@code {codigo, estado}}. */
    public static final String PERIODO_ACTUALIZADO = "academico.PeriodoActualizado";

    private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9._-]{1,20}");

    private final RepositorioEventosRecibidos eventos;
    private final RepositorioPeriodosConocidos periodos;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public RecibirEventosServicio(
            RepositorioEventosRecibidos eventos,
            RepositorioPeriodosConocidos periodos,
            Transaccion transaccion,
            Clock reloj) {
        this.eventos = eventos;
        this.periodos = periodos;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void recibir(EventoRecibido evento) {
        PeriodoConocido avisado = periodoDe(evento);
        transaccion.ejecutar(
                () -> {
                    Instant ahora = reloj.instant();
                    if (eventos.registrarSiEsNuevo(
                            evento.idEvento(), evento.tipo(), evento.origen(), ahora)) {
                        PeriodoConocido copia =
                                periodos.bloquear(avisado.codigo())
                                        .map(conocido -> conocido.conEstado(avisado.estado()))
                                        .orElse(avisado);
                        periodos.guardar(copia, ahora);
                    }
                    return null;
                });
    }

    // Un tipo que no es el aviso, o un contenido que no tiene su forma, no se aplica.
    private static PeriodoConocido periodoDe(EventoRecibido evento) {
        if (!PERIODO_ACTUALIZADO.equals(evento.tipo())
                || !(evento.contenido().get("codigo") instanceof String codigo)
                || !CODIGO.matcher(codigo).matches()
                || !(evento.contenido().get("estado") instanceof String estado)) {
            throw new EventoNoReconocido(evento.tipo());
        }
        try {
            return new PeriodoConocido(codigo, EstadoConocido.valueOf(estado));
        } catch (IllegalArgumentException estadoDesconocido) {
            throw new EventoNoReconocido(evento.tipo());
        }
    }
}
