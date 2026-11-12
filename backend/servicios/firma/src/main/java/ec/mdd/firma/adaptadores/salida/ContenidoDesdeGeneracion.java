package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.dominio.ContenidoDelDocumento;
import ec.mdd.firma.puertos.salida.ContenidoDelDistributivo;
import ec.mdd.generacion.puertos.entrada.ContenidoCongelado;
import ec.mdd.generacion.puertos.entrada.ContenidoDeLaVersion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El contenido de la versión congelada por la interfaz publicada de Generación en el mismo proceso
 * (principio V, F13-H02): lo pasa a los tipos de Firma, sin los firmantes. Cada asignatura nombra
 * su carrera por el nombre, como el resumen.
 */
public final class ContenidoDesdeGeneracion implements ContenidoDelDistributivo {

    private final ContenidoCongelado generacion;

    /** Recibe la interfaz publicada de Generación. */
    public ContenidoDesdeGeneracion(ContenidoCongelado generacion) {
        this.generacion = generacion;
    }

    @Override
    public ContenidoDelDocumento contenido(String periodo) {
        ContenidoDeLaVersion leido = generacion.contenido(periodo);
        Map<String, String> nombres =
                leido.carreras().stream()
                        .collect(
                                Collectors.toMap(
                                        ContenidoDeLaVersion.Carrera::codigo,
                                        ContenidoDeLaVersion.Carrera::nombre,
                                        (una, otra) -> una));
        return new ContenidoDelDocumento(
                leido.periodo(),
                leido.version(),
                leido.hash(),
                leido.carreras().stream()
                        .map(
                                una ->
                                        new ContenidoDelDocumento.Carrera(
                                                una.nombre(),
                                                una.estado(),
                                                una.paralelos(),
                                                una.propuestos(),
                                                horas(una.horas())))
                        .toList(),
                horas(leido.totales()),
                leido.docentes().stream().map(una -> docente(una, nombres)).toList(),
                List.of());
    }

    private static ContenidoDelDocumento.Docente docente(
            ContenidoDeLaVersion.Docente docente, Map<String, String> nombres) {
        return new ContenidoDelDocumento.Docente(
                docente.identificacion(),
                docente.nombre(),
                docente.asignaturas().stream()
                        .map(
                                una ->
                                        new ContenidoDelDocumento.Asignatura(
                                                una.codigo(),
                                                una.nombre(),
                                                una.paralelo(),
                                                nombres.getOrDefault(una.carrera(), una.carrera()),
                                                una.horas()))
                        .toList(),
                docente.actividades().stream()
                        .map(
                                una ->
                                        new ContenidoDelDocumento.Actividad(
                                                una.nombre(), una.componente(), una.horas()))
                        .toList(),
                horas(docente.horas()));
    }

    private static ContenidoDelDocumento.Horas horas(ContenidoDeLaVersion.Horas carga) {
        return new ContenidoDelDocumento.Horas(
                carga.clase(),
                carga.otrasDocencia(),
                carga.investigacion(),
                carga.vinculacion(),
                carga.gestion());
    }
}
