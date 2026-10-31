package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Calcula el puntaje de afinidad de un docente con su ficha y los pesos de la normativa del periodo
 * (RN-19, RN-24, MD-02, ADR-010). Es una función pura: no guarda nada ni depende del reloj.
 *
 * <ul>
 *   <li>Un título suma el peso del campo específico de su nivel si coincide ese campo, o el del
 *       campo amplio si solo coincide el amplio.
 *   <li>Una experiencia o un curso suman solo si coincide el campo específico.
 *   <li>Cada uno suma por separado, y solo los cursos tienen tope: el curso que lo alcanza lleva lo
 *       que cabe, y los que siguen no aparecen en el desglose.
 * </ul>
 *
 * <p>Las palabras clave, las asignaturas afines declaradas y el tronco común no cambian el puntaje
 * (F06-H01): el tronco común exime del mínimo de afinidad, no del cálculo.
 */
public final class CalculadoraAfinidad {

    /**
     * El puntaje en varias asignaturas.
     *
     * @return del mayor puntaje al menor y, a igual puntaje, por nombre
     */
    public List<PuntajeAfinidad> calcular(
            FormacionDelDocente formacion,
            List<AsignaturaOfertada> asignaturas,
            PesosAfinidad pesos) {
        Collator nombres = Collator.getInstance(Locale.of("es"));
        return asignaturas.stream()
                .map(asignatura -> calcular(formacion, asignatura, pesos))
                .sorted(
                        Comparator.comparing(PuntajeAfinidad::total)
                                .reversed()
                                .thenComparing(PuntajeAfinidad::nombre, nombres))
                .toList();
    }

    /** El puntaje en una asignatura, con su desglose. */
    public PuntajeAfinidad calcular(
            FormacionDelDocente formacion, AsignaturaOfertada asignatura, PesosAfinidad pesos) {
        List<AporteDeAfinidad> desglose = new ArrayList<>();
        tercerNivel(formacion.tercerNivel(), asignatura, pesos).ifPresent(desglose::add);
        formacion.cuartoNivel().stream()
                .flatMap(titulo -> cuartoNivel(titulo, asignatura, pesos).stream())
                .forEach(desglose::add);
        formacion.experiencia().stream()
                .filter(cargo -> cargo.campoEspecifico().equals(asignatura.campoEspecifico()))
                .flatMap(
                        cargo ->
                                aporte(
                                        TipoDeAporte.EXPERIENCIA,
                                        cargo.nombre(),
                                        CoincidenciaDeCampo.CAMPO_ESPECIFICO,
                                        pesos.experienciaRelacionada())
                                        .stream())
                .forEach(desglose::add);
        boolean topeAplicado = cursos(formacion.cursos(), asignatura, pesos, desglose);
        BigDecimal total =
                desglose.stream()
                        .map(AporteDeAfinidad::aporte)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PuntajeAfinidad(
                asignatura.codigo(), asignatura.nombre(), total, topeAplicado, desglose);
    }

    private static Optional<AporteDeAfinidad> tercerNivel(
            TituloDeTercerNivel titulo, AsignaturaOfertada asignatura, PesosAfinidad pesos) {
        if (titulo.campoEspecifico().equals(asignatura.campoEspecifico())) {
            return aporte(
                    TipoDeAporte.TERCER_NIVEL,
                    titulo.nombre(),
                    CoincidenciaDeCampo.CAMPO_ESPECIFICO,
                    pesos.tercerNivelCampoEspecifico());
        }
        if (titulo.campoAmplio().equals(asignatura.campoAmplio())) {
            return aporte(
                    TipoDeAporte.TERCER_NIVEL,
                    titulo.nombre(),
                    CoincidenciaDeCampo.CAMPO_AMPLIO,
                    pesos.tercerNivelCampoAmplio());
        }
        return Optional.empty();
    }

    private static Optional<AporteDeAfinidad> cuartoNivel(
            ElementoDeFormacion titulo, AsignaturaOfertada asignatura, PesosAfinidad pesos) {
        if (titulo.campoEspecifico().equals(asignatura.campoEspecifico())) {
            return aporte(
                    TipoDeAporte.CUARTO_NIVEL,
                    titulo.nombre(),
                    CoincidenciaDeCampo.CAMPO_ESPECIFICO,
                    pesos.cuartoNivelCampoEspecifico());
        }
        // El campo específico empieza por el amplio: la ficha solo guarda el específico.
        if (titulo.campoEspecifico().startsWith(asignatura.campoAmplio())) {
            return aporte(
                    TipoDeAporte.CUARTO_NIVEL,
                    titulo.nombre(),
                    CoincidenciaDeCampo.CAMPO_AMPLIO,
                    pesos.cuartoNivelCampoAmplio());
        }
        return Optional.empty();
    }

    /** Agrega los cursos que caben en el tope y dice si alguno pasó de él. */
    private static boolean cursos(
            List<ElementoDeFormacion> cursos,
            AsignaturaOfertada asignatura,
            PesosAfinidad pesos,
            List<AporteDeAfinidad> desglose) {
        BigDecimal disponible = pesos.topeCursos();
        boolean topeAplicado = false;
        for (ElementoDeFormacion curso : cursos) {
            if (curso.campoEspecifico().equals(asignatura.campoEspecifico())) {
                BigDecimal cabe = pesos.cursoRelacionado().min(disponible);
                topeAplicado |= cabe.compareTo(pesos.cursoRelacionado()) < 0;
                disponible = disponible.subtract(cabe);
                aporte(
                                TipoDeAporte.CURSO,
                                curso.nombre(),
                                CoincidenciaDeCampo.CAMPO_ESPECIFICO,
                                cabe)
                        .ifPresent(desglose::add);
            }
        }
        return topeAplicado;
    }

    // Un peso en 0 no aporta y no va al desglose.
    private static Optional<AporteDeAfinidad> aporte(
            TipoDeAporte tipo, String nombre, CoincidenciaDeCampo campo, BigDecimal peso) {
        return peso.signum() > 0
                ? Optional.of(new AporteDeAfinidad(tipo, nombre, campo, peso))
                : Optional.empty();
    }
}
