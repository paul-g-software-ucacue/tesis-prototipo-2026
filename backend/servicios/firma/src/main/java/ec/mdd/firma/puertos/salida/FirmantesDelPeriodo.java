package ec.mdd.firma.puertos.salida;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Quién ocupa cada rol de la cadena en el periodo (F13-H02, research §6; F14-H01, D-102): su
 * nombre, que se imprime, y la persona, que queda como firmante esperado. El Rector y el
 * Vicerrector académico salen de sus cargos vigentes en Datos, con la identificación del docente;
 * el Responsable de consolidación, de su designación en Roles, con su usuario. Llama a otros
 * procesos: no se usa dentro de una transacción (D-83).
 */
@FunctionalInterface
public interface FirmantesDelPeriodo {

    /**
     * @param periodo el código del periodo
     * @param roles los roles de la cadena
     * @return quién ocupa cada rol; un rol sin titular no está
     */
    Map<String, FirmanteDelPeriodo> firmantes(String periodo, Set<String> roles);

    /**
     * Quién ocupa un rol: su nombre y la persona, por su usuario o por su identificación.
     *
     * @param nombre el nombre que se imprime
     * @param usuario el usuario, para una designación
     * @param identificacion la identificación del docente, para un cargo
     */
    record FirmanteDelPeriodo(
            String nombre, Optional<String> usuario, Optional<String> identificacion) {

        /** Exige el nombre y las dos opciones. */
        public FirmanteDelPeriodo {
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(usuario, "usuario");
            Objects.requireNonNull(identificacion, "identificacion");
        }

        /** El designado de un periodo: se reconoce por su usuario. */
        public static FirmanteDelPeriodo porUsuario(String nombre, String usuario) {
            return new FirmanteDelPeriodo(nombre, Optional.of(usuario), Optional.empty());
        }

        /** El docente de un cargo: se reconoce por su identificación. */
        public static FirmanteDelPeriodo porIdentificacion(String nombre, String identificacion) {
            return new FirmanteDelPeriodo(nombre, Optional.empty(), Optional.of(identificacion));
        }
    }
}
