package ec.mdd.generacion.dominio;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Las inconsistencias de la versión vigente de un periodo, abiertas y justificadas (RF-29). Se
 * calcula al abrirlo: no se guarda.
 *
 * @param periodo el código del periodo
 * @param modificable si admite justificaciones
 * @param version el id de la versión vigente; vacío sin distributivo
 * @param numero el número de la versión; 0 sin distributivo
 * @param entradas primero las abiertas, y entre ellas las duras
 */
public record PanelDeInconsistencias(
        String periodo,
        boolean modificable,
        Optional<UUID> version,
        int numero,
        List<EntradaDelPanel> entradas) {

    private static final Comparator<EntradaDelPanel> ORDEN =
            Comparator.comparing((EntradaDelPanel una) -> !una.abierta())
                    .thenComparing(una -> una.inconsistencia().severidad())
                    .thenComparing(una -> una.inconsistencia().tipo())
                    .thenComparing(una -> una.inconsistencia().objeto().nombre());

    /** Copia la lista. */
    public PanelDeInconsistencias {
        entradas = List.copyOf(entradas);
    }

    /** Un periodo sin distributivo: no hay nada que evaluar. */
    public static PanelDeInconsistencias sinDistributivo(String periodo, boolean modificable) {
        return new PanelDeInconsistencias(periodo, modificable, Optional.empty(), 0, List.of());
    }

    /**
     * Arma el panel con lo que el motor halló, las justificaciones vigentes y las excepciones de
     * RN-19, que se justifican en su propuesta. Una justificación sin inconsistencia no se muestra,
     * y en una dura no cuenta.
     *
     * @param periodo el código del periodo
     * @param modificable si admite justificaciones
     * @param version la versión vigente
     * @param halladas lo que el motor halló
     * @param justificaciones las vigentes de la versión
     * @param oferta las asignaturas-paralelo del periodo
     * @return el panel, primero las abiertas y entre ellas las duras
     */
    public static PanelDeInconsistencias armar(
            String periodo,
            boolean modificable,
            VersionDistributivo version,
            List<InconsistenciaGlobal> halladas,
            List<JustificacionDeInconsistencia> justificaciones,
            List<AsignaturaParalelo> oferta) {
        Stream<EntradaDelPanel> evaluadas =
                halladas.stream().map(una -> entrada(una, justificaciones));
        Stream<EntradaDelPanel> excepciones =
                oferta.stream()
                        .filter(una -> !una.retirada())
                        .flatMap(una -> excepcion(una, version).stream());
        return new PanelDeInconsistencias(
                periodo,
                modificable,
                Optional.of(version.id()),
                version.numero(),
                Stream.concat(evaluadas, excepciones).sorted(ORDEN).toList());
    }

    private static EntradaDelPanel entrada(
            InconsistenciaGlobal una, List<JustificacionDeInconsistencia> justificaciones) {
        Optional<JustificacionDeInconsistencia> suya =
                una.severidad() == Severidad.DURA
                        ? Optional.empty()
                        : justificaciones.stream()
                                .filter(
                                        otra ->
                                                otra.tipo() == una.tipo()
                                                        && otra.objeto()
                                                                .equals(una.objeto().clave()))
                                .findFirst();
        return new EntradaDelPanel(una, suya);
    }

    private static Optional<EntradaDelPanel> excepcion(
            AsignaturaParalelo una, VersionDistributivo version) {
        return version.propuestaDe(una.asignatura().codigo(), una.paralelo())
                .filter(propuesta -> propuesta.inconsistenciaJustificada().isPresent())
                .map(
                        propuesta -> {
                            ObjetoAfectado objeto = ObjetoAfectado.de(una);
                            return new EntradaDelPanel(
                                    new InconsistenciaGlobal(
                                            TipoDeInconsistencia.EXCEPCION_DE_AFINIDAD,
                                            Severidad.BLANDA,
                                            objeto,
                                            DatosDeLaInconsistencia.lista(
                                                    List.of(propuesta.nombreDocente()))),
                                    Optional.of(
                                            new JustificacionDeInconsistencia(
                                                    TipoDeInconsistencia.EXCEPCION_DE_AFINIDAD,
                                                    objeto.clave(),
                                                    propuesta.justificacion().texto(),
                                                    propuesta.propuestaPor(),
                                                    propuesta.propuestaEn())));
                        });
    }

    /** Si el periodo tiene distributivo. */
    public boolean creado() {
        return version.isPresent();
    }

    /** Cuántas siguen abiertas con esa severidad. */
    public long abiertas(Severidad severidad) {
        return entradas.stream()
                .filter(EntradaDelPanel::abierta)
                .filter(una -> una.inconsistencia().severidad() == severidad)
                .count();
    }

    /** La entrada de ese tipo y ese objeto. */
    public Optional<EntradaDelPanel> entrada(TipoDeInconsistencia tipo, String objeto) {
        return entradas.stream()
                .filter(una -> una.inconsistencia().tipo() == tipo)
                .filter(una -> una.inconsistencia().objeto().clave().equals(objeto))
                .findFirst();
    }
}
