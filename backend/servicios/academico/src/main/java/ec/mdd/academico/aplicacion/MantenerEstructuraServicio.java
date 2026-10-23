package ec.mdd.academico.aplicacion;

import ec.mdd.academico.dominio.EstructuraOrganica;
import ec.mdd.academico.dominio.GestionAutorizada;
import ec.mdd.academico.dominio.TipoDeUnidad;
import ec.mdd.academico.dominio.UnidadOrganica;
import ec.mdd.academico.puertos.entrada.MantenerEstructura;
import ec.mdd.academico.puertos.entrada.UnidadNoEncontrada;
import ec.mdd.academico.puertos.entrada.UnidadRepetida;
import ec.mdd.academico.puertos.salida.CambioRegistrado;
import ec.mdd.academico.puertos.salida.CambioRegistrado.Accion;
import ec.mdd.academico.puertos.salida.RegistroDeCambios;
import ec.mdd.academico.puertos.salida.RepositorioEstructura;
import ec.mdd.academico.puertos.salida.Transaccion;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mantener la estructura orgánica: en una transacción se bloquea la estructura, se carga entera, el
 * dominio valida el cambio y, si algo cambió, se guarda y se registra (research §1). Sin cambios,
 * no se registra.
 */
public final class MantenerEstructuraServicio implements MantenerEstructura {

    private final RepositorioEstructura repositorio;
    private final RegistroDeCambios registro;
    private final Transaccion transaccion;
    private final Clock reloj;

    /** Recibe los puertos de salida, la transacción y el reloj. */
    public MantenerEstructuraServicio(
            RepositorioEstructura repositorio,
            RegistroDeCambios registro,
            Transaccion transaccion,
            Clock reloj) {
        this.repositorio = repositorio;
        this.registro = registro;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public UnidadOrganica registrar(
            String codigo,
            String nombre,
            TipoDeUnidad tipo,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente,
            String quien) {
        UnidadOrganica nueva =
                UnidadOrganica.nueva(
                        codigo, nombre, tipo, padre, gestionAutorizada, comisionPermanente);
        return transaccion.ejecutar(
                () -> {
                    EstructuraOrganica estructura = bloqueada();
                    if (estructura.unidad(codigo).isPresent()) {
                        throw new UnidadRepetida(codigo);
                    }
                    estructura.registrar(nueva);
                    Instant ahora = reloj.instant();
                    // La clave primaria tiene la última palabra.
                    if (!repositorio.registrar(nueva, quien, ahora)) {
                        throw new UnidadRepetida(codigo);
                    }
                    registro.registrar(
                            new CambioRegistrado(
                                    ahora, quien, Accion.UNIDAD_REGISTRADA, datos(nueva)));
                    return nueva;
                });
    }

    @Override
    public UnidadOrganica corregir(
            String codigo,
            String nombre,
            String padre,
            String gestionAutorizada,
            Boolean comisionPermanente,
            String quien) {
        return transaccion.ejecutar(
                () -> {
                    EstructuraOrganica estructura = bloqueada();
                    UnidadOrganica.Correccion correccion =
                            estructura.corregir(
                                    actual(estructura, codigo),
                                    nombre,
                                    padre,
                                    gestionAutorizada,
                                    comisionPermanente);
                    if (!correccion.cambios().isEmpty()) {
                        repositorio.guardar(correccion.unidad());
                        registro.registrar(
                                new CambioRegistrado(
                                        reloj.instant(),
                                        quien,
                                        Accion.UNIDAD_CORREGIDA,
                                        Map.of(
                                                "codigo",
                                                codigo,
                                                "cambios",
                                                correccion.cambios().stream()
                                                        .map(Detalles::de)
                                                        .toList())));
                    }
                    return correccion.unidad();
                });
    }

    @Override
    public UnidadOrganica cambiarEstado(String codigo, boolean activa, String quien) {
        return transaccion.ejecutar(
                () -> {
                    EstructuraOrganica estructura = bloqueada();
                    UnidadOrganica actual = actual(estructura, codigo);
                    UnidadOrganica nueva = estructura.cambiarEstado(actual, activa);
                    if (nueva.activa() != actual.activa()) {
                        repositorio.guardar(nueva);
                        registro.registrar(
                                new CambioRegistrado(
                                        reloj.instant(),
                                        quien,
                                        activa
                                                ? Accion.UNIDAD_REACTIVADA
                                                : Accion.UNIDAD_INACTIVADA,
                                        Map.of("codigo", codigo)));
                    }
                    return nueva;
                });
    }

    private EstructuraOrganica bloqueada() {
        repositorio.bloquear();
        return new EstructuraOrganica(
                repositorio.todas(),
                repositorio.catalogo().stream().map(GestionAutorizada::codigo).toList());
    }

    private static UnidadOrganica actual(EstructuraOrganica estructura, String codigo) {
        return estructura.unidad(codigo).orElseThrow(() -> new UnidadNoEncontrada(codigo));
    }

    // Solo los datos que tiene: el registro no guarda valores vacíos.
    private static Map<String, Object> datos(UnidadOrganica unidad) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("codigo", unidad.codigo());
        datos.put("nombre", unidad.nombre());
        datos.put("tipo", unidad.tipo().name());
        if (unidad.padre() != null) {
            datos.put("padre", unidad.padre());
        }
        if (unidad.gestionAutorizada() != null) {
            datos.put("gestionAutorizada", unidad.gestionAutorizada());
        }
        if (unidad.comisionPermanente() != null) {
            datos.put("comisionPermanente", unidad.comisionPermanente());
        }
        return datos;
    }
}
