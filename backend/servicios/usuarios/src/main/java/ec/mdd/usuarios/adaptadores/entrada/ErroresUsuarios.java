package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.DefinicionDeCodigo;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.usuarios.aplicacion.CredencialesInvalidas;
import ec.mdd.usuarios.aplicacion.EventoNoReconocido;
import ec.mdd.usuarios.aplicacion.InicioBloqueado;
import ec.mdd.usuarios.aplicacion.SesionNoRenovable;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Los errores de Usuarios en el catálogo (ADR-016). Sus textos son los de {@code
 * contratos/comun/errores.md}; una prueba lo comprueba.
 */
@Component
class ErroresUsuarios implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        CredencialesInvalidas.class,
                        "USUARIOS_CREDENCIALES_INVALIDAS",
                        401,
                        "Credenciales inválidas",
                        "El usuario o la contraseña no son correctos."),
                EntradaCatalogo.conEncabezados(
                        InicioBloqueado.class,
                        new DefinicionDeCodigo(
                                "USUARIOS_INICIO_BLOQUEADO",
                                429,
                                "Demasiados intentos",
                                "Espera unos minutos y vuelve a intentarlo."),
                        error -> Map.of("Retry-After", String.valueOf(error.segundosRestantes()))),
                // La renovación rechazada responde igual que un token inválido (ADR-020).
                EntradaCatalogo.conEncabezados(
                        SesionNoRenovable.class,
                        CodigosComunes.SEGURIDAD_NO_AUTENTICADO,
                        error -> Map.of("WWW-Authenticate", "Bearer")),
                // Código común del receptor de eventos (ADR-018), con su definición de la
                // plataforma.
                new EntradaCatalogo(EventoNoReconocido.class, CodigosComunes.EVENTO_NO_RECONOCIDO));
    }
}
