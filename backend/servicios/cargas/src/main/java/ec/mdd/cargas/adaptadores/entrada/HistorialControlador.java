package ec.mdd.cargas.adaptadores.entrada;

import ec.mdd.cargas.adaptadores.entrada.api.EntradaDeHistorial;
import ec.mdd.cargas.adaptadores.entrada.api.HistorialApi;
import ec.mdd.cargas.adaptadores.entrada.api.ResultadoDeHistorial;
import ec.mdd.cargas.puertos.entrada.CargaNoEncontrada;
import ec.mdd.cargas.puertos.entrada.ConsultarHistorial;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * El historial de una carga ({@code contratos/cargas/openapi.yaml}, RF-10, F01-H04). Lo leen
 * quienes leen las cargas o el distributivo (D-70), como las plantillas; {@code PermisosCargasTest}
 * lo comprueba.
 */
@RestController
class HistorialControlador implements HistorialApi {

    private final ConsultarHistorial consultar;

    HistorialControlador(ConsultarHistorial consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize("hasAnyAuthority('PERMISO_CARGAS:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA')")
    public ResponseEntity<List<EntradaDeHistorial>> listarHistorial(String codigo) {
        try {
            return ResponseEntity.ok(
                    consultar.historial(codigo).stream()
                            .map(HistorialControlador::entrada)
                            .toList());
        } catch (CargaNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /** La entrada del historial en el contrato; también la usa la confirmación «sin proyectos». */
    static EntradaDeHistorial entrada(ec.mdd.cargas.dominio.EntradaDeHistorial entrada) {
        EntradaDeHistorial respuesta =
                new EntradaDeHistorial(
                        entrada.usuario(),
                        entrada.momento().atOffset(ZoneOffset.UTC),
                        ResultadoDeHistorial.fromValue(entrada.resultado().name()),
                        entrada.nuevos(),
                        entrada.cambiados(),
                        entrada.bajasSinAplicar());
        respuesta.setArchivo(entrada.archivo());
        respuesta.setHash(entrada.hash());
        entrada.periodo().ifPresent(respuesta::setPeriodo);
        entrada.motivo().map(HistorialControlador::codigo).ifPresent(respuesta::setMotivo);
        return respuesta;
    }

    /** El código del catálogo de cada motivo de rechazo. */
    static String codigo(ec.mdd.cargas.dominio.MotivoDeRechazo motivo) {
        return switch (motivo) {
            case CON_ERRORES -> "CARGAS_CARGA_CON_ERRORES";
            case VISTA_PREVIA_DESACTUALIZADA -> "CARGAS_VISTA_PREVIA_DESACTUALIZADA";
            case RECHAZADA_POR_EL_DUENO -> "CARGAS_CARGA_RECHAZADA";
        };
    }
}
