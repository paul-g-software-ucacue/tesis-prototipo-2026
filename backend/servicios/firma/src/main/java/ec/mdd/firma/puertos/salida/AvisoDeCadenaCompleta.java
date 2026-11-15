package ec.mdd.firma.puertos.salida;

import ec.mdd.firma.puertos.entrada.CadenaDeFirmasCompleta;

/** Esqueleto (F14-H03, T012). */
@FunctionalInterface
public interface AvisoDeCadenaCompleta {

    void avisar(CadenaDeFirmasCompleta aviso);
}
