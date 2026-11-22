package ec.mdd.reporting.aplicacion;

import ec.mdd.reporting.dominio.LayoutCargado;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.LayoutInvalido;
import ec.mdd.reporting.dominio.ValidacionDelLayout;
import ec.mdd.reporting.puertos.entrada.GestionarLayoutSiga;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados.LayoutGuardado;
import ec.mdd.reporting.puertos.salida.LectorDeLayout;
import ec.mdd.reporting.puertos.salida.LectorDeLayout.LecturaDelLayout;
import ec.mdd.reporting.puertos.salida.RegistroEnBitacora;
import ec.mdd.reporting.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * El layout de exportación (F19-H01, D-119). La carga se valida entera antes de tocar la base; si
 * vale, la fila y su entrada en la bitácora van en una sola transacción, así que no queda una sin
 * la otra.
 */
public final class GestionarLayoutSigaServicio implements GestionarLayoutSiga {

    private final LayoutVigente layouts;
    private final LectorDeLayout lector;
    private final LayoutsGuardados guardados;
    private final RegistroEnBitacora registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    GestionarLayoutSigaServicio(
            LayoutVigente layouts,
            LectorDeLayout lector,
            LayoutsGuardados guardados,
            RegistroEnBitacora registro,
            Transaccion transaccion,
            Clock reloj) {
        this.layouts = layouts;
        this.lector = lector;
        this.guardados = guardados;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    /**
     * @param guardados las versiones del layout de exportación
     * @param lector el lector del archivo de mapeo
     * @param registro la bitácora
     * @param transaccion la transacción de la carga
     * @param reloj cuándo
     * @return el servicio
     */
    public static GestionarLayoutSigaServicio de(
            LayoutsGuardados guardados,
            LectorDeLayout lector,
            RegistroEnBitacora registro,
            Transaccion transaccion,
            Clock reloj) {
        return new GestionarLayoutSigaServicio(
                new LayoutVigente(guardados, lector),
                lector,
                guardados,
                registro,
                transaccion,
                reloj);
    }

    @Override
    public LayoutsSiga consultar() {
        List<LayoutGuardado> todos = layouts.todos();
        if (todos.isEmpty()) {
            return new LayoutsSiga(layouts.porDefecto(), List.of());
        }
        return new LayoutsSiga(
                layouts.de(todos.get(0)),
                todos.subList(1, todos.size()).stream().map(layouts::de).toList());
    }

    @Override
    public String plantilla() {
        return guardados.porDefecto();
    }

    @Override
    public LayoutExportacion cargar(byte[] archivo, String quien) {
        LecturaDelLayout lectura = lector.leer(archivo);
        ValidacionDelLayout validacion = LayoutExportacion.validar(lectura.borrador());
        if (!validacion.esValida()) {
            throw new LayoutInvalido(validacion.problemas());
        }
        String sha256 = LayoutExportacion.huella(lectura.texto());
        Instant momento = reloj.instant();
        int numero =
                transaccion.ejecutar(
                        () -> {
                            int guardado =
                                    guardados.guardar(lectura.texto(), sha256, quien, momento);
                            registro.registrar(
                                    new LayoutCargado(
                                            quien,
                                            momento,
                                            guardado,
                                            validacion.columnas().size(),
                                            sha256));
                            return guardado;
                        });
        return LayoutExportacion.cargado(
                numero, lectura.texto(), validacion.columnas(), quien, momento);
    }
}
