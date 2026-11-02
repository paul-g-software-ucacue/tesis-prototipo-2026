package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.ActividadDelCatalogo;
import ec.mdd.normativa.dominio.Regla;
import ec.mdd.normativa.dominio.Severidad;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarReglasDelPanel;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.ReglasDelPanelDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioActividades;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lo que el panel de inconsistencias necesita de la versión de un periodo. Si falta un dato, falla:
 * no lo inventa (principio VI). El rango de RN-10 es el del Tipo 2, fijo en el prototipo.
 */
public final class ConsultarReglasDelPanelServicio implements ConsultarReglasDelPanel {

    private static final List<String> REGLAS =
            List.of("RF-24", "RN-02", "RN-05", "RN-10", "RN-12", "RN-17", "RN-18");

    private final RepositorioVersiones versiones;
    private final RepositorioActividades actividades;

    /** Recibe los repositorios. */
    public ConsultarReglasDelPanelServicio(
            RepositorioVersiones versiones, RepositorioActividades actividades) {
        this.versiones = versiones;
        this.actividades = actividades;
    }

    @Override
    public ReglasDelPanelDeLaVersion reglas(String periodo) {
        VersionNormativa version =
                versiones.porPeriodo(periodo).orElseThrow(() -> new PeriodoSinVersion(periodo));
        Map<String, Severidad> severidades = new LinkedHashMap<>();
        for (String regla : REGLAS) {
            severidades.put(
                    regla,
                    version.regla(regla)
                            .map(Regla::severidad)
                            .orElseThrow(() -> sinDato(version, "la severidad de " + regla)));
        }
        List<String> obligatorias =
                actividades.deLaVersion(version.numero()).stream()
                        .filter(ActividadDelCatalogo::obligatoria)
                        .map(ActividadDelCatalogo::codigo)
                        .toList();
        if (obligatorias.isEmpty()) {
            throw sinDato(version, "actividades obligatorias");
        }
        return new ReglasDelPanelDeLaVersion(
                version.numero(),
                severidades,
                cifra(version, "RN-02", "horas_maximas_docente"),
                cifra(version, "RN-05", "porcentaje_clase"),
                cifra(version, "RN-05", "porcentaje_otras_docencia"),
                cifra(version, "RN-05", "margen_puntos"),
                cifra(version, "RN-10", "t2_minimo").intValueExact(),
                cifra(version, "RN-10", "t2_maximo").intValueExact(),
                obligatorias);
    }

    private static BigDecimal cifra(VersionNormativa version, String regla, String parametro) {
        return version.regla(regla)
                .flatMap(una -> una.parametro(parametro))
                .orElseThrow(() -> sinDato(version, parametro));
    }

    private static IllegalStateException sinDato(VersionNormativa version, String falta) {
        return new IllegalStateException("La versión " + version.numero() + " no tiene " + falta);
    }
}
