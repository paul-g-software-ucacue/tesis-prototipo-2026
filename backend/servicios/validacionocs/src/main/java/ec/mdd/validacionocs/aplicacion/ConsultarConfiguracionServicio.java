package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.CargaGuardada;
import ec.mdd.validacionocs.dominio.ConfiguracionFlujo;
import ec.mdd.validacionocs.puertos.entrada.ConfiguracionVigente;
import ec.mdd.validacionocs.puertos.entrada.ConsultarConfiguracion;
import ec.mdd.validacionocs.puertos.entrada.PeriodoNoEncontrado;
import ec.mdd.validacionocs.puertos.salida.DatosDelPeriodo;
import ec.mdd.validacionocs.puertos.salida.LectorDeConfiguracion;
import ec.mdd.validacionocs.puertos.salida.PlantillaDelFlujo;
import ec.mdd.validacionocs.puertos.salida.RepositorioConfiguracion;
import java.util.Optional;

/** Lo que rige en un periodo: la última cargada o, si no hay, la de por defecto (D-89). */
public final class ConsultarConfiguracionServicio implements ConsultarConfiguracion {

    private final DatosDelPeriodo datos;
    private final LectorDeConfiguracion lector;
    private final RepositorioConfiguracion repositorio;
    private final PlantillaDelFlujo plantilla;

    /** Recibe los puertos. */
    public ConsultarConfiguracionServicio(
            DatosDelPeriodo datos,
            LectorDeConfiguracion lector,
            RepositorioConfiguracion repositorio,
            PlantillaDelFlujo plantilla) {
        this.datos = datos;
        this.lector = lector;
        this.repositorio = repositorio;
        this.plantilla = plantilla;
    }

    @Override
    public ConfiguracionVigente dePeriodo(String periodo) {
        boolean enPlanificacion =
                datos.estado(periodo).orElseThrow(PeriodoNoEncontrado::new).enPlanificacion();
        Optional<CargaGuardada> carga = repositorio.vigente(periodo);
        String texto = carga.map(CargaGuardada::contenido).orElseGet(plantilla::texto);
        return new ConfiguracionVigente(periodo, deTexto(texto), carga, enPlanificacion);
    }

    // Lo guardado ya se validó al cargarlo: si ahora no vale, cambió una regla y hay que
    // corregirlo.
    private ConfiguracionFlujo deTexto(String texto) {
        return ConfiguracionFlujo.revisar(lector.leer(texto))
                .configuracion()
                .orElseThrow(() -> new IllegalStateException("La configuración guardada no vale"));
    }

    @Override
    public String plantilla() {
        return plantilla.texto();
    }
}
