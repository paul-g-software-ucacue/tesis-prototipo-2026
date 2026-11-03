package ec.mdd.validacionocs.puertos.entrada;

import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import java.util.Optional;

/**
 * La configuración que rige en un periodo.
 *
 * @param periodo el código del periodo
 * @param configuracion sus etapas y firmas
 * @param carga el archivo cargado; vacío si rige la configuración por defecto
 * @param modificable si admite otra carga
 */
public record ConfiguracionVigente(
        String periodo,
        ConfiguracionFlujo configuracion,
        Optional<CargaGuardada> carga,
        boolean modificable) {}
