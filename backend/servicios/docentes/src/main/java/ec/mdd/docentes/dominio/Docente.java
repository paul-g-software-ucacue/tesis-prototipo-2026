package ec.mdd.docentes.dominio;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Una persona contratada por la institución a la que se asignan horas, con su ficha académica
 * (glosario «Docente», RF-16, RF-17, carga 6). Su identificación es su clave y no cambia; su estado
 * laboral cambia, pero ningún docente se borra (principio VII).
 *
 * @param identificacion su cédula o pasaporte
 * @param datos sus datos personales y de contrato, su carrera base y su ficha
 * @param estado activo, en licencia o inactivo
 */
public record Docente(Identificacion identificacion, DatosDelDocente datos, EstadoLaboral estado) {

    /** Exige las tres partes. */
    public Docente {
        Objects.requireNonNull(identificacion, "identificacion");
        Objects.requireNonNull(datos, "datos");
        Objects.requireNonNull(estado, "estado");
    }

    /**
     * Un docente recién registrado: activo.
     *
     * @param actual el año actual, para los años de la ficha
     * @throws DatoInvalido si un año de la ficha pasa del actual
     */
    public static Docente nuevo(Identificacion identificacion, DatosDelDocente datos, Year actual) {
        datos.ficha().verificarAnios(actual);
        return new Docente(identificacion, datos, EstadoLaboral.ACTIVO);
    }

    /**
     * El docente con sus datos corregidos; la identificación y el estado no cambian.
     *
     * @param actual el año actual, para los años de la ficha
     * @return el docente corregido y lo que cambió; sin cambios, la lista viene vacía
     * @throws DatoInvalido si un año de la ficha pasa del actual
     */
    public Correccion corregido(DatosDelDocente nuevos, Year actual) {
        nuevos.ficha().verificarAnios(actual);
        List<CambioDeValor> cambios = new ArrayList<>();
        DatosDelDocente antes = datos;
        Cambios.agregar(cambios, "primerApellido", antes.primerApellido(), nuevos.primerApellido());
        Cambios.agregar(
                cambios, "segundoApellido", antes.segundoApellido(), nuevos.segundoApellido());
        Cambios.agregar(cambios, "primerNombre", antes.primerNombre(), nuevos.primerNombre());
        Cambios.agregar(cambios, "segundoNombre", antes.segundoNombre(), nuevos.segundoNombre());
        Cambios.agregar(cambios, "correoInstitucional", antes.correo(), nuevos.correo());
        Cambios.agregar(cambios, "telefono", antes.telefono(), nuevos.telefono());
        Cambios.agregar(cambios, "tipoPersonal", antes.tipoPersonal(), nuevos.tipoPersonal());
        Cambios.agregar(cambios, "categoriaEscalafon", antes.categoria(), nuevos.categoria());
        Cambios.agregar(cambios, "dedicacion", antes.dedicacion(), nuevos.dedicacion());
        Cambios.agregar(
                cambios, "horasContratadas", antes.horasContratadas(), nuevos.horasContratadas());
        Cambios.agregar(cambios, "fechaIngreso", antes.fechaIngreso(), nuevos.fechaIngreso());
        Cambios.agregar(cambios, "inicioContrato", antes.inicioContrato(), nuevos.inicioContrato());
        Cambios.agregar(cambios, "finContrato", antes.finContrato(), nuevos.finContrato());
        Cambios.agregar(
                cambios,
                "condicionEspecial",
                antes.condicionEspecial(),
                nuevos.condicionEspecial());
        Cambios.agregar(cambios, "carreraBase", antes.carreraBase(), nuevos.carreraBase());
        agregarFicha(cambios, antes.ficha(), nuevos.ficha());
        return new Correccion(new Docente(identificacion, nuevos, estado), cambios);
    }

    /** El docente en otro estado laboral; los demás datos no cambian. */
    public Docente conEstado(EstadoLaboral nuevo) {
        return new Docente(identificacion, datos, nuevo);
    }

    private static void agregarFicha(
            List<CambioDeValor> cambios, FichaAcademica antes, FichaAcademica despues) {
        TituloTercerNivel tercero = antes.tercerNivel();
        TituloTercerNivel nuevo = despues.tercerNivel();
        Cambios.agregar(cambios, "ficha.tercerNivel.titulo", tercero.titulo(), nuevo.titulo());
        Cambios.agregar(
                cambios,
                "ficha.tercerNivel.registroSenescyt",
                tercero.registroSenescyt(),
                nuevo.registroSenescyt());
        Cambios.agregar(
                cambios,
                "ficha.tercerNivel.campoAmplio",
                tercero.campo().amplio(),
                nuevo.campo().amplio());
        Cambios.agregar(
                cambios,
                "ficha.tercerNivel.campoEspecifico",
                tercero.campo().especifico(),
                nuevo.campo().especifico());
        Cambios.agregar(cambios, "ficha.cuartoNivel", antes.cuartoNivel(), despues.cuartoNivel());
        Cambios.agregar(cambios, "ficha.cursos", antes.cursos(), despues.cursos());
        Cambios.agregar(cambios, "ficha.experiencia", antes.experiencia(), despues.experiencia());
        Cambios.agregar(
                cambios,
                "ficha.asignaturasAfines",
                antes.asignaturasAfines(),
                despues.asignaturasAfines());
    }

    /**
     * Un docente corregido y lo que cambió, para el registro (FR-010).
     *
     * @param docente el docente corregido
     * @param cambios cada campo cambiado con su valor anterior y el nuevo
     */
    public record Correccion(Docente docente, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
