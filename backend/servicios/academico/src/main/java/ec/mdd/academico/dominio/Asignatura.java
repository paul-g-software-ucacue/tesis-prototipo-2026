package ec.mdd.academico.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Una materia de una malla con su código, ciclo, unidad de organización curricular, horas, campo de
 * conocimiento y palabras clave (glosario «Asignatura», RF-14, carga 4). El campo, las palabras
 * clave y el tronco común son los datos para la afinidad (RN-19, RN-24). Las reglas que miran a su
 * carrera y su malla están en {@link DatosMaestros}.
 *
 * @param codigo como {@code DS-P-II-POO-204}; no cambia
 * @param carrera el código de su carrera; no cambia
 * @param malla el código de versión de su malla, de la misma carrera; no cambia
 * @param nombre hasta 150 caracteres
 * @param ciclo de 1 a 10, sin pasar el número de ciclos de su carrera
 * @param unidadOrganizacionCurricular básica, profesional o de integración curricular
 * @param horasSemanalesClase de 0,5 a 20, con dos decimales como máximo
 * @param horasPracticasSemanales de 0 a 20, o {@code null}
 * @param creditos mayores que 0, o {@code null}
 * @param troncoComun si queda exenta del mínimo de afinidad (glosario «Tronco común»)
 * @param campo su campo amplio y específico
 * @param palabrasClave al menos dos, distintas, sin espacios en los extremos
 * @param activa si está activa
 */
