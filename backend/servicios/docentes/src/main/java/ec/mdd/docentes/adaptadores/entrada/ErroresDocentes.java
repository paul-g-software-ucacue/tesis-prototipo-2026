package ec.mdd.docentes.adaptadores.entrada;

import ec.mdd.docentes.dominio.DocenteNoHabilitado;
import ec.mdd.docentes.puertos.entrada.CargoRepetido;
import ec.mdd.docentes.puertos.entrada.CoordinadorVigente;
import ec.mdd.docentes.puertos.entrada.CorreoRepetido;
import ec.mdd.docentes.puertos.entrada.DocenteRepetido;
import ec.mdd.docentes.puertos.entrada.ProyectoRepetido;
import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Docentes en el catálogo (ADR-016): la identificación y el correo repetidos
 * (F03-H04); el cargo repetido, el segundo coordinador vigente, el docente no habilitado y el
 * proyecto repetido (F03-H05). Sus textos son los de {@code contratos/comun/errores.md}, con sus 8
 * columnas (D-72); una prueba lo comprueba.
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
                        "Otro docente ya usa ese correo institucional. Revisa el correo."),
                EntradaCatalogo.de(
                        CargoRepetido.class,
                        "DOCENTES_CARGO_REPETIDO",
                        409,
                        "Cargo repetido",
                        "Ese docente ya tiene ese cargo con esa fecha de designación. Búscalo y"
                                + " corrígelo."),
                EntradaCatalogo.de(
                        CoordinadorVigente.class,
                        "DOCENTES_COORDINADOR_VIGENTE",
                        409,
                        "Coordinador vigente",
                        "La carrera ya tiene un coordinador vigente. Finaliza su cargo antes de"
                                + " registrar otro."),
                EntradaCatalogo.de(
                        DocenteNoHabilitado.class,
                        "DOCENTES_DOCENTE_NO_HABILITADO",
                        409,
                        "Docente no habilitado",
                        "Un cargo solo se da a un docente activo a tiempo completo. Revisa su"
                                + " estado laboral y su dedicación."),
                EntradaCatalogo.de(
                        ProyectoRepetido.class,
                        "DOCENTES_PROYECTO_REPETIDO",
                        409,
                        "Proyecto repetido",
                        "Ya existe un proyecto con ese código. Búscalo y corrígelo."));
    }
}
