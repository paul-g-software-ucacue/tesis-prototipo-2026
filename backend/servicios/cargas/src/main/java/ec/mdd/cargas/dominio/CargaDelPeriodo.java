package ec.mdd.cargas.dominio;

import java.util.Optional;

/**
 * Una de las 10 cargas con su estado en el periodo del tablero de preparación (F02-H02).
 *
 * @param numero su orden en la ERS §8.3
 * @param codigo su código
 * @param nombre su nombre
 * @param obligatoriedad si es obligatoria, del diccionario
 * @param estado si tiene datos en el periodo, «sin proyectos», «con errores» o pendiente
 * @param motivo solo en {@link EstadoDeLaCarga#CON_ERRORES}, por qué se rechazó su último intento
 */
public record CargaDelPeriodo(
        int numero,
        String codigo,
        String nombre,
        Obligatoriedad obligatoriedad,
        EstadoDeLaCarga estado,
        Optional<MotivoDeRechazo> motivo) {

    /** Si su estado impide crear el distributivo: obligatoria y ni cargada ni «sin proyectos». */
    boolean falta() {
        return obligatoriedad != Obligatoriedad.RECOMENDADA
                && estado != EstadoDeLaCarga.CARGADA
                && estado != EstadoDeLaCarga.SIN_PROYECTOS;
    }
}
