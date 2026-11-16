package ec.mdd.validacionocs.aplicacion;

import ec.mdd.validacionocs.dominio.ResolucionOcs;
import ec.mdd.validacionocs.dominio.Transicion;
import ec.mdd.validacionocs.puertos.entrada.DescargarResolucion;
import ec.mdd.validacionocs.puertos.entrada.ResolucionNoEncontrada;
import ec.mdd.validacionocs.puertos.salida.AlmacenDeResoluciones;
import ec.mdd.validacionocs.puertos.salida.RepositorioProceso;

/**
 * Entrega el PDF de la resolución del OCS, idéntico al registrado (F15-H01, D-107): el almacén
 * comprueba su huella antes de devolverlo, así que nunca entrega un archivo dañado.
 */
public final class DescargarResolucionServicio implements DescargarResolucion {

    private final RepositorioProceso procesos;
    private final AlmacenDeResoluciones almacen;

    /**
     * @param procesos los procesos de aprobación, con sus resoluciones
     * @param almacen los PDF de las resoluciones
     */
    public DescargarResolucionServicio(RepositorioProceso procesos, AlmacenDeResoluciones almacen) {
        this.procesos = procesos;
        this.almacen = almacen;
    }

    @Override
    public ArchivoDeLaResolucion descargar(String periodo) {
        ResolucionOcs resolucion =
                procesos.de(periodo)
                        .flatMap(proceso -> proceso.resolucion())
                        .flatMap(Transicion::resolucion)
                        .orElseThrow(ResolucionNoEncontrada::new);
        return new ArchivoDeLaResolucion(resolucion.nombre(), almacen.leer(resolucion.sha256()));
    }
}
