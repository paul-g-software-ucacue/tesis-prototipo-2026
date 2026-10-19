package ec.mdd.normativa.aplicacion;

import ec.mdd.normativa.dominio.DatosDeVersion;
import ec.mdd.normativa.dominio.VersionNormativa;
import ec.mdd.normativa.puertos.entrada.CodigoRepetido;
import ec.mdd.normativa.puertos.entrada.CrearVersion;
import ec.mdd.normativa.puertos.entrada.VersionNoEncontrada;
import ec.mdd.normativa.puertos.salida.CambioDeNormativa;
import ec.mdd.normativa.puertos.salida.RegistroDeCambios;
import ec.mdd.normativa.puertos.salida.RepositorioVersiones;
import ec.mdd.normativa.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Crear una versión copiando otra. El número se pide primero: su candado ordena las creaciones
 * simultáneas, así que la comprobación del código y el guardado no se cruzan (research §3).
 */
public final class CrearVersionServicio implements CrearVersion {

    private final RepositorioVersiones repositorio;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public CrearVersionServicio(
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
    public VersionNormativa crear(int copiarDe, DatosDeVersion datos, String quien) {
        return transaccion.ejecutar(
                () -> {
                    int numero = repositorio.siguienteNumero();
                    VersionNormativa origen =
                            repositorio
                                    .buscar(copiarDe)
                                    .orElseThrow(() -> new VersionNoEncontrada(copiarDe));
                    if (repositorio.existeCodigo(datos.codigo(), 0)) {
                        throw new CodigoRepetido(datos.codigo());
                    }
                    Instant ahora = reloj.instant();
                    VersionNormativa.Copia copia =
                            VersionNormativa.copiaDe(origen, numero, datos, ahora);
                    repositorio.guardar(copia.version(), quien, ahora);
                    registro.registrar(
                            new CambioDeNormativa(
                                    ahora,
                                    quien,
                                    CambioDeNormativa.Accion.VERSION_CREADA,
                                    Map.of(
                                            "numero",
                                            copia.evento().numero(),
                                            "codigo",
                                            copia.evento().codigo(),
                                            "copiadaDe",
                                            copia.evento().copiadaDe())));
                    return copia.version();
                });
    }
}
