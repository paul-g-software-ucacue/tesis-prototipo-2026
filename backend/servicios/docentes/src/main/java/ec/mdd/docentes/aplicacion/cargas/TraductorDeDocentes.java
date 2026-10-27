package ec.mdd.docentes.aplicacion.cargas;

import ec.mdd.docentes.dominio.DatoInvalido;
import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.TraductorDeCarga;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Lo común a los traductores de las cargas de Docentes (ADR-022): un {@link DatoInvalido} de un
 * caso de uso se rechaza con su campo y su motivo, como lo responden sus controladores (FR-005).
 *
 * @param <T> la entidad de Docentes de cada fila
 */
abstract class TraductorDeDocentes<T> implements TraductorDeCarga<T> {

    @Override
    public Optional<RechazoDelLote> rechazo(FilaDelLote fila, RuntimeException error) {
        if (error instanceof DatoInvalido invalido) {
            return Optional.of(
                    new RechazoDelLote(fila.fila(), invalido.campo(), invalido.motivo()));
        }
        return Optional.empty();
    }

    /** Un texto vacío como {@code null}. */
    static String nulo(String texto) {
        return texto == null || texto.isEmpty() ? null : texto;
    }

    /** Un valor opcional como texto canónico: vacío si no tiene. */
    static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    /** Un número como texto canónico: {@code 2.50} y {@code 2.5} son iguales. */
    static String numero(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString();
    }

    /** El texto canónico de un número del archivo. */
    static String numero(String texto) {
        return texto.isEmpty() ? "" : numero(new BigDecimal(texto));
    }

    /** Una fecha opcional del archivo. */
    static LocalDate fecha(String texto) {
        return texto.isEmpty() ? null : LocalDate.parse(texto);
    }

    /** Los elementos de una lista del archivo, separados por «;», sin espacios ni vacíos. */
    static List<String> elementos(String lista) {
        return Arrays.stream(lista.split(";"))
                .map(String::strip)
                .filter(elemento -> !elemento.isEmpty())
                .toList();
    }

    /** Las partes de un elemento, separadas por «|», sin espacios. */
    static String[] partes(String elemento) {
        return Arrays.stream(elemento.split("\\|", -1)).map(String::strip).toArray(String[]::new);
    }
}
