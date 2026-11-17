package ec.mdd.notificaciones.adaptadores.entrada;

import ec.mdd.notificaciones.adaptadores.entrada.api.Bandeja;
import ec.mdd.notificaciones.adaptadores.entrada.api.BandejaApi;
import ec.mdd.notificaciones.adaptadores.entrada.api.Notificacion;
import ec.mdd.notificaciones.adaptadores.entrada.api.SinLeer;
import ec.mdd.notificaciones.puertos.entrada.BandejaDeUsuario;
import ec.mdd.notificaciones.puertos.entrada.ConsultarBandeja;
import ec.mdd.notificaciones.puertos.entrada.MarcarLeida;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * La bandeja del usuario de la sesión ({@code contratos/notificaciones}, F16-H01, US2).
 *
 * <p>Sin {@code @PreAuthorize}: basta con la sesión, que exige la seguridad común para {@code
 * /api/**}. El destinatario siempre es el usuario del token, nunca un dato de la petición, así que
 * nadie ve ni marca la bandeja de otro (D-108, principio IX).
 */
@RestController
class BandejaControlador implements BandejaApi {

    private static final int LIMITE_POR_DEFECTO = 50;

    private final ConsultarBandeja consultar;
    private final MarcarLeida marcar;

    BandejaControlador(ConsultarBandeja consultar, MarcarLeida marcar) {
        this.consultar = consultar;
        this.marcar = marcar;
    }

    @Override
    public ResponseEntity<Bandeja> consultarBandeja(Integer limite) {
        BandejaDeUsuario bandeja =
                consultar.bandeja(usuario(), limite == null ? LIMITE_POR_DEFECTO : limite);
        return ResponseEntity.ok(
                new Bandeja()
                        .notificaciones(
                                bandeja.notificaciones().stream()
                                        .map(BandejaControlador::modelo)
                                        .toList())
                        .sinLeer(bandeja.sinLeer()));
    }

    @Override
    public ResponseEntity<SinLeer> consultarSinLeer() {
        return ResponseEntity.ok(new SinLeer().sinLeer(consultar.sinLeer(usuario())));
    }

    @Override
    public ResponseEntity<Notificacion> marcarLeida(UUID id) {
        return ResponseEntity.ok(modelo(marcar.marcar(id, usuario())));
    }

    private static String usuario() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static Notificacion modelo(ec.mdd.notificaciones.dominio.Notificacion notificacion) {
        Notificacion modelo =
                new Notificacion()
                        .id(notificacion.id())
                        .periodo(notificacion.periodo())
                        .titulo(notificacion.titulo())
                        .detalle(notificacion.detalle())
                        .enlace(notificacion.enlace())
                        .autor(notificacion.autor())
                        .autorNombre(notificacion.autorNombre())
                        .momento(notificacion.momento().atOffset(ZoneOffset.UTC))
                        .leida(notificacion.leida());
        notificacion.carrera().ifPresent(modelo::carrera);
        notificacion.leidaEn().ifPresent(leida -> modelo.leidaEn(leida.atOffset(ZoneOffset.UTC)));
        return modelo;
    }
}
