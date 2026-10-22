package ec.mdd.academico.dominio;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Toda la estructura orgánica, con las reglas que miran a varias unidades (RF-12, RN-10, FR-003):
 * el padre existe y está activo, no hay ciclos, hay una sola sede matriz, la gestión autorizada es
 * del catálogo y no se inactiva una unidad con unidades activas debajo. La estructura es chica
 * (decenas de unidades) y se carga entera para validar (research §1).
 *
 * <p>Es un valor: sus métodos validan y devuelven la unidad nueva, sin cambiar la estructura;
 * {@link #con} devuelve otra estructura con esa unidad.
 */
public final class EstructuraOrganica {

    private final Map<String, UnidadOrganica> unidades;
    private final Set<String> gestiones;

    /**
     * @param unidades las unidades registradas
     * @param gestiones los códigos del catálogo de gestiones autorizadas
     */
    public EstructuraOrganica(Collection<UnidadOrganica> unidades, Collection<String> gestiones) {
        Map<String, UnidadOrganica> porCodigo = new LinkedHashMap<>();
        unidades.forEach(unidad -> porCodigo.put(unidad.codigo(), unidad));
        this.unidades = porCodigo;
        this.gestiones = Set.copyOf(gestiones);
    }

    /** Las unidades, en el orden en que llegaron. */
    public List<UnidadOrganica> unidades() {
        return List.copyOf(unidades.values());
    }

    /** La unidad con ese código, si existe. */
    public Optional<UnidadOrganica> unidad(String codigo) {
        return Optional.ofNullable(unidades.get(codigo));
    }

    /** Otra estructura con la unidad agregada, o reemplazada si ya estaba. */
    public EstructuraOrganica con(UnidadOrganica unidad) {
        Map<String, UnidadOrganica> nuevas = new LinkedHashMap<>(unidades);
        nuevas.put(unidad.codigo(), unidad);
        return new EstructuraOrganica(nuevas.values(), gestiones);
    }

    /**
     * Valida una unidad nueva contra la estructura. Que el código no esté repetido lo comprueba la
     * aplicación al guardarla.
     *
     * @return la misma unidad
     * @throws SedeMatrizExistente si es una segunda sede matriz
     * @throws DatoInvalido si el padre no existe o está inactivo, o la gestión no es del catálogo
     */
    public UnidadOrganica registrar(UnidadOrganica nueva) {
        if (nueva.tipo() == TipoDeUnidad.SEDE_MATRIZ) {
            unidades.values().stream()
                    .filter(unidad -> unidad.tipo() == TipoDeUnidad.SEDE_MATRIZ)
                    .findFirst()
                    .ifPresent(
                            matriz -> {
                                throw new SedeMatrizExistente(matriz.codigo());
                            });
        }
        validarPadre(nueva);
        validarGestion(nueva);
        return nueva;
    }

    /**
     * Corrige una unidad de la estructura.
     *
     * @throws DatoInvalido si un dato no cumple su regla, el padre no existe o (en una unidad
     *     activa) está inactivo, forma un ciclo, o la gestión no es del catálogo
     */
    public UnidadOrganica.Correccion corregir(
            UnidadOrganica actual,
            String nombre,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente) {
        UnidadOrganica.Correccion correccion =
                actual.corregida(nombre, padre, gestionAutorizada, comisionPermanente);
        UnidadOrganica corregida = correccion.unidad();
        validarCiclo(corregida);
        validarPadre(corregida);
        validarGestion(corregida);
        return correccion;
    }

    /**
     * Inactiva o reactiva una unidad.
     *
     * @return la unidad en su estado nuevo; si ya estaba en él, la misma
     * @throws UnidadConDependientes si se inactiva y tiene unidades activas debajo
     * @throws DatoInvalido si se reactiva y su padre está inactivo
     */
    public UnidadOrganica cambiarEstado(UnidadOrganica actual, boolean activa) {
        if (actual.activa() == activa) {
            return actual;
        }
        if (!activa
                && unidades.values().stream()
                        .anyMatch(
                                unidad ->
                                        unidad.activa()
                                                && actual.codigo().equals(unidad.padre()))) {
            throw new UnidadConDependientes(actual.codigo());
        }
        if (activa && actual.padre() != null && !padreActivo(actual)) {
            throw new DatoInvalido(
                    "padre",
                    "La unidad "
                            + actual.padre()
                            + " de la que depende está inactiva: reactívala antes");
        }
        return actual.conEstado(activa);
    }

    private void validarPadre(UnidadOrganica unidad) {
        if (unidad.padre() == null) {
            return;
        }
        UnidadOrganica padre = unidades.get(unidad.padre());
        if (padre == null) {
            throw new DatoInvalido("padre", "La unidad " + unidad.padre() + " no existe");
        }
        if (unidad.activa() && !padre.activa()) {
            throw new DatoInvalido("padre", "La unidad " + unidad.padre() + " está inactiva");
        }
    }

    // Sube desde el padre nuevo: si llega a la unidad, el padre depende de ella.
    private void validarCiclo(UnidadOrganica unidad) {
        if (unidad.codigo().equals(unidad.padre())) {
            throw new DatoInvalido("padre", "La unidad no puede depender de sí misma");
        }
        String actual = unidad.padre();
        for (int pasos = 0; actual != null && pasos <= unidades.size(); pasos++) {
            if (actual.equals(unidad.codigo())) {
                throw new DatoInvalido(
                        "padre",
                        "La unidad no puede depender de "
                                + unidad.padre()
                                + ", porque "
                                + unidad.padre()
                                + " depende de ella");
            }
            UnidadOrganica siguiente = unidades.get(actual);
            actual = siguiente == null ? null : siguiente.padre();
        }
    }

    private void validarGestion(UnidadOrganica unidad) {
        String codigo = unidad.gestionAutorizada();
        if (codigo != null && !gestiones.contains(codigo)) {
            throw new DatoInvalido(
                    "gestionAutorizada",
                    codigo + " no está entre las 21 gestiones autorizadas de POL 6");
        }
    }

    private boolean padreActivo(UnidadOrganica unidad) {
        UnidadOrganica padre = unidades.get(unidad.padre());
        return padre != null && padre.activa();
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof EstructuraOrganica estructura
                && unidades.equals(estructura.unidades)
                && gestiones.equals(estructura.gestiones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unidades, gestiones);
    }
}
