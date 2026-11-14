package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.net.URI;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * La identificación de una cuenta, por la ruta interna de Usuarios en el proceso identidad
 * (ADR-023, F14-H01): {@code /interno/usuarios/{usuario}/identificacion}, con token de proceso. Un
 * 404 (sin cuenta o sin identificación) es vacío; otro error se propaga, para no tomar un fallo de
 * Usuarios por «no es el firmante».
 */
public final class IdentificacionDesdeUsuarios implements IdentificacionDeUsuarios {

    private final ConsultasEntreProcesos consultas;
    private final URI identidad;

    /**
     * @param consultas el cliente con token de proceso
     * @param identidad la dirección del proceso identidad
     */
    public IdentificacionDesdeUsuarios(ConsultasEntreProcesos consultas, URI identidad) {
        this.consultas = consultas;
        this.identidad = identidad;
    }

    @Override
    public Optional<String> de(String usuario) {
        URI ruta =
                UriComponentsBuilder.fromUri(identidad)
                        .pathSegment("interno", "usuarios", usuario, "identificacion")
                        .build()
                        .encode()
                        .toUri();
        return consultas
                .obtener(ruta, Identificacion.class)
                .map(Identificacion::identificacion)
                .filter(identificacion -> !identificacion.isBlank());
    }

    /** La respuesta de Usuarios. */
    record Identificacion(String identificacion) {}
}
