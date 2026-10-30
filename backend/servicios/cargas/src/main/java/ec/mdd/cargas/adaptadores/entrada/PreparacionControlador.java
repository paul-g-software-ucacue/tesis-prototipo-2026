package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.CargaDelPeriodo;
import ec.mdd.cargas.adaptadores.entrada.api.CargaQueFalta;
import ec.mdd.cargas.adaptadores.entrada.api.EstadoDeLaCarga;
import ec.mdd.cargas.adaptadores.entrada.api.LoQueFalta;
import ec.mdd.cargas.adaptadores.entrada.api.Obligatoriedad;
import ec.mdd.cargas.adaptadores.entrada.api.PeriodoParaElegir;
import ec.mdd.cargas.adaptadores.entrada.api.PreparacionApi;
import ec.mdd.cargas.adaptadores.entrada.api.PreparacionDelPeriodo;
import ec.mdd.cargas.adaptadores.entrada.api.TipoDeLoQueFalta;
import ec.mdd.cargas.puertos.entrada.ConsultarPreparacion;
import ec.mdd.cargas.puertos.entrada.PreparacionConsultada;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * El tablero de preparación de un periodo ({@code contratos/cargas/openapi.yaml}, RF-56, F02-H02).
 * Lo leen quienes leen el distributivo o las cargas (D-70); {@code PermisosCargasTest} lo
 * comprueba.
 */
@RestController
class PreparacionControlador implements PreparacionApi {

    private final ConsultarPreparacion consultar;

    PreparacionControlador(ConsultarPreparacion consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize("hasAnyAuthority('PERMISO_CARGAS:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA')")
    public ResponseEntity<PreparacionDelPeriodo> consultarPreparacionDelPeriodo(String periodo) {
        PreparacionConsultada consultada = consultar.preparacion(Optional.ofNullable(periodo));
        ec.mdd.cargas.dominio.PreparacionDelPeriodo tablero = consultada.tablero();
        PreparacionDelPeriodo respuesta =
                new PreparacionDelPeriodo(
                        consultada.periodos().stream()
                                .map(uno -> new PeriodoParaElegir(uno.codigo(), uno.nombre()))
                                .toList(),
                        tablero.cargas().stream().map(PreparacionControlador::carga).toList(),
                        tablero.faltan().stream().map(PreparacionControlador::falta).toList(),
                        tablero.listo());
        tablero.periodo()
                .ifPresent(
                        codigo -> {
                            respuesta.setPeriodo(codigo);
                            respuesta.setAbierto(tablero.abierto());
                            respuesta.setCerrado(tablero.cerrado());
                        });
        return ResponseEntity.ok(respuesta);
    }

    private static CargaDelPeriodo carga(ec.mdd.cargas.dominio.CargaDelPeriodo carga) {
        CargaDelPeriodo respuesta =
                new CargaDelPeriodo(
                        carga.numero(),
                        carga.codigo(),
                        carga.nombre(),
                        Obligatoriedad.fromValue(carga.obligatoriedad().name()),
                        EstadoDeLaCarga.fromValue(carga.estado().name()));
        carga.motivo().map(HistorialControlador::codigo).ifPresent(respuesta::setMotivo);
        return respuesta;
    }

    private static LoQueFalta falta(ec.mdd.cargas.dominio.LoQueFalta falta) {
        LoQueFalta respuesta = new LoQueFalta(TipoDeLoQueFalta.fromValue(falta.tipo().name()));
        falta.carga()
                .ifPresent(
                        carga ->
                                respuesta.setCarga(
                                        new CargaQueFalta(
                                                carga.numero(), carga.codigo(), carga.nombre())));
        falta.estado()
                .ifPresent(estado -> respuesta.setEstado(EstadoDeLaCarga.fromValue(estado.name())));
        return respuesta;
    }
}
