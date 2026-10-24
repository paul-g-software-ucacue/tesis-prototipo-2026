package ec.mdd.academico.dominio;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Una carrera ofertada, con su código SENESCYT, la sede donde se oferta, su modalidad, sus jornadas
 * y su campo de conocimiento (glosario «Carrera», RF-12, carga 2). Las reglas entre entidades, como
 * la sede activa, están en {@link DatosMaestros}.
 *
 * @param codigo el código SENESCYT, como {@code 550613A01-P-0301}; no cambia
 * @param nombre hasta 150 caracteres
 * @param siglas mayúsculas y dígitos, hasta 10
 * @param sede el código de una sede, extensión o campus de la estructura orgánica
 * @param nivelFormacion su nivel de formación
 * @param modalidad su modalidad de estudios
 * @param jornadas al menos una, sin repetir, en el orden en que se dieron
 * @param campo su campo amplio y específico
 * @param campoDetallado cuatro dígitos que empiezan por el específico, o {@code null}
 * @param numeroCiclos de 1 a 10
 * @param estado vigente, no vigente o en cierre
 */
public record Carrera(
        String codigo,
        String nombre,
        String siglas,
        String sede,
        NivelDeFormacion nivelFormacion,
        Modalidad modalidad,
        List<Jornada> jornadas,
        CampoConocimiento campo,
        String campoDetallado,
        int numeroCiclos,
        EstadoDeCarrera estado) {

    /** El largo máximo del nombre. */
    public static final int NOMBRE_MAXIMO = 150;

    /** El mayor número de ciclos de una malla. */
    public static final int CICLOS_MAXIMO = 10;

    // Los formatos del diccionario de la carga 02.
    private static final Pattern CODIGO = Pattern.compile("[0-9A-Z-]{1,20}");
    private static final Pattern SIGLAS = Pattern.compile("[0-9A-Z]{1,10}");
    private static final Pattern DETALLADO = Pattern.compile("[0-9]{4}");

    /** Valida sus reglas y quita los espacios de los extremos del nombre. */
    public Carrera {
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new DatoInvalido(
                    "codigo",
                    "El código solo admite mayúsculas, números y guion, hasta 20 caracteres");
        }
        nombre = Textos.nombre(nombre, NOMBRE_MAXIMO);
        if (siglas == null || !SIGLAS.matcher(siglas).matches()) {
            throw new DatoInvalido(
                    "siglas", "Las siglas solo admiten mayúsculas y números, hasta 10 caracteres");
        }
        if (sede == null || sede.isBlank()) {
            throw new DatoInvalido("sede", "Elige la sede, extensión o campus de la carrera");
        }
        Objects.requireNonNull(nivelFormacion, "nivelFormacion");
        Objects.requireNonNull(modalidad, "modalidad");
        if (jornadas == null || jornadas.isEmpty()) {
            throw new DatoInvalido("jornadas", "Elige al menos una jornada");
        }
        if (new HashSet<>(jornadas).size() != jornadas.size()) {
            throw new DatoInvalido("jornadas", "Una jornada no se repite");
        }
        jornadas = List.copyOf(jornadas);
        Objects.requireNonNull(campo, "campo");
        if (campoDetallado != null
                && (!DETALLADO.matcher(campoDetallado).matches()
                        || !campoDetallado.startsWith(campo.especifico()))) {
            throw new DatoInvalido(
                    "campoDetallado",
                    "El campo detallado tiene cuatro dígitos y empieza por el campo específico");
        }
        if (numeroCiclos < 1 || numeroCiclos > CICLOS_MAXIMO) {
            throw new DatoInvalido(
                    "numeroCiclos", "El número de ciclos va de 1 a " + CICLOS_MAXIMO);
        }
        Objects.requireNonNull(estado, "estado");
    }

    /** Una carrera recién registrada, vigente. */
    public static Carrera nueva(
            String codigo,
            String nombre,
            String siglas,
            String sede,
            NivelDeFormacion nivelFormacion,
            Modalidad modalidad,
            List<Jornada> jornadas,
            CampoConocimiento campo,
            String campoDetallado,
            int numeroCiclos) {
        return new Carrera(
                codigo,
                nombre,
                siglas,
                sede,
                nivelFormacion,
                modalidad,
                jornadas,
                campo,
                campoDetallado,
                numeroCiclos,
                EstadoDeCarrera.VIGENTE);
    }

    /**
     * La carrera con sus datos corregidos; el código y el estado no cambian.
     *
     * @return la carrera corregida y lo que cambió; sin cambios, la lista viene vacía
     * @throws DatoInvalido si un dato nuevo no cumple su regla
     */
    public Correccion corregida(
            String nombre,
            String siglas,
            String sede,
            NivelDeFormacion nivelFormacion,
            Modalidad modalidad,
            List<Jornada> jornadas,
            CampoConocimiento campo,
            String campoDetallado,
            int numeroCiclos) {
        Carrera corregida =
                new Carrera(
                        codigo,
                        nombre,
                        siglas,
                        sede,
                        nivelFormacion,
                        modalidad,
                        jornadas,
                        campo,
                        campoDetallado,
                        numeroCiclos,
                        estado);
        List<CambioDeValor> cambios = new ArrayList<>();
        Cambios.agregar(cambios, "nombre", this.nombre, corregida.nombre);
        Cambios.agregar(cambios, "siglas", this.siglas, corregida.siglas);
        Cambios.agregar(cambios, "sede", this.sede, corregida.sede);
        Cambios.agregar(cambios, "nivelFormacion", this.nivelFormacion, corregida.nivelFormacion);
        Cambios.agregar(cambios, "modalidad", this.modalidad, corregida.modalidad);
        Cambios.agregar(cambios, "jornadas", this.jornadas, corregida.jornadas);
        Cambios.agregar(cambios, "campoAmplio", this.campo.amplio(), corregida.campo.amplio());
        Cambios.agregar(
                cambios, "campoEspecifico", this.campo.especifico(), corregida.campo.especifico());
        Cambios.agregar(cambios, "campoDetallado", this.campoDetallado, corregida.campoDetallado);
        Cambios.agregar(cambios, "numeroCiclos", this.numeroCiclos, corregida.numeroCiclos);
        return new Correccion(corregida, cambios);
    }

    /** La carrera con otro estado; los demás datos no cambian. */
    public Carrera conEstado(EstadoDeCarrera estado) {
        return new Carrera(
                codigo,
                nombre,
                siglas,
                sede,
                nivelFormacion,
                modalidad,
                jornadas,
                campo,
                campoDetallado,
                numeroCiclos,
                estado);
    }

    /**
     * Una carrera corregida y lo que cambió, para el registro.
     *
     * @param carrera la carrera corregida
     * @param cambios cada dato que cambió, con su valor anterior y el nuevo
     */
    public record Correccion(Carrera carrera, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
