package ec.mdd.usuarios.aplicacion;

import ec.mdd.usuarios.dominio.EntradaBitacora;
import ec.mdd.usuarios.puertos.entrada.EventoRecibido;
import ec.mdd.usuarios.puertos.entrada.RegistrarEventoRecibido;
import ec.mdd.usuarios.puertos.salida.RepositorioBitacora;
import ec.mdd.usuarios.puertos.salida.RepositorioEventosRecibidos;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

/**
 * El receptor de la bitácora de Usuarios (F17-H01, ADR-018, ADR-024).
 *
 * <p>Admite los eventos {@code <servicio>.EntradaBitacora} de los servicios que registran cambios
 * (D-90), con el prefijo del tipo igual a su {@code origen}. Primero valida la entrada; después, en
 * una sola transacción, registra el {@code idEvento} y, solo si es nuevo, la entrada: si algo
 * falla, nada queda y el emisor lo vuelve a enviar. Una entrada repetida no se registra dos veces.
 *
 * <p>No es {@code final} porque la transacción la pone un proxy de Spring.
 */
public class RegistrarEventoRecibidoServicio implements RegistrarEventoRecibido {

    /** El nombre del evento de la bitácora, después del prefijo del servicio. */
    public static final String ENTRADA_BITACORA = "EntradaBitacora";

    private static final Set<String> SERVICIOS =
            Set.of("generacion", "academico", "docentes", "normativa", "roles", "validacionocs");

    private final RepositorioEventosRecibidos eventos;
    private final RepositorioBitacora bitacora;

    /**
     * @param eventos los eventos ya registrados
     * @param bitacora la bitácora
     */
    public RegistrarEventoRecibidoServicio(
            RepositorioEventosRecibidos eventos, RepositorioBitacora bitacora) {
        this.eventos = eventos;
        this.bitacora = bitacora;
    }

    /**
     * @throws EventoNoReconocido si el tipo no es de la bitácora o no coincide con el origen
     * @throws ec.mdd.usuarios.dominio.EntradaBitacoraInvalida si el contenido no es una entrada
     *     válida
     */
    @Override
    @Transactional
    public void registrar(EventoRecibido evento) {
        String servicio = servicioDe(evento);
        EntradaBitacora entrada =
                EntradaBitacora.de(evento.idEvento(), servicio, evento.contenido());
        if (eventos.registrarSiEsNuevo(evento)) {
            bitacora.registrar(entrada);
        }
    }

    private static String servicioDe(EventoRecibido evento) {
        String tipo = evento.tipo();
        int punto = tipo == null ? -1 : tipo.indexOf('.');
        if (punto < 0) {
            throw new EventoNoReconocido(String.valueOf(tipo));
        }
        String servicio = tipo.substring(0, punto);
        if (!SERVICIOS.contains(servicio)
                || !ENTRADA_BITACORA.equals(tipo.substring(punto + 1))
                || !servicio.equals(evento.origen())) {
            throw new EventoNoReconocido(tipo);
        }
        return servicio;
    }
}
