package ec.mdd.firma.puertos.entrada;

import java.time.Instant;

/** Esqueleto (F14-H03, T012). */
public record CadenaDeFirmasCompleta(String periodo, int version, Instant momento) {}
