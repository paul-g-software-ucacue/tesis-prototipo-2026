package ec.mdd.docentes.puertos.entrada;

import java.util.Set;

/**
 * Caso de uso: las identificaciones de los docentes registrados, sin más datos. Es la API publicada
 * para que Cargas compruebe que existen los docentes que citan las cargas 8 y 9 (F01-H03, RF-08),
 * sin datos personales ni tipos del dominio de Docentes (principios V y IX).
 */
public interface ConsultarIdentificaciones {

    /** Las identificaciones de todos los docentes registrados, en cualquier estado laboral. */
    Set<String> identificaciones();
}
