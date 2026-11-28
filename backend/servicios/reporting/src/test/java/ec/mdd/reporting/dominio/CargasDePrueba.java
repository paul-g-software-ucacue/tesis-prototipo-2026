package ec.mdd.reporting.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Contenido congelado completo para las pruebas del CSV para SIGA (F19-H01, D-119), armado a partir
 * de {@link DocentesDePrueba}: cada docente tiene una asignatura con sus horas de clase y una
 * actividad por cada otro componente con horas, así que sus filas suman su carga.
 */
public final class CargasDePrueba {

    private CargasDePrueba() {}

    /**
     * @param hash el hash de la versión
     * @param docentes los docentes, con sus horas
     * @return su contenido completo
     */
    public static CargasDeLaVersion de(String hash, List<DocentesDeLaVersion.Docente> docentes) {
        List<ContenidoDelDocente> cargas = new ArrayList<>();
        for (int i = 0; i < docentes.size(); i++) {
            cargas.add(contenido(hash, docentes.get(i), i));
        }
        return new CargasDeLaVersion(hash, cargas);
    }

    /**
     * @param hash el hash de la versión
     * @param docente el docente
     * @param indice para los códigos de su asignatura
     * @return su contenido
     */
    public static ContenidoDelDocente contenido(
            String hash, DocentesDeLaVersion.Docente docente, int indice) {
        HorasPorComponente horas = docente.horas();
        List<FilaDeAsignatura> asignaturas = new ArrayList<>();
        if (horas.de(Componente.CLASE).signum() > 0) {
            asignaturas.add(
                    new FilaDeAsignatura(
                            "Software",
                            "ASG-" + (100 + indice % 50),
                            "Asignatura",
                            "A",
                            horas.de(Componente.CLASE),
                            "TDS"));
        }
        List<FilaDeActividad> actividades = new ArrayList<>();
        for (Componente componente : Componente.values()) {
            BigDecimal deEste = horas.de(componente);
            if (componente != Componente.CLASE && deEste.signum() > 0) {
                actividades.add(
                        new FilaDeActividad(
                                componente,
                                codigo(componente),
                                "Actividad",
                                "TDS",
                                deEste,
                                "TDS",
                                componente == Componente.INVESTIGACION ? "INV-01" : null));
            }
        }
        return new ContenidoDelDocente(
                hash,
                new DatosDelDocente(
                        docente.identificacion(),
                        docente.nombre(),
                        docente.dedicacion().orElse(null),
                        null,
                        TipoDeIdentificacion.CEDULA),
                asignaturas,
                actividades,
                horas);
    }

    private static String codigo(Componente componente) {
        return switch (componente) {
            case CLASE -> throw new IllegalArgumentException("la clase no es una actividad");
            case OTRAS_DOCENCIA -> "140-b";
            case INVESTIGACION -> "141-a";
            case VINCULACION -> "142-a";
            case GESTION -> "143-a";
        };
    }
}
