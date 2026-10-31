package ec.mdd.roles.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.CatalogoDeErrores;
import ec.mdd.plataforma.errores.nucleo.CodigosComunes;
import ec.mdd.plataforma.errores.nucleo.EntradaCatalogo;
import ec.mdd.roles.aplicacion.EventoNoReconocido;
import ec.mdd.roles.dominio.NombreRepetido;
import ec.mdd.roles.dominio.PeriodoCerrado;
import ec.mdd.roles.dominio.RolAsignado;
import ec.mdd.roles.dominio.RolPredefinido;
import ec.mdd.roles.dominio.SinAdministrador;
import ec.mdd.roles.puertos.entrada.CarreraSinRol;
import ec.mdd.roles.puertos.entrada.UsuarioSinCarrera;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los errores de Roles en el catálogo (ADR-016): las cuatro reglas de la administración de roles.
 * Sus textos son los de {@code contratos/comun/errores.md}; una prueba lo comprueba.
 */
@Component
class ErroresRoles implements CatalogoDeErrores {

    @Override
    public List<EntradaCatalogo> entradas() {
        return List.of(
                EntradaCatalogo.de(
                        NombreRepetido.class,
                        "ROLES_NOMBRE_REPETIDO",
                        409,
                        "Nombre repetido",
                        "Ya existe un rol con ese nombre, o con uno que solo cambia en mayúsculas,"
                                + " tildes o signos. Elige otro."),
                EntradaCatalogo.de(
                        RolPredefinido.class,
                        "ROLES_ROL_PREDEFINIDO",
                        409,
                        "Rol de la matriz D-16",
                        "Los roles de la matriz D-16 no se pueden cambiar ni borrar. Crea un rol"
                                + " nuevo con los permisos que necesitas."),
                EntradaCatalogo.de(
                        RolAsignado.class,
                        "ROLES_ROL_ASIGNADO",
                        409,
                        "Rol asignado",
                        "El rol está asignado a usuarios. Quítaselo antes de borrarlo."),
                EntradaCatalogo.de(
                        SinAdministrador.class,
                        "ROLES_SIN_ADMINISTRADOR",
                        409,
                        "Sin administrador",
                        "El cambio dejaría al sistema sin nadie que administre los roles."),
                EntradaCatalogo.de(
                        PeriodoCerrado.class,
                        "ROLES_PERIODO_CERRADO",
                        409,
                        "Periodo cerrado",
                        "El periodo está cerrado y ya no admite cambios."),
                EntradaCatalogo.de(
                        CarreraSinRol.class,
                        "ROLES_CARRERA_SIN_ROL",
                        409,
                        "Usuario sin rol de coordinador",
                        "Solo un usuario con el rol Coordinador de carrera puede tener carrera."
                                + " Dale ese rol primero."),
                EntradaCatalogo.de(
                        UsuarioSinCarrera.class,
                        "ROLES_USUARIO_SIN_CARRERA",
                        404,
                        "Usuario sin carrera",
                        "Este usuario no tiene una carrera indicada."),
                new EntradaCatalogo(EventoNoReconocido.class, CodigosComunes.EVENTO_NO_RECONOCIDO));
    }
}
