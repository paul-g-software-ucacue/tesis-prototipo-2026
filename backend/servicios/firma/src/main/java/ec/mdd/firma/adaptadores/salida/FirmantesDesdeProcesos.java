package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.FirmantesDelPeriodo;
import ec.mdd.plataforma.eventos.ConsultasEntreProcesos;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Quién firma en el periodo, por las rutas internas de otros procesos (principio V, ADR-023,
 * F13-H02, research §6; F14-H01, D-102): el nombre que se imprime y la persona que queda como
 * firmante esperado.
 *
 * <ul>
 *   <li>el Rector y el Vicerrector académico: su cargo vigente, con el nombre y la identificación
 *       de ese docente, en Docentes (proceso datos);
 *   <li>el Responsable de consolidación: su designación del periodo, con su usuario y su nombre, en
 *       Roles (proceso identidad), porque no es un cargo de la carga de autoridades (ADR-003).
 * </ul>
 *
 * <p>Un rol sin titular o sin nombre no está en la respuesta: el dominio falla con su cargo. Solo
 * se consulta lo que pide la cadena.
 */
public final class FirmantesDesdeProcesos implements FirmantesDelPeriodo {

    private static final Set<String> CARGOS_DE_AUTORIDAD = Set.of("RECTOR", "VICERRECTOR");
    private static final String RESPONSABLE = "RESPONSABLE_CONSOLIDACION";

    private final ConsultasEntreProcesos consultas;
    private final URI datos;
    private final URI identidad;

    /**
     * @param consultas el cliente con token de proceso
     * @param datos la dirección del proceso datos
     * @param identidad la dirección del proceso identidad
     */
    public FirmantesDesdeProcesos(ConsultasEntreProcesos consultas, URI datos, URI identidad) {
        this.consultas = consultas;
        this.datos = datos;
        this.identidad = identidad;
    }

    @Override
    public Map<String, FirmanteDelPeriodo> firmantes(String periodo, Set<String> roles) {
        Map<String, FirmanteDelPeriodo> firmantes = new HashMap<>();
        if (roles.stream().anyMatch(CARGOS_DE_AUTORIDAD::contains)) {
            firmantes.putAll(autoridades(roles));
        }
        if (roles.contains(RESPONSABLE)) {
            consultas
                    .obtener(
                            ruta(identidad, "roles", "periodos", periodo, "responsable"),
                            Responsable.class)
                    .filter(responsable -> !responsable.nombre().isBlank())
                    .ifPresent(
                            responsable ->
                                    firmantes.put(
                                            RESPONSABLE,
                                            FirmanteDelPeriodo.porUsuario(
                                                    responsable.nombre(), responsable.usuario())));
        }
        return Map.copyOf(firmantes);
    }

    // El docente de cada cargo de autoridad vigente, por su nombre entre los activos y con su
    // identificación, que reconoce a la cuenta firmante (D-102).
    private Map<String, FirmanteDelPeriodo> autoridades(Set<String> roles) {
        List<Cargo> cargos =
                consultas
                        .obtener(ruta(datos, "docentes", "cargos-vigentes"), Cargo[].class)
                        .map(List::of)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Docentes no publica los cargos vigentes"));
        Map<String, String> activos =
                consultas
                        .obtener(ruta(datos, "docentes", "activos"), Activo[].class)
                        .map(List::of)
                        .orElseThrow(
                                () -> new IllegalStateException("Docentes no publica los activos"))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Activo::identificacion,
                                        Activo::nombre,
                                        (una, otra) -> una));
        Map<String, FirmanteDelPeriodo> firmantes = new HashMap<>();
        cargos.stream()
                .filter(cargo -> roles.contains(cargo.tipo()))
                .forEach(
                        cargo -> {
                            String nombre = activos.get(cargo.docente());
                            if (nombre != null && !nombre.isBlank()) {
                                firmantes.putIfAbsent(
                                        cargo.tipo(),
                                        FirmanteDelPeriodo.porIdentificacion(
                                                nombre, cargo.docente()));
                            }
                        });
        return firmantes;
    }

    private static URI ruta(URI base, String... segmentos) {
        return UriComponentsBuilder.fromUri(base)
                .pathSegment("interno")
                .pathSegment(segmentos)
                .build()
                .encode()
                .toUri();
    }

    /** Un cargo vigente de Docentes. */
    record Cargo(String docente, String tipo, String carrera) {}

    /** Un docente activo de Docentes: solo su identificación y su nombre. */
    record Activo(String identificacion, String nombre) {}

    /** El Responsable del periodo, de Roles. */
    record Responsable(String usuario, String nombre) {}
}
