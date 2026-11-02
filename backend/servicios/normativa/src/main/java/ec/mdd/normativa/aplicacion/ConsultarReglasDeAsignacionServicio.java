package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.FilaCargaHoraria;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.ConsultarReglasDeAsignacion;
import ec.mdd.normativa.puertos.entrada.PeriodoSinVersion;
import ec.mdd.normativa.puertos.entrada.ReglasDeLaVersion;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import java.math.BigDecimal;

/**
 * Las cifras de las reglas duras de un periodo: el tope de horas de RN-02 y el tope de clase de la
 * fila de quien tiene proyecto, que es donde la política guarda las 16 h de RN-13.
 */
public final class ConsultarReglasDeAsignacionServicio implements ConsultarReglasDeAsignacion {

    private static final String REGLA_DE_HORAS = "RN-02";
    private static final String PARAMETRO_DE_HORAS = "horas_maximas_docente";
    private static final String PERFIL_CON_PROYECTO = "DOCENTE_INVESTIGACION_VINCULACION";

    private final RepositorioVersiones repositorio;

    /** Recibe el repositorio de versiones. */
    public ConsultarReglasDeAsignacionServicio(RepositorioVersiones repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public ReglasDeLaVersion reglas(String periodo) {
        VersionNormativa version =
                repositorio.porPeriodo(periodo).orElseThrow(() -> new PeriodoSinVersion(periodo));
        BigDecimal horas =
                version.regla(REGLA_DE_HORAS)
                        .flatMap(regla -> regla.parametro(PARAMETRO_DE_HORAS))
                        .orElseThrow(() -> sinCifra(version, PARAMETRO_DE_HORAS));
        int clase =
                version.tabla().filas().stream()
                        .filter(fila -> PERFIL_CON_PROYECTO.equals(fila.perfil()))
                        .findFirst()
                        .map(FilaCargaHoraria::horasClaseMaximo)
                        .orElseThrow(() -> sinCifra(version, PERFIL_CON_PROYECTO));
        return new ReglasDeLaVersion(version.numero(), horas, BigDecimal.valueOf(clase));
    }

    private static IllegalStateException sinCifra(VersionNormativa version, String falta) {
        return new IllegalStateException("La versión " + version.numero() + " no tiene " + falta);
    }
}
