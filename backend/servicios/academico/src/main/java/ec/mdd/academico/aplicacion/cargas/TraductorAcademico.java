package ec.mdd.academico.aplicacion.cargas;

import ec.mdd.academico.dominio.DatoInvalido;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.TraductorDeCarga;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lo común a los traductores de las cargas de Académico (ADR-022): un {@link DatoInvalido} de un
 * caso de uso se rechaza con su campo y su motivo, como lo responden sus controladores (FR-005).
 *
 * @param <T> la entidad de Académico de cada fila
 */
abstract class TraductorAcademico<T> implements TraductorDeCarga<T> {

    @Override
    public Optional<RechazoDelLote> rechazo(FilaDelLote fila, RuntimeException error) {
        if (error instanceof DatoInvalido invalido) {
            return Optional.of(
                    new RechazoDelLote(fila.fila(), invalido.campo(), invalido.motivo()));
        }
        return Optional.empty();
    }

    /** Un texto vacío como {@code null}, como guardan Académico los opcionales. */
    static String nulo(String texto) {
        return texto == null || texto.isEmpty() ? null : texto;
    }

    /** Un texto opcional como texto canónico: vacío si no tiene. */
    static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    /** Un número como texto canónico, sin ceros de más: {@code 3.0} y {@code 3} son iguales. */
    static String numero(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }

    /** El texto canónico de un número escrito en el archivo; vacío si no tiene. */
    static String numero(String texto) {
        return texto.isEmpty() ? "" : numero(new BigDecimal(texto));
    }

    /** Un número decimal del archivo, o {@code null} si está vacío. */
    static BigDecimal decimal(String texto) {
        return texto.isEmpty() ? null : new BigDecimal(texto);
    }
}
