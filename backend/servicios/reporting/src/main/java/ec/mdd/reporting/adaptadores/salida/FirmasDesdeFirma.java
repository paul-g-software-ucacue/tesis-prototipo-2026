package ec.mdd.reporting.adaptadores.salida;

import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import ec.mdd.plataforma.eventos.ProcesoConError;
import ec.mdd.reporting.dominio.FirmasDeLaVersion;
import ec.mdd.reporting.puertos.salida.FirmasDeLasVersiones;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * La cadena de firmas por la ruta interna de Firma, en el proceso distributivo (ADR-023, F19-H02,
 * D-120): {@code /interno/firma/periodos/{periodo}/versiones/{numero}/firmas}. Un 404 es una
 * versión sin documento firmable.
 */
public final class FirmasDesdeFirma implements FirmasDeLasVersiones {

    private final ConsultasEntreProcesos consultas;
    private final URI distributivo;

    /**
     * @param consultas el cliente con token de proceso
     * @param distributivo la dirección del proceso distributivo
     */
    public FirmasDesdeFirma(ConsultasEntreProcesos consultas, URI distributivo) {
        this.consultas = consultas;
        this.distributivo = distributivo;
    }

    @Override
    public Optional<FirmasDeLaVersion> firmas(String periodo, int version) {
        return consultas
                .obtener(
                        UriComponentsBuilder.fromUri(distributivo)
                                .pathSegment(
                                        "interno",
                                        "firma",
                                        "periodos",
                                        periodo,
                                        "versiones",
                                        String.valueOf(version),
                                        "firmas")
                                .build()
                                .encode()
                                .toUri(),
                        Firmas.class)
                .map(FirmasDesdeFirma::firmas);
    }

    // Leído con su esquema (principio XIV): sin hash, sin pasos o con un paso sin estado no es una
    // respuesta válida de Firma, y no se debe tomar como una cadena completa ni vacía.
    private static FirmasDeLaVersion firmas(Firmas firmas) {
        if (firmas.hashContenido() == null
                || firmas.pasos() == null
                || firmas.anulada() == null
                || firmas.rechazada() == null
                || firmas.pasos().stream()
                        .anyMatch(paso -> paso.orden() == null || paso.estado() == null)) {
            throw new ProcesoConError(
                    "Firma respondió la cadena de una versión sin hash, pasos o estados.");
        }
        try {
            return new FirmasDeLaVersion(
                    firmas.hashContenido(),
                    firmas.pasos().stream()
                            .map(
                                    paso ->
                                            new FirmasDeLaVersion.PasoFirmado(
                                                    paso.orden(), "FIRMADO".equals(paso.estado())))
                            .toList(),
                    firmas.anulada(),
                    firmas.rechazada());
        } catch (IllegalArgumentException invalida) {
            throw new ProcesoConError("Firma respondió una cadena inválida.");
        }
    }

    /** La cadena, como la responde Firma. */
    record Firmas(
            String periodo,
            Integer version,
            String hashContenido,
            List<Paso> pasos,
            Boolean anulada,
            Boolean rechazada) {}

    /** Un paso, como lo responde Firma. */
    record Paso(Integer orden, String estado) {}
}
