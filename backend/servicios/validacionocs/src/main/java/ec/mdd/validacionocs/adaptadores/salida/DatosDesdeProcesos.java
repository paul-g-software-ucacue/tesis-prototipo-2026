package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pide a Académico (proceso datos) y a Roles (proceso identidad) lo que necesita de un periodo, por
 * sus rutas internas (principio V, ADR-023).
 */
public final class DatosDesdeProcesos implements DatosDelPeriodo {

    private final ConsultasEntreProcesos consultas;
    private final URI datos;
    private final URI identidad;

    /**
     * @param consultas el cliente con token de proceso
     * @param datos la dirección del proceso datos
     * @param identidad la dirección del proceso identidad
     */
    public DatosDesdeProcesos(ConsultasEntreProcesos consultas, URI datos, URI identidad) {
        this.consultas = consultas;
        this.datos = datos;
        this.identidad = identidad;
    }

    @Override
    public Optional<EstadoDelPeriodo> estado(String periodo) {
        URI ruta =
                UriComponentsBuilder.fromUri(datos)
                        .pathSegment("interno", "academico", "periodos", periodo, "estado")
                        .build()
                        .encode()
                        .toUri();
        return consultas
                .obtener(ruta, RespuestaEstado.class)
                .map(respuesta -> new EstadoDelPeriodo(respuesta.estado()));
    }

    @Override
    public Firmantes firmantes(String periodo) {
        URI ruta =
                UriComponentsBuilder.fromUri(identidad)
                        .pathSegment("interno", "roles", "firmantes")
                        .queryParam("periodo", periodo)
                        .build()
                        .encode()
                        .toUri();
        // La ruta responde siempre: si no está, es un error de despliegue, no un periodo sin roles.
        RespuestaFirmantes respuesta =
                consultas
                        .obtener(ruta, RespuestaFirmantes.class)
                        .orElseThrow(() -> new IllegalStateException("Roles no tiene firmantes"));
        return new Firmantes(
                Set.copyOf(respuesta.rolesConUsuario()), respuesta.responsableDesignado());
    }

    /** La respuesta de Académico. */
    record RespuestaEstado(String periodo, String estado) {}

    /** La respuesta de Roles. */
    record RespuestaFirmantes(List<String> rolesConUsuario, boolean responsableDesignado) {}
}
