package ec.mdd.generacion.adaptadores.entrada;

import ec.mdd.generacion.adaptadores.entrada.api.Distributivo;
import ec.mdd.generacion.adaptadores.entrada.api.DistributivoApi;
import ec.mdd.generacion.adaptadores.entrada.api.EstadoDelParcial;
import ec.mdd.generacion.adaptadores.entrada.api.ParcialDeCarrera;
import ec.mdd.generacion.adaptadores.entrada.api.Parciales;
import ec.mdd.generacion.adaptadores.entrada.api.Propuesta;
import ec.mdd.generacion.adaptadores.entrada.api.PropuestaNueva;
import ec.mdd.generacion.adaptadores.entrada.api.TipoDeJustificacion;
import ec.mdd.generacion.dominio.JustificacionObligatoria;
import ec.mdd.generacion.puertos.entrada.ConsultarDistributivo;
import ec.mdd.generacion.puertos.entrada.ConsultarParciales;
import ec.mdd.generacion.puertos.entrada.CrearDistributivo;
import ec.mdd.generacion.puertos.entrada.DistributivoDelPeriodo;
import ec.mdd.generacion.puertos.entrada.OtraCarrera;
import ec.mdd.generacion.puertos.entrada.ParcialConAvance;
import ec.mdd.generacion.puertos.entrada.ProponerDocente;
import ec.mdd.generacion.puertos.entrada.PropuestaVista;
import ec.mdd.generacion.puertos.entrada.SolicitudDePropuesta;
import ec.mdd.plataforma.errores.nucleo.AlcanceDeCarrera;
import ec.mdd.plataforma.errores.nucleo.DetalleRespuesta;
import ec.mdd.plataforma.errores.nucleo.SolicitudInvalida;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * El distributivo, los parciales y las propuestas ({@code contratos/generacion/openapi.yaml},
 * RF-24, RF-56). Crea quien escribe el distributivo; propone quien escribe la oferta, y su alcance
 * se mira sobre ese permiso, no sobre los de lectura (D-64). {@code PermisosGeneracionTest} lo
 * comprueba. Los modelos del contrato se llaman como los del dominio: los del dominio van con su
 * nombre completo.
 */
@RestController
class DistributivoControlador implements DistributivoApi {

    private static final String ESCRIBE_LA_PROPUESTA = "OFERTA:ESCRITURA";
    private static final String CREAR = "hasAuthority('PERMISO_DISTRIBUTIVO:ESCRITURA')";
    private static final String CONSULTAR =
            "hasAnyAuthority('PERMISO_DISTRIBUTIVO:LECTURA', 'PERMISO_CARGAS:LECTURA')";
    private static final String LEER_PARCIALES =
            "hasAnyAuthority('PERMISO_OFERTA:LECTURA', 'PERMISO_DISTRIBUTIVO:LECTURA',"
                    + " 'PERMISO_CATALOGOS:LECTURA')";
    private static final String PROPONER = "hasAuthority('PERMISO_OFERTA:ESCRITURA')";

    private final CrearDistributivo crear;
    private final ConsultarDistributivo consultar;
    private final ProponerDocente proponer;
    private final ConsultarParciales parciales;

    DistributivoControlador(
            CrearDistributivo crear,
            ConsultarDistributivo consultar,
            ProponerDocente proponer,
            ConsultarParciales parciales) {
        this.crear = crear;
        this.consultar = consultar;
        this.proponer = proponer;
        this.parciales = parciales;
    }

    @Override
    @PreAuthorize(CREAR)
    public ResponseEntity<Distributivo> crearDistributivo(String periodo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(distributivo(crear.crear(periodo, autenticacion().getName())));
    }

    @Override
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<Distributivo> consultarDistributivo(String periodo) {
        return ResponseEntity.ok(distributivo(consultar.distributivo(periodo)));
    }

    @Override
    @PreAuthorize(LEER_PARCIALES)
    public ResponseEntity<Parciales> consultarParciales(String periodo) {
        ec.mdd.generacion.puertos.entrada.ParcialesDelPeriodo delPeriodo =
                parciales.parciales(periodo, AfinidadControlador.carreraPropia());
        return ResponseEntity.ok(
                new Parciales(
                        delPeriodo.creado(),
                        delPeriodo.parciales().stream()
                                .map(DistributivoControlador::parcial)
                                .toList()));
    }

    @Override
    @PreAuthorize(PROPONER)
    public ResponseEntity<Propuesta> proponerDocente(
            String periodo, String asignatura, String paralelo, PropuestaNueva nueva) {
        try {
            return ResponseEntity.ok(
                    propuesta(
                            proponer.proponer(
                                    new SolicitudDePropuesta(
                                            periodo,
                                            asignatura,
                                            paralelo,
                                            nueva.getDocente(),
                                            nueva.getTexto(),
                                            carreraDeQuienEscribe(),
                                            autenticacion().getName()))));
        } catch (OtraCarrera error) {
            throw new AccessDeniedException("La asignatura-paralelo es de otra carrera", error);
        } catch (JustificacionObligatoria error) {
            throw new SolicitudInvalida(
                    List.of(
                            new DetalleRespuesta(
                                    "texto",
                                    "Escribe por qué propones a este docente, en 10 a 500"
                                            + " caracteres.")));
        }
    }

    @Override
    @PreAuthorize(PROPONER)
    public ResponseEntity<Void> quitarPropuesta(
            String periodo, String asignatura, String paralelo) {
        try {
            proponer.quitar(
                    periodo,
                    asignatura,
                    paralelo,
                    carreraDeQuienEscribe(),
                    autenticacion().getName());
        } catch (OtraCarrera error) {
            throw new AccessDeniedException("La asignatura-paralelo es de otra carrera", error);
        }
        return ResponseEntity.noContent().build();
    }

    /** La carrera a la que alcanza quien escribe; vacío si escribe sobre todo (D-64). */
    static Optional<String> carreraDeQuienEscribe() {
        return AlcanceDeCarrera.propia(
                autenticacion().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList(),
                ESCRIBE_LA_PROPUESTA);
    }

    /** Quien hace la petición. */
    static Authentication autenticacion() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static Distributivo distributivo(DistributivoDelPeriodo delPeriodo) {
        return new Distributivo(delPeriodo.periodo(), delPeriodo.version(), delPeriodo.carreras());
    }

    private static ParcialDeCarrera parcial(ParcialConAvance uno) {
        return new ParcialDeCarrera(
                uno.parcial().carrera(),
                EstadoDelParcial.fromValue(uno.parcial().estado().name()),
                uno.avance().ofertadas(),
                uno.avance().conPropuesta(),
                uno.avance().completo(),
                uno.propuestas().stream().map(DistributivoControlador::propuesta).toList());
    }

    /** La propuesta en el contrato; también la usa la sugerencia. */
    static Propuesta propuesta(PropuestaVista vista) {
        ec.mdd.generacion.dominio.Propuesta una = vista.propuesta();
        Propuesta respuesta =
                new Propuesta(
                        una.asignatura(),
                        una.paralelo(),
                        una.docente(),
                        una.nombreDocente(),
                        TipoDeJustificacion.fromValue(una.justificacion().tipo().name()),
                        vista.retirada(),
                        vista.horasSuficientes());
        respuesta.setTexto(una.justificacion().texto());
        return respuesta;
    }
}
