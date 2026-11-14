package ec.mdd.firma.aplicacion;

import ec.mdd.firma.dominio.ResultadoValidacion;
import ec.mdd.firma.puertos.entrada.Solicitante;
import ec.mdd.firma.puertos.entrada.SubirFirma;
import ec.mdd.firma.puertos.salida.AlmacenDeDocumentos;
import ec.mdd.firma.puertos.salida.IdentificacionDeUsuarios;
import ec.mdd.firma.puertos.salida.RegistroDeCambios;
import ec.mdd.firma.puertos.salida.RepositorioDocumentos;
import ec.mdd.firma.puertos.salida.Transaccion;
import ec.mdd.firma.puertos.salida.ValidadorDeFirmas;
import java.time.Clock;
import java.util.UUID;

/** Esqueleto (F14-H02, T010). */
public final class SubirFirmaServicio implements SubirFirma {

    public SubirFirmaServicio(
            RepositorioDocumentos documentos,
            AlmacenDeDocumentos almacen,
            IdentificacionDeUsuarios identificaciones,
            ValidadorDeFirmas validador,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {}

    @Override
    public ResultadoValidacion subir(UUID documento, byte[] archivo, Solicitante quien) {
        throw new UnsupportedOperationException();
    }
}
