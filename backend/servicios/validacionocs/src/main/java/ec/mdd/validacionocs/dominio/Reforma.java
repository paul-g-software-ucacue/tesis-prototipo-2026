package ec.mdd.validacionocs.dominio;

import java.util.Objects;

/**
 * La reforma de un distributivo publicado (F18-H02, RF-40, D-113): la versión publicada que se
 * reforma y por qué. La versión nueva va en la transición. La publicada no se anula: sigue siendo
 * la vigente hasta que se publique la de la reforma.
 *
 * @param versionPublicada la versión publicada que se reforma
 * @param motivo por qué hay que cambiarla, de 10 a 500 caracteres
 */
public record Reforma(int versionPublicada, Observacion motivo) {

    /** Exige una versión desde 1 y el motivo. */
    public Reforma {
        if (versionPublicada < 1) {
            throw new IllegalArgumentException("La versión empieza en 1");
        }
        Objects.requireNonNull(motivo, "motivo");
    }
}
