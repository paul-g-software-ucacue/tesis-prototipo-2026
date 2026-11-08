package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import ec.mdd.validacionocs.dominio.EtapaDelProceso;
import ec.mdd.validacionocs.puertos.salida.LectorDeConfiguracion;
import ec.mdd.validacionocs.puertos.salida.PlantillaDelFlujo;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import java.time.Instant;
import java.util.List;

/**
 * Las etapas que regían cuando se creó un distributivo (F12-H02, FR-003, research §5): las de la
 * última carga anterior a su creación o, si no había, las de la configuración por defecto. Las
 * cargas son solo de inserción, así que se pueden reconstruir.
 */
public final class EtapasAlCrearse {

    private final RepositorioConfiguracion configuraciones;
    private final LectorDeConfiguracion lector;
    private final PlantillaDelFlujo plantilla;

    /**
     * @param configuraciones las cargas de cada periodo
     * @param lector lee un archivo de configuración
     * @param plantilla la configuración por defecto
     */
    public EtapasAlCrearse(
            RepositorioConfiguracion configuraciones,
            LectorDeConfiguracion lector,
            PlantillaDelFlujo plantilla) {
        this.configuraciones = configuraciones;
        this.lector = lector;
        this.plantilla = plantilla;
    }

    /**
     * @param periodo el código del periodo
     * @param creadoEn cuándo se creó el distributivo
     * @return las 9 etapas en orden, como regían entonces
     */
    public List<EtapaDelProceso> de(String periodo, Instant creadoEn) {
        String texto =
                configuraciones
                        .vigenteEn(periodo, creadoEn)
                        .map(CargaGuardada::contenido)
                        .orElseGet(plantilla::texto);
        // Lo guardado ya se validó al cargarlo, como en ConsultarConfiguracionServicio.
        return ConfiguracionFlujo.revisar(lector.leer(texto))
                .configuracion()
                .orElseThrow(() -> new IllegalStateException("La configuración guardada no vale"))
                .etapas()
                .stream()
                .map(EtapaDelProceso::de)
                .toList();
    }
}
