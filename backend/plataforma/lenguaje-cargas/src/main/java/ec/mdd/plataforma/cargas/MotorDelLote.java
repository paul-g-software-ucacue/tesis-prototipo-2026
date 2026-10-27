package ec.mdd.plataforma.cargas;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compara un lote con lo que tiene su dueño y lo aplica todo o nada (RF-10, RF-55, MD-13, ADR-022).
 * La comparación, la huella y el orden de aplicación son iguales para las 10 cargas; lo propio de
 * cada una está en su {@link TraductorDeCarga}. No abre transacciones: {@link #aplicar} corre
 * dentro de la del dueño, y la primera excepción la deshace.
 */
public final class MotorDelLote {

    private MotorDelLote() {}

    /**
     * La vista previa del lote: los nuevos, los que cambian con sus campos, las bajas y los
     * iguales, en orden de clave, con su huella.
     *
     * @throws RechazoDelLote si dos filas tienen la misma clave
     */
    public static <T> VistaPreviaDelLote previsualizar(
            TraductorDeCarga<T> traductor, LoteDeCarga lote) {
        Map<String, T> existentes = traductor.existentes(lote);
        Map<String, FilaDelLote> delArchivo = porClave(traductor, lote);
        List<RegistroDeLaVista> registros = new ArrayList<>();
        delArchivo.forEach(
                (clave, fila) -> {
                    T actual = existentes.get(clave);
                    if (actual == null) {
                        registros.add(
                                new RegistroDeLaVista(
                                        clave, TipoDeRegistro.NUEVO, fila.fila(), List.of()));
                        return;
                    }
                    List<CambioDeCampo> cambios =
                            cambios(traductor.campos(actual), traductor.campos(fila));
                    registros.add(
                            new RegistroDeLaVista(
                                    clave,
                                    cambios.isEmpty()
                                            ? TipoDeRegistro.IGUAL
                                            : TipoDeRegistro.CAMBIA,
                                    fila.fila(),
                                    cambios));
                });
        existentes.keySet().stream()
                .filter(clave -> !delArchivo.containsKey(clave))
                .forEach(
                        clave ->
                                registros.add(
                                        new RegistroDeLaVista(
                                                clave, TipoDeRegistro.BAJA, null, List.of())));
        registros.sort(Comparator.comparing(RegistroDeLaVista::clave));
        return new VistaPreviaDelLote(registros, huella(registros));
    }

    /**
     * Aplica el lote si su vista previa, recalculada con las tablas bloqueadas, es la revisada:
     * registra los nuevos y corrige los que cambian, en el orden del traductor. No toca las bajas
     * ni los iguales. Llámese dentro de la transacción del dueño.
     *
     * @param huella la de la vista previa que se revisó
     * @param quien el usuario que confirma
     * @throws VistaPreviaDesactualizada si la huella cambió; nada se aplica
     * @throws RechazoDelLote si una regla del dueño falla en una fila; la transacción se deshace
     */
    public static <T> ResultadoDelLote aplicar(
            TraductorDeCarga<T> traductor, LoteDeCarga lote, String huella, String quien) {
        traductor.bloquear();
        VistaPreviaDelLote vista = previsualizar(traductor, lote);
        if (!vista.huella().equals(huella)) {
            throw new VistaPreviaDesactualizada();
        }
        return traductor.aplicar(lote, vista, quien);
    }

    /**
     * Aplica la vista previa fila por fila, en el orden del traductor: registra los nuevos y
     * corrige los que cambian. Una excepción del dueño en una fila se rechaza con esa fila.
     *
     * @throws RechazoDelLote si una regla del dueño falla en una fila
     */
    public static <T> ResultadoDelLote filaPorFila(
            TraductorDeCarga<T> traductor,
            LoteDeCarga lote,
            VistaPreviaDelLote vista,
            String quien) {
        Map<String, T> existentes = traductor.existentes(lote);
        Map<Integer, TipoDeRegistro> tipoDeFila = new HashMap<>();
        for (RegistroDeLaVista registro : vista.registros()) {
            if (registro.fila() != null) {
                tipoDeFila.put(registro.fila(), registro.tipo());
            }
        }
        int nuevos = 0;
        int cambiados = 0;
        for (FilaDelLote fila : traductor.enOrden(lote.filas())) {
            TipoDeRegistro tipo = tipoDeFila.get(fila.fila());
            try {
                if (tipo == TipoDeRegistro.NUEVO) {
                    traductor.registrar(fila, quien);
                    nuevos++;
                } else if (tipo == TipoDeRegistro.CAMBIA) {
                    traductor.corregir(fila, existentes.get(traductor.clave(fila)), quien);
                    cambiados++;
                }
            } catch (RechazoDelLote | VistaPreviaDesactualizada error) {
                throw error;
            } catch (RuntimeException error) {
                throw traductor
                        .rechazo(fila, error)
                        .orElseGet(() -> new RechazoDelLote(fila.fila(), error));
            }
        }
        return new ResultadoDelLote(nuevos, cambiados, vista.cuantos(TipoDeRegistro.BAJA));
    }

    private static <T> Map<String, FilaDelLote> porClave(
            TraductorDeCarga<T> traductor, LoteDeCarga lote) {
        Map<String, FilaDelLote> porClave = new java.util.LinkedHashMap<>();
        for (FilaDelLote fila : lote.filas()) {
            FilaDelLote anterior = porClave.putIfAbsent(traductor.clave(fila), fila);
            if (anterior != null) {
                throw new RechazoDelLote(
                        fila.fila(),
                        "Esta clave ya está en la fila " + anterior.fila() + ": no la repitas.");
            }
        }
        return porClave;
    }

    private static List<CambioDeCampo> cambios(
            Map<String, String> ahora, Map<String, String> archivo) {
        Set<String> campos = new LinkedHashSet<>(archivo.keySet());
        campos.addAll(ahora.keySet());
        List<CambioDeCampo> cambios = new ArrayList<>();
        for (String campo : campos) {
            String antes = ahora.getOrDefault(campo, "");
            String despues = archivo.getOrDefault(campo, "");
            if (!Objects.equals(antes, despues)) {
                cambios.add(new CambioDeCampo(campo, antes, despues));
            }
        }
        return cambios;
    }

    /** SHA-256 de la forma canónica: una línea por registro, en orden de clave. */
    static String huella(List<RegistroDeLaVista> registros) {
        StringBuilder canonica = new StringBuilder();
        for (RegistroDeLaVista registro : registros) {
            canonica.append(registro.tipo()).append('\t').append(registro.clave());
            for (CambioDeCampo cambio : registro.cambios()) {
                canonica.append('\t')
                        .append(cambio.campo())
                        .append('=')
                        .append(cambio.ahora())
                        .append('>')
                        .append(cambio.archivo());
            }
            canonica.append('\n');
        }
        return sha256(canonica.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** El SHA-256 de unos bytes, en hexadecimal: el hash de un archivo de carga. */
    public static String sha256(byte[] contenido) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("La JVM no tiene SHA-256", e);
        }
    }
}
