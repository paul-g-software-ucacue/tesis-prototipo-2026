package ec.mdd.docentes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Un proyecto aprobado por el OCS con sus participantes (glosario «Proyecto aprobado», RF-18, carga
 * 9). Su código no cambia. Que sus carreras existan en Académico lo comprueba {@link Referencias};
 * que el director y los participantes sean docentes registrados, {@link #verificarDocentes}.
 *
 * @param codigo mayúsculas, dígitos y guion (aclaración 5 de F03-H05)
 * @param nombre el nombre del proyecto
 * @param tipo investigación, vinculación, prácticas preprofesionales o servicio comunitario
 * @param carreras los códigos de las carreras involucradas, sin repetir
 * @param resolucionOcs la resolución del OCS que lo aprueba
 * @param fechaResolucion la fecha de esa resolución
 * @param estado aprobado, en ejecución, finalizado o suspendido
 * @param vigenciaInicio el primer día de su vigencia
 * @param vigenciaFin el último día de su vigencia
 * @param director la identificación del docente que lo dirige
 * @param participantes los docentes con su rol y sus horas, sin repetir docente
 * @param horasAprobadas las horas semanales aprobadas; los participantes no suman más
 */
public record Proyecto(
        String codigo,
        String nombre,
        TipoDeProyecto tipo,
        List<String> carreras,
        String resolucionOcs,
        LocalDate fechaResolucion,
        EstadoDeProyecto estado,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFin,
        String director,
        List<Participante> participantes,
        BigDecimal horasAprobadas) {

    /** El largo máximo del nombre. */
    public static final int NOMBRE_MAXIMO = 250;

    /** El largo máximo de la resolución del OCS. */
    public static final int RESOLUCION_MAXIMA = 60;

    /** Cuántas carreras admite: cubren los 200 caracteres de la columna en el archivo. */
    public static final int CARRERAS_MAXIMAS = 20;

    /** Cuántos participantes admite: cubren los 1000 caracteres de la columna en el archivo. */
    public static final int PARTICIPANTES_MAXIMOS = 40;

    private static final Pattern CODIGO = Pattern.compile("[0-9A-Z-]{1,30}");
    private static final Pattern CARRERA = Pattern.compile("[0-9A-Z-]{1,20}");
    private static final Pattern DOCENTE = Pattern.compile("[0-9A-Z]{1,13}");

    /** Valida cada campo según la carga 9 y deja las horas sin ceros de más. */
    public Proyecto {
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new DatoInvalido(
                    "codigo",
                    "El código tiene de 1 a 30 mayúsculas, dígitos o guiones, sin espacios");
        }
        nombre = texto(nombre, "nombre", "el nombre del proyecto", NOMBRE_MAXIMO);
        if (tipo == null) {
            throw new DatoInvalido("tipo", "Elige el tipo de proyecto");
        }
        carreras = carreras(carreras);
        resolucionOcs =
                texto(resolucionOcs, "resolucionOcs", "la resolución del OCS", RESOLUCION_MAXIMA);
        if (fechaResolucion == null) {
            throw new DatoInvalido("fechaResolucion", "Indica la fecha de la resolución");
        }
        if (estado == null) {
            throw new DatoInvalido("estado", "Elige el estado del proyecto");
        }
        if (vigenciaInicio == null) {
            throw new DatoInvalido("vigenciaInicio", "Indica el inicio de la vigencia");
        }
        if (vigenciaFin == null) {
            throw new DatoInvalido("vigenciaFin", "Indica el fin de la vigencia");
        }
        if (!vigenciaFin.isAfter(vigenciaInicio)) {
            throw new DatoInvalido(
                    "vigenciaFin", "El fin de la vigencia tiene que ser posterior a su inicio");
        }
        if (director == null || !DOCENTE.matcher(director).matches()) {
            throw new DatoInvalido("director", "Elige el docente que dirige el proyecto");
        }
        horasAprobadas = HorasSemanales.verificar(horasAprobadas, "horasAprobadas");
        participantes = participantes(participantes, horasAprobadas);
    }

    /**
     * Comprueba que el director y cada participante sean docentes registrados.
     *
     * @param registrado si una identificación es la de un docente registrado
     * @throws DatoInvalido en {@code director} o en {@code participantes[i].docente}
     */
    public void verificarDocentes(Predicate<String> registrado) {
        if (!registrado.test(director)) {
            throw new DatoInvalido("director", "El director tiene que ser un docente registrado");
        }
        for (int i = 0; i < participantes.size(); i++) {
            if (!registrado.test(participantes.get(i).docente())) {
                throw new DatoInvalido(
                        "participantes[" + i + "].docente",
                        "El participante tiene que ser un docente registrado");
            }
        }
    }

    /**
     * Si el proyecto es asignable en un periodo: aprobado o en ejecución y con una vigencia que se
     * cruza con las fechas del periodo, las dos inclusive (glosario «Proyecto asignable»).
     *
     * @param inicio el primer día del periodo
     * @param fin el último día del periodo
     */
    public Asignabilidad asignableEn(LocalDate inicio, LocalDate fin) {
        Set<MotivoNoAsignable> motivos = EnumSet.noneOf(MotivoNoAsignable.class);
        if (!estado.recibeHoras()) {
            motivos.add(MotivoNoAsignable.ESTADO);
        }
        if (vigenciaInicio.isAfter(fin) || vigenciaFin.isBefore(inicio)) {
            motivos.add(MotivoNoAsignable.VIGENCIA);
        }
        return new Asignabilidad(motivos.isEmpty(), motivos);
    }

    /**
     * El proyecto con sus datos corregidos; el código no cambia.
     *
     * @param nuevo el proyecto con el mismo código y los datos nuevos
     * @return el proyecto corregido y lo que cambió; sin cambios, la lista viene vacía
     */
    public Correccion corregido(Proyecto nuevo) {
        if (!codigo.equals(nuevo.codigo)) {
            throw new IllegalArgumentException("El código de un proyecto no cambia");
        }
        List<CambioDeValor> cambios = new ArrayList<>();
        Cambios.agregar(cambios, "nombre", nombre, nuevo.nombre);
        Cambios.agregar(cambios, "tipo", tipo, nuevo.tipo);
        Cambios.agregar(cambios, "carreras", carreras, nuevo.carreras);
        Cambios.agregar(cambios, "resolucionOcs", resolucionOcs, nuevo.resolucionOcs);
        Cambios.agregar(cambios, "fechaResolucion", fechaResolucion, nuevo.fechaResolucion);
        Cambios.agregar(cambios, "estado", estado, nuevo.estado);
        Cambios.agregar(cambios, "vigenciaInicio", vigenciaInicio, nuevo.vigenciaInicio);
        Cambios.agregar(cambios, "vigenciaFin", vigenciaFin, nuevo.vigenciaFin);
        Cambios.agregar(cambios, "director", director, nuevo.director);
        Cambios.agregar(
                cambios,
                "participantes",
                participantes.stream().map(Proyecto::comoTexto).toList(),
                nuevo.participantes.stream().map(Proyecto::comoTexto).toList());
        Cambios.agregar(cambios, "horasAprobadas", horasAprobadas, nuevo.horasAprobadas);
        return new Correccion(nuevo, cambios);
    }

    private static String texto(String valor, String campo, String que, int maximo) {
        String limpio = valor == null ? "" : valor.strip();
        if (limpio.isEmpty()) {
            throw new DatoInvalido(campo, "Indica " + que);
        }
        if (limpio.length() > maximo) {
            throw new DatoInvalido(campo, "No puede tener más de " + maximo + " caracteres");
        }
        return limpio;
    }

    private static List<String> carreras(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            throw new DatoInvalido("carreras", "Elige al menos una carrera");
        }
        if (codigos.size() > CARRERAS_MAXIMAS) {
            throw new DatoInvalido(
                    "carreras", "Un proyecto admite hasta " + CARRERAS_MAXIMAS + " carreras");
        }
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < codigos.size(); i++) {
            String codigo = codigos.get(i) == null ? "" : codigos.get(i).strip();
            if (!CARRERA.matcher(codigo).matches()) {
                throw new DatoInvalido("carreras[" + i + "]", "Elige una carrera de Académico");
            }
            if (!vistas.add(codigo)) {
                throw new DatoInvalido("carreras[" + i + "]", "La carrera ya está en la lista");
            }
        }
        return codigos.stream().map(String::strip).toList();
    }

    private static List<Participante> participantes(
            List<Participante> lista, BigDecimal horasAprobadas) {
        if (lista == null || lista.isEmpty()) {
            throw new DatoInvalido("participantes", "Agrega al menos un participante");
        }
        if (lista.size() > PARTICIPANTES_MAXIMOS) {
            throw new DatoInvalido(
                    "participantes",
                    "Un proyecto admite hasta " + PARTICIPANTES_MAXIMOS + " participantes");
        }
        List<Participante> validos = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        BigDecimal suma = BigDecimal.ZERO;
        for (int i = 0; i < lista.size(); i++) {
            String base = "participantes[" + i + "].";
            Participante uno = lista.get(i);
            if (uno == null) {
                throw new DatoInvalido("participantes[" + i + "]", "Falta el participante");
            }
            if (uno.docente() == null || !DOCENTE.matcher(uno.docente()).matches()) {
                throw new DatoInvalido(base + "docente", "Elige un docente registrado");
            }
            if (!vistos.add(uno.docente())) {
                throw new DatoInvalido(base + "docente", "El docente ya está en la lista");
            }
            if (uno.rol() == null) {
                throw new DatoInvalido(base + "rol", "Elige el rol del participante");
            }
            BigDecimal horas = HorasSemanales.verificar(uno.horas(), base + "horas");
            suma = suma.add(horas);
            validos.add(new Participante(uno.docente(), uno.rol(), horas));
        }
        if (suma.compareTo(horasAprobadas) > 0) {
            throw new DatoInvalido(
                    "participantes",
                    "Las horas de los participantes suman "
                            + suma.stripTrailingZeros().toPlainString()
                            + " y pasan las "
                            + horasAprobadas.toPlainString()
                            + " aprobadas");
        }
        return List.copyOf(validos);
    }

    private static String comoTexto(Participante uno) {
        return uno.docente() + "|" + uno.rol() + "|" + uno.horas().toPlainString();
    }

    /**
     * Un proyecto corregido con lo que cambió.
     *
     * @param proyecto el proyecto corregido
     * @param cambios cada campo con su valor anterior y el nuevo
     */
    public record Correccion(Proyecto proyecto, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
