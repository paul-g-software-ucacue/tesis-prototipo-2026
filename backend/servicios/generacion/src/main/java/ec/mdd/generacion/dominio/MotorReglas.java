package ec.mdd.generacion.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Evalúa una asignación al guardarla, comparando la carga del docente antes y después (RF-28). Una
 * regla se viola solo si el docente queda sobre el límite y además sube: así se puede llegar justo
 * al límite, y quien ya estaba excedido puede bajar o repetir, pero no subir. Es una función pura.
 *
 * <p>Las cifras son datos de la normativa; qué tipos de proyecto activan el tope de clase es parte
 * de la comprobación (principio VI).
 *
 * <p>También evalúa la versión completa, con las comprobaciones globales (RF-29).
 */
public final class MotorReglas {

    private static final Set<String> TIPOS_CON_TOPE = Set.of("INVESTIGACION", "VINCULACION");

    private MotorReglas() {}

    /**
     * Evalúa una asignación. Si viola las dos reglas, devuelve la de las horas.
     *
     * @param antes la carga del docente sin la asignación
     * @param despues su carga con la asignación
     * @param horasContratadas sus horas contratadas
     * @param conProyecto si tiene el tope de clase
     * @param reglas las cifras de la normativa
     * @return la regla violada; vacío si se puede guardar
     */
    public static Optional<Inconsistencia> evaluar(
            CargaHoraria antes,
            CargaHoraria despues,
            int horasContratadas,
            boolean conProyecto,
            ReglasDelPeriodo reglas) {
        BigDecimal limite = reglas.limite(horasContratadas);
        if (sePasaYSube(antes.asignadas(), despues.asignadas(), limite)) {
            return Optional.of(
                    new Inconsistencia(
                            ReglaDeAsignacion.RN_02,
                            Severidad.DURA,
                            queQueda(limite, antes.asignadas())));
        }
        BigDecimal tope = reglas.horasClaseConProyecto();
        if (conProyecto && sePasaYSube(antes.clase(), despues.clase(), tope)) {
            return Optional.of(
                    new Inconsistencia(
                            ReglaDeAsignacion.RN_13,
                            Severidad.DURA,
                            queQueda(tope, antes.clase())));
        }
        return Optional.empty();
    }

    /**
     * Evalúa RN-19: un docente bajo el mínimo, salvo en tronco común, con la severidad de la
     * normativa. Se evalúa después de las reglas de horas.
     *
     * @param puntaje su puntaje de afinidad
     * @param minimo el mínimo de la versión
     * @param troncoComun si la asignatura es de tronco común
     * @param reglas las cifras de la normativa
     * @return la regla, si no se cumple
     */
    public static Optional<Inconsistencia> evaluarAfinidad(
            BigDecimal puntaje, BigDecimal minimo, boolean troncoComun, ReglasDelPeriodo reglas) {
        if (troncoComun || puntaje.compareTo(minimo) >= 0) {
            return Optional.empty();
        }
        return Optional.of(
                new Inconsistencia(
                        ReglaDeAsignacion.RN_19, reglas.severidadAfinidad(), BigDecimal.ZERO));
    }

    /**
     * Evalúa la versión completa (RF-29). Es una función pura: las cifras y las severidades son
     * datos de la normativa, y un paralelo retirado no cuenta.
     *
     * @param datos la versión y lo que hace falta para evaluarla
     * @param reglas las severidades y las cifras de la normativa
     * @return lo que incumple, sin ningún orden en particular
     */
    public static List<InconsistenciaGlobal> evaluarVersion(
            DatosDelPeriodo datos, ReglasDelPanel reglas) {
        List<AsignaturaParalelo> vigentes =
                datos.oferta().stream().filter(una -> !una.retirada()).toList();
        List<InconsistenciaGlobal> halladas = new ArrayList<>();
        datos.docentes().forEach(docente -> evaluarDocente(docente, datos, reglas, halladas));
        vigentes.stream()
                .filter(una -> sinPropuesta(una, datos.version()))
                .map(una -> sinPropuesta(una, reglas))
                .forEach(halladas::add);
        evaluarCarreras(vigentes, datos.cargos(), reglas, halladas);
        evaluarGestores(datos, reglas).ifPresent(halladas::add);
        return List.copyOf(halladas);
    }

    private static void evaluarDocente(
            DocenteActivo docente,
            DatosDelPeriodo datos,
            ReglasDelPanel reglas,
            List<InconsistenciaGlobal> halladas) {
        CargaHoraria carga =
                CargaHoraria.calcular(
                        docente.identificacion(),
                        datos.version().propuestas(),
                        datos.version().actividades(),
                        datos.oferta());
        ObjetoAfectado objeto = ObjetoAfectado.de(docente);
        if (carga.clase().signum() == 0) {
            halladas.add(
                    global(
                            TipoDeInconsistencia.DOCENTE_SIN_CLASE,
                            reglas,
                            objeto,
                            DatosDeLaInconsistencia.ninguno()));
        }
        BigDecimal limite = reglas.limite(docente.horasContratadas());
        if (carga.asignadas().compareTo(limite) > 0) {
            halladas.add(
                    global(
                            TipoDeInconsistencia.SOBRECARGA,
                            reglas,
                            objeto,
                            DatosDeLaInconsistencia.rango(carga.asignadas(), null, limite)));
        }
        fueraDelMargen(carga, reglas)
                .map(
                        datosDeRn05 ->
                                global(
                                        TipoDeInconsistencia.PROPORCION,
                                        reglas,
                                        objeto,
                                        datosDeRn05))
                .ifPresent(halladas::add);
        if (carga.clase().signum() > 0) {
            List<String> faltan = sinConstar(docente, datos.version(), reglas);
            if (!faltan.isEmpty()) {
                halladas.add(
                        global(
                                TipoDeInconsistencia.ACTIVIDADES_OBLIGATORIAS,
                                reglas,
                                objeto,
                                DatosDeLaInconsistencia.lista(faltan)));
            }
        }
    }

