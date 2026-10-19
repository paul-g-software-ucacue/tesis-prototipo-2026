package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.CambioDeValor;
import ec.mdd.normativa.dominio.CambiosDeVersion;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.CodigoRepetido;
import ec.mdd.normativa.puertos.entrada.EditarVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.RegistroDeCambios;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import ec.mdd.normativa.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Editar una versión. La versión se bloquea antes de comprobar si está en uso, así que una edición
 * no se cuela después de que otra transacción la asocie a un periodo (research §3).
 */
public final class EditarVersionServicio implements EditarVersion {

    private final RepositorioVersiones repositorio;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public EditarVersionServicio(
            RepositorioVersiones repositorio,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public VersionNormativa editar(int numero, CambiosDeVersion cambios, String quien) {
        return transaccion.ejecutar(
                () -> {
                    VersionNormativa actual =
                            repositorio
                                    .bloquear(numero)
                                    .orElseThrow(() -> new VersionNoEncontrada(numero));
                    VersionNormativa.Edicion edicion = actual.editada(cambios);
                    if (edicion.cambios().isEmpty()) {
                        return actual;
                    }
                    String codigo = cambios.datos().codigo();
                    if (!codigo.equals(actual.codigo())
                            && repositorio.existeCodigo(codigo, numero)) {
                        throw new CodigoRepetido(codigo);
                    }
                    Instant ahora = reloj.instant();
                    repositorio.guardar(edicion.version(), quien, ahora);
                    registro.registrar(
                            new CambioDeNormativa(
                                    ahora,
                                    quien,
                                    CambioDeNormativa.Accion.VERSION_EDITADA,
                                    Map.of(
                                            "numero",
                                            numero,
                                            "cambios",
                                            detalle(edicion.cambios()))));
                    return edicion.version();
                });
    }

    // Un mapa por cambio, con null cuando no había o ya no hay valor.
    private static List<Map<String, Object>> detalle(List<CambioDeValor> cambios) {
        return cambios.stream()
                .map(
                        cambio -> {
                            Map<String, Object> uno = new LinkedHashMap<>();
                            uno.put("campo", cambio.campo());
                            uno.put("anterior", cambio.anterior());
                            uno.put("nuevo", cambio.nuevo());
                            return uno;
                        })
                .toList();
    }
}
