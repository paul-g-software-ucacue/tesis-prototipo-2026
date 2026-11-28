package ec.mdd.reporting.aplicacion;

import ec.mdd.reporting.dominio.ColumnaDelLayout;
import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.ValidacionDelLayout;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados;
import ec.mdd.reporting.puertos.salida.LayoutsGuardados.LayoutGuardado;
import ec.mdd.reporting.puertos.salida.LectorDeLayout;
import ec.mdd.reporting.puertos.salida.LectorDeLayout.LecturaDelLayout;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Arma el layout de exportación a partir de su texto guardado o de la plantilla (D-119). Lo usan
 * los dos casos de uso: generar el CSV y consultar o cargar el layout.
 *
 * <p>Un texto guardado ya se validó al cargarlo, y las reglas no dependen de datos externos, así
 * que siempre vuelve a valer. Si no, es un error del sistema, no del usuario.
 */
final class LayoutVigente {

    private final LayoutsGuardados guardados;
    private final LectorDeLayout lector;

    LayoutVigente(LayoutsGuardados guardados, LectorDeLayout lector) {
        this.guardados = guardados;
        this.lector = lector;
    }

    /** El último cargado o, si no hay, el por defecto. Se lee una vez por llamada. */
    LayoutExportacion vigente() {
        return guardados.vigente().map(this::de).orElseGet(this::porDefecto);
    }

    LayoutExportacion porDefecto() {
        LecturaDelLayout lectura =
                lector.leer(guardados.porDefecto().getBytes(StandardCharsets.UTF_8));
        return LayoutExportacion.porDefecto(lectura.texto(), columnas(lectura, 0));
    }

    LayoutExportacion de(LayoutGuardado guardado) {
        LecturaDelLayout lectura = lector.leer(guardado.texto().getBytes(StandardCharsets.UTF_8));
        return LayoutExportacion.cargado(
                guardado.numero(),
                lectura.texto(),
                columnas(lectura, guardado.numero()),
                guardado.cargadoPor(),
                guardado.cargadoEn());
    }

    List<LayoutGuardado> todos() {
        return guardados.todos();
    }

    private static List<ColumnaDelLayout> columnas(LecturaDelLayout lectura, int numero) {
        ValidacionDelLayout validacion = LayoutExportacion.validar(lectura.borrador());
        if (!validacion.esValida()) {
            throw new IllegalStateException(
                    "La versión "
                            + numero
                            + " del layout de exportación guardada ya no es válida.");
        }
        return validacion.columnas();
    }
}
