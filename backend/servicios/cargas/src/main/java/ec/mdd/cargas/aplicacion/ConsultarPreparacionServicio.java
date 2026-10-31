package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.DiccionarioDeDatos;
import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.MotivoDeRechazo;
import ec.mdd.cargas.dominio.PreparacionDelPeriodo;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.puertos.entrada.ConsultarPlantillas;
import ec.mdd.cargas.puertos.entrada.ConsultarPreparacion;
import ec.mdd.cargas.puertos.entrada.PeriodoParaElegir;
import ec.mdd.cargas.puertos.entrada.PreparacionConsultada;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.PeriodoAcademico;
import ec.mdd.cargas.puertos.salida.PeriodoNoEncontrado;
import ec.mdd.cargas.puertos.salida.ProyectosDelPeriodo;
import ec.mdd.cargas.puertos.salida.ReferenciasDelSistema;
import java.util.List;
import java.util.Optional;

/**
 * El tablero de preparación de un periodo (RF-56, F02-H02): lo que dice cada dueño de sus datos en
 * el periodo, los proyectos asignables o «sin proyectos», el último intento de cada carga en el
 * historial y si el periodo está abierto o cerrado. Se calcula en cada consulta; no se guarda.
 */
public final class ConsultarPreparacionServicio implements ConsultarPreparacion {

    private final ConsultarPlantillas plantillas;
    private final DuenosDeLasCargas duenos;
    private final HistorialDeCargas historial;
    private final ProyectosDelPeriodo proyectos;
    private final ReferenciasDelSistema referencias;

    /**
     * Recibe el diccionario, los dueños, el historial, la consulta de proyectos de Docentes y los
     * periodos de Académico.
     */
    public ConsultarPreparacionServicio(
            ConsultarPlantillas plantillas,
            DuenosDeLasCargas duenos,
            HistorialDeCargas historial,
            ProyectosDelPeriodo proyectos,
            ReferenciasDelSistema referencias) {
        this.plantillas = plantillas;
        this.duenos = duenos;
        this.historial = historial;
        this.proyectos = proyectos;
        this.referencias = referencias;
    }

    @Override
    public PreparacionConsultada preparacion(Optional<String> periodo) {
        List<PeriodoAcademico> todos = referencias.periodos();
        List<PeriodoParaElegir> paraElegir =
                todos.stream()
                        .map(uno -> new PeriodoParaElegir(uno.codigo(), uno.nombre()))
                        .toList();
        if (todos.isEmpty() && periodo.isEmpty()) {
            return new PreparacionConsultada(paraElegir, PreparacionDelPeriodo.sinPeriodos());
        }
        PeriodoAcademico elegido =
                periodo.map(
                                codigo ->
                                        todos.stream()
                                                .filter(uno -> uno.codigo().equals(codigo))
                                                .findFirst()
                                                .orElseThrow(() -> new PeriodoNoEncontrado(codigo)))
                        .orElse(todos.getFirst());
        DiccionarioDeDatos diccionario = plantillas.diccionario();
        String deProyectos = CargaDeProyectos.codigo(diccionario);
        boolean sinProyectos =
                historial.sinProyectos(deProyectos).stream()
                        .anyMatch(
                                entrada -> entrada.periodo().equals(Optional.of(elegido.codigo())));
        PreparacionDelPeriodo tablero =
                PreparacionDelPeriodo.de(
                        diccionario,
                        elegido.codigo(),
                        elegido.abierto(),
                        elegido.cerrado(),
                        carga ->
                                carga.equals(deProyectos)
                                        ? proyectos.hayAsignables(elegido.codigo())
                                        : duenos.tieneDatosEn(carga, elegido.codigo()),
                        sinProyectos,
                        this::ultimoRechazo);
        return new PreparacionConsultada(paraElegir, tablero);
    }

    /**
     * El motivo del último intento de confirmar la carga, si fue rechazado; sin «sin proyectos».
     */
    private Optional<MotivoDeRechazo> ultimoRechazo(String carga) {
        return historial.deLaCarga(carga).stream()
                .filter(entrada -> entrada.resultado() != ResultadoDeHistorial.SIN_PROYECTOS)
                .findFirst()
                .flatMap(EntradaDeHistorial::motivo);
    }
}
