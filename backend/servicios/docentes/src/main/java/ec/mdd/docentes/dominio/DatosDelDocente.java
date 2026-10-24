package ec.mdd.docentes.dominio;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lo que se corrige de un docente: sus datos personales y de contrato, su carrera base y su ficha
 * académica (glosario «Docente», RF-16, RF-17, carga 6). La identificación y el estado laboral van
 * aparte, en {@link Docente}: no se corrigen.
 *
 * <p>Cada campo se valida con el nombre del contrato. Las horas contratadas van de 1 a 40; que
 * cuadren con la dedicación (RN-01) es una cifra de la política, versionada en Normativa, y la
 * revisa la validación del distributivo con la normativa del periodo (principio VI). Tampoco se
 * revisa aquí que el ocasional sea solo TC o MT (POL 5.1) ni que el contrato cubra el periodo.
 *
 * @param primerApellido hasta 60 caracteres; se guarda en mayúsculas
 * @param segundoApellido igual, o {@code null}
 * @param primerNombre hasta 60 caracteres; se guarda en mayúsculas
 * @param segundoNombre igual, o {@code null}
 * @param correo el correo institucional, hasta 100; se guarda en minúsculas
 * @param telefono de 7 a 10 dígitos, o {@code null}
 * @param tipoPersonal titular, ocasional, invitado u honorario
 * @param categoria la del escalafón si es titular; si no, {@link CategoriaDeEscalafon#NO_APLICA}
 * @param dedicacion tiempo completo, medio tiempo o tiempo parcial
 * @param horasContratadas horas semanales, de 1 a 40
 * @param fechaIngreso el ingreso a la institución
 * @param inicioContrato el inicio del contrato o nombramiento vigente
 * @param finContrato posterior al inicio; obligatorio para un ocasional
 * @param condicionEspecial por omisión, {@link CondicionEspecial#NINGUNA}
 * @param carreraBase el código de la carrera a la que pertenece, de Académico
 * @param ficha su ficha académica
 */
public record DatosDelDocente(
        String primerApellido,
        String segundoApellido,
        String primerNombre,
        String segundoNombre,
        String correo,
        String telefono,
        TipoPersonal tipoPersonal,
        CategoriaDeEscalafon categoria,
        Dedicacion dedicacion,
        int horasContratadas,
        LocalDate fechaIngreso,
        LocalDate inicioContrato,
        LocalDate finContrato,
        CondicionEspecial condicionEspecial,
        String carreraBase,
        FichaAcademica ficha) {

    /** El largo máximo de cada nombre o apellido. */
    public static final int NOMBRE_MAXIMO = 60;

    /** El tope de las horas semanales contratadas (RCE). */
    public static final int HORAS_MAXIMAS = 40;

    private static final int CORREO_MAXIMO = 100;
    private static final Pattern NOMBRE = Pattern.compile("\\p{L}[\\p{L} '\\-]*");
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern TELEFONO = Pattern.compile("[0-9]{7,10}");
    private static final Pattern CARRERA = Pattern.compile("[0-9A-Z-]{1,20}");

    /** Valida cada campo y deja los nombres en mayúsculas y el correo en minúsculas. */
    public DatosDelDocente {
        primerApellido = nombre(primerApellido, "primerApellido", true);
        segundoApellido = nombre(segundoApellido, "segundoApellido", false);
        primerNombre = nombre(primerNombre, "primerNombre", true);
        segundoNombre = nombre(segundoNombre, "segundoNombre", false);
        correo = correo(correo);
        telefono = telefono(telefono);
        if (tipoPersonal == null) {
            throw new DatoInvalido("tipoPersonal", "Elige el tipo de personal");
        }
        if (categoria == null) {
            throw new DatoInvalido("categoriaEscalafon", "Elige la categoría de escalafón");
        }
        if (tipoPersonal == TipoPersonal.TITULAR && categoria == CategoriaDeEscalafon.NO_APLICA) {
            throw new DatoInvalido(
                    "categoriaEscalafon",
                    "Un titular tiene una categoría de escalafón: auxiliar, agregado o principal");
        }
        if (tipoPersonal != TipoPersonal.TITULAR && categoria != CategoriaDeEscalafon.NO_APLICA) {
            throw new DatoInvalido(
                    "categoriaEscalafon",
                    "Solo un titular tiene categoría de escalafón: «No aplica»");
        }
        if (dedicacion == null) {
            throw new DatoInvalido("dedicacion", "Elige la dedicación");
        }
        if (horasContratadas < 1 || horasContratadas > HORAS_MAXIMAS) {
            throw new DatoInvalido(
                    "horasContratadas",
                    "Las horas contratadas son enteras, de 1 a " + HORAS_MAXIMAS);
        }
        if (fechaIngreso == null) {
            throw new DatoInvalido("fechaIngreso", "Indica la fecha de ingreso");
        }
        if (inicioContrato == null) {
            throw new DatoInvalido("inicioContrato", "Indica el inicio del contrato");
        }
        if (finContrato == null && tipoPersonal == TipoPersonal.OCASIONAL) {
            throw new DatoInvalido(
                    "finContrato", "Un docente ocasional necesita el fin de su contrato");
        }
        if (finContrato != null && !finContrato.isAfter(inicioContrato)) {
            throw new DatoInvalido(
                    "finContrato", "El fin del contrato tiene que ser posterior a su inicio");
        }
        condicionEspecial =
                condicionEspecial == null ? CondicionEspecial.NINGUNA : condicionEspecial;
        carreraBase = carreraBase == null ? "" : carreraBase.strip();
        if (!CARRERA.matcher(carreraBase).matches()) {
            throw new DatoInvalido("carreraBase", "Elige la carrera base");
        }
        Objects.requireNonNull(ficha, "ficha");
    }

    /** Los apellidos y los nombres, como «PERALTA MALDONADO JUAN ANDRES». */
    public String nombreCompleto() {
        return Stream.of(primerApellido, segundoApellido, primerNombre, segundoNombre)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private static String nombre(String valor, String campo, boolean obligatorio) {
        String limpio = valor == null ? "" : valor.strip().toUpperCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            if (obligatorio) {
                throw new DatoInvalido(campo, "No puede estar en blanco");
            }
            return null;
        }
        if (limpio.length() > NOMBRE_MAXIMO) {
            throw new DatoInvalido(campo, "No puede tener más de " + NOMBRE_MAXIMO + " caracteres");
        }
        if (!NOMBRE.matcher(limpio).matches()) {
            throw new DatoInvalido(campo, "Solo admite letras, espacios, apóstrofo y guion");
        }
        return limpio;
    }

    private static String correo(String valor) {
        String limpio = valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
        if (limpio.length() > CORREO_MAXIMO || !CORREO.matcher(limpio).matches()) {
            throw new DatoInvalido(
                    "correoInstitucional",
                    "Escribe un correo como usuario@dominio, hasta "
                            + CORREO_MAXIMO
                            + " caracteres");
        }
        return limpio;
    }

    private static String telefono(String valor) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty()) {
            return null;
        }
        if (!TELEFONO.matcher(limpio).matches()) {
            throw new DatoInvalido("telefono", "El teléfono tiene de 7 a 10 dígitos");
        }
        return limpio;
    }
}
