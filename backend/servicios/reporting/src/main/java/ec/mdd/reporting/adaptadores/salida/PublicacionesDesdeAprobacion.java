package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.reporting.dominio.Publicacion;
import ec.mdd.reporting.puertos.salida.PublicacionesDelDistributivo;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Las publicaciones vigentes por la ruta interna de Aprobación, en el proceso distributivo
 * (ADR-023, F20-H01): {@code /interno/validacionocs/publicaciones}.
 */
public final class PublicacionesDesdeAprobacion implements PublicacionesDelDistributivo {

    private final ConsultasEntreProcesos consultas;
    private final URI distributivo;

    /**
     * @param consultas el cliente con token de proceso
     * @param distributivo la dirección del proceso distributivo
     */
    public PublicacionesDesdeAprobacion(ConsultasEntreProcesos consultas, URI distributivo) {
        this.consultas = consultas;
        this.distributivo = distributivo;
    }

    @Override
    public List<Publicacion> vigentes() {
        URI ruta =
                UriComponentsBuilder.fromUri(distributivo)
                        .pathSegment("interno", "validacionocs", "publicaciones")
                        .build()
                        .toUri();
        return consultas
                .obtener(ruta, PublicacionVigente[].class)
                .map(List::of)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Aprobación no publica las publicaciones vigentes"))
                .stream()
                .map(
                        una ->
                                new Publicacion(
                                        una.periodo(),
                                        una.version(),
                                        una.hash(),
                                        una.publicadaEn(),
                                        una.publicadaPor(),
                                        una.enReforma()))
                .toList();
    }

    /** Una publicación vigente, como la responde Aprobación. */
    record PublicacionVigente(
            String periodo,
            int version,
            String hash,
            String publicadaPor,
            Instant publicadaEn,
            boolean enReforma) {}
}
