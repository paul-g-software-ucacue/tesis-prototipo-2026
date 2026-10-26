package ec.mdd.cargas.aplicacion;

import ec.mdd.cargas.dominio.EntradaDeHistorial;
import ec.mdd.cargas.dominio.MotivoDeRechazo;
import ec.mdd.cargas.dominio.ResultadoDeHistorial;
import ec.mdd.cargas.puertos.entrada.ArchivoSubido;
import ec.mdd.cargas.puertos.entrada.CargaConErrores;
import ec.mdd.cargas.puertos.entrada.Confirmacion;
import ec.mdd.cargas.puertos.entrada.ConfirmarCarga;
import ec.mdd.cargas.puertos.salida.HistorialDeCargas;
import ec.mdd.cargas.puertos.salida.Transaccion;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.RechazoDelLote;
import ec.mdd.plataforma.cargas.ResultadoDelLote;
import ec.mdd.plataforma.cargas.VistaPreviaDesactualizada;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Confirmar una carga (RF-10, MD-13, F01-H04): valida otra vez el archivo, que no se guardó entre
 * la vista previa y la confirmación; si el mismo archivo ya se aplicó en esa carga, responde
 * repetida sin llamar al dueño; si no, el dueño aplica la vista previa revisada en su transacción,
 * completa o nada. Cada intento queda en el historial, en la transacción de Cargas, después de la
 * respuesta del dueño (research §8).
 */
public final class ConfirmarCargaServicio implements ConfirmarCarga {

    private final ValidarArchivoServicio validar;
    private final DuenosDeLasCargas duenos;
    private final HistorialDeCargas historial;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe la validación, los dueños, el historial, su transacción y el reloj. */
    public ConfirmarCargaServicio(
            ValidarArchivoServicio validar,
            DuenosDeLasCargas duenos,
            HistorialDeCargas historial,
            Transaccion transaccion,
            Clock reloj) {
        this.validar = validar;
        this.duenos = duenos;
        this.historial = historial;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public Confirmacion confirmar(
            String codigo, ArchivoSubido archivo, String huella, String quien) {
        ValidarArchivoServicio.Revision revision = validar.revisar(codigo, archivo);
        LoteDeCarga lote = revision.lote();
        Registro registro = new Registro(lote, archivo.nombre(), quien);
        if (!revision.informe().valido()) {
            registro.rechazo(MotivoDeRechazo.CON_ERRORES);
            throw new CargaConErrores(revision.informe().filasConErrores());
        }
        Optional<EntradaDeHistorial> primera = historial.aplicada(lote.carga(), lote.hash());
        if (primera.isPresent()) {
            registro.guardar(ResultadoDeHistorial.REPETIDA, new ResultadoDelLote(0, 0, 0));
            return new Confirmacion(
                    ResultadoDeHistorial.REPETIDA, 0, 0, 0, primera.get().momento());
        }
        ResultadoDelLote resultado;
        try {
            resultado = duenos.de(lote.carga()).aplicar(lote, huella, quien);
        } catch (VistaPreviaDesactualizada error) {
            registro.rechazo(MotivoDeRechazo.VISTA_PREVIA_DESACTUALIZADA);
            throw error;
        } catch (RechazoDelLote error) {
            registro.rechazo(MotivoDeRechazo.RECHAZADA_POR_EL_DUENO);
            throw error;
        }
        Instant momento = registro.guardar(ResultadoDeHistorial.APLICADA, resultado);
        return new Confirmacion(
                ResultadoDeHistorial.APLICADA,
                resultado.nuevos(),
                resultado.cambiados(),
                resultado.bajasSinAplicar(),
                momento);
    }

    /** El intento que se registra en el historial. */
    private final class Registro {

        private final LoteDeCarga lote;
        private final String archivo;
        private final String quien;

        Registro(LoteDeCarga lote, String archivo, String quien) {
            this.lote = lote;
            this.archivo = archivo;
            this.quien = quien;
        }

        void rechazo(MotivoDeRechazo motivo) {
            guardar(
                    ResultadoDeHistorial.RECHAZADA,
                    new ResultadoDelLote(0, 0, 0),
                    Optional.of(motivo));
        }

        Instant guardar(ResultadoDeHistorial resultado, ResultadoDelLote totales) {
            return guardar(resultado, totales, Optional.empty());
        }

        private Instant guardar(
                ResultadoDeHistorial resultado,
                ResultadoDelLote totales,
                Optional<MotivoDeRechazo> motivo) {
            Instant ahora = reloj.instant();
            EntradaDeHistorial entrada =
                    new EntradaDeHistorial(
                            lote.carga(),
                            quien,
                            ahora,
                            archivo,
                            lote.hash(),
                            resultado,
                            totales.nuevos(),
                            totales.cambiados(),
                            totales.bajasSinAplicar(),
                            motivo);
            transaccion.ejecutar(
                    () -> {
                        historial.registrar(entrada);
                        return null;
                    });
            return ahora;
        }
    }
}
