package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.puertos.entrada.CorreoRepetido;
import ec.mdd.docentes.puertos.entrada.DocenteRepetido;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Docentes en el catálogo (ADR-016): la identificación y el correo repetidos
 * (F03-H04). Sus textos son los de {@code contratos/comun/errores.md}; una prueba lo comprueba.
 */
@Component
class ErroresDocentes implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        DocenteRepetido.class,
                        "DOCENTES_DOCENTE_REPETIDO",
                        409,
                        "Docente repetido",
                        "Ya existe un docente con esa identificación. Búscalo y corrígelo."),
                EntradaCatalogo.de(
                        CorreoRepetido.class,
                        "DOCENTES_CORREO_REPETIDO",
                        409,
                        "Correo repetido",
                        "Otro docente ya usa ese correo institucional. Revisa el correo."));
    }
}
