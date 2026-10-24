package ec.mdd.docentes.dominio;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * La formación y la experiencia de un docente, dentro de su registro (glosario «Ficha académica»,
 * RF-17, carga 6): el título de tercer nivel, los títulos de cuarto nivel, los cursos y
 * capacitaciones, la experiencia laboral y las asignaturas afines declaradas. Alimenta el puntaje
 * de afinidad, que calcula Generación (MD-02).
 *
 * <p>Valida cada parte con el campo del contrato y su posición (por ejemplo, {@code
 * ficha.cursos[1].horas}), y quita los espacios de los extremos de los textos. Que los años no
 * pasen del actual lo comprueba {@link #verificarAnios}, porque el año actual no es un dato de la
 * ficha.
 *
 * @param tercerNivel el título de tercer nivel, obligatorio
 * @param cuartoNivel hasta 20 títulos de cuarto nivel
 * @param cursos hasta 20 cursos y capacitaciones
 * @param experiencia hasta 20 experiencias laborales
 * @param asignaturasAfines hasta 20 códigos de asignaturas, sin repetir
 */
public record FichaAcademica(
        TituloTercerNivel tercerNivel,
        List<TituloCuartoNivel> cuartoNivel,
        List<Curso> cursos,
        List<ExperienciaLaboral> experiencia,
        List<String> asignaturasAfines) {

    /** El tope de cada lista de la ficha. */
    public static final int ELEMENTOS_MAXIMOS = 20;

    /** El primer año admitido en un título o un curso (carga 6). */
    public static final int PRIMER_ANIO = 1950;

    private static final int TEXTO_MAXIMO = 150;
    private static final int REGISTRO_MAXIMO = 30;
    private static final int HORAS_MAXIMAS = 2000;
    private static final BigDecimal ANIOS_MAXIMOS = new BigDecimal("60");
    private static final Pattern AMPLIO = Pattern.compile("[0-9]{2}");
    private static final Pattern ESPECIFICO = Pattern.compile("[0-9]{3}");
    private static final Pattern ASIGNATURA = Pattern.compile("[0-9A-Z-]{1,30}");
    private static final String PREFIJO = "ficha.";

    /** Valida y limpia cada parte; las listas nulas quedan vacías. */
    public FichaAcademica {
        tercerNivel = tercerNivel(tercerNivel);
        cuartoNivel = lista(cuartoNivel, "cuartoNivel", FichaAcademica::tituloCuartoNivel);
        cursos = lista(cursos, "cursos", FichaAcademica::curso);
        experiencia = lista(experiencia, "experiencia", FichaAcademica::experiencia);
        asignaturasAfines = asignaturas(asignaturasAfines);
    }

    /**
     * Comprueba que ningún título ni curso sea de un año posterior al actual.
     *
     * @throws DatoInvalido en {@code ficha.cuartoNivel[i].anio} o {@code ficha.cursos[i].anio}
     */
    public void verificarAnios(Year actual) {
        for (int i = 0; i < cuartoNivel.size(); i++) {
            anioHasta(cuartoNivel.get(i).anio(), actual, campo("cuartoNivel", i, "anio"));
        }
        for (int i = 0; i < cursos.size(); i++) {
            anioHasta(cursos.get(i).anio(), actual, campo("cursos", i, "anio"));
        }
    }

    /**
     * Los campos amplios que usa la ficha: el del tercer nivel y los de las listas, sin repetir.
     */
    public Set<String> camposAmplios() {
        Set<String> campos = new TreeSet<>();
        campos.add(tercerNivel.campo().amplio());
        cuartoNivel.forEach(titulo -> campos.add(titulo.campoAmplio()));
        cursos.forEach(curso -> campos.add(curso.campoAmplio()));
        experiencia.forEach(una -> campos.add(una.campoAmplio()));
        return campos;
    }

    private static TituloTercerNivel tercerNivel(TituloTercerNivel titulo) {
        String base = PREFIJO + "tercerNivel.";
        if (titulo == null) {
            throw new DatoInvalido(PREFIJO + "tercerNivel", "Indica el título de tercer nivel");
        }
        CampoConocimiento campo = titulo.campo();
        String amplio = campo == null ? null : campo.amplio();
        String especifico = campo == null ? null : campo.especifico();
        if (amplio == null || !AMPLIO.matcher(amplio).matches()) {
            throw new DatoInvalido(base + "campoAmplio", "El campo amplio tiene dos dígitos");
        }
        especifico(especifico, base + "campoEspecifico");
        if (!especifico.startsWith(amplio)) {
            throw new DatoInvalido(
                    base + "campoEspecifico",
                    "El campo específico tiene que empezar por el campo amplio");
        }
        String registro = titulo.registroSenescyt();
        if (registro != null) {
            registro = registro.strip();
            if (registro.isEmpty()) {
                registro = null;
            } else if (registro.length() > REGISTRO_MAXIMO) {
                throw new DatoInvalido(
                        base + "registroSenescyt",
                        "El registro SENESCYT no puede tener más de "
                                + REGISTRO_MAXIMO
                                + " caracteres");
            }
        }
        return new TituloTercerNivel(
                texto(titulo.titulo(), base + "titulo", "el título"),
                registro,
                new CampoConocimiento(amplio, especifico));
    }

    private static TituloCuartoNivel tituloCuartoNivel(TituloCuartoNivel titulo, String base) {
        if (titulo.tipo() == null) {
            throw new DatoInvalido(
                    base + "tipo", "El tipo es maestría, especialización o doctorado");
        }
        anioDesde(titulo.anio(), base + "anio");
        return new TituloCuartoNivel(
                texto(titulo.titulo(), base + "titulo", "el título"),
                titulo.tipo(),
                especifico(titulo.campoEspecifico(), base + "campoEspecifico"),
                titulo.anio());
    }

    private static Curso curso(Curso curso, String base) {
        if (curso.horas() < 1 || curso.horas() > HORAS_MAXIMAS) {
            throw new DatoInvalido(
                    base + "horas", "Las horas del curso son enteras, de 1 a " + HORAS_MAXIMAS);
        }
        anioDesde(curso.anio(), base + "anio");
        return new Curso(
                texto(curso.nombre(), base + "nombre", "el nombre"),
                especifico(curso.campoEspecifico(), base + "campoEspecifico"),
                curso.horas(),
                curso.anio());
    }

    private static ExperienciaLaboral experiencia(ExperienciaLaboral una, String base) {
        BigDecimal anios = una.anios();
        if (anios == null || anios.signum() <= 0 || anios.compareTo(ANIOS_MAXIMOS) > 0) {
            throw new DatoInvalido(
                    base + "anios", "Los años son mayores que 0 y hasta " + ANIOS_MAXIMOS);
        }
        return new ExperienciaLaboral(
                texto(una.cargo(), base + "cargo", "el cargo"),
                texto(una.institucion(), base + "institucion", "la institución"),
                especifico(una.campoEspecifico(), base + "campoEspecifico"),
                anios.stripTrailingZeros());
    }

    private static <T> List<T> lista(
            List<T> elementos, String nombre, BiFunction<T, String, T> validar) {
        if (elementos == null) {
            return List.of();
        }
        if (elementos.size() > ELEMENTOS_MAXIMOS) {
            throw new DatoInvalido(
                    PREFIJO + nombre, "La lista admite hasta " + ELEMENTOS_MAXIMOS + " elementos");
        }
        List<T> validos = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i++) {
            T elemento = elementos.get(i);
            if (elemento == null) {
                throw new DatoInvalido(PREFIJO + nombre + "[" + i + "]", "Falta el elemento");
            }
            validos.add(validar.apply(elemento, PREFIJO + nombre + "[" + i + "]."));
        }
        return List.copyOf(validos);
    }

    private static List<String> asignaturas(List<String> codigos) {
        if (codigos == null) {
            return List.of();
        }
        String campo = PREFIJO + "asignaturasAfines";
        if (codigos.size() > ELEMENTOS_MAXIMOS) {
            throw new DatoInvalido(
                    campo, "La lista admite hasta " + ELEMENTOS_MAXIMOS + " elementos");
        }
        List<String> limpias = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < codigos.size(); i++) {
            String codigo = codigos.get(i) == null ? "" : codigos.get(i).strip();
            if (!ASIGNATURA.matcher(codigo).matches()) {
                throw new DatoInvalido(
                        campo + "[" + i + "]",
                        "El código de asignatura solo admite mayúsculas, números y guion, hasta 30"
                                + " caracteres");
            }
            if (!vistas.add(codigo)) {
                throw new DatoInvalido(campo + "[" + i + "]", "La asignatura ya está en la lista");
            }
            limpias.add(codigo);
        }
        return List.copyOf(limpias);
    }

    private static String texto(String valor, String campo, String que) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty()) {
            throw new DatoInvalido(campo, "Indica " + que);
        }
        if (limpio.length() > TEXTO_MAXIMO) {
            throw new DatoInvalido(campo, "No puede tener más de " + TEXTO_MAXIMO + " caracteres");
        }
        return limpio;
    }

    private static String especifico(String especifico, String campo) {
        if (especifico == null || !ESPECIFICO.matcher(especifico).matches()) {
            throw new DatoInvalido(campo, "El campo específico tiene tres dígitos");
        }
        return especifico;
    }

    private static void anioDesde(int anio, String campo) {
        if (anio < PRIMER_ANIO) {
            throw new DatoInvalido(campo, "El año va desde " + PRIMER_ANIO);
        }
    }

    private static void anioHasta(int anio, Year actual, String campo) {
        if (anio > actual.getValue()) {
            throw new DatoInvalido(
                    campo, "El año va de " + PRIMER_ANIO + " a " + actual.getValue());
        }
    }

    private static String campo(String lista, int posicion, String nombre) {
        return PREFIJO + lista + "[" + posicion + "]." + nombre;
    }
}
