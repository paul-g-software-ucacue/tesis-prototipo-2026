package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Los candidatos de una asignatura-paralelo en un periodo (RF-25, RN-19, MD-02). Se calcula en cada
 * consulta; no se guarda.
 *
 * @param periodo el código del periodo
 * @param paralelo la asignatura-paralelo
 * @param version el número de la versión de normativa que rige el periodo
 * @param minimoAfinidad el mínimo de esa versión
 * @param candidatos los candidatos, en orden
 * @param fueraPorElMinimo los docentes activos que no alcanzan el mínimo; 0 en tronco común
 */
public record Sugerencia(
        String periodo,
        AsignaturaParalelo paralelo,
        int version,
        BigDecimal minimoAfinidad,
        List<Candidato> candidatos,
        int fueraPorElMinimo) {

    /** Exige los datos y copia los candidatos. */
    public Sugerencia {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(paralelo, "paralelo");
        Objects.requireNonNull(minimoAfinidad, "minimoAfinidad");
        candidatos = List.copyOf(candidatos);
    }

    /**
     * Arma la sugerencia. Primero aplica el mínimo, salvo en tronco común; después ordena: quienes
     * tienen horas para el paralelo van antes que los demás.
     *
     * @param periodo el código del periodo
     * @param paralelo la asignatura-paralelo
     * @param pesos los pesos y el mínimo de la versión que rige el periodo
     * @param docentes los docentes activos
     * @param horas las horas asignadas por identificación; quien no está tiene 0 h
     * @return la sugerencia
     */
    public static Sugerencia armar(
            String periodo,
            AsignaturaParalelo paralelo,
            PesosDelPeriodo pesos,
            List<DocenteActivo> docentes,
            Map<String, HorasDelDocente> horas) {
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
                                                        pesos.pesos())))
                        .toList();
        List<Candidato> candidatos =
                todos.stream()
                        .filter(
                                candidato ->
                                        paralelo.troncoComun()
                                                || alcanza(candidato, pesos.minimoAfinidad()))
                        .sorted(orden(paralelo.horasSemanales()))
                        .toList();
        return new Sugerencia(
                periodo,
                paralelo,
                pesos.version(),
                pesos.minimoAfinidad(),
                candidatos,
                todos.size() - candidatos.size());
    }

    private static boolean alcanza(Candidato candidato, BigDecimal minimo) {
        return candidato.puntaje().total().compareTo(minimo) >= 0;
    }

    /** Primero, quienes tienen horas; en cada grupo, puntaje, horas disponibles y nombre. */
    private static Comparator<Candidato> orden(BigDecimal horasDelParalelo) {
        Collator nombres = Collator.getInstance(Locale.of("es"));
        return Comparator.comparing(
                        (Candidato candidato) -> !candidato.tieneHorasPara(horasDelParalelo))
                .thenComparing(candidato -> candidato.puntaje().total(), Comparator.reverseOrder())
                .thenComparing(Candidato::horasDisponibles, Comparator.reverseOrder())
                .thenComparing(Candidato::nombre, nombres);
    }
}
