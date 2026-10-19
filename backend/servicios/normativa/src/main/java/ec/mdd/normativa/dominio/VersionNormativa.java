package ec.mdd.normativa.dominio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Versión de normativa: el conjunto numerado de parámetros de RN-01 a RN-26 que rige un periodo
 * (glosario §7.3, RF-19, modelo de dominio §5.1). No cambia una vez asociada a un periodo.
 *
 * <p>En F04-H01 solo se reconstruye desde la base, donde la versión 1 llega por migración con la
 * Política SENESCYT 2024; crear una versión, y su evento {@code VersionNormativaCreada}, es de
 * F04-H02.
 *
 * @param numero número de la versión, desde 1
 * @param codigo código de su origen, por ejemplo {@code SENESCYT-2024-03}
 * @param origen de dónde sale, por ejemplo «Política SENESCYT 2024»
 * @param vigenteDesde desde cuándo rige la norma
 * @param tipoIst el tipo de IST de la institución (Tipo 2 en el prototipo)
 * @param tabla la tabla de carga horaria
 * @param reglas las reglas con sus parámetros, sin repetir, ordenadas por identificador
 */
public record VersionNormativa(
        int numero,
        String codigo,
        String origen,
        LocalDate vigenteDesde,
        TipoIst tipoIst,
        TablaCargaHoraria tabla,
        List<Regla> reglas) {

    /** Comprueba las invariantes y ordena las reglas. */
    public VersionNormativa {
        if (numero < 1) {
            throw new IllegalArgumentException("El número de versión empieza en 1: " + numero);
        }
        if (codigo == null || codigo.isBlank() || origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("La versión necesita código y origen");
        }
        Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        Objects.requireNonNull(tipoIst, "tipoIst");
        Objects.requireNonNull(tabla, "tabla");
        Set<String> identificadores = new HashSet<>();
        for (Regla regla : Objects.requireNonNull(reglas, "reglas")) {
            if (!identificadores.add(regla.identificador())) {
                throw new IllegalArgumentException(
                        "La regla "
                                + regla.identificador()
                                + " está repetida en la versión "
                                + numero);
            }
        }
        reglas = reglas.stream().sorted(Comparator.comparing(Regla::identificador)).toList();
    }

    /** La regla con ese identificador, si la versión la tiene. */
    public Optional<Regla> regla(String identificador) {
        return reglas.stream()
                .filter(regla -> regla.identificador().equals(identificador))
                .findFirst();
    }
}
