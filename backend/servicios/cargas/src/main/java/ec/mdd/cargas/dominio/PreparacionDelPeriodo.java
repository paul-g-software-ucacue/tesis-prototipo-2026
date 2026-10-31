package ec.mdd.cargas.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * El tablero de preparación de un periodo (glosario «Tablero de preparación», RF-56, F02-H02): el
 * estado de cada carga en el periodo, lo que falta para crear el distributivo y si está listo. Se
 * calcula en cada consulta; no se guarda. La obligatoriedad sale del diccionario de datos.
 *
 * @param periodo el periodo; vacío si no hay periodos
 * @param abierto si tiene su versión de normativa (F02-H01)
 * @param cerrado si está cerrado
 * @param cargas las 10 cargas en su orden; ninguna si no hay periodos
 * @param faltan lo que falta, las cargas en su orden y después el periodo
 */
public record PreparacionDelPeriodo(
        Optional<String> periodo,
        boolean abierto,
        boolean cerrado,
        List<CargaDelPeriodo> cargas,
        List<LoQueFalta> faltan) {

    /** Copia las listas. */
    public PreparacionDelPeriodo {
        cargas = List.copyOf(cargas);
        faltan = List.copyOf(faltan);
    }

    /** Si se puede crear el distributivo del periodo: no falta nada. */
    public boolean listo() {
        return faltan.isEmpty();
    }

    /** El tablero cuando no hay ningún periodo en los ciclos académicos. */
    public static PreparacionDelPeriodo sinPeriodos() {
        return new PreparacionDelPeriodo(
                Optional.empty(),
                false,
                false,
                List.of(),
                List.of(LoQueFalta.del(TipoDeLoQueFalta.SIN_PERIODOS)));
    }

    /**
     * El tablero de un periodo.
     *
     * @param diccionario las 10 cargas, con su obligatoriedad
     * @param periodo el código del periodo
     * @param abierto si tiene su versión de normativa
     * @param cerrado si está cerrado
     * @param tieneDatosEnElPeriodo si la carga con ese código tiene datos en el periodo
     * @param sinProyectos si el periodo está confirmado «sin proyectos»
     * @param ultimoRechazo el motivo del último intento de confirmar la carga, si fue rechazado
     */
    public static PreparacionDelPeriodo de(
            DiccionarioDeDatos diccionario,
            String periodo,
            boolean abierto,
            boolean cerrado,
            Predicate<String> tieneDatosEnElPeriodo,
            boolean sinProyectos,
            Function<String, Optional<MotivoDeRechazo>> ultimoRechazo) {
        List<CargaDelPeriodo> cargas =
                diccionario.cargas().stream()
                        .map(
                                carga ->
                                        cargaDelPeriodo(
                                                carga,
                                                tieneDatosEnElPeriodo.test(carga.codigo()),
                                                sinProyectos,
                                                ultimoRechazo.apply(carga.codigo())))
                        .toList();
        List<LoQueFalta> faltan = new ArrayList<>();
        cargas.stream()
                .filter(CargaDelPeriodo::falta)
                .map(LoQueFalta::laCarga)
                .forEach(faltan::add);
        if (!abierto) {
            faltan.add(LoQueFalta.del(TipoDeLoQueFalta.PERIODO_SIN_ABRIR));
        }
        if (cerrado) {
            faltan.add(LoQueFalta.del(TipoDeLoQueFalta.PERIODO_CERRADO));
        }
        return new PreparacionDelPeriodo(Optional.of(periodo), abierto, cerrado, cargas, faltan);
    }

    private static CargaDelPeriodo cargaDelPeriodo(
            Carga carga,
            boolean tieneDatos,
            boolean sinProyectos,
            Optional<MotivoDeRechazo> rechazo) {
        boolean admiteSinProyectos =
                carga.obligatoriedad() == Obligatoriedad.OBLIGATORIA_SALVO_SIN_PROYECTOS;
        EstadoDeLaCarga estado;
        Optional<MotivoDeRechazo> motivo = Optional.empty();
        if (tieneDatos) {
            estado = EstadoDeLaCarga.CARGADA;
        } else if (admiteSinProyectos && sinProyectos) {
            estado = EstadoDeLaCarga.SIN_PROYECTOS;
        } else if (rechazo.isPresent()) {
            estado = EstadoDeLaCarga.CON_ERRORES;
            motivo = rechazo;
        } else {
            estado = EstadoDeLaCarga.PENDIENTE;
        }
        return new CargaDelPeriodo(
                carga.numero(),
                carga.codigo(),
                carga.nombre(),
                carga.obligatoriedad(),
                estado,
                motivo);
    }
}
