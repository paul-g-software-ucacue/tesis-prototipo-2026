package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Los candidatos de una asignatura-paralelo en un periodo (RF-25, RN-19, MD-02). Se calcula en cada
 * consulta; no se guarda.
 *
 * @param periodo el código del periodo
 * @param paralelo la asignatura-paralelo
 * @param version el número de la versión de normativa que rige el periodo
 * @param minimoAfinidad el mínimo de esa versión
 * @param candidatos los candidatos, en orden
 * @param bajoElMinimo los docentes activos que no alcanzan el mínimo, en el mismo orden; vacía en
 *     tronco común
 */
public record Sugerencia(
        String periodo,
        AsignaturaParalelo paralelo,
        int version,
        BigDecimal minimoAfinidad,
        List<Candidato> candidatos,
        List<Candidato> bajoElMinimo) {

    /** Exige los datos y copia las listas. */
    public Sugerencia {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(paralelo, "paralelo");
        Objects.requireNonNull(minimoAfinidad, "minimoAfinidad");
        candidatos = List.copyOf(candidatos);
        bajoElMinimo = List.copyOf(bajoElMinimo);
    }

    /** Cuántos docentes activos no alcanzan el mínimo. */
    public int fueraPorElMinimo() {
        return bajoElMinimo.size();
    }

    /**
     * Arma la sugerencia. Primero aplica el mínimo, salvo en tronco común; después ordena: quienes
     * se pueden proponer van antes que los demás (RF-28).
     *
     * @param periodo el código del periodo
     * @param paralelo la asignatura-paralelo
     * @param pesos los pesos y el mínimo de la versión que rige el periodo
     * @param docentes los docentes activos
     * @param horas las horas asignadas por identificación; quien no está tiene 0 h
     * @param reglas las cifras de la normativa
     * @param motivos por qué no se le puede proponer a cada docente; vacío si se puede
     * @return la sugerencia
     */
    public static Sugerencia armar(
            String periodo,
            AsignaturaParalelo paralelo,
            PesosDelPeriodo pesos,
            List<DocenteActivo> docentes,
            Map<String, HorasDelDocente> horas,
            ReglasDelPeriodo reglas,
            Function<DocenteActivo, Optional<ReglaDeAsignacion>> motivos) {
        CalculadoraAfinidad calculadora = new CalculadoraAfinidad();
        List<Candidato> todos =
                docentes.stream()
                        .map(
                                docente ->
                                        new Candidato(
                                                docente.identificacion(),
                                                docente.nombre(),
                                                docente.dedicacion(),
                                                docente.horasContratadas(),
                                                horas.getOrDefault(
                                                        docente.identificacion(),
                                                        HorasDelDocente.NINGUNA),
                                                calculadora.calcular(
                                                        docente.formacion(),
                                                        paralelo.asignatura(),
                                                        pesos.pesos()),
                                                reglas.limite(docente.horasContratadas()),
                                                motivos.apply(docente).orElse(null)))
                        .toList();
        Comparator<Candidato> orden = orden(paralelo.horasSemanales());
        return new Sugerencia(
                periodo,
                paralelo,
                pesos.version(),
                pesos.minimoAfinidad(),
                todos.stream()
                        .filter(
                                candidato ->
                                        paralelo.troncoComun()
                                                || alcanza(candidato, pesos.minimoAfinidad()))
                        .sorted(orden)
                        .toList(),
                todos.stream()
                        .filter(
                                candidato ->
                                        !paralelo.troncoComun()
                                                && !alcanza(candidato, pesos.minimoAfinidad()))
                        .sorted(orden)
                        .toList());
    }

    private static boolean alcanza(Candidato candidato, BigDecimal minimo) {
        return candidato.puntaje().total().compareTo(minimo) >= 0;
    }

    /** Primero, quienes se pueden proponer; en cada grupo, puntaje, disponibles y nombre. */
    private static Comparator<Candidato> orden(BigDecimal horasDelParalelo) {
        Collator nombres = Collator.getInstance(Locale.of("es"));
        return Comparator.comparing(
                        (Candidato candidato) ->
                                !candidato.sePuedeProponer()
                                        || !candidato.tieneHorasPara(horasDelParalelo))
                .thenComparing(candidato -> candidato.puntaje().total(), Comparator.reverseOrder())
                .thenComparing(Candidato::horasDisponibles, Comparator.reverseOrder())
                .thenComparing(Candidato::nombre, nombres);
    }
}
