package ec.mdd.docentes.dominio;

import java.math.BigDecimal;

/**
 * Un docente que trabaja en un proyecto aprobado (glosario «Participante», carga 9). Que el docente
 * esté registrado lo comprueba {@link Proyecto#verificarDocentes}.
 *
 * @param docente la identificación del docente
 * @param rol director, investigador, docente de apoyo o tutor de prácticas
 * @param horas sus horas semanales en el proyecto
 */
public record Participante(String docente, RolDeParticipante rol, BigDecimal horas) {}
