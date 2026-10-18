package ec.mdd.normativa.adaptadores.entrada;

import ec.mdd.normativa.adaptadores.entrada.api.FilaCargaHoraria;
import ec.mdd.normativa.adaptadores.entrada.api.Parametro;
import ec.mdd.normativa.adaptadores.entrada.api.Regla;
import ec.mdd.normativa.adaptadores.entrada.api.Severidad;
import ec.mdd.normativa.adaptadores.entrada.api.TipoIst;
import ec.mdd.normativa.adaptadores.entrada.api.VersionNormativa;
import ec.mdd.normativa.adaptadores.entrada.api.VersionResumen;
import ec.mdd.normativa.adaptadores.entrada.api.VersionesApi;
import ec.mdd.normativa.puertos.entrada.ConsultarNormativa;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * La consulta de las versiones de normativa ({@code contratos/normativa/openapi.yaml}, RF-19,
 * F04-H01). Las dos operaciones exigen «Parámetros y normativa: lectura», como dice su {@code
 * x-mdd-permiso} (ADR-021, D-66); {@code PermisosNormativaTest} lo comprueba. Una versión que no
 * existe responde 404 con el código común.
 */
@RestController
class NormativaControlador implements VersionesApi {

    private static final String LECTURA = "hasAuthority('PERMISO_PARAMETROS:LECTURA')";

    private final ConsultarNormativa consultar;

    NormativaControlador(ConsultarNormativa consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<VersionResumen>> listarVersiones() {
        return ResponseEntity.ok(
                consultar.versiones().stream()
                        .map(
                                version ->
                                        new VersionResumen(
                                                version.numero(),
                                                version.codigo(),
                                                version.origen(),
                                                version.vigenteDesde(),
                                                TipoIst.fromValue(version.tipoIst().name())))
                        .toList());
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<VersionNormativa> consultarVersion(Integer numero) {
        ec.mdd.normativa.dominio.VersionNormativa version;
        try {
            version = consultar.version(numero);
        } catch (VersionNoEncontrada error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(
                new VersionNormativa(
                        version.numero(),
                        version.codigo(),
                        version.origen(),
                        version.vigenteDesde(),
                        TipoIst.fromValue(version.tipoIst().name()),
                        version.reglas().stream().map(NormativaControlador::regla).toList(),
                        version.tabla().filas().stream().map(NormativaControlador::fila).toList()));
    }

    private static Regla regla(ec.mdd.normativa.dominio.Regla regla) {
        Regla respuesta =
                new Regla(
                        regla.identificador(),
                        regla.parametros().entrySet().stream()
                                .map(
                                        parametro ->
                                                new Parametro(
                                                        parametro.getKey(),
                                                        sinCerosALaDerecha(parametro.getValue())))
                                .toList());
        if (regla.severidad() != null) {
            respuesta.setSeveridad(Severidad.fromValue(regla.severidad().name()));
        }
        return respuesta;
    }

    private static FilaCargaHoraria fila(ec.mdd.normativa.dominio.FilaCargaHoraria fila) {
        FilaCargaHoraria respuesta =
                new FilaCargaHoraria(
                        FilaCargaHoraria.AplicaAEnum.fromValue(
                                fila.aplicaATodos() ? "TODOS" : fila.aplicaA().name()),
                        fila.perfil(),
                        fila.dedicacionTotal(),
                        fila.horasClase(),
                        fila.horasClaseMinimo(),
                        fila.horasClaseMaximo(),
                        Severidad.fromValue(fila.severidad().name()),
                        fila.referencia());
        // Lo que la política no da queda en null y se omite en la respuesta (contrato).
        respuesta.setHorasOtrasDocencia(fila.horasOtrasDocencia());
        respuesta.setHorasGestion(fila.horasGestion());
        respuesta.setHorasInvestigacionVinculacionMaximo(
                fila.horasInvestigacionVinculacionMaximo());
        respuesta.setObservacion(fila.observacion());
        return respuesta;
    }

    // El 40.00 de la base se muestra como 40, sin notación científica.
    private static BigDecimal sinCerosALaDerecha(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }
}
