package ec.mdd.normativa.puertos.entrada;

import ec.mdd.normativa.dominio.Severidad;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Lo que el panel de inconsistencias necesita de la versión que rige un periodo (RF-29, D-86).
 *
 * @param version el número de la versión
 * @param severidades la severidad de cada regla del panel, por su identificador
 * @param horasMaximasDocente el tope de RN-02
 * @param metaClase el porcentaje de clase de RN-05
 * @param metaOtrasDocencia el porcentaje de otras actividades de docencia de RN-05
 * @param margenPuntos los puntos que un docente puede apartarse de la meta
 * @param gestoresMinimo el mínimo de RN-10
 * @param gestoresMaximo el máximo de RN-10
 * @param actividadesObligatorias los códigos de las actividades que tienen que constar
 */
public record ReglasDelPanelDeLaVersion(
        int version,
        Map<String, Severidad> severidades,
        BigDecimal horasMaximasDocente,
        BigDecimal metaClase,
        BigDecimal metaOtrasDocencia,
        BigDecimal margenPuntos,
        int gestoresMinimo,
        int gestoresMaximo,
        List<String> actividadesObligatorias) {}
