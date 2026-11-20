package ec.mdd.validacionocs.adaptadores.salida;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.validacionocs.dominio.EstadoDelPeriodo;
import ec.mdd.validacionocs.dominio.Firmantes;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.NombresDeCarreras;
import ec.mdd.validacionocs.puertos.salida.NormativaDelPeriodo;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pide a Académico (proceso datos) y a Roles (proceso identidad) lo que necesita de un periodo, a
 * Académico el nombre de las carreras y, desde F12-H03, a Normativa (también en el proceso datos)
 * los días de RN-23, por sus rutas internas (principio V, ADR-023).
 */
public final class DatosDesdeProcesos
        implements DatosDelPeriodo, NombresDeCarreras, NormativaDelPeriodo {

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
                .map(
                        respuesta ->
                                new EstadoDelPeriodo(
                                        respuesta.estado(),
                                        Optional.ofNullable(respuesta.inicio())));
    }

    // F12-H03 (D-114): un 404 es un periodo sin versión de normativa, no un error.
    @Override
    public OptionalInt diasDeRemision(String periodo) {
        URI ruta =
                UriComponentsBuilder.fromUri(datos)
                        .pathSegment("interno", "normativa", "periodos", periodo, "remision")
                        .build()
                        .encode()
                        .toUri();
        return consultas
                .obtener(ruta, RespuestaRemision.class)
                .map(respuesta -> OptionalInt.of(respuesta.diasAntesDelInicio()))
                .orElseGet(OptionalInt::empty);
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

    @Override
    public Map<String, String> nombres() {
        URI ruta =
                UriComponentsBuilder.fromUri(datos)
                        .pathSegment("interno", "academico", "carreras")
                        .build()
                        .toUri();
        return Arrays.stream(
                        consultas
                                .obtener(ruta, NombreDeCarrera[].class)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Académico no publica las carreras")))
                .collect(
                        Collectors.toUnmodifiableMap(
                                NombreDeCarrera::codigo, NombreDeCarrera::nombre, (a, b) -> a));
    }

    /** La respuesta de Académico. */
    record RespuestaEstado(String periodo, String estado, LocalDate inicio) {}

    /** La respuesta de Normativa. */
    record RespuestaRemision(String periodo, int version, int diasAntesDelInicio) {}

    /** Una carrera en la respuesta de Académico. */
    record NombreDeCarrera(String codigo, String nombre) {}

    /** La respuesta de Roles. */
    record RespuestaFirmantes(List<String> rolesConUsuario, boolean responsableDesignado) {}
}
