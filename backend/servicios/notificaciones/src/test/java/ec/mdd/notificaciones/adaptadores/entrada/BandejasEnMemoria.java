package ec.mdd.notificaciones.adaptadores.entrada;

import ec.mdd.notificaciones.dominio.Destinatario;
import ec.mdd.notificaciones.dominio.DestinoDelMensaje;
import ec.mdd.notificaciones.dominio.Notificacion;
import ec.mdd.notificaciones.puertos.entrada.EventoRecibido;
import ec.mdd.notificaciones.puertos.salida.Destinatarios;
import ec.mdd.notificaciones.puertos.salida.RepositorioEventosRecibidos;
import ec.mdd.notificaciones.puertos.salida.RepositorioNotificaciones;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Las bandejas, los eventos recibidos y los destinatarios en memoria, para las pruebas de contrato
 * (F16-H01). Los destinatarios: cada rol con sus usuarios fijos.
 */
final class BandejasEnMemoria
        implements RepositorioNotificaciones, RepositorioEventosRecibidos, Destinatarios {

    static final Map<String, List<String>> USUARIOS_POR_ROL =
            Map.of(
                    "VICERRECTOR", List.of("vicerrectorado.demo"),
                    "SECRETARIA_GENERAL", List.of("secretaria.demo"));

    final List<Notificacion> guardadas = new CopyOnWriteArrayList<>();
    final Set<UUID> recibidos = ConcurrentHashMap.newKeySet();

    void limpiar() {
        guardadas.clear();
        recibidos.clear();
    }

    @Override
    public boolean registrarSiEsNuevo(EventoRecibido evento) {
        return recibidos.add(evento.idEvento());
    }

    @Override
    public void guardar(List<Notificacion> notificaciones) {
        guardadas.addAll(notificaciones);
    }

    @Override
    public List<Notificacion> bandeja(String destinatario, int limite) {
        return guardadas.stream()
                .filter(una -> una.destinatario().equals(destinatario))
                .sorted(
                        Comparator.comparing(Notificacion::momento)
                                .reversed()
                                .thenComparing(Notificacion::id))
                .limit(limite)
                .toList();
    }

    @Override
    public int sinLeer(String destinatario) {
        return (int)
                guardadas.stream()
                        .filter(una -> una.destinatario().equals(destinatario) && !una.leida())
                        .count();
    }

    @Override
    public Optional<Notificacion> de(UUID id, String destinatario) {
        return guardadas.stream()
                .filter(una -> una.id().equals(id) && una.destinatario().equals(destinatario))
                .findFirst();
    }

    @Override
    public void marcarLeida(UUID id, Instant momento) {
        for (int i = 0; i < guardadas.size(); i++) {
            Notificacion una = guardadas.get(i);
            if (una.id().equals(id)) {
                guardadas.set(i, una.marcarLeida(momento));
            }
        }
    }

    @Override
    public List<Destinatario> de(DestinoDelMensaje destino, String periodo) {
        return USUARIOS_POR_ROL.getOrDefault(destino.rol(), List.of()).stream()
                .map(usuario -> new Destinatario(usuario, destino.carrera()))
                .toList();
    }

    @Override
    public Optional<String> nombre(String usuario) {
        return Optional.of("Nombre de " + usuario);
    }
}
