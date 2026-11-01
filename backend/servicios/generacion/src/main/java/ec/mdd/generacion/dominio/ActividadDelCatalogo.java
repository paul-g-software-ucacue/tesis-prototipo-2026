package ec.mdd.generacion.dominio;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Una actividad del catálogo de la versión de normativa (RN-22, D-79), con lo que exige.
 *
 * @param codigo artículo y literal, por ejemplo {@code 140-b}
 * @param nombre el texto del literal
 * @param componente dónde cuentan sus horas
 * @param tiposDeProyecto los tipos que sirven; vacía si no exige un proyecto
 * @param cargos los cargos que sirven; vacía si no exige un cargo
 */
public record ActividadDelCatalogo(
        String codigo,
        String nombre,
        Componente componente,
        List<String> tiposDeProyecto,
        List<String> cargos) {

    /** Exige los datos y copia las listas. */
    public ActividadDelCatalogo {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(componente, "componente");
        tiposDeProyecto = List.copyOf(tiposDeProyecto);
        cargos = List.copyOf(cargos);
    }

    /** Si exige un proyecto. */
    public boolean exigeProyecto() {
        return !tiposDeProyecto.isEmpty();
    }

    /** Los proyectos del docente que sirven para esta actividad. */
    public List<ProyectoDelDocente> proyectosPosibles(ProyectosYCargosDelDocente delDocente) {
        return delDocente.proyectos().stream()
                .filter(proyecto -> tiposDeProyecto.contains(proyecto.tipo()))
                .toList();
    }

    /** Por qué no se le puede asignar al docente; vacío si se puede. */
    public Optional<String> motivoParaNoAsignar(ProyectosYCargosDelDocente delDocente) {
        if (exigeProyecto() && proyectosPosibles(delDocente).isEmpty()) {
            return Optional.of(
                    "El docente no tiene proyectos asignables de este tipo en el periodo.");
        }
        if (!tieneElCargo(delDocente)) {
            return Optional.of("El docente no tiene vigente el cargo que exige esta actividad.");
        }
        return Optional.empty();
    }

    /**
     * Comprueba lo que exige la actividad.
     *
     * @param proyecto el código del proyecto elegido; {@code null} si no hay
     * @param delDocente sus proyectos y sus cargos
     * @return el proyecto elegido; vacío si la actividad no lo pide
     * @throws ProyectoNoValido si falta, sobra, es ajeno o es de otro tipo
     * @throws CargoNoVigente si el docente no tiene el cargo
     */
    public Optional<ProyectoDelDocente> exigir(
            String proyecto, ProyectosYCargosDelDocente delDocente) {
        if (!tieneElCargo(delDocente)) {
            throw new CargoNoVigente();
        }
        if (!exigeProyecto()) {
            if (proyecto != null) {
                throw new ProyectoNoValido();
            }
            return Optional.empty();
        }
        return Optional.of(
                proyectosPosibles(delDocente).stream()
                        .filter(uno -> uno.codigo().equals(proyecto))
                        .findFirst()
                        .orElseThrow(ProyectoNoValido::new));
    }

    private boolean tieneElCargo(ProyectosYCargosDelDocente delDocente) {
        return cargos.isEmpty() || delDocente.cargos().stream().anyMatch(cargos::contains);
    }
}