    // Con la meta y el margen, el límite está incluido (D-86).
    private static Optional<DatosDeLaInconsistencia> fueraDelMargen(
            CargaHoraria carga, ReglasDelPanel reglas) {
        BigDecimal minimo = reglas.metaClase().subtract(reglas.margenPuntos());
        BigDecimal maximo = reglas.metaClase().add(reglas.margenPuntos());
        return carga.proporcion()
                .map(proporcion -> BigDecimal.valueOf(proporcion.clase()))
                .filter(clase -> clase.compareTo(minimo) < 0 || clase.compareTo(maximo) > 0)
                .map(
                        clase ->
                                new DatosDeLaInconsistencia(
                                        clase, reglas.metaClase(), minimo, maximo, List.of()));
    }

    private static List<String> sinConstar(
            DocenteActivo docente, VersionDistributivo version, ReglasDelPanel reglas) {
        Set<String> suyas =
                version.actividades().stream()
                        .filter(una -> una.docente().equals(docente.identificacion()))
                        .map(ActividadNoLectiva::actividad)
                        .collect(Collectors.toSet());
        return reglas.actividadesObligatorias().stream()
                .filter(codigo -> !suyas.contains(codigo))
                .toList();
    }

    private static boolean sinPropuesta(AsignaturaParalelo una, VersionDistributivo version) {
        return version.propuestaDe(una.asignatura().codigo(), una.paralelo()).isEmpty();
    }

    private static InconsistenciaGlobal sinPropuesta(
            AsignaturaParalelo una, ReglasDelPanel reglas) {
        return global(
                TipoDeInconsistencia.PARALELO_SIN_PROPUESTA,
                reglas,
                ObjetoAfectado.de(una),
                DatosDeLaInconsistencia.cantidad(una.horasSemanales()));
    }

    private static void evaluarCarreras(
            List<AsignaturaParalelo> vigentes,
            List<CargoVigente> cargos,
            ReglasDelPanel reglas,
            List<InconsistenciaGlobal> halladas) {
        Map<String, List<AsignaturaParalelo>> porCarrera =
                vigentes.stream()
                        .collect(
                                Collectors.groupingBy(
                                        AsignaturaParalelo::carrera,
                                        TreeMap::new,
                                        Collectors.toList()));
        porCarrera.forEach(
                (carrera, paralelos) -> {
                    long coordinadores =
                            cargos.stream().filter(uno -> uno.coordina(carrera)).count();
                    if (coordinadores != 1) {
                        halladas.add(
                                global(
                                        TipoDeInconsistencia.COORDINADORES_DE_LA_CARRERA,
                                        reglas,
                                        ObjetoAfectado.carrera(carrera),
                                        DatosDeLaInconsistencia.cantidad(
                                                BigDecimal.valueOf(coordinadores))));
                    }
                    List<String> sobreLaMalla =
                            paralelos.stream()
                                    .filter(MotorReglas::sobreLaMalla)
                                    .map(una -> ObjetoAfectado.de(una).nombre())
                                    .sorted()
                                    .toList();
                    if (!sobreLaMalla.isEmpty()) {
                        halladas.add(
                                global(
                                        TipoDeInconsistencia.HORAS_SOBRE_LA_MALLA,
                                        reglas,
                                        ObjetoAfectado.carrera(carrera),
                                        DatosDeLaInconsistencia.lista(sobreLaMalla)));
                    }
                });
    }

    private static boolean sobreLaMalla(AsignaturaParalelo una) {
        return una.horasSemanales().compareTo(una.horasDeLaAsignatura()) > 0;
    }

    private static Optional<InconsistenciaGlobal> evaluarGestores(
            DatosDelPeriodo datos, ReglasDelPanel reglas) {
        long gestores = datos.cargos().stream().filter(CargoVigente::esDeGestion).count();
        if (gestores >= reglas.gestoresMinimo() && gestores <= reglas.gestoresMaximo()) {
            return Optional.empty();
        }
        return Optional.of(
                global(
                        TipoDeInconsistencia.GESTORES_FUERA_DE_RANGO,
                        reglas,
                        ObjetoAfectado.periodo(datos.periodo()),
                        DatosDeLaInconsistencia.rango(
                                BigDecimal.valueOf(gestores),
                                BigDecimal.valueOf(reglas.gestoresMinimo()),
                                BigDecimal.valueOf(reglas.gestoresMaximo()))));
    }

    private static InconsistenciaGlobal global(
            TipoDeInconsistencia tipo,
            ReglasDelPanel reglas,
            ObjetoAfectado objeto,
            DatosDeLaInconsistencia datos) {
        return new InconsistenciaGlobal(tipo, reglas.severidad(tipo), objeto, datos);
    }

    /** Si alguno de sus proyectos asignables activa el tope de clase (RN-13). */
    public static boolean conProyecto(Collection<String> tipos) {
        return tipos.stream().anyMatch(TIPOS_CON_TOPE::contains);
    }

    private static boolean sePasaYSube(BigDecimal antes, BigDecimal despues, BigDecimal limite) {
        return despues.compareTo(limite) > 0 && despues.compareTo(antes) > 0;
    }

    private static BigDecimal queQueda(BigDecimal limite, BigDecimal tiene) {
        return limite.subtract(tiene).max(BigDecimal.ZERO);
    }
}
