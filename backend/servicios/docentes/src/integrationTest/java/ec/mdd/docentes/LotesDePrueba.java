package ec.mdd.docentes;

import ec.mdd.plataforma.cargas.FilaDelLote;
import ec.mdd.plataforma.cargas.LoteDeCarga;
import ec.mdd.plataforma.cargas.MotorDelLote;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Los lotes de las cargas de prueba de la ERS (ficticias, RNF-SEG-06), como los arma Cargas: las
 * filas con sus valores sin espacios y el hash del archivo.
 */
final class LotesDePrueba {

    private LotesDePrueba() {}

    /** El lote de un archivo de prueba, para su carga. */
    static LoteDeCarga lote(String carga, String archivo) {
        byte[] contenido = contenido(archivo);
        return new LoteDeCarga(carga, MotorDelLote.sha256(contenido), filas(contenido));
    }

    /** El lote con las filas cambiadas, y el hash de lo que serían. */
    static LoteDeCarga con(LoteDeCarga lote, List<FilaDelLote> filas) {
        return new LoteDeCarga(
                lote.carga(),
                MotorDelLote.sha256(filas.toString().getBytes(StandardCharsets.UTF_8)),
                filas);
    }

    /** Una fila con un valor cambiado. */
    static FilaDelLote cambiar(FilaDelLote fila, String campo, String valor) {
        Map<String, String> valores = new HashMap<>(fila.valores());
        valores.put(campo, valor);
        return new FilaDelLote(fila.fila(), valores);
    }

    private static byte[] contenido(String archivo) {
        try (InputStream entrada =
                LotesDePrueba.class.getResourceAsStream("/docentes/datos-de-prueba/" + archivo)) {
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Un CSV con comillas dobles (RFC 4180), como los de prueba. */
    private static List<FilaDelLote> filas(byte[] contenido) {
        List<List<String>> registros = new ArrayList<>();
        List<String> registro = new ArrayList<>();
        StringBuilder valor = new StringBuilder();
        String texto = new String(contenido, StandardCharsets.UTF_8).replace("﻿", "");
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    valor.append('"');
                    i++;
                } else if (c == '"') {
                    entreComillas = false;
                } else {
                    valor.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == ',') {
                registro.add(valor.toString());
                valor.setLength(0);
            } else if (c == '\n') {
                registro.add(valor.toString());
                valor.setLength(0);
                registros.add(registro);
                registro = new ArrayList<>();
            } else if (c != '\r') {
                valor.append(c);
            }
        }
        if (!valor.isEmpty() || !registro.isEmpty()) {
            registro.add(valor.toString());
            registros.add(registro);
        }
        List<String> encabezados = registros.getFirst();
        List<FilaDelLote> filas = new ArrayList<>();
        for (int i = 1; i < registros.size(); i++) {
            List<String> celdas = registros.get(i);
            if (celdas.stream().allMatch(String::isBlank)) {
                continue;
            }
            Map<String, String> valores = new HashMap<>();
            for (int c = 0; c < encabezados.size(); c++) {
                valores.put(encabezados.get(c).strip(), celdas.get(c).strip());
            }
            filas.add(new FilaDelLote(i + 1, valores));
        }
        return filas;
    }
}
