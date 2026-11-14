package ec.mdd.firma.adaptadores.salida;

import ec.mdd.firma.puertos.salida.ValidadorDeFirmas;

/** Esqueleto (F14-H02, T008). */
public final class ValidadorPades implements ValidadorDeFirmas {

    public ValidadorPades(ListaConfianza lista) {}

    @Override
    public HallazgosDeLaFirma validar(byte[] vigente, byte[] subido) {
        throw new UnsupportedOperationException();
    }
}
