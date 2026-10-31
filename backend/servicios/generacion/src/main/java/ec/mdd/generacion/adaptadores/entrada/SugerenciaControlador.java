package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.Candidato;
import ec.mdd.generacion.adaptadores.entrada.api.Dedicacion;
import ec.mdd.generacion.adaptadores.entrada.api.Sugerencia;
import ec.mdd.generacion.adaptadores.entrada.api.SugerenciaApi;
import ec.mdd.generacion.puertos.entrada.ConsultarSugerencia;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * La sugerencia de docentes ({@code contratos/generacion/openapi.yaml}, RF-25). La pueden ver
 * quienes pueden leer un docente, como dice su {@code x-mdd-permiso} (D-70); {@code
 * PermisosGeneracionTest} lo comprueba. Los modelos del contrato se llaman como los del dominio:
 * los del dominio van con su nombre completo.
 */
@RestController
class SugerenciaControlador implements SugerenciaApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_DOCENTES:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_CATALOGOS:LECTURA')";

    private final ConsultarSugerencia consultar;

    SugerenciaControlador(ConsultarSugerencia consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<Sugerencia> consultarSugerencia(
            String periodo, String asignatura, String paralelo) {
        ec.mdd.generacion.dominio.Sugerencia sugerencia;
        try {
            sugerencia =
                    consultar.sugerencia(
                            periodo, asignatura, paralelo, AfinidadControlador.carreraPropia());
        } catch (OtraCarrera error) {
            throw new AccessDeniedException("La asignatura-paralelo es de otra carrera", error);
        }
        BigDecimal horasDelParalelo = sugerencia.paralelo().horasSemanales();
        return ResponseEntity.ok(
                new Sugerencia(
                        sugerencia.periodo(),
                        sugerencia.paralelo().asignatura().codigo(),
                        sugerencia.paralelo().asignatura().nombre(),
                        sugerencia.paralelo().paralelo(),
                        horasDelParalelo,
                        sugerencia.paralelo().troncoComun(),
                        sugerencia.version(),
                        sugerencia.minimoAfinidad(),
                        sugerencia.fueraPorElMinimo(),
                        sugerencia.candidatos().stream()
                                .map(candidato -> candidato(candidato, horasDelParalelo))
                                .toList()));
    }

    private static Candidato candidato(
            ec.mdd.generacion.dominio.Candidato candidato, BigDecimal horasDelParalelo) {
        return new Candidato(
                candidato.identificacion(),
                candidato.nombre(),
                Dedicacion.fromValue(candidato.dedicacion().name()),
                candidato.horasContratadas(),
                candidato.horasDisponibles(),
                candidato.horas().enOtrasCarreras(),
                candidato.tieneHorasPara(horasDelParalelo),
                AfinidadControlador.puntaje(candidato.puntaje()));
    }
}
