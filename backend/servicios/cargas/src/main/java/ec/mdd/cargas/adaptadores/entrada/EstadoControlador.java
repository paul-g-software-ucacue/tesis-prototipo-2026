package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.CargaQueFalta;
import ec.mdd.cargas.adaptadores.entrada.api.ConfirmacionSinProyectos;
import ec.mdd.cargas.adaptadores.entrada.api.EntradaDeHistorial;
import ec.mdd.cargas.adaptadores.entrada.api.EstadoApi;
import ec.mdd.cargas.adaptadores.entrada.api.EstadoDeCarga;
import ec.mdd.cargas.adaptadores.entrada.api.EstadoDeLaCarga;
import ec.mdd.cargas.adaptadores.entrada.api.EstadoDeLasCargas;
import ec.mdd.cargas.puertos.entrada.ConfirmarSinProyectos;
import ec.mdd.cargas.puertos.entrada.ConsultarEstadoDeLasCargas;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * El estado de las cargas y la confirmación «sin proyectos» ({@code contratos/cargas/openapi.yaml},
 * RF-56, RF-18, F01-H05). El estado lo leen quienes leen las cargas o el distributivo (D-70);
 * confirmar exige «Cargas: escritura», como confirmar una carga (D-71). {@code PermisosCargasTest}
 * lo comprueba.
 */
@RestController
class EstadoControlador implements EstadoApi {

    private final ConsultarEstadoDeLasCargas consultar;
    private final ConfirmarSinProyectos confirmar;

    EstadoControlador(ConsultarEstadoDeLasCargas consultar, ConfirmarSinProyectos confirmar) {
        this.consultar = consultar;
        this.confirmar = confirmar;
    }

    @Override
    @PreAuthorize("hasAnyAuthority('PERMISO_CARGAS:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA')")
    public ResponseEntity<EstadoDeLasCargas> consultarEstadoDeLasCargas() {
        return ResponseEntity.ok(
                new EstadoDeLasCargas(
                        consultar.estados().stream().map(EstadoControlador::estado).toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_CARGAS:ESCRITURA')")
    public ResponseEntity<EntradaDeHistorial> confirmarSinProyectos(
            ConfirmacionSinProyectos confirmacion) {
        ec.mdd.cargas.dominio.ConfirmacionSinProyectos confirmada =
                confirmar.confirmar(
                        confirmacion.getPeriodo(),
                        SecurityContextHolder.getContext().getAuthentication().getName());
        return ResponseEntity.status(confirmada.nueva() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(HistorialControlador.entrada(confirmada.entrada()));
    }

    private static EstadoDeCarga estado(ec.mdd.cargas.dominio.EstadoDeCarga estado) {
        return new EstadoDeCarga(
                estado.numero(),
                estado.codigo(),
                EstadoDeLaCarga.fromValue(estado.estado().name()),
                estado.faltan().stream()
                        .map(
                                falta ->
                                        new CargaQueFalta(
                                                falta.numero(), falta.codigo(), falta.nombre()))
                        .toList(),
                new ArrayList<>(estado.periodosSinProyectos()));
    }
}
