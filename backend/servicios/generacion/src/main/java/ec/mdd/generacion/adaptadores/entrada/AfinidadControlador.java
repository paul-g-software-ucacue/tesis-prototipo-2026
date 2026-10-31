package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.AfinidadApi;
import ec.mdd.generacion.adaptadores.entrada.api.AfinidadDelDocente;
import ec.mdd.generacion.adaptadores.entrada.api.AporteDeAfinidad;
import ec.mdd.generacion.adaptadores.entrada.api.CoincidenciaDeCampo;
import ec.mdd.generacion.adaptadores.entrada.api.PuntajeAfinidad;
import ec.mdd.generacion.adaptadores.entrada.api.TipoDeAporte;
import ec.mdd.generacion.puertos.entrada.ConsultarAfinidad;
import ec.mdd.plataforma.errores.nucleo.AlcanceDeCarrera;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * La afinidad de un docente ({@code contratos/generacion/openapi.yaml}, RF-17). La pueden ver
 * quienes pueden leer un docente, como dice su {@code x-mdd-permiso} (D-70); {@code
 * PermisosGeneracionTest} lo comprueba. Los modelos del contrato se llaman como los del dominio:
 * los del dominio van con su nombre completo.
 */
@RestController
class AfinidadControlador implements AfinidadApi {

    private static final String LECTURA =
            "hasAnyAuthority('PERMISO_DOCENTES:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_CATALOGOS:LECTURA')";

    private final ConsultarAfinidad consultar;

    AfinidadControlador(ConsultarAfinidad consultar) {
        this.consultar = consultar;
    }

    @Override
    @PreAuthorize(LECTURA)
    public ResponseEntity<AfinidadDelDocente> consultarAfinidadDelDocente(
            String identificacion, String periodo) {
        ec.mdd.generacion.dominio.AfinidadDelDocente afinidad =
                consultar.afinidad(identificacion, periodo, carreraPropia());
        return ResponseEntity.ok(
                new AfinidadDelDocente(
                        afinidad.periodo(),
                        afinidad.version(),
                        afinidad.puntajes().stream().map(AfinidadControlador::puntaje).toList()));
    }

    /** La carrera a la que se limita quien solo tiene «lo propio» sobre la oferta (D-64). */
    static Optional<String> carreraPropia() {
        return AlcanceDeCarrera.propia(
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());
    }

    static PuntajeAfinidad puntaje(ec.mdd.generacion.dominio.PuntajeAfinidad puntaje) {
        return new PuntajeAfinidad(
                puntaje.codigo(),
                puntaje.nombre(),
                puntaje.total(),
                puntaje.topeAplicado(),
                puntaje.desglose().stream().map(AfinidadControlador::aporte).toList());
    }

    private static AporteDeAfinidad aporte(ec.mdd.generacion.dominio.AporteDeAfinidad aporte) {
        return new AporteDeAfinidad(
                TipoDeAporte.fromValue(aporte.tipo().name()),
                aporte.nombre(),
                CoincidenciaDeCampo.fromValue(aporte.coincidencia().name()),
                aporte.aporte());
    }
}