public record Asignatura(
        String codigo,
        String carrera,
        String malla,
        String nombre,
        int ciclo,
        UnidadDeOrganizacionCurricular unidadOrganizacionCurricular,
        BigDecimal horasSemanalesClase,
        BigDecimal horasPracticasSemanales,
        BigDecimal creditos,
        boolean troncoComun,
        CampoConocimiento campo,
        List<String> palabrasClave,
        boolean activa) {

    /** El largo máximo del nombre. */
    public static final int NOMBRE_MAXIMO = 150;

    // Los formatos y los rangos del diccionario de la carga 04.
    private static final Pattern CODIGO = Pattern.compile("[0-9A-Z-]{1,30}");
    private static final BigDecimal MINIMO_DE_CLASE = new BigDecimal("0.5");
    private static final BigDecimal MAXIMO_DE_HORAS = new BigDecimal("20");
    private static final int PALABRAS_MINIMAS = 2;

    /** Valida sus reglas y quita los espacios de los extremos del nombre y de las palabras. */
    public Asignatura {
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new DatoInvalido(
                    "codigo",
                    "El código solo admite mayúsculas, números y guion, hasta 30 caracteres");
        }
        if (carrera == null || carrera.isBlank()) {
            throw new DatoInvalido("carrera", "Elige la carrera de la asignatura");
        }
        if (malla == null || malla.isBlank()) {
            throw new DatoInvalido("malla", "Elige la malla de la asignatura");
        }
        nombre = Textos.nombre(nombre, NOMBRE_MAXIMO);
        if (ciclo < 1 || ciclo > Carrera.CICLOS_MAXIMO) {
            throw new DatoInvalido("ciclo", "El ciclo va de 1 a " + Carrera.CICLOS_MAXIMO);
        }
        Objects.requireNonNull(unidadOrganizacionCurricular, "unidadOrganizacionCurricular");
        if (horasSemanalesClase == null) {
            throw new DatoInvalido("horasSemanalesClase", "Indica las horas semanales de clase");
        }
        if (horasSemanalesClase.compareTo(MINIMO_DE_CLASE) < 0
                || horasSemanalesClase.compareTo(MAXIMO_DE_HORAS) > 0
                || horasSemanalesClase.stripTrailingZeros().scale() > 2) {
            throw new DatoInvalido(
                    "horasSemanalesClase",
                    "Las horas semanales de clase van de 0,5 a 20, con dos decimales como máximo");
        }
        if (horasPracticasSemanales != null
                && (horasPracticasSemanales.signum() < 0
                        || horasPracticasSemanales.compareTo(MAXIMO_DE_HORAS) > 0)) {
            throw new DatoInvalido(
                    "horasPracticasSemanales", "Las horas semanales de prácticas van de 0 a 20");
        }
        if (creditos != null && creditos.signum() <= 0) {
            throw new DatoInvalido("creditos", "Los créditos tienen que ser mayores que 0");
        }
        Objects.requireNonNull(campo, "campo");
        palabrasClave = palabras(palabrasClave);
    }

    /** Una asignatura recién registrada, activa. */
    public static Asignatura nueva(
            String codigo,
            String carrera,
            String malla,
            String nombre,
            int ciclo,
            UnidadDeOrganizacionCurricular unidadOrganizacionCurricular,
            BigDecimal horasSemanalesClase,
            BigDecimal horasPracticasSemanales,
            BigDecimal creditos,
            boolean troncoComun,
            CampoConocimiento campo,
            List<String> palabrasClave) {
        return new Asignatura(
                codigo,
                carrera,
                malla,
                nombre,
                ciclo,
                unidadOrganizacionCurricular,
                horasSemanalesClase,
                horasPracticasSemanales,
                creditos,
                troncoComun,
                campo,
                palabrasClave,
                true);
    }

    /**
     * La asignatura con sus datos corregidos; el código, la carrera, la malla y el estado no
     * cambian.
     *
     * @return la asignatura corregida y lo que cambió; sin cambios, la lista viene vacía
     * @throws DatoInvalido si un dato nuevo no cumple su regla
     */
    public Correccion corregida(
            String nombre,
            int ciclo,
            UnidadDeOrganizacionCurricular unidadOrganizacionCurricular,
            BigDecimal horasSemanalesClase,
            BigDecimal horasPracticasSemanales,
            BigDecimal creditos,
            boolean troncoComun,
            CampoConocimiento campo,
            List<String> palabrasClave) {
        Asignatura corregida =
                new Asignatura(
                        codigo,
                        carrera,
                        malla,
                        nombre,
                        ciclo,
                        unidadOrganizacionCurricular,
                        horasSemanalesClase,
                        horasPracticasSemanales,
                        creditos,
                        troncoComun,
                        campo,
                        palabrasClave,
                        activa);
        List<CambioDeValor> cambios = new ArrayList<>();
        Cambios.agregar(cambios, "nombre", this.nombre, corregida.nombre);
        Cambios.agregar(cambios, "ciclo", this.ciclo, corregida.ciclo);
        Cambios.agregar(
                cambios,
                "unidadOrganizacionCurricular",
                this.unidadOrganizacionCurricular,
                corregida.unidadOrganizacionCurricular);
        agregarNumero(
                cambios,
                "horasSemanalesClase",
                this.horasSemanalesClase,
                corregida.horasSemanalesClase);
        agregarNumero(
                cambios,
                "horasPracticasSemanales",
                this.horasPracticasSemanales,
                corregida.horasPracticasSemanales);
        agregarNumero(cambios, "creditos", this.creditos, corregida.creditos);
        Cambios.agregar(cambios, "troncoComun", this.troncoComun, corregida.troncoComun);
        Cambios.agregar(cambios, "campoAmplio", this.campo.amplio(), corregida.campo.amplio());
        Cambios.agregar(
                cambios, "campoEspecifico", this.campo.especifico(), corregida.campo.especifico());
        Cambios.agregar(cambios, "palabrasClave", this.palabrasClave, corregida.palabrasClave);
        return new Correccion(corregida, cambios);
    }

    /** La asignatura activa o inactiva; los demás datos no cambian. */
    public Asignatura conEstado(boolean activa) {
        return new Asignatura(
                codigo,
                carrera,
                malla,
                nombre,
                ciclo,
                unidadOrganizacionCurricular,
                horasSemanalesClase,
                horasPracticasSemanales,
                creditos,
                troncoComun,
                campo,
                palabrasClave,
                activa);
    }

    // 3 y 3.00 son el mismo número de horas: no es un cambio.
    private static void agregarNumero(
            List<CambioDeValor> cambios, String campo, BigDecimal anterior, BigDecimal nuevo) {
        boolean iguales =
                anterior == null ? nuevo == null : nuevo != null && anterior.compareTo(nuevo) == 0;
        if (!iguales) {
            Cambios.agregar(cambios, campo, anterior, nuevo);
        }
    }

    private static List<String> palabras(List<String> palabras) {
        if (palabras == null) {
            throw new DatoInvalido("palabrasClave", "Indica al menos dos palabras clave");
        }
        List<String> limpias = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (String palabra : palabras) {
            String limpia = palabra == null ? "" : palabra.strip();
            if (limpia.isEmpty()) {
                throw new DatoInvalido("palabrasClave", "Una palabra clave no puede estar vacía");
            }
            if (!vistas.add(limpia.toLowerCase(Locale.ROOT))) {
                throw new DatoInvalido(
                        "palabrasClave", "La palabra clave «" + limpia + "» se repite");
            }
            limpias.add(limpia);
        }
        if (limpias.size() < PALABRAS_MINIMAS) {
            throw new DatoInvalido("palabrasClave", "Indica al menos dos palabras clave");
        }
        return List.copyOf(limpias);
    }

    /**
     * Una asignatura corregida y lo que cambió, para el registro.
     *
     * @param asignatura la asignatura corregida
     * @param cambios cada dato que cambió, con su valor anterior y el nuevo
     */
    public record Correccion(Asignatura asignatura, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
