package ec.mdd.generacion.aplicacion;

import ec.mdd.generacion.puertos.entrada.PublicarEventoDePrueba;
import ec.mdd.generacion.puertos.salida.EventoSaliente;
import ec.mdd.generacion.puertos.salida.PublicadorDeEventos;
import ec.mdd.generacion.puertos.salida.RepositorioEventosDePrueba;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * El evento de prueba de B00-H05 (D-48): una fila en {@code generacion.evento_prueba} y la
 * publicación de {@code generacion.EventoDePrueba} hacia Usuarios, en una sola transacción. Así se
 * demuestra que la transacción del servicio se confirma aunque el receptor esté caído, y que una
 * transacción revertida no deja publicación.
 *
 * <p>No es {@code final} porque la transacción la pone un proxy de Spring.
 */
public class PublicarEventoDePruebaServicio implements PublicarEventoDePrueba {

    /** El tipo del evento de prueba. */
    public static final String TIPO = "generacion.EventoDePrueba";

    /** Contenido ficticio del evento de prueba (data-model.md). */
    static final Map<String, Object> CONTENIDO = Map.of("nota", "evento de prueba B00-H05");

    private final RepositorioEventosDePrueba repositorio;
    private final PublicadorDeEventos publicador;
    private final Clock reloj;

    /**
     * @param repositorio los eventos de prueba guardados
     * @param publicador publica en la transacción en curso
     * @param reloj de dónde sale el momento
     */
    public PublicarEventoDePruebaServicio(
            RepositorioEventosDePrueba repositorio, PublicadorDeEventos publicador, Clock reloj) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public UUID publicar() {
        UUID idEvento = UUID.randomUUID();
        Instant momento = reloj.instant();
        repositorio.guardar(idEvento, momento);
        publicador.publicar(new EventoSaliente(idEvento, TIPO, "usuarios", momento, CONTENIDO));
        return idEvento;
    }
}
