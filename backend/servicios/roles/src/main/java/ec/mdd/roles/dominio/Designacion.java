package ec.mdd.roles.dominio;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * El Responsable de consolidación de un periodo académico: el usuario que el Vicerrector designa
 * para consolidar el distributivo de ese periodo y firmar «Elaborado por» (D-13, ADR-003). Es un
 * rol, no un cargo: no tiene horas.
 *
 * <p>Hay uno por periodo, y se reemplaza mientras el periodo no esté cerrado (F05-H03,
 * aclaraciones). Mientras el usuario tenga una designación en un periodo no cerrado, recibe los
 * permisos del rol {@value #ROL}.
 *
 * @param periodo el código del periodo
 * @param usuario el Responsable, normalizado
 * @param designadoPor quién lo designó
 * @param designadoEn cuándo
 */
public record Designacion(
        String periodo, String usuario, String designadoPor, Instant designadoEn) {

    /** El rol de la matriz D-16 que da la designación. */
    public static final String ROL = "RESPONSABLE_CONSOLIDACION";

    // Solo un docente se puede designar (aclaración 3); hoy la marca es su rol (F03-H04 traerá la
    // entidad docente).
    private static final String DOCENTE = "DOCENTE";

    /** Exige todos los datos. */
    public Designacion {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(designadoPor, "designadoPor");
        Objects.requireNonNull(designadoEn, "designadoEn");
    }

    /**
     * La designación de un usuario como Responsable de consolidación de un periodo.
     *
     * @param periodo el periodo, según la copia de Roles
     * @param usuario a quién se designa
     * @param quien quién designa
     * @param rolesDelUsuario los roles asignados al usuario designado
     * @param anterior la designación vigente del periodo, si tiene
     * @param ahora cuándo
     * @return la designación nueva; con el mismo usuario que la anterior, la anterior, sin cambios
     * @throws PeriodoCerrado si el periodo está cerrado
     * @throws DesignacionInvalida si el usuario no tiene el rol Docente o es quien designa
     */
    public static Designacion de(
            PeriodoConocido periodo,
            String usuario,
            String quien,
            Collection<String> rolesDelUsuario,
            Optional<Designacion> anterior,
            Instant ahora) {
        if (periodo.estado().cerrado()) {
            throw new PeriodoCerrado(periodo.codigo());
        }
        String designado = NombreDeUsuario.normalizar(usuario);
        String designador = NombreDeUsuario.normalizar(quien);
        if (designado.equals(designador)) {
            throw new DesignacionInvalida(
                    "usuario",
                    "No puedes designarte a ti mismo: la consolidación y la revisión van en manos"
                            + " distintas");
        }
        if (!rolesDelUsuario.contains(DOCENTE)) {
            throw new DesignacionInvalida(
                    "usuario", "Solo se puede designar a un usuario con el rol Docente");
        }
        if (anterior.isPresent() && anterior.get().usuario().equals(designado)) {
            return anterior.get();
        }
        return new Designacion(periodo.codigo(), designado, designador, ahora);
    }
}
