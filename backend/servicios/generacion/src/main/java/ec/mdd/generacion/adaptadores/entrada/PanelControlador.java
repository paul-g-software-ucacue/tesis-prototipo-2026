package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.InconsistenciaDelPanel;
import ec.mdd.generacion.adaptadores.entrada.api.JustificacionDelPanel;
import ec.mdd.generacion.adaptadores.entrada.api.JustificacionNueva;
import ec.mdd.generacion.adaptadores.entrada.api.ObjetoAfectado;
import ec.mdd.generacion.adaptadores.entrada.api.PanelApi;
import ec.mdd.generacion.adaptadores.entrada.api.PanelDeInconsistencias;
import ec.mdd.generacion.adaptadores.entrada.api.TipoDeInconsistencia;
import ec.mdd.generacion.adaptadores.entrada.api.TotalesDelPanel;
import ec.mdd.generacion.dominio.DatosDeLaInconsistencia;
import ec.mdd.generacion.dominio.EntradaDelPanel;
import ec.mdd.generacion.dominio.InconsistenciaGlobal;
import ec.mdd.generacion.dominio.JustificacionObligatoria;
import ec.mdd.generacion.dominio.Severidad;
import ec.mdd.generacion.puertos.entrada.ConsultarPanel;
import ec.mdd.generacion.puertos.entrada.JustificarInconsistencia;
import ec.mdd.generacion.puertos.entrada.SolicitudDeJustificacion;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * El panel de inconsistencias (RF-29). El dominio entrega los hechos, y aquí se arma lo que lee
 * quien abre el panel: qué pasa, qué hacer, quién debe hacerlo y en qué pantalla. Lo leen
 * Consolidación y el Vicerrectorado, y justifica Consolidación (D-87).
 */
@RestController
class PanelControlador implements PanelApi {

    private static final String LEER =
            "hasAnyAuthority('PERMISO_CONSOLIDACION:LECTURA', 'PERMISO_DISTRIBUTIVO:ESCRITURA')";
    private static final String JUSTIFICAR = "hasAuthority('PERMISO_CONSOLIDACION:ESCRITURA')";

    /** Las reglas globales que el panel todavía no evalúa: el panel lo dice (FR-005). */
    private static final List<String> SIN_EVALUAR =
            List.of(
                    "RN-03", "RN-04", "RN-07", "RN-08", "RN-09", "RN-11", "RN-14", "RN-15",
                    "RN-16");

    private static final String COORDINADOR = "El coordinador de la carrera debe ";
    private static final String VICERRECTORADO = "El Vicerrectorado debe ";
    private static final String SE_PUEDE_JUSTIFICAR = " Consolidación puede justificarla aquí.";

    private final ConsultarPanel consultar;
    private final JustificarInconsistencia justificar;

    PanelControlador(ConsultarPanel consultar, JustificarInconsistencia justificar) {
        this.consultar = consultar;
        this.justificar = justificar;
    }

    @Override
    @PreAuthorize(LEER)
    public ResponseEntity<PanelDeInconsistencias> consultarPanelDeInconsistencias(String periodo) {
        ec.mdd.generacion.dominio.PanelDeInconsistencias panel = consultar.panel(periodo);
        PanelDeInconsistencias respuesta =
                new PanelDeInconsistencias(
                        periodo,
                        panel.creado(),
                        panel.modificable(),
                        false, // #215: lo llena T006
                        new TotalesDelPanel(
                                Math.toIntExact(panel.abiertas(Severidad.DURA)),
                                Math.toIntExact(panel.abiertas(Severidad.BLANDA))),
                        panel.entradas().stream().map(PanelControlador::respuesta).toList(),
                        SIN_EVALUAR);
        if (panel.creado()) {
            respuesta.setVersion(panel.numero());
        }
        return ResponseEntity.ok(respuesta);
    }

    @Override
    @PreAuthorize(JUSTIFICAR)
    public ResponseEntity<InconsistenciaDelPanel> justificarInconsistencia(
            String periodo, JustificacionNueva nueva) {
        try {
            return ResponseEntity.ok(
                    respuesta(
                            justificar.justificar(
                                    new SolicitudDeJustificacion(
                                            periodo,
                                            ec.mdd.generacion.dominio.TipoDeInconsistencia.valueOf(
                                                    nueva.getTipo().name()),
                                            nueva.getObjeto(),
                                            nueva.getTexto(),
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication()
                                                    .getName()))));
        } catch (JustificacionObligatoria error) {
            throw new SolicitudInvalida(
                    List.of(
                            new DetalleRespuesta(
                                    "texto",
                                    "Escribe por qué se acepta, en 10 a 500 caracteres.")));
        }
    }

