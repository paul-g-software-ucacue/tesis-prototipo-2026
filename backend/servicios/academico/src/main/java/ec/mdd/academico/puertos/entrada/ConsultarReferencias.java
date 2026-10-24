package ec.mdd.academico.puertos.entrada;

/**
 * Caso de uso: las referencias que Docentes valida y muestra en la ficha de cada docente: la
 * carrera base, las asignaturas afines declaradas y los campos amplios de sus títulos (F03-H04,
 * research §4). Es parte de la API publicada del servicio y refleja la operación {@code
 * consultarReferencias} del contrato: si Docentes se separa de Académico, la llama por HTTP sin
 * cambiar su puerto (principio V). Devuelve solo <i>records</i> de este paquete, nunca el dominio.
 */
public interface ConsultarReferencias {

    /**
     * Todas las carreras y asignaturas, en cualquier estado, y los 11 campos amplios, por código.
     */
    ReferenciasDeAcademico referencias();
}
