package ec.mdd.plataforma.errores;

import ec.mdd.plataforma.errores.nucleo.CarreraSinIndicar;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;

/** El coordinador sin carrera, con su código común (D-64). */
public final class ErroresDeAlcance implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                new EntradaCatalogo(CarreraSinIndicar.class, CodigosComunes.CARRERA_SIN_INDICAR));
    }
}