    private static InconsistenciaDelPanel respuesta(EntradaDelPanel entrada) {
        InconsistenciaGlobal una = entrada.inconsistencia();
        InconsistenciaDelPanel respuesta =
                new InconsistenciaDelPanel(
                        TipoDeInconsistencia.fromValue(una.tipo().name()),
                        una.tipo().regla(),
                        InconsistenciaDelPanel.SeveridadEnum.fromValue(una.severidad().name()),
                        entrada.abierta()
                                ? InconsistenciaDelPanel.EstadoEnum.ABIERTA
                                : InconsistenciaDelPanel.EstadoEnum.JUSTIFICADA,
                        new ObjetoAfectado(
                                ObjetoAfectado.ClaseEnum.fromValue(una.objeto().clase().name()),
                                una.objeto().clave(),
                                una.objeto().nombre()),
                        detalle(una),
                        indicacion(una),
                        pantalla(una.tipo()));
        entrada.justificacion()
                .ifPresent(
                        justificacion ->
                                respuesta.setJustificacion(
                                        new JustificacionDelPanel(
                                                justificacion.texto(),
                                                justificacion.por(),
                                                justificacion.en().atOffset(ZoneOffset.UTC))));
        return respuesta;
    }

    private static String detalle(InconsistenciaGlobal una) {
        DatosDeLaInconsistencia datos = una.datos();
        return switch (una.tipo()) {
            case DOCENTE_SIN_CLASE -> "No tiene horas de clase.";
            case PARALELO_SIN_PROPUESTA ->
                    "Falta el docente de " + numero(datos.cantidad()) + " h de clase.";
            case COORDINADORES_DE_LA_CARRERA ->
                    datos.cantidad().signum() == 0
                            ? "La carrera no tiene coordinador."
                            : "La carrera tiene " + numero(datos.cantidad()) + " coordinadores.";
            case GESTORES_FUERA_DE_RANGO ->
                    "Hay "
                            + numero(datos.cantidad())
                            + " docentes con cargo de gestión; la normativa admite de "
                            + numero(datos.minimo())
                            + " a "
                            + numero(datos.maximo())
                            + ".";
            case HORAS_SOBRE_LA_MALLA ->
                    "Estos paralelos pasan las horas de su asignatura en la malla: "
                            + String.join(", ", datos.lista())
                            + ".";
            case SOBRECARGA ->
                    "Tiene "
                            + numero(datos.cantidad())
                            + " h asignadas y su límite es "
                            + numero(datos.maximo())
                            + " h.";
            case PROPORCION -> proporcion(datos);
            case ACTIVIDADES_OBLIGATORIAS ->
                    "Le falta en su carga: " + String.join(", ", datos.lista()) + ".";
            case EXCEPCION_DE_AFINIDAD ->
                    "Propuesta bajo el mínimo de afinidad: "
                            + String.join(", ", datos.lista())
                            + ".";
        };
    }

    private static String proporcion(DatosDeLaInconsistencia datos) {
        BigDecimal cien = BigDecimal.valueOf(100);
        return "Tiene "
                + numero(datos.cantidad())
                + " % de clase y "
                + numero(cien.subtract(datos.cantidad()))
                + " % de otras actividades; la meta es "
                + numero(datos.referencia())
                + "/"
                + numero(cien.subtract(datos.referencia()))
                + ", y se admite de "
                + numero(datos.minimo())
                + " a "
                + numero(datos.maximo())
                + " % de clase.";
    }

    private static String indicacion(InconsistenciaGlobal una) {
        String base =
                switch (una.tipo()) {
                    case DOCENTE_SIN_CLASE -> COORDINADOR + "proponerle al menos un paralelo.";
                    case PARALELO_SIN_PROPUESTA -> COORDINADOR + "proponer un docente.";
                    case COORDINADORES_DE_LA_CARRERA ->
                            VICERRECTORADO + "dejar un solo coordinador vigente en esta carrera.";
                    case GESTORES_FUERA_DE_RANGO ->
                            VICERRECTORADO + "revisar los gestores designados.";
                    case HORAS_SOBRE_LA_MALLA ->
                            VICERRECTORADO + "bajar sus horas a las de su asignatura.";
                    case SOBRECARGA -> COORDINADOR + "bajar sus horas hasta su límite.";
                    case PROPORCION -> COORDINADOR + "ajustar sus horas.";
                    case ACTIVIDADES_OBLIGATORIAS -> COORDINADOR + "asignarle lo que falta.";
                    case EXCEPCION_DE_AFINIDAD -> "Ya está justificada en su propuesta.";
                };
        return una.severidad() == Severidad.BLANDA && una.tipo().evalua()
                ? base + SE_PUEDE_JUSTIFICAR
                : base;
    }

    private static InconsistenciaDelPanel.PantallaEnum pantalla(
            ec.mdd.generacion.dominio.TipoDeInconsistencia tipo) {
        return switch (tipo) {
            case COORDINADORES_DE_LA_CARRERA, GESTORES_FUERA_DE_RANGO ->
                    InconsistenciaDelPanel.PantallaEnum.CARGOS;
            case SOBRECARGA, PROPORCION, ACTIVIDADES_OBLIGATORIAS ->
                    InconsistenciaDelPanel.PantallaEnum.ACTIVIDADES;
            case DOCENTE_SIN_CLASE,
                    PARALELO_SIN_PROPUESTA,
                    HORAS_SOBRE_LA_MALLA,
                    EXCEPCION_DE_AFINIDAD ->
                    InconsistenciaDelPanel.PantallaEnum.OFERTA;
        };
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
