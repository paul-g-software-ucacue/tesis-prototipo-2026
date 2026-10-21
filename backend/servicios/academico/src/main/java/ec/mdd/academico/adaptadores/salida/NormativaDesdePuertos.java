package ec.mdd.academico.adaptadores.salida;

import ec.mdd.academico.puertos.entrada.PeriodoConOtraVersion;
import ec.mdd.academico.puertos.entrada.VersionInexistente;
import ec.mdd.academico.puertos.salida.NormativaDelPeriodo;
import ec.mdd.normativa.puertos.entrada.AsociarVersionAPeriodo;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import java.util.OptionalInt;

/**
 * La versión de normativa de cada periodo, pedida a Normativa por su interfaz publicada dentro del
 * proceso {@code datos} (principio V, research §4), como Usuarios con Roles. Solo usa {@code
 * ec.mdd.normativa.puertos.entrada}; {@code ModulosTest} lo comprueba.
 *
 * <p>Traduce los errores de Normativa a los de Académico, sin reenviarlos (principio XIII). Las
 * excepciones de Normativa van con su paquete completo, porque Académico tiene las suyas con el
 * mismo nombre ({@code PeriodoSinVersion}). La llamada no pasa por el {@code @PreAuthorize} del
 * controlador de Normativa: la autorización es la de la operación de Académico (D-69).
 */
public final class NormativaDesdePuertos implements NormativaDelPeriodo {

    private final ConsultarNormativa consultar;
    private final AsociarVersionAPeriodo asociar;

    /** Recibe los dos puertos publicados de Normativa. */
    public NormativaDesdePuertos(ConsultarNormativa consultar, AsociarVersionAPeriodo asociar) {
        this.consultar = consultar;
        this.asociar = asociar;
    }

    @Override
    public OptionalInt versionDe(String codigo) {
        try {
            return OptionalInt.of(consultar.periodo(codigo).version());
        } catch (ec.mdd.normativa.puertos.entrada.PeriodoSinVersion sinVersion) {
            return OptionalInt.empty();
        }
    }

    @Override
    public void asociar(String codigo, int version, String quien) {
        try {
            asociar.asociar(codigo, version, quien);
        } catch (ec.mdd.normativa.puertos.entrada.PeriodoConVersion conVersion) {
            // Normativa no dice cuál tiene; ya está confirmada, así que se puede leer.
            throw new PeriodoConOtraVersion(codigo, versionDe(codigo).orElseThrow());
        } catch (ec.mdd.normativa.puertos.entrada.VersionNoEncontrada noEncontrada) {
            throw new VersionInexistente(version);
        }
    }
}
