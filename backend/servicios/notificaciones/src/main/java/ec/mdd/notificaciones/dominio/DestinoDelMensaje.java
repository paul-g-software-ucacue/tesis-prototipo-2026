package ec.mdd.notificaciones.dominio;

import java.util.Objects;
import java.util.Optional;

/**
 * A quién toca un mensaje (D-108, D-110): un rol, o una persona por su usuario o por su
 * identificación. Notificaciones lo resuelve a cuentas activas, y nunca al autor.
 */
public sealed interface DestinoDelMensaje {

    /**
     * @return la carrera, solo en un destino por rol de una devolución
     */
    default Optional<String> carrera() {
        return Optional.empty();
    }

    /**
     * Los usuarios activos con el rol. RESPONSABLE_CONSOLIDACION son quienes lo tienen asignado y
     * el designado del periodo; con carrera, solo los coordinadores de esa carrera (D-108).
     *
     * @param rol uno de los diez roles de D-63
     * @param carrera la carrera, solo en una devolución
     */
    record PorRol(String rol, Optional<String> carrera) implements DestinoDelMensaje {

        /** Exige los datos. */
        public PorRol {
            Objects.requireNonNull(rol, "rol");
            Objects.requireNonNull(carrera, "carrera");
        }
    }

    /**
     * Una cuenta por su nombre de usuario: el firmante que se reconoce por su usuario (D-102).
     *
     * @param usuario el nombre de usuario
     */
    record PorUsuario(String usuario) implements DestinoDelMensaje {

        /** Exige el usuario. */
        public PorUsuario {
            Objects.requireNonNull(usuario, "usuario");
        }
    }

    /**
     * La cuenta con esa identificación, que es única: el docente de un cargo firmante (D-102). La
     * identificación es un dato personal: no se guarda en la notificación ni se repite en un error.
     *
     * @param identificacion la cédula o el pasaporte, con el formato de Docentes
     */
    record PorIdentificacion(String identificacion) implements DestinoDelMensaje {

        /** Exige la identificación. */
        public PorIdentificacion {
            Objects.requireNonNull(identificacion, "identificacion");
        }

        // Sin la identificación: un registro o un error no la muestran.
        @Override
        public String toString() {
            return "PorIdentificacion[…]";
        }
    }
}
