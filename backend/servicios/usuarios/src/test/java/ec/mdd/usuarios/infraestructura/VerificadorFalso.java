package ec.mdd.usuarios.infraestructura;

import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;

/** Doble del verificador: el hash es la clave con un prefijo, y cuenta cuántos calculó. */
final class VerificadorFalso implements VerificadorDeClaves {

    int hashesCalculados;

    @Override
    public String calcularHash(String clave) {
        hashesCalculados++;
        return "hash:" + clave;
    }

    @Override
    public boolean coincide(String clave, String hash) {
        return hash.equals("hash:" + clave);
    }
}
