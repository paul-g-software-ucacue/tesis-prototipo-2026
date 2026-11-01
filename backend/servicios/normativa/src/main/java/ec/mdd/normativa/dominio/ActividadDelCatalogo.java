package ec.mdd.normativa.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Una actividad de los arts. 140 a 143 del RCE (RF-21, RN-22, D-79), con lo que exige. Es un dato
 * de la versión de normativa (principio VI).
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
}
