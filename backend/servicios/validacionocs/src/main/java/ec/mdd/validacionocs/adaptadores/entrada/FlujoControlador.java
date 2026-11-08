package ec.mdd.validacionocs.adaptadores.entrada;

import ec.mdd.plataforma.errores.nucleo.AlcanceDeCarrera;
import ec.mdd.plataforma.errores.nucleo.CarreraSinIndicar;
import ec.mdd.validacionocs.adaptadores.entrada.api.AccionDelFlujo;
import ec.mdd.validacionocs.adaptadores.entrada.api.AprobacionApi;
import ec.mdd.validacionocs.adaptadores.entrada.api.EstadoDelFlujo;
import ec.mdd.validacionocs.adaptadores.entrada.api.ParcialDelFlujo;
import ec.mdd.validacionocs.adaptadores.entrada.api.TipoDeEtapa;
import ec.mdd.validacionocs.adaptadores.entrada.api.TransicionDelFlujo;
import ec.mdd.validacionocs.dominio.ParcialDeOtraCarrera;
import ec.mdd.validacionocs.dominio.RolDeOtraEtapa;
import ec.mdd.validacionocs.dominio.Solicitante;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.AvanzarEtapa;
import ec.mdd.validacionocs.puertos.entrada.ConsultarFlujo;
import ec.mdd.validacionocs.puertos.entrada.EnviarParcial;
import ec.mdd.validacionocs.puertos.entrada.FlujoDelDistributivo;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * El flujo del distributivo ({@code contratos/validacionocs/openapi.yaml}, etiqueta {@code
 * Aprobacion}, F12-H02). El permiso de cada operación lo exige su {@code x-mdd-permiso} (ADR-021);
 * el rol de la etapa y la carrera los exige el dominio con los roles y la carrera del token. Si no
 * corresponden, el mismo 403 común que sin permiso, sin decir qué falta.
 */
@RestController
class FlujoControlador implements AprobacionApi {

    private static final String ROL = "ROLE_";
    private static final String ENVIAR = "OFERTA:APROBACION";

    private final ConsultarFlujo consultar;
    private final EnviarParcial enviar;
    private final AvanzarEtapa avanzar;

    FlujoControlador(ConsultarFlujo consultar, EnviarParcial enviar, AvanzarEtapa avanzar) {
        this.consultar = consultar;
        this.enviar = enviar;
        this.avanzar = avanzar;
    }

    @Override
    @PreAuthorize(
            "hasAnyAuthority('PERMISO_DISTRIBUTIVO:LECTURA', 'PERMISO_OFERTA:LECTURA',"
                    + " 'PERMISO_PROYECTOS:LECTURA')")
    public ResponseEntity<EstadoDelFlujo> consultarFlujo(String periodo) {
        // Al consultar, sin carrera indicada solo no ve acciones; el error es al enviar.
        return ResponseEntity.ok(respuesta(consultar.flujo(periodo, solicitante(true))));
    }

    @Override
    @PreAuthorize("hasAuthority('PERMISO_OFERTA:APROBACION')")
    public ResponseEntity<EstadoDelFlujo> enviarParcial(String periodo, String carrera) {
        Solicitante quien = solicitante(false);
        return ResponseEntity.ok(
                respuesta(sinPermiso(() -> enviar.enviar(periodo, carrera, quien))));
    }

    @Override
    public ResponseEntity<EstadoDelFlujo> avanzarEtapa(String periodo) {
        Solicitante quien = solicitante(true);
        return ResponseEntity.ok(respuesta(sinPermiso(() -> avanzar.avanzar(periodo, quien))));
    }

    private static FlujoDelDistributivo sinPermiso(Supplier<FlujoDelDistributivo> caso) {
        try {
            return caso.get();
        } catch (RolDeOtraEtapa | ParcialDeOtraCarrera error) {
            throw new AccessDeniedException("No mueve esta etapa", error);
        }
    }

    // Quién es, sus roles y, si solo puede lo propio al enviar, su carrera (D-64).
    private static Solicitante solicitante(boolean sinCarreraEsVacia) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        List<String> autoridades =
                autenticacion.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();
        Set<String> roles =
                autoridades.stream()
                        .filter(una -> una.startsWith(ROL))
                        .map(una -> una.substring(ROL.length()))
                        .collect(Collectors.toSet());
        Optional<String> carrera;
        try {
            carrera = AlcanceDeCarrera.propia(autoridades, ENVIAR);
        } catch (CarreraSinIndicar sinCarrera) {
            if (!sinCarreraEsVacia) {
                throw sinCarrera;
            }
            carrera = Optional.of("");
        }
        return new Solicitante(autenticacion.getName(), roles, carrera);
    }

    private static EstadoDelFlujo respuesta(FlujoDelDistributivo flujo) {
        return new EstadoDelFlujo(
                flujo.periodo(),
                TipoDeEtapa.fromValue(flujo.etapa().name()),
                flujo.nombreDeEtapa(),
                flujo.version(),
                flujo.parciales().stream().map(FlujoControlador::parcial).toList(),
                flujo.acciones().stream()
                        .map(accion -> AccionDelFlujo.fromValue(accion.name()))
                        .toList(),
                flujo.transiciones().stream().map(FlujoControlador::transicion).toList());
    }

    private static ParcialDelFlujo parcial(FlujoDelDistributivo.Parcial parcial) {
        ParcialDelFlujo respuesta =
                new ParcialDelFlujo(
                        parcial.carrera(), parcial.enviado(), parcial.pendienteDeConfirmar());
        parcial.enviadoPor().ifPresent(respuesta::enviadoPor);
        parcial.enviadoEn().ifPresent(en -> respuesta.enviadoEn(en.atOffset(ZoneOffset.UTC)));
        return respuesta;
    }

    private static TransicionDelFlujo transicion(Transicion transicion) {
        TransicionDelFlujo respuesta =
                new TransicionDelFlujo(
                        TransicionDelFlujo.TipoEnum.fromValue(transicion.tipo().name()),
                        TipoDeEtapa.fromValue(transicion.desde().name()),
                        TipoDeEtapa.fromValue(transicion.hacia().name()),
                        transicion.version(),
                        transicion.hash(),
                        transicion.quien(),
                        transicion.momento().atOffset(ZoneOffset.UTC));
        transicion.carrera().ifPresent(respuesta::carrera);
        return respuesta;
    }
}
