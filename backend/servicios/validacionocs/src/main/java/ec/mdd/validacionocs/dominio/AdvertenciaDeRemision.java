package ec.mdd.validacionocs.dominio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * La advertencia de RN-23 al cargar la configuración del flujo (F12-H03, D-114; POL 5.2: el
 * distributivo aprobado por el OCS se remite al menos N días antes del inicio del periodo).
 *
 * <p>El último plazo es el día de la carga más la suma de los plazos de las etapas activas hasta
 * APROBADO, como si el flujo arrancara ese día (aclaración del 2026-11-20); una devolución o una
 * reforma pueden alargarlo, y eso no se estima. Si queda después del inicio del periodo menos los
 * días de la normativa, advierte; sin versión de normativa, dice que no se pudo comprobar. Nunca
 * rechaza la carga, y se guarda con ella para verla al consultarla.
 *
 * @param codigo el código estable de la advertencia
 * @param ultimoPlazo el día estimado de la aprobación
 * @param inicioDelPeriodo el primer día del periodo
 * @param diasAntesDelInicio los días de RN-23 de la normativa; vacío sin normativa
 * @param fechaDeRemision el inicio menos esos días; vacío sin normativa
 * @param diasDeMargen la fecha de remisión menos el último plazo, negativo; vacío sin normativa
 */
public record AdvertenciaDeRemision(
        CodigoDeAdvertencia codigo,
        LocalDate ultimoPlazo,
        LocalDate inicioDelPeriodo,
        OptionalInt diasAntesDelInicio,
        Optional<LocalDate> fechaDeRemision,
        OptionalInt diasDeMargen) {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    /** Exige todo, y las cifras de la normativa solo con el margen insuficiente. */
    public AdvertenciaDeRemision {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(ultimoPlazo, "ultimoPlazo");
        Objects.requireNonNull(inicioDelPeriodo, "inicioDelPeriodo");
        Objects.requireNonNull(diasAntesDelInicio, "diasAntesDelInicio");
        Objects.requireNonNull(fechaDeRemision, "fechaDeRemision");
        Objects.requireNonNull(diasDeMargen, "diasDeMargen");
        boolean conNormativa = codigo == CodigoDeAdvertencia.RN23_MARGEN_INSUFICIENTE;
        if (conNormativa
                != (diasAntesDelInicio.isPresent()
                        && fechaDeRemision.isPresent()
                        && diasDeMargen.isPresent())) {
            throw new IllegalArgumentException(
                    "Solo el margen insuficiente lleva las cifras de la normativa, y siempre");
        }
    }

    /**
     * @param hoy el día de la carga, en Ecuador
     * @param plazos los plazos de la configuración que se carga
     * @param inicio el primer día del periodo
     * @param dias los días de RN-23 de la normativa del periodo; vacío si no tiene versión
     * @return la advertencia, o vacío si el último plazo cae en la fecha de RN-23 o antes
     */
    public static Optional<AdvertenciaDeRemision> de(
            LocalDate hoy, PlazosDeEtapas plazos, LocalDate inicio, OptionalInt dias) {
        Objects.requireNonNull(inicio, "inicio");
        LocalDate ultimo = hoy.plusDays(plazos.sumaHastaAprobado());
        if (dias.isEmpty()) {
            return Optional.of(
                    new AdvertenciaDeRemision(
                            CodigoDeAdvertencia.RN23_SIN_NORMATIVA,
                            ultimo,
                            inicio,
                            OptionalInt.empty(),
                            Optional.empty(),
                            OptionalInt.empty()));
        }
        LocalDate remision = inicio.minusDays(dias.getAsInt());
        int margen = (int) ChronoUnit.DAYS.between(ultimo, remision);
        return margen >= 0
                ? Optional.empty()
                : Optional.of(
                        new AdvertenciaDeRemision(
                                CodigoDeAdvertencia.RN23_MARGEN_INSUFICIENTE,
                                ultimo,
                                inicio,
                                dias,
                                Optional.of(remision),
                                OptionalInt.of(margen)));
    }

    /** El texto para el administrador, en español, con qué hacer. */
    public String mensaje() {
        String aprobacion =
                "Con estos plazos, contados desde hoy, el distributivo se aprobaría el "
                        + FECHA.format(ultimoPlazo);
        if (codigo == CodigoDeAdvertencia.RN23_SIN_NORMATIVA) {
            return "El periodo no tiene versión de normativa, así que no se pudo comprobar RN-23. "
                    + aprobacion
                    + ", y el periodo empieza el "
                    + FECHA.format(inicioDelPeriodo)
                    + ". Elige la versión de normativa del periodo y vuelve a cargar la"
                    + " configuración para comprobarlo.";
        }
        int faltan = -diasDeMargen.getAsInt();
        return aprobacion
                + ", después del "
                + FECHA.format(fechaDeRemision.orElseThrow())
                + ", que es "
                + diasAntesDelInicio.getAsInt()
                + " días antes del inicio del periodo (RN-23). "
                + (faltan == 1 ? "Le falta 1 día" : "Le faltan " + faltan + " días")
                + ": acorta plazos o desactiva etapas opcionales.";
    }
}
