package ec.mdd.docentes.dominio;

/**
 * Cargo de gestión de un docente (glosario «Cargo», carga 8): las autoridades, los coordinadores,
 * un gestor por cada gestión autorizada G06 a G21 de POL 6 (D-73) y el miembro de comisión, que no
 * tiene horas (C-12).
 */
public enum TipoDeCargo {
    RECTOR(Clase.AUTORIDAD, "G01"),
    VICERRECTOR(Clase.AUTORIDAD, "G02"),
    COORDINADOR_ACADEMICO_INSTITUCIONAL(Clase.COORDINADOR, "G03"),
    COORDINADOR_SEDE(Clase.COORDINADOR, "G04"),
    COORDINADOR_CARRERA(Clase.COORDINADOR, "G05"),
    G06_GESTOR_VINCULACION(Clase.GESTOR, "G06"),
    G07_GESTOR_INVESTIGACION(Clase.GESTOR, "G07"),
    G08_GESTOR_IDIOMAS(Clase.GESTOR, "G08"),
    G09_GESTOR_FORMACION_INTEGRAL(Clase.GESTOR, "G09"),
    G10_COORDINADOR_CALIDAD(Clase.GESTOR, "G10"),
    G11_SECRETARIO_GENERAL(Clase.GESTOR, "G11"),
    G12_PROCURADOR(Clase.GESTOR, "G12"),
    G13_GESTOR_BIENESTAR(Clase.GESTOR, "G13"),
    G14_GESTOR_ADMINISTRATIVO_FINANCIERO(Clase.GESTOR, "G14"),
    G15_GESTOR_PLANIFICACION(Clase.GESTOR, "G15"),
    G16_GESTOR_TALENTO_HUMANO(Clase.GESTOR, "G16"),
    G17_GESTOR_BIBLIOTECA(Clase.GESTOR, "G17"),
    G18_GESTOR_COMUNICACION(Clase.GESTOR, "G18"),
    G19_GESTOR_RELACIONES_INTERNACIONALES(Clase.GESTOR, "G19"),
    G20_GESTOR_TIC(Clase.GESTOR, "G20"),
    G21_GESTOR_ESTADISTICAS(Clase.GESTOR, "G21"),
    MIEMBRO_COMISION(Clase.COMISION, null);

    private enum Clase {
        AUTORIDAD,
        COORDINADOR,
        GESTOR,
        COMISION
    }

    private final Clase clase;
    private final String gestion;

    TipoDeCargo(Clase clase, String gestion) {
        this.clase = clase;
        this.gestion = gestion;
    }

    /**
     * La gestión autorizada de POL 6 del cargo, como {@code G07}; nula para el miembro de comisión.
     */
    public String gestion() {
        return gestion;
    }

    /** Si su unidad tiene que ser la de su gestión autorizada: los gestores G06 a G21 (D-73). */
    public boolean esGestor() {
        return clase == Clase.GESTOR;
    }

    /** Si es el miembro de una comisión, cuya unidad tiene que ser una comisión. */
    public boolean esDeComision() {
        return clase == Clase.COMISION;
    }

    /** Si coordina una carrera: su carrera es obligatoria y su unidad, opcional. */
    public boolean coordinaCarrera() {
        return this == COORDINADOR_CARRERA;
    }

    /**
     * Si su designación pide un título de cuarto nivel: los coordinadores y los gestores (RN-20).
     */
    public boolean exigeCuartoNivel() {
        return clase == Clase.COORDINADOR || clase == Clase.GESTOR;
    }
}
