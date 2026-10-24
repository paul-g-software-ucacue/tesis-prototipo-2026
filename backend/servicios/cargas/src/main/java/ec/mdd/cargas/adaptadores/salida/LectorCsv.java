package ec.mdd.cargas.adaptadores.salida;

import ec.mdd.cargas.dominio.TablaDeDatos;
import ec.mdd.cargas.puertos.salida.ArchivoIlegible;
import ec.mdd.cargas.puertos.salida.CodificacionNoAdmitida;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lee el CSV de una carga como dicen las convenciones del diccionario de datos (research §4):
 * UTF-8, con o sin BOM; coma (RFC 4180) o punto y coma, el que aparezca primero en la fila de
 * encabezados; comillas dobles, con las internas duplicadas y saltos de línea dentro de un valor.
 * Una línea sin valores es una fila vacía. Una fila con otro número de valores que los encabezados,
 * o unas comillas sin cerrar, hacen que el archivo no se pueda leer.
 */
final class LectorCsv {

    private static final char COMILLAS = '"';

    /**
     * Lee el archivo.
     *
     * @param contenido los bytes del CSV
     * @return sus encabezados y sus filas; vacía si el archivo no tiene nada
     * @throws CodificacionNoAdmitida si no es UTF-8 válido
     * @throws ArchivoIlegible si no se puede leer, con la fila
     */
    TablaDeDatos leer(byte[] contenido) {
        String texto = sinBom(utf8(contenido));
        if (texto.isEmpty()) {
            return new TablaDeDatos(List.of(), List.of());
        }
        List<List<String>> registros = registros(texto, separador(texto));
        List<String> encabezados = registros.getFirst();
        List<List<String>> filas = new ArrayList<>();
        for (int i = 1; i < registros.size(); i++) {
            List<String> fila = registros.get(i);
            if (fila.stream().allMatch(String::isEmpty)) {
                filas.add(Collections.nCopies(encabezados.size(), ""));
            } else if (fila.size() != encabezados.size()) {
                throw new ArchivoIlegible(
                        "La fila "
                                + (i + 1)
                                + " tiene "
                                + fila.size()
                                + " valores y los encabezados, "
                                + encabezados.size()
                                + ".");
            } else {
                filas.add(fila);
            }
        }
        return new TablaDeDatos(encabezados, filas);
    }

    private static String utf8(byte[] contenido) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contenido))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new CodificacionNoAdmitida();
        }
    }

    private static String sinBom(String texto) {
        return texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
    }

    /** La coma o el punto y coma que aparece primero, fuera de comillas, en la primera línea. */
    private static char separador(String texto) {
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == COMILLAS) {
                entreComillas = !entreComillas;
            } else if (!entreComillas && (c == ',' || c == ';')) {
                return c;
            } else if (!entreComillas && (c == '\n' || c == '\r')) {
                break;
            }
        }
        return ',';
    }

    /** Los registros según RFC 4180; el fin de línea del final no agrega un registro vacío. */
    private static List<List<String>> registros(String texto, char separador) {
        List<List<String>> registros = new ArrayList<>();
        List<String> registro = new ArrayList<>();
        StringBuilder valor = new StringBuilder();
        boolean entreComillas = false;
        boolean cerradas = false;
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == COMILLAS && i + 1 < texto.length() && texto.charAt(i + 1) == COMILLAS) {
                    valor.append(COMILLAS);
                    i++;
                } else if (c == COMILLAS) {
                    entreComillas = false;
                    cerradas = true;
                } else {
                    valor.append(c);
                }
            } else if (c == separador) {
                registro.add(valor.toString());
                valor.setLength(0);
                cerradas = false;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                registro.add(valor.toString());
                registros.add(registro);
                registro = new ArrayList<>();
                valor.setLength(0);
                cerradas = false;
            } else if (cerradas) {
                throw new ArchivoIlegible(
                        "La fila "
                                + (registros.size() + 1)
                                + " tiene un valor con texto después de cerrar sus comillas.");
            } else if (c == COMILLAS && valor.isEmpty()) {
                entreComillas = true;
            } else {
                valor.append(c);
            }
            i++;
        }
        if (entreComillas) {
            throw new ArchivoIlegible(
                    "La fila " + (registros.size() + 1) + " tiene comillas sin cerrar.");
        }
        if (!valor.isEmpty() || !registro.isEmpty() || cerradas) {
            registro.add(valor.toString());
            registros.add(registro);
        }
        return registros;
    }
}
