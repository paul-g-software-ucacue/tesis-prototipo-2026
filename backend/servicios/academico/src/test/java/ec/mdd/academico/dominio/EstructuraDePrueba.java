package ec.mdd.academico.dominio;

import static ec.mdd.academico.dominio.TipoDeUnidad.COMISION;
import static ec.mdd.academico.dominio.TipoDeUnidad.GESTION;
import static ec.mdd.academico.dominio.TipoDeUnidad.GOBIERNO;
import static ec.mdd.academico.dominio.TipoDeUnidad.SEDE_MATRIZ;
import static ec.mdd.academico.dominio.TipoDeUnidad.UNIDAD_ACADEMICA;

import java.util.List;

/**
 * La estructura de la carga 01 de los datos de prueba (24 unidades ficticias, V-02, D-07) y el
 * catálogo de las 21 gestiones autorizadas de POL 6, en el orden en que se registran.
 */
final class EstructuraDePrueba {

    static final List<String> GESTIONES =
            List.of(
                    "G01", "G02", "G03", "G04", "G05", "G06", "G07", "G08", "G09", "G10", "G11",
                    "G12", "G13", "G14", "G15", "G16", "G17", "G18", "G19", "G20", "G21");

    static final List<UnidadOrganica> UNIDADES =
            List.of(
                    nueva("MATRIZ", "Sede matriz", SEDE_MATRIZ, null, null, null),
                    nueva("OCS", "Órgano Colegiado Superior", GOBIERNO, "MATRIZ", null, null),
                    nueva("RECT", "Rectorado", GOBIERNO, "MATRIZ", "G01", null),
                    nueva("VRA", "Vicerrectorado Académico", GOBIERNO, "RECT", "G02", null),
                    nueva(
                            "CAI",
                            "Coordinación Académica Institucional",
                            UNIDAD_ACADEMICA,
                            "VRA",
                            null,
                            null),
                    carrera("CC-CONT", "Contabilidad"),
                    carrera("CC-DS", "Desarrollo De Software"),
                    carrera("CC-ELE", "Electricidad"),
                    carrera("CC-MA", "Mecanica Automotriz"),
                    carrera("CC-CYGOC", "Construccion Y Gestion De Obra Civil"),
                    carrera("CC-PP", "Produccion Pecuaria"),
                    carrera(
                            "CC-TSCIOR",
                            "Tecnologia Superior En Control De Incendios Y Operaciones De Rescate"),
                    gestion("G-VIN", "Gestión de Vinculación con la Sociedad", "VRA", "G06"),
                    gestion(
                            "G-INV",
                            "Gestión de Investigación, Desarrollo Tecnológico e Innovación",
                            "VRA",
                            "G07"),
                    gestion("G-CAL", "Coordinación de Aseguramiento de la Calidad", "RECT", "G10"),
                    gestion("G-SEC", "Secretaría General Académica", "RECT", "G11"),
                    gestion("G-BIE", "Gestión de Bienestar Institucional", "VRA", "G13"),
                    gestion("G-PLA", "Gestión de Planificación Estratégica", "RECT", "G15"),
                    gestion("G-BIB", "Unidad de Servicios de Biblioteca", "VRA", "G17"),
                    gestion("G-TIC", "Unidad de TIC", "VRA", "G20"),
                    gestion("G-EST", "Unidad de Estadísticas", "VRA", "G21"),
                    comision("COM-EVAL", "Comisión de Evaluación Interna", "G-CAL", true),
                    comision("COM-INV", "Comisión de Investigación", "G-INV", true),
                    comision(
                            "COM-DIST",
                            "Comisión de elaboración del distributivo 2026-I",
                            "VRA",
                            false));

    private EstructuraDePrueba() {}

    static UnidadOrganica nueva(
            String codigo,
            String nombre,
            TipoDeUnidad tipo,
            String padre,
            String gestion,
            Boolean permanente) {
        return UnidadOrganica.nueva(codigo, nombre, tipo, padre, gestion, permanente);
    }

    static UnidadOrganica gestion(String codigo, String nombre, String padre, String gestion) {
        return nueva(codigo, nombre, GESTION, padre, gestion, null);
    }

    static UnidadOrganica comision(String codigo, String nombre, String padre, boolean permanente) {
        return nueva(codigo, nombre, COMISION, padre, null, permanente);
    }

    private static UnidadOrganica carrera(String codigo, String carrera) {
        return nueva(
                codigo,
                "Coordinación de la carrera de " + carrera,
                UNIDAD_ACADEMICA,
                "CAI",
                null,
                null);
    }

    /** Las 24 unidades registradas, una por una, sobre una estructura vacía. */
    static EstructuraOrganica completa() {
        EstructuraOrganica estructura = new EstructuraOrganica(List.of(), GESTIONES);
        for (UnidadOrganica unidad : UNIDADES) {
            estructura = estructura.con(estructura.registrar(unidad));
        }
        return estructura;
    }
}
