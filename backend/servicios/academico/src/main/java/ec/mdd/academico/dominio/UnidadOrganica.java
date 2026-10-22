package ec.mdd.academico.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Una unidad de la estructura orgánica: una sede, un órgano, una coordinación, una gestión o una
 * comisión (RF-12, glosario §7.3 «Unidad»). Depende de otra unidad, salvo la sede matriz; se
 * inactiva, nunca se borra. Académico es de soporte (principio II): es un valor inmutable con las
 * reglas de su tipo; las que miran a otras unidades están en {@link EstructuraOrganica}.
 *
 * @param codigo mayúsculas, dígitos, «-» o «_», hasta 20; no cambia nunca
 * @param nombre en blanco no vale; hasta {@value #NOMBRE_MAXIMO} caracteres
 * @param tipo no cambia nunca
 * @param padre el código de la unidad de la que depende; {@code null} solo en la sede matriz
 * @param gestionAutorizada el código de la gestión autorizada que representa, o {@code null}: una
 *     gestión la exige, una comisión no la lleva y los demás tipos pueden llevarla (FR-004)
 * @param comisionPermanente si la comisión es permanente; {@code null} en los demás tipos
 * @param activa si está activa
 * @throws DatoInvalido si un dato no cumple su regla (FR-007)
 */
public record UnidadOrganica(
        String codigo,
        String nombre,
        TipoDeUnidad tipo,
        String padre,
        String gestionAutorizada,
        Boolean comisionPermanente,
        boolean activa) {

    /** El largo máximo del nombre. */
    public static final int NOMBRE_MAXIMO = 150;

    // El formato del diccionario de la carga 01.
    private static final Pattern CODIGO = Pattern.compile("[A-Z0-9_-]{1,20}");

    /** Valida los datos de su tipo y quita los espacios de los extremos del nombre. */
    public UnidadOrganica {
        Objects.requireNonNull(tipo, "tipo");
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new DatoInvalido(
                    "codigo",
                    "El código solo admite mayúsculas, números, guion y guion bajo, hasta 20"
                            + " caracteres");
        }
        nombre = nombre == null ? "" : nombre.strip();
        if (nombre.isEmpty()) {
            throw new DatoInvalido("nombre", "El nombre no puede estar en blanco");
        }
        if (nombre.length() > NOMBRE_MAXIMO) {
            throw new DatoInvalido(
                    "nombre", "El nombre no puede tener más de " + NOMBRE_MAXIMO + " caracteres");
        }
        if (tipo == TipoDeUnidad.SEDE_MATRIZ && padre != null) {
            throw new DatoInvalido("padre", "La sede matriz no depende de otra unidad");
        }
        if (tipo != TipoDeUnidad.SEDE_MATRIZ && padre == null) {
            throw new DatoInvalido("padre", "Elige la unidad de la que depende");
        }
        if (tipo == TipoDeUnidad.GESTION && gestionAutorizada == null) {
            throw new DatoInvalido(
                    "gestionAutorizada",
                    "Una gestión tiene que representar una de las 21 gestiones autorizadas");
        }
        if (tipo == TipoDeUnidad.COMISION && gestionAutorizada != null) {
            throw new DatoInvalido(
                    "gestionAutorizada",
                    "Una comisión es un dato informativo: no representa una gestión autorizada");
        }
        if (tipo == TipoDeUnidad.COMISION && comisionPermanente == null) {
            throw new DatoInvalido("comisionPermanente", "Indica si la comisión es permanente");
        }
        if (tipo != TipoDeUnidad.COMISION && comisionPermanente != null) {
            throw new DatoInvalido(
                    "comisionPermanente", "Solo una comisión indica si es permanente");
        }
    }

    /** Una unidad recién registrada, activa (FR-002). */
    public static UnidadOrganica nueva(
            String codigo,
            String nombre,
            TipoDeUnidad tipo,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente) {
        return new UnidadOrganica(
                codigo, nombre, tipo, padre, gestionAutorizada, comisionPermanente, true);
    }

    /**
     * La unidad con su nombre, su padre y los datos de su tipo corregidos; el código, el tipo y el
     * estado no cambian.
     *
     * @return la unidad corregida y lo que cambió; sin cambios, la lista viene vacía
     * @throws DatoInvalido si un dato nuevo no cumple su regla
     */
    public Correccion corregida(
            String nombre, String padre, String gestionAutorizada, Boolean comisionPermanente) {
        UnidadOrganica corregida =
                new UnidadOrganica(
                        codigo, nombre, tipo, padre, gestionAutorizada, comisionPermanente, activa);
        List<CambioDeValor> cambios = new ArrayList<>();
        agregar(cambios, "nombre", this.nombre, corregida.nombre);
        agregar(cambios, "padre", this.padre, corregida.padre);
        agregar(cambios, "gestionAutorizada", this.gestionAutorizada, corregida.gestionAutorizada);
        agregar(
                cambios,
                "comisionPermanente",
                this.comisionPermanente,
                corregida.comisionPermanente);
        return new Correccion(corregida, cambios);
    }

    /** La unidad activa o inactiva; los demás datos no cambian. */
    public UnidadOrganica conEstado(boolean activa) {
        return new UnidadOrganica(
                codigo, nombre, tipo, padre, gestionAutorizada, comisionPermanente, activa);
    }

    private static void agregar(
            List<CambioDeValor> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(new CambioDeValor(campo, texto(anterior), texto(nuevo)));
        }
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    /**
     * Una unidad corregida y lo que cambió, para el registro.
     *
     * @param unidad la unidad corregida
     * @param cambios cada dato que cambió, con su valor anterior y el nuevo; vacío es «sin valor»
     */
    public record Correccion(UnidadOrganica unidad, List<CambioDeValor> cambios) {

        /** Copia la lista. */
        public Correccion {
            cambios = List.copyOf(cambios);
        }
    }
}
