package ec.mdd.notificaciones.adaptadores.entrada;

import ec.mdd.notificaciones.aplicacion.EventoNoReconocido;
import ec.mdd.notificaciones.dominio.NotificacionInvalida;
import ec.mdd.notificaciones.dominio.NotificacionNoEncontrada;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Notificaciones en el catálogo (ADR-016, F16-H01). Sus textos son los de {@code
 * contratos/comun/errores.md}; una prueba lo comprueba.
 */
@Component
class ErroresNotificaciones implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                // Código común del receptor de eventos (ADR-018).
                new EntradaCatalogo(EventoNoReconocido.class, CodigosComunes.EVENTO_NO_RECONOCIDO),
                EntradaCatalogo.de(
                        NotificacionInvalida.class,
                        "NOTIFICACIONES_NOTIFICACION_INVALIDA",
                        400,
                        "Notificación no válida",
                        "La bandeja no admite esta notificación tal como llegó."),
                EntradaCatalogo.de(
                        NotificacionNoEncontrada.class,
                        "NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA",
                        404,
                        "Notificación no encontrada",
                        "Esta notificación no está en tu bandeja."));
    }
}
