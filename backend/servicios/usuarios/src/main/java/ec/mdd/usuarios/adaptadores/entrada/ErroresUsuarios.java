package ec.mdd.usuarios.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.usuarios.aplicacion.CredencialesInvalidas;
import ec.mdd.usuarios.aplicacion.EventoNoReconocido;
import java.util.List;
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
                // Código común del receptor de eventos (ADR-018), con su definición de la
                // plataforma.
                new EntradaCatalogo(EventoNoReconocido.class, CodigosComunes.EVENTO_NO_RECONOCIDO));
    }
}
